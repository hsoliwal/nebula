/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import org.junit.Test;

/** Deterministic, array-stride-guarded randomized decoder checks. */
public final class RfbCodecPropertyTest {
    private static final long SEED = 6143L;

    @Test public void rawTilesAcrossPartialEdges() throws Exception {
        Random random = new Random(SEED);
        for (int trial = 0; trial < 300; trial++) {
            int w = 1 + random.nextInt(42);
            int h = 1 + random.nextInt(33);
            int stride = w + random.nextInt(6);
            int y = random.nextInt(3);
            int[] expected = new int[stride * (y + h + 1)];
            int[] actual = expected.clone();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);
            for (int ty = 0; ty < h; ty += 16) {
                for (int tx = 0; tx < w; tx += 16) {
                    out.writeByte(0);
                    for (int row = 0; row < Math.min(16, h - ty); row++) {
                        for (int col = 0; col < Math.min(16, w - tx); col++) {
                            int rgb = random.nextInt(0x1000000);
                            pixel(out, rgb);
                            expected[(y + ty + row) * stride + tx + col] = rgb;
                        }
                    }
                }
            }
            try (RfbTileDecoder decoder = new RfbTileDecoder()) {
                decoder.decodeTrle(input(buffer), actual, stride, 0, y, w, h);
            }
            assertArrayEquals("Raw trial " + trial, expected, actual);
        }
    }

    @Test public void packedPaletteTilesAcrossPartialEdges() throws Exception {
        Random random = new Random(SEED + 1);
        int[] sizes = {2, 3, 4, 5, 9, 16};
        for (int trial = 0; trial < 300; trial++) {
            int w = 1 + random.nextInt(44);
            int h = 1 + random.nextInt(39);
            int stride = w + 4;
            int y = 2;
            int n = sizes[random.nextInt(sizes.length)];
            int bits = n == 2 ? 1 : n <= 4 ? 2 : 4;
            int perByte = 8 / bits;
            int[] expected = new int[stride * (y + h + 1)];
            int[] actual = expected.clone();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);
            for (int ty = 0; ty < h; ty += 16) {
                for (int tx = 0; tx < w; tx += 16) {
                    int[] palette = new int[n];
                    out.writeByte(n);
                    for (int i = 0; i < n; i++) {
                        palette[i] = random.nextInt(0x1000000);
                        pixel(out, palette[i]);
                    }
                    for (int row = 0; row < Math.min(16, h - ty); row++) {
                        int packed = 0;
                        int count = 0;
                        for (int col = 0; col < Math.min(16, w - tx); col++) {
                            int index = random.nextInt(n);
                            expected[(y + ty + row) * stride + tx + col] = palette[index];
                            packed |= index << (8 - bits * ((col % perByte) + 1));
                            if (++count == perByte) {
                                out.writeByte(packed);
                                packed = 0;
                                count = 0;
                            }
                        }
                        if (count > 0) {
                            out.writeByte(packed);
                        }
                    }
                }
            }
            try (RfbTileDecoder decoder = new RfbTileDecoder()) {
                decoder.decodeTrle(input(buffer), actual, stride, 0, y, w, h);
            }
            assertArrayEquals("Packed palette trial " + trial, expected, actual);
        }
    }

    @Test public void plainAndPaletteRunsAcrossTileRows() throws Exception {
        Random random = new Random(SEED + 2);
        for (int trial = 0; trial < 300; trial++) {
            int w = 1 + random.nextInt(50);
            int h = 1 + random.nextInt(32);
            int stride = w + 7;
            int y = 1;
            boolean paletteMode = random.nextBoolean();
            int[] expected = new int[stride * (y + h + 1)];
            int[] actual = expected.clone();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);
            int[] palette = {0x102030, 0x40a0b0};
            for (int ty = 0; ty < h; ty += 16) {
                for (int tx = 0; tx < w; tx += 16) {
                    int tw = Math.min(16, w - tx);
                    int th = Math.min(16, h - ty);
                    int count = tw * th;
                    int pos = 0;
                    out.writeByte(paletteMode ? 130 : 128);
                    if (paletteMode) {
                        for (int rgb : palette) {
                            pixel(out, rgb);
                        }
                    }
                    while (pos < count) {
                        int run = Math.min(count - pos, 1 + random.nextInt(300));
                        int rgb = paletteMode
                                ? palette[random.nextInt(2)] : random.nextInt(0x1000000);
                        if (paletteMode) {
                            int index = rgb == palette[0] ? 0 : 1;
                            out.writeByte(run == 1 ? index : index | 0x80);
                            if (run != 1) {
                                runLength(out, run);
                            }
                        } else {
                            pixel(out, rgb);
                            runLength(out, run);
                        }
                        for (int i = 0; i < run; i++) {
                            int n = pos + i;
                            expected[(y + ty + n / tw) * stride + tx + n % tw] = rgb;
                        }
                        pos += run;
                    }
                }
            }
            try (RfbTileDecoder decoder = new RfbTileDecoder()) {
                decoder.decodeTrle(input(buffer), actual, stride, 0, y, w, h);
            }
            assertArrayEquals("RLE trial " + trial, expected, actual);
        }
    }

    private static DataInputStream input(ByteArrayOutputStream buffer) {
        return new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()));
    }
    private static void pixel(DataOutputStream output, int color) throws IOException {
        output.writeByte(color);
        output.writeByte(color >>> 8);
        output.writeByte(color >>> 16);
    }
    private static void runLength(DataOutputStream output, int length) throws IOException {
        int count = length - 1;
        while (count >= 255) {
            output.writeByte(255);
            count -= 255;
        }
        output.writeByte(count);
    }
}
