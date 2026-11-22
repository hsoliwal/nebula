/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
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

/** Real screen pixels and link input for layout-item stacking and row-spanning cell backgrounds. */
public class DrawnBrowserStackingCompositionTest {

	private Display display;
	private Shell shell;
	private DrawnBrowser browser;
	private final List<String> targets = new ArrayList<>();

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setLayout(new FillLayout());
		browser = new DrawnBrowser(shell, SWT.NONE);
		browser.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		browser.addLocationListener(LocationListener.changingAdapter(event -> {
			targets.add(event.location);
			event.doit = false;
		}));
		shell.setBounds(40, 40, 320, 240);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void staticFlexItemStackLevelControlsVisiblePixelsAndLinkPicking() throws IOException {
		assertItemStacking("flex", "display:flex");
	}

	@Test
	public void staticGridItemStackLevelControlsVisiblePixelsAndLinkPicking() throws IOException {
		assertItemStacking("grid", "display:grid;grid-template-columns:100px 60px");
	}

	@Test
	public void laterRowBackgroundStaysBelowTheLowerPartOfASpanningCell() throws IOException {
		show("<div style='position:relative;width:160px'><table>"
				+ "<tr style='background:red'><td rowspan='2' style='height:80px;background:blue'>span</td>"
				+ "<td style='height:40px'>top</td></tr><tr style='background:lime'><td style='height:40px'>last</td></tr>"
				+ "</table></div>");
		ImageData screen = capture("table-rowspan");
		assertEquals("upper part of the spanning cell", new RGB(0, 0, 255), pixel(screen, 1, 10));
		assertEquals("second row must not cover the spanning cell", new RGB(0, 0, 255), pixel(screen, 1, 60));
		assertEquals("capture includes the surrounding Canvas", new RGB(255, 255, 255), pixel(screen, 250, 180));
	}

	private void assertItemStacking(String label, String containerStyle) throws IOException {
		String template = "<div style='" + containerStyle + ";width:160px'>"
				+ "<div style='flex:1;z-index:%s;height:60px;background:red'><a href='item'>item</a></div>"
				+ "<div>other</div></div>"
				+ "<div style='position:absolute;left:0;top:0;width:80px;height:60px;z-index:1;background:blue'>"
				+ "<a href='overlay'>overlay</a></div>";
		show(String.format(template, "auto"));
		ImageData automatic = capture(label + "-auto");
		assertEquals("the explicit external level covers an automatic item", new RGB(0, 0, 255), pixel(automatic, 60, 40));
		click(10, 14);
		assertEquals(List.of("overlay"), targets);

		show(String.format(template, "3"));
		ImageData integer = capture(label + "-integer");
		assertEquals("a static item with integer z-index paints above the external level", new RGB(255, 0, 0), pixel(integer, 60, 40));
		assertEquals("the screenshot is aligned to the Canvas", new RGB(255, 255, 255), pixel(integer, 250, 180));
		click(10, 14);
		assertEquals("input follows the same new stack level", List.of("overlay", "item"), targets);
	}

	private void show(String html) {
		browser.setText(html);
		shell.open();
		boolean[] elapsed = { false };
		// Wait for the native frame clock, not merely an empty SWT event queue.
		display.timerExec(100, () -> elapsed[0] = true);
		while (!elapsed[0]) {
			if (!display.readAndDispatch()) display.sleep();
			browser.update();
		}
	}

	private void click(int x, int y) {
		Event event = new Event();
		event.button = 1;
		event.x = x;
		event.y = y;
		browser.notifyListeners(SWT.MouseUp, event);
	}

	private ImageData capture(String name) throws IOException {
		Rectangle area = browser.getClientArea();
		assertTrue("the test Canvas is visible at the expected size", area.width > 250 && area.height > 180 && browser.isVisible());
		Point origin = browser.toDisplay(0, 0);
		Image image = new Image(display, area.width, area.height);
		GC gc = new GC(display);
		ImageData screen;
		try {
			gc.copyArea(image, origin.x, origin.y);
			screen = image.getImageData();
		} finally {
			gc.dispose();
			image.dispose();
		}
		String directory = System.getProperty("drawnbrowser.stacking.captureDir");
		if (directory != null) {
			Path output = Path.of(directory);
			Files.createDirectories(output);
			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] { screen };
			loader.save(output.resolve(name + ".png").toString(), SWT.IMAGE_PNG);
		}
		return screen;
	}

	private static RGB pixel(ImageData image, int x, int y) {
		return image.palette.getRGB(image.getPixel(x, y));
	}
}
