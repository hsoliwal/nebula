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
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.nebula.widgets.m3.M3ImageView;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3ImageView: a pan paints only the exposed strip and leaves the same pixels as a view opened at that origin, zoom
 * keeps the image point under the pointer, fit to window, a resize sweep, the hidden gate, errors. Runs in both
 * startup modes.
 */
public class M3ImageViewTest {

	Display display;
	Shell shell;
	Image image;
	M3ImageView view;
	List<Rectangle> paints;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 420, 320);
		ImageData data = new ImageData(400, 300, 24, new PaletteData(0xff0000, 0xff00, 0xff));
		for (int y = 0; y < 300; y++) {
			for (int x = 0; x < 400; x++) {
				data.setPixel(x, y, ((x * 7) & 0xff) << 16 | ((y * 5) & 0xff) << 8 | ((x ^ y) & 0xff));
			}
		}
		image = new Image(display, data);
		view = new M3ImageView(shell, SWT.NONE);
		view.setBounds(0, 0, 200, 150);
		view.setImage(image);
		view.setScale(2);
		view.setOrigin(10, 10);
		paints = M3TestSupport.recordPaints(view);
		shell.open();
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
		image.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3ImageView", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	M3ImageView fresh(double originX, double originY) {
		M3ImageView other = new M3ImageView(shell, SWT.NONE);
		other.setBounds(205, 0, 200, 150);
		other.setImage(image);
		other.setScale(2);
		other.setOrigin(originX, originY);
		M3TestSupport.flush(display);
		return other;
	}

	@Test
	public void panPaintsTheExposedStripOnly() {
		paints.clear();
		view.panBy(12, 0);
		view.update();
		if (viewport()) {
			M3TestSupport.assertPaintsInside(paints, new Rectangle(200 - 12, 0, 12, 150));
		}
		for (int i = 0; i < 5; i++) {
			view.panBy(3, 2);
			view.update();
		}
		double[] origin = view.getOrigin();
		assertEquals(10 + (12 + 15) / 2.0, origin[0], 1e-9);
		assertEquals(10 + 10 / 2.0, origin[1], 1e-9);
		assertArrayEquals(M3TestSupport.pixels(fresh(origin[0], origin[1])).data, M3TestSupport.pixels(view).data);
	}

	@Test
	public void zoomKeepsThePointUnderThePointer() {
		Point before = view.toClient(60, 40);
		view.zoomAt(2, before.x, before.y);
		assertEquals(4, view.getScale(), 1e-9);
		Point after = view.toClient(60, 40);
		assertTrue(before + " -> " + after, Math.abs(before.x - after.x) <= 1 && Math.abs(before.y - after.y) <= 1);
		view.setScaleLimits(0.5, 3);
		assertEquals(3, view.getScale(), 1e-9);
		view.fitToWindow();
		assertEquals(0.5, view.getScale(), 1e-9);
		view.setScaleLimits(0.1, 8);
		view.fitToWindow();
		assertEquals(Math.min(200 / 400.0, 150 / 300.0), view.getScale(), 1e-9);
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		int height = view.getSize().y, old = view.getSize().x;
		M3Counters.reset();
		for (int width : M3TestSupport.sweepWidths(old)) {
			paints.clear();
			view.setSize(width, height);
			view.update();
			if (viewport()) {
				for (Rectangle paint : paints) {
					assertTrue(old + " -> " + width + ": " + paint, paint.x >= Math.min(old, width));
				}
			}
			old = width;
		}
		assertEquals(M3Counters.line(), 0, M3Counters.fullRepaints);
	}

	@Test
	public void hiddenViewFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		M3TestSupport.assertHiddenGate(view, paints, i -> {
			view.panBy(i % 2 == 0 ? 1 : -1, 0);
			view.zoomAt(i % 2 == 0 ? 2 : 0.5, 50, 50);
		});
		double[] origin = view.getOrigin();
		assertEquals(2, view.getScale(), 1e-9);
		assertArrayEquals(M3TestSupport.pixels(fresh(origin[0], origin[1])).data, M3TestSupport.pixels(view).data);
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> view.setScale(0));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> view.setScaleLimits(2, 1));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> view.zoomAt(-1, 0, 0));
		view.setImage(null);
		view.fitToWindow();
		assertEquals(new Point(1, 1), view.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
}
