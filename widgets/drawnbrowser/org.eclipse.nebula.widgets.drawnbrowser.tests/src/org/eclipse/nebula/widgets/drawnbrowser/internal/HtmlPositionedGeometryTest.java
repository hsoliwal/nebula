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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

/** Exact geometry and picking contracts with deterministic text metrics, without an SWT display. */
public class HtmlPositionedGeometryTest {

	@Test
	public void relativeOffsetsPreserveNormalFlowAllocation() {
		Scene scene = new Scene("<p style='position:relative;left:7px;top:20px'>first</p><p>second</p>", 200, 100);
		assertEquals(7, scene.layout.blockX(0));
		assertEquals(20, scene.layout.blockY(0, 0));
		assertEquals(30, scene.layout.blockY(1, 0));
		assertEquals(60, scene.layout.boxHeight(0));
		assertEquals(60, scene.layout.totalHeight());
	}

	@Test
	public void overflowClipRestrictsChildPaintAndPickingWithoutClippingOwnBorder() {
		Scene scene = new Scene("<div style='position:relative;width:100px;height:40px;overflow:hidden;border:2px solid red'>"
				+ "<p style='position:absolute;left:80px;top:5px;width:60px;height:20px'>child</p></div>", 400, 200);
		int parent = scene.document.boxParent[scene.document.blockBox[0]], child = scene.document.blockBox[0];
		assertTrue("panel background draws before its own clip", !scene.layout.hasClip(parent, false));
		assertTrue(scene.layout.hasClip(parent, true));
		assertTrue(scene.layout.hasClip(child, false));
		int[] clip = { 0, 0, 400, 200 };
		assertTrue(scene.layout.clipBounds(child, true, 0, clip));
		assertEquals(scene.layout.boxX(parent) + 2, clip[0]);
		assertEquals(scene.layout.boxX(parent) + scene.layout.boxWidth(parent) - 2, clip[2]);
		assertEquals(0, scene.layout.blockAt(clip[2] - 8, 12, 0));
		assertEquals(-1, scene.layout.blockAt(clip[2] + 8, 12, 0));
	}

	@Test
	public void nestedFixedOverflowClipsRemainInViewportCoordinatesAcrossScroll() {
		Scene scene = new Scene("<div style='position:fixed;left:20px;top:12px;width:110px;height:50px;overflow:hidden;border:3px solid blue'>"
				+ "<div style='position:relative;left:60px;top:10px;width:70px;height:40px;overflow:clip;border:2px solid red'>"
				+ "<p>nested</p></div></div><p>normal</p>", 400, 180);
		int block = scene.document.blockBox[0];
		int[] original = { 0, 0, 400, 180 }, scrolled = { 0, 0, 400, 180 };
		assertTrue(scene.layout.clipBounds(block, true, 0, original));
		assertTrue(scene.layout.clipBounds(block, true, 125, scrolled));
		assertArrayEquals("both clips of a fixed subtree use screen pixels", original, scrolled);
		assertEquals(scene.layout.boxX(1) + scene.layout.boxWidth(1) - 3, original[2]);
	}

	@Test
	public void nearestTextSelectionSkipsFullyHiddenOverflowChildren() {
		Scene scene = new Scene("<div style='position:relative;width:30px;height:30px;overflow:hidden'>"
				+ "<p style='position:absolute;left:80px;top:1px;width:20px;height:10px'>hidden</p></div>"
				+ "<p>visible</p>", 200, 100);
		assertEquals(-1, scene.layout.blockAt(85, 5, 0));
		assertEquals("selection must not lock onto completely invisible text", 1, scene.layout.nearestBlock(85, 5, 0));
	}

	@Test
	public void relativeVisualOverflowExtendsTheScrollableExtent() {
		Scene scene = new Scene("<p style='position:relative;top:70px'>first</p><p>second</p>", 200, 100);
		assertEquals(30, scene.layout.blockY(1, 0));
		assertEquals(60, scene.layout.boxHeight(0));
		assertEquals(100, scene.layout.totalHeight());
	}

