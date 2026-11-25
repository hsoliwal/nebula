/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others.
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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.RandomAccess;

import org.eclipse.nebula.widgets.grid.model.RowSparseBits;

/**
 * The selected rows of a flat {@code SWT.VIRTUAL} Grid in the default (viewport) mode, kept by row
 * index so that selecting rows makes no item: the Grid's selection list with the stock list
 * semantics (selection order, first-occurrence removal) for every caller, without an item or an
 * entry object per selected row.
 * <ul>
 * <li>membership is a {@link RowSparseBits} lane over the rows (run containers for select-all and
 * ranges, arrays for scattered rows): {@code contains}, the paint-time check, is O(log n) and makes
 * no item;</li>
 * <li>the selection order is a list of row runs ({@code starts[i]..ends[i]}, ascending inside a
 * run) in the order they were selected: selectAll is one run, a range selection one run per
 * unselected gap, a ctrl+click one run of one row;</li>
 * <li>rows move with the Grid's rows: an inserted row splits the run it lands in, a removed row
 * leaves its run (and the lane), a truncation drops the rows past the new count, so no entry ever
 * outlives its row;</li>
 * <li>an item is made only when a caller asks for one ({@code get}, {@code toArray}, iteration):
 * {@code getSelection()} still returns the selected items.</li>
 * </ul>
 * A caller that needs what row runs cannot express (the same item twice, an insertion or
 * replacement inside the list, an item that is not bound to its row) or a Grid that leaves the flat
 * virtual shape (a tree, a row permutation or grouping) turns the list into a plain
 * {@code GridSelectionList} of the selected items, in order; {@code clear()} returns to rows.
 */
final class GridRowSelection extends AbstractList<GridItem> implements RandomAccess {

	/** Bound of the lane's positions for its range and shift operations. */
	private static final int LIMIT = Integer.MAX_VALUE - 1;

	private final Grid grid;
	private final RowSparseBits rows = new RowSparseBits();
	private final int[][] scratch = { new int[8] };

	/* row runs in selection order: starts[i]..ends[i] inclusive */
	private int[] starts = new int[4];
	private int[] ends = new int[4];
	private int runs;
	private int count;
	/* whether the runs are also in ascending row order (each starts after the previous ends) */
	private boolean ascending = true;
	/* rows selected before each run, rebuilt on demand */
	private int[] prefix = new int[0];
	private boolean prefixValid;

	/* the item list this selection turned into, or null while it keeps rows */
	private GridSelectionList explicit;

	GridRowSelection(final Grid grid) {
		this.grid = grid;
	}

	// ----------------------------------------------------------------------------------- rows

	/**
	 * @return whether the selection is kept by row (else it is an item list); turns into an item
	 *         list first when the Grid no longer has flat virtual rows
	 */
	boolean lanes() {
		if (explicit != null) {
			if (!explicit.isEmpty() || !grid.rowSelectionLanes()) {
				return false;
			}
			// an empty item list holds nothing rows cannot: back to rows
			explicit = null;
		}
		if (!grid.rowSelectionLanes()) {
			degrade();
			return false;
		}
		return true;
	}

	/** @return whether the selection is kept by row (no validity check, nothing is made) */
	boolean keepsRows() {
		return explicit == null;
	}

	/** Turns the row selection into an item list of the selected items, in order (makes them). */
	void degrade() {
		if (explicit != null) {
			return;
		}
		final GridSelectionList list = new GridSelectionList();
		for (int r = 0; r < runs; r++) {
			for (int row = starts[r]; row <= ends[r]; row++) {
				list.add(grid.sparseItem(row));
			}
		}
		resetRows();
		explicit = list;
	}

	/** @return whether row {@code row} is selected (row mode) */
	boolean isRowSelected(final int row) {
		return row >= 0 && rows.get(row);
	}

	/** Appends row {@code row} unless it is selected. */
	void selectRow(final int row) {
		if (!rows.get(row)) {
			append(row, row);
		}
	}

