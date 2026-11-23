/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Visible-screen pixel and input regressions for positioned boxes in the existing Canvas owner. */
public class DrawnBrowserPositionedTest {

	private Display display;
	private Shell shell;
	private DrawnBrowser browser;

	@Test
	public void currentBorderSchemesPaintOnEmptyNestedPositionedBoxes() {
		browser.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		RGB dark = new RGB(64, 64, 64), light = new RGB(191, 191, 191);
		RGB base = new RGB(128, 128, 128), white = new RGB(255, 255, 255);
		for (String kind : new String[] {"solid", "inset", "outset", "groove", "ridge", "double"}) {
			show("<div style='position:relative;left:17px;top:11px;width:200px;height:150px'>"
					+ "<div style='position:absolute;left:13px;top:19px;width:80px;height:60px;"
					+ "border:6px " + kind + " #808080;background:white'></div></div>");
			ImageData image = capture();
			RGB outer = kind.equals("solid") || kind.equals("double") ? base
					: kind.equals("inset") || kind.equals("groove") ? dark : light;
			RGB opposite = kind.equals("solid") || kind.equals("double") ? base
					: kind.equals("inset") || kind.equals("groove") ? light : dark;
			assertEquals(kind + " outer left", outer, image.palette.getRGB(image.getPixel(31, 50)));
			assertEquals(kind + " outer right", opposite, image.palette.getRGB(image.getPixel(120, 50)));
			if (kind.equals("groove") || kind.equals("ridge")) {
				assertEquals(kind + " inner left", opposite, image.palette.getRGB(image.getPixel(34, 50)));
			} else if (kind.equals("double")) {
				assertEquals("double gap", white, image.palette.getRGB(image.getPixel(33, 50)));
				assertEquals("double inner", base, image.palette.getRGB(image.getPixel(35, 50)));
			}
			assertEquals(kind + " exterior", white, image.palette.getRGB(image.getPixel(29, 50)));
			assertEquals(kind + " interior", white, image.palette.getRGB(image.getPixel(40, 50)));
		}
	}

	@Test
	public void positionedInputKeepsItsNativeIdentityAndContainingBlockBounds() {
		show("<div style='position:relative;left:40px;top:20px;width:240px;height:100px'><input name=field value=hello></div>");
		org.eclipse.swt.widgets.Control[] children = browser.getChildren();
		assertEquals(1, children.length);
		assertTrue(children[0] instanceof org.eclipse.swt.widgets.Text);
		assertEquals("hello", ((org.eclipse.swt.widgets.Text) children[0]).getText());
		assertEquals(40, children[0].getBounds().x);
		assertEquals(28, children[0].getBounds().y);
		assertTrue(children[0].getVisible());
		browser.redraw();
		settle();
		assertTrue(children[0] == browser.getChildren()[0]);
		browser.setText("<p>replacement</p>");
		assertTrue(children[0].isDisposed());
	}

	@Test
	public void nestedWheelScrollMovesPanelPixelsWithoutMovingTheRootPage() {
		browser.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		for (String mode : new String[] { "auto", "scroll" }) {
			show("<section style='position:relative;width:100px;height:45px;overflow:" + mode + ";background:lime'>"
					+ "<article style='position:relative;top:70px;width:60px;height:20px;background:red'></article>"
					+ "</section><aside style='height:300px'></aside>");
			assertEquals("before wheel " + mode, new RGB(0, 255, 0), pixel(10, 30));
			Event wheel = new Event();
			wheel.x = 10; wheel.y = 30; wheel.count = -3;
			browser.notifyListeners(SWT.MouseWheel, wheel);
			settle();
			assertEquals("root scrollbar must stay unchanged " + mode, 0, browser.getScrollY());
			assertEquals("panel scrolled inner red item into view " + mode, new RGB(255, 0, 0), pixel(10, 30));
			assertEquals("panel cannot paint outside its scrollport " + mode, new RGB(255, 255, 255), pixel(10, 55));
		}
	}

	@Test
	public void rootScrollStartsOnTheNextEventAfterThePanelReachesItsLimit() {
		browser.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		show("<section style='position:relative;width:100px;height:40px;overflow:auto;background:lime'>"
				+ "<article style='position:relative;top:60px;width:60px;height:20px;background:red'></article>"
				+ "</section><aside style='height:500px'></aside>");
		Event wheel = new Event();
		wheel.x = 10; wheel.y = 10; wheel.count = -10; wheel.doit = true;
		browser.notifyListeners(SWT.MouseWheel, wheel);
		settle();
		assertTrue("the SWT default wheel action stays cancelled", !wheel.doit);
		assertEquals("the panel consumed part of the event; root stays put", 0, browser.getScrollY());
		Event afterBoundary = new Event();
		afterBoundary.x = 10; afterBoundary.y = 10; afterBoundary.count = -10; afterBoundary.doit = true;
		browser.notifyListeners(SWT.MouseWheel, afterBoundary);
		settle();
		assertTrue(!afterBoundary.doit);
		assertEquals("the next event reaches the root after saturation", 160, browser.getScrollY());
		browser.setScrollY(0);
		settle();
		assertEquals("local panel offset is retained after root scroll", new RGB(255, 0, 0), pixel(10, 30));
	}

