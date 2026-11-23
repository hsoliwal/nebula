/*
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

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

    @Test
    public void firstFrameIsCompleteButOnePixelUpdateDoesNotCopyTheScreen() {
        RfbFramebuffer f = new RfbFramebuffer(2048, 1024);
        RfbFramebuffer.FrameUpdate first = f.snapshotSince(-1);
        assertTrue(first.complete());
        assertEquals(2048 * 1024, first.pixels().length);

        f.writePixels(173, 810, 1, 1, new int[] {0xff010203});
        RfbFramebuffer.FrameUpdate change = f.snapshotSince(first.revision());
        assertFalse(change.complete());
        assertEquals(173, change.x());
        assertEquals(810, change.y());
        assertEquals(1, change.changedWidth());
        assertEquals(1, change.changedHeight());
        assertArrayEquals(new int[] {0xff010203}, change.pixels());
        assertEquals(0, f.snapshotSince(change.revision()).pixels().length);
    }

    @Test
    public void disjointTilesCoalesceIntoOneBoundedPixelCopy() {
        RfbFramebuffer f = new RfbFramebuffer(8, 6);
        long baseline = f.snapshotSince(-1).revision();
        f.writePixels(1, 1, 2, 2, new int[] {1, 2, 3, 4});
        f.writePixels(4, 3, 2, 1, new int[] {5, 6});
        RfbFramebuffer.FrameUpdate change = f.snapshotSince(baseline);
        assertEquals(1, change.x());
        assertEquals(1, change.y());
        assertEquals(5, change.changedWidth());
        assertEquals(3, change.changedHeight());
        assertArrayEquals(new int[] {
                1, 2, 0xff000000, 0xff000000, 0xff000000,
                3, 4, 0xff000000, 0xff000000, 0xff000000,
                0xff000000, 0xff000000, 0xff000000, 5, 6
        }, change.pixels());
    }

    @Test
    public void missedDamageIntervalAndResizingAlwaysReturnCompleteFrames() {
        RfbFramebuffer f = new RfbFramebuffer(4, 3);
        long first = f.snapshotSince(-1).revision();
        f.writePixels(0, 0, 1, 1, new int[] {1});
        long delivered = f.snapshotSince(first).revision();
        f.writePixels(3, 2, 1, 1, new int[] {2});
        RfbFramebuffer.FrameUpdate missed = f.snapshotSince(first);
        assertTrue(missed.complete());
        assertEquals(12, missed.pixels().length);
        assertEquals(1, missed.pixels()[0]);
        assertEquals(2, missed.pixels()[11]);
        assertTrue(missed.revision() > delivered);

        f.resize(2, 2);
        RfbFramebuffer.FrameUpdate resized = f.snapshotSince(missed.revision());
        assertTrue(resized.complete());
        assertEquals(2, resized.width());
        assertEquals(2, resized.height());
        assertArrayEquals(new int[] {
                0xff000000, 0xff000000, 0xff000000, 0xff000000
        }, resized.pixels());
    }

    @Test
    public void copyRectDamagesOnlyDestinationAndPublishedPixelsStayDetached() {
        RfbFramebuffer f = new RfbFramebuffer(3, 3);
        f.writePixels(0, 0, 3, 1, new int[] {1, 2, 3});
        long baseline = f.snapshotSince(-1).revision();
        f.copyRect(0, 1, 3, 1, 0, 0);
        RfbFramebuffer.FrameUpdate copied = f.snapshotSince(baseline);
        assertEquals(0, copied.x());
        assertEquals(1, copied.y());
        assertEquals(3, copied.changedWidth());
        assertEquals(1, copied.changedHeight());
        assertArrayEquals(new int[] {1, 2, 3}, copied.pixels());
        f.writePixels(0, 1, 3, 1, new int[] {4, 5, 6});
        assertArrayEquals(new int[] {1, 2, 3}, copied.pixels());
    }

    @Test
    public void batchedRawAndOverlappingCopiesReconstructTheExactFullFrame() {
        final int width = 31, height = 23;
        RfbFramebuffer f = new RfbFramebuffer(width, height);
        RfbFramebuffer.FrameUpdate first = f.snapshotSince(-1);
        int[] expected = first.pixels().clone();
        int[] drawn = first.pixels().clone();
        long seen = first.revision();
        Random random = new Random(0x5246422026L);

        for (int batch = 0; batch < 400; batch++) {
            for (int change = 0, count = 1 + random.nextInt(8); change < count; change++) {
                int w = 1 + random.nextInt(9), h = 1 + random.nextInt(7);
                int x = random.nextInt(width - w + 1), y = random.nextInt(height - h + 1);
                if (random.nextBoolean()) {
                    int[] patch = new int[w * h];
                    for (int i = 0; i < patch.length; i++) patch[i] = random.nextInt();
                    f.writePixels(x, y, w, h, patch);
                    for (int row = 0; row < h; row++) {
                        System.arraycopy(patch, row * w, expected, (y + row) * width + x, w);
                    }
                } else {
                    int sx = random.nextInt(width - w + 1), sy = random.nextInt(height - h + 1);
                    int[] copied = new int[w * h];
                    for (int row = 0; row < h; row++) {
                        System.arraycopy(expected, (sy + row) * width + sx, copied, row * w, w);
                    }
                    for (int row = 0; row < h; row++) {
                        System.arraycopy(copied, row * w, expected, (y + row) * width + x, w);
                    }
                    f.copyRect(x, y, w, h, sx, sy);
                }
            }
            RfbFramebuffer.Damage hint = f.damageSince(seen);
            assertTrue(hint.width() > 0 && hint.height() > 0);
            assertEquals(hint, f.damageSince(seen)); // a UI redraw hint must not consume damage
            RfbFramebuffer.FrameUpdate update = f.snapshotSince(seen);
            assertEquals(hint.x(), update.x());
            assertEquals(hint.y(), update.y());
            assertEquals(hint.width(), update.changedWidth());
            assertEquals(hint.height(), update.changedHeight());
            for (int row = 0; row < update.changedHeight(); row++) {
                System.arraycopy(update.pixels(), row * update.changedWidth(), drawn,
                        (update.y() + row) * width + update.x(), update.changedWidth());
            }
            assertArrayEquals("batch " + batch, expected, drawn);
            seen = update.revision();
        }
    }
}
