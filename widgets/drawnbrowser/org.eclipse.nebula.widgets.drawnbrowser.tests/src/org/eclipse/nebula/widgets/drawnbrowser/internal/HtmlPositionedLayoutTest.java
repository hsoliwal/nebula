/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Test;

/** Positioned geometry using real SWT text measurements; ordinary lazy layout remains a separate regression. */
public class HtmlPositionedLayoutTest {

	private final Display display = Display.getDefault();
	private HtmlLayout layout;

	private HtmlLayout layout(String html, int width, int height) {
		layout = new HtmlLayout(display, HtmlDocument.parse(html), display.getSystemFont(), display.getSystemColor(SWT.COLOR_BLUE));
		layout.setWidth(width);
		layout.setViewport(height, 0);
		return layout;
	}

	@After
	public void dispose() { if (layout != null) layout.dispose(); }

	@Test
	public void positionedTextWrapsAtItsOwnWidth() {
		HtmlLayout l = layout("<p style='position:absolute;left:25px;top:30px;width:80px'>"
				+ "several ordinary words that must wrap in this narrow box</p>", 400, 200);
		assertEquals(25, l.indent(0));
		assertEquals(30, l.top(0));
		assertEquals(80, l.textLayout(0).getWidth());
		assertTrue(l.textLayout(0).getLineCount() > 1);
	}

	@Test
	public void hitCoordinatesFollowRelativeOffset() {
		HtmlLayout l = layout("<p style='position:relative;left:40px;top:35px;width:180px'><a href='target'>link words</a></p>", 400, 200);
		assertEquals("target", l.linkAt(43, 35 + HtmlLayout.BLOCK_GAP + 3));
		assertNull(l.linkAt(3, HtmlLayout.BLOCK_GAP + 3));
		assertEquals(0, l.offsetAt(40, 35 + HtmlLayout.BLOCK_GAP));
	}

	@Test
	public void fixedScrollReusesMeasuredGeometryAndTextLayout() {
		HtmlLayout l = layout("<p style='position:fixed;left:10px;top:12px;width:160px'><a href='fixed'>fixed link</a></p>"
				+ "<p>flow</p>".repeat(30), 400, 200);
		TextLayout text = l.textLayout(0);
		int made = l.layoutsMade;
		l.setViewport(200, 150);
		assertEquals(162, l.top(0));
		assertEquals("fixed", l.linkAt(13, 162 + HtmlLayout.BLOCK_GAP + 3));
		assertSame(text, l.textLayout(0));
		assertEquals(made, l.layoutsMade);
	}

	@Test
	public void positionedContainerRetainsSharedTableRows() {
		HtmlLayout l = layout("<div style='position:relative;left:20px;top:10px;width:200px'><table>"
				+ "<tr><td>long words that will wrap repeatedly</td><td><a href='cell'>cell</a></td></tr>"
				+ "<tr><td>next</td></tr></table></div>", 500, 300);
		assertEquals(l.top(0), l.top(1));
		assertEquals(l.height(0), l.height(1));
		assertEquals(l.top(0) + l.height(0), l.top(2));
		assertEquals(20, l.cellX(0));
		assertEquals(l.cellX(0) + l.cellWidth(0), l.cellX(1));
		assertEquals(1, l.blockAt(l.cellX(1) + 1, l.top(1) + 1));
		assertEquals("cell", l.linkAt(l.indent(1) + 2, l.top(1) + HtmlLayout.CELL_PADDING + 2));
	}

	@Test
	public void imageArrivalInvalidatesAutoHeightAndFollowingFlow() {
		HtmlLayout l = layout("<div style='position:relative'><img src='a'><p>after</p></div>", 400, 200);
		int before = l.top(1);
		assertTrue(l.setNaturalSize(0, 30, 80));
		assertEquals(before + 80 - HtmlLayout.IMAGE_PLACEHOLDER, l.top(1));
	}

	@Test
	public void ordinaryPagesKeepTheLazyPrefixAndCachedLayouts() {
		HtmlLayout l = layout("<p>ordinary page words</p>".repeat(10000), 400, 200);
		assertNull(l.positioned());
		l.ensure(200);
		assertTrue(l.measured() < 20);
		assertTrue(l.layoutsMade < 20);
	}

