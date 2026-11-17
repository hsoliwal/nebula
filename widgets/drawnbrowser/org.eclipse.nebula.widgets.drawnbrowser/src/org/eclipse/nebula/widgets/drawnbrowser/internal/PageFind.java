/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import java.util.Locale;

/**
 * Find in the drawn page. Offsets are in the page's text ({@link HtmlDocument#text}: the text of every block, one after
 * another), not in the HTML. Pure functions of the document: tested without a {@code Display}.
 * <p>
 * {@link #find} follows JFace's {@code IFindReplaceTarget.findAndSelect}: searching forward finds the first match that
 * starts at or after the offset, backward the last one that starts at or before it; -1 searches the whole page from
 * its start (forward) or end (backward). There is no wrap, as in JFace (the find dialog wraps). A match never spans
 * two blocks: the blocks' texts touch in {@link HtmlDocument#text}, but they are separate paragraphs.
 * </p>
 */
public final class PageFind {

	private PageFind() {
	}

	/** The block whose text holds {@code offset}, or -1. */
	public static int blockOf(HtmlDocument doc, int offset) {
		int low = 0, high = doc.blockCount - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			if (offset < doc.textStart[mid]) high = mid - 1;
			else if (offset >= doc.textEnd[mid]) low = mid + 1;
			else return mid;
		}
		return -1;
	}

	/** The start offset of the match, or -1. */
	public static int find(HtmlDocument doc, int offset, String find, boolean forward, boolean caseSensitive, boolean wholeWord) {
		if (find == null || find.isEmpty() || doc.text.isEmpty()) return -1;
		String text = caseSensitive ? doc.text : doc.text.toLowerCase(Locale.ROOT);
		String key = caseSensitive ? find : find.toLowerCase(Locale.ROOT);
		if (text.length() != doc.text.length()) {
			text = doc.text; // a case mapping that changes lengths: offsets must stay the page's, so search as written
			key = find;
		}
		int from = offset < 0 ? (forward ? 0 : text.length()) : offset;
		int at = forward ? text.indexOf(key, from) : text.lastIndexOf(key, from);
		while (at >= 0 && !acceptable(doc, text, at, key.length(), wholeWord)) {
			at = forward ? text.indexOf(key, at + 1) : at == 0 ? -1 : text.lastIndexOf(key, at - 1);
		}
		return at;
	}

	/* inside one block, and with no letter or digit touching it when whole words are asked for */
	private static boolean acceptable(HtmlDocument doc, String text, int at, int length, boolean wholeWord) {
		int b = blockOf(doc, at);
		if (b < 0 || at + length > doc.textEnd[b]) return false;
		if (!wholeWord) return true;
		boolean before = at > doc.textStart[b] && isWord(text.charAt(at - 1));
		boolean after = at + length < doc.textEnd[b] && isWord(text.charAt(at + length));
		return !before && !after;
	}

	private static boolean isWord(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}
}
