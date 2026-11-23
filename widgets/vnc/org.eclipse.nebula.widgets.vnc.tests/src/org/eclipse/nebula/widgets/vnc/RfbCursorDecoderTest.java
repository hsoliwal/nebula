/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.junit.Test;

/** Bounds, RGB order, bit masks and immutability of RFB cursor shapes. */
public final class RfbCursorDecoderTest {
    private static DataInputStream input(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return new DataInputStream(new ByteArrayInputStream(bytes));
    }

    @Test public void richCursorUsesLsbRgbPixelsAndMsbOpacityMask() throws Exception {
        RfbCursorDecoder.Shape shape = RfbCursorDecoder.readRich(
                input(0, 0, 255, 0, 0, 255, 0, 0, 0x80),
                1, 0, 2, 1);
        assertEquals(2, shape.width());
        assertEquals(1, shape.height());
        assertEquals(1, shape.hotspotX());
        assertEquals(0, shape.hotspotY());
        assertArrayEquals(new int[] {0xffff0000, 0x0000ff00}, shape.argbCopy());
        int[] escaped = shape.argbCopy();
        escaped[0] = 0;
        assertEquals(0xffff0000, shape.argbCopy()[0]);
    }

    @Test public void xCursorUsesForegroundBackgroundAndAlphaMask() throws Exception {
        RfbCursorDecoder.Shape shape = RfbCursorDecoder.readX(
                input(255, 0, 0, 0, 0, 255, 0x80, 0xc0),
                0, 0, 2, 1);
        assertArrayEquals(new int[] {0xffff0000, 0xff0000ff}, shape.argbCopy());
    }

    @Test public void zeroSizeCursorHidesAndInvalidSizesFailClosed() throws Exception {
        assertNull(RfbCursorDecoder.readRich(input(), 0, 0, 0, 0));
        assertNull(RfbCursorDecoder.readX(input(), 0, 0, 0, 0));
        assertThrows(IOException.class, () -> RfbCursorDecoder.readRich(input(), 1, 0, 0, 0));
        assertThrows(IOException.class, () -> RfbCursorDecoder.readX(input(), 0, 0, 513, 1));
        assertThrows(IOException.class, () -> RfbCursorDecoder.readX(input(), 2, 0, 2, 1));
    }

    @Test public void truncatedRichAndXCursorPayloadsFailClosed() throws Exception {
        assertThrows(IOException.class, () -> RfbCursorDecoder.readRich(
                input(0, 0, 255, 0), 0, 0, 2, 1));
        assertThrows(IOException.class, () -> RfbCursorDecoder.readX(
                input(255, 0, 0, 0, 0, 255, 0x80), 0, 0, 2, 1));
        assertThrows(IOException.class, () -> RfbCursorDecoder.readRich(
                input(0, 0, 255, 0), 0, 0, 1, 1));
    }
}
