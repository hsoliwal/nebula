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
 * The mouse gestures that select whole columns or long row ranges of a 100,000-row by 5-column
 * {@code SWT.VIRTUAL} Grid with cell selection make no item for the rows they select and ask no
 * SetData for rows that are not shown: a click on a column header or a column group header, a drag
 * across column headers, a press on a row header dragged past the last row or (after a scroll) up
 * to a row far above, a shift+click on a row header far from the focus row, and a body drag past the
 * last row. The default mode keeps the cells as blocks of rows by columns, so each gesture costs the
 * items of the rows shown (and the focus row a header click moves to), not one per selected row.
 * The answers are the stock ones (GridHeaderDragSelectionDifferential_Test compares the selection
 * and the events with the point list); the original mode keeps the stock point list.
 */
public class GridHeaderDragSelectionResidency_Test {

	/* the original mode's stock point list adds a cell in O(cells): fewer rows there */
	private static final int ROWS = GridTestUtil.fixesUpstreamDefects() ? 100_000 : 2_000;
	private static final int HALF = ROWS / 2;
	private static final int COLUMNS = 5;

	private Display display;
	private Shell shell;
	private Grid grid;
	private int setData;
	private final List<String> events = new ArrayList<>();

	private void open(final boolean group) {
		display = Display.getDefault();
		shell = new Shell(display);
		grid = new Grid(shell, SWT.VIRTUAL | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 0, 560, 300);
		grid.setHeaderVisible(true);
		grid.setRowHeaderVisible(true);
		final GridColumnGroup columnGroup = group ? new GridColumnGroup(grid, SWT.NONE) : null;
		if (columnGroup != null) {
			columnGroup.setText("group");
		}
		for (int c = 0; c < COLUMNS; c++) {
			final GridColumn column = columnGroup != null && (c == 1 || c == 2) ? new GridColumn(columnGroup, SWT.NONE)
					: new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setWidth(90);
		}
		grid.setCellSelectionEnabled(true);
		grid.addListener(SWT.SetData, e -> {
			setData++;
			final GridItem item = (GridItem) e.item;
			for (int c = 0; c < COLUMNS; c++) {
				item.setText(c, "r" + e.index + "c" + c);
			}
		});
		grid.addListener(SWT.Selection, e -> events.add("S" + row(e) + "/" + e.detail + "/" + e.stateMask));
		grid.addListener(SWT.DefaultSelection, e -> events.add("D" + row(e) + "/" + e.detail + "/" + e.stateMask));
		grid.setItemCount(ROWS);
		shell.setSize(600, 360);
		shell.open();
		paint();
	}

	private int row(final Event e) {
		return e.item instanceof GridItem ? grid.indexOf((GridItem) e.item) : -1;
	}

