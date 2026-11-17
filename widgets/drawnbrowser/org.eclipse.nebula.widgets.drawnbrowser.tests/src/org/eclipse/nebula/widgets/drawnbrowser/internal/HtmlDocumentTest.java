/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.BOLD;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.BULLET;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.CODE;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.PRE;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.RULE;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.HEADING;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.IMAGE;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.ITALIC;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.LINK;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.LIST_ITEM;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.PARAGRAPH;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.STRIKE;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument.UNDERLINE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The drawn browser's document lanes, tag by tag of prasser/swtbrowser's handlers. */
public class HtmlDocumentTest {

	/* each block as kind:level:marker:text */
	private static String[] blocks(HtmlDocument d) {
		String[] out = new String[d.blockCount];
		for (int b = 0; b < d.blockCount; b++) {
			out[b] = d.kind[b] + ":" + d.level[b] + ":" + d.marker[b] + ":" + d.blockText(b);
		}
		return out;
	}

	/* the runs of block b as style@text, link targets as style>href@text */
	private static String[] runs(HtmlDocument d, int b) {
		String[] out = new String[d.runCount[b]];
		for (int k = 0; k < d.runCount[b]; k++) {
			int r = d.firstRun[b] + k;
			String text = d.text.substring(d.runStart[r], d.runEnd[r]);
			out[k] = d.runStyle[r] + (d.runLink[r] >= 0 ? ">" + d.href[d.runLink[r]] : "") + "@" + text;
		}
		return out;
	}

	@Test
	public void paragraphsAndHeadingsCollapseWhitespace() {
		HtmlDocument d = HtmlDocument.parse("<h1> Title </h1>\n<p>  one\n  two\t three </p><h3>Sub</h3><p></p>");
		assertArrayEquals(new String[] { HEADING + ":1:0:Title", PARAGRAPH + ":0:0:one two three", HEADING + ":3:0:Sub" },
				blocks(d));
	}

	@Test
	public void textOutsideBlocksIsAnImplicitParagraph() {
		HtmlDocument d = HtmlDocument.parse("<html><body>Hello <b>world</b></body></html>");
		assertArrayEquals(new String[] { PARAGRAPH + ":0:0:Hello world" }, blocks(d));
	}

	@Test
	public void inlineStylesNestAndCloseIndependently() {
		HtmlDocument d = HtmlDocument.parse("<p>a <strong>b <em>c</em></strong> <u>d</u> <del>e</del><strike>f</strike></p>");
		assertArrayEquals(new String[] { "0@a ", BOLD + "@b ", (BOLD | ITALIC) + "@c", "0@ ", UNDERLINE + "@d", "0@ ",
				STRIKE + "@ef" }, runs(d, 0));
	}

	@Test
	public void linksCarryTheirTarget() {
		HtmlDocument d = HtmlDocument.parse("<p>see <a href=\"https://www.eclipse.org/nebula\">Nebula <b>home</b></a>.</p>");
		assertArrayEquals(new String[] { "0@see ", LINK + ">https://www.eclipse.org/nebula@Nebula ",
				(LINK | BOLD) + ">https://www.eclipse.org/nebula@home", "0@." }, runs(d, 0));
		assertEquals(1, d.linkCount);
	}

	@Test
	public void listsNumberAndNest() {
		HtmlDocument d = HtmlDocument.parse("<ol start=\"3\"><li>three</li><li>four<ul><li>dot</li></ul></li></ol><ul><li>x</li></ul>");
		assertArrayEquals(new String[] { LIST_ITEM + ":1:3:three", LIST_ITEM + ":1:4:four", LIST_ITEM + ":2:" + BULLET + ":dot",
				LIST_ITEM + ":1:" + BULLET + ":x" }, blocks(d));
	}

	@Test
	public void breakIsAHardLineBreak() {
		HtmlDocument d = HtmlDocument.parse("<p>one <br> two<br/>three</p>");
		assertEquals("one\ntwo\nthree", d.blockText(0));
	}

	@Test
	public void imagesAreBlocksWithTheirSize() {
		HtmlDocument d = HtmlDocument.parse("<p>before</p><img src='logo.png' width=\"32\" height=16px><img alt=x><p>after</p>");
		assertArrayEquals(new String[] { PARAGRAPH + ":0:0:before", IMAGE + ":0:0:", PARAGRAPH + ":0:0:after" }, blocks(d));
		assertEquals(0, d.image[1]);
		assertEquals("logo.png", d.source[0]);
		assertEquals(32, d.imageWidth[0]);
		assertEquals(16, d.imageHeight[0]);
		assertEquals(1, d.imageCount);
	}

