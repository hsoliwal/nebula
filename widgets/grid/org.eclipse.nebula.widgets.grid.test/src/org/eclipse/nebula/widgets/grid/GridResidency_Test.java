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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Reproduction round trip: states set on rows, scroll away so the rows are released, scroll back;
 * API values and printed pixels (capture mode CONTROL_PRINT) are identical and every row saw
 * exactly one {@code SWT.SetData}.
 */
public class GridResidency_Test {

	private static final int ROWS = 10_000;
	private static final int COLUMNS = 6;
	private static final int CAPACITY = 120;

	private Display display;
	private Shell shell;
	private Color tint;
	private Grid grid;
	private final int[] setData = new int[ROWS];

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		new Text(shell, SWT.SINGLE).setBounds(0, 0, 40, 20);
		tint = new Color(display, 250, 230, 200);
		grid = new Grid(shell, SWT.VIRTUAL | SWT.CHECK | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 30, 640, 400);
		grid.setHeaderVisible(true);
		for (int c = 0; c < COLUMNS; c++) {
			new GridColumn(grid, c == 0 ? SWT.CHECK : SWT.NONE).setWidth(100);
		}
		final RowPermutation permutation = new RowPermutation(ROWS);
		grid.addListener(SWT.SetData, e -> {
			final int m = permutation.modelRow(e.index);
			setData[m]++;
			final GridItem item = (GridItem) e.item;
			for (int c = 0; c < COLUMNS; c++) {
				item.setText(c, "m" + m + "c" + c);
			}
		});
		grid.setItemCount(ROWS);
		grid.setRowPermutation(permutation);
		grid.setItemResidency(Grid.RESIDENCY_EVICT_COLD, CAPACITY);
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
	public void releasedRowsComeBackIdenticalWithoutAnotherSetData() throws Exception {
		for (int r = 2; r < 6; r++) {
			final GridItem item = grid.getItem(r);
			item.setBackground(1, tint);
			item.setText(3, "edited " + r);
			item.setChecked(0, true);
		}
		grid.select(4);
		flush();
		final String[] before = snapshot(0, 25);
		final String pixels = GridRowPermutation_Test.pixels(grid);

		for (final int top : new int[] { 1000, 3000, 6000, 9000 }) {
			grid.setTopIndex(top);
			grid.update();
			flush();
		}
		assertTrue("cold rows were released: " + grid.residencyReleased, grid.residencyReleased > 0);
		assertTrue("resident items bounded: " + grid.virtualMaterializedItemCount(),
				grid.virtualMaterializedItemCount() <= CAPACITY + 64);

		grid.setTopIndex(0);
		grid.update();
		flush();
		assertArrayEquals(before, snapshot(0, 25));
		assertEquals(pixels, GridRowPermutation_Test.pixels(grid));
		assertTrue(grid.residencyRestored > 0);
		for (int m = 0; m < 25; m++) {
			assertEquals("SetData calls for row " + m, 1, setData[m]);
		}
		assertTrue(grid.getItem(4).getParent() == grid && grid.isSelected(4));
	}

	private String[] snapshot(final int from, final int to) {
		final String[] out = new String[to - from];
		for (int r = from; r < to; r++) {
			final GridItem item = grid.getItem(r);
			final StringBuilder b = new StringBuilder();
			for (int c = 0; c < COLUMNS; c++) {
				b.append(item.getText(c)).append('|').append(item.getBackground(c)).append('|')
						.append(item.getForeground(c)).append('|').append(item.getChecked(c)).append(';');
			}
			b.append(item.getHeight()).append(grid.isSelected(r));
			out[r - from] = b.toString();
		}
		return out;
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain
		}
	}
}
