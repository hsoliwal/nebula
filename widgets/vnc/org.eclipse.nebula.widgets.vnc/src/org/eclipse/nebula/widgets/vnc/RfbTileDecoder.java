/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Decodes RFC 6143 TRLE, ZRLE and RRE rectangles into an existing framebuffer.
 *
 * <p>The caller owns the framebuffer and does not publish it to other threads
 * while decoding. The agreed pixel format is RGB888 with little-endian 32bpp
 * PIXEL (B,G,R,pad). Compact pixels are three bytes (B,G,R).</p>
 *
 * <p>The zlib dictionary/stream is retained across ZRLE rectangles in a
 * connection. Only the RFB protocol's single reader thread may call decode.
 * Invalid or unexpectedly large inputs fail closed with IOException.</p>
 */
final class RfbTileDecoder implements AutoCloseable {
    private static final int MAX_PIXELS = 16_777_216;
    private static final int MAX_COMPRESSED_RECTANGLE = 64 * 1024 * 1024;
    private static final int MAX_DECOMPRESSED_RECTANGLE = 96 * 1024 * 1024;
    private static final int IO_BUFFER = 8192;

    private final Inflater inflater = new Inflater();
    private final byte[] compressedBuffer = new byte[IO_BUFFER];
    private final byte[] expandedBuffer = new byte[IO_BUFFER];
    private boolean closed;

    /** Decode a rectangle with RFB encoding 16 (ZRLE). */
    void decodeZrle(DataInputStream wire, int[] framebuffer, int stride,
            int x, int y, int w, int h) throws IOException {
        ensureOpen();
        validateFrame(framebuffer, stride, x, y, w, h);
        long length = Integer.toUnsignedLong(wire.readInt());
        if (length == 0 || length > MAX_COMPRESSED_RECTANGLE) {
            throw new IOException("Invalid ZRLE compressed size: " + length);
        }
        // ZRLE streams are continuous across rectangles. Never recreate or
        // reset the Inflater in this method: the server uses a shared state.
        // A malicious peer cannot inflate a tiny tile into a huge allocation.
        // Four bytes per pixel plus a bounded per-tile palette allowance covers
        // all RFC TRLE tile variants under this negotiated pixel format.
        long tilesWide = (w + 63L) / 64;
        long tilesHigh = (h + 63L) / 64;
        long expandedLimit = Math.min(MAX_DECOMPRESSED_RECTANGLE,
                4L * w * h + 384L * tilesWide * tilesHigh + 1024);
        ByteArrayOutputStream raw = new ByteArrayOutputStream(Math.min((int) length * 2, IO_BUFFER));
        long pending = length;
        while (pending > 0) {
            int n = (int) Math.min(pending, compressedBuffer.length);
            wire.readFully(compressedBuffer, 0, n);
            pending -= n;
            if (!inflater.needsInput()) {
                throw new IOException("ZRLE zlib stream retained unconsumed input");
            }
            inflater.setInput(compressedBuffer, 0, n);
            inflateAll(raw, expandedLimit);
        }
        if (!inflater.needsInput() || inflater.needsDictionary() || inflater.finished()) {
            throw new IOException("ZRLE deflate stream did not reach a flush boundary");
        }
        byte[] tiles = raw.toByteArray();
        DataInputStream uncompressed = new DataInputStream(new ByteArrayInputStream(tiles));
        decodeTiles(uncompressed, framebuffer, stride, x, y, w, h, 64, false);
        if (uncompressed.available() != 0) {
            throw new IOException("Extra decompressed ZRLE bytes: " + uncompressed.available());
        }
    }

    /** Decode a rectangle with RFB encoding 15 (TRLE). */
    void decodeTrle(DataInputStream wire, int[] framebuffer, int stride,
            int x, int y, int w, int h) throws IOException {
        ensureOpen();
        validateFrame(framebuffer, stride, x, y, w, h);
        decodeTiles(wire, framebuffer, stride, x, y, w, h, 16, true);
    }

