/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others.
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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Test;

/**
 * Differential test of the row-kept selection of a virtual Grid: the same seeded scripts run on a
 * Grid whose selection is kept by row and on one whose selection is the item list of the earlier
 * releases (the oracle), and both leave the same trace: events, selection order and contents, count,
 * index and indices, focus, cell selection. Covers select/deselect/selectAll/deselectAll/
 * setSelection, insert/remove/dispose/setItemCount/clear, keys and clicks with shift and ctrl, row
 * permutations, a tree conversion, SINGLE, MULTI and cell selection.
 */
public class GridRowSelectionDifferential_Test {

	private static final int STEPS = 300;
	private Shell shell;

	@After
	public void tearDown() {
		Grid.rowSelectionByRow = true;
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	private List<String> trace(final boolean byRow, final int style, final boolean cells, final long seed) {
		return GridSelectionScript.trace(() -> {
			Grid.rowSelectionByRow = byRow;
			shell = new Shell(Display.getDefault());
			shell.setSize(420, 360);
			final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL | SWT.H_SCROLL | style);
			Grid.rowSelectionByRow = true;
			grid.setBounds(0, 0, 400, 300);
			grid.setHeaderVisible(true);
			for (int c = 0; c < 3; c++) {
				new GridColumn(grid, SWT.NONE).setWidth(100);
			}
			grid.setCellSelectionEnabled(cells);
			final GridSelectionScript script = new GridSelectionScript(grid);
			if (byRow) {
				script.afterStep = grid::checkRowSelection;
			}
			grid.setItemCount(300);
			shell.open();
			return script;
		}, seed, STEPS);
	}

	private void compare(final int style, final boolean cells, final int seeds) {
		GridSelectionScript.takeReruns();
		for (long seed = 1; seed <= seeds; seed++) {
			final List<String> oracle = trace(false, style, cells, seed);
			final List<String> rows = trace(true, style, cells, seed);
			for (int i = 0; i < oracle.size(); i++) {
				assertEquals("seed " + seed + " step " + i, oracle.get(i), rows.get(i));
			}
		}
		final int reruns = GridSelectionScript.takeReruns();
		assertTrue("interrupted script runs run again: " + reruns + " (at most one)", reruns <= 1);
	}

	@Test
	public void multiSelectionScriptsMatchTheItemList() {
		compare(SWT.MULTI, false, 12);
	}

	@Test
	public void singleSelectionScriptsMatchTheItemList() {
		compare(SWT.SINGLE, false, 6);
	}

	@Test
	public void cellSelectionScriptsMatchTheItemList() {
		compare(SWT.MULTI, true, 4);
	}
}
