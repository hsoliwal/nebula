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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Leaf G6: the Grid's column lanes ({@link GridColumnMetrics}) answer every column question as the
 * linear scans they replace, after seeded scripts that add and remove columns, change widths
 * (zero included), visibility, the fixed state and the column order, and expand and collapse column
 * groups with detail and summary columns. The lanes are never told about a change by the test: the
 * Grid's and the columns' own hooks must invalidate them.
 */
public class GridColumnMetrics_Test {
	/** The package's logger (brief rule 22: java.util.logging only). */
	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("org.eclipse.nebula.widgets.grid");
	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(10, 10, 500, 300);
		// the original mode keeps stock's duplicate column indexes after an insertion (UPSTREAM_DEFECTS.tsv),
		// so its column order is no list of distinct columns; the lanes serve the viewport mode
		assumeTrue("the column lanes serve the viewport mode", GridViewport.ENABLED);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void lanesAnswerAsTheLinearScans() {
		int checks = 0;
		int deltas = 0;
		for (int seed = 0; seed < 16; seed++) {
			final Grid grid = new Grid(shell, SWT.H_SCROLL | SWT.V_SCROLL);
			grid.setBounds(0, 0, 480, 280);
			final List<GridColumnGroup> groups = new ArrayList<>();
			final Random random = new Random(0xC0L + seed);
			for (int step = 0; step < 120; step++) {
				final int replans = grid.fullReplanCount();
				final String op = apply(grid, groups, random);
				checks += check(grid, random, "seed " + seed + " step " + step + " (" + op + ")");
				// brief rule 24, viewport deltas: only a column added or removed rebuilds the lanes
				if (!op.startsWith("add") && !op.equals("remove")) {
					assertEquals("full re-plans after " + op + " (seed " + seed + " step " + step + ")", replans,
							grid.fullReplanCount());
					deltas++;
				}
			}
			grid.dispose();
		}
		LOG.info("GridColumnMetrics_Test: " + checks + " answers equal to the linear scans; " + deltas
				+ " width, visibility, fixed, order and group changes applied as deltas (0 full re-plans)");
	}

	private static String apply(final Grid grid, final List<GridColumnGroup> groups, final Random random) {
		final int n = grid.getColumnCount();
		final GridColumn any = n == 0 ? null : grid.getColumn(random.nextInt(n));
		switch (n < 2 ? 0 : random.nextInt(10)) {
		case 0:
		case 1: {
			final GridColumn column = new GridColumn(grid, SWT.NONE, random.nextInt(n + 1));
			column.setWidth(width(random));
			return "add";
		}
		case 2: {
			if (groups.isEmpty()) {
				groups.add(new GridColumnGroup(grid, SWT.TOGGLE));
				return "group";
			}
			final GridColumn column = new GridColumn(groups.get(random.nextInt(groups.size())), SWT.NONE);
			column.setWidth(width(random));
			column.setDetail(random.nextBoolean());
			column.setSummary(random.nextBoolean());
			return "add to group";
		}
		case 3:
			any.dispose();
			return "remove";
		case 4:
			any.setWidth(width(random));
			return "width";
		case 5:
			any.setVisible(random.nextInt(3) != 0);
			return "visible";
		case 6:
			any.setFixed(random.nextInt(3) == 0);
			return "fixed";
		case 7: {
			// shuffle blocks: a column group's columns stay together, as setColumnOrder requires
			final int[] order = grid.getColumnOrder();
			final List<int[]> blocks = new ArrayList<>();
			for (int i = 0; i < order.length;) {
				final GridColumnGroup group = grid.getColumn(order[i]).getColumnGroup();
				int j = i + 1;
				while (group != null && j < order.length && grid.getColumn(order[j]).getColumnGroup() == group) {
					j++;
				}
				blocks.add(java.util.Arrays.copyOfRange(order, i, j));
				i = j;
			}
			// a column inserted between a group's columns splits it: no valid order keeps the blocks
			final java.util.Set<GridColumnGroup> seen = new java.util.HashSet<>();
			for (final int[] block : blocks) {
				final GridColumnGroup group = grid.getColumn(block[0]).getColumnGroup();
				if (group != null && !seen.add(group)) {
					return "order skipped";
				}
			}
			java.util.Collections.shuffle(blocks, random);
			int k = 0;
			for (final int[] block : blocks) {
				for (final int index : block) {
					order[k++] = index;
				}
			}
			grid.setColumnOrder(order);
			return "order";
		}
		case 8:
			if (!groups.isEmpty()) {
				final GridColumnGroup group = groups.get(random.nextInt(groups.size()));
				if (!group.isDisposed()) {
					group.setExpanded(!group.getExpanded());
					return "expand";
				}
			}
			return "none";
		default:
			if (any.getColumnGroup() != null) {
				any.setDetail(random.nextBoolean());
				any.setSummary(random.nextBoolean());
				return "detail/summary";
			}
			any.setMinimumWidth(random.nextInt(40));
			return "minimum width";
		}
	}

	private static int width(final Random random) {
		return random.nextInt(5) == 0 ? 0 : 1 + random.nextInt(150);
	}

