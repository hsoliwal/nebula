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

	/* the view's pixels as the window shows them */
	private int[] capture() {
		org.eclipse.swt.graphics.Rectangle area = browser.getClientArea();
		Image shot = new Image(display, area.width, area.height);
		GC gc = new GC(browser);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int[] pixels = new int[area.width * area.height];
		for (int y = 0; y < area.height; y++) data.getPixels(0, y, area.width, pixels, y * area.width);
		return pixels;
	}

	/* the first row (of the view) where two captures differ, or -1 */
	private int firstDifferentRow(int[] a, int[] b) {
		int width = browser.getClientArea().width;
		for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return i / width;
		return -1;
	}

	/**
	 * The backing store's delta path (the kept pixels moved, only the exposed band painted) must leave the window
	 * exactly as a full repaint at the new position would: scrolling down, up, and by less than a line. With
	 * {@code -Dswt.widget.mode.DrawnBrowser=maya} the same test covers maya's retained back buffer.
	 */
	@Test
	public void scrolledPixelsEqualAFullRepaint() {
		StringBuilder html = new StringBuilder();
		for (int i = 0; i < 80; i++) {
			html.append("<p style=\"background-color: #").append(String.format("%06x", (i * 0x2F1B37) & 0xFFFFFF)).append("\">Row ").append(i)
					.append(" <b>bold</b> <a href=\"x\">link</a></p>");
		}
		open(html.toString());
		for (int target : new int[] { 24, 31, 10, 9, 200, 150 }) {
			browser.setScrollY(target);
			settle();
			int[] moved = capture();
			browser.redraw();
			settle();
			int[] full = capture();
			assertEquals("scrolled to " + target + ": first row that differs from a full repaint", -1, firstDifferentRow(moved, full));
		}
	}

	@Test
	public void findSelectsScrollsToAndPaintsTheMatch() {
		open(paragraphs(300) + "<p>a needle here</p>");
		int at = browser.findAndSelect(-1, "NEEDLE", true, false, false);
		assertTrue("found at " + at, at > 0);
		assertEquals("needle", browser.getSelectionText());
		assertEquals(new org.eclipse.swt.graphics.Point(at, at + 6), browser.getSelection());
		settle();
		assertTrue("scrolled to the match: " + browser.getScrollY(), browser.getScrollY() > 1000);
		org.eclipse.swt.graphics.RGB selection = display.getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB();
		org.eclipse.swt.graphics.Rectangle area = browser.getClientArea();
		Image shot = new Image(display, area.width, area.height);
		GC gc = new GC(browser);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int selected = 0;
		for (int y = 0; y < area.height; y++) {
			for (int x = 0; x < area.width; x++) {
				if (data.palette.getRGB(data.getPixel(x, y)).equals(selection)) selected++;
			}
		}
		assertTrue("selection-coloured pixels " + selected, selected > 20);
		assertEquals("no later match", -1, browser.findAndSelect(at + 1, "needle", true, false, false));
		assertEquals("a miss keeps the selection", "needle", browser.getSelectionText());
		browser.setText("<p>new</p>");
		assertEquals("a new page selects nothing", "", browser.getSelectionText());
	}

	private void mouse(int type, int x, int y, int stateMask) {
		Event e = new Event();
		e.x = x;
		e.y = y;
		e.button = type == SWT.MouseMove ? 0 : 1;
		e.stateMask = stateMask;
		browser.notifyListeners(type, e);
	}

	@Test
	public void aDragSelectsAcrossBlocksAndDoesNotFollowTheLinkItStartedOn() {
		open("<p><a href=\"next.html\">LINKLINKLINKLINK</a> first</p><p>second paragraph</p>");
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
		int secondLine = 8 + 6 + browser.getClientArea().height; // clamped: the drag ends in the last block
		mouse(SWT.MouseDown, 6, 8 + 6, 0);
		mouse(SWT.MouseMove, 60, secondLine / 4, SWT.BUTTON1);
		mouse(SWT.MouseMove, 2000, 400, SWT.BUTTON1);
		mouse(SWT.MouseUp, 2000, 400, SWT.BUTTON1);
		settle();
		assertEquals("the drag selected text, so the link is not followed", List.of(), calls);
		String selected = browser.getSelectionText();
		assertTrue("selected " + selected, selected.endsWith("first\nsecond paragraph"));
		mouse(SWT.MouseDown, 6, 8 + 6, 0);
		mouse(SWT.MouseMove, 40, 8 + 6, SWT.BUTTON1);
		mouse(SWT.MouseUp, 40, 8 + 6, SWT.BUTTON1);
		String inLink = browser.getSelectionText();
		assertTrue("part of the link is selected: " + inLink, !inLink.isEmpty() && "LINKLINKLINKLINK".contains(inLink));
		assertEquals("a drag inside the link does not follow it either", List.of(), calls);
		mouse(SWT.MouseDown, 6, 8 + 6, 0);
		mouse(SWT.MouseUp, 6, 8 + 6, SWT.BUTTON1);
		assertEquals("a click without a drag follows the link", List.of("changing next.html"), calls);
		assertEquals("and selects nothing", "", browser.getSelectionText());
	}

	/* runs the event loop, timers included, for about {@code millis} or until the condition holds */
	private void pump(long millis, java.util.function.BooleanSupplier done) {
		long end = System.currentTimeMillis() + millis;
		while (!done.getAsBoolean() && System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	@Test
	public void aDragHeldBelowTheViewScrollsAndExtendsTheSelectionUntilReleased() {
		open(paragraphs(300));
		int height = browser.getClientArea().height;
		mouse(SWT.MouseDown, 6, 20, 0);
		mouse(SWT.MouseMove, 60, height + 40, SWT.BUTTON1);
		int endAtMove = browser.getSelection().y;
		pump(5000, () -> browser.getScrollY() >= 300);
		assertTrue("auto-scrolled to " + browser.getScrollY(), browser.getScrollY() >= 300);
		org.eclipse.swt.graphics.Point selected = browser.getSelection();
		assertTrue("the selection follows the page past " + endAtMove + ": " + selected, selected.y > endAtMove + 100);
		mouse(SWT.MouseUp, 60, height + 40, SWT.BUTTON1);
		int released = browser.getScrollY();
		pump(400, () -> false);
		assertEquals("released: the page stops", released, browser.getScrollY());
	}

	@Test
	public void selectAllAndCopyPutTheTextOnTheClipboard() {
		open("<p>one</p><p>two <b>three</b></p>");
		org.eclipse.swt.dnd.Clipboard before = new org.eclipse.swt.dnd.Clipboard(display);
		try {
			before.setContents(new Object[] { "before" }, new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });
		} finally {
			before.dispose();
		}
		Event all = new Event();
		all.keyCode = 'a';
		all.stateMask = SWT.MOD1;
		browser.notifyListeners(SWT.KeyDown, all);
		assertEquals("one\ntwo three", browser.getSelectionText());
		Event copy = new Event();
		copy.keyCode = 'c';
		copy.stateMask = SWT.MOD1;
		browser.notifyListeners(SWT.KeyDown, copy);
		org.eclipse.swt.dnd.Clipboard clipboard = new org.eclipse.swt.dnd.Clipboard(display);
		try {
			assertEquals("one\ntwo three", clipboard.getContents(org.eclipse.swt.dnd.TextTransfer.getInstance()));
		} finally {
			clipboard.dispose();
		}
		browser.setSelection(9, 3);
		assertEquals(new org.eclipse.swt.graphics.Point(3, 9), browser.getSelection());
		assertEquals("two th", browser.getSelectionText());
		browser.setSelection(-5, 999);
		assertEquals("clamped to the page", new org.eclipse.swt.graphics.Point(0, 12), browser.getSelection());
	}

	/* the number of pixels of the colour in the top rows of the view, and the rightmost x holding one (-1 none) */
	private int[] pixels(org.eclipse.swt.graphics.RGB rgb, int rows) {
		org.eclipse.swt.graphics.Rectangle area = browser.getClientArea();
		Image shot = new Image(display, area.width, rows);
		GC gc = new GC(browser);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int count = 0, right = -1;
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < area.width; x++) {
				if (data.palette.getRGB(data.getPixel(x, y)).equals(rgb)) {
					count++;
					right = Math.max(right, x);
				}
			}
		}
		return new int[] { count, right };
	}

	@Test
	public void aBorderedTableDrawsItsCellBoxes() {
		org.eclipse.swt.graphics.RGB shadow = display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW).getRGB();
		open("<table><tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></table>");
		int plain = pixels(shadow, 80)[0];
		open("<table border=1><tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></table>");
		int bordered = pixels(shadow, 80)[0];
		// four boxes, each at least as tall as a line of text on both sides
		assertTrue("border pixels " + bordered + " vs " + plain, bordered - plain >= 4 * 2 * 2 * 10);
	}

	@Test
	public void aCellBackgroundFillsOnlyItsCell() {
		open("<table><tr><td style=\"background-color: #ff0000\">red</td><td>plain</td></tr></table>");
		int[] red = pixels(new org.eclipse.swt.graphics.RGB(255, 0, 0), 40);
		assertTrue("red pixels " + red[0], red[0] > 100);
		assertTrue("the fill stops at its column: " + red[1], red[1] < browser.getClientArea().width / 2);
	}

	@Test
	public void aRuleIsPaintedAcrossTheView() {
		open("<hr><p>after</p>");
		org.eclipse.swt.graphics.RGB shadow = display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW).getRGB();
		org.eclipse.swt.graphics.Rectangle area = browser.getClientArea();
		Image shot = new Image(display, area.width, 30);
		GC gc = new GC(browser);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int ruled = 0;
		for (int y = 0; y < 30; y++) {
			for (int x = 0; x < area.width; x++) if (data.palette.getRGB(data.getPixel(x, y)).equals(shadow)) ruled++;
		}
		assertTrue("rule pixels " + ruled, ruled >= (area.width - 2 * 8) * 2);
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
