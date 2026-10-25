/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.widgets.m3.M3VirtualTableBinding;
import org.eclipse.nebula.widgets.m3.M3VirtualTableBinding.Page;
import org.eclipse.nebula.widgets.m3.M3VirtualTableBinding.Rows;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.junit.Test;

/** Actual original SWT display test; RWT sessions are a separate acceptance lane. */
public class M3VirtualTableBindingTest {

    private static final class FakeRows implements Rows {
        private final int size;
        private final boolean reversed;
        private int calls;

        FakeRows(int size, boolean reversed) {
            this.size = size;
            this.reversed = reversed;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public int columns() {
            return 2;
        }

        private int original(int viewRow) {
            return reversed ? size - viewRow - 1 : viewRow;
        }

        @Override
        public Page fetch(int offset, int length) {
            calls++;
            String[] ids = new String[length];
            String[] texts = new String[length * 2];
            for (int i = 0; i < length; i++) {
                int identity = original(offset + i);
                ids[i] = "row-" + identity;
                texts[i * 2] = "Name " + identity;
                texts[i * 2 + 1] = "Value " + identity;
            }
            return new Page(offset, 2, ids, texts);
        }

        @Override
        public int indexOfKey(String key) {
            if (!key.startsWith("row-")) {
                return -1;
            }
            int identity = Integer.parseInt(key.substring(4));
            if (identity < 0 || identity >= size) {
                return -1;
            }
            return reversed ? size - identity - 1 : identity;
        }
    }

    @Test
    public void millionLogicalRowsUseOriginalSwtVirtualItemProtocolAndBoundedPages() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                Table table = new Table(shell, SWT.VIRTUAL | SWT.MULTI | SWT.BORDER);
                new TableColumn(table, SWT.NONE).setText("Name");
                new TableColumn(table, SWT.NONE).setText("Value");
                FakeRows source = new FakeRows(1_000_000, false);
                try (M3VirtualTableBinding binding = M3VirtualTableBinding.bind(table, source)) {
                    assertEquals(1_000_000, table.getItemCount());
                    assertEquals("Name 0", table.getItem(0).getText(0));
                    assertEquals("Value 0", table.getItem(0).getText(1));
                    assertEquals("Name 62", table.getItem(62).getText(0));
                    assertEquals("Name 128", table.getItem(128).getText(0));
                    assertEquals("Name 999999", table.getItem(999999).getText(0));
                    // More than four disjoint page requests force bounded LRU eviction.
                    for (int page = 0; page < 12; page++) {
                        int first = page * M3VirtualTableBinding.PAGE_ROWS;
                        assertEquals("Name " + first, table.getItem(first).getText(0));
                    }
                    assertEquals(M3VirtualTableBinding.HOT_PAGES,
                            binding.getResidentPageCount());
                    assertTrue("Paged source should be used", source.calls >= 12);

                    binding.setSelectedKeys("row-7", "row-128");
                    assertArrayEquals(new String[] {"row-7", "row-128"}, binding.getSelectedKeys());
                    FakeRows reordered = new FakeRows(1_000_000, true);
                    binding.replaceRows(reordered);
                    assertEquals("Name 7", table.getItem(999992).getText(0));
                    assertArrayEquals(new String[] {"row-7", "row-128"}, binding.getSelectedKeys());
                    assertTrue(table.isSelected(999992));
                    assertTrue(table.isSelected(999871));

                    binding.invalidate(0, 63);
                    assertEquals("Name 999999", table.getItem(0).getText(0));
                    assertTrue(binding.getResidentPageCount() <= M3VirtualTableBinding.HOT_PAGES);
                }
                assertEquals(1_000_000, table.getItemCount());
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }

    @Test
    public void selectionFromSwtEventRetainsKeysAndUnqualifiedStyleFailsClosed() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                Table ordinary = new Table(shell, SWT.NONE);
                new TableColumn(ordinary, SWT.NONE);
                new TableColumn(ordinary, SWT.NONE);
                boolean refused = false;
                try {
                    M3VirtualTableBinding.bind(ordinary, new FakeRows(10, false));
                } catch (IllegalArgumentException expected) {
                    refused = true;
                }
                assertTrue("Non-virtual Table must be refused", refused);

                Table table = new Table(shell, SWT.VIRTUAL | SWT.MULTI);
                new TableColumn(table, SWT.NONE);
                new TableColumn(table, SWT.NONE);
                try (M3VirtualTableBinding binding =
                        M3VirtualTableBinding.bind(table, new FakeRows(100, false))) {
                    table.select(12);
                    table.notifyListeners(SWT.Selection, new Event());
                    assertArrayEquals(new String[] {"row-12"}, binding.getSelectedKeys());
                    binding.setSelectedKeys("row-9999");
                    assertEquals(0, table.getSelectionCount());
                    binding.replaceRows(new FakeRows(200, false));
                    assertEquals(0, table.getSelectionCount());
                    binding.setSelectedKeys("row-12");
                    assertEquals(1, table.getSelectionCount());
                }
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }
}
