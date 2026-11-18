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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/** Box ancestry and flat-text compatibility, including empty and malformed containers. */
public class HtmlBoxesTest {

	@Test
	public void flatPositionedBlocksKeepTheExistingFragmentPath() {
		HtmlDocument d = HtmlDocument.parse("<p>flow</p><div style='position:absolute;top:20px;left:-5px;z-index:-2'>abs</div>"
				+ "<p style='position:relative;top:7px;left:9px'>rel</p><div style='position:fixed;top:10px'>fixed</div>");
		assertEquals(4, d.blockCount);
		assertArrayEquals(new int[] { HtmlStyles.STATIC, HtmlStyles.ABSOLUTE, HtmlStyles.RELATIVE, HtmlStyles.FIXED },
				Arrays.copyOf(d.blockPosition, 4));
		assertArrayEquals(new int[] { HtmlStyles.AUTO, 20, 7, 10 }, Arrays.copyOf(d.blockTop, 4));
		assertArrayEquals(new int[] { HtmlStyles.AUTO, -5, 9, HtmlStyles.AUTO }, Arrays.copyOf(d.blockLeft, 4));
		assertArrayEquals(new int[] { 0, -2, 0, 0 }, Arrays.copyOf(d.blockZ, 4));
		assertTrue(d.positioned);
		assertFalse("existing nonempty root-level blocks retain their flat path", d.boxLayout);
	}

	@Test
	public void nestedAndEmptyPositionedBoxesRequireTheResolvedScene() {
		for (String html : new String[] {
				"<div style='position:relative'><p>child</p></div>",
				"<div style='position:relative'>head<p>child</p>tail</div>",
				"<div><p style='position:absolute'>nested</p></div>",
				"<body><p style='position:fixed'>nested</p></body>",
				"<div style='position:relative'></div>" }) {
			HtmlDocument d = HtmlDocument.parse(html);
			assertTrue(html, d.positioned);
			assertTrue(html, d.boxLayout);
		}
		assertFalse(HtmlDocument.parse("<body><div><p>ordinary</p></div></body>").boxLayout);
	}

	@Test
	public void positionedLeavesWithoutAnAuthoritativeFlatFragmentRequireTheScene() {
		for (String html : new String[] {
				"<img src='x' style='position:relative;left:3px'>",
				"<hr style='position:absolute;top:4px'>",
				"<blockquote style='position:fixed;top:5px'>text</blockquote>",
				"<body style='position:relative;left:6px'>text</body>" }) {
			HtmlDocument d = HtmlDocument.parse(html);
			assertEquals(html, 1, d.blockCount);
			assertEquals(html, HtmlStyles.STATIC, d.blockPosition[0]);
			assertEquals(html, HtmlStyles.AUTO, d.blockTop[0]);
			assertEquals(html, HtmlStyles.AUTO, d.blockLeft[0]);
			assertEquals(html, 0, d.blockZ[0]);
			assertTrue(html, d.positioned);
			assertTrue(html, d.boxLayout);
		}
	}

	@Test
	public void newUsedLengthsRequireTheSceneButAutoResetsRetainTheFlatPath() {
		for (String declaration : new String[] { "width:0", "height:20px", "right:0", "bottom:-2px" }) {
			HtmlDocument d = HtmlDocument.parse("<div style='position:absolute;" + declaration + "'>text</div>");
			assertTrue(declaration, d.boxLayout);
		}
		HtmlDocument reset = HtmlDocument.parse("<div style='position:relative;width:3px;width:auto;height:auto;"
				+ "right:2px;right:auto;bottom:auto'>text</div>");
		assertTrue(reset.positioned);
		assertFalse(reset.boxLayout);
		HtmlDocument staticInset = HtmlDocument.parse("<p style='right:0'>text</p>");
		assertFalse("the position summary remains independent of scene routing", staticInset.positioned);
		assertTrue(staticInset.boxLayout);
	}