	@After
	public void tearDown() {
		if (shell != null) {
			shell.dispose();
		}
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

	/* scrolls to top row {@code top} and waits until the Grid answers it */
	private void top(final int top) {
		final long deadline = System.nanoTime() + 5_000_000_000L;
		do {
			grid.setTopIndex(top);
			paint();
		} while (grid.getTopIndex() != top && System.nanoTime() < deadline);
		assertEquals("top index", top, grid.getTopIndex());
	}

	/* the default mode keeps blocks; the original mode keeps the stock point list */
	private static boolean byBlocks() {
		return GridTestUtil.fixesUpstreamDefects();
	}

	private void assertItemsAndRequests(final String gesture, final int items, final int requests, final int windows) {
		paint();
		if (byBlocks()) {
			// the rows shown in each view the gesture passed through, the focus row and a few more
			final int bound = windows * windowRows() + 4;
			assertTrue(gesture + " made items: " + items + " -> " + grid.virtualMaterializedItemCount(),
					grid.virtualMaterializedItemCount() <= items + bound);
			assertTrue(gesture + " asked SetData: " + (setData - requests), setData - requests <= bound);
		}
		assertEquals(0, grid.getChildren().length);
	}

	/* where a click hits row {@code row}'s header (the row must be shown) */
	private Point rowHeader(final int row) {
		final Point point = GridHeaderGestures.rowHeader(grid, row, false);
		assertTrue("row " + row + " header not shown", point != null);
		return point;
	}

	/* where a click hits column {@code col}'s header (below its group header, if any) */
	private Point columnHeader(final int col) {
		final Point point = GridHeaderGestures.columnHeader(grid, col, false);
		assertTrue("column " + col + " header not hit", point != null);
		return point;
	}

	private void press(final Point point, final int stateMask) {
		GridHeaderGestures.press(grid, point, stateMask);
	}

	private void drag(final Point point) {
		GridHeaderGestures.drag(grid, point, 0);
	}

	private void release(final Point point) {
		GridHeaderGestures.release(grid, point, 0);
	}

	private void click(final Point point, final int stateMask) {
		GridHeaderGestures.click(grid, point, stateMask);
	}

	@Test
	public void columnHeaderClicksMakeNoItemPerRow() {
		open(false);
		top(HALF);
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		click(columnHeader(2), 0);
		assertItemsAndRequests("header click", items, requests, 1);
		if (!GridTestUtil.fixesUpstreamDefects()) {
			return; // stock: the header push swallows the click (UPSTREAM_DEFECTS.tsv D27)
		}
		assertEquals(Arrays.asList("S-1/0/" + SWT.BUTTON1), events);
		assertEquals(ROWS, grid.getCellSelectionCount());
		assertTrue(grid.isCellSelected(new Point(2, ROWS - 1)));
		assertTrue(grid.isCellSelected(new Point(2, HALF)));
		assertFalse(grid.isCellSelected(new Point(1, HALF)));
		assertEquals(new Point(2, 0), grid.getFocusCell());
		assertEquals(ROWS, grid.getSelectionCount());
		events.clear();

		// ctrl+click on the same header takes the column back (all its cells were selected)
		click(columnHeader(2), SWT.MOD1);
		assertEquals(0, grid.getCellSelectionCount());
		// ctrl+clicks on two headers add both columns, column by column
		click(columnHeader(1), SWT.MOD1);
		click(columnHeader(3), SWT.MOD1);
		assertEquals(Arrays.asList("S-1/0/" + (SWT.MOD1 | SWT.BUTTON1), "S-1/0/" + (SWT.MOD1 | SWT.BUTTON1),
				"S-1/0/" + (SWT.MOD1 | SWT.BUTTON1)), events);
		assertItemsAndRequests("ctrl+header clicks", items, requests, 1);
		assertEquals(2 * ROWS, grid.getCellSelectionCount());
		final Point[] cells = grid.getCellSelection();
		assertEquals(new Point(1, 0), cells[0]);
		assertEquals(new Point(1, ROWS - 1), cells[ROWS - 1]);
		assertEquals(new Point(3, 0), cells[ROWS]);
		assertEquals(new Point(3, ROWS - 1), cells[2 * ROWS - 1]);
	}

	@Test
	public void columnGroupHeaderPressAndDragAcrossHeadersMakeNoItemPerRow() {
		open(true);
		top(HALF);
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		// a press on the group header (above column 1) selects the group's columns
		final Point groupHeader = new Point(columnHeader(1).x, Math.max(1, grid.getGroupHeaderHeight() / 2));
		press(groupHeader, 0);
		assertEquals(2 * ROWS, grid.getCellSelectionCount());
		assertTrue(grid.isCellSelected(new Point(2, ROWS - 1)));
		// dragged on to column 4's header: the columns from 4 back to the focus column 1
		drag(columnHeader(4));
		assertEquals(4 * ROWS, grid.getCellSelectionCount());
		release(columnHeader(4));
		assertItemsAndRequests("group header drag", items, requests, 1);
		assertEquals(Arrays.asList("S-1/0/0", "S-1/" + SWT.DRAG + "/" + SWT.BUTTON1, "S-1/0/" + SWT.BUTTON1), events);
		assertFalse(grid.isCellSelected(new Point(0, HALF)));
		final Point[] cells = grid.getCellSelection();
		assertEquals(new Point(4, 0), cells[0]);
		assertEquals(new Point(3, 0), cells[ROWS]);
		assertEquals(new Point(2, 0), cells[2 * ROWS]);
		assertEquals(new Point(1, ROWS - 1), cells[4 * ROWS - 1]);
	}

	@Test
	public void rowHeaderDragPastTheLastRowMakesNoItemPerRow() {
		open(false);
		top(0);
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		final Point press = rowHeader(3);
		press(press, 0);
		assertEquals(COLUMNS, grid.getCellSelectionCount());
		// below the Grid: the drag reaches the last row (and shows it)
		final Point below = GridHeaderGestures.below(grid, press.x);
		drag(below);
		release(below);
		assertItemsAndRequests("row header drag down", items, requests, 2);
		assertEquals(Arrays.asList("S3/0/0", "S-1/" + SWT.DRAG + "/" + SWT.BUTTON1, "S-1/0/" + SWT.BUTTON1), events);
		assertEquals((long) (ROWS - 3) * COLUMNS, grid.getCellSelectionCount());
		assertEquals(ROWS - 3, grid.getSelectionCount());
		assertTrue(grid.isCellSelected(new Point(4, ROWS - 1)));
		assertFalse(grid.isCellSelected(new Point(0, 2)));
		final Point[] cells = grid.getCellSelection();
		assertEquals(new Point(0, 3), cells[0]);
		assertEquals(new Point(4, ROWS - 1), cells[cells.length - 1]);
	}

	@Test
	public void rowHeaderDragUpAfterAScrollMakesNoItemPerRow() {
		open(false);
		top(ROWS - windowRows() + 1);
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		press(rowHeader(ROWS - 3), 0);
		// the wheel scrolls to the top while the button is down, then the drag goes on to row 2
		top(0);
		final Point to = rowHeader(2);
		drag(to);
		release(to);
		assertItemsAndRequests("row header drag up", items, requests, 2);
		assertEquals(Arrays.asList("S" + (ROWS - 3) + "/0/0", "S2/" + SWT.DRAG + "/" + SWT.BUTTON1,
				"S2/0/" + SWT.BUTTON1), events);
		assertEquals((long) (ROWS - 4) * COLUMNS, grid.getCellSelectionCount());
		// the stock order: from the anchor row up to the hovered row
		final Point[] cells = grid.getCellSelection();
		assertEquals(new Point(0, ROWS - 3), cells[0]);
		assertEquals(new Point(4, ROWS - 3), cells[COLUMNS - 1]);
		assertEquals(new Point(0, ROWS - 4), cells[COLUMNS]);
		assertEquals(new Point(4, 2), cells[cells.length - 1]);
		final int[] indices = grid.getSelectionIndices();
		assertEquals(ROWS - 4, indices.length);
		assertEquals(ROWS - 3, indices[0]);
		assertEquals(2, indices[indices.length - 1]);
	}

	@Test
	public void rowHeaderShiftClickFarFromTheFocusMakesNoItemPerRow() {
		open(false);
		top(0);
		click(rowHeader(2), 0);
		top(ROWS - windowRows() + 1);
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		events.clear();
		click(rowHeader(ROWS - 2), SWT.MOD2);
		assertItemsAndRequests("row header shift+click", items, requests, 1);
		assertEquals(Arrays.asList("S" + (ROWS - 2) + "/" + SWT.DRAG + "/" + SWT.MOD2), events);
		assertEquals((long) (ROWS - 3) * COLUMNS, grid.getCellSelectionCount());
		final Point[] cells = grid.getCellSelection();
		assertEquals(new Point(0, 2), cells[0]);
		assertEquals(new Point(4, ROWS - 2), cells[cells.length - 1]);
		// the rows in the order of their first cell: from the focus row down
		assertArrayEquals(new int[] { 2, 3 }, Arrays.copyOf(grid.getSelectionIndices(), 2));
	}

	@Test
	public void bodyDragPastTheLastRowMakesNoItemPerRow() {
		open(false);
		top(0);
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		final GridItem item = grid.getItem(3);
		final Rectangle bounds = item.getBounds(1);
		final Point press = new Point(bounds.x + 4, bounds.y + bounds.height / 2);
		press(press, 0);
		final Rectangle to = grid.getItem(3).getBounds(3);
		final Point below = GridHeaderGestures.below(grid, to.x + 4);
		drag(below);
		release(below);
		assertItemsAndRequests("body drag", items, requests, 2);
		assertEquals((long) (ROWS - 3) * 3, grid.getCellSelectionCount());
		assertTrue(grid.isCellSelected(new Point(3, ROWS - 1)));
		assertFalse(grid.isCellSelected(new Point(4, ROWS - 1)));
	}
}
