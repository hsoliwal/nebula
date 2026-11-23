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

import org.junit.Test;

/** Paint-order regressions for CSS stacking contexts, independent of SWT. */
public class HtmlPaintOrderTest {

	@Test
	public void nestedContextCannotEscapeItsParentsStackLevel() {
		HtmlDocument document = HtmlDocument.parse("<div style='position:relative;z-index:0'>parent"
				+ "<p style='position:absolute;z-index:999'>child</p></div>"
				+ "<p style='position:relative;z-index:1'>sibling</p>");
		assertArrayEquals(new String[] { "parent", "child", "sibling" }, content(document));
		assertArrayEquals(new String[] { "background:0", "background:1", "parent", "background:2", "child",
				"background:3", "sibling" }, fragments(document));
	}

	@Test
	public void autoAndIntegerZeroHaveDifferentContextSemantics() {
		String template = "<div style='position:relative;z-index:%s'>parent"
				+ "<p style='position:absolute;z-index:999'>child</p></div>"
				+ "<p style='position:relative;z-index:1'>sibling</p>";
		assertArrayEquals(new String[] { "parent", "sibling", "child" },
				content(HtmlDocument.parse(String.format(template, "auto"))));
		assertArrayEquals(new String[] { "parent", "child", "sibling" },
				content(HtmlDocument.parse(String.format(template, "0"))));
	}

	@Test
	public void negativeContextsFollowOwnerBackgroundButPrecedeFlow() {
		HtmlDocument document = HtmlDocument.parse("<div style='position:relative;z-index:0;background:red'>parent"
				+ "<p style='position:absolute;z-index:-2'>negative</p><p>flow</p></div>");
		assertArrayEquals(new String[] { "background:0", "background:1", "background:2", "negative",
				"background:3", "parent", "flow" }, fragments(document));
	}

	@Test
	public void negativeDescendantOfAutoGroupJoinsTheEnclosingContext() {
		HtmlDocument document = HtmlDocument.parse("<div style='position:relative'>parent"
				+ "<p style='position:absolute;z-index:-1'>negative</p></div>");
		assertArrayEquals(new String[] { "background:0", "background:2", "negative", "background:1", "parent" },
				fragments(document));
	}

	@Test
	public void equalStackLevelsKeepTreeOrderThroughStaticAndAutoWrappers() {
		HtmlDocument document = HtmlDocument.parse("<p style='position:relative;z-index:4'>first</p>"
				+ "<div><div style='position:relative'><p style='position:absolute;z-index:4'>second</p></div></div>"
				+ "<p style='position:relative;z-index:4'>third</p>");
		assertArrayEquals(new String[] { "first", "second", "third" }, content(document));
	}

	@Test
	public void zeroContextsAndAutoGroupsShareTreeOrder() {
		HtmlDocument document = HtmlDocument.parse("<p style='position:relative;z-index:0'>first</p>"
				+ "<p style='position:relative'>second</p><p style='position:absolute;z-index:0'>third</p>");
		assertArrayEquals(new String[] { "first", "second", "third" }, content(document));
	}

	@Test
	public void fixedAutoStillCreatesAContext() {
		HtmlDocument document = HtmlDocument.parse("<div style='position:fixed'>fixed"
				+ "<p style='position:absolute;z-index:999'>child</p></div>"
				+ "<p style='position:relative;z-index:1'>sibling</p>");
		assertArrayEquals(new String[] { "fixed", "child", "sibling" }, content(document));
	}

	@Test
	public void fixedNegativeIsNotAnUnconditionalOverlay() {
		HtmlDocument document = HtmlDocument.parse("<p>flow</p><p style='position:fixed;z-index:-2'>fixed</p>"
				+ "<p style='position:relative;z-index:1'>positive</p>");
		assertArrayEquals(new String[] { "fixed", "flow", "positive" }, content(document));
	}

	@Test
	public void staticZIndexDoesNotCreateAContextOrReorderItsContent() {
		HtmlDocument document = HtmlDocument.parse("<div style='z-index:999'>first"
				+ "<p style='position:absolute;z-index:2'>child</p></div>"
				+ "<p style='position:relative;z-index:1'>sibling</p><p>last</p>");
		assertArrayEquals(new String[] { "first", "last", "sibling", "child" }, content(document));
	}

	@Test
	public void ownerContentSurroundingChildBlocksKeepsItsDocumentOrder() {
		HtmlDocument document = HtmlDocument.parse("<div style='position:relative;z-index:0'>before"
				+ "<p>inside</p>after</div>");
		assertArrayEquals(new String[] { "background:0", "background:1", "background:2", "before", "inside", "after" },
				fragments(document));
	}

