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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.security.MessageDigest;

import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
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
 * testing returns the pinned items. Capture mode CONTROL_PRINT.
 */
public class GridPinnedRows_Test {

	private static final int ROWS = 500;

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		new Text(shell, SWT.SINGLE).setBounds(0, 0, 40, 20);
		shell.setSize(1000, 480);
		shell.open();
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
		assertEquals(GridRowPermutation_Test.pixels(plain), GridRowPermutation_Test.pixels(pinned));
	}

	@Test
	public void pinnedPlanesKeepTheirPixelsWhileTheBodyScrolls() throws Exception {
		final Grid grid = grid(0);
		grid.setPinnedRowCount(2, 2);
		flush();
		final Rectangle top = topPlane(grid, 2);
		final Rectangle bottom = bottomPlane(grid, 2);
		final String top0 = region(grid, top);
		final String bottom0 = region(grid, bottom);
		final String body0 = region(grid, new Rectangle(0, top.y + top.height, top.width, 40));
		for (final int index : new int[] { 1, 37, 250, 480 }) {
			grid.setTopIndex(index);
			flush();
			assertEquals("top plane at " + index, top0, region(grid, top));
			assertEquals("bottom plane at " + index, bottom0, region(grid, bottom));
		}
		assertNotEquals("the body itself scrolled", body0,
				region(grid, new Rectangle(0, top.y + top.height, top.width, 40)));
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

	private static String region(final Grid grid, final Rectangle r) throws Exception {
		final Point size = grid.getSize();
		final Image image = new Image(grid.getDisplay(), size.x, size.y);
		try {
			final GC gc = new GC(image);
			try {
				grid.print(gc);
			} finally {
				gc.dispose();
			}
			final ImageData data = image.getImageData();
			final MessageDigest md = MessageDigest.getInstance("SHA-256");
			for (int y = r.y; y < r.y + r.height; y++) {
				for (int x = r.x; x < r.x + r.width; x++) {
					final int pixel = data.getPixel(x, y);
					md.update((byte) pixel);
					md.update((byte) (pixel >>> 8));
					md.update((byte) (pixel >>> 16));
				}
			}
			final StringBuilder hex = new StringBuilder();
			for (final byte b : md.digest()) {
				hex.append(String.format("%02x", b & 0xff));
			}
			return hex.toString();
		} finally {
			image.dispose();
		}
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain
		}
	}
}
