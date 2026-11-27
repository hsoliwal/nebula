/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Boundary and ownership contracts for the original Grid's cached column projection. */
public class GridColumnMetricsViewport_Test {

	private Shell shell;
	private Grid grid;

	@Before
	public void setUp() {
		shell = new Shell(Display.getDefault());
		grid = new Grid(shell, SWT.H_SCROLL | SWT.V_SCROLL);
		grid.setBounds(0, 0, 320, 160);
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	@Test
	public void positivePinnedPrefixKeepsExactRightEdgeSemantics() {
		for (int i = 0; i < 2048; i++) {
			column(7, true, true);
		}
		final List<GridColumn> columns = ordered();
		final GridColumnMetrics metrics = ready(columns);
		assertArrayEquals(new GridColumn[0], metrics.visibleColumns(0, 0, 0, true));
		assertArrayEquals(new GridColumn[] { columns.get(0) }, metrics.visibleColumns(0, 0, 1, true));
		assertArrayEquals(new GridColumn[] { columns.get(0) }, metrics.visibleColumns(0, 0, 7, true));
		assertArrayEquals(new GridColumn[] { columns.get(0), columns.get(1) },
				metrics.visibleColumns(0, 0, 8, true));
		assertArrayEquals(new GridColumn[] { columns.get(0), columns.get(1), columns.get(2) },
				metrics.visibleColumns(Integer.MAX_VALUE, 40, 61, true));
		assertArrayEquals(columns.toArray(new GridColumn[0]),
				metrics.visibleColumns(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, true));
	}

	@Test
	public void zeroWidthAndHiddenFixedColumnsKeepRepeatedPrefixOrder() {
		column(0, true, true);
		column(0, true, true);
		final GridColumn first = column(7, true, true);
		column(0, true, true);
		column(80, true, false);
		final GridColumn second = column(9, true, true);
		column(0, true, true);
		final GridColumn third = column(11, true, true);
		column(0, true, true);
		final GridColumnMetrics metrics = ready(ordered());
		assertArrayEquals(new GridColumn[] { first }, metrics.visibleColumns(0, 0, 7, true));
		assertArrayEquals(new GridColumn[] { first, second }, metrics.visibleColumns(0, 0, 16, true));
		assertArrayEquals(new GridColumn[] { first, second, third }, metrics.visibleColumns(0, 0, 17, true));
		assertProjection();
		first.setWidth(0);
		second.setVisible(false);
		third.setWidth(0);
		assertProjection();
		assertEquals(0, ready(ordered()).visibleColumns(0, 0, 40, true).length);
	}

	@Test
	public void wideCoordinatesAndMixedReorderedColumnsMatchTheLinearProjection() {
		final int[] widths = { Integer.MAX_VALUE, 0, 7, 12, 0, Integer.MAX_VALUE, 3, 31, 5, 0, 1, 17 };
		for (int i = 0; i < widths.length; i++) {
			column(widths[i], i % 3 != 1, i % 5 != 4);
		}
		assertProjection();
		final int[] reverse = new int[widths.length];
		for (int i = 0; i < reverse.length; i++) {
			reverse[i] = reverse.length - 1 - i;
		}
		grid.setColumnOrder(reverse);
		assertProjection();
		for (int i = 0; i < widths.length; i++) {
			grid.getColumn(i).setFixed(i % 2 == 0);
			grid.getColumn(i).setVisible(i % 4 != 0);
			grid.getColumn(i).setWidth(i % 3 == 0 ? 0 : 1 + i * 13);
			assertProjection();
		}
	}

	@Test
	public void cachedProjectionDoesNotInvokeSubclassAccessors() {
		final CountingColumn first = new CountingColumn(grid);
		first.setWidth(11);
		first.setFixed(true);
		final CountingColumn second = new CountingColumn(grid);
		second.setWidth(37);
		second.setFixed(true);
		final GridColumnMetrics metrics = ready(ordered());
		final int before = first.calls + second.calls;
		for (int i = 0; i < 20; i++) {
			assertArrayEquals(new GridColumn[] { first }, metrics.visibleColumns(i - 10, 0, 11, true));
		}
		assertEquals("a cached query must not reenter column methods", before, first.calls + second.calls);
	}

	@Test
	public void cachedProjectionUsesCapturedMembershipWhenFixedChangesFromTrueToFalse() {
		assertCapturedPinnedMembership(true, false, 0);
	}

	@Test
	public void cachedProjectionUsesCapturedMembershipWhenFixedChangesFromFalseToTrue() {
		assertCapturedPinnedMembership(false, true, -6);
	}

	private void assertCapturedPinnedMembership(final boolean first, final boolean second,
			final int horizontalSelection) {
		final StatefulFixedColumn column = new StatefulFixedColumn(grid);
		column.setWidth(12);
		final List<GridColumn> columns = ordered();
		final GridColumnMetrics metrics = grid.columnMetrics();
		metrics.invalidate();
		column.begin(first, second);
		try {
			assertTrue(metrics.ready(columns));
			assertEquals("rebuild retains both existing subclass observations", 2, column.calls);
			assertArrayEquals("projection follows the captured pinned membership",
					new GridColumn[] { column }, metrics.visibleColumns(horizontalSelection, 0, 12, true));
			assertEquals("projection adds no subclass observation", 2, column.calls);
		} finally {
			column.end();
		}
	}

	@Test
	public void negativeWidthKeepsTheExistingScalarFallback() {
		grid.setSize(20, 100);
		grid.setRowHeaderVisible(true, 100);
		final GridColumn column = column(40, true, true);
		assertTrue("the row header is wider than the client area", column.getWidth() < 0);
		assertFalse(grid.columnMetrics().ready(ordered()));
		assertEquals(0, grid.getVisibleRange().getColumns().length);
		grid.setSize(320, 160);
		column.setWidth(40);
		assertProjection();
	}

	@Test
	public void returnedArraysRemainIndependentSnapshots() {
		final GridColumn first = column(10, true, true);
		column(20, true, true);
		final GridColumnMetrics metrics = ready(ordered());
		final GridColumn[] a = metrics.visibleColumns(0, 0, 10, true);
		final GridColumn[] b = metrics.visibleColumns(0, 0, 10, true);
		assertNotSame(a, b);
		a[0] = null;
		assertSame(first, b[0]);
		assertSame(first, metrics.visibleColumns(0, 0, 10, true)[0]);
	}

	/*
	 * A new display order repaints the moved columns. Stock Nebula only changed the order, so the
	 * window kept the old one until some other paint (GridViewportDelta_Test, "rotate order", GTK3:
	 * no native paint followed). The same order again requests no paint. The original mode keeps
	 * stock's behaviour.
	 */
	@Test
	public void aNewColumnOrderRepaintsAndTheSameOrderDoesNot() {
		assumeTrue("the original mode keeps stock's setColumnOrder", GridTestUtil.fixesUpstreamDefects());
		final Display display = shell.getDisplay();
		shell.setBounds(10, 10, 420, 260);
		final Text text = new Text(shell, SWT.SINGLE);
		text.setBounds(5, 220, 60, 22);
		// first in the tab order, so Shell.open() gives the focus to the Text and not to the Grid,
		// which repaints on every FocusIn and FocusOut
		text.moveAbove(null);
		grid.setBounds(5, 5, 400, 200);
		grid.setHeaderVisible(true);
		for (int c = 0; c < 4; c++) {
			column(80, false, true).setText("c" + c);
		}
		for (int r = 0; r < 5; r++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 4; c++) {
				item.setText(c, r + ":" + c);
			}
		}
		/*
		 * Under X11 the real focus events come when the window manager activates the shell, at a time
		 * the X server picks, possibly after the first paint (#736). isFocusControl() cannot tell:
		 * SWT GTK answers the window's focus widget before the window has the X focus. So the focus
		 * events are counted from before the open, the Text is given the focus, and the events are
		 * dispatched until it has had its real FocusIn (bounded, logged if it does not come). The
		 * first paint and the paint of any earlier Grid focus change then settle, and a Grid focus
		 * change inside a measured interval fails the test instead of passing or failing its count.
		 */
		final int[] gridFocusChanges = new int[1];
		final boolean[] textFocused = new boolean[1];
		grid.addListener(SWT.FocusIn, e -> gridFocusChanges[0]++);
		grid.addListener(SWT.FocusOut, e -> gridFocusChanges[0]++);
		text.addListener(SWT.FocusIn, e -> textFocused[0] = true);
		text.addListener(SWT.FocusOut, e -> textFocused[0] = false);
		shell.open();
		text.forceFocus();
		final long deadline = System.nanoTime() + 5_000_000_000L;
		while (!textFocused[0] && System.nanoTime() < deadline) {
			if (!display.readAndDispatch()) {
				LockSupport.parkNanos(5_000_000L);
			}
		}
		if (!textFocused[0]) {
			System.out.println("GRID_COLUMN_ORDER_REPAINT the Text had no FocusIn within 5 s");
		}
		GridSwtScreenshotCapture.awaitStableCounter(display, () -> grid.paintPasses, 1, "first Grid paint");
		final int focusChanges = gridFocusChanges[0];
		final int before = grid.paintPasses;
		grid.setColumnOrder(new int[] { 1, 2, 3, 0 });
		assertArrayEquals(new int[] { 1, 2, 3, 0 }, grid.getColumnOrder());
		GridSwtScreenshotCapture.awaitStableCounter(display, () -> grid.paintPasses, before + 1,
				"setColumnOrder left the window on the old column order");
		assertEquals("the Grid's focus changed while the new order's paints were counted", focusChanges,
				gridFocusChanges[0]);
		final int settled = grid.paintPasses;
		grid.setColumnOrder(new int[] { 1, 2, 3, 0 });
		GridSwtScreenshotCapture.awaitStableCounter(display, () -> grid.paintPasses, settled,
				"Grid paints after the same column order");
		assertEquals("the Grid's focus changed while the same order's paints were counted", focusChanges,
				gridFocusChanges[0]);
		assertEquals("the same order paints nothing", settled, grid.paintPasses);
	}

