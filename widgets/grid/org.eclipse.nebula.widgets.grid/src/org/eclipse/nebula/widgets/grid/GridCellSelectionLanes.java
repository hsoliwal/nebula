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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Predicate;

import org.eclipse.nebula.widgets.grid.model.RowSparseBits;
import org.eclipse.swt.graphics.Point;

/**
 * The selected cells of a flat {@code SWT.VIRTUAL} Grid in the default (viewport) mode, kept as
 * blocks of rows by columns instead of a {@code Point} per cell: the Grid's cell list, with the
 * list semantics of {@code GridCellSelectionList} (selection order, first-occurrence removal,
 * copied points) for every caller.
 * <ul>
 * <li>membership is one {@link RowSparseBits} lane of rows per column index: {@code contains}, the
 * paint-time check, is O(log n) and makes no Point;</li>
 * <li>the order is a list of blocks in selection order; block {@code b} holds the cells
 * {@code (cols[b][j], row)} for {@code row} from {@code first[b]} to {@code last[b]}, row by row,
 * columns in {@code cols[b]} order (the order of the stock range loops). selectAllCells or a shift
 * range is one block, a ctrl+click a block of one cell, and cells appended next to the last block
 * in its order extend it;</li>
 * <li>a removal only clears the lane, O(log n); the blocks are compacted to the lanes in one sweep
 * when their order is read or a cell is appended, so a cell selected again goes after the others;</li>
 * <li>cells keep their row when rows are inserted or removed, as the list's points do; a removed
 * column drops its cells and the cells right of it move one column left, as the Grid's in-place
 * shift of the points does; a truncation drops the rows past the new count;</li>
 * <li>a {@code Point} is made only when a caller asks for points ({@code get}, iteration,
 * {@code toArray}); no item is made.</li>
 * </ul>
 * What blocks cannot express (the same cell twice, an insertion or replacement inside the list) or
 * a Grid that is no longer flat and virtual without a row view turns the list into a
 * {@code GridCellSelectionList} of the same points, in order; it keeps blocks again once it is
 * cleared while the Grid is flat. Catalogue: ALG-GRID-CELL-BLOCKS (row-range by column-set blocks
 * over per-column row lanes).
 */
final class GridCellSelectionLanes extends AbstractList<Point> implements RandomAccess {

	private static final int LIMIT = Integer.MAX_VALUE - 1;

	private final Grid grid;
	/* the rows selected per column index; null for a column without a selected cell */
	private RowSparseBits[] lanes = new RowSparseBits[0];
	/* the blocks, in selection order; a column array is never changed once it is in a block */
	private int[] first = new int[4];
	private int[] last = new int[4];
	private int[][] cols = new int[4][];
	private int blocks;
	private int count;
	/* whether a block may hold deselected cells (holes): they are compacted away on demand */
	private boolean holes;
	/* the cells before each block, rebuilt on demand */
	private int[] prefix = new int[0];
	private boolean prefixValid;
	/* the point list this selection turned into, or null while it keeps blocks */
	private GridCellSelectionList explicit;

	GridCellSelectionLanes(final Grid grid) {
		this.grid = grid;
	}

	// ----------------------------------------------------------------------------- blocks

	/**
	 * @return whether the cells are kept as blocks of a flat virtual Grid; a list that cannot keep
	 *         them turns into a point list here
	 */
	boolean lanes() {
		if (explicit != null) {
			if (!explicit.isEmpty() || !grid.rowSelectionLanes()) {
				return false;
			}
			explicit = null;
			reset();
		}
		if (!grid.rowSelectionLanes()) {
			degrade();
			return false;
		}
		return true;
	}

	/** @return whether the cells are kept as blocks (the Grid's shape is not checked) */
	boolean keepsBlocks() {
		return explicit == null;
	}

	/** Turns the blocks into a point list of the same cells, in order (this makes the points). */
	void degrade() {
		if (explicit != null) {
			return;
		}
		final GridCellSelectionList list = new GridCellSelectionList();
		compact();
		for (int b = 0; b < blocks; b++) {
			for (int row = first[b]; row <= last[b]; row++) {
				for (final int col : cols[b]) {
					list.add(new Point(col, row));
				}
			}
		}
		reset();
		explicit = list;
	}