	@Test
	public void scriptsStylesAndCommentsAreNotText() {
		HtmlDocument d = HtmlDocument.parse("<style>p{color:red}</style><!-- note --><p>x<script>var a = '<p>no</p>';</script>y</p>");
		assertArrayEquals(new String[] { PARAGRAPH + ":0:0:xy" }, blocks(d));
	}

	@Test
	public void entitiesDecodeAndUnknownOnesStay() {
		HtmlDocument d = HtmlDocument.parse("<p>a &amp; b &lt;c&gt; &#65;&#x42; &bogus; &copy;</p>");
		assertEquals("a & b <c> AB &bogus; \u00a9", d.blockText(0));
	}

	@Test
	public void cssGivesRunsTheirColourAndStyleAndBlocksTheirBox() {
		HtmlDocument d = HtmlDocument.parse("<html><head><style>p.box { background-color: #ff0; text-align: center; margin-left: 12px }"
				+ " .hot { color: red; font-weight: bold }</style></head><body>"
				+ "<p class=\"box\">a <span class=\"hot\">b <em>c</em></span> d</p><p style=\"text-decoration: underline\">u</p></body></html>");
		assertEquals(2, d.blockCount);
		assertEquals("a b c d", d.blockText(0));
		assertEquals(0xFFFFFF00, d.blockBackground[0]);
		assertEquals(HtmlStyles.CENTER, d.blockAlign[0]);
		assertEquals(12, d.blockIndent[0]);
		assertArrayEquals(new String[] { "0@a ", BOLD + "@b ", (BOLD | ITALIC) + "@c", "0@ d" }, runs(d, 0));
		int r = d.firstRun[0];
		assertEquals(0, d.runColor[r]);
		assertEquals(0xFFFF0000, d.runColor[r + 1]);
		assertEquals("the colour inherits into em", 0xFFFF0000, d.runColor[r + 2]);
		assertEquals(0, d.runColor[r + 3]);
		assertArrayEquals(new String[] { UNDERLINE + "@u" }, runs(d, 1));
		assertEquals(0, d.blockBackground[1]);
		HtmlDocument colourOnly = HtmlDocument.parse("<p>x<span style=\"color: blue\">y</span>z</p>");
		assertArrayEquals("a colour change alone starts a run", new String[] { "0@x", "0@y", "0@z" }, runs(colourOnly, 0));
		assertEquals(0xFF0000FF, colourOnly.runColor[colourOnly.firstRun[0] + 1]);
	}

	@Test
	public void preKeepsItsWhiteSpaceAndIsCode() {
		HtmlDocument d = HtmlDocument.parse("<p>a</p><pre>\n  x  =  1;\r\n\ty\n</pre><p>b   c</p>");
		assertArrayEquals(new String[] { PARAGRAPH + ":0:0:a", PRE + ":0:0:  x  =  1;\n\ty", PARAGRAPH + ":0:0:b c" }, blocks(d));
		assertArrayEquals(new String[] { CODE + "@  x  =  1;\n\ty" }, runs(d, 1));
		HtmlDocument bold = HtmlDocument.parse("<pre>a <b>b</b>\r c</pre>");
		assertArrayEquals("styles inside pre, a lone CR is a line end", new String[] { CODE + "@a ", (CODE | BOLD) + "@b", CODE + "@\n c" },
				runs(bold, 0));
	}

	@Test
	public void inlineCodeIsAStyle() {
		HtmlDocument d = HtmlDocument.parse("<p>call <code>f(x)</code> or <kbd>Ctrl</kbd>+<tt>C</tt> <samp>ok</samp></p>");
		assertArrayEquals(new String[] { "0@call ", CODE + "@f(x)", "0@ or ", CODE + "@Ctrl", "0@+", CODE + "@C", "0@ ", CODE + "@ok" },
				runs(d, 0));
	}

	@Test
	public void blockquotesIndentTheirBlocksAndHrIsARule() {
		HtmlDocument d = HtmlDocument.parse("<p>before</p><blockquote><p>q1</p><blockquote>q2</blockquote></blockquote><hr><p>after</p>");
		assertArrayEquals(new String[] { PARAGRAPH + ":0:0:before", PARAGRAPH + ":0:0:q1", PARAGRAPH + ":0:0:q2", RULE + ":0:0:",
				PARAGRAPH + ":0:0:after" }, blocks(d));
		assertArrayEquals(new int[] { 0, HtmlDocument.QUOTE_INDENT, 2 * HtmlDocument.QUOTE_INDENT, 0, 0 },
				java.util.Arrays.copyOf(d.blockIndent, d.blockCount));
	}

	@Test
	public void malformedInputNeverFails() {
		HtmlDocument d = HtmlDocument.parse("<p>open <b>bold <i>both</p> after</b> < lone <p");
		assertArrayEquals(new String[] { PARAGRAPH + ":0:0:open bold both", PARAGRAPH + ":0:0:after < lone <p" }, blocks(d));
	}
}
