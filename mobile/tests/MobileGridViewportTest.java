/* SPDX-License-Identifier: EPL-2.0 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.nebula.widgets.grid.internal.mobile.MobileGridViewport;
import org.eclipse.nebula.widgets.grid.internal.mobile.MobileGridViewport.*;

public final class MobileGridViewportTest {
	static final class Sink implements RowSink {
		final Map<Integer, Long> slots = new HashMap<>();
		int binds;
		int releases;
		@Override public void bind(int slot, long row, String[] cells, boolean selected) {
			if (!cells[0].equals("row-" + row)) throw new AssertionError("wrong cell");
			slots.put(slot, row);
			binds++;
		}
		@Override public void release(int slot, long oldRow) {
			if (!slots.remove(slot, oldRow)) throw new AssertionError("stale slot");
			releases++;
		}
	}
	static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
	public static void main(String[] args) {
		Sink sink = new Sink();
		long huge = 1_000_000_000_000L;
		MobileGridViewport viewport = new MobileGridViewport(huge, 3, 20, 2,
				(row, col) -> col == 0 ? "row-" + row : "cell-" + col, sink);
		Window first = viewport.show(0, 80);
		check(first.visibleStart() == 0 && first.visibleEnd() == 4, "initial 4 visible");
		check(sink.slots.size() <= 6 && sink.binds == 6, "bounded overscan");
		viewport.show(0, 80);
		check(sink.binds == 6, "stable view does not fetch model twice");
		viewport.select(2);
		viewport.show(0, 80);
		check(sink.binds == 7, "selection updates one retained row");
		viewport.invalidate(3);
		viewport.show(0, 80);
		check(sink.binds == 8, "single row invalidation");
		long jump = huge - 4;
		Window far = viewport.show(jump * 20L, 80);
		check(far.visibleStart() == jump && far.visibleEnd() == huge, "long index jump");
		check(sink.slots.size() <= 8, "bounded far viewport");
		viewport.setItemCount(10);
		viewport.show(0, 80);
		check(sink.slots.values().stream().allMatch(r -> r < 10), "shrink releases out of range");
		viewport.setItemCount(0);
		viewport.show(0, 80);
		check(sink.slots.isEmpty(), "zero row cleanup");
		viewport.close();
		check(sink.slots.isEmpty(), "close cleanup");
        providerFailureDoesNotReleaseUnboundSlot();
        providerFailureRemainsRetryableWithoutExtraFetches();
        replacementFailurePreservesReleaseThenFetchOrder();
        dirtyRefreshFailureRetainsBindingAndRetryDemand();
        shrinkingAndEmptyWindowIgnoreNeverBoundRows();
        partiallyStartedSinkBindingRetainsCleanupBehavior();
        providerErrorDoesNotReleaseUnboundSlot();
        capacityGrowthKeepsOnlyStartedBindingsReleasable();
        providerInvalidationDuringBindingRemainsPending();
        sinkSelectionAndInvalidationRemainPending();
        failedSinkBindingRemainsRetryable();
        reserveContract();
        growthFailurePreservesResidentBindings();
        reentrantReservationPreservesOldBindings();
        unchangedFramesReuseWindow();
        zeroCountClearsPresentedWindow();
		System.out.println("MobileGridViewportTest PASSED");
	}


    private static final class FailureSink implements RowSink {
        final Map<Integer, Long> slots = new HashMap<>();
        final java.util.List<String> calls = new java.util.ArrayList<>();
        int binds;
        int releases;
        RuntimeException bindFailure;

        @Override public void bind(int slot, long row, String[] cells, boolean selected) {
            calls.add("bind:" + row);
            slots.put(slot, row);
            binds++;
            if (bindFailure != null) throw bindFailure;
        }

        @Override public void release(int slot, long row) {
            calls.add("release:" + row);
            assertTrue("release requires a started binding", slots.remove(slot, row));
            releases++;
        }
    }

    private static void expectSameFailure(Throwable expected, Runnable action) {
        Throwable actual = null;
        try { action.run(); } catch (Throwable failure) { actual = failure; }
        check(expected == actual, "preserve provider or sink failure");
    }

    static void providerFailureDoesNotReleaseUnboundSlot() {
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

    static void providerFailureRemainsRetryableWithoutExtraFetches() {
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
        assertEquals(Long.valueOf(0), sink.slots.get(0));
        view.close();
        assertEquals(1, sink.releases);
    }

    static void replacementFailurePreservesReleaseThenFetchOrder() {
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

    static void dirtyRefreshFailureRetainsBindingAndRetryDemand() {
        FailureSink sink = new FailureSink();
        RuntimeException failure = new IllegalStateException("refresh unavailable");
        boolean[] fail = {false};
        MobileGridViewport view = new MobileGridViewport(10, 1, 20, 0,
                (row, col) -> { if (fail[0]) throw failure; return "cell"; }, sink);
        view.show(0, 20);
        view.invalidate(0);
        fail[0] = true;
        expectSameFailure(failure, () -> view.show(0, 20));
        assertEquals(Long.valueOf(0), sink.slots.get(0));
        assertEquals(0, sink.releases);
        fail[0] = false;
        view.show(0, 20);
        view.show(0, 20);
        assertEquals(2, sink.binds);
        view.close();
        assertEquals(1, sink.releases);
    }

    static void shrinkingAndEmptyWindowIgnoreNeverBoundRows() {
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

    static void partiallyStartedSinkBindingRetainsCleanupBehavior() {
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

    static void providerErrorDoesNotReleaseUnboundSlot() {
        FailureSink sink = new FailureSink();
        AssertionError failure = new AssertionError("provider error");
        MobileGridViewport view = new MobileGridViewport(10, 1, 20, 0,
                (row, col) -> { throw failure; }, sink);
        expectSameFailure(failure, () -> view.show(0, 20));
        view.close();
        assertEquals(0, sink.releases);
    }

    static void capacityGrowthKeepsOnlyStartedBindingsReleasable() {
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

    static void providerInvalidationDuringBindingRemainsPending() {
        final MobileGridViewport[] holder = new MobileGridViewport[1];
        int[] reads = {0};
        int[] binds = {0};
        int[] releases = {0};
        holder[0] = new MobileGridViewport(1, 1, 20, 0,
                (row, column) -> {
                    if (++reads[0] == 1) holder[0].invalidate(row);
                    return "cell";
                }, new RowSink() {
                    @Override public void bind(int slot, long row, String[] cells, boolean selected) {
                        binds[0]++;
                    }
                    @Override public void release(int slot, long row) { releases[0]++; }
                });
        holder[0].show(0, 20);
        holder[0].show(0, 20);
        holder[0].show(0, 20);
        assertEquals(2, reads[0]);
        assertEquals(2, binds[0]);
        holder[0].close();
        assertEquals(1, releases[0]);
    }

    static void sinkSelectionAndInvalidationRemainPending() {
        final MobileGridViewport[] holder = new MobileGridViewport[1];
        java.util.List<Boolean> selectedAtBind = new java.util.ArrayList<>();
        int[] releases = {0};
        holder[0] = new MobileGridViewport(1, 1, 20, 0,
                (row, column) -> "cell", new RowSink() {
                    @Override public void bind(int slot, long row, String[] cells, boolean selected) {
                        selectedAtBind.add(selected);
                        if (selectedAtBind.size() == 1) holder[0].select(row);
                        if (selectedAtBind.size() == 2) holder[0].invalidateAll();
                    }
                    @Override public void release(int slot, long row) { releases[0]++; }
                });
        holder[0].show(0, 20);
        holder[0].show(0, 20);
        holder[0].show(0, 20);
        holder[0].show(0, 20);
        assertEquals(java.util.List.of(false, true, true), selectedAtBind);
        assertEquals(0L, holder[0].selectedRow());
        holder[0].close();
        assertEquals(1, releases[0]);
    }

    static void failedSinkBindingRemainsRetryable() {
        RuntimeException failure = new IllegalStateException("native bind failed");
        int[] binds = {0};
        int[] releases = {0};
        MobileGridViewport view = new MobileGridViewport(1, 1, 20, 0,
                (row, column) -> "cell", new RowSink() {
                    @Override public void bind(int slot, long row, String[] cells, boolean selected) {
                        if (++binds[0] == 1) throw failure;
                    }
                    @Override public void release(int slot, long row) { releases[0]++; }
                });
        expectSameFailure(failure, () -> view.show(0, 20));
        view.show(0, 20);
        view.show(0, 20);
        assertEquals(2, binds[0]);
        view.close();
        assertEquals(1, releases[0]);
    }


    private static void reserveContract() {
        final int[] reserved = {0}, calls = {0}, binds = {0};
        RowSink sink = new RowSink() {
            @Override public void reserve(int count) {
                reserved[0] = count;
                calls[0]++;
            }
            @Override public void bind(int slot, long row, String[] cells, boolean selected) {
                check(slot < reserved[0], "slot bound without preflight");
                binds[0]++;
            }
            @Override public void release(int slot, long row) { }
        };
        MobileGridViewport view = new MobileGridViewport(1_000_000L, 1, 1, 0,
                (row, col) -> "cell", sink);
        Window first = view.show(0, 8192);
        check(first.nativeSlots() == 8192, "native slot limit");
        check(calls[0] == 1 && reserved[0] == 8192 && binds[0] == 8192,
                "one exact native reservation before all binding");
        view.show(16, 8192);
        view.show(0, 10);
        check(calls[0] == 1, "no repeat reservation for stable capacity");
        view.close();

        RuntimeException error = new IllegalStateException("native reserve failed");
        boolean[] rejected = {true};
        int[] reservedAttempts = {0}, partialBinds = {0};
        MobileGridViewport rejectedView = new MobileGridViewport(100, 1, 1, 0,
                (row, col) -> "cell", new RowSink() {
                    @Override public void reserve(int count) {
                        reservedAttempts[0]++;
                        if (rejected[0]) throw error;
                    }
                    @Override public void bind(int slot, long row, String[] cells, boolean selected) {
                        partialBinds[0]++;
                    }
                    @Override public void release(int slot, long row) { }
                });
        expectSameFailure(error, () -> rejectedView.show(0, 20));
        check(partialBinds[0] == 0 && rejectedView.window().visibleEnd() == 0,
                "failed preflight preserves unbound initial viewport");
        rejected[0] = false;
        rejectedView.show(0, 20);
        check(reservedAttempts[0] == 2 && partialBinds[0] == 20, "retry after preflight failure");
        rejectedView.close();
    }


    private static void growthFailurePreservesResidentBindings() {
        int[] capacity = {0}, binds = {0}, releases = {0};
        boolean[] failGrowth = {false};
        RowSink sink = new RowSink() {
            @Override public void reserve(int slots) {
                if (failGrowth[0]) throw new IllegalStateException("native growth denied");
                capacity[0] = slots;
            }
            @Override public void bind(int slot, long row, String[] cells, boolean selected) {
                check(slot < capacity[0], "slot was bound without reserve");
                binds[0]++;
            }
            @Override public void release(int slot, long row) { releases[0]++; }
        };
        MobileGridViewport view = new MobileGridViewport(100, 1, 1, 0,
                (row, col) -> "cell", sink);
        Window before = view.show(0, 4);
        check(binds[0] == 4, "four initial bindings");
        failGrowth[0] = true;
        boolean rejected = false;
        try { view.show(0, 8); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected && view.window() == before, "failed native growth preserves window");
        check(binds[0] == 4 && releases[0] == 0, "previous bindings intact after failure");
        failGrowth[0] = false;
        Window after = view.show(0, 8);
        check(after.nativeSlots() == 8 && binds[0] == 12 && releases[0] == 4,
                "retry replaces exactly four retained bindings");
        view.close();
    }


    static void unchangedFramesReuseWindow() {
        Sink sink = new Sink();
        MobileGridViewport view = new MobileGridViewport(1000, 2, 24, 2,
                (row, col) -> col == 0 ? "row-" + row : "cell", sink);
        Window first = view.show(0, 120);
        int initialBinds = sink.binds;
        for (int i = 0; i < 1000; i++)
            check(view.show(0, 120) == first, "unchanged Window identity");
        check(sink.binds == initialBinds, "no duplicate binding on stable frame");
        view.select(1);
        check(view.show(0, 120) == first, "selection keeps Window geometry");
        check(sink.binds == initialBinds + 1, "only one selection row rebound");
        Window moved = view.show(24, 120);
        check(moved != first && moved.visibleStart() == 1, "scroll changes Window");
        Window empty = view.show(0, 0);
        check(empty.visibleEnd() == 0 && empty.retainedEnd() == 0, "zero-height viewport");
        check(view.show(0, 0) == empty, "unchanged empty Window identity");
        view.close();
        check(sink.slots.isEmpty(), "no bindings remain after close");
    }

    static void zeroCountClearsPresentedWindow() {
        final long huge = 1_000_000_000_000L;
        Sink sink = new Sink();
        MobileGridViewport view = new MobileGridViewport(huge, 2, 24, 2,
                (row, col) -> col == 0 ? "row-" + row : "cell", sink);
        Window far = view.show((huge - 1) * 24L, 120);
        check(far.visibleStart() == huge - 1 && far.visibleEnd() == huge,
                "trillion-row last viewport");
        view.select(huge - 1);
        view.setItemCount(0);
        Window reset = view.window();
        check(reset.visibleStart() == 0 && reset.visibleEnd() == 0
                && reset.retainedStart() == 0 && reset.retainedEnd() == 0,
                "zero count clears presented Window synchronously");
        check(view.selectedRow() == -1 && sink.slots.isEmpty(),
                "zero count releases selection and slot bindings");
        view.setItemCount(0);
        check(view.window() == reset, "repeated zero reset reuses Window");
        check(view.show(0, 120) == reset, "empty paint reuses reset Window");
        view.setItemCount(7);
        Window reopened = view.show(0, 120);
        check(reopened.visibleStart() == 0 && reopened.visibleEnd() == 5
                && reopened.retainedEnd() == 7, "regrown viewport and overscan");
        check(sink.slots.size() <= 7, "regrown native slots bounded");
        view.close();
        check(sink.slots.isEmpty(), "regrown slots released");
    }


    private static void reentrantReservationPreservesOldBindings() {
        MobileGridViewport[] owner = new MobileGridViewport[1];
        int[] mode = {0}, reserves = {0}, binds = {0}, releases = {0};
        RowSink sink = new RowSink() {
            @Override public void reserve(int count) {
                reserves[0]++;
                if (mode[0] == 1) owner[0].show(0, 8);
                if (mode[0] == 2) owner[0].setItemCount(0);
                if (mode[0] == 3) owner[0].close();
            }
            @Override public void bind(int slot, long row, String[] cells, boolean selected) {
                binds[0]++;
            }
            @Override public void release(int slot, long row) {
                releases[0]++;
            }
        };
        owner[0] = new MobileGridViewport(30, 1, 1, 0, (row, col) -> "cell", sink);
        Window first = owner[0].show(0, 4);
        for (int request = 1; request <= 3; request++) {
            mode[0] = request;
            boolean rejected = false;
            try { owner[0].show(0, 8); }
            catch (IllegalStateException expected) { rejected = true; }
            check(rejected, "reentrant native reserve must fail");
            check(owner[0].window() == first && owner[0].itemCount() == 30,
                    "failed reserve preserves window and model");
            check(binds[0] == 4 && releases[0] == 0, "old bindings preserved");
        }
        mode[0] = 0;
        Window expanded = owner[0].show(0, 8);
        check(expanded.nativeSlots() == 8 && reserves[0] == 5,
                "subsequent retry reserves exact new capacity");
        check(binds[0] == 12 && releases[0] == 4, "growth releases each old native slot once");
        owner[0].close();
        check(releases[0] == 12, "close releases all retained bindings");
    }

    private static void assertTrue(String message, boolean value) { check(value, message); }
    private static void assertTrue(boolean value) { check(value, "expected true"); }
    private static void assertEquals(Object expected, Object actual) {
        check(java.util.Objects.equals(expected, actual), "expected " + expected + " but was " + actual);
    }
}
