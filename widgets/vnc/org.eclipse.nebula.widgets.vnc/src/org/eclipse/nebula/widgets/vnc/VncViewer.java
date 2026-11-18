/*
 * SPDX-License-Identifier: EPL-2.0
 *
 * The SWT rendering layer of Nebula's RFB remote desktop widget.
 */
package org.eclipse.nebula.widgets.vnc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

/**
 * SWT VNC desktop with a CPU-owned RFB framebuffer. IO and decoding are handled
 * by a worker; only the UI thread owns SWT Image, GC and input dispatch.
 *
 * <p>Plain VNC and VNC password authentication do not encrypt traffic. The
 * standard host/port connector intentionally permits loopback only. Forward a
 * remote VNC port through SSH/TLS, pass an already connected secure Socket, or
 * explicitly opt into an insecure remote connection.</p>
 */
public class VncViewer extends Canvas {
    private interface SocketOpener {
        Socket open() throws IOException;
    }

    private final Display display;
    private final AtomicInteger generation = new AtomicInteger();
    private final AtomicBoolean redrawQueued = new AtomicBoolean();
    private volatile RfbConnection connection;
    private volatile RfbFramebuffer framebuffer;
    private volatile String status = "Disconnected";
    private volatile String clipboardText = "";
    private Image cachedImage;
    private RfbFramebuffer cachedOwner;
    private long cachedRevision = -1;
    private Rectangle displayedBounds = new Rectangle(0, 0, 0, 0);
    private int pointerButtons;

    public VncViewer(Composite parent, int style) {
        super(parent, style | SWT.DOUBLE_BUFFERED);
        display = getDisplay();
        addPaintListener(this::paintFrame);
        addListener(SWT.Resize, event -> redraw());
        addListener(SWT.MouseDown, event -> {
            setFocus();
            pointerButtons |= mouseButton(event.button);
            sendPointer(event.x, event.y, pointerButtons);
        });
        addListener(SWT.MouseUp, event -> {
            pointerButtons &= ~mouseButton(event.button);
            sendPointer(event.x, event.y, pointerButtons);
        });
        addListener(SWT.MouseMove, event -> sendPointer(event.x, event.y, pointerButtons));
        addListener(SWT.MouseWheel, event -> {
            int wheel = event.count > 0 ? 8 : 16;
            int steps = Math.min(8, Math.abs(event.count));
            for (int i = 0; i < steps; i++) {
                sendPointer(event.x, event.y, pointerButtons | wheel);
                sendPointer(event.x, event.y, pointerButtons);
            }
        });
        addListener(SWT.KeyDown, event -> sendKey(event, true));
        addListener(SWT.KeyUp, event -> sendKey(event, false));
        addDisposeListener(event -> {
            generation.incrementAndGet();
            RfbConnection active = connection;
            connection = null;
            if (active != null) active.close();
            discardImage();
        });
    }