	@Test
	public void styledLeafSeparatesBorderContentAndFlowAllocation() {
		String template = "<p style='margin:10px 20px 30px 40px;padding:2px 4px 6px 8px;border:2px solid red;%s'>box</p>"
				+ "<p>after</p>";
		Scene plain = new Scene(String.format(template, ""), 300, 200);
		Scene shifted = new Scene(String.format(template, "position:relative;left:9px;top:11px"), 300, 200);
		int box = plain.document.blockBox[0];
		assertEquals(40, plain.layout.boxX(box));
		assertEquals(18, plain.layout.boxY(box, 0));
		assertEquals(240, plain.layout.boxWidth(box));
		assertEquals(26, plain.layout.boxHeight(box));
		assertEquals(50, plain.layout.blockX(0));
		assertEquals(0, plain.layout.blockY(0, 0));
		assertEquals(22, plain.layout.blockContentTop(0));
		assertEquals(82, plain.layout.blockHeight(0));
		assertEquals(82, plain.layout.blockY(1, 0));
		assertEquals(112, plain.layout.totalHeight());
		assertEquals(49, shifted.layout.boxX(box));
		assertEquals(29, shifted.layout.boxY(box, 0));
		assertEquals(59, shifted.layout.blockX(0));
		assertEquals(11, shifted.layout.blockY(0, 0));
		assertEquals(22, shifted.layout.blockContentTop(0));
		assertEquals(82, shifted.layout.blockY(1, 0));
		assertEquals(112, shifted.layout.totalHeight());
	}

	@Test
	public void styledWrapperDoesNotInventLeafGapsAroundItsChildren() {
		Scene scene = new Scene("<div style='position:relative;margin:5px;padding:10px;border:2px solid blue'>"
				+ "<p>inner</p></div>", 300, 200);
		int parent = scene.document.boxParent[scene.document.blockBox[0]];
		assertEquals(5, scene.layout.boxX(parent));
		assertEquals(5, scene.layout.boxY(parent, 0));
		assertEquals(290, scene.layout.boxWidth(parent));
		assertEquals(54, scene.layout.boxHeight(parent));
		assertEquals(17, scene.layout.blockX(0));
		assertEquals(17, scene.layout.blockY(0, 0));
		assertEquals(8, scene.layout.blockContentTop(0));
		assertEquals(64, scene.layout.totalHeight());
	}

	@Test
	public void absoluteContainingBlockUsesPaddingEdgesInsideTheBorder() {
		Scene scene = new Scene("<div style='position:relative;width:100px;height:70px;padding:10px;border:2px solid blue'>"
				+ "<p style='position:absolute;left:0;top:0;width:20px;height:10px'>left</p>"
				+ "<p style='position:absolute;right:0;bottom:0;width:20px;height:10px'>right</p></div>", 300, 200);
		int parent = scene.document.boxParent[scene.document.blockBox[0]];
		assertEquals(124, scene.layout.boxWidth(parent));
		assertEquals(94, scene.layout.boxHeight(parent));
		assertEquals(2, scene.layout.blockX(0));
		assertEquals(2, scene.layout.blockY(0, 0));
		assertEquals(102, scene.layout.blockX(1));
		assertEquals(82, scene.layout.blockY(1, 0));
	}

	@Test
	public void ownerPaddingAndBorderAreChargedOnceAcrossHeadChildAndTail() {
		Scene scene = new Scene("<div style='position:relative;padding:5px;border:2px solid red'>head<p>inner</p>tail</div>"
				+ "<p>after</p>", 300, 200);
		int box = scene.document.blockBox[0];
		assertEquals(8, scene.layout.boxY(box, 0));
		assertEquals(88, scene.layout.boxHeight(box));
		assertEquals(0, scene.layout.blockY(0, 0));
		assertEquals(15, scene.layout.blockContentTop(0));
		assertEquals(37, scene.layout.blockY(1, 0));
		assertEquals(67, scene.layout.blockY(2, 0));
		assertEquals(8, scene.layout.blockContentTop(2));
		assertEquals(104, scene.layout.blockY(3, 0));
	}

