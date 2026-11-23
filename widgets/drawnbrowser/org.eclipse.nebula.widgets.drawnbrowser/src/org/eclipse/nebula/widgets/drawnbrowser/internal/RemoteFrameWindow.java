/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Contributors to the Nebula project.
 */
package org.eclipse.nebula.widgets.drawnbrowser.internal;

/**
 * Display-free geometry and paint-revision guard for the existing DrawnBrowser's
 * opt-in remote-image presentation. This is not a browser, CDP or RFB client.
 * Only the owning SWT UI thread mutates an instance; no native resources live here.
 * Input authorization and transport trust belong to the embedding application.
 */
public final class RemoteFrameWindow {
    public static final int MAX_AXIS = 8192;
    public static final long MAX_PIXELS = 16_777_216L;
    public static final int MAX_CSS_AXIS = 32_768;

    public record Rect(int x, int y, int width, int height) { }

    private long epoch = -1;
    private long revision = -1;
    private long retiredEpoch = -1;
    private long paintedEpoch = -1;
    private long paintedRevision = -1;
    private int pixelWidth;
    private int pixelHeight;
    private int cssWidth;
    private int cssHeight;
    private Rect paintedBounds;

    /** Validate without advancing the state, before any native SWT Image allocation. */
    public boolean canReceive(long nextEpoch, long nextRevision,
            int nextPixelWidth, int nextPixelHeight, int nextCssWidth, int nextCssHeight) {
        if (nextEpoch < 0 || nextRevision < 0 || nextPixelWidth < 1 || nextPixelHeight < 1
                || nextPixelWidth > MAX_AXIS || nextPixelHeight > MAX_AXIS
                || (long) nextPixelWidth * nextPixelHeight > MAX_PIXELS
                || nextCssWidth < 1 || nextCssHeight < 1
                || nextCssWidth > MAX_CSS_AXIS || nextCssHeight > MAX_CSS_AXIS) {
            throw new IllegalArgumentException("invalid remote frame dimensions or revision");
        }
        return nextEpoch > retiredEpoch && (nextEpoch > epoch
                || nextEpoch == epoch && nextRevision > revision);
    }

    /** Called only after allocation of the corresponding image succeeded. */
    public boolean receive(long nextEpoch, long nextRevision,
            int nextPixelWidth, int nextPixelHeight, int nextCssWidth, int nextCssHeight) {
        if (!canReceive(nextEpoch, nextRevision, nextPixelWidth,
                nextPixelHeight, nextCssWidth, nextCssHeight)) {
            return false;
        }
        epoch = nextEpoch;
        revision = nextRevision;
        pixelWidth = nextPixelWidth;
        pixelHeight = nextPixelHeight;
        cssWidth = nextCssWidth;
        cssHeight = nextCssHeight;
        invalidatePaint();
        return true;
    }

    public long epoch() { return epoch; }

    public long revision() { return revision; }

    /** Compute visible image bounds in SWT client coordinates; black gutters are outside. */
    public Rect content(int width, int height) {
        if (epoch < 0 || width < 1 || height < 1) return null;
        int w, h;
        if ((long) width * pixelHeight <= (long) height * pixelWidth) {
            w = width;
            h = (int) Math.max(1L, (long) width * pixelHeight / pixelWidth);
        } else {
            h = height;
            w = (int) Math.max(1L, (long) height * pixelWidth / pixelHeight);
        }
        return new Rect((width - w) / 2, (height - h) / 2, w, h);
    }

    /**
     * Mark displayed only after the SWT GC successfully drew a clip that covers
     * the entire content rectangle. A partial paint must not re-enable input.
     * This proves a completed paint callback, not OS compositor scan-out.
     */
    public boolean markPainted(long expectedEpoch, long expectedRevision, int width, int height,
            int damageX, int damageY, int damageWidth, int damageHeight) {
        if (epoch != expectedEpoch || revision != expectedRevision
                || damageWidth < 1 || damageHeight < 1) return false;
        Rect fitted = content(width, height);
        if (fitted == null || (long) damageX > fitted.x || (long) damageY > fitted.y
                || (long) damageX + damageWidth < (long) fitted.x + fitted.width
                || (long) damageY + damageHeight < (long) fitted.y + fitted.height) return false;
        boolean changed = paintedEpoch != epoch || paintedRevision != revision
                || !fitted.equals(paintedBounds);
        paintedEpoch = epoch;
        paintedRevision = revision;
        paintedBounds = fitted;
        return changed;
    }

    /** A resize invalidates input until the newly scaled image is painted. */
    public void invalidatePaint() {
        paintedEpoch = paintedRevision = -1;
        paintedBounds = null;
    }

    /**
     * Return CSS coordinates only for a frame that actually passed its paint callback.
     * This method grants NO permission to send an input event to the remote browser.
     */
    public int[] cssPoint(long expectedEpoch, long expectedRevision, int x, int y) {
        if (epoch != expectedEpoch || revision != expectedRevision
                || paintedEpoch != epoch || paintedRevision != revision || paintedBounds == null) {
            return null;
        }
        Rect r = paintedBounds;
        if ((long) x < r.x || (long) y < r.y
                || (long) x >= (long) r.x + r.width || (long) y >= (long) r.y + r.height) {
            return null;
        }
        int px = (int) ((long) (x - r.x) * pixelWidth / r.width);
        int py = (int) ((long) (y - r.y) * pixelHeight / r.height);
        return new int[] { (int) ((long) px * cssWidth / pixelWidth),
                (int) ((long) py * cssHeight / pixelHeight) };
    }

    /** Retire the current transport session, including delayed callbacks from it. */
    public void retire() {
        if (epoch > retiredEpoch) retiredEpoch = epoch;
        epoch = revision = -1;
        pixelWidth = pixelHeight = cssWidth = cssHeight = 0;
        invalidatePaint();
    }
}
