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
 * blocks of rows by columns instead of a {@code Point} per cell: the Grid's cell list, in selection
 * order with first-occurrence removal, as the earlier point list.
 * <ul>
 * <li>membership is one {@link RowSparseBits} lane of rows per column index: {@code contains}, the
 * paint-time check, is O(log n) and makes no Point;</li>
 * <li>the order is a list of blocks in selection order; block {@code b} covers the cells
 * {@code (cols[b][j], row)} for {@code row} from {@code first[b]} to {@code last[b]}, row by row,
 * columns in {@code cols[b]} order (the order of the stock range loops), or from {@code last[b]}
 * down to {@code first[b]} when {@code down[b]} (a range the stock loops walk upwards: a row-header
 * drag or shift+click above its anchor). selectAllCells, a shift range, a column or a row-header
 * drag is one block, a ctrl+click a block of one cell, and cells appended next to the last block in
 * its order extend it;</li>
 * <li>a selected cell belongs to the last block that covers it: a removal only clears the lane and a
 * cell selected again is appended as a new block, both O(log n); the blocks are compacted to the
 * cells they own when their order is read, or when appends have doubled them since the last
 * compaction (amortized), so toggling cells costs no pass over the blocks;</li>
 * <li>cells keep their row when rows are inserted or removed, as the list's points do; a removed
 * column drops its cells and the cells right of it move one column left, as the Grid's in-place
 * shift of the points does; a truncation drops the rows past the new count;</li>
 * <li>a {@code Point} is made only when a caller asks for points ({@code get}, iteration,
 * {@code toArray}); no item is made.</li>
 * </ul>
 * The earlier range-select snapshot was an {@code ArrayList} that held the list's own points, so the
 * in-place column shift moved the snapshot's cells that were still selected too: a snapshot made by
 * {@code addAll} of this list keeps which of its cells the list still holds and moves them likewise.
 * What blocks cannot express (the same cell twice, an insertion or replacement inside the list) or
 * a Grid that is no longer flat and virtual without a row view turns the list into a
 * {@code GridCellSelectionList} of the same points, in order; it keeps blocks again once it is
 * cleared while the Grid is flat. Catalogue: ALG-GRID-CELL-BLOCKS (row-range by column-set blocks
 * over per-column row lanes, last-cover ownership).
 */
final class GridCellSelectionLanes extends AbstractList<Point> implements RandomAccess {

	private static final int LIMIT = Integer.MAX_VALUE - 1;
	private static final RowSparseBits[] NO_LANES = new RowSparseBits[0];

