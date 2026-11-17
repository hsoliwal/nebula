/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

/**
 * The selection in the drawn page: a range of the page's text ({@link HtmlDocument#text}), made by find or by the
 * mouse. It holds no display or widget, so it is tested without a {@code Display}.
 * <p>
 * The range is kept as an anchor (where a drag started) and a caret (where it is now); {@link #start()} and
 * {@link #end()} are the two in order, as StyledText's selection is.
 * </p>
 */
public final class PageSelection {

	private int anchor, caret;

	/** The selection's start offset (equal to {@link #end()} when nothing is selected). */
	public int start() {
		return Math.min(anchor, caret);
	}

	/** The selection's end offset, exclusive. */
	public int end() {
		return Math.max(anchor, caret);
	}

	/** Where the selection was started. */
	public int anchor() {
		return anchor;
	}

	/** Whether nothing is selected. */
	public boolean isEmpty() {
		return anchor == caret;
	}

	/** Selects from {@code anchor} to {@code caret}, in either order. */
	public void select(int anchor, int caret) {
		this.anchor = anchor;
		this.caret = caret;
	}

	/** Selects nothing (a new page). */
	public void clear() {
		anchor = caret = 0;
	}

	/** The selected text of {@code doc}, or the empty string. */
	public String text(HtmlDocument doc) {
		int length = doc.text.length();
		return doc.text.substring(Math.min(start(), length), Math.min(end(), length));
	}
}
