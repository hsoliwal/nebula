/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.widgets.grid.internal.mobile;

import java.util.Arrays;
import java.util.Objects;

/**
 * Array-backed viewport projection for an SWT mobile implementation of Nebula
 * Grid. This is a private paint/presentation atom, not a second Grid API.
 * Logical rows are long-indexed; only viewport+overscan rows own native slots.
 */
public final class MobileGridViewport {
	@FunctionalInterface public interface CellSource { String cell(long row, int column); }
	public interface RowSink {
		/** Optional preflight when native slot capacity grows; legacy sinks need no changes. */
		default void reserve(int slots) {}
		void bind(int slot, long row, String[] cells, boolean selected);
		void release(int slot, long oldRow);
	}
	/** Immutable presentation window without java.lang.Record (Android API 26). */
	public static final class Window {
		private final long visibleStart, visibleEnd, retainedStart, retainedEnd;
		private final int nativeSlots;
		public Window(long visibleStart, long visibleEnd, long retainedStart,
				long retainedEnd, int nativeSlots) {
			this.visibleStart = visibleStart; this.visibleEnd = visibleEnd;
			this.retainedStart = retainedStart; this.retainedEnd = retainedEnd;
			this.nativeSlots = nativeSlots;
		}
		public long visibleStart() { return visibleStart; }
		public long visibleEnd() { return visibleEnd; }
		public long retainedStart() { return retainedStart; }
		public long retainedEnd() { return retainedEnd; }
		public int nativeSlots() { return nativeSlots; }
	}
	private static final int MAX_NATIVE_SLOTS = 8192;
	private final int columns;
	private final int rowHeight;
	private final int overscan;
	private final CellSource data;
	private final RowSink sink;
	private long itemCount;
	private long selectedRow = -1;
	private long[] rowAt = new long[0];
	private boolean[] selectedAt = new boolean[0];
	// A row can be assigned before its provider has allowed sink binding to start.
	private boolean[] bindingStarted = new boolean[0];
	private boolean[] dirty = new boolean[0];
	private int[] seenEpoch = new int[0];
	private int epoch;
	private Window window = new Window(0, 0, 0, 0, 0);

