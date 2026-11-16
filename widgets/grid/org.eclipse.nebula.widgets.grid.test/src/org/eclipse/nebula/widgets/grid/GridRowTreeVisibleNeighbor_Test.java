/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;

/** The original Grid owns viewport selection; no GridItem is realized for neighbor queries. */
public final class GridRowTreeVisibleNeighbor_Test {

    private static void assertOracle(final GridRowTree tree, final List<Integer> extents) {
        assertEquals(extents.size(), tree.size());
        int count = 0;
        for (int extent : extents) {
            if (extent > 0) count++;
        }
        assertEquals(count, tree.visibleCount());
        assertEquals(-1, tree.previousVisible(-1));
        for (int position = 0; position <= extents.size(); position++) {
            int expectedNext = -1;
            int expectedPrevious = -1;
            for (int index = 0; index < extents.size(); index++) {
                if (extents.get(index) > 0) {
                    if (index >= position && expectedNext == -1) expectedNext = index;
                    if (index <= position) expectedPrevious = index;
                }
            }
            assertEquals("next at " + position, expectedNext, tree.nextVisible(position));
            if (position < extents.size()) {
                assertEquals("previous at " + position,
                        expectedPrevious, tree.previousVisible(position));
            }
        }
    }

    @Test
    public void emptyHiddenAndSparseRowsPreserveNativeNeighborContracts() {
        GridRowTree tree = new GridRowTree();
        tree.setFacades(slot -> { throw new AssertionError("must not realize row " + slot); });
        assertOracle(tree, new ArrayList<Integer>());
        int[] slots = tree.insertUnread(0, null, 9, 0);
        List<Integer> extents = new ArrayList<Integer>();
        for (int index = 0; index < 9; index++) extents.add(0);
        assertOracle(tree, extents);
        tree.reweighSlot(slots[1], 11);
        tree.reweighSlot(slots[4], 6);
        tree.reweighSlot(slots[8], 3);
        extents.set(1, 11);
        extents.set(4, 6);
        extents.set(8, 3);
        assertOracle(tree, extents);
        assertEquals(0, tree.materializedCount());
    }

    @Test
    public void insertRemoveAndReweighMatchTheSimpleReference() {
        GridRowTree tree = new GridRowTree();
        tree.setFacades(slot -> { throw new AssertionError("must not realize row " + slot); });
        ArrayList<Integer> extents = new ArrayList<Integer>();
        ArrayList<Integer> slots = new ArrayList<Integer>();
        Random random = new Random(8101292L);
        for (int step = 0; step < 900; step++) {
            int operation = random.nextInt(4);
            if (extents.isEmpty() || (operation == 0 && extents.size() < 85)) {
                int index = random.nextInt(extents.size() + 1);
                int extent = random.nextInt(5) == 0 ? 0 : 1 + random.nextInt(40);
                int slot = tree.insertUnread(index, null, 1, extent)[0];
                extents.add(index, extent);
                slots.add(index, slot);
            } else if (operation == 1) {
                int index = random.nextInt(extents.size());
                tree.removeUnread(slots.remove(index));
                extents.remove(index);
            } else {
                int index = random.nextInt(extents.size());
                int extent = random.nextInt(3) == 0 ? 0 : 1 + random.nextInt(30);
                tree.reweighSlot(slots.get(index), extent);
                extents.set(index, extent);
            }
            assertOracle(tree, extents);
            assertEquals(0, tree.materializedCount());
        }
    }

    @Test
    public void dirtyBatchedAndNestedUpdatesReadCorrectNeighbors() {
        GridRowTree tree = new GridRowTree();
        tree.setFacades(slot -> { throw new AssertionError("must not realize row " + slot); });
        int[] slots = tree.insertUnread(0, null, 11, 1);
        List<Integer> extents = new ArrayList<Integer>();
        for (int i = 0; i < 11; i++) extents.add(1);
        tree.beginBatch();
        try {
            tree.reweighSlot(slots[0], 0);
            extents.set(0, 0);
            tree.beginBatch();
            try {
                tree.reweighSlot(slots[4], 0);
                extents.set(4, 0);
                tree.reweighSlot(slots[10], 0);
                extents.set(10, 0);
                assertOracle(tree, extents);
            } finally {
                tree.endBatch();
            }
            assertOracle(tree, extents);
            tree.reweighSlot(slots[4], 5);
            extents.set(4, 5);
            assertOracle(tree, extents);
        } finally {
            tree.endBatch();
        }
        assertOracle(tree, extents);
        assertEquals(0, tree.materializedCount());
    }

    @Test
    public void largeSparseTreeResolvesRowsWithoutFacades() {
        final int n = 4096;
        GridRowTree tree = new GridRowTree();
        tree.setFacades(slot -> { throw new AssertionError("must not realize row " + slot); });
        int[] slots = tree.insertUnread(0, null, n, 0);
        List<Integer> extents = new ArrayList<Integer>();
        for (int index = 0; index < n; index++) {
            int extent = (index % 31) == 0 ? 7 : 0;
            extents.add(extent);
            if (extent != 0) tree.reweighSlot(slots[index], extent);
        }
        Random random = new Random(327911L);
        for (int sample = 0; sample < 1500; sample++) {
            int position = random.nextInt(n);
            int next = -1;
            int previous = -1;
            for (int index = 0; index < n; index++) {
                if (extents.get(index) > 0) {
                    if (index >= position && next == -1) next = index;
                    if (index <= position) previous = index;
                }
            }
            assertEquals(next, tree.nextVisible(position));
            assertEquals(previous, tree.previousVisible(position));
        }
        assertEquals(0, tree.materializedCount());
    }
}
