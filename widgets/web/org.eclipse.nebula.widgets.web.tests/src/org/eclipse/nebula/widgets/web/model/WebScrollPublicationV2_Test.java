// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** Public publication protocol, existing hook failures, and scoped reentry. */
public class WebScrollPublicationV2_Test {
    private enum Fault {
        NONE, LENGTH_BEFORE, ARRANGE, LENGTH_AFTER, ASPECTS, ORIGIN, AXIS,
        DAMAGE, DAMAGE_ALL, FIRE_BEFORE, FIRE_AFTER
    }

    private static final class Probe extends CollectionModel {
        final List<String> trace = new ArrayList<>();
        final RuntimeException failure = new IllegalStateException("fixture hook failure");
        final boolean horizontal;
        final int origin;
        Fault fault = Fault.NONE;
        boolean inArrange, arrangedSinceArm, markDuringArrange;
        int arrangements, callbacks, advertisedAspects = A_R | A_G | A_D | A_P;
        Runnable duringArrange, duringExtent;

        Probe(boolean horizontal, int origin) {
            super("scroll-publication-probe", 0, REORDER,
                    WebStates.RECT, WebStates.V_DEFAULT);
            this.horizontal = horizontal;
            this.origin = origin;
        }

        void arm(Fault value) {
            fault = value;
            arrangedSinceArm = false;
        }

        void record(String value) {
            if (!inArrange) {
                trace.add(value);
            }
        }

        int axisLength() {
            return Math.max(0, (horizontal ? width : height) - origin);
        }

        void observe() {
            setDelta((item, mask) -> {
                trace.add("delta");
                callbacks++;
                assertEquals(-1, item);
                assertEquals(D_SCROLL, mask);
            });
        }

        @Override
        public long logicalExtent() {
            record("extent");
            Runnable action = duringExtent;
            duringExtent = null;
            if (action != null) {
                action.run();
            }
            return super.logicalExtent();
        }

        @Override
        public int viewLength() {
            record("length");
            if (!inArrange && ((fault == Fault.LENGTH_BEFORE && !arrangedSinceArm)
                    || (fault == Fault.LENGTH_AFTER && arrangedSinceArm))) {
                throw failure;
            }
            return axisLength();
        }

        @Override
        public int aspects() {
            record("aspects");
            if (fault == Fault.ASPECTS) {
                throw failure;
            }
            return advertisedAspects;
        }

        @Override
        public int viewOrigin() {
            record("origin");
            if (fault == Fault.ORIGIN) {
                throw failure;
            }
            return origin;
        }

        @Override
        public boolean scrollsHorizontally() {
            record("axis");
            if (fault == Fault.AXIS) {
                throw failure;
            }
            return horizontal;
        }

        @Override
        protected void arrange() {
            boolean previous = inArrange;
            inArrange = true;
            arrangements++;
            trace.add("arrange.enter");
            try {
                if (fault == Fault.ARRANGE) {
                    throw failure;
                }
                super.arrange();
                if (markDuringArrange) {
                    damage(3, 7, 9, 11);
                }
                Runnable action = duringArrange;
                duringArrange = null;
                if (action != null) {
                    action.run();
                }
                arrangedSinceArm = true;
            } finally {
                inArrange = previous;
                trace.add("arrange.exit");
            }
        }

        @Override
        protected void damage(int x, int y, int w, int h) {
            record("damage");
            if (fault == Fault.DAMAGE) {
                throw failure;
            }
            super.damage(x, y, w, h);
        }

        @Override
        protected void damageAll() {
            record("damageAll");
            if (fault == Fault.DAMAGE_ALL) {
                throw failure;
            }
            super.damageAll();
        }

        @Override
        protected void fire(int item, int mask) {
            record("fire");
            if (fault == Fault.FIRE_BEFORE) {
                throw failure;
            }
            super.fire(item, mask);
            if (fault == Fault.FIRE_AFTER) {
                throw failure;
            }
        }
    }

    private static Probe fresh() {
        return fresh(false, 0);
    }

