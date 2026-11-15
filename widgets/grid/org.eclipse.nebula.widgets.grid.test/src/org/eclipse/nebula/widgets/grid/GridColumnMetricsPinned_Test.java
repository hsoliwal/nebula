/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Exercises pinned-column clipping through the original Grid and column owners. */
public class GridColumnMetricsPinned_Test {

	private Shell shell;
	private Grid grid;

	@Before
	public void setUp() {
		shell = new Shell(Display.getDefault());
		grid = new Grid(shell, SWT.H_SCROLL | SWT.V_SCROLL);
		grid.setSize(480, 240);
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	@Test
	public void zeroWidthHiddenAndBoundaryColumnsKeepTheirIdentityAndOrder() {
		column(0, true);
		GridColumn first = column(10, true);
		GridColumn hidden = column(70, true);
		hidden.setVisible(false);
		column(0, true);
		GridColumn second = column(10, true);
		column(17, false);
		column(20, true);
		GridColumnMetrics metrics = ready();
		assertArrayEquals(new GridColumn[] { first }, metrics.visibleColumns(0, 0, 10, true));
		assertArrayEquals(new GridColumn[] { first, second }, metrics.visibleColumns(0, 0, 11, true));
		checkProjection();
	}

	@Test
	public void changingAndReorderingPinnedColumnsKeepsProjection() {
		for (int i = 0; i < 12; i++) {
			column(i % 4 == 0 ? 0 : 11 + i, i % 3 != 0);
		}
		checkProjection();
		grid.getColumn(1).setWidth(0);
		grid.getColumn(3).setFixed(true);
		grid.getColumn(7).setVisible(false);
		checkProjection();
		grid.setColumnOrder(new int[] { 11, 3, 5, 0, 8, 9, 7, 6, 4, 2, 10, 1 });
		checkProjection();
		grid.getColumn(7).setVisible(true);
		grid.getColumn(1).setWidth(31);
		grid.getColumn(3).setFixed(false);
		checkProjection();
	}

	@Test
	public void columnGroupVisibilityChangesKeepProjection() {
		column(13, true);
		GridColumnGroup group = new GridColumnGroup(grid, SWT.TOGGLE);
		GridColumn detail = new GridColumn(group, SWT.NONE);
		detail.setWidth(31);
		detail.setDetail(true);
		detail.setFixed(true);
		GridColumn summary = new GridColumn(group, SWT.NONE);
		summary.setWidth(7);
		summary.setSummary(true);
		summary.setFixed(true);
		column(23, false);
		column(29, true);
		group.setExpanded(true);
		checkProjection();
		group.setExpanded(false);
		checkProjection();
		summary.setWidth(0);
		checkProjection();
		group.setExpanded(true);
		checkProjection();
	}

	@Test
	public void offscreenPinnedSuffixDoesNotChangeTheVisiblePrefix() {
		for (int i = 0; i < 256; i++) {
			column(10, true);
		}
		GridColumn[] expected = { grid.getColumn(0), grid.getColumn(1), grid.getColumn(2) };
		assertArrayEquals(expected, ready().visibleColumns(Integer.MAX_VALUE, 0, 29, true));
		grid.getColumn(255).setWidth(Integer.MAX_VALUE);
		grid.getColumn(254).setVisible(false);
		assertArrayEquals(expected, ready().visibleColumns(Integer.MIN_VALUE, 0, 29, true));
		checkProjection();
	}

	@Test
	public void zeroWidthPinnedColumnsReturnFreshEmptyProjections() {
		for (int i = 0; i < 64; i++) {
			column(0, true);
		}
		column(37, true).setVisible(false);
		GridColumnMetrics metrics = ready();
		GridColumn[] first = metrics.visibleColumns(0, 0, 300, true);
		GridColumn[] second = metrics.visibleColumns(0, 0, 300, true);
		assertArrayEquals(new GridColumn[0], first);
		assertArrayEquals(new GridColumn[0], second);
		assertNotSame(first, second);
		checkProjection();
	}

	@Test
	public void wideCoordinatesAndColumnWidthsUseTheSameClippingContract() {
		column(Integer.MAX_VALUE, true);
		column(0, true);
		column(Integer.MAX_VALUE, false);
		column(17, true);
		checkProjection();
	}

	private GridColumn column(int width, boolean fixed) {
		GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setWidth(width);
		column.setFixed(fixed);
		return column;
	}

	private List<GridColumn> columns() {
		List<GridColumn> columns = new ArrayList<>();
		for (int index : grid.getColumnOrder()) {
			columns.add(grid.getColumn(index));
		}
		return columns;
	}

	private GridColumnMetrics ready() {
		GridColumnMetrics metrics = grid.columnMetrics();
		assertTrue(metrics.ready(columns()));
		return metrics;
	}

	private void checkProjection() {
		List<GridColumn> columns = columns();
		GridColumnMetrics metrics = ready();
		int[][] windows = { { 0, 0 }, { 10, 5 }, { 0, 1 }, { 0, 10 }, { 0, 11 }, { 0, 20 },
				{ 40, 41 }, { -100, -71 }, { 40, 440 }, { Integer.MIN_VALUE, Integer.MIN_VALUE + 29 },
				{ Integer.MAX_VALUE - 29, Integer.MAX_VALUE }, { Integer.MIN_VALUE, Integer.MAX_VALUE } };
		for (int selection : new int[] { Integer.MIN_VALUE, -40, -1, 0, 13, 400, Integer.MAX_VALUE }) {
			for (boolean overlay : new boolean[] { false, true }) {
				for (int[] window : windows) {
					assertArrayEquals("selection=" + selection + ", window=" + window[0] + ".." + window[1]
							+ ", overlay=" + overlay,
							GridViewportProjection.visibleColumns(columns, selection, window[0], window[1], overlay),
							metrics.visibleColumns(selection, window[0], window[1], overlay));
				}
			}
		}
	}
}
