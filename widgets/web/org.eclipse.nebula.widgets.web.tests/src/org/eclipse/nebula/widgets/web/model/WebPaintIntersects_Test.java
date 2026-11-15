// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Test;

/**
 * Stored-box intersection maintenance contract. Edges remain inclusive; dimensions and
 * record ordinals are not normalized. This fixture makes no native-pixel or SCREEN claim.
 */
public class WebPaintIntersects_Test {
    private static final int MIN = Integer.MIN_VALUE;
    private static final int MAX = Integer.MAX_VALUE;
    private static volatile long queryObservationSink;

    private static BigInteger integer(int value) {
        return BigInteger.valueOf(value);
    }

    private static boolean exact(int[] box, int[] clip) {
        return integer(box[0]).compareTo(integer(clip[0]).add(integer(clip[2]))) <= 0
                && integer(box[0]).add(integer(box[2])).compareTo(integer(clip[0])) >= 0
                && integer(box[1]).compareTo(integer(clip[1]).add(integer(clip[3]))) <= 0
                && integer(box[1]).add(integer(box[3])).compareTo(integer(clip[1])) >= 0;
    }

    private static boolean legacy(int[] box, int[] clip) {
        return box[0] <= clip[0] + clip[2] && box[0] + box[2] >= clip[0]
                && box[1] <= clip[1] + clip[3] && box[1] + box[3] >= clip[1];
    }

    private static WebPaint paint(int[] box) {
        WebPaint p = new WebPaint();
        p.rect(box[0], box[1], box[2], box[3], 0x123456);
        return p;
    }

    private static boolean query(WebPaint p, int record, int[] clip) {
        return p.intersects(record, clip[0], clip[1], clip[2], clip[3]);
    }

    private static void check(int[] box, int[] clip, boolean expected) {
        assertEquals("independent exact oracle", expected, exact(box, clip));
        assertEquals("record " + Arrays.toString(box) + " clip " + Arrays.toString(clip),
                expected, query(paint(box), 0, clip));
    }

    private static Field field(String name) throws Exception {
        Field field = WebPaint.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object storage(WebPaint p, String name) throws Exception {
        return field(name).get(p);
    }

    private static int[] records(WebPaint p) {
        int[] values = new int[p.count() * WebPaint.STRIDE];
        for (int i = 0; i < p.count(); i++) {
            for (int f = 0; f < WebPaint.STRIDE; f++) {
                values[i * WebPaint.STRIDE + f] = p.get(i, f);
            }
        }
        return values;
    }

    private static int[] counters(WebPaint p) throws Exception {
        return new int[] { field("n").getInt(p), field("np").getInt(p),
                field("nt").getInt(p), field("tx").getInt(p), field("ty").getInt(p) };
    }

    private static void failsExactly(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            assertSame("natural failure class", expected, failure.getClass());
            return;
        }
        fail("Expected " + expected.getSimpleName());
    }

    @Test
    public void recordXPositiveOverflow() {
        check(new int[] { MAX - 5, 0, 10, 10 }, new int[] { MAX - 4, 1, 1, 1 }, true);
    }

    @Test
    public void clipXPositiveOverflow() {
        check(new int[] { MAX - 4, 1, 1, 1 }, new int[] { MAX - 5, 0, 10, 10 }, true);
    }

    @Test
    public void recordYPositiveOverflow() {
        check(new int[] { 0, MAX - 5, 10, 10 }, new int[] { 1, MAX - 4, 1, 1 }, true);
    }

    @Test
    public void clipYPositiveOverflow() {
        check(new int[] { 1, MAX - 4, 1, 1 }, new int[] { 0, MAX - 5, 10, 10 }, true);
    }

    @Test
    public void recordXNegativeUnderflow() {
        check(new int[] { MIN + 1, 0, -2, 1 }, new int[] { MIN + 1, 0, 0, 1 }, false);
    }

    @Test
    public void clipXNegativeUnderflow() {
        check(new int[] { 0, 0, 0, 1 }, new int[] { MIN + 1, 0, -2, 1 }, false);
    }

    @Test
    public void recordYNegativeUnderflow() {
        check(new int[] { 0, MIN + 1, 1, -2 }, new int[] { 0, MIN + 1, 1, 0 }, false);
    }

    @Test
    public void clipYNegativeUnderflow() {
        check(new int[] { 0, 0, 1, 0 }, new int[] { 0, MIN + 1, 1, -2 }, false);
    }

