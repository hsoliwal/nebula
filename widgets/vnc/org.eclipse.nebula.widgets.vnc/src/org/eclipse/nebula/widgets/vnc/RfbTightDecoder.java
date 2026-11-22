/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Worker-thread-only Tight (encoding 7) decoder for RGB888.
 *
 * <p>Uses four persistent zlib streams as specified by Tight. Per-rectangle
 * reset bits are honored for all subencodings, even Fill and JPEG. The decoder
 * bounds dimensions, compact lengths and decompressed data before allocating.
 * No SWT objects are accessed from this code.</p>
 */
final class RfbTightDecoder implements AutoCloseable {
    private static final int MAX_PIXELS = 16_777_216;
    private static final int MAX_COMPACT_LENGTH = 4_194_303;
    private final Inflater[] streams = new Inflater[4];
    private byte[] compressed = new byte[0];
    private byte[] raw = new byte[0];
    private boolean disposed;

    RfbTightDecoder() {
        for (int i = 0; i < streams.length; i++) {
            streams[i] = new Inflater();
        }
    }

    void decode(DataInputStream wire, int[] framebuffer, int stride,
            int x, int y, int width, int height) throws IOException {
        if (disposed) {
            throw new IOException("Tight decoder disposed");
        }
        if (framebuffer == null || stride < 1 || width < 1 || height < 1 || x < 0 || y < 0
                || (long) x + width > stride
                || (long) (y + height) * stride > framebuffer.length
                || (long) width * height > MAX_PIXELS) {
            throw new IOException("Invalid Tight framebuffer rectangle");
        }
        int control = wire.readUnsignedByte();
        for (int stream = 0; stream < 4; stream++) {
            if ((control & (1 << stream)) != 0) {
                streams[stream].reset();
            }
        }
        int subtype = control >>> 4;
        if (subtype == 8) {
            fill(framebuffer, stride, x, y, width, height, readRgb(wire));
            return;
        }
        if (subtype == 9) {
            decodeJpeg(wire, framebuffer, stride, x, y, width, height);
            return;
        }
        boolean explicitUncompressed = subtype == 10 || subtype == 14;
        if (subtype > 7 && !explicitUncompressed) {
            throw new IOException("Unsupported Tight subencoding: " + subtype);
        }
        int filter = subtype == 14 || subtype < 8 && (subtype & 4) != 0
                ? wire.readUnsignedByte() : 0;
        if (filter > 2) {
            throw new IOException("Invalid Tight filter: " + filter);
        }
        int[] palette = null;
        int length;
        if (filter == 1) {
            int colors = wire.readUnsignedByte() + 1;
            if (colors < 2 || colors > 256) {
                throw new IOException("Invalid Tight palette count: " + colors);
            }
            palette = new int[colors];
            for (int i = 0; i < colors; i++) {
                palette[i] = readRgb(wire);
            }
            length = colors == 2 ? ((width + 7) / 8) * height : width * height;
        } else {
            length = width * height * 3;
        }
        if (raw.length < length) {
            raw = new byte[length];
        }
        if (explicitUncompressed || length < 12) {
            wire.readFully(raw, 0, length);
        } else {
            int compressedSize = readCompactLength(wire);
            if (compressed.length < compressedSize) {
                compressed = new byte[compressedSize];
            }
            wire.readFully(compressed, 0, compressedSize);
            inflateExactly(streams[subtype & 3], compressedSize, length);
        }
        if (filter == 0) {
            copyColors(framebuffer, stride, x, y, width, height);
        } else if (filter == 1) {
            paletteColors(framebuffer, stride, x, y, width, height, palette);
        } else {
            gradientColors(framebuffer, stride, x, y, width, height);
        }
    }

    private void inflateExactly(Inflater decoder, int compressedSize, int expected) throws IOException {
        if (!decoder.needsInput()) {
            throw new IOException("Previous Tight zlib fragment not completely consumed");
        }
        decoder.setInput(compressed, 0, compressedSize);
        int offset = 0;
        try {
            while (offset < expected) {
                int count = decoder.inflate(raw, offset, expected - offset);
                if (count > 0) {
                    offset += count;
                } else if (decoder.needsInput() || decoder.needsDictionary() || decoder.finished()) {
                    throw new IOException("Tight zlib stream ended before requested pixel data");
                } else {
                    throw new IOException("Tight zlib decoder made no progress");
                }
            }
            byte[] probe = new byte[1];
            while (!decoder.needsInput()) {
                int remainingBefore = decoder.getRemaining();
                int count = decoder.inflate(probe);
                if (count != 0) {
                    throw new IOException("Tight inflated data exceeds expected rectangle size");
                }
                if (decoder.needsDictionary() || decoder.finished()) {
                    throw new IOException("Unexpected Tight zlib dictionary or terminal stream");
                }
                if (!decoder.needsInput() && decoder.getRemaining() >= remainingBefore) {
                    throw new IOException("Tight compressed stream made no progress");
                }
            }
        } catch (DataFormatException error) {
            throw new IOException("Invalid Tight compressed data", error);
        }
    }

