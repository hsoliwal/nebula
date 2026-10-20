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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Shared helpers of the M3 widget tests: event pumping, timed spinning, paint recording and on-screen pixels.
 */
public final class M3TestSupport {

	private M3TestSupport() {
	}

	/** dispatches everything queued */
	public static void flush(Display display) {
		while (display.readAndDispatch()) {
			// dispatch everything queued
		}
	}

	/** runs the event loop for about the given time (timers fire) */
	public static void spin(Display display, int milliseconds) {
		boolean[] done = new boolean[1];
		display.timerExec(milliseconds, () -> done[0] = true);
		while (!done[0]) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		flush(display);
	}

	/** records the clip rectangle of every paint of a control */
	public static List<Rectangle> recordPaints(Control control) {
		List<Rectangle> paints = new ArrayList<>();
		control.addListener(SWT.Paint, e -> paints.add(new Rectangle(e.x, e.y, e.width, e.height)));
		return paints;
	}

	/** asserts that every recorded paint lies inside the allowed rectangle */
	public static void assertPaintsInside(List<Rectangle> paints, Rectangle allowed) {
		for (Rectangle paint : paints) {
			assertTrue(paint + " outside " + allowed, allowed.contains(paint.x, paint.y)
					&& allowed.contains(paint.x + paint.width - 1, paint.y + paint.height - 1));
		}
	}

	/** asserts that at least one recorded paint covers the whole rectangle */
	public static void assertSomePaintCovers(List<Rectangle> paints, Rectangle area) {
		for (Rectangle paint : paints) {
			if (paint.contains(area.x, area.y) && paint.contains(area.x + area.width - 1, area.y + area.height - 1)) {
				return;
			}
		}
		throw new AssertionError("no paint covers " + area + ": " + paints);
	}

	/**
	 * The resize sweep of the viewport-delta gate (brief rule 24): 50 widths, 20 one-pixel steps up from the base, 20
	 * down back to it, then 10 large jumps; consecutive widths always differ.
	 */
	public static int[] sweepWidths(int base) {
		int[] widths = new int[50];
		int i = 0;
		for (int k = 1; k <= 20; k++) {
			widths[i++] = base + k;
		}
		for (int k = 19; k >= 0; k--) {
			widths[i++] = base + k;
		}
		for (int jump : new int[] { base * 2, base / 2, base * 3 / 2, base + 7, base - 13, base * 2 + 1, base / 3, base, base + 100, base }) {
			widths[i++] = jump;
		}
		return widths;
	}

	/**
	 * The hidden-viewport gate (brief rule 24, "Hidden viewports"): hides the control, applies 10^4 model deltas,
	 * checks that they caused no paint, no visibility probe, no measure, move, creation or full repaint, shows the
	 * control and checks for exactly one paint pass and one reveal.
	 */
	public static void assertHiddenGate(Control control, List<Rectangle> paints, IntConsumer delta) {
		Display display = control.getDisplay();
		control.setVisible(false);
		flush(display);
		paints.clear();
		M3Counters.reset();
		for (int i = 0; i < 10_000; i++) {
			delta.accept(i);
		}
		flush(display);
		assertEquals("paints while hidden", 0, paints.size());
		assertEquals(M3Counters.line(), 0, M3Counters.probes + M3Counters.measures + M3Counters.moves + M3Counters.creates + M3Counters.fullRepaints);
		assertTrue(M3Counters.line(), M3Counters.hiddenDeltas >= 10_000);
		control.setVisible(true);
		flush(display);
		control.update();
		assertEquals("one paint pass after show: " + paints, 1, paints.size());
		assertEquals(M3Counters.line(), 1, M3Counters.reveals);
	}

	/**
	 * Asserts two images have the same pixels; on a mismatch saves both as PNG under
	 * S:/m3-scratch/custom-widgets/logs/pixels-&lt;name&gt;-{expected,actual}.png and reports the first differing pixel.
	 */
	public static void assertSamePixels(String name, ImageData expected, ImageData actual) {
		if (expected.width == actual.width && expected.height == actual.height) {
			for (int y = 0; y < expected.height; y++) {
				for (int x = 0; x < expected.width; x++) {
					RGB e = expected.palette.getRGB(expected.getPixel(x, y)), a = actual.palette.getRGB(actual.getPixel(x, y));
					if (!e.equals(a)) {
						save(name, expected, actual);
						throw new AssertionError(name + ": first difference at (" + x + "," + y + "): expected " + e + " but was " + a);
					}
				}
			}
			return;
		}
		save(name, expected, actual);
		throw new AssertionError(name + ": size " + expected.width + "x" + expected.height + " vs " + actual.width + "x" + actual.height);
	}

	private static void save(String name, ImageData expected, ImageData actual) {
		String dir = System.getProperty("m3.test.pixels.dir", "S:/m3-scratch/custom-widgets/logs"); //$NON-NLS-1$ //$NON-NLS-2$
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { expected };
		loader.save(dir + "/pixels-" + name + "-expected.png", SWT.IMAGE_PNG); //$NON-NLS-1$ //$NON-NLS-2$
		loader.data = new ImageData[] { actual };
		loader.save(dir + "/pixels-" + name + "-actual.png", SWT.IMAGE_PNG); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/** the on-screen pixels of a control's client area, after pending paints */
	public static ImageData pixels(Control control) {
		control.update();
		Rectangle client = control instanceof org.eclipse.swt.widgets.Scrollable
				? ((org.eclipse.swt.widgets.Scrollable) control).getClientArea()
				: new Rectangle(0, 0, control.getSize().x, control.getSize().y);
		Image image = new Image(control.getDisplay(), Math.max(1, client.width), Math.max(1, client.height));
		GC gc = new GC(control);
		gc.copyArea(image, client.x, client.y);
		gc.dispose();
		ImageData data = image.getImageData();
		image.dispose();
		return data;
	}
}