	public MobileGridViewport(long itemCount, int columns, int rowHeight,
			int overscan, CellSource data, RowSink sink) {
		if (itemCount < 0 || columns <= 0 || rowHeight <= 0 || overscan < 0)
			throw new IllegalArgumentException("invalid Grid viewport parameters");
		this.itemCount = itemCount;
		this.columns = columns;
		this.rowHeight = rowHeight;
		this.overscan = overscan;
		this.data = Objects.requireNonNull(data);
		this.sink = Objects.requireNonNull(sink);
	}
	public Window window() { return window; }
	public long selectedRow() { return selectedRow; }
	public long itemCount() { return itemCount; }
	public void setItemCount(long count) {
		if (count < 0) throw new IllegalArgumentException("negative rows");
		itemCount = count;
		if (selectedRow >= count) selectedRow = -1;
		for (int i = 0; i < rowAt.length; i++) if (rowAt[i] >= count) release(i);
		// The empty model cannot expose a stale last-presented row.
		// Preserve Window identity for repeated zero-count updates.
		if (count == 0 && window.visibleEnd() != 0)
			window = new Window(0, 0, 0, 0, rowAt.length);
	}
	public void select(long row) {
		if (row < -1 || row >= itemCount) throw new IndexOutOfBoundsException("row " + row);
		if (selectedRow == row) return;
		long previous = selectedRow;
		selectedRow = row;
		invalidate(previous);
		invalidate(row);
	}
	/** Invalidate only a retained row; offscreen values remain deferred. */
	public void invalidate(long row) {
		if (row < 0 || rowAt.length == 0) return;
		int slot = (int) (row % rowAt.length);
		if (rowAt[slot] == row) dirty[slot] = true;
	}
	public void invalidateAll() {
		Arrays.fill(dirty, true);
	}
	/** Scroll coordinates use long pixels to avoid overflow for huge virtual grids. */
	public Window show(long scrollY, int viewportHeight) {
		if (scrollY < 0 || viewportHeight < 0)
			throw new IllegalArgumentException("negative scroll extent");
		if (itemCount == 0 || viewportHeight == 0) {
			for (int i = 0; i < rowAt.length; i++) release(i);
			if (window.visibleStart() == 0 && window.visibleEnd() == 0
					&& window.retainedStart() == 0 && window.retainedEnd() == 0
					&& window.nativeSlots() == rowAt.length)
				return window;
			return window = new Window(0, 0, 0, 0, rowAt.length);
		}
		long first = Math.min(itemCount - 1, scrollY / rowHeight);
		long rowsInView = 1 + ((scrollY % rowHeight + viewportHeight - 1L) / rowHeight);
		long last = Math.min(itemCount, saturatedAdd(first, rowsInView));
		long start = Math.max(0L, first - Math.min(first, (long)overscan));
		long end = Math.min(itemCount, saturatedAdd(last, overscan));
		long nativeRows = end - start;
		if (nativeRows > MAX_NATIVE_SLOTS)
			throw new IllegalArgumentException("mobile viewport exceeds " + MAX_NATIVE_SLOTS + " rows");
		ensureCapacity((int)nativeRows);
		if (++epoch == 0) { Arrays.fill(seenEpoch, 0); epoch = 1; }
		for (long row = start; row < end; row++) {
			int slot = (int)(row % rowAt.length);
			seenEpoch[slot] = epoch;
			if (rowAt[slot] != row) {
				release(slot);
				rowAt[slot] = row;
				dirty[slot] = true;
			}
			boolean selected = row == selectedRow;
			if (dirty[slot] || selectedAt[slot] != selected) {
				// The provider or native receiver may synchronously invalidate
				// or select this row. Consume only the refresh being delivered;
				// do not erase a new request made inside a callback.
				dirty[slot] = false;
				selectedAt[slot] = selected;
				try {
					String[] cells = new String[columns];
					for (int col = 0; col < columns; col++) {
						String cell = data.cell(row, col);
						cells[col] = cell == null ? "" : cell;
					}
					bindingStarted[slot] = true;
					sink.bind(slot, row, cells, selected);
				} catch (RuntimeException | Error failure) {
					// A failed provider or partial native bind must remain retryable.
					// A reentrant removal may already have released the slot.
					if (rowAt[slot] == row) dirty[slot] = true;
					throw failure;
				}
			}
		}
		for (int slot = 0; slot < rowAt.length; slot++)
			if (seenEpoch[slot] != epoch) release(slot);
		// Selection and cell updates do not change the immutable row geometry.
		if (window.visibleStart() == first && window.visibleEnd() == last
				&& window.retainedStart() == start && window.retainedEnd() == end
				&& window.nativeSlots() == rowAt.length)
			return window;
		return window = new Window(first, last, start, end, rowAt.length);
	}
	private static long saturatedAdd(long first, long second) {
		return Long.MAX_VALUE - first < second ? Long.MAX_VALUE : first + second;
	}
	private void ensureCapacity(int required) {
		if (required <= rowAt.length) return;
		// Reserve the receiver's parallel lanes once before any old slot is released.
		// Failure leaves this viewport's old slot identity and binding untouched.
		sink.reserve(required);
		for (int i = 0; i < rowAt.length; i++) release(i);
		rowAt = new long[required];
		Arrays.fill(rowAt, -1);
		selectedAt = new boolean[required];
		bindingStarted = new boolean[required];
		dirty = new boolean[required];
		seenEpoch = new int[required];
		epoch = 0;
	}
	private void release(int slot) {
		if (rowAt[slot] >= 0 && bindingStarted[slot]) sink.release(slot, rowAt[slot]);
		rowAt[slot] = -1;
		bindingStarted[slot] = false;
		dirty[slot] = false;
		selectedAt[slot] = false;
	}
	public void close() {
		for (int i = 0; i < rowAt.length; i++) release(i);
		window = new Window(0, 0, 0, 0, rowAt.length);
	}
}
