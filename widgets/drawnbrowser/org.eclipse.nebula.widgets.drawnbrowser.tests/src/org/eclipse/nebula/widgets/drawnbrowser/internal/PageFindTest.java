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

import org.junit.Test;

/** Find in the drawn page, without a Display. */
public class PageFindTest {

	/* blocks "Alpha beta" [0,10), "gamma Beta" [10,20), "alphabet" [20,28) */
	private static final HtmlDocument DOC = HtmlDocument.parse("<p>Alpha beta</p><p>gamma Beta</p><p>alphabet</p>");

	private static int find(int offset, String key, boolean forward, boolean caseSensitive, boolean wholeWord) {
		return PageFind.find(DOC, offset, key, forward, caseSensitive, wholeWord);
	}

	@Test
	public void theDocumentIsAsAssumed() {
		assertEquals("Alpha betagamma Betaalphabet", DOC.text);
	}

	@Test
	public void forwardFindsTheFirstMatchAtOrAfterTheOffset() {
		assertEquals(6, find(-1, "beta", true, false, false));
		assertEquals(6, find(6, "beta", true, false, false));
		assertEquals(16, find(7, "beta", true, false, false));
		assertEquals(-1, find(17, "beta", true, false, false));
	}

	@Test
	public void backwardFindsTheLastMatchAtOrBeforeTheOffset() {
		assertEquals(16, find(-1, "beta", false, false, false));
		assertEquals(16, find(16, "beta", false, false, false));
		assertEquals(6, find(15, "beta", false, false, false));
		assertEquals(-1, find(5, "beta", false, false, false));
	}

	@Test
	public void caseAndWholeWords() {
		assertEquals("only the lower case beta", -1, find(7, "beta", true, true, false));
		assertEquals(16, find(-1, "Beta", true, true, false));
		assertEquals(0, find(-1, "alpha", true, false, true));
		assertEquals("alphabet is not the word alpha", -1, find(1, "alpha", true, false, true));
		assertEquals(20, find(1, "alpha", true, false, false));
		assertEquals("a word ends at its block's end", 6, find(-1, "beta", true, false, true));
	}

	@Test
	public void aMatchNeverSpansTwoBlocks() {
		assertEquals(-1, find(-1, "betagamma", true, false, false));
		assertEquals(-1, find(-1, "Betaalpha", true, true, false));
		assertEquals(-1, find(-1, "ta ga", true, false, false));
	}

	@Test
	public void nothingToFindFindsNothing() {
		assertEquals(-1, find(-1, "", true, false, false));
		assertEquals(-1, find(-1, null, true, false, false));
		assertEquals(-1, PageFind.find(HtmlDocument.parse(""), -1, "a", true, false, false));
	}

	@Test
	public void blocksOfOffsets() {
		assertEquals(0, PageFind.blockOf(DOC, 0));
		assertEquals(0, PageFind.blockOf(DOC, 9));
		assertEquals(1, PageFind.blockOf(DOC, 10));
		assertEquals(2, PageFind.blockOf(DOC, 27));
		assertEquals(-1, PageFind.blockOf(DOC, 28));
		assertEquals(-1, PageFind.blockOf(DOC, -1));
	}
}
