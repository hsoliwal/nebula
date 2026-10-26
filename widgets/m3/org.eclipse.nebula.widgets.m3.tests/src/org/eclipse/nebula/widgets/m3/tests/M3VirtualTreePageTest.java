/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding;
import org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding.Page;
import org.junit.Test;

/** Headless page-lane verification, independent of the native SWT display. */
public class M3VirtualTreePageTest {

    @Test
    public void keyedSiblingPageClonesBackingLanes() {
        String[] keys = {"child-a", "child-b"};
        int[] children = {12, 0};
        String[] text = {"A", "active", "B", "done"};
        Page page = new Page("root", 64, 2, keys, children, text);
        keys[0] = "changed";
        children[0] = 999;
        text[0] = "tampered";
        assertEquals("root", page.parentKey());
        assertEquals(64, page.offset());
        assertEquals(2, page.size());
        assertEquals(2, page.columns());
        assertEquals("child-a", page.keyAt(0));
        assertEquals(12, page.childCountAt(0));
        assertEquals("A", page.textAt(0, 0));
        assertEquals("done", page.textAt(1, 1));
    }

    @Test
    public void invalidSiblingGeometryOrIdentityFailsClosed() {
        reject(() -> new Page(null, -1, 1, new String[] {"a"},
                new int[] {0}, new String[] {"a"}));
        reject(() -> new Page(null, 0, 0, new String[] {"a"},
                new int[] {0}, new String[] {"a"}));
        reject(() -> new Page(null, 0, 65, new String[] {"a"},
                new int[] {0}, new String[65]));
        reject(() -> new Page(null, 0, 1, new String[0],
                new int[0], new String[0]));
        reject(() -> new Page(null, 0, 1, new String[] {"a"},
                new int[0], new String[] {"a"}));
        reject(() -> new Page(null, 0, 1, new String[] {"a", "a"},
                new int[] {0, 0}, new String[] {"a", "b"}));
        reject(() -> new Page("same", 0, 1, new String[] {"same"},
                new int[] {0}, new String[] {"a"}));
        reject(() -> new Page(null, 0, 1, new String[] {null},
                new int[] {0}, new String[] {"a"}));
        reject(() -> new Page(null, 0, 1, new String[] {"a"},
                new int[] {-1}, new String[] {"a"}));
        reject(() -> new Page(null, 0, 1, new String[] {"a"},
                new int[] {0}, new String[] {null}));
        reject(() -> new Page(null, 0, 1, new String[] {"a"},
                new int[] {0}, new String[] {"x".repeat(16385)}));
        reject(() -> new Page(null, 0, 1, new String[] {"a"},
                new int[] {0}, new String[0]));
    }

    @Test
    public void flatCellBudgetPreventsUnboundedPageMemory() {
        String[] keys = new String[M3VirtualTreeBinding.PAGE_ROWS];
        int[] children = new int[keys.length];
        String[] text = new String[keys.length * 2];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = "id-" + i;
            text[2 * i] = "a".repeat(4096);
            text[2 * i + 1] = "b".repeat(4096);
        }
        reject(() -> new Page(null, 0, 2, keys, children, text));
        for (int i = 0; i < text.length; i++) {
            text[i] = "ok";
        }
        assertEquals(64, new Page(null, 0, 2, keys, children, text).size());
    }

    private static void reject(Runnable operation) {
        boolean thrown = false;
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            thrown = true;
        }
        assertTrue("Expected IllegalArgumentException", thrown);
    }
}