	/**
	 * Appends the cells of the rows {@code [from, to]} by the columns {@code columns} (distinct,
	 * non-negative) that are not selected yet, row by row, columns in the given order: the stock
	 * range loop of {@code addToCellSelection} calls, without its checks. Blocks only.
	 */
	void addBlock(final int from, final int to, final int[] columns) {
		compact();
		int row = from;
		while (row <= to) {
			int busy = to + 1;
			for (final int col : columns) {
				final RowSparseBits lane = col < lanes.length ? lanes[col] : null;
				if (lane != null) {
					final int selected = lane.next(row, busy);
					if (selected >= 0) {
						busy = selected;
					}
				}
			}
			if (busy > row) {
				append(row, busy - 1, columns);
				row = busy;
				continue;
			}
			for (final int col : columns) {
				if (col >= lanes.length || lanes[col] == null || !lanes[col].get(row)) {
					append(row, row, new int[] { col });
				}
			}
			row++;
		}
	}

	/**
	 * Deselects the cells of the rows {@code [from, to]} by the columns {@code columns} (distinct).
	 *
	 * @return whether a cell was selected
	 */
	boolean removeBlock(final int from, final int to, final int[] columns) {
		if (explicit != null) {
			boolean removed = false;
			for (int row = Math.max(0, from); row <= to; row++) {
				for (final int col : columns) {
					removed |= explicit.remove(new Point(col, row));
				}
			}
			modCount++;
			return removed;
		}
		int removed = 0;
		for (final int col : columns) {
			final RowSparseBits lane = col >= 0 && col < lanes.length ? lanes[col] : null;
			final int n = lane == null ? 0 : lane.count(Math.max(0, from), to + 1);
			if (n > 0) {
				lane.setRange(Math.max(0, from), to + 1, false);
				removed += n;
			}
		}
		if (removed == 0) {
			return false;
		}
		count -= removed;
		holes = true;
		changed();
		return true;
	}

	/** @return whether a cell of row {@code row} is selected */
	boolean rowHasCells(final int row) {
		if (explicit != null) {
			for (int i = 0; i < explicit.size(); i++) {
				if (explicit.get(i).y == row) {
					return true;
				}
			}
			return false;
		}
		if (row < 0) {
			return false;
		}
		for (final RowSparseBits lane : lanes) {
			if (lane != null && lane.get(row)) {
				return true;
			}
		}
		return false;
	}

