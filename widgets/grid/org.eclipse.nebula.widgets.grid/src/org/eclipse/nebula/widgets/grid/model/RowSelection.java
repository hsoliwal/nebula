/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid.model;

import java.util.Arrays;

/**
 * Packed selection keyed by model row: selected rows in {@link RowSparseBits} containers (runs
 * for range selections such as select-all, arrays for scattered rows, bitmaps for dense pages, so
 * the representation switches by density), and cell selection as one column bitset per row that
 * has selected cells. No item or point object is kept.
 *
 * <p>Filter-aware queries take the displayed-row words of a {@link RowPermutation}
 * ({@link RowPermutation#displayedBits(long[])}) and AND them with the selection.</p>
 *
 * <p>Pure Java, GWT-safe ({@code int} state, {@code long} only for the filter words).</p>
 *
 * @since 1.2
 */
public final class RowSelection {

	private static final int[] NO_INTS = new int[0];
	private static final int[][] NO_COLUMNS = new int[0][];

	private final RowSparseBits rows = new RowSparseBits();
	private int rowLimit;

	/** Rows with selected cells, ascending, and their column bitsets (32 columns per int). */
	private int[] cellRows = NO_INTS;
	private int[][] cellColumns = NO_COLUMNS;
	private int cellRowCount;

	/**
	 * @param modelCount number of model rows (selection positions are {@code 0..modelCount-1})
	 */
	public RowSelection(final int modelCount) {
		setModelCount(modelCount);
	}

	/**
	 * Sets the number of model rows; selection past the new end is dropped.
	 *
	 * @param modelCount number of model rows
	 */
	public void setModelCount(final int modelCount) {
		if (modelCount < 0) {
			throw new IllegalArgumentException("negative model count: " + modelCount);
		}
		if (modelCount < rowLimit) {
			rows.setRange(modelCount, rowLimit, false);
			int keep = 0;
			while (keep < cellRowCount && cellRows[keep] < modelCount) {
				keep++;
			}
			Arrays.fill(cellColumns, keep, cellRowCount, null);
			cellRowCount = keep;
		}
		rowLimit = modelCount;
	}

	/**
	 * @return number of model rows
	 */
	public int getModelCount() {
		return rowLimit;
	}

	/**
	 * @param row      model row
	 * @param selected new state
	 */
	public void setSelected(final int row, final boolean selected) {
		checkRow(row);
		rows.set(row, selected);
	}

	/**
	 * Selects or deselects rows {@code [from, to)} as one range.
	 *
	 * @param from     first model row
	 * @param to       end model row (exclusive)
	 * @param selected new state
	 */
	public void setSelected(final int from, final int to, final boolean selected) {
		if (from < 0 || to > rowLimit || from > to) {
			throw new IndexOutOfBoundsException("from=" + from + ", to=" + to + ", rows=" + rowLimit);
		}
		rows.setRange(from, to, selected);
	}

	/**
	 * Selects every model row (one run).
	 */
	public void selectAll() {
		rows.setRange(0, rowLimit, true);
	}

	/**
	 * Deselects every row and cell.
	 */
	public void clear() {
		rows.setRange(0, rowLimit, false);
		Arrays.fill(cellColumns, 0, cellRowCount, null);
		cellRowCount = 0;
	}

	/**
	 * @param row model row
	 * @return {@code true} if the row is selected
	 */
	public boolean isSelected(final int row) {
		checkRow(row);
		return rows.get(row);
	}

	/**
	 * @return number of selected rows
	 */
	public int getSelectionCount() {
		return rows.count(0, rowLimit);
	}

	/**
	 * @param displayed one bit per model row that is displayed (for example
	 *                  {@link RowPermutation#displayedBits(long[])})
	 * @return number of selected rows that are displayed
	 */
	public int getSelectionCount(final long[] displayed) {
		int count = 0;
		for (int row = rows.next(0, rowLimit); row >= 0; row = rows.next(row + 1, rowLimit)) {
			if ((row >>> 6) < displayed.length && (displayed[row >>> 6] & (1L << (row & 63))) != 0L) {
				count++;
			}
		}
		return count;
	}

