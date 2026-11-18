// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Immutable viewport clips augment the existing display-list records. */
public class WebPaintClip_Test {
    private static int[] clip(WebPaint paint, int record) {
        return new int[] {paint.clipRect(record, 0), paint.clipRect(record, 1),
                paint.clipRect(record, 2), paint.clipRect(record, 3)};
    }

    @Test
    public void clipCapturesTranslationOnceAndDoesNotRewriteEarlierRecords() {
        WebPaint paint = new WebPaint();
        paint.rect(0, 0, 8, 8, 0x123456);
        paint.translate(10, 20);
        paint.clip(1, 2, 30, 40);
        paint.rect(3, 4, 5, 6, 0xabcdef);
        paint.translate(100, 200);
        paint.rect(3, 4, 5, 6, 0xabcdef);
        paint.clip(7, 8, 9, 10);
        paint.rect(0, 0, 4, 4, 0);
        paint.clearClip();
        paint.rect(0, 0, 4, 4, 0);
        assertFalse(paint.hasClip(0));
        assertArrayEquals(new int[] {11, 22, 30, 40}, clip(paint, 1));
        assertArrayEquals(new int[] {11, 22, 30, 40}, clip(paint, 2));
        assertArrayEquals(new int[] {107, 208, 9, 10}, clip(paint, 3));
        assertFalse(paint.hasClip(4));
        assertEquals(13, paint.get(1, WebPaint.F_X));
        assertEquals(24, paint.get(1, WebPaint.F_Y));
        assertEquals(103, paint.get(2, WebPaint.F_X));
        assertEquals(204, paint.get(2, WebPaint.F_Y));
    }

    @Test
    public void emptyClipsAreRecordedButNeverAdmittedForReplay() {
        for (int[] size : new int[][] {{0, 10}, {10, 0}, {-1, 10}, {10, -1}, {-9, -7}}) {
            WebPaint paint = new WebPaint();
            paint.clip(0, 0, size[0], size[1]);
            paint.rect(-10, -10, 100, 100, 0x123456);
            assertTrue(paint.hasClip(0));
            assertEquals(Math.max(0, size[0]), paint.clipRect(0, 2));
            assertEquals(Math.max(0, size[1]), paint.clipRect(0, 3));
            assertFalse(paint.intersects(0, -100, -100, 200, 200));
        }
    }

    @Test
    public void replayAdmissionIntersectsRecordDamageAndCapturedClip() {
        WebPaint paint = new WebPaint();
        paint.clip(20, 30, 40, 50);
        paint.rect(0, 0, 100, 100, 0);
        assertTrue(paint.intersects(0, 30, 40, 10, 10));
        assertFalse(paint.intersects(0, 0, 0, 10, 10));
        assertFalse(paint.intersects(0, 60, 30, 10, 10));
        assertFalse(paint.intersects(0, 20, 80, 10, 10));
        assertTrue(paint.intersects(0, 59, 79, 2, 2));
        paint.rect(0, 0, 5, 5, 0);
        assertFalse(paint.intersects(1, 0, 0, 100, 100));
        // A zero-height line touching the lower clip edge stays conservatively admitted.
        paint.line(10, 30, 20, 30, 0);
        assertTrue(paint.intersects(2, 19, 29, 2, 2));
    }

    @Test
    public void clipBoundsUseWidenedRightAndBottomEdges() {
        WebPaint paint = new WebPaint();
        paint.clip(Integer.MAX_VALUE - 10, Integer.MAX_VALUE - 12, 100, 100);
        paint.rect(Integer.MAX_VALUE - 8, Integer.MAX_VALUE - 9, 100, 100, 0);
        assertTrue(paint.intersects(0, Integer.MAX_VALUE - 3, Integer.MAX_VALUE - 4, 50, 50));
        assertFalse(paint.intersects(0, Integer.MIN_VALUE, Integer.MIN_VALUE, 50, 50));
    }

    @Test
    public void growingPrimitivePoolsPreservesEveryCapturedRectangle() {
        WebPaint paint = new WebPaint();
        for (int i = 0; i < 513; i++) {
            paint.clip(i, -i, 30 + i, 50 + i);
            paint.rect(-100, -100, 1000, 1000, i);
            paint.clearClip();
            paint.line(0, 0, i, i, i);
        }
        assertEquals(1026, paint.count());
        for (int i = 0; i < 513; i++) {
            assertArrayEquals(new int[] {i, -i, 30 + i, 50 + i}, clip(paint, 2 * i));
            assertFalse(paint.hasClip(2 * i + 1));
        }
    }

    @Test
    public void resetAndClearClipKeepTheOriginalUnclippedRecordHash() {
        WebPaint original = new WebPaint(), reused = new WebPaint();
        original.rect(1, 2, 3, 4, 0x123456);
        int expected = 0x811c9dc5;
        for (int field : new int[] {WebPaint.RECT, 1, 2, 3, 4, 0x123456, 0, 0}) {
            expected = (expected ^ field) * 0x01000193;
        }
        assertEquals(expected, original.hash());
        reused.clip(7, 8, 9, 10);
        for (int i = 0; i < 100; i++) reused.rect(i, i, 3, 4, 0);
        reused.reset();
        reused.rect(1, 2, 3, 4, 0x123456);
        assertFalse(reused.hasClip(0));
        assertArrayEquals(new int[4], clip(reused, 0));
        assertEquals(expected, reused.hash());
        reused.reset();
        reused.clip(3, 4, 5, 6);
        reused.clearClip();
        reused.rect(1, 2, 3, 4, 0x123456);
        assertEquals(expected, reused.hash());
    }

    @Test
    public void hashIncludesCapturedClipValuesWithoutPoolAllocationHistory() {
        WebPaint a = new WebPaint(), b = new WebPaint(), c = new WebPaint();
        a.clip(1, 2, 3, 4);
        b.clip(91, 92, 93, 94);
        b.clip(1, 2, 3, 4);
        c.clip(1, 2, 30, 40);
        a.rect(0, 0, 100, 100, 7);
        b.rect(0, 0, 100, 100, 7);
        c.rect(0, 0, 100, 100, 7);
        assertEquals(a.hash(), b.hash());
        assertNotEquals(a.hash(), c.hash());
    }

    @Test
    public void reusedUnclippedRecordsOverwriteEveryOldClipReference() {
        WebPaint paint = new WebPaint();
        paint.clip(0, 0, 0, 0);
        for (int i = 0; i < 257; i++) paint.rect(i, i, 10, 10, i);
        paint.reset();
        for (int i = 0; i < 257; i++) paint.rect(i, i, 10, 10, i);
        for (int i = 0; i < 257; i++) {
            assertFalse(paint.hasClip(i));
            assertTrue(paint.intersects(i, i, i, 10, 10));
        }
    }
}