	/** @return whether a cell of a row at or after {@code row} is selected (blocks only) */
	boolean hasRowAtOrAfter(final int row) {
		for (final RowSparseBits lane : lanes) {
			if (lane != null && lane.next(Math.max(0, row), LIMIT) >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the rows with a selected cell, each once, in the order of their first cell (blocks
	 *         only): what the stock loops that collect the selected rows from the cells find
	 */
	int[] rowsInOrder() {
		compact();
		final RowSparseBits seen = new RowSparseBits();
		int[] rows = new int[Math.min(count, 1024)];
		int n = 0;
		for (int b = 0; b < blocks; b++) {
			final int limit = last[b] + 1;
			int position = first[b];
			while (position < limit) {
				final int taken = seen.next(position, limit);
				final int gapEnd = taken < 0 ? limit : taken;
				for (int row = position; row < gapEnd; row++) {
					if (n == rows.length) {
						rows = Arrays.copyOf(rows, Math.max(16, n * 2));
					}
					rows[n++] = row;
				}
				if (taken < 0) {
					break;
				}
				position = seen.runEnd(taken, limit);
			}
			seen.setRange(first[b], limit, true);
		}
		return Arrays.copyOf(rows, n);
	}

	/** @return whether a cell of column {@code x} is selected */
	boolean columnHasCells(final int x) {
		if (explicit != null) {
			return explicit.columnHasCells(x);
		}
		return x >= 0 && x < lanes.length && lanes[x] != null && !lanes[x].isEmpty();
	}

	/**
	 * Column {@code index} was removed: its cells go and the cells right of it move one column left
	 * (the Grid's in-place shift of the points).
	 *
	 * @return whether a cell went or moved
	 */
	boolean columnRemoved(final int index) {
		if (explicit != null) {
			boolean modified = explicit.removeIf(cell -> cell.x == index);
			for (int i = 0; i < explicit.size(); i++) {
				final Point cell = explicit.get(i);
				if (cell.x >= index) {
					cell.x--;
					modified = true;
				}
			}
			explicit.reindex();
			modCount++;
			return modified;
		}
		boolean modified = false;
		for (int x = Math.max(0, index); x < lanes.length; x++) {
			modified |= lanes[x] != null && !lanes[x].isEmpty();
		}
		if (index >= 0 && index < lanes.length) {
			if (lanes[index] != null) {
				count -= lanes[index].count(0, LIMIT);
			}
			System.arraycopy(lanes, index + 1, lanes, index, lanes.length - index - 1);
			lanes[lanes.length - 1] = null;
		}
		final int[] oldFirst = first;
		final int[] oldLast = last;
		final int[][] oldCols = cols;
		final int oldBlocks = blocks;
		first = new int[Math.max(4, oldBlocks)];
		last = new int[first.length];
		cols = new int[first.length][];
		blocks = 0;
		for (int b = 0; b < oldBlocks; b++) {
			int n = 0;
			final int[] kept = new int[oldCols[b].length];
			for (final int col : oldCols[b]) {
				if (col != index) {
					kept[n++] = col > index ? col - 1 : col;
				}
			}
			if (n > 0) {
				push(oldFirst[b], oldLast[b], n == kept.length ? kept : Arrays.copyOf(kept, n));
			}
		}
		changed();
		return modified;
	}

	/** The rows at and after {@code rowCount} were removed: their cells go. */
	void rowsTruncated(final int rowCount) {
		if (explicit != null) {
			explicit.removeIf(cell -> cell.y >= rowCount);
			modCount++;
			return;
		}
		for (final RowSparseBits lane : lanes) {
			if (lane != null) {
				count -= lane.count(rowCount, LIMIT);
				lane.setRange(rowCount, LIMIT, false);
			}
		}
		int to = 0;
		for (int b = 0; b < blocks; b++) {
			if (first[b] < rowCount) {
				first[to] = first[b];
				last[to] = Math.min(last[b], rowCount - 1);
				cols[to] = cols[b];
				to++;
			}
		}
		Arrays.fill(cols, to, blocks, null);
		blocks = to;
		changed();
	}

	/** Makes this list hold the cells of {@code source} (which keeps blocks), in order. */
	void copyFrom(final GridCellSelectionLanes source) {
		explicit = null;
		reset();
		source.compact();
		for (int b = 0; b < source.blocks; b++) {
			append(source.first[b], source.last[b], source.cols[b]);
		}
	}

	// ---------------------------------------------------------------------------- the list

	@Override
	public int size() {
		return explicit != null ? explicit.size() : count;
	}

	@Override
	public Point get(final int index) {
		if (explicit != null) {
			return explicit.get(index);
		}
		if (index < 0 || index >= count) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + count);
		}
		final int[] before = prefix();
		int low = 0;
		int high = blocks - 1;
		while (low < high) {
			final int mid = (low + high + 1) >>> 1;
			if (before[mid] <= index) {
				low = mid;
			} else {
				high = mid - 1;
			}
		}
		final int offset = index - before[low];
		final int width = cols[low].length;
		return new Point(cols[low][offset % width], first[low] + offset / width);
	}

	@Override
	public boolean contains(final Object o) {
		if (explicit != null) {
			return explicit.contains(o);
		}
		if (!(o instanceof Point)) {
			return false;
		}
		final Point p = (Point) o;
		return p.x >= 0 && p.x < lanes.length && p.y >= 0 && lanes[p.x] != null && lanes[p.x].get(p.y);
	}

	@Override
	public int indexOf(final Object o) {
		if (explicit != null) {
			return explicit.indexOf(o);
		}
		if (!contains(o)) {
			return -1;
		}
		final Point p = (Point) o;
		final int[] before = prefix();
		for (int b = 0; b < blocks; b++) {
			if (first[b] <= p.y && p.y <= last[b]) {
				for (int j = 0; j < cols[b].length; j++) {
					if (cols[b][j] == p.x) {
						return before[b] + (p.y - first[b]) * cols[b].length + j;
					}
				}
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(final Object o) {
		return explicit != null ? explicit.lastIndexOf(o) : indexOf(o);
	}

	@Override
	public boolean add(final Point p) {
		if (p != null && p.x >= 0 && p.y >= 0 && !contains(p) && lanes()) {
			compact();
			append(p.y, p.y, new int[] { p.x });
			return true;
		}
		// the same cell twice (or a cell blocks do not hold): only a point list keeps it
		degrade();
		modCount++;
		return explicit.add(p);
	}

	@Override
	public void add(final int index, final Point p) {
		if (index == size()) {
			add(p);
			return;
		}
		degrade();
		modCount++;
		explicit.add(index, p);
	}

	@Override
	public boolean addAll(final Collection<? extends Point> c) {
		if (c instanceof GridCellSelectionLanes && ((GridCellSelectionLanes) c).keepsBlocks() && isEmpty()
				&& lanes()) {
			copyFrom((GridCellSelectionLanes) c);
			return !isEmpty();
		}
		boolean changed = false;
		for (final Point p : new ArrayList<>(c)) {
			changed |= add(p);
		}
		return changed;
	}

	@Override
	public Point set(final int index, final Point p) {
		degrade();
		modCount++;
		return explicit.set(index, p);
	}

	@Override
	public boolean remove(final Object o) {
		if (explicit != null) {
			modCount++;
			return explicit.remove(o);
		}
		if (!contains(o)) {
			return false;
		}
		final Point p = (Point) o;
		lanes[p.x].set(p.y, false);
		count--;
		holes = true;
		changed();
		return true;
	}

	@Override
	public Point remove(final int index) {
		if (explicit != null) {
			modCount++;
			return explicit.remove(index);
		}
		final Point p = get(index);
		remove(p);
		return p;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		if (explicit != null) {
			modCount++;
			return explicit.removeAll(c);
		}
		boolean changed = false;
		for (final Object o : c) {
			changed |= remove(o);
		}
		return changed;
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		degrade();
		modCount++;
		return explicit.retainAll(c);
	}

	@Override
	public boolean removeIf(final Predicate<? super Point> filter) {
		if (explicit != null) {
			modCount++;
			return explicit.removeIf(filter);
		}
		final List<Point> matches = new ArrayList<>();
		for (final Point p : toArray(new Point[0])) {
			if (filter.test(p)) {
				matches.add(p);
			}
		}
		for (final Point p : matches) {
			remove(p);
		}
		return !matches.isEmpty();
	}

	@Override
	public void clear() {
		if (explicit != null) {
			explicit.clear();
			modCount++;
			if (grid.rowSelectionLanes()) {
				explicit = null;
			}
		}
		reset();
	}

	@Override
	public Object[] toArray() {
		return toArray(new Point[size()]);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(final T[] a) {
		if (explicit != null) {
			return explicit.toArray(a);
		}
		compact();
		final T[] result = a.length >= count ? a
				: (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), count);
		int i = 0;
		for (int b = 0; b < blocks; b++) {
			for (int row = first[b]; row <= last[b]; row++) {
				for (final int col : cols[b]) {
					result[i++] = (T) new Point(col, row);
				}
			}
		}
		if (result.length > count) {
			result[count] = null;
		}
		return result;
	}

	// --------------------------------------------------------------------------- internals

	private RowSparseBits lane(final int col) {
		if (col >= lanes.length) {
			lanes = Arrays.copyOf(lanes, Math.max(col + 1, lanes.length * 2));
		}
		if (lanes[col] == null) {
			lanes[col] = new RowSparseBits();
		}
		return lanes[col];
	}

	/* appends cells none of which is selected (no holes: the caller compacted) */
	private void append(final int from, final int to, final int[] columns) {
		if (from > to || columns.length == 0) {
			return;
		}
		for (final int col : columns) {
			lane(col).setRange(from, to + 1, true);
		}
		count += (to - from + 1) * columns.length;
		push(from, to, columns);
		changed();
	}

	/*
	 * adds a block after the others; the last block absorbs it when the two read as one in order:
	 * the next rows by the same columns, or more columns of the last block's last row
	 */
	private void push(final int from, final int to, final int[] columns) {
		final int b = blocks - 1;
		if (b >= 0 && last[b] + 1 == from && Arrays.equals(cols[b], columns)) {
			last[b] = to;
			return;
		}
		if (b >= 0 && from == to && last[b] == from) {
			final int[] wider = Arrays.copyOf(cols[b], cols[b].length + columns.length);
			System.arraycopy(columns, 0, wider, cols[b].length, columns.length);
			if (first[b] < last[b]) {
				last[b]--;
				pushNew(from, from, wider);
			} else {
				cols[b] = wider;
			}
			final int c = blocks - 1;
			if (c > 0 && last[c - 1] + 1 == first[c] && Arrays.equals(cols[c - 1], cols[c])) {
				last[c - 1] = last[c];
				cols[c] = null;
				blocks--;
			}
			return;
		}
		pushNew(from, to, columns);
	}

	private void pushNew(final int from, final int to, final int[] columns) {
		if (blocks == first.length) {
			first = Arrays.copyOf(first, blocks * 2);
			last = Arrays.copyOf(last, blocks * 2);
			cols = Arrays.copyOf(cols, blocks * 2);
		}
		first[blocks] = from;
		last[blocks] = to;
		cols[blocks] = columns;
		blocks++;
	}

	/*
	 * the blocks reduced to the selected cells they hold, in order: each block splits where the set
	 * of its columns still selected changes (per column, O(lane runs in the block) lane steps)
	 */
	private void compact() {
		if (!holes) {
			return;
		}
		final int[] oldFirst = first;
		final int[] oldLast = last;
		final int[][] oldCols = cols;
		final int oldBlocks = blocks;
		first = new int[Math.max(4, oldBlocks)];
		last = new int[first.length];
		cols = new int[first.length][];
		blocks = 0;
		final int[] kept = new int[8];
		int[] shared = null;
		for (int b = 0; b < oldBlocks; b++) {
			final int[] columns = oldCols[b];
			final int[] bounds = boundaries(oldFirst[b], oldLast[b] + 1, columns);
			for (int s = 0; s + 1 < bounds.length; s++) {
				final int row = bounds[s];
				int n = 0;
				final int[] keep = columns.length <= kept.length ? kept : new int[columns.length];
				for (final int col : columns) {
					if (col < lanes.length && lanes[col] != null && lanes[col].get(row)) {
						keep[n++] = col;
					}
				}
				if (n == 0) {
					continue;
				}
				// segments that keep the same columns share one array
				int[] segment = columns;
				if (n < columns.length) {
					if (shared == null || shared.length != n || !Arrays.equals(shared, 0, n, keep, 0, n)) {
						shared = Arrays.copyOf(keep, n);
					}
					segment = shared;
				}
				push(row, bounds[s + 1] - 1, segment);
			}
		}
		holes = false;
		prefixValid = false;
	}

	/* from, limit and the rows between where a column's selection starts or ends: ascending, distinct */
	private int[] boundaries(final int from, final int limit, final int[] columns) {
		int[] bounds = new int[8];
		int n = 0;
		bounds[n++] = from;
		bounds[n++] = limit;
		for (final int col : columns) {
			final RowSparseBits lane = col < lanes.length ? lanes[col] : null;
			int position = from;
			while (lane != null && position < limit) {
				final int start = lane.next(position, limit);
				if (start < 0) {
					break;
				}
				final int end = lane.runEnd(start, limit);
				if (n + 2 > bounds.length) {
					bounds = Arrays.copyOf(bounds, bounds.length * 2);
				}
				bounds[n++] = start;
				bounds[n++] = end;
				position = end;
			}
		}
		Arrays.sort(bounds, 0, n);
		int distinct = 0;
		for (int i = 0; i < n; i++) {
			if (distinct == 0 || bounds[distinct - 1] != bounds[i]) {
				bounds[distinct++] = bounds[i];
			}
		}
		return Arrays.copyOf(bounds, distinct);
	}

	private int[] prefix() {
		compact();
		if (!prefixValid) {
			if (prefix.length < blocks) {
				prefix = new int[Math.max(blocks, prefix.length * 2)];
			}
			int before = 0;
			for (int b = 0; b < blocks; b++) {
				prefix[b] = before;
				before += (last[b] - first[b] + 1) * cols[b].length;
			}
			prefixValid = true;
		}
		return prefix;
	}

	private void changed() {
		prefixValid = false;
		modCount++;
	}

	private void reset() {
		lanes = new RowSparseBits[0];
		Arrays.fill(cols, 0, blocks, null);
		blocks = 0;
		count = 0;
		holes = false;
		changed();
	}

	/** Test view: the number of blocks after compaction, or -1 for a point list. */
	int blockCount() {
		if (explicit != null) {
			return -1;
		}
		compact();
		return blocks;
	}

	/** Checks the blocks against the lanes (tests). */
	void checkInvariants() {
		if (explicit != null) {
			return;
		}
		int total = 0;
		RowSparseBits[] seen = new RowSparseBits[lanes.length];
		for (int b = 0; b < blocks; b++) {
			if (first[b] > last[b] || cols[b].length == 0) {
				throw new IllegalStateException("empty block " + b);
			}
			for (int row = first[b]; row <= last[b]; row++) {
				for (final int col : cols[b]) {
					final boolean selected = col < lanes.length && lanes[col] != null && lanes[col].get(row);
					if (!selected) {
						if (!holes) {
							throw new IllegalStateException("cell " + col + "," + row + " of a block is not selected");
						}
						continue;
					}
					if (col >= seen.length) {
						seen = Arrays.copyOf(seen, col + 1);
					}
					if (seen[col] == null) {
						seen[col] = new RowSparseBits();
					}
					if (seen[col].get(row)) {
						throw new IllegalStateException("cell " + col + "," + row + " in two blocks");
					}
					seen[col].set(row, true);
					total++;
				}
			}
		}
		int inLanes = 0;
		for (final RowSparseBits lane : lanes) {
			inLanes += lane == null ? 0 : lane.count(0, LIMIT);
		}
		if (total != count || inLanes != count) {
			throw new IllegalStateException("count " + count + ", blocks " + total + ", lanes " + inLanes);
		}
	}
}
