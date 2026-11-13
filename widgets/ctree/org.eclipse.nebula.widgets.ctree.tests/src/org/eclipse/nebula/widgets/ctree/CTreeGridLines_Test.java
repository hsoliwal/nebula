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
package org.eclipse.nebula.widgets.ctree;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget D leaf of CTree: hiding or showing the horizontal or vertical grid lines shows at once, to the pixels
 * of a full repaint (before, the old lines stayed until the next unrelated repaint); the value in use paints nothing.
 */
public class CTreeGridLines_Test {

	private Display display;
	private Shell shell;
	private CTree tree;
	private int paints;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setBounds(10, 10, 500, 300);
		tree = new CTree(shell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		for (String name : new String[] { "Name", "Size", "Kind" }) {
			CTreeColumn column = new CTreeColumn(tree, SWT.LEFT);
			column.setText(name);
			column.setWidth(140);
		}
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		for (int i = 0; i < 6; i++) {
			new CTreeItem(tree, SWT.NONE).setText("item " + i);
		}
		tree.body.addListener(SWT.Paint, e -> paints++);
		shell.open();
		spin();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void theLineFlagsShowAtOnce() {
		step("horizontal off", () -> tree.setHorizontalLinesVisible(false));
		step("vertical off", () -> tree.setVerticalLinesVisible(false));
		step("horizontal on", () -> tree.setHorizontalLinesVisible(true));
		paints = 0;
		tree.setHorizontalLinesVisible(true);
		tree.setVerticalLinesVisible(false);
		spin();
		assertEquals("the values in use paint nothing", 0, paints);
	}

	@Test
	public void theLinesFlagShowsAtOnce() {
		step("lines off", () -> tree.setLinesVisible(false));
		step("lines on", () -> tree.setLinesVisible(true));
	}

	private void step(String name, Runnable change) {
		change.run();
		spin();
		int[] shown = pixels();
		tree.body.redraw();
		tree.body.update();
		spin();
		assertArrayEquals(name + ": the change shows without another repaint", pixels(), shown);
	}

	private int[] pixels() {
		Rectangle size = tree.body.getBounds();
		Image shot = new Image(display, size.width, size.height);
		GC gc = new GC(tree.body);
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
