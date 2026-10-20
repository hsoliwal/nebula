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
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Brief rule 24, viewport deltas (user 2026-10-20: "resize and layout change the port"): a resize
 * and a column resize, hide, show, move or fixed change are deltas of the viewport. The gates:
 * <ul>
 * <li>a sweep over 50 Grid sizes (width only, height only, both; scroll bars appearing and going;
 * scrolled to the right and to the bottom) and a sweep over column changes rebuild no row index
 * ({@link Grid#fullRebuildCount()}) and no column lane ({@link Grid#fullReplanCount()}): full
 * re-plans = 0;</li>
 * <li>after every step the window equals the Grid's own paint, so the bands a resize repaints
 * (and the pixels the system keeps) are exactly right;</li>
 * <li>in the viewport mode a resize that keeps the rows and columns in place repaints only the
 * bands at the moved edges; the test logs the painted share of the client area.</li>
 * </ul>
 */
public class GridViewportDelta_Test {
	private static final Logger LOG = Logger.getLogger("org.eclipse.nebula.widgets.grid");

	private Display display;
	private Shell shell;
	private Grid grid;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(10, 10, 1000, 720);
		// the focus stays here: a focused Grid paints a focus rectangle that is not part of print()
		new Text(shell, SWT.SINGLE).setBounds(5, 690, 60, 22);
		grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 600, 400);
		grid.setHeaderVisible(true);
		grid.setFooterVisible(true);
		grid.setLinesVisible(true);
		for (int c = 0; c < 9; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setFooterText("f" + c);
			column.setWidth(80 + c % 3 * 10);
			column.setMoveable(true);
			column.setFixed(c == 0);
		}
		for (int r = 0; r < 10_000; r++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 9; c++) {
				item.setText(c, r + ":" + c);
			}
		}
		grid.setSelection(new int[] { 1, 3, 4, 8 });
		shell.open();
		flush();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void resizeSweepReplansNothingAndKeepsTheWindowExact() {
		final int rebuilds = grid.fullRebuildCount();
		final int replans = grid.fullReplanCount();
		final int deltasBefore = grid.resizeDeltas;
		final int fullBefore = grid.resizeFullRepaints;
		long painted = 0;
		long areas = 0;
		int width = 600;
		int height = 400;
		for (int step = 0; step < 50; step++) {
			switch (step % 5) {
			case 0:
				width += step % 2 == 0 ? 47 : -61;
				break;
			case 1:
				height += step % 3 == 0 ? -53 : 41;
				break;
			case 2:
				width += 23;
				height -= 17;
				break;
			case 3:
				// past the columns' width: the horizontal scroll bar goes, then comes back
				width = step % 2 == 0 ? 900 : 520;
				break;
			default:
				if (step % 10 == 4) {
					// scrolled to the right and down: a growing size moves the content back
					scroll(grid.getHorizontalBar(), 400);
					scroll(grid.getVerticalBar(), 9_990);
					flush();
				}
				height += step % 2 == 0 ? 90 : -90;
			}
			width = Math.max(150, Math.min(980, width));
			height = Math.max(120, Math.min(680, height));
			final long before = grid.paintedPixels;
			grid.setSize(width, height);
			flush();
			painted += grid.paintedPixels - before;
			areas += (long) grid.getClientArea().width * grid.getClientArea().height;
			assertScreenIsPaint("resize step " + step + " to " + width + "x" + height);
			assertEquals("row index rebuilds after resize step " + step, rebuilds, grid.fullRebuildCount());
			assertEquals("column re-plans after resize step " + step, replans, grid.fullReplanCount());
		}
		final int deltas = grid.resizeDeltas - deltasBefore;
		final int full = grid.resizeFullRepaints - fullBefore;
		LOG.info("GridViewportDelta_Test: 50 sizes, full re-plans 0, row index rebuilds 0, window equal to the paint;"
				+ " resizes as edge bands " + deltas + ", whole repaints " + full + ", painted " + painted * 100 / areas
				+ "% of the client areas");
		if (GridViewport.ENABLED) {
			assertTrue("most resizes repaint only the edge bands", deltas > full);
		}
	}

	@Test
	public void columnSweepReplansNothingAndKeepsTheWindowExact() {
		final int rebuilds = grid.fullRebuildCount();
		final int replans = grid.fullReplanCount();
		final GridColumn[] columns = grid.getColumns();
		int steps = 0;
		for (int round = 0; round < 3; round++) {
			final int pixels = 150 * (round + 1);
			for (int c = 0; c < columns.length; c++) {
				final GridColumn column = columns[c];
				steps += step("width " + c, () -> column.setWidth(column.getWidth() + (column.index % 2 == 0 ? 37 : -29)), rebuilds,
						replans);
				steps += step("hide " + c, () -> column.setVisible(false), rebuilds, replans);
				steps += step("show " + c, () -> column.setVisible(true), rebuilds, replans);
				if (c % 3 == round) {
					steps += step("fixed " + c, () -> column.setFixed(!column.isFixed()), rebuilds, replans);
				}
			}
			// moves: rotate the order by one, then swap the first two
			final int[] order = grid.getColumnOrder();
			final int[] rotated = new int[order.length];
			for (int i = 0; i < order.length; i++) {
				rotated[i] = order[(i + 1) % order.length];
			}
			steps += step("rotate order", () -> grid.setColumnOrder(rotated), rebuilds, replans);
			final int[] swapped = grid.getColumnOrder();
			final int first = swapped[0];
			swapped[0] = swapped[1];
			swapped[1] = first;
			steps += step("swap first two", () -> grid.setColumnOrder(swapped), rebuilds, replans);
			steps += step("scroll right", () -> scroll(grid.getHorizontalBar(), pixels), rebuilds, replans);
		}
		LOG.info("GridViewportDelta_Test: " + steps
				+ " column resizes, hides, shows, fixed changes and moves; full re-plans 0, window equal to the paint");
	}

	private int step(final String what, final Runnable change, final int rebuilds, final int replans) {
		change.run();
		flush();
		assertScreenIsPaint(what);
		assertEquals("row index rebuilds after " + what, rebuilds, grid.fullRebuildCount());
		assertEquals("column re-plans after " + what, replans, grid.fullReplanCount());
		return 1;
	}

	private static void scroll(final ScrollBar bar, final int value) {
		bar.setSelection(value);
		bar.notifyListeners(SWT.Selection, new Event());
	}

	private void assertScreenIsPaint(final String step) {
		flush();
		final ImageData now = GridScrollPaint_Test.screen(grid);
		final ImageData painted = GridScrollPaint_Test.print(grid);
		if (!GridScrollPaint_Test.sameClientArea(grid, now, painted)) {
			GridViewportDifferential_Test.save(now, "viewportdelta-now");
			GridViewportDifferential_Test.save(painted, "viewportdelta-painted");
			throw new AssertionError(step + ": the window differs from the Grid's paint ("
					+ GridViewportDifferential_Test.difference(painted, now) + ")");
		}
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain paints and timers
		}
	}
}
