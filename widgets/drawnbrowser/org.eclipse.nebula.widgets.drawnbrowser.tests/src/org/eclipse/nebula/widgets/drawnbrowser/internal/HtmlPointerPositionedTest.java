/*******************************************************************************
 * Copyright (c) 2026 Synexia and others.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Test;

/** SWT-measured reverse z-order pointer targeting of retained painted fragments. */
public class HtmlPointerPositionedTest {

	private final Display display = Display.getDefault();
	private HtmlLayout layout;

	private HtmlPositionedLayout scene(String html) {
		document = HtmlDocument.parse(html);
		layout = new HtmlLayout(display, document,
				display.getSystemFont(), display.getSystemColor(SWT.COLOR_BLUE));
		layout.setWidth(350);
		layout.setViewport(220, 0);
		HtmlPositionedLayout scene = layout.positioned();
		assertNotNull(scene);
		return scene;
	}

	@After
	public void dispose() {
		if (layout != null) layout.dispose();
	}

	private static String overlapping(String frontPointer) {
		return "<div style='position:relative;width:200px;height:120px'>"
				+ "<p style='position:absolute;left:10px;top:10px;width:100px;height:40px'>"
				+ "<a href='back'>back</a></p>"
				+ "<p style='position:absolute;left:10px;top:10px;width:100px;height:40px;"
				+ "z-index:7;pointer-events:" + frontPointer + "'>"
				+ "<a href='front'>front</a></p></div>";
	}

	@Test
	public void nonTargetableForegroundAllowsBackgroundLinkHit() {
		HtmlPositionedLayout scene = scene(overlapping("none"));
		HtmlDocument document = layoutDocument();
		int back = blockWithText(document, "back");
		int front = blockWithText(document, "front");
		assertTrue(back >= 0 && front >= 0);
		int px = scene.blockX(back) + 5;
		int py = scene.blockY(back, 0) + 5;
		assertEquals("the higher painted layer must not steal a pointer hit",
				back, scene.blockAt(px, py, 0));
	}

	@Test
	public void targetableForegroundWinsTheSameZOrder() {
		HtmlPositionedLayout scene = scene(overlapping("auto"));
		HtmlDocument document = layoutDocument();
		int front = blockWithText(document, "front");
		assertTrue(front >= 0);
		int px = scene.blockX(front) + 5;
		int py = scene.blockY(front, 0) + 5;
		assertEquals(front, scene.blockAt(px, py, 0));
	}

	@Test
	public void explicitAutoChildRemainsTargetableUnderNoneParent() {
		HtmlPositionedLayout scene = scene("<div style='position:relative;width:190px;height:120px;"
				+ "pointer-events:none'>"
				+ "<p style='position:absolute;left:20px;top:20px;width:100px;height:40px;"
				+ "pointer-events:auto'>child</p></div>");
		HtmlDocument document = layoutDocument();
		int child = blockWithText(document, "child");
		assertTrue(child >= 0);
		assertEquals(child, scene.blockAt(scene.blockX(child) + 5,
				scene.blockY(child, 0) + 5, 0));
	}

	private HtmlDocument document;

	private HtmlDocument layoutDocument() {
		return document;
	}

	private static int blockWithText(HtmlDocument document, String target) {
		for (int block = 0; block < document.blockCount; block++) {
			if (target.equals(document.blockText(block))) return block;
		}
		return -1;
	}
}
