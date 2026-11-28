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
 * The row tree's lanes made on demand ({@link GridRowTree}): a Grid whose rows are all shown with
 * one height keeps 21 bytes per slot; the first hidden row adds the visible lane, the first row of
 * another height the weight and sum lanes, a uniform {@link Grid#setItemHeight(int)} gives those
 * two up again, and a virtual Grid's first row without an item adds the owner lane. After every
 * step of each transition every row, rank, select and pixel answer equals a linear walk over the
 * items, as before the lanes were derived.
 */
public class GridRowTreeLanes_Test {
	private static final int ALL = 21;
	private static final int VISIBLE = 4;
	private static final int WEIGHTS = 12;
	private static final int OWNERS = 4;

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

	private Grid grid(final int style) {
		assumeTrue("the row tree is the item list of the viewport mode only", GridViewport.ENABLED);
		return new Grid(shell, style | SWT.MULTI | SWT.V_SCROLL);
	}

	@Test
	public void uniformRowsKeepOnlyTheSlotLanesThroughInsertsAndDisposals() {
		final Grid grid = grid(SWT.NONE);
		new GridColumn(grid, SWT.NONE).setWidth(80);
		final GridRowTree tree = grid.rowTree();
		for (int i = 0; i < 300; i++) {
			new GridItem(grid, SWT.NONE);
		}
		assertEquals("rows of one height: no visible, weight, sum or owner lane", ALL, tree.laneBytesPerSlot());
		check(grid, tree, new Random(1));
		final Random random = new Random(0xD0E5L);
		for (int step = 0; step < 400; step++) {
			if (random.nextBoolean()) {
				new GridItem(grid, SWT.NONE, random.nextInt(grid.getItemCount() + 1));
			} else {
				grid.getItem(random.nextInt(grid.getItemCount())).dispose();
			}
			if (step % 50 == 0) {
				check(grid, tree, random);
			}
		}
		assertEquals(ALL, tree.laneBytesPerSlot());
		check(grid, tree, random);
		grid.setItemHeight(31);
		assertEquals("one height for all: still derived", ALL, tree.laneBytesPerSlot());
		new GridItem(grid, SWT.NONE, 7);
		assertEquals("a new row takes the Grid's height before it joins", ALL, tree.laneBytesPerSlot());
		check(grid, tree, random);
	}

	@Test
	public void lanesAreMadeByTheFirstRowThatNeedsThemAndGivenUpByAUniformHeight() {
		final Grid grid = grid(SWT.NONE);
		new GridColumn(grid, SWT.NONE).setWidth(80);
		final GridRowTree tree = grid.rowTree();
		for (int i = 0; i < 200; i++) {
			new GridItem(grid, SWT.NONE);
		}
		final Random random = new Random(0x1A9EL);
		assertEquals(ALL, tree.laneBytesPerSlot());

		final GridItem hidden = grid.getItem(57);
		hidden.setVisible(false);
		assertEquals("a hidden row: the visible lane", ALL + VISIBLE, tree.laneBytesPerSlot());
		check(grid, tree, random);

		final int height = grid.getItemHeight();
		grid.getItem(3).setHeight(height);
		assertEquals("the same height again changes nothing", ALL + VISIBLE, tree.laneBytesPerSlot());
		grid.getItem(3).setHeight(height + 9);
		assertEquals("another height: the weight and sum lanes", ALL + VISIBLE + WEIGHTS, tree.laneBytesPerSlot());
		check(grid, tree, random);
		grid.getItem(150).setHeight(height + 2);
		new GridItem(grid, SWT.NONE, 100);
		grid.getItem(10).dispose();
		check(grid, tree, random);

		hidden.setVisible(true);
		check(grid, tree, random);

		grid.setItemHeight(height + 4);
		assertEquals("one height for all rows: weight and sum derived again", ALL + VISIBLE, tree.laneBytesPerSlot());
		check(grid, tree, random);
		grid.getItem(20).setVisible(false);
		grid.getItem(21).setHeight(height + 1);
		check(grid, tree, random);

		grid.removeAll();
		for (int i = 0; i < 50; i++) {
			new GridItem(grid, SWT.NONE);
		}
		assertEquals("an emptied tree starts derived again", ALL, tree.laneBytesPerSlot());
		check(grid, tree, random);
	}

	@Test
	public void aSingleShownRowMovesTheUniformExtent() {
		final Grid grid = grid(SWT.NONE);
		new GridColumn(grid, SWT.NONE).setWidth(80);
		final GridRowTree tree = grid.rowTree();
		final GridItem only = new GridItem(grid, SWT.NONE);
		only.setHeight(40);
		only.setHeight(7);
		assertEquals("no other row to differ from", ALL, tree.laneBytesPerSlot());
		check(grid, tree, new Random(3));
		new GridItem(grid, SWT.NONE);
		check(grid, tree, new Random(4));
	}

	@Test
	public void treeRowsCollapsedAndExpandedAnswerAsALinearWalk() {
		final Grid grid = grid(SWT.NONE);
		new GridColumn(grid, SWT.NONE).setTree(true);
		final GridRowTree tree = grid.rowTree();
		final Random random = new Random(0x7EE5L);
		for (int r = 0; r < 30; r++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 4; c++) {
				new GridItem(root, SWT.NONE);
			}
		}
		assertEquals("collapsed children are hidden rows", ALL + VISIBLE, tree.laneBytesPerSlot());
		check(grid, tree, random);
		for (int step = 0; step < 600; step++) {
			final GridItem[] items = grid.getItems();
			final GridItem item = items[random.nextInt(items.length)];
			switch (random.nextInt(5)) {
			case 0:
				item.setExpanded(!item.isExpanded());
				break;
			case 1:
				new GridItem(item.getParentItem() != null ? item.getParentItem() : item, SWT.NONE);
				break;
			case 2:
				if (items.length > 30) {
					item.dispose();
				}
				break;
			case 3:
				if (random.nextInt(4) == 0) {
					item.setHeight(1 + random.nextInt(30));
				}
				break;
			default:
				if (random.nextInt(40) == 0) {
					grid.setItemHeight(12 + random.nextInt(12));
				}
			}
			if (step % 30 == 0) {
				check(grid, tree, random);
			}
		}
		check(grid, tree, random);
	}

	@Test
	public void virtualChildrenWithoutItemsAddTheOwnerLane() {
		final Grid grid = grid(SWT.VIRTUAL);
		new GridColumn(grid, SWT.NONE).setTree(true);
		grid.addListener(SWT.SetData, e -> ((GridItem) e.item).setText("r" + e.index));
		grid.setItemCount(20);
		final GridItem root = grid.getItem(5);
		root.setItemCount(1000);
		final GridRowTree tree = grid.rowTree();
		assertEquals("rows without items under a collapsed root", ALL + VISIBLE + OWNERS, tree.laneBytesPerSlot());
		root.setExpanded(true);
		assertEquals(1020, grid.getItemCount());
		assertEquals(1020, tree.size());
		assertEquals(1020, tree.visibleCount());
		assertEquals(tree.visibleCount() * (long) (grid.getItemHeight() + 1), tree.totalExtent());
		root.setItemCount(400);
		assertEquals(420, tree.size());
		root.setExpanded(false);
		assertEquals(20, tree.visibleCount());
		root.setExpanded(true);
		// reading rows makes their items; the linear walk reads all of them
		check(grid, tree, new Random(9));
	}

	/* every answer of the tree against a linear walk over the Grid's items */
	static void check(final Grid grid, final GridRowTree tree, final Random random) {
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
				visibleRows.add(Integer.valueOf(i));
			}
		}
		assertEquals(visibleRows.size(), tree.visibleCount());
		assertEquals(before[items.length], tree.totalExtent());
		for (int k = 0; k < 40; k++) {
			final int position = random.nextInt(items.length + 1);
			assertEquals(rankBefore[position], tree.rankOf(position));
			assertEquals(before[position], tree.extentBefore(position));
			final int next = rankBefore[position] < visibleRows.size() ? visibleRows.get(rankBefore[position]).intValue() : -1;
			assertEquals(next, tree.nextVisible(position));
			if (position > 0) {
				final int previous = rankBefore[position] > 0 ? visibleRows.get(rankBefore[position] - 1).intValue() : -1;
				assertEquals(previous, tree.previousVisible(position - 1));
			}
			if (!visibleRows.isEmpty()) {
				final int rank = random.nextInt(visibleRows.size());
				assertEquals(visibleRows.get(rank).intValue(), tree.positionOfRank(rank));
				assertEquals(before[visibleRows.get(rank).intValue()], tree.extentOfRank(rank));
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
	}
}
