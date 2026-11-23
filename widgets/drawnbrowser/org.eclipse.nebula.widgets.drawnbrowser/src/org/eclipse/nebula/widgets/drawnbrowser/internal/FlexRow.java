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
		return layout(min, max, basis, grow, null, null, available, gap, justify);
	}

	/**
	 * Lays out a row with explicit finite, nonnegative shrink factors and no horizontal insets.
	 *
	 * @param min minimum outer widths
	 * @param max unwrapped outer content widths
	 * @param basis outer basis widths, or -1 for content width
	 * @param grow integer grow factors
	 * @param shrink shrink factors, or null to retain the legacy unit-factor rounding
	 * @param available available row width
	 * @param gap gap between items
	 * @param justify one of the justify-content constants
	 * @return item x coordinates followed by their outer widths
	 */
	public static int[] layout(int[] min, int[] max, int[] basis, int[] grow, double[] shrink,
			int available, int gap, int justify) {
		return layout(min, max, basis, grow, shrink, null, available, gap, justify);
	}

	/**
	 * Lays out the existing one-line row, including CSS weighted shrink. Shrink weights use the original inner
	 * base sizes: padding and borders do not become shrinkable content. Zero factors freeze an item. When the
	 * sum of unfrozen factors is below one, only that fraction of the initial deficit is distributed; overflow
	 * can remain. Each minimum violation freezes an item before the next distribution from original bases.
	 * Output uses cumulative integer rounding, not a subpixel browser layout. The original overload retains
	 * its legacy rounding; explicit factors use this weighted algorithm and saturate unrepresentable coordinates.
	 *
	 * @param min minimum outer widths
	 * @param max unwrapped outer content widths
	 * @param basis outer basis widths, or -1 for content width
	 * @param grow integer grow factors
	 * @param shrink finite nonnegative shrink factors, or null for the legacy path
	 * @param insets horizontal padding and borders per item, or null for zero
	 * @param available available row width
	 * @param gap gap between items
	 * @param justify one of the justify-content constants
	 * @return item x coordinates followed by their outer widths
	 * @throws IllegalArgumentException for mismatched factor/inset counts or invalid factors/insets
	 */
	public static int[] layout(int[] min, int[] max, int[] basis, int[] grow, double[] shrink, int[] insets,
			int available, int gap, int justify) {
		int n = min.length;
		if (shrink != null) {
			if (shrink.length != n || insets != null && insets.length != n) throw new IllegalArgumentException("item counts");
			for (int i = 0; i < n; i++) {
				if (!Double.isFinite(shrink[i]) || shrink[i] < 0) throw new IllegalArgumentException("flex-shrink");
				if (insets != null && insets[i] < 0) throw new IllegalArgumentException("insets");
			}
		}
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
				long share = (shrink == null ? free * weight / growTotal : share(free, weight, growTotal)) - done;
				width[i] += (int) share;
				done += share;
			}
			free = 0;
		} else if (free < 0) {
			free = shrink == null ? shrink(width, min, free)
					: shrink(width, min, max, basis, shrink, insets, available, gap, free);
		}
		long x = free > 0 ? justify == CENTER ? free / 2 : justify == END ? free : 0 : 0;
		long extraGap = free > 0 && justify == SPACE_BETWEEN && n > 1 ? free / (n - 1) : 0;
		for (int i = 0; i < n; i++) {
			out[i] = shrink == null ? (int) x : coordinate(x);
			out[n + i] = width[i];
			x += shrink == null ? width[i] + gap + extraGap : (long) width[i] + gap + extraGap;
		}
		return out;
	}

	/* The common growth path stays primitive; only a product outside long needs exact wider arithmetic. */
	private static long share(long amount, long weight, long total) {
		if (weight == 0 || amount == 0) return 0;
		if (amount <= Long.MAX_VALUE / weight) return amount * weight / total;
		return java.math.BigInteger.valueOf(amount).multiply(java.math.BigInteger.valueOf(weight))
				.divide(java.math.BigInteger.valueOf(total)).longValueExact();
	}

	private static int coordinate(long value) {
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
	}

	private static long free(int[] width, int available, int gap) {
		long remaining = (long) available - (long) gap * (width.length - 1);
		for (int value : width) remaining -= value;
		return remaining;
	}

	/* A zero content basis has zero weight even when factor/scale would overflow. */
	private static double shrinkWeight(int base, int inset, double factor, double scale) {
		int inner = Math.max(0, base - inset);
		return inner == 0 ? 0 : factor / scale * inner;
	}

	/* CSS 9.7 for the admitted row/minimum subset. One writer; original bases remain immutable in each round. */
	private static long shrink(int[] width, int[] min, int[] max, int[] basis, double[] factors, int[] insets,
			int available, int gap, long initialFree) {
		int n = width.length;
		int[] base = new int[n];
		boolean[] frozen = new boolean[n];
		for (int i = 0; i < n; i++) {
			base[i] = basis[i] >= 0 ? basis[i] : max[i];
			frozen[i] = factors[i] == 0 || base[i] < min[i];
		}
		while (true) {
			double factorSum = 0, scale = 0;
			for (int i = 0; i < n; i++) {
				if (frozen[i]) continue;
				width[i] = base[i];
				factorSum = Math.min(1, factorSum + factors[i]);
				if (base[i] > (insets == null ? 0 : insets[i])) scale = Math.max(scale, factors[i]);
			}
			long remaining = free(width, available, gap);
			if (remaining >= 0 || scale == 0) return remaining;
			double deficit = -(double) remaining;
			if (factorSum < 1) deficit = Math.min(deficit, -(double) initialFree * factorSum);
			double total = 0;
			for (int i = 0; i < n; i++) {
				if (!frozen[i]) total += shrinkWeight(base[i], insets == null ? 0 : insets[i], factors[i], scale);
			}
			if (total == 0) return remaining;
			double weight = 0;
			long done = 0;
			boolean violated = false;
			for (int i = 0; i < n; i++) {
				if (frozen[i]) continue;
				weight += shrinkWeight(base[i], insets == null ? 0 : insets[i], factors[i], scale);
				long edge = (long) Math.floor(deficit * Math.min(1, weight / total));
				long next = (long) base[i] - (edge - done);
				done = edge;
				if (next < min[i]) {
					width[i] = min[i];
					frozen[i] = true;
					violated = true;
				} else {
					width[i] = (int) next;
				}
			}
			if (!violated) return free(width, available, gap);
		}
	}

	/* Legacy unit-factor entry point: preserve its established integer rounding for existing callers. */
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
