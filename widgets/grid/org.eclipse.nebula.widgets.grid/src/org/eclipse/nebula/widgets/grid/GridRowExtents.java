/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

import org.eclipse.nebula.widgets.grid.model.HeapGrowth;

/**
 * Row geometry of a Grid's item list: which item is the k-th visible row, how many visible rows
 * lie before an item, and how many pixels the visible rows before an item take.
 * <p>
 * Every item position {@code 0 .. size - 1} has an <em>extent</em>: the item height plus the
 * one-pixel row separator when the item is visible (all its ancestors are expanded), or 0 when it
 * is hidden. Two Fenwick (binary indexed) trees over the positions hold the visible-row counts
 * and the extents, so every question is answered in O(log n) and no item is visited:
 * </p>
 * <ul>
 * <li>{@link #rankOf(int)}: visible rows before a position (the vertical scroll selection of a
 * visible item);</li>
 * <li>{@link #positionOfRank(int)}: the position of the k-th visible row;</li>
 * <li>{@link #extentBefore(int)} and {@link #extentOfRank(int)}: pixels above a position or a
 * visible row;</li>
 * <li>{@link #rankAtExtent(long)}: the visible row that holds a pixel offset.</li>
 * </ul>
 * <p>
 * Changing one extent is O(log n), appending a position is O(log n), and {@link #rebuild} is a
 * linear O(n) build. The Fenwick update and linear build follow SWT's {@code VirtualRowExtents}
 * (EPL-2.0); the count/extent rank and select descents are specific to the Grid's flat item list,
 * where hidden tree rows stay in the list with weight 0.
 * </p>
 */
final class GridRowExtents {

	/* bytes per position over the three lanes: extent (int), count tree (int), extent tree (long) */
	private static final int SLOT_BYTES = 16;

	private int size;
	private boolean valid;
	private int visibleRows;
	private long totalExtent;
	/* extent per position, 0-based */
	private int[] extents = new int[0];
	/* Fenwick trees, 1-based; element 0 is unused */
	private int[] countTree = new int[1];
	private long[] extentTree = new long[1];

	/**
	 * @param expectedSize the number of items the caller holds
	 * @return {@code true} when the index was built for exactly that many items and no change has
	 *         invalidated it since
	 */
	boolean isValid(final int expectedSize) {
		return valid && size == expectedSize;
	}

	/** Marks the index stale; the next {@link #rebuild} restores it. */
	void invalidate() {
		valid = false;
	}

	/** @return the number of positions */
	int size() {
		return size;
	}

	/** @return the number of visible rows */
	int visibleCount() {
		return visibleRows;
	}

	/** @return the extent of all visible rows */
	long totalExtent() {
		return totalExtent;
	}

	/**
	 * Builds the index in O(n).
	 *
	 * @param count    number of positions
	 * @param extentAt extent of a position (non-negative)
	 */
	void rebuild(final int count, final IntUnaryOperator extentAt) {
		ensureCapacity(count);
		Arrays.fill(countTree, 0, count + 1, 0);
		Arrays.fill(extentTree, 0, count + 1, 0L);
		int visible = 0;
		long total = 0;
		for (int i = 1; i <= count; i++) {
			final int extent = checkExtent(extentAt.applyAsInt(i - 1));
			final int counted = extent > 0 ? 1 : 0;
			extents[i - 1] = extent;
			countTree[i] += counted;
			extentTree[i] += extent;
			final int parent = i + (i & -i);
			if (parent <= count) {
				countTree[parent] += countTree[i];
				extentTree[parent] += extentTree[i];
			}
			visible += counted;
			total += extent;
		}
		size = count;
		visibleRows = visible;
		totalExtent = total;
		valid = true;
	}

	/**
	 * Appends a position at the end in O(log n).
	 *
	 * @param extent extent of the new position
	 */
	void append(final int extent) {
		checkExtent(extent);
		ensureCapacity(size + 1);
		final int i = size + 1;
		final int counted = extent > 0 ? 1 : 0;
		int count = counted;
		long sum = extent;
		// node i covers positions (i - lowbit(i), i]; its children are the nodes below i
		final int low = i - (i & -i);
		for (int child = i - 1; child > low; child -= child & -child) {
			count += countTree[child];
			sum += extentTree[child];
		}
		extents[size] = extent;
		countTree[i] = count;
		extentTree[i] = sum;
		size = i;
		visibleRows += counted;
		totalExtent += extent;
	}

