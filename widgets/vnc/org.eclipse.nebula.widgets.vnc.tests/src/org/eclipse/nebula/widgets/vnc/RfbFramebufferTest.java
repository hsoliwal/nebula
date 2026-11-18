/*
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.junit.Test;

public class RfbFramebufferTest {
    @Test
    public void readsLittleEndianBgrIntoArgbAndCopiesRectangles() throws IOException {
        RfbFramebuffer f = new RfbFramebuffer(2, 2);
        f.readRaw(new DataInputStream(new ByteArrayInputStream(new byte[] {
                0, 0, (byte) 255, 0,
                0, (byte) 255, 0, 0
        })), 0, 0, 2, 1);
        f.copyRect(0, 1, 2, 1, 0, 0);
        assertArrayEquals(new int[] {
                0xffff0000, 0xff00ff00, 0xffff0000, 0xff00ff00
        }, f.snapshot().pixels());
    }

    @Test
    public void copyRectOverlappingDownIsMemmoveSafe() throws IOException {
        RfbFramebuffer f = new RfbFramebuffer(1, 3);
        f.readRaw(new DataInputStream(new ByteArrayInputStream(new byte[] {
                1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0
        })), 0, 0, 1, 3);
        f.copyRect(0, 1, 1, 2, 0, 0);
        assertArrayEquals(new int[] {0xff000001, 0xff000001, 0xff000002},
                f.snapshot().pixels());
    }

    @Test
    public void resizingChangesRevisionAndBounds() {
        RfbFramebuffer f = new RfbFramebuffer(1, 1);
        long before = f.snapshot().revision();
        f.resize(4, 3);
        assertEquals(4, f.width());
        assertEquals(3, f.height());
        assertEquals(12, f.snapshot().pixels().length);
        org.junit.Assert.assertTrue(f.revision() > before);
    }

    @Test
    public void invalidRectanglesFailBeforePixelsCanBeWritten() {
        RfbFramebuffer f = new RfbFramebuffer(2, 2);
        assertThrows(IllegalArgumentException.class, () -> f.copyRect(1, 1, 2, 2, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> f.copyRect(0, 0, 1, 1, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> f.resize(8192, 8192));
        assertThrows(IllegalArgumentException.class, () -> new RfbFramebuffer(0, 2));
    }

    @Test
    public void visiblePaintTilesDoNotCopyTheEntireSourceOrExposeItsArray() {
        RfbFramebuffer f = new RfbFramebuffer(4, 2);
        f.writePixels(0, 0, 4, 2, new int[] {
                0xff010101, 0xff020202, 0xff030303, 0xff040404,
                0xff050505, 0xff060606, 0xff070707, 0xff080808
        });
        assertArrayEquals(new int[] {0xff010101, 0xff030303},
                f.snapshotScaled(0, 0, 2, 1, 0, 0, 2, 1).pixels());
        RfbFramebuffer.Snapshot tile = f.snapshot(1, 1, 2, 1);
        assertArrayEquals(new int[] {0xff060606, 0xff070707}, tile.pixels());
        tile.pixels()[0] = 0;
        assertEquals(0xff060606, f.snapshot(1, 1, 1, 1).pixels()[0]);
        assertThrows(IllegalArgumentException.class,
                () -> f.snapshotScaled(0, 0, 0, 1, 0, 0, 1, 1));
    }
}
