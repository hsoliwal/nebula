/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid.internal.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.nebula.widgets.grid.internal.mobile.MobileGridViewport.RowSink;
import org.eclipse.nebula.widgets.grid.internal.mobile.MobileGridViewport.Window;
import org.junit.Test;

/**
 * The native slots of {@link MobileGridViewport} stay bounded: overscan, a stable view, single-row updates, a long
 * index jump, shrinking and closing. Same checks as the former isolated main() harness of the mobile preview.
 */
public class MobileGridViewport_Test {

	private static final class Sink implements RowSink {
		final Map<Integer, Long> slots = new HashMap<>();
		int binds;

		@Override
		public void bind(int slot, long row, String[] cells, boolean selected) {
			assertEquals("wrong cell", "row-" + row, cells[0]);
			slots.put(Integer.valueOf(slot), Long.valueOf(row));
			binds++;
		}

		@Override
		public void release(int slot, long oldRow) {
			assertTrue("stale slot", slots.remove(Integer.valueOf(slot), Long.valueOf(oldRow)));
		}
	}

	@Test
	public void nativeSlotsStayBounded() {
		Sink sink = new Sink();
		long huge = 1_000_000_000_000L;
		MobileGridViewport viewport = new MobileGridViewport(huge, 3, 20, 2,
				(row, col) -> col == 0 ? "row-" + row : "cell-" + col, sink);
		Window first = viewport.show(0, 80);
		assertTrue("initial 4 visible", first.visibleStart() == 0 && first.visibleEnd() == 4);
		assertTrue("bounded overscan", sink.slots.size() <= 6 && sink.binds == 6);
		viewport.show(0, 80);
		assertEquals("stable view does not fetch model twice", 6, sink.binds);
		viewport.select(2);
		viewport.show(0, 80);
		assertEquals("selection updates one retained row", 7, sink.binds);
		viewport.invalidate(3);
		viewport.show(0, 80);
		assertEquals("single row invalidation", 8, sink.binds);
		long jump = huge - 4;
		Window far = viewport.show(jump * 20L, 80);
		assertTrue("long index jump", far.visibleStart() == jump && far.visibleEnd() == huge);
		assertTrue("bounded far viewport", sink.slots.size() <= 8);
		viewport.setItemCount(10);
		viewport.show(0, 80);
		assertTrue("shrink releases out of range", sink.slots.values().stream().allMatch(r -> r.longValue() < 10));
		viewport.setItemCount(0);
		viewport.show(0, 80);
		assertTrue("zero row cleanup", sink.slots.isEmpty());
		viewport.close();
		assertTrue("close cleanup", sink.slots.isEmpty());
	}


	@Test
	public void steadyViewportPaintsReuseWindowSnapshot() {
		Sink sink = new Sink();
		MobileGridViewport viewport = new MobileGridViewport(1000, 2, 24, 2,
				(row, column) -> column == 0 ? "row-" + row : "cell", sink);
		Window first = viewport.show(0, 120);
		int binds = sink.binds;
		for (int i = 0; i < 1000; i++) {
			assertTrue("unchanged frame must reuse immutable Window", first == viewport.show(0, 120));
		}
		assertEquals("no repeated model provider work", binds, sink.binds);
		viewport.select(1);
		assertTrue("selection redraw keeps same row range", first == viewport.show(0, 120));
		assertEquals("exactly one selected row rebound", binds + 1, sink.binds);
		Window moved = viewport.show(24, 120);
		assertTrue("scrolled row range has new snapshot", moved != first);
		Window empty = viewport.show(0, 0);
		assertEquals(0L, empty.visibleEnd());
		assertTrue("repeated empty paints reuse snapshot", empty == viewport.show(0, 0));
		viewport.close();
		assertTrue(sink.slots.isEmpty());
	}

	@Test
	public void zeroRowsClearPresentedWindowWithoutAnotherNativePaint() {
		Sink sink = new Sink();
		final long huge = 1_000_000_000_000L;
		MobileGridViewport viewport = new MobileGridViewport(huge, 2, 24, 2,
				(row, column) -> column == 0 ? "row-" + row : "cell", sink);
		viewport.show((huge - 1) * 24L, 120);
		assertEquals("last visible row presented", huge - 1, viewport.window().visibleStart());
		assertTrue("some resident rows were bound", !sink.slots.isEmpty());
		viewport.select(huge - 1);
		viewport.setItemCount(0);
		assertEquals("logical count reset", 0L, viewport.itemCount());
		assertEquals("selection reset", -1L, viewport.selectedRow());
		assertTrue("all native row bindings released", sink.slots.isEmpty());
		Window cleared = viewport.window();
		assertEquals("presented start cleared synchronously", 0L, cleared.visibleStart());
		assertEquals("presented end cleared synchronously", 0L, cleared.visibleEnd());
		assertEquals("retained start cleared synchronously", 0L, cleared.retainedStart());
		assertEquals("retained end cleared synchronously", 0L, cleared.retainedEnd());
		assertTrue("slot capacity stays bounded", cleared.nativeSlots() <= 9);
		viewport.setItemCount(0);
		assertTrue("repeated empty reset does not allocate a Window", cleared == viewport.window());
		viewport.setItemCount(7);
		viewport.show(0, 120);
		assertEquals("reopening the model rebinds only visible rows", 5L, viewport.window().visibleEnd());
		assertTrue("new native row bindings remain bounded", sink.slots.size() <= 7);
		viewport.close();
		assertTrue("close releases rebound rows", sink.slots.isEmpty());
	}


