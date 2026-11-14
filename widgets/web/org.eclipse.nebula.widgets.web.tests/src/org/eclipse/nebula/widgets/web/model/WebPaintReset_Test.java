// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.Test;

/** Reset lifetime contract; reference inspection is bounded and never writes storage. */
public class WebPaintReset_Test {
    private static final int EMPTY_HASH = 0x811c9dc5;

    private static Object storage(WebPaint p, String name) throws Exception {
        Field field = WebPaint.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(p);
    }

    private static String[] slots(WebPaint p) throws Exception {
        String[] values = (String[]) storage(p, "texts");
        assertTrue("bounded reference oracle", values.length <= 128);
        return values;
    }

    private static void cleared(String[] values) {
        for (int i = 0; i < values.length; i++) {
            assertNull("retired text slot " + i, values[i]);
        }
    }

    private static String label(int index) {
        char[] value = new char[96];
        Arrays.fill(value, (char) ('a' + index % 26));
        value[0] = (char) ('A' + index % 26);
        value[1] = (char) (0x400 + index);
        return new String(value);
    }

    private static void text(WebPaint p, String value) {
        p.text(3, 4, 80, 20, 0x123456, 12 | WebPaint.BOLD, value);
    }

    private static void fill(WebPaint p, int count, int firstLabel) {
        for (int i = 0; i < count; i++) {
            text(p, label(firstLabel + i));
        }
    }

    private static int[] records(WebPaint p) {
        int[] result = new int[p.count() * WebPaint.STRIDE];
        for (int r = 0; r < p.count(); r++) {
            for (int f = 0; f < WebPaint.STRIDE; f++) {
                result[r * WebPaint.STRIDE + f] = p.get(r, f);
            }
        }
        return result;
    }

    private static void frame(WebPaint p, String a, String b) {
        p.translate(7, -3);
        p.rect(1, 2, 20, 30, 0x102030);
        text(p, a);
        p.poly(new int[] { 1, 2, 7, 9, 11, 3 }, 0, 3, 0x203040);
        text(p, b);
    }

    @Test
    public void emptyResetIsIdempotentAndRestoresEmptyHash() {
        WebPaint p = new WebPaint();
        for (int i = 0; i < 4; i++) {
            p.reset();
            assertEquals(0, p.count());
            assertEquals(EMPTY_HASH, p.hash());
        }
    }

    @Test
    public void nullAndEmptyTextsDoNotCreateRecordsOrReferences() throws Exception {
        WebPaint p = new WebPaint();
        text(p, null);
        text(p, "");
        assertEquals(0, p.count());
        assertEquals(EMPTY_HASH, p.hash());
        cleared(slots(p));
        p.reset();
        cleared(slots(p));
    }

    @Test
    public void liveTextReferencesAndHashesRemainStableWithinFrame() {
        WebPaint p = new WebPaint();
        String a = label(1), b = label(2);
        p.rect(0, 0, 10, 10, 0x010203);
        text(p, a);
        p.line(1, 2, 9, 7, 0x203040);
        text(p, b);
        assertEquals(4, p.count());
        assertEquals(WebPaint.TEXT, p.get(1, WebPaint.F_OP));
        assertEquals(0, p.get(1, WebPaint.F_REF));
        assertEquals(1, p.get(3, WebPaint.F_REF));
        assertSame(a, p.text(0));
        assertSame(b, p.text(1));
        int hash = p.hash();
        assertEquals(hash, p.hash());
        assertSame(a, p.text(0));
    }

    @Test
    public void rebuildingSameFramePreservesRecordsPointsAndHash() {
        WebPaint p = new WebPaint();
        String a = label(3), b = label(4);
        frame(p, a, b);
        int[] before = records(p);
        int[] points = new int[6];
        for (int i = 0; i < points.length; i++) {
            points[i] = p.point(i);
        }
        int hash = p.hash();
        p.reset();
        assertEquals(0, p.count());
        assertEquals(EMPTY_HASH, p.hash());
        frame(p, a, b);
        assertArrayEquals(before, records(p));
        for (int i = 0; i < points.length; i++) {
            assertEquals(points[i], p.point(i));
        }
        assertEquals(hash, p.hash());
        assertSame(a, p.text(0));
        assertSame(b, p.text(1));
    }

    @Test
    public void translationAndPointOffsetsRestartAtZero() {
        WebPaint p = new WebPaint();
        int[] xy = { 4, 2, 9, 6 };
        p.translate(13, -7);
        p.poly(xy, 0, 2, 0x102030);
        assertEquals(17, p.point(0));
        assertEquals(-5, p.point(1));
        p.reset();
        p.poly(xy, 0, 2, 0x102030);
        assertEquals(4, p.point(0));
        assertEquals(2, p.point(1));
        assertEquals(4, p.get(0, WebPaint.F_X));
        assertEquals(2, p.get(0, WebPaint.F_Y));
        assertEquals(0, p.get(0, WebPaint.F_REF));
        p.rect(1, 2, 3, 4, 0x405060);
        assertEquals(1, p.get(1, WebPaint.F_X));
        assertEquals(2, p.get(1, WebPaint.F_Y));
    }

