/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.nebula.widgets.m3.M3VirtualTableBinding;
import org.eclipse.nebula.widgets.m3.M3VirtualTableBinding.Page;
import org.eclipse.nebula.widgets.m3.M3VirtualTableBinding.Rows;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.junit.Test;

/** Original SWT Table lifecycle regression; test stand-ins are not shipped. */
public final class M3VirtualTableCloseDuringFetchTest {

    @Test
    public void providerClosingBindingCannotPublishTheReturningPage() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                Table table = new Table(shell, SWT.VIRTUAL);
                AtomicReference<M3VirtualTableBinding> current = new AtomicReference<>();
                int[] calls = {0};
                Rows closing = new Rows() {
                    @Override public int size() { return 1; }
                    @Override public int columns() { return 1; }
                    @Override public int indexOfKey(String key) { return -1; }
                    @Override public Page fetch(int offset, int length) {
                        calls[0]++;
                        current.get().close();
                        return new Page(offset, 1, new String[] {"row-0"},
                                new String[] {"must-not-publish"});
                    }
                };
                M3VirtualTableBinding binding = M3VirtualTableBinding.bind(table, closing);
                current.set(binding);
                try {
                    try {
                        table.getItem(0).getText(0); // real SWT.VIRTUAL SetData
                        fail("A returned page must not be published after close()");
                    } catch (IllegalStateException expected) {
                        assertEquals("Virtual Table binding closed during SetData",
                                expected.getMessage());
                    }
                    assertEquals(1, calls[0]);
                    assertEquals(1, table.getItemCount());
                    assertFalse(table.isDisposed()); // Table remains SWT-owned.
                    assertNull(table.getItem(0).getData(M3VirtualTableBinding.ROW_KEY));
                } finally {
                    binding.close();
                }
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }

    @Test
    public void ordinaryNonclosingPageMaintainsExistingVirtualData() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                Table table = new Table(shell, SWT.VIRTUAL);
                Rows steady = new Rows() {
                    @Override public int size() { return 1; }
                    @Override public int columns() { return 1; }
                    @Override public int indexOfKey(String key) {
                        return "row-0".equals(key) ? 0 : -1;
                    }
                    @Override public Page fetch(int offset, int length) {
                        return new Page(offset, 1, new String[] {"row-0"},
                                new String[] {"ordinary"});
                    }
                };
                try (M3VirtualTableBinding binding = M3VirtualTableBinding.bind(table, steady)) {
                    assertEquals("ordinary", table.getItem(0).getText(0));
                    assertEquals("row-0", table.getItem(0).getData(M3VirtualTableBinding.ROW_KEY));
                    assertEquals(1, binding.getResidentPageCount());
                }
                assertFalse(table.isDisposed());
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }
}
