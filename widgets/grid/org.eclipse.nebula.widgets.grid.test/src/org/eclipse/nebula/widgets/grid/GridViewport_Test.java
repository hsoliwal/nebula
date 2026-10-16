/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
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
	}

	@After
	public void tearDown() {
		if (shell != null) {
			shell.dispose();
		}
	}

	private Grid millionRowTree() {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
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

	/* median nanoseconds of a scroll to each root, a top-row read and a synchronous paint */
	private long medianScrollStep(final Grid grid, final int firstRoot) {
		final long[] times = new long[21];
		for (int i = 0; i < times.length; i++) {
			final int index = (firstRoot + i) * ROW;
			final long start = System.nanoTime();
			grid.setTopIndex(index);
			final int top = grid.getTopIndex();
			grid.redraw();
			grid.update();
			times[i] = System.nanoTime() - start;
			assertEquals(index, top);
		}
		Arrays.sort(times);
		return times[times.length / 2];
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
		final long top = medianScrollStep(grid, 10);
		final long bottom = medianScrollStep(grid, ROOTS - 40);
		System.out.println("GridViewport_Test: " + ROOTS * ROW + " items built in " + buildMillis + " ms; median scroll+paint step "
				+ top / 1000 + " us near the top, " + bottom / 1000 + " us near the bottom; thumb " + thumb);
		assertTrue("scroll step near the bottom (" + bottom + " ns) must not grow with the position (top " + top + " ns)",
				bottom <= 2 * top + 1_000_000);

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
		final Function<int[], Object> stock = driver(stockBuild, 5);
		final Function<int[], Object> viewport = driver(getClass().getClassLoader(), 330);
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
		final int[] pixels = { GridDifferentialDriver.PIXELS, 0, 0, 0 };
		final ImageData expected = (ImageData) stock.apply(pixels);
		final ImageData actual = (ImageData) viewport.apply(pixels);
		assertTrue("printed pixels of the distant rows", Arrays.equals(expected.data, actual.data));
	}

	@SuppressWarnings("unchecked")
	private Function<int[], Object> driver(final ClassLoader build, final int x) throws Exception {
		return (Function<int[], Object>) build.loadClass(GridDifferentialDriver.class.getName())
				.getConstructor(Composite.class, Integer.class, Integer.class)
				.newInstance(shell, Integer.valueOf(x), Integer.valueOf(GridDifferentialDriver.LARGE));
	}
}