    @Test
    public void endpointGridMatchesExactOracle() {
        int[] origins = { MIN, MIN + 1, -1, 0, 1, MAX - 1, MAX };
        int[] extents = { MIN, -2, -1, 0, 1, 2, MAX };
        WebPaint p = new WebPaint();
        int queries = 0, legacyDifferences = 0, actualDifferences = 0;
        String firstDifference = null;
        for (int axis = 0; axis < 2; axis++) {
            for (int origin : origins) {
                for (int extent : extents) {
                    int[] box = axis == 0 ? new int[] { origin, 0, extent, 1 }
                            : new int[] { 0, origin, 1, extent };
                    p.reset();
                    p.rect(box[0], box[1], box[2], box[3], 0x123456);
                    for (int clipOrigin : origins) {
                        for (int clipExtent : extents) {
                            int[] clip = axis == 0 ? new int[] { clipOrigin, 0, clipExtent, 1 }
                                    : new int[] { 0, clipOrigin, 1, clipExtent };
                            boolean expected = exact(box, clip);
                            if (legacy(box, clip) != expected) {
                                legacyDifferences++;
                            }
                            if (query(p, 0, clip) != expected) {
                                actualDifferences++;
                                if (firstDifference == null) {
                                    firstDifference = Arrays.toString(box) + " / " + Arrays.toString(clip);
                                }
                            }
                            queries++;
                        }
                    }
                }
            }
        }
        assertEquals("finite endpoint grid", 4802, queries);
        assertEquals("frozen int-wrap counterexamples", 1036, legacyDifferences);
        assertEquals("actual query differs from exact oracle; first " + firstDifference,
                0, actualDifferences);
    }

    @Test
    public void ordinaryEdgesZerosAndSignedExtentsUnchanged() {
        int[][] boxes = {
                { 0, 0, 1, 1 }, { 0, 0, 1, 1 }, { 0, 0, 0, 0 },
                { 0, 0, -1, 1 }, { -5, -5, 10, 10 }, { -5, -5, 10, 10 },
                { 0, 0, 5, -2 }, { 0, 0, 2, 2 }
        };
        int[][] clips = {
                { 1, 1, 1, 1 }, { 2, 2, 0, 0 }, { 0, 0, 0, 0 },
                { -1, 0, 1, 1 }, { 1, 1, -1, -1 }, { -10, -10, 1, 1 },
                { 0, -2, 5, 2 }, { 0, 0, -1, -1 }
        };
        boolean[] expected = { true, false, true, true, true, false, true, false };
        for (int i = 0; i < boxes.length; i++) {
            assertEquals("ordinary legacy equation " + i, expected[i], legacy(boxes[i], clips[i]));
            check(boxes[i], clips[i], expected[i]);
        }
    }

    @Test
    public void translationAndPackedStateRemainUnchanged() throws Exception {
        WebPaint p = new WebPaint();
        String label = new String(new char[] { 'm', '3' });
        p.translate(MAX, MIN);
        p.rect(10, -10, 2, 3, 0x010203);
        p.poly(new int[] { 10, -10, 11, -9 }, 0, 2, 0x040506);
        p.text(10, -10, 2, 3, 0x070809, 12, label);
        assertEquals(MIN + 9, p.get(0, WebPaint.F_X));
        assertEquals(MAX - 9, p.get(0, WebPaint.F_Y));
        assertEquals(MIN + 9, p.point(0));
        assertEquals(MAX - 9, p.point(1));
        assertEquals(MIN + 10, p.point(2));
        assertEquals(MAX - 8, p.point(3));
        int[] before = records(p), state = counters(p);
        int[] points = { p.point(0), p.point(1), p.point(2), p.point(3) };
        int hash = p.hash();
        for (int repeat = 0; repeat < 16; repeat++) {
            for (int record = 0; record < p.count(); record++) {
                assertTrue(p.intersects(record, MIN + 9, MAX - 9, 0, 0));
            }
        }
        assertArrayEquals(before, records(p));
        assertArrayEquals(state, counters(p));
        for (int i = 0; i < points.length; i++) {
            assertEquals(points[i], p.point(i));
        }
        assertEquals(hash, p.hash());
        assertSame(label, p.text(0));
    }

    @Test
    public void rawCapacityAndIndexWrapRemainUnchanged() throws Exception {
        WebPaint p = new WebPaint();
        assertEquals(0, p.count());
        assertEquals(64 * WebPaint.STRIDE, ((int[]) storage(p, "ops")).length);
        assertTrue("unused first raw slot is readable", p.intersects(0, 0, 0, 0, 0));
        assertTrue("unused last raw slot is readable", p.intersects(63, 0, 0, 0, 0));
        p.rect(3, 4, 5, 6, 0x010203);
        boolean first = p.intersects(0, 4, 5, 1, 1);
        assertTrue(first);
        assertEquals(first, p.intersects(536_870_912, 4, 5, 1, 1));
        assertEquals(first, p.intersects(MIN, 4, 5, 1, 1));
        assertTrue("no live-count guard", p.intersects(1, 0, 0, 0, 0));
        for (int i = 1; i <= 64; i++) {
            p.rect(i, i, 1, 1, i);
        }
        int[] backing = (int[]) storage(p, "ops");
        assertTrue(backing.length > 64 * WebPaint.STRIDE);
        p.reset();
        assertEquals(0, p.count());
        assertSame(backing, storage(p, "ops"));
        assertTrue("retired raw slot remains readable after reset", p.intersects(64, 64, 64, 0, 0));
    }

