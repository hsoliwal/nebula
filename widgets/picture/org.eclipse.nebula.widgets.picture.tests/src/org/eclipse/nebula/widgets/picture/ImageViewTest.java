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
package org.eclipse.nebula.widgets.picture;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.nebula.widgets.picture.internal.RepaintCounters;
import org.eclipse.nebula.widgets.picture.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * ImageView: a pan paints only the exposed strip and leaves the same pixels as a view opened at that origin, zoom
 * keeps the image point under the pointer, fit to window, a resize sweep, the hidden gate, errors. Runs in both
 * startup modes.
 */
public class ImageViewTest {

	Display display;
	Shell shell;
	Image image;
	ImageView view;
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
		view = new ImageView(shell, SWT.NONE);
		view.setBounds(0, 0, 200, 150);
		view.setImage(image);
		view.setScale(2);
		view.setOrigin(10, 10);
		paints = TestSupport.recordPaints(view);
		shell.open();
		TestSupport.awaitNativePaint(view, paints, 1, "initial ImageView did not paint");
	}

	@After
	public void tearDown() {
		shell.dispose();
		image.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("ImageView", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	ImageView fresh(double originX, double originY) {
		ImageView other = new ImageView(shell, SWT.NONE);
		other.setBounds(205, 0, 200, 150);
		other.setImage(image);
		other.setScale(2);
		other.setOrigin(originX, originY);
		List<Rectangle> freshPaints = TestSupport.recordPaints(other);
		TestSupport.awaitNativePaint(other, freshPaints, 1, "fresh ImageView did not paint");
		return other;
	}

	@Test
	public void panPaintsTheExposedStripOnly() {
		paints.clear();
		view.panBy(12, 0);
		view.update();
		if (viewport()) {
			TestSupport.awaitNativePaint(view, paints, 1, "panned ImageView did not paint its exposed strip");
			TestSupport.assertPaintsInside(paints, new Rectangle(200 - 12, 0, 12, 150));
		}
		for (int i = 0; i < 5; i++) {
			view.panBy(3, 2);
			view.update();
		}
		double[] origin = view.getOrigin();
		assertEquals(10 + (12 + 15) / 2.0, origin[0], 1e-9);
		assertEquals(10 + 10 / 2.0, origin[1], 1e-9);
		assertSameVisiblePixels(fresh(origin[0], origin[1]), view);
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
		RepaintCounters.reset();
		for (int width : TestSupport.sweepWidths(old)) {
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
		assertEquals(RepaintCounters.line(), 0, RepaintCounters.fullRepaints);
	}

	@Test
	public void hiddenViewFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		TestSupport.assertHiddenGate(view, paints, i -> {
			view.panBy(i % 2 == 0 ? 1 : -1, 0);
			view.zoomAt(i % 2 == 0 ? 2 : 0.5, 50, 50);
		});
		double[] origin = view.getOrigin();
		assertEquals(2, view.getScale(), 1e-9);
		assertSameVisiblePixels(fresh(origin[0], origin[1]), view);
	}

	/**
	 * Original Nebula ImageView consumes inherited Canvas layers. This qualifies
	 * its native-screen presentation and the shared painted fallback contract;
	 * it does not assert selection of a DirectComposition backend.
	 */
	@Test
	public void inheritedCanvasAffineClipAndOrderPreserveImageViewOwnership() {
		RGB red = new RGB(255, 0, 0), blue = new RGB(0, 0, 255);
		RGB beforeInside = screenPixel(44, 47), beforeOutside = screenPixel(50, 47);
		assertFalse("capture must distinguish the underlying ImageView", red.equals(beforeInside));
		assertFalse("capture must distinguish the reordered layer", blue.equals(beforeInside));
		double[] originalOrigin = view.getOrigin();
		double originalScale = view.getScale();
		ImageData source = solid(8, 8, 0xff0000);
		int lower = view.addLayer(source);
		Transform transform = new Transform(display, 2, 0, 0, 3, 40, 40);
		try {
			view.setLayerTransform(lower, transform);
			transform.identity(); // Canvas copied the caller's transform
		} finally {
			transform.dispose();
		}
		Rectangle clip = new Rectangle(1, 1, 3, 4);
		view.setLayerClip(lower, clip);
		clip.x = 7; // Canvas copied the caller's local clip
		for (int y = 0; y < source.height; y++) {
			for (int x = 0; x < source.width; x++) source.setPixel(x, y, 0x0000ff);
		}
		try {
			assertEquals(red, screenPixel(44, 47));
			assertEquals(beforeOutside, screenPixel(50, 47));
			assertEquals(lower, view.getLayerAtPoint(44, 47));
			assertEquals(-1, view.getLayerAtPoint(50, 47));
			int upper = view.addLayer(solid(4, 4, 0x0000ff));
			view.setLayerLocation(upper, 42, 45);
			assertArrayEquals(new int[] { lower, upper }, view.getLayerIds());
			assertEquals(blue, screenPixel(44, 47));
			assertEquals(upper, view.getLayerAtPoint(44, 47));
			view.setLayerZOrder(lower, 1);
			assertArrayEquals(new int[] { upper, lower }, view.getLayerIds());
			assertEquals(red, screenPixel(44, 47));
			assertEquals(lower, view.getLayerAtPoint(44, 47));
			view.setLayerZOrder(lower, 0);
			assertArrayEquals(new int[] { lower, upper }, view.getLayerIds());
			assertEquals("equal Z restores creation order", blue, screenPixel(44, 47));
			view.setLayerOpacity(upper, 0);
			assertEquals(red, screenPixel(44, 47));
			assertEquals(lower, view.getLayerAtPoint(44, 47));
			view.setLayerClip(lower, new Rectangle(0, 0, 0, 0));
			assertEquals("empty clip restores the ImageView pixels", beforeInside, screenPixel(44, 47));
			assertEquals(-1, view.getLayerAtPoint(44, 47));
			assertSame(image, view.getImage());
			assertFalse(image.isDisposed());
			assertEquals(originalScale, view.getScale(), 0.0);
			assertArrayEquals(originalOrigin, view.getOrigin(), 0.0);
			assertEquals("layers stay in the original widget", 0, view.getChildren().length);
		} finally {
			view.removeAllLayers();
		}
		assertEquals(beforeInside, screenPixel(44, 47));
		assertEquals(beforeOutside, screenPixel(50, 47));
		assertArrayEquals(new int[0], view.getLayerIds());
	}

	@Test
	public void inheritedCanvasLayersReleaseWithImageViewAndRetainCallerImage() {
		int retired = view.addLayer(solid(6, 6, 0xff0000));
		view.setLayerLocation(retired, 70, 70);
		assertEquals(new RGB(255, 0, 0), screenPixel(72, 72));
		view.removeLayer(retired);
		int replacement = view.addLayer(solid(6, 6, 0x0000ff));
		view.setLayerLocation(replacement, 70, 70);
		assertTrue("retired Canvas handles are not reused", replacement != retired);
		assertEquals(new RGB(0, 0, 255), screenPixel(72, 72));
		try {
			view.setLayerOpacity(retired, 0);
			fail("retired layer handle must be refused");
		} catch (IllegalArgumentException expected) {
			assertEquals(new RGB(0, 0, 255), screenPixel(72, 72));
		}
		view.dispose();
		assertTrue(view.isDisposed());
		assertFalse("ImageView never owns the caller's Image", image.isDisposed());
		assertEquals(400, image.getBounds().width);
		try {
			view.getLayerIds();
			fail("disposed original widget must refuse layer access");
		} catch (SWTException expected) {
			assertEquals(SWT.ERROR_WIDGET_DISPOSED, expected.code);
		}
	}

	private static ImageData solid(int width, int height, int rgb) {
		ImageData data = new ImageData(width, height, 24, new PaletteData(0xff0000, 0xff00, 0xff));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) data.setPixel(x, y, rgb);
		}
		return data;
	}

	/** Compare final visible RGB values, excluding storage padding and alpha layout. */
	private void assertSameVisiblePixels(ImageView expected, ImageView actual) {
		ImageData expectedPixels = screenPixels(expected), actualPixels = screenPixels(actual);
		assertEquals(expectedPixels.width, actualPixels.width);
		assertEquals(expectedPixels.height, actualPixels.height);
		for (int y = 0; y < expectedPixels.height; y++) {
			for (int x = 0; x < expectedPixels.width; x++) {
				assertEquals("visible ImageView pixel (" + x + ", " + y + ")",
						expectedPixels.palette.getRGB(expectedPixels.getPixel(x, y)),
						actualPixels.palette.getRGB(actualPixels.getPixel(x, y)));
			}
		}
	}

	private ImageData screenPixels(ImageView target) {
		assertFalse(target.isDisposed());
		assertTrue("capture requires a visible original ImageView", target.isVisible());
		Rectangle client = target.getClientArea();
		Point point = target.toDisplay(client.x, client.y);
		assertTrue("capture must be wholly on screen", display.getBounds().contains(point)
				&& display.getBounds().contains(point.x + client.width - 1, point.y + client.height - 1));
		Image pixels = new Image(display, client.width, client.height);
		try {
			GC capture = new GC(display);
			try {
				capture.copyArea(pixels, point.x, point.y);
			} finally {
				capture.dispose();
			}
			return pixels.getImageData();
		} finally {
			pixels.dispose();
		}
	}

	/** Visible-screen capture; control-GC copies do not prove final presentation. */
	private RGB screenPixel(int x, int y) {
		assertFalse(view.isDisposed());
		assertTrue("capture requires a visible original ImageView", view.isVisible());
		assertTrue(view.getClientArea().contains(x, y));
		view.update();
		TestSupport.spin(display, 60);
		Point point = view.toDisplay(x, y);
		assertTrue("capture must be on screen", display.getBounds().contains(point));
		Image pixel = new Image(display, 1, 1);
		try {
			GC capture = new GC(display);
			try {
				capture.copyArea(pixel, point.x, point.y);
			} finally {
				capture.dispose();
			}
			ImageData captured = pixel.getImageData();
			return captured.palette.getRGB(captured.getPixel(0, 0));
		} finally {
			pixel.dispose();
		}
	}

	@Test
	public void errors() {
		TestSupport.expect(SWT.ERROR_INVALID_ARGUMENT, () -> view.setScale(0));
		TestSupport.expect(SWT.ERROR_INVALID_ARGUMENT, () -> view.setScaleLimits(2, 1));
		TestSupport.expect(SWT.ERROR_INVALID_ARGUMENT, () -> view.zoomAt(-1, 0, 0));
		view.setImage(null);
		view.fitToWindow();
		assertEquals(new Point(1, 1), view.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
}
