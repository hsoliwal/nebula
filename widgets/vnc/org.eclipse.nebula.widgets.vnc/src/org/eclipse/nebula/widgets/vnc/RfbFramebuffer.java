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