    @Test
    public void primitiveBackingIdentityCapacityAndBitsArePreserved() throws Exception {
        WebPaint p = new WebPaint();
        int[] xy = new int[80];
        for (int i = 0; i < xy.length; i++) {
            xy[i] = i;
        }
        p.poly(xy, 0, 40, 0x102030);
        for (int i = 0; i < 130; i++) {
            p.rect(i, i + 1, 3, 4, i);
        }
        int[] ops = (int[]) storage(p, "ops");
        int[] points = (int[]) storage(p, "pts");
        assertTrue(ops.length <= 2048);
        assertTrue(points.length <= 128);
        int[] opsBefore = ops.clone(), pointsBefore = points.clone();
        p.reset();
        assertSame(ops, storage(p, "ops"));
        assertSame(points, storage(p, "pts"));
        assertArrayEquals(opsBefore, ops);
        assertArrayEquals(pointsBefore, points);
        assertEquals(0, p.count());
        assertEquals(EMPTY_HASH, p.hash());
    }

    @Test
    public void resetClearsEveryUsedTextSlotWithoutReplacingArray() throws Exception {
        WebPaint p = new WebPaint();
        fill(p, 37, 10);
        String[] values = slots(p);
        for (int i = 0; i < 37; i++) {
            assertNotNull(values[i]);
        }
        p.reset();
        assertSame(values, slots(p));
        cleared(values);
        assertEquals(0, p.count());
        assertEquals(EMPTY_HASH, p.hash());
    }

    @Test
    public void grownTextCapacityIsReusedWithoutRetiredStrings() throws Exception {
        WebPaint p = new WebPaint();
        fill(p, 73, 20);
        String[] values = slots(p);
        assertEquals(128, values.length);
        p.reset();
        assertSame(values, slots(p));
        cleared(values);
        String current = label(200);
        text(p, current);
        assertSame(values, slots(p));
        assertSame(current, p.text(0));
        assertEquals(128, values.length);
        p.reset();
        cleared(values);
    }

    @Test
    public void largerThenSmallerFramesCannotRetainOldTail() throws Exception {
        WebPaint p = new WebPaint();
        fill(p, 80, 30);
        p.reset();
        fill(p, 3, 300);
        p.reset();
        cleared(slots(p));
    }

    @Test
    public void equalButDistinctLabelsAllLoseStorageReferences() throws Exception {
        WebPaint p = new WebPaint();
        String a = new String(new char[] { 'a', 'b', 'c' });
        String b = new String(new char[] { 'a', 'b', 'c' });
        assertNotSame(a, b);
        assertEquals(a, b);
        text(p, a);
        text(p, b);
        String[] values = slots(p);
        assertSame(a, values[0]);
        assertSame(b, values[1]);
        p.reset();
        cleared(values);
    }

    @Test
    public void nonTextRecordCountDoesNotDefineTextCleanupRange() throws Exception {
        WebPaint p = new WebPaint();
        for (int i = 0; i < 130; i++) {
            p.rect(i, i, 2, 3, i);
        }
        fill(p, 3, 400);
        assertEquals(133, p.count());
        assertEquals(16, slots(p).length);
        p.reset();
        cleared(slots(p));
        assertEquals(EMPTY_HASH, p.hash());
    }

    @Test
    public void repeatedEmptyResetsCannotKeepPriorTextReferences() throws Exception {
        WebPaint p = new WebPaint();
        fill(p, 7, 500);
        String[] values = slots(p);
        for (int i = 0; i < 4; i++) {
            p.reset();
        }
        cleared(values);
        assertSame(values, slots(p));
    }

    @Test
    public void retiredTextSlotsReturnNullAfterReset() {
        WebPaint p = new WebPaint();
        String value = label(600);
        text(p, value);
        int reference = p.get(0, WebPaint.F_REF);
        assertSame(value, p.text(reference));
        p.reset();
        assertEquals(0, p.count());
        assertNull(p.text(reference));
    }

    @Test
    public void freshSmallFrameRetainsOnlyItsCurrentText() throws Exception {
        WebPaint p = new WebPaint();
        fill(p, 60, 700);
        p.reset();
        String current = label(800);
        text(p, current);
        String[] values = slots(p);
        assertSame(current, values[0]);
        for (int i = 1; i < values.length; i++) {
            assertNull("not in the current frame: " + i, values[i]);
        }
        assertEquals(1, p.count());
        assertSame(current, p.text(0));
        p.reset();
        cleared(values);
    }

    @Test
    public void resetAfterExistingPointFailureReleasesPublishedTexts() throws Exception {
        WebPaint p = new WebPaint();
        String current = label(900);
        text(p, current);
        try {
            p.poly(new int[] { 1, 2 }, 0, 2, 0x123456);
            fail("expected existing point-range failure");
        } catch (IndexOutOfBoundsException expected) {
            assertEquals(1, p.count());
            assertSame(current, p.text(0));
        }
        p.reset();
        assertEquals(0, p.count());
        assertEquals(EMPTY_HASH, p.hash());
        cleared(slots(p));
    }
}