	private final Grid grid;
	/* the rows selected per column index; null for a column without a selected cell */
	private RowSparseBits[] lanes = new RowSparseBits[0];
	/* the blocks, in selection order; a column array is never changed once it is in a block */
	private int[] first = new int[4];
	private int[] last = new int[4];
	/* whether block b's rows run from last[b] down to first[b]; never for a block of one row */
	private boolean[] down = new boolean[4];
	private int[][] cols = new int[4][];
	private int blocks;
	/* the selected cells (rows by columns may exceed an int) */
	private long count;
	/*
	 * whether a block may cover cells it does not own (deselected, or selected again by a later
	 * block): compaction reduces the blocks to the cells they own
	 */
	private boolean dirty;
	/* the blocks after the last compaction (appends compact again once they have doubled) */
	private int compacted;
	/* the cells before each block, rebuilt on demand */
	private long[] prefix = new long[0];
	private boolean prefixValid;
	/* the point list this selection turned into, or null while it keeps blocks */
	private GridCellSelectionList explicit;
	/* the snapshot whose cells are this list's own (an addAll of this list), or null */
	private GridCellSelectionLanes sharer;
	/*
	 * in a snapshot: the cells the list still holds since the snapshot (per column, while no cell has
	 * moved: the snapshot's cells are distinct), or null
	 */
	private RowSparseBits[] linked;
	/*
	 * in a snapshot whose cells moved (a point list since): its entries the list still holds, by cell
	 * (the list holds each cell once), so that only those entries move again; or null
	 */
	private java.util.Map<Long, Point> linkedPoints;

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
			for (int k = 0; k <= last[b] - first[b]; k++) {
				final int row = rowAt(b, k);
				for (final int col : cols[b]) {
					list.add(new Point(col, row));
				}
			}
		}
		reset();
		explicit = list;
	}

	/* the k-th row of block b in its order */
	private int rowAt(final int b, final int k) {
		return down[b] ? last[b] - k : first[b] + k;
	}

	/**
	 * Appends the cells of the rows {@code [from, to]} by the columns {@code columns} (distinct,
	 * non-negative) that are not selected yet, row by row, columns in the given order: the stock
	 * range loop of {@code addToCellSelection} calls, without its checks. Blocks only.
	 */
	void addBlock(final int from, final int to, final int[] columns) {
		mayCompact();
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
	 * {@link #addBlock} for the rows taken from {@code to} up to {@code from}: the stock loop of a range
	 * walked upwards (a row-header drag or shift+click above its anchor), row by row from the bottom,
	 * columns in the given order. Blocks only.
	 */
	void addBlockDown(final int from, final int to, final int[] columns) {
		if (from >= to) {
			addBlock(from, to, columns);
			return;
		}
		mayCompact();
		// between two of these rows each column is selected in every row or in none of them
		final int[] bounds = boundaries(from, to + 1, columns, NO_LANES);
		for (int s = bounds.length - 2; s >= 0; s--) {
			final int start = bounds[s];
			final int[] free = new int[columns.length];
			int n = 0;
			for (final int col : columns) {
				if (col >= lanes.length || lanes[col] == null || !lanes[col].get(start)) {
					free[n++] = col;
				}
			}
			if (n > 0) {
				// the stock loop's cells of these rows, bottom row first: the columns not selected there
				append(start, bounds[s + 1] - 1, true, n == columns.length ? columns : Arrays.copyOf(free, n));
			}
		}
	}

	/**
	 * Deselects the cells of the rows {@code [from, to]} by the columns {@code columns} (distinct).
	 *
	 * @return whether a cell was selected
	 */
	boolean removeBlock(final int from, final int to, final int[] columns) {
		for (final int col : columns) {
			unshare(col, from, to);
		}
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
		long removed = 0;
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
		dirty = true;
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
		int[] rows = new int[(int) Math.min(count, 1024)];
		int n = 0;
		for (int b = 0; b < blocks; b++) {
			final int limit = last[b] + 1;
			final int start = n;
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
			if (down[b]) {
				// the block's new rows, bottom row first
				for (int i = start, j = n - 1; i < j; i++, j--) {
					final int t = rows[i];
					rows[i] = rows[j];
					rows[j] = t;
				}
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
	 * (the Grid's in-place shift of the points), in a snapshot sharing them too.
	 *
	 * @return whether a cell went or moved
	 */
	boolean columnRemoved(final int index) {
		if (sharer != null) {
			sharer.unlink(index, 0, LIMIT - 1);
			sharer.shiftLinked(index);
		}
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
		final boolean[] oldDown = down;
		final int[][] oldCols = cols;
		final int oldBlocks = blocks;
		first = new int[Math.max(4, oldBlocks)];
		last = new int[first.length];
		down = new boolean[first.length];
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
				push(oldFirst[b], oldLast[b], oldDown[b], n == kept.length ? kept : Arrays.copyOf(kept, n));
			}
		}
		changed();
		return modified;
	}

	/** The rows at and after {@code rowCount} were removed: their cells go. */
	void rowsTruncated(final int rowCount) {
		unlinkRows(rowCount);
		if (sharer != null) {
			sharer.unlinkRows(rowCount);
		}
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
				// a block walked upwards loses its first rows: the rest still runs upwards
				down[to] = down[b] && first[to] < last[to];
				cols[to] = cols[b];
				to++;
			}
		}
		Arrays.fill(cols, to, blocks, null);
		blocks = to;
		changed();
	}

	/** Makes this list hold copies of the cells of {@code source} (which keeps blocks), in order. */
	void copyFrom(final GridCellSelectionLanes source) {
		explicit = null;
		reset();
		source.compact();
		for (int b = 0; b < source.blocks; b++) {
			append(source.first[b], source.last[b], source.down[b], source.cols[b]);
		}
	}

	// ------------------------------------------------------------- cells named by a gesture

	/**
	 * Cells a gesture names before they are selected, as blocks in the order the stock loops name
	 * them: a column's cells (its rows), or the rows between a row-header drag's anchor and the
	 * hovered row by the visible columns. Caller owned: a few blocks, no lane, no point, no item.
	 */
	static final class Cells {
		int blocks;
		int[] first = new int[2];
		int[] last = new int[2];
		boolean[] down = new boolean[2];
		int[][] cols = new int[2][];

		/*
		 * the cells (columns[j], row) for row from from to to (to down to from when downward), row by
		 * row; columns are distinct and are not changed afterwards
		 */
		void add(final int from, final int to, final boolean downward, final int[] columns) {
			if (blocks == first.length) {
				first = Arrays.copyOf(first, blocks * 2);
				last = Arrays.copyOf(last, blocks * 2);
				down = Arrays.copyOf(down, blocks * 2);
				cols = Arrays.copyOf(cols, blocks * 2);
			}
			first[blocks] = from;
			last[blocks] = to;
			down[blocks] = downward && from < to;
			cols[blocks] = columns;
			blocks++;
		}

		private boolean empty(final int b) {
			return first[b] > last[b] || cols[b].length == 0;
		}

		/** @return the number of cells */
		long count() {
			long n = 0;
			for (int b = 0; b < blocks; b++) {
				if (!empty(b)) {
					n += (long) (last[b] - first[b] + 1) * cols[b].length;
				}
			}
			return n;
		}

		/** @return the first cell, or null when there is none */
		Point firstCell() {
			for (int b = 0; b < blocks; b++) {
				if (!empty(b)) {
					return new Point(cols[b][0], down[b] ? last[b] : first[b]);
				}
			}
			return null;
		}

		/** @return a point per cell, in order (for a list that does not keep blocks) */
		List<Point> points() {
			final List<Point> points = new ArrayList<>();
			for (int b = 0; b < blocks; b++) {
				for (int k = 0; !empty(b) && k <= last[b] - first[b]; k++) {
					final int row = down[b] ? last[b] - k : first[b] + k;
					for (final int col : cols[b]) {
						points.add(new Point(col, row));
					}
				}
			}
			return points;
		}
	}

	/**
	 * @return whether this list holds exactly the given cells in their order (the stock
	 *         {@code cells.equals(list)}), in O(blocks) steps where the blocks line up, without a point
	 */
	boolean sameCells(final Cells cells) {
		if (explicit != null) {
			return explicit.equals(cells.points());
		}
		if (cells.count() != count) {
			return false;
		}
		compact();
		// a cursor on each side: block, row within it, column within the row
		int a = 0;
		int ka = 0;
		int ja = 0;
		int c = 0;
		int kc = 0;
		int jc = 0;
		while (true) {
			while (a < blocks && ka > last[a] - first[a]) {
				a++;
				ka = 0;
			}
			while (c < cells.blocks && (cells.empty(c) || kc > cells.last[c] - cells.first[c])) {
				c++;
				kc = 0;
			}
			if (a == blocks || c == cells.blocks) {
				return a == blocks && c == cells.blocks;
			}
			final int rowA = rowAt(a, ka);
			final int rowC = cells.down[c] ? cells.last[c] - kc : cells.first[c] + kc;
			if (ja == 0 && jc == 0 && Arrays.equals(cols[a], cells.cols[c])) {
				// whole rows by the same columns: the rows run alike while both go the same way
				if (rowA != rowC) {
					return false;
				}
				final int restA = last[a] - first[a] - ka;
				final int restC = cells.last[c] - cells.first[c] - kc;
				final int k = down[a] == cells.down[c] ? 1 + Math.min(restA, restC) : 1;
				ka += k;
				kc += k;
				continue;
			}
			if (cols[a][ja] != cells.cols[c][jc] || rowA != rowC) {
				return false;
			}
			if (++ja == cols[a].length) {
				ja = 0;
				ka++;
			}
			if (++jc == cells.cols[c].length) {
				jc = 0;
				kc++;
			}
		}
	}

	/** @return whether every given cell is selected (the stock {@code list.containsAll(cells)}) */
	boolean containsCells(final Cells cells) {
		for (int b = 0; b < cells.blocks; b++) {
			if (cells.empty(b)) {
				continue;
			}
			for (final int col : cells.cols[b]) {
				if (explicit != null) {
					for (int row = cells.first[b]; row <= cells.last[b]; row++) {
						if (!explicit.contains(new Point(col, row))) {
							return false;
						}
					}
				} else if (col < 0 || col >= lanes.length || lanes[col] == null
						|| lanes[col].count(cells.first[b], cells.last[b] + 1) != cells.last[b] - cells.first[b] + 1) {
					return false;
				}
			}
		}
		return true;
	}

	/** Deselects the given cells (the stock {@code list.removeAll(cells)}: this list holds each once). */
	void removeCells(final Cells cells) {
		for (int b = 0; b < cells.blocks; b++) {
			if (!cells.empty(b)) {
				removeBlock(cells.first[b], cells.last[b], cells.cols[b]);
			}
		}
	}

	// ------------------------------------------------------------- the snapshot's shared cells

	/* the list let cells go: the snapshot's copies of them are no longer its points */
	private void unshare(final int col, final int from, final int to) {
		if (sharer != null) {
			sharer.unlink(col, from, to);
		}
	}

	private static long cellKey(final int x, final int y) {
		return (long) x << 32 | y & 0xFFFFFFFFL;
	}

	private void unlink(final int col, final int from, final int to) {
		if (linkedPoints != null) {
			if (from == to) {
				linkedPoints.remove(Long.valueOf(cellKey(col, from)));
			} else {
				linkedPoints.values().removeIf(p -> p.x == col && p.y >= from && p.y <= to);
			}
			return;
		}
		if (linked != null && col >= 0 && col < linked.length && linked[col] != null) {
			linked[col].setRange(Math.max(0, from), to + 1, false);
		}
	}

	private void unlinkRows(final int rowCount) {
		if (linkedPoints != null) {
			linkedPoints.values().removeIf(p -> p.y >= rowCount);
		}
		if (linked != null) {
			for (final RowSparseBits lane : linked) {
				if (lane != null) {
					lane.setRange(Math.max(0, rowCount), LIMIT, false);
				}
			}
		}
	}

	/*
	 * the list moved its cells right of a removed column one column left in place: the entries of this
	 * snapshot it still holds move with them, the others stay (only a point list holds both, and from
	 * then on the shared entries are tracked one by one, as the earlier list's shared points were)
	 */
	private void shiftLinked(final int index) {
		if (linkedPoints == null) {
			if (linked == null) {
				return;
			}
			boolean moves = false;
			for (int x = Math.max(0, index + 1); x < linked.length; x++) {
				moves |= linked[x] != null && !linked[x].isEmpty();
			}
			if (!moves) {
				return;
			}
			degrade();
			linkedPoints = new java.util.HashMap<>();
			for (int i = 0; i < explicit.size(); i++) {
				final Point cell = explicit.get(i);
				if (cell.x >= 0 && cell.x < linked.length && linked[cell.x] != null && cell.y >= 0
						&& linked[cell.x].get(cell.y)) {
					linkedPoints.put(Long.valueOf(cellKey(cell.x, cell.y)), cell);
				}
			}
			linked = null;
		}
		final List<Point> moving = new ArrayList<>();
		for (final Point cell : linkedPoints.values()) {
			if (cell.x > index) {
				moving.add(cell);
			}
		}
		if (moving.isEmpty()) {
			return;
		}
		for (final Point cell : moving) {
			linkedPoints.remove(Long.valueOf(cellKey(cell.x, cell.y)));
		}
		for (final Point cell : moving) {
			cell.x--;
			linkedPoints.put(Long.valueOf(cellKey(cell.x, cell.y)), cell);
		}
		explicit.reindex();
		modCount++;
	}

	/* a copy of this list's cells as lanes */
	private RowSparseBits[] cellLanes() {
		RowSparseBits[] copy = new RowSparseBits[lanes.length];
		if (explicit != null) {
			for (int i = 0; i < explicit.size(); i++) {
				final Point cell = explicit.get(i);
				if (cell.x >= 0 && cell.y >= 0) {
					if (cell.x >= copy.length) {
						copy = Arrays.copyOf(copy, cell.x + 1);
					}
					if (copy[cell.x] == null) {
						copy[cell.x] = new RowSparseBits();
					}
					copy[cell.x].set(cell.y, true);
				}
			}
			return copy;
		}
		for (int x = 0; x < lanes.length; x++) {
			final RowSparseBits lane = lanes[x];
			int position = 0;
			while (lane != null && position < LIMIT) {
				final int start = lane.next(position, LIMIT);
				if (start < 0) {
					break;
				}
				final int end = lane.runEnd(start, LIMIT);
				if (copy[x] == null) {
					copy[x] = new RowSparseBits();
				}
				copy[x].setRange(start, end, true);
				position = end;
			}
		}
		return copy;
	}

	// ---------------------------------------------------------------------------- the list

	@Override
	public int size() {
		return explicit != null ? explicit.size() : (int) Math.min(count, Integer.MAX_VALUE);
	}

	@Override
	public Point get(final int index) {
		if (explicit != null) {
			return explicit.get(index);
		}
		if (index < 0 || index >= count) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + count);
		}
		final long[] before = prefix();
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
		final long offset = index - before[low];
		final int width = cols[low].length;
		return new Point(cols[low][(int) (offset % width)], rowAt(low, (int) (offset / width)));
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
		final long[] before = prefix();
		for (int b = 0; b < blocks; b++) {
			if (first[b] <= p.y && p.y <= last[b]) {
				for (int j = 0; j < cols[b].length; j++) {
					if (cols[b][j] == p.x) {
						final long k = down[b] ? last[b] - p.y : p.y - first[b];
						return (int) Math.min(before[b] + k * cols[b].length + j, Integer.MAX_VALUE);
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
			mayCompact();
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

	/*
	 * the range-select snapshot: an addAll of the cell list holds the list's own cells, as the
	 * earlier ArrayList held its points, until the list lets them go
	 */
	@Override
	public boolean addAll(final Collection<? extends Point> c) {
		final boolean changed;
		if (c instanceof GridCellSelectionLanes && ((GridCellSelectionLanes) c).keepsBlocks() && isEmpty()
				&& lanes()) {
			copyFrom((GridCellSelectionLanes) c);
			changed = !isEmpty();
		} else {
			boolean added = false;
			for (final Point p : new ArrayList<>(c)) {
				added |= add(p);
			}
			changed = added;
		}
		if (c instanceof GridCellSelectionLanes) {
			linked = cellLanes();
			linkedPoints = null;
			((GridCellSelectionLanes) c).sharer = this;
		}
		return changed;
	}

	@Override
	public Point set(final int index, final Point p) {
		degrade();
		modCount++;
		final Point old = explicit.set(index, p);
		if (old != null) {
			unshare(old.x, old.y, old.y);
		}
		return old;
	}

	@Override
	public boolean remove(final Object o) {
		if (explicit != null) {
			modCount++;
			final boolean removed = explicit.remove(o);
			if (removed) {
				final Point p = (Point) o;
				unshare(p.x, p.y, p.y);
			}
			return removed;
		}
		if (!contains(o)) {
			return false;
		}
		final Point p = (Point) o;
		lanes[p.x].set(p.y, false);
		unshare(p.x, p.y, p.y);
		count--;
		dirty = true;
		changed();
		return true;
	}

	@Override
	public Point remove(final int index) {
		if (explicit != null) {
			modCount++;
			final Point p = explicit.remove(index);
			unshare(p.x, p.y, p.y);
			return p;
		}
		final Point p = get(index);
		remove(p);
		return p;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		if (explicit != null) {
			modCount++;
			for (final Object o : c) {
				if (o instanceof Point) {
					unshare(((Point) o).x, ((Point) o).y, ((Point) o).y);
				}
			}
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
		final List<Point> gone = new ArrayList<>();
		for (int i = 0; i < explicit.size(); i++) {
			if (!c.contains(explicit.get(i))) {
				gone.add(explicit.get(i));
			}
		}
		for (final Point p : gone) {
			unshare(p.x, p.y, p.y);
		}
		return explicit.retainAll(c);
	}

	@Override
	public boolean removeIf(final Predicate<? super Point> filter) {
		final List<Point> matches = new ArrayList<>();
		for (final Point p : toArray(new Point[0])) {
			if (filter.test(p)) {
				matches.add(p);
			}
		}
		if (explicit != null) {
			modCount++;
			for (final Point p : matches) {
				unshare(p.x, p.y, p.y);
			}
			return explicit.removeIf(filter);
		}
		for (final Point p : matches) {
			remove(p);
		}
		return !matches.isEmpty();
	}

	@Override
	public void clear() {
		linked = null;
		linkedPoints = null;
		if (sharer != null) {
			sharer.linked = null;
			sharer.linkedPoints = null;
		}
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
		if (count > Integer.MAX_VALUE - 8) {
			throw new OutOfMemoryError("cells: " + count);
		}
		final int n = (int) count;
		final T[] result = a.length >= n ? a
				: (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), n);
		int i = 0;
		for (int b = 0; b < blocks; b++) {
			for (int k = 0; k <= last[b] - first[b]; k++) {
				final int row = rowAt(b, k);
				for (final int col : cols[b]) {
					result[i++] = (T) new Point(col, row);
				}
			}
		}
		if (result.length > n) {
			result[n] = null;
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

	/* appends cells none of which is selected; they belong to the new last block */
	private void append(final int from, final int to, final int[] columns) {
		append(from, to, false, columns);
	}

	/* append for the rows from to down to from when downward */
	private void append(final int from, final int to, final boolean downward, final int[] columns) {
		if (from > to || columns.length == 0) {
			return;
		}
		for (final int col : columns) {
			lane(col).setRange(from, to + 1, true);
		}
		count += (long) (to - from + 1) * columns.length;
		push(from, to, downward, columns);
		changed();
	}

	/* appends compact the blocks once they have doubled since the last compaction (amortized) */
	private void mayCompact() {
		if (dirty && blocks > 2 * compacted + 64) {
			compact();
		}
	}

	/*
	 * adds a block after the others (its rows from to down to from when downward); the last block
	 * absorbs it when the two read as one in order: the next rows by the same columns in the same
	 * direction, or other columns of the last block's last row
	 */
	private void push(final int from, final int to, final boolean downward, final int[] columns) {
		final boolean desc = downward && from < to;
		final int b = blocks - 1;
		if (b >= 0 && extend(b, from, to, desc, columns)) {
			return;
		}
		if (b >= 0 && from == to && (down[b] ? first[b] : last[b]) == from && disjoint(cols[b], columns)) {
			final int[] wider = Arrays.copyOf(cols[b], cols[b].length + columns.length);
			System.arraycopy(columns, 0, wider, cols[b].length, columns.length);
			if (first[b] < last[b]) {
				// the last row in order leaves the block
				if (down[b]) {
					first[b]++;
				} else {
					last[b]--;
				}
				down[b] &= first[b] < last[b];
				pushNew(from, from, false, wider);
			} else {
				cols[b] = wider;
			}
			final int c = blocks - 1;
			if (c > 0 && extend(c - 1, first[c], last[c], down[c], cols[c])) {
				cols[c] = null;
				blocks--;
			}
			return;
		}
		pushNew(from, to, desc, columns);
	}

	/*
	 * whether block b reads on into the rows [from, to] (from to down to from when desc) by the same
	 * columns: then it takes them. A block of one row goes either way.
	 */
	private boolean extend(final int b, final int from, final int to, final boolean desc, final int[] columns) {
		if (!Arrays.equals(cols[b], columns)) {
			return false;
		}
		final boolean single = first[b] == last[b];
		if ((single || !down[b]) && !desc && last[b] + 1 == from) {
			last[b] = to;
			down[b] = false;
			return true;
		}
		if ((single || down[b]) && (desc || from == to) && first[b] - 1 == to) {
			first[b] = from;
			down[b] = true;
			return true;
		}
		return false;
	}

	private static boolean disjoint(final int[] a, final int[] b) {
		for (final int x : a) {
			for (final int y : b) {
				if (x == y) {
					return false;
				}
			}
		}
		return true;
	}

	private void pushNew(final int from, final int to, final boolean desc, final int[] columns) {
		if (blocks == first.length) {
			first = Arrays.copyOf(first, blocks * 2);
			last = Arrays.copyOf(last, blocks * 2);
			down = Arrays.copyOf(down, blocks * 2);
			cols = Arrays.copyOf(cols, blocks * 2);
		}
		first[blocks] = from;
		last[blocks] = to;
		down[blocks] = desc && from < to;
		cols[blocks] = columns;
		blocks++;
	}

	/*
	 * the blocks reduced to the cells they own, in order: a selected cell belongs to the last block
	 * that covers it. From the last block back, each block keeps its selected cells that no later
	 * block covers and splits where that set of columns changes (per column, O(lane runs and later
	 * coverage runs in the block) steps)
	 */
	private void compact() {
		if (!dirty) {
			return;
		}
		final int oldBlocks = blocks;
		final RowSparseBits[] taken = new RowSparseBits[lanes.length];
		int[] pf = new int[Math.max(8, oldBlocks)];
		int[] pl = new int[pf.length];
		int[][] pc = new int[pf.length][];
		int pieces = 0;
		final int[] begin = new int[oldBlocks + 1];
		final int[] keep = new int[8];
		int[] shared = null;
		for (int b = oldBlocks - 1; b >= 0; b--) {
			final int from = first[b];
			final int limit = last[b] + 1;
			final int[] columns = cols[b];
			begin[b] = pieces;
			final int[] bounds = boundaries(from, limit, columns, taken);
			final int[] kept = columns.length <= keep.length ? keep : new int[columns.length];
			for (int s = 0; s + 1 < bounds.length; s++) {
				final int row = bounds[s];
				int n = 0;
				for (final int col : columns) {
					if (owned(col, row, taken)) {
						kept[n++] = col;
					}
				}
				if (n == 0) {
					continue;
				}
				// pieces that keep the same columns share one array
				int[] piece = columns;
				if (n < columns.length) {
					if (shared == null || shared.length != n || !Arrays.equals(shared, 0, n, kept, 0, n)) {
						shared = Arrays.copyOf(kept, n);
					}
					piece = shared;
				}
				if (pieces == pf.length) {
					pf = Arrays.copyOf(pf, pieces * 2);
					pl = Arrays.copyOf(pl, pieces * 2);
					pc = Arrays.copyOf(pc, pieces * 2);
				}
				pf[pieces] = row;
				pl[pieces] = bounds[s + 1] - 1;
				pc[pieces] = piece;
				pieces++;
			}
			for (final int col : columns) {
				if (col < taken.length) {
					if (taken[col] == null) {
						taken[col] = new RowSparseBits();
					}
					taken[col].setRange(from, limit, true);
				}
			}
		}
		begin[oldBlocks] = pieces;
		// block b's pieces are [begin[b], begin[b - 1]) (built from the last block back), rows ascending
		final boolean[] oldDown = down;
		first = new int[Math.max(4, pieces)];
		last = new int[first.length];
		down = new boolean[first.length];
		cols = new int[first.length][];
		blocks = 0;
		for (int b = 0; b < oldBlocks; b++) {
			final int end = b == 0 ? pieces : begin[b - 1];
			if (oldDown[b]) {
				// a block walked upwards keeps its pieces bottom first
				for (int p = end - 1; p >= begin[b]; p--) {
					push(pf[p], pl[p], true, pc[p]);
				}
			} else {
				for (int p = begin[b]; p < end; p++) {
					push(pf[p], pl[p], false, pc[p]);
				}
			}
		}
		dirty = false;
		compacted = blocks;
		prefixValid = false;
	}

	private boolean owned(final int col, final int row, final RowSparseBits[] taken) {
		return col < lanes.length && lanes[col] != null && lanes[col].get(row)
				&& (col >= taken.length || taken[col] == null || !taken[col].get(row));
	}

	/*
	 * from, limit and the rows between where a column's selection or a later block's coverage starts
	 * or ends: ascending, distinct
	 */
	private int[] boundaries(final int from, final int limit, final int[] columns, final RowSparseBits[] taken) {
		int[] bounds = new int[8];
		int n = 0;
		bounds[n++] = from;
		bounds[n++] = limit;
		for (final int col : columns) {
			for (int k = 0; k < 2; k++) {
				final RowSparseBits lane = k == 0 ? col < lanes.length ? lanes[col] : null
						: col < taken.length ? taken[col] : null;
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

	private long[] prefix() {
		compact();
		if (!prefixValid) {
			if (prefix.length < blocks) {
				prefix = new long[Math.max(blocks, prefix.length * 2)];
			}
			long before = 0;
			for (int b = 0; b < blocks; b++) {
				prefix[b] = before;
				before += (long) (last[b] - first[b] + 1) * cols[b].length;
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
		dirty = false;
		compacted = 0;
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

	/** Checks the count against the lanes, then the compacted blocks against the lanes (tests). */
	void checkInvariants() {
		if (explicit != null) {
			return;
		}
		long inLanes = 0;
		for (final RowSparseBits lane : lanes) {
			inLanes += lane == null ? 0 : lane.count(0, LIMIT);
		}
		if (inLanes != count) {
			throw new IllegalStateException("count " + count + ", lanes " + inLanes);
		}
		compact();
		long total = 0;
		RowSparseBits[] seen = new RowSparseBits[lanes.length];
		for (int b = 0; b < blocks; b++) {
			if (first[b] > last[b] || cols[b].length == 0) {
				throw new IllegalStateException("empty block " + b);
			}
			if (down[b] && first[b] == last[b]) {
				throw new IllegalStateException("one-row block " + b + " runs upwards");
			}
			for (int row = first[b]; row <= last[b]; row++) {
				for (final int col : cols[b]) {
					if (col >= lanes.length || lanes[col] == null || !lanes[col].get(row)) {
						throw new IllegalStateException("cell " + col + "," + row + " of a block is not selected");
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
		if (total != count) {
			throw new IllegalStateException("count " + count + ", blocks " + total);
		}
	}
}