	@Test
	public void styledListBorderIncludesLegacyIndentWithoutDoublingContentInset() {
		Scene scene = new Scene("<ul><li style='position:relative;padding:5px;border:2px solid red'>item</li></ul>", 300, 200);
		int box = scene.document.blockBox[0];
		assertEquals(24, scene.layout.boxX(box));
		assertEquals(276, scene.layout.boxWidth(box));
		assertEquals(31, scene.layout.blockX(0));
		assertEquals(262, scene.layout.blockWidth(0));
		Scene quoted = new Scene("<blockquote><ul><li style='position:relative;padding:5px;border:2px solid red'>item</li></ul></blockquote>",
				300, 200);
		assertEquals(56, quoted.layout.boxX(quoted.document.blockBox[0]));
		assertEquals(63, quoted.layout.blockX(0));
	}

	@Test
	public void dragAboveAllContentChoosesTheFirstVerticalFragment() {
		Scene scene = new Scene("<p style='position:relative;left:40px'>first</p><p>second</p>", 300, 200);
		assertEquals(0, scene.layout.nearestBlock(-100, -100, 0));
		assertEquals(1, scene.layout.nearestBlock(-100, 100, 0));
	}

	@Test
	public void relativeEndInsetsNegateAndStartInsetsWinInLeftToRightLayout() {
		Scene scene = new Scene("<p style='position:relative;right:7px;bottom:9px'>end</p>"
				+ "<p style='position:relative;left:4px;right:20px;top:3px;bottom:30px'>both</p>", 200, 100);
		assertEquals(-7, scene.layout.blockX(0));
		assertEquals(-9, scene.layout.blockY(0, 0));
		assertEquals(4, scene.layout.blockX(1));
		assertEquals(33, scene.layout.blockY(1, 0));
	}

	@Test
	public void absoluteAndFixedBoxesTakeNoFlowSpace() {
		Scene scene = new Scene("<p>before</p><p style='position:absolute;top:5px;width:40px;height:300px'>absolute</p>"
				+ "<p style='position:fixed;top:1000px;width:40px;height:500px'>fixed</p><p>after</p>", 200, 100);
		assertEquals(30, scene.layout.blockY(3, 0));
		assertEquals(60, scene.layout.boxHeight(0));
		assertEquals("absolute overflow is scrollable, fixed overflow is not", 305, scene.layout.totalHeight());
	}

	@Test
	public void absoluteUsesNearestPositionedAncestorAcrossStaticWrappers() {
		Scene scene = new Scene("<div style='position:relative;left:40px;top:10px;width:180px;height:100px'>"
				+ "<div style='margin-left:30px;width:100px'><p>normal</p>"
				+ "<p style='position:absolute;left:7px;top:9px;width:20px;height:10px'>absolute</p></div></div>", 300, 200);
		assertEquals(70, scene.layout.blockX(0));
		assertEquals(47, scene.layout.blockX(1));
		assertEquals(19, scene.layout.blockY(1, 0));
	}

	@Test
	public void unpositionedAncestorDoesNotCaptureAbsoluteContainingBlock() {
		Scene scene = new Scene("<div style='margin-left:50px;width:80px;height:100px'>"
				+ "<p style='position:absolute;right:10px;top:7px;width:40px;height:20px'>absolute</p></div>", 200, 100);
		assertEquals(150, scene.layout.blockX(0));
		assertEquals(7, scene.layout.blockY(0, 0));
	}

	@Test
	public void opposingInsetsStretchAutomaticSize() {
		Scene scene = new Scene("<p style='position:absolute;left:20px;right:30px;top:10px;bottom:15px'>stretch</p>", 200, 100);
		int box = scene.document.blockBox[0];
		assertEquals(20, scene.layout.boxX(box));
		assertEquals(10, scene.layout.boxY(box, 0));
		assertEquals(150, scene.layout.boxWidth(box));
		assertEquals(75, scene.layout.boxHeight(box));
		assertEquals("fully constrained width needs no intrinsic-width pass", 0, scene.metrics.intrinsicCalls);
	}

	@Test
	public void rightAndBottomAnchorExplicitSize() {
		Scene scene = new Scene("<p style='position:absolute;right:10px;bottom:15px;width:40px;height:20px'>corner</p>", 200, 80);
		assertEquals(150, scene.layout.blockX(0));
		assertEquals(45, scene.layout.blockY(0, 0));
	}

