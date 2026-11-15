/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import org.junit.Test;

/** Regression checks for rank-to-pixel queries using the owning Grid's packed row index. */
public class GridRowTreeRankExtent_Test {

    @Test
    public void hiddenRowsAndBoundariesKeepExactPixelOffsets() {
        final GridRowTree tree = new GridRowTree();
        final int[] slots = tree.insertUnread(0, null, 5, 1);
        final int[] heights = { 4, 0, 9, 3, 0 };
        for (int i = 0; i < slots.length; i++) {
            tree.reweighSlot(slots[i], heights[i]);
        }
        assertEquals(0L, tree.extentOfRank(0));
        assertEquals(4L, tree.extentOfRank(1));
        assertEquals(13L, tree.extentOfRank(2));
        assertEquals(16L, tree.extentOfRank(3));
        assertEquals(0, tree.materializedCount());
    }

    @Test
    public void extentQueriesDoNotMaterializeUnreadRowFacades() {
        final GridRowTree tree = new GridRowTree();
        tree.setFacades(slot -> { throw new AssertionError("unexpected row " + slot); });
        tree.insertUnread(0, null, 4096, 17);
        for (int rank = 0; rank <= 4096; rank += 17) {
            assertEquals(rank * 17L, tree.extentOfRank(rank));
        }
        assertEquals(0, tree.materializedCount());
    }

    @Test
    public void zeroRankStaysLazyInsideDirtyBatch() throws ReflectiveOperationException {
        final GridRowTree tree = new GridRowTree();
        final int[] slots = tree.insertUnread(0, null, 3, 2);
        final Field field = GridRowTree.class.getDeclaredField("anyDirty");
        field.setAccessible(true);
        tree.beginBatch();
        try {
            tree.reweighSlot(slots[0], 0);
            tree.reweighSlot(slots[1], 30);
            tree.reweighSlot(slots[2], 11);
            assertTrue(field.getBoolean(tree));
            assertEquals(0L, tree.extentOfRank(0));
            assertTrue("rank zero must not settle", field.getBoolean(tree));
            assertEquals(30L, tree.extentOfRank(1));
            assertEquals(41L, tree.extentOfRank(2));
        } finally {
            tree.endBatch();
        }
    }

    @Test
    public void invalidRankRetainsExceptionContract() {
        final GridRowTree tree = new GridRowTree();
        tree.insertUnread(0, null, 2, 1);
        for (int rank : new int[] { -1, 3, Integer.MAX_VALUE }) {
            try {
                tree.extentOfRank(rank);
                fail("accepted invalid rank " + rank);
            } catch (IndexOutOfBoundsException expected) {
                assertEquals("visible row " + rank + ", visible rows 2", expected.getMessage());
            }
        }
    }
}
