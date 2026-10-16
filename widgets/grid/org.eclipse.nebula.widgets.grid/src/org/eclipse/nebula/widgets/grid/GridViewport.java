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

import java.lang.System.Logger.Level;
import java.util.List;

import org.eclipse.nebula.widgets.grid.internal.IScrollBarProxy;
import org.eclipse.nebula.widgets.grid.internal.WidgetMode;
import org.eclipse.swt.SWT;

/**
 * Viewport mediation of a {@link Grid}: answers the Grid's row geometry (top row, rows that fit a
 * height, row at a point, row origin, next and previous visible row) from a {@link GridRowExtents}
 * index instead of walking the items, so a scroll step or a paint visits only the rows on screen.
 * <p>
 * Every answer equals the one of the item walk it replaces, edge cases included; the Grid calls
 * the mediator from one-line hooks and keeps its own code for everything else. The mediator holds
 * no item state of its own: the index is derived from {@link GridItem#isVisible()} and
 * {@link GridItem#getHeight()}, built lazily, updated in O(log n) when one item changes and rebuilt
 * after an insertion in the middle or a removal.
 * </p>
 * <p>
 * Grid is a viewport by default. Start with {@code -Dswt.widget.mode.Grid=original} (or the global
 * {@code -Dswt.widget.mode=original}) for the original code paths; the mode is read once.
 * </p>
 */
final class GridViewport {

