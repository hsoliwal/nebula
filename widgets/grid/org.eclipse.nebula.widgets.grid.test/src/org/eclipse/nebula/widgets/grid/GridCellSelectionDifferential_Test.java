/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others.
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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Test;

/**
 * Differential test of the cell selection a virtual Grid keeps as blocks: the same seeded scripts
 * run on a Grid whose cells are blocks and on one whose cells are the point list of the earlier
 * releases (the oracle), and both leave the same trace after every step: the cells in order, the
 * focus cell, events, the rows (count, index, indices, getSelection, isSelected). The scripts mix
 * selectCell(s)/deselectCell(s)/setCellSelection/selectAllCells/deselectAllCells/selectColumn, the
 * row index selections, clicks and drags with shift and ctrl, keys, removed/inserted/disposed rows,
 * truncation, columns removed, inserted, moved, hidden and made unselectable, column spans, row
 * permutations and a tree conversion, in SINGLE and MULTI. The blocks are checked against their
 * lanes after every step.
 */
public class GridCellSelectionDifferential_Test {

	private static final int STEPS = 300;
	private Shell shell;

	@After
	public void tearDown() {
		Grid.cellSelectionByBlocks = true;
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	private Grid grid(final boolean blocks, final int style) {
		Grid.cellSelectionByBlocks = blocks;
		shell = new Shell(Display.getDefault());
		shell.setSize(420, 360);
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL | SWT.H_SCROLL | style);
		Grid.cellSelectionByBlocks = true;
		grid.setBounds(0, 0, 400, 300);
		grid.setHeaderVisible(true);
		for (int c = 0; c < 3; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(100);
		}
		grid.setCellSelectionEnabled(true);
		return grid;
	}

	private List<String> trace(final boolean blocks, final int style, final boolean spans, final long seed) {
		final Grid grid = grid(blocks, style);
		final GridSelectionScript script = new GridSelectionScript(grid);
		script.cellOps = true;
		script.spans = spans;
		if (blocks) {
			script.afterStep = grid::checkCellSelection;
		}
		grid.setItemCount(300);
		shell.open();
		script.run(seed, STEPS);
		shell.dispose();
		return script.trace;
	}

	private void compare(final int style, final boolean spans, final int seeds) {
		// the original mode keeps the stock point list on both sides: a couple of seeds suffice
		final int runs = GridTestUtil.fixesUpstreamDefects() ? seeds : Math.min(seeds, 2);
		for (long seed = 1; seed <= runs; seed++) {
			final List<String> oracle = trace(false, style, spans, seed);
			final List<String> cells = trace(true, style, spans, seed);
			for (int i = 0; i < oracle.size(); i++) {
				assertEquals("seed " + seed + " step " + i, oracle.get(i), cells.get(i));
			}
		}
	}

	@Test
	public void multiSelectionCellScriptsMatchThePointList() {
		compare(SWT.MULTI, false, 10);
	}

	@Test
	public void singleSelectionCellScriptsMatchThePointList() {
		compare(SWT.SINGLE, false, 4);
	}

	@Test
	public void cellScriptsWithColumnSpansMatchThePointList() {
		compare(SWT.MULTI, true, 4);
	}

	/* the cells and rows a Grid answers, as text */
	private static String answers(final Grid grid) {
		return grid.getCellSelectionCount() + Arrays.toString(grid.getCellSelection()) + " rows "
				+ Arrays.toString(grid.getSelectionIndices());
	}

