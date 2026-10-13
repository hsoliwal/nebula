/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

/**
 * Sparse mutation-driven index for row spans owned by the built-in Grid data visualizer.
 *
 * <p>The index deliberately retains only items that own at least one positive row span.
 * Its sorted projection is rebuilt from those sparse owners after a mutation. Paint-time
 * lookup is logarithmic in the number of span-bearing rows and never walks cold logical
 * rows merely to discover whether they span into the viewport.</p>
 */
final class GridRowSpanIndex {

	private GridItem[] owners = new GridItem[4];
	private int[][] spansByColumn = new int[4][];
	private int size;

	private int[] starts = new int[4];
	private long[] ends = new long[4];
	private long[] prefixMaxEnds = new long[4];
	private int projectedCount;
	private boolean projectionDirty = true;
	private boolean[] projectedVisibleColumns = new boolean[0];

	void update(GridItem item, int column, int span) {
		if (item == null || column < 0) return;
		int slot = indexOf(item);
		int value = Math.max(0, span);
		if (slot < 0) {
			if (value == 0) return;
			slot = size;
			ensureOwnerCapacity(size + 1);
			owners[slot] = item;
			spansByColumn[slot] = new int[Math.max(1, column + 1)];
			size++;
		} else if (column >= spansByColumn[slot].length) {
			int[] old = spansByColumn[slot];
			int[] next = new int[Math.max(column + 1, old.length * 2)];
			System.arraycopy(old, 0, next, 0, old.length);
			spansByColumn[slot] = next;
		}

		spansByColumn[slot][column] = value;
		if (value == 0 && maxSpan(spansByColumn[slot]) == 0) removeAt(slot);
		projectionDirty = true;
	}

	void remove(GridItem item) {
		int slot = indexOf(item);
		if (slot >= 0) {
			removeAt(slot);
			projectionDirty = true;
		}
	}

	void rowsChanged() {
		projectionDirty = true;
	}

	void columnInserted(int column) {
		if (column < 0) return;
		for (int slot = 0; slot < size; slot++) {
			int[] old = spansByColumn[slot];
			if (old == null || column > old.length) continue;
			int[] next = new int[old.length + 1];
			System.arraycopy(old, 0, next, 0, Math.min(column, old.length));
			if (column < old.length) {
				System.arraycopy(old, column, next, column + 1, old.length - column);
			}
			spansByColumn[slot] = next;
		}
		projectionDirty = true;
	}

	void columnRemoved(int column) {
		if (column < 0) return;
		for (int slot = size - 1; slot >= 0; slot--) {
			int[] old = spansByColumn[slot];
			if (old == null || column >= old.length) continue;
			if (old.length == 1) {
				removeAt(slot);
				continue;
			}
			int[] next = new int[old.length - 1];
			System.arraycopy(old, 0, next, 0, column);
			if (column < next.length) {
				System.arraycopy(old, column + 1, next, column, next.length - column);
			}
			if (maxSpan(next) == 0) removeAt(slot);
			else spansByColumn[slot] = next;
		}
		projectionDirty = true;
	}

	int earliestCovering(int firstVisible, boolean[] visibleColumns) {
		if (firstVisible <= 0 || size == 0) return Math.max(0, firstVisible);
		rebuildProjection(visibleColumns);
		if (projectedCount == 0) return firstVisible;

		return earliestCovering(firstVisible, starts, prefixMaxEnds, projectedCount);
	}

	static int earliestCovering(int firstVisible, int[] starts, long[] prefixMaxEnds, int count) {
		if (firstVisible <= 0 || count <= 0) return Math.max(0, firstVisible);
		int hi = upperBound(starts, count, firstVisible - 1) - 1;
		if (hi < 0 || prefixMaxEnds[hi] < firstVisible) return firstVisible;

		int low = 0;
		int high = hi;
		while (low < high) {
			int mid = (low + high) >>> 1;
			if (prefixMaxEnds[mid] >= firstVisible) high = mid;
			else low = mid + 1;
		}
		return starts[low];
	}

	int sparseOwnerCount() {
		return size;
	}

	boolean owns(GridItem item) {
		return size > 0 && indexOf(item) >= 0;
	}

	void clear() {
		for (int i = 0; i < size; i++) {
			owners[i] = null;
			spansByColumn[i] = null;
		}
		size = 0;
		projectedCount = 0;
		projectionDirty = true;
	}

