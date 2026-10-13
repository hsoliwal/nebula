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

import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Viewport-only redraw suppression: bulk operations paint the body once, a thumb drag moves the
 * scroll bar at once and paints the body at most once per frame plus once at the end.
 */
public class GridBodyDefer_Test {

	private static final int ROWS = 50_000;

	private Display display;
	private Shell shell;
	private Grid grid;
	private RowPermutation permutation;
	private int[] keys;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		new Text(shell, SWT.SINGLE).setBounds(0, 0, 40, 20);
		grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 30, 600, 400);
		grid.setHeaderVisible(true);
		for (int c = 0; c < 5; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(110);
		}
		keys = new int[ROWS];
		for (int m = 0; m < ROWS; m++) {
			keys[m] = (m * 7919) % 1000;
		}
		permutation = new RowPermutation(ROWS);
		grid.addListener(SWT.SetData, e -> {
			final GridItem item = (GridItem) e.item;
			final int m = permutation.modelRow(e.index);
			for (int c = 0; c < 5; c++) {
				item.setText(c, "m" + m + "/" + keys[m]);
			}
		});
		grid.setItemCount(ROWS);
		grid.setRowPermutation(permutation);
		shell.setSize(660, 480);
		shell.open();
		flush();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void sortFilterAndCountChangesPaintTheBodyOnce() {
		assertBodyPaints(1, () -> {
			permutation.sortByIntKeys(keys, true);
			grid.refreshRowPermutation();
		});
		assertBodyPaints(1, () -> {
			permutation.setFilter(m -> keys[m] % 3 != 0);
			grid.refreshRowPermutation();
		});
		assertBodyPaints(1, () -> {
			grid.setRowPermutation(null);
			grid.setItemCount(ROWS / 2);
		});
	}

	@Test
	public void thumbDragTracksTheBarAndCoalescesBodyPaints() {
		final ScrollBar bar = grid.getVerticalBar();
		final int before = grid.bodyPaintCount;
		final long t0 = System.nanoTime();
		for (int i = 1; i <= 60; i++) {
			bar.setSelection(i * 50);
			final Event e = new Event();
			e.detail = SWT.DRAG;
			bar.notifyListeners(SWT.Selection, e);
			assertEquals("the thumb follows each drag event at once", i * 50, bar.getSelection());
			display.readAndDispatch();
		}
		bar.setSelection(3000);
		bar.notifyListeners(SWT.Selection, new Event());
		grid.update();
		flush();
		final long frames = (System.nanoTime() - t0) / 16_000_000L + 2;
		final int paints = grid.bodyPaintCount - before;
		assertTrue("body paints " + paints + " <= frames " + frames + " and < drag events", paints <= frames && paints < 60);
		assertEquals(3000, grid.getTopIndex());
	}

	private void assertBodyPaints(final int expected, final Runnable op) {
		flush();
		final int before = grid.bodyPaintCount;
		op.run();
		grid.update();
		flush();
		assertEquals(expected, grid.bodyPaintCount - before);
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain
		}
	}
}
