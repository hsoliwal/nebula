/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.widgets.m3.M3VirtualTableBinding.Page;
import org.eclipse.nebula.widgets.m3.M3VirtualTableBinding;
import org.junit.Test;

/** Pure Java page-geometry tests; native Table is exercised separately. */
public class M3VirtualTablePageTest {

    @Test
    public void rowMajorArraysPreserveIdentityAndMakeDefensiveCopies() {
        String[] keys = {"id-0", "id-1"};
        String[] cells = {"Ada", "Engineer", "Lin", "Architect"};
        Page page = new Page(64, 2, keys, cells);
        keys[0] = "tampered";
        cells[0] = "tampered";
        assertEquals(64, page.offset());
        assertEquals(2, page.size());
        assertEquals(2, page.columns());
        assertEquals("id-0", page.keyAt(0));
        assertEquals("Ada", page.textAt(0, 0));
        assertEquals("Engineer", page.textAt(0, 1));
        assertEquals("Architect", page.textAt(1, 1));
    }

    @Test
    public void geometryAndDuplicateIdsFailBeforeAnyWidgetMutation() {
        assertRejected(() -> new Page(-1, 1, new String[] {"a"}, new String[] {"a"}));
        assertRejected(() -> new Page(0, 0, new String[] {"a"}, new String[] {"a"}));
        assertRejected(() -> new Page(0, 65, new String[] {"a"}, new String[65]));
        assertRejected(() -> new Page(0, 1, new String[0], new String[0]));
        assertRejected(() -> new Page(0, 1, new String[] {"a"}, new String[0]));
        assertRejected(() -> new Page(0, 1, new String[] {"a", "a"}, new String[] {"x", "y"}));
        assertRejected(() -> new Page(0, 1, new String[] {null}, new String[] {"x"}));
        assertRejected(() -> new Page(0, 1, new String[] {""}, new String[] {"x"}));
        assertRejected(() -> new Page(0, 1, new String[] {"a"}, new String[] {null}));
        assertRejected(() -> new Page(0, 1, new String[] {"a"}, new String[] {"x".repeat(16385)}));
        assertRejected(() -> new Page(0, 1, new String[65], new String[65]));
    }

    @Test
    public void totalTextBudgetIsIndependentOfIndividualCellLimit() {
        String[] ids = new String[64];
        String[] text = new String[64 * 2];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = "id-" + i;
            text[2 * i] = "a".repeat(3000);
            text[2 * i + 1] = "b".repeat(3000);
        }
        assertRejected(() -> new Page(0, 2, ids, text));
        text[0] = "short";
        for (int i = 1; i < text.length; i++) {
            text[i] = "ok";
        }
        assertTrue(new Page(0, 2, ids, text).size() <= M3VirtualTableBinding.PAGE_ROWS);
    }

    private static void assertRejected(Runnable operation) {
        boolean rejected = false;
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTrue("Expected IllegalArgumentException", rejected);
    }
}
