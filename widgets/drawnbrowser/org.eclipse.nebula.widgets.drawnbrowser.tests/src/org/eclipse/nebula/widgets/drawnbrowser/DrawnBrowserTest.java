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
		List<Integer> clips = new ArrayList<>();
		browser.addListener(SWT.Paint, e -> clips.add(e.height));
		browser.setScrollY(24);
		browser.update();
		assertEquals(24, browser.getScrollY());
		assertTrue("paints " + clips, !clips.isEmpty());
		int painted = clips.stream().mapToInt(Integer::intValue).sum();
		assertTrue("painted " + painted + " rows for a 24 pixel scroll", painted <= 24 + 2);
	}

	@Test
	public void linkClickSendsChangingThenChangedAndCanBeVetoed() {
		open("<p><a href=\"next.html\">LINKLINKLINKLINKLINK</a></p><p>plain</p>");
		List<String> calls = new ArrayList<>();
		LocationListener record = new LocationListener() {
			@Override
			public void changing(LocationEvent event) {
				calls.add("changing " + event.location);
			}

			@Override
			public void changed(LocationEvent event) {
				calls.add("changed " + event.location);
			}
		};
		browser.addLocationListener(record);
		click(6, 8 + 6);
		assertEquals(List.of("changing next.html", "changed next.html"), calls);
		calls.clear();
		browser.addLocationListener(LocationListener.changingAdapter(e -> e.doit = false));
		click(6, 8 + 6);
		assertEquals(List.of("changing next.html"), calls);
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
