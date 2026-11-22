/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import javax.imageio.ImageIO;

import org.junit.Test;

/** Tight RGB888, palette, zlib, JPEG and fail-closed packet contracts. */
public class RfbTightDecoderTest {
    private static final int RED = 0xff0000;
    private static final int GREEN = 0x00ff00;
    private static final int BLUE = 0x0000ff;

    @Test public void fillAndUncompressedRgbOrder() throws Exception {
        int[] frame = new int[6];
        try (RfbTightDecoder decoder = new RfbTightDecoder()) {
            decoder.decode(input(packet(out -> {
                out.writeByte(0x80); rgb(out, 0x123456);
            })), frame, 3, 0, 0, 3, 2);
            assertArrayEquals(new int[] {
                    0x123456,0x123456,0x123456,0x123456,0x123456,0x123456
            }, frame);
            Arrays.fill(frame, 0);
            decoder.decode(input(packet(out -> {
                out.writeByte(0xa0); rgb(out, RED); rgb(out, GREEN);
                rgb(out, BLUE); rgb(out, 0xffffff);
            })), frame, 3, 0, 0, 2, 2);
            assertArrayEquals(new int[] {RED, GREEN, 0, BLUE, 0xffffff, 0}, frame);
        }
    }

    @Test public void paletteTwoColorsAndBytePalette() throws Exception {
        int[] frame = new int[6];
        try (RfbTightDecoder decoder = new RfbTightDecoder()) {
            decoder.decode(input(packet(out -> {
                out.writeByte(0xe0); out.writeByte(1); out.writeByte(1);
                rgb(out, RED); rgb(out, BLUE); out.writeByte(0b10100000);
            })), frame, 3, 0, 0, 3, 1);
            assertArrayEquals(new int[] {BLUE, RED, BLUE, 0, 0, 0}, frame);
            Arrays.fill(frame, 0);
            decoder.decode(input(packet(out -> {
                out.writeByte(0xe0); out.writeByte(1); out.writeByte(2);
                rgb(out, RED); rgb(out, GREEN); rgb(out, BLUE);
                out.writeByte(2); out.writeByte(0); out.writeByte(1);
            })), frame, 3, 0, 0, 3, 1);
            assertArrayEquals(new int[] {BLUE, RED, GREEN, 0, 0, 0}, frame);
        }
    }

    @Test public void gradientReconstructsPreviousPixel() throws Exception {
        int[] frame = new int[3];
        try (RfbTightDecoder decoder = new RfbTightDecoder()) {
            decoder.decode(input(packet(out -> {
                out.writeByte(0xe0); out.writeByte(2);
                rgb(out, 0x102030); rgb(out, 0x020202);
            })), frame, 3, 0, 0, 2, 1);
        }
        assertArrayEquals(new int[] {0x102030, 0x122232, 0}, frame);
    }

