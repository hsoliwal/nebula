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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Leaf G7, one model with deltas: a change of one row's data or height inside the window repaints
 * that row (from that row down for a height), and a change outside the window or on a hidden row
 * repaints nothing, since the rows on screen and the row-based scroll bar do not move. After every
 * change the window equals the Grid's own full paint, in both modes; the paint counts hold in the
 * viewport mode (the original mode repaints the Grid, as stock).
 */
public class GridRowDelta_Test {
	private Display display;
	private Shell shell;
	private Grid grid;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(10, 10, 460, 420);
		// the focus stays here: a focused Grid paints a focus rectangle that is not part of print()
		new Text(shell, SWT.SINGLE).setBounds(5, 390, 60, 22);
		grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 440, 370);
		grid.setHeaderVisible(true);
		for (int c = 0; c < 3; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setWidth(120);
			column.setTree(c == 0);
		}
		for (int r = 0; r < 600; r++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			root.setText(0, "root " + r);
			root.setText(1, "value " + r);
			for (int k = 0; k < 2; k++) {
				new GridItem(root, SWT.NONE).setText(0, "child " + r + "." + k);
			}
			root.setExpanded(r % 2 == 0);
		}
		shell.open();
		flush();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void dataChangesOffScreenPaintNothingAndOnScreenPaintTheRow() {
		final GridItem far = grid.getItem(grid.getItemCount() - 10);
		assertDelta("text off screen", () -> far.setText(1, "changed"), 0, 1);
		assertDelta("background off screen", () -> far.setBackground(1, display.getSystemColor(SWT.COLOR_YELLOW)), 0, 1);
		final GridItem hidden = grid.getRootItem(1).getItem(0);
		assertDelta("text of a hidden row", () -> hidden.setText(1, "hidden changed"), 0, 1);
		final GridItem shown = grid.getItem(4);
		assertDelta("text on screen", () -> shown.setText(1, "visible changed"), 1, 1);
		assertDelta("checked on screen", () -> shown.setBackground(2, display.getSystemColor(SWT.COLOR_CYAN)), 1, 1);
		// the changes are there when their rows come into view
		grid.getRootItem(1).setExpanded(true);
		assertScreenIsPaint("after expanding the hidden row's parent");
		grid.showItem(far);
		assertScreenIsPaint("after showing the far row");
		assertEquals("changed", far.getText(1));
	}

	@Test
	public void heightChangesOffScreenLeaveTheWindowAndTheScrollBarAlone() {
		final ScrollBar bar = grid.getVerticalBar();
		final int selection = bar.getSelection();
		final int maximum = bar.getMaximum();
		final int thumb = bar.getThumb();
		final int bottom = grid.getBottomIndex();
		final GridItem far = grid.getItem(grid.getItemCount() - 10);
		assertDelta("height off screen", () -> far.setHeight(40), 0, 1);
		assertEquals(selection, bar.getSelection());
		assertEquals(maximum, bar.getMaximum());
		assertEquals(thumb, bar.getThumb());
		assertEquals(bottom, grid.getBottomIndex());
		final GridItem hidden = grid.getRootItem(1).getItem(1);
		assertDelta("height of a hidden row", () -> hidden.setHeight(30), 0, 1);
		grid.showItem(far);
		assertScreenIsPaint("after showing the taller far row");
		assertEquals(40, far.getHeight());
		grid.setTopIndex(0);
		flush();
		final GridItem shown = grid.getItem(3);
		assertDelta("height on screen", () -> shown.setHeight(45), 1, 1);
		assertTrue("the bottom row moved up", grid.getBottomIndex() < bottom);
		grid.getRootItem(1).setExpanded(true);
		assertScreenIsPaint("after expanding the taller hidden row's parent");
	}

	/* applies the change; the viewport mode paints the body paintsIfDelta times, the original mode stockPaints */
	private void assertDelta(final String step, final Runnable change, final int paintsIfDelta, final int stockPaints) {
		flush();
		final int before = grid.bodyPaintCount;
		final int deltas = grid.rowDeltaPaints + grid.rowDeltaSkips;
		change.run();
		grid.update();
		flush();
		assertEquals(step + ": body paints", GridViewport.ENABLED ? paintsIfDelta : stockPaints, grid.bodyPaintCount - before);
		if (GridViewport.ENABLED) {
			assertEquals(step + ": handled as a row delta", deltas + 1, grid.rowDeltaPaints + grid.rowDeltaSkips);
		}
		assertScreenIsPaint(step);
	}

	private void assertScreenIsPaint(final String step) {
		flush();
		final ImageData now = GridScrollPaint_Test.screen(grid);
		final ImageData painted = GridScrollPaint_Test.print(grid);
		if (!GridScrollPaint_Test.sameClientArea(grid, now, painted)) {
			GridViewportDifferential_Test.save(now, "rowdelta-now");
			GridViewportDifferential_Test.save(painted, "rowdelta-painted");
			throw new AssertionError(step + ": the window differs from the Grid's paint");
		}
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain paints and timers
		}
	}
}
