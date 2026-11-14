// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.Test;

/** Finite reset observations; reference counts are not GC or heap-liveness evidence. */
public class WebPaintResetPerf_Test {
    private static final int ITERATIONS = 1024, LABELS = 32;
    private static volatile long sink;

    private static Object storage(WebPaint p, String name) throws Exception {
        Field field = WebPaint.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(p);
    }

    private static void populate(WebPaint p, String[] labels) {
        for (int i = 0; i < labels.length; i++) {
            p.text(i, 2, 30, 20, 0x123456, 12, labels[i]);
        }
    }

    private static void cleared(String[] values) {
        for (String value : values) {
            assertNull(value);
        }
    }

    private static long sample(WebPaint p, String[] labels, String[] values) {
        long resetNs = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            populate(p, labels);
            assertEquals(LABELS, p.count());
            long start = System.nanoTime();
            p.reset();
            resetNs += System.nanoTime() - start;
            // Outside the timed interval: inspect only the retained bounded table.
            cleared(values);
            assertEquals(0, p.count());
            assertEquals(0x811c9dc5, p.hash());
        }
        sink = resetNs;
        return resetNs;
    }

    @Test
    public void boundedPopulatedResetsReleaseSlotsWithFiniteObservations() throws Exception {
        WebPaint p = new WebPaint();
        String[] labels = new String[LABELS];
        for (int i = 0; i < labels.length; i++) {
            char[] chars = new char[64];
            Arrays.fill(chars, (char) ('a' + i % 26));
            chars[0] = (char) (0x400 + i);
            labels[i] = new String(chars);
        }
        populate(p, labels);
        Object ops = storage(p, "ops"), points = storage(p, "pts");
        String[] values = (String[]) storage(p, "texts");
        assertEquals(LABELS, values.length);
        p.reset();
        cleared(values);

        for (int i = 0; i < 2; i++) {
            sample(p, labels, values);
        }
        long[] sampleNs = new long[5];
        for (int i = 0; i < sampleNs.length; i++) {
            sampleNs[i] = sample(p, labels, values);
        }

        assertEquals(sampleNs[4], sink);
        assertSame(values, storage(p, "texts"));
        assertSame(ops, storage(p, "ops"));
        assertSame(points, storage(p, "pts"));
        cleared(values);
        assertEquals(0, p.count());
        assertEquals(0x811c9dc5, p.hash());
        System.out.println("PERF\tm3.paint-reset-observation/v1"
                + "\tprimingResets\t1\twarmups\t2\tsamples\t5"
                + "\tpopulatedResetsPerSample\t" + ITERATIONS
                + "\tusedTextSlotsPerReset\t" + LABELS
                + "\tsampleNs\t" + Arrays.toString(sampleNs)
                + "\ttiming\treset-plus-clock-overhead"
                + "\tspeedThreshold\tNONE\tgcLivenessClaim\tfalse");
    }
}