    @Test public void jpegCodecEnforcesDimensions() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xff0000);
        image.setRGB(1, 0, 0x00ff00);
        image.setRGB(0, 1, 0x0000ff);
        image.setRGB(1, 1, 0xffffff);
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "jpeg", jpeg));
        byte[] stream = packet(out -> {
            out.writeByte(0x90); compact(out, jpeg.toByteArray());
        });
        int[] frame = new int[4];
        try (RfbTightDecoder decoder = new RfbTightDecoder()) {
            decoder.decode(input(stream), frame, 2, 0, 0, 2, 2);
            assertFalse(Arrays.equals(frame, new int[4]));
        }
        try (RfbTightDecoder decoder = new RfbTightDecoder()) {
            assertThrows(IOException.class, () ->
                    decoder.decode(input(stream), frame, 2, 0, 0, 1, 1));
        }
    }

    @Test public void keepsZlibDictionaryAcrossRectangles() throws Exception {
        byte[] first = packet(out -> {
            rgb(out, 0x010203); rgb(out, 0x040506);
            rgb(out, 0x070809); rgb(out, 0x0a0b0c);
        });
        byte[] second = packet(out -> {
            rgb(out, 0x111213); rgb(out, 0x141516);
            rgb(out, 0x171819); rgb(out, 0x1a1b1c);
        });
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        Deflater deflater = new Deflater();
        byte[] a;
        byte[] b;
        try (DeflaterOutputStream out =
                new DeflaterOutputStream(compressed, deflater, 512, true)) {
            out.write(first);
            out.flush();
            a = compressed.toByteArray();
            compressed.reset();
            out.write(second);
            out.flush();
            b = compressed.toByteArray();
        }
        int[] frame = new int[4];
        try (RfbTightDecoder decoder = new RfbTightDecoder()) {
            decoder.decode(input(packet(out -> {
                out.writeByte(0); compact(out, a);
            })), frame, 2, 0, 0, 2, 2);
            assertArrayEquals(new int[] {0x010203,0x040506,0x070809,0x0a0b0c}, frame);
            decoder.decode(input(packet(out -> {
                out.writeByte(0); compact(out, b);
            })), frame, 2, 0, 0, 2, 2);
            assertArrayEquals(new int[] {0x111213,0x141516,0x171819,0x1a1b1c}, frame);
        }
    }

    @Test public void malformedDimensionsAndSubencodingsFailClosed() throws Exception {
        int[] frame = new int[5];
        try (RfbTightDecoder decoder = new RfbTightDecoder()) {
            assertThrows(IOException.class, () ->
                    decoder.decode(input(new byte[] {(byte) 0xf0}), frame, 5, 0, 0, 5, 1));
            assertThrows(IOException.class, () ->
                    decoder.decode(input(new byte[] {(byte) 0xe0, 3}), frame, 5, 0, 0, 5, 1));
            assertThrows(IOException.class, () ->
                    decoder.decode(input(new byte[] {(byte) 0xa0, 0, 0}), frame, 5, 0, 0, 5, 1));
            assertThrows(IOException.class, () ->
                    decoder.decode(input(new byte[] {(byte) 0x90, 0}), frame, 5, 0, 0, 5, 1));
            assertThrows(IOException.class, () ->
                    decoder.decode(input(new byte[0]), frame, 2049, 0, 0, 2049, 1));
        }
    }

    @Test public void fourHundredDeterministicCodecCases() throws Exception {
        Random random = new Random(0x6143);
        try (RfbTightDecoder decoder = new RfbTightDecoder()) {
            for (int mode = 0; mode < 4; mode++) {
                for (int trial = 0; trial < 100; trial++) {
                    int w = 1 + random.nextInt(25);
                    int h = 1 + random.nextInt(21);
                    int stride = w + 2;
                    int[] expected = new int[stride * (h + 2)];
                    int[] actual = new int[expected.length];
                    final int width = w, height = h, currentMode = mode;
                    byte[] bytes = packet(out -> {
                        if (currentMode == 0) {
                            out.writeByte(0xa0);
                            for (int y = 0; y < height; y++) {
                                for (int x = 0; x < width; x++) {
                                    int rgb = random.nextInt(0x1000000);
                                    expected[(y + 1) * stride + x + 1] = rgb;
                                    rgb(out, rgb);
                                }
                            }
                        } else if (currentMode == 1 || currentMode == 2) {
                            int count = currentMode == 1 ? 2 : 3 + random.nextInt(30);
                            out.writeByte(0xe0); out.writeByte(1); out.writeByte(count - 1);
                            int[] palette = new int[count];
                            for (int i = 0; i < count; i++) {
                                palette[i] = random.nextInt(0x1000000);
                                rgb(out, palette[i]);
                            }
                            for (int y = 0; y < height; y++) {
                                int packed = 0, filled = 0;
                                for (int x = 0; x < width; x++) {
                                    int index = random.nextInt(count);
                                    expected[(y + 1) * stride + x + 1] = palette[index];
                                    if (count == 2) {
                                        packed |= index << (7 - (x & 7));
                                        if (++filled == 8) {
                                            out.writeByte(packed);
                                            packed = 0;
                                            filled = 0;
                                        }
                                    } else {
                                        out.writeByte(index);
                                    }
                                }
                                if (count == 2 && filled > 0) {
                                    out.writeByte(packed);
                                }
                            }
                        } else {
                            out.writeByte(0xe0); out.writeByte(2);
                            int[] above = new int[width * 3];
                            int[] current = new int[width * 3];
                            for (int y = 0; y < height; y++) {
                                for (int x = 0; x < width; x++) {
                                    int color = 0;
                                    for (int c = 0; c < 3; c++) {
                                        int value = random.nextInt(256);
                                        int position = x * 3 + c;
                                        int left = x == 0 ? 0 : current[position - 3];
                                        int top = above[position];
                                        int topLeft = x == 0 ? 0 : above[position - 3];
                                        int predict = Math.max(0,
                                                Math.min(255, left + top - topLeft));
                                        out.writeByte((value - predict) & 255);
                                        current[position] = value;
                                        color = color << 8 | value;
                                    }
                                    expected[(y + 1) * stride + x + 1] = color;
                                }
                                int[] saved = above;
                                above = current;
                                current = saved;
                            }
                        }
                    });
                    decoder.decode(input(bytes), actual, stride, 1, 1, w, h);
                    assertArrayEquals("Tight mode " + mode + " trial " + trial, expected, actual);
                }
            }
        }
    }

    private interface Writer { void write(DataOutputStream out) throws IOException; }

    private static byte[] packet(Writer build) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        build.write(output);
        output.flush();
        return bytes.toByteArray();
    }

    private static DataInputStream input(byte[] bytes) {
        return new DataInputStream(new ByteArrayInputStream(bytes));
    }

    private static void rgb(DataOutputStream output, int rgb) throws IOException {
        output.writeByte(rgb >>> 16);
        output.writeByte(rgb >>> 8);
        output.writeByte(rgb);
    }

    private static void compact(DataOutputStream output, byte[] bytes) throws IOException {
        int size = bytes.length;
        int first = size & 127;
        size >>>= 7;
        if (size == 0) {
            output.writeByte(first);
        } else {
            output.writeByte(first | 128);
            int second = size & 127;
            size >>>= 7;
            if (size == 0) {
                output.writeByte(second);
            } else {
                output.writeByte(second | 128);
                output.writeByte(size);
            }
        }
        output.write(bytes);
    }
}