    private static final class FailureSink implements RowSink {
        final Map<Integer, Long> slots = new HashMap<>();
        final java.util.List<String> calls = new java.util.ArrayList<>();
        int binds;
        int releases;
        RuntimeException bindFailure;

        @Override public void bind(int slot, long row, String[] cells, boolean selected) {
            calls.add("bind:" + row);
            slots.put(Integer.valueOf(slot), Long.valueOf(row));
            binds++;
            if (bindFailure != null) throw bindFailure;
        }

        @Override public void release(int slot, long row) {
            calls.add("release:" + row);
            assertTrue("release requires a started binding", slots.remove(Integer.valueOf(slot), Long.valueOf(row)));
            releases++;
        }
    }

    private static void expectSameFailure(Throwable expected, Runnable action) {
        Throwable actual = null;
        try { action.run(); } catch (Throwable failure) { actual = failure; }
        org.junit.Assert.assertSame("preserve provider or sink failure", expected, actual);
    }

    @Test public void providerFailureDoesNotReleaseUnboundSlot() {
        FailureSink sink = new FailureSink();
        RuntimeException failure = new IllegalStateException("cell unavailable");
        MobileGridViewport view = new MobileGridViewport(10, 3, 20, 0,
                (row, col) -> { if (col == 1) throw failure; return "cell"; }, sink);
        expectSameFailure(failure, () -> view.show(0, 20));
        view.close();
        view.close();
        assertEquals(0, sink.binds);
        assertEquals(0, sink.releases);
        assertTrue(sink.slots.isEmpty());
    }

    @Test public void providerFailureRemainsRetryableWithoutExtraFetches() {
        FailureSink sink = new FailureSink();
        RuntimeException failure = new IllegalStateException("retryable cell");
        boolean[] fail = {true};
        int[] reads = {0};
        MobileGridViewport view = new MobileGridViewport(10, 3, 20, 0,
                (row, col) -> { reads[0]++; if (fail[0] && col == 1) throw failure; return "cell"; }, sink);
        view.select(0);
        expectSameFailure(failure, () -> view.show(0, 20));
        fail[0] = false;
        view.show(0, 20);
        view.show(0, 20);
        assertEquals(5, reads[0]);
        assertEquals(1, sink.binds);
        assertEquals(Long.valueOf(0), sink.slots.get(Integer.valueOf(0)));
        view.close();
        assertEquals(1, sink.releases);
    }

    @Test public void replacementFailurePreservesReleaseThenFetchOrder() {
        FailureSink sink = new FailureSink();
        RuntimeException failure = new IllegalStateException("replacement unavailable");
        boolean[] fail = {false};
        MobileGridViewport view = new MobileGridViewport(10, 2, 20, 0,
                (row, col) -> {
                    sink.calls.add("cell:" + row + ":" + col);
                    if (fail[0] && row == 1 && col == 1) throw failure;
                    return "cell";
                }, sink);
        view.show(0, 20);
        sink.calls.clear();
        fail[0] = true;
        expectSameFailure(failure, () -> view.show(20, 20));
        assertEquals(java.util.List.of("release:0", "cell:1:0", "cell:1:1"), sink.calls);
        view.close();
        assertEquals(1, sink.releases);
        assertTrue(sink.slots.isEmpty());
        fail[0] = false;
        sink.calls.clear();
        view.show(20, 20);
        assertEquals(java.util.List.of("cell:1:0", "cell:1:1", "bind:1"), sink.calls);
        view.close();
        assertEquals(2, sink.releases);
    }

    @Test public void dirtyRefreshFailureRetainsBindingAndRetryDemand() {
        FailureSink sink = new FailureSink();
        RuntimeException failure = new IllegalStateException("refresh unavailable");
        boolean[] fail = {false};
        MobileGridViewport view = new MobileGridViewport(10, 1, 20, 0,
                (row, col) -> { if (fail[0]) throw failure; return "cell"; }, sink);
        view.show(0, 20);
        view.invalidate(0);
        fail[0] = true;
        expectSameFailure(failure, () -> view.show(0, 20));
        assertEquals(Long.valueOf(0), sink.slots.get(Integer.valueOf(0)));
        assertEquals(0, sink.releases);
        fail[0] = false;
        view.show(0, 20);
        view.show(0, 20);
        assertEquals(2, sink.binds);
        view.close();
        assertEquals(1, sink.releases);
    }

