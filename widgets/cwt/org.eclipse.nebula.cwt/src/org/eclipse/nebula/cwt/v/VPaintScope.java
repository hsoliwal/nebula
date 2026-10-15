/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.cwt.v;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Region;

/** Per-paint borrowed-GC scope. Exact clip polygons are retained, not only their bounds. */
final class VPaintScope implements AutoCloseable {
    private final GC gc;
    private final int alpha;
    private final boolean clipped;
    private final Region saved;
    private Region work;
    private boolean closed;

    VPaintScope(GC gc) {
        this.gc = gc;
        alpha = gc.getAlpha();
        clipped = gc.isClipped();
        saved = new Region(gc.getDevice());
        try {
            gc.getClipping(saved);
            work = new Region(gc.getDevice());
        } catch (RuntimeException | Error failure) {
            try {
                saved.dispose();
            } catch (RuntimeException | Error cleanup) {
                if (cleanup != failure) failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    void clip(int x, int y, int width, int height) {
        // work is always a subset of saved, so subtract(saved) empties it without self-aliasing.
        work.subtract(saved);
        work.add(saved);
        work.intersect(x, y, Math.max(0, width), Math.max(0, height));
        gc.setClipping(work);
    }

    void restore() {
        if (!gc.isDisposed()) {
            Throwable failure = null;
            try { gc.setClipping(clipped ? saved : null); }
            catch (RuntimeException | Error problem) { failure = problem; }
            try { gc.setAlpha(alpha); }
            catch (RuntimeException | Error problem) { failure = retain(failure, problem); }
            if (failure instanceof RuntimeException) throw (RuntimeException) failure;
            if (failure != null) throw (Error) failure;
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        Throwable failure = null;
        try { restore(); } catch (RuntimeException | Error problem) { failure = problem; }
        try { work.dispose(); } catch (RuntimeException | Error problem) { failure = retain(failure, problem); }
        try { saved.dispose(); } catch (RuntimeException | Error problem) { failure = retain(failure, problem); }
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        if (failure != null) throw (Error) failure;
    }

    private static Throwable retain(Throwable primary, Throwable secondary) {
        if (primary == null) return secondary;
        if (primary != secondary) primary.addSuppressed(secondary);
        return primary;
    }
}
