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
	/** Supplies the text of a cell. */
	@FunctionalInterface public interface CellSource {
		/** The text of the cell at a logical row and column. */
		String cell(long row, int column);
	}
	/** Receives the rows of the native slots. */
	public interface RowSink {
		/** Binds a logical row and its cell texts to a native slot. */
		void bind(int slot, long row, String[] cells, boolean selected);
		/** Releases a native slot from the row it showed. */
		void release(int slot, long oldRow);
	}
	/** Immutable presentation window without java.lang.Record (Android API 26). */
	public static final class Window {
		private final long visibleStart, visibleEnd, retainedStart, retainedEnd;
		private final int nativeSlots;
		/** Creates a window of visible and retained rows and native slots. */
		public Window(long visibleStart, long visibleEnd, long retainedStart,
				long retainedEnd, int nativeSlots) {
			this.visibleStart = visibleStart; this.visibleEnd = visibleEnd;
			this.retainedStart = retainedStart; this.retainedEnd = retainedEnd;
			this.nativeSlots = nativeSlots;
		}
		/** First visible logical row. */
		public long visibleStart() { return visibleStart; }
		/** Logical row after the last visible one. */
		public long visibleEnd() { return visibleEnd; }
		/** First logical row that owns a native slot. */
		public long retainedStart() { return retainedStart; }
		/** Logical row after the last one that owns a native slot. */
		public long retainedEnd() { return retainedEnd; }
		/** Number of native slots. */
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
	private boolean[] dirty = new boolean[0];
	private int[] seenEpoch = new int[0];
	private int epoch;
	private Window window = new Window(0, 0, 0, 0, 0);

	/** Creates a viewport over itemCount logical rows with the given columns, row height and overscan rows. */
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
	/** The last presented window. */
	public Window window() { return window; }
	/** The selected row, or -1. */
	public long selectedRow() { return selectedRow; }
	/** The number of logical rows. */
	public long itemCount() { return itemCount; }
	/** Sets the number of logical rows and releases the slots of rows beyond it. */
	public void setItemCount(long count) {
		if (count < 0) throw new IllegalArgumentException("negative rows");
		itemCount = count;
		if (selectedRow >= count) selectedRow = -1;
		for (int i = 0; i < rowAt.length; i++) if (rowAt[i] >= count) release(i);
	}
	/** Selects a row (-1 for none) and invalidates the previous and the new selection. */
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
	/** Invalidates every retained row. */
	public void invalidateAll() {
		Arrays.fill(dirty, true);
	}
	/** Scroll coordinates use long pixels to avoid overflow for huge virtual grids. */
	public Window show(long scrollY, int viewportHeight) {
		if (scrollY < 0 || viewportHeight < 0)
			throw new IllegalArgumentException("negative scroll extent");
		if (itemCount == 0 || viewportHeight == 0) {
			for (int i = 0; i < rowAt.length; i++) release(i);
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
				String[] cells = new String[columns];
				for (int col = 0; col < columns; col++) {
					String cell = data.cell(row, col);
					cells[col] = cell == null ? "" : cell;
				}
				sink.bind(slot, row, cells, selected);
				dirty[slot] = false;
				selectedAt[slot] = selected;
			}
		}
		for (int slot = 0; slot < rowAt.length; slot++)
			if (seenEpoch[slot] != epoch) release(slot);
		return window = new Window(first, last, start, end, rowAt.length);
	}
	private static long saturatedAdd(long first, long second) {
		return Long.MAX_VALUE - first < second ? Long.MAX_VALUE : first + second;
	}
	private void ensureCapacity(int required) {
		if (required <= rowAt.length) return;
		for (int i = 0; i < rowAt.length; i++) release(i);
		rowAt = new long[required];
		Arrays.fill(rowAt, -1);
		selectedAt = new boolean[required];
		dirty = new boolean[required];
		seenEpoch = new int[required];
		epoch = 0;
	}
	private void release(int slot) {
		if (rowAt[slot] >= 0) sink.release(slot, rowAt[slot]);
		rowAt[slot] = -1;
		dirty[slot] = false;
		selectedAt[slot] = false;
	}
	/** Releases every native slot. */
	public void close() {
		for (int i = 0; i < rowAt.length; i++) release(i);
		window = new Window(0, 0, 0, 0, rowAt.length);
	}
}
