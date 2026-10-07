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
package org.eclipse.nebula.widgets.grid;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sparse logical row owner for a flat {@link Grid} created with SWT.VIRTUAL.
 *
 * <p>The list size is the logical row count.  Only coordinates whose
 * {@link GridItem} facade has actually been requested are retained in the
 * parallel sorted index/value lanes.  Public indexed access therefore keeps the
 * long-standing Grid API while a million logical rows no longer imply a million
 * Java item objects.</p>
 *
 * <p>This owner is intentionally package-private. Tree-mode Grid keeps the
 * existing dense topology for now; Grid converts this owner to a fully
 * materialized state before entering tree mode.</p>
 */
final class GridVirtualItemList extends AbstractList<GridItem> {

    private final Grid grid;
    private int logicalSize;
    private int[] indices = new int[4];
    private GridItem[] values = new GridItem[4];
    private int materializedCount;

    GridVirtualItemList(final Grid grid) {
        this.grid = grid;
    }

    @Override
    public int size() {
        return logicalSize;
    }

    @Override
    public GridItem get(final int index) {
        checkElementIndex(index);
        int slot = find(index);
        if (slot >= 0) {
            return values[slot];
        }

        final GridItem item = grid.materializeVirtualItem(index);
        bind(index, item);
        return item;
    }

    @Override
    public void add(final int index, final GridItem item) {
        if (index < 0 || index > logicalSize) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + logicalSize);
        }
        if (item == null) {
            throw new NullPointerException("item");
        }

        final int slot = insertionPoint(index);
        ensureCapacity(materializedCount + 1);
        System.arraycopy(indices, slot, indices, slot + 1, materializedCount - slot);
        System.arraycopy(values, slot, values, slot + 1, materializedCount - slot);
        for (int i = slot + 1; i <= materializedCount; i++) {
            indices[i]++;
            values[i].increaseRow();
        }
        indices[slot] = index;
        values[slot] = item;
        materializedCount++;
        logicalSize++;
        modCount++;
    }

    @Override
    public boolean add(final GridItem item) {
        add(logicalSize, item);
        return true;
    }

    @Override
    public GridItem remove(final int index) {
        checkElementIndex(index);
        final int slot = find(index);
        final GridItem result = slot >= 0 ? values[slot] : null;
        removeLogicalCoordinate(index, slot);
        return result;
    }

    @Override
    public boolean remove(final Object value) {
        final int slot = identitySlot(value);
        if (slot < 0) {
            return false;
        }
        removeLogicalCoordinate(indices[slot], slot);
        return true;
    }

    @Override
    public int indexOf(final Object value) {
        final int slot = identitySlot(value);
        return slot < 0 ? -1 : indices[slot];
    }

    @Override
    public boolean contains(final Object value) {
        return identitySlot(value) >= 0;
    }

    @Override
    public void clear() {
        Arrays.fill(values, 0, materializedCount, null);
        materializedCount = 0;
        logicalSize = 0;
        modCount++;
    }

    int materializedCount() {
        return materializedCount;
    }

    GridItem getMaterialized(final int index) {
        if (index < 0 || index >= logicalSize) {
            return null;
        }
        final int slot = find(index);
        return slot >= 0 ? values[slot] : null;
    }

    List<GridItem> materializedSnapshot() {
        final List<GridItem> result = new ArrayList<>(materializedCount);
        for (int i = 0; i < materializedCount; i++) {
            result.add(values[i]);
        }
        return result;
    }

    /**
     * Changes only the logical extent. Materialized facades beyond a shrinking
     * boundary are detached and returned in descending logical order so callers
     * can preserve Grid's historical reverse-disposal order.
     */
    List<GridItem> setLogicalSize(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("negative logical size");
        }
        if (count == logicalSize) {
            return List.of();
        }

        final List<GridItem> removed = new ArrayList<>();
        if (count < logicalSize) {
            final int keep = insertionPoint(count);
            for (int i = materializedCount - 1; i >= keep; i--) {
                removed.add(values[i]);
                values[i] = null;
            }
            materializedCount = keep;
        }
        logicalSize = count;
        modCount++;
        return removed;
    }

    void materializeAll() {
        for (int i = 0; i < logicalSize; i++) {
            get(i);
        }
    }

    private void bind(final int index, final GridItem item) {
        final int slot = insertionPoint(index);
        ensureCapacity(materializedCount + 1);
        System.arraycopy(indices, slot, indices, slot + 1, materializedCount - slot);
        System.arraycopy(values, slot, values, slot + 1, materializedCount - slot);
        indices[slot] = index;
        values[slot] = item;
        materializedCount++;
        modCount++;
    }

    private void removeLogicalCoordinate(final int index, final int slot) {
        if (slot >= 0) {
            final int move = materializedCount - slot - 1;
            if (move > 0) {
                System.arraycopy(indices, slot + 1, indices, slot, move);
                System.arraycopy(values, slot + 1, values, slot, move);
            }
            materializedCount--;
            values[materializedCount] = null;
        }

        final int firstShift = insertionPoint(index + 1);
        for (int i = firstShift; i < materializedCount; i++) {
            indices[i]--;
            values[i].decreaseRow();
        }
        logicalSize--;
        modCount++;
    }

    private int identitySlot(final Object value) {
        for (int i = 0; i < materializedCount; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private int find(final int index) {
        int low = 0;
        int high = materializedCount - 1;
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final int value = indices[mid];
            if (value < index) {
                low = mid + 1;
            } else if (value > index) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    private int insertionPoint(final int index) {
        final int found = find(index);
        return found >= 0 ? found : -found - 1;
    }

    private void ensureCapacity(final int required) {
        if (required <= indices.length) {
            return;
        }
        final int next = Math.max(required, indices.length * 3 / 2 + 1);
        indices = Arrays.copyOf(indices, next);
        values = Arrays.copyOf(values, next);
    }

    private void checkElementIndex(final int index) {
        if (index < 0 || index >= logicalSize) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + logicalSize);
        }
    }
}
