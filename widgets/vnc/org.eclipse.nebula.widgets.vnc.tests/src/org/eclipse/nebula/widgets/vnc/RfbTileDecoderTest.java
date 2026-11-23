/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.junit.Test;

/** Independent decoder tests run inside Tycho, without SWT or external peers. */
public class RfbTileDecoderTest {

    @Test public void rreRectangleAndBounding() throws Exception {
        int[] frame = new int[8];
        byte[] stream = bytes(out -> {
            out.writeInt(1);
            pixel(out, 0x112233);
            pixel(out, 0xaabbcc);
            out.writeShort(1); out.writeShort(0);
            out.writeShort(2); out.writeShort(2);
        });
        try (RfbTileDecoder decoder = new RfbTileDecoder()) {
            decoder.decodeRre(in(stream), frame, 4, 0, 0, 4, 2);
        }
        assertArrayEquals(new int[] {
                0x112233, 0xaabbcc, 0xaabbcc, 0x112233,
                0x112233, 0xaabbcc, 0xaabbcc, 0x112233 }, frame);
    }

    @Test public void trlePaletteReuseAndPackedRows() throws Exception {
        int[] frame = new int[32];
        byte[] stream = bytes(out -> {
            out.writeByte(2);
            compact(out, 0x102030);
            compact(out, 0x405060);
            out.writeByte(0x55); out.writeByte(0x55);
            out.writeByte(127); // reuse the immediately preceding palette
            out.writeByte(0xaa); out.writeByte(0xaa);
        });
        try (RfbTileDecoder decoder = new RfbTileDecoder()) {
            decoder.decodeTrle(in(stream), frame, 32, 0, 0, 32, 1);
        }
        for (int i = 0; i < 16; i++) {
            assertEquals(i % 2 == 0 ? 0x102030 : 0x405060, frame[i]);
            assertEquals(i % 2 == 0 ? 0x405060 : 0x102030, frame[i + 16]);
        }
    }

    @Test public void trlePlainAndPaletteRunLength() throws Exception {
        int[] plain = new int[5];
        byte[] p = bytes(out -> {
            out.writeByte(128); compact(out, 0xabcdef);
            out.writeByte(4); // one plus four = 5
        });
        int[] palette = new int[4];
        byte[] q = bytes(out -> {
            out.writeByte(130);
            compact(out, 0x112233); compact(out, 0x445566);
            out.writeByte(0x81); out.writeByte(3);
        });
        try (RfbTileDecoder d = new RfbTileDecoder()) {
            d.decodeTrle(in(p), plain, 5, 0, 0, 5, 1);
            d.decodeTrle(in(q), palette, 4, 0, 0, 4, 1);
        }
        assertArrayEquals(new int[] {0xabcdef, 0xabcdef, 0xabcdef, 0xabcdef, 0xabcdef}, plain);
        assertArrayEquals(new int[] {0x445566, 0x445566, 0x445566, 0x445566}, palette);
    }

    @Test public void zrleRetainsInflaterAcrossRectangles() throws Exception {
        byte[] firstTile = bytes(out -> {out.writeByte(1); compact(out, 0x445566);});
        byte[] secondTile = bytes(out -> {out.writeByte(1); compact(out, 0xaabbcc);});
        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        Deflater deflater = new Deflater();
        byte[] a;
        byte[] b;
        try (DeflaterOutputStream out = new DeflaterOutputStream(packed, deflater, 512, true)) {
            out.write(firstTile);
            out.flush();
            a = packed.toByteArray();
            packed.reset();
            out.write(secondTile);
            out.flush();
            b = packed.toByteArray();
        }
        int[] frame = new int[2];
        try (RfbTileDecoder d = new RfbTileDecoder()) {
            d.decodeZrle(in(envelope(a)), frame, 2, 0, 0, 2, 1);
            assertArrayEquals(new int[] {0x445566, 0x445566}, frame);
            d.decodeZrle(in(envelope(b)), frame, 2, 0, 0, 2, 1);
            assertArrayEquals(new int[] {0xaabbcc, 0xaabbcc}, frame);
        }
    }

    @Test public void zrleRejectsInvalidPaletteReuseAndDecompressionBomb() throws Exception {
        int[] frame = new int[1];
        byte[] illegalPalette = deflate(new byte[] {(byte)127});
        try (RfbTileDecoder d = new RfbTileDecoder()) {
            assertThrows(IOException.class, () -> d.decodeZrle(in(envelope(illegalPalette)), frame, 1, 0, 0, 1, 1));
        }
        // A one-pixel tile cannot legally inflate to an unbounded body.
        byte[] junk = new byte[8192];
        Arrays.fill(junk, (byte) 0x33);
        byte[] payload = deflate(junk);
        try (RfbTileDecoder d = new RfbTileDecoder()) {
            assertThrows(IOException.class, () -> d.decodeZrle(in(envelope(payload)), frame, 1, 0, 0, 1, 1));
        }
    }

    @Test public void rejectInvalidRectanglesAndPaletteIndices() throws Exception {
        int[] pixels = new int[4];
        try (RfbTileDecoder d = new RfbTileDecoder()) {
            assertThrows(IOException.class, () -> d.decodeTrle(in(new byte[0]), pixels, 2, 0, 0, 3, 1));
            assertThrows(IOException.class, () -> d.decodeTrle(in(new byte[] {127}), pixels, 2, 0, 0, 1, 1));
            assertThrows(IOException.class, () -> d.decodeTrle(in(new byte[] {(byte)128, 0, 0, 0, 4}), pixels, 2, 0, 0, 1, 1));
        }
    }

    @Test public void boundedDeterministicPaletteRoundTrips() throws Exception {
        Random random = new Random(0x6143);
        for (int iteration = 0; iteration < 128; iteration++) {
            int[] pixels = new int[16];
            int colorA = random.nextInt(0x1000000);
            int colorB = random.nextInt(0x1000000);
            byte[] stream = bytes(out -> {
                out.writeByte(2); compact(out, colorA); compact(out, colorB);
                int mask = 0;
                for (int i = 0; i < 8; i++) if (random.nextBoolean()) mask |= 1 << (7 - i);
                out.writeByte(mask);
                mask = 0;
                for (int i = 0; i < 8; i++) if (random.nextBoolean()) mask |= 1 << (7 - i);
                out.writeByte(mask);
            });
            try (RfbTileDecoder d = new RfbTileDecoder()) {
                d.decodeTrle(in(stream), pixels, 16, 0, 0, 16, 1);
            }
            for (int value : pixels) assertTrue(value == colorA || value == colorB);
        }
    }

    private interface Write { void accept(DataOutputStream out) throws IOException; }
    private static byte[] bytes(Write writer) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        writer.accept(out);
        out.flush();
        return b.toByteArray();
    }
    private static DataInputStream in(byte[] b) {return new DataInputStream(new ByteArrayInputStream(b));}
    private static void compact(DataOutputStream o, int pixel) throws IOException {
        o.writeByte(pixel); o.writeByte(pixel >>> 8); o.writeByte(pixel >>> 16);
    }
    private static void pixel(DataOutputStream o, int pixel) throws IOException {compact(o, pixel); o.writeByte(0);}
    private static byte[] envelope(byte[] bytes) throws IOException {
        return bytes(o -> {o.writeInt(bytes.length);o.write(bytes);});
    }
    private static byte[] deflate(byte[] bytes) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        Deflater deflater = new Deflater();
        try (DeflaterOutputStream out = new DeflaterOutputStream(b, deflater, 512, true)) {
            out.write(bytes); out.flush();
            return b.toByteArray();
        }
    }
}
