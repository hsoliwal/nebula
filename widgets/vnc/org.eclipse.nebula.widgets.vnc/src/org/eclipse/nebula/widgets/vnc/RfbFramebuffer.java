/*
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * CPU-owned RFB framebuffer. The network thread writes it and the SWT thread
 * takes versioned snapshots. No SWT Image, GC, or Control is used here.
 */
final class RfbFramebuffer {
    static final int MAX_DIMENSION = 8192;
    static final long MAX_PIXELS = 16_777_216L;

    record Snapshot(int width, int height, long revision, int[] pixels) { }

    private int width;
    private int height;
    private int[] pixels;
    private long revision;

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

    /** Defensively copies just a visible source tile, not the entire desktop. */
    synchronized Snapshot snapshot(int x, int y, int w, int h) {
        requireRectangle(x, y, w, h);
        int[] tile = new int[w * h];
        for (int row = 0; row < h; row++) {
            System.arraycopy(pixels, (y + row) * width + x, tile, row * w, w);
        }
        return new Snapshot(w, h, revision, tile);
    }

    /**
     * Maps only the visible native paint clip into a primitive pixel tile.
     * Even when a large desktop is fitted into a small Canvas, no full-size
     * SWT Image or temporary copy of the remote framebuffer is materialized.
     */
    synchronized Snapshot snapshotScaled(int x, int y, int w, int h,
            int drawX, int drawY, int drawW, int drawH) {
        if (w < 1 || h < 1 || drawW < 1 || drawH < 1 || (long) w * h > MAX_PIXELS) {
            throw new IllegalArgumentException("Invalid visible framebuffer tile");
        }
        int[] tile = new int[w * h];
        for (int row = 0; row < h; row++) {
            long sy = ((long) y + row - drawY) * height / drawH;
            int sourceY = (int) Math.max(0, Math.min(height - 1, sy));
            for (int col = 0; col < w; col++) {
                long sx = ((long) x + col - drawX) * width / drawW;
                int sourceX = (int) Math.max(0, Math.min(width - 1, sx));
                tile[row * w + col] = pixels[sourceY * width + sourceX];
            }
        }
        return new Snapshot(w, h, revision, tile);
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
            revision++;
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
        revision++;
    }

    synchronized void fillRectangle(int x, int y, int w, int h, int argb) {
        requireRectangle(x, y, w, h);
        for (int row = y; row < y + h; row++) {
            Arrays.fill(pixels, row * width + x, row * width + x + w, argb);
        }
        if (w > 0 && h > 0) revision++;
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
        revision++;
    }
}
