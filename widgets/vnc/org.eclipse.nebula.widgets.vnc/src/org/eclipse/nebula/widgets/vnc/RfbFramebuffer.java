/*
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import java.io.DataInput;
import java.io.IOException;
import java.util.Objects;

/** A bounded ARGB framebuffer for the RFB Raw and CopyRect encodings. */
public final class RfbFramebuffer {
    public static final int MAX_DIMENSION = 8192;
    public static final int MAX_PIXELS = 16_777_216;
    private int width;
    private int height;
    private int[] pixels;

    public RfbFramebuffer(int width, int height) {
        resize(width, height);
    }

    /** A remote resize discards stale pixels; bounds are checked before allocation. */
    public synchronized void resize(int newWidth, int newHeight) {
        if (newWidth < 1 || newHeight < 1 || newWidth > MAX_DIMENSION
                || newHeight > MAX_DIMENSION || (long) newWidth * newHeight > MAX_PIXELS) {
            throw new IllegalArgumentException("RFB framebuffer exceeds limits");
        }
        pixels = new int[newWidth * newHeight];
        width = newWidth;
        height = newHeight;
    }

    public synchronized int width() {
        return width;
    }

    public synchronized int height() {
        return height;
    }

    /**
     * Read a Raw rectangle in the 32-bpp, little-endian, depth-24, true-colour
     * format explicitly negotiated by {@link RfbClient}. A short read fails.
     */
    public synchronized void readRaw(DataInput input, int x, int y, int w, int h) throws IOException {
        Objects.requireNonNull(input, "input");
        checkRect(x, y, w, h);
        byte[] row = new byte[w * 4];
        for (int j = 0; j < h; j++) {
            input.readFully(row);
            int offset = (y + j) * width + x;
            for (int i = 0; i < w; i++) {
                int b = row[4 * i] & 0xff;
                int g = row[4 * i + 1] & 0xff;
                int r = row[4 * i + 2] & 0xff;
                pixels[offset + i] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    /** CopyRect is overlap-safe in either vertical direction. */
    public synchronized void copyRect(int x, int y, int w, int h, int srcX, int srcY) {
        checkRect(x, y, w, h);
        checkRect(srcX, srcY, w, h);
        if (y > srcY) {
            for (int j = h - 1; j >= 0; j--) {
                System.arraycopy(pixels, (srcY + j) * width + srcX,
                        pixels, (y + j) * width + x, w);
            }
        } else {
            for (int j = 0; j < h; j++) {
                System.arraycopy(pixels, (srcY + j) * width + srcX,
                        pixels, (y + j) * width + x, w);
            }
        }
    }

    public synchronized Frame snapshot() {
        return new Frame(width, height, pixels);
    }

    private void checkRect(int x, int y, int w, int h) {
        if (w < 1 || h < 1 || x < 0 || y < 0 || w > width || h > height
                || x > width - w || y > height - h) {
            throw new IllegalArgumentException("RFB rectangle outside framebuffer");
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
}
