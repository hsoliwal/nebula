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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
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
		return grid(shell);
	}

	private Grid grid(final Composite parent) {
		final Grid grid = new Grid(parent, SWT.VIRTUAL | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
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

	/*
	 * Brief rule 24, hidden viewports: while the grid is hidden, 10^4 model deltas update the view's lanes but the
	 * grid is not touched; showing it redoes it once, and its rows equal the view.
	 */
	@Test
	public void hiddenGridIsRedoneOnceOnShow() {
		final Grid grid = grid();
		final M3ColumnFilter filter = new M3ColumnFilter(2, ROWS, this::cell);
		filter.setText(1, "n1", M3TextMatch.CONTAINS);
		final M3ColumnSort sort = new M3ColumnSort(2, filter);
		sort.setKey(0, M3ColumnKey.ofInts(r -> numbers[r]));
		sort.setSortKeys(new int[] { 0 }, new int[] { SWT.UP });
		final M3GridRowBinding binding = M3GridRowBinding.bind(grid, sort, (item, row) -> {
			item.setText(0, cell(row, 0));
			item.setText(1, cell(row, 1));
		});
		shell.open();
		drain();
		final int refreshes = binding.refreshes;
		grid.setVisible(false);
		final Random random = new Random(24);
		for (int k = 0; k < 10_000; k++) {
			final int row = random.nextInt(ROWS);
			numbers[row] = random.nextInt(50);
			names[row] = "n" + random.nextInt(1000);
			sort.rowsChanged(row);
		}
		drain();
		assertEquals("grid refreshes while hidden", refreshes, binding.refreshes);
		grid.setVisible(true);
		assertEquals("grid refreshes on show", refreshes + 1, binding.refreshes);
		drain();
		assertEquals(sort.size(), grid.getItemCount());
		for (int i = 0; i < Math.min(40, sort.size()); i++) {
			assertEquals("row " + i, cell(sort.map(i), 1), grid.getItem(i).getText(1));
		}
	}

	private Composite parent(final Composite owner) {
		final Composite parent = new Composite(owner, SWT.NONE);
		parent.setLayout(new FillLayout());
		return parent;
	}

	@Test
	public void reparentedGridBatchesHiddenChangesAndKeepsSourceSelection() {
		final Composite oldParent = parent(shell);
		final Composite newParent = parent(shell);
		newParent.setVisible(false);
		final Grid grid = grid(oldParent);
		final M3ColumnSort view = new M3ColumnSort(1, 6);
		final M3GridRowBinding binding = M3GridRowBinding.bind(grid, view,
				(item, row) -> item.setText(0, "source " + row));
		shell.open();
		drain();
		binding.setSelectedRows(2);
		final int refreshes = binding.refreshes;

		assertTrue("GTK must support reparenting this grid", grid.setParent(newParent));
		assertFalse(grid.isVisible());
		view.rowsInserted(0, 2);
		view.rowsRemoved(0, 1);
		view.rowsChanged(0);
		assertEquals("reparented hidden grid is untouched", refreshes, binding.refreshes);
		assertEquals(6, grid.getItemCount());
		assertArrayEquals(new int[] { 3 }, binding.getSelectedRows());
		oldParent.setVisible(false);
		oldParent.setVisible(true);
		oldParent.dispose();
		assertEquals("former ancestors cannot flush the grid", refreshes, binding.refreshes);

		newParent.setVisible(true);
		assertEquals("new ancestor flushes the batch once", refreshes + 1, binding.refreshes);
		assertEquals(7, grid.getItemCount());
		assertArrayEquals(new int[] { 3 }, grid.getSelectionIndices());
		assertEquals("source 3", grid.getItem(3).getText(0));
		drain();
		assertEquals(refreshes + 1, binding.refreshes);

		/* A model delta can reveal the new ancestry before the next native Paint arrives. */
		newParent.setVisible(false);
		view.rowsInserted(0, 2);
		final Composite visibleParent = parent(shell);
		assertTrue(grid.setParent(visibleParent));
		view.rowsRemoved(0, 1);
		assertEquals("pending batch and current delta share one refresh", refreshes + 2, binding.refreshes);
		assertEquals(8, grid.getItemCount());
		assertArrayEquals(new int[] { 4 }, binding.getSelectedRows());
		assertArrayEquals(new int[] { 4 }, grid.getSelectionIndices());
		newParent.setVisible(true);
		assertEquals("former parent's Show is ignored", refreshes + 2, binding.refreshes);
	}

	@Test
	public void ancestorReparentingUsesNewVisibilityChain() {
		final Composite oldParent = parent(shell);
		final Composite newParent = parent(shell);
		newParent.setVisible(false);
		final Composite branch = parent(oldParent);
		final Grid grid = grid(branch);
		final M3ColumnSort view = new M3ColumnSort(1, 4);
		final M3GridRowBinding binding = M3GridRowBinding.bind(grid, view,
				(item, row) -> item.setText(0, "source " + row));
		shell.open();
		drain();
		final int refreshes = binding.refreshes;

		assertTrue("GTK must support reparenting this ancestor", branch.setParent(newParent));
		view.rowsRemoved(0, 1);
		assertEquals("moved ancestor hides the grid", refreshes, binding.refreshes);
		oldParent.setVisible(false);
		oldParent.setVisible(true);
		assertEquals("old ancestry is irrelevant", refreshes, binding.refreshes);
		newParent.setVisible(true);
		assertEquals(refreshes + 1, binding.refreshes);
		assertEquals(view.size(), grid.getItemCount());

		newParent.setVisible(false);
		view.rowsRemoved(0, 1);
		shell.setVisible(false);
		newParent.setVisible(true);
		assertEquals("another hidden ancestor still blocks the flush", refreshes + 1, binding.refreshes);
		shell.setVisible(true);
		assertEquals(refreshes + 2, binding.refreshes);
		assertEquals(view.size(), grid.getItemCount());
	}

	@Test
	public void hiddenDirtyReparentRefreshesBeforeNativeGridPaint() {
		final Composite oldParent = parent(shell);
		final Composite newParent = parent(shell);
		final Grid grid = grid(oldParent);
		final M3ColumnSort view = new M3ColumnSort(1, 4);
		final int[] revision = { 0 };
		final M3GridRowBinding binding = M3GridRowBinding.bind(grid, view,
				(item, row) -> item.setText(0, "revision " + revision[0] + " source " + row));
		shell.open();
		drain();
		assertEquals("revision 0 source 0", grid.getItem(0).getText(0));
		oldParent.setVisible(false);
		revision[0]++;
		view.rowsRemoved(0, 1);
		final int refreshes = binding.refreshes;
		final int[] shows = { 0 }, paints = { 0 };
		grid.addListener(SWT.Show, event -> shows[0]++);
		/* A display filter observes the real event before Grid's existing paint listener. */
		final Listener beforeGridPaint = event -> {
			if (event.widget == grid) {
				assertTrue("native paint supplies a GC", event.gc != null);
				assertEquals("dirty count is current before Grid paints", view.size(), grid.getItemCount());
				assertEquals("one full refresh before Grid paints", refreshes + 1, binding.refreshes);
				paints[0]++;
			}
		};
		display.addFilter(SWT.Paint, beforeGridPaint);
		try {
			assertTrue("GTK must support reparenting this grid", grid.setParent(newParent));
			assertTrue(grid.isVisible());
			assertEquals("setParent did not send Show", 0, shows[0]);
			newParent.layout(true, true);
			grid.redraw();
			grid.update();
			final long deadline = System.nanoTime() + 2_000_000_000L;
			while (paints[0] == 0 && System.nanoTime() < deadline) {
				display.readAndDispatch();
			}
			assertTrue("a native paint must actually be dispatched", paints[0] > 0);
			assertEquals("revision 1 source 0", grid.getItem(0).getText(0));
			assertEquals(refreshes + 1, binding.refreshes);
		} finally {
			display.removeFilter(SWT.Paint, beforeGridPaint);
		}
	}

	@Test
	public void visibilityEventsUseRequestedStateAndStopAtTheFirstShell() {
		final Composite parent = parent(shell);
		final Grid grid = grid(parent);
		final M3ColumnSort view = new M3ColumnSort(1, 4);
		final M3GridRowBinding binding = M3GridRowBinding.bind(grid, view, (item, row) -> { });
		shell.open();
		drain();
		final int refreshes = binding.refreshes;
		parent.addListener(SWT.Hide, event -> {
			view.rowsChanged(0);
			assertEquals("Hide is effective before the widget flag changes", refreshes, binding.refreshes);
		});
		parent.addListener(SWT.Show, event -> {
			assertEquals("pending work is flushed before Show listeners", refreshes + 1, binding.refreshes);
			view.rowsChanged(0);
			assertEquals("Show is effective before the widget flag changes", refreshes + 2, binding.refreshes);
		});
		parent.setVisible(false);
		parent.setVisible(true);
		assertEquals(refreshes + 2, binding.refreshes);

		final Shell childShell = new Shell(shell);
		try {
			childShell.setLayout(new FillLayout());
			childShell.setSize(200, 150);
			final Grid childGrid = grid(childShell);
			final M3ColumnSort childView = new M3ColumnSort(1, 4);
			final M3GridRowBinding childBinding = M3GridRowBinding.bind(childGrid, childView, (item, row) -> { });
			childShell.open();
			drain();
			final int childRefreshes = childBinding.refreshes;
			shell.setVisible(false);
			assertTrue("owned shell remains independently visible", childGrid.isVisible());
			childView.rowsChanged(0);
			assertEquals("owner beyond the first shell is irrelevant", childRefreshes + 1, childBinding.refreshes);
		} finally {
			childShell.dispose();
		}
	}

	@Test
	public void bindingAndGridDisposalReleaseVisibilityAndPaintFilters() throws ReflectiveOperationException {
		for (final boolean disposeGrid : new boolean[] { false, true }) {
			final int[] before = filterCounts();
			final Composite parent = parent(shell);
			final Grid grid = grid(parent);
			final M3ColumnSort view = new M3ColumnSort(1, 4);
			final M3GridRowBinding binding = M3GridRowBinding.bind(grid, view, (item, row) -> { });
			shell.open();
			drain();
			parent.setVisible(false);
			view.rowsRemoved(0, 1);
			final int refreshes = binding.refreshes;
			if (disposeGrid) {
				grid.dispose();
			} else {
				binding.dispose();
			}
			assertArrayEquals("all display filters are released", before, filterCounts());
			parent.setVisible(true);
			view.rowsChanged(0);
			drain();
			assertEquals("disposed binding does no further work", refreshes, binding.refreshes);
			binding.dispose();
			parent.dispose();
		}
	}

	/* Count retained display callbacks without adding a production test hook. */
	private int[] filterCounts() throws ReflectiveOperationException {
		final int[] counts = new int[3];
		final Field tableField = Display.class.getDeclaredField("filterTable");
		tableField.setAccessible(true);
		final Object table = tableField.get(display);
		if (table != null) {
			final Field typesField = table.getClass().getDeclaredField("types");
			typesField.setAccessible(true);
			for (final int type : (int[]) typesField.get(table)) {
				if (type == SWT.Show) counts[0]++;
				if (type == SWT.Hide) counts[1]++;
				if (type == SWT.Paint) counts[2]++;
			}
		}
		return counts;
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
		assertEquals(0, sort.getSortKeyCount());
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