	/*
	 * a removed row without an item: the earlier build's range loops made the items of the rows
	 * they selected, so removing such a row went through the item's removal and took its cells; the
	 * blocks make no item, and the removal still takes the cells (and only them)
	 */
	@Test
	public void removedRowsWithoutItemsTakeTheirCellsAsTheItemRemovalDid() {
		if (!GridTestUtil.fixesUpstreamDefects()) {
			return; // the original mode keeps the stock point list: no blocks to compare
		}
		final List<String> answers = new ArrayList<>();
		for (final boolean blocks : new boolean[] { false, true }) {
			final Grid grid = grid(blocks, SWT.MULTI);
			grid.setItemCount(5_000);
			shell.open();
			grid.selectAllCells();
			grid.remove(2_500);
			grid.remove(new int[] { 4_000, 10, 4_000 });
			grid.remove(3_000, 3_004);
			grid.getColumn(0).dispose();
			grid.setItemCount(4_500);
			final StringBuilder b = new StringBuilder(answers(grid));
			grid.deselect(100, 199);
			grid.select(new int[] { 150, 4_400, 150 });
			grid.setSelection(4_490, 4_499);
			b.append(" | ").append(answers(grid));
			if (blocks) {
				grid.checkCellSelection();
				assertTrue("cells did not stay blocks", grid.cellSelectionBlocks() >= 1);
			}
			answers.add(b.toString());
			shell.dispose();
		}
		assertEquals(answers.get(0), answers.get(1));
	}