	@Test
	public void nestedPanelClipsAbsoluteChildPixelsButKeepsItsOwnBorder() {
		browser.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		show("<div style='position:relative;left:20px;top:10px;width:80px;height:50px;"
				+ "border:4px solid blue;overflow:hidden;background:lime'>"
				+ "<div style='position:absolute;left:60px;top:10px;width:100px;height:30px;background:red'></div></div>");
		assertEquals(new RGB(0, 255, 0), pixel(40, 35));
		assertEquals(new RGB(255, 0, 0), pixel(95, 35));
		assertEquals("parent border cannot be overpainted by clipped child", new RGB(0, 0, 255), pixel(106, 35));
		assertEquals("child must not leak beyond parent", new RGB(255, 255, 255), pixel(120, 35));
	}

	@Test
	public void responsivePanelHidesPartiallyClippedNativeControlWithoutDisposingIt() {
		show("<div style='overflow:hidden;height:55px;border:2px solid red'>"
				+ "<input name=field size=18 value=kept></div>");
		org.eclipse.swt.widgets.Control[] controls = browser.getChildren();
		assertEquals(1, controls.length);
		assertTrue(controls[0].getVisible());
		shell.setSize(120, 240);
		settle();
		assertTrue("a partially cut native Text cannot leak beyond its panel", !controls[0].getVisible());
		assertTrue(!controls[0].isDisposed());
		shell.setSize(320, 240);
		settle();
		assertTrue("the same native Text returns when its panel fits", controls[0].getVisible());
		assertTrue(controls[0] == browser.getChildren()[0]);
	}

	@Test
	public void semanticPanelsPaintTheirOwnDecorationAndKeepFollowingContentInFlow() {
		browser.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		show("<main style='padding:10px;border:3px solid blue;background:lime'>"
				+ "<section style='height:30px;background:red'></section></main>"
				+ "<footer style='height:20px;background:yellow'></footer>");
		assertEquals(new RGB(0, 0, 255), pixel(1, 20));
		assertEquals(new RGB(0, 255, 0), pixel(7, 20));
		assertEquals(new RGB(255, 0, 0), pixel(20, 20));
		assertEquals(new RGB(255, 255, 0), pixel(20, 60));
		assertEquals(new RGB(255, 255, 255), pixel(20, 80));
	}

	@Test
	public void fixedSemanticPanelEscapesAncestorClipOnScreen() {
		browser.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		show("<main style='position:relative;width:40px;height:40px;overflow:hidden;background:lime'>"
				+ "<aside style='position:fixed;left:120px;top:15px;width:50px;height:25px;background:red'></aside></main>");
		assertEquals(new RGB(0, 255, 0), pixel(10, 20));
		assertEquals("viewport-fixed content escapes the ancestor's 40px clip", new RGB(255, 0, 0), pixel(130, 25));
		assertEquals(new RGB(255, 255, 255), pixel(180, 25));
	}

	@Test
	public void panelPaintingPreservesDisjointCallerRegionsAndTheirHoles() {
		show("<section style='height:80px;overflow:hidden;background:red'>"
				+ "<article style='height:80px;background:blue'></article></section>");
		Image image = new Image(display, 200, 80);
		GC gc = new GC(image);
		Region caller = new Region(display), restored = new Region(display);
		try {
			gc.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
			gc.fillRectangle(0, 0, 200, 80);
			caller.add(0, 0, 20, 80);
			caller.add(180, 0, 20, 80);
			caller.subtract(5, 10, 5, 10);
			gc.setClipping(caller);
			paintWithCallerGC(gc, 200, 80);
			assertTrue(gc.isClipped());
			gc.getClipping(restored);
			assertTrue(restored.contains(2, 20));
			assertTrue(restored.contains(190, 20));
			assertTrue("the disjoint gap must remain", !restored.contains(100, 20));
			assertTrue("the hole inside one island must remain", !restored.contains(7, 15));
			ImageData data = image.getImageData();
			assertEquals(new RGB(0, 255, 0), data.palette.getRGB(data.getPixel(100, 20)));
			assertEquals(new RGB(0, 255, 0), data.palette.getRGB(data.getPixel(7, 15)));
			assertEquals(new RGB(0, 0, 255), data.palette.getRGB(data.getPixel(2, 20)));
		} finally {
			restored.dispose();
			caller.dispose();
			gc.dispose();
			image.dispose();
		}
	}

