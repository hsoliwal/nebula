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
		System.out.println("MobileGridViewportTest PASSED");
	}
}