	@Test
	public void explicitSizeWithAllInsetsKeepsStartEdges() {
		Scene scene = new Scene("<p style='position:absolute;left:4px;right:20px;top:3px;bottom:30px;width:30px;height:10px'>box</p>",
				200, 100);
		int box = scene.document.blockBox[0];
		assertEquals(4, scene.layout.boxX(box));
		assertEquals(3, scene.layout.boxY(box, 0));
		assertEquals(30, scene.layout.boxWidth(box));
		assertEquals(10, scene.layout.boxHeight(box));
	}

	@Test
	public void automaticInsetsUseStaticPositionWithoutConsumingSpace() {
		Scene scene = new Scene("<p>before</p><p style='position:absolute;width:40px;height:10px'>absolute</p><p>after</p>", 200, 100);
		assertEquals(30, scene.layout.blockY(1, 0));
		assertEquals(30, scene.layout.blockY(2, 0));
	}

	@Test
	public void automaticAbsoluteWidthUsesMinimumAndPreferredContentWidths() {
		Scene scene = new Scene("<p style='position:absolute'>shrink</p>", 50, 100);
		int box = scene.document.blockBox[0];
		assertEquals(50, scene.layout.boxWidth(box));
		scene.layout.layout(100, 100, scene.metrics);
		assertEquals(80, scene.layout.boxWidth(box));
		scene.layout.layout(10, 100, scene.metrics);
		assertEquals("minimum content width may overflow", 20, scene.layout.boxWidth(box));
	}

	@Test
	public void fixedDescendantsUseScrollProjectionWithoutRemeasuring() {
		Scene scene = new Scene("<div style='position:fixed;left:7px;top:10px;width:60px;height:40px'><p>flow-child</p>"
				+ "<p style='position:absolute;left:3px;top:5px;width:20px;height:10px'>absolute-child</p></div><p>normal</p>", 200, 100);
		int measurements = scene.metrics.heightCalls;
		assertTrue(scene.layout.hasFixed());
		for (int scroll : new int[] { 0, 5, 100, 2500, 0 }) {
			assertEquals(10 + scroll, scene.layout.blockY(0, scroll));
			assertEquals(15 + scroll, scene.layout.blockY(1, scroll));
			assertEquals(0, scene.layout.blockY(2, scroll));
			assertEquals(1, scene.layout.blockAt(11, 16 + scroll, scroll));
		}
		assertEquals(measurements, scene.metrics.heightCalls);
	}

	@Test
	public void fixedOnlyPageDoesNotGrowScrollableHeight() {
		Scene scene = new Scene("<p style='position:fixed;top:1000px;width:20px;height:1000px'>fixed</p>", 200, 100);
		assertEquals(0, scene.layout.boxHeight(0));
		assertEquals(0, scene.layout.totalHeight());
	}

	@Test
	public void positionedAncestorDoesNotCaptureViewportFixedCoordinates() {
		Scene scene = new Scene("<div style='position:relative;left:50px;top:40px;width:100px;height:80px'>"
				+ "<p style='position:fixed;left:5px;top:7px;width:20px;height:10px'>fixed</p></div>", 200, 100);
		assertEquals(5, scene.layout.blockX(0));
		assertEquals(7, scene.layout.blockY(0, 0));
		assertEquals(107, scene.layout.blockY(0, 100));
	}

	@Test
	public void nestedFixedUsesViewportWhileAbsoluteUsesFixedAncestor() {
		Scene scene = new Scene("<div style='position:fixed;left:30px;top:40px;width:100px;height:80px'>"
				+ "<div style='margin-left:10px'><p style='position:absolute;left:5px;top:7px;width:20px;height:10px'>absolute</p>"
				+ "<p style='position:fixed;left:5px;top:7px;width:20px;height:10px'>fixed</p></div></div>", 200, 100);
		assertEquals(35, scene.layout.blockX(0));
		assertEquals(147, scene.layout.blockY(0, 100));
		assertEquals(5, scene.layout.blockX(1));
		assertEquals(107, scene.layout.blockY(1, 100));
	}

