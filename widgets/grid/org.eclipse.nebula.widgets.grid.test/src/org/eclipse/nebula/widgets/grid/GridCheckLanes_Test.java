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

import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Packed check state: a Grid with a row view keeps checked/grayed/checkable per model row in
 * bit lanes. Compared with a classic Grid whose application keeps a {@code boolean[]} per model
 * row and pushes it in SetData: identical item results, counts and printed pixels (capture mode
 * CONTROL_PRINT) across sort, filter and filter-aware check-all.
 */
public class GridCheckLanes_Test {

	private static final int ROWS = 300;

	private Display display;
	private Shell shell;
	private Grid classic;
	private Grid packed;
	private RowPermutation permutation;
	private final boolean[] appChecked = new boolean[ROWS];
	private int[] appView = identity();
	private final int[] keys = new int[ROWS];

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		new Text(shell, SWT.SINGLE).setBounds(0, 0, 40, 20);
		for (int m = 0; m < ROWS; m++) {
			keys[m] = (m * 37) % 101;
		}
		classic = grid(0);
		classic.addListener(SWT.SetData, e -> {
			final GridItem item = (GridItem) e.item;
			item.setText("m" + appView[e.index]);
			item.setChecked(appChecked[appView[e.index]]);
		});
		classic.setItemCount(ROWS);
		packed = grid(430);
		permutation = new RowPermutation(ROWS);
		packed.addListener(SWT.SetData, e -> ((GridItem) e.item).setText("m" + permutation.modelRow(e.index)));
		packed.setItemCount(ROWS);
		packed.setRowPermutation(permutation);
		shell.setSize(900, 420);
		shell.open();
		flush();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void checkStateFollowsModelRowsAndCheckAllIsFilterAware() throws Exception {
		for (final int v : new int[] { 0, 3, 17, 120, 299 }) {
			packed.getItem(v).setChecked(true);
			appChecked[appView[v]] = true;
			classic.getItem(v).setChecked(true);
		}
		assertSame("checked");

		permutation.sortByIntKeys(keys, true);
		packed.refreshRowPermutation();
		classicView(RowPermutation.sortedOrder(keys, ROWS, true));
		assertSame("sorted");

		permutation.setFilter(m -> m % 4 != 0);
		packed.refreshRowPermutation();
		final int[] sorted = RowPermutation.sortedOrder(keys, ROWS, true);
		classicView(java.util.Arrays.stream(sorted).filter(m -> m % 4 != 0).toArray());
		assertSame("filtered");

		packed.setCheckedAll(0, true, true);
		for (int m = 0; m < ROWS; m++) {
			if (m % 4 != 0) {
				appChecked[m] = true;
			}
		}
		classicView(appView);
		assertSame("check all displayed");
		int all = 0;
		for (final boolean c : appChecked) {
			all += c ? 1 : 0;
		}
		assertEquals(all, packed.getCheckedCount(0, false));

		packed.setCheckedAll(0, false, false);
		java.util.Arrays.fill(appChecked, false);
		classicView(appView);
		assertSame("uncheck all");
		assertEquals(0, packed.getCheckedCount(0, false));
	}

	private void assertSame(final String step) throws Exception {
		flush();
		assertEquals(step, classic.getItemCount(), packed.getItemCount());
		int displayedChecked = 0;
		for (int v = 0; v < classic.getItemCount(); v++) {
			// getChecked(int) fires SetData first; the no-argument getChecked() of the classic Grid
			// would read a recycled item's stale state
			assertEquals(step + " v=" + v, classic.getItem(v).getChecked(0), packed.getItem(v).getChecked(0));
			displayedChecked += classic.getItem(v).getChecked(0) ? 1 : 0;
		}
		assertEquals(step + " count", displayedChecked, packed.getCheckedCount(0, true));
		assertEquals(step + " pixels", GridRowPermutation_Test.pixels(classic), GridRowPermutation_Test.pixels(packed));
	}

	private void classicView(final int[] view) {
		appView = view;
		classic.setItemCount(view.length);
		classic.clearAll(false);
	}

	private Grid grid(final int x) {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.CHECK | SWT.V_SCROLL);
		grid.setBounds(x, 30, 420, 360);
		grid.setHeaderVisible(true);
		final GridColumn column = new GridColumn(grid, SWT.CHECK);
		column.setText("checked");
		column.setWidth(200);
		return grid;
	}

	private static int[] identity() {
		final int[] a = new int[ROWS];
		for (int i = 0; i < ROWS; i++) {
			a[i] = i;
		}
		return a;
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain
		}
	}
}
