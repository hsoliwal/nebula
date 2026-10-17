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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Reports of the archived Eclipse Bugzilla (Nebula Grid, closed in bulk on 2021-07-05) reproduced
 * against the Grid of each mode; K:/m3-work/runs/bugs/nebula-BUGS.tsv lists them.
 */
public class GridArchivedBugs_Test {
	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setSize(400, 300);
		shell.open();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	/* bug 455474: disposing the background colour of an item destroyed the Grid's paint */
	@Test
	public void bug455474_disposedItemColourPaints() {
		final Grid grid = new Grid(shell, SWT.V_SCROLL);
		grid.setBounds(0, 0, 300, 200);
		new GridColumn(grid, SWT.NONE).setWidth(100);
		final GridItem item = new GridItem(grid, SWT.NONE);
		item.setText("item");
		final Color color = new Color(display, 200, 30, 30);
		item.setBackground(0, color);
		item.setForeground(0, color);
		flush(grid);
		color.dispose();
		GridTestUtil.assertThrowsByMode(null, IllegalArgumentException.class, () -> flush(grid));
	}

	/* bug 218708: a click on a column header of an empty Grid with a row header crashed the paint */
	@Test
	public void bug218708_headerClickOnAnEmptyGrid() {
		final Grid grid = new Grid(shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 0, 300, 200);
		grid.setHeaderVisible(true);
		grid.setRowHeaderVisible(true);
		final GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setWidth(100);
		column.setText("c");
		final int[] selections = { 0 };
		column.addListener(SWT.Selection, e -> selections[0]++);
		flush(grid);
		final Rectangle header = new Rectangle(grid.getRowHeaderWidth() + 10, 2, 1, 1);
		mouse(grid, SWT.MouseDown, header.x, header.y);
		mouse(grid, SWT.MouseUp, header.x, header.y);
		flush(grid);
		assertEquals(1, selections[0]);
	}