	@Test
	public void panelPaintingRestoresTheInitialCallerClipArea() {
		show("<section style='height:40px;overflow:clip'><article style='height:80px;background:blue'></article></section>");
		Image image = new Image(display, 200, 80);
		GC gc = new GC(image);
		Region initial = new Region(display), restored = new Region(display);
		try {
			assertTrue(!gc.isClipped());
			gc.getClipping(initial);
			paintWithCallerGC(gc, 200, 80);
			gc.getClipping(restored);
			assertEquals(initial.getBounds(), restored.getBounds());
			restored.subtract(initial);
			assertTrue("restored clip cannot add any pixels", restored.isEmpty());
			gc.getClipping(restored);
			initial.subtract(restored);
			assertTrue("restored clip cannot remove any pixels", initial.isEmpty());
			// GTK's null reset restores the original area but may report isClipped()==true afterward.
			gc.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
			gc.fillRectangle(120, 60, 10, 10);
			ImageData data = image.getImageData();
			assertEquals("caller can draw beyond the previous panel clip", new RGB(0, 255, 0), data.palette.getRGB(data.getPixel(125, 65)));
		} finally {
			restored.dispose();
			initial.dispose();
			gc.dispose();
			image.dispose();
		}
	}

	private void paintWithCallerGC(GC gc, int width, int height) {
		Event event = new Event();
		event.gc = gc;
		event.width = width;
		event.height = height;
		browser.notifyListeners(SWT.Paint, event);
	}

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setLayout(new FillLayout());
		browser = new DrawnBrowser(shell, SWT.NONE);
		shell.setBounds(40, 40, 320, 240);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	private void show(String html) {
		browser.setText(html);
		shell.open();
		settle();
	}

	private void settle() {
		// GTK schedules drawing on its frame clock. An empty event queue is not a completed screen frame.
		boolean[] elapsed = { false };
		display.timerExec(100, () -> elapsed[0] = true);
		while (!elapsed[0]) {
			if (!display.readAndDispatch()) display.sleep();
			browser.update();
		}
	}

	private ImageData capture() {
		Rectangle area = browser.getClientArea();
		Point origin = browser.toDisplay(0, 0);
		Image image = new Image(display, area.width, area.height);
		GC gc = new GC(display);
		try {
			gc.copyArea(image, origin.x, origin.y);
			return image.getImageData();
		} finally {
			gc.dispose();
			image.dispose();
		}
	}

	private RGB pixel(int x, int y) {
		ImageData image = capture();
		return image.palette.getRGB(image.getPixel(x, y));
	}

	private static int[] rgbPixels(ImageData image, int contentWidth) {
		assertTrue("content width must fit the capture", contentWidth > 0 && contentWidth <= image.width);
		int[] pixels = new int[contentWidth * image.height];
		for (int y = 0; y < image.height; y++) for (int x = 0; x < contentWidth; x++) {
			RGB rgb = image.palette.getRGB(image.getPixel(x, y));
			pixels[y * contentWidth + x] = rgb.red << 16 | rgb.green << 8 | rgb.blue;
		}
		return pixels;
	}

	private static String context(String z) {
		return "<div style='position:relative;z-index:" + z + ";width:100px;height:100px'>"
				+ "<div style='position:absolute;z-index:1000;left:0;top:0;width:80px;height:80px;background:red'>"
				+ "<a href='child'>child</a></div></div>"
				+ "<div style='position:absolute;z-index:1;left:0;top:0;width:80px;height:80px;background:blue'>"
				+ "<a href='sibling'>sibling</a></div>";
	}

	@Test
	public void nestedContextCannotEscapeButAutoCan() {
		show(context("0"));
		assertEquals(new RGB(0, 0, 255), pixel(60, 40));
		show(context("auto"));
		assertEquals(new RGB(255, 0, 0), pixel(60, 40));
	}

	@Test
	public void pointerUsesTheSameStackingOrderAsPaint() {
		List<String> targets = new ArrayList<>();
		browser.addLocationListener(new LocationListener() {
			@Override public void changing(LocationEvent event) { targets.add(event.location); event.doit = false; }
			@Override public void changed(LocationEvent event) { }
		});
		show(context("0"));
		click(3, 14);
		assertEquals(List.of("sibling"), targets);
		show(context("auto"));
		click(3, 14);
		assertEquals(List.of("sibling", "child"), targets);
	}

