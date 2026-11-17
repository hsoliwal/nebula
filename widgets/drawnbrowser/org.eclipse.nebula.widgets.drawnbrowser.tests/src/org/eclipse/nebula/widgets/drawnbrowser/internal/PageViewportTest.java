/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The drawn browser's scroll window, without a Display. */
public class PageViewportTest {

	private static PageViewport viewport(int view, int content, int y) {
		PageViewport v = new PageViewport();
		v.resize(view, content);
		v.scrollTo(y);
		return v;
	}

	@Test
	public void scrollingIsClampedToThePage() {
		PageViewport v = viewport(100, 250, 0);
		assertEquals(150, v.maxY());
		assertEquals(150, v.scrollTo(1000));
		assertEquals(150, v.y());
		assertEquals(0, v.scrollTo(150));
		assertEquals(-150, v.scrollTo(-5));
		assertEquals(0, v.y());
		PageViewport fits = viewport(100, 60, 10);
		assertEquals(0, fits.maxY());
		assertEquals(0, fits.y());
	}

	@Test
	public void aResizeClampsThePositionAndSaysSo() {
		PageViewport v = viewport(100, 250, 150);
		assertTrue(v.resize(200, 250));
		assertEquals(50, v.y());
		assertFalse(v.resize(200, 250));
		assertFalse("a longer page keeps the position", v.resize(200, 900));
		assertEquals(50, v.y());
		v.resize(-1, -1);
		assertEquals(0, v.view());
		assertEquals(0, v.content());
		assertEquals(0, v.y());
	}

	@Test
	public void scrollingDownKeepsTheLowerRowsAndExposesTheBottom() {
		PageViewport v = viewport(100, 1000, 0);
		int delta = v.scrollTo(30);
		assertEquals(30, delta);
		assertFalse(v.isFullRepaint(delta));
		assertArrayEquals(new int[] { 30, 0, 70, 70, 30 }, band(v, delta));
	}

	@Test
	public void scrollingUpKeepsTheUpperRowsAndExposesTheTop() {
		PageViewport v = viewport(100, 1000, 500);
		int delta = v.scrollTo(470);
		assertEquals(-30, delta);
		assertArrayEquals(new int[] { 0, 30, 70, 0, 30 }, band(v, delta));
	}

	@Test
	public void aMoveOfAWholeViewRepaintsEverything() {
		PageViewport v = viewport(100, 1000, 0);
		for (int delta : new int[] { 100, -100, 250, Integer.MIN_VALUE, Integer.MAX_VALUE }) {
			assertTrue(v.isFullRepaint(delta));
			assertEquals(0, v.copyFrom(delta));
			assertEquals(0, v.copyTo(delta));
			assertEquals(0, v.copyHeight(delta));
			assertEquals(0, v.exposedTop(delta));
			assertEquals(100, v.exposedHeight(delta));
		}
		assertFalse(v.isFullRepaint(99));
		assertFalse(v.isFullRepaint(-99));
	}

	/* copy from, copy to, kept rows, exposed top, exposed rows */
	private static int[] band(PageViewport v, int delta) {
		return new int[] { v.copyFrom(delta), v.copyTo(delta), v.copyHeight(delta), v.exposedTop(delta), v.exposedHeight(delta) };
	}

	/**
	 * The window as a column of pixel rows, each holding the page y it shows. After every scroll the kept rows are
	 * copied and only the exposed band is painted; the window must then show exactly the page from the new position.
	 */
	@Test
	public void keptAndExposedRowsRebuildTheWindowForEveryMove() {
		int view = 37, content = 400;
		for (int from = 0; from <= content - view; from += 7) {
			for (int to = -20; to <= content + 20; to += 5) {
				PageViewport v = viewport(view, content, from);
				int[] window = new int[view];
				for (int r = 0; r < view; r++) window[r] = v.y() + r;
				int delta = v.scrollTo(to);
				int[] next = window.clone();
				if (delta != 0) {
					System.arraycopy(window, v.copyFrom(delta), next, v.copyTo(delta), v.copyHeight(delta));
					for (int r = v.exposedTop(delta); r < v.exposedTop(delta) + v.exposedHeight(delta); r++) next[r] = v.y() + r;
					assertEquals("kept and exposed rows fill the view", view, v.copyHeight(delta) + v.exposedHeight(delta));
				}
				for (int r = 0; r < view; r++) assertEquals("from " + from + " to " + to + " row " + r, v.y() + r, next[r]);
			}
		}
	}

	@Test
	public void aDragOutsideTheViewStepsByHowFarOutCappedAtHalfTheView() {
		PageViewport v = viewport(100, 1000, 0);
		for (int y : new int[] { 0, 50, 99 }) assertEquals("inside " + y, 0, v.edgeStep(y, 16));
		assertEquals(-16, v.edgeStep(-1, 16));
		assertEquals(-40, v.edgeStep(-40, 16));
		assertEquals(-50, v.edgeStep(-500, 16));
		assertEquals(16, v.edgeStep(100, 16));
		assertEquals(31, v.edgeStep(130, 16));
		assertEquals(50, v.edgeStep(1000, 16));
		assertEquals("a small view still moves a line", 16, viewport(20, 1000, 0).edgeStep(500, 16));
	}

	@Test
	public void aPageStepIsTheViewLessALineButAtLeastALine() {
		assertEquals(84, viewport(100, 1000, 0).page(16));
		assertEquals(16, viewport(10, 1000, 0).page(16));
	}

	@Test
	public void aNewPageStartsAtTheTop() {
		PageViewport v = viewport(100, 1000, 400);
		v.reset();
		assertEquals(0, v.y());
	}
}