	/** Appends the unselected rows of {@code [from, to]}, ascending, as the stock row loop does. */
	void selectRows(final int from, final int to) {
		int position = from;
		while (position <= to) {
			final int selected = rows.next(position, to + 1);
			final int gapEnd = selected < 0 ? to : selected - 1;
			if (gapEnd >= position) {
				append(position, gapEnd);
			}
			if (selected < 0) {
				break;
			}
			position = rows.runEnd(selected, to + 1);
		}
	}

	/** Selects the rows {@code [0, rowCount)} only, in row order. */
	void selectAll(final int rowCount) {
		resetRows();
		if (rowCount > 0) {
			append(0, rowCount - 1);
		}
	}

	/** @return whether row {@code row} was selected (it is not any more) */
	boolean deselectRow(final int row) {
		if (row < 0 || !rows.get(row)) {
			return false;
		}
		rows.set(row, false);
		count--;
		final int r = runOf(row);
		if (starts[r] == ends[r]) {
			removeRun(r);
		} else if (row == starts[r]) {
			starts[r]++;
		} else if (row == ends[r]) {
			ends[r]--;
		} else {
			insertRun(r + 1, row + 1, ends[r]);
			ends[r] = row - 1;
		}
		changed();
		return true;
	}

	/** Deselects the rows {@code [from, to]}. */
	void deselectRows(int from, final int to) {
		from = Math.max(0, from);
		if (from > to || count == 0) {
			return;
		}
		final int removed = rows.count(from, to + 1);
		if (removed == 0) {
			return;
		}
		rows.setRange(from, to + 1, false);
		count -= removed;
		cutRuns(from, to);
		changed();
	}

