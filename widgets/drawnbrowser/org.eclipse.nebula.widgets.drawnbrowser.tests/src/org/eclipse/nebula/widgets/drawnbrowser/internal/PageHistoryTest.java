/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.eclipse.nebula.widgets.drawnbrowser.internal.PageHistory.ABOUT_BLANK;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.PageHistory.BACK;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.PageHistory.FORWARD;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.PageHistory.PUSH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The drawn browser's location and history, without a Display. */
public class PageHistoryTest {

	private static void go(PageHistory h, String url) {
		assertTrue(h.arrive(url, PUSH, h.begin()));
	}

	@Test
	public void itStartsBlankWithNoHistory() {
		PageHistory h = new PageHistory();
		assertEquals(ABOUT_BLANK, h.location());
		assertEquals(0, h.size());
		assertFalse(h.canBack());
		assertFalse(h.canForward());
	}

	@Test
	public void backAndForwardMoveAndANewPageDropsTheForwardEntries() {
		PageHistory h = new PageHistory();
		go(h, "file:/a.html");
		assertFalse("one page has nothing behind it", h.canBack());
		go(h, "file:/b.html");
		assertTrue(h.canBack());
		assertEquals("file:/a.html", h.backTarget());
		assertTrue(h.arrive(h.backTarget(), BACK, h.begin()));
		assertEquals("file:/a.html", h.location());
		assertTrue(h.canForward());
		assertEquals("file:/b.html", h.forwardTarget());
		assertTrue(h.arrive(h.forwardTarget(), FORWARD, h.begin()));
		assertEquals("file:/b.html", h.location());
		assertFalse(h.canForward());
		h.arrive(h.backTarget(), BACK, h.begin());
		go(h, "file:/c.html");
		assertEquals(2, h.size());
		assertFalse(h.canForward());
		assertEquals("file:/a.html", h.backTarget());
	}

	@Test
	public void aPageForAnOlderNavigationIsDropped() {
		PageHistory h = new PageHistory();
		int older = h.begin(), newer = h.begin();
		assertFalse(h.arrive("file:/old.html", PUSH, older));
		assertEquals(ABOUT_BLANK, h.location());
		assertEquals(0, h.size());
		assertTrue(h.arrive("file:/new.html", PUSH, newer));
		int stopped = h.begin();
		h.stop();
		assertFalse("stop drops the load", h.arrive("file:/x.html", PUSH, stopped));
		assertEquals("file:/new.html", h.location());
	}

	@Test
	public void textReplacesALoadAndIsAtAboutBlankKeepingTheHistory() {
		PageHistory h = new PageHistory();
		go(h, "file:/a.html");
		go(h, "file:/b.html");
		int loading = h.begin();
		h.setText();
		assertFalse(h.isCurrent(loading));
		assertFalse(h.arrive("file:/c.html", PUSH, loading));
		assertEquals(ABOUT_BLANK, h.location());
		assertEquals(2, h.size());
		assertTrue(h.canBack());
	}

	@Test
	public void referencesResolveAgainstTheLocation() {
		PageHistory h = new PageHistory();
		assertEquals("as written at about:blank", "img/x.png", h.resolve("img/x.png"));
		go(h, "http://example.org/docs/a/page.html");
		assertEquals("http://example.org/docs/a/x.png", h.resolve("x.png"));
		assertEquals("http://example.org/docs/y.html", h.resolve("../y.html"));
		assertEquals("https://other.org/", h.resolve("https://other.org/"));
		assertEquals("an invalid reference stays as written", "a b", h.resolve("a b"));
	}
}
