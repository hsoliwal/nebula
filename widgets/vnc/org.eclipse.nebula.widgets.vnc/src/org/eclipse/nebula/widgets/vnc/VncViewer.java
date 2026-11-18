/*
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * SWT presentation for a caller-authorized RFB session. The worker only reads
 * protocol data; all SWT image creation, drawing and disposal run on the UI thread.
 * Frame publication is latest-wins and never queues unbounded UI repaint tasks.
 */
public final class VncViewer extends Canvas {
    private final Display display;
    private final AtomicReference<RfbFramebuffer.Frame> pending = new AtomicReference<>();
    private final AtomicBoolean repaintQueued = new AtomicBoolean();
    private volatile boolean stopping;
    private volatile RfbClient client;
    private RfbInputDispatcher inputDispatcher;
    private Consumer<IOException> failureListener = error -> { };
    private Image surface;
    private int remoteWidth;
    private int remoteHeight;
    private int buttons;
    private boolean inputEnabled;

    public VncViewer(Composite parent, int style) {
        super(parent, style | SWT.DOUBLE_BUFFERED);
        display = getDisplay();
        addPaintListener(event -> {
            if (surface != null && !surface.isDisposed()) {
                Rectangle bounds = getClientArea();
                if (bounds.width > 0 && bounds.height > 0) {
                    event.gc.drawImage(surface, 0, 0, remoteWidth, remoteHeight,
                            0, 0, bounds.width, bounds.height);
                }
            }
        });
        addDisposeListener(event -> {
            stopping = true;
            if (inputDispatcher != null) {
                inputDispatcher.close();
            }
            RfbClient current = client;
            if (current != null) {
                try {
                    current.close();
                } catch (IOException ignored) {
                    // A disposed widget has no UI recipient for close failures.
                }
            }
            if (surface != null) {
                surface.dispose();
                surface = null;
            }
            pending.set(null);
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent event) {
                if (event.button >= 1 && event.button <= 3) {
                    buttons |= 1 << (event.button - 1);
                    sendPointer(event.x, event.y, buttons);
                }
            }

            @Override
            public void mouseUp(MouseEvent event) {
                if (event.button >= 1 && event.button <= 3) {
                    buttons &= ~(1 << (event.button - 1));
                    sendPointer(event.x, event.y, buttons);
                }
            }
        });
        addMouseMoveListener(event -> sendPointer(event.x, event.y, buttons));
        addMouseWheelListener(event -> {
            if (!inputEnabled || client == null || event.count == 0) {
                return;
            }
            int pulse = event.count > 0 ? 8 : 16;
            for (int i = 0; i < Math.min(Math.abs(event.count), 16); i++) {
                sendPointer(event.x, event.y, buttons | pulse);
                sendPointer(event.x, event.y, buttons);
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                sendKey(event, true);
            }

            @Override
            public void keyReleased(KeyEvent event) {
                sendKey(event, false);
            }
        });
    }

    /** Takes ownership of an initialized client; callers must open transport first. */
    public void attach(RfbClient authorizedClient) {
        checkWidget();
        if (client != null) {
            throw new IllegalStateException("VNC client already attached");
        }
        client = Objects.requireNonNull(authorizedClient, "authorizedClient");
        inputDispatcher = new RfbInputDispatcher(authorizedClient, this::reportFailure);
        present(authorizedClient.currentFrame());
        Thread.ofVirtual().name("nebula-vnc-frame-receiver").start(() -> {
            try {
                while (!stopping) {
                    present(authorizedClient.nextFrame());
                }
            } catch (IOException exception) {
                reportFailure(exception);
            }
        });
    }

    /** Remote pointer and keyboard events are disabled until explicitly enabled. */
    public void setInputEnabled(boolean enabled) {
        checkWidget();
        inputEnabled = enabled;
        if (!enabled) {
            buttons = 0;
        }
    }

    public void setFailureListener(Consumer<IOException> listener) {
        checkWidget();
        failureListener = Objects.requireNonNull(listener, "listener");
    }

    private void reportFailure(IOException error) {
        if (!stopping && !display.isDisposed()) {
            display.asyncExec(() -> {
                if (!isDisposed()) {
                    failureListener.accept(error);
                }
            });
        }
    }

    private void rejectOverflow() {
        inputEnabled = false;
        buttons = 0;
        failureListener.accept(new IOException("RFB outbound input queue full or closed"));
    }

    private void present(RfbFramebuffer.Frame frame) {
        if (stopping || display.isDisposed()) {
            return;
        }
        pending.set(frame);
        if (repaintQueued.compareAndSet(false, true)) {
            display.asyncExec(() -> {
                repaintQueued.set(false);
                if (isDisposed()) {
                    return;
                }
                RfbFramebuffer.Frame current = pending.getAndSet(null);
                if (current == null) {
                    return;
                }
                ImageData data = new ImageData(current.width(), current.height(), 24,
                        new PaletteData(0xff0000, 0x00ff00, 0x0000ff));
                int[] pixels = current.argb();
                for (int y = 0; y < current.height(); y++) {
                    data.setPixels(0, y, current.width(), pixels, y * current.width());
                }
                Image next = new Image(display, data);
                Image previous = surface;
                surface = next;
                remoteWidth = current.width();
                remoteHeight = current.height();
                if (previous != null) {
                    previous.dispose();
                }
                redraw();
            });
        }
    }

    private void sendPointer(int x, int y, int mask) {
        RfbClient current = client;
        if (!inputEnabled || current == null || remoteWidth <= 0 || remoteHeight <= 0) {
            return;
        }
        Rectangle r = getClientArea();
        if (r.width <= 0 || r.height <= 0) {
            return;
        }
        int rx = Math.clamp((int) ((long) x * remoteWidth / r.width), 0, remoteWidth - 1);
        int ry = Math.clamp((int) ((long) y * remoteHeight / r.height), 0, remoteHeight - 1);
        if (inputDispatcher == null || !inputDispatcher.offerPointer(rx, ry, mask)) {
            rejectOverflow();
        }
    }

    private void sendKey(KeyEvent event, boolean pressed) {
        if (!inputEnabled || client == null) {
            return;
        }
        int keysym = switch (event.keyCode) {
            case SWT.ARROW_LEFT -> 0xff51;
            case SWT.ARROW_UP -> 0xff52;
            case SWT.ARROW_RIGHT -> 0xff53;
            case SWT.ARROW_DOWN -> 0xff54;
            case SWT.CR, SWT.KEYPAD_CR -> 0xff0d;
            case SWT.BS -> 0xff08;
            case SWT.TAB -> 0xff09;
            case SWT.ESC -> 0xff1b;
            case SWT.DEL -> 0xffff;
            default -> event.character;
        };
        if (keysym == 0) {
            return;
        }
        if (inputDispatcher == null || !inputDispatcher.offerKey(pressed, keysym)) {
            rejectOverflow();
        }
    }
}
