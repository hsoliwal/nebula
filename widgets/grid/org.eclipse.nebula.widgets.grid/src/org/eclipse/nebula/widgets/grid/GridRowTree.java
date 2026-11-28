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
 * The AVL lanes (rebalance, rotations, successor detach) are adapted from
 * hsoliwal/eclipse.platform.swt VirtualTreeSiblingIndex.java (PR #197,
 * m3/viewport-integration-20261016 cbc458de66, EPL-2.0), itself adapted from
 * hsoliwal/com.synexia synexia-primitives LongOrderStatisticMultiset.java
 * (b1b665299048, Apache-2.0). See docs/m3/grid-viewport/FOSS_REUSE_DECISION.tsv.
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.nebula.widgets.grid.model.HeapGrowth;

/**
 * The item list of a Grid in the viewport mode: a height-balanced tree ordered by row, whose nodes
 * are lane slots (one per item) and whose subtrees count their items, their visible rows and the
 * pixel extent of those rows. Inserting or removing a row anywhere, finding the row of an item,
 * the item of a row, and every rank, select and pixel prefix question of {@code GridRowIndex} are
 * O(log n); nothing is rebuilt and no other item is touched, so no structural change costs O(n)
 * on the UI thread. An item's extent is its height plus the one-pixel line when it is visible, 0
 * when it is hidden; {@code update(GridItem)} records a change of either.
 * <p>
 * In an {@code SWT.VIRTUAL} tree Grid a row may have no item yet (G4 step 2c): a child that
 * {@link GridItem#setItemCount(int)} created and nobody has read. Its slot keeps its parent in the
 * owner lane and its extent in the weight lane; {@code get(int)} and the iterator make its item
 * through the Grid ({@code setFacades(java.util.function.IntFunction)}), {@code materialized()}
 * walks only the items made so far.
 * </p>
 * <p>
 * Density: every slot keeps five 4-byte lanes (left, right, up, size, item) and one byte (AVL
 * height, the batch's dirty mark and whether the row is shown). The other lanes are made only when
 * a row needs them, and their answers are derived until then: the visible lane while every row is
 * shown (a subtree's visible rows are its size), the weight and sum lanes while every shown row
 * has the same extent (a subtree's extent is its visible rows times that extent) and the owner
 * lane until the first row without an item. A Grid of n uniform rows thus keeps 21 bytes per slot
 * instead of 50. The weight and sum lanes are given up again when the Grid gives every row one
 * height ({@link #setVisibleExtent(int)}).
 * </p>
 */
final class GridRowTree extends AbstractList<GridItem> implements GridRowIndex {
	private static final int NONE = -1;
	/* bytes per slot of the lanes every tree keeps: left, right, up, size (int), item (ref), height (byte) */
	private static final int SLOT_BYTES = 21;

	/* the height byte: AVL height (bits 0..5, at most 45 for 2^31 slots), dirty mark, shown */
	private static final int HEIGHT = 0x3F;
	private static final int DIRTY = 0x40;
	private static final int SHOWN = 0x80;

	/* the parent item of each row without an item (null for a row with one); null until the first such row */
	private GridItem[] owners;
	private int unread;
	private java.util.function.IntFunction<GridItem> facades;

	private int root = NONE;
	private int[] left = new int[0];
	private int[] right = new int[0];
	private int[] up = new int[0];
	private byte[] heights = new byte[0];
	private int[] sizes = new int[0];
	/* the visible rows of each subtree; null while every row is shown (they equal the sizes) */
	private int[] visibles;
	/* the extent of each slot and of each subtree; null while every shown row's extent is uniform */
	private int[] weights;
	private long[] sums;
	/* the extent of every shown row while weights is null */
	private int uniform;
	/* the rows of the tree that are shown (extent above 0), whatever the batch state */
	private int shown;
	private GridItem[] items = new GridItem[0];
	private int[] free = new int[0];
	private int freeCount;
	private int nextSlot;