	@Test
	public void flatPositionLanesSurviveGrowthAndDiscardedBlockReuse() {
		HtmlDocument d = HtmlDocument.parse("<div style='position:fixed;top:999px;left:888px;z-index:77'></div>"
				+ ("<p style='position:relative;top:-2px;left:3px;z-index:5'>positioned</p><p>plain</p>").repeat(20));
		assertEquals(40, d.blockCount);
		for (int b = 0; b < d.blockCount; b++) {
			boolean positioned = b % 2 == 0;
			assertEquals(positioned ? HtmlStyles.RELATIVE : HtmlStyles.STATIC, d.blockPosition[b]);
			assertEquals(positioned ? -2 : HtmlStyles.AUTO, d.blockTop[b]);
			assertEquals(positioned ? 3 : HtmlStyles.AUTO, d.blockLeft[b]);
			assertEquals(positioned ? 5 : 0, d.blockZ[b]);
		}
		assertTrue("the discarded empty fragment remains a positioned element box", d.boxLayout);
	}

	@Test
	public void nestedBoxesOwnDirectFragmentsAndContainDescendantRanges() {
		HtmlDocument d = HtmlDocument.parse("<div style='position:relative;left:3px;background:navy;margin-left:7px'>head"
				+ "<p style='position:absolute;top:4px;z-index:0'>child</p>tail</div><p>after</p>");
		assertEquals(4, d.boxCount);
		assertEquals(4, d.blockCount);
		assertArrayEquals(new int[] { -1, 0, 1, 0 }, Arrays.copyOf(d.boxParent, 4));
		assertArrayEquals(new int[] { 1, 2, -1, -1 }, Arrays.copyOf(d.boxFirstChild, 4));
		assertArrayEquals(new int[] { -1, 3, -1, -1 }, Arrays.copyOf(d.boxNextSibling, 4));
		assertArrayEquals(new int[] { 0, 0, 1, 3 }, Arrays.copyOf(d.boxFirstBlock, 4));
		assertArrayEquals(new int[] { 4, 3, 2, 4 }, Arrays.copyOf(d.boxEndBlock, 4));
		assertArrayEquals(new int[] { 1, 2, 1, 3 }, Arrays.copyOf(d.blockBox, 4));
		assertEquals("head", d.blockText(0));
		assertEquals("tail", d.blockText(2));
		assertEquals(HtmlStyles.RELATIVE, d.boxPosition[1]);
		assertEquals(HtmlStyles.ABSOLUTE, d.boxPosition[2]);
		assertEquals(3, d.boxLeft[1]);
		assertEquals(4, d.boxTop[2]);
		assertEquals(7, d.boxIndent[1]);
		assertEquals(0xFF000080, d.boxBackground[1]);
		assertFalse(d.boxZAuto[2]);
		assertTrue(d.boxZAuto[1]);
		assertTrue(d.positioned);
	}

	@Test
	public void emptyContainersKeepOrderAtIdenticalBlockOffsets() {
		HtmlDocument d = HtmlDocument.parse("<div style='position:absolute;width:10px;height:20px;background:red'></div>"
				+ "<div></div><p>A</p><div></div>");
		assertEquals(5, d.boxCount);
		assertEquals(1, d.blockCount);
		assertArrayEquals(new int[] { 0, 0, 0, 0, 1 }, Arrays.copyOf(d.boxFirstBlock, 5));
		assertArrayEquals(new int[] { 1, 0, 0, 1, 1 }, Arrays.copyOf(d.boxEndBlock, 5));
		assertArrayEquals(new int[] { -1, 2, 3, 4, -1 }, Arrays.copyOf(d.boxNextSibling, 5));
		assertEquals(3, d.blockBox[0]);
		assertEquals(10, d.boxWidth[1]);
		assertEquals(20, d.boxHeight[1]);
		assertEquals(0xFFFF0000, d.boxBackground[1]);
	}

	@Test
	public void boxBackgroundSurvivesWithoutItsOwnTextFragment() {
		HtmlDocument d = HtmlDocument.parse("<div style='position:relative;background:blue'><p>only child</p></div>");
		assertEquals(1, d.blockCount);
		assertEquals(0xFF0000FF, d.boxBackground[1]);
		assertEquals(0, d.boxBackground[2]);
		assertEquals(0, d.blockBackground[0]);
		assertEquals(2, d.blockBox[0]);
		assertEquals(0, d.boxFirstBlock[1]);
		assertEquals(1, d.boxEndBlock[1]);
	}

