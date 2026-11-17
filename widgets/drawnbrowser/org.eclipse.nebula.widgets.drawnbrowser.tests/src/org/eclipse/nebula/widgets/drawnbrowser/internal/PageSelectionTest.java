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
	public void aRangeAcrossBlocksTakesTheirTextsAndPastTheEndIsClipped() {
		PageSelection selection = new PageSelection();
		selection.select(6, 15);
		assertEquals("betagamma", selection.text(DOC));
		selection.select(12, 99);
		assertEquals("mma", selection.text(DOC));
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
