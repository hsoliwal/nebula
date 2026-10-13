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
 * Per-row boolean state packed as bit lanes: lane {@code l} is a {@code long[]} with one bit per
 * row. A lane is allocated on its first {@code true} bit; an absent lane reads {@code false}.
 *
 * <p>Bulk operations work a word (64 rows) at a time: range set/clear, OR/AND/ANDNOT with an
 * external mask such as a filter bitset, and counts with {@link Long#bitCount(long)}. Row
 * insertion and removal shift every lane, so a lane stays keyed by row index.</p>
 *
 * <p>Pure Java, GWT-safe (no {@code int} overflow reliance; {@code long} is emulated by GWT).</p>
 *
 * @since 1.2
 */
public final class RowBitLanes {

	private final long[][] lanes;
	private int rowCount;

	/**
	 * @param laneCount number of lanes (each lane is one per-row boolean)
	 */
	public RowBitLanes(final int laneCount) {
		if (laneCount < 0) {
			throw new IllegalArgumentException("negative lane count: " + laneCount);
		}
		lanes = new long[laneCount][];
	}

	/**
	 * @return number of lanes
	 */
	public int getLaneCount() {
		return lanes.length;
	}

	/**
	 * @return number of rows
	 */
	public int getRowCount() {
		return rowCount;
	}

	/**
	 * Sets the number of rows. New rows read {@code false}; bits of removed rows are cleared.
	 *
	 * @param rows number of rows
	 */
	public void setRowCount(final int rows) {
		if (rows < 0) {
			throw new IllegalArgumentException("negative row count: " + rows);
		}
		if (rows < rowCount) {
			for (int l = 0; l < lanes.length; l++) {
				clearTail(lanes[l], rows, rowCount);
			}
		}
		rowCount = rows;
	}

	/**
	 * @param lane lane
	 * @param row  row
	 * @return the bit
	 */
	public boolean get(final int lane, final int row) {
		checkRow(row);
		final long[] w = lanes[lane];
		return w != null && (row >>> 6) < w.length && (w[row >>> 6] & (1L << (row & 63))) != 0L;
	}

	/**
	 * @param lane  lane
	 * @param row   row
	 * @param value new bit
	 */
	public void set(final int lane, final int row, final boolean value) {
		checkRow(row);
		if (value) {
			final long[] w = ensure(lane, row + 1);
			w[row >>> 6] |= 1L << (row & 63);
		} else {
			final long[] w = lanes[lane];
			if (w != null && (row >>> 6) < w.length) {
				w[row >>> 6] &= ~(1L << (row & 63));
			}
		}
	}

	/**
	 * Sets or clears rows {@code [from, to)} a word at a time.
	 *
	 * @param lane  lane
	 * @param from  first row
	 * @param to    end row (exclusive)
	 * @param value new bit
	 */
	public void setRange(final int lane, final int from, final int to, final boolean value) {
		checkRange(from, to);
		if (from == to) {
			return;
		}
		final long[] w;
		if (value) {
			w = ensure(lane, to);
		} else {
			w = lanes[lane];
			if (w == null) {
				return;
			}
		}
		final int last = Math.min(to, w.length << 6);
		if (from >= last) {
			return;
		}
		final int fw = from >>> 6;
		final int lw = (last - 1) >>> 6;
		final long first = -1L << (from & 63);
		final long end = -1L >>> (63 - ((last - 1) & 63));
		if (fw == lw) {
			apply(w, fw, first & end, value);
			return;
		}
		apply(w, fw, first, value);
		for (int i = fw + 1; i < lw; i++) {
			w[i] = value ? -1L : 0L;
		}
		apply(w, lw, end, value);
	}

	/**
	 * Sets or clears every row.
	 *
	 * @param lane  lane
	 * @param value new bit
	 */
	public void fill(final int lane, final boolean value) {
		setRange(lane, 0, rowCount, value);
	}

	/**
	 * For every row whose {@code mask} bit is set, sets ({@code value}) or clears the lane bit:
	 * {@code lane |= mask} or {@code lane &= ~mask}. Rows outside the mask are untouched.
	 *
	 * @param lane  lane
	 * @param mask  one bit per row (for example a filter bitset); shorter masks read as zero
	 * @param value new bit for masked rows
	 */
	public void setWhere(final int lane, final long[] mask, final boolean value) {
		final int words = Math.min(mask.length, words(rowCount));
		if (value) {
			final long[] w = ensure(lane, rowCount);
			for (int i = 0; i < words; i++) {
				w[i] |= mask[i];
			}
			clearTail(w, rowCount, w.length << 6);
		} else {
			final long[] w = lanes[lane];
			if (w == null) {
				return;
			}
			for (int i = 0, n = Math.min(words, w.length); i < n; i++) {
				w[i] &= ~mask[i];
			}
		}
	}

	/**
	 * {@code lane &= mask}: keeps only rows whose mask bit is set.
	 *
	 * @param lane lane
	 * @param mask one bit per row; shorter masks read as zero
	 */
	public void retainWhere(final int lane, final long[] mask) {
		final long[] w = lanes[lane];
		if (w == null) {
			return;
		}
		for (int i = 0; i < w.length; i++) {
			w[i] &= i < mask.length ? mask[i] : 0L;
		}
	}

	/**
	 * @param lane lane
	 * @return number of set rows
	 */
	public int cardinality(final int lane) {
		final long[] w = lanes[lane];
		if (w == null) {
			return 0;
		}
		int count = 0;
		for (int i = 0, n = Math.min(w.length, words(rowCount)); i < n; i++) {
			count += Long.bitCount(w[i]);
		}
		return count;
	}

	/**
	 * @param lane lane
	 * @param mask one bit per row; shorter masks read as zero
	 * @return number of rows set in both the lane and the mask
	 */
	public int cardinality(final int lane, final long[] mask) {
		final long[] w = lanes[lane];
		if (w == null) {
			return 0;
		}
		int count = 0;
		for (int i = 0, n = Math.min(Math.min(w.length, mask.length), words(rowCount)); i < n; i++) {
			count += Long.bitCount(w[i] & mask[i]);
		}
		return count;
	}

	/**
	 * @param lane lane
	 * @param from first row to look at
	 * @return the first set row {@code >= from}, or {@code -1}
	 */
	public int nextSetRow(final int lane, final int from) {
		final long[] w = lanes[lane];
		if (w == null || from >= rowCount) {
			return -1;
		}
		int i = Math.max(0, from) >>> 6;
		if (i >= w.length) {
			return -1;
		}
		long word = w[i] & (-1L << (Math.max(0, from) & 63));
		while (true) {
			if (word != 0L) {
				final int row = (i << 6) + Long.numberOfTrailingZeros(word);
				return row < rowCount ? row : -1;
			}
			if (++i >= w.length) {
				return -1;
			}
			word = w[i];
		}
	}

	/**
	 * Copies a lane into {@code target} (one bit per row).
	 *
	 * @param lane   lane
	 * @param target destination, at least {@code (getRowCount() + 63) / 64} words
	 * @return {@code target}
	 */
	public long[] copyWords(final int lane, final long[] target) {
		final int words = words(rowCount);
		final long[] w = lanes[lane];
		final int n = w == null ? 0 : Math.min(w.length, words);
		if (n > 0) {
			System.arraycopy(w, 0, target, 0, n);
		}
		Arrays.fill(target, n, words, 0L);
		return target;
	}

	/**
	 * Inserts {@code count} {@code false} rows at {@code at}; rows {@code >= at} move up.
	 *
	 * @param at    insertion row
	 * @param count number of rows
	 */
	public void insertRows(final int at, final int count) {
		if (at < 0 || at > rowCount || count < 0) {
			throw new IndexOutOfBoundsException("at=" + at + ", count=" + count + ", rows=" + rowCount);
		}
		final int oldRows = rowCount;
		rowCount += count;
		for (int l = 0; l < lanes.length; l++) {
			if (lanes[l] != null) {
				final long[] w = ensure(l, rowCount);
				moveUp(w, at, oldRows, count);
				clearBits(w, at, at + count);
			}
		}
	}

	/**
	 * Removes rows {@code [at, at + count)}; rows above move down.
	 *
	 * @param at    first removed row
	 * @param count number of rows
	 */
	public void removeRows(final int at, final int count) {
		if (at < 0 || count < 0 || at > rowCount - count) {
			throw new IndexOutOfBoundsException("at=" + at + ", count=" + count + ", rows=" + rowCount);
		}
		final int oldRows = rowCount;
		rowCount -= count;
		for (int l = 0; l < lanes.length; l++) {
			final long[] w = lanes[l];
			if (w != null) {
				moveDown(w, at + count, oldRows, count);
				clearTail(w, rowCount, oldRows);
			}
		}
	}

	private long[] ensure(final int lane, final int rows) {
		long[] w = lanes[lane];
		final int need = words(Math.max(rows, rowCount));
		if (w == null) {
			w = new long[need];
			lanes[lane] = w;
		} else if (w.length < need) {
			w = Arrays.copyOf(w, Math.min(words(Math.max(rows, rowCount)) + 1, HeapGrowth.capacity(w.length, need, 8)));
			lanes[lane] = w;
		}
		return w;
	}

	/** Moves bits {@code [from, end)} up by {@code shift}, highest bits first. */
	private static void moveUp(final long[] w, final int from, final int end, final int shift) {
		for (int i = end - 1; i >= from; i--) {
			final boolean bit = (i >>> 6) < w.length && (w[i >>> 6] & (1L << (i & 63))) != 0L;
			final int j = i + shift;
			apply(w, j >>> 6, 1L << (j & 63), bit);
		}
	}

	/** Moves bits {@code [from, end)} down by {@code shift}, lowest bits first. */
	private static void moveDown(final long[] w, final int from, final int end, final int shift) {
		final int limit = Math.min(end, (w.length << 6) + shift);
		for (int i = from; i < limit; i++) {
			moveBit(w, i, i - shift);
		}
	}

	private static void moveBit(final long[] w, final int from, final int to) {
		final boolean bit = (from >>> 6) < w.length && (w[from >>> 6] & (1L << (from & 63))) != 0L;
		apply(w, to >>> 6, 1L << (to & 63), bit);
	}

	private static void clearBits(final long[] w, final int from, final int to) {
		for (int i = from; i < to && (i >>> 6) < w.length; i++) {
			w[i >>> 6] &= ~(1L << (i & 63));
		}
	}

	private static void clearTail(final long[] w, final int from, final int to) {
		if (w == null) {
			return;
		}
		final int last = Math.min(to, w.length << 6);
		if (from >= last) {
			return;
		}
		final int fw = from >>> 6;
		w[fw] &= (from & 63) == 0 ? 0L : (-1L >>> (64 - (from & 63)));
		for (int i = fw + 1; i < words(last); i++) {
			w[i] = 0L;
		}
	}

	private static void apply(final long[] w, final int word, final long mask, final boolean value) {
		if (value) {
			w[word] |= mask;
		} else {
			w[word] &= ~mask;
		}
	}

	private static int words(final int rows) {
		return (rows + 63) >>> 6;
	}

	private void checkRow(final int row) {
		if (row < 0 || row >= rowCount) {
			throw new IndexOutOfBoundsException("row=" + row + ", rows=" + rowCount);
		}
	}

	private void checkRange(final int from, final int to) {
		if (from < 0 || to > rowCount || from > to) {
			throw new IndexOutOfBoundsException("from=" + from + ", to=" + to + ", rows=" + rowCount);
		}
	}
}
