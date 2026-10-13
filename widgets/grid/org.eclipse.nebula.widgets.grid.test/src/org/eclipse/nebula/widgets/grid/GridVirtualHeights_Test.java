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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * computeSize of a flat virtual Grid with differing row heights measures materialized rows only:
 * the same size as a Grid whose every row is materialized, with no row created to measure it.
 */
public class GridVirtualHeights_Test {

	private static final int ROWS = 20_000;

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void computeSizeDoesNotMaterializeRows() {
		final Grid lazy = grid();
		final Grid full = grid();
		for (int i = 0; i < ROWS; i++) {
			full.getItem(i);
		}
		for (final Grid g : new Grid[] { lazy, full }) {
			g.getItem(10).setHeight(40);
			g.getItem(15_000).setHeight(5);
			g.getItem(ROWS - 1).setHeight(33);
		}
		final int before = lazy.virtualMaterializedItemCount();
		final Point lazySize = lazy.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		assertEquals(full.computeSize(SWT.DEFAULT, SWT.DEFAULT), lazySize);
		assertTrue("rows created to measure: " + (lazy.virtualMaterializedItemCount() - before),
				lazy.virtualMaterializedItemCount() - before <= 2);
		assertEquals(ROWS, full.virtualMaterializedItemCount());
	}

	private Grid grid() {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL);
		grid.setHeaderVisible(true);
		new GridColumn(grid, SWT.NONE).setWidth(120);
		grid.setItemCount(ROWS);
		return grid;
	}
}
