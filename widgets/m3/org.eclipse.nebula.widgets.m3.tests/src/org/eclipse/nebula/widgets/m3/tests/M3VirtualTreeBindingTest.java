/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding;
import org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding.Nodes;
import org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding.Page;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.Test;

/**
 * Must execute on a real native SWT display. RWT browser and transport
 * qualification is a separate, not-yet-executed test lane.
 */
public class M3VirtualTreeBindingTest {

    private static final class FakeHierarchy implements Nodes {
        private final int roots;
        private final boolean reversed;
        int fetches;

        FakeHierarchy(int roots, boolean reversed) {
            this.roots = roots;
            this.reversed = reversed;
        }

        @Override public int rootCount() { return roots; }
        @Override public int columns() { return 2; }

        private int original(int viewIndex) {
            return reversed ? roots - viewIndex - 1 : viewIndex;
        }

        @Override
        public Page fetch(String parentKey, int offset, int length) {
            fetches++;
            String[] keys = new String[length];
            int[] children = new int[length];
            String[] text = new String[length * 2];
            for (int i = 0; i < length; i++) {
                int item = offset + i;
                if (parentKey == null) {
                    int original = original(item);
                    keys[i] = "root-" + original;
                    children[i] = original == 0 ? 10 : 0;
                    text[2 * i] = "Root " + original;
                    text[2 * i + 1] = "Category " + original;
                } else if ("root-0".equals(parentKey)) {
                    keys[i] = "child-" + item;
                    children[i] = item == 0 ? 3 : 0;
                    text[2 * i] = "Child " + item;
                    text[2 * i + 1] = "Nested";
                } else if ("child-0".equals(parentKey)) {
                    keys[i] = "grandchild-" + item;
                    children[i] = 0;
                    text[2 * i] = "Grandchild " + item;
                    text[2 * i + 1] = "Leaf";
                } else {
                    throw new AssertionError("Unexpected requested parent " + parentKey);
                }
            }
            return new Page(parentKey, offset, 2, keys, children, text);
        }

        @Override
        public int[] path(String key) {
            if ("child-9".equals(key)) {
                return new int[] {reversed ? roots - 1 : 0, 9};
            }
            if ("grandchild-2".equals(key)) {
                return new int[] {reversed ? roots - 1 : 0, 0, 2};
            }
            if (!key.startsWith("root-")) {
                return null;
            }
            int logical;
            try {
                logical = Integer.parseInt(key.substring(5));
            } catch (NumberFormatException bad) {
                return null;
            }
            if (logical < 0 || logical >= roots) {
                return null;
            }
            return new int[] {reversed ? roots - logical - 1 : logical};
        }
    }

    private static Tree createTree(Shell shell) {
        Tree tree = new Tree(shell, SWT.VIRTUAL | SWT.MULTI | SWT.BORDER);
        new TreeColumn(tree, SWT.NONE).setText("Name");
        new TreeColumn(tree, SWT.NONE).setText("Kind");
        return tree;
    }

