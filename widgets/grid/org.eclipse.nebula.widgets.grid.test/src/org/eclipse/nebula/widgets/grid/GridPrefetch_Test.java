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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Idle prefetch: rows below the viewport are populated before they are scrolled in. */
public class GridPrefetch_Test {

	private Display display;
	private Shell shell;
	private Grid grid;
	private final int[] setData = new int[100_000];

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL);
		grid.setBounds(0, 0, 400, 300);
		new GridColumn(grid, SWT.NONE).setWidth(200);
		grid.addListener(SWT.SetData, e -> {
			setData[e.index]++;
			((GridItem) e.item).setText("row " + e.index);
		});
		grid.setItemCount(setData.length);
		grid.setPrefetchRows(30);
		shell.setSize(420, 340);
		shell.open();
		flush();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void rowsBelowTheViewportArePopulatedWhileIdle() {
		final int bottom = grid.getBottomIndex();
		assertTrue("prefetched " + grid.prefetchedRows, grid.prefetchedRows >= 30);
		for (int r = bottom + 1; r <= bottom + 30; r++) {
			assertEquals("row " + r + " populated once", 1, setData[r]);
		}
		final int[] before = setData.clone();
		grid.setTopIndex(bottom + 1);
		grid.update();
		for (int r = bottom + 1; r <= bottom + 12; r++) {
			assertEquals("no SetData when row " + r + " scrolls in", before[r], setData[r]);
		}
	}

	@Test
	public void zeroDisablesPrefetch() {
		grid.setPrefetchRows(0);
		final int bottom = grid.getBottomIndex();
		grid.setTopIndex(5000);
		grid.update();
		flush();
		assertEquals(0, setData[5000 + 60]);
		assertTrue(bottom > 0);
	}

	private void flush() {
		for (int i = 0; i < 50; i++) {
			while (display.readAndDispatch()) {
				// drain
			}
		}
	}
}