    private static Probe fresh(boolean horizontal, int origin) {
        Probe p = new Probe(horizontal, origin);
        p.setLogicalCount(40);
        p.setItemExtent(10);
        p.layout(120, 100);
        discard(p);
        p.trace.clear();
        p.arrangements = p.callbacks = 0;
        p.arm(Fault.NONE);
        p.observe();
        return p;
    }

    private static void discard(Probe p) {
        p.takeBlit();
        p.takeDamage(new int[4]);
    }

    private static void whole(Probe p) {
        assertEquals(0, p.takeBlit());
        int[] d = new int[4];
        assertTrue(p.takeDamage(d));
        assertArrayEquals(new int[] { 0, 0, p.width, p.height }, d);
        assertFalse(p.takeDamage(new int[4]));
    }

    private static void strip(Probe p, int expectedBlit) {
        assertEquals(expectedBlit, p.takeBlit());
        int[] d = new int[4];
        assertTrue(p.takeDamage(d));
        int at = p.origin + (expectedBlit > 0 ? 0 : p.axisLength() + expectedBlit);
        int len = Math.abs(expectedBlit);
        assertArrayEquals(p.horizontal ? new int[] { at, 0, len, p.height }
                : new int[] { 0, at, p.width, len }, d);
        assertFalse(p.takeDamage(new int[4]));
    }

    private static void fails(Probe p, Runnable action) {
        try {
            action.run();
            fail("expected the original hook failure");
        } catch (RuntimeException ex) {
            assertSame(p.failure, ex);
        }
    }

    private static List<int[]> drainingObserver(Probe p) {
        List<int[]> events = new ArrayList<>();
        p.setDelta((item, mask) -> {
            p.trace.add("delta");
            p.callbacks++;
            assertEquals(-1, item);
            assertEquals(WebView.D_SCROLL, mask);
            int blit = p.takeBlit();
            int[] d = new int[4];
            assertTrue(p.takeDamage(d));
            events.add(new int[] { blit, d[0], d[1], d[2], d[3] });
        });
        return events;
    }

    private static void fullEvent(Probe p, int[] event) {
        assertArrayEquals(new int[] { 0, 0, 0, p.width, p.height }, event);
    }

    @Test
    public void cleanForwardCopyAndHookOrderAreUnchanged() {
        Probe p = fresh();
        int generation = p.generation();
        p.scrollTo(10);
        assertEquals(10, p.scrollOffset());
        assertEquals(1, p.arrangements);
        assertEquals(1, p.callbacks);
        assertEquals(generation + 1, p.generation());
        assertEquals(Arrays.asList("extent", "length", "arrange.enter", "arrange.exit",
                "length", "aspects", "origin", "length", "axis", "damage", "fire", "delta"), p.trace);
        strip(p, -10);
    }

    @Test
    public void cleanReverseCopyAndHookOrderAreUnchanged() {
        Probe p = fresh();
        p.scrollTo(30);
        strip(p, -30);
        p.trace.clear();
        p.scrollTo(20);
        assertEquals(Arrays.asList("extent", "length", "arrange.enter", "arrange.exit",
                "length", "aspects", "origin", "axis", "damage", "fire", "delta"), p.trace);
        strip(p, 10);
    }

    @Test
    public void queuedForwardScrollsPublishFullDamageWithoutCopy() {
        Probe p = fresh();
        int generation = p.generation();
        p.scrollTo(10);
        p.scrollTo(20);
        assertEquals(20, p.scrollOffset());
        assertEquals(2, p.arrangements);
        assertEquals(2, p.callbacks);
        assertEquals(generation + 2, p.generation());
        whole(p);
    }

    @Test
    public void queuedReversalLeavesNoCopyAndFullDamage() {
        Probe p = fresh();
        p.scrollTo(10);
        p.scrollTo(0);
        assertEquals(0, p.scrollOffset());
        whole(p);
    }

    @Test
    public void accumulatedMovementBeyondViewportHasNoCopy() {
        Probe p = fresh();
        p.scrollTo(60);
        p.scrollTo(120);
        assertEquals(120, p.scrollOffset());
        whole(p);
    }

