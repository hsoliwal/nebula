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
import java.util.List;

/**
 * The children of an item of an {@code SWT.VIRTUAL} tree Grid in the viewport mode (G4 step 2c):
 * two lanes in child order, the row-tree slot of every child and its {@link GridItem}, which is
 * {@code null} until the child is first read. A child nobody has read is a row of the row tree and
 * nothing else; {@link #get(int)} makes its item (a facade on that row). The list never drops an
 * item it made, so a child without one was never asked for its data.
 */
final class GridChildList extends AbstractList<GridItem> {
	private final GridItem owner;
	private int[] slots;
	private GridItem[] items;
	private int size;

	/**
	 * @param owner    the parent item
	 * @param children its current children, each in the row tree
	 */
	GridChildList(final GridItem owner, final List<GridItem> children) {
		this.owner = owner;
		final int n = children == null ? 0 : children.size();
		slots = new int[Math.max(4, n)];
		items = new GridItem[slots.length];
		for (int i = 0; i < n; i++) {
			final GridItem child = children.get(i);
			slots[i] = child.rowTreeSlot();
			items[i] = child;
		}
		size = n;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public GridItem get(final int index) {
		check(index);
		final GridItem item = items[index];
		return item != null ? item : owner.getParent().materializeChild(owner, index, slots[index]);
	}

	/** @return the child's item, or {@code null} when nobody has read the child yet */
	GridItem materialized(final int index) {
		return items[index];
	}

	/** @return the row-tree slot of the child */
	int slot(final int index) {
		return slots[index];
	}

	/** Records the item made for a child. */
	void bind(final int index, final GridItem item) {
		items[index] = item;
	}

	/**
	 * @param slot     a row-tree slot of one of the children
	 * @param rowTree  the Grid's row tree
	 * @return the child index of the slot: a binary search over the children's rows
	 */
	int indexOfSlot(final int slot, final GridRowTree rowTree) {
		final int row = rowTree.rowOfSlotIndex(slot);
		int low = 0, high = size - 1;
		while (low <= high) {
			final int mid = (low + high) >>> 1;
			final int midRow = rowTree.rowOfSlotIndex(slots[mid]);
			if (midRow < row) {
				low = mid + 1;
			} else if (midRow > row) {
				high = mid - 1;
			} else {
				return mid;
			}
		}
		return -1;
	}

	/** Appends children that have rows (slots) but no items. */
	void appendSlots(final int[] newSlots) {
		ensure(size + newSlots.length);
		System.arraycopy(newSlots, 0, slots, size, newSlots.length);
		size += newSlots.length;
	}

	@Override
	public void add(final int index, final GridItem item) {
		if (index < 0 || index > size) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + size);
		}
		ensure(size + 1);
		System.arraycopy(slots, index, slots, index + 1, size - index);
		System.arraycopy(items, index, items, index + 1, size - index);
		slots[index] = item.rowTreeSlot();
		items[index] = item;
		size++;
		modCount++;
	}

	@Override
	public GridItem remove(final int index) {
		check(index);
		final GridItem item = items[index];
		System.arraycopy(slots, index + 1, slots, index, size - index - 1);
		System.arraycopy(items, index + 1, items, index, size - index - 1);
		size--;
		items[size] = null;
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
	public int indexOf(final Object o) {
		if (!(o instanceof GridItem)) {
			return -1;
		}
		for (int i = 0; i < size; i++) {
			if (items[i] == o) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean contains(final Object o) {
		return indexOf(o) >= 0;
	}

	@Override
	public void clear() {
		Arrays.fill(items, 0, size, null);
		size = 0;
		modCount++;
	}

	/** @return the items made so far, in child order (no item is made) */
	GridItem[] materializedArray() {
		int n = 0;
		for (int i = 0; i < size; i++) {
			if (items[i] != null) {
				n++;
			}
		}
		final GridItem[] result = new GridItem[n];
		n = 0;
		for (int i = 0; i < size; i++) {
			if (items[i] != null) {
				result[n++] = items[i];
			}
		}
		return result;
	}

	private void ensure(final int capacity) {
		if (capacity > slots.length) {
			final int grown = HeapGrowth.capacity(slots.length, capacity, 8);
			slots = Arrays.copyOf(slots, grown);
			items = Arrays.copyOf(items, grown);
		}
	}

	private void check(final int index) {
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + size);
		}
	}
}