    /** Connects to a VNC service forwarded onto the local machine. */
    public void connect(String host, int port, char[] password) {
        checkWidget();
        Objects.requireNonNull(host, "host");
        if (!("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "::1".equals(host) || "[::1]".equals(host))) {
            throw new IllegalArgumentException(
                    "Use a loopback tunnel or explicitly call connectInsecureRemote()");
        }
        connectToHost(host, port, password);
    }

    /** Explicit opt-in for unencrypted VNC traffic to a non-loopback host. */
    public void connectInsecureRemote(String host, int port, char[] password) {
        checkWidget();
        connectToHost(Objects.requireNonNull(host, "host"), port, password);
    }

    private void connectToHost(String host, int port, char[] password) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid VNC port");
        start(() -> {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(host, port), 10_000);
                return socket;
            } catch (IOException error) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // Preserve the original connection failure.
                }
                throw error;
            }
        }, password);
    }

    /**
     * Attaches a previously connected Socket, including a socket protected by
     * a caller-managed SSH or TLS tunnel. This widget takes ownership of it.
     */
    public void connect(Socket socket, char[] password) {
        checkWidget();
        Objects.requireNonNull(socket, "socket");
        if (!socket.isConnected() || socket.isClosed()) {
            throw new IllegalArgumentException("Socket must be connected and open");
        }
        start(() -> socket, password);
    }

    private void start(SocketOpener opener, char[] suppliedPassword) {
        Objects.requireNonNull(opener, "opener");
        disconnect();
        int epoch = generation.incrementAndGet();
        char[] password = suppliedPassword == null ? new char[0] : suppliedPassword.clone();
        status = "Connecting";
        redraw();
        Thread.ofPlatform().daemon().name("Nebula-RFB-reader-" + epoch).start(() -> {
            Socket socket = null;
            RfbConnection client = null;
            try {
                socket = opener.open();
                RfbConnection.Listener callbacks = new RfbConnection.Listener() {
                    @Override
                    public void changed(RfbFramebuffer newFramebuffer) {
                        if (generation.get() == epoch) {
                            framebuffer = newFramebuffer;
                            queueRedraw(epoch);
                        }
                    }

                    @Override
                    public void bell() {
                        // SWT system bell policy belongs to the embedding application.
                    }

                    @Override
                    public void clipboard(String text) {
                        if (generation.get() == epoch) clipboardText = text;
                    }

                    @Override
                    public void failed(IOException exception) {
                        if (generation.get() == epoch) {
                            status = "RFB error: " + exception.getMessage();
                            queueRedraw(epoch);
                        }
                    }
                };
                client = new RfbConnection(socket, password, callbacks);
                if (generation.get() != epoch) return;
                connection = client;
                framebuffer = client.framebuffer();
                status = "Connected: " + client.serverName();
                queueRedraw(epoch);
                client.readLoop();
            } catch (IOException | IllegalArgumentException failure) {
                if (generation.get() == epoch) {
                    status = "RFB error: " + failure.getMessage();
                    queueRedraw(epoch);
                }
            } finally {
                Arrays.fill(password, '\0');
                if (client != null) client.close();
                else if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                        // Shutdown is best-effort.
                    }
                }
                if (generation.get() == epoch) {
                    connection = null;
                    if (status.startsWith("Connected:")) status = "Disconnected";
                    queueRedraw(epoch);
                }
            }
        });
    }

    /** Disconnects and releases the last cached SWT framebuffer. */
    public void disconnect() {
        checkWidget();
        generation.incrementAndGet();
        RfbConnection active = connection;
        connection = null;
        if (active != null) active.close();
        framebuffer = null;
        pointerButtons = 0;
        clipboardText = "";
        status = "Disconnected";
        discardImage();
        redraw();
    }

    public boolean isConnected() {
        checkWidget();
        return connection != null;
    }

    public String getConnectionStatus() {
        checkWidget();
        return status;
    }

    /** Remote clipboard is not automatically written to the operating system clipboard. */
    public String getRemoteClipboardText() {
        checkWidget();
        return clipboardText;
    }

    public void sendClipboardText(String text) throws IOException {
        checkWidget();
        RfbConnection active = connection;
        if (active != null) active.sendClipboard(Objects.requireNonNull(text, "text"));
    }

    private void queueRedraw(int epoch) {
        if (display.isDisposed() || !redrawQueued.compareAndSet(false, true)) return;
        display.asyncExec(() -> {
            redrawQueued.set(false);
            if (!isDisposed() && generation.get() == epoch) redraw();
        });
    }

    private void discardImage() {
        if (cachedImage != null && !cachedImage.isDisposed()) cachedImage.dispose();
        cachedImage = null;
        cachedOwner = null;
        cachedRevision = -1;
        displayedBounds = new Rectangle(0, 0, 0, 0);
    }

    private void paintFrame(PaintEvent event) {
        RfbFramebuffer current = framebuffer;
        if (current == null) {
            event.gc.drawText(status, 8, 8, true);
            return;
        }
        if (cachedOwner != current || cachedRevision != current.revision()) {
            RfbFramebuffer.Snapshot snap = current.snapshot();
            ImageData data = new ImageData(snap.width(), snap.height(), 24,
                    new PaletteData(0x00ff0000, 0x0000ff00, 0x000000ff));
            for (int row = 0; row < snap.height(); row++) {
                data.setPixels(0, row, snap.width(), snap.pixels(), row * snap.width());
            }
            Image next = new Image(display, data);
            discardImage();
            cachedImage = next;
            cachedOwner = current;
            cachedRevision = snap.revision();
        }
        Rectangle bounds = getClientArea();
        if (bounds.width == 0 || bounds.height == 0) return;
        double scale = Math.min((double) bounds.width / cachedImage.getBounds().width,
                (double) bounds.height / cachedImage.getBounds().height);
        int scaledWidth = Math.max(1, (int) Math.round(cachedImage.getBounds().width * scale));
        int scaledHeight = Math.max(1, (int) Math.round(cachedImage.getBounds().height * scale));
        int x = bounds.x + (bounds.width - scaledWidth) / 2;
        int y = bounds.y + (bounds.height - scaledHeight) / 2;
        displayedBounds = new Rectangle(x, y, scaledWidth, scaledHeight);
        event.gc.drawImage(cachedImage, 0, 0, cachedImage.getBounds().width,
                cachedImage.getBounds().height, x, y, scaledWidth, scaledHeight);
    }

    private static int mouseButton(int button) {
        return switch (button) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            default -> 0;
        };
    }

    private void sendPointer(int x, int y, int buttons) {
        RfbConnection active = connection;
        RfbFramebuffer current = framebuffer;
        Rectangle target = displayedBounds;
        if (active == null || current == null || target.width == 0 || target.height == 0) return;
        int px = (int) ((long) (x - target.x) * current.width() / target.width);
        int py = (int) ((long) (y - target.y) * current.height() / target.height);
        try {
            active.sendPointer(px, py, buttons);
        } catch (IOException failed) {
            status = "RFB input error: " + failed.getMessage();
        }
    }

    private void sendKey(Event event, boolean down) {
        RfbConnection active = connection;
        if (active == null) return;
        int keysym = keysym(event);
        if (keysym == 0) return;
        try {
            active.sendKey(keysym, down);
        } catch (IOException failed) {
            status = "RFB input error: " + failed.getMessage();
        }
    }

    /** X11 keysyms used on the RFB wire. */
    private static int keysym(Event event) {
        return switch (event.keyCode) {
            case SWT.BS -> 0xff08;
            case SWT.TAB -> 0xff09;
            case SWT.CR, SWT.KEYPAD_CR -> 0xff0d;
            case SWT.ESC -> 0xff1b;
            case SWT.DEL -> 0xffff;
            case SWT.HOME -> 0xff50;
            case SWT.ARROW_LEFT -> 0xff51;
            case SWT.ARROW_UP -> 0xff52;
            case SWT.ARROW_RIGHT -> 0xff53;
            case SWT.ARROW_DOWN -> 0xff54;
            case SWT.PAGE_UP -> 0xff55;
            case SWT.PAGE_DOWN -> 0xff56;
            case SWT.END -> 0xff57;
            case SWT.INSERT -> 0xff63;
            default -> {
                if (event.keyCode >= SWT.F1 && event.keyCode <= SWT.F12) {
                    yield 0xffbe + (event.keyCode - SWT.F1);
                }
                int ch = event.character != 0 ? event.character : event.keyCode;
                yield ch >= 32 && ch <= 0x10ffff
                        ? (ch > 0xff ? 0x01000000 | ch : ch) : 0;
            }
        };
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        checkWidget();
        return new Point(wHint == SWT.DEFAULT ? 640 : wHint,
                hHint == SWT.DEFAULT ? 480 : hHint);
    }

    @Override
    protected void checkSubclass() {
        // Public Nebula widget may be specialised by applications.
    }
}
