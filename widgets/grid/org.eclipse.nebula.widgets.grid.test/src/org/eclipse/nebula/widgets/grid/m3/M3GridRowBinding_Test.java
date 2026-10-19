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
package org.eclipse.nebula.widgets.grid.m3;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.m3.M3ColumnFilter;
import org.eclipse.swt.m3.M3ColumnKey;
import org.eclipse.swt.m3.M3ColumnSort;
import org.eclipse.swt.m3.M3TextMatch;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A virtual Grid bound to the M3 column filter and sort: header clicks, patches on view changes,
 * selection kept by source row. Runs on the hidden test desktop.
 */
public class M3GridRowBinding_Test {

	private static final int ROWS = 20_000;
	private Display display;
	private Shell shell;
	private int[] numbers;
	private String[] names;
	private int fills;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(500, 400);
		final Random random = new Random(5);
		numbers = new int[ROWS];
		names = new String[ROWS];
		for (int i = 0; i < ROWS; i++) {
			numbers[i] = random.nextInt(50);
			names[i] = "n" + random.nextInt(1000);
		}
	}

	@After
	public void tearDown() {
		shell.dispose();
		drain();
	}

	private void drain() {
		while (display.readAndDispatch()) {
			/* run pending events */
		}
	}

	private String cell(final int row, final int column) {
		return column == 0 ? String.valueOf(numbers[row]) : names[row];
	}

	private Grid grid() {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setHeaderVisible(true);
		for (final String title : new String[] { "Number", "Name" }) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText(title);
			column.setWidth(150);
		}
		return grid;
	}

	private static void click(final GridColumn column, final boolean shift) {
		final Event event = new Event();
		event.stateMask = shift ? SWT.SHIFT : 0;
		column.notifyListeners(SWT.Selection, event);
	}

	@Test
	public void bindingSortsFiltersAndKeepsTheSelection() {
		final Grid grid = grid();
		final M3ColumnFilter filter = new M3ColumnFilter(2, ROWS, this::cell);
		final M3ColumnSort sort = new M3ColumnSort(2, filter);
		sort.setKey(0, M3ColumnKey.ofInts(r -> numbers[r]));
		sort.setCellText(this::cell);
		fills = 0;
		final M3GridRowBinding binding = M3GridRowBinding.bind(grid, sort, (item, row) -> {
			fills++;
			item.setText(0, cell(row, 0));
			item.setText(1, cell(row, 1));
		});
		M3GridSortHeader.install(grid, sort);
		M3GridSortHeader.install(grid, sort);
		shell.open();
		drain();
		assertEquals(ROWS, grid.getItemCount());
		final int visible = grid.getClientArea().height / grid.getItemHeight() + 2;
		assertTrue("fills " + fills, fills <= visible * 3);

		click(grid.getColumn(0), false);
		assertEquals(SWT.UP, sort.getDirection(0));
		assertEquals(SWT.UP, grid.getColumn(0).getSort());
		click(grid.getColumn(1), true);
		assertEquals(2, sort.getSortKeyCount());
		assertEquals(SWT.UP, grid.getColumn(1).getSort());
		click(grid.getColumn(1), true);
		assertEquals(SWT.DOWN, grid.getColumn(1).getSort());
		drain();
		final GridItem first = grid.getItem(0);
		assertEquals(cell(sort.map(0), 1), first.getText(1));

		final int keep = sort.map(3);
		binding.setSelectedRows(keep);
		assertArrayEquals(new int[] { 3 }, grid.getSelectionIndices());
		fills = 0;
		filter.setText(1, "n1*", M3TextMatch.WILDCARD);
		drain();
		assertEquals(sort.size(), grid.getItemCount());
		assertTrue(grid.getItem(0).getText(1).startsWith("n1"));
		assertTrue("fills after filter " + fills, fills <= visible * 3);
		final int shown = sort.unmap(keep);
		assertArrayEquals(shown < 0 ? new int[0] : new int[] { shown }, grid.getSelectionIndices());
		assertArrayEquals(new int[] { keep }, binding.getSelectedRows());

		grid.setSelection(new int[] { 0, 1 });
		grid.notifyListeners(SWT.Selection, new Event());
		assertArrayEquals(new int[] { sort.map(0), sort.map(1) }, binding.getSelectedRows());
		numbers[sort.map(0)] = 99;
		sort.rowsChanged(sort.map(0));
		drain();
		assertEquals(2, grid.getSelectionCount());
		sort.rowsInserted(0, 2);
		sort.rowsRemoved(0, 2);
		assertEquals(2, binding.getSelectedRows().length);
		filter.clear(1);
		drain();
		assertEquals(ROWS, grid.getItemCount());

		click(grid.getColumn(0), false);
		click(grid.getColumn(0), false);
		assertEquals(SWT.NONE, grid.getColumn(0).getSort());
		final M3ColumnSort other = new M3ColumnSort(2, ROWS);
		other.setKey(0, M3ColumnKey.ofInts(r -> numbers[r]));
		M3GridSortHeader.install(grid, other);
		click(grid.getColumn(0), false);
		assertEquals(1, other.getSortKeyCount());
		assertEquals(1, sort.getSortKeyCount());
		assertNotSame(sort, other);
		binding.setSelectedRows();
		assertEquals(0, grid.getSelectionCount());
		binding.dispose();
		grid.dispose();
		other.toggle(0, false);
		sort.toggle(1, false);
		try {
			M3GridRowBinding.bind(new Grid(shell, SWT.NONE), sort, (item, row) -> fail());
			fail("non-virtual grid accepted");
		} catch (final IllegalArgumentException expected) {
			/* ERROR_INVALID_ARGUMENT */
		}
		try {
			M3GridSortHeader.install(null, sort);
			fail("null grid accepted");
		} catch (final IllegalArgumentException expected) {
			/* ERROR_NULL_ARGUMENT */
		}
	}
}
