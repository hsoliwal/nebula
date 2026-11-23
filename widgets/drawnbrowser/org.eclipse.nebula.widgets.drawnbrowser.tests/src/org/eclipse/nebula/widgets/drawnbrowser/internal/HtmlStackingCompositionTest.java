/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Composition regressions using the real parser and deterministic metrics, without an SWT display. */
public class HtmlStackingCompositionTest {

	@Test
	public void staticFlexAndGridItemsUseSignedIntegerStackLevels() {
		for (String display : new String[] { "flex", "grid" }) {
			HtmlDocument document = HtmlDocument.parse("<div style='display:" + display + "'>"
					+ "<p style='z-index:8'>positive</p><p style='z-index:-3'>negative</p>"
					+ "<p>auto</p><p style='z-index:0'>zero</p></div><p>flow</p>");
			assertArrayEquals(display, new String[] { "negative", "auto", "flow", "zero", "positive" }, content(document));
			assertComplete(document);
		}
	}

	@Test
	public void staticIntegerItemsSelectTheSceneWithoutBecomingPositioned() {
		for (String display : new String[] { "flex", "grid" }) {
			for (String z : new String[] { "-1", "0", "1" }) {
				HtmlDocument document = HtmlDocument.parse("<div style='display:" + display + "'><p style='z-index:"
						+ z + "'>item</p></div>");
				assertTrue(display + " z=" + z + " needs the shared paint/pick scene", document.boxLayout);
				assertFalse("stacking does not turn a static item into a positioned box", document.positioned);
				assertEquals(HtmlStyles.STATIC, document.boxPosition[document.blockBox[0]]);
			}
			assertFalse(HtmlDocument.parse("<div style='display:" + display + "'><p>auto</p></div>").boxLayout);
		}
	}

	@Test
	public void explicitZeroIsAtomicAndDistinctFromAuto() {
		for (String display : new String[] { "flex", "grid" }) {
			String template = "<div style='display:" + display + "'><p style='z-index:%s'>first</p><p>second</p></div>";
			assertArrayEquals(new String[] { "first", "second" }, content(HtmlDocument.parse(String.format(template, "auto"))));
			HtmlDocument zero = HtmlDocument.parse(String.format(template, "0"));
			assertArrayEquals(new String[] { "second", "first" }, content(zero));
			HtmlPaintOrder order = new HtmlPaintOrder(zero);
			assertEquals("the item's own background and content stay together", backgroundStep(order, zero.blockBox[0]) + 1,
					contentStep(order, 0));
		}
	}

	@Test
	public void equalAndExtremeItemStackLevelsKeepNumericAndTreeOrder() {
		for (String display : new String[] { "flex", "grid" }) {
			HtmlDocument document = HtmlDocument.parse("<div style='display:" + display + "'>"
					+ "<p style='z-index:2147483647'>maximum</p><p style='z-index:7'>first</p>"
					+ "<p style='z-index:-2147483648'>minimum</p><p style='z-index:7'>second</p></div>");
			assertArrayEquals(new String[] { "minimum", "first", "second", "maximum" }, content(document));
		}
	}

	@Test
	public void itemContextsRemainInsideTheirNearestActualAncestorContext() {
		for (String display : new String[] { "flex", "grid" }) {
			String template = "<div style='position:relative;z-index:%s'><div style='display:" + display + "'>"
					+ "<p style='z-index:999'>item</p></div></div><p style='position:relative;z-index:1'>sibling</p>";
			assertArrayEquals(new String[] { "item", "sibling" }, content(HtmlDocument.parse(String.format(template, "0"))));
			assertArrayEquals(new String[] { "sibling", "item" }, content(HtmlDocument.parse(String.format(template, "auto"))));
		}
	}

	@Test
	public void anIntegerItemContextOwnsItsPositionedDescendants() {
		for (int role : new int[] { HtmlDocument.FLEX_BOX, HtmlDocument.GRID_BOX }) {
			for (String z : new String[] { "auto", "0" }) {
				HtmlDocument document = HtmlDocument.parse("<div><div style='z-index:" + z + "'>item"
						+ "<p style='position:absolute;z-index:999'>child</p></div></div>"
						+ "<p style='position:relative;z-index:1'>sibling</p>");
				int item = boxWithText(document, "item");
				// Exercise the public primitive box contract: today's item parser flattens such inner blocks.
				// Relabel an already parsed tree so the plan's context ownership is checked independently.
				document.boxRole[document.boxParent[item]] = role;
				document.boxRole[item] = HtmlDocument.CELL_BOX;
				assertArrayEquals(z.equals("auto") ? new String[] { "item", "sibling", "child" }
						: new String[] { "item", "child", "sibling" }, content(document));
			}
		}
	}

