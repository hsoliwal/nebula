// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;

import java.lang.reflect.Modifier;

import org.junit.Test;

/** CPU-only contract for the package-final, non-consuming pending-damage query. */
public class WebDamageStateReflective_Test {
    private static final class Probe extends CollectionModel {
        Probe() {
            super("damage-query-probe", 0, REORDER, WebStates.RECT,
                    WebStates.V_DEFAULT);
        }

        void mark(int x, int y, int w, int h) {
            damage(x, y, w, h);
        }
    }

    private static boolean pending(Probe m) {
        try {
            var method = WebLanes.class.getDeclaredMethod("hasDamage");
            method.setAccessible(true);
            return ((Boolean) method.invoke(m)).booleanValue();
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Missing or failing pending-damage query", ex);
        }
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

    private static void drainEquals(Probe m, int x, int y, int w, int h) {
        int[] out = new int[4];
        assertTrue(m.takeDamage(out));
        assertArrayEquals(new int[] { x, y, w, h }, out);
        assertFalse(pending(m));
        assertFalse(m.takeDamage(new int[4]));
    }

    @Test
    public void queryHasPackageFinalBooleanSignature() throws Exception {
        var method = WebLanes.class.getDeclaredMethod("hasDamage");
        assertEquals(boolean.class, method.getReturnType());
        assertEquals(0, method.getParameterCount());
        assertEquals(Modifier.FINAL, method.getModifiers());
    }

    @Test
    public void emptyStateRemainsEmptyAcrossQueries() {
        Probe m = fresh();
        int generation = m.generation();
        int[] state = m.snapshot();
        for (int i = 0; i < 3; i++) {
            assertFalse(pending(m));
        }
        assertEquals(generation, m.generation());
        assertArrayEquals(state, m.snapshot());
        assertEquals(0, m.takeBlit());
        assertFalse(m.takeDamage(new int[4]));
    }

    @Test
    public void positiveDamageQueryDoesNotConsumeOrNotify() {
        Probe m = fresh();
        int[] callbacks = { 0 };
        m.setDelta((item, mask) -> callbacks[0]++);
        m.mark(8, 9, 3, 4);
        int generation = m.generation();
        int[] state = m.snapshot();
        for (int i = 0; i < 3; i++) {
            assertTrue(pending(m));
        }
        assertEquals(0, callbacks[0]);
        assertEquals(generation, m.generation());
        assertArrayEquals(state, m.snapshot());
        assertEquals(0, m.takeBlit());
        drainEquals(m, 8, 9, 3, 4);
    }

    @Test
    public void queryPreservesAccumulatedUnion() {
        Probe m = fresh();
        m.mark(8, 9, 3, 4);
        assertTrue(pending(m));
        m.mark(20, 30, 5, 6);
        assertTrue(pending(m));
        drainEquals(m, 8, 9, 17, 27);
    }

    @Test
    public void zeroAreaIsPendingUnderExistingPredicate() {
        for (int[] box : new int[][] {
                { 4, 5, 0, 0 }, { 4, 5, 0, 7 }, { 4, 5, 7, 0 } }) {
            Probe m = fresh();
            m.mark(box[0], box[1], box[2], box[3]);
            assertTrue(pending(m));
            drainEquals(m, box[0], box[1], box[2], box[3]);
        }
    }

    @Test
    public void offscreenPendingIsNotMistakenForNoDamage() {
        Probe left = fresh();
        left.mark(-20, -20, 5, 5);
        assertTrue(pending(left));
        // Preserve takeDamage's existing clipped-output behavior exactly.
        drainEquals(left, 0, 0, -15, -15);
        Probe right = fresh();
        right.mark(150, 160, 5, 5);
        assertTrue(pending(right));
        drainEquals(right, 150, 160, -50, -60);
        Probe crossing = fresh();
        crossing.mark(-5, -6, 12, 14);
        assertTrue(pending(crossing));
        drainEquals(crossing, 0, 0, 7, 8);
    }

    @Test
    public void reversedRawBoundsMatchExistingEmptyPredicate() {
        for (int[] box : new int[][] {
                { 8, 9, -1, 3 }, { 8, 9, 3, -1 } }) {
            Probe m = fresh();
            m.mark(box[0], box[1], box[2], box[3]);
            assertFalse(pending(m));
            assertFalse(m.takeDamage(new int[4]));
            assertFalse(pending(m));
        }
    }

    @Test
    public void invalidOutputRetainsPendingDamageAndQueryState() {
        for (int[] bad : new int[][] {
                null, new int[0], new int[1], new int[2], new int[3] }) {
            Probe m = fresh();
            int[] callbacks = { 0 };
            m.setDelta((item, mask) -> callbacks[0]++);
            m.mark(8, 9, 3, 4);
            int generation = m.generation();
            int[] state = m.snapshot();
            RuntimeException failure = null;
            try {
                m.takeDamage(bad);
            } catch (RuntimeException ex) {
                failure = ex;
            }
            assertNotNull(failure);
            assertEquals(bad == null ? NullPointerException.class
                    : ArrayIndexOutOfBoundsException.class, failure.getClass());
            assertTrue(pending(m));
            assertTrue(pending(m));
            assertEquals(0, callbacks[0]);
            assertEquals(generation, m.generation());
            assertArrayEquals(state, m.snapshot());
            drainEquals(m, 8, 9, 3, 4);
        }
    }

    @Test
    public void invalidOutputWithNoDamageRemainsUnobserved() {
        Probe m = fresh();
        assertFalse(pending(m));
        assertFalse(m.takeDamage(null));
        for (int n = 0; n < 4; n++) {
            assertFalse(m.takeDamage(new int[n]));
            assertFalse(pending(m));
        }
    }
}
