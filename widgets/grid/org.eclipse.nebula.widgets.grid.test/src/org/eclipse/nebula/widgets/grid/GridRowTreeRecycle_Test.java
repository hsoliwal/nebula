/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assume.assumeTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Regression oracle for the original GridRowTree's released-slot reuse and
 * viewport row/pixel queries. The companion OpenRewrite candidate is in
 * hsoliwal/com.synexia#12076. This test does not simulate OutOfMemoryError:
 * failure-atomicity additionally requires a qualified allocation-failure gate.
 */
public class GridRowTreeRecycle_Test {
	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	@Test
	public void removeAndReinsertKeepRowRanksAndPixelExtents() {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL);
		final GridRowTree tree = grid.rowTree();
		assumeTrue("GridRowTree is active only in viewport mode", tree != null);
		new GridColumn(grid, SWT.NONE).setTree(true);

		for (int i = 0; i < 48; i++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			root.setHeight(12 + (i % 8));
			new GridItem(root, SWT.NONE);
			if ((i & 3) == 0) {
				root.setExpanded(true);
			}
		}
		assertLinearOracle(grid, tree);

		// Repeated middle, first and trailing removals exercise detached AVL nodes,
		// the free slot directory, and slot reuse by subsequent inserted items.
		for (int step = 0; step < 128; step++) {
			final GridItem[] before = grid.getItems();
			final int deleted = step % 3 == 0 ? 0
					: step % 3 == 1 ? before.length / 2 : before.length - 1;
			before[deleted].dispose();

			final int at = step % (grid.getRootItemCount() + 1);
			final GridItem added = new GridItem(grid, SWT.NONE, at);
			added.setHeight(13 + (step % 11));
			if (step % 4 == 0) {
				new GridItem(added, SWT.NONE);
				added.setExpanded(true);
			}
			if (step % 9 == 0) {
				final GridItem[] current = grid.getItems();
				for (final GridItem item : current) {
					if (item.getParentItem() == null && item.getItemCount() > 0) {
						item.setExpanded(!item.isExpanded());
						break;
					}
				}
			}
			assertLinearOracle(grid, tree);
		}
	}

	private static void assertLinearOracle(final Grid grid, final GridRowTree tree) {
		final GridItem[] rows = grid.getItems();
		assertEquals(rows.length, tree.size());
		long expectedPixels = 0;
		int expectedVisible = 0;

		for (int row = 0; row < rows.length; row++) {
			final GridItem item = rows[row];
			assertSame("item at " + row, item, tree.get(row));
			assertEquals("row index " + row, row, tree.indexOf(item));
			assertEquals("visible rank at " + row, expectedVisible, tree.rankOf(row));
			assertEquals("pixel prefix at " + row, expectedPixels, tree.extentBefore(row));

			final int extent = item.isVisible() ? item.getHeight() + 1 : 0;
			assertEquals("row extent " + row, extent, tree.extentAt(row));
			if (extent > 0) {
				assertEquals("rank select " + expectedVisible, row, tree.positionOfRank(expectedVisible));
				assertEquals("rank prefix " + expectedVisible, expectedPixels,
						tree.extentOfRank(expectedVisible));
				expectedVisible++;
			}
			expectedPixels += extent;
		}
		assertEquals(expectedVisible, tree.visibleCount());
		assertEquals(expectedPixels, tree.totalExtent());
		assertEquals(expectedVisible, tree.rankOf(rows.length));
		assertEquals(expectedPixels, tree.extentBefore(rows.length));
		assertEquals(expectedPixels, tree.extentOfRank(expectedVisible));
		assertEquals(-1, tree.rankAtExtent(-1));
		assertEquals(expectedVisible, tree.rankAtExtent(expectedPixels));
	}
}