	private static int check(final Grid grid, final Random random, final String where) {
		final List<GridColumn> columns = new ArrayList<>();
		for (final int index : grid.getColumnOrder()) {
			columns.add(grid.getColumn(index));
		}
		final GridColumnMetrics metrics = grid.columnMetrics();
		assertTrue(where + ": no negative width", metrics.ready(columns));
		int total = 0;
		for (final GridColumn column : columns) {
			total += column.isVisible() ? column.getWidth() : 0;
		}
		int checks = 0;
		int deltas = 0;
		for (final GridColumn column : columns) {
			assertEquals(where, columns.indexOf(column), metrics.positionOf(column));
		}
		assertEquals(where, oracleFixedRun(columns), metrics.fixedRunWidth());
		final int[] selections = { 0, 13, total / 2, total + 5, -4 };
		for (final int hsel : selections) {
			assertEquals(where + " hsel " + hsel, GridViewportProjection.startColumnIndex(columns, hsel),
					metrics.startColumnIndex(hsel));
			for (final boolean rowHeader : new boolean[] { false, true }) {
				final int rowHeaderWidth = rowHeader ? 40 : 0;
				for (final GridColumn column : columns) {
					assertEquals(where + " header x", oracleHeaderX(columns, column, hsel, rowHeader, rowHeaderWidth),
							metrics.headerX(column, hsel, rowHeader, rowHeaderWidth));
					assertEquals(where + " origin x", oracleOriginX(columns, column, hsel, rowHeader, rowHeaderWidth),
							metrics.originX(column, hsel, rowHeader, rowHeaderWidth));
					checks += 2;
				}
				assertEquals(where + " origin x of null", oracleOriginX(columns, null, hsel, rowHeader, rowHeaderWidth),
						metrics.originX(null, hsel, rowHeader, rowHeaderWidth));
				for (final boolean overlay : new boolean[] { false, true }) {
					for (final int x : probes(columns, hsel, rowHeaderWidth, random)) {
						assertSame(where + " column at " + x + " hsel " + hsel + " overlay " + overlay,
								GridViewportProjection.columnAt(columns, x, rowHeader, rowHeaderWidth, overlay, hsel),
								metrics.columnAt(x, rowHeader, rowHeaderWidth, overlay, hsel));
						checks++;
					}
					for (final int[] range : new int[][] { { 0, 400 }, { 40, 400 }, { 0, 0 }, { 10, 5 }, { 40, 41 } }) {
						assertArrayEquals(where + " visible columns " + range[0] + ".." + range[1],
								GridViewportProjection.visibleColumns(columns, hsel, range[0], range[1], overlay),
								metrics.visibleColumns(hsel, range[0], range[1], overlay));
						checks++;
					}
				}
			}
			final int start = metrics.startColumnIndex(hsel);
			for (final int from : new int[] { 0, start, columns.size(), Math.max(0, start - 1) }) {
				for (final int x : new int[] { -hsel, -hsel + 40, 3, 600 }) {
					for (final int clientWidth : new int[] { 0, 1, 200, 480, 5000 }) {
						assertEquals(where + " end column " + from + " " + x + " " + clientWidth,
								GridViewportProjection.endColumnIndex(columns, from, x, clientWidth),
								metrics.endColumnIndex(from, x, clientWidth));
						checks++;
					}
				}
			}
		}
		return checks;
	}

	/* every column edge and its neighbours, in both planes, and some random points */
	private static int[] probes(final List<GridColumn> columns, final int hsel, final int rowHeaderWidth,
			final Random random) {
		final List<Integer> xs = new ArrayList<>();
		int scrolled = rowHeaderWidth - hsel;
		int fixed = rowHeaderWidth;
		for (final GridColumn column : columns) {
			for (int d = -1; d <= 1; d++) {
				xs.add(java.lang.Integer.valueOf(scrolled + d));
				xs.add(java.lang.Integer.valueOf(fixed + d));
			}
			if (column.isVisible()) {
				scrolled += column.getWidth();
				if (column.isFixed()) {
					fixed += column.getWidth();
				}
			}
		}
		for (int d = -1; d <= 1; d++) {
			xs.add(java.lang.Integer.valueOf(scrolled + d));
			xs.add(java.lang.Integer.valueOf(fixed + d));
			xs.add(java.lang.Integer.valueOf(rowHeaderWidth + d));
		}
		for (int i = 0; i < 10; i++) {
			xs.add(java.lang.Integer.valueOf(random.nextInt(scrolled + hsel + 50) - 20));
		}
		final int[] result = new int[xs.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = (xs.get(i)).intValue();
		}
		return result;
	}

	/* Grid.getColumnHeaderXPosition's loop */
	private static int oracleHeaderX(final List<GridColumn> columns, final GridColumn column, final int hsel,
			final boolean rowHeader, final int rowHeaderWidth) {
		if (!column.isVisible()) {
			return -1;
		}
		int x = 0;
		if (!column.isFixed()) {
			x -= hsel;
		}
		if (rowHeader) {
			x += rowHeaderWidth;
		}
		for (final GridColumn other : columns) {
			if (!other.isVisible()) {
				continue;
			}
			if (other == column) {
				break;
			}
			if (column.isFixed() && !other.isFixed()) {
				continue;
			}
			x += other.getWidth();
		}
		return x;
	}

	/* Grid.getOrigin's column loop */
	private static int oracleOriginX(final List<GridColumn> columns, final GridColumn column, final int hsel,
			final boolean rowHeader, final int rowHeaderWidth) {
		int x = rowHeader ? rowHeaderWidth : 0;
		if (column == null || !column.isFixed()) {
			x -= hsel;
		}
		for (final GridColumn other : columns) {
			if (other == column) {
				break;
			}
			if (other.isVisible()) {
				if (column != null && column.isFixed() && !other.isFixed()) {
					continue;
				}
				x += other.getWidth();
			}
		}
		return x;
	}

	/* Grid.getFixedColumnsWidth's loop */
	private static int oracleFixedRun(final List<GridColumn> columns) {
		int width = 0;
		for (final GridColumn column : columns) {
			if (!column.isVisible()) {
				continue;
			}
			if (!column.isFixed()) {
				break;
			}
			width += column.getWidth();
		}
		return width;
	}
}
