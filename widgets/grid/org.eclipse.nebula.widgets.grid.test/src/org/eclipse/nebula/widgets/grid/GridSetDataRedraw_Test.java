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
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A SetData callback fired by paint fills the item being painted; its text, colour and check
 * setters must not invalidate the Grid again. Proof: the first paint pass (which fires SetData)
 * prints (capture mode CONTROL_PRINT) exactly what the settled second pass prints, and the
 * guarded Grid needs fewer paints per scroll frame.
 */
public class GridSetDataRedraw_Test {

	private Display display;
	private Shell shell;
	private Grid grid;
	private Color tint;
	private int paints;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		new Text(shell, SWT.SINGLE).setBounds(0, 0, 40, 20);
		tint = new Color(display, 230, 240, 255);
		grid = new Grid(shell, SWT.VIRTUAL | SWT.MULTI | SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 30, 640, 400);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		for (int c = 0; c < 12; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(70);
		}
		grid.addListener(SWT.SetData, e -> {
			final GridItem item = (GridItem) e.item;
			for (int c = 0; c < 12; c++) {
				item.setText(c, "r" + e.index + "c" + c);
				if ((e.index + c) % 7 == 0) {
					item.setBackground(c, tint);
				}
				if ((e.index + c) % 5 == 0) {
					item.setForeground(c, display.getSystemColor(SWT.COLOR_DARK_RED));
				}
			}
			item.setChecked(e.index % 3 == 0);
			item.setGrayed(e.index % 6 == 0);
		});
		grid.addListener(SWT.Paint, e -> paints++);
		grid.setItemCount(10_000);
		shell.setSize(700, 480);
		shell.open();
		flush();
	}

	@After
	public void tearDown() {
		shell.dispose();
		tint.dispose();
	}

	@Test
	public void firstPassEqualsSettledPass() throws Exception {
		for (final int top : new int[] { 0, 17, 500, 9_980 }) {
			grid.setTopIndex(top);
			grid.clearAll(false);
			final String firstPass = GridRowPermutation_Test.pixels(grid);
			final String settled = GridRowPermutation_Test.pixels(grid);
			assertEquals("top=" + top, settled, firstPass);
		}
	}

	@Test
	public void guardedGridPaintsLessPerScrollFrame() {
		final int guarded = paintsForScroll(true, 0);
		final int unguarded = paintsForScroll(false, 3000);
		assertTrue("guarded " + guarded + " < unguarded " + unguarded, guarded < unguarded);
	}

	private int paintsForScroll(final boolean guard, final int base) {
		grid.setDataRedrawGuard = guard;
		flush();
		paints = 0;
		for (int i = 1; i <= 20; i++) {
			grid.setTopIndex(base + i * 25);
			grid.update();
			flush();
		}
		return paints;
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain
		}
	}
}
