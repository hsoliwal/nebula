/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.util.Arrays;
import java.util.List;

/**
 * Column geometry of a Grid in display order (leaf G6, the viewport mode). Lanes per display
 * position: the column, its width when visible (0 when hidden), the prefix sums of those widths,
 * whether it is visible, and how many pinned columns (visible and fixed) come before it; and the
 * prefix sums of the pinned widths. A change of one column's width, visibility or fixed state and a
 * change of the order are applied as deltas ({@link #columnChanged}, {@link #reordered}); the lanes
 * are rebuilt in O(c) only after a change of the column set ({@code invalidate()}). The column
 * at an x, the x of a column, the first and last column on screen and the columns a paint covers
 * are then binary searches or lookups (O(log c), O(1) or O(log c + k)) instead of a walk over
 * every column.
 * <p>
 * Each answer equals the linear scan it replaces ({@code GridViewportProjection} and Grid's column
 * loops; GridColumnMetrics_Test compares them under seeded scripts). A negative width (column
 * scrolling in a client area narrower than the row header) breaks the prefix order, so
 * {@code ready(List)} then leaves the answers to those scans.
 * </p>
 */
final class GridColumnMetrics {
	private boolean valid;
	private boolean exact;
	private int count;
	private GridColumn[] columns = new GridColumn[0];
	private int[] widths = new int[0];
	private long[] prefix = new long[1];
	private boolean[] visible = new boolean[0];
	private int[] pinnedBefore = new int[1];
	private int[] pinned = new int[0];
	private long[] pinnedPrefix = new long[1];
	private int pinnedCount;
	/* the display position of the first visible column that is not fixed (count if none) */
	private int fixedRun;
	/* display position by GridColumn.index (the column's place in the creation order) */
	private int[] positionOfIndex = new int[0];
	/* the fixed state per display position, as last read */
	private boolean[] fixed = new boolean[0];

	/* brief rule 24, viewport deltas: rebuilds from every column after the first build */
	private boolean built;
	private int fullReplans;

	/** The column set changed (a column added or removed): the lanes are rebuilt when next read. */
	void invalidate() {
		valid = false;
	}

	/** @return rebuilds of every lane after the first build (a column added or removed) */
	int fullReplans() {
		return fullReplans;
	}

	/**
	 * One column changed width, visibility or fixed state (a column resize, hide or show, a group
	 * expanded or collapsed, detail or summary): only that column is read again, the offsets after
	 * it move by the change of its width (O(columns after it), no other column read), and the pinned
	 * lanes follow from the lanes when its pinned share changed (brief rule 24, viewport deltas).
	 *
	 * @param column the column that changed
	 */
	void columnChanged(final GridColumn column) {
		if (!valid) {
			return;
		}
		final int position = positionOf(column);
		if (position < 0) {
			valid = false;
			return;
		}
		final boolean shown = column.isVisible();
		final int width = shown ? column.getWidth() : 0;
		final boolean fixedNow = column.isFixed();
		if (width < 0 || !exact) {
			// a negative width leaves the answers to the linear scans: rebuild when next read
			valid = false;
			return;
		}
		final int delta = width - widths[position];
		final boolean pinnedShare = visible[position] != shown || fixed[position] != fixedNow
				|| delta != 0 && shown && fixedNow;
		widths[position] = width;
		visible[position] = shown;
		fixed[position] = fixedNow;
		if (delta != 0) {
			for (int i = position + 1; i <= count; i++) {
				prefix[i] += delta;
			}
		}
		if (pinnedShare) {
			replanPinned();
		}
	}

	/**
	 * The display order changed (setColumnOrder, a header drag): the run of positions whose column
	 * changed is found from both ends (two pointers) and only its columns are read again; the
	 * offsets before and after the run keep their values (a permutation of the run keeps its sum).
	 *
	 * @param displayOrdered the Grid's columns in their new display order
	 */
	void reordered(final List<GridColumn> displayOrdered) {
		if (!valid || displayOrdered.size() != count) {
			valid = false;
			return;
		}
		int first = 0;
		while (first < count && columns[first] == displayOrdered.get(first)) {
			first++;
		}
		if (first == count) {
			return;
		}
		int last = count - 1;
		while (columns[last] == displayOrdered.get(last)) {
			last--;
		}
		for (int i = first; i <= last; i++) {
			final GridColumn column = displayOrdered.get(i);
			final boolean shown = column.isVisible();
			final int width = shown ? column.getWidth() : 0;
			if (width < 0) {
				valid = false;
				return;
			}
			columns[i] = column;
			visible[i] = shown;
			widths[i] = width;
			fixed[i] = column.isFixed();
			prefix[i + 1] = prefix[i] + width;
			if (column.index >= 0 && column.index < positionOfIndex.length) {
				positionOfIndex[column.index] = i;
			} else {
				valid = false;
				return;
			}
		}
		replanPinned();
	}

