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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Selecting cells of a 100,000-row by 5-column {@code SWT.VIRTUAL} Grid makes no item, no
 * {@code Point} per selected cell and asks no SetData: the default mode keeps the cells as blocks of
 * rows by columns (one block for selectAllCells or a shift range), so selectAllCells, the row and
 * column range selections and a shift+click range cost a few bytes, not an item per row and a point
 * per cell. The answers (count, order, rows, removal of rows and columns) stay the stock ones. The
 * original mode keeps the stock point list.
 */
public class GridCellSelectionResidency_Test {

	/* the original mode's stock point list adds a cell in O(cells): fewer rows there */
	private static final int ROWS = GridTestUtil.fixesUpstreamDefects() ? 100_000 : 2_000;
	private static final int HALF = ROWS / 2;
	private static final int COLUMNS = 5;

	private Display display;
	private Shell shell;
	private Grid grid;
	private int setData;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		grid = new Grid(shell, SWT.VIRTUAL | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 0, 500, 300);
		grid.setHeaderVisible(true);
		for (int c = 0; c < COLUMNS; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(90);
		}
		grid.setCellSelectionEnabled(true);
		grid.addListener(SWT.SetData, e -> {
			setData++;
			final GridItem item = (GridItem) e.item;
			for (int c = 0; c < COLUMNS; c++) {
				item.setText(c, "r" + e.index + "c" + c);
			}
		});
		grid.setItemCount(ROWS);
		shell.setSize(540, 360);
		shell.open();
		paint();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	private void paint() {
		final int before = grid.paintPasses;
		grid.redraw();
		grid.update();
		GridSwtScreenshotCapture.awaitStableCounter(display, () -> grid.paintPasses, before + 1,
				"Grid paint did not settle");
	}

	private int windowRows() {
		return grid.getVisibleGridHeight() / (grid.getItemHeight() + 1) + 1;
	}

	private static long usedHeap() {
		final Runtime runtime = Runtime.getRuntime();
		for (int i = 0; i < 3; i++) {
			System.gc();
		}
		return runtime.totalMemory() - runtime.freeMemory();
	}

	/* the default mode keeps blocks; the original mode keeps the stock point list */
	private static boolean byBlocks() {
		return GridTestUtil.fixesUpstreamDefects();
	}

	private void assertNoItemPerSelectedRow(final String operation, final int items, final int requests,
			final int windows) {
		paint();
		if (byBlocks()) {
			assertTrue(operation + " made items: " + items + " -> " + grid.virtualMaterializedItemCount(),
					grid.virtualMaterializedItemCount() <= items + windows * windowRows() + 2);
			assertTrue(operation + " asked SetData: " + (setData - requests),
					setData - requests <= windows * windowRows() + 2);
		}
		assertTrue("cells painted " + grid.paintCellCount, grid.paintCellCount <= (windowRows() + 1) * COLUMNS);
		assertEquals(0, grid.getChildren().length);
	}

	private void assertRetained(final String operation, final long retained) {
		if (byBlocks()) {
			// 100,000 items and 500,000 points retained about 40 MB; the blocks are a few bytes
			assertTrue(operation + " retained " + retained + " bytes", retained < 4_000_000L);
		}
	}

	/* a click where the Grid hit-tests cell (col, row) once the row is shown */
	private void clickCell(final int col, final int row, final int stateMask) {
		final GridItem item = grid.getItem(row);
		Point point = null;
		final long deadline = System.nanoTime() + 5_000_000_000L;
		do {
			grid.showItem(item);
			grid.update();
			while (display.readAndDispatch()) {
				// drain
			}
			final Rectangle bounds = item.getBounds(col);
			final Point candidate = new Point(bounds.x + 4, bounds.y + bounds.height / 2);
			if (new Point(col, row).equals(grid.getCell(candidate))) {
				point = candidate;
			}
		} while (point == null && System.nanoTime() < deadline);
		assertTrue("cell " + col + "," + row + " not shown", point != null);
		final Event down = new Event();
		down.button = 1;
		down.x = point.x;
		down.y = point.y;
		down.stateMask = stateMask;
		grid.notifyListeners(SWT.MouseDown, down);
		final Event up = new Event();
		up.button = 1;
		up.x = point.x;
		up.y = point.y;
		up.stateMask = stateMask | SWT.BUTTON1;
		grid.notifyListeners(SWT.MouseUp, up);
	}

	@Test
	public void selectAllCellsMakesNoItemOrPointPerCell() {
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		final long heap = usedHeap();
		grid.selectAllCells();
		final long retained = usedHeap() - heap;
		assertNoItemPerSelectedRow("selectAllCells", items, requests, 1);
		assertRetained("selectAllCells", retained);
		assertEquals(ROWS * COLUMNS, grid.getCellSelectionCount());
		assertTrue(grid.isCellSelected(new Point(COLUMNS - 1, ROWS - 1)));
		assertTrue(grid.isCellSelected(new Point(2, HALF)));
		assertFalse(grid.isCellSelected(new Point(COLUMNS, 0)));
		assertTrue(grid.isSelected(ROWS - 1));
		assertEquals(ROWS, grid.getSelectionCount());
		assertEquals(0, grid.getSelectionIndex());
		final int[] indices = grid.getSelectionIndices();
		assertEquals(ROWS, indices.length);
		assertEquals(ROWS - 1, indices[ROWS - 1]);
		assertNoItemPerSelectedRow("selection queries", items, requests, 1);
		// points are made when they are asked for, in the stock order: row by row
		final Point[] cells = grid.getCellSelection();
		assertEquals(ROWS * COLUMNS, cells.length);
		assertEquals(new Point(0, 0), cells[0]);
		assertEquals(new Point(1, 0), cells[1]);
		assertEquals(new Point(0, 1), cells[COLUMNS]);
		assertEquals(new Point(COLUMNS - 1, ROWS - 1), cells[cells.length - 1]);
	}

