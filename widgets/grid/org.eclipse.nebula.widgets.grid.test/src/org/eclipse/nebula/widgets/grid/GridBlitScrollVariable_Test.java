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
import static org.junit.Assert.assertTrue;

import java.security.MessageDigest;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Leaf G5 (docs/m3/GRID_MEDIATED_VIEWPORT.md): rows of differing heights and tree rows scroll by a blit of the exact
 * pixel delta of the row tree, and the screen after each blit equals the same Grid fully repainted (capture mode
 * DISPLAY_COPY, shell on top and active, as GridBlitScroll_Test).
 */
public class GridBlitScrollVariable_Test {

	private static final int[] STEPS = { 1, 2, 5, 3, 17, 1, 9, 30, 2, 4, 6 };

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void differingHeightsBlitEqualsFullRepaintOnScreen() throws Exception {
		final Grid grid = grid();
		for (int i = 0; i < 3000; i++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 6; c++) {
				item.setText(c, "r" + i + "c" + c);
			}
			if (i % 11 == 0) {
				item.setHeight(grid.getItemHeight() * 2);
			} else if (i % 7 == 0) {
				item.setHeight(grid.getItemHeight() + 9);
			}
		}
		open(grid);
		assertTrue("rows of differing heights are indexed", grid.rowsIndexed());
		assertBlitEqualsRepaint(grid);
	}

	@Test
	public void treeRowsBlitEqualsFullRepaintOnScreen() throws Exception {
		final Grid grid = grid();
		grid.getColumn(0).setTree(true);
		for (int i = 0; i < 400; i++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 6; c++) {
				root.setText(c, "root " + i + " c" + c);
			}
			for (int k = 0; k < 4; k++) {
				final GridItem child = new GridItem(root, SWT.NONE);
				child.setText(0, "child " + i + "." + k);
			}
			root.setExpanded(i % 3 == 0);
		}
		open(grid);
		assertTrue("tree rows are indexed", grid.rowsIndexed());
		assertBlitEqualsRepaint(grid);
	}

	private Grid grid() {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 0, 520, 360);
		grid.setHeaderVisible(true);
		grid.setRowHeaderVisible(true);
		grid.setLinesVisible(true);
		for (int c = 0; c < 6; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(90);
		}
		return grid;
	}

	private void open(final Grid grid) {
		grid.select(new int[] { 3, 9, 40 });
		shell.setSize(560, 420);
		shell.open();
		shell.forceActive();
		settle();
	}

	private void assertBlitEqualsRepaint(final Grid grid) throws Exception {
		final ScrollBar bar = grid.getVerticalBar();
		final int before = grid.blitScrollCount;
		for (final int step : STEPS) {
			bar.setSelection(bar.getSelection() + step);
			bar.notifyListeners(SWT.Selection, new Event());
			grid.update();
			settle();
			final String blitted = screen(grid);
			grid.redraw();
			grid.update();
			settle();
			assertEquals("after +" + step + " rows", screen(grid), blitted);
		}
		assertTrue("blit path used: " + (grid.blitScrollCount - before), grid.blitScrollCount > before);
	}

	/** SHA-256 of the Grid's on-screen pixels (display copy). */
	private String screen(final Grid grid) throws Exception {
		final Point size = grid.getSize();
		final Image image = new Image(display, size.x, size.y);
		try {
			GridSwtScreenshotCapture.screenBounds(grid);
			final GC gc = new GC(display);
			try {
				final Point origin = grid.toDisplay(0, 0);
				gc.copyArea(image, origin.x, origin.y);
			} finally {
				gc.dispose();
			}
			final byte[] digest = MessageDigest.getInstance("SHA-256").digest(image.getImageData().data);
			final StringBuilder hex = new StringBuilder();
			for (final byte b : digest) {
				hex.append(String.format("%02x", java.lang.Integer.valueOf(b & 0xff)));
			}
			return hex.toString();
		} finally {
			image.dispose();
		}
	}

	private void settle() {
        // GTK paints at frame boundaries; an empty event queue is not a settled frame.
        long idleSince = System.nanoTime();
        final long deadline = idleSince + 5_000_000_000L;
        while (System.nanoTime() - idleSince < 200_000_000L) {
            assertTrue("native paint queue did not settle", System.nanoTime() < deadline);
            if (display.readAndDispatch()) {
                idleSince = System.nanoTime();
            } else {
                try { Thread.sleep(5L); }
                catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("interrupted while settling native paint", failure);
                }
            }
        }
    }
}