	/* bug 454575: disposeAllItems leaves the state removeItem resets */
	@Test
	public void bug454575_disposeAllItemsResetsTheState() {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL);
		grid.setBounds(0, 0, 300, 200);
		new GridColumn(grid, SWT.NONE).setWidth(100);
		for (int i = 0; i < 50; i++) {
			new GridItem(grid, SWT.NONE).setText("row " + i);
		}
		grid.setSelection(new int[] { 3, 4, 40 });
		grid.setFocusItem(grid.getItem(40));
		grid.setTopIndex(30);
		flush(grid);
		grid.disposeAllItems();
		flush(grid);
		assertEquals(0, grid.getItemCount());
		assertEquals(0, grid.getSelectionCount());
		assertEquals(0, grid.getSelectionIndices().length);
		assertNull(grid.getFocusItem());
		assertEquals(0, grid.getTopIndex());
		for (int i = 0; i < 5; i++) {
			new GridItem(grid, SWT.NONE).setText("new " + i);
		}
		flush(grid);
		assertEquals(5, grid.getItemCount());
		assertEquals(0, grid.getSelectionCount());
		assertEquals(4, grid.getItem(4).getRowIndex());
	}

	/* bug 377617: a 0-width column with a wrapping header made the whole header very tall */
	@Test
	public void bug377617_zeroWidthWrappingHeaderKeepsTheHeaderHeight() {
		final Grid grid = new Grid(shell, SWT.V_SCROLL);
		grid.setBounds(0, 0, 300, 200);
		grid.setHeaderVisible(true);
		grid.setAutoHeight(true);
		final GridColumn plain = new GridColumn(grid, SWT.NONE);
		plain.setText("plain");
		plain.setWidth(100);
		flush(grid);
		final int before = grid.getHeaderHeight();
		final GridColumn hidden = new GridColumn(grid, SWT.NONE);
		hidden.setHeaderWordWrap(true);
		hidden.setText("a header text long enough to wrap once per character at width zero");
		hidden.setWidth(0);
		hidden.setVisible(false);
		flush(grid);
		if (GridTestUtil.fixesUpstreamDefects()) {
			// a hidden or zero-width column shows no header: it does not count (D24)
			assertEquals(before, grid.getHeaderHeight());
		} else {
			assertTrue(grid.getHeaderHeight() > 2 * before);
		}
	}

	/* bug 259087: packing the column of a group ignored the group header text */
	@Test
	public void bug259087_packFitsTheGroupHeaderText() {
		final Grid grid = new Grid(shell, SWT.V_SCROLL);
		grid.setBounds(0, 0, 300, 200);
		grid.setHeaderVisible(true);
		final GridColumnGroup group = new GridColumnGroup(grid, SWT.NONE);
		group.setText("A column group header text far wider than its single short column");
		final GridColumn column = new GridColumn(group, SWT.NONE);
		column.setText("c");
		column.setWidth(40);
		new GridItem(grid, SWT.NONE).setText("x");
		flush(grid);
		column.pack();
		final GC gc = new GC(grid);
		final int groupWidth;
		try {
			groupWidth = group.getHeaderRenderer().computeSize(gc, SWT.DEFAULT, SWT.DEFAULT, group).x;
		} finally {
			gc.dispose();
		}
		if (GridTestUtil.fixesUpstreamDefects()) {
			// the group header spans its columns: the pack makes its text fit (D25)
			assertTrue(column.getWidth() + " >= " + groupWidth, column.getWidth() >= groupWidth);
		} else {
			assertTrue(column.getWidth() < groupWidth / 2);
		}
	}

	/* bug 382376: packing a column attributed the whole content of a spanning cell to it */
	@Test
	public void bug382376_packHonoursTheColumnSpan() {
		final Grid grid = new Grid(shell, SWT.V_SCROLL);
		grid.setBounds(0, 0, 300, 200);
		grid.setHeaderVisible(true);
		final String text = "cell content measured across the span of two columns";
		final int spanned = packedWidth(grid, text, 1);
		final Grid plain = new Grid(shell, SWT.V_SCROLL);
		plain.setBounds(0, 0, 300, 200);
		plain.setHeaderVisible(true);
		final int unspanned = packedWidth(plain, text, 0);
		if (GridTestUtil.fixesUpstreamDefects()) {
			// the next column already covers 120 px of the spanning cell (D26)
			assertEquals(Math.max(0, unspanned - 120), spanned);
		} else {
			assertEquals(unspanned, spanned);
		}
	}

	private int packedWidth(final Grid grid, final String text, final int span) {
		final GridColumn first = new GridColumn(grid, SWT.NONE);
		first.setText("c0");
		first.setWidth(40);
		final GridColumn second = new GridColumn(grid, SWT.NONE);
		second.setText("c1");
		second.setWidth(120);
		final GridItem item = new GridItem(grid, SWT.NONE);
		item.setText(0, text);
		if (span > 0) {
			item.setColumnSpan(0, span);
		}
		flush(grid);
		first.pack();
		return first.getWidth();
	}

	/* bugs 218713 and 238601: with cell selection a header click fired no column Selection event */
	@Test
	public void bug238601_headerClickWithCellSelectionFiresTheColumnEvent() {
		final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 0, 300, 200);
		grid.setHeaderVisible(true);
		grid.setCellSelectionEnabled(true);
		final GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setText("c");
		column.setWidth(100);
		for (int i = 0; i < 3; i++) {
			new GridItem(grid, SWT.NONE).setText("row " + i);
		}
		final int[] selections = { 0 };
		column.addListener(SWT.Selection, e -> selections[0]++);
		flush(grid);
		mouse(grid, SWT.MouseDown, 50, 2);
		mouse(grid, SWT.MouseUp, 50, 2);
		flush(grid);
		assertEquals("column Selection events", 1, selections[0]);
		assertEquals("cells of the clicked column", 3, grid.getCellSelectionCount());
	}

	private static void mouse(final Grid grid, final int type, final int x, final int y) {
		final Event event = new Event();
		event.button = 1;
		event.x = x;
		event.y = y;
		grid.notifyListeners(type, event);
	}

	private void flush(final Grid grid) {
		grid.redraw();
		grid.update();
		while (display.readAndDispatch()) {
			// drain
		}
	}
}
