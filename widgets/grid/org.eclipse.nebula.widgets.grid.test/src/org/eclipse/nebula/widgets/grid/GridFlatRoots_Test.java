/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The root items and the kept rows of the items in both modes. The viewport mode keeps no second
 * list of a flat Grid's root items (they are its items) until the first child item, and no lock
 * object per item; every answer stays the stock one: root items, their count and order, the root
 * at an index, inserting a root at an index, disposing, shrinking, clearing, becoming a tree, and
 * the row of an item of a virtual Grid as rows are inserted and removed before it, also read from
 * another thread.
 */
public class GridFlatRoots_Test {
	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void flatRootsFollowInsertsAtIndicesDisposalsAndShrinking() {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL);
		new GridColumn(grid, SWT.NONE).setWidth(80);
		final List<GridItem> expected = new ArrayList<>();
		final Random random = new Random(0x2007L);
		for (int i = 0; i < 100; i++) {
			expected.add(new GridItem(grid, SWT.NONE));
		}
		checkRoots(grid, expected);
		for (int step = 0; step < 300; step++) {
			if (random.nextInt(3) > 0 || expected.size() < 10) {
				final int index = random.nextInt(expected.size() + 1);
				expected.add(index, new GridItem(grid, SWT.NONE, index));
			} else {
				expected.remove(random.nextInt(expected.size())).dispose();
			}
			if (step % 20 == 0) {
				checkRoots(grid, expected);
			}
		}
		checkRoots(grid, expected);
		// an index past the end appends, as stock
		expected.add(new GridItem(grid, SWT.NONE, expected.size() + 5));
		checkRoots(grid, expected);

		grid.setItemCount(40);
		while (expected.size() > 40) {
			assertTrue(expected.remove(expected.size() - 1).isDisposed());
		}
		checkRoots(grid, expected);
		grid.setItemCount(45);
		for (int i = expected.size(); i < 45; i++) {
			expected.add(grid.getItem(i));
		}
		checkRoots(grid, expected);

		try {
			grid.getRootItem(45);
			fail("a root index past the last root is out of range");
		} catch (final IllegalArgumentException e) {
			// SWT.ERROR_INVALID_RANGE, as stock
		}
		try {
			grid.getRootItem(-1);
			fail("a negative root index is out of range");
		} catch (final IllegalArgumentException e) {
			// SWT.ERROR_INVALID_RANGE, as stock
		}

		grid.removeAll();
		expected.clear();
		checkRoots(grid, expected);
		expected.add(new GridItem(grid, SWT.NONE));
		expected.add(0, new GridItem(grid, SWT.NONE, 0));
		checkRoots(grid, expected);
	}

	@Test
	public void theFirstChildItemTurnsTheItemsIntoTheTreesRoots() {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL);
		new GridColumn(grid, SWT.NONE).setTree(true);
		final List<GridItem> roots = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			roots.add(new GridItem(grid, SWT.NONE));
		}
		final GridItem parent = roots.get(7);
		final GridItem child = new GridItem(parent, SWT.NONE);
		assertSame(parent, child.getParentItem());
		assertEquals(21, grid.getItemCount());
		checkRootsOnly(grid, roots);
		parent.setExpanded(true);
		assertEquals(8, child.getRowIndex());
		assertEquals(8, grid.indexOf(child));

		// a root inserted by root index lands before that root's row, after its children
		final GridItem inserted = new GridItem(grid, SWT.NONE, 8);
		roots.add(8, inserted);
		checkRootsOnly(grid, roots);
		assertEquals(9, inserted.getRowIndex());

		roots.remove(3).dispose();
		checkRootsOnly(grid, roots);
		parent.dispose();
		roots.remove(parent);
		assertTrue(child.isDisposed());
		checkRootsOnly(grid, roots);
		assertEquals(roots.size(), grid.getItemCount());
	}

	@Test
	public void clearItemsForgetsTheRoots() {
		final Grid grid = new Grid(shell, SWT.NONE);
		new GridColumn(grid, SWT.NONE);
		for (int i = 0; i < 10; i++) {
			new GridItem(grid, SWT.NONE);
		}
		grid.disposeAllItems();
		assertEquals(0, grid.getRootItemCount());
		assertEquals(0, grid.getRootItems().length);
		final GridItem item = new GridItem(grid, SWT.NONE);
		assertArrayEquals(new GridItem[] { item }, grid.getRootItems());
	}

	@Test
	public void keptRowsOfAVirtualGridFollowInsertsAndRemovalsBeforeThem() throws Exception {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL);
		new GridColumn(grid, SWT.NONE).setWidth(80);
		grid.addListener(SWT.SetData, e -> ((GridItem) e.item).setText("r" + e.index));
		grid.setItemCount(1000);
		final GridItem watched = grid.getItem(600);
		assertEquals(600, watched.getRowIndex());
		for (int i = 0; i < 30; i++) {
			new GridItem(grid, SWT.NONE, 10 + i);
		}
		assertEquals(630, watched.getRowIndex());
		assertSame(watched, grid.getItem(630));
		for (int i = 0; i < 12; i++) {
			grid.getItem(100).dispose();
		}
		assertEquals(618, watched.getRowIndex());

		// a reader on another thread sees the latest row (stock: under the item's lock)
		final AtomicInteger seen = new AtomicInteger(-2);
		final Thread reader = new Thread(() -> seen.set(watched.getRowIndex()));
		reader.start();
		reader.join(10_000);
		assertEquals(618, seen.get());
	}

	private static void checkRoots(final Grid grid, final List<GridItem> expected) {
		final GridItem[] array = expected.toArray(new GridItem[0]);
		assertArrayEquals(array, grid.getItems());
		checkRootsOnly(grid, expected);
		for (int i = 0; i < array.length; i++) {
			assertEquals(i, array[i].getRowIndex());
		}
	}

	private static void checkRootsOnly(final Grid grid, final List<GridItem> expected) {
		final GridItem[] array = expected.toArray(new GridItem[0]);
		assertEquals(array.length, grid.getRootItemCount());
		assertArrayEquals(array, grid.getRootItems());
		for (int i = 0; i < array.length; i++) {
			assertSame(array[i], grid.getRootItem(i));
			assertFalse(array[i].isDisposed());
		}
	}
}