	@Test
	public void ordinaryStaticBlocksAndTableCellsStillIgnoreZIndex() {
		HtmlDocument document = HtmlDocument.parse("<p style='z-index:999'>block</p><table><tr>"
				+ "<td style='z-index:8'>first</td><td style='z-index:-4'>second</td></tr></table><p>after</p>");
		assertFalse(document.boxLayout);
		assertArrayEquals(new String[] { "block", "first", "second", "after" }, content(document));
		HtmlPaintOrder order = new HtmlPaintOrder(document);
		assertTrue("ordinary cell backgrounds remain in the flow phase", backgroundStep(order, document.blockBox[2])
				< contentStep(order, 0));
	}

	@Test
	public void staticItemStackingDoesNotApplyPositionInsetsOrChangeGeometry() {
		for (String display : new String[] { "flex", "grid;grid-template-columns:80px 120px" }) {
			String template = "<div style='display:" + display + "'><p style='z-index:%s;left:90px;top:70px'>first</p>"
					+ "<p>second</p></div><p>after</p>";
			Scene automatic = new Scene(String.format(template, "auto"));
			Scene integer = new Scene(String.format(template, "2"));
			for (int b = 0; b < automatic.document.blockCount; b++) {
				assertEquals(automatic.layout.blockX(b), integer.layout.blockX(b));
				assertEquals(automatic.layout.blockY(b, 0), integer.layout.blockY(b, 0));
				assertEquals(automatic.layout.blockHeight(b), integer.layout.blockHeight(b));
			}
			assertEquals(0, integer.layout.blockX(0));
			assertEquals(80, integer.layout.blockX(1));
			assertEquals(0, integer.layout.blockY(0, 0));
		}
	}

	@Test
	public void reversedPickingUsesStaticItemStackLevelAtAnOverlap() {
		for (String display : new String[] { "flex", "grid;grid-template-columns:80px 120px" }) {
			Scene scene = new Scene("<div style='display:" + display + "'><p style='z-index:1;background:red'>higher</p>"
					+ "<p style='position:relative;left:-80px;background:blue'>later</p></div>");
			assertEquals(0, scene.layout.boxX(scene.document.blockBox[0]));
			assertEquals(0, scene.layout.boxX(scene.document.blockBox[1]));
			assertEquals(0, scene.layout.blockAt(10, 10, 0));
			assertEquals(0xFFFF0000, backgroundAt(scene, 10, 10));
		}
	}

	@Test
	public void tableRowsPaintBeforeSpanningCellsOnTheLinearPath() {
		HtmlDocument document = HtmlDocument.parse("<p>before</p><table><caption>caption</caption>" + rows()
				+ "</table><p>after</p><table><tr><td>other table</td></tr></table>");
		assertFalse(document.boxLayout);
		assertTableLayers(document);
		HtmlPaintOrder order = new HtmlPaintOrder(document);
		int caption = boxWithText(document, "caption"), spanning = boxWithText(document, "span");
		int after = boxWithText(document, "after"), other = boxWithText(document, "other table");
		assertTrue(backgroundStep(order, caption) < backgroundStep(order, document.boxParent[spanning]));
		assertTrue(backgroundStep(order, spanning) < backgroundStep(order, after));
		assertTrue(backgroundStep(order, after) < backgroundStep(order, other));
		assertComplete(document);
	}

	@Test
	public void tableLayersStayInsideTheirContextAndAheadOfItsContent() {
		HtmlDocument document = HtmlDocument.parse("<div style='position:relative;z-index:0'>"
				+ "<p style='position:absolute;z-index:-1'>negative</p><table>" + rows()
				+ "</table></div><p style='position:relative;z-index:1'>sibling</p>");
		assertTableLayers(document);
		HtmlPaintOrder order = new HtmlPaintOrder(document);
		int spanning = boxWithText(document, "span"), table = document.boxParent[document.boxParent[spanning]];
		assertTrue(contentStep(order, blockWithText(document, "negative")) < backgroundStep(order, table));
		assertTrue(backgroundStep(order, spanning) < contentStep(order, blockWithText(document, "span")));
		assertTrue(contentStep(order, blockWithText(document, "last")) < contentStep(order, blockWithText(document, "sibling")));
		assertComplete(document);
	}

	@Test
	public void laterColoredRowCannotCoverOrStealASpanningCellsBackground() {
		Scene scene = new Scene("<div style='position:relative'><table>" + rows() + "</table></div>");
		int block = blockWithText(scene.document, "span"), last = boxWithText(scene.document, "last");
		int pointY = scene.layout.boxY(last, 0) + 1;
		assertEquals("the spanning cell extends into the second row", 60, scene.layout.boxHeight(scene.document.blockBox[block]));
		assertEquals("cell background is above both row backgrounds", 0xFF0000FF, backgroundAt(scene, 1, pointY));
		assertEquals("padding hit testing shares the exact same order", block, scene.layout.blockAt(1, pointY, 0));
	}

