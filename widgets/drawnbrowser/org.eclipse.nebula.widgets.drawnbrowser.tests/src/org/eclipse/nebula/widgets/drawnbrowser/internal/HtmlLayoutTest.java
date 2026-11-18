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
	public void preDoesNotWrapIsMonospaceAndARuleIsThin() {
		String line = "a long line of code that would wrap at this narrow width without pre";
		HtmlLayout l = layout("<p>" + line + "</p><pre>" + line + "</pre><hr><p>after <code>x</code></p>", 120);
		TextLayout paragraph = l.textLayout(0), pre = l.textLayout(1);
		assertTrue("the paragraph wraps: " + paragraph.getLineCount(), paragraph.getLineCount() > 1);
		assertEquals("pre does not", 1, pre.getLineCount());
		assertTrue("wider than the view", pre.getBounds().width > 120);
		String mono = pre.getFont().getFontData()[0].getName();
		assertTrue("pre is monospace, not " + mono, !mono.equals(paragraph.getFont().getFontData()[0].getName()));
		assertEquals("inline code too", mono, l.textLayout(3).getStyle(6).font.getFontData()[0].getName());
		assertNull("a rule has no text", l.textLayout(2));
		assertEquals(HtmlLayout.RULE_HEIGHT + 2 * HtmlLayout.BLOCK_GAP, l.height(2));
		assertEquals("a rule has no link", null, l.linkAt(10, l.top(2) + HtmlLayout.BLOCK_GAP));
	}

	@Test
	public void cellsOfARowShareTheirTopAndTheTallestHeight() {
		// the tallest cell is not the last: the row takes the maximum, not the last measured
		HtmlLayout l = layout("<p>before</p><table><tr><td>a much longer cell that has to wrap onto several lines at this "
				+ "width</td><td>short</td></tr><tr><td>next</td></tr></table>", 300);
		assertEquals(l.top(1), l.top(2));
		assertEquals(l.height(1), l.height(2));
		assertTrue("the long cell wraps", l.textLayout(1).getLineCount() > 1);
		assertEquals(1, l.textLayout(2).getLineCount());
		assertEquals("as tall as the tallest", l.textLayout(1).getBounds().height + 2 * HtmlLayout.CELL_PADDING, l.height(2));
		assertEquals("the next row", l.top(1) + l.height(1), l.top(3));
		assertEquals("a row is found by its first cell", 1, l.blockAt(l.top(1)));
		assertEquals(1, l.blockAt(l.top(1) + l.height(1) - 1));
		assertEquals(2, l.blockAt(l.cellX(2) + 1, l.top(1) + 2));
		assertEquals(1, l.blockAt(l.cellX(2) - 1, l.top(1) + 2));
		assertEquals("the table fills the view", 300, l.cellX(2) + l.cellWidth(2));
	}

	@Test
	public void columnsAreAsWideAsTheirContentWhenItFits() {
		HtmlLayout l = layout("<blockquote><table><tr><td>ab</td><td>abcdef</td></tr><tr><td>x</td></tr></table></blockquote>", 600);
		TextLayout probe = new TextLayout(display);
		try {
			probe.setFont(l.textLayout(0).getFont());
			probe.setText("abcdef");
			assertEquals(probe.getBounds().width + 2 * HtmlLayout.CELL_PADDING, l.cellWidth(1));
			probe.setText("ab");
			assertEquals(probe.getBounds().width + 2 * HtmlLayout.CELL_PADDING, l.cellWidth(0));
		} finally {
			probe.dispose();
		}
		assertEquals("indented by the blockquote", HtmlDocument.QUOTE_INDENT, l.cellX(0));
		assertEquals(l.cellX(0) + l.cellWidth(0), l.cellX(1));
		assertEquals("a column keeps its width in every row", l.cellWidth(0), l.cellWidth(2));
		assertEquals(l.cellX(0) + HtmlLayout.CELL_PADDING, l.indent(0));
		assertEquals(HtmlLayout.CELL_PADDING, l.contentTop(0));
	}

	@Test
	public void aColumnIsNeverNarrowerThanItsWidestWordAndAHeaderIsBold() {
		HtmlLayout l = layout("<table><tr><th>supercalifragilistic</th><td>a b c d e f g h i j k l m n o p q r s</td></tr></table>", 150);
		TextLayout probe = new TextLayout(display);
		try {
			probe.setFont(l.textLayout(0).getFont());
			probe.setText("supercalifragilistic");
			assertTrue(l.cellWidth(0) + " holds the word", l.cellWidth(0) >= probe.getBounds().width + 2 * HtmlLayout.CELL_PADDING);
		} finally {
			probe.dispose();
		}
		assertTrue("th is bold", (l.textLayout(0).getFont().getFontData()[0].getStyle() & SWT.BOLD) != 0);
		assertEquals("td is not", 0, l.textLayout(1).getFont().getFontData()[0].getStyle() & SWT.BOLD);
		assertTrue("the other column wraps", l.textLayout(1).getLineCount() > 1);
	}


	@Test
	public void aBoxInsetsItsContentByBorderAndPaddingInsideItsMargins() {
		String text = "some words in a box";
		HtmlLayout l = layout("<p>" + text + "</p><p style='margin:10px 20px 30px 40px; padding:5px; border:2px solid red'>"
				+ text + "</p>", 300);
		int h = l.textLayout(1).getBounds().height;
		assertEquals("a plain block is unchanged", l.textLayout(0).getBounds().height + 2 * HtmlLayout.BLOCK_GAP, l.height(0));
		assertEquals(false, l.hasBox(0));
		assertEquals(true, l.hasBox(1));
		assertEquals(HtmlLayout.BLOCK_GAP + 10 + 2 + 5, l.contentTop(1));
		assertEquals(40 + 2 + 5, l.indent(1));
		assertEquals(h + (HtmlLayout.BLOCK_GAP + 10) + (HtmlLayout.BLOCK_GAP + 30) + 2 * 2 + 5 + 5, l.height(1));
		assertEquals(40, l.boxLeft(1));
		assertEquals(l.top(1) + HtmlLayout.BLOCK_GAP + 10, l.boxTop(1));
		assertEquals(300 - 40 - 20, l.boxWidth(1));
		assertEquals(h + 2 * 2 + 5 + 5, l.boxHeight(1));
		assertEquals("the text wraps inside the padding", 300 - 47 - 2 - 5 - 20, l.textLayout(1).getWidth());
		assertEquals("a link or an offset starts at the content", 0, l.offsetAt(l.indent(1), l.top(1) + l.contentTop(1) + 1)
				- text.length());
	}
	@Test
	public void linksAndOffsetsAreFoundInTheCellUnderThePointer() {
		HtmlLayout l = layout("<table><tr><td>left</td><td><a href='r'>right</a></td></tr></table><p>after</p>", 400);
		int y = l.top(0) + HtmlLayout.CELL_PADDING + 2;
		assertEquals("r", l.linkAt(l.indent(1) + 2, y));
		assertNull(l.linkAt(l.indent(0) + 2, y));
		assertEquals("the second cell's text starts after the first's", 4, l.offsetAt(l.indent(1), y));
		assertEquals(0, l.offsetAt(l.indent(0), y));
		assertEquals("after the table", 2, l.blockAt(l.top(0) + l.height(0)));
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
	public void aLoadedImageKeepsItsDeclaredSideAndScalesTheOther() {
		HtmlLayout l = layout("<p>above</p><img src=a.png width=40><img src=b.png><p>below</p>", 400);
		int below = l.top(3);
		assertTrue(l.setNaturalSize(0, 20, 10));
		assertEquals(40, l.shownWidth(0));
		assertEquals(20, l.shownHeight(0));
		assertTrue(l.setNaturalSize(1, 30, 50));
		assertEquals(30, l.shownWidth(1));
		assertEquals(50, l.height(2) - 2 * HtmlLayout.BLOCK_GAP);
		assertEquals("the blocks below move", below + (20 - HtmlLayout.IMAGE_PLACEHOLDER) + (50 - HtmlLayout.IMAGE_PLACEHOLDER), l.top(3));
		assertTrue("the same size again changes nothing", !l.setNaturalSize(0, 20, 10));
	}

	@Test
	public void cssAlignsIndentsAndColours() {
		HtmlLayout l = layout("<p style=\"text-align: right; margin-left: 20px\">a <span style=\"color: #102030\">b</span></p><ul><li style=\"margin-left: 5px\">x</li></ul>", 400);
		TextLayout text = l.textLayout(0);
		assertEquals(SWT.RIGHT, text.getAlignment());
		assertEquals(20, l.indent(0));
		assertEquals(HtmlLayout.LIST_INDENT + 5, l.indent(1));
		org.eclipse.swt.graphics.Color colour = text.getStyle(2).foreground;
		assertEquals(new org.eclipse.swt.graphics.RGB(0x10, 0x20, 0x30), colour.getRGB());
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
