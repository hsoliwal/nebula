/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Screen-independent RichCursor and XCursor pseudo-encoding parser.
 * The caller must negotiate little-endian 32bpp RGB888 true-color pixels.
 * Mask bits have 1=opaque, and each cursor row is aligned to a full byte.
 */
final class RfbCursorDecoder {
    static final int MAX_DIMENSION = 512;

    static final class Shape {
        private final int width, height, hotspotX, hotspotY;
        private final int[] argb;

        Shape(int width, int height, int hotspotX, int hotspotY, int[] argb) {
            this.width = width;
            this.height = height;
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
            this.argb = argb.clone();
        }

        int width() { return width; }
        int height() { return height; }
        int hotspotX() { return hotspotX; }
        int hotspotY() { return hotspotY; }
        int[] argbCopy() { return argb.clone(); }
    }

    static Shape readRich(DataInputStream in, int hotspotX, int hotspotY,
            int width, int height) throws IOException {
        validate(hotspotX, hotspotY, width, height);
        if (width == 0 && height == 0) {
            return null;
        }
        int[] argb = new int[width * height];
        for (int i = 0; i < argb.length; i++) {
            int blue = in.readUnsignedByte();
            int green = in.readUnsignedByte();
            int red = in.readUnsignedByte();
            in.readUnsignedByte(); // requested 32-bit pixel alignment byte
            argb[i] = (red << 16) | (green << 8) | blue;
        }
        int rowStride = (width + 7) / 8;
        byte[] transparency = new byte[rowStride * height];
        in.readFully(transparency);
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                if ((transparency[y * rowStride + x / 8] & (0x80 >>> (x & 7))) != 0) {
                    argb[row + x] |= 0xff000000;
                }
            }
        }
        return new Shape(width, height, hotspotX, hotspotY, argb);
    }

    static Shape readX(DataInputStream in, int hotspotX, int hotspotY,
            int width, int height) throws IOException {
        validate(hotspotX, hotspotY, width, height);
        if (width == 0 && height == 0) {
            return null;
        }
        int fg = readColor(in);
        int bg = readColor(in);
        int rowStride = (width + 7) / 8;
        int len = rowStride * height;
        byte[] data = new byte[len];
        byte[] transparency = new byte[len];
        in.readFully(data);
        in.readFully(transparency);
        int[] argb = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * rowStride + x / 8;
                int bit = 0x80 >>> (x & 7);
                int alpha = (transparency[index] & bit) != 0 ? 0xff000000 : 0;
                argb[y * width + x] = alpha | ((data[index] & bit) != 0 ? fg : bg);
            }
        }
        return new Shape(width, height, hotspotX, hotspotY, argb);
    }

    private static int readColor(DataInputStream in) throws IOException {
        return (in.readUnsignedByte() << 16) | (in.readUnsignedByte() << 8)
                | in.readUnsignedByte();
    }

    private static void validate(int hotspotX, int hotspotY, int w, int h) throws IOException {
        if (w == 0 && h == 0 && hotspotX == 0 && hotspotY == 0) {
            return;
        }
        if (w <= 0 || h <= 0 || w > MAX_DIMENSION || h > MAX_DIMENSION
                || hotspotX < 0 || hotspotY < 0 || hotspotX >= w || hotspotY >= h) {
            throw new IOException("Invalid RFB cursor shape dimensions or hotspot");
        }
    }

    private RfbCursorDecoder() { }
}