    /** Decode a rectangle with RFB encoding 2 (RRE). */
    void decodeRre(DataInputStream wire, int[] framebuffer, int stride,
            int x, int y, int w, int h) throws IOException {
        ensureOpen();
        validateFrame(framebuffer, stride, x, y, w, h);
        long number = Integer.toUnsignedLong(wire.readInt());
        if (number > MAX_PIXELS) {
            throw new IOException("Excessive RRE subrectangles: " + number);
        }
        fill(framebuffer, stride, x, y, w, h, readPixel(wire));
        for (long i = 0; i < number; i++) {
            int pixel = readPixel(wire);
            int sx = wire.readUnsignedShort();
            int sy = wire.readUnsignedShort();
            int sw = wire.readUnsignedShort();
            int sh = wire.readUnsignedShort();
            if (sw == 0 || sh == 0 || (long) sx + sw > w || (long) sy + sh > h) {
                throw new IOException("RRE subrectangle extends outside parent rectangle");
            }
            fill(framebuffer, stride, x + sx, y + sy, sw, sh, pixel);
        }
    }

    private void inflateAll(ByteArrayOutputStream output, long expandedLimit) throws IOException {
        try {
            while (!inflater.needsInput()) {
                int n = inflater.inflate(expandedBuffer);
                if (n != 0) {
                    if ((long) output.size() + n > expandedLimit) {
                        throw new IOException("ZRLE inflated rectangle exceeds memory limit");
                    }
                    output.write(expandedBuffer, 0, n);
                } else if (inflater.needsDictionary() || inflater.finished()) {
                    throw new IOException("ZRLE zlib dictionary or stream termination not supported");
                } else if (!inflater.needsInput()) {
                    throw new IOException("ZRLE decoder made no progress");
                }
            }
        } catch (DataFormatException e) {
            throw new IOException("Invalid ZRLE compressed stream", e);
        }
    }

    private static void decodeTiles(DataInputStream stream, int[] pixels, int stride,
            int x, int y, int width, int height, int tileSize, boolean paletteReuse)
            throws IOException {
        int[] previousPalette = null;
        for (int dy = 0; dy < height; dy += tileSize) {
            int th = Math.min(tileSize, height - dy);
            for (int dx = 0; dx < width; dx += tileSize) {
                int tw = Math.min(tileSize, width - dx);
                int encoding = stream.readUnsignedByte();
                int paletteSize;
                int[] palette;
                if (encoding == 127 || encoding == 129) {
                    if (!paletteReuse || previousPalette == null) {
                        throw new IOException("Invalid tile palette reuse");
                    }
                    palette = previousPalette;
                    paletteSize = palette.length;
                } else {
                    paletteSize = encoding & 0x7f;
                    if (encoding != 0 && encoding != 128
                            && paletteSize != 0 && paletteSize != 1
                            && !(paletteSize >= 2 && paletteSize <= 16)
                            && encoding < 128) {
                        throw new IOException("Reserved packed-palette tile type: " + encoding);
                    }
                    if (encoding >= 128 && paletteSize == 1) {
                        throw new IOException("Invalid RLE tile palette size 1");
                    }
                    palette = new int[paletteSize];
                    for (int i = 0; i < paletteSize; i++) {
                        palette[i] = readCompactPixel(stream);
                    }
                    if (paletteSize > 0) {
                        previousPalette = palette;
                    }
                }

                int px = x + dx;
                int py = y + dy;
                if (encoding == 0) {
                    for (int row = 0; row < th; row++) {
                        int offset = (py + row) * stride + px;
                        for (int col = 0; col < tw; col++) {
                            pixels[offset + col] = readCompactPixel(stream);
                        }
                    }
                } else if (encoding == 1) {
                    fill(pixels, stride, px, py, tw, th, palette[0]);
                } else if (encoding >= 2 && encoding <= 16 || encoding == 127) {
                    if (paletteSize < 2 || paletteSize > 16) {
                        throw new IOException("Invalid packed tile palette size: " + paletteSize);
                    }
                    decodePacked(stream, pixels, stride, px, py, tw, th, palette);
                } else if (encoding == 128) {
                    decodeRle(stream, pixels, stride, px, py, tw, th, null);
                } else if (encoding == 129 || encoding >= 130) {
                    decodeRle(stream, pixels, stride, px, py, tw, th, palette);
                } else {
                    throw new IOException("Invalid tile subencoding: " + encoding);
                }
            }
        }
    }