	/**
	 * @param from first model row to look at
	 * @return the first selected model row {@code >= from}, or {@code -1}
	 */
	public int nextSelected(final int from) {
		return from >= rowLimit ? -1 : rows.next(Math.max(0, from), rowLimit);
	}

	/**
	 * Maps the selected rows through a view: ascending view positions of the selected rows that
	 * the view displays (the result of {@code getSelectionIndices} for that view).
	 *
	 * @param view a view over the same model rows
	 * @return ascending view positions
	 */
	public int[] toViewIndices(final RowView view) {
		int[] out = new int[Math.min(getSelectionCount(), view.getViewCount())];
		int n = 0;
		for (int row = rows.next(0, rowLimit); row >= 0; row = rows.next(row + 1, rowLimit)) {
			final int v = row < view.getModelCount() ? view.viewIndex(row) : -1;
			if (v >= 0) {
				if (n == out.length) {
					out = Arrays.copyOf(out, Math.max(16, out.length * 2));
				}
				out[n++] = v;
			}
		}
		out = n == out.length ? out : Arrays.copyOf(out, n);
		Arrays.sort(out);
		return out;
	}

	/**
	 * @param row      model row
	 * @param column   column index
	 * @param selected new state
	 */
	public void setCellSelected(final int row, final int column, final boolean selected) {
		checkRow(row);
		if (column < 0) {
			throw new IndexOutOfBoundsException("column=" + column);
		}
		int at = cellSlot(row);
		if (at < 0) {
			if (!selected) {
				return;
			}
			at = -at - 1;
			if (cellRowCount == cellRows.length) {
				final int next = HeapGrowth.capacity(cellRows.length, cellRowCount + 1, 8);
				cellRows = Arrays.copyOf(cellRows, next);
				cellColumns = Arrays.copyOf(cellColumns, next);
			}
			System.arraycopy(cellRows, at, cellRows, at + 1, cellRowCount - at);
			System.arraycopy(cellColumns, at, cellColumns, at + 1, cellRowCount - at);
			cellRows[at] = row;
			cellColumns[at] = new int[(column >>> 5) + 1];
			cellRowCount++;
		}
		int[] bits = cellColumns[at];
		if ((column >>> 5) >= bits.length) {
			if (!selected) {
				return;
			}
			bits = cellColumns[at] = Arrays.copyOf(bits, (column >>> 5) + 1);
		}
		if (selected) {
			bits[column >>> 5] |= 1 << (column & 31);
		} else {
			bits[column >>> 5] &= ~(1 << (column & 31));
		}
	}

	/**
	 * @param row    model row
	 * @param column column index
	 * @return {@code true} if the cell is selected
	 */
	public boolean isCellSelected(final int row, final int column) {
		checkRow(row);
		final int at = cellSlot(row);
		if (at < 0 || column < 0) {
			return false;
		}
		final int[] bits = cellColumns[at];
		return (column >>> 5) < bits.length && (bits[column >>> 5] & (1 << (column & 31))) != 0;
	}

	/**
	 * @return number of selected cells
	 */
	public int getCellCount() {
		int count = 0;
		for (int i = 0; i < cellRowCount; i++) {
			for (final int word : cellColumns[i]) {
				count += Integer.bitCount(word);
			}
		}
		return count;
	}

	/**
	 * @return number of rows that hold selected cells (rows without any are not stored)
	 */
	public int getCellRowCount() {
		int count = 0;
		for (int i = 0; i < cellRowCount; i++) {
			for (final int word : cellColumns[i]) {
				if (word != 0) {
					count++;
					break;
				}
			}
		}
		return count;
	}

	private int cellSlot(final int row) {
		return Arrays.binarySearch(cellRows, 0, cellRowCount, row);
	}

	private void checkRow(final int row) {
		if (row < 0 || row >= rowLimit) {
			throw new IndexOutOfBoundsException("row=" + row + ", rows=" + rowLimit);
		}
	}
}
