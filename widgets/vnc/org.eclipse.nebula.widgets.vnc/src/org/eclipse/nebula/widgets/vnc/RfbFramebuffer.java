/*
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * CPU-owned RFB framebuffer. The network thread writes it and the SWT thread
 * takes versioned snapshots. No SWT Image, GC, or Control is used here.
 *
 * <p>The class is public only so that callers of the caller-owned stream
 * session ({@link RfbClient#open}) can use its immutable {@link Frame}
 * snapshots; the decoder-facing members remain package-private.</p>
 */
public final class RfbFramebuffer {
    static final int MAX_DIMENSION = 8192;
    static final long MAX_PIXELS = 16_777_216L;

    record Snapshot(int width, int height, long revision, int[] pixels) {
        /** A defensive ARGB copy; snapshot pixels never alias the framebuffer. */
        int[] argb() {
            return pixels.clone();
        }
    }

    /** Immutable snapshot. Pixel ownership never escapes to a consumer. */
    public record Frame(int width, int height, int[] argb) {
        public Frame {
            Objects.requireNonNull(argb, "argb");
            if (width <= 0 || height <= 0 || (long) width * height != argb.length) {
                throw new IllegalArgumentException("frame dimensions do not match pixels");
            }
            argb = argb.clone();
        }

        @Override
        public int[] argb() {
            return argb.clone();
        }
    }

    /**
     * A single, defensively copied update for the only SWT image consumer.
     * The region is the union of writes since its last snapshot, or the whole
     * framebuffer if that consumer missed the retained damage interval.
     */
    record FrameUpdate(int width, int height, long revision, int x, int y,
            int changedWidth, int changedHeight, int[] pixels) {
        boolean complete() {
            return x == 0 && y == 0 && changedWidth == width && changedHeight == height;
        }
    }

    /** Damage coordinates without allocating or copying any framebuffer pixels. */
    record Damage(int x, int y, int width, int height, boolean complete) { }

    private int width;
    private int height;
    private int[] pixels;
    private long revision;
    /* A bounded damage union, not a per-packet queue or a full-frame copy. */
    private long damageBaseRevision;
    private int dirtyLeft = Integer.MAX_VALUE, dirtyTop = Integer.MAX_VALUE;
    private int dirtyRight, dirtyBottom;
    private boolean fullDamage;

    RfbFramebuffer(int width, int height) {
        resize(width, height);
    }

    synchronized void resize(int newWidth, int newHeight) {
        if (newWidth < 1 || newHeight < 1
                || newWidth > MAX_DIMENSION || newHeight > MAX_DIMENSION
                || (long) newWidth * newHeight > MAX_PIXELS) {
            throw new IllegalArgumentException("RFB framebuffer dimensions exceed limits");
        }
        width = newWidth;
        height = newHeight;
        pixels = new int[width * height];
        Arrays.fill(pixels, 0xff000000);
        revision++;
        fullDamage = true;
        dirtyLeft = 0;
        dirtyTop = 0;
        dirtyRight = width;
        dirtyBottom = height;
    }

    synchronized int width() {
        return width;
    }

    synchronized int height() {
        return height;
    }

    synchronized Snapshot snapshot() {
        return new Snapshot(width, height, revision, pixels.clone());
    }

    /** An immutable whole-frame snapshot for {@link RfbClient#currentFrame()} and {@link RfbClient#nextFrame()}. */
    synchronized Frame frame() {
        return new Frame(width, height, pixels);
    }

    /**
     * Captures just the changed region under one short lock. A late consumer
     * whose revision precedes the retained damage interval receives a complete
     * frame, so coalesced network updates never lose pixels. The pixel copy is
     * detached before the SWT thread creates an Image.
     */
    synchronized FrameUpdate snapshotSince(long seenRevision) {
        Damage damage = damageSince(seenRevision);
        int changedWidth = damage.width(), changedHeight = damage.height();
        int[] changedPixels = new int[changedWidth * changedHeight];
        for (int row = 0; row < changedHeight; row++) {
            System.arraycopy(pixels, (damage.y() + row) * width + damage.x(),
                    changedPixels, row * changedWidth, changedWidth);
        }
        damageBaseRevision = revision;
        fullDamage = false;
        dirtyLeft = dirtyTop = Integer.MAX_VALUE;
        dirtyRight = dirtyBottom = 0;
        return new FrameUpdate(width, height, revision, damage.x(), damage.y(),
                changedWidth, changedHeight, changedPixels);
    }