	@Test
	public void inlineGeometryIsIgnoredAndStaticZDoesNotActivateLayout() {
		HtmlDocument d = HtmlDocument.parse("<p style='left:10px;z-index:8'>a<span style='position:fixed;width:99px;z-index:1'>b</span>c</p>");
		assertEquals(2, d.boxCount);
		assertEquals("abc", d.blockText(0));
		assertFalse(d.positioned);
		assertFalse(d.boxLayout);
		assertEquals(HtmlStyles.STATIC, d.boxPosition[1]);
		assertEquals(HtmlStyles.AUTO, d.boxWidth[1]);
		assertEquals(8, d.boxZIndex[1]);
		assertFalse(d.boxZAuto[1]);
		assertTrue(HtmlDocument.parse("<p style='width:0'>x</p>").positioned);
		assertFalse(HtmlDocument.parse("<p style='width:10px;width:auto;position:fixed;position:static'>x</p>").positioned);
	}

	@Test
	public void imageAndRuleHaveTheirOwnNonInheritedStyle() {
		HtmlDocument d = HtmlDocument.parse("<div style='position:relative;left:30px;background:blue'><img src=x.png "
				+ "style='position:fixed;right:7px;bottom:8px;width:9px;height:10px;z-index:-2147483648;background:red'>"
				+ "<hr style='position:absolute;top:5px'></div>");
		assertEquals(4, d.boxCount);
		assertEquals(2, d.blockCount);
		assertArrayEquals(new int[] { 2, 3 }, Arrays.copyOf(d.blockBox, 2));
		assertEquals(HtmlDocument.IMAGE, d.kind[0]);
		assertEquals(HtmlDocument.RULE, d.kind[1]);
		assertEquals(HtmlStyles.FIXED, d.boxPosition[2]);
		assertEquals(HtmlStyles.AUTO, d.boxLeft[2]);
		assertEquals(7, d.boxRight[2]);
		assertEquals(8, d.boxBottom[2]);
		assertEquals(9, d.boxWidth[2]);
		assertEquals(10, d.boxHeight[2]);
		assertEquals(Integer.MIN_VALUE, d.boxZIndex[2]);
		assertFalse(d.boxZAuto[2]);
		assertEquals(0xFFFF0000, d.boxBackground[2]);
		assertEquals(0, d.boxBackground[3]);
		assertEquals(5, d.boxTop[3]);
	}

	@Test
	public void cellsRetainRolesWhileTheirInnerBlocksRemainCollapsed() {
		HtmlDocument d = HtmlDocument.parse("<table><tr><td>A<p style='position:absolute;width:10px'>B</p>"
				+ "<img src=x style='position:fixed'>C</td><td>D</td></tr></table>");
		assertEquals(5, d.boxCount);
		assertEquals(2, d.blockCount);
		assertArrayEquals(new int[] { HtmlDocument.NORMAL_BOX, HtmlDocument.TABLE_BOX, HtmlDocument.ROW_BOX,
				HtmlDocument.CELL_BOX, HtmlDocument.CELL_BOX }, Arrays.copyOf(d.boxRole, 5));
		assertArrayEquals(new int[] { -1, 0, 1, 2, 2 }, Arrays.copyOf(d.boxParent, 5));
		assertArrayEquals(new int[] { 3, 4 }, Arrays.copyOf(d.blockBox, 2));
		assertArrayEquals(new int[] { 0, 0, 0, 0, 1 }, Arrays.copyOf(d.boxFirstBlock, 5));
		assertArrayEquals(new int[] { 2, 2, 2, 1, 2 }, Arrays.copyOf(d.boxEndBlock, 5));
		assertEquals("A\nBC", d.blockText(0));
		assertFalse("cell-inner geometry is outside the supported box subset", d.positioned);
	}

