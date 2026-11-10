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
package org.eclipse.nebula.widgets.pagination.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.eclipse.nebula.widgets.pagination.renderers.navigation.graphics.NavigationPageGraphics;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget D leaf of the page links: a style setter shows at once, to the pixels of a full repaint (before, at the
 * next unrelated repaint); the style in use paints nothing.
 */
public class NavigationPageGraphicsStyleTest {

	private Display display;
	private Shell shell;
	private NavigationPageGraphics links;
	private int paints;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setSize(500, 120);
		links = new NavigationPageGraphics(shell, SWT.NONE);
		links.setBounds(10, 10, 460, 40);
		links.update(new int[] { 0, 1, 2, 3, 4 }, 1, Locale.ENGLISH);
		links.addListener(SWT.Paint, e -> paints++);
		shell.open();
		spin();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void aStyleSetterShowsAtOnce() {
		step("selected background", () -> links.setSelectedItemBackground(display.getSystemColor(SWT.COLOR_RED)));
		step("selected foreground", () -> links.setSelectedItemForeground(display.getSystemColor(SWT.COLOR_YELLOW)));
		step("item foreground", () -> links.setItemForeground(display.getSystemColor(SWT.COLOR_DARK_GREEN)));
		step("round", () -> links.setRound(12));

		paints = 0;
		links.setSelectedItemBackground(display.getSystemColor(SWT.COLOR_RED));
		links.setRound(12);
		spin();
		assertEquals("the style in use paints nothing", 0, paints);
	}

	private void step(String name, Runnable change) {
		change.run();
		spin();
		int[] shown = pixels();
		links.redraw();
		links.update();
		spin();
		assertArrayEquals(name + ": the new style shows without another repaint", pixels(), shown);
	}

	private int[] pixels() {
		Rectangle size = links.getBounds();
		Image shot = new Image(display, size.width, size.height);
		GC gc = new GC(links);
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