    @Test
    public void virtualHierarchyRequestsParentRelativeChildrenWithoutPerNodeControls() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                Tree tree = createTree(shell);
                FakeHierarchy model = new FakeHierarchy(100_000, false);
                try (M3VirtualTreeBinding binding = M3VirtualTreeBinding.bind(tree, model)) {
                    assertEquals(100_000, tree.getItemCount());
                    TreeItem root = tree.getItem(0);
                    assertEquals("Root 0", root.getText(0));
                    assertEquals("root-0", root.getData(M3VirtualTreeBinding.ROW_KEY));
                    assertEquals(10, root.getItemCount());
                    root.setExpanded(true);
                    TreeItem child = root.getItem(9);
                    assertEquals("Child 9", child.getText(0));
                    assertEquals("child-9", child.getData(M3VirtualTreeBinding.ROW_KEY));
                    assertEquals(0, child.getItemCount());

                    TreeItem first = root.getItem(0);
                    assertEquals("Child 0", first.getText(0));
                    assertEquals(3, first.getItemCount());
                    first.setExpanded(true);
                    assertEquals("Grandchild 2", first.getItem(2).getText(0));

                    // Logical keys survive a collapsed ancestor without
                    // forcing SWT/RWT to expand or realize its descendants.
                    root.setExpanded(false);
                    binding.setSelectedKeys("child-9");
                    assertArrayEquals(new String[] {"child-9"}, binding.getSelectedKeys());
                    assertEquals(0, tree.getSelectionCount());
                    root.setExpanded(true);
                    binding.setSelectedKeys("child-9");
                    assertEquals(1, tree.getSelectionCount());

                    for (int page = 0; page < 12; page++) {
                        int index = page * M3VirtualTreeBinding.PAGE_ROWS;
                        assertEquals("Root " + index, tree.getItem(index).getText(0));
                    }
                    assertEquals(M3VirtualTreeBinding.HOT_PAGES,
                            binding.getResidentPageCount());
                    assertTrue(model.fetches >= 12);

                    binding.setSelectedKeys("root-7", "child-9");
                    assertArrayEquals(new String[] {"root-7", "child-9"},
                            binding.getSelectedKeys());
                    assertEquals(2, tree.getSelectionCount());
                    TreeItem seventh = tree.getItem(7);
                    seventh.getText(0); // materialize virtual node
                    seventh.setData(M3VirtualTreeBinding.ROW_KEY, "stale-metadata");
                    binding.invalidateRoots(7, 7);
                    tree.setSelection(new TreeItem[] {seventh});
                    tree.notifyListeners(SWT.Selection, new Event());
                    // Must force SetData after clear, not capture the old
                    // application-defined TreeItem.setData property.
                    assertArrayEquals(new String[] {"root-7"}, binding.getSelectedKeys());

                    FakeHierarchy reverse = new FakeHierarchy(100_000, true);
                    binding.replaceNodes(reverse);
                    // The formerly expanded first root now has a different
                    // stable key; expansion must not migrate by index.
                    assertTrue(!tree.getItem(0).getExpanded());
                    assertEquals("Root 7", tree.getItem(99_992).getText(0));
                    assertArrayEquals(new String[] {"root-7"}, binding.getSelectedKeys());
                    assertEquals(1, tree.getSelectionCount());
                    binding.invalidateRoots(0, 63);
                    assertEquals("Root 99999", tree.getItem(0).getText(0));
                    assertTrue(binding.getResidentPageCount()
                            <= M3VirtualTreeBinding.HOT_PAGES);
                }
                assertEquals(100_000, tree.getItemCount());
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }

    @Test
    public void badPathsAreRejectedBeforeInstallingNewHierarchy() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                Tree tree = createTree(shell);
                try (M3VirtualTreeBinding binding =
                        M3VirtualTreeBinding.bind(tree, new FakeHierarchy(100, false))) {
                    binding.setSelectedKeys("root-7");
                    Nodes invalid = new Nodes() {
                        @Override public int rootCount() { return 120; }
                        @Override public int columns() { return 2; }
                        @Override public Page fetch(String parent, int offset, int length) {
                            throw new AssertionError("Invalid provider must never fetch");
                        }
                        @Override public int[] path(String key) {
                            return new int[] {120}; // invalid candidate root
                        }
                    };
                    boolean refused = false;
                    try {
                        binding.replaceNodes(invalid);
                    } catch (IllegalArgumentException expected) {
                        refused = true;
                    }
                    assertTrue(refused);
                    assertEquals(100, tree.getItemCount());
                    assertArrayEquals(new String[] {"root-7"}, binding.getSelectedKeys());
                }

                Tree plain = new Tree(shell, SWT.NONE);
                boolean refused = false;
                try {
                    M3VirtualTreeBinding.bind(plain, new FakeHierarchy(10, false));
                } catch (IllegalArgumentException expected) {
                    refused = true;
                }
                assertTrue("Tree without SWT.VIRTUAL must be refused", refused);
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }
}
