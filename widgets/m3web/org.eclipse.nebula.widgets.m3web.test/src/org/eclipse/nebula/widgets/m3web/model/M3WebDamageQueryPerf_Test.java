// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.m3web.model;

import static org.junit.Assert.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import org.junit.Test;

/** Finite query observations; no speed threshold or comparative claim. */
public class M3WebDamageQueryPerf_Test {
    private static final int ITERATIONS = 65_536;
    private static volatile int sink;

    private static final class Probe extends M3CollectionModel {
        Probe() {
            super("damage-query-perf", 0, REORDER,
                    M3WebStates.RECT, M3WebStates.V_DEFAULT);
        }
        void mark() { damage(8, 9, 3, 4); }
    }

    private static Probe fresh() {
        Probe m = new Probe();
        m.setLogicalCount(10);
        m.setItemExtent(10);
        m.layout(100, 100);
        m.takeBlit();
        m.takeDamage(new int[4]);
        return m;
    }

    private static int sample(MethodHandle empty, MethodHandle pending) throws Throwable {
        int observed = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            if ((boolean) empty.invokeExact()) { observed++; }
            if ((boolean) pending.invokeExact()) { observed++; }
        }
        sink = observed;
        return observed;
    }

    @Test
    public void boundedQueryObservationsPreserveState() throws Throwable {
        Probe empty = fresh();
        Probe pending = fresh();
        pending.mark();
        int[] callbacks = { 0 };
        empty.setDelta((item, mask) -> callbacks[0]++);
        pending.setDelta((item, mask) -> callbacks[0]++);
        int emptyGeneration = empty.generation(), pendingGeneration = pending.generation();
        int[] emptyState = empty.snapshot(), pendingState = pending.snapshot();
        MethodHandle query = MethodHandles.lookup().findVirtual(
                M3WebLanes.class, "hasDamage", MethodType.methodType(boolean.class));
        MethodHandle emptyQuery = query.bindTo(empty), pendingQuery = query.bindTo(pending);
        assertFalse((boolean) emptyQuery.invokeExact());
        assertTrue((boolean) pendingQuery.invokeExact());
        for (int i = 0; i < 2; i++) {
            assertEquals(ITERATIONS, sample(emptyQuery, pendingQuery));
        }
        long[] sampleNs = new long[5];
        for (int i = 0; i < sampleNs.length; i++) {
            long start = System.nanoTime();
            int observed = sample(emptyQuery, pendingQuery);
            sampleNs[i] = System.nanoTime() - start;
            assertEquals(ITERATIONS, observed);
        }
        assertEquals(ITERATIONS, sink);
        assertEquals(0, callbacks[0]);
        assertEquals(emptyGeneration, empty.generation());
        assertEquals(pendingGeneration, pending.generation());
        assertArrayEquals(emptyState, empty.snapshot());
        assertArrayEquals(pendingState, pending.snapshot());
        assertFalse((boolean) emptyQuery.invokeExact());
        assertTrue((boolean) pendingQuery.invokeExact());
        assertEquals(0, empty.takeBlit());
        assertEquals(0, pending.takeBlit());
        assertFalse(empty.takeDamage(new int[4]));
        int[] damage = new int[4];
        assertTrue(pending.takeDamage(damage));
        assertArrayEquals(new int[] { 8, 9, 3, 4 }, damage);
        assertFalse((boolean) pendingQuery.invokeExact());
        System.out.println("PERF\tm3.damage-query-observation/v1"
                + "\twarmups\t2\tsamples\t5"
                + "\tqueriesPerSample\t" + (2 * ITERATIONS)
                + "\tsampleNs\t" + Arrays.toString(sampleNs)
                + "\tspeedThreshold\tNONE");
    }
}
