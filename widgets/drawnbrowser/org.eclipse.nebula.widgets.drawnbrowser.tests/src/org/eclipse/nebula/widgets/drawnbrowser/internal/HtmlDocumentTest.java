/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import java.util.ArrayList;
import java.util.List;

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
	public void tableCellsAreBlocksWithTheirTableRowAndColumn() {
		HtmlDocument d = HtmlDocument.parse("<p>before</p><table border=1><caption>Cap</caption><tr><th>Name</th><th>Size</th></tr>"
				+ "<tr><td>a <b>b</b></td><td></td></tr><tr><td>only</td></tr></table><p>after</p>");
		int cell = HtmlDocument.CELL;
		assertArrayEquals(new String[] { PARAGRAPH + ":0:0:before", PARAGRAPH + ":0:0:Cap", cell + ":1:0:Name", cell + ":1:0:Size",
				cell + ":0:0:a b", cell + ":0:0:", cell + ":0:0:only", PARAGRAPH + ":0:0:after" }, blocks(d));
		assertEquals(1, d.tableCount);
		assertEquals("the widest row", 2, d.tableColumns[0]);
		assertEquals(1, d.tableBorder[0]);
		assertArrayEquals(new int[] { -1, -1, 0, 0, 1, 1, 2, -1 }, java.util.Arrays.copyOf(d.cellRow, 8));
		assertArrayEquals(new int[] { -1, -1, 0, 1, 0, 1, 0, -1 }, java.util.Arrays.copyOf(d.cellColumn, 8));
		assertArrayEquals(new int[] { -1, -1, 0, 0, 0, 0, 0, -1 }, java.util.Arrays.copyOf(d.cellTable, 8));
		assertEquals("caption centred", HtmlStyles.CENTER, d.blockAlign[1]);
		assertEquals("th centred", HtmlStyles.CENTER, d.blockAlign[2]);
		assertEquals(HtmlStyles.LEFT, d.blockAlign[4]);
		assertEquals(true, d.sameRow(2, 3));
		assertEquals(false, d.sameRow(3, 4));
		assertEquals(false, d.sameRow(0, 1));
	}

	@Test
	public void blockElementsInACellAreLineBreaksAndANestedTableFlattens() {
		HtmlDocument d = HtmlDocument.parse("<table><tr><td><p>one</p><p>two</p><ul><li>three</li></ul><img src=x.png>"
				+ "<table><tr><td>in</td><td>ner</td></tr><tr><td>row</td></tr></table></td><td>next</td></tr></table><p>after</p>");
		int cell = HtmlDocument.CELL;
		assertArrayEquals(new String[] { cell + ":0:0:one\ntwo\nthree\nin ner\nrow", cell + ":0:0:next", PARAGRAPH + ":0:0:after" }, blocks(d));
		assertEquals("one table", 1, d.tableCount);
		assertEquals("no image in a cell yet", 0, d.imageCount);
		assertEquals(true, d.sameRow(0, 1));
	}

	@Test
	public void strayTextInATableGetsACellAndAnOpenCellEndsWithThePage() {
		HtmlDocument d = HtmlDocument.parse("<blockquote><table>stray<tr><td>x ");
		int cell = HtmlDocument.CELL;
		assertArrayEquals(new String[] { cell + ":0:0:stray", cell + ":0:0:x" }, blocks(d));
		assertArrayEquals(new int[] { 0, 1 }, java.util.Arrays.copyOf(d.cellRow, 2));
		assertEquals("in a blockquote", HtmlDocument.QUOTE_INDENT, d.blockIndent[1]);
	}

	@Test
	public void malformedInputNeverFails() {
		HtmlDocument d = HtmlDocument.parse("<p>open <b>bold <i>both</p> after</b> < lone <p");
		assertArrayEquals(new String[] { PARAGRAPH + ":0:0:open bold both", PARAGRAPH + ":0:0:after < lone <p" }, blocks(d));
	}

	@Test
	public void aBlockKeepsTheBoxOfItsOwnElement() {
		HtmlDocument d = HtmlDocument.parse("<p style='margin:1px 2px 3px 4px; padding:5px 6px 7px 8px; border:9px solid #00ff00'>"
				+ "a <span style='padding:20px'>b</span></p><p>plain</p><table><tr><td style='padding:30px'>c</td></tr></table>");
		assertEquals(1, d.blockMarginTop[0]);
		assertEquals(2, d.blockMarginRight[0]);
		assertEquals(3, d.blockMarginBottom[0]);
		assertEquals(4, d.blockIndent[0]);
		assertEquals(5, d.blockPadTop[0]);
		assertEquals(6, d.blockPadRight[0]);
		assertEquals(7, d.blockPadBottom[0]);
		assertEquals(8, d.blockPadLeft[0]);
		assertEquals(9, d.blockBorder[0]);
		assertEquals(0xFF00FF00, d.blockBorderColor[0]);
		assertEquals("an inline element's box is not the block's", 5, d.blockPadTop[0]);
		assertEquals("a plain block has none", 0, d.blockPadTop[1] + d.blockBorder[1] + d.blockMarginTop[1]);
		assertEquals("a table cell keeps its cell padding", 0, d.blockPadTop[2]);
	}

	@Test
	public void formControlsAreControlBlocksWithTheirLanes() {
		HtmlDocument d = HtmlDocument.parse("<form action='http://example.test/search'><input name=q value=hi><input type=password name=pw><input type=checkbox name=agree checked><input type=radio name=c value=a><input type=radio name=c value=b checked><input type=hidden name=h value=x><select name=s><option>one<option selected>two</select><textarea name=t rows=3 cols=10>\nline1\nline2</textarea><button name=go value=1>Go now</button></form><p>after <input name=outside></p>");
		int controls = 0;
		for (int b = 0; b < d.blockCount; b++) if (d.kind[b] == HtmlDocument.CONTROL) controls++;
		assertEquals("every control but the hidden input has a block", 9, controls);
		assertEquals(10, d.controlCount);
		assertEquals(1, d.formCount);
		assertEquals("http://example.test/search", d.formAction[0]);
		int[] types = { HtmlDocument.TEXT, HtmlDocument.PASSWORD, HtmlDocument.CHECKBOX, HtmlDocument.RADIO, HtmlDocument.RADIO,
				HtmlDocument.HIDDEN, HtmlDocument.SELECT, HtmlDocument.TEXTAREA, HtmlDocument.SUBMIT, HtmlDocument.TEXT };
		assertArrayEquals(types, java.util.Arrays.copyOf(d.controlType, 10));
		assertEquals("hi", d.controlValue[0]);
		assertEquals("on", d.controlValue[2]);
		assertEquals(1, d.controlChecked[2]);
		assertEquals(0, d.controlChecked[3]);
		assertEquals(1, d.controlChecked[4]);
		assertEquals("x", d.controlValue[5]);
		assertArrayEquals(new String[] { "one", "two" }, d.controlOptions[6]);
		assertEquals("the selected option", 1, d.controlChecked[6]);
		assertEquals("the newline after <textarea> is not content", "line1\nline2", d.controlValue[7]);
		assertEquals(3, d.controlRows[7]);
		assertEquals(10, d.controlCols[7]);
		assertEquals("Go now", d.controlLabel[8]);
		assertEquals("1", d.controlValue[8]);
		assertEquals(0, d.controlForm[8]);
		assertEquals("outside the form", -1, d.controlForm[9]);
		assertEquals("text after a control is a paragraph again", "after", d.blockText(d.blockCount - 2));
	}

	/* the first block whose text is this */
	private static int block(HtmlDocument d, String text) {
		for (int b = 0; b < d.blockCount; b++) if (d.blockText(b).equals(text)) return b;
		throw new AssertionError("no block " + text);
	}

	/* the colour of the run of a block that holds this text */
	private static int colorOf(HtmlDocument d, int b, String text) {
		int at = d.textStart[b] + d.blockText(b).indexOf(text);
		for (int k = 0; k < d.runCount[b]; k++) {
			int r = d.firstRun[b] + k;
			if (at >= d.runStart[r] && at < d.runEnd[r]) return d.runColor[r];
		}
		return 0;
	}

	private static int styleOf(HtmlDocument d, int b, String text) {
		int at = d.textStart[b] + d.blockText(b).indexOf(text);
		for (int k = 0; k < d.runCount[b]; k++) {
			int r = d.firstRun[b] + k;
			if (at >= d.runStart[r] && at < d.runEnd[r]) return d.runStyle[r];
		}
		return 0;
	}

	@Test
	public void selectorsAxesAttributeConditionsAndSpecificity() {
		HtmlDocument d = HtmlDocument.parse("<style>"
				+ "div p { color: #ff0000 } div > p { color: #0000ff } h1 + p { color: #008000 } p:first-child { font-weight: bold }"
				+ " a[href^='http'] { color: #808080 } #main .note { color: #800000 } .note { color: #000080 }"
				+ " [lang|=en] { color: #800080 } p:hover { color: #ffff00 } span.x.y { text-decoration: underline }"
				+ " p + p + p { color: #00ffff }</style>"
				+ "<div><section><p>deep</p></section><p>child</p></div>"
				+ "<h1>t</h1><p>next</p>"
				+ "<div id=main><p class=note>main note</p></div><p class=note>plain note</p>"
				+ "<p><a href='http://x'>web</a> <a href='mailto:y'>mail</a></p>"
				+ "<p lang=en-GB>english</p><p><span class='x y'>both</span> <span class=x>one</span></p>");
		assertEquals("descendant", 0xFFFF0000, colorOf(d, block(d, "deep"), "deep"));
		assertEquals("child beats descendant by sheet order at equal specificity", 0xFF0000FF, colorOf(d, block(d, "child"), "child"));
		assertEquals("adjacent sibling", 0xFF008000, colorOf(d, block(d, "next"), "next"));
		assertEquals("first child", BOLD, styleOf(d, block(d, "deep"), "deep") & BOLD);
		assertEquals("not the first child", 0, styleOf(d, block(d, "child"), "child") & BOLD);
		assertEquals("an id outweighs a later class", 0xFF800000, colorOf(d, block(d, "main note"), "main note"));
		assertEquals(0xFF000080, colorOf(d, block(d, "plain note"), "plain note"));
		int links = block(d, "web mail");
		assertEquals("attribute prefix", 0xFF808080, colorOf(d, links, "web"));
		assertEquals("no match: the link colour stays", 0, colorOf(d, links, "mail"));
		assertEquals("attribute dash match", 0xFF800080, colorOf(d, block(d, "english"), "english"));
		int spans = block(d, "both one");
		assertEquals("both classes", UNDERLINE, styleOf(d, spans, "both") & UNDERLINE);
		assertEquals("one class is not enough", 0, styleOf(d, spans, "one") & UNDERLINE);
		for (int b = 0; b < d.blockCount; b++) {
			assertEquals("an unsupported pseudo-class never matches", false, colorOf(d, b, d.blockText(b)) == 0xFFFFFF00);
		}
		HtmlDocument chain = HtmlDocument.parse("<style>p + p + p { color: #00ffff }</style><p>a</p><p>b</p><p>c</p>");
		assertEquals("two siblings back", 0xFF00FFFF, colorOf(chain, block(chain, "c"), "c"));
		assertEquals(0, colorOf(chain, block(chain, "b"), "b"));
	}

	@Test
	public void colspanPlacesTheNextCellsAfterIt() {
		HtmlDocument d = HtmlDocument.parse("<table><tr><td colspan=2>wide</td><td>c</td></tr><tr><td>a</td><td colspan=x>b</td>"
				+ "<td colspan=0>c2</td></tr></table>");
		assertEquals(3, d.tableColumns[0]);
		assertArrayEquals("columns", new int[] { 0, 2, 0, 1, 2 }, java.util.Arrays.copyOf(d.cellColumn, 5));
		assertArrayEquals("spans; a bad or zero colspan is 1", new int[] { 2, 1, 1, 1, 1 }, java.util.Arrays.copyOf(d.cellSpan, 5));
	}

	@Test
	public void rowspanCoversColumnsInTheRowsBelow() {
		HtmlDocument d = HtmlDocument.parse("<table><tr><td rowspan=2>tall</td><td>a</td></tr><tr><td>b</td></tr>"
				+ "<tr><td>c</td><td>d</td></tr><tr><td rowspan=x>e</td></tr></table>");
		assertArrayEquals("b skips the column tall covers", new int[] { 0, 1, 1, 0, 1, 0 }, java.util.Arrays.copyOf(d.cellColumn, 6));
		assertArrayEquals("a bad rowspan is 1", new int[] { 2, 1, 1, 1, 1, 1 }, java.util.Arrays.copyOf(d.cellRowSpan, 6));
		assertEquals(2, d.tableColumns[0]);
	}

	@Test
	public void aFlexContainersChildrenAreItemsAndDisplayNoneHides() {
		HtmlDocument d = HtmlDocument.parse("<p>before</p><div style='display:flex; gap:8px; justify-content:center'>"
				+ "<div style='flex-grow:2; padding:5px'>one <p>inner</p></div>loose text<div style='width:70px'>three</div></div>"
				+ "<p>after</p><p style='display:none'>hidden <b>bold</b><input name=gone></p><div style='display:none'><p>deep</p></div>"
				+ "<p>shown<script>x</script> end</p>");
		int one = block(d, "one\ninner"), loose = block(d, "loose text"), three = block(d, "three");
		assertEquals(HtmlDocument.CELL, d.kind[one]);
		assertEquals(HtmlStyles.DISPLAY_FLEX, d.tableDisplay[d.cellTable[one]]);
		assertEquals(8, d.tableGap[d.cellTable[one]]);
		assertEquals(FlexRow.CENTER, d.tableJustify[d.cellTable[one]]);
		assertArrayEquals("items in order on one row", new int[] { 0, 1, 2 },
				new int[] { d.cellColumn[one], d.cellColumn[loose], d.cellColumn[three] });
		assertEquals(true, d.sameRow(one, three));
		assertEquals(2, d.cellGrow[one]);
		assertEquals(5, d.blockPadLeft[one]);
		assertEquals("an anonymous item has no box", 0, d.blockPadLeft[loose]);
		assertEquals(70, d.cellBasis[three]);
		assertEquals(-1, d.cellBasis[one]);
		assertEquals(PARAGRAPH, d.kind[block(d, "after")]);
		for (int b = 0; b < d.blockCount; b++) {
			assertEquals("nothing hidden is a block", false, d.blockText(b).contains("hidden") || d.blockText(b).contains("deep"));
		}
		assertEquals("a control in a hidden element is not made", 0, d.controlCount);
		block(d, "shown end");
	}

	@Test
	public void gridItemsArePlacedRowByRowInTheTemplatesColumns() {
		HtmlDocument d = HtmlDocument.parse("<p>before</p><div style='display:grid; grid-template-columns: 50px 1fr 2fr; gap: 6px 10px'>"
				+ "<div>a</div><div style='grid-column: span 2; padding: 4px'>b</div><div>c</div><div style='grid-column: span 2'>d</div>"
				+ "loose<div style='grid-column: span 9'>e</div></div><div style='display:grid'><p>one</p><p>two</p></div><p>after</p>");
		int a = block(d, "a"), b = block(d, "b"), c = block(d, "c"), dd = block(d, "d"), loose = block(d, "loose"), e = block(d, "e");
		int t = d.cellTable[a];
		assertEquals(HtmlStyles.DISPLAY_GRID, d.tableDisplay[t]);
		assertEquals(3, d.tableColumns[t]);
		assertArrayEquals(new int[] { 50, GridTracks.FR | 100, GridTracks.FR | 200 },
				java.util.Arrays.copyOfRange(d.tracks, d.tableTrackStart[t], d.tableTrackStart[t] + d.tableTrackCount[t]));
		assertEquals(10, d.tableGap[t]);
		assertEquals(6, d.tableRowGap[t]);
		int[] items = { a, b, c, dd, loose, e }, column = new int[6], span = new int[6], row = new int[6];
		for (int i = 0; i < 6; i++) {
			column[i] = d.cellColumn[items[i]];
			span[i] = d.cellSpan[items[i]];
			row[i] = d.cellRow[items[i]] - d.cellRow[a];
		}
		assertArrayEquals("an item that does not fit starts the next row", new int[] { 0, 1, 0, 1, 0, 0 }, column);
		assertArrayEquals("a span wider than the grid is the grid", new int[] { 1, 2, 1, 2, 1, 3 }, span);
		assertArrayEquals(new int[] { 0, 0, 1, 1, 2, 3 }, row);
		assertEquals("a grid item keeps its box", 4, d.blockPadLeft[b]);
		int one = block(d, "one"), two = block(d, "two");
		assertEquals("no template: one column", 1, d.tableColumns[d.cellTable[one]]);
		assertEquals("of one auto track", GridTracks.AUTO, d.tracks[d.tableTrackStart[d.cellTable[one]]]);
		assertEquals(1, d.tableTrackCount[d.cellTable[one]]);
		assertEquals(d.cellRow[one] + 1, d.cellRow[two]);
		assertEquals(PARAGRAPH, d.kind[block(d, "after")]);
	}

	@Test
	public void aStickyBoxEndsWithItsContainingElement() {
		HtmlDocument d = HtmlDocument.parse("<div><h2 style='position:sticky; top:0'>A</h2><p>a1</p><p>a2</p></div>"
				+ "<div><h2 style='position:sticky; top:4px'>B</h2><p>b1</p></div><p>after</p>"
				+ "<table><tr><th style='position:sticky; top:0'>h1</th><th style='position:sticky; top:0'>h2</th></tr>"
				+ "<tr><td>x</td><td>y</td></tr></table><p>last</p><h3 style='position:sticky; top:1px'>open</h3><p>end</p>");
		int a = block(d, "A"), b = block(d, "B"), h1 = block(d, "h1"), h2 = block(d, "h2"), open = block(d, "open");
		assertEquals(HtmlStyles.STICKY, d.blockPosition[a]);
		assertEquals(block(d, "a2"), d.blockStickyEnd[a]);
		assertEquals(block(d, "b1"), d.blockStickyEnd[b]);
		assertEquals(4, d.blockTop[b]);
		assertEquals("a sticky header cell", HtmlStyles.STICKY, d.blockPosition[h1]);
		assertEquals("ends with its table, not its row", block(d, "y"), d.blockStickyEnd[h1]);
		assertEquals(block(d, "y"), d.blockStickyEnd[h2]);
		assertEquals("an element still open at the end: the last block", block(d, "end"), d.blockStickyEnd[open]);
		assertEquals(-1, d.blockStickyEnd[block(d, "after")]);
		assertEquals(HtmlStyles.STATIC, d.blockPosition[block(d, "x")]);
	}

	@Test
	public void tokenizerNumericReferencesAcceptOptionalSemicolonsAndSupplementaryScalars() {
		String raw = "&#65;&#x1f600;! &#65";
		assertEquals("A😀! A", HtmlTokenizer.decode(raw, 0, raw.length()));
		raw = "&#x10FFFF; &#128; &#x99;";
		assertEquals(new String(Character.toChars(0x10FFFF)) + " € ™", HtmlTokenizer.decode(raw, 0, raw.length()));
	}

	@Test
	public void tokenizerInvalidUnicodeScalarsBecomeReplacementButUnknownNamesStayLiteral() {
		String raw = "&#0; &#xD800; &#1114112; &#99999999999999999999; &not-a-name; &#x; &#;";
		assertEquals("� � � � &not-a-name; &#x; &#;", HtmlTokenizer.decode(raw, 0, raw.length()));
		raw = "&amp; &lt; &gt; &quot; &apos; &nbsp; &nosuch;";
		assertEquals("& < > \" ' \u00a0 &nosuch;", HtmlTokenizer.decode(raw, 0, raw.length()));
	}

	@Test(timeout = 15000)
	public void tokenizerAdversarialAmpersandRunIsBoundedAndPreservesText() {
		String raw = "&".repeat(131_072) + "end &amp;";
		assertEquals("&".repeat(131_072) + "end &", HtmlTokenizer.decode(raw, 0, raw.length()));
	}

	@Test
	public void tokenizerRawTextClosesOnlyAtCompleteCaseInsensitiveEndTags() {
		List<String> events = new ArrayList<>();
		HtmlTokenizer.tokenize("<script>var s='</scripture>'; &amp; </ScRiPt ><style>a{content:'</stylesheet>'}</STYLE><p>ok</p>",
				tokenizerHandler(events));
		assertEquals(List.of("start:script", "text:var s='</scripture>'; &amp; ", "end:script",
				"start:style", "text:a{content:'</stylesheet>'}", "end:style",
				"start:p", "text:ok", "end:p"), events);
	}

	@Test
	public void tokenizerPrefixEndTagWithoutRealClosingTagRemainsRaw() {
		List<String> events = new ArrayList<>();
		HtmlTokenizer.tokenize("<script>before </scriptX> after", tokenizerHandler(events));
		assertEquals(List.of("start:script", "text:before </scriptX> after"), events);
	}

	@Test
	public void tokenizerNumericAttributeReferencesShareDecoder() {
		List<String> events = new ArrayList<>();
		HtmlTokenizer.tokenize("<p title='&#x1F600; &copy; &#0;' data-code=&#65;>x</p>", new HtmlTokenizer.Handler() {
			@Override
			public void startTag(String name, String[] attrs, boolean selfClosing) {
				events.add(name + ":" + HtmlTokenizer.attribute(attrs, "title") + ":"
						+ HtmlTokenizer.attribute(attrs, "data-code"));
			}
			@Override public void endTag(String name) { }
			@Override public void text(String text) { }
		});
		assertEquals(List.of("p:😀 © �:A"), events);
	}

	private static HtmlTokenizer.Handler tokenizerHandler(List<String> events) {
		return new HtmlTokenizer.Handler() {
			@Override public void startTag(String name, String[] attributes, boolean selfClosing) {
				events.add("start:" + name);
			}
			@Override public void endTag(String name) { events.add("end:" + name); }
			@Override public void text(String text) { events.add("text:" + text); }
		};
	}
}