	@Test
	public void emptySizedBoxConsumesFlowAndHasNoContentTarget() {
		Scene scene = new Scene("<div style='height:40px;background:red'></div><p>after</p>", 200, 100);
		assertEquals(40, scene.layout.blockY(0, 0));
		assertEquals(70, scene.layout.totalHeight());
		assertEquals(-1, scene.layout.blockAt(1, 1, 0));
		assertEquals(0, scene.layout.blockAt(1, 41, 0));
	}

	@Test
	public void emptyForegroundBoxBlocksUnderlyingContentPicking() {
		Scene scene = new Scene("<p>under</p><div style='position:absolute;left:0;top:0;width:20px;height:20px;z-index:1'></div>",
				200, 100);
		assertEquals(-1, scene.layout.blockAt(1, 1, 0));
		assertEquals(0, scene.layout.blockAt(25, 1, 0));
	}

	@Test
	public void pickingDistinguishesAutoFromIntegerZeroContexts() {
		String template = "<div style='position:relative;z-index:%s;width:100px;height:20px'>parent"
				+ "<p style='position:absolute;left:0;top:0;width:50px;height:20px;z-index:999'>child</p></div>"
				+ "<p style='position:absolute;left:0;top:0;width:50px;height:20px;z-index:1'>sibling</p>";
		Scene auto = new Scene(String.format(template, "auto"), 200, 100);
		Scene zero = new Scene(String.format(template, "0"), 200, 100);
		assertEquals(1, auto.layout.blockAt(5, 5, 0));
		assertEquals(2, zero.layout.blockAt(5, 5, 0));
	}

	@Test
	public void contextDoesNotClipVisibleOverflow() {
		Scene scene = new Scene("<div style='position:relative;z-index:0;width:10px;height:10px'>"
				+ "<p style='position:absolute;left:20px;top:20px;width:30px;height:20px'>outside</p></div>", 200, 100);
		assertEquals(0, scene.layout.blockAt(21, 21, 0));
		assertEquals(40, scene.layout.boxY(scene.document.blockBox[0], 0) + scene.layout.boxHeight(scene.document.blockBox[0]));
	}

	@Test
	public void relayoutResetsLocalPositionsAndReanchorsFixedEdges() {
		Scene scene = new Scene("<p>one</p><p>two</p><p style='position:fixed;right:10px;bottom:15px;width:40px;height:20px'>fixed</p>",
				200, 100);
		assertEquals(150, scene.layout.blockX(2));
		assertEquals(65, scene.layout.blockY(2, 0));
		scene.layout.layout(300, 200, scene.metrics);
		assertEquals(250, scene.layout.blockX(2));
		assertEquals(165, scene.layout.blockY(2, 0));
		scene.layout.layout(200, 100, scene.metrics);
		assertEquals(0, scene.layout.blockY(0, 0));
		assertEquals(30, scene.layout.blockY(1, 0));
		assertEquals(150, scene.layout.blockX(2));
		assertEquals(65, scene.layout.blockY(2, 0));
		assertEquals(60, scene.layout.totalHeight());
	}

	@Test
	public void coordinateEndpointsDoNotWrapDuringExtentAndHitCalculations() {
		Scene scene = new Scene("<p style='position:absolute;left:2147483647px;top:2147483647px;width:10px;height:10px'>edge</p>",
				200, 100);
		assertEquals(Integer.MAX_VALUE, scene.layout.blockX(0));
		assertEquals(Integer.MAX_VALUE, scene.layout.blockY(0, 0));
		assertEquals(Integer.MAX_VALUE, scene.layout.totalHeight());
		assertEquals(0, scene.layout.blockAt(Integer.MAX_VALUE, Integer.MAX_VALUE, 0));
		assertEquals(-1, scene.layout.blockAt(Integer.MIN_VALUE, Integer.MIN_VALUE, 0));
	}

