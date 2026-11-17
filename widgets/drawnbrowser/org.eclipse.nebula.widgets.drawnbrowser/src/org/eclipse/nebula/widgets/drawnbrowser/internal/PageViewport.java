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
 * The drawn browser's vertical viewport: the scroll position over the page, clamped to the page, and the pixels a
 * scroll keeps or exposes. It holds no display, widget or image, so it is tested without a {@code Display}.
 * <p>
 * A scroll by {@code delta} pixels keeps {@code view - |delta|} rows of the window's pixels, copied from
 * {@link #copyFrom(int)} to {@link #copyTo(int)}, and exposes the band {@link #exposedTop(int)} of
 * {@link #exposedHeight(int)} rows, the only part that is painted again. A scroll of a whole view or more repaints
 * everything ({@link #isFullRepaint(int)}).
 * </p>
 * <p>
 * Pattern: SWT's {@code org.eclipse.swt.internal.ViewportRuntime} (the origin and what a move dirties), for one axis
 * in whole pixels.
 * </p>
 */
public final class PageViewport {

	private int y, view, content;

	/** The scroll position: the page y at the top of the view. */
	public int y() {
		return y;
	}

	/** The view height in pixels. */
	public int view() {
		return view;
	}

	/** The page height in pixels, as far as the page is laid out. */
	public int content() {
		return content;
	}

	/** The largest scroll position: the page's end at the view's bottom, or 0 when the page fits. */
	public int maxY() {
		return Math.max(0, content - view);
	}

	/**
	 * Sets the view and page heights (negative values count as 0) and clamps the scroll position.
	 *
	 * @return whether the scroll position changed
	 */
	public boolean resize(int viewHeight, int contentHeight) {
		view = Math.max(0, viewHeight);
		content = Math.max(0, contentHeight);
		int clamped = Math.min(y, maxY());
		boolean moved = clamped != y;
		y = clamped;
		return moved;
	}

	/** Back to the page's top, for a new page. */
	public void reset() {
		y = 0;
	}

	/**
	 * Scrolls to page y {@code target}, clamped to {@code [0, maxY()]}.
	 *
	 * @return the move: the new position minus the old one, 0 when it did not move
	 */
	public int scrollTo(int target) {
		int clamped = Math.max(0, Math.min(target, maxY()));
		int delta = clamped - y;
		y = clamped;
		return delta;
	}

	/** Whether a move of {@code delta} keeps no pixels, so the whole view is painted again. */
	public boolean isFullRepaint(int delta) {
		return Math.abs((long) delta) >= view;
	}

	/** The view y of the first kept row before a move of {@code delta}; 0 when no row is kept. */
	public int copyFrom(int delta) {
		return isFullRepaint(delta) ? 0 : Math.max(0, delta);
	}

	/** The view y the kept rows move to; 0 when no row is kept. */
	public int copyTo(int delta) {
		return isFullRepaint(delta) ? 0 : Math.max(0, -delta);
	}

	/** The number of rows a move of {@code delta} keeps. */
	public int copyHeight(int delta) {
		return isFullRepaint(delta) ? 0 : view - Math.abs(delta);
	}

	/** The view y of the band a move of {@code delta} exposes: the bottom when scrolling down, the top when up. */
	public int exposedTop(int delta) {
		return isFullRepaint(delta) || delta < 0 ? 0 : view - delta;
	}

	/** The height of the exposed band: {@code |delta|}, or the whole view. */
	public int exposedHeight(int delta) {
		return isFullRepaint(delta) ? view : Math.abs(delta);
	}

	/** A page step for page up and down: the view less one line, at least one line. */
	public int page(int line) {
		return Math.max(line, view - line);
	}
}