	@Test
	public void omittedCellAndRowEndsDoNotNestSiblingBoxes() {
		HtmlDocument d = HtmlDocument.parse("<table><tr><td>a<td>b<tr><td>c");
		assertEquals(7, d.boxCount);
		assertEquals(3, d.blockCount);
		assertArrayEquals(new int[] { -1, 0, 1, 2, 2, 1, 5 }, Arrays.copyOf(d.boxParent, 7));
		assertArrayEquals(new int[] { 3, 4, 6 }, Arrays.copyOf(d.blockBox, 3));
		assertArrayEquals(new int[] { 3, 3, 2, 1, 2, 3, 3 }, Arrays.copyOf(d.boxEndBlock, 7));
		assertEquals(5, d.boxNextSibling[2]);
		assertEquals(4, d.boxNextSibling[3]);
	}

	@Test
	public void omittedTrCreatesOneAnonymousRowForSiblingCells() {
		HtmlDocument d = HtmlDocument.parse("<style>table {color:red} tr {position:fixed;background:blue}</style>"
				+ "<table><td>A</td><td>B</td></table>");
		assertEquals(5, d.boxCount);
		assertArrayEquals(new int[] { -1, 0, 1, 2, 2 }, Arrays.copyOf(d.boxParent, 5));
		assertArrayEquals(new int[] { HtmlDocument.NORMAL_BOX, HtmlDocument.TABLE_BOX, HtmlDocument.ROW_BOX,
				HtmlDocument.CELL_BOX, HtmlDocument.CELL_BOX }, Arrays.copyOf(d.boxRole, 5));
		assertArrayEquals(new int[] { 3, 4 }, Arrays.copyOf(d.blockBox, 2));
		assertEquals(0, d.boxFirstBlock[2]);
		assertEquals(2, d.boxEndBlock[2]);
		assertTrue(d.sameRow(0, 1));
		assertEquals(HtmlStyles.STATIC, d.boxPosition[2]);
		assertEquals(0, d.boxBackground[2]);
		assertEquals(0xFFFF0000, d.runColor[d.firstRun[0]]);
		assertEquals(0xFFFF0000, d.runColor[d.firstRun[1]]);
		assertFalse("an implied row does not match tr rules", d.positioned);
	}

	@Test
	public void explicitAndImpliedRowsRemainSiblings() {
		HtmlDocument d = HtmlDocument.parse("<table><td>A</td><td>B</td><tr><td>C</td></tr><td>D</td></table>");
		assertEquals(9, d.boxCount);
		assertArrayEquals(new int[] { -1, 0, 1, 2, 2, 1, 5, 1, 7 }, Arrays.copyOf(d.boxParent, 9));
		assertArrayEquals(new int[] { 3, 4, 6, 8 }, Arrays.copyOf(d.blockBox, 4));
		assertArrayEquals(new int[] { 0, 0, 1, 2 }, Arrays.copyOf(d.cellRow, 4));
		assertEquals(5, d.boxNextSibling[2]);
		assertEquals(7, d.boxNextSibling[5]);
		assertEquals(-1, d.boxNextSibling[7]);
		assertEquals(2, d.boxEndBlock[2]);
		assertEquals(3, d.boxEndBlock[5]);
		assertEquals(4, d.boxEndBlock[7]);
	}

	@Test
	public void strayTableTextOwnsAnonymousCellsInItsExistingRows() {
		HtmlDocument d = HtmlDocument.parse("<style>table {color:red} tr {position:fixed} td {background:blue}</style>"
				+ "<table>stray<td>B</td><tr><td>C</td></tr>tail</table>");
		assertEquals(9, d.boxCount);
		assertEquals(4, d.blockCount);
		assertArrayEquals(new int[] { -1, 0, 1, 2, 2, 1, 5, 1, 7 }, Arrays.copyOf(d.boxParent, 9));
		assertArrayEquals(new int[] { 3, 4, 6, 8 }, Arrays.copyOf(d.blockBox, 4));
		assertArrayEquals(new int[] { 0, 0, 1, 2 }, Arrays.copyOf(d.cellRow, 4));
		assertArrayEquals(new int[] { 0, 1, 0, 0 }, Arrays.copyOf(d.cellColumn, 4));
		assertEquals("stray", d.blockText(0));
		assertEquals("tail", d.blockText(3));
		assertEquals(HtmlDocument.CELL_BOX, d.boxRole[3]);
		assertEquals(HtmlDocument.CELL_BOX, d.boxRole[8]);
		assertEquals(0, d.boxBackground[3]);
		assertEquals(0xFF0000FF, d.boxBackground[4]);
		assertEquals(0, d.boxBackground[8]);
		assertEquals(HtmlStyles.STATIC, d.boxPosition[2]);
		assertEquals(HtmlStyles.FIXED, d.boxPosition[5]);
		assertEquals(HtmlStyles.STATIC, d.boxPosition[7]);
		assertEquals(0xFFFF0000, d.runColor[d.firstRun[0]]);
		assertEquals(0, d.boxFirstBlock[3]);
		assertEquals(1, d.boxEndBlock[3]);
		assertEquals(3, d.boxFirstBlock[8]);
		assertEquals(4, d.boxEndBlock[8]);
	}