	/** Whether Grids mediate their row geometry (the startup mode is {@code viewport}). */
	static final boolean ENABLED = WidgetMode.of("Grid", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT;

	private static final System.Logger LOG = System.getLogger("org.eclipse.nebula.widgets.grid.Grid");

	/**
	 * The last row range answered by {@link #rowRange(int, int)} or
	 * {@link #rowRange(int, int, boolean, boolean)}: start index, end index, rows, height.
	 */
	final int[] range = new int[4];

	private final Grid grid;
	private final List<GridItem> items;
	private final GridRowExtents rows = new GridRowExtents();

	/**
	 * @param grid  the mediated Grid
	 * @param items the Grid's item list, in display (preorder) order
	 */
	GridViewport(final Grid grid, final List<GridItem> items) {
		this.grid = grid;
		this.items = items;
	}

	/**
	 * @param tree whether the Grid has child items
	 * @return the index of the item at the top: the visible item at the vertical scroll selection
	 */
	int getTopIndex(final boolean tree) {
		final IScrollBarProxy bar = grid.getVerticalScrollBarProxy();
		if (!bar.getVisible()) {
			return 0;
		}
		final int selection = bar.getSelection();
		if (!tree) {
			return selection;
		}
		final GridRowExtents index = rows();
		// past the last visible row the selection itself is kept, as the item walk does
		return selection < index.visibleCount() ? items.get(index.positionOfRank(selection)).getRowIndex() : selection;
	}

	/**
	 * @param itemIndex a visible item
	 * @param tree      whether the Grid has child items
	 * @return the vertical scroll selection that shows the item at the top
	 */
	int scrollSelectionOf(final int itemIndex, final boolean tree) {
		return tree ? rows().rankOf(itemIndex) : itemIndex;
	}

	/**
	 * Measures the visible rows from {@code startIndex} to {@code endIndex} into {@link #range}.
	 *
	 * @param startIndex first item, or -1 for the first visible item
	 * @param endIndex   last item, or -1 for the last visible item
	 * @return {@code false} when there is no visible item to start or end with
	 */
	boolean rowRange(int startIndex, int endIndex) {
		final GridRowExtents index = rows();
		final int count = items.size();
		if (startIndex == -1) {
			startIndex = index.nextVisible(0);
			if (startIndex < 0) {
				return false;
			}
		}
		if (endIndex == -1) {
			endIndex = index.previousVisible(count - 1);
			if (endIndex < 0) {
				return false;
			}
		}
		if (startIndex < 0 || endIndex < 0 || startIndex >= count || endIndex >= count || endIndex < startIndex
				|| index.extentAt(startIndex) == 0 || index.extentAt(endIndex) == 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		final int visible = index.rankOf(endIndex + 1) - index.rankOf(startIndex);
		final long extent = index.extentBefore(endIndex + 1) - index.extentBefore(startIndex);
		return answer(startIndex, endIndex, visible, extent - 1);
	}

	/**
	 * Measures into {@link #range} the visible rows that fit {@code availableHeight} pixels, going
	 * down (or up, with {@code inverse}) from {@code startIndex}. The first row is always taken; a
	 * further row is taken while the rows taken so far leave room for its separator and one pixel,
	 * and, with {@code complete}, only when the row fits entirely.
	 *
	 * @param startIndex      first (or, with {@code inverse}, last) item, or -1 for the first (last)
	 *                        visible item
	 * @param availableHeight height in pixels
	 * @param complete        whether the far row must lie completely inside the height
	 * @param inverse         whether to go up from {@code startIndex}
	 * @return {@code false} when there is no visible item to start with
	 */
	boolean rowRange(int startIndex, final int availableHeight, final boolean complete, final boolean inverse) {
		final GridRowExtents index = rows();
		final int count = items.size();
		if (startIndex == -1) {
			startIndex = inverse ? index.previousVisible(count - 1) : index.nextVisible(0);
			if (startIndex < 0) {
				return false;
			}
		}
		if (startIndex < 0 || startIndex >= count || index.extentAt(startIndex) == 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		if (availableHeight <= 0) {
			return answer(startIndex, startIndex, 0, 0);
		}
		final int rank = index.rankOf(startIndex);
		int taken = inverse ? rowsAbove(index, rank, availableHeight) : rowsBelow(index, rank, availableHeight);
		long extent = takenExtent(index, rank, taken, inverse);
		if (complete && taken > 1 && extent > availableHeight + 1L) {
			// the last row taken would stick out by more than its separator
			taken--;
			extent = takenExtent(index, rank, taken, inverse);
		}
		final int other = index.positionOfRank(inverse ? rank + 1 - taken : rank + taken - 1);
		return inverse ? answer(other, startIndex, taken, extent - 1) : answer(startIndex, other, taken, extent - 1);
	}

	/**
	 * @param topIndex item at the top of the body
	 * @param y        offset below the top of the body
	 * @param limit    largest offset at which a row may start (the body height)
	 * @return the visible item whose row, including its separator, holds {@code y}, or {@code null}
	 */
	GridItem itemAt(final int topIndex, final int y, final int limit) {
		if (topIndex >= items.size() || y < 0) {
			return null;
		}
		final GridRowExtents index = rows();
		final long top = index.extentBefore(topIndex);
		final int rank = index.rankAtExtent(top + y);
		if (rank >= index.visibleCount() || index.extentOfRank(rank) - top > limit) {
			return null;
		}
		return items.get(index.positionOfRank(rank));
	}

	/**
	 * @param fromIndex an item index in {@code [0, items]}
	 * @param toIndex   an item index in {@code [0, items]}
	 * @return the pixels from the row of {@code fromIndex} down to the row of {@code toIndex}
	 *         (negative when {@code toIndex} is above)
	 */
	int offsetBetween(final int fromIndex, final int toIndex) {
		final GridRowExtents index = rows();
		return (int) (index.extentBefore(toIndex) - index.extentBefore(fromIndex));
	}

	/**
	 * @param row a hidden item
	 * @return the first visible item after it, or the last item when none is visible
	 */
	int nextVisibleRow(final int row) {
		final int next = rows().nextVisible(row);
		return next < 0 ? items.size() - 1 : next;
	}

	/**
	 * @param position an item index in {@code [0, items]}
	 * @return the first visible item at or after it, or {@code null}
	 */
	GridItem nextVisibleItem(final int position) {
		if (position == items.size()) {
			return null;
		}
		final GridItem item = items.get(position);
		if (item.isVisible()) {
			return item;
		}
		final int next = rows().nextVisible(position);
		return next < 0 ? null : items.get(next);
	}

	/**
	 * @param position an item index in {@code [0, items)}
	 * @return the last visible item at or before it, or {@code null}
	 */
	GridItem previousVisibleItem(final int position) {
		final GridItem item = items.get(position);
		if (item.isVisible()) {
			return item;
		}
		final int previous = rows().previousVisible(position);
		return previous < 0 ? null : items.get(previous);
	}

	/**
	 * An item was added to the item list at {@code row}; O(log n) when it was appended.
	 *
	 * @param item the new item
	 * @param row  its index
	 */
	void inserted(final GridItem item, final int row) {
		final int count = items.size();
		if (row == count - 1 && rows.isValid(count - 1) && items.get(row) == item) {
			rows.append(extentOf(item));
		} else {
			rows.invalidate();
		}
	}

	/** Items were removed, reordered or changed in bulk; the index is rebuilt when next needed. */
	void rowsChanged() {
		rows.invalidate();
	}

	/**
	 * The visibility or the height of {@code item} changed; O(log n).
	 *
	 * @param item the item
	 */
	void rowChanged(final GridItem item) {
		final int count = items.size();
		if (!rows.isValid(count)) {
			return;
		}
		final int row = item.knownRow();
		if (row >= 0 && row < count && items.get(row) == item) {
			rows.set(row, extentOf(item));
		} else {
			rows.invalidate();
		}
	}

	private GridRowExtents rows() {
		final int count = items.size();
		if (!rows.isValid(count)) {
			rows.rebuild(count, position -> extentOf(items.get(position)));
			LOG.log(Level.DEBUG, () -> "row extent index rebuilt for " + count + " items");
		}
		return rows;
	}

	private boolean answer(final int startIndex, final int endIndex, final int visible, final long height) {
		range[0] = startIndex;
		range[1] = endIndex;
		range[2] = visible;
		range[3] = (int) height;
		return true;
	}

	/* the smallest number of rows from rank down whose extent reaches the height, or all rows */
	private static int rowsBelow(final GridRowExtents index, final int rank, final int availableHeight) {
		final long base = index.extentOfRank(rank);
		final int last = index.rankAtExtent(base + availableHeight - 1);
		return Math.min(index.visibleCount(), last + 1) - rank;
	}

	/* the smallest number of rows from rank up whose extent reaches the height, or all rows */
	private static int rowsAbove(final GridRowExtents index, final int rank, final int availableHeight) {
		final long limit = index.extentOfRank(rank + 1) - availableHeight;
		return limit < 0 ? rank + 1 : rank + 1 - index.rankAtExtent(limit);
	}

	private static long takenExtent(final GridRowExtents index, final int rank, final int taken, final boolean inverse) {
		return inverse ? index.extentOfRank(rank + 1) - index.extentOfRank(rank + 1 - taken)
				: index.extentOfRank(rank + taken) - index.extentOfRank(rank);
	}

	private static int extentOf(final GridItem item) {
		return item.isVisible() ? item.getHeight() + 1 : 0;
	}
}
