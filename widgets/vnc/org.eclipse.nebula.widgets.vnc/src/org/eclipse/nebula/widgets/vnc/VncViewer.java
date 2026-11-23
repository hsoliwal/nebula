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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
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
 *
 * <p>{@link #attach(RfbClient)} instead presents a caller-authorized
 * {@link RfbClient#open caller-owned stream session}. Its framebuffer is
 * painted by the same retained-image pipeline; remote pointer and keyboard
 * events for it are disabled until {@link #setInputEnabled(boolean)} and are
 * sent by a bounded serial dispatcher that never writes on the SWT thread.</p>
 */
public class VncViewer extends Canvas {
    private interface SocketOpener {
        Socket open() throws IOException;
    }

    @FunctionalInterface
    private interface NetworkInput {
        void send(RfbConnection client) throws IOException;
    }

    private record PointerUpdate(RfbConnection client, int x, int y, int buttonMask) { }

    private final Display display;
    private final AtomicInteger generation = new AtomicInteger();
    private final AtomicBoolean redrawQueued = new AtomicBoolean();
    private final ThreadPoolExecutor outbound = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(256), runnable -> {
                Thread thread = new Thread(runnable, "Nebula-RFB-outbound");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private final AtomicReference<PointerUpdate> pendingMotion = new AtomicReference<>();
    private final AtomicBoolean motionQueued = new AtomicBoolean();
    private volatile RfbConnection connection;
    private volatile RfbFramebuffer framebuffer;
    private volatile String status = "Disconnected";
    private volatile String clipboardText = "";
    private Image cachedImage;
    private RfbFramebuffer cachedOwner;
    private long cachedRevision = -1;
    private Rectangle displayedBounds = new Rectangle(0, 0, 0, 0);
    private final RfbViewportInput pointerInput = new RfbViewportInput();
    /* A caller-attached stream session (attach); its input is opt-in. */
    private volatile RfbClient attachedClient;
    private RfbInputDispatcher inputDispatcher;
    private Consumer<IOException> failureListener = error -> { };
    private boolean inputEnabled;
    private final RfbViewportInput attachedInput = new RfbViewportInput();

    @SuppressWarnings("this-escape") // SWT widgets register their listeners in the constructor.
    public VncViewer(Composite parent, int style) {
        super(parent, style | SWT.DOUBLE_BUFFERED);
        display = getDisplay();
        addPaintListener(this::paintFrame);
        addListener(SWT.Resize, event -> redraw());
        addListener(SWT.MouseDown, event -> {
            setFocus();
            // Presses in letterbox margins must not start a remote drag.
            sendCaptured(pointerInput.press(mouseButton(event.button), mappedPointer(event.x, event.y)));
        });
        addListener(SWT.MouseUp, event -> {
            // Drop unsent movement before the release. If the pointer has left
            // the image, release at the last captured remote pixel.
            pendingMotion.set(null);
            sendCaptured(pointerInput.release(mouseButton(event.button), mappedPointer(event.x, event.y)));
        });
        addListener(SWT.MouseMove, event -> {
            int[] command = pointerInput.move(mappedPointer(event.x, event.y));
            if (command != null) sendPointerCoalesced(command);
        });
        addListener(SWT.MouseWheel, event -> {
            int wheel = event.count > 0 ? 8 : 16;
            int steps = Math.min(8, Math.abs(event.count));
            int mask = pointerInput.buttons();
            for (int i = 0; i < steps; i++) {
                sendPointer(event.x, event.y, mask | wheel);
                sendPointer(event.x, event.y, mask);
            }
        });
        addListener(SWT.KeyDown, event -> sendKey(event, true));
        addListener(SWT.KeyUp, event -> sendKey(event, false));
        // Attached stream sessions: separate capture state, explicit input opt-in.
        addListener(SWT.MouseDown, event -> sendAttached(attachedInput.press(
                mouseButton(event.button), attachedPointer(event.x, event.y))));
        addListener(SWT.MouseUp, event -> sendAttached(attachedInput.release(
                mouseButton(event.button), attachedPointer(event.x, event.y))));
        addListener(SWT.MouseMove, event -> sendAttached(attachedInput.move(
                attachedPointer(event.x, event.y))));
        addListener(SWT.MouseWheel, event -> {
            int[] at = attachedPointer(event.x, event.y);
            if (at == null || event.count == 0) return;
            int pulse = event.count > 0 ? 8 : 16;
            int mask = attachedInput.buttons();
            for (int i = 0; i < Math.min(Math.abs(event.count), 16); i++) {
                sendAttached(new int[] { at[0], at[1], mask | pulse });
                sendAttached(new int[] { at[0], at[1], mask });
            }
        });
        addListener(SWT.KeyDown, event -> sendAttachedKey(event, true));
        addListener(SWT.KeyUp, event -> sendAttachedKey(event, false));
        addDisposeListener(event -> {
            generation.incrementAndGet();
            detachClient();
            RfbConnection active = connection;
            connection = null;
            if (active != null) active.close();
            pendingMotion.set(null);
            pointerInput.reset();
            outbound.shutdownNow();
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
                            queueRedraw();
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
                            queueRedraw();
                        }
                    }
                };
                client = new RfbConnection(socket, password, callbacks);
                if (generation.get() != epoch) return;
                connection = client;
                framebuffer = client.framebuffer();
                status = "Connected: " + client.serverName();
                queueRedraw();
                client.readLoop();
            } catch (IOException | IllegalArgumentException failure) {
                if (generation.get() == epoch) {
                    status = "RFB error: " + failure.getMessage();
                    queueRedraw();
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
                    queueRedraw();
                }
            }
        });
    }

    /** Takes ownership of an initialized client; callers must open transport first. */
    public void attach(RfbClient authorizedClient) {
        checkWidget();
        Objects.requireNonNull(authorizedClient, "authorizedClient");
        if (attachedClient != null) {
            throw new IllegalStateException("VNC client already attached");
        }
        RfbFramebuffer remote = authorizedClient.streamFramebuffer();
        disconnect();
        int epoch = generation.incrementAndGet();
        attachedClient = authorizedClient;
        inputDispatcher = new RfbInputDispatcher(authorizedClient, this::reportFailure);
        framebuffer = remote;
        status = "Connected: " + authorizedClient.desktopName();
        queueRedraw();
        Thread.ofVirtual().name("nebula-vnc-frame-receiver").start(() -> {
            try {
                while (generation.get() == epoch) {
                    authorizedClient.readStreamUpdate();
                    if (generation.get() == epoch) queueRedraw();
                }
            } catch (IOException exception) {
                if (generation.get() == epoch) {
                    status = "RFB error: " + exception.getMessage();
                    queueRedraw();
                    reportFailure(exception);
                }
            }
        });
    }

    /**
     * Remote pointer and keyboard events of an attached session are disabled
     * until explicitly enabled. Sessions opened by connect() are unaffected.
     */
    public void setInputEnabled(boolean enabled) {
        checkWidget();
        inputEnabled = enabled;
        if (!enabled) {
            attachedInput.reset();
        }
    }

    /** Receives transport and input failures of an attached session on the SWT thread. */
    public void setFailureListener(Consumer<IOException> listener) {
        checkWidget();
        failureListener = Objects.requireNonNull(listener, "listener");
    }

    /** Disconnects and releases the last cached SWT framebuffer. */
    public void disconnect() {
        checkWidget();
        generation.incrementAndGet();
        detachClient();
        RfbConnection active = connection;
        connection = null;
        if (active != null) active.close();
        framebuffer = null;
        pendingMotion.set(null);
        pointerInput.reset();
        clipboardText = "";
        status = "Disconnected";
        discardImage();
        redraw();
    }

    public boolean isConnected() {
        checkWidget();
        return connection != null || attachedClient != null;
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

    public void sendClipboardText(String text) {
        checkWidget();
        Objects.requireNonNull(text, "text");
        RfbConnection active = connection;
        if (active != null) {
            queueInput(active, client -> client.sendClipboard(text));
        }
    }

    private void queueRedraw() {
        if (display.isDisposed() || !redrawQueued.compareAndSet(false, true)) return;
        display.asyncExec(() -> {
            redrawQueued.set(false);
            // A pending callback from an old connection may be the only one
            // queued when a new connection publishes its first framebuffer.
            // Repaint the latest owner, not the callback's stale epoch.
            if (!isDisposed()) redrawChangedFrame();
        });
    }

    private void redrawChangedFrame() {
        RfbFramebuffer current = framebuffer;
        Rectangle target = displayedBounds;
        if (current == null || current != cachedOwner || cachedImage == null
                || target.width == 0 || target.height == 0) {
            redraw();
            return;
        }
        RfbFramebuffer.Damage damage = current.damageSince(cachedRevision);
        if (damage.complete() || damage.width() == 0 || damage.height() == 0) {
            redraw();
            return;
        }
        // Floor the leading edge, ceil the trailing edge, and keep a one-pixel
        // interpolation guard. GC clipping then limits native screen painting
        // to the area affected by Raw, Hextile or CopyRect.
        Rectangle view = getClientArea();
        int x0 = (int) Math.max(view.x, (long) target.x
                + (long) damage.x() * target.width / current.width() - 1);
        int y0 = (int) Math.max(view.y, (long) target.y
                + (long) damage.y() * target.height / current.height() - 1);
        int x1 = (int) Math.min((long) view.x + view.width, (long) target.x
                + Math.ceilDiv((long) (damage.x() + damage.width()) * target.width, current.width()) + 1);
        int y1 = (int) Math.min((long) view.y + view.height, (long) target.y
                + Math.ceilDiv((long) (damage.y() + damage.height()) * target.height, current.height()) + 1);
        if (x1 > x0 && y1 > y0) redraw(x0, y0, x1 - x0, y1 - y0, false);
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
            RfbFramebuffer.FrameUpdate change = current.snapshotSince(
                    cachedOwner == current && cachedImage != null ? cachedRevision : -1);
            if (change.complete()) {
                // Initial frame, resize, or an update too old for the bounded
                // damage interval: install a complete native SWT image.
                Image next = new Image(display, imageData(change));
                discardImage();
                cachedImage = next;
            } else if (change.changedWidth() > 0 && change.changedHeight() > 0) {
                // Normal RFB Raw/Hextile/CopyRect: upload only changed pixels
                // to the retained image. Do not allocate/copy the full screen.
                Image patch = new Image(display, imageData(change));
                try {
                    GC target = new GC(cachedImage);
                    try {
                        target.drawImage(patch, change.x(), change.y());
                    } finally {
                        target.dispose();
                    }
                } finally {
                    patch.dispose();
                }
            }
            cachedOwner = current;
            cachedRevision = change.revision();
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

    private static ImageData imageData(RfbFramebuffer.FrameUpdate change) {
        int width = change.changedWidth(), height = change.changedHeight();
        ImageData data = new ImageData(width, height, 24,
                new PaletteData(0x00ff0000, 0x0000ff00, 0x000000ff));
        for (int row = 0; row < height; row++) {
            data.setPixels(0, row, width, change.pixels(), row * width);
        }
        return data;
    }

    private static int mouseButton(int button) {
        return switch (button) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            default -> 0;
        };
    }

    private PointerUpdate pointerUpdate(int x, int y, int buttons) {
        RfbConnection active = connection;
        RfbFramebuffer current = framebuffer;
        Rectangle target = displayedBounds;
        // A newly decoded frame cannot receive input until its exact revision
        // has actually been painted. Do not send to stale/letterboxed geometry.
        if (active == null || current == null || cachedOwner != current
                || cachedRevision != current.revision()) return null;
        int remoteWidth = current.width();
        int remoteHeight = current.height();
        if (cachedRevision != current.revision()) return null;
        int[] xy = RfbViewportInput.map(x, y, target.x, target.y, target.width,
                target.height, remoteWidth, remoteHeight);
        return xy == null ? null : new PointerUpdate(active, xy[0], xy[1], buttons);
    }

    private int[] mappedPointer(int x, int y) {
        PointerUpdate update = pointerUpdate(x, y, 0);
        return update == null ? null : new int[] { update.x(), update.y() };
    }

    private void sendCaptured(int[] command) {
        RfbConnection active = connection;
        if (active != null && command != null) {
            queueInput(active, client -> client.sendPointer(command[0], command[1], command[2]));
        }
    }

    private void sendPointer(int x, int y, int buttons) {
        PointerUpdate update = pointerUpdate(x, y, buttons);
        if (update != null) {
            // Preserve mouse-down, mouse-up and wheel press/release events.
            queueInput(update.client(), client -> client.sendPointer(update.x(),
                    update.y(), update.buttonMask()));
        }
    }

    private void sendPointerCoalesced(int[] command) {
        RfbConnection active = connection;
        if (active != null) {
            // Never enqueue one network write for every high-frequency move.
            pendingMotion.set(new PointerUpdate(active, command[0], command[1], command[2]));
            scheduleMotion();
        }
    }

    private void scheduleMotion() {
        if (!motionQueued.compareAndSet(false, true)) return;
        try {
            outbound.execute(() -> {
                try {
                    PointerUpdate update;
                    while ((update = pendingMotion.getAndSet(null)) != null) {
                        if (connection == update.client()) {
                            update.client().sendPointer(update.x(), update.y(), update.buttonMask());
                        }
                    }
                } catch (IOException error) {
                    inputFailure(error);
                } finally {
                    motionQueued.set(false);
                    if (pendingMotion.get() != null) scheduleMotion();
                }
            });
        } catch (RejectedExecutionException rejected) {
            motionQueued.set(false);
            inputFailure(new IOException("RFB output queue is saturated", rejected));
        }
    }

    private void queueInput(RfbConnection target, NetworkInput input) {
        try {
            outbound.execute(() -> {
                if (connection != target) return;
                try {
                    input.send(target);
                } catch (IOException error) {
                    inputFailure(error);
                }
            });
        } catch (RejectedExecutionException rejected) {
            inputFailure(new IOException("RFB output queue is saturated", rejected));
        }
    }

    private void detachClient() {
        RfbInputDispatcher dispatcher = inputDispatcher;
        inputDispatcher = null;
        if (dispatcher != null) dispatcher.close();
        attachedInput.reset();
        RfbClient attached = attachedClient;
        attachedClient = null;
        if (attached != null) {
            // Closing caller-supplied streams can block; never do it on SWT.
            Thread.ofVirtual().name("nebula-vnc-close").start(attached::close);
        }
    }

    private void reportFailure(IOException error) {
        if (!display.isDisposed()) {
            display.asyncExec(() -> {
                if (!isDisposed()) {
                    failureListener.accept(error);
                }
            });
        }
    }

    private void rejectOverflow() {
        inputEnabled = false;
        attachedInput.reset();
        failureListener.accept(new IOException("RFB outbound input queue full or closed"));
    }

    /* Same painted-geometry rule as connect() sessions, for an enabled attached session. */
    private int[] attachedPointer(int x, int y) {
        RfbFramebuffer current = framebuffer;
        if (!inputEnabled || attachedClient == null || current == null
                || cachedOwner != current || cachedRevision != current.revision()) return null;
        Rectangle target = displayedBounds;
        return RfbViewportInput.map(x, y, target.x, target.y, target.width,
                target.height, current.width(), current.height());
    }

    private void sendAttached(int[] command) {
        if (command == null || !inputEnabled || attachedClient == null) return;
        RfbInputDispatcher dispatcher = inputDispatcher;
        if (dispatcher == null || !dispatcher.offerPointer(command[0], command[1], command[2])) {
            rejectOverflow();
        }
    }

    private void sendAttachedKey(Event event, boolean down) {
        if (!inputEnabled || attachedClient == null) return;
        int keysym = keysym(event);
        if (keysym == 0) return;
        RfbInputDispatcher dispatcher = inputDispatcher;
        if (dispatcher == null || !dispatcher.offerKey(down, keysym)) {
            rejectOverflow();
        }
    }

    private void inputFailure(IOException error) {
        status = "RFB input error: " + error.getMessage();
        queueRedraw();
    }

    private void sendKey(Event event, boolean down) {
        RfbConnection active = connection;
        if (active == null) return;
        int keysym = keysym(event);
        if (keysym == 0) return;
        queueInput(active, client -> client.sendKey(keysym, down));
    }

    /** X11 keysyms used on the RFB wire. */
    private static int keysym(Event event) {
        return switch (event.keyCode) {
            case SWT.SHIFT -> 0xffe1;
            case SWT.CTRL -> 0xffe3;
            case SWT.ALT -> 0xffe9;
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
                yield ch >= 32 && ch < 0xd800
                        || ch >= 0xe000 && ch <= 0xffff
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
