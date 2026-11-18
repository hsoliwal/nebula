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

	private static int[] rgbPixels(ImageData image) {
		int[] pixels = new int[image.width * image.height];
		for (int y = 0; y < image.height; y++) for (int x = 0; x < image.width; x++) {
			RGB rgb = image.palette.getRGB(image.getPixel(x, y));
			pixels[y * image.width + x] = rgb.red << 16 | rgb.green << 8 | rgb.blue;
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
			assertArrayEquals("scroll " + scroll, rgbPixels(moved), rgbPixels(capture()));
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
