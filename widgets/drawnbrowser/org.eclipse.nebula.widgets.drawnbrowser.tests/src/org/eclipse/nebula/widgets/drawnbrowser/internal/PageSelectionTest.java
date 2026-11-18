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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The selection in the drawn page, without a Display. */
public class PageSelectionTest {

	private static final HtmlDocument DOC = HtmlDocument.parse("<p>Alpha beta</p><p>gamma</p>");

	@Test
	public void itStartsEmpty() {
		PageSelection selection = new PageSelection();
		assertTrue(selection.isEmpty());
		assertEquals("", selection.text(DOC));
	}

	@Test
	public void aRangeIsOrderedWhicheverWayItWasMade() {
		PageSelection cells = new PageSelection();
		HtmlDocument table = HtmlDocument.parse("<table><tr><td>a</td><td>b</td></tr><tr><td>c</td></tr></table><p>d</p>");
		cells.select(0, table.text.length());
		assertEquals("cells of a row are joined by tabs", "a\tb\nc\nd", cells.text(table));
		PageSelection selection = new PageSelection();
		selection.select(10, 6);
		assertEquals(6, selection.start());
		assertEquals(10, selection.end());
		assertEquals(10, selection.anchor());
		assertEquals("beta", selection.text(DOC));
		assertFalse(selection.isEmpty());
		selection.select(6, 10);
		assertEquals(6, selection.start());
		assertEquals(10, selection.end());
		assertEquals(6, selection.anchor());
	}

	@Test
	public void aRangeAcrossBlocksSeparatesThemAndPastTheEndIsClipped() {
		PageSelection selection = new PageSelection();
		selection.select(6, 15);
		assertEquals("beta\ngamma", selection.text(DOC));
		selection.select(0, 10);
		assertEquals("a whole block, no break after it", "Alpha beta", selection.text(DOC));
		selection.select(12, 99);
		assertEquals("mma", selection.text(DOC));
		HtmlDocument three = HtmlDocument.parse("<p>a</p><p>b</p><p>c</p>");
		selection.select(0, 3);
		assertEquals("a\nb\nc", selection.text(three));
	}

	@Test
	public void aDragSelectsFromThePressToWhereTheMouseIs() {
		PageSelection selection = new PageSelection();
		assertFalse("no drag without a press", selection.drag(5));
		selection.press(8);
		assertTrue(selection.isDragging());
		assertTrue(selection.isEmpty());
		assertTrue(selection.drag(3));
		assertFalse("the same place changes nothing", selection.drag(3));
		assertEquals(3, selection.start());
		assertEquals(8, selection.end());
		assertEquals(8, selection.anchor());
		selection.pointer(12, -30);
		assertEquals(12, selection.pointerX());
		assertEquals(-30, selection.pointerY());
		selection.release();
		assertFalse(selection.isDragging());
		assertFalse("after the release the mouse moves freely", selection.drag(1));
		assertEquals(3, selection.start());
	}

	@Test
	public void clearSelectsNothing() {
		PageSelection selection = new PageSelection();
		selection.select(1, 4);
		selection.clear();
		assertTrue(selection.isEmpty());
		assertEquals(0, selection.start());
	}
}