    @Test public void shrinkingAndEmptyWindowIgnoreNeverBoundRows() {
        for (int operation = 0; operation < 3; operation++) {
            FailureSink sink = new FailureSink();
            RuntimeException failure = new IllegalStateException("initial cell");
            MobileGridViewport view = new MobileGridViewport(10, 1, 20, 0,
                    (row, col) -> { throw failure; }, sink);
            expectSameFailure(failure, () -> view.show(0, 20));
            if (operation == 0) view.setItemCount(0);
            else if (operation == 1) view.show(0, 0);
            else view.close();
            view.close();
            assertEquals(0, sink.binds);
            assertEquals(0, sink.releases);
        }
    }

    @Test public void partiallyStartedSinkBindingRetainsCleanupBehavior() {
        FailureSink sink = new FailureSink();
        RuntimeException failure = new IllegalStateException("sink partially bound");
        sink.bindFailure = failure;
        MobileGridViewport view = new MobileGridViewport(10, 1, 20, 0,
                (row, col) -> "cell", sink);
        expectSameFailure(failure, () -> view.show(0, 20));
        view.close();
        assertEquals(1, sink.binds);
        assertEquals(1, sink.releases);
        assertTrue(sink.slots.isEmpty());
    }

    @Test public void providerErrorDoesNotReleaseUnboundSlot() {
        FailureSink sink = new FailureSink();
        AssertionError failure = new AssertionError("provider error");
        MobileGridViewport view = new MobileGridViewport(10, 1, 20, 0,
                (row, col) -> { throw failure; }, sink);
        expectSameFailure(failure, () -> view.show(0, 20));
        view.close();
        assertEquals(0, sink.releases);
    }

    @Test public void capacityGrowthKeepsOnlyStartedBindingsReleasable() {
        FailureSink sink = new FailureSink();
        RuntimeException failure = new IllegalStateException("second row unavailable");
        boolean[] fail = {false};
        MobileGridViewport view = new MobileGridViewport(10, 1, 20, 0,
                (row, col) -> { if (fail[0] && row == 1) throw failure; return "cell"; }, sink);
        view.show(0, 20);
        fail[0] = true;
        expectSameFailure(failure, () -> view.show(0, 40));
        assertEquals(2, sink.binds);
        assertEquals(1, sink.releases);
        view.close();
        assertEquals(2, sink.releases);
        assertTrue(sink.slots.isEmpty());
    }

    @Test public void providerInvalidationDuringBindingSurvives() {
        final MobileGridViewport[] holder = new MobileGridViewport[1];
        int[] reads = {0};
        int[] binds = {0};
        int[] releases = {0};
        holder[0] = new MobileGridViewport(1, 1, 20, 0,
                (row, column) -> {
                    if (++reads[0] == 1) holder[0].invalidate(row);
                    return "cell";
                }, new RowSink() {
                    @Override public void bind(int slot, long row, String[] cells, boolean active) {
                        binds[0]++;
                    }
                    @Override public void release(int slot, long row) { releases[0]++; }
                });
        holder[0].show(0, 20);
        holder[0].show(0, 20);
        holder[0].show(0, 20);
        assertEquals("provider invalidation must survive first bind", 2, reads[0]);
        assertEquals("only the pending refresh is rebound", 2, binds[0]);
        holder[0].close();
        assertEquals("one started native binding is released once", 1, releases[0]);
    }

    @Test public void sinkSelectionAndInvalidateAllAreDeferred() {
        final MobileGridViewport[] holder = new MobileGridViewport[1];
        java.util.List<Boolean> boundSelections = new java.util.ArrayList<>();
        int[] releases = {0};
        holder[0] = new MobileGridViewport(1, 1, 20, 0,
                (row, column) -> "cell", new RowSink() {
                    @Override public void bind(int slot, long row, String[] cells, boolean active) {
                        boundSelections.add(Boolean.valueOf(active));
                        if (boundSelections.size() == 1) holder[0].select(row);
                        if (boundSelections.size() == 2) holder[0].invalidateAll();
                    }
                    @Override public void release(int slot, long row) { releases[0]++; }
                });
        holder[0].show(0, 20);
        holder[0].show(0, 20);
        holder[0].show(0, 20);
        holder[0].show(0, 20);
        assertEquals(java.util.List.of(Boolean.FALSE, Boolean.TRUE, Boolean.TRUE), boundSelections);
        assertEquals(0L, holder[0].selectedRow());
        holder[0].close();
        assertEquals(1, releases[0]);
    }