	@Override
	public int size() {
		return size(root);
	}

	@Override
	public GridItem get(final int index) {
		checkIndex(index, size());
		return itemOf(slotAt(index));
	}

	/* the item of a slot, made now for a row nobody has read */
	private GridItem itemOf(final int slot) {
		final GridItem item = items[slot];
		return item != null ? item : facades.apply(slot);
	}

	/** @param facades makes the item of a row without one (the Grid), given the row's slot */
	void setFacades(final java.util.function.IntFunction<GridItem> facades) {
		this.facades = facades;
	}

	/**
	 * Inserts rows without items: children of {@code owner} that nobody has read.
	 *
	 * @param index  the row of the first
	 * @param owner  their parent item
	 * @param count  how many
	 * @param weight the extent of each (0 for hidden rows)
	 * @return their slots, in row order
	 */
	int[] insertUnread(final int index, final GridItem owner, final int count, final int weight) {
		settle();
		if (index < 0 || index > size()) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + size());
		}
		if (count > 0) {
			admit(NONE, weight);
		}
		ensureCapacity(nextSlot + count - Math.min(count, freeCount));
		if (owners == null && count > 0) {
			owners = new GridItem[items.length];
		}
		final int[] slots = new int[count];
		for (int i = 0; i < count; i++) {
			final int slot = allocate();
			owners[slot] = owner;
			putWeight(slot, weight);
			if (weight > 0) {
				shown++;
			}
			root = insertAt(root, index + i, slot);
			up[root] = NONE;
			slots[i] = slot;
		}
		unread += count;
		modCount++;
		return slots;
	}

	/** Gives a row without an item the item just made for it. */
	void bind(final int slot, final GridItem item) {
		items[slot] = item;
		if (owners != null) {
			owners[slot] = null;
		}
		unread--;
		item.enteredRowTree(slot);
	}

	/** @return the parent item of a row without an item, else {@code null} */
	GridItem ownerOf(final int slot) {
		return owners == null ? null : owners[slot];
	}

	/** @return whether some rows have no item yet */
	boolean hasUnread() {
		return unread > 0;
	}

	/** @return the number of rows with an item */
	int materializedCount() {
		return size() - unread;
	}

	/** @return the slot of a row, O(log n) (rows without items included) */
	int slotAtRow(final int row) {
		checkIndex(row, size());
		return slotAt(row);
	}

	/** @return the row of a slot, O(log n) */
	int rowOfSlotIndex(final int slot) {
		return rowOfSlot(slot);
	}

	/** @return the extent of a slot (0: hidden) */
	int weightOf(final int slot) {
		return weight(slot);
	}

	/** Sets the extent of a slot, O(log n). */
	void reweighSlot(final int slot, final int weight) {
		reweigh(slot, weight);
	}

	/** Removes a row without an item, O(log n). */
	void removeUnread(final int slot) {
		settle();
		final int index = rowOfSlot(slot);
		root = removeAt(root, index);
		if (root != NONE) {
			up[root] = NONE;
		}
		if (isShown(slot)) {
			shown--;
		}
		unread--;
		release(slot);
		if (root == NONE) {
			forgetLanes();
		}
		modCount++;
	}

	/** Sets the extent of every visible row without an item (the Grid's item height changed). */
	void reweighUnread(final int visibleWeight) {
		settle();
		if (unread == 0) {
			return;
		}
		for (int slot = first(root); slot != NONE; slot = successor(slot)) {
			if (items[slot] == null && weight(slot) > 0) {
				reweigh(slot, visibleWeight);
			}
		}
	}

	/** @return the items made so far, in row order; walking it makes none */
	Iterable<GridItem> materialized() {
		return () -> new Iterator<GridItem>() {
			private int next = skipUnread(first(root));

			@Override
			public boolean hasNext() {
				return next != NONE;
			}

			@Override
			public GridItem next() {
				if (next == NONE) {
					throw new NoSuchElementException();
				}
				final GridItem item = items[next];
				next = skipUnread(successor(next));
				return item;
			}
		};
	}

	private int skipUnread(final int from) {
		int slot = from;
		while (slot != NONE && items[slot] == null) {
			slot = successor(slot);
		}
		return slot;
	}

	@Override
	public void add(final int index, final GridItem item) {
		settle();
		if (index < 0 || index > size()) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + size());
		}
		final int weight = extentOf(item);
		admit(NONE, weight);
		final int slot = allocate();
		items[slot] = item;
		putWeight(slot, weight);
		if (weight > 0) {
			shown++;
		}
		root = insertAt(root, index, slot);
		up[root] = NONE;
		item.enteredRowTree(slot);
		modCount++;
	}

	@Override
	public GridItem remove(final int index) {
		settle();
		checkIndex(index, size());
		final int slot = slotAt(index);
		final GridItem item = itemOf(slot);
		root = removeAt(root, index);
		if (root != NONE) {
			up[root] = NONE;
		}
		if (isShown(slot)) {
			shown--;
		}
		release(slot);
		if (root == NONE) {
			forgetLanes();
		}
		item.leftRowTree(index);
		modCount++;
		return item;
	}

	@Override
	public boolean remove(final Object o) {
		final int index = indexOf(o);
		if (index < 0) {
			return false;
		}
		remove(index);
		return true;
	}

	@Override
	public GridItem set(final int index, final GridItem item) {
		settle();
		checkIndex(index, size());
		final int slot = slotAt(index);
		final GridItem old = itemOf(slot);
		old.leftRowTree(index);
		items[slot] = item;
		item.enteredRowTree(slot);
		reweigh(slot, extentOf(item));
		return old;
	}

	@Override
	public int indexOf(final Object o) {
		final int slot = slotOf(o);
		return slot < 0 ? -1 : rowOfSlot(slot);
	}

	@Override
	public int lastIndexOf(final Object o) {
		// an item is in the list once at most
		return indexOf(o);
	}

	@Override
	public boolean contains(final Object o) {
		return slotOf(o) >= 0;
	}

	@Override
	public void clear() {
		settle();
		int row = 0;
		for (int slot = first(root); slot != NONE; slot = successor(slot)) {
			if (items[slot] != null) {
				items[slot].leftRowTree(row);
			}
			row++;
		}
		root = NONE;
		Arrays.fill(items, 0, nextSlot, null);
		forgetLanes();
		unread = 0;
		nextSlot = 0;
		freeCount = 0;
		modCount++;
	}

	/* no row is left: every lane made on demand goes, the answers are derived again */
	private void forgetLanes() {
		owners = null;
		visibles = null;
		weights = null;
		sums = null;
		uniform = 0;
		shown = 0;
	}

	@Override
	public Iterator<GridItem> iterator() {
		return new Iterator<GridItem>() {
			private int next = first(root);
			private final int expectedModCount = modCount;

			@Override
			public boolean hasNext() {
				return next != NONE;
			}

			@Override
			public GridItem next() {
				if (modCount != expectedModCount) {
					throw new ConcurrentModificationException();
				}
				if (next == NONE) {
					throw new NoSuchElementException();
				}
				final GridItem item = itemOf(next);
				next = successor(next);
				return item;
			}
		};
	}

	@Override
	public Object[] toArray() {
		return toArray(new Object[size()]);
	}

	/*
	 * The rows in order, each placed by its subtree size in one descent per subtree (no iterator,
	 * no climb to a successor): Grid.getItems() and, while the Grid has no child items, its root
	 * items. A row without an item gets one, as the iterator gives it.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(final T[] a) {
		final int size = size();
		final T[] out = a.length >= size ? a
				: (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
		fill(root, out, 0);
		if (out.length > size) {
			out[size] = null;
		}
		return out;
	}

	private void fill(final int subtree, final Object[] out, final int from) {
		int node = subtree;
		int offset = from;
		while (node != NONE) {
			fill(left[node], out, offset);
			offset += size(left[node]);
			out[offset++] = itemOf(node);
			node = right[node];
		}
	}

	/**
	 * @param selected items of this list
	 * @return their rows, in their order: O(k log n) for few items, one O(n) walk for many
	 */
	int[] rowsOf(final java.util.List<GridItem> selected) {
		final int[] rows = new int[selected.size()];
		int i = 0;
		if ((long) selected.size() * 16 < size()) {
			for (final GridItem item : selected) {
				final int slot = slotOf(item);
				rows[i++] = slot < 0 ? item.getRowIndex() : rowOfSlot(slot);
			}
			return rows;
		}
		final int[] rowOfSlot = new int[nextSlot];
		int row = 0;
		for (int slot = first(root); slot != NONE; slot = successor(slot)) {
			rowOfSlot[slot] = row++;
		}
		for (final GridItem item : selected) {
			final int slot = slotOf(item);
			rows[i++] = slot < 0 ? item.getRowIndex() : rowOfSlot[slot];
		}
		return rows;
	}

	/**
	 * Records that the visibility or the height of {@code item} changed; O(log n).
	 *
	 * @param item an item of this list (others are ignored)
	 */
	void update(final GridItem item) {
		final int slot = slotOf(item);
		if (slot >= 0) {
			reweigh(slot, extentOf(item));
		}
	}

	@Override
	public int visibleCount() {
		settle();
		return visible(root);
	}

	@Override
	public long totalExtent() {
		settle();
		return sum(root);
	}

	@Override
	public int extentAt(final int position) {
		return weight(slotAt(position));
	}

	@Override
	public int rankOf(final int position) {
		settle();
		int k = position;
		int rank = 0;
		int node = root;
		while (node != NONE && k > 0) {
			final int before = size(left[node]);
			if (k <= before) {
				node = left[node];
			} else {
				rank += visible(left[node]) + (isShown(node) ? 1 : 0);
				k -= before + 1;
				node = right[node];
			}
		}
		return rank;
	}

	@Override
	public long extentBefore(final int position) {
		settle();
		int k = position;
		long extent = 0;
		int node = root;
		while (node != NONE && k > 0) {
			final int before = size(left[node]);
			if (k <= before) {
				node = left[node];
			} else {
				extent += sum(left[node]) + weight(node);
				k -= before + 1;
				node = right[node];
			}
		}
		return extent;
	}

	@Override
	public int positionOfRank(final int rank) {
		settle();
		int remaining = rank;
		int position = 0;
		int node = root;
		while (node != NONE) {
			final int before = visible(left[node]);
			if (remaining < before) {
				node = left[node];
				continue;
			}
			final int own = isShown(node) ? 1 : 0;
			if (remaining < before + own) {
				return position + size(left[node]);
			}
			remaining -= before + own;
			position += size(left[node]) + 1;
			node = right[node];
		}
		throw new IndexOutOfBoundsException("visible row " + rank + ", visible rows " + visibleCount());
	}

	@Override
	public long extentOfRank(final int rank) {
		if (rank == 0) {
			return 0;
		}
		settle();
		final int count = visible(root);
		if (rank == count) {
			return sum(root);
		}
		int remaining = rank;
		long extent = 0;
		int node = root;
		// Select the visible row and accumulate its pixel prefix in the same descent.
		while (node != NONE) {
			final int before = visible(left[node]);
			if (remaining < before) {
				node = left[node];
				continue;
			}
			final int own = isShown(node) ? 1 : 0;
			if (remaining < before + own) {
				return extent + sum(left[node]);
			}
			extent += sum(left[node]) + weight(node);
			remaining -= before + own;
			node = right[node];
		}
		throw new IndexOutOfBoundsException("visible row " + rank + ", visible rows " + count);
	}

	@Override
	public int rankAtExtent(final long offset) {
		settle();
		if (offset < 0) {
			return -1;
		}
		int rank = 0;
		long extent = 0;
		int node = root;
		while (node != NONE) {
			if (extent + sum(left[node]) > offset) {
				node = left[node];
				continue;
			}
			extent += sum(left[node]);
			rank += visible(left[node]);
			if (extent + weight(node) > offset) {
				return rank;
			}
			extent += weight(node);
			rank += isShown(node) ? 1 : 0;
			node = right[node];
		}
		return rank;
	}

	@Override
	public int nextVisible(final int position) {
		settle();
		return findVisibleAfter(root, 0, position);
	}

	@Override
	public int previousVisible(final int position) {
		settle();
		return findVisibleBefore(root, 0, position);
	}

	/* Select the closest visible position directly from the native Grid's packed lanes.
	 * Subtrees without visible rows are pruned; no GridItem or iterator is made. */
	private int findVisibleAfter(final int node, final int start, final int position) {
		if (node == NONE || visible(node) == 0) {
			return -1;
		}
		final int index = start + size(left[node]);
		if (index >= position) {
			final int fromLeft = findVisibleAfter(left[node], start, position);
			if (fromLeft != -1) {
				return fromLeft;
			}
			if (isShown(node)) {
				return index;
			}
		}
		return findVisibleAfter(right[node], index + 1, position);
	}

	private int findVisibleBefore(final int node, final int start, final int position) {
		if (node == NONE || visible(node) == 0) {
			return -1;
		}
		final int index = start + size(left[node]);
		if (index <= position) {
			final int fromRight = findVisibleBefore(right[node], index + 1, position);
			if (fromRight != -1) {
				return fromRight;
			}
			if (isShown(node)) {
				return index;
			}
		}
		return findVisibleBefore(left[node], start, position);
	}

	/* the extent a visible item adds: its height and the one-pixel line below it */
	private static int extentOf(final GridItem item) {
		return item.isVisible() ? item.getHeight() + 1 : 0;
	}

	private int slotOf(final Object o) {
		if (!(o instanceof GridItem)) {
			return NONE;
		}
		final int slot = ((GridItem) o).rowTreeSlot();
		return slot >= 0 && slot < nextSlot && items[slot] == o ? slot : NONE;
	}

	private int slotAt(final int index) {
		int k = index;
		int node = root;
		while (true) {
			final int before = size(left[node]);
			if (k < before) {
				node = left[node];
			} else if (k == before) {
				return node;
			} else {
				k -= before + 1;
				node = right[node];
			}
		}
	}

	private int rowOfSlot(final int slot) {
		int row = size(left[slot]);
		for (int node = slot; up[node] != NONE; node = up[node]) {
			final int parent = up[node];
			if (right[parent] == node) {
				row += size(left[parent]) + 1;
			}
		}
		return row;
	}

	private void reweigh(final int slot, final int weight) {
		final int old = weight(slot);
		final long delta = (long) weight - old;
		final int visibleDelta = (weight > 0 ? 1 : 0) - (old > 0 ? 1 : 0);
		if (delta == 0 && visibleDelta == 0) {
			return;
		}
		admit(slot, weight);
		putWeight(slot, weight);
		shown += visibleDelta;
		if (visibles == null && sums == null) {
			// both aggregates are derived from the sizes: nothing to carry up
			return;
		}
		if (batchDepth > 0) {
			// marks the path to the root up to the first marked node: k changes in a subtree mark
			// O(k + log n) nodes, fixed once by settle()
			for (int node = slot; node != NONE && (heights[node] & DIRTY) == 0; node = up[node]) {
				heights[node] |= DIRTY;
			}
			anyDirty = true;
			return;
		}
		for (int node = slot; node != NONE; node = up[node]) {
			if (sums != null) {
				sums[node] += delta;
			}
			if (visibles != null) {
				visibles[node] += visibleDelta;
			}
		}
	}

	/* whether a slot's row is shown (its extent is above 0) */
	private boolean isShown(final int slot) {
		return (heights[slot] & SHOWN) != 0;
	}

	/* the extent of a slot: its lane, or the uniform extent of a shown row */
	private int weight(final int slot) {
		if (weights != null) {
			return weights[slot];
		}
		return isShown(slot) ? uniform : 0;
	}

	/* records the extent of a slot (the aggregates are the caller's) */
	private void putWeight(final int slot, final int weight) {
		if (weights != null) {
			weights[slot] = weight;
		}
		heights[slot] = (byte) (weight > 0 ? heights[slot] | SHOWN : heights[slot] & ~SHOWN);
	}

	/*
	 * Makes the lanes able to hold the extent weight at slot (NONE: a row not in the tree yet),
	 * before it is recorded: the visible lane for the first hidden row, the weight and sum lanes for
	 * the first shown row whose extent differs from the uniform one while another row is shown.
	 * When no other row is shown the uniform extent simply becomes weight. Each lane is made once,
	 * O(n), from the answers that were derived until then.
	 */
	private void admit(final int slot, final int weight) {
		if (weight > 0) {
			if (weights == null && weight != uniform) {
				final int others = shown - (slot != NONE && isShown(slot) ? 1 : 0);
				if (others == 0) {
					uniform = weight;
				} else {
					makeWeights();
				}
			}
			return;
		}
		if (visibles == null) {
			makeVisibles();
		}
		if (weight != 0 && weights == null) {
			makeWeights();
		}
	}

	/* every row was shown: the visible rows of each subtree are its size */
	private void makeVisibles() {
		final int[] lane = new int[items.length];
		System.arraycopy(sizes, 0, lane, 0, nextSlot);
		visibles = lane;
	}

	/* every shown row had the uniform extent: each subtree's extent is its visible rows times it */
	private void makeWeights() {
		final int[] weightLane = new int[items.length];
		final long[] sumLane = new long[items.length];
		for (int slot = 0; slot < nextSlot; slot++) {
			if (isShown(slot)) {
				weightLane[slot] = uniform;
			}
			sumLane[slot] = (long) visible(slot) * uniform;
		}
		weights = weightLane;
		sums = sumLane;
	}

	/* batched reweighs (expand, collapse, a uniform height): weights set at once, aggregates fixed once */
	private int batchDepth;
	private boolean anyDirty;

	/** Starts a batch of extent changes (nested); the aggregates are fixed at its end or first read. */
	void beginBatch() {
		batchDepth++;
	}

	/** Ends a batch of extent changes: the marked nodes are fixed, O(changed + log n). */
	void endBatch() {
		if (--batchDepth == 0) {
			settle();
		}
	}

	/* fixes the aggregates of the marked nodes, children before parents */
	private void settle() {
		if (anyDirty) {
			anyDirty = false;
			fix(root);
		}
	}

	private void fix(final int node) {
		if (node == NONE || (heights[node] & DIRTY) == 0) {
			return;
		}
		fix(left[node]);
		fix(right[node]);
		refresh(node);
		heights[node] &= ~DIRTY;
	}

	/**
	 * Gives every visible row the extent {@code extent} (the Grid's item height changed for all
	 * rows) in one pass over the slots and one over the tree: O(n), with no call per item. Every
	 * shown row then has the same extent, so the weight and sum lanes are given up (derived again).
	 *
	 * @param extent the new extent of a visible row (height plus line)
	 */
	void setVisibleExtent(final int extent) {
		settle();
		if (extent > 0) {
			if (weights == null) {
				uniform = extent;
				return;
			}
			boolean uniformAfter = true;
			for (int slot = 0; slot < nextSlot && uniformAfter; slot++) {
				// a hidden row with a negative extent keeps it: only then are the lanes still needed
				uniformAfter = weights[slot] >= 0;
			}
			if (uniformAfter) {
				weights = null;
				sums = null;
				uniform = extent;
				return;
			}
		} else {
			// every shown row becomes hidden with this extent: the lanes that answer it
			if (visibles == null) {
				makeVisibles();
			}
			if (weights == null) {
				makeWeights();
			}
			shown = 0;
		}
		for (int slot = 0; slot < nextSlot; slot++) {
			if (weights[slot] > 0) {
				putWeight(slot, extent);
			}
		}
		refreshAll(root);
	}

	private void refreshAll(final int node) {
		if (node == NONE) {
			return;
		}
		refreshAll(left[node]);
		refreshAll(right[node]);
		refresh(node);
	}

	private int first(final int node) {
		int first = node;
		if (first == NONE) {
			return NONE;
		}
		while (left[first] != NONE) {
			first = left[first];
		}
		return first;
	}

	private int successor(final int slot) {
		if (right[slot] != NONE) {
			return first(right[slot]);
		}
		int node = slot;
		while (up[node] != NONE && right[up[node]] == node) {
			node = up[node];
		}
		return up[node];
	}

	private int insertAt(final int node, final int index, final int slot) {
		if (node == NONE) {
			left[slot] = right[slot] = up[slot] = NONE;
			refresh(slot);
			return slot;
		}
		final int before = size(left[node]);
		if (index <= before) {
			final int child = insertAt(left[node], index, slot);
			left[node] = child;
			up[child] = node;
		} else {
			final int child = insertAt(right[node], index - before - 1, slot);
			right[node] = child;
			up[child] = node;
		}
		return rebalance(node);
	}

	private int removeAt(final int node, final int index) {
		final int before = size(left[node]);
		if (index < before) {
			final int child = removeAt(left[node], index);
			left[node] = child;
			if (child != NONE) {
				up[child] = node;
			}
		} else if (index > before) {
			final int child = removeAt(right[node], index - before - 1);
			right[node] = child;
			if (child != NONE) {
				up[child] = node;
			}
		} else {
			return detach(node);
		}
		return rebalance(node);
	}

	/* the successor takes the removed slot's place (as in the donor) */
	private int detach(final int slot) {
		final int l = left[slot], r = right[slot];
		left[slot] = right[slot] = up[slot] = NONE;
		if (l == NONE) {
			return r;
		}
		if (r == NONE) {
			return l;
		}
		final int successor = first(r);
		final int rest = removeFirst(r);
		right[successor] = rest;
		if (rest != NONE) {
			up[rest] = successor;
		}
		left[successor] = l;
		up[l] = successor;
		return rebalance(successor);
	}

	private int removeFirst(final int node) {
		if (left[node] == NONE) {
			return right[node];
		}
		final int child = removeFirst(left[node]);
		left[node] = child;
		if (child != NONE) {
			up[child] = node;
		}
		return rebalance(node);
	}

	private int rebalance(final int node) {
		refresh(node);
		final int balance = height(left[node]) - height(right[node]);
		if (balance > 1) {
			if (height(left[left[node]]) < height(right[left[node]])) {
				left[node] = rotateLeft(left[node]);
				up[left[node]] = node;
			}
			return rotateRight(node);
		}
		if (balance < -1) {
			if (height(right[right[node]]) < height(left[right[node]])) {
				right[node] = rotateRight(right[node]);
				up[right[node]] = node;
			}
			return rotateLeft(node);
		}
		return node;
	}

	private int rotateLeft(final int node) {
		final int pivot = right[node];
		right[node] = left[pivot];
		if (right[node] != NONE) {
			up[right[node]] = node;
		}
		left[pivot] = node;
		up[node] = pivot;
		refresh(node);
		refresh(pivot);
		return pivot;
	}

	private int rotateRight(final int node) {
		final int pivot = left[node];
		left[node] = right[pivot];
		if (left[node] != NONE) {
			up[left[node]] = node;
		}
		right[pivot] = node;
		up[node] = pivot;
		refresh(node);
		refresh(pivot);
		return pivot;
	}

	private void refresh(final int node) {
		final int l = left[node], r = right[node];
		sizes[node] = 1 + size(l) + size(r);
		if (visibles != null) {
			visibles[node] = (isShown(node) ? 1 : 0) + visible(l) + visible(r);
		}
		if (sums != null) {
			sums[node] = weights[node] + sum(l) + sum(r);
		}
		heights[node] = (byte) ((heights[node] & (DIRTY | SHOWN)) | (1 + Math.max(height(l), height(r))));
	}

	private int height(final int node) {
		return node == NONE ? 0 : heights[node] & HEIGHT;
	}

	private int size(final int node) {
		return node == NONE ? 0 : sizes[node];
	}

	/* the visible rows of a subtree: all of it while every row is shown */
	private int visible(final int node) {
		if (node == NONE) {
			return 0;
		}
		return visibles != null ? visibles[node] : sizes[node];
	}

	/* the extent of a subtree: its visible rows times the uniform extent while all shown rows have it */
	private long sum(final int node) {
		if (node == NONE) {
			return 0;
		}
		return sums != null ? sums[node] : (long) visible(node) * uniform;
	}

	private int allocate() {
		final int slot;
		if (freeCount > 0) {
			slot = free[--freeCount];
		} else {
			ensureCapacity(nextSlot + 1);
			slot = nextSlot++;
		}
		heights[slot] = 0;
		return slot;
	}

	private void release(final int slot) {
		items[slot] = null;
		if (owners != null) {
			owners[slot] = null;
		}
		if (weights != null) {
			weights[slot] = 0;
		}
		heights[slot] = 0;
		if (freeCount == free.length) {
			free = Arrays.copyOf(free, Math.max(8, free.length * 2));
		}
		free[freeCount++] = slot;
	}

	/* bytes per slot over the lanes this tree keeps now (for the heap-aware growth) */
	private int slotBytes() {
		return SLOT_BYTES + (owners != null ? 4 : 0) + (visibles != null ? 4 : 0) + (weights != null ? 12 : 0);
	}

	/**
	 * @return the bytes per slot of the lanes this tree keeps: 21 for rows that are all shown with
	 *         one extent, up to 41 (tests; see the class comment)
	 */
	int laneBytesPerSlot() {
		return slotBytes();
	}

	private void ensureCapacity(final int slots) {
		if (slots <= items.length) {
			return;
		}
		// a quarter more at most: a slot is 21 to 41 bytes over all lanes, and doubling them left up
		// to a whole spare slot per row (the stock ArrayList adds a half of 4-byte references)
		final int quarter = Math.max(16, items.length + (items.length >> 2));
		final int capacity = Math.min(HeapGrowth.capacity(items.length, slots, slotBytes()), Math.max(slots, quarter));
		// stage every lane before publishing, so a failed allocation leaves the tree unchanged
		final int[] nextLeft = Arrays.copyOf(left, capacity);
		final int[] nextRight = Arrays.copyOf(right, capacity);
		final int[] nextUp = Arrays.copyOf(up, capacity);
		final byte[] nextHeights = Arrays.copyOf(heights, capacity);
		final int[] nextSizes = Arrays.copyOf(sizes, capacity);
		final int[] nextVisibles = visibles == null ? null : Arrays.copyOf(visibles, capacity);
		final int[] nextWeights = weights == null ? null : Arrays.copyOf(weights, capacity);
		final long[] nextSums = sums == null ? null : Arrays.copyOf(sums, capacity);
		final GridItem[] nextItems = Arrays.copyOf(items, capacity);
		final GridItem[] nextOwners = owners == null ? null : Arrays.copyOf(owners, capacity);
		left = nextLeft;
		right = nextRight;
		up = nextUp;
		heights = nextHeights;
		sizes = nextSizes;
		visibles = nextVisibles;
		weights = nextWeights;
		sums = nextSums;
		items = nextItems;
		owners = nextOwners;
	}

	private static void checkIndex(final int index, final int size) {
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + size);
		}
	}
}
