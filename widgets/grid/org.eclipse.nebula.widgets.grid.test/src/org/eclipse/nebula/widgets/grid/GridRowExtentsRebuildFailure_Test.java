/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * The existing row extent index must fail closed after a partial rebuild.
 * The matching Synexia source recipe is NebulaGridExtentRebuildFailClosed.
 * This independent product oracle intentionally fails on the historical
 * source until that qualified recipe is applied.
 */
public class GridRowExtentsRebuildFailure_Test {

    @Test
    public void throwingHeightCallbackInvalidatesTheIndexForEveryFailurePosition() {
        for (final int failurePosition : new int[] {0, 1, 2, 3}) {
            final GridRowExtents index = new GridRowExtents();
            index.rebuild(4, i -> i + 2);
            assertTrue(index.isValid(4));
            try {
                index.rebuild(4, i -> {
                    if (i == failurePosition) {
                        throw new IllegalStateException("height provider failed at " + i);
                    }
                    return 40 + i;
                });
                fail("expected the height provider to propagate its exception");
            } catch (final IllegalStateException expected) {
                assertEquals("height provider failed at " + failurePosition, expected.getMessage());
            }
            assertFalse("partially written Fenwick lanes must not be marked valid", index.isValid(4));
            index.rebuild(4, i -> 5);
            assertTrue(index.isValid(4));
            assertEquals(4, index.visibleCount());
            assertEquals(20L, index.totalExtent());
            assertEquals(2, index.positionOfRank(2));
            assertEquals(3, index.rankAtExtent(17));
        }
    }

    @Test
    public void rejectedNegativeHeightAlsoInvalidatesBeforeTheNextUse() {
        final GridRowExtents index = new GridRowExtents();
        index.rebuild(3, i -> 7);
        assertTrue(index.isValid(3));
        try {
            index.rebuild(3, i -> i == 1 ? -1 : 4);
            fail("a negative extent must be rejected");
        } catch (final IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("negative row extent"));
        }
        assertFalse(index.isValid(3));
        index.rebuild(3, i -> i + 1);
        assertTrue(index.isValid(3));
        assertEquals(6L, index.totalExtent());
        assertEquals(3, index.visibleCount());
    }
}