	/* the pinned lanes and the first visible scrolled column, from the lanes alone (O(columns)) */
	private void replanPinned() {
		fixedRun = -1;
		pinnedCount = 0;
		long pinnedSum = 0;
		for (int i = 0; i < count; i++) {
			pinnedBefore[i] = pinnedCount;
			if (visible[i] && fixed[i]) {
				pinned[pinnedCount] = i;
				pinnedPrefix[pinnedCount++] = pinnedSum;
				pinnedSum += widths[i];
			} else if (visible[i] && fixedRun < 0) {
				fixedRun = i;
			}
		}
		pinnedBefore[count] = pinnedCount;
		pinnedPrefix[pinnedCount] = pinnedSum;
		if (fixedRun < 0) {
			fixedRun = count;
		}
	}

	/**
	 * @param displayOrdered the Grid's columns in display order
	 * @return {@code true} if the lanes answer exactly (rebuilt first when stale)
	 */
	boolean ready(final List<GridColumn> displayOrdered) {
		if (!valid) {
			rebuild(displayOrdered);
		}
		return exact;
	}

	private void rebuild(final List<GridColumn> displayOrdered) {
		if (built) {
			fullReplans++;
		}
		built = true;
		final int newCount = displayOrdered.size();
		if (newCount < count) {
			// Keep capacity, but release references removed from the occupied prefix.
			Arrays.fill(columns, newCount, count, null);
		}
		count = newCount;
		if (columns.length < count) {
			columns = new GridColumn[count];
			widths = new int[count];
			visible = new boolean[count];
			fixed = new boolean[count];
			pinned = new int[count];
			prefix = new long[count + 1];
			pinnedBefore = new int[count + 1];
			pinnedPrefix = new long[count + 1];
		}
		exact = true;
		fixedRun = -1;
		pinnedCount = 0;
		long sum = 0;
		long pinnedSum = 0;
		int maxIndex = -1;
		for (int i = 0; i < count; i++) {
			final GridColumn column = displayOrdered.get(i);
			final boolean shown = column.isVisible();
			final int width = shown ? column.getWidth() : 0;
			exact &= width >= 0;
			columns[i] = column;
			visible[i] = shown;
			widths[i] = width;
			prefix[i] = sum;
			fixed[i] = column.isFixed();
			pinnedBefore[i] = pinnedCount;
			sum += width;
			if (shown && column.isFixed()) {
				pinned[pinnedCount] = i;
				pinnedPrefix[pinnedCount++] = pinnedSum;
				pinnedSum += width;
			} else if (shown && fixedRun < 0) {
				fixedRun = i;
			}
			maxIndex = Math.max(maxIndex, column.index);
		}
		prefix[count] = sum;
		pinnedBefore[count] = pinnedCount;
		pinnedPrefix[pinnedCount] = pinnedSum;
		if (fixedRun < 0) {
			fixedRun = count;
		}
		if (positionOfIndex.length < maxIndex + 1) {
			positionOfIndex = new int[maxIndex + 1];
		}
		Arrays.fill(positionOfIndex, -1);
		for (int i = 0; i < count; i++) {
			final int index = columns[i].index;
			if (index >= 0) {
				positionOfIndex[index] = i;
			}
		}
		valid = true;
	}

	/** @return the display position of {@code column}, or -1 (as {@code List.indexOf}) */
	int positionOf(final GridColumn column) {
		if (column == null) {
			return -1;
		}
		final int index = column.index;
		final int position = index >= 0 && index < positionOfIndex.length ? positionOfIndex[index] : -1;
		return position >= 0 && columns[position] == column ? position : -1;
	}

	/** @return whether the column at a display position is visible */
	boolean visibleAt(final int position) {
		return visible[position];
	}

	/**
	 * As {@link GridViewportProjection#columnAt}: the column under {@code pointX}, the fixed overlay
	 * first.
	 */
	GridColumn columnAt(final int pointX, final boolean rowHeaderVisible, final int rowHeaderWidth,
			final boolean fixedOverlayActive, final int horizontalSelectionPixels) {
		int base = 0;
		if (rowHeaderVisible) {
			if (pointX <= rowHeaderWidth) {
				return null;
			}
			base = rowHeaderWidth;
		}
		if (fixedOverlayActive) {
			// the overlay is the run of visible fixed columns before the first visible other one
			final long p = (long) pointX - base;
			if (p >= 0 && p < prefix[fixedRun]) {
				return columns[firstEndingAfter(p)];
			}
		}
		final long p = (long) pointX - base + horizontalSelectionPixels;
		return p >= 0 && p < prefix[count] ? columns[firstEndingAfter(p)] : null;
	}

	/** As {@link GridViewportProjection#startColumnIndex}. */
	int startColumnIndex(final int horizontalSelectionPixels) {
		final long h = Math.max(horizontalSelectionPixels, 0);
		return h < prefix[count] ? firstEndingAfter(h) : count;
	}