	@Test
	public void unpositionedBackgroundsPrecedeContentAndPreserveAncestorOrder() {
		HtmlDocument document = HtmlDocument.parse("<div style='background:red'>before<p style='background:blue'>inside</p>after</div>");
		assertArrayEquals(new String[] { "background:0", "background:1", "background:2", "before", "inside", "after" },
				fragments(document));
	}

	@Test
	public void emptyPositionedBoxStillHasABackgroundFragment() {
		HtmlDocument document = HtmlDocument.parse("<div style='position:absolute;z-index:1;background:red;width:10px;height:10px'></div>");
		assertArrayEquals(new String[] { "background:0", "background:1" }, fragments(document));
	}

	@Test
	public void extremeStackLevelsCompareWithoutSubtractionOverflow() {
		HtmlDocument document = HtmlDocument.parse("<p style='position:relative;z-index:1'>high</p>"
				+ "<p style='position:relative;z-index:-1'>low</p><p>flow</p>");
		document.boxZIndex[document.blockBox[0]] = Integer.MAX_VALUE;
		document.boxZIndex[document.blockBox[1]] = Integer.MIN_VALUE;
		assertArrayEquals(new String[] { "low", "flow", "high" }, content(document));
	}

	@Test
	public void eachBoxAndBlockAppearsExactlyOnce() {
		HtmlDocument document = HtmlDocument.parse("<div><p>flow</p><div style='position:relative'>auto"
				+ "<p style='position:absolute;z-index:-2'>negative</p><div style='position:fixed'>fixed"
				+ "<p style='position:relative;z-index:1'>positive</p></div>end</div></div>");
		assertComplete(document, new HtmlPaintOrder(document));
	}

	@Test
	public void planIsAnImmutableSnapshotOfTheStyleLanes() {
		HtmlDocument document = HtmlDocument.parse("<p style='position:relative;z-index:1'>first</p>"
				+ "<p style='position:relative;z-index:2'>second</p>");
		HtmlPaintOrder before = new HtmlPaintOrder(document);
		document.boxZIndex[document.blockBox[0]] = 3;
		assertArrayEquals(new String[] { "first", "second" }, content(document, before));
		assertArrayEquals(new String[] { "second", "first" }, content(document));
	}

	@Test
	public void deepContextsUseNoRecursiveJavaTraversal() {
		StringBuilder html = new StringBuilder();
		for (int n = 0; n < 8000; n++) html.append("<div style='position:relative;z-index:0'>");
		html.append("<p>leaf</p>");
		for (int n = 0; n < 8000; n++) html.append("</div>");
		HtmlDocument document = HtmlDocument.parse(html.toString());
		HtmlPaintOrder order = new HtmlPaintOrder(document);
		assertArrayEquals(new String[] { "leaf" }, content(document, order));
		assertComplete(document, order);
	}

	@Test
	public void wideContextsAreSortedByNumericStackLevel() {
		StringBuilder html = new StringBuilder();
		String[] expected = new String[257];
		for (int z = 257; z > 0; z--) {
			html.append("<p style='position:absolute;z-index:").append(z).append("'>").append(z).append("</p>");
			expected[z - 1] = Integer.toString(z);
		}
		HtmlDocument document = HtmlDocument.parse(html.toString());
		assertArrayEquals(expected, content(document));
	}

	private static String[] fragments(HtmlDocument document) {
		HtmlPaintOrder order = new HtmlPaintOrder(document);
		String[] result = new String[order.size()];
		for (int step = 0; step < result.length; step++) {
			result[step] = order.isBackground(step) ? "background:" + order.index(step) : document.blockText(order.index(step));
		}
		return result;
	}

	private static String[] content(HtmlDocument document) {
		return content(document, new HtmlPaintOrder(document));
	}

	private static String[] content(HtmlDocument document, HtmlPaintOrder order) {
		String[] result = new String[document.blockCount];
		int next = 0;
		for (int step = 0; step < order.size(); step++) {
			if (!order.isBackground(step)) result[next++] = document.blockText(order.index(step));
		}
		assertEquals("all blocks present", document.blockCount, next);
		return result;
	}

	private static void assertComplete(HtmlDocument document, HtmlPaintOrder order) {
		assertEquals(document.boxCount + document.blockCount, order.size());
		boolean[] backgrounds = new boolean[document.boxCount], contents = new boolean[document.blockCount];
		for (int step = 0; step < order.size(); step++) {
			boolean[] seen = order.isBackground(step) ? backgrounds : contents;
			int index = order.index(step);
			assertFalse("duplicate fragment at " + step, seen[index]);
			seen[index] = true;
		}
		for (boolean seen : backgrounds) assertEquals(true, seen);
		for (boolean seen : contents) assertEquals(true, seen);
	}
}