	@Test
	public void fixedAndStickyAutoContextsRetainTheirExistingIsolation() {
		for (String position : new String[] { "fixed", "sticky" }) {
			HtmlDocument document = HtmlDocument.parse("<div style='position:" + position + "'>owner"
					+ "<p style='position:absolute;z-index:999'>child</p></div><p style='position:relative;z-index:1'>sibling</p>");
			assertArrayEquals(new String[] { "owner", "child", "sibling" }, content(document));
		}
	}

	private static String rows() {
		return "<tr style='background:red'><td rowspan='2' style='background:blue'>span</td><td>top</td></tr>"
				+ "<tr style='background:lime'><td>last</td></tr>";
	}

	private static void assertTableLayers(HtmlDocument document) {
		HtmlPaintOrder order = new HtmlPaintOrder(document);
		int spanning = boxWithText(document, "span"), top = boxWithText(document, "top"), last = boxWithText(document, "last");
		int firstRow = document.boxParent[spanning], lastRow = document.boxParent[last];
		assertTrue("row backgrounds keep row order", backgroundStep(order, firstRow) < backgroundStep(order, lastRow));
		assertTrue("every row background precedes a spanning cell", backgroundStep(order, lastRow) < backgroundStep(order, spanning));
		assertTrue("cells keep source order", backgroundStep(order, spanning) < backgroundStep(order, top));
		assertTrue("cells keep source order across rows", backgroundStep(order, top) < backgroundStep(order, last));
	}

	private static int backgroundAt(Scene scene, int pointX, int pointY) {
		int color = 0;
		HtmlPaintOrder order = scene.layout.order();
		for (int step = 0; step < order.size(); step++) {
			if (!order.isBackground(step)) continue;
			int box = order.index(step), fill = scene.document.boxBackground[box];
			int x = scene.layout.boxX(box), y = scene.layout.boxY(box, 0);
			if (fill != 0 && pointX >= x && pointX < (long) x + scene.layout.boxWidth(box)
					&& pointY >= y && pointY < (long) y + scene.layout.boxHeight(box)) color = fill;
		}
		return color;
	}

	private static int blockWithText(HtmlDocument document, String text) {
		for (int block = 0; block < document.blockCount; block++) if (text.equals(document.blockText(block))) return block;
		throw new AssertionError("missing block: " + text);
	}

	private static int boxWithText(HtmlDocument document, String text) {
		return document.blockBox[blockWithText(document, text)];
	}

	private static int backgroundStep(HtmlPaintOrder order, int box) {
		for (int step = 0; step < order.size(); step++) if (order.isBackground(step) && order.index(step) == box) return step;
		throw new AssertionError("missing background: " + box);
	}

	private static int contentStep(HtmlPaintOrder order, int block) {
		for (int step = 0; step < order.size(); step++) if (!order.isBackground(step) && order.index(step) == block) return step;
		throw new AssertionError("missing content: " + block);
	}

	private static String[] content(HtmlDocument document) {
		HtmlPaintOrder order = new HtmlPaintOrder(document);
		String[] result = new String[document.blockCount];
		int next = 0;
		for (int step = 0; step < order.size(); step++) if (!order.isBackground(step)) result[next++] = document.blockText(order.index(step));
		assertEquals(document.blockCount, next);
		return result;
	}

	private static void assertComplete(HtmlDocument document) {
		HtmlPaintOrder order = new HtmlPaintOrder(document);
		assertEquals(document.boxCount + document.blockCount, order.size());
		boolean[] boxes = new boolean[document.boxCount], blocks = new boolean[document.blockCount];
		for (int step = 0; step < order.size(); step++) {
			boolean[] seen = order.isBackground(step) ? boxes : blocks;
			assertFalse("duplicate paint fragment", seen[order.index(step)]);
			seen[order.index(step)] = true;
		}
		for (boolean seen : boxes) assertTrue(seen);
		for (boolean seen : blocks) assertTrue(seen);
	}

	private static final class Scene {
		final HtmlDocument document;
		final HtmlPositionedLayout layout;

		Scene(String html) {
			document = HtmlDocument.parse(html);
			layout = new HtmlPositionedLayout(document);
			layout.layout(200, 100, new HtmlPositionedLayout.Metrics() {
				@Override
				public int height(int block, int wrapWidth) {
					assertTrue(wrapWidth > 0);
					return 30;
				}

				@Override
				public long intrinsicWidths(int block) {
					return 20L << 32 | 80L;
				}

				@Override
				public int[] columns(int table, int availableWidth) {
					assertEquals(2, document.tableColumns[table]);
					return document.tableDisplay[table] == HtmlStyles.DISPLAY_DEFAULT
							? new int[] { 0, 80, 200 } : new int[] { 0, 80, 80, 120 };
				}
			});
		}
	}
}
