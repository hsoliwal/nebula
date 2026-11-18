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

import org.junit.Test;

/** CSS 2.1 automatic table column widths, without spans. */
public class TableColumnsTest {

	@Test
	public void aTableThatFitsTakesItsMaximums() {
		assertArrayEquals(new int[] { 0, 30, 70 }, TableColumns.edges(new int[] { 10, 20 }, new int[] { 30, 40 }, 100));
		assertArrayEquals("exactly fitting", new int[] { 0, 30, 70 }, TableColumns.edges(new int[] { 10, 20 }, new int[] { 30, 40 }, 70));
	}

	@Test
	public void aTableThatCannotFitItsMinimumsOverflows() {
		assertArrayEquals(new int[] { 0, 60, 130 }, TableColumns.edges(new int[] { 60, 70 }, new int[] { 80, 90 }, 100));
		assertArrayEquals("exactly the minimums", new int[] { 0, 60, 130 }, TableColumns.edges(new int[] { 60, 70 }, new int[] { 80, 90 }, 130));
	}

	@Test
	public void theRestIsSharedByMaximumLessMinimum() {
		// 40 px beyond the minimums; the columns can grow by 20 and 60, so they get 10 and 30
		assertArrayEquals(new int[] { 0, 20, 70 }, TableColumns.edges(new int[] { 10, 20 }, new int[] { 30, 80 }, 70));
	}

	@Test
	public void roundingKeepsTheTableExactlyAsWideAsAvailable() {
		assertArrayEquals(new int[] { 0, 6, 13, 20 }, TableColumns.edges(new int[3], new int[] { 10, 10, 10 }, 20));
	}

	@Test
	public void aMaximumBelowTheMinimumCountsAsTheMinimum() {
		assertArrayEquals(new int[] { 0, 10 }, TableColumns.edges(new int[] { 10 }, new int[] { 5 }, 100));
		assertArrayEquals("no columns", new int[] { 0 }, TableColumns.edges(new int[0], new int[0], 100));
	}
}