	@Test
	public void cssImageSizesDrivePixelsAndFlowIncludingZero() {
		HtmlLayout l = layout("<img src='a' width='100' height='100' style='position:relative;width:20px;height:30px'>"
				+ "<p>after</p><img src='zero' style='width:0;height:0'>", 400, 200);
		l.setNaturalSize(0, 100, 100);
		assertEquals(20, l.shownWidth(0));
		assertEquals(30, l.shownHeight(0));
		assertEquals(30 + 2 * HtmlLayout.BLOCK_GAP, l.top(1));
		assertEquals(20, l.positioned().blockWidth(0));
		assertEquals(0, l.shownWidth(1));
		assertEquals(0, l.shownHeight(1));
	}

	@Test
	public void cssImageSingleDimensionKeepsNaturalAspectRatio() {
		HtmlLayout l = layout("<img src='a' style='width:30px'>", 400, 200);
		l.setNaturalSize(0, 100, 50);
		assertEquals(30, l.shownWidth(0));
		assertEquals(15, l.shownHeight(0));
	}

	@Test
	public void dragBeyondPositionedContentUsesNearestTextEnd() {
		HtmlLayout l = layout("<p style='position:relative;left:40px;top:20px;width:150px'>one</p><p>last</p>", 400, 200);
		assertEquals(7, l.offsetAt(2000, 2000));
		assertEquals(0, l.offsetAt(-100, -100));
	}

	@Test
	public void tableWithoutExplicitRowAndQuoteIndentRetainExistingGeometry() {
		HtmlLayout l = layout("<div style='position:relative'><blockquote><table><td>A</td><td>B</td></table></blockquote></div>", 400, 200);
		assertEquals(l.top(0), l.top(1));
		assertEquals(HtmlDocument.QUOTE_INDENT, l.cellX(0));
		assertEquals(l.cellX(0) + l.cellWidth(0), l.cellX(1));
	}

	@Test
	public void asymmetricBoxModelMatchesOrdinaryLayoutApartFromRelativeOffset() {
		String style = "margin:10px 20px 30px 40px;padding:3px 5px 7px 11px;border:2px solid red";
		String words = "several ordinary words that wrap inside a padded box";
		HtmlLayout plain = new HtmlLayout(display, HtmlDocument.parse("<p>before</p><p style='" + style + "'>"
				+ words + "</p><p>after</p>"), display.getSystemFont(), display.getSystemColor(SWT.COLOR_BLUE));
		try {
			plain.setWidth(300);
			HtmlLayout moved = layout("<div><p>before</p><p style='" + style + ";position:relative;left:6px;top:9px'>"
					+ words + "</p><p>after</p></div>", 300, 200);
			assertEquals(plain.textLayout(1).getWidth(), moved.textLayout(1).getWidth());
			assertEquals(plain.textLayout(1).getBounds(), moved.textLayout(1).getBounds());
			assertEquals(plain.indent(1) + 6, moved.indent(1));
			assertEquals(plain.top(1) + 9, moved.top(1));
			assertEquals(plain.contentTop(1), moved.contentTop(1));
			assertEquals(plain.height(1), moved.height(1));
			assertEquals(plain.boxLeft(1) + 6, moved.boxLeft(1));
			assertEquals(plain.boxTop(1) + 9, moved.boxTop(1));
			assertEquals(plain.boxWidth(1), moved.boxWidth(1));
			assertEquals(plain.boxHeight(1), moved.boxHeight(1));
			assertEquals(plain.top(2), moved.top(2));
		} finally {
			plain.dispose();
		}
	}

	@Test
	public void unrelatedFixedBoxPreservesOrdinaryBoxModelGeometry() {
		String html = "<p style='margin:9px 13px 17px 21px;padding:2px 4px 6px 8px;border:3px solid blue'>"
				+ "a box with asymmetric edges</p><p>after</p>";
		HtmlLayout plain = new HtmlLayout(display, HtmlDocument.parse(html), display.getSystemFont(), display.getSystemColor(SWT.COLOR_BLUE));
		try {
			plain.setWidth(300);
			HtmlLayout scene = layout(html + "<div style='position:fixed;left:290px;top:190px;width:5px;height:5px'></div>", 300, 200);
			for (int b = 0; b < 2; b++) {
				assertEquals(plain.indent(b), scene.indent(b));
				assertEquals(plain.top(b), scene.top(b));
				assertEquals(plain.contentTop(b), scene.contentTop(b));
				assertEquals(plain.height(b), scene.height(b));
				assertEquals(plain.textLayout(b).getWidth(), scene.textLayout(b).getWidth());
			}
		} finally {
			plain.dispose();
		}
	}

}