	@Test
	public void positionedTableKeepsRowsColumnsAndTallestCellHeight() {
		Scene scene = tableScene();
		assertEquals(0, scene.layout.blockY(0, 0));
		assertEquals(0, scene.layout.blockY(1, 0));
		assertEquals(40, scene.layout.blockHeight(0));
		assertEquals(40, scene.layout.blockHeight(1));
		assertEquals(40, scene.layout.blockY(2, 0));
		assertEquals(40, scene.layout.blockY(3, 0));
		assertEquals(15, scene.layout.blockHeight(2));
		assertEquals(15, scene.layout.blockHeight(3));
		assertEquals(9, scene.layout.blockX(0));
		assertEquals(69, scene.layout.blockX(1));
		assertEquals(52, scene.layout.blockWidth(0));
		assertEquals(72, scene.layout.blockWidth(1));
		assertEquals(55, scene.layout.totalHeight());
		assertEquals(1, scene.metrics.columnCalls);
	}

	@Test
	public void positionedTableCellPickingIncludesItsPadding() {
		Scene scene = tableScene();
		assertEquals(0, scene.layout.blockAt(6, 1, 0));
		assertEquals(1, scene.layout.blockAt(66, 1, 0));
		assertEquals(2, scene.layout.blockAt(6, 41, 0));
	}

	@Test
	public void automaticTableAndWrapperWidthsUseTheSumOfColumnMaxima() {
		HtmlDocument document = HtmlDocument.parse("<div style='position:absolute;top:0'><table>"
				+ "<tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></table></div>");
		HtmlPositionedLayout layout = new HtmlPositionedLayout(document);
		layout.layout(200, 100, new HtmlPositionedLayout.Metrics() {
			@Override
			public int height(int block, int width) { return 20; }

			@Override
			public long intrinsicWidths(int block) {
				// As in HtmlLayout, each pair already includes both cell paddings.
				int preferred = block == 0 || block == 3 ? 80 : 20;
				return (18L << 32) | preferred;
			}

			@Override
			public int[] columns(int table, int width) {
				return TableColumns.edges(new int[] { 18, 18 }, new int[] { 80, 80 }, width);
			}
		});
		int table = document.boxParent[document.boxParent[document.blockBox[0]]];
		assertEquals(160, layout.boxWidth(table));
		assertEquals(160, layout.boxWidth(document.boxParent[table]));
		assertEquals(80, layout.boxWidth(document.blockBox[0]));
		assertEquals(80, layout.boxWidth(document.blockBox[1]));
	}

	@Test
	public void viewportIndexMatchesIndependentScanForSignedPositionsAndFixedScroll() {
		Random random = new Random(0x5A17C0DEL);
		StringBuilder html = new StringBuilder();
		int[] heights = new int[321];
		for (int n = 0; n < heights.length; n++) {
			int top = random.nextInt(4001) - 2000, height = random.nextInt(301);
			html.append("<p style='position:").append(n % 4 == 0 ? "fixed" : "absolute")
					.append(";top:").append(top).append("px;height:").append(height)
					.append("px;width:40px;z-index:").append(random.nextInt(21) - 10).append("'>")
					.append(n).append("</p>");
			heights[n] = random.nextInt(101);
		}
		Scene scene = new Scene(html.toString(), 200, 100, heights);
		int measurements = scene.metrics.heightCalls;
		for (int query = 0; query < 300; query++) {
			assertVisible(scene, random.nextInt(6001) - 3000, random.nextInt(1001), random.nextInt(4001) - 2000);
		}
		assertEquals("viewport queries never remeasure", measurements, scene.metrics.heightCalls);
	}

	@Test
	public void viewportIndexFindsTallAncestorBackgroundBeyondItsText() {
		Scene scene = new Scene("<div style='position:absolute;top:-1000000px;width:100px;height:2000000px'>"
				+ "<p>far-away-text</p><div style='position:relative;top:999970px;width:50px;height:500000px'></div></div>"
				+ "<p style='position:fixed;top:5px;width:20px;height:20px'>fixed</p>", 200, 100);
		assertVisible(scene, 0, 100, 0);
		assertVisible(scene, 250000, 10, 250000);
		assertVisible(scene, 999999, 1, 999999);
		assertVisible(scene, 1000000, 10, 0);
	}

	@Test
	public void viewportIndexUsesHalfOpenIntervalsAndRejectsEmptyQueries() {
		Scene scene = new Scene("<p style='position:absolute;top:10px;width:20px;height:20px'>normal</p>"
				+ "<p style='position:fixed;top:-10px;width:20px;height:10px'>fixed</p>", 200, 100, 20, 10);
		assertVisible(scene, 0, 10, 0);
		assertVisible(scene, 10, 1, 0);
		assertVisible(scene, 29, 1, 0);
		assertVisible(scene, 30, 1, 0);
		assertVisible(scene, 10, 0, 0);
		assertVisible(scene, 10, -1, 0);
		assertVisible(scene, -10, 10, 0);
	}