    private static void decodePacked(DataInputStream input, int[] pixels, int stride,
            int x, int y, int w, int h, int[] palette) throws IOException {
        int bits = palette.length == 2 ? 1 : palette.length <= 4 ? 2 : 4;
        int perByte = 8 / bits;
        int mask = (1 << bits) - 1;
        for (int row = 0; row < h; row++) {
            int packed = 0;
            for (int col = 0; col < w; col++) {
                if (col % perByte == 0) {
                    packed = input.readUnsignedByte();
                }
                int index = (packed >>> (8 - bits * ((col % perByte) + 1))) & mask;
                if (index >= palette.length) {
                    throw new IOException("Palette index out of bounds");
                }
                pixels[(y + row) * stride + x + col] = palette[index];
            }
        }
    }

    private static void decodeRle(DataInputStream input, int[] pixels, int stride,
            int x, int y, int w, int h, int[] palette) throws IOException {
        int position = 0;
        int count = w * h;
        while (position < count) {
            final int color;
            final int length;
            if (palette == null) {
                color = readCompactPixel(input);
                length = readRunLength(input, count - position);
            } else {
                int index = input.readUnsignedByte();
                if ((index & 127) >= palette.length) {
                    throw new IOException("Invalid RLE palette index");
                }
                color = palette[index & 127];
                length = (index & 128) != 0
                        ? readRunLength(input, count - position) : 1;
            }
            int remaining = length;
            while (remaining > 0) {
                int row = position / w;
                int col = position % w;
                int span = Math.min(remaining, w - col);
                Arrays.fill(pixels, (y + row) * stride + x + col,
                        (y + row) * stride + x + col + span, color);
                position += span;
                remaining -= span;
            }
        }
    }

    private static int readRunLength(DataInputStream input, int remaining) throws IOException {
        long count = 1;
        int piece;
        do {
            piece = input.readUnsignedByte();
            count += piece;
            if (count > remaining) {
                throw new IOException("RLE run extends outside tile");
            }
        } while (piece == 255);
        return (int) count;
    }

    private static int readCompactPixel(DataInputStream input) throws IOException {
        int blue = input.readUnsignedByte();
        int green = input.readUnsignedByte();
        int red = input.readUnsignedByte();
        return (red << 16) | (green << 8) | blue;
    }

    private static int readPixel(DataInputStream input) throws IOException {
        int rgb = readCompactPixel(input);
        input.readUnsignedByte(); // requested 32-bit PIXEL has a fourth byte
        return rgb;
    }

    private static void fill(int[] pixels, int stride, int x, int y,
            int w, int h, int color) {
        for (int row = 0; row < h; row++) {
            Arrays.fill(pixels, (y + row) * stride + x,
                    (y + row) * stride + x + w, color);
        }
    }

    private static void validateFrame(int[] pixels, int stride,
            int x, int y, int w, int h) throws IOException {
        if (pixels == null || stride <= 0 || x < 0 || y < 0
                || w <= 0 || h <= 0 || (long) x + w > stride
                || (y + (long) h) * stride > pixels.length
                || (long) w * h > MAX_PIXELS) {
            throw new IOException("RFB tile rectangle is outside the framebuffer");
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("RFB tile decoder already closed");
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            inflater.end();
        }
    }
}