	/**
	 * Changes the extent of one position in O(log n).
	 *
	 * @param position the position
	 * @param extent   its new extent
	 */
	void set(final int position, final int extent) {
		checkExtent(extent);
		final int old = extents[position];
		if (old == extent) {
			return;
		}
		extents[position] = extent;
		final int countDelta = (extent > 0 ? 1 : 0) - (old > 0 ? 1 : 0);
		final long extentDelta = (long) extent - old;
		for (int i = position + 1; i <= size; i += i & -i) {
			countTree[i] += countDelta;
			extentTree[i] += extentDelta;
		}
		visibleRows += countDelta;
		totalExtent += extentDelta;
	}

	/**
	 * @param position a position
	 * @return its extent, 0 when hidden
	 */
	int extentAt(final int position) {
		return extents[position];
	}

	/**
	 * @param position a position in {@code [0, size]}
	 * @return the number of visible rows before it
	 */
	int rankOf(final int position) {
		int rank = 0;
		for (int i = position; i > 0; i -= i & -i) {
			rank += countTree[i];
		}
		return rank;
	}

	/**
	 * @param position a position in {@code [0, size]}
	 * @return the extent of the visible rows before it
	 */
	long extentBefore(final int position) {
		long extent = 0;
		for (int i = position; i > 0; i -= i & -i) {
			extent += extentTree[i];
		}
		return extent;
	}

	/**
	 * @param rank a visible row, {@code 0 <= rank < visibleCount()}
	 * @return its position
	 */
	int positionOfRank(final int rank) {
		int position = 0;
		int remaining = rank + 1;
		for (int step = Integer.highestOneBit(size); step > 0; step >>= 1) {
			final int next = position + step;
			if (next <= size && countTree[next] < remaining) {
				position = next;
				remaining -= countTree[next];
			}
		}
		return position;
	}

	/**
	 * @param rank a number of visible rows, {@code 0 <= rank <= visibleCount()}
	 * @return the extent of the first {@code rank} visible rows
	 */
	long extentOfRank(final int rank) {
		if (rank == 0) {
			return 0;
		}
		if (rank == visibleRows) {
			return totalExtent;
		}
		return extentBefore(positionOfRank(rank));
	}

	/**
	 * @param offset a pixel offset from the first visible row
	 * @return the largest number of visible rows whose total extent is at most {@code offset}, that
	 *         is the visible row holding {@code offset} ({@link #visibleCount()} when the offset lies
	 *         past the last row), or -1 when {@code offset} is negative
	 */
	int rankAtExtent(final long offset) {
		if (offset < 0) {
			return -1;
		}
		int position = 0;
		int rank = 0;
		long extent = 0;
		for (int step = Integer.highestOneBit(size); step > 0; step >>= 1) {
			final int next = position + step;
			if (next <= size && extent + extentTree[next] <= offset) {
				position = next;
				extent += extentTree[next];
				rank += countTree[next];
			}
		}
		return rank;
	}

	/**
	 * @param position a position in {@code [0, size]}
	 * @return the first visible position at or after it, or -1
	 */
	int nextVisible(final int position) {
		final int rank = rankOf(position);
		return rank < visibleRows ? positionOfRank(rank) : -1;
	}

	/**
	 * @param position a position in {@code [-1, size - 1]}
	 * @return the last visible position at or before it, or -1
	 */
	int previousVisible(final int position) {
		final int rank = rankOf(position + 1) - 1;
		return rank >= 0 ? positionOfRank(rank) : -1;
	}

	private void ensureCapacity(final int positions) {
		if (positions <= extents.length) {
			return;
		}
		final int capacity = HeapGrowth.capacity(extents.length, positions, SLOT_BYTES);
		// stage every lane before publishing, so a failed allocation leaves the index unchanged
		final int[] nextExtents = Arrays.copyOf(extents, capacity);
		final int[] nextCounts = Arrays.copyOf(countTree, capacity + 1);
		final long[] nextSums = Arrays.copyOf(extentTree, capacity + 1);
		extents = nextExtents;
		countTree = nextCounts;
		extentTree = nextSums;
	}

	private static int checkExtent(final int extent) {
		if (extent < 0) {
			throw new IllegalArgumentException("negative row extent " + extent);
		}
		return extent;
	}
}
