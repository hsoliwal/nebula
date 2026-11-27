/*******************************************************************************
 * Copyright (c) 2026 Synexia and others.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CSS pointer eligibility is independent from visibility, paint order and native controls. */
public class HtmlPointerEventsTest {

	@Test
	public void propertyInheritanceAndBlockChildOverride() {
		HtmlStyles styles = new HtmlStyles();
		int[] root = HtmlStyles.root();
		assertEquals(HtmlStyles.POINTER_AUTO, root[HtmlStyles.POINTER_EVENTS]);
		int[] none = styles.compute(root, "section", null, "pointer-events:none");
		assertEquals(HtmlStyles.POINTER_NONE, none[HtmlStyles.POINTER_EVENTS]);
		assertEquals(HtmlStyles.POINTER_NONE,
				styles.compute(none, "p", null, null)[HtmlStyles.POINTER_EVENTS]);
		assertEquals(HtmlStyles.POINTER_AUTO,
				styles.compute(none, "p", null, "pointer-events:auto")[HtmlStyles.POINTER_EVENTS]);
		assertEquals(HtmlStyles.POINTER_NONE,
				styles.compute(none, "p", null, "pointer-events:invalid")[HtmlStyles.POINTER_EVENTS]);
		assertEquals(HtmlStyles.POINTER_AUTO,
				styles.compute(none, "p", null, "pointer-events:auto;pointer-events:invalid")
					[HtmlStyles.POINTER_EVENTS]);
	}

	@Test
	public void blockAndBoxLanesRetainInheritedAndOverriddenState() {
		HtmlDocument document = HtmlDocument.parse(
				"<section style='pointer-events:none'><p>disabled</p>"
				+ "<p style='pointer-events:auto'>enabled</p></section>");
		int disabled = -1, enabled = -1;
		for (int b = 0; b < document.blockCount; b++) {
			if ("disabled".equals(document.blockText(b))) disabled = b;
			if ("enabled".equals(document.blockText(b))) enabled = b;
		}
		assertTrue(disabled >= 0 && enabled >= 0);
		assertTrue("pointer eligibility uses the resolved scene", document.boxLayout);
		assertEquals(HtmlStyles.POINTER_NONE, document.blockPointerEvents[disabled]);
		assertEquals(HtmlStyles.POINTER_NONE, document.boxPointerEvents[document.blockBox[disabled]]);
		assertEquals(HtmlStyles.POINTER_AUTO, document.blockPointerEvents[enabled]);
		assertEquals(HtmlStyles.POINTER_AUTO, document.boxPointerEvents[document.blockBox[enabled]]);
	}

	@Test
	public void inlineLinksUseTheirOwnComputedPointerLanes() {
		HtmlDocument document = HtmlDocument.parse(
				"<p><a href='/off' style='pointer-events:none'>off</a>"
				+ "<a href='/on'>on</a></p>");
		int disabled = 0, enabled = 0;
		for (int run = 0; run < document.runTotal; run++) {
			int link = document.runLink[run];
			if (link < 0) continue;
			String href = document.href[link];
			if ("/off".equals(href)) {
				assertEquals(HtmlStyles.POINTER_NONE, document.runPointerEvents[run]);
				disabled++;
			} else if ("/on".equals(href)) {
				assertEquals(HtmlStyles.POINTER_AUTO, document.runPointerEvents[run]);
				enabled++;
			}
		}
		assertTrue("disabled link run", disabled > 0);
		assertTrue("enabled link run", enabled > 0);
	}

	@Test
	public void paintOrderDoesNotDependOnPointerEligibility() {
		String start = "<div style='position:relative;z-index:0'>";
		HtmlDocument original = HtmlDocument.parse(start + "<p>first</p><p>second</p></div>");
		HtmlDocument disabled = HtmlDocument.parse(
				start + "<p style='pointer-events:none'>first</p><p>second</p></div>");
		HtmlPaintOrder a = new HtmlPaintOrder(original), b = new HtmlPaintOrder(disabled);
		assertEquals(a.size(), b.size());
		for (int step = 0; step < a.size(); step++) {
			assertEquals(a.isBackground(step), b.isBackground(step));
			assertEquals(a.index(step), b.index(step));
		}
	}

	@Test
	public void ordinaryDocumentRetainsLazyLayoutWithoutPointerRule() {
		HtmlDocument normal = HtmlDocument.parse("<p>ordinary</p>");
		assertFalse(normal.boxLayout);
		assertEquals(HtmlStyles.POINTER_AUTO, normal.blockPointerEvents[0]);
		assertEquals(HtmlStyles.POINTER_AUTO, normal.runPointerEvents[0]);
	}
}
