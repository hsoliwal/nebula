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

    private static void assertTrue(String message, boolean value) { check(value, message); }
    private static void assertTrue(boolean value) { check(value, "expected true"); }
    private static void assertEquals(Object expected, Object actual) {
        check(java.util.Objects.equals(expected, actual), "expected " + expected + " but was " + actual);
    }
}