	/*
	 * a row permutation maps the cells through their rows: with blocks a row without an item keeps
	 * its cells as rows keep their selection (the point list dropped cells whose row had no item:
	 * cells selected by selectCell on unread rows, or range-selected cells once rows were inserted
	 * or removed above them, which moves the items off the cells' rows; see the test below)
	 */
	@Test
	public void rowPermutationKeepsTheCellsOfRowsWithoutItems() {
		if (!GridTestUtil.fixesUpstreamDefects()) {
			return; // the original mode keeps the stock point list: no blocks to compare
		}
		final Grid grid = grid(true, SWT.MULTI);
		grid.setItemCount(1_000);
		shell.open();
		grid.selectCell(new Point(1, 900));
		grid.selectCell(new Point(2, 5));
		final org.eclipse.nebula.widgets.grid.model.RowPermutation permutation =
				new org.eclipse.nebula.widgets.grid.model.RowPermutation(1_000);
		final int[] keys = new int[1_000];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = 1_000 - i;
		}
		permutation.sortByIntKeys(keys, false);
		grid.setRowPermutation(permutation);
		assertArrayEquals(new Point[] { new Point(1, 99), new Point(2, 994) }, grid.getCellSelection());
		// under a row view the cells are the point list again, which maps rows through their items
		grid.getItem(99);
		grid.getItem(994);
		grid.setRowPermutation(null);
		assertArrayEquals(new Point[] { new Point(1, 900), new Point(2, 5) }, grid.getCellSelection());
		grid.checkCellSelection();
	}

	@Test
	public void blocksStayCompactUnderScatteredDeselection() {
		if (!GridTestUtil.fixesUpstreamDefects()) {
			return; // the original mode keeps the stock point list: no blocks to compare
		}
		final Grid grid = grid(true, SWT.MULTI);
		grid.setItemCount(200_000);
		shell.open();
		grid.selectAllCells();
		assertEquals(1, grid.cellSelectionBlocks());
		final long start = System.nanoTime();
		for (int row = 0; row < 200_000; row += 2) {
			grid.deselectCell(new Point(1, row));
		}
		grid.deselect(1_000, 1_999);
		final long millis = (System.nanoTime() - start) / 1_000_000;
		assertEquals(3 * 200_000 - 100_000 - (1_000 * 3 - 500), grid.getCellSelectionCount());
		// one block per run of rows with the same columns, not a point per cell
		assertTrue("blocks " + grid.cellSelectionBlocks(), grid.cellSelectionBlocks() <= 200_003);
		final Point[] cells = grid.getCellSelection();
		assertEquals(new Point(0, 0), cells[0]);
		assertEquals(new Point(2, 0), cells[1]);
		assertEquals(new Point(0, 1), cells[2]);
		assertEquals(new Point(2, 199_999), cells[cells.length - 1]);
		grid.checkCellSelection();
		assertTrue("deselecting 100,000 scattered cells took " + millis + " ms", millis < 4_000);
		grid.selectCell(new Point(1, 0));
		assertEquals(new Point(1, 0), grid.getCellSelection()[grid.getCellSelectionCount() - 1]);
		grid.checkCellSelection();
	}

	/*
	 * the earlier point list's range loops made the items of the rows they selected, and the item
	 * removal took the cells of its row; cells keep their row when rows are inserted or removed
	 * above them, so the items move off the cells' rows. Removing such a row: the point list kept
	 * its cells (the row's item was another row's), the blocks take them (the row is removed through
	 * its item, as when the range loop had made it). A row permutation: the point list dropped the
	 * cells of the row without an item, the blocks keep them by row. The deliberate difference,
	 * documented here.
	 */
	@Test
	public void rangeSelectedCellsBelowInsertedRowsFollowTheirRowsNotTheItems() {
		if (!GridTestUtil.fixesUpstreamDefects()) {
			return; // the original mode keeps the stock point list: no blocks to compare
		}
		final List<List<Point>> removed = new ArrayList<>();
		final List<List<Point>> permuted = new ArrayList<>();
		for (final boolean blocks : new boolean[] { false, true }) {
			for (final boolean permute : new boolean[] { false, true }) {
				final Grid grid = grid(blocks, SWT.MULTI);
				grid.setItemCount(1_000);
				shell.open();
				grid.select(500, 502);
				new GridItem(grid, SWT.NONE, 0);
				if (permute) {
					final org.eclipse.nebula.widgets.grid.model.RowPermutation permutation =
							new org.eclipse.nebula.widgets.grid.model.RowPermutation(1_001);
					final int[] keys = new int[1_001];
					for (int i = 0; i < keys.length; i++) {
						keys[i] = 1_001 - i;
					}
					permutation.sortByIntKeys(keys, false);
					grid.setRowPermutation(permutation);
					permuted.add(Arrays.asList(grid.getCellSelection()));
				} else {
					grid.remove(500);
					removed.add(Arrays.asList(grid.getCellSelection()));
				}
				shell.dispose();
			}
		}
		// removing row 500 (the old row 499, no item): the point list keeps (x, 500), the blocks take them
		final List<Point> row500 = Arrays.asList(new Point(0, 500), new Point(1, 500), new Point(2, 500));
		assertTrue(removed.get(0).toString(), removed.get(0).containsAll(row500));
		assertEquals(9, removed.get(0).size());
		assertEquals(removed.get(0).subList(3, 9), removed.get(1));
		// a reversed permutation: the point list drops the cells of row 500, the blocks keep them
		assertEquals(6, permuted.get(0).size());
		assertEquals(9, permuted.get(1).size());
		assertTrue(permuted.get(1).toString(), permuted.get(1).containsAll(permuted.get(0)));
	}

	/*
	 * a hover over cell (col, row), a press, a move to cell (toCol, toRow) or within the cell, and a
	 * release
	 */
	private static void press(final Grid grid, final int col, final int row, final int stateMask, final int toCol,
			final int toRow) {
		final Point point = shown(grid, col, row);
		final Event hover = new Event();
		hover.x = point.x;
		hover.y = point.y;
		grid.notifyListeners(SWT.MouseMove, hover);
		final Event down = new Event();
		down.button = 1;
		down.x = point.x;
		down.y = point.y;
		down.stateMask = stateMask;
		grid.notifyListeners(SWT.MouseDown, down);
		final Point to = toCol < 0 ? new Point(point.x + 1, point.y) : shown(grid, toCol, toRow);
		final Event move = new Event();
		move.x = to.x;
		move.y = to.y;
		move.stateMask = stateMask | SWT.BUTTON1;
		grid.notifyListeners(SWT.MouseMove, move);
		final Event up = new Event();
		up.button = 1;
		up.x = to.x;
		up.y = to.y;
		up.stateMask = stateMask | SWT.BUTTON1;
		grid.notifyListeners(SWT.MouseUp, up);
	}

	private static Point shown(final Grid grid, final int col, final int row) {
		final GridItem item = grid.getItem(row);
		final long deadline = System.nanoTime() + 5_000_000_000L;
		while (System.nanoTime() < deadline) {
			grid.showItem(item);
			grid.update();
			while (grid.getDisplay().readAndDispatch()) {
				// drain
			}
			final Rectangle b = item.getBounds(col);
			final Point candidate = new Point(b.x + 4, b.y + b.height / 2);
			if (new Point(col, row).equals(grid.getCell(candidate))) {
				return candidate;
			}
		}
		throw new AssertionError("cell " + col + "," + row + " not shown");
	}

	/*
	 * the earlier range-select snapshot was an ArrayList holding the point list's own points, so a
	 * removed column moved the snapshot's cells the list still held (and only those) with the list:
	 * a ctrl+press that starts a drag within the hovered cell takes the snapshot (a drag into another
	 * cell rebuilds the selection from it, with copied points); a column goes; a ctrl+shift click then
	 * rebuilds the selection from the snapshot. The blocks give the point list's answers when the
	 * snapshot still shares all, some (one cell deselected) or none (the drag moved on) of its cells.
	 */
	@Test
	public void snapshotCellsMoveWithTheirColumnLikeThePointList() {
		if (!GridTestUtil.fixesUpstreamDefects()) {
			return; // the original mode keeps the stock point list on both sides
		}
		final String[] cases = { "shared", "partly shared", "not shared" };
		for (final String scenario : cases) {
			final List<String> answers = new ArrayList<>();
			for (final boolean blocks : new boolean[] { false, true }) {
				final Grid grid = grid(blocks, SWT.MULTI);
				grid.setItemCount(50);
				shell.open();
				grid.selectCell(new Point(2, 5));
				if ("not shared".equals(scenario)) {
					press(grid, 1, 1, SWT.MOD1, 1, 2);
				} else {
					press(grid, 1, 1, SWT.MOD1, -1, -1);
				}
				if ("partly shared".equals(scenario)) {
					grid.deselectCell(new Point(2, 5));
				}
				grid.getColumn(0).dispose();
				press(grid, 0, 3, SWT.MOD1 | SWT.MOD2, -1, -1);
				answers.add(Arrays.toString(grid.getCellSelection()));
				if (blocks) {
					grid.checkCellSelection();
				}
				shell.dispose();
			}
			assertEquals(scenario, answers.get(0), answers.get(1));
			if ("shared".equals(scenario)) {
				assertEquals("[Point {1, 5}, Point {0, 1}, Point {0, 2}, Point {0, 3}]", answers.get(1));
			}
		}
	}

	/*
	 * ctrl+click toggling: a removal clears the lane and a cell selected again is a new last block,
	 * both O(log n), and the blocks are compacted only when appends have doubled them or the order is
	 * read; 32ff57cf compacted every block on each append after a removal (8,000 toggles in 300,000
	 * cells: 15 s, the point list 21 ms)
	 */
	@Test
	public void togglingCellsCostsNoPassOverTheBlocks() {
		if (!GridTestUtil.fixesUpstreamDefects()) {
			return; // the original mode's stock point list is not what this measures
		}
		final Grid grid = grid(true, SWT.MULTI);
		grid.setItemCount(100_000);
		shell.open();
		grid.selectAllCells();
		final java.util.Random random = new java.util.Random(11);
		final java.util.LinkedHashSet<Point> picks = new java.util.LinkedHashSet<>();
		while (picks.size() < 8_000) {
			picks.add(new Point(random.nextInt(3), random.nextInt(100_000)));
		}
		final Point[] toggled = picks.toArray(new Point[0]);
		final long start = System.nanoTime();
		for (final Point cell : toggled) {
			grid.deselectCell(cell);
			grid.selectCell(cell);
		}
		final long millis = (System.nanoTime() - start) / 1_000_000;
		assertEquals(300_000, grid.getCellSelectionCount());
		// each toggled cell went to the end, in toggle order
		final Point[] cells = grid.getCellSelection();
		assertArrayEquals(toggled, Arrays.copyOfRange(cells, 300_000 - toggled.length, 300_000));
		grid.checkCellSelection();
		assertTrue("8,000 toggles took " + millis + " ms", millis < 3_000);
	}
}