    @Test
    public void naturalArrayFailureOrderPreserved() throws Exception {
        WebPaint ordinary = new WebPaint();
        failsExactly(ArrayIndexOutOfBoundsException.class,
                () -> ordinary.intersects(-1, 0, 0, 0, 0));
        failsExactly(ArrayIndexOutOfBoundsException.class,
                () -> ordinary.intersects(64, 0, 0, 0, 0));
        failsExactly(ArrayIndexOutOfBoundsException.class,
                () -> ordinary.intersects(MAX, 0, 0, 0, 0));

        // Bounded adversarial storage oracle: expose which existing array read occurs next.
        WebPaint shortStorage = new WebPaint();
        int[] onlyX = new int[2];
        onlyX[WebPaint.F_X] = 5;
        field("ops").set(shortStorage, onlyX);
        assertFalse("first comparison short-circuits before width", shortStorage.intersects(0, 0, 0, 0, 0));
        failsExactly(ArrayIndexOutOfBoundsException.class,
                () -> shortStorage.intersects(0, 5, 0, 0, 0));

        int[] noHeight = new int[4];
        noHeight[WebPaint.F_X] = 5;
        noHeight[WebPaint.F_Y] = 5;
        noHeight[WebPaint.F_W] = -10;
        field("ops").set(shortStorage, noHeight);
        assertFalse("second comparison short-circuits", shortStorage.intersects(0, 0, 0, 10, 0));
        noHeight[WebPaint.F_W] = 1;
        assertFalse("third comparison short-circuits before height", shortStorage.intersects(0, 0, 0, 10, 0));
        failsExactly(ArrayIndexOutOfBoundsException.class,
                () -> shortStorage.intersects(0, 0, 5, 10, 0));
        field("ops").set(shortStorage, null);
        failsExactly(NullPointerException.class,
                () -> shortStorage.intersects(0, 0, 0, 0, 0));
    }

    @Test
    public void queriesDoNotMutateReusedStorage() throws Exception {
        WebPaint p = new WebPaint();
        for (int i = 0; i < 65; i++) {
            p.rect(i, i + 1, 2, 3, i);
        }
        for (int i = 0; i < 17; i++) {
            p.text(i, i, 20, 10, i, 12, "label-" + i);
        }
        p.poly(new int[] { 1, 2, 7, 9, 11, 3 }, 0, 3, 0x203040);
        int[] ops = (int[]) storage(p, "ops"), points = (int[]) storage(p, "pts");
        String[] texts = (String[]) storage(p, "texts");
        int[] savedOps = ops.clone(), savedPoints = points.clone(), state = counters(p);
        String[] savedTexts = texts.clone();
        int hash = p.hash();
        for (int repeat = 0; repeat < 64; repeat++) {
            for (int record = 0; record < p.count(); record++) {
                p.intersects(record, repeat - 20, repeat - 10, 5, 7);
            }
        }
        assertSame(ops, storage(p, "ops"));
        assertSame(points, storage(p, "pts"));
        assertSame(texts, storage(p, "texts"));
        assertArrayEquals(savedOps, ops);
        assertArrayEquals(savedPoints, points);
        assertArrayEquals(state, counters(p));
        for (int i = 0; i < savedTexts.length; i++) {
            assertSame("text identity " + i, savedTexts[i], texts[i]);
        }
        assertEquals(hash, p.hash());
    }

    private static long observe(WebPaint p, int iterations) {
        long matches = 0;
        for (int i = 0; i < iterations; i++) {
            if (p.intersects(0, (i & 1) == 0 ? 21 : 200, 31, 7, 9)) {
                matches++;
            }
        }
        queryObservationSink = matches;
        return matches;
    }

    @Test
    public void finiteQueryObservationHasNoSpeedThreshold() throws Exception {
        WebPaint p = paint(new int[] { 20, 30, 40, 50 });
        int[] before = records(p), state = counters(p);
        Object ops = storage(p, "ops");
        int hash = p.hash();
        final int iterations = 8192;
        for (int warm = 0; warm < 2; warm++) {
            assertEquals(iterations / 2L, observe(p, iterations));
        }
        long[] nanos = new long[5];
        for (int sample = 0; sample < nanos.length; sample++) {
            long start = System.nanoTime();
            assertEquals(iterations / 2L, observe(p, iterations));
            nanos[sample] = System.nanoTime() - start;
        }
        assertEquals(iterations / 2L, queryObservationSink);
        assertArrayEquals(before, records(p));
        assertArrayEquals(state, counters(p));
        assertSame(ops, storage(p, "ops"));
        assertEquals(hash, p.hash());
        System.out.println("M3_PAINT_INTERSECTS_OBSERVATION\titerations=" + iterations
                + "\twarmups=2\tsamples=5\tnanos=" + Arrays.toString(nanos));
    }
}
