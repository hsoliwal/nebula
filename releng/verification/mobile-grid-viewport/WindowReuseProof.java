/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.widgets.grid.internal.mobile;

import java.util.HashMap;
import java.util.Map;

/** No native widget substitutes: compiles/runs the exact original M3 viewport source. */
public final class WindowReuseProof {
    private static final class Sink implements MobileGridViewport.RowSink {
        final Map<Integer, Long> active = new HashMap<>();
        int binds;
        int releases;
        @Override
        public void bind(int slot, long row, String[] cells, boolean selected) {
            if (!("row-" + row).equals(cells[0])) throw new AssertionError("wrong data");
            active.put(Integer.valueOf(slot), Long.valueOf(row));
            binds++;
        }
        @Override
        public void release(int slot, long row) {
            if (!active.remove(Integer.valueOf(slot), Long.valueOf(row))) {
                throw new AssertionError("releasing a never-bound or mismatched row");
            }
            releases++;
        }
    }
    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        final long huge = 1_000_000_000_000L;
        Sink sink = new Sink();
        MobileGridViewport view = new MobileGridViewport(huge, 2, 24, 2,
                (row, col) -> col == 0 ? "row-" + row : "value", sink);
        MobileGridViewport.Window first = view.show(0, 120);
        int initialBinds = sink.binds;
        for (int i = 0; i < 1000; i++) {
            require(first == view.show(0, 120), "unchanged paints must reuse Window object");
        }
        require(initialBinds == sink.binds, "unchanged paints must not refetch cells");
        view.select(1);
        require(first == view.show(0, 120), "selection must preserve geometry snapshot");
        require(initialBinds + 1 == sink.binds, "selection rebinds one row");

        final MobileGridViewport.Window far = view.show((huge - 1L) * 24L, 120);
        require(far.visibleStart() == huge - 1L && far.visibleEnd() == huge,
                "long-index viewport must stay exact");
        require(sink.active.size() <= 9, "bounded native residency");
        view.select(huge - 1L);
        view.setItemCount(0);
        MobileGridViewport.Window zero = view.window();
        require(view.itemCount() == 0L && view.selectedRow() == -1L,
                "zero item count must reset logical selection");
        require(zero.visibleStart() == 0L && zero.visibleEnd() == 0L
                && zero.retainedStart() == 0L && zero.retainedEnd() == 0L,
                "zero item count must clear stale last-presented trillion-row window");
        require(sink.active.isEmpty(), "zero item count must release every resident slot");
        view.setItemCount(0);
        require(zero == view.window(), "repeated zero count must not allocate Window");
        require(zero == view.show(0, 120), "empty paint must reuse Window");
        require(zero == view.show(0, 0), "zero-height empty paint must reuse Window");

        view.setItemCount(7);
        MobileGridViewport.Window reopened = view.show(0, 120);
        require(reopened.visibleEnd() == 5L && reopened.retainedEnd() == 7L,
                "reopened rows must be painted only through viewport+overscan");
        require(sink.active.size() <= 7, "reopened view must stay bounded");
        require(reopened == view.show(0, 120), "reopened stationary paints must reuse Window");
        view.close();
        require(sink.active.isEmpty(), "close must release rebound native slots");

        // A failed provider must not release an unbound row, even during zero reset.
        Sink failureSink = new Sink();
        MobileGridViewport failing = new MobileGridViewport(5, 1, 24, 0,
                (row, col) -> { throw new IllegalStateException("provider error"); }, failureSink);
        try {
            failing.show(0, 24);
            throw new AssertionError("provider error must propagate");
        } catch (IllegalStateException expected) {
            require("provider error".equals(expected.getMessage()), "same failure contract");
        }
        failing.setItemCount(0);
        require(failureSink.releases == 0 && failureSink.active.isEmpty(),
                "never-bound slots must not be released");
        require(failing.window().visibleEnd() == 0L, "failed/empty model must stay empty");
        failing.close();
        System.out.println("PASS 1000 stationary paints, 1e12 rows, zero reset, bounded residency, failure cleanup");
    }
}
