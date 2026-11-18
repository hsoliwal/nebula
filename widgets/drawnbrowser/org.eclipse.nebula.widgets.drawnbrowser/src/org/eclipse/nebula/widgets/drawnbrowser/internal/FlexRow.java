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
 * One line of a row flex container (CSS Flexible Box Layout, "resolve flexible lengths", one line, no wrap): pure,
 * Display-free. Pattern: Yoga (MIT, react/yoga cac7b345c0), its flex basis, grow and shrink steps.
 */
public final class FlexRow {

	/** justify-content. */
	public static final int START = 0, CENTER = 1, END = 2, SPACE_BETWEEN = 3;

	private FlexRow() {
	}

	/**
	 * The x and the width of each item: {@code result[i]} is item i's x from the line's start and
	 * {@code result[n + i]} its width.
	 * <ul>
	 * <li>An item's base size is its basis (at least its minimum) or, without one, its content width (its maximum).</li>
	 * <li>Free space left with a growing item is shared by flex-grow weights (rounded so the line is exactly full).</li>
	 * <li>A line too narrow shrinks the items in proportion to their base size (flex-shrink 1) but never below their
	 * minimum; an item that reaches its minimum is frozen and the rest shrink again.</li>
	 * <li>What is left places the items by justify-content; {@code gap} separates them.</li>
	 * </ul>
	 *
	 * @param min   per item, its minimum content width
	 * @param max   per item, its content width laid out without wrapping
	 * @param basis per item, a flex basis or width in px, or -1 for none
	 * @param grow  per item, its flex-grow weight (0: does not grow)
	 */
	public static int[] layout(int[] min, int[] max, int[] basis, int[] grow, int available, int gap, int justify) {
		int n = min.length;
		int[] width = new int[n], out = new int[2 * n];
		if (n == 0) return out;
		long used = (long) gap * (n - 1), growTotal = 0;
		for (int i = 0; i < n; i++) {
			width[i] = basis[i] >= 0 ? Math.max(basis[i], min[i]) : Math.max(max[i], min[i]);
			used += width[i];
			growTotal += Math.max(0, grow[i]);
		}
		long free = available - used;
		if (free > 0 && growTotal > 0) {
			long done = 0, weight = 0;
			for (int i = 0; i < n; i++) {
				weight += Math.max(0, grow[i]);
				long share = free * weight / growTotal - done; // cumulative: the line ends exactly at available
				width[i] += (int) share;
				done += share;
			}
			free = 0;
		} else if (free < 0) {
			free = shrink(width, min, free);
		}
		long x = free > 0 ? justify == CENTER ? free / 2 : justify == END ? free : 0 : 0;
		long extraGap = free > 0 && justify == SPACE_BETWEEN && n > 1 ? free / (n - 1) : 0;
		for (int i = 0; i < n; i++) {
			out[i] = (int) x;
			out[n + i] = width[i];
			x += width[i] + gap + extraGap;
		}
		return out;
	}

	/* shrinks the widths by -free in proportion to them, never below their minimum; returns what is still missing (<= 0) */
	private static long shrink(int[] width, int[] min, long free) {
		boolean[] frozen = new boolean[width.length];
		while (free < 0) {
			long weights = 0;
			for (int i = 0; i < width.length; i++) if (!frozen[i]) weights += width[i];
			if (weights == 0) return free;
			long taken = 0, weight = 0, done = 0;
			boolean violated = false;
			int[] next = width.clone();
			for (int i = 0; i < width.length; i++) {
				if (frozen[i]) continue;
				weight += width[i];
				long share = -free * weight / weights - done;
				done += share;
				next[i] = (int) (width[i] - share);
				if (next[i] < min[i]) {
					next[i] = min[i];
					frozen[i] = true;
					violated = true;
				}
			}
			for (int i = 0; i < width.length; i++) {
				taken += width[i] - next[i];
				width[i] = next[i];
			}
			free += taken;
			if (!violated) return free;
		}
		return free;
	}
}
