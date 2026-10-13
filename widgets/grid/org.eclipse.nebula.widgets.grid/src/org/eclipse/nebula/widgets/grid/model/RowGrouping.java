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
 * Packed group index over a {@link RowPermutation}: rows of the permutation's view are grouped
 * by an {@code int} key per model row; the display shows, per group, one header row followed by
 * the group's rows when the group is expanded.
 *
 * <p>Groups are numbered by first appearance in the source view, and rows keep their source
 * view order inside a group, so a sort of the source orders rows within groups and, when the
 * source is sorted by the group key, orders the groups. All state is {@code int[]} plus a
 * collapsed bit lane per group; expand/collapse of many groups is a range bit operation and a
 * prefix rebuild over groups, never over rows. Aggregates run over packed {@code int[]} columns
 * into caller arrays.</p>
 *
 * <p>The grouping rebuilds itself lazily when the source permutation's version changes. Pure
 * Java, GWT-safe (no {@code int} multiplication in hashing, no overflow reliance).</p>
 *
 * @since 1.2
 */
public final class RowGrouping implements RowView {

	private static final int VERSION_MASK = 0x7fffffff;
	private static final int[] NO_INTS = new int[0];

	private final RowPermutation source;
	private int[] keys;
	private int sourceVersion = -1;
	private int version;

	private int groupCount;
	private int rowCount;
	private int[] groupKey = NO_INTS;
	private int[] groupStart = new int[1];
	private int[] leafRows = NO_INTS;
	private int[] displayStart = new int[1];
	private int[] leafPos = NO_INTS;
	private boolean leafPosValid;
	private final RowBitLanes collapsed = new RowBitLanes(1);

	private int[] tableKeys = NO_INTS;
	private int[] tableGroups = NO_INTS;
	private int[] rowGroup = NO_INTS;
	private int[] collapsedKeys = NO_INTS;

	/**
	 * @param source rows to group
	 */
	public RowGrouping(final RowPermutation source) {
		if (source == null) {
			throw new NullPointerException("source");
		}
		this.source = source;
	}

	/**
	 * @return the grouped permutation
	 */
	public RowPermutation getSource() {
		return source;
	}

	/**
	 * Groups by {@code keys[modelRow]}. All groups start expanded.
	 *
	 * @param groupKeys one key per model row ({@code length >= getModelCount()}); kept by
	 *                  reference and re-read on every rebuild
	 */
	public void groupBy(final int[] groupKeys) {
		if (groupKeys.length < source.getModelCount()) {
			throw new IllegalArgumentException("keys.length=" + groupKeys.length + " < " + source.getModelCount());
		}
		keys = groupKeys;
		sourceVersion = -1;
		ensure();
		collapsed.setRowCount(0);
		collapsed.setRowCount(groupCount);
		rebuildDisplay();
	}

	/**
	 * Removes the grouping: the display equals the source view.
	 */
	public void clear() {
		keys = null;
		sourceVersion = -1;
		ensure();
	}

	/**
	 * @return {@code true} if a group key column is set
	 */
	public boolean isGrouped() {
		return keys != null;
	}

	@Override
	public int getModelCount() {
		return source.getModelCount();
	}

	@Override
	public int getViewCount() {
		ensure();
		return keys == null ? rowCount : displayStart[groupCount];
	}

	/**
	 * @return number of groups
	 */
	public int getGroupCount() {
		ensure();
		return groupCount;
	}

	/**
	 * @param displayIndex a displayed row
	 * @return {@code true} if it is a group header
	 */
	public boolean isGroupHeader(final int displayIndex) {
		ensure();
		if (keys == null) {
			checkIndex(displayIndex, rowCount);
			return false;
		}
		final int g = groupAt(displayIndex);
		return displayIndex == displayStart[g];
	}

	/**
	 * @param displayIndex a displayed row
	 * @return the group of the header or row shown there, {@code -1} when not grouped
	 */
	public int groupOf(final int displayIndex) {
		ensure();
		if (keys == null) {
			checkIndex(displayIndex, rowCount);
			return -1;
		}
		return groupAt(displayIndex);
	}

	@Override
	public int modelRow(final int displayIndex) {
		ensure();
		if (keys == null) {
			return source.modelRow(displayIndex);
		}
		final int g = groupAt(displayIndex);
		final int offset = displayIndex - displayStart[g];
		return offset == 0 ? -1 : leafRows[groupStart[g] + offset - 1];
	}

