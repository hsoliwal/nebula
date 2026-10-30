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
import static org.junit.Assert.assertTrue;

import java.security.MessageDigest;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Blit scrolling must leave the screen exactly as a full repaint would. Capture mode
 * DISPLAY_COPY: the on-screen pixels of the Grid (shell on top and active) after scrollbar
 * scrolling, compared with the same Grid fully repainted.
 */
public class GridBlitScroll_Test {

	private Display display;
	private Shell shell;
	private Grid grid;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
		grid = new Grid(shell, SWT.VIRTUAL | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 0, 520, 360);
		grid.setHeaderVisible(true);
		grid.setRowHeaderVisible(true);
		grid.setLinesVisible(true);
		for (int c = 0; c < 6; c++) {
			new GridColumn(grid, SWT.NONE).setWidth(90);
		}
		grid.addListener(SWT.SetData, e -> {
			final GridItem item = (GridItem) e.item;
			for (int c = 0; c < 6; c++) {
				item.setText(c, "r" + e.index + "c" + c);
			}
		});
		grid.setItemCount(5000);
		grid.select(new int[] { 3, 9, 40 });
		shell.setSize(560, 420);
		shell.setLocation(20, 20);
		shell.open();
		shell.forceActive();
		settle();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void blitScrollEqualsFullRepaintOnScreen() throws Exception {
		final ScrollBar bar = grid.getVerticalBar();
		final int before = grid.blitScrollCount;
		final String initial = screen();
		for (final int step : new int[] { 1, 2, 5, 3, 17, 1, 9, 30, 2 }) {
			final int beforeScroll = grid.paintPasses;
			bar.setSelection(bar.getSelection() + step);
			bar.notifyListeners(SWT.Selection, new Event());
			grid.update();
			GridSwtScreenshotCapture.awaitStableCounter(display, () -> grid.paintPasses,
					beforeScroll + 1, "blit-scroll paint did not settle");
			final String blitted = screen();
			final int beforeRepaint = grid.paintPasses;
			grid.redraw();
			grid.update();
			GridSwtScreenshotCapture.awaitStableCounter(display, () -> grid.paintPasses,
					beforeRepaint + 1, "full repaint did not settle");
			assertEquals("after +" + step + " rows", screen(), blitted);
		}
		assertTrue("blit path used: " + (grid.blitScrollCount - before), grid.blitScrollCount > before);
		assertNotEquals("scrolling must change the visible Grid pixels", initial, screen());
	}

	/** SHA-256 of the Grid's actual on-screen pixels (DISPLAY_COPY_AREA). */
	private String screen() throws Exception {
		final byte[] pixels = GridSwtScreenshotCapture.captureScreenData(grid, "blit-scroll").data;
		final byte[] digest = MessageDigest.getInstance("SHA-256").digest(pixels);
		final StringBuilder hex = new StringBuilder();
		for (final byte b : digest) {
			hex.append(String.format("%02x", java.lang.Integer.valueOf(b & 0xff)));
		}
		return hex.toString();
	}

	private void settle() {
		GridSwtScreenshotCapture.awaitStableCounter(display, () -> grid.paintPasses, 1,
				"initial blit-scrolling Grid paint did not settle");
	}
}
