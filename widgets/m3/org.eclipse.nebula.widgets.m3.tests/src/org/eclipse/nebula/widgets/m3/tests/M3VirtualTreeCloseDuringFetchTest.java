/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding;
import org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding.Nodes;
import org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding.Page;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** The native SWT Tree owns its viewport and releases optional pages on disposal. */
public final class M3VirtualTreeCloseDuringFetchTest {
    private Display display;
    private Shell shell;
    private Tree tree;
    private boolean ownsDisplay;

    @Before
    public void createTree() {
        display = Display.getCurrent();
        ownsDisplay = display == null;
        if (ownsDisplay) display = new Display();
        shell = new Shell(display);
        tree = new Tree(shell, SWT.VIRTUAL);
    }

    @After
    public void disposeTree() {
        if (shell != null && !shell.isDisposed()) shell.dispose();
        if (ownsDisplay && display != null && !display.isDisposed()) display.dispose();
    }

    private abstract static class OneRoot implements Nodes {
        @Override public int rootCount() { return 1; }
        @Override public int columns() { return 1; }
        @Override public int[] path(String key) { return null; }
    }

    private static Page page(String text) {
        return new Page(null, 0, 1, new String[] { "root" },
                new int[] { 0 }, new String[] { text });
    }

    private static void assertReleased(M3VirtualTreeBinding binding) throws Exception {
        Field pages = M3VirtualTreeBinding.class.getDeclaredField("pages");
        pages.setAccessible(true);
        for (Object candidate : (Object[]) pages.get(binding)) {
            assertNull("closed binding retained a page", candidate);
        }
        Field nodes = M3VirtualTreeBinding.class.getDeclaredField("nodes");
        nodes.setAccessible(true);
        assertNull(nodes.get(binding));
    }

    @Test
    public void closeDuringSetDataDoesNotResurrectItsPage() throws Exception {
        final AtomicReference<M3VirtualTreeBinding> bindingRef = new AtomicReference<>();
        Nodes provider = new OneRoot() {
            @Override public Page fetch(String parent, int offset, int length) {
                assertNull(parent);
                assertEquals(0, offset);
                assertEquals(1, length);
                bindingRef.get().close();
                return page("must-not-publish");
            }
        };
        M3VirtualTreeBinding binding = M3VirtualTreeBinding.bind(tree, provider);
        bindingRef.set(binding);
        try {
            try {
                tree.getItem(0).getText(0);
                fail("closed binding must reject a returning SetData page");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("closed"));
            }
            assertReleased(binding);
        } finally {
            binding.close();
        }
    }

    @Test
    public void disposingNativeTreeDuringFetchCannotPublishItsPage() throws Exception {
        Nodes provider = new OneRoot() {
            @Override public Page fetch(String parent, int offset, int length) {
                tree.dispose();
                return page("must-not-publish");
            }
        };
        M3VirtualTreeBinding binding = M3VirtualTreeBinding.bind(tree, provider);
        try {
            try {
                tree.getItem(0).getText(0);
                fail("disposed widget must reject a returning SetData page");
            } catch (IllegalStateException expected) {
                assertTrue(tree.isDisposed());
            }
            assertReleased(binding);
        } finally {
            binding.close();
        }
    }

    @Test
    public void normalVirtualRowStillUsesOriginalSwtTreeItem() {
        Nodes provider = new OneRoot() {
            @Override public Page fetch(String parent, int offset, int length) {
                return page("native row");
            }
        };
        M3VirtualTreeBinding binding = M3VirtualTreeBinding.bind(tree, provider);
        try {
            assertEquals("native row", tree.getItem(0).getText(0));
            assertEquals(1, binding.getResidentPageCount());
        } finally {
            binding.close();
        }
    }
}