    @Test
    public void pendingItemDamagePreventsCopy() {
        Probe p = fresh();
        p.setDelta(null);
        p.setState(2, WebStates.SELECTED, true);
        p.observe();
        p.scrollTo(10);
        whole(p);
    }

    @Test
    public void damageProducedByArrangePreventsCopy() {
        Probe p = fresh();
        p.markDuringArrange = true;
        p.scrollTo(10);
        assertEquals(1, p.arrangements);
        assertEquals(1, p.callbacks);
        whole(p);
    }

    @Test
    public void drainingOnlyBlitDoesNotMakePendingDamageCopyable() {
        Probe p = fresh();
        p.scrollTo(10);
        assertEquals(-10, p.takeBlit());
        p.scrollTo(20);
        whole(p);
    }

    @Test
    public void drainingOnlyDamageDoesNotMakePendingCopyStable() {
        Probe p = fresh();
        p.scrollTo(10);
        assertTrue(p.takeDamage(new int[4]));
        p.scrollTo(20);
        whole(p);
    }

    @Test
    public void drainingBothChannelsAllowsNextCleanCopy() {
        Probe p = fresh();
        p.scrollTo(10);
        strip(p, -10);
        p.scrollTo(20);
        strip(p, -10);
    }

    @Test
    public void longJumpDropsAnOutstandingCopy() {
        Probe p = fresh();
        p.scrollTo(10);
        p.scrollTo(300);
        assertEquals(300, p.scrollOffset());
        whole(p);
    }

    @Test
    public void noOpPreservesPendingPublicationAndCallbacks() {
        Probe p = fresh();
        p.scrollTo(10);
        int generation = p.generation();
        int arrangements = p.arrangements;
        int callbacks = p.callbacks;
        p.trace.clear();
        p.scrollTo(10);
        assertEquals(Arrays.asList("extent", "length"), p.trace);
        assertEquals(generation, p.generation());
        assertEquals(arrangements, p.arrangements);
        assertEquals(callbacks, p.callbacks);
        strip(p, -10);
    }

    @Test
    public void synchronousObserverKeepsIndependentCleanCopies() {
        Probe p = fresh();
        List<int[]> events = drainingObserver(p);
        p.scrollTo(10);
        p.scrollTo(20);
        assertEquals(2, events.size());
        for (int[] event : events) {
            assertArrayEquals(new int[] { -10, 0, 90, 120, 10 }, event);
        }
        assertEquals(0, p.takeBlit());
        assertFalse(p.takeDamage(new int[4]));
    }

    @Test
    public void throwingObserverLeavesPublicationForConservativeNextMove() {
        Probe p = fresh();
        int generation = p.generation();
        p.setDelta((item, mask) -> { throw p.failure; });
        fails(p, () -> p.scrollTo(10));
        assertEquals(generation + 1, p.generation());
        assertEquals(10, p.scrollOffset());
        p.observe();
        p.scrollTo(20);
        whole(p);
        p.scrollTo(30);
        strip(p, -10);
    }

    @Test
    public void arrangeNonzeroAbaWithBothDrainsForcesFullPublication() {
        Probe p = fresh();
        List<int[]> events = drainingObserver(p);
        int generation = p.generation();
        p.duringArrange = () -> {
            p.scrollTo(30);
            p.scrollTo(10);
        };
        p.scrollTo(10);
        assertEquals(10, p.scrollOffset());
        assertEquals(3, p.arrangements);
        assertEquals(3, events.size());
        assertEquals(generation + 3, p.generation());
        for (int[] event : events) {
            fullEvent(p, event);
        }
        p.scrollTo(20);
        assertArrayEquals(new int[] { -10, 0, 90, 120, 10 }, events.get(3));
    }

    @Test
    public void nestedNoOpDoesNotInvalidateCleanCopy() {
        Probe p = fresh();
        p.duringArrange = () -> p.scrollTo(p.scrollOffset());
        p.scrollTo(10);
        assertEquals(1, p.arrangements);
        assertEquals(1, p.callbacks);
        strip(p, -10);
    }

