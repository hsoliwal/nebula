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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Native form geometry, identity and sibling order through the original browser owner. */
public class DrawnBrowserFormPositionTest {

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

	@Test
	public void eachDirectControlKeepsItsFixedCssBoundsAndIdentityThroughScroll() {
		String style = " style='position:fixed;left:17px;top:11px;width:110px;height:28px;z-index:4'";
		String[] controls = {
				"<input value=field" + style + ">",
				"<textarea" + style + ">area</textarea>",
				"<select" + style + "><option>one</option><option>two</option></select>",
				"<button" + style + ">button</button>" };
		for (String control : controls) {
			show(control + longPage());
			assertEquals(control, 1, browser.getChildren().length);
			Control widget = browser.getChildren()[0];
			Rectangle expected = acceptedNativeBounds(widget, new Rectangle(17, 11, 110, 28));
			assertEquals(control, expected, widget.getBounds());
			for (int scroll : new int[] { 120, 500, 0 }) {
				browser.setScrollY(scroll);
				settle();
				assertEquals(scroll, browser.getScrollY());
				assertEquals(control, expected, widget.getBounds());
				assertSame(widget, browser.getChildren()[0]);
				assertTrue(widget.getVisible());
			}
			browser.setText("<p>replacement</p>");
			assertTrue(widget.isDisposed());
		}
	}

	private Rectangle acceptedNativeBounds(Control widget, Rectangle requested) {
		if (!(widget instanceof Combo combo)) return requested;
		// Native Combo may impose a theme minimum. A standalone SWT peer is the size-contract oracle.
		Shell reference = new Shell(display);
		try {
			Combo peer = new Combo(reference, SWT.READ_ONLY | SWT.DROP_DOWN);
			peer.setFont(combo.getFont());
			peer.setItems("one", "two");
			peer.select(0);
			peer.setBounds(requested);
			return peer.getBounds();
		} finally {
			reference.dispose();
		}
	}

	@Test
	public void relativeControlMovesVisuallyWithoutMovingTheFollowingControl() {
		show("<input value=first><input value=second>");
		Rectangle first = field("first").getBounds(), second = field("second").getBounds();
		show("<input value=first style='position:relative;left:23px;top:11px'><input value=second>");
		assertEquals(new Rectangle(first.x + 23, first.y + 11, first.width, first.height), field("first").getBounds());
		assertEquals("relative positioning retains the ordinary flow allocation", second, field("second").getBounds());
	}

	@Test
	public void absoluteControlUsesItsPositionedAncestorsPaddingBox() {
		show("<div style='position:relative;left:40px;top:30px;width:240px;height:120px;"
				+ "border:2px solid blue;padding:3px'><input value=field "
				+ "style='position:absolute;left:7px;top:9px;width:70px;height:22px'></div>");
		assertEquals(new Rectangle(49, 41, 70, 22), field("field").getBounds());
	}

	@Test
	public void trailingInsetsUseTheControlsIntrinsicSizeWithoutBlockGaps() {
		show("<input value=field>");
		Rectangle ordinary = field("field").getBounds();
		for (String position : new String[] { "absolute", "fixed" }) {
			show("<input value=field style='position:" + position + ";right:13px;bottom:17px'>");
			Rectangle area = browser.getClientArea();
			assertEquals(position, new Rectangle(area.width - 13 - ordinary.width,
					area.height - 17 - ordinary.height, ordinary.width, ordinary.height), field("field").getBounds());
		}
	}

	@Test
	public void anAncestorSizeDoesNotBecomeTheNativeControlsOwnSize() {
		show("<input value=field>");
		Rectangle ordinary = field("field").getBounds();
		show("<div style='position:relative;left:40px;top:20px;width:240px;height:100px'>"
				+ "<input value=field></div>");
		Rectangle nested = field("field").getBounds();
		assertEquals(ordinary.width, nested.width);
		assertEquals(ordinary.height, nested.height);
		assertEquals(40, nested.x);
		assertEquals(28, nested.y);
	}