	private GridColumn column(final int width, final boolean fixed, final boolean visible) {
		final GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setWidth(width);
		column.setFixed(fixed);
		column.setVisible(visible);
		return column;
	}

	private List<GridColumn> ordered() {
		final List<GridColumn> columns = new ArrayList<>();
		for (final int index : grid.getColumnOrder()) {
			columns.add(grid.getColumn(index));
		}
		return columns;
	}

	private GridColumnMetrics ready(final List<GridColumn> columns) {
		final GridColumnMetrics metrics = grid.columnMetrics();
		assertTrue(metrics.ready(columns));
		return metrics;
	}

	private void assertProjection() {
		final List<GridColumn> columns = ordered();
		final GridColumnMetrics metrics = ready(columns);
		final int[] selections = { Integer.MIN_VALUE, -1000, -1, 0, 1, 23, 1000, Integer.MAX_VALUE };
		final int[][] ranges = { { 0, 0 }, { 10, 5 }, { 0, 1 }, { 0, 7 }, { 0, 8 }, { 40, 61 },
				{ -50, 50 }, { Integer.MIN_VALUE, Integer.MAX_VALUE },
				{ Integer.MAX_VALUE - 31, Integer.MAX_VALUE }, { Integer.MIN_VALUE, -1 } };
		for (final int selection : selections) {
			for (final int[] range : ranges) {
				for (final boolean overlay : new boolean[] { false, true }) {
					assertArrayEquals("selection " + selection + " range " + Arrays.toString(range)
							+ " overlay " + overlay,
							GridViewportProjection.visibleColumns(columns, selection, range[0], range[1], overlay),
							metrics.visibleColumns(selection, range[0], range[1], overlay));
				}
			}
		}
	}

	private static final class StatefulFixedColumn extends GridColumn {
		private boolean[] observations;
		private int calls;

		StatefulFixedColumn(final Grid parent) {
			super(parent, SWT.NONE);
		}

		void begin(final boolean first, final boolean second) {
			observations = new boolean[] { first, second };
			calls = 0;
		}

		void end() {
			observations = null;
		}

		@Override
		public boolean isFixed() {
			if (observations == null) {
				return super.isFixed();
			}
			if (calls >= observations.length) {
				throw new AssertionError("unexpected subclass observation during cached projection");
			}
			return observations[calls++];
		}
	}

	private static final class CountingColumn extends GridColumn {
		int calls;

		CountingColumn(final Grid parent) {
			super(parent, SWT.NONE);
		}

		@Override
		public int getWidth() {
			calls++;
			return super.getWidth();
		}

		@Override
		public boolean isVisible() {
			calls++;
			return super.isVisible();
		}

		@Override
		public boolean isFixed() {
			calls++;
			return super.isFixed();
		}
	}
}