    @Test
    public void caughtNestedGeometryFailureAndRestoreRemainInvalidated() {
        Probe p = fresh();
        List<int[]> events = drainingObserver(p);
        p.duringArrange = () -> {
            p.arm(Fault.ORIGIN);
            fails(p, () -> p.scrollTo(30));
            assertEquals(-20, p.takeBlit());
            assertFalse(p.takeDamage(new int[4]));
            p.arm(Fault.NONE);
            p.scrollTo(10);
        };
        p.scrollTo(10);
        assertEquals(10, p.scrollOffset());
        assertEquals(2, events.size());
        for (int[] event : events) {
            fullEvent(p, event);
        }
        p.scrollTo(20);
        assertArrayEquals(new int[] { -10, 0, 90, 120, 10 }, events.get(2));
    }

    @Test
    public void geometryFailureBoundariesAndLaterScopeResetArePreserved() {
        for (Fault fault : new Fault[] { Fault.LENGTH_BEFORE, Fault.ARRANGE,
                Fault.LENGTH_AFTER, Fault.ASPECTS, Fault.ORIGIN, Fault.AXIS }) {
            Probe p = fresh();
            p.scrollTo(10);
            int generation = p.generation();
            int callbacks = p.callbacks;
            p.arm(fault);
            fails(p, () -> p.scrollTo(20));
            long expectedOffset = fault == Fault.LENGTH_BEFORE ? 10 : 20;
            int expectedBlit = fault == Fault.ORIGIN || fault == Fault.AXIS ? -20 : -10;
            assertEquals(expectedOffset, p.scrollOffset());
            assertEquals(expectedBlit, p.takeBlit());
            int[] d = new int[4];
            assertTrue(p.takeDamage(d));
            assertArrayEquals(new int[] { 0, 90, 120, 10 }, d);
            assertEquals(generation, p.generation());
            assertEquals(callbacks, p.callbacks);
            p.arm(Fault.NONE);
            p.scrollTo(expectedOffset + 10);
            strip(p, -10);
        }
    }

    @Test
    public void horizontalOffsetForwardAndReverseStripsStayInViewport() {
        Probe p = fresh(true, 20);
        p.scrollTo(10);
        strip(p, -10);
        p.scrollTo(0);
        strip(p, 10);
    }

    @Test
    public void verticalOffsetForwardAndReverseStripsStayInViewport() {
        Probe p = fresh(false, 20);
        p.scrollTo(10);
        strip(p, -10);
        p.scrollTo(0);
        strip(p, 10);
    }

    @Test
    public void cleanLargeJumpKeepsExistingShortCircuitHookOrder() {
        Probe p = fresh();
        p.scrollTo(Long.MAX_VALUE);
        assertEquals(300, p.scrollOffset());
        assertEquals(Arrays.asList("extent", "length", "arrange.enter", "arrange.exit",
                "length", "damageAll", "damage", "fire", "delta"), p.trace);
        whole(p);
    }

    @Test
    public void absentDamageAspectKeepsFallbackHookOrder() {
        Probe p = fresh();
        p.advertisedAspects &= ~WebView.A_D;
        p.scrollTo(10);
        assertEquals(Arrays.asList("extent", "length", "arrange.enter", "arrange.exit",
                "length", "aspects", "damageAll", "damage", "fire", "delta"), p.trace);
        whole(p);
    }

    @Test
    public void dirtyFallbackStillEvaluatesLegacyShortPathGeometryHooks() {
        Probe p = fresh();
        p.scrollTo(10);
        p.trace.clear();
        p.scrollTo(20);
        assertEquals(Arrays.asList("extent", "length", "arrange.enter", "arrange.exit",
                "length", "aspects", "origin", "length", "axis",
                "damageAll", "damage", "fire", "delta"), p.trace);
        whole(p);
    }

    @Test
    public void uncaughtNestedFailureRestoresScopeForLaterCleanMove() {
        Probe p = fresh();
        int generation = p.generation();
        p.duringArrange = () -> {
            p.arm(Fault.ORIGIN);
            p.scrollTo(30);
        };
        fails(p, () -> p.scrollTo(10));
        assertEquals(30, p.scrollOffset());
        assertEquals(-20, p.takeBlit());
        assertFalse(p.takeDamage(new int[4]));
        assertEquals(generation, p.generation());
        p.arm(Fault.NONE);
        p.scrollTo(40);
        strip(p, -10);
    }

