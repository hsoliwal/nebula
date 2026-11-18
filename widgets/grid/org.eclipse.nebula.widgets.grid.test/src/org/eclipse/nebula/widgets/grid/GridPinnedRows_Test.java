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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Pinned rows are a fixed plane of the same Grid: unscrolled they paint exactly like the
 * unpinned Grid, scrolled the plane keeps its pixels while rows scroll beneath, and hit
 * testing returns the pinned items. Capture mode DISPLAY_COPY_AREA; compare client pixels without native scroll bars.
 */
public class GridPinnedRows_Test {

	private static final int ROWS = 500;

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		// on top: the test compares screen pixels, and a shell that a background process opens stays behind the
		// window that holds the foreground (same style as GridBlitScroll_Test)
		shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
		new Text(shell, SWT.SINGLE).setBounds(0, 0, 40, 20);
		shell.setSize(1000, 480);
		shell.setLocation(20, 20);
		shell.open();
		shell.forceActive();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void unscrolledPinnedTopRowsPaintLikeTheUnpinnedGrid() throws Exception {
		final Grid plain = grid(0);
		final Grid pinned = grid(470);
		pinned.setPinnedRowCount(3, 0);
		flush();
		assertEquals(plain.getClientArea(), pinned.getClientArea());

		final String[] hashes = GridSwtScreenshotCapture.comparableGridClientHashes(
				plain, snapshot(plain), snapshot(pinned), plain.getClientArea());
		assertEquals("pinned and unpinned native body pixels", hashes[0], hashes[1]);
	}

	@Test
	public void pinnedPlanesKeepTheirPixelsWhileTheBodyScrolls() throws Exception {
		final Grid grid = grid(0);
		grid.setPinnedRowCount(2, 2);
		flush();
		final Rectangle top = topPlane(grid, 2);
		final Rectangle bottom = bottomPlane(grid, 2);
		final Rectangle body = new Rectangle(0, top.y + top.height, top.width, 40);
		final GridSwtScreenshotCapture.GridClientFrame initial = snapshot(grid);
		GridSwtScreenshotCapture.GridClientFrame latest = initial;
		for (final int index : new int[] { 1, 37, 250, 480 }) {
			final int before = grid.paintPasses;
			grid.setTopIndex(index);
			GridSwtScreenshotCapture.awaitStableCounter(display, () -> grid.paintPasses, before + 1,
					"pinned-row scroll paint did not settle");
			latest = snapshot(grid);
			final String[] topHashes = GridSwtScreenshotCapture.comparableGridClientHashes(
					grid, initial, latest, top);
			if (!topHashes[0].equals(topHashes[1])) {
				final org.eclipse.swt.widgets.ScrollBar vertical = grid.getVerticalBar();
				System.out.println("GRID_NATIVE_SCROLLBAR_DIAGNOSTIC pinned top=" + index
						+ " plane=" + top + " client=" + grid.getClientArea() + " screen=" + grid.getSize()
						+ " track=" + (vertical == null ? "none" : vertical.getThumbTrackBounds())
						+ " thumb=" + (vertical == null ? "none" : vertical.getThumbBounds())
						+ " visible=" + (vertical != null && vertical.getVisible()));
			}
			assertEquals("top plane at " + index, topHashes[0], topHashes[1]);
			final String[] bottomHashes = GridSwtScreenshotCapture.comparableGridClientHashes(
					grid, initial, latest, bottom);
			assertEquals("bottom plane at " + index, bottomHashes[0], bottomHashes[1]);
		}
		final String[] bodyHashes = GridSwtScreenshotCapture.comparableGridClientHashes(
				grid, initial, latest, body);
		assertNotEquals("the body itself scrolled", bodyHashes[0], bodyHashes[1]);
	}

	@Test
	public void hitTestingReturnsPinnedItems() {
		final Grid grid = grid(0);
		grid.setPinnedRowCount(2, 1);
		grid.setTopIndex(200);
		flush();
		final Rectangle top = topPlane(grid, 2);
		final int h = grid.getItem(0).getHeight() + 1;
		assertSame(grid.getItem(0), grid.getItem(new Point(30, top.y + 1)));
		assertSame(grid.getItem(1), grid.getItem(new Point(30, top.y + h + 1)));
		final Rectangle bottom = bottomPlane(grid, 1);
		assertSame(grid.getItem(ROWS - 1), grid.getItem(new Point(30, bottom.y + 2)));
		final GridItem below = grid.getItem(new Point(30, top.y + top.height + 2));
		assertEquals("first scrolled row under the plane", 200 + 2, grid.indexOf(below));
	}