	@Override
	public int viewIndex(final int modelRow) {
		ensure();
		if (keys == null) {
			return source.viewIndex(modelRow);
		}
		checkIndex(modelRow, source.getModelCount());
		ensureLeafPositions();
		final int pos = leafPos[modelRow];
		if (pos < 0) {
			return -1;
		}
		final int g = rowGroup[pos];
		if (collapsed.get(0, g)) {
			return -1;
		}
		return displayStart[g] + 1 + (pos - groupStart[g]);
	}

	/**
	 * @param group a group
	 * @return the displayed row of the group's header
	 */
	public int headerIndex(final int group) {
		ensure();
		checkIndex(group, groupCount);
		return displayStart[group];
	}

	/**
	 * @param group a group
	 * @return the group's key
	 */
	public int getGroupKey(final int group) {
		ensure();
		checkIndex(group, groupCount);
		return groupKey[group];
	}

	/**
	 * @param group a group
	 * @return number of rows in the group (filtered rows excluded)
	 */
	public int getGroupSize(final int group) {
		ensure();
		checkIndex(group, groupCount);
		return groupStart[group + 1] - groupStart[group];
	}

	/**
	 * @param group a group
	 * @return {@code true} if the group's rows are displayed
	 */
	public boolean isExpanded(final int group) {
		ensure();
		checkIndex(group, groupCount);
		return !collapsed.get(0, group);
	}

	/**
	 * @param group    a group
	 * @param expanded {@code true} to display its rows
	 */
	public void setExpanded(final int group, final boolean expanded) {
		ensure();
		checkIndex(group, groupCount);
		collapsed.set(0, group, !expanded);
		rebuildDisplay();
	}

	/**
	 * Expands or collapses groups {@code [from, to)} with one range bit operation.
	 *
	 * @param from     first group
	 * @param to       end group (exclusive)
	 * @param expanded {@code true} to display their rows
	 */
	public void setExpanded(final int from, final int to, final boolean expanded) {
		ensure();
		collapsed.setRange(0, from, to, !expanded);
		rebuildDisplay();
	}

	/**
	 * @return number of collapsed groups ({@code bitCount} over the collapsed lane)
	 */
	public int getCollapsedCount() {
		ensure();
		return collapsed.cardinality(0);
	}

	/**
	 * Aggregates a packed column per group over all rows of each group (collapsed or not). Each
	 * output array may be {@code null}; non-null arrays need {@code getGroupCount()} slots.
	 *
	 * @param column one value per model row
	 * @param sum    per-group sum
	 * @param min    per-group minimum ({@code Integer.MAX_VALUE} for an empty group)
	 * @param max    per-group maximum ({@code Integer.MIN_VALUE} for an empty group)
	 */
	public void aggregate(final int[] column, final long[] sum, final int[] min, final int[] max) {
		ensure();
		for (int g = 0; g < groupCount; g++) {
			long s = 0L;
			int lo = Integer.MAX_VALUE;
			int hi = Integer.MIN_VALUE;
			for (int p = groupStart[g], end = groupStart[g + 1]; p < end; p++) {
				final int value = column[leafRows[p]];
				s += value;
				lo = Math.min(lo, value);
				hi = Math.max(hi, value);
			}
			if (sum != null) {
				sum[g] = s;
			}
			if (min != null) {
				min[g] = lo;
			}
			if (max != null) {
				max[g] = hi;
			}
		}
	}

	@Override
	public int getVersion() {
		ensure();
		return version;
	}

	private void ensure() {
		final int v = source.getVersion();
		if (v == sourceVersion) {
			return;
		}
		sourceVersion = v;
		rowCount = source.getViewCount();
		leafPosValid = false;
		if (keys == null) {
			groupCount = 0;
			displayStart[0] = 0;
		} else {
			// collapsed state follows the group key across a rebuild (group ids may renumber)
			final int collapsedCount = collapsed.cardinality(0);
			if (collapsedKeys.length < collapsedCount) {
				collapsedKeys = new int[collapsedCount];
			}
			int k = 0;
			for (int g = collapsed.nextSetRow(0, 0); g >= 0; g = collapsed.nextSetRow(0, g + 1)) {
				collapsedKeys[k++] = groupKey[g];
			}
			build();
			collapsed.setRowCount(0);
			collapsed.setRowCount(groupCount);
			for (int i = 0; i < k; i++) {
				final int g = groupOfKey(collapsedKeys[i]);
				if (g >= 0) {
					collapsed.set(0, g, true);
				}
			}
			rebuildDisplay();
		}
		version = (version + 1) & VERSION_MASK;
	}