    @Test
    public void preflightHookAbaWithBothDrainsInvalidatesOuterCopy() {
        Probe p = fresh();
        List<int[]> events = drainingObserver(p);
        p.duringExtent = () -> {
            p.scrollTo(20);
            p.scrollTo(0);
        };
        p.scrollTo(10);
        assertEquals(10, p.scrollOffset());
        assertEquals(3, events.size());
        for (int[] event : events) {
            fullEvent(p, event);
        }
        p.scrollTo(20);
        assertArrayEquals(new int[] { -10, 0, 90, 120, 10 }, events.get(3));
    }

    @Test
    public void observerReentryAfterBothDrainsStillUsesNestedFallback() {
        Probe p = fresh();
        List<int[]> events = new ArrayList<>();
        p.setDelta((item, mask) -> {
            p.callbacks++;
            int blit = p.takeBlit();
            int[] d = new int[4];
            assertTrue(p.takeDamage(d));
            events.add(new int[] { blit, d[0], d[1], d[2], d[3] });
            if (events.size() == 1) {
                p.scrollTo(20);
            }
        });
        p.scrollTo(10);
        assertEquals(20, p.scrollOffset());
        assertEquals(2, events.size());
        assertArrayEquals(new int[] { -10, 0, 90, 120, 10 }, events.get(0));
        fullEvent(p, events.get(1));
        p.scrollTo(30);
        assertArrayEquals(new int[] { -10, 0, 90, 120, 10 }, events.get(2));
    }

    @Test
    public void damageAndFireFailuresPreserveBoundariesAndRestoreScope() {
        for (Fault fault : new Fault[] { Fault.DAMAGE, Fault.FIRE_BEFORE, Fault.FIRE_AFTER }) {
            Probe p = fresh();
            int generation = p.generation();
            p.arm(fault);
            fails(p, () -> p.scrollTo(10));
            assertEquals(10, p.scrollOffset());
            assertEquals(-10, p.takeBlit());
            int[] d = new int[4];
            assertEquals(fault != Fault.DAMAGE, p.takeDamage(d));
            if (fault != Fault.DAMAGE) {
                assertArrayEquals(new int[] { 0, 90, 120, 10 }, d);
            }
            assertEquals(generation + (fault == Fault.FIRE_AFTER ? 1 : 0), p.generation());
            assertEquals(fault == Fault.FIRE_AFTER ? 1 : 0, p.callbacks);
            p.arm(Fault.NONE);
            p.scrollTo(20);
            strip(p, -10);
        }
    }

    @Test
    public void longFallbackDamageFailureRestoresScope() {
        Probe p = fresh();
        int generation = p.generation();
        p.arm(Fault.DAMAGE_ALL);
        fails(p, () -> p.scrollTo(300));
        assertEquals(300, p.scrollOffset());
        assertEquals(0, p.takeBlit());
        assertFalse(p.takeDamage(new int[4]));
        assertEquals(generation, p.generation());
        assertEquals(0, p.callbacks);
        p.arm(Fault.NONE);
        p.scrollTo(290);
        strip(p, 10);
    }

    @Test
    public void longJumpInvalidatesQueuedCopyEvenWhenFullDamageHookThrows() {
        Probe p = fresh();
        p.scrollTo(10);
        int generation = p.generation();
        int callbacks = p.callbacks;
        p.arm(Fault.DAMAGE_ALL);
        fails(p, () -> p.scrollTo(300));
        assertEquals(300, p.scrollOffset());
        assertEquals(0, p.takeBlit());
        int[] d = new int[4];
        assertTrue(p.takeDamage(d));
        assertArrayEquals(new int[] { 0, 90, 120, 10 }, d);
        assertFalse(p.takeDamage(new int[4]));
        assertEquals(generation, p.generation());
        assertEquals(callbacks, p.callbacks);
        p.arm(Fault.NONE);
        p.scrollTo(290);
        strip(p, 10);
    }
}