	@Test
	public void treeAncestorsStayAboveScrolledChildrenAndBelowTheColumnHeader() throws Exception {
		final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 30, 470, 400);
		grid.setHeaderVisible(true);
		final GridColumn treeColumn = new GridColumn(grid, SWT.NONE);
		treeColumn.setTree(true);
		treeColumn.setFixed(true);
		treeColumn.setWidth(190);
		new GridColumn(grid, SWT.NONE).setWidth(300);
		final GridItem first = new GridItem(grid, SWT.NONE);
		first.setText(0, "Project A");
		final GridItem branch = new GridItem(first, SWT.NONE);
		branch.setText(0, "Package A");
		final GridItem[] leaves = new GridItem[65];
		for (int i = 0; i < leaves.length; i++) {
			leaves[i] = new GridItem(branch, SWT.NONE);
			leaves[i].setText(0, "Source " + i);
		}
		first.setExpanded(true);
		branch.setExpanded(true);
		final GridItem second = new GridItem(grid, SWT.NONE);
		second.setText(0, "Project B");
		final GridItem branchB = new GridItem(second, SWT.NONE);
		branchB.setText(0, "Package B");
		final GridItem[] other = new GridItem[65];
		for (int i = 0; i < other.length; i++) {
			other[i] = new GridItem(branchB, SWT.NONE);
			other[i].setText(0, "Other " + i);
		}
		second.setExpanded(true);
		branchB.setExpanded(true);
		assertEquals(false, grid.getStickyAncestorRows());
		grid.setStickyAncestorRows(true);
		assertTrue(grid.getStickyAncestorRows());
		grid.setTopIndex(leaves[12].getRowIndex());
		flush();
		final int top = grid.getHeaderHeight(), row = first.getHeight() + 1;
		assertSame(first, grid.getItem(new Point(25, top + 2)));
		assertSame(branch, grid.getItem(new Point(25, top + row + 2)));
		assertEquals(top, grid.getOrigin(treeColumn, first).y);
		assertEquals(top + row, grid.getOrigin(treeColumn, branch).y);
		final Rectangle pinned = new Rectangle(0, top, grid.getClientArea().width, 2 * row);
		final GridSwtScreenshotCapture.GridClientFrame before = snapshot(grid);
		grid.setTopIndex(leaves[39].getRowIndex());
		flush();
		final GridSwtScreenshotCapture.GridClientFrame after = snapshot(grid);
		final String[] hashes = GridSwtScreenshotCapture.comparableGridClientHashes(
				grid, before, after, pinned);
		assertEquals("ancestor headers remain unchanged over scrolling descendants", hashes[0], hashes[1]);
		assertSame(first, grid.getItem(new Point(25, top + 2)));
		assertSame(branch, grid.getItem(new Point(25, top + row + 2)));
		grid.setTopIndex(other[15].getRowIndex());
		flush();
		assertSame(second, grid.getItem(new Point(25, top + 2)));
		assertSame(branchB, grid.getItem(new Point(25, top + row + 2)));
		grid.setStickyAncestorRows(false);
		flush();
		assertSame(other[15], grid.getItem(new Point(25, top + 2)));
	}

	@Test
	public void stickyAncestorsDoNotChangeFlatGridPinningOrTreeRowSelection() {
		final Grid flat = grid(0);
		assertEquals(false, flat.getStickyAncestorRows());
		flat.setStickyAncestorRows(true);
		flat.setPinnedRowCount(2, 1);
		assertEquals(2, flat.getPinnedTopRowCount());
		assertSame(flat.getItem(0), flat.getItem(new Point(25, flat.getHeaderHeight() + 2)));
		final Grid tree = new Grid(shell, SWT.V_SCROLL);
		tree.setBounds(470, 30, 460, 390);
		new GridColumn(tree, SWT.NONE).setTree(true);
		final GridItem root = new GridItem(tree, SWT.NONE);
		final GridItem child = new GridItem(root, SWT.NONE);
		root.setExpanded(true);
		tree.setStickyAncestorRows(true);
		try {
			tree.setPinnedRowCount(1, 0);
			fail("flat pinned-row API still refuses a hierarchical Grid");
		} catch (IllegalArgumentException expected) {
			// the original flat-grid pin API is forbidden for tree rows
		}
		assertSame(root, child.getParentItem());
		assertEquals(true, tree.getStickyAncestorRows());
	}

	@Test
	public void permutationChoosesThePinnedModelRows() throws Exception {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 30, 600, 400);
		grid.setHeaderVisible(true);
		for (int c = 0; c < 4; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(100);
		}
		final RowPermutation p = new RowPermutation(ROWS);
		grid.addListener(SWT.SetData, e -> ((GridItem) e.item).setText(0, "m" + p.modelRow(e.index)));
		grid.setItemCount(ROWS);
		grid.setRowPermutation(p);
		p.setPinnedRows(new int[] { 42 }, new int[] { 7 });
		p.sortByIntKeys(descendingKeys(), false);
		grid.refreshRowPermutation();
		grid.setPinnedRowCount(p.getPinnedTopCount(), p.getPinnedBottomCount());
		grid.setTopIndex(100);
		flush();
		assertEquals("m42", grid.getItem(new Point(30, topPlane(grid, 1).y + 1)).getText(0));
		assertEquals("m7", grid.getItem(new Point(30, bottomPlane(grid, 1).y + 1)).getText(0));
	}

	@Test
	public void rejectsNegativeCounts() {
		final Grid grid = grid(0);
		try {
			grid.setPinnedRowCount(-1, 0);
			fail();
		} catch (final IllegalArgumentException expected) {
			// expected
		}
	}

	private static int[] descendingKeys() {
		final int[] keys = new int[ROWS];
		for (int m = 0; m < ROWS; m++) {
			keys[m] = ROWS - m;
		}
		return keys;
	}

	private Grid grid(final int x) {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(x, 30, 460, 400);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		for (int c = 0; c < 6; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("C" + c);
			column.setWidth(90);
		}
		grid.getColumn(0).setFixed(true);
		for (int r = 0; r < ROWS; r++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 6; c++) {
				item.setText(c, "r" + r + "c" + c);
			}
		}
		flush();
		return grid;
	}

	private static Rectangle topPlane(final Grid grid, final int rows) {
		final int h = (grid.getItem(0).getHeight() + 1) * rows;
		return new Rectangle(0, grid.getHeaderHeight(), grid.getClientArea().width, h);
	}

	private static Rectangle bottomPlane(final Grid grid, final int rows) {
		final int h = (grid.getItem(0).getHeight() + 1) * rows;
		return new Rectangle(0, grid.getClientArea().height - h, grid.getClientArea().width, h);
	}

	private static GridSwtScreenshotCapture.GridClientFrame snapshot(final Grid grid) throws Exception {
		return GridSwtScreenshotCapture.captureGridClientFrame(grid, "pinned-rows");
	}

	@Test
	public void differentAnimatedOverlayWidthsUseAnIdenticalBodyPixelExtent() {
		final Grid grid = new Grid(shell, SWT.V_SCROLL);
		grid.setBounds(0, 30, 460, 400);
		final Rectangle client = grid.getClientArea();
		final int border = grid.getBorderWidth();
		final PaletteData palette = new PaletteData(0xff0000, 0x00ff00, 0x0000ff);
		final ImageData first = new ImageData(460, 400, 24, palette);
		final ImageData later = new ImageData(460, 400, 24, palette);
		final int endX = border + client.width - 6;
		assertTrue("fixture must have a native right-hand content strip",
				endX > 10 && endX + 1 < later.width);
		// The scrollbar is only 2 pixels in one frame and 6 in another.
		// A single-frame hash would hash unequal numbers of pixels.
		later.setPixel(endX + 1, border + 10, 0x00ffffff);
		final GridSwtScreenshotCapture.GridClientFrame narrow =
				new GridSwtScreenshotCapture.GridClientFrame(first, client, border, 2);
		final GridSwtScreenshotCapture.GridClientFrame wide =
				new GridSwtScreenshotCapture.GridClientFrame(later, client, border, 6);
		final String[] matching = GridSwtScreenshotCapture.comparableGridClientHashes(
				grid, narrow, wide, client);
		assertEquals("only measured native overlay pixels may differ", matching[0], matching[1]);
		later.setPixel(border + 10, border + 10, 0x00ffffff);
		final String[] changed = GridSwtScreenshotCapture.comparableGridClientHashes(
				grid, narrow, wide, client);
		assertNotEquals("any interior body pixel difference must fail", changed[0], changed[1]);
	}

	@Test
	public void nativeScrollbarExclusionNeverMasksBodyPixels() {
		// Synthetic pixel fixture proves that the exact native comparator
		// ignores ONLY a two-pixel right overlay, never interior cell changes.
		final PaletteData palette = new PaletteData(0xff0000, 0x00ff00, 0x0000ff);
		final ImageData reference = new ImageData(10, 4, 24, palette);
		final ImageData changed = new ImageData(10, 4, 24, palette);
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 10; x++) {
				final int pixel = (x * 71 << 16) | (y * 13 << 8) | (x + y);
				reference.setPixel(x, y, pixel);
				changed.setPixel(x, y, pixel);
			}
		}
		final Rectangle full = new Rectangle(0, 0, 10, 4);
		changed.setPixel(9, 2, 0x00ffffff);
		assertEquals("only native overlay pixels may differ",
				GridSwtScreenshotCapture.exactContentHash(reference, full, 8),
				GridSwtScreenshotCapture.exactContentHash(changed, full, 8));
		assertNotEquals("full screen comparison must still detect native overlay delta",
				GridSwtScreenshotCapture.exactContentHash(reference, full, 10),
				GridSwtScreenshotCapture.exactContentHash(changed, full, 10));
		changed.setPixel(3, 1, 0x00ffffff);
		assertNotEquals("a single body pixel difference remains fatal",
				GridSwtScreenshotCapture.exactContentHash(reference, full, 8),
				GridSwtScreenshotCapture.exactContentHash(changed, full, 8));
	}

	private void flush() {
		for (final org.eclipse.swt.widgets.Control child : shell.getChildren()) {
			if (child instanceof Grid) {
				final Grid target = (Grid) child;
				GridSwtScreenshotCapture.awaitStableCounter(display, () -> target.paintPasses, 1,
						"pinned-row Grid paint did not settle");
			}
		}
	}
}