	@Test
	public void imageAndRuleIndentIncludesTheirOwnMarginExactlyOnce() {
		HtmlDocument d = HtmlDocument.parse("<blockquote><img src='x' style='position:relative;margin-left:12px'>"
				+ "<hr style='position:relative;margin-left:9px'></blockquote>");
		assertEquals(12, d.boxIndent[d.blockBox[0]]);
		assertEquals(12 + HtmlDocument.QUOTE_INDENT, d.blockIndent[0]);
		assertEquals(9, d.boxIndent[d.blockBox[1]]);
		assertEquals(9 + HtmlDocument.QUOTE_INDENT, d.blockIndent[1]);
	}

	@Test
	public void elementBoxesCarryTheAcceptedBlockBoxModelWithoutInheritance() {
		HtmlDocument d = HtmlDocument.parse("<div style='position:relative;margin:1px 2px 3px 4px;"
				+ "padding:5px 6px 7px 8px;border:9px solid red'>head<p>child</p></div>");
		assertEquals(1, d.boxMarginTop[1]);
		assertEquals(2, d.boxMarginRight[1]);
		assertEquals(3, d.boxMarginBottom[1]);
		assertEquals(4, d.boxIndent[1]);
		assertEquals(5, d.boxPadTop[1]);
		assertEquals(6, d.boxPadRight[1]);
		assertEquals(7, d.boxPadBottom[1]);
		assertEquals(8, d.boxPadLeft[1]);
		assertEquals(9, d.boxBorder[1]);
		assertEquals(0xFFFF0000, d.boxBorderColor[1]);
		assertEquals(d.boxMarginTop[1], d.blockMarginTop[0]);
		assertEquals(d.boxMarginRight[1], d.blockMarginRight[0]);
		assertEquals(d.boxMarginBottom[1], d.blockMarginBottom[0]);
		assertEquals(d.boxPadTop[1], d.blockPadTop[0]);
		assertEquals(d.boxPadRight[1], d.blockPadRight[0]);
		assertEquals(d.boxPadBottom[1], d.blockPadBottom[0]);
		assertEquals(d.boxPadLeft[1], d.blockPadLeft[0]);
		assertEquals(d.boxBorder[1], d.blockBorder[0]);
		assertEquals(d.boxBorderColor[1], d.blockBorderColor[0]);
		for (int[] lane : new int[][] { d.boxMarginTop, d.boxMarginRight, d.boxMarginBottom, d.boxPadTop, d.boxPadRight,
				d.boxPadBottom, d.boxPadLeft, d.boxBorder, d.boxBorderColor }) assertEquals(0, lane[2]);
	}