	@Test
	public void cellRangesMakeNoItemOrPointPerCell() {
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		final long heap = usedHeap();
		grid.setSelection(10, ROWS - 10);
		grid.select(ROWS - 5, ROWS - 1);
		grid.deselect(HALF, HALF + 9);
		grid.select(new int[] { 2, 4, 6 });
		grid.selectColumn(3);
		final long retained = usedHeap() - heap;
		assertNoItemPerSelectedRow("cell ranges", items, requests, 1);
		assertRetained("cell ranges", retained);
		// column 3 adds the rows 0..9 but 2, 4 and 6, HALF..HALF+9 and ROWS-9..ROWS-6
		final int expected = ((ROWS - 19) + 5 - 10 + 3) * COLUMNS + 7 + 10 + 4;
		assertEquals(expected, grid.getCellSelectionCount());
		assertTrue(grid.isCellSelected(new Point(4, 10)));
		assertFalse(grid.isCellSelected(new Point(0, HALF + 5)));
		assertTrue(grid.isCellSelected(new Point(3, HALF + 5)));
		assertFalse(grid.isCellSelected(new Point(2, ROWS - 7)));
		assertTrue(grid.isCellSelected(new Point(3, ROWS - 7)));
		assertTrue(grid.isSelected(0));
		assertEquals(ROWS, grid.getSelectionCount());
		final int[] indices = grid.getSelectionIndices();
		assertEquals(10, indices[0]);
		assertArrayEquals(new int[] { 2, 4, 6, 0 }, java.util.Arrays.copyOfRange(indices, ROWS - 24, ROWS - 20));
		assertNoItemPerSelectedRow("selection queries", items, requests, 1);
		final Point[] cells = grid.getCellSelection();
		assertEquals(new Point(0, 10), cells[0]);
		assertEquals(new Point(3, 0), cells[cells.length - 21]);
		assertEquals(new Point(3, ROWS - 6), cells[cells.length - 1]);

		grid.deselect(new int[] { 2, 11 });
		grid.setSelection(new int[] { 7, 3 });
		assertEquals(2 * COLUMNS, grid.getCellSelectionCount());
		assertArrayEquals(new int[] { 7, 3 }, grid.getSelectionIndices());
	}

	@Test
	public void shiftRangeMakesNoItemOrPointPerCell() {
		grid.setFocus();
		clickCell(1, 5, 0);
		assertEquals(1, grid.getCellSelectionCount());
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		final int[] events = { 0 };
		grid.addListener(SWT.Selection, e -> events[0]++);
		final long heap = usedHeap();
		clickCell(3, ROWS - 10, SWT.MOD2);
		final long retained = usedHeap() - heap;
		assertNoItemPerSelectedRow("shift+click", items, requests, 2);
		assertRetained("shift+click", retained);
		assertEquals(1, events[0]);
		assertEquals((ROWS - 14) * 3, grid.getCellSelectionCount());
		assertTrue(grid.isCellSelected(new Point(2, HALF)));
		assertFalse(grid.isCellSelected(new Point(0, HALF)));
		assertFalse(grid.isCellSelected(new Point(4, HALF)));
		assertFalse(grid.isCellSelected(new Point(1, ROWS - 9)));
		assertEquals(ROWS - 14, grid.getSelectionCount());
		final Point[] cells = grid.getCellSelection();
		assertEquals(new Point(1, 5), cells[0]);
		assertEquals(new Point(3, 5), cells[2]);
		assertEquals(new Point(1, 6), cells[3]);
		assertEquals(new Point(3, ROWS - 10), cells[cells.length - 1]);
	}

	@Test
	public void cellsKeepTheStockAnswersWhenRowsAndColumnsChange() {
		grid.selectAllCells();
		// a removed row takes its cells; the cells of the rows after it keep their row (stock)
		grid.remove(HALF);
		assertEquals((ROWS - 1) * COLUMNS, grid.getCellSelectionCount());
		assertTrue(grid.isCellSelected(new Point(0, ROWS - 1)));
		assertFalse(grid.isCellSelected(new Point(0, HALF)));
		assertTrue(grid.isCellSelected(new Point(0, HALF + 1)));
		grid.remove(0, 9);
		assertEquals((ROWS - 11) * COLUMNS, grid.getCellSelectionCount());
		// a removed column takes its cells; the cells right of it move one column left
		grid.getColumn(1).dispose();
		assertEquals((ROWS - 11) * (COLUMNS - 1), grid.getCellSelectionCount());
		assertTrue(grid.isCellSelected(new Point(COLUMNS - 2, ROWS - 1)));
		assertFalse(grid.isCellSelected(new Point(COLUMNS - 1, ROWS - 1)));
		// a truncation drops the rows past the count, and they do not come back
		grid.setItemCount(HALF);
		assertEquals((HALF - 10) * (COLUMNS - 1), grid.getCellSelectionCount());
		grid.setItemCount(ROWS);
		assertFalse(grid.isCellSelected(new Point(0, HALF)));
		final Point[] cells = grid.getCellSelection();
		assertEquals(new Point(0, 10), cells[0]);
		assertEquals(new Point(COLUMNS - 2, HALF - 1), cells[cells.length - 1]);
		grid.deselectAllCells();
		grid.disposeAllItems();
		grid.setItemCount(ROWS);
		assertEquals(0, grid.getCellSelectionCount());
		paint();
		assertTrue(grid.virtualMaterializedItemCount() <= windowRows() + 2);
	}
}
