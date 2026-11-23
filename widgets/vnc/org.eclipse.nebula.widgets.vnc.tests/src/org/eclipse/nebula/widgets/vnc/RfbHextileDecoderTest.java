/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 *
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

import org.junit.Test;

/** RFC 6143 Hextile tile flags, carried colors and rectangular bounds. */
public final class RfbHextileDecoderTest {
    private static final int RED = 0x112233;
    private static final int GREEN = 0x445566;

    @Test public void rawIgnoresAllOtherFlagsAndReadsBgrPixels() throws Exception {
        // All flags including reserved bits are irrelevant with the Raw bit.
        byte[] payload = packet(out -> {
            out.writeByte(0xff);
            rgb(out, RED);
        });
        int[] frame = new int[1];
        decode(payload, frame, 1, 0, 0, 1, 1);
        assertArrayEquals(new int[] {RED}, frame);
    }

    @Test public void backgroundCarriesToNextTileOfRectangle() throws Exception {
        int[] frame = new int[17];
        byte[] payload = packet(out -> {
            out.writeByte(2);
            rgb(out, RED);
            out.writeByte(0); // one-pixel second tile carries background
        });
        decode(payload, frame, 17, 0, 0, 17, 1);
        for (int color : frame) {
            assertEquals(RED, color);
        }
    }

    @Test public void firstNonRawTileRequiresBackground() {
        assertThrows(IOException.class, () ->
                decode(new byte[] {0}, new int[1], 1, 0, 0, 1, 1));
    }

    @Test public void rawInvalidatesCarriedBackgroundAndForeground() throws Exception {
        byte[] payload = packet(out -> {
            out.writeByte(14); // first tile: bg, fg, subrectangle
            rgb(out, RED);
            rgb(out, GREEN);
            out.writeByte(1);
            out.writeByte(0); // subrect x=0,y=0
            out.writeByte(0); // 1x1
            out.writeByte(0xfd); // Raw tile with irrelevant other bits
            for (int i = 0; i < 16; i++) {
                rgb(out, RED);
            }
            out.writeByte(0); // now invalid: cannot carry bg past Raw
        });
        assertThrows(IOException.class, () ->
                decode(payload, new int[33], 33, 0, 0, 33, 1));
    }

    @Test public void foregroundCarriesToNextUncoloredTile() throws Exception {
        byte[] payload = packet(out -> {
            out.writeByte(14); // background, foreground, uncolored subrect
            rgb(out, RED);
            rgb(out, GREEN);
            out.writeByte(1);
            out.writeByte(0);
            out.writeByte(0);
            out.writeByte(10); // new background, one subrect using carried fg
            rgb(out, RED);
            out.writeByte(1);
            out.writeByte(0);
            out.writeByte(0);
        });
        int[] frame = new int[17];
        decode(payload, frame, 17, 0, 0, 17, 1);
        assertEquals(GREEN, frame[0]);
        assertEquals(RED, frame[1]);
        assertEquals(GREEN, frame[16]);
    }

    @Test public void coloredSubrectInvalidatesPreviouslyCarriedForeground() throws Exception {
        byte[] payload = packet(out -> {
            out.writeByte(26); // background, colored subrectangles
            rgb(out, RED);
            out.writeByte(1);
            rgb(out, GREEN);
            out.writeByte(0);
            out.writeByte(0);
            out.writeByte(10); // next tile has bg and uncolored subrect
            rgb(out, RED);
            out.writeByte(1);
            out.writeByte(0);
            out.writeByte(0);
        });
        assertThrows(IOException.class, () ->
                decode(payload, new int[17], 17, 0, 0, 17, 1));
    }

    @Test public void illegalFlagCombosAndSubrectBoundsFailClosed() throws Exception {
        assertThrows(IOException.class, () ->
                decode(new byte[] {0x20}, new int[1], 1, 0, 0, 1, 1));
        assertThrows(IOException.class, () ->
                decode(new byte[] {30}, new int[1], 1, 0, 0, 1, 1));
        byte[] overflow = packet(out -> {
            out.writeByte(14);
            rgb(out, RED);
            rgb(out, GREEN);
            out.writeByte(1);
            out.writeByte(0x10); // x=1 outside a 1x1 tile
            out.writeByte(0);
        });
        assertThrows(IOException.class, () ->
                decode(overflow, new int[1], 1, 0, 0, 1, 1));
    }

    @Test public void truncatedRawAndInvalidDestinationFailClosed() {
        assertThrows(IOException.class, () ->
                decode(new byte[] {1, 0, 0, 0}, new int[1], 1, 0, 0, 1, 1));
        assertThrows(IOException.class, () ->
                decode(new byte[] {1}, new int[2], 1, 0, 0, 2, 1));
        assertThrows(IOException.class, () ->
                decode(new byte[] {1}, new int[1], 1, 0, 0, 0, 1));
    }

    @Test public void offsetRowsPreserveUnwrittenData() throws Exception {
        int[] frame = new int[5 * 4];
        Arrays.fill(frame, 0x777777);
        byte[] payload = packet(out -> {
            out.writeByte(2);
            rgb(out, GREEN);
        });
        decode(payload, frame, 5, 2, 1, 2, 2);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 5; col++) {
                assertEquals(row >= 1 && row <= 2 && col >= 2 && col <= 3
                        ? GREEN : 0x777777, frame[row * 5 + col]);
            }
        }
    }

    private static void decode(byte[] bytes, int[] frame, int stride,
            int x, int y, int w, int h) throws IOException {
        RfbHextileDecoder.decode(new DataInputStream(new ByteArrayInputStream(bytes)),
                frame, stride, x, y, w, h);
    }

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    private static byte[] packet(Writer writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(bytes);
        writer.write(stream);
        stream.flush();
        return bytes.toByteArray();
    }

    private static void rgb(DataOutputStream out, int rgb) throws IOException {
        out.writeByte(rgb);
        out.writeByte(rgb >>> 8);
        out.writeByte(rgb >>> 16);
        out.writeByte(0);
    }
}
