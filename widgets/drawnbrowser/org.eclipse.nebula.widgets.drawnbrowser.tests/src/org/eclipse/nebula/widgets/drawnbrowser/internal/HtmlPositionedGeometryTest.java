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
	public void hiddenProgrammaticScrollPreservesTheParentBorderButClipRefusesScroll() {
		for (String mode : new String[] { "hidden", "clip" }) {
			Scene scene = new Scene("<section style='position:relative;height:40px;overflow:" + mode + ";border:2px solid red'>"
					+ "<article style='position:relative;top:100px;height:20px;width:20px'>moved</article></section>",
					200, 150);
			int panel = scene.document.boxParent[scene.document.blockBox[0]];
			int child = scene.document.blockBox[0], before = scene.layout.boxY(child, 0);
			int applied = scene.layout.scrollPanel(panel, 15);
			assertEquals(mode, mode.equals("hidden") ? 15 : 0, applied);
			assertEquals(mode, mode.equals("hidden") ? before - 15 : before,
					scene.layout.boxY(child, 0));
			assertEquals("panel border remains in place", 0, scene.layout.boxY(panel, 0));
			assertEquals("clipped content cannot enlarge root scroll", 44, scene.layout.totalHeight());
		}
	}

	@Test
	public void autoAndScrollPanelsConsumeWheelWithoutMovingRoot() {
		for (String mode : new String[] { "auto", "scroll" }) {
			Scene scene = new Scene("<section style='height:40px;width:100px;overflow:" + mode + "'>"
					+ "<article style='height:300px'>inner</article></section>", 240, 180);
			int panel = scene.document.boxParent[scene.document.blockBox[0]];
			int child = scene.document.blockBox[0];
			int top = scene.layout.boxY(child, 0);
			assertTrue(mode, scene.layout.panelMaximumY(panel) > 30);
			assertEquals(mode, 30, scene.layout.scrollWheelAt(10, 10, 0, 30));
			assertEquals(30, scene.layout.panelScrollY(panel));
			assertEquals(top - 30, scene.layout.boxY(child, 0));
			assertEquals("outer root stays at the panel's 40px height", 40, scene.layout.totalHeight());
			assertVisible(scene, 0, 180, 0);
			assertEquals("bounded scrolling clamps", scene.layout.panelMaximumY(panel) - 30,
					scene.layout.scrollPanel(panel, Integer.MAX_VALUE));
			assertEquals("no additional pixels at the end", 0, scene.layout.scrollPanel(panel, Integer.MAX_VALUE));
		}
	}

	@Test
	public void nestedPanelScrollOffsetsAddAndFixedChildrenEscapeThem() {
		Scene scene = new Scene("<section style='position:relative;height:50px;overflow:auto'>"
				+ "<article style='height:40px;overflow:auto'>"
				+ "<nav style='position:relative;top:200px;height:20px'>nested</nav>"
				+ "<header style='position:fixed;top:5px;height:10px;width:20px'>fixed</header>"
				+ "</article>"
				+ "<footer style='position:relative;top:150px;height:20px'>far outer</footer>"
				+ "</section>", 240, 180);
		int inner = scene.document.boxParent[scene.document.blockBox[0]];
		int outer = scene.document.boxParent[inner];
		int moving = scene.document.blockBox[0], fixed = scene.document.blockBox[1];
		int before = scene.layout.boxY(moving, 0);
		int fixedBefore = scene.layout.boxY(fixed, 0);
		assertTrue(scene.layout.panelMaximumY(inner) >= 30);
		assertTrue(scene.layout.panelMaximumY(outer) >= 15);
		assertEquals(30, scene.layout.scrollPanel(inner, 30));
		assertEquals(15, scene.layout.scrollPanel(outer, 15));
		assertEquals(before - 45, scene.layout.boxY(moving, 0));
		assertEquals(fixedBefore, scene.layout.boxY(fixed, 0));
		assertEquals(50, scene.layout.totalHeight());
		assertVisible(scene, 0, 180, 0);
	}

	@Test
	public void staticSemanticPanelOwnsItsPaddingAndMarginsAroundItsChildrenOnce() {
		for (String tag : new String[] { "div", "main", "section", "article", "nav", "aside", "header", "footer" }) {
			Scene scene = new Scene("<" + tag + " style='margin:5px;padding:10px;border:2px solid blue;background:red'>"
					+ "<p>inner</p></" + tag + "><p>after</p>", 300, 200);
			int panel = scene.document.boxParent[scene.document.blockBox[0]];
			assertTrue(tag, scene.document.boxLayout);
			assertEquals(tag, 5, scene.layout.boxX(panel));
			assertEquals(tag, 5, scene.layout.boxY(panel, 0));
			assertEquals(tag, 290, scene.layout.boxWidth(panel));
			assertEquals(tag, 54, scene.layout.boxHeight(panel));
			assertEquals(tag, 17, scene.layout.blockX(0));
			assertEquals(tag, 17, scene.layout.blockY(0, 0));
			assertEquals(tag, 64, scene.layout.blockY(1, 0));
			assertEquals(tag, 94, scene.layout.totalHeight());
		}
	}

	@Test
	public void emptySemanticPanelsKeepDecorationAndTheFollowingFlowPosition() {
		Scene scene = new Scene("<main style='padding:4px;border:2px solid red'></main><p>after</p>", 200, 100);
		assertTrue(scene.document.boxLayout);
		assertEquals(12, scene.layout.boxHeight(1));
		assertEquals(12, scene.layout.blockY(0, 0));
		assertEquals(42, scene.layout.totalHeight());
	}

	@Test
	public void aClippedOwnTextFragmentCannotCreateBlankOuterScrollRange() {
		Scene scene = new Scene("<section style='height:40px;overflow:clip'>long content</section><p>after</p>",
				200, 100, 700, 20);
		assertEquals(40, scene.layout.boxHeight(1));
		assertEquals(40, scene.layout.blockY(1, 0));
		assertEquals(60, scene.layout.totalHeight());
	}

	@Test
	public void viewportFixedBoxesAndTheirNormalChildrenEscapeAncestorClips() {
		for (String ancestor : new String[] { "", "position:relative;" }) {
			for (String content : new String[] { "fixed", "<p>ordinary child</p>" }) {
				Scene scene = new Scene("<section style='" + ancestor + "width:40px;height:40px;overflow:hidden'>"
						+ "<aside style='position:fixed;left:120px;top:15px;width:50px;height:30px'>"
						+ content + "</aside></section>", 300, 180, 12);
				int owner = scene.document.blockBox[0];
				assertTrue("a viewport-fixed box escapes the intermediate overflow ancestor", !scene.layout.hasClip(owner, true));
				for (int scroll : new int[] { 0, 70 }) {
					int[] bounds = { 0, 0, 300, 180 };
					assertTrue(scene.layout.clipBounds(owner, true, scroll, bounds));
					assertArrayEquals(new int[] { 0, 0, 300, 180 }, bounds);
					assertEquals(0, scene.layout.blockAt(125, 20 + scroll, scroll));
					assertEquals(0, scene.layout.nearestBlock(125, 20 + scroll, scroll));
				}
			}
		}
	}

	@Test
	public void absoluteBoxesSkipInterveningClipsButKeepTheirContainingBlockClip() {
		for (String content : new String[] { "absolute", "<p>ordinary child</p>" }) {
			Scene scene = new Scene("<main style='position:relative;width:140px;height:70px;overflow:hidden'>"
					+ "<section style='width:40px;height:40px;overflow:hidden'>"
					+ "<article style='position:absolute;left:120px;top:15px;width:50px;height:30px'>"
					+ content + "</article></section></main>", 300, 180, 12);
			int[] bounds = { 0, 0, 300, 180 };
			assertTrue(scene.layout.clipBounds(scene.document.blockBox[0], true, 0, bounds));
			assertArrayEquals("the actual 140px containing block clips, the intervening 40px box does not",
					new int[] { 0, 0, 140, 70 }, bounds);
			assertEquals(0, scene.layout.blockAt(125, 20, 0));
			assertEquals(-1, scene.layout.blockAt(150, 20, 0));
		}
	}

	@Test
	public void anAbsoluteChildOfAFixedClippingBoxKeepsItsViewportClip() {
		Scene scene = new Scene("<nav style='position:fixed;left:40px;top:20px;width:100px;height:60px;overflow:clip'>"
				+ "<p style='position:absolute;left:80px;top:0;width:40px;height:20px'>clipped</p></nav>", 300, 180, 12);
		for (int scroll : new int[] { 0, 70 }) {
			int[] bounds = { 0, 0, 300, 180 };
			assertTrue(scene.layout.clipBounds(scene.document.blockBox[0], true, scroll, bounds));
			assertArrayEquals(new int[] { 40, 20, 140, 80 }, bounds);
			assertEquals(0, scene.layout.blockAt(125, 25 + scroll, scroll));
			assertEquals(-1, scene.layout.blockAt(150, 25 + scroll, scroll));
		}
	}

	@Test
	public void clippedVisualOverflowDoesNotPropagateIntoTheOuterScrollRange() {
		for (String overflow : new String[] { "hidden", "clip", "visible" }) {
			for (String position : new String[] { "absolute", "relative" }) {
				Scene scene = new Scene("<section style='position:relative;width:80px;height:40px;overflow:" + overflow + "'>"
						+ "<article style='position:" + position + ";top:500px;width:40px;height:20px'></article></section>"
						+ "<footer style='height:20px'></footer>", 300, 180);
				assertEquals(overflow + " " + position, overflow.equals("visible") ? 520 : 60, scene.layout.totalHeight());
			}
		}
	}

	@Test
	public void absoluteOverflowEscapingAnIntermediateClipKeepsItsOuterScrollRange() {
		Scene scene = new Scene("<main style='position:relative;width:80px;height:40px'>"
				+ "<section style='height:20px;overflow:hidden'>"
				+ "<article style='position:absolute;top:500px;width:40px;height:20px'></article>"
				+ "</section></main><footer style='height:20px'></footer>", 300, 180);
		assertEquals(520, scene.layout.totalHeight());
	}

	@Test
	public void hiddenAndClipPanelsKeepTheirDifferentStickyScrollReferences() {
		for (String overflow : new String[] { "hidden", "clip" }) {
			Scene scene = new Scene("<section style='height:300px;overflow:" + overflow + "'>"
					+ "<header style='position:sticky;top:0;height:20px;background:red'></header></section>"
					+ "<footer style='height:300px'></footer>", 300, 180);
			assertEquals(0, scene.layout.boxY(2, 0));
			assertEquals(overflow + " establishes " + (overflow.equals("hidden") ? "a local" : "no local") + " scrollport",
					overflow.equals("hidden") ? 0 : 100, scene.layout.boxY(2, 100));
			assertEquals(0, scene.layout.boxY(2, 0));
		}
	}

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
	public void farAbsoluteDescendantsDoNotGrowAnOuterClippedPanelScrollbar() {
		String far = "<p style='position:absolute;top:5000px;width:20px;height:200px'>far</p></div>";
		for (String overflow : new String[] { "hidden", "clip" }) {
			Scene clipped = new Scene("<div style='position:relative;height:40px;overflow:" + overflow + "'>" + far, 200, 100);
			assertEquals("an invisible descendant cannot inflate the outer scroll range", 40, clipped.layout.totalHeight());
		}
		Scene visible = new Scene("<div style='position:relative;height:40px;overflow:visible'>" + far, 200, 100);
		assertTrue("visible overflow must still extend the outer scroll range", visible.layout.totalHeight() >= 5200);
	}

	@Test
	public void clippedRelativeOverflowDoesNotHideLaterFlowSiblings() {
		Scene scene = new Scene("<div style='position:relative;height:40px;overflow:hidden'>"
				+ "<p style='position:relative;top:5000px;height:200px'>moved</p></div>"
				+ "<p>following</p>", 200, 100);
		assertTrue("subsequent visible siblings still extend document height", scene.layout.totalHeight() >= 70);
		assertTrue("hidden relative content cannot create a long outer scroll range", scene.layout.totalHeight() < 200);
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
