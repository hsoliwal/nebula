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
 * Column widths of a table from its columns' minimum and maximum widths, as in CSS 2.1 automatic table layout (section
 * 17.5.2.2, no spans): pure, Display-free.
 */
public final class TableColumns {

	private TableColumns() {
	}

	/**
	 * Makes {@code widths[from .. from + span - 1]} hold at least {@code need} together: what they lack is shared
	 * equally over the columns, the first ones taking the remainder. A span of 1 is a plain maximum.
	 */
	public static void spread(int[] widths, int from, int span, int need) {
		int end = Math.min(widths.length, from + span), have = 0;
		for (int c = from; c < end; c++) have += widths[c];
		int lack = need - have, n = end - from;
		if (lack <= 0 || n <= 0) return;
		for (int c = from; c < end; c++) widths[c] += lack / n + (c - from < lack % n ? 1 : 0);
	}

	/**
	 * The column edges for {@code available} pixels: {@code edges[c]} is column c's left edge from the table's left, and
	 * {@code edges[n]} the table's width. A table whose maximums fit takes them (it is as narrow as its content); one
	 * whose minimums do not fit takes the minimums (it overflows); otherwise each column gets its minimum plus a share
	 * of the rest in proportion to its maximum less its minimum, rounded so that the table is exactly
	 * {@code available} wide.
	 *
	 * @param min per column, its widest unbreakable content
	 * @param max per column, its content laid out without wrapping (at least its min)
	 */
	public static int[] edges(int[] min, int[] max, int available) {
		int n = min.length;
		long sumMin = 0, sumMax = 0;
		for (int c = 0; c < n; c++) {
			sumMin += min[c];
			sumMax += Math.max(min[c], max[c]);
		}
		int[] edges = new int[n + 1];
		long extra = available - sumMin, spread = sumMax - sumMin, low = 0, grown = 0;
		for (int c = 0; c < n; c++) {
			low += min[c];
			grown += Math.max(min[c], max[c]) - min[c];
			long edge = sumMax <= available ? low + grown : extra <= 0 ? low : low + grown * extra / spread;
			edges[c + 1] = (int) edge;
		}
		return edges;
	}
}
