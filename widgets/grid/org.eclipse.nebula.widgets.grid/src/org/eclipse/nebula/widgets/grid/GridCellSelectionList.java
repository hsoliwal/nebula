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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.RandomAccess;
import java.util.function.Predicate;

import org.eclipse.swt.graphics.Point;

/**
 * The selected cells of a Grid in the viewport mode: the stock list (same order, same entries,
 * duplicates included) as an array with holes, plus an open-addressing table from the packed cell
 * to its number of entries and the position of its first entry, and a count of selected cells per
 * column.
 * <ul>
 * <li>{@code contains} (asked for every painted cell and every cell a range selection adds) is a
 * table probe, O(1), without allocation;</li>
 * <li>removing a cell by value (a ctrl+click toggle) leaves a hole at its first entry, O(1); holes
 * are closed when they outnumber the cells or when an index other than the first is read, so
 * removals cost O(1) amortized;</li>
 * <li>whether a column has a selected cell (the column header's selected look) is a count lookup,
 * so the Grid's column selection costs O(columns), not a pass over the selection.</li>
 * </ul>
 * The list keeps its own points: added points are copied and {@link #toArray(Object[])} hands out
 * copies, so no caller can change a cell behind the table; the Grid that shifts cells in place
 * calls {@link #reindex()}. Catalogue: ALG-GRID-CELL-COUNT, ALG-GRID-SELECTION-POSITION.
 */
final class GridCellSelectionList extends AbstractList<Point> implements RandomAccess {
	private static final long MIX = 0x9E3779B97F4A7C15L;

	private Point[] cells = new Point[16];
	/* positions [head, end) hold the cells and the holes */
	private int head;
	private int end;
	private int live;

	/* open addressing, linear probing: packed cell, its entries (0 = free slot), its first entry's position */
	private long[] keys = new long[32];
	private int[] counts = new int[32];
	private int[] firsts = new int[32];
	private int used;

	/* selected cells per column index (cell.x) */
	private int[] columnCounts = new int[8];

	private static long key(final int x, final int y) {
		return (long) x << 32 | y & 0xFFFFFFFFL;
	}

	private int home(final long key) {
		return (int) ((key * MIX) >>> 40) & keys.length - 1;
	}

	private int slot(final long key) {
		final int mask = keys.length - 1;
		int i = home(key);
		while (counts[i] != 0 && keys[i] != key) {
			i = i + 1 & mask;
		}
		return i;
	}

	/* one more entry of p; a new cell's first entry is at position */
	private void inc(final Point p, final int position) {
		if (used + 1 > keys.length >> 1) {
			grow();
		}
		final long key = key(p.x, p.y);
		final int i = slot(key);
		if (counts[i] == 0) {
			keys[i] = key;
			firsts[i] = position;
			used++;
		}
		counts[i]++;
		column(p.x, 1);
	}

	/* one entry of p less */
	private void dec(final Point p) {
		final int i = slot(key(p.x, p.y));
		if (counts[i] == 0) {
			return;
		}
		column(p.x, -1);
		if (--counts[i] == 0) {
			delete(i);
		}
	}

	/* backward-shift deletion keeps every probe chain intact without tombstones */
	private void delete(final int slot) {
		final int mask = keys.length - 1;
		int hole = slot;
		int i = slot;
		while (true) {
			i = i + 1 & mask;
			if (counts[i] == 0) {
				break;
			}
			final int home = home(keys[i]);
			// the entry at i may move to the hole when its home is not cyclically in (hole, i]
			final boolean between = hole <= i ? home > hole && home <= i : home > hole || home <= i;
			if (!between) {
				keys[hole] = keys[i];
				counts[hole] = counts[i];
				firsts[hole] = firsts[i];
				hole = i;
			}
		}
		counts[hole] = 0;
		used--;
	}

	private void grow() {
		final long[] oldKeys = keys;
		final int[] oldCounts = counts;
		final int[] oldFirsts = firsts;
		keys = new long[oldKeys.length << 1];
		counts = new int[keys.length];
		firsts = new int[keys.length];
		for (int i = 0; i < oldKeys.length; i++) {
			if (oldCounts[i] != 0) {
				final int j = slot(oldKeys[i]);
				keys[j] = oldKeys[i];
				counts[j] = oldCounts[i];
				firsts[j] = oldFirsts[i];
			}
		}
	}

	private void column(final int x, final int delta) {
		if (x < 0) {
			return;
		}
		if (x >= columnCounts.length) {
			columnCounts = Arrays.copyOf(columnCounts, Math.max(x + 1, columnCounts.length * 2));
		}
		columnCounts[x] += delta;
	}

	/** @return whether a cell of column {@code x} is selected, O(1) */
	boolean columnHasCells(final int x) {
		return x >= 0 && x < columnCounts.length && columnCounts[x] > 0;
	}

	/** Rebuilds the table after the Grid changed cells in place. */
	void reindex() {
		compact();
		rebuild();
	}

	/* the table and the column counts from the cells, in order (no holes: positions are indices) */
	private void rebuild() {
		Arrays.fill(counts, 0);
		used = 0;
		Arrays.fill(columnCounts, 0);
		for (int i = 0; i < end; i++) {
			inc(cells[i], i);
		}
	}

