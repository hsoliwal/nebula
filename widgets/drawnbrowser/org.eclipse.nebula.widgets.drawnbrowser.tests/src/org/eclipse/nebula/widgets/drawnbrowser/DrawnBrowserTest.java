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
	public void aBoxPaintsItsBorderAndItsBackgroundInsideItsMargins() {
		open("<p style='margin:10px 30px 10px 30px; padding:10px; border:4px solid #0000ff; background-color:#ff0000'>boxed</p>");
		int[] blue = pixels(new org.eclipse.swt.graphics.RGB(0, 0, 255), 80);
		int[] red = pixels(new org.eclipse.swt.graphics.RGB(255, 0, 0), 80);
		org.eclipse.swt.graphics.Rectangle area = browser.getClientArea();
		assertTrue("border pixels " + blue[0], blue[0] > 2 * (area.width - 60) * 4 / 2);
		assertTrue("the border ends at the right margin: " + blue[1], blue[1] == area.width - 30 - 1);
		assertTrue("background pixels " + red[0], red[0] > 100);
		assertTrue("the background stays inside the border: " + red[1], red[1] < area.width - 30 - 4);
	}

	@Test
	public void positionedBoxesPaintInZIndexOrderAroundTheFlow() {
		open("<p style='background-color:#00ff00'>flow row</p>"
				+ "<div style='position:absolute; top:60px; left:20px; z-index:2; background-color:#ff0000; padding:5px'>a</div>"
				+ "<div style='position:absolute; top:70px; left:30px; z-index:1; background-color:#0000ff; padding:40px'>b</div>"
				+ "<div style='position:absolute; top:0px; left:150px; z-index:-1; background-color:#0000ff; padding:30px'>under</div>");
		ImageData shown = shot();
		// boxes start a block gap (8 px) below their top offset; an absolute box keeps the width of its flow place
		assertEquals("z-index 2 over z-index 1, whatever the document order", new org.eclipse.swt.graphics.RGB(255, 0, 0), rgbAt(shown, 45, 82));
		assertEquals("z-index 1 shows where 2 does not", new org.eclipse.swt.graphics.RGB(0, 0, 255), rgbAt(shown, 45, 150));
		assertEquals("a negative z-index is under the flow", new org.eclipse.swt.graphics.RGB(0, 255, 0), rgbAt(shown, 170, 15));
	}

	@Test
	public void aRelativeBoxIsShiftedAndAFixedBoxStaysThroughAScroll() {
		StringBuilder html = new StringBuilder("<div style='position:fixed; top:0px; left:0px; background-color:#ff0000; padding:6px'>bar</div>"
				+ "<p style='position:relative; left:60px; background-color:#0000ff; padding:4px'>shifted</p>");
		for (int i = 0; i < 60; i++) html.append("<p>line ").append(i).append("</p>");
		open(html.toString());
		ImageData before = shot();
		assertEquals(new org.eclipse.swt.graphics.RGB(255, 0, 0), rgbAt(before, 3, 12));
		int shiftedY = findRow(before, new org.eclipse.swt.graphics.RGB(0, 0, 255));
		assertTrue("the relative box is drawn", shiftedY >= 0);
		assertTrue("shifted right by 60 px", !rgbAt(before, 30, shiftedY).equals(new org.eclipse.swt.graphics.RGB(0, 0, 255))
				&& rgbAt(before, 80, shiftedY).equals(new org.eclipse.swt.graphics.RGB(0, 0, 255)));
		browser.setScrollY(100);
		settle();
		assertEquals("the fixed bar is still at the top of the view", new org.eclipse.swt.graphics.RGB(255, 0, 0), rgbAt(shot(), 3, 12));
	}

	@Test
	public void aClickHitsTheTopmostPositionedLink() {
		List<String> changing = new ArrayList<>();
		browser.addLocationListener(new LocationListener() {
			@Override
			public void changing(LocationEvent event) {
				changing.add(event.location);
				event.doit = false;
			}

			@Override
			public void changed(LocationEvent event) {
			}
		});
		open("<p><a href='http://flow.example/'>flow link flow link flow link</a></p>"
				+ "<div style='position:absolute; top:0px; left:0px; z-index:1'><a href='http://top.example/'>top link top link</a></div>");
		click(30, 16);
		assertEquals("[http://top.example/]", changing.toString());
	}

	/* the view's pixels */
	private ImageData shot() {
		org.eclipse.swt.graphics.Rectangle area = browser.getClientArea();
		Image image = new Image(display, area.width, area.height);
		GC gc = new GC(browser);
		gc.copyArea(image, 0, 0);
		gc.dispose();
		ImageData data = image.getImageData();
		image.dispose();
		return data;
	}

	private static org.eclipse.swt.graphics.RGB rgbAt(ImageData data, int x, int y) {
		return data.palette.getRGB(data.getPixel(x, y));
	}

	/* the first row with a pixel of the colour, or -1 */
	private static int findRow(ImageData data, org.eclipse.swt.graphics.RGB rgb) {
		for (int y = 0; y < data.height; y++) {
			for (int x = 0; x < data.width; x++) if (rgbAt(data, x, y).equals(rgb)) return y;
		}
		return -1;
	}

	/* the n-th child button (in the order they were made) with the style bit, or null */
	private org.eclipse.swt.widgets.Button button(int style, int n) {
		List<org.eclipse.swt.widgets.Button> buttons = new ArrayList<>();
		for (org.eclipse.swt.widgets.Button b : children(org.eclipse.swt.widgets.Button.class)) if ((b.getStyle() & style) != 0) buttons.add(b);
		buttons.sort(java.util.Comparator.comparingInt(b -> b.getBounds().y));
		return n < buttons.size() ? buttons.get(n) : null;
	}

	/* the browser's child widgets of a type */
	private <T> List<T> children(Class<T> type) {
		List<T> out = new ArrayList<>();
		for (org.eclipse.swt.widgets.Control child : browser.getChildren()) if (type.isInstance(child)) out.add(type.cast(child));
		return out;
	}

	@Test
	public void formControlsAreNativeWidgetsAndSubmitTheirForm() {
		List<String> changing = new ArrayList<>();
		browser.addLocationListener(new LocationListener() {
			@Override
			public void changing(LocationEvent event) {
				changing.add(event.location);
				event.doit = false;
			}

			@Override
			public void changed(LocationEvent event) {
			}
		});
		open("<form action='http://example.test/search'><input name=q value=hi><input type=password name=pw><input type=checkbox name=agree checked><input type=radio name=c value=a><input type=radio name=c value=b checked><input type=hidden name=h value=x><select name=s><option>one<option selected>two</select><textarea name=t rows=3 cols=10>\nline1\nline2</textarea><button name=go value=1>Go now</button></form><p>after <input name=outside></p>");
		List<org.eclipse.swt.widgets.Text> texts = children(org.eclipse.swt.widgets.Text.class);
		assertTrue("fields and the text area in view are native: " + texts.size(), texts.size() >= 3);
		org.eclipse.swt.widgets.Text q = texts.get(0);
		assertEquals("hi", q.getText());
		assertTrue(q.getVisible());
		assertEquals("the field sits in its block", org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlLayout.BLOCK_GAP, q.getBounds().y);
		q.setText("typed words");
		org.eclipse.swt.widgets.Button agree = button(SWT.CHECK, 0), radioA = button(SWT.RADIO, 0), radioB = button(SWT.RADIO, 1);
		assertTrue(agree.getSelection());
		assertTrue(radioB.getSelection());
		radioA.setSelection(true);
		radioA.notifyListeners(SWT.Selection, new Event());
		assertTrue("radios of one name exclude each other", !radioB.getSelection());
		assertEquals("the submit button is below the view: not made yet", null, button(SWT.PUSH, 0));
		browser.setScrollY(100000);
		settle();
		org.eclipse.swt.widgets.Button go = button(SWT.PUSH, 0);
		assertEquals("Go now", go.getText());
		go.notifyListeners(SWT.Selection, new Event());
		assertEquals("[http://example.test/search?q=typed+words&pw=&agree=on&c=a&h=x&s=two&t=line1%0Aline2&go=1]", changing.toString());
		open("<p>another page</p>");
		assertEquals("a new page disposes the old widgets", 0, browser.getChildren().length);
	}

	@Test
	public void aControlBelowTheViewIsMadeWhenScrolledTo() {
		StringBuilder html = new StringBuilder();
		for (int i = 0; i < 60; i++) html.append("<p>line ").append(i).append("</p>");
		html.append("<input name=far value=deep>");
		open(html.toString());
		assertEquals("not made before it is in view", 0, browser.getChildren().length);
		browser.setScrollY(100000);
		settle();
		List<org.eclipse.swt.widgets.Text> texts = children(org.eclipse.swt.widgets.Text.class);
		assertEquals(1, texts.size());
		assertEquals("deep", texts.get(0).getText());
		assertTrue(texts.get(0).getVisible());
		int y = texts.get(0).getBounds().y;
		assertTrue("inside the view: " + y, y >= 0 && y < browser.getClientArea().height);
		browser.setScrollY(0);
		settle();
		assertTrue("hidden when out of view", !texts.get(0).getVisible());
	}

	@Test
	public void aBandBelowASpanningCellsFirstRowRepaintsIt() {
		open("<table><tr><td rowspan=2 style='background-color:#ff0000'>tall cell</td><td>first row</td></tr>"
				+ "<tr><td>second row</td></tr></table>");
		ImageData full = shot();
		org.eclipse.swt.graphics.RGB red = new org.eclipse.swt.graphics.RGB(255, 0, 0);
		int bottom = -1;
		for (int y = 0; y < full.height; y++) if (rgbAt(full, 4, y).equals(red)) bottom = y;
		assertTrue("the spanning cell is painted", bottom > 0);
		int band = bottom - 4; // in the second row
		GC gc = new GC(browser);
		gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		gc.fillRectangle(0, band, browser.getClientArea().width, 3);
		gc.dispose();
		browser.redraw(0, band, browser.getClientArea().width, 3, false);
		settle();
		assertEquals("only the band was damaged, and the spanning cell is painted in it", red, rgbAt(shot(), 4, band + 1));
	}

	@Test
	public void flexItemsPaintSideBySideWithTheirBoxes() {
		open("<div style='display:flex; gap:20px'><div style='flex-grow:1; background-color:#ff0000'>left</div>"
				+ "<div style='flex-grow:1; background-color:#0000ff'>right</div></div><p style='display:none'>hidden text</p>");
		ImageData shown = shot();
		int width = browser.getClientArea().width, y = 6;
		org.eclipse.swt.graphics.RGB red = new org.eclipse.swt.graphics.RGB(255, 0, 0), blue = new org.eclipse.swt.graphics.RGB(0, 0, 255);
		assertEquals("the first half is red", red, rgbAt(shown, width / 4, y));
		assertEquals("the second half blue", blue, rgbAt(shown, 3 * width / 4, y));
		assertTrue("the gap between them is neither", !rgbAt(shown, width / 2, y).equals(red) && !rgbAt(shown, width / 2, y).equals(blue));
	}

	@Test
	public void gridItemsPaintInTheirTracks() {
		open("<div style='display:grid; grid-template-columns: 1fr 1fr; gap: 0 20px'>"
				+ "<div style='background-color:#ff0000'>a</div><div style='background-color:#0000ff'>b</div>"
				+ "<div style='grid-column: span 2; background-color:#00ff00'>wide</div></div>");
		ImageData shown = shot();
		int width = browser.getClientArea().width;
		org.eclipse.swt.graphics.RGB red = new org.eclipse.swt.graphics.RGB(255, 0, 0), blue = new org.eclipse.swt.graphics.RGB(0, 0, 255),
				green = new org.eclipse.swt.graphics.RGB(0, 255, 0);
		assertEquals("the first track is red", red, rgbAt(shown, width / 4, 6));
		assertEquals("the second blue", blue, rgbAt(shown, 3 * width / 4, 6));
		assertTrue("the column gap is neither", !rgbAt(shown, width / 2, 6).equals(red) && !rgbAt(shown, width / 2, 6).equals(blue));
		int y = 6;
		while (y < 200 && rgbAt(shown, width / 4, y).equals(red)) y++;
		assertEquals("the spanning item fills the second row", green, rgbAt(shown, width / 4, y + 2));
		assertEquals("across the gap", green, rgbAt(shown, width / 2, y + 2));
		assertEquals(green, rgbAt(shown, 3 * width / 4, y + 2));
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
