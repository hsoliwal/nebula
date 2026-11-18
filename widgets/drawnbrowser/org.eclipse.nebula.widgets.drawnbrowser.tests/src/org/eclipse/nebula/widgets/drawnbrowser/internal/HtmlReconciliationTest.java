/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.junit.Test;

/** Cross-feature regressions for nested boxes with the current forms, selectors, flex and table owners. */
public class HtmlReconciliationTest {

	@Test
	public void currentBorderSlotAndGrowingBoxLanesRetainPositionStyles() {
		assertEquals(26, HtmlStyles.BORDER_STYLE);
		int[] style = new HtmlStyles().compute(HtmlStyles.root(), "div", null,
				"border:4px ridge #808080;width:70px;height:40px;right:8px;z-index:2147483647");
		assertEquals(HtmlStyles.BORDER_RIDGE, style[HtmlStyles.BORDER_STYLE]);
		assertEquals(70, style[HtmlStyles.WIDTH]);
		assertEquals(40, style[HtmlStyles.HEIGHT]);
		assertEquals(8, style[HtmlStyles.OFFSET_RIGHT]);
		assertEquals(Integer.MAX_VALUE, style[HtmlStyles.Z_INDEX]);
		assertEquals(0, style[HtmlStyles.Z_AUTO]);
		assertEquals(1, new HtmlStyles().compute(style, "div", null, "z-index:auto")[HtmlStyles.Z_AUTO]);
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;width:200px;border:4px inset #808080'>"
				+ "<div style='position:absolute;width:30px;height:20px;border:4px ridge #808080'></div>".repeat(20)
				+ "<p>child</p></div>");
		int ridge = 0, inset = 0;
		assertTrue(doc.boxCount > 16);
		for (int box = 0; box < doc.boxCount; box++) {
			if (doc.boxBorderStyle[box] == HtmlStyles.BORDER_RIDGE) ridge++;
			if (doc.boxBorderStyle[box] == HtmlStyles.BORDER_INSET) inset++;
		}
		assertEquals(20, ridge);
		assertEquals(1, inset);
		assertEquals(HtmlStyles.BORDER_SOLID, doc.boxBorderStyle[doc.blockBox[0]]);
		assertEquals(HtmlStyles.BORDER_SOLID, doc.blockBorderStyle[0]);
	}

	private static HtmlLayout layout(HtmlDocument doc, int width) {
		Display display = Display.getDefault();
		HtmlLayout layout = new HtmlLayout(display, doc, display.getSystemFont(), display.getSystemColor(SWT.COLOR_BLUE));
		layout.setWidth(width);
		layout.setViewport(300, 0);
		layout.ensure(Integer.MAX_VALUE);
		return layout;
	}

	@Test
	public void priorStyleSlotNumbersAndFlexWidthSemanticsRemainStable() {
		assertEquals(18, HtmlStyles.DISPLAY);
		assertEquals(19, HtmlStyles.FLEX_GROW);
		assertEquals(20, HtmlStyles.FLEX_BASIS);
		assertEquals(21, HtmlStyles.JUSTIFY);
		assertEquals(22, HtmlStyles.GAP);
		assertEquals(23, HtmlStyles.ROW_GAP);
		assertEquals(24, HtmlStyles.TRACKS);
		assertEquals(25, HtmlStyles.COLUMN_SPAN);
		int[] style = new HtmlStyles().compute(HtmlStyles.root(), "div", null,
				"width:70px;flex-grow:2;position:absolute;right:8px;z-index:auto");
		assertEquals(70, style[HtmlStyles.FLEX_BASIS]);
		assertEquals(70, style[HtmlStyles.WIDTH]);
		assertEquals(2, style[HtmlStyles.FLEX_GROW]);
		assertEquals(8, style[HtmlStyles.OFFSET_RIGHT]);
		assertEquals(1, style[HtmlStyles.Z_AUTO]);
	}

	@Test
	public void selectorAncestryAndHiddenSubtreesShareTheBoxParser() {
		HtmlDocument doc = HtmlDocument.parse("<style>#host {position:relative;left:17px;width:200px} "
				+ "#host > p.item {position:absolute;left:9px;top:11px;width:40px;z-index:3} .hide {display:none}</style>"
				+ "<div id=host><p class=item>shown</p><div class=hide><p>hidden</p></div></div>");
		assertEquals(1, doc.blockCount);
		assertEquals("shown", doc.blockText(0));
		int box = doc.blockBox[0];
		assertEquals(3, doc.boxZIndex[box]);
		HtmlLayout layout = layout(doc, 400);
		try {
			assertNotNull(layout.positioned());
			assertEquals(26, layout.indent(0));
			assertEquals(11, layout.top(0));
		} finally { layout.dispose(); }
	}

	@Test
	public void positionedFlexUsesTheCurrentGrowAndGapAlgorithm() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;left:13px;display:flex;gap:10px'>"
				+ "<div style='flex-grow:1'>a</div><span style='flex-grow:3'>b</span></div><p>after</p>");
		HtmlLayout layout = layout(doc, 400);
		try {
			assertNotNull(layout.positioned());
			assertEquals(layout.top(0), layout.top(1));
			assertEquals(13, layout.cellX(0));
			assertEquals(layout.cellX(0) + layout.cellWidth(0) + 10, layout.cellX(1));
			assertEquals(413, layout.cellX(1) + layout.cellWidth(1));
			assertTrue(layout.cellWidth(1) > 2 * layout.cellWidth(0));
			assertTrue(layout.top(2) >= layout.top(0) + layout.height(0));
		} finally { layout.dispose(); }
	}

	@Test
	public void positionedFlexWidthBasisAndAnonymousTextRetainOneRow() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;width:240px;display:flex;gap:5px'>"
				+ "<div style='width:60px'>first</div>loose<div style='flex-grow:1'>last</div></div>");
		assertEquals(3, doc.blockCount);
		HtmlLayout layout = layout(doc, 400);
		try {
			assertEquals(layout.top(0), layout.top(1));
			assertEquals(layout.top(0), layout.top(2));
			assertEquals(60, layout.cellWidth(0));
			assertEquals(240, layout.cellX(2) + layout.cellWidth(2));
			assertEquals("loose", doc.blockText(1));
		} finally { layout.dispose(); }
	}

	@Test
	public void positionedFlexContainerKeepsItsWidthWhenChildrenDoNotGrow() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;width:240px;display:flex;justify-content:center'>"
				+ "<div style='width:60px'>first</div><div style='width:40px'>last</div></div>");
		HtmlLayout layout = layout(doc, 400);
		try {
			int container = doc.boxParent[doc.blockBox[0]];
			assertEquals(240, layout.positioned().boxWidth(container));
			assertEquals(70, layout.cellX(0));
			assertEquals(130, layout.cellX(1));
		} finally { layout.dispose(); }
	}

	@Test
	public void colspanGeometrySurvivesAContainingBlock() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;left:19px'><table>"
				+ "<tr><td colspan=2>both columns</td></tr><tr><td>a</td><td>b</td></tr></table></div>");
		HtmlLayout layout = layout(doc, 400);
		try {
			assertNotNull(layout.positioned());
			assertEquals(19, layout.cellX(0));
			assertEquals(layout.cellWidth(1) + layout.cellWidth(2), layout.cellWidth(0));
			assertEquals(layout.cellX(1) + layout.cellWidth(1), layout.cellX(2));
		} finally { layout.dispose(); }
	}

	@Test
	public void rowspanGeometrySurvivesAContainingBlock() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;left:19px'><table>"
				+ "<tr><td rowspan=2>spanning</td><td>first</td></tr><tr><td>second</td></tr></table></div>");
		HtmlLayout layout = layout(doc, 400);
		try {
			assertNotNull(layout.positioned());
			assertEquals(layout.height(1) + layout.height(2), layout.height(0));
			assertEquals(layout.top(1) + layout.height(1), layout.top(2));
			assertEquals(layout.cellX(1), layout.cellX(2));
			assertEquals(0, layout.blockAt(layout.cellX(0) + 1, layout.top(2) + 1));
		} finally { layout.dispose(); }
	}

	@Test
	public void positionedNativeControlsUseOriginalIntrinsicMetrics() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;left:23px;width:250px'>"
				+ "<input name=field><textarea rows=3>value</textarea></div>");
		HtmlLayout layout = layout(doc, 400);
		try {
			assertNotNull(layout.positioned());
			assertEquals(2, doc.controlCount);
			assertEquals(23, layout.indent(0));
			assertTrue(layout.height(0) >= layout.controlHeight(0));
			assertTrue(layout.height(1) >= layout.controlHeight(1));
			assertEquals(layout.top(0) + layout.height(0), layout.top(1));
		} finally { layout.dispose(); }
	}

	@Test
	public void positionedGridPreservesTracksSpansAndBothGaps() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;left:13px;width:400px;display:grid;"
				+ "grid-template-columns:100px 1fr 2fr;gap:6px 10px'><div>a</div><div>b</div><div>c</div>"
				+ "<div style='grid-column:span 2'>d</div><div style='padding:5px'>e</div></div><p>after</p>");
		HtmlLayout layout = layout(doc, 500);
		try {
			assertNotNull(layout.positioned());
			assertArrayEquals(new int[] { 13, 123, 226, 13, 226 },
					new int[] { layout.cellX(0), layout.cellX(1), layout.cellX(2), layout.cellX(3), layout.cellX(4) });
			assertArrayEquals(new int[] { 100, 93, 187, 203, 187 },
					new int[] { layout.cellWidth(0), layout.cellWidth(1), layout.cellWidth(2), layout.cellWidth(3), layout.cellWidth(4) });
			assertEquals(layout.top(0), layout.top(2));
			assertEquals(layout.top(0) + layout.height(0) + 6, layout.top(3));
			assertEquals(layout.top(3), layout.top(4));
			assertEquals(layout.top(3) + layout.height(3), layout.top(5));
			assertEquals(layout.cellX(4) + 5, layout.indent(4));
		} finally { layout.dispose(); }
	}

	@Test
	public void positionedGridRetainsAnonymousAutoPlacedItems() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;display:grid;width:200px;"
				+ "grid-template-columns:1fr 1fr;gap:7px 10px'>loose<div>second</div><div style='grid-column:span 2'>last</div></div>");
		HtmlLayout layout = layout(doc, 400);
		try {
			assertEquals(3, doc.blockCount);
			assertEquals("loose", doc.blockText(0));
			assertEquals(95, layout.cellWidth(0));
			assertEquals(105, layout.cellX(1));
			assertEquals(200, layout.cellWidth(2));
			assertEquals(layout.top(0) + layout.height(0) + 7, layout.top(2));
		} finally { layout.dispose(); }
	}

	@Test
	public void stickyProjectionInAResolvedParentKeepsItsBoundsAndTextMeasurements() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;width:300px;height:200px'>"
				+ "<p style='position:sticky;top:5px;height:20px;padding:1px;background:red'>header</p><p>body</p></div>");
		HtmlLayout layout = layout(doc, 400);
		try {
			HtmlPositionedLayout scene = layout.positioned();
			assertNotNull(scene);
			int box = doc.blockBox[0], measured = layout.layoutsMade;
			assertTrue(scene.hasSticky());
			assertEquals(8, scene.boxY(box, 0));
			assertEquals(55, scene.boxY(box, 50));
			assertEquals(200, scene.boxY(box, 500) + scene.boxHeight(box) + HtmlLayout.BLOCK_GAP);
			assertEquals(0, scene.blockAt(2, 56, 50));
			assertTrue(scene.collectVisible(50, 20, 50) > 0);
			assertEquals(measured, layout.layoutsMade);
		} finally { layout.dispose(); }
	}

	@Test
	public void stickyDescendantsMoveTogetherWhileFixedDescendantsUseTheViewport() {
		HtmlDocument doc = HtmlDocument.parse("<div style='position:relative;width:300px;height:300px'>"
				+ "<div style='position:sticky;top:4px'><p>first</p><p>second</p>"
				+ "<p style='position:fixed;top:7px;width:40px'>fixed</p></div><p>after</p></div>");
		HtmlLayout layout = layout(doc, 400);
		try {
			HtmlPositionedLayout scene = layout.positioned();
			int delta = scene.blockY(0, 80) - scene.blockY(0, 0);
			assertTrue(delta > 0);
			assertEquals(delta, scene.blockY(1, 80) - scene.blockY(1, 0));
			assertEquals(7, scene.blockY(2, 80) - 80);
			assertEquals(scene.blockY(3, 0), scene.blockY(3, 80));
		} finally { layout.dispose(); }
	}

	@Test
	public void stickyTableCellsAreBoundedByTheWholeTableInAResolvedParent() {
		StringBuilder html = new StringBuilder("<div style='position:relative;left:13px'><table>"
				+ "<tr><th style='position:sticky;top:0'>head</th><th>other</th></tr>");
		for (int i = 0; i < 20; i++) html.append("<tr><td>row</td><td>other</td></tr>");
		html.append("</table></div>");
		HtmlDocument doc = HtmlDocument.parse(html.toString());
		HtmlLayout layout = layout(doc, 400);
		try {
			HtmlPositionedLayout scene = layout.positioned();
			assertTrue(scene.hasSticky());
			assertEquals(60, scene.blockY(0, 60));
			assertEquals(0, scene.blockAt(layout.cellX(0) + 1, 61, 60));
			int end = layout.top(doc.blockCount - 1) + layout.height(doc.blockCount - 1);
			assertEquals(end, scene.blockY(0, end + 500) + scene.blockHeight(0));
		} finally { layout.dispose(); }
	}
}
