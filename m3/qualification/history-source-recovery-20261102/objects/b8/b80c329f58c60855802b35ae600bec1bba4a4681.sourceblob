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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Acceptance of the Grid viewport (docs/m3/GRID_MEDIATED_VIEWPORT.md): a 1,000,000-row tree scrolls
 * and paints in time independent of its position, its geometry answers match the row arithmetic,
 * and a large tree paints the same pixels as the stock Grid. Skipped when the viewport is not
 * active.
 */
public class GridViewport_Test {

	/* 10,000 roots with 99 children each: 1,000,000 items (-Dgrid.acceptance.roots for a smaller run) */
	private static final int ROOTS = Integer.getInteger("grid.acceptance.roots", 10_000).intValue();
	private static final int CHILDREN = 99, ROW = CHILDREN + 1;

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		assumeTrue("Grid viewport mediation is not active (swt.widget.mode.Grid)", GridViewport.ENABLED);
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(10, 10, 660, 420);
		// takes the focus when the shell opens, so no Grid starts with a focus item the other lacks
		new Text(shell, SWT.SINGLE).setBounds(5, 390, 60, 22);
	}

	@After
	public void tearDown() {
		if (shell != null) {
			shell.dispose();
		}
	}

	private Grid millionRowTree() {
		// data kept in the items: the row-addressed default visualizer re-maps every stored row per
		// insert, which makes building a large tree quadratic (see GRID_MEDIATED_VIEWPORT.md)
		final Grid grid = new Grid(new GridItemDataVisualizer(display.getSystemColor(SWT.COLOR_WHITE),
				display.getSystemColor(SWT.COLOR_BLACK), null), shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 320, 380);
		grid.setHeaderVisible(true);
		final GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setTree(true);
		column.setWidth(200);
		for (int r = 0; r < ROOTS; r++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			root.setText("r" + r);
			for (int c = 0; c < CHILDREN; c++) {
				new GridItem(root, SWT.NONE).setText("r" + r + "/" + c);
			}
		}
		return grid;
	}

	private void settle() {
		while (display.readAndDispatch()) {
			// drain
		}
	}

	/*
	 * median nanoseconds of the row geometry of a scroll to each root (setTopIndex and a top-row
	 * read) in [0], and of the synchronous paint that follows in [1]
	 */
	private long[] medianScrollStep(final Grid grid, final int firstRoot) {
		final long[] geometry = new long[21], paint = new long[21];
		for (int i = 0; i < geometry.length; i++) {
			final int index = (firstRoot + i) * ROW;
			final long start = System.nanoTime();
			grid.setTopIndex(index);
			final int top = grid.getTopIndex();
			final long scrolled = System.nanoTime();
			grid.redraw();
			grid.update();
			paint[i] = System.nanoTime() - scrolled;
			geometry[i] = scrolled - start;
			assertEquals(index, top);
		}
		Arrays.sort(geometry);
		Arrays.sort(paint);
		return new long[] { geometry[geometry.length / 2], paint[paint.length / 2] };
	}

	@Test
	public void millionRowTreeScrollsAndPaintsInPositionIndependentTime() {
		final long built = System.nanoTime();
		final Grid grid = millionRowTree();
		final long buildMillis = (System.nanoTime() - built) / 1_000_000;
		shell.open();
		settle();
		assertEquals(ROOTS * ROW, grid.getItemCount());
		// the bar counts visible rows: the collapsed roots
		final int thumb = grid.getVerticalBar().getThumb();
		assertEquals(ROOTS, grid.getVerticalBar().getMaximum());
		System.gc();
		final long[] top = medianScrollStep(grid, 10);
		final long[] bottom = medianScrollStep(grid, ROOTS - 40);
		System.out.println("GridViewport_Test: " + ROOTS * ROW + " items built in " + buildMillis
				+ " ms; median scroll step near the top: geometry " + top[0] / 1000 + " us, paint " + top[1] / 1000
				+ " us; near the bottom: geometry " + bottom[0] / 1000 + " us, paint " + bottom[1] / 1000 + " us; thumb "
				+ thumb);
		assertTrue("row geometry near the bottom (" + bottom[0] + " ns) must not grow with the position (top " + top[0]
				+ " ns)", bottom[0] <= 2 * top[0] + 500_000);
		assertTrue("paint near the bottom (" + bottom[1] + " ns) must not grow with the position (top " + top[1] + " ns)",
				bottom[1] <= 2 * top[1] + 2_000_000);

		// expanding a distant root adds its rows to the bar and keeps the geometry exact
		final GridItem root = grid.getItem((ROOTS - 2) * ROW);
		root.setExpanded(true);
		settle();
		assertEquals(ROOTS + CHILDREN, grid.getVerticalBar().getMaximum());
		grid.setTopIndex((ROOTS - 2) * ROW + 50);
		assertEquals((ROOTS - 2) * ROW + 50, grid.getTopIndex());
		assertSame(grid.getItem((ROOTS - 2) * ROW + 51), grid.getNextVisibleItem(grid.getItem((ROOTS - 2) * ROW + 50)));
		assertSame(grid.getItem((ROOTS - 1) * ROW), grid.getNextVisibleItem(grid.getItem((ROOTS - 2) * ROW + CHILDREN)));
		assertSame(grid.getItem((ROOTS - 3) * ROW), grid.getPreviousVisibleItem(root));
		final int rowHeight = grid.getItemHeight() + 1;
		final Point inThirdRow = new Point(20, grid.getHeaderHeight() + 2 * rowHeight + 1);
		assertSame(grid.getItem((ROOTS - 2) * ROW + 52), grid.getItem(inThirdRow));
		assertEquals(grid.getHeaderHeight() + 3 * rowHeight, grid.getItem((ROOTS - 2) * ROW + 53).getBounds(0).y);
	}

	@Test
	public void largeTreePaintsLikeTheStockGrid() throws Exception {
		final ClassLoader stockBuild = GridViewportDifferential_Test.gridBuild(
				Grid.class.getProtectionDomain().getCodeSource().getLocation(), "original");
		// both Grids at the same place in their own shell: Grid reads the real cursor position
		final Shell stockShell = new Shell(display);
		stockShell.setBounds(shell.getBounds());
		new Text(stockShell, SWT.SINGLE).setBounds(5, 390, 60, 22);
		try {
			largeTreeScript(GridViewportDifferential_Test.driver(stockBuild, stockShell, GridDifferentialDriver.LARGE),
					GridViewportDifferential_Test.driver(GridViewportDifferential_Test.gridBuild(
							Grid.class.getProtectionDomain().getCodeSource().getLocation(), "viewport"), shell,
							GridDifferentialDriver.LARGE),
					stockShell);
		} finally {
			stockShell.dispose();
		}
	}

	private void largeTreeScript(final Function<int[], Object> stock, final Function<int[], Object> viewport,
			final Shell stockShell) {
		stockShell.open();
		shell.open();
		settle();
		final int[][] script = { { 0, 99_900, 0, 0 }, { 0, 99_500, 0, 0 }, { 15, 99_903, 0, 21 }, { 2, 99_480, 0, 0 },
				{ 3, 99_950, 0, 0 }, { 4, 99_904, 0, 0 }, { 20, 0, 1, 0 }, { 21, 0, 3, 1 } };
		for (final int[] op : script) {
			assertEquals(Arrays.toString(op), stock.apply(op), viewport.apply(op));
			settle();
			final int[] state = { GridDifferentialDriver.STATE, 0, 0, 0 };
			assertEquals(Arrays.toString(op), stock.apply(state), viewport.apply(state));
		}
		settle();
		final int[] pixels = { GridDifferentialDriver.PIXELS, 0, 0, 0 };
		final ImageData expected = (ImageData) stock.apply(pixels);
		final ImageData actual = (ImageData) viewport.apply(pixels);
		if (!Arrays.equals(expected.data, actual.data)) {
			GridViewportDifferential_Test.save(expected, "large-stock");
			GridViewportDifferential_Test.save(actual, "large-viewport");
			fail("printed pixels of the distant rows differ: " + GridViewportDifferential_Test.difference(expected, actual));
		}
	}

}
