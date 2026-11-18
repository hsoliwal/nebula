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
	private boolean dragging;
	/* where the dragging mouse is, in view coordinates (it may be outside the view) */
	private int pointerX, pointerY;

	/** The dragging mouse is at ({@code x}, {@code y}) of the view. */
	public void pointer(int x, int y) {
		pointerX = x;
		pointerY = y;
	}

	/** The dragging mouse's x in the view. */
	public int pointerX() {
		return pointerX;
	}

	/** The dragging mouse's y in the view (negative above it, past its height below it). */
	public int pointerY() {
		return pointerY;
	}

	/** A mouse press at {@code offset}: a drag starts there, nothing is selected yet. */
	public void press(int offset) {
		anchor = caret = offset;
		dragging = true;
	}

	/**
	 * The drag reaches {@code offset}: the selection runs from where it started to there.
	 *
	 * @return whether the selection changed (false when no drag is going on)
	 */
	public boolean drag(int offset) {
		if (!dragging || caret == offset) return false;
		caret = offset;
		return true;
	}

	/** The mouse is released: the drag ends, the selection stays. */
	public void release() {
		dragging = false;
	}

	/** Whether a drag is going on. */
	public boolean isDragging() {
		return dragging;
	}

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
		dragging = false;
	}

	/**
	 * The selected text of {@code doc}, or the empty string. Blocks are separate paragraphs, so a line break separates
	 * the text taken from one block from the next one's (the page's text has none between them).
	 */
	public String text(HtmlDocument doc) {
		int length = doc.text.length(), to = Math.min(end(), length);
		StringBuilder out = new StringBuilder();
		for (int at = Math.min(start(), length); at < to;) {
			int b = PageFind.blockOf(doc, at);
			int stop = b < 0 ? to : Math.min(to, doc.textEnd[b]);
			out.append(doc.text, at, stop);
			at = stop;
			int next = at < to ? PageFind.blockOf(doc, at) : -1;
			if (at < to) out.append(b >= 0 && next >= 0 && doc.sameRow(b, next) ? '\t' : '\n'); // cells of a row: tabs
		}
		return out.toString();
	}
}