	@Test
	public void laterCreatedLowerControlStaysBelowTheExistingHigherControl() {
		show("<div style='position:fixed;z-index:10;left:30px;top:20px;width:180px;height:70px'>"
				+ "<input value=front></div>"
				+ "<div style='position:absolute;z-index:0;left:30px;top:280px;width:180px;height:70px'>"
				+ "<input value=back></div>" + longPage());
		assertEquals("the offscreen lower control is still lazy", 1, browser.getChildren().length);
		Text front = field("front");
		front.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
		browser.setScrollY(260);
		settle();
		Text back = field("back");
		back.setBackground(display.getSystemColor(SWT.COLOR_RED));
		settle();
		assertEquals(front.getBounds(), back.getBounds());
		assertSame("SWT returns native children from topmost to bottommost", front, browser.getChildren()[0]);
		Rectangle overlap = front.getBounds();
		assertBlue(pixel(overlap.x + overlap.width - 20, overlap.y + overlap.height / 2));
		front.setText("edited");
		browser.setScrollY(0);
		settle();
		assertFalse(back.getVisible());
		browser.setScrollY(260);
		settle();
		assertSame(front, field("edited"));
		assertSame(back, field("back"));
		assertSame(front, browser.getChildren()[0]);
	}

	private Text field(String value) {
		for (Control child : browser.getChildren()) {
			if (child instanceof Text text && value.equals(text.getText())) return text;
		}
		throw new AssertionError("missing native field " + value);
	}

	@Test
	public void laterCreatedHigherControlMovesAboveTheExistingLowerControl() {
		show("<div style='position:fixed;z-index:0;left:30px;top:20px;width:180px;height:70px'>"
				+ "<input value=back></div>"
				+ "<div style='position:absolute;z-index:10;left:30px;top:280px;width:180px;height:70px'>"
				+ "<input value=front></div>" + longPage());
		assertEquals("the offscreen higher control is still lazy", 1, browser.getChildren().length);
		Text back = field("back");
		back.setBackground(display.getSystemColor(SWT.COLOR_RED));
		browser.setScrollY(260);
		settle();
		Text front = field("front");
		front.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
		settle();
		assertEquals(back.getBounds(), front.getBounds());
		assertSame("creation time must not override the paint plan", front, browser.getChildren()[0]);
		Rectangle overlap = front.getBounds();
		assertBlue(pixel(overlap.x + overlap.width - 20, overlap.y + overlap.height / 2));
		browser.setScrollY(0);
		settle();
		assertFalse(front.getVisible());
		browser.setScrollY(260);
		settle();
		assertSame(front, field("front"));
		assertSame(back, field("back"));
		assertSame(front, browser.getChildren()[0]);
	}

	private static String longPage() {
		return "<p>long document content</p>".repeat(100);
	}

	private static void assertBlue(RGB sample) {
		// Native Text may blend with its theme; dominant blue distinguishes the front control from red, white and black.
		assertTrue("the blue control must cover the red control: " + sample,
				sample.blue >= 240 && sample.blue - sample.red >= 128 && sample.blue - sample.green >= 128);
	}

	private void show(String html) {
		browser.setText(html);
		shell.open();
		settle();
	}

	private void settle() {
		boolean[] elapsed = { false };
		display.timerExec(100, () -> elapsed[0] = true);
		while (!elapsed[0]) {
			if (!display.readAndDispatch()) display.sleep();
			browser.update();
		}
	}

	private RGB pixel(int x, int y) {
		Point origin = browser.toDisplay(x, y);
		Image image = new Image(display, 1, 1);
		GC gc = new GC(display);
		try {
			gc.copyArea(image, origin.x, origin.y);
			ImageData data = image.getImageData();
			return data.palette.getRGB(data.getPixel(0, 0));
		} finally {
			gc.dispose();
			image.dispose();
		}
	}
}
