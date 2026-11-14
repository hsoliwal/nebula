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
package org.eclipse.nebula.widgets.pshelf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link PShelfViewport} on a display (docs/m3/every-widget, aspects H, D and R): a hidden open ends like the animation
 * of a shown shelf, and hover and setText damage only the headers that change, with the pixels of a full repaint.
 */
public class PShelfViewportTest {

	private static final int ITEMS = 6;

	private Display display;
	private final List<Shell> shells = new ArrayList<>();

	@Before
	public void setUp() {
		assumeTrue("viewport mode", PShelfViewport.ENABLED);
		display = Display.getDefault();
	}

	@After
	public void tearDown() {
		shells.forEach(Shell::dispose);
	}

	private PShelf shelf(int x) {
		Shell shell = new Shell(display, SWT.NO_TRIM);
		shells.add(shell);
		shell.setBounds(x, 10, 220, 320);
		PShelf shelf = new PShelf(shell, SWT.NONE);
		shelf.setBounds(0, 0, 200, 300);
		for (int i = 0; i < ITEMS; i++) {
			PShelfItem item = new PShelfItem(shelf, SWT.NONE);
			item.setText("item " + i);
			item.getBody().setLayout(new FillLayout());
			new Label(item.getBody(), SWT.NONE).setText("body " + i);
		}
		shell.open();
		settle(shelf);
		return shelf;
	}

	private void settle(PShelf shelf) {
		while (display.readAndDispatch()) {
			// drain
		}
		shelf.update();
	}

	private static void move(PShelf shelf, int y) {
		Event event = new Event();
		event.x = 10;
		event.y = y;
		shelf.notifyListeners(SWT.MouseMove, event);
	}

	private static List<Rectangle> recordPaints(PShelf shelf) {
		List<Rectangle> paints = new ArrayList<>();
		shelf.addListener(SWT.Paint, e -> paints.add(new Rectangle(e.x, e.y, e.width, e.height)));
		return paints;
	}

	private static int[] pixels(PShelf shelf) {
		Rectangle client = shelf.getClientArea();
		Image image = new Image(shelf.getDisplay(), client.width, client.height);
		GC gc = new GC(shelf);
		gc.copyArea(image, 0, 0);
		gc.dispose();
		ImageData data = image.getImageData();
		image.dispose();
		int[] out = new int[client.width * client.height];
		for (int y = 0; y < client.height; y++) {
			data.getPixels(0, y, client.width, out, y * client.width);
		}
		return out;
	}

	/* the pixels after a stock full repaint of the same state */
	private int[] fullRepaintPixels(PShelf shelf) {
		shelf.redraw();
		settle(shelf);
		return pixels(shelf);
	}

	@Test
	public void hiddenOpenEndsLikeTheShownAnimation() {
		PShelf hidden = shelf(10), shown = shelf(300);
		hidden.setVisible(false);
		PShelfItem target = hidden.getItems()[4];
		hidden.setSelection(target);
		shown.setSelection(shown.getItems()[4]);
		assertEquals("hidden opens that jumped", 1, hidden.viewport.jumps);
		assertEquals("a shown shelf animates", 0, shown.viewport.jumps);
		assertTrue("frames not run: " + hidden.viewport.framesSkipped, hidden.viewport.framesSkipped >= 50);
		for (int i = 0; i < ITEMS; i++) {
			Composite a = hidden.getItems()[i].getBodyParent(), b = shown.getItems()[i].getBodyParent();
			assertEquals("body parent " + i + " bounds", b.getBounds(), a.getBounds());
			assertEquals("body parent " + i + " visible", b.getVisible(), a.getVisible());
		}
		assertSame(target, hidden.getSelection());
		hidden.setVisible(true);
		settle(hidden);
		settle(shown);
		assertArrayEquals("pixels after show", pixels(shown), pixels(hidden));
	}

	@Test
	public void hoverDamagesTheChangedHeadersOnly() {
		PShelf shelf = shelf(10);
		List<Rectangle> paints = recordPaints(shelf);
		int fullRedraws = shelf.viewport.fullRedraws; // the setText calls of the construction, before the shell showed
		int[] lane = shelf.m3Lane();
		int height = shelf.m3ItemHeight();
		move(shelf, lane[2] + height / 2);
		settle(shelf);
		move(shelf, lane[3] + height / 2);
		settle(shelf);
		assertEquals("header damages", 2, shelf.viewport.headerDamages);
		assertEquals("full redraws", fullRedraws, shelf.viewport.fullRedraws);
		assertTrue("paints: " + paints, !paints.isEmpty());
		for (Rectangle paint : paints) {
			assertTrue("at most the two changed headers: " + paint, paint.height <= 2 * height);
		}
		int[] damaged = pixels(shelf);
		assertArrayEquals("pixels equal a full repaint", fullRepaintPixels(shelf), damaged);
	}

	@Test
	public void setTextDamagesOneHeaderAndCullsTheOthers() {
		PShelf shelf = shelf(10);
		List<Rectangle> paints = recordPaints(shelf);
		int culled = shelf.viewport.culled;
		shelf.getItems()[2].setText("renamed");
		settle(shelf);
		assertEquals("header damages", 1, shelf.viewport.headerDamages);
		assertEquals("one paint of one header: " + paints, 1, paints.size());
		assertTrue("header rect: " + paints, paints.get(0).height <= shelf.m3ItemHeight());
		assertTrue("headers outside the clip skipped", shelf.viewport.culled > culled);
		int[] damaged = pixels(shelf);
		assertArrayEquals("pixels equal a full repaint", fullRepaintPixels(shelf), damaged);
	}
}