    private void copyColors(int[] framebuffer, int stride, int x, int y, int width, int height) {
        int position = 0;
        for (int row = 0; row < height; row++) {
            int offset = (y + row) * stride + x;
            for (int col = 0; col < width; col++) {
                int red = raw[position++] & 0xff;
                int green = raw[position++] & 0xff;
                int blue = raw[position++] & 0xff;
                framebuffer[offset + col] = (red << 16) | (green << 8) | blue;
            }
        }
    }

    private void paletteColors(int[] framebuffer, int stride, int x, int y,
            int width, int height, int[] palette) throws IOException {
        int position = 0;
        for (int row = 0; row < height; row++) {
            int offset = (y + row) * stride + x;
            if (palette.length == 2) {
                int packed = 0;
                for (int col = 0; col < width; col++) {
                    if ((col & 7) == 0) {
                        packed = raw[position++] & 0xff;
                    }
                    framebuffer[offset + col] = palette[(packed >>> (7 - (col & 7))) & 1];
                }
            } else {
                for (int col = 0; col < width; col++) {
                    int index = raw[position++] & 0xff;
                    if (index >= palette.length) {
                        throw new IOException("Tight palette index outside declared palette");
                    }
                    framebuffer[offset + col] = palette[index];
                }
            }
        }
    }

    private void gradientColors(int[] framebuffer, int stride, int x, int y,
            int width, int height) {
        byte[] above = new byte[width * 3];
        byte[] current = new byte[width * 3];
        int position = 0;
        for (int row = 0; row < height; row++) {
            int offset = (y + row) * stride + x;
            for (int col = 0; col < width; col++) {
                int rgb = 0;
                for (int channel = 0; channel < 3; channel++) {
                    int index = col * 3 + channel;
                    int left = col == 0 ? 0 : current[index - 3] & 0xff;
                    int top = above[index] & 0xff;
                    int topLeft = col == 0 ? 0 : above[index - 3] & 0xff;
                    int predicted = Math.max(0, Math.min(255, left + top - topLeft));
                    int color = ((raw[position++] & 0xff) + predicted) & 0xff;
                    current[index] = (byte) color;
                    rgb = (rgb << 8) | color;
                }
                framebuffer[offset + col] = rgb;
            }
            byte[] old = above;
            above = current;
            current = old;
        }
    }

    private void decodeJpeg(DataInputStream wire, int[] framebuffer, int stride,
            int x, int y, int width, int height) throws IOException {
        int n = readCompactLength(wire);
        byte[] contents = new byte[n];
        wire.readFully(contents);
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(contents))) {
            if (stream == null) {
                throw new IOException("No ImageIO input stream for Tight JPEG");
            }
            Iterator<ImageReader> providers = ImageIO.getImageReaders(stream);
            if (!providers.hasNext()) {
                throw new IOException("Unsupported Tight JPEG image");
            }
            ImageReader reader = providers.next();
            try {
                if (!reader.getFormatName().equalsIgnoreCase("JPEG")) {
                    throw new IOException("Tight image is not JPEG");
                }
                reader.setInput(stream, true, true);
                if (reader.getWidth(0) != width || reader.getHeight(0) != height) {
                    throw new IOException("Tight JPEG image dimensions disagree with RFB rectangle");
                }
                BufferedImage decoded = reader.read(0);
                int[] scanline = new int[width];
                for (int row = 0; row < height; row++) {
                    decoded.getRGB(0, row, width, 1, scanline, 0, width);
                    for (int col = 0; col < width; col++) {
                        framebuffer[(y + row) * stride + x + col] = scanline[col] & 0xffffff;
                    }
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private static int readCompactLength(DataInputStream input) throws IOException {
        int first = input.readUnsignedByte();
        int result = first & 0x7f;
        if ((first & 0x80) != 0) {
            int second = input.readUnsignedByte();
            result |= (second & 0x7f) << 7;
            if ((second & 0x80) != 0) {
                result |= input.readUnsignedByte() << 14;
            }
        }
        if (result <= 0 || result > MAX_COMPACT_LENGTH) {
            throw new IOException("Invalid Tight compact length: " + result);
        }
        return result;
    }

    private static int readRgb(DataInputStream input) throws IOException {
        return (input.readUnsignedByte() << 16)
                | (input.readUnsignedByte() << 8)
                | input.readUnsignedByte();
    }

    private static void fill(int[] framebuffer, int stride, int x, int y,
            int width, int height, int rgb) {
        for (int row = 0; row < height; row++) {
            Arrays.fill(framebuffer, (y + row) * stride + x,
                    (y + row) * stride + x + width, rgb);
        }
    }

    @Override
    public void close() {
        if (!disposed) {
            disposed = true;
            for (Inflater stream : streams) {
                stream.end();
            }
        }
    }
}
