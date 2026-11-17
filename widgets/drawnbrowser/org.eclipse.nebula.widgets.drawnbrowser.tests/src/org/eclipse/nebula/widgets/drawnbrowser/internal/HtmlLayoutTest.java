/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Test;

/** The drawn browser's lazy block layout. */
public class HtmlLayoutTest {

	private final Display display = Display.getDefault();
	private HtmlLayout layout;

	private HtmlLayout layout(String html, int width) {
		layout = new HtmlLayout(display, HtmlDocument.parse(html), display.getSystemFont(), display.getSystemColor(SWT.COLOR_BLUE));
		layout.setWidth(width);
		return layout;
	}

	@After
	public void tearDown() {
		if (layout != null) layout.dispose();
	}

	private static String paragraphs(int n) {
		StringBuilder html = new StringBuilder();
		for (int i = 0; i < n; i++) html.append("<p>Paragraph ").append(i).append(" with some words that may wrap at narrow widths.</p>");
		return html.toString();
	}

	@Test
	public void onlyWhatIsAskedForIsMeasured() {
		HtmlLayout l = layout(paragraphs(1000), 400);
		assertEquals(0, l.blockAt(5));
		assertTrue(l.blockAt(50) < 3);
		assertTrue("measured " + l.measured(), l.measured() < 10);
		assertTrue("layouts " + l.layoutsMade, l.layoutsMade < 10);
		assertTrue(l.totalHeight() > 50 * 100);
	}

	@Test
	public void lazyMeasuresAgreeWithAFullMeasure() {
		HtmlLayout full = layout(paragraphs(120), 300);
		full.ensure(Integer.MAX_VALUE);
		int[] tops = new int[121];
		for (int b = 0; b < 120; b++) tops[b] = full.top(b);
		tops[120] = full.totalHeight();
		full.dispose();
		HtmlLayout lazy = layout(paragraphs(120), 300);
		int[] lazyTops = new int[121];
		for (int y = 0; y < tops[120]; y += 97) lazy.blockAt(y);
		for (int b = 0; b < 120; b++) lazyTops[b] = lazy.top(b);
		lazyTops[120] = lazy.totalHeight();
		assertArrayEquals(tops, lazyTops);
	}

	@Test
	public void blockAtFindsTheBlockAtEveryEdge() {
		HtmlLayout l = layout(paragraphs(40), 250);
		for (int b = 0; b < 40; b++) {
			assertEquals(b, l.blockAt(l.top(b)));
			assertEquals(b, l.blockAt(l.top(b) + l.height(b) - 1));
		}
		assertEquals(39, l.blockAt(Integer.MAX_VALUE));
	}

	@Test
	public void narrowerIsTaller() {
		String text = paragraphs(1);
		int wide = layout(text, 2000).height(0);
		layout.dispose();
		int narrow = layout(text, 120).height(0);
		assertTrue(narrow + " > " + wide, narrow > wide);
		layout.setWidth(2000);
		assertEquals("a new width forgets the old measures", wide, layout.height(0));
	}

	@Test
	public void headingsAreTallerAndListsIndent() {
		HtmlLayout l = layout("<h1>Big</h1><p>Big</p><ol><li>one<ul><li>two</li></ul></li></ol>", 400);
		assertTrue(l.height(0) > l.height(1));
		assertEquals(HtmlLayout.LIST_INDENT, l.indent(2));
		assertEquals(2 * HtmlLayout.LIST_INDENT, l.indent(3));
	}

	@Test
	public void imagesTakeTheirDeclaredHeight() {
		HtmlLayout l = layout("<img src=a.png height=40><img src=b.png>", 400);
		assertEquals(40 + 2 * HtmlLayout.BLOCK_GAP, l.height(0));
		assertEquals(HtmlLayout.IMAGE_PLACEHOLDER + 2 * HtmlLayout.BLOCK_GAP, l.height(1));
		assertNull(l.textLayout(0));
	}

	@Test
	public void linkAtHitsOnlyTheLink() {
		HtmlLayout l = layout("<p>plain words then <a href=\"next.html\">the link</a> end</p>", 2000);
		TextLayout text = l.textLayout(0);
		int linkStart = "plain words then ".length();
		Point at = text.getLocation(linkStart + 2, false);
		int y = l.top(0) + HtmlLayout.BLOCK_GAP + at.y + 2;
		assertEquals("next.html", l.linkAt(at.x + 1, y));
		Point plain = text.getLocation(2, false);
		assertNull(l.linkAt(plain.x + 1, y));
		assertNull(l.linkAt(1, l.totalHeight() + 50));
	}

	@Test
	public void textLayoutsAreCachedAndEvicted() {
		HtmlLayout l = layout(paragraphs(100), 400);
		TextLayout first = l.textLayout(0);
		assertSame(first, l.textLayout(0));
		for (int b = 1; b <= 64; b++) l.textLayout(b);
		assertTrue("the least recently used layout is disposed", first.isDisposed());
		assertTrue(!l.textLayout(64).isDisposed());
	}
}
