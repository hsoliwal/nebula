/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser;

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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** The drawn browser widget: viewport painting, scrolling by moving pixels, links. */
public class DrawnBrowserTest {

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

	private void open(String html) {
		browser.setText(html);
		shell.open();
		browser.setFocus();
		settle();
	}

	private void settle() {
		for (int i = 0; i < 50; i++) {
			while (display.readAndDispatch()) {
				// drain
			}
			browser.update();
		}
	}

	private static String paragraphs(int n) {
		StringBuilder html = new StringBuilder();
		for (int i = 0; i < n; i++) html.append("<p>Paragraph ").append(i).append(" of the drawn browser.</p>");
		return html.toString();
	}

	@Test
	public void scrollingRepaintsOnlyTheExposedBand() {
		open(paragraphs(300));
		int height = browser.getClientArea().height;
		List<int[]> clips = new ArrayList<>();
		browser.addListener(SWT.Paint, e -> clips.add(new int[] { e.y, e.height }));
		browser.setScrollY(24);
		browser.update();
		assertEquals(24, browser.getScrollY());
		assertTrue("paints", !clips.isEmpty());
		int painted = clips.stream().mapToInt(c -> c[1]).sum();
		assertTrue("painted " + painted + " rows for a 24 pixel scroll", painted <= 24 + 2);
		for (int[] clip : clips) {
			assertTrue("scrolling down exposes the bottom band, painted y " + clip[0], clip[0] >= height - 24 - 1 && clip[0] + clip[1] <= height);
		}
		clips.clear();
		browser.setScrollY(10);
		browser.update();
		assertTrue("paints", !clips.isEmpty());
		for (int[] clip : clips) {
			assertTrue("scrolling up exposes the top band, painted y " + clip[0] + " h " + clip[1], clip[0] == 0 && clip[1] <= 14 + 1);
		}
	}

	@Test
	public void linkClickSendsChangingWithTheTarget() {
		open("<p><a href=\"next.html\">LINKLINKLINKLINKLINK</a></p><p>plain</p>");
		List<String> calls = new ArrayList<>();
		browser.addLocationListener(new LocationListener() {
			@Override
			public void changing(LocationEvent event) {
				calls.add("changing " + event.location);
			}

			@Override
			public void changed(LocationEvent event) {
				calls.add("changed " + event.location);
			}
		});
		click(6, 8 + 6);
		settle();
		assertEquals("at about:blank a relative page cannot load: no changed", List.of("changing next.html"), calls);
		calls.clear();
		click(6, 200);
		assertEquals(List.of(), calls);
	}

	private void click(int x, int y) {
		Event e = new Event();
		e.x = x;
		e.y = y;
		e.button = 1;
		browser.notifyListeners(SWT.MouseUp, e);
	}

	@Test
	public void endScrollsToTheBottomAndHomeBack() {
		open(paragraphs(100));
		Event end = new Event();
		end.keyCode = SWT.END;
		browser.notifyListeners(SWT.KeyDown, end);
		int bottom = browser.getVerticalBar().getMaximum() - browser.getVerticalBar().getThumb();
		assertEquals(bottom, browser.getScrollY());
		assertTrue(bottom > 1000);
		Event home = new Event();
		home.keyCode = SWT.HOME;
		browser.notifyListeners(SWT.KeyDown, home);
		assertEquals(0, browser.getScrollY());
	}

	@Test
	public void setTextKeepsTheSourceAndStartsAtTheTop() {
		open(paragraphs(100));
		browser.setScrollY(300);
		browser.setText("<p>new</p>");
		assertEquals("<p>new</p>", browser.getText());
		assertEquals(0, browser.getScrollY());
	}

	@Test
	public void anImageLoadsWhenShownAndIsPainted() {
		open("<img src=\"" + org.eclipse.nebula.widgets.drawnbrowser.internal.ImageStoreTest.dataUri(30, 20, 0xFF0000) + "\">");
		long end = System.currentTimeMillis() + 10_000;
		int red = 0;
		while (red < 400 && System.currentTimeMillis() < end) {
			settle();
			Image shot = new Image(display, 60, 40);
			GC gc = new GC(browser);
			gc.copyArea(shot, 0, 0);
			gc.dispose();
			ImageData data = shot.getImageData();
			shot.dispose();
			red = 0;
			for (int y = 0; y < 40; y++) for (int x = 0; x < 60; x++) {
				org.eclipse.swt.graphics.RGB rgb = data.palette.getRGB(data.getPixel(x, y));
				if (rgb.red > 200 && rgb.green < 60 && rgb.blue < 60) red++;
			}
		}
		assertTrue("red pixels " + red, red >= 30 * 20 - 40);
	}

	@Test
	public void aBlockBackgroundIsPaintedAcrossTheView() {
		open("<p style=\"background-color: #00ff00\">green row</p><p>plain</p>");
		Image shot = new Image(display, 300, 30);
		GC gc = new GC(browser);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int green = 0;
		for (int y = 0; y < 30; y++) for (int x = 0; x < 300; x++) {
			org.eclipse.swt.graphics.RGB rgb = data.palette.getRGB(data.getPixel(x, y));
			if (rgb.green > 200 && rgb.red < 60 && rgb.blue < 60) green++;
		}
		assertTrue("green pixels " + green, green > 300 * 20);
	}

	@Test
	public void textIsPaintedInTheFirstBlock() {
		open("<h1>Drawn</h1>");
		Image shot = new Image(display, 200, 60);
		GC gc = new GC(browser);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int background = data.getPixel(199, 59), differing = 0;
		for (int y = 0; y < 60; y++) for (int x = 0; x < 200; x++) if (data.getPixel(x, y) != background) differing++;
		assertTrue("text pixels " + differing, differing > 50);
	}
}
