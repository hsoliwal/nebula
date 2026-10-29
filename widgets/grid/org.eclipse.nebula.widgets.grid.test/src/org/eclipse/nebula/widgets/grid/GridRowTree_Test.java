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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The viewport mode's item list ({@link GridRowTree}) against brute force over a real tree Grid:
 * after every random insertion, removal, expansion, collapse and height change, every item's row,
 * the item of every row and every rank, select and pixel prefix answer equal a linear walk.
 */
public class GridRowTree_Test {
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
	public void randomEditsAnswerAsALinearWalk() {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL);
		final GridRowTree tree = grid.rowTree();
		assumeTrue("the row tree is the item list of the viewport mode only", tree != null);
		new GridColumn(grid, SWT.NONE).setTree(true);
		final Random random = new Random(0x67521EL);
		for (int r = 0; r < 40; r++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 5; c++) {
				new GridItem(root, SWT.NONE);
			}
		}
		for (int step = 0; step < 1500; step++) {
			final GridItem[] items = grid.getItems();
			final GridItem item = items[random.nextInt(items.length)];
			switch (random.nextInt(6)) {
			case 0:
				new GridItem(grid, SWT.NONE, random.nextInt(grid.getRootItemCount() + 1));
				break;
			case 1:
				final GridItem parent = item.getParentItem() != null ? item.getParentItem() : item;
				new GridItem(parent, SWT.NONE, random.nextInt(parent.getItemCount() + 1));
				break;
			case 2:
				if (items.length > 20) {
					item.dispose();
				}
				break;
			case 3:
				item.setExpanded(!item.isExpanded());
				break;
			case 4:
				item.setHeight(1 + random.nextInt(40));
				break;
			default:
				if (random.nextInt(50) == 0) {
					grid.setItemHeight(10 + random.nextInt(20));
				}
			}
			if (step % 25 == 0 || step > 1450) {
				check(grid, tree, random);
			}
		}
	}

	private static void check(final Grid grid, final GridRowTree tree, final Random random) {
		final GridItem[] items = grid.getItems();
		assertEquals(items.length, tree.size());
		final List<Integer> visibleRows = new ArrayList<>();
		final long[] before = new long[items.length + 1];
		final int[] rankBefore = new int[items.length + 1];
		for (int i = 0; i < items.length; i++) {
			assertSame(items[i], tree.get(i));
			assertEquals(i, items[i].getRowIndex());
			assertEquals(i, tree.indexOf(items[i]));
			final int extent = items[i].isVisible() ? items[i].getHeight() + 1 : 0;
			assertEquals("extent at " + i, extent, tree.extentAt(i));
			before[i + 1] = before[i] + extent;
			rankBefore[i + 1] = rankBefore[i] + (extent > 0 ? 1 : 0);
			if (extent > 0) {
				visibleRows.add(java.lang.Integer.valueOf(i));
			}
		}
		assertEquals(visibleRows.size(), tree.visibleCount());
		assertEquals(before[items.length], tree.totalExtent());
		for (int k = 0; k < 30; k++) {
			final int position = random.nextInt(items.length + 1);
			assertEquals(rankBefore[position], tree.rankOf(position));
			assertEquals(before[position], tree.extentBefore(position));
			final int next = rankBefore[position] < visibleRows.size() ? (visibleRows.get(rankBefore[position])).intValue() : -1;
			assertEquals(next, tree.nextVisible(position));
			if (!visibleRows.isEmpty()) {
				final int rank = random.nextInt(visibleRows.size());
				assertEquals((int) (visibleRows.get(rank)).intValue(), tree.positionOfRank(rank));
				assertEquals(before[(visibleRows.get(rank)).intValue()], tree.extentOfRank(rank));
			}
			final long offset = random.nextInt((int) Math.max(1, before[items.length] + 10)) - 1;
			int rank = 0;
			if (offset < 0) {
				rank = -1;
			} else {
				for (int i = 0; i < items.length && before[i + 1] <= offset; i++) {
					rank = rankBefore[i + 1];
				}
			}
			assertEquals("rank at extent " + offset, rank, tree.rankAtExtent(offset));
		}
		assertNotNull(tree.iterator());
	}
}
