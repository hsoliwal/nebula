/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.nebula.visualization.internal.xygraph.decimation;

/**
 * Min/max index pyramid over one packed {@code double[]} column (level of detail for the sample index).
 * <p>
 * Level 1 holds min and max of blocks of {@value #LEAF} samples; every further level folds {@value #FAN} blocks of
 * the level below. Only complete blocks are written and a complete block is never rewritten, so an older pyramid
 * object stays valid while a newer one (built by {@link #extend(double[], int)} for appended samples) shares and
 * extends the same arrays. NaN propagates through {@link Math#min(double, double)}/{@link Math#max(double, double)},
 * so a NaN anywhere in a range shows up as a NaN range result.
 * <p>
 * Shared with the GWT/RWT client renderer: int and double state only.
 */
public final class PackedMinMaxPyramid {

	/** Samples per level-1 block (power of two). */
	public static final int LEAF = 16;
	private static final int LEAF_SHIFT = 4;
	/** Blocks of the level below folded per block from level 2 up (power of two). */
	public static final int FAN = 8;
	private static final int FAN_SHIFT = 3;

	private final double[] data;
	private final int size;
	/** mins[k], maxs[k] and counts[k] describe level k + 1. */
	private final double[][] mins;
	private final double[][] maxs;
	private final int[] counts;

	private PackedMinMaxPyramid(double[] data, int size, double[][] mins, double[][] maxs, int[] counts) {
		this.data = data;
		this.size = size;
		this.mins = mins;
		this.maxs = maxs;
		this.counts = counts;
	}

	/** Builds the pyramid of {@code data[0 .. size)}. */
	public static PackedMinMaxPyramid build(double[] data, int size) {
		return new PackedMinMaxPyramid(data, 0, new double[0][], new double[0][], new int[0]).extend(data, size);
	}

	/**
	 * Returns a pyramid for {@code data[0 .. newSize)} that reuses every complete block of this one. {@code data}
	 * must hold the same values as this pyramid's data for indices below {@link #size()} (appended or copied on
	 * growth).
	 */
	public PackedMinMaxPyramid extend(double[] newData, int newSize) {
		int levels = levelsFor(newSize);
		double[][] nm = new double[levels][];
		double[][] nx = new double[levels][];
		int[] nc = new int[levels];
		int blockSize = LEAF;
		for (int k = 0; k < levels; k++) {
			int need = newSize / blockSize;
			int have = k < counts.length ? counts[k] : 0;
			nm[k] = ensure(k < mins.length ? mins[k] : null, need);
			nx[k] = ensure(k < maxs.length ? maxs[k] : null, need);
			fillLevel(k, newData, nm, nx, have, need);
			nc[k] = need;
			blockSize <<= FAN_SHIFT;
		}
		return new PackedMinMaxPyramid(newData, newSize, nm, nx, nc);
	}

	private static int levelsFor(int n) {
		int levels = 0;
		int blocks = n >> LEAF_SHIFT;
		while (blocks > 0) {
			levels++;
			blocks >>= FAN_SHIFT;
		}
		return levels;
	}

	private static double[] ensure(double[] a, int need) {
		if (a != null && a.length >= need) {
			return a;
		}
		int cap = a == null ? Math.max(need, 16) : Math.max(need, a.length + (a.length >> 1));
		double[] b = new double[cap];
		if (a != null) {
			System.arraycopy(a, 0, b, 0, a.length);
		}
		return b;
	}

	private static void fillLevel(int k, double[] d, double[][] nm, double[][] nx, int from, int to) {
		double[] mn = nm[k];
		double[] mx = nx[k];
		for (int b = from; b < to; b++) {
			double lo;
			double hi;
			if (k == 0) {
				int i = b << LEAF_SHIFT;
				lo = d[i];
				hi = lo;
				for (int e = i + LEAF; ++i < e;) {
					lo = Math.min(lo, d[i]);
					hi = Math.max(hi, d[i]);
				}
			} else {
				double[] cm = nm[k - 1];
				double[] cx = nx[k - 1];
				int c = b << FAN_SHIFT;
				lo = cm[c];
				hi = cx[c];
				for (int e = c + FAN; ++c < e;) {
					lo = Math.min(lo, cm[c]);
					hi = Math.max(hi, cx[c]);
				}
			}
			mn[b] = lo;
			mx[b] = hi;
		}
	}

	public int size() {
		return size;
	}

	public double[] data() {
		return data;
	}

	private static int shift(int level) {
		return level == 0 ? 0 : LEAF_SHIFT + (level - 1) * FAN_SHIFT;
	}

	private double minAt(int level, int i) {
		return level == 0 ? data[i] : mins[level - 1][i];
	}

	private double maxAt(int level, int i) {
		return level == 0 ? data[i] : maxs[level - 1][i];
	}

