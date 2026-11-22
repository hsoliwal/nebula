/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * SWT RFB framebuffer viewer. Network work runs on RfbClient's threads;
 * immutable framebuffer versions are coalesced and converted into SWT Images
 * only on this widget's Display thread. Disposing the widget closes its socket.
 */
public final class VncCanvas extends Canvas {

    public interface Listener {
        default void connected(String title, int width, int height) { }
        default void disconnected(IOException cause) { }
        default void bell() { }
        default void clipboard(String text) { }
    }

    private record PendingFrame(long generation, RfbClient.Frame snapshot) { }

    private final Display display;
    private final AtomicReference<PendingFrame> pending = new AtomicReference<>();
    private final AtomicBoolean redrawQueued = new AtomicBoolean();
    private final AtomicLong sessionGeneration = new AtomicLong();
    private volatile RfbClient client;
    private Listener listener = new Listener() { };
    private RfbClient.Frame frame;
    private Image image;
    private int buttonMask;
    private int lastRemoteX;
    private int lastRemoteY;
    // Only accessed on SWT's Display thread, not on the RFB I/O threads.
    private final Map<Long, Integer> keysDown = new HashMap<>();

    public VncCanvas(Composite parent, int style) {
        super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        display = getDisplay();
        addPaintListener(event -> paintFrame(event.gc, getClientArea()));
        addMouseMoveListener(this::forwardPointer);
        addMouseListener(new org.eclipse.swt.events.MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                setFocus();
                buttonMask |= buttonBit(e.button);
                setCapture(true);
                forwardPointer(e);
            }

            @Override
            public void mouseUp(MouseEvent e) {
                buttonMask &= ~buttonBit(e.button);
                forwardPointer(e);
                if (buttonMask == 0) {
                    setCapture(false);
                }
            }
        });
        addMouseWheelListener(e -> {
            if (client == null) {
                return;
            }
            int clicks = (int) Math.min(16L, Math.abs((long) e.count));
            int wheelBit = e.count > 0 ? 8 : 16;
            for (int i = 0; i < clicks; i++) {
                sendPointer(e.x, e.y, buttonMask | wheelBit);
                sendPointer(e.x, e.y, buttonMask);
            }
        });
        addKeyListener(new org.eclipse.swt.events.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                sendKey(e, true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                sendKey(e, false);
            }
        });
        addFocusListener(new org.eclipse.swt.events.FocusAdapter() {
            @Override
            public void focusLost(org.eclipse.swt.events.FocusEvent event) {
                releaseRemoteInput();
            }
        });
        addDisposeListener(e -> {
            sessionGeneration.incrementAndGet();
            RfbClient old = client;
            client = null;
            if (old != null) {
                old.close();
            }
            pending.set(null);
            if (image != null) {
                image.dispose();
                image = null;
            }
        });
    }

    public void setSessionListener(Listener listener) {
        checkWidget();
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Opens asynchronously. Only localhost is permitted by default. To use a
     * remote server, establish a local SSH tunnel or explicitly acknowledge
     * the lack of RFB transport encryption.
     */
    public void connect(String host, int port, char[] password, boolean allowUnencryptedRemote) {
        checkWidget();
        disconnect();
        long session = sessionGeneration.incrementAndGet();
        RfbClient next = createListenerClient(session);
        client = next;
        next.connectAsync(host, port, password, allowUnencryptedRemote);
    }

    private RfbClient createListenerClient(long session) {
        return new RfbClient(new RfbClient.Listener() {
            @Override
            public void connected(String name, int width, int height) {
                onUi(session, () -> {
                    setToolTipText(name);
                    listener().connected(name, width, height);
                });
            }

            @Override
            public void frame(RfbClient.Frame snapshot) {
                // A previous generation may report one last frame just as a
                // new session is opened. Guard the atomic publication AND the
                // SWT consumer with the generation, not just a prior read.
                pending.updateAndGet(previous -> {
                    if (sessionGeneration.get() != session) {
                        return previous;
                    }
                    RfbClient.Frame prior = previous != null
                            && previous.generation() == session
                            ? previous.snapshot() : null;
                    return new PendingFrame(session, snapshot.withAdditionalDamage(prior));
                });
                if (sessionGeneration.get() == session) {
                    scheduleFrame();
                }
            }

            @Override
            public void bell() {
                onUi(session, () -> listener().bell());
            }

            @Override
            public void clipboard(String text) {
                onUi(session, () -> listener().clipboard(text));
            }

            @Override
            public void disconnected(IOException cause) {
                onUi(session, () -> listener().disconnected(cause));
            }
        });
    }

    /** Connect to an RFB server wrapped in direct TLS with strict certificate
     * and hostname validation, without plaintext fallback. */
    public void connectTls(String host, int port, char[] password) {
        checkWidget();
        disconnect();
        long session = sessionGeneration.incrementAndGet();
        RfbClient next = createListenerClient(session);
        client = next;
        next.connectTlsAsync(host, port, password);
    }

    /**
     * Negotiate VeNCrypt 0.2 with a certificate-validated X509None/X509Vnc
     * subtype. This never falls back to unencrypted or anonymous TLS.
     */
    public void connectVeNCrypt(String host, int port, char[] password) {
        checkWidget();
        disconnect();
        long session = sessionGeneration.incrementAndGet();
        RfbClient next = createListenerClient(session);
        client = next;
        next.connectVeNCryptAsync(host, port, password);
    }

    public void disconnect() {
        checkWidget();
        releaseRemoteInput();
        sessionGeneration.incrementAndGet();
        RfbClient old = client;
        client = null;
        if (old != null) {
            old.close();
        }
        buttonMask = 0;
        pending.set(null);
        frame = null;
        if (image != null) {
            image.dispose();
            image = null;
        }
        redraw();
    }

    public boolean requestRefresh() {
        checkWidget();
        return client != null && client.requestRefresh();
    }

    public boolean sendClipboard(String text) {
        checkWidget();
        return client != null && client.sendClipboard(text);
    }

    public RfbClient.Frame getFrame() {
        checkWidget();
        return frame;
    }

    private Listener listener() {
        return listener;
    }

    private void onUi(long session, Runnable action) {
        if (!display.isDisposed() && sessionGeneration.get() == session) {
            display.asyncExec(() -> {
                if (!isDisposed() && sessionGeneration.get() == session) {
                    action.run();
                }
            });
        }
    }

    private void scheduleFrame() {
        if (display.isDisposed() || !redrawQueued.compareAndSet(false, true)) {
            return;
        }
        display.asyncExec(() -> {
            redrawQueued.set(false);
            PendingFrame queued = pending.getAndSet(null);
            if (isDisposed() || queued == null
                    || queued.generation() != sessionGeneration.get()) {
                return;
            }
            RfbClient.Frame latest = queued.snapshot();
            // Keep the existing native Image for small updates. Only the
            // dirty rectangle becomes a temporary SWT Image, so a 4K desktop
            // with a blinking caret does not churn full-screen native bitmaps.
            Rectangle existingBounds = image == null ? null : image.getBounds();
            boolean dimensionsMatch = existingBounds != null
                    && existingBounds.width == latest.width()
                    && existingBounds.height == latest.height();
            long dirtyArea = (long) latest.dirtyWidth() * latest.dirtyHeight();
            long frameArea = (long) latest.width() * latest.height();
            boolean partial = dimensionsMatch && dirtyArea > 0
                    && dirtyArea * 2 < frameArea;
            int x = partial ? latest.dirtyX() : 0;
            int y = partial ? latest.dirtyY() : 0;
            int w = partial ? latest.dirtyWidth() : latest.width();
            int h = partial ? latest.dirtyHeight() : latest.height();
            ImageData data = new ImageData(w, h, 24,
                    new PaletteData(0xff0000, 0xff00, 0xff));
            int[] pixels = latest.rgbForDisplay();
            for (int row = 0; row < h; row++) {
                data.setPixels(0, row, w, pixels, (y + row) * latest.width() + x);
            }
            Image replacement = new Image(display, data);
            if (partial) {
                try {
                    GC target = new GC(image);
                    try {
                        target.drawImage(replacement, x, y);
                    } finally {
                        target.dispose();
                    }
                } finally {
                    replacement.dispose();
                }
                frame = latest;
                redrawDamaged(latest);
            } else {
                Image previous = image;
                image = replacement;
                frame = latest;
                if (previous != null) {
                    previous.dispose();
                }
                redraw();
            }
        });
    }

    /** Map changed remote pixels into the scaled/letterboxed SWT viewport. */
    private void redrawDamaged(RfbClient.Frame snapshot) {
        Rectangle client = getClientArea();
        Rectangle target = destination(client);
        if (target.width == 0 || target.height == 0) {
            return;
        }
        int left = target.x + (int) Math.floor(
                snapshot.dirtyX() * (double) target.width / snapshot.width()) - 2;
        int top = target.y + (int) Math.floor(
                snapshot.dirtyY() * (double) target.height / snapshot.height()) - 2;
        int right = target.x + (int) Math.ceil(
                (snapshot.dirtyX() + snapshot.dirtyWidth()) *
                (double) target.width / snapshot.width()) + 2;
        int bottom = target.y + (int) Math.ceil(
                (snapshot.dirtyY() + snapshot.dirtyHeight()) *
                (double) target.height / snapshot.height()) + 2;
        left = Math.max(client.x, left);
        top = Math.max(client.y, top);
        right = Math.min(client.x + client.width, right);
        bottom = Math.min(client.y + client.height, bottom);
        if (right > left && bottom > top) {
            redraw(left, top, right - left, bottom - top, false);
        }
    }

    private void paintFrame(org.eclipse.swt.graphics.GC gc, Rectangle area) {
        gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
        gc.fillRectangle(area);
        if (image == null || frame == null) {
            return;
        }
        Rectangle target = destination(area);
        if (target.width > 0 && target.height > 0) {
            gc.drawImage(image, 0, 0, frame.width(), frame.height(),
                    target.x, target.y, target.width, target.height);
        }
    }

    private Rectangle destination(Rectangle area) {
        if (frame == null || area.width <= 0 || area.height <= 0) {
            return new Rectangle(0, 0, 0, 0);
        }
        double scale = Math.min(area.width / (double) frame.width(),
                area.height / (double) frame.height());
        int w = Math.max(1, (int) Math.round(frame.width() * scale));
        int h = Math.max(1, (int) Math.round(frame.height() * scale));
        return new Rectangle(area.x + (area.width - w) / 2,
                area.y + (area.height - h) / 2, w, h);
    }

    private static int buttonBit(int button) {
        return switch (button) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            default -> 0;
        };
    }

    private void forwardPointer(MouseEvent e) {
        sendPointer(e.x, e.y, buttonMask);
    }

    private void sendPointer(int x, int y, int mask) {
        RfbClient current = client;
        if (current == null || frame == null) {
            return;
        }
        Rectangle d = destination(getClientArea());
        if (d.width == 0 || d.height == 0) {
            return;
        }
        int translatedX = Math.max(0, Math.min(frame.width() - 1,
                (int) (((long) (x - d.x) * frame.width()) / d.width)));
        int translatedY = Math.max(0, Math.min(frame.height() - 1,
                (int) (((long) (y - d.y) * frame.height()) / d.height)));
        lastRemoteX = translatedX;
        lastRemoteY = translatedY;
        current.pointer(mask, translatedX, translatedY);
    }

    private void sendKey(KeyEvent e, boolean down) {
        RfbClient current = client;
        if (current == null) {
            return;
        }
        // KeyReleased often carries character=0 (or a changed case/layout).
        // Remember the exact keysym sent for KeyPressed and release THAT symbol.
        long identity = (((long) (e.keyCode != 0 ? e.keyCode : e.character)) << 32)
                | (e.keyLocation & 0xffffffffL);
        if (down) {
            int keysym = toKeysym(e);
            if (keysym != 0) {
                Integer previous = keysDown.put(identity, keysym);
                if (previous != null && previous.intValue() != keysym) {
                    current.key(false, previous);
                }
                current.key(true, keysym);
            }
        } else {
            Integer remembered = keysDown.remove(identity);
            int keysym = remembered != null ? remembered : toKeysym(e);
            if (keysym != 0) {
                current.key(false, keysym);
            }
        }
    }

    private void releaseRemoteInput() {
        RfbClient current = client;
        if (current != null) {
            for (int keysym : keysDown.values()) {
                current.key(false, keysym);
            }
            if (buttonMask != 0 && frame != null) {
                current.pointer(0, lastRemoteX, lastRemoteY);
            }
        }
        keysDown.clear();
        if (buttonMask != 0 && !isDisposed()) {
            setCapture(false);
        }
        buttonMask = 0;
    }

    private static int toKeysym(KeyEvent event) {
        return switch (event.keyCode) {
            case SWT.ARROW_LEFT -> 0xff51;
            case SWT.ARROW_UP -> 0xff52;
            case SWT.ARROW_RIGHT -> 0xff53;
            case SWT.ARROW_DOWN -> 0xff54;
            case SWT.HOME -> 0xff50;
            case SWT.END -> 0xff57;
            case SWT.PAGE_UP -> 0xff55;
            case SWT.PAGE_DOWN -> 0xff56;
            case SWT.INSERT -> 0xff63;
            case SWT.DEL -> 0xffff;
            case SWT.BS -> 0xff08;
            case SWT.TAB -> 0xff09;
            case SWT.CR, SWT.KEYPAD_CR -> 0xff0d;
            case SWT.ESC -> 0xff1b;
            case SWT.F1 -> 0xffbe;
            case SWT.F2 -> 0xffbf;
            case SWT.F3 -> 0xffc0;
            case SWT.F4 -> 0xffc1;
            case SWT.F5 -> 0xffc2;
            case SWT.F6 -> 0xffc3;
            case SWT.F7 -> 0xffc4;
            case SWT.F8 -> 0xffc5;
            case SWT.F9 -> 0xffc6;
            case SWT.F10 -> 0xffc7;
            case SWT.F11 -> 0xffc8;
            case SWT.F12 -> 0xffc9;
            case SWT.SHIFT -> 0xffe1;
            case SWT.CTRL -> 0xffe3;
            case SWT.ALT -> 0xffe9;
            default -> {
                int codepoint = event.character;
                // Ctrl+letter may arrive as a C0 control character; RFB
                // still needs the underlying alphabetic keysym with Ctrl down.
                if (codepoint < 0x20 && event.keyCode >= 'a'
                        && event.keyCode <= 'z') {
                    yield event.keyCode;
                }
                if (codepoint >= 0x20 && codepoint <= 0xff) {
                    yield codepoint;
                }
                yield codepoint > 0xff ? 0x01000000 | codepoint : 0;
            }
        };
    }
}
