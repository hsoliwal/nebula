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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.BitSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The Grid is its own viewport: a flat {@code SWT.VIRTUAL} Grid keeps an item only for a row that
 * was painted or read, paints only the rows of its window, and no API call that does not read a row
 * makes one. Measured with the Grid's residency and paint counters, in every widget mode.
 */
public class GridSparseRowResidency_Test {

	private static final int ROWS = 100_000;
	private static final int COLUMNS = 5;

	private Display display;
	private Shell shell;
	private Grid grid;
	/* rows (at the time of the request) that SWT.SetData was asked for */
	private final BitSet requested = new BitSet();
	private int setData;
	private int shellsBefore;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shellsBefore = display.getShells().length;
		shell = new Shell(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	private void openVirtual(final int rows) {
		grid = new Grid(shell, SWT.VIRTUAL | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 0, 500, 300);
		grid.setHeaderVisible(true);
		for (int c = 0; c < COLUMNS; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(90);
		}
		grid.addListener(SWT.SetData, e -> {
			setData++;
			requested.set(e.index);
			final GridItem item = (GridItem) e.item;
			for (int c = 0; c < COLUMNS; c++) {
				item.setText(c, "r" + e.index + "c" + c);
			}
		});
		grid.setItemCount(rows);
		shell.setSize(540, 360);
		shell.open();
		paint();
	}

	private void paint() {
		final int before = grid.paintPasses;
		grid.redraw();
		grid.update();
		GridSwtScreenshotCapture.awaitStableCounter(display, () -> grid.paintPasses, before + 1,
				"Grid paint did not settle");
	}

	/* rows of the window, the partly shown last one included */
	private int windowRows() {
		return grid.getVisibleGridHeight() / (grid.getItemHeight() + 1) + 1;
	}

	private void assertPaintedOnlyTheWindow(final String when) {
		final int window = windowRows();
		assertTrue(when + ": cells painted " + grid.paintCellCount + " for a window of " + window + " rows",
				grid.paintCellCount <= (window + 1) * COLUMNS);
	}

	@Test
	public void everyItemOfAVirtualGridBelongsToAPaintedRow() {
		openVirtual(ROWS);
		// the scroll bar update measures the rows' extent without making an item for the last row
		assertEquals("items after the first paint", setData, grid.virtualMaterializedItemCount());
		assertTrue(setData <= windowRows() + 1);
		assertPaintedOnlyTheWindow("first paint");

		final int beforeJump = setData;
		grid.setTopIndex(ROWS / 2);
		paint();
		assertTrue("SetData calls for one window: " + (setData - beforeJump), setData - beforeJump <= windowRows() + 1);
		assertEquals("items after a jump", setData, grid.virtualMaterializedItemCount());
		assertPaintedOnlyTheWindow("after a jump");

		final int pageRows = windowRows() - 1;
		for (int page = 1; page <= 40; page++) {
			grid.setTopIndex(ROWS / 2 + page * pageRows);
			paint();
			assertPaintedOnlyTheWindow("page " + page);
		}
		assertEquals("items after paging", setData, grid.virtualMaterializedItemCount());
		assertEquals("the Grid paints itself: no child control", 0, grid.getChildren().length);
		assertEquals("no popup or helper shell", shellsBefore + 1, display.getShells().length);
	}

	@Test
	public void clearKeepsUnreadRowsWithoutItems() {
		openVirtual(ROWS);
		final GridItem read = grid.getItem(ROWS - 10);
		assertEquals("r" + (ROWS - 10) + "c1", read.getText(1));
		final int items = grid.virtualMaterializedItemCount();
		final int painted = setData - 1;

		final int beforeClear = setData;
		grid.clearAll(true);
		paint();
		assertEquals("clearAll makes no item for an unread row", items, grid.virtualMaterializedItemCount());
		assertEquals("the painted rows ask their data again", painted, setData - beforeClear);

		grid.clear(0, ROWS - 1, false);
		grid.clear(new int[] { 7, ROWS / 3, ROWS - 1 }, false);
		grid.clear(ROWS / 2, false);
		paint();
		assertEquals("clear makes no item for an unread row", items, grid.virtualMaterializedItemCount());

		// a cleared item keeps its identity and asks again when read
		final int beforeRead = setData;
		assertTrue(read == grid.getItem(ROWS - 10));
		assertEquals("r" + (ROWS - 10) + "c1", read.getText(1));
		assertEquals(1, setData - beforeRead);
		// an unread row reads as a cleared one: default attributes, one SetData
		final GridItem unread = grid.getItem(ROWS / 3 + 1);
		assertEquals("r" + (ROWS / 3 + 1) + "c4", unread.getText(4));
		assertEquals(2, setData - beforeRead);
	}

	@Test
	public void clearForgetsTheCellsOfReleasedRows() {
		final Color red = display.getSystemColor(SWT.COLOR_RED);
		final int[] generation = { 0 };
		grid = new Grid(shell, SWT.VIRTUAL | SWT.CHECK | SWT.MULTI | SWT.V_SCROLL);
		grid.setBounds(0, 0, 400, 300);
		for (int c = 0; c < 3; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(100);
		}
		grid.addListener(SWT.SetData, e -> {
			setData++;
			final GridItem item = (GridItem) e.item;
			item.setText(0, "g" + generation[0] + "r" + e.index);
			if (generation[0] == 0 && e.index % 2 == 1 && e.index < 10) {
				item.setBackground(0, red);
				item.setChecked(0, true);
				item.setText(1, "gen0");
			}
		});
		grid.setItemCount(1000);
		grid.setItemResidency(Grid.RESIDENCY_EVICT_COLD, 5);
		shell.setSize(440, 340);
		shell.open();
		paint();
		final int[] decorated = { 3, 5, 7, 9 };
		final GridItem[] released = new GridItem[decorated.length];
		for (int i = 0; i < decorated.length; i++) {
			released[i] = grid.getItem(decorated[i]);
			assertTrue(red.equals(released[i].getBackground(0)) && released[i].getChecked(0));
		}
		grid.setTopIndex(500);
		paint();
		for (final GridItem item : released) {
			assertTrue("the decorated rows were released", item.isDisposed());
		}

		generation[0] = 1;
		final int items = grid.virtualMaterializedItemCount();
		grid.clear(3, false);
		grid.clear(new int[] { 5 }, false);
		grid.clear(6, 8, false);
		grid.clearAll(true);
		assertEquals("clear makes no item", items, grid.virtualMaterializedItemCount());

		final boolean fixes = GridTestUtil.fixesUpstreamDefects();
		final Color defaultBackground = grid.getItem(4).getBackground(0);
		for (final int row : decorated) {
			final int before = setData;
			final GridItem item = grid.getItem(row);
			assertEquals("g1r" + row, item.getText(0));
			assertEquals("one SetData for released row " + row, 1, setData - before);
			// the default mode clears the cells (Grid.clear Javadoc); the original mode keeps them, as stock
			assertEquals("background of row " + row, fixes ? defaultBackground : red, item.getBackground(0));
			assertEquals("check of row " + row, Boolean.valueOf(!fixes), Boolean.valueOf(item.getChecked(0)));
			assertEquals("column 1 of row " + row, fixes ? "" : "gen0", item.getText(1));
		}
	}

	@Test
	public void selectAllSelectsEveryVirtualRow() {
		openVirtual(20_000);
		grid.selectAll();
		assertEquals(20_000, grid.getSelectionCount());
		assertTrue(grid.isSelected(19_999));
		assertEquals(20_000, grid.getSelectionIndices().length);
		grid.deselectAll();
		assertEquals(0, grid.getSelectionCount());
	}

	@Test
	public void removingUnreadRowsMakesNoItem() {
		openVirtual(ROWS);
		final int painted = setData;
		grid.remove(1000, ROWS - 1);
		for (int row = 999; row >= 900; row--) {
			grid.remove(row);
		}
		paint();
		assertEquals(900, grid.getItemCount());
		assertEquals("no SetData for removed rows", painted, setData);
		assertEquals("items left: the painted rows only", painted, grid.virtualMaterializedItemCount());
		assertPaintedOnlyTheWindow("after removal");
	}

	@Test
	public void userItemsPaintOnlyTheWindow() {
		grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 0, 500, 300);
		grid.setHeaderVisible(true);
		for (int c = 0; c < COLUMNS; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(90);
		}
		final int rows = 50_000;
		for (int r = 0; r < rows; r++) {
			new GridItem(grid, SWT.NONE).setText(0, "r" + r);
		}
		shell.setSize(540, 360);
		shell.open();
		paint();
		assertPaintedOnlyTheWindow("top");
		for (final int top : new int[] { rows / 2, rows - 1, 3 }) {
			grid.setTopIndex(top);
			paint();
			assertPaintedOnlyTheWindow("top index " + top);
		}
		assertEquals(0, grid.getChildren().length);
	}
}
