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
package org.eclipse.nebula.visualization.widgets.figureparts;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget D leaf of AlphaLabel: a new alpha shows at once (before, it showed at the next unrelated repaint), to
 * the pixels of a full repaint; the alpha in use paints nothing.
 */
public class AlphaLabelDamageTest {

	static final class CountingLabel extends AlphaLabel {
		int paints;

		CountingLabel(String text) {
			setText(text);
		}

		@Override
		protected void paintFigure(Graphics graphics) {
			paints++;
			super.paintFigure(graphics);
		}
	}

	private Display display;
	private Shell shell;
	private Canvas canvas;
	private CountingLabel label;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setSize(300, 200);
		canvas = new Canvas(shell, SWT.DOUBLE_BUFFERED);
		canvas.setBounds(0, 0, 200, 100);
		LightweightSystem system = new LightweightSystem(canvas);
		Figure root = new Figure();
		root.setOpaque(true);
		root.setBackgroundColor(display.getSystemColor(SWT.COLOR_WHITE));
		root.setLayoutManager(new XYLayout());
		label = new CountingLabel("alpha");
		label.setBackgroundColor(display.getSystemColor(SWT.COLOR_RED));
		root.add(label, new org.eclipse.draw2d.geometry.Rectangle(10, 10, 100, 40));
		system.setContents(root);
		shell.open();
		spin();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void aNewAlphaShowsAtOnce() {
		label.setAlpha(255);
		spin();
		int[] shown = pixels();
		canvas.redraw();
		canvas.update();
		spin();
		assertArrayEquals("the new alpha shows without another repaint", pixels(), shown);

		label.paints = 0;
		label.setAlpha(255);
		spin();
		assertEquals("the alpha in use paints nothing", 0, label.paints);
	}

	private int[] pixels() {
		Rectangle size = canvas.getBounds();
		Image shot = new Image(display, size.width, size.height);
		GC gc = new GC(canvas);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int[] all = new int[size.width * size.height];
		for (int y = 0; y < size.height; y++) {
			data.getPixels(0, y, size.width, all, y * size.width);
		}
		return all;
	}

	private void spin() {
		long end = System.currentTimeMillis() + 200;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
}