	private void click(int x, int y) {
		Event event = new Event();
		event.button = 1;
		event.x = x;
		event.y = y;
		browser.notifyListeners(SWT.MouseUp, event);
	}

	@Test
	public void emptyPositionedBoxesPaintAndRetainEqualZTreeOrder() {
		show("<div style='position:absolute;z-index:4;left:5px;top:7px;width:90px;height:60px;background:red'></div>"
				+ "<div style='position:absolute;z-index:4;left:20px;top:20px;width:50px;height:30px;background:lime'></div>");
		assertEquals(new RGB(255, 0, 0), pixel(10, 10));
		assertEquals(new RGB(0, 255, 0), pixel(30, 30));
	}

	@Test
	public void negativeChildPaintsAboveItsContextBackground() {
		show("<div style='position:relative;z-index:0;width:120px;height:80px;background:lime'>parent"
				+ "<div style='position:absolute;z-index:-4;left:10px;top:10px;width:90px;height:60px;background:red'></div></div>");
		assertEquals(new RGB(0, 255, 0), pixel(110, 70));
		assertEquals(new RGB(255, 0, 0), pixel(70, 40));
	}

	@Test
	public void relativeOffsetPreservesTheFollowingFlowBox() {
		show("<div style='position:relative;left:30px;top:20px;width:60px;height:40px;background:red'></div>"
				+ "<div style='width:120px;height:30px;background:lime'></div>");
		// The following green box starts at y=40. The positioned red box paints above it until y=60.
		assertEquals(new RGB(0, 255, 0), pixel(10, 45));
		assertEquals(new RGB(255, 0, 0), pixel(40, 45));
		assertEquals(new RGB(0, 255, 0), pixel(40, 65));
	}

	@Test
	public void overflowVisibleDoesNotClipAtTheContextBoundary() {
		show("<div style='position:relative;z-index:0;left:20px;width:40px;height:50px;background:lime'>"
				+ "<div style='position:absolute;left:30px;top:10px;width:70px;height:30px;background:red'></div></div>");
		assertEquals(new RGB(255, 0, 0), pixel(100, 25));
	}

	@Test
	public void fixedBoxStaysPutAndScrollPixelsEqualAFullRepaint() {
		String html = "<div style='position:fixed;z-index:10;left:10px;top:10px;width:50px;height:30px;background:red'></div>"
				+ "<p style='background:lime'>long document</p>".repeat(100);
		show(html);
		for (int scroll : new int[] { 20, 55, 10, 200 }) {
			browser.setScrollY(scroll);
			settle();
			assertEquals(scroll, browser.getScrollY());
			assertEquals(new RGB(255, 0, 0), pixel(20, 20));
			ImageData moved = capture();
			browser.redraw();
			settle();
			ImageData repainted = capture();
			assertEquals("capture width", moved.width, repainted.width);
			assertEquals("capture height", moved.height, repainted.height);
			org.eclipse.swt.widgets.ScrollBar bar = browser.getVerticalBar();
			assertEquals("native bar selection " + scroll, scroll, bar.getSelection());
			int contentWidth = moved.width;
			if (bar.getVisible() && browser.getClientArea().width == browser.getSize().x) {
				contentWidth -= bar.getSize().x; // GTK overlay bar paints inside the client.
			}
			assertArrayEquals("scroll " + scroll, rgbPixels(moved, contentWidth), rgbPixels(repainted, contentWidth));
		}
	}

	@Test
	public void fixedBottomRightReanchorsOnResize() {
		show("<div style='position:fixed;right:7px;bottom:9px;width:40px;height:30px;background:red'></div>");
		for (int size : new int[] { 320, 380, 280 }) {
			shell.setSize(size, size - 70);
			settle();
			Rectangle area = browser.getClientArea();
			assertTrue(area.width > 100 && area.height > 100);
			assertEquals(new RGB(255, 0, 0), pixel(area.width - 15, area.height - 15));
		}
	}

	@Test
	public void positionedBorderAndPaddingSurroundTheContentSizedBox() {
		show("<div style='position:absolute;z-index:0;left:20px;top:15px;width:60px;height:40px;"
				+ "margin:5px 7px 9px 11px;padding:8px;border:4px solid blue;background:red'>"
				+ "<div style='position:absolute;z-index:-1;left:0;top:0;width:16px;height:12px;background:lime'></div></div>");
		// Border box starts at (31,20), spans84x64, and contains a padding-box anchored child at (35,24).
		assertEquals(new RGB(0, 0, 255), pixel(32, 21));
		assertEquals(new RGB(0, 255, 0), pixel(40, 29));
		assertEquals(new RGB(255, 0, 0), pixel(70, 40));
		assertEquals(new RGB(0, 0, 255), pixel(113, 82));
	}

}