	/* closes the holes; the first entries' positions follow */
	private void compact() {
		if (head == 0 && end == live) {
			return;
		}
		int to = 0;
		for (int i = head; i < end; i++) {
			if (cells[i] != null) {
				cells[to++] = cells[i];
			}
		}
		Arrays.fill(cells, to, end, null);
		head = 0;
		end = to;
		// mark every first entry unknown; the first pass over the cells in order sets it
		for (int i = 0; i < keys.length; i++) {
			if (counts[i] != 0) {
				firsts[i] = -1;
			}
		}
		for (int i = 0; i < end; i++) {
			final int s = slot(key(cells[i].x, cells[i].y));
			if (counts[s] != 0 && firsts[s] < 0) {
				firsts[s] = i;
			}
		}
	}

	private void skipHoles() {
		while (head < end && cells[head] == null) {
			head++;
		}
	}

	private void ensureCapacity(final int capacity) {
		if (capacity > cells.length) {
			cells = Arrays.copyOf(cells, Math.max(capacity, cells.length * 2));
		}
	}

	@Override
	public int size() {
		return live;
	}

	@Override
	public boolean contains(final Object o) {
		if (!(o instanceof Point)) {
			return false;
		}
		final Point p = (Point) o;
		return counts[slot(key(p.x, p.y))] != 0;
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		for (final Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int indexOf(final Object o) {
		if (!contains(o)) {
			return -1;
		}
		compact();
		final Point p = (Point) o;
		return firsts[slot(key(p.x, p.y))];
	}

	@Override
	public Point get(final int index) {
		if (index < 0 || index >= live) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + live);
		}
		if (index == 0) {
			skipHoles();
			return cells[head];
		}
		compact();
		return cells[index];
	}

	@Override
	public boolean add(final Point p) {
		final Point copy = new Point(p.x, p.y);
		ensureCapacity(end + 1);
		cells[end] = copy;
		inc(copy, end);
		end++;
		live++;
		modCount++;
		return true;
	}

	@Override
	public void add(final int index, final Point p) {
		if (index == live) {
			add(p);
			return;
		}
		compact();
		if (index < 0 || index > live) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + live);
		}
		ensureCapacity(end + 1);
		System.arraycopy(cells, index, cells, index + 1, end - index);
		cells[index] = new Point(p.x, p.y);
		end++;
		live++;
		modCount++;
		// the positions after the insertion moved: O(n), the Grid only appends
		rebuild();
	}

	@Override
	public boolean addAll(final Collection<? extends Point> c) {
		ensureCapacity(end + c.size());
		for (final Point p : c) {
			add(p);
		}
		return !c.isEmpty();
	}

	@Override
	public Point set(final int index, final Point p) {
		compact();
		if (index < 0 || index >= live) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + live);
		}
		final Point old = cells[index];
		cells[index] = new Point(p.x, p.y);
		rebuild();
		return old;
	}

	@Override
	public Point remove(final int index) {
		compact();
		if (index < 0 || index >= live) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + live);
		}
		final Point old = cells[index];
		System.arraycopy(cells, index + 1, cells, index, end - index - 1);
		cells[--end] = null;
		live--;
		modCount++;
		rebuild();
		return old;
	}

	@Override
	public boolean remove(final Object o) {
		if (!(o instanceof Point)) {
			return false;
		}
		final Point p = (Point) o;
		final int s = slot(key(p.x, p.y));
		if (counts[s] == 0) {
			return false;
		}
		final int position = firsts[s];
		final Point old = cells[position];
		cells[position] = null;
		live--;
		modCount++;
		if (counts[s] > 1) {
			// a duplicate entry: the next one becomes the first (duplicates are rare)
			int next = position + 1;
			while (cells[next] == null || cells[next].x != p.x || cells[next].y != p.y) {
				next++;
			}
			firsts[s] = next;
		}
		dec(old);
		if (live == 0) {
			head = end = 0;
		} else if (end - head > 2 * live + 16) {
			compact();
		}
		return true;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		if (c.isEmpty() || isEmpty()) {
			return false;
		}
		final GridCellSelectionList doomed = new GridCellSelectionList();
		for (final Object o : c) {
			if (o instanceof Point) {
				doomed.inc((Point) o, 0);
			}
		}
		return removeIf(doomed::contains);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		final GridCellSelectionList kept = new GridCellSelectionList();
		for (final Object o : c) {
			if (o instanceof Point) {
				kept.inc((Point) o, 0);
			}
		}
		return removeIf(p -> !kept.contains(p));
	}

	@Override
	public boolean removeIf(final Predicate<? super Point> filter) {
		compact();
		int to = 0;
		for (int i = 0; i < end; i++) {
			if (!filter.test(cells[i])) {
				cells[to++] = cells[i];
			}
		}
		if (to == end) {
			return false;
		}
		Arrays.fill(cells, to, end, null);
		end = to;
		live = to;
		modCount++;
		rebuild();
		return true;
	}

	@Override
	public void clear() {
		Arrays.fill(cells, head, end, null);
		head = end = live = 0;
		if (used > 0) {
			Arrays.fill(counts, 0);
			used = 0;
		}
		Arrays.fill(columnCounts, 0);
		modCount++;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(final T[] a) {
		compact();
		final T[] array = a.length >= live ? a
				: (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), live);
		for (int i = 0; i < live; i++) {
			array[i] = (T) new Point(cells[i].x, cells[i].y);
		}
		if (array.length > live) {
			array[live] = null;
		}
		return array;
	}
}