	/** @return the row at position {@code index} of the selection order */
	int rowAt(final int index) {
		if (index < 0 || index >= count) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + count);
		}
		final int[] before = prefix();
		int low = 0;
		int high = runs - 1;
		while (low < high) {
			final int mid = (low + high + 1) >>> 1;
			if (before[mid] <= index) {
				low = mid;
			} else {
				high = mid - 1;
			}
		}
		return starts[low] + index - before[low];
	}

	/** @return the selected rows in selection order, a new array */
	int[] rowsInOrder() {
		final int[] result = new int[count];
		int i = 0;
		for (int r = 0; r < runs; r++) {
			for (int row = starts[r]; row <= ends[r]; row++) {
				result[i++] = row;
			}
		}
		return result;
	}

	// ------------------------------------------------------- the Grid's rows move (row mode only)

	/** A row was inserted at {@code row}: the selected rows at or after it move down by one. */
	void rowInserted(final int row) {
		if (explicit != null || count == 0) {
			return;
		}
		rows.shift(row, 1, LIMIT, scratch);
		for (int r = 0; r < runs; r++) {
			if (starts[r] >= row) {
				starts[r]++;
				ends[r]++;
			} else if (ends[r] >= row) {
				// the new, unselected row splits the run; both parts keep their place in the order
				insertRun(r + 1, row + 1, ends[r] + 1);
				ends[r] = row - 1;
				r++;
			}
		}
		changed();
	}

	/** The row {@code row} was removed: it leaves the selection, the rows after it move up by one. */
	void rowRemoved(final int row) {
		if (explicit != null || count == 0) {
			return;
		}
		if (rows.get(row)) {
			rows.set(row, false);
			count--;
		}
		rows.shift(row + 1, -1, LIMIT, scratch);
		int to = 0;
		for (int r = 0; r < runs; r++) {
			int start = starts[r];
			int end = ends[r];
			if (start > row) {
				start--;
				end--;
			} else if (end >= row) {
				// the run loses the row; its parts before and after it join again, in order
				end--;
			}
			if (start <= end) {
				starts[to] = start;
				ends[to] = end;
				to++;
			}
		}
		runs = to;
		changed();
	}

	/** The rows at and after {@code rowCount} were removed. */
	void rowsTruncated(final int rowCount) {
		if (explicit != null || count == 0) {
			return;
		}
		final int removed = rows.count(rowCount, LIMIT);
		if (removed == 0) {
			return;
		}
		rows.setRange(rowCount, LIMIT, false);
		count -= removed;
		cutRuns(rowCount, LIMIT);
		changed();
	}

	/** Every row was removed. */
	void rowsCleared() {
		if (explicit == null) {
			resetRows();
		}
	}

	// ------------------------------------------------------------------------------ the list

	@Override
	public int size() {
		return explicit != null ? explicit.size() : count;
	}

	@Override
	public GridItem get(final int index) {
		if (explicit != null) {
			return explicit.get(index);
		}
		return grid.sparseItem(rowAt(index));
	}

	@Override
	public boolean contains(final Object o) {
		if (explicit != null) {
			return explicit.contains(o);
		}
		final int row = rowOf(o);
		return row >= 0 && rows.get(row);
	}

	@Override
	public int indexOf(final Object o) {
		if (explicit != null) {
			return explicit.indexOf(o);
		}
		final int row = rowOf(o);
		if (row < 0 || !rows.get(row)) {
			return -1;
		}
		final int r = runOf(row);
		return prefix()[r] + row - starts[r];
	}

	@Override
	public int lastIndexOf(final Object o) {
		return explicit != null ? explicit.lastIndexOf(o) : indexOf(o);
	}

	@Override
	public boolean add(final GridItem item) {
		if (lanes()) {
			final int row = rowOf(item);
			if (row >= 0 && !rows.get(row)) {
				append(row, row);
				return true;
			}
			// the same item twice, or an item that is not its row's: only a list keeps that
			degrade();
		}
		final boolean added = explicit.add(item);
		modCount++;
		return added;
	}

	@Override
	public void add(final int index, final GridItem item) {
		if (index == size()) {
			add(item);
			return;
		}
		degrade();
		explicit.add(index, item);
		modCount++;
	}

	@Override
	public boolean addAll(final Collection<? extends GridItem> c) {
		boolean changed = false;
		for (final GridItem item : c) {
			changed |= add(item);
		}
		return changed;
	}

	@Override
	public GridItem set(final int index, final GridItem item) {
		degrade();
		return explicit.set(index, item);
	}

	@Override
	public boolean remove(final Object o) {
		if (explicit != null) {
			final boolean removed = explicit.remove(o);
			modCount++;
			return removed;
		}
		final int row = rowOf(o);
		return row >= 0 && deselectRow(row);
	}

	@Override
	public GridItem remove(final int index) {
		if (explicit != null) {
			modCount++;
			return explicit.remove(index);
		}
		final int row = rowAt(index);
		final GridItem item = grid.sparseItem(row);
		deselectRow(row);
		return item;
	}

	@Override
	public void clear() {
		if (explicit != null) {
			explicit.clear();
			if (grid.rowSelectionLanes()) {
				explicit = null;
			}
		}
		resetRows();
	}

	@Override
	public Object[] toArray() {
		return toArray(new GridItem[size()]);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(final T[] a) {
		if (explicit != null) {
			return explicit.toArray(a);
		}
		final T[] result = a.length >= count ? a
				: (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), count);
		int i = 0;
		for (int r = 0; r < runs; r++) {
			for (int row = starts[r]; row <= ends[r]; row++) {
				result[i++] = (T) grid.sparseItem(row);
			}
		}
		if (result.length > count) {
			result[count] = null;
		}
		return result;
	}

	// ---------------------------------------------------------------------------- internals

	/* the row of an item of this Grid bound to it, else -1 (no item is made, no widget check) */
	private int rowOf(final Object o) {
		if (!(o instanceof GridItem)) {
			return -1;
		}
		final GridItem item = (GridItem) o;
		if (item.parentGrid() != grid) {
			return -1;
		}
		final int row = item.knownRow();
		return row >= 0 && grid.sparseMaterialized(row) == item ? row : -1;
	}

	/* appends the unselected rows [from, to] as the last part of the order */
	private void append(final int from, final int to) {
		rows.setRange(from, to + 1, true);
		count += to - from + 1;
		if (runs > 0 && ends[runs - 1] == from - 1) {
			ends[runs - 1] = to;
		} else {
			if (runs > 0 && from < starts[runs - 1]) {
				ascending = false;
			}
			insertRun(runs, from, to);
		}
		changed();
	}

	/* the run holding the selected row */
	private int runOf(final int row) {
		if (ascending) {
			int low = 0;
			int high = runs - 1;
			while (low < high) {
				final int mid = (low + high + 1) >>> 1;
				if (starts[mid] <= row) {
					low = mid;
				} else {
					high = mid - 1;
				}
			}
			return low;
		}
		for (int r = 0; r < runs; r++) {
			if (starts[r] <= row && row <= ends[r]) {
				return r;
			}
		}
		throw new IllegalStateException("selected row " + row + " is in no run");
	}

	/*
	 * removes the rows [from, to] from every run, in order; the runs are disjoint, so at most one run
	 * holds rows on both sides of the cut and splits in two
	 */
	private void cutRuns(final int from, final int to) {
		final int[] newStarts = new int[Math.max(4, runs + 1)];
		final int[] newEnds = new int[newStarts.length];
		int n = 0;
		for (int r = 0; r < runs; r++) {
			final int start = starts[r];
			final int end = ends[r];
			if (end < from || start > to) {
				n = put(newStarts, newEnds, n, start, end);
				continue;
			}
			if (start < from) {
				n = put(newStarts, newEnds, n, start, from - 1);
			}
			if (end > to) {
				n = put(newStarts, newEnds, n, to + 1, end);
			}
		}
		starts = newStarts;
		ends = newEnds;
		runs = n;
	}

	private static int put(final int[] s, final int[] e, final int n, final int start, final int end) {
		s[n] = start;
		e[n] = end;
		return n + 1;
	}

	private void insertRun(final int at, final int start, final int end) {
		if (runs == starts.length) {
			starts = Arrays.copyOf(starts, Math.max(4, runs * 2));
			ends = Arrays.copyOf(ends, starts.length);
		}
		System.arraycopy(starts, at, starts, at + 1, runs - at);
		System.arraycopy(ends, at, ends, at + 1, runs - at);
		starts[at] = start;
		ends[at] = end;
		runs++;
	}

	private void removeRun(final int at) {
		System.arraycopy(starts, at + 1, starts, at, runs - at - 1);
		System.arraycopy(ends, at + 1, ends, at, runs - at - 1);
		runs--;
	}

	private int[] prefix() {
		if (!prefixValid) {
			if (prefix.length < runs) {
				prefix = new int[Math.max(runs, prefix.length * 2)];
			}
			int before = 0;
			for (int r = 0; r < runs; r++) {
				prefix[r] = before;
				before += ends[r] - starts[r] + 1;
			}
			prefixValid = true;
		}
		return prefix;
	}

	private void changed() {
		prefixValid = false;
		modCount++;
	}

	private void resetRows() {
		if (count > 0 || runs > 0) {
			rows.setRange(0, LIMIT, false);
		}
		runs = 0;
		count = 0;
		ascending = true;
		changed();
	}

	/** Test/bench view: selection runs and the lane's bytes (package-private). */
	int runCount() {
		return runs;
	}

	int laneBytes() {
		return rows.bytes();
	}

	/** Checks the lane against the runs (tests). */
	void checkInvariants() {
		if (explicit != null) {
			return;
		}
		int total = 0;
		final RowSparseBits seen = new RowSparseBits();
		for (int r = 0; r < runs; r++) {
			if (starts[r] > ends[r]) {
				throw new IllegalStateException("empty run " + r);
			}
			if (ascending && r > 0 && starts[r] <= ends[r - 1]) {
				throw new IllegalStateException("runs not ascending at " + r);
			}
			for (int row = starts[r]; row <= ends[r]; row++) {
				if (seen.get(row) || !rows.get(row)) {
					throw new IllegalStateException("row " + row + " twice or not in the lane");
				}
				seen.set(row, true);
				total++;
			}
		}
		if (total != count || rows.count(0, LIMIT) != count) {
			throw new IllegalStateException("count " + count + ", runs " + total + ", lane " + rows.count(0, LIMIT));
		}
	}
}
