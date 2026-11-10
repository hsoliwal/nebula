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
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget D leaf of the Grid colours: a new line colour and a new cell header selection background show at once,
 * to the pixels of a full repaint (before, at the next unrelated repaint); the colours in use paint nothing.
 */
public class GridStyleColour_Test {

	private Display display;
	private Shell shell;
	private Grid grid;
	private int paints;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(10, 10, 460, 320);
		grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 0, 440, 280);
		grid.setHeaderVisible(true);
		grid.setRowHeaderVisible(true);
		grid.setLinesVisible(true);
		grid.setCellSelectionEnabled(true);
		for (int c = 0; c < 3; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("column " + c);
			column.setWidth(110);
		}
		for (int r = 0; r < 10; r++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 3; c++) {
				item.setText(c, "cell " + r + "." + c);
			}
		}
		grid.addListener(SWT.Paint, e -> paints++);
		shell.open();
		spin();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void theColoursShowAtOnce() {
		step("setLineColor", () -> grid.setLineColor(display.getSystemColor(SWT.COLOR_RED)));
		grid.setCellSelection(new Point(1, 2));
		spin();
		step("setCellHeaderSelectionBackground",
				() -> grid.setCellHeaderSelectionBackground(display.getSystemColor(SWT.COLOR_BLUE)));

		paints = 0;
		grid.setLineColor(display.getSystemColor(SWT.COLOR_RED));
		grid.setCellHeaderSelectionBackground(display.getSystemColor(SWT.COLOR_BLUE));
		spin();
		assertEquals("the colours in use paint nothing", 0, paints);
	}

	private void step(final String name, final Runnable change) {
		change.run();
		spin();
		final int[] shown = pixels();
		grid.redraw();
		grid.update();
		spin();
		assertArrayEquals(name + ": the new colour shows without another repaint", pixels(), shown);
	}

	private int[] pixels() {
		final Rectangle size = grid.getBounds();
		final Image shot = new Image(display, size.width, size.height);
		final GC gc = new GC(grid);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		final ImageData data = shot.getImageData();
		shot.dispose();
		final int[] all = new int[size.width * size.height];
		for (int y = 0; y < size.height; y++) {
			data.getPixels(0, y, size.width, all, y * size.width);
		}
		return all;
	}

	private void spin() {
		final long end = System.currentTimeMillis() + 200;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (final InterruptedException e) {
					return;
				}
			}
		}
	}
}
