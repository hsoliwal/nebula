/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Pure Java, single-reader Hextile (encoding 5) decoder for the negotiated
 * little-endian 32bpp RGB888 framebuffer.
 *
 * <p>Tile background and foreground are scoped to one Hextile rectangle.
 * A Raw tile invalidates both carried colors. SubrectsColored invalidates
 * the carried foreground. A Raw tile's other flag bits are deliberately
 * ignored, as RFC 6143 section 7.6.1 requires.</p>
 *
 * <p>No SWT objects or native buffers are owned by this class.</p>
 */
final class RfbHextileDecoder {
    private static final int RAW = 1;
    private static final int BACKGROUND = 2;
    private static final int FOREGROUND = 4;
    private static final int ANY_SUBRECTS = 8;
    private static final int COLORED_SUBRECTS = 16;
    private static final int MAX_PIXELS = 16_777_216;

    private RfbHextileDecoder() { }

    static void decode(DataInputStream input, int[] framebuffer, int stride,
            int x, int y, int w, int h) throws IOException {
        if (input == null || framebuffer == null || x < 0 || y < 0
                || w <= 0 || h <= 0 || stride <= 0
                || (long) x + w > stride
                || (y + (long) h) * stride > framebuffer.length
                || (long) w * h > MAX_PIXELS) {
            throw new IOException("Hextile rectangle is outside the framebuffer");
        }
        int background = 0;
        int foreground = 0;
        boolean backgroundValid = false;
        boolean foregroundValid = false;
        // Each Hextile Raw tile is at most 16 RGB888 pixels per row.
        byte[] scanline = new byte[16 * 4];
        int bottom = y + h;
        int right = x + w;
        for (int top = y; top < bottom; top += 16) {
            int th = Math.min(16, bottom - top);
            for (int left = x; left < right; left += 16) {
                int tw = Math.min(16, right - left);
                int flags = input.readUnsignedByte();

                if ((flags & RAW) != 0) {
                    // RFC 6143: remaining subencoding bits are irrelevant when
                    // Raw is set, including bits otherwise treated as reserved.
                    for (int row = 0; row < th; row++) {
                        int length = tw * 4;
                        input.readFully(scanline, 0, length);
                        int offset = (top + row) * stride + left;
                        for (int col = 0, bytePos = 0; col < tw; col++, bytePos += 4) {
                            int blue = scanline[bytePos] & 255;
                            int green = scanline[bytePos + 1] & 255;
                            int red = scanline[bytePos + 2] & 255;
                            framebuffer[offset + col] = (red << 16) | (green << 8) | blue;
                        }
                    }
                    backgroundValid = false;
                    foregroundValid = false;
                    continue;
                }
                if ((flags & ~(BACKGROUND | FOREGROUND | ANY_SUBRECTS | COLORED_SUBRECTS)) != 0) {
                    throw new IOException("Reserved Hextile flags without Raw");
                }
                if ((flags & FOREGROUND) != 0 && (flags & COLORED_SUBRECTS) != 0) {
                    throw new IOException("Hextile ForegroundSpecified conflicts with SubrectsColored");
                }
                if ((flags & BACKGROUND) != 0) {
                    background = readRgb(input);
                    backgroundValid = true;
                }
                if (!backgroundValid) {
                    throw new IOException("First non-Raw Hextile tile requires a background");
                }
                if ((flags & FOREGROUND) != 0) {
                    foreground = readRgb(input);
                    foregroundValid = true;
                }
                if ((flags & COLORED_SUBRECTS) != 0) {
                    // A colored tile may not leave a reusable foreground for
                    // a subsequent uncolored tile.
                    foregroundValid = false;
                }
                fill(framebuffer, stride, left, top, tw, th, background);
                if ((flags & ANY_SUBRECTS) != 0) {
                    int count = input.readUnsignedByte();
                    boolean colored = (flags & COLORED_SUBRECTS) != 0;
                    if (count > 0 && !colored && !foregroundValid) {
                        throw new IOException("Hextile subrectangles have no foreground color");
                    }
                    for (int i = 0; i < count; i++) {
                        int color = colored ? readRgb(input) : foreground;
                        int position = input.readUnsignedByte();
                        int size = input.readUnsignedByte();
                        int sx = position >>> 4;
                        int sy = position & 15;
                        int sw = (size >>> 4) + 1;
                        int sh = (size & 15) + 1;
                        if (sx + sw > tw || sy + sh > th) {
                            throw new IOException("Hextile subrectangle exceeds tile");
                        }
                        fill(framebuffer, stride, left + sx, top + sy, sw, sh, color);
                    }
                } else if ((flags & COLORED_SUBRECTS) != 0) {
                    throw new IOException("Hextile colored-subrect flag without subrectangles");
                }
            }
        }
    }

    private static int readRgb(DataInputStream input) throws IOException {
        int blue = input.readUnsignedByte();
        int green = input.readUnsignedByte();
        int red = input.readUnsignedByte();
        input.readUnsignedByte(); // 32bpp high alignment byte
        return (red << 16) | (green << 8) | blue;
    }

    private static void fill(int[] framebuffer, int stride,
            int x, int y, int w, int h, int color) {
        for (int row = 0; row < h; row++) {
            int from = (y + row) * stride + x;
            Arrays.fill(framebuffer, from, from + w, color);
        }
    }
}