    @Test public void failedSinkBindingRemainsRetryableWithoutReleasingTwice() {
        RuntimeException error = new IllegalStateException("mobile native bind failed");
        int[] binds = {0};
        int[] releases = {0};
        MobileGridViewport view = new MobileGridViewport(1, 1, 20, 0,
                (row, column) -> "cell", new RowSink() {
                    @Override public void bind(int slot, long row, String[] cells, boolean active) {
                        if (++binds[0] == 1) throw error;
                    }
                    @Override public void release(int slot, long row) { releases[0]++; }
                });
        expectSameFailure(error, () -> view.show(0, 20));
        view.show(0, 20);
        view.show(0, 20);
        assertEquals(2, binds[0]);
        view.close();
        assertEquals(1, releases[0]);
    }

    @Test
    public void reserveIsOnePreflightPerNativeSlotCapacity() {
        final int[] reserved = {0}, count = {0}, binds = {0}, releases = {0};
        final RowSink sink = new RowSink() {
            @Override public void reserve(int slots) {
                reserved[0] = slots;
                count[0]++;
            }
            @Override public void bind(int slot, long row, String[] cells, boolean active) {
                assertTrue("reserve occurs before native bind", slot < reserved[0]);
                binds[0]++;
            }
            @Override public void release(int slot, long row) { releases[0]++; }
        };
        MobileGridViewport view = new MobileGridViewport(1_000_000L, 1, 1, 0,
                (row, col) -> "cell", sink);
        Window first = view.show(0, 8192);
        assertEquals(8192, first.nativeSlots());
        assertEquals("one preflight instead of 8192 per-slot array growths", 1, count[0]);
        assertEquals(8192, reserved[0]);
        assertEquals(8192, binds[0]);
        view.show(16, 8192);
        view.show(0, 20);
        assertEquals("no duplicate native reservation on stable/shrinking windows", 1, count[0]);
        boolean exceeded = false;
        try {
            view.show(0, 8193);
        } catch (IllegalArgumentException expected) {
            exceeded = true;
        }
        assertTrue("max resident slots guard remains authoritative", exceeded);
        assertEquals(1, count[0]);
        view.close();
        assertTrue("all started native bindings can still be released", releases[0] >= 8192);
    }

    @Test
    public void failedReservationPreservesOriginalSlotsAndAllowsRetry() {
        final RuntimeException failure = new IllegalStateException("native slot reserve rejected");
        final boolean[] refuse = {true};
        final int[] binds = {0}, reservations = {0};
        final RowSink sink = new RowSink() {
            @Override public void reserve(int slots) {
                reservations[0]++;
                if (refuse[0]) throw failure;
            }
            @Override public void bind(int slot, long row, String[] cells, boolean selected) {
                binds[0]++;
            }
            @Override public void release(int slot, long row) { }
        };
        MobileGridViewport view = new MobileGridViewport(100L, 1, 1, 0,
                (row, col) -> "cell", sink);
        boolean rejected = false;
        try {
            view.show(0, 20);
        } catch (IllegalStateException caught) {
            rejected = caught == failure;
        }
        assertTrue("reserve failure propagates unchanged", rejected);
        assertEquals("no bind occurred after failed preflight", 0, binds[0]);
        assertEquals(0L, view.window().visibleEnd());
        refuse[0] = false;
        view.show(0, 20);
        assertEquals(2, reservations[0]);
        assertEquals(20, binds[0]);
        view.close();
    }

    @Test
    public void rejectedGrowthPreservesPreviouslyBoundNativeSlots() {
        final int[] reserved = {0}, binds = {0}, releases = {0};
        final boolean[] failGrowth = {false};
        final RowSink sink = new RowSink() {
            @Override public void reserve(int capacity) {
                if (failGrowth[0]) throw new IllegalStateException("reservation rejected");
                reserved[0] = capacity;
            }
            @Override public void bind(int slot, long row, String[] cells, boolean selected) {
                assertTrue(slot < reserved[0]);
                binds[0]++;
            }
            @Override public void release(int slot, long row) { releases[0]++; }
        };
        MobileGridViewport view = new MobileGridViewport(100, 1, 1, 0,
                (row, col) -> "cell", sink);
        Window stable = view.show(0, 4);
        assertEquals(4, binds[0]);
        assertEquals(0, releases[0]);
        failGrowth[0] = true;
        boolean rejected = false;
        try {
            view.show(0, 8);
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        assertTrue("a failed growth preflight must propagate", rejected);
        assertTrue("prior visible window identity remains intact", stable == view.window());
        assertEquals("old native slots must not be released before reserve", 0, releases[0]);
        assertEquals(4, binds[0]);
        failGrowth[0] = false;
        Window grown = view.show(0, 8);
        assertEquals(8, grown.nativeSlots());
        assertEquals(12, binds[0]);
        assertEquals(4, releases[0]);
        view.close();
    }
}