	@Test
	public void viewportIndexMatchesProjectedGeometryAtBothIntegerEndpoints() {
		Scene scene = new Scene("<p style='position:absolute;top:-2147483648px;width:20px;height:40px'>low</p>"
				+ "<p style='position:absolute;top:2147483647px;width:20px;height:40px'>high</p>"
				+ "<p style='position:fixed;top:2147483647px;width:20px;height:40px'>fixed-high</p>"
				+ "<p style='position:fixed;top:-2147483648px;width:20px;height:40px'>fixed-low</p>", 200, 100);
		for (int scroll : new int[] { 0, 1, -1, 100, -100, Integer.MAX_VALUE, Integer.MIN_VALUE + 1 }) {
			assertVisible(scene, Integer.MAX_VALUE, 1, scroll);
			assertVisible(scene, Integer.MIN_VALUE, 100, scroll);
			assertVisible(scene, -1, Integer.MAX_VALUE, scroll);
		}
	}

	@Test
	public void relativeMinimumEndInsetsSaturateAfterLongNegation() {
		Scene scene = new Scene("<p style='position:relative;right:-2147483648px;bottom:-2147483648px'>edge</p>", 200, 100);
		assertEquals(Integer.MAX_VALUE, scene.layout.blockX(0));
		assertEquals(Integer.MAX_VALUE, scene.layout.blockY(0, 0));
	}

	private static void assertVisible(Scene scene, int top, int extent, int scroll) {
		HtmlPaintOrder order = scene.layout.order();
		int[] expected = new int[order.size()];
		int count = 0;
		if (extent > 0) {
			long bottom = (long) top + extent;
			for (int step = 0; step < order.size(); step++) {
				int index = order.index(step);
				int y = order.isBackground(step) ? scene.layout.boxY(index, scroll) : scene.layout.blockY(index, scroll);
				int height = order.isBackground(step) ? scene.layout.boxHeight(index) : scene.layout.blockHeight(index);
				if (height > 0 && y < bottom && (long) y + height > top) expected[count++] = step;
			}
		}
		int found = scene.layout.collectVisible(top, extent, scroll);
		int[] actual = new int[found];
		for (int n = 0; n < found; n++) actual[n] = scene.layout.visibleStep(n);
		assertArrayEquals("visible steps for top=" + top + ", extent=" + extent + ", scroll=" + scroll,
				Arrays.copyOf(expected, count), actual);
	}

	private static Scene tableScene() {
		return new Scene("<div style='position:relative;left:5px'><table><tr><td>short</td><td>tall</td></tr>"
				+ "<tr><td>next</td><td>end</td></tr></table></div>", 200, 100, 12, 40, 15, 10);
	}

	private static final class Scene {
		final HtmlDocument document;
		final Measures metrics;
		final HtmlPositionedLayout layout;

		Scene(String html, int width, int height, int... heights) {
			document = HtmlDocument.parse(html);
			metrics = new Measures(document, heights);
			layout = new HtmlPositionedLayout(document);
			layout.layout(width, height, metrics);
		}
	}

	private static final class Measures implements HtmlPositionedLayout.Metrics {
		final HtmlDocument document;
		final int[] heights;
		int heightCalls, intrinsicCalls, columnCalls;

		Measures(HtmlDocument document, int[] heights) {
			this.document = document;
			this.heights = heights;
		}

		@Override
		public int height(int block, int width) {
			heightCalls++;
			assertTrue("a text measure receives a positive width", width > 0);
			return block < heights.length ? heights[block] : 30;
		}

		@Override
		public long intrinsicWidths(int block) {
			intrinsicCalls++;
			return (20L << 32) | 80L;
		}

		@Override
		public int[] columns(int table, int width) {
			columnCalls++;
			assertEquals(2, document.tableColumns[table]);
			return new int[] { 0, 60, 140 };
		}
	}
}