	/** Dense group ids by first appearance, then a stable counting placement of the rows. */
	private void build() {
		final int n = rowCount;
		if (rowGroup.length < n) {
			rowGroup = new int[n];
		}
		if (leafRows.length < n) {
			leafRows = new int[n];
		}
		final int capacity = tableCapacity(n);
		if (tableKeys.length < capacity) {
			tableKeys = new int[capacity];
			tableGroups = new int[capacity];
		}
		Arrays.fill(tableGroups, 0, capacity, -1);
		final int mask = capacity - 1;
		groupCount = 0;
		for (int v = 0; v < n; v++) {
			final int key = keys[source.modelRow(v)];
			int h = mix(key) & mask;
			while (tableGroups[h] >= 0 && tableKeys[h] != key) {
				h = (h + 1) & mask;
			}
			if (tableGroups[h] < 0) {
				tableKeys[h] = key;
				tableGroups[h] = groupCount;
				if (groupKey.length <= groupCount) {
					groupKey = Arrays.copyOf(groupKey, Math.max(16, groupKey.length * 2));
				}
				groupKey[groupCount++] = key;
			}
			rowGroup[v] = tableGroups[h];
		}
		if (groupStart.length < groupCount + 1) {
			groupStart = new int[groupCount + 1];
			displayStart = new int[groupCount + 1];
		}
		Arrays.fill(groupStart, 0, groupCount + 1, 0);
		for (int v = 0; v < n; v++) {
			groupStart[rowGroup[v] + 1]++;
		}
		for (int g = 0; g < groupCount; g++) {
			groupStart[g + 1] += groupStart[g];
		}
		// placement: displayStart doubles as the per-group fill cursor, rebuilt afterwards
		System.arraycopy(groupStart, 0, displayStart, 0, groupCount + 1);
		for (int v = 0; v < n; v++) {
			final int g = rowGroup[v];
			leafRows[displayStart[g]++] = source.modelRow(v);
		}
		// rowGroup is re-keyed by leaf position for viewIndex()
		for (int g = 0; g < groupCount; g++) {
			Arrays.fill(rowGroup, groupStart[g], groupStart[g + 1], g);
		}
	}

	private void rebuildDisplay() {
		if (keys == null) {
			return;
		}
		displayStart[0] = 0;
		for (int g = 0; g < groupCount; g++) {
			final int rows = collapsed.get(0, g) ? 0 : groupStart[g + 1] - groupStart[g];
			displayStart[g + 1] = displayStart[g] + 1 + rows;
		}
		version = (version + 1) & VERSION_MASK;
	}

	private void ensureLeafPositions() {
		if (leafPosValid) {
			return;
		}
		final int models = source.getModelCount();
		if (leafPos.length < models) {
			leafPos = new int[models];
		}
		Arrays.fill(leafPos, 0, models, -1);
		for (int p = 0; p < rowCount; p++) {
			leafPos[leafRows[p]] = p;
		}
		leafPosValid = true;
	}

	/** Group whose display range holds {@code displayIndex} (binary search over groups). */
	private int groupAt(final int displayIndex) {
		checkIndex(displayIndex, displayStart[groupCount]);
		int lo = 0;
		int hi = groupCount - 1;
		while (lo < hi) {
			final int mid = lo + ((hi - lo + 1) >> 1);
			if (displayStart[mid] <= displayIndex) {
				lo = mid;
			} else {
				hi = mid - 1;
			}
		}
		return lo;
	}

	private int groupOfKey(final int key) {
		if (tableKeys.length == 0) {
			return -1;
		}
		final int mask = tableCapacity(rowCount) - 1;
		int h = mix(key) & mask;
		while (tableGroups[h] >= 0) {
			if (tableKeys[h] == key) {
				return tableGroups[h];
			}
			h = (h + 1) & mask;
		}
		return -1;
	}

	private static int tableCapacity(final int rows) {
		int c = 16;
		while (c < rows * 2 && c < (1 << 30)) {
			c <<= 1;
		}
		return c;
	}

	/** Shift/xor mix: no multiplication, so GWT and the JVM agree without overflow. */
	private static int mix(final int key) {
		int h = key ^ (key >>> 16);
		h ^= h >>> 7;
		h ^= h << 9;
		return h ^ (h >>> 13);
	}

	private static void checkIndex(final int index, final int size) {
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
		}
	}
}
