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
		for (final int step : new int[] { 1, 2, 5, 3, 17, 1, 9, 30, 2 }) {
			bar.setSelection(bar.getSelection() + step);
			bar.notifyListeners(SWT.Selection, new Event());
			grid.update();
			settle();
			final String blitted = screen();
			grid.redraw();
			grid.update();
			settle();
			assertEquals("after +" + step + " rows", screen(), blitted);
		}
		assertTrue("blit path used: " + (grid.blitScrollCount - before), grid.blitScrollCount > before);
	}

	/** SHA-256 of the Grid's on-screen pixels (display copy). */
	private String screen() throws Exception {
		final Point size = grid.getSize();
		final Image image = new Image(display, size.x, size.y);
		try {
			final GC gc = new GC(grid);
			try {
				gc.copyArea(image, 0, 0);
			} finally {
				gc.dispose();
			}
			final byte[] digest = MessageDigest.getInstance("SHA-256").digest(image.getImageData().data);
			final StringBuilder hex = new StringBuilder();
			for (final byte b : digest) {
				hex.append(String.format("%02x", b & 0xff));
			}
			return hex.toString();
		} finally {
			image.dispose();
		}
	}

	private void settle() {
		for (int i = 0; i < 5; i++) {
			while (display.readAndDispatch()) {
				// drain
			}
		}
	}
}