	private void rebuildProjection(boolean[] visibleColumns) {
		if (!projectionDirty && sameVisibility(visibleColumns)) return;
		ensureProjectionCapacity(size);
		projectedCount = 0;

		for (int slot = 0; slot < size; slot++) {
			GridItem item = owners[slot];
			if (item == null || item.isDisposed()) continue;
			int span = maxSpan(spansByColumn[slot], visibleColumns);
			if (span <= 0) continue;
			int row = item.getRowIndex();
			if (row < 0) continue;
			starts[projectedCount] = row;
			ends[projectedCount] = (long) row + span;
			projectedCount++;
		}

		// Sparse insertion sort avoids boxing and is cheap for the expected small frontier.
		for (int i = 1; i < projectedCount; i++) {
			int start = starts[i];
			long end = ends[i];
			int j = i - 1;
			while (j >= 0 && starts[j] > start) {
				starts[j + 1] = starts[j];
				ends[j + 1] = ends[j];
				j--;
			}
			starts[j + 1] = start;
			ends[j + 1] = end;
		}

		long max = Long.MIN_VALUE;
		for (int i = 0; i < projectedCount; i++) {
			max = Math.max(max, ends[i]);
			prefixMaxEnds[i] = max;
		}
		projectionDirty = false;
		projectedVisibleColumns = copyVisibility(visibleColumns);
	}

	private int indexOf(GridItem item) {
		for (int i = 0; i < size; i++) if (owners[i] == item) return i;
		return -1;
	}

	private void removeAt(int slot) {
		int move = size - slot - 1;
		if (move > 0) {
			System.arraycopy(owners, slot + 1, owners, slot, move);
			System.arraycopy(spansByColumn, slot + 1, spansByColumn, slot, move);
		}
		owners[--size] = null;
		spansByColumn[size] = null;
	}

	private static int maxSpan(int[] spans) {
		return maxSpan(spans, null);
	}

	private static int maxSpan(int[] spans, boolean[] visibleColumns) {
		int max = 0;
		if (spans != null) {
			int limit = visibleColumns == null ? spans.length : Math.min(spans.length, visibleColumns.length);
			for (int column = 0; column < limit; column++) {
				if (visibleColumns == null || visibleColumns[column]) max = Math.max(max, spans[column]);
			}
		}
		return max;
	}

	private boolean sameVisibility(boolean[] visibleColumns) {
		if (visibleColumns == null) return projectedVisibleColumns.length == 0;
		if (projectedVisibleColumns.length != visibleColumns.length) return false;
		for (int i = 0; i < visibleColumns.length; i++) {
			if (projectedVisibleColumns[i] != visibleColumns[i]) return false;
		}
		return true;
	}

	private static boolean[] copyVisibility(boolean[] visibleColumns) {
		if (visibleColumns == null || visibleColumns.length == 0) return new boolean[0];
		boolean[] copy = new boolean[visibleColumns.length];
		System.arraycopy(visibleColumns, 0, copy, 0, visibleColumns.length);
		return copy;
	}

	private void ensureOwnerCapacity(int required) {
		if (required <= owners.length) return;
		int nextCapacity = Math.max(required, owners.length * 2);
		GridItem[] nextOwners = new GridItem[nextCapacity];
		int[][] nextSpans = new int[nextCapacity][];
		System.arraycopy(owners, 0, nextOwners, 0, size);
		System.arraycopy(spansByColumn, 0, nextSpans, 0, size);
		owners = nextOwners;
		spansByColumn = nextSpans;
	}

	private void ensureProjectionCapacity(int required) {
		if (required <= starts.length) return;
		int nextCapacity = Math.max(required, starts.length * 2);
		int[] nextStarts = new int[nextCapacity];
		long[] nextEnds = new long[nextCapacity];
		long[] nextPrefix = new long[nextCapacity];
		System.arraycopy(starts, 0, nextStarts, 0, projectedCount);
		System.arraycopy(ends, 0, nextEnds, 0, projectedCount);
		System.arraycopy(prefixMaxEnds, 0, nextPrefix, 0, projectedCount);
		starts = nextStarts;
		ends = nextEnds;
		prefixMaxEnds = nextPrefix;
	}

	private static int upperBound(int[] values, int length, int key) {
		int low = 0;
		int high = length;
		while (low < high) {
			int mid = (low + high) >>> 1;
			if (values[mid] <= key) low = mid + 1;
			else high = mid;
		}
		return low;
	}
}