	/** As {@link GridViewportProjection#endColumnIndex}. */
	int endColumnIndex(final int startIndex, final int x, final int clientWidth) {
		final long target = (long) clientWidth - x;
		if (prefix[count] == 0 || target <= 0) {
			// no column with a width, or the first one already starts right of the client area
			return startIndex - 1;
		}
		// the scan stops after the first column whose right edge reaches the client width; that
		// column, or else the last one with a width, is the last it records if any
		final int last = prefix[count] >= target ? firstReaching(target) : firstReaching(prefix[count]);
		return last >= startIndex && x + prefix[last + 1] > 0 ? last : startIndex - 1;
	}

	/** As {@link GridViewportProjection#visibleColumns(List, int, int, int, boolean)}. */
	GridColumn[] visibleColumns(final int horizontalSelectionPixels, final int viewportLeft, final int viewportRight,
			final boolean fixedOverlayActive) {
		if (viewportRight <= viewportLeft || count == 0) {
			return new GridColumn[0];
		}
		final long span = (long) viewportRight - viewportLeft;
		final long fixedWidth = fixedOverlayActive ? pinnedPrefix[pinnedCount] : 0;
		// scrolled columns: right edge past the body's left (behind the overlay), left edge before
		// the viewport's right
		final long left = horizontalSelectionPixels + fixedWidth;
		final long right = span + horizontalSelectionPixels;
		final int from = left < prefix[count] ? firstEndingAfter(Math.max(left, 0)) : count;
		final int to = firstStartingAtOrAfter(right);
		final GridColumn[] result = new GridColumn[Math.max(0, to - from) + (fixedOverlayActive ? pinnedCount : 0)];
		int n = 0;
		int k = 0;
		for (int i = from; i < to; i++) {
			if (fixedOverlayActive) {
				// pinned columns before this one, in display order
				for (; k < pinnedCount && pinned[k] < i; k++) {
					n = addPinned(result, n, k, span);
				}
				if (k < pinnedCount && pinned[k] == i) {
					n = addPinned(result, n, k++, span);
					continue;
				}
			}
			if (widths[i] > 0) {
				result[n++] = columns[i];
			}
		}
		if (fixedOverlayActive) {
			for (; k < pinnedCount; k++) {
				n = addPinned(result, n, k, span);
			}
		}
		return n == result.length ? result : Arrays.copyOf(result, n);
	}

	/* a pinned column paints at the viewport's left plus the pinned widths before it */
	private int addPinned(final GridColumn[] result, final int n, final int k, final long span) {
		final int position = pinned[k];
		if (widths[position] > 0 && pinnedPrefix[k] < span && pinnedPrefix[k + 1] > 0) {
			result[n] = columns[position];
			return n + 1;
		}
		return n;
	}

	/**
	 * As Grid's column header x loop: the x of a visible column, a fixed one counting only the
	 * visible fixed columns before it; -1 for a hidden column.
	 */
	int headerX(final GridColumn column, final int horizontalSelectionPixels, final boolean rowHeaderVisible,
			final int rowHeaderWidth) {
		final int position = positionOf(column);
		if (position < 0 || !visible[position]) {
			return -1;
		}
		long x = rowHeaderVisible ? rowHeaderWidth : 0;
		if (column.isFixed()) {
			x += pinnedPrefix[pinnedBefore[position]];
		} else {
			x += prefix[position] - horizontalSelectionPixels;
		}
		return (int) x;
	}

	/**
	 * As Grid's origin x loop: the x of a column ({@code null}: right of the last), a fixed one
	 * counting only the visible fixed columns before it.
	 */
	int originX(final GridColumn column, final int horizontalSelectionPixels, final boolean rowHeaderVisible,
			final int rowHeaderWidth) {
		long x = rowHeaderVisible ? rowHeaderWidth : 0;
		if (column != null && column.isFixed()) {
			final int position = positionOf(column);
			return (int) (x + pinnedPrefix[pinnedBefore[position < 0 ? count : position]]);
		}
		final int position = column == null ? count : positionOf(column);
		return (int) (x - horizontalSelectionPixels + prefix[position < 0 ? count : position]);
	}

	/** @return the width of the visible fixed columns before the first visible other one */
	int fixedRunWidth() {
		return (int) prefix[fixedRun];
	}

	/* the first display position whose column ends after p (p in [0, prefix[count])) */
	private int firstEndingAfter(final long p) {
		int low = 0;
		int high = count - 1;
		while (low < high) {
			final int mid = (low + high) >>> 1;
			if (prefix[mid + 1] > p) {
				high = mid;
			} else {
				low = mid + 1;
			}
		}
		return low;
	}

	/* the first display position whose column's right edge is at least target (target <= prefix[count]) */
	private int firstReaching(final long target) {
		int low = 0;
		int high = count - 1;
		while (low < high) {
			final int mid = (low + high) >>> 1;
			if (prefix[mid + 1] >= target) {
				high = mid;
			} else {
				low = mid + 1;
			}
		}
		return low;
	}

	/* the first display position whose column starts at or after p (count if none) */
	private int firstStartingAtOrAfter(final long p) {
		int low = 0;
		int high = count;
		while (low < high) {
			final int mid = (low + high) >>> 1;
			if (prefix[mid] >= p) {
				high = mid;
			} else {
				low = mid + 1;
			}
		}
		return low;
	}
}