	/**
	 * Min and max of {@code data[from .. to]} (inclusive) into {@code out[0]} and {@code out[1]}; NaN if any value
	 * in the range is NaN. Cost O(levels * FAN).
	 */
	public void rangeMinMax(int from, int to, double[] out) {
		double lo = Double.POSITIVE_INFINITY;
		double hi = Double.NEGATIVE_INFINITY;
		int a = from;
		int b = to + 1;
		int level = 0;
		int top = counts.length;
		while (a < b) {
			if (level == top) {
				for (int i = a; i < b; i++) {
					lo = Math.min(lo, minAt(level, i));
					hi = Math.max(hi, maxAt(level, i));
				}
				break;
			}
			int fan = level == 0 ? LEAF : FAN;
			while (a < b && (a & (fan - 1)) != 0) {
				lo = Math.min(lo, minAt(level, a));
				hi = Math.max(hi, maxAt(level, a++));
			}
			while (a < b && (b & (fan - 1)) != 0) {
				lo = Math.min(lo, minAt(level, --b));
				hi = Math.max(hi, maxAt(level, b));
			}
			if (a >= b) {
				break;
			}
			int cut = Math.max(a, counts[level] * fan);
			if (cut < b) {
				for (int i = cut; i < b; i++) {
					lo = Math.min(lo, minAt(level, i));
					hi = Math.max(hi, maxAt(level, i));
				}
				b = cut;
			}
			a = a / fan;
			b = b / fan;
			level++;
		}
		out[0] = lo;
		out[1] = hi;
	}

	/**
	 * First index in {@code [from, to]} whose pixel position under {@code m} differs from {@code pixel}, or -1.
	 * {@code m} must be monotone and the range NaN free.
	 */
	public int firstDiff(int from, int to, int pixel, AxisMapping m) {
		int k = from;
		while (k <= to) {
			int level = largestBlockForward(k, to);
			if (level == 0) {
				if (m.position(data[k]) != pixel) {
					return k;
				}
				k++;
				continue;
			}
			int bi = k >> shift(level);
			if (uniform(level, bi, pixel, m)) {
				k += 1 << shift(level);
				continue;
			}
			return descendFirst(level, bi, pixel, m);
		}
		return -1;
	}

	/**
	 * Last index in {@code [from, to]} whose pixel position under {@code m} differs from {@code pixel}, or -1.
	 */
	public int lastDiff(int from, int to, int pixel, AxisMapping m) {
		int k = to;
		while (k >= from) {
			int level = largestBlockBackward(from, k);
			if (level == 0) {
				if (m.position(data[k]) != pixel) {
					return k;
				}
				k--;
				continue;
			}
			int bi = (k + 1 >> shift(level)) - 1;
			if (uniform(level, bi, pixel, m)) {
				k -= 1 << shift(level);
				continue;
			}
			return descendLast(level, bi, pixel, m);
		}
		return -1;
	}

	private boolean uniform(int level, int bi, int pixel, AxisMapping m) {
		return m.position(mins[level - 1][bi]) == pixel && m.position(maxs[level - 1][bi]) == pixel;
	}

	/** Largest level whose block starts at k, is complete and ends at or before to (0 = single sample). */
	private int largestBlockForward(int k, int to) {
		int best = 0;
		for (int level = 1; level <= counts.length; level++) {
			int s = shift(level);
			if ((k & ((1 << s) - 1)) != 0 || (k >> s) >= counts[level - 1] || k + (1 << s) - 1 > to) {
				break;
			}
			best = level;
		}
		return best;
	}

	/** Largest level whose block ends at k, is complete and starts at or after from. */
	private int largestBlockBackward(int from, int k) {
		int best = 0;
		for (int level = 1; level <= counts.length; level++) {
			int s = shift(level);
			int end = k + 1;
			if ((end & ((1 << s) - 1)) != 0 || (end >> s) > counts[level - 1] || end - (1 << s) < from) {
				break;
			}
			best = level;
		}
		return best;
	}

	private int descendFirst(int level, int bi, int pixel, AxisMapping m) {
		while (level > 1) {
			int child = bi << FAN_SHIFT;
			int end = child + FAN;
			while (child < end && uniform(level - 1, child, pixel, m)) {
				child++;
			}
			bi = child;
			level--;
		}
		int i = bi << LEAF_SHIFT;
		for (int e = i + LEAF; i < e; i++) {
			if (m.position(data[i]) != pixel) {
				return i;
			}
		}
		throw new IllegalStateException("non-uniform block without a differing sample"); //$NON-NLS-1$
	}

	private int descendLast(int level, int bi, int pixel, AxisMapping m) {
		while (level > 1) {
			int child = (bi << FAN_SHIFT) + FAN - 1;
			int end = bi << FAN_SHIFT;
			while (child > end && uniform(level - 1, child, pixel, m)) {
				child--;
			}
			bi = child;
			level--;
		}
		int i = (bi << LEAF_SHIFT) + LEAF - 1;
		for (int e = bi << LEAF_SHIFT; i >= e; i--) {
			if (m.position(data[i]) != pixel) {
				return i;
			}
		}
		throw new IllegalStateException("non-uniform block without a differing sample"); //$NON-NLS-1$
	}
}
