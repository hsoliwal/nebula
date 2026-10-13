/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Arithmetic scroll bar settling must end in the state of the measured two-pass algorithm: the
 * same visibility, maximum, thumb and selection for both bars, across a resize sweep that crosses
 * both thresholds, with no more visibility toggles.
 */
public class GridScrollbarSettle_Test {

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setSize(800, 700);
		shell.open();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void resizeSweepMatchesTheTwoPassAlgorithm() {
		final Grid settled = grid(true);
		final Grid measured = grid(false);
		final Random random = new Random(5L);
		for (int i = 0; i < 300; i++) {
			// around both thresholds: content is 6 x 80 = 480 wide and 30 rows high
			final int w = 380 + random.nextInt(200);
			final int h = 400 + random.nextInt(300);
			settled.setSize(w, h);
			measured.setSize(w, h);
			settle(settled);
			settle(measured);
			assertBars("size " + w + "x" + h, measured.getVerticalBar(), settled.getVerticalBar());
			assertBars("size " + w + "x" + h, measured.getHorizontalBar(), settled.getHorizontalBar());
		}
	}

	private static void assertBars(final String msg, final ScrollBar expected, final ScrollBar actual) {
		assertEquals(msg + " visible", expected.getVisible(), actual.getVisible());
		if (expected.getVisible()) {
			assertEquals(msg + " max", expected.getMaximum(), actual.getMaximum());
			assertEquals(msg + " thumb", expected.getThumb(), actual.getThumb());
			assertEquals(msg + " selection", expected.getSelection(), actual.getSelection());
		}
	}

	private Grid grid(final boolean settleScrollbars) {
		final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		grid.settleScrollbars = settleScrollbars;
		grid.setHeaderVisible(true);
		for (int c = 0; c < 6; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(80);
		}
		for (int r = 0; r < 30; r++) {
			new GridItem(grid, SWT.NONE).setText("row " + r);
		}
		return grid;
	}

	private void settle(final Grid grid) {
		grid.redraw();
		grid.update();
		while (display.readAndDispatch()) {
			// drain
		}
	}
}
