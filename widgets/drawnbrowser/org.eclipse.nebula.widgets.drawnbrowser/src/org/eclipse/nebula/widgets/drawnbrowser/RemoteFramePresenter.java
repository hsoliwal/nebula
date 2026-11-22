/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.drawnbrowser;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.nebula.widgets.drawnbrowser.internal.RemoteFrameDecode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

/**
 * Bounded, transport-independent CPU decode handoff into one existing DrawnBrowser.
 * No browser process, WebSocket, CDP/VNC command, authentication, or input dispatch
 * is owned here. The caller validates its transport and sends protocol ACKs itself.
 * Decode and ImageData creation run on one worker; the SWT Image and painting
 * remain owned by DrawnBrowser on the Display thread. At most one waiting, one decoding and
 * one decoded frame are retained, with latest-wins coalescing and epoch guards.
 */
public final class RemoteFramePresenter implements AutoCloseable {
    public record Failure(long epoch, long revision, IOException error) { }

    private record Incoming(long epoch, long revision, int cssWidth, int cssHeight,
            RemoteFrameDecode.Format format, byte[] compressed) { }
    private record Ready(Incoming source, ImageData pixels) { }

    private final DrawnBrowser browser;
    private final Display display;
    private final ExecutorService decoder;
    private final AtomicReference<Incoming> pending = new AtomicReference<>();
    private final AtomicReference<Ready> ready = new AtomicReference<>();
    private final AtomicReference<Failure> lastFailure = new AtomicReference<>();
    private final AtomicBoolean decodeQueued = new AtomicBoolean();
    private final AtomicBoolean uiQueued = new AtomicBoolean();
    private volatile boolean closed;
    private long acceptedEpoch = -1;
    private long acceptedRevision = -1;
    private long retiredEpoch = -1;
    private Consumer<Failure> failureListener = ignored -> { };