    /** Cheap UI-thread damage hint; consuming it would lose the changed pixels. */
    synchronized Damage damageSince(long seenRevision) {
        if (seenRevision == revision) return new Damage(0, 0, 0, 0, false);
        if (fullDamage || seenRevision < damageBaseRevision || seenRevision > revision) {
            return new Damage(0, 0, width, height, true);
        }
        if (dirtyLeft == Integer.MAX_VALUE) return new Damage(0, 0, 0, 0, false);
        return new Damage(dirtyLeft, dirtyTop,
                dirtyRight - dirtyLeft, dirtyBottom - dirtyTop, false);
    }

    private void changed(int x, int y, int w, int h) {
        if (w == 0 || h == 0) return;
        dirtyLeft = Math.min(dirtyLeft, x);
        dirtyTop = Math.min(dirtyTop, y);
        dirtyRight = Math.max(dirtyRight, x + w);
        dirtyBottom = Math.max(dirtyBottom, y + h);
        revision++;
    }

    synchronized long revision() {
        return revision;
    }

    private void requireRectangle(int x, int y, int w, int h) {
        if (x < 0 || y < 0 || w < 0 || h < 0
                || (long) x + w > width || (long) y + h > height) {
            throw new IllegalArgumentException("Invalid RFB rectangle");
        }
    }

    /**
     * Reads the negotiated 32-bit little-endian true-colour B,G,R,pad format
     * before holding the framebuffer lock. The SWT thread never waits for IO.
     */
    void readRaw(DataInputStream input, int x, int y, int w, int h) throws IOException {
        if (w < 0 || h < 0 || (long) w * h > MAX_PIXELS) {
            throw new IOException("Oversized raw rectangle");
        }
        synchronized (this) {
            requireRectangle(x, y, w, h);
        }
        int[] patch = new int[w * h];
        byte[] scanline = new byte[w * 4];
        for (int row = 0; row < h; row++) {
            input.readFully(scanline);
            for (int col = 0, i = 0; col < w; col++, i += 4) {
                int b = scanline[i] & 0xff;
                int g = scanline[i + 1] & 0xff;
                int r = scanline[i + 2] & 0xff;
                patch[row * w + col] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
        synchronized (this) {
            requireRectangle(x, y, w, h);
            for (int row = 0; row < h; row++) {
                System.arraycopy(patch, row * w, pixels, (y + row) * width + x, w);
            }
            changed(x, y, w, h);
        }
    }

    synchronized void writePixels(int x, int y, int w, int h, int[] patch) {
        requireRectangle(x, y, w, h);
        if (patch.length != w * h) {
            throw new IllegalArgumentException("Invalid tile length");
        }
        for (int row = 0; row < h; row++) {
            System.arraycopy(patch, row * w, pixels, (y + row) * width + x, w);
        }
        changed(x, y, w, h);
    }

    synchronized void copyRect(int x, int y, int w, int h, int sourceX, int sourceY) {
        requireRectangle(x, y, w, h);
        requireRectangle(sourceX, sourceY, w, h);
        if (y > sourceY && y < sourceY + h) {
            for (int row = h - 1; row >= 0; row--) {
                System.arraycopy(pixels, (sourceY + row) * width + sourceX,
                        pixels, (y + row) * width + x, w);
            }
        } else {
            for (int row = 0; row < h; row++) {
                System.arraycopy(pixels, (sourceY + row) * width + sourceX,
                        pixels, (y + row) * width + x, w);
            }
        }
        changed(x, y, w, h);
    }
}