	@Test
	public void cellBoxesRetainTablePaddingAndBorderSemantics() {
		HtmlDocument d = HtmlDocument.parse("<table border=1><tr><td style='position:relative;margin:9px;"
				+ "padding:8px;border:7px solid red;background:blue'>cell</td></tr></table>");
		int cell = d.blockBox[0];
		assertEquals(HtmlDocument.CELL_BOX, d.boxRole[cell]);
		assertEquals(0, d.boxIndent[cell]);
		for (int[] lane : new int[][] { d.boxMarginTop, d.boxMarginRight, d.boxMarginBottom, d.boxPadTop, d.boxPadRight,
				d.boxPadBottom, d.boxPadLeft, d.boxBorder, d.boxBorderColor }) assertEquals(0, lane[cell]);
		assertEquals(0, d.blockPadTop[0]);
		assertEquals(0, d.blockBorder[0]);
		assertEquals(1, d.tableBorder[0]);
		assertEquals(HtmlStyles.RELATIVE, d.boxPosition[cell]);
		assertEquals(0xFF0000FF, d.boxBackground[cell]);
		assertEquals(HtmlStyles.STATIC, d.blockPosition[0]);
		assertEquals(HtmlStyles.AUTO, d.blockTop[0]);
		assertEquals(HtmlStyles.AUTO, d.blockLeft[0]);
		assertEquals(0, d.blockZ[0]);
	}

	@Test
	public void boxModelLanesSurviveCapacityGrowthAndEmptyBlockReuse() {
		HtmlDocument d = HtmlDocument.parse("<div style='padding:99px;border:88px solid red'></div>"
				+ "<p style='margin:1px;padding:2px;border:3px solid blue'>x</p>".repeat(40));
		assertEquals(40, d.blockCount);
		assertEquals(42, d.boxCount);
		assertEquals(99, d.boxPadTop[1]);
		for (int b = 0; b < d.blockCount; b++) {
			int box = d.blockBox[b];
			assertEquals(1, d.blockMarginTop[b]);
			assertEquals(2, d.blockPadLeft[b]);
			assertEquals(3, d.blockBorder[b]);
			assertEquals(0xFF0000FF, d.blockBorderColor[b]);
			assertEquals(1, d.boxMarginBottom[box]);
			assertEquals(2, d.boxPadRight[box]);
			assertEquals(3, d.boxBorder[box]);
			assertEquals(0xFF0000FF, d.boxBorderColor[box]);
		}
	}

	@Test
	public void misnestedEndAndEndOfInputCloseEveryRange() {
		HtmlDocument d = HtmlDocument.parse("<div><p>before</div>after</p><div><p>open");
		assertEquals(5, d.boxCount);
		assertEquals(3, d.blockCount);
		assertArrayEquals(new int[] { 2, 0, 4 }, Arrays.copyOf(d.blockBox, 3));
		assertArrayEquals(new int[] { 3, 1, 1, 3, 3 }, Arrays.copyOf(d.boxEndBlock, 5));
		assertEquals(3, d.boxNextSibling[1]);
		assertEquals("after", d.blockText(1));
		HtmlDocument empty = HtmlDocument.parse("");
		assertEquals(1, empty.boxCount);
		assertEquals(-1, empty.boxParent[0]);
		assertEquals(-1, empty.boxFirstChild[0]);
		assertEquals(0, empty.boxEndBlock[0]);
		assertTrue(empty.boxZAuto[0]);
	}

	@Test
	public void boxAndFrameGrowthPreservePreorderAndBodyAncestry() {
		StringBuilder html = new StringBuilder("<body>");
		for (int i = 0; i < 40; i++) html.append("<div>");
		html.append("inside");
		for (int i = 0; i < 40; i++) html.append("</div>");
		html.append("</body><p>after</p>");
		HtmlDocument d = HtmlDocument.parse(html.toString());
		assertEquals(43, d.boxCount);
		assertEquals(2, d.blockCount);
		assertArrayEquals(new int[] { 41, 42 }, Arrays.copyOf(d.blockBox, 2));
		for (int box = 1; box <= 41; box++) {
			assertEquals(box - 1, d.boxParent[box]);
			assertEquals(0, d.boxFirstBlock[box]);
			assertEquals(1, d.boxEndBlock[box]);
			assertEquals(HtmlStyles.AUTO, d.boxLeft[box]);
			assertTrue(d.boxZAuto[box]);
		}
		assertEquals(-1, d.boxFirstChild[41]);
		assertEquals(42, d.boxNextSibling[1]);
		assertEquals(-1, d.boxNextSibling[42]);
		assertFalse(d.positioned);
	}
}