    /** Attach a worker to a UI-owned browser, without altering its existing HTML mode. */
    public RemoteFramePresenter(DrawnBrowser browser) {
        this.browser = Objects.requireNonNull(browser, "browser");
        display = browser.getDisplay(); // SWT checks the constructing UI thread
        decoder = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Nebula-remote-browser-decode");
            thread.setDaemon(true);
            return thread;
        });
        browser.addDisposeListener(event -> shutdown());
    }

    /** The UI-thread recipient of the latest decoder/presentation failure. */
    public void setFailureListener(Consumer<Failure> listener) {
        requireUi();
        failureListener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Admit a compressed full-image frame from an authenticated host-owned
     * browser session. This call is safe on an I/O worker and never blocks on
     * image decoding or SWT. True means admitted, NOT painted or CDP-ACKed.
     * The host MUST independently acknowledge a valid CDP screencast event,
     * even when this latest-wins mailbox discards the corresponding frame.
     */
    public synchronized boolean offer(long epoch, long revision, int cssWidth, int cssHeight,
            RemoteFrameDecode.Format format, byte[] compressed) {
        if (closed) return false;
        if (epoch < 0 || revision < 0 || cssWidth < 1 || cssHeight < 1
                || cssWidth > 32768 || cssHeight > 32768) {
            throw new IllegalArgumentException("Invalid remote session or CSS viewport");
        }
        RemoteFrameDecode.validateEnvelope(format, compressed);
        if (epoch <= retiredEpoch || epoch < acceptedEpoch
                || epoch == acceptedEpoch && revision <= acceptedRevision) return false;
        Incoming frame = new Incoming(epoch, revision, cssWidth, cssHeight,
                format, compressed.clone());
        acceptedEpoch = epoch;
        acceptedRevision = revision;
        pending.set(frame);
        scheduleDecode();
        return true;
    }

    /** Return to the original HTML view. Future frames require a higher epoch. */
    public void endSession() {
        requireUi();
        synchronized (this) {
            retiredEpoch = Math.max(retiredEpoch, acceptedEpoch);
            pending.set(null);
            ready.set(null);
            lastFailure.set(null);
        }
        browser.clearRemoteFrame();
    }

    /** Shutdown is UI-thread-only; disposing DrawnBrowser also stops this worker. */
    @Override
    public void close() {
        requireUi();
        shutdown();
        if (!browser.isDisposed()) browser.clearRemoteFrame();
    }

    private synchronized boolean current(Incoming frame) {
        return !closed && frame.epoch() > retiredEpoch && frame.epoch() == acceptedEpoch
                && frame.revision() == acceptedRevision;
    }

    private void scheduleDecode() {
        if (!decodeQueued.compareAndSet(false, true)) return;
        try {
            decoder.execute(this::decodeLoop);
        } catch (RejectedExecutionException ignored) {
            decodeQueued.set(false); // shutdown is already in progress
        }
    }

    private void decodeLoop() {
        try {
            Incoming frame;
            while ((frame = pending.getAndSet(null)) != null && !closed) {
                if (!current(frame)) continue;
                try {
                    RemoteFrameDecode.Pixels image = RemoteFrameDecode.decode(
                            frame.format(), frame.compressed());
                    if (!current(frame)) continue;
                    ImageData data = imageData(image);
                    if (!current(frame)) continue;
                    ready.set(new Ready(frame, data));
                    scheduleUi();
                } catch (IOException | IllegalArgumentException error) {
                    if (current(frame)) {
                        IOException problem = error instanceof IOException io
                                ? io : new IOException("Invalid browser image envelope", error);
                        lastFailure.set(new Failure(frame.epoch(), frame.revision(), problem));
                        scheduleUi();
                    }
                }
            }
        } finally {
            decodeQueued.set(false);
            if (!closed && pending.get() != null) scheduleDecode();
        }
    }

    private void scheduleUi() {
        if (!uiQueued.compareAndSet(false, true)) return;
        if (display.isDisposed()) {
            uiQueued.set(false);
            return;
        }
        display.asyncExec(this::publish);
    }

    private void publish() {
        try {
            if (closed || browser.isDisposed()) return;
            Ready frame = ready.getAndSet(null);
            if (frame != null && current(frame.source())) {
                try {
                    Incoming source = frame.source();
                    browser.presentRemoteFrame(source.epoch(), source.revision(),
                            source.cssWidth(), source.cssHeight(), frame.pixels());
                } catch (RuntimeException exception) {
                    Incoming source = frame.source();
                    lastFailure.set(new Failure(source.epoch(), source.revision(),
                            new IOException("Native browser frame publication failed", exception)));
                }
            }
            Failure failure = lastFailure.getAndSet(null);
            if (failure != null && currentFailure(failure)) failureListener.accept(failure);
        } finally {
            uiQueued.set(false);
            if (!closed && (ready.get() != null || lastFailure.get() != null)) scheduleUi();
        }
    }

    private synchronized boolean currentFailure(Failure failure) {
        return !closed && failure.epoch() > retiredEpoch && failure.epoch() == acceptedEpoch
                && failure.revision() == acceptedRevision;
    }

    /** Convert buffered pixels on the CPU worker; no SWT Device/Image is created. */
    private static ImageData imageData(RemoteFrameDecode.Pixels image) {
        int w = image.width(), h = image.height();
        int[] argb = image.argb();
        ImageData data = new ImageData(w, h, 24,
                new PaletteData(0x00ff0000, 0x0000ff00, 0x000000ff));
        boolean hasAlpha = false;
        for (int pixel : argb) {
            if ((pixel >>> 24) != 255) { hasAlpha = true; break; }
        }
        if (hasAlpha) data.alphaData = new byte[argb.length];
        int[] rgbRow = new int[w];
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                rgbRow[col] = argb[row * w + col] & 0x00ffffff;
            }
            data.setPixels(0, row, w, rgbRow, 0);
            if (hasAlpha) {
                for (int col = 0; col < w; col++) {
                    data.alphaData[row * w + col] = (byte) (argb[row * w + col] >>> 24);
                }
            }
        }
        return data;
    }

    private void requireUi() {
        if (Thread.currentThread() != display.getThread()) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);
        if (closed || browser.isDisposed()) throw new IllegalStateException("Remote frame presenter closed");
    }

    private synchronized void shutdown() {
        if (closed) return;
        closed = true;
        retiredEpoch = Math.max(retiredEpoch, acceptedEpoch);
        pending.set(null);
        ready.set(null);
        lastFailure.set(null);
        decoder.shutdownNow();
    }
}
