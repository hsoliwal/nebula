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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Selecting rows of a 100,000-row {@code SWT.VIRTUAL} Grid makes no item and asks no SetData: the
 * default mode keeps the selection by row (one run for select-all), so selectAll, range selection
 * and shift+End cost no item per selected row, and the selection moves with inserted, removed and
 * truncated rows without leaving an entry behind. The original mode keeps the stock item list.
 */
public class GridRowSelectionResidency_Test {

	/* the original mode's stock item list adds a shift range in O(rows^2): fewer rows there */
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

	/* the default mode keeps rows; the original mode keeps the stock item list */
	private static boolean byRow() {
		return GridTestUtil.fixesUpstreamDefects();
	}

	private void assertNoItemPerSelectedRow(final String operation, final int items, final int requests) {
		paint();
		if (byRow()) {
			assertTrue(operation + " made items: " + items + " -> " + grid.virtualMaterializedItemCount(),
					grid.virtualMaterializedItemCount() <= items + windowRows() + 2);
			assertTrue(operation + " asked SetData: " + (setData - requests), setData - requests <= windowRows() + 2);
		}
		assertTrue("cells painted " + grid.paintCellCount, grid.paintCellCount <= (windowRows() + 1) * COLUMNS);
		assertEquals(0, grid.getChildren().length);
	}

	@Test
	public void selectAllMakesNoItemPerRow() {
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		final long heap = usedHeap();
		grid.selectAll();
		final long retained = usedHeap() - heap;
		assertNoItemPerSelectedRow("selectAll", items, requests);
		assertEquals(ROWS, grid.getSelectionCount());
		assertTrue(grid.isSelected(ROWS - 1));
		assertEquals(0, grid.getSelectionIndex());
		final int[] indices = grid.getSelectionIndices();
		assertEquals(ROWS, indices.length);
		assertEquals(ROWS - 1, indices[ROWS - 1]);
		if (byRow()) {
			// the item list retained about 28 MB for 100,000 rows; one run is a few bytes
			assertTrue("retained after selectAll: " + retained, retained < 4_000_000L);
		}
	}

	@Test
	public void rangeSelectionMakesNoItemPerRow() {
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		grid.setSelection(10, ROWS - 10);
		grid.select(ROWS - 5, ROWS - 1);
		grid.deselect(HALF, HALF + 9);
		grid.select(new int[] { 2, 4, 6 });
		assertNoItemPerSelectedRow("range selection", items, requests);
		assertEquals((ROWS - 19) + 5 - 10 + 3, grid.getSelectionCount());
		assertFalse(grid.isSelected(HALF + 5));
		assertTrue(grid.isSelected(HALF + 10));
		assertFalse(grid.isSelected(ROWS - 7));
		final int[] indices = grid.getSelectionIndices();
		assertEquals(10, indices[0]);
		assertEquals(ROWS - 1, indices[indices.length - 4]);
		assertArrayEquals(new int[] { 2, 4, 6 }, java.util.Arrays.copyOfRange(indices, indices.length - 3, indices.length));
	}

	@Test
	public void shiftEndMakesNoItemPerRow() {
		grid.setFocus();
		grid.setSelection(0);
		grid.setFocusItem(grid.getItem(0));
		final int items = grid.virtualMaterializedItemCount();
		final int requests = setData;
		final int[] events = { 0 };
		grid.addListener(SWT.Selection, e -> events[0]++);
		final Event end = new Event();
		end.keyCode = SWT.END;
		end.stateMask = SWT.MOD2;
		grid.notifyListeners(SWT.KeyDown, end);
		assertNoItemPerSelectedRow("shift+End", items, requests);
		assertEquals(1, events[0]);
		assertEquals(ROWS, grid.getSelectionCount());
		assertEquals(ROWS - 1, grid.indexOf(grid.getFocusItem()));
	}

	@Test
	public void selectedRowsMoveWithTheRowsAndLeaveNoEntryBehind() {
		grid.selectAll();
		grid.remove(0, HALF - 1);
		assertEquals(ROWS - HALF, grid.getSelectionCount());
		new GridItem(grid, SWT.NONE, 10).setText(0, "inserted");
		assertEquals(ROWS - HALF, grid.getSelectionCount());
		assertFalse(grid.isSelected(10));
		assertTrue(grid.isSelected(9) && grid.isSelected(11));
		grid.setItemCount(5);
		assertEquals(5, grid.getSelectionCount());
		grid.setItemCount(ROWS);
		// the truncated rows do not come back selected
		assertEquals(5, grid.getSelectionCount());
		assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, grid.getSelectionIndices());
		grid.selectAll();
		grid.disposeAllItems();
		grid.setItemCount(ROWS);
		assertEquals(0, grid.getSelectionCount());
		assertFalse(grid.isSelected(0));
		paint();
		assertTrue(grid.virtualMaterializedItemCount() <= windowRows() + 2);
	}

	@Test
	public void getSelectionMakesTheItemsItReturns() {
		grid.setSelection(HALF, HALF + 9);
		final GridItem[] selection = grid.getSelection();
		assertEquals(10, selection.length);
		for (int i = 0; i < selection.length; i++) {
			assertEquals(HALF + i, grid.indexOf(selection[i]));
			assertTrue(selection[i] == grid.getItem(HALF + i));
		}
	}
}
