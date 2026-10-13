/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The column window must paint exactly what the full column walk paints: the same Grid is
 * printed (capture mode CONTROL_PRINT) with the window on and off at many horizontal scroll
 * positions, with hidden, fixed and fill columns, row headers, column groups and spans.
 */
public class GridColumnWindow_Test {

	private Display display;
	private boolean rowHeader = true;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		new Text(shell, SWT.SINGLE).setBounds(0, 0, 40, 20);
		shell.setSize(700, 500);
		shell.open();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void plainColumnsPaintIdentically() throws Exception {
		final Grid grid = grid(SWT.NONE, false, false);
		assertWindowInvisible(grid, true);
	}

	@Test
	public void pixelScrollingWithoutRowHeaderPaintsIdentically() throws Exception {
		rowHeader = false;
		final Grid grid = grid(SWT.VIRTUAL, true, false);
		assertTrue(!grid.getColumnScrolling());
		assertWindowInvisible(grid, true);
	}

	@Test
	public void virtualGridWithFixedAndHiddenColumnsPaintsIdentically() throws Exception {
		final Grid grid = grid(SWT.VIRTUAL, true, false);
		assertWindowInvisible(grid, true);
	}

	@Test
	public void columnGroupsKeepTheFullHeaderWalkAndPaintIdentically() throws Exception {
		final Grid grid = grid(SWT.NONE, true, true);
		assertWindowInvisible(grid, true); // body window stays active; the header walks all columns
	}

	@Test
	public void spanningGridsKeepTheFullWalk() throws Exception {
		final Grid grid = grid(SWT.NONE, false, false);
		grid.getItem(3).setColumnSpan(2, 3);
		assertWindowInvisible(grid, false);
	}

	private void assertWindowInvisible(final Grid grid, final boolean expectFewerVisits) throws Exception {
		grid.redraw();
		grid.update(); // the scroll metrics are settled by the paint
		final ScrollBar bar = grid.getHorizontalBar();
		final int max = bar.getMaximum() - bar.getThumb();
		assertTrue("needs horizontal scrolling, max=" + max + " size=" + grid.getSize() + " client=" + grid.getClientArea()
				+ " bar=" + bar.getMaximum() + "/" + bar.getThumb() + "/" + bar.getSelection() + " visible=" + bar.getVisible(),
				max > 20);
		final Random random = new Random(11L);
		// pixel scrolling: probe edges and random offsets; column scrolling (row header): every column
		final int[] positions = max <= 100 ? java.util.stream.IntStream.rangeClosed(0, max).toArray()
				: new int[] { 0, 1, 2, 37, 79, 80, 81, 500, max / 2, max - 1, max, random.nextInt(max),
						random.nextInt(max), random.nextInt(max) };
		boolean fewer = false;
		for (final int position : positions) {
			bar.setSelection(position);
			bar.notifyListeners(SWT.Selection, new Event());
			grid.update();
			flush();
			grid.columnWindowEnabled = false;
			final String full = GridRowPermutation_Test.pixels(grid);
			final int fullVisits = grid.paintColumnVisits;
			final int fullCells = grid.paintCellCount;
			grid.columnWindowEnabled = true;
			final String windowed = GridRowPermutation_Test.pixels(grid);
			assertEquals("pixels at hScroll=" + position, full, windowed);
			assertEquals("painted cells at hScroll=" + position, fullCells, grid.paintCellCount);
			assertTrue(grid.paintColumnVisits <= fullVisits);
			fewer |= grid.paintColumnVisits < fullVisits;
		}
		assertEquals("column visits drop when the window is active", expectFewerVisits, fewer);
	}

	private Grid grid(final int style, final boolean fixedAndHidden, final boolean groups) {
		final Grid grid = new Grid(shell, style | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 30, 660, 420);
		grid.setHeaderVisible(true);
		if (rowHeader) {
			grid.setRowHeaderVisible(true); // also switches to column scrolling
		}
		grid.setLinesVisible(true);
		final Random random = new Random(3L);
		GridColumnGroup group = null;
		for (int c = 0; c < 60; c++) {
			final GridColumn column;
			if (groups && c % 7 == 0) {
				group = new GridColumnGroup(grid, SWT.NONE);
				group.setText("G" + c);
			}
			column = groups && group != null ? new GridColumn(group, SWT.NONE) : new GridColumn(grid, SWT.NONE);
			column.setText("Column " + c);
			column.setWidth(20 + random.nextInt(121));
			if (fixedAndHidden && c < 2 && !groups) {
				column.setFixed(true);
			}
			if (fixedAndHidden && c % 11 == 5) {
				column.setVisible(false);
			}
		}
		if (style == SWT.VIRTUAL) {
			grid.addListener(SWT.SetData, e -> {
				final GridItem item = (GridItem) e.item;
				for (int c = 0; c < 60; c++) {
					item.setText(c, "v" + e.index + "/" + c);
				}
			});
			grid.setItemCount(300);
		} else {
			for (int r = 0; r < 300; r++) {
				final GridItem item = new GridItem(grid, SWT.NONE);
				for (int c = 0; c < 60; c++) {
					item.setText(c, "r" + r + "/" + c + (c % 9 == 0 ? " a longer cell text" : ""));
				}
			}
		}
		grid.setSelection(new int[] { 2, 5, 9 });
		flush();
		return grid;
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain
		}
	}
}
