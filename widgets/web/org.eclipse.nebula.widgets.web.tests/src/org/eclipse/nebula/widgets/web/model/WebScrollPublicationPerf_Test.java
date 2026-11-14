// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

/** Finite public-protocol observations; includes layout/drain/assertion overhead. */
public class WebScrollPublicationPerf_Test {
    private static final int ITERATIONS = 512;
    private static volatile int sink;

    private static final class Probe extends CollectionModel {
        int arrangements;

        Probe() {
            super("scroll-publication-perf", 0, REORDER,
                    WebStates.RECT, WebStates.V_DEFAULT);
        }

        @Override
        protected void arrange() {
            arrangements++;
            super.arrange();
        }
    }

    private static void strip(Probe p, int blit, int y, int h, int[] d) {
        assertEquals(blit, p.takeBlit());
        assertTrue(p.takeDamage(d));
        assertEquals(0, d[0]);
        assertEquals(y, d[1]);
        assertEquals(120, d[2]);
        assertEquals(h, d[3]);
    }

    private static int sample(Probe p, int[] d) {
        int fullFallbacks = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            p.scrollTo(10);
            strip(p, -10, 90, 10, d);
            p.scrollTo(0);
            strip(p, 10, 0, 10, d);
            p.scrollTo(10);
            p.scrollTo(20);
            assertEquals(0, p.takeBlit());
            assertTrue(p.takeDamage(d));
            assertEquals(0, d[0]);
            assertEquals(0, d[1]);
            assertEquals(120, d[2]);
            assertEquals(100, d[3]);
            fullFallbacks++;
            p.scrollTo(0);
            strip(p, 20, 0, 20, d);
        }
        sink = fullFallbacks;
        return fullFallbacks;
    }

    @Test
    public void boundedCleanAndQueuedPublicationsHaveFiniteObservations() {
        Probe p = new Probe();
        p.setLogicalCount(40);
        p.setItemExtent(10);
        p.layout(120, 100);
        p.takeBlit();
        p.takeDamage(new int[4]);
        int[] callbacks = { 0 };
        p.setDelta((item, mask) -> callbacks[0]++);
        int generation = p.generation();
        int arrangements = p.arrangements;
        int[] state = p.snapshot();
        int[] damage = new int[4];

        for (int i = 0; i < 2; i++) {
            assertEquals(ITERATIONS, sample(p, damage));
        }
        long[] sampleNs = new long[5];
        for (int i = 0; i < sampleNs.length; i++) {
            long start = System.nanoTime();
            int fullFallbacks = sample(p, damage);
            sampleNs[i] = System.nanoTime() - start;
            assertEquals(ITERATIONS, fullFallbacks);
        }

        int calls = 5 * ITERATIONS * 7;
        assertEquals(ITERATIONS, sink);
        assertEquals(generation + calls, p.generation());
        assertEquals(arrangements + calls, p.arrangements);
        assertEquals(calls, callbacks[0]);
        assertEquals(0, p.scrollOffset());
        assertEquals(0, p.takeBlit());
        assertFalse(p.takeDamage(damage));
        assertArrayEquals(state, p.snapshot());
        System.out.println("PERF\tm3.scroll-publication-observation/v1"
                + "\twarmups\t2\tsamples\t5"
                + "\tcyclesPerSample\t" + ITERATIONS
                + "\tscrollCallsPerSample\t" + (5 * ITERATIONS)
                + "\tsampleNs\t" + Arrays.toString(sampleNs)
                + "\tspeedThreshold\tNONE");
    }
}
