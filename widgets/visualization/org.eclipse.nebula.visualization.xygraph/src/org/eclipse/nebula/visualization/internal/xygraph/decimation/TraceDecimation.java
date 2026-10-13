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
 * Pure decimation of a chronological line trace to the vertex list that {@code Trace.paintInternalFigure} builds
 * for the same view, in O(columns * log(samples)) instead of O(visible samples).
 * <p>
 * The view is decimated only when the per-sample loop would build one polyline from a contiguous run of samples
 * inside both axis ranges, optionally preceded and followed by one clip point on the x range ends; everything else
 * returns {@link DecimatedTrace#INELIGIBLE} and the caller runs the per-sample loop. The column rule is written out in
 * {@code .m3/nebula-viz-decimation-20261013/DESIGN.md} section 2.2.
 * <p>
 * Shared with the GWT/RWT client renderer: int/double state, no int-overflow reliance.
 */
public final class TraceDecimation {

	private TraceDecimation() {
	}

	/**
	 * @param view    the view to decimate
	 * @param pyramid min/max pyramid over {@code view.columns.y[0 .. size)}
	 * @param out     reusable vertex buffer
	 * @param tmp     scratch of length 4 or more
	 */
	public static DecimatedTrace decimate(TraceView view, PackedMinMaxPyramid pyramid, PolylineBuilder out,
			double[] tmp) {
		PackedColumns c = view.columns;
		int n = c.size;
		if (n <= 0 || !c.sorted || !view.x.isMonotone() || !view.y.isMonotone() || pyramid.size() != n) {
			return DecimatedTrace.ineligible(view);
		}
		double[] xs = c.x;
		double[] ys = c.y;
		double min = view.xLower > view.xUpper ? view.xUpper : view.xLower;
		double max = view.xUpper > view.xLower ? view.xUpper : view.xLower;
		if (min > xs[n - 1] || max < xs[0]) {
			return DecimatedTrace.ineligible(view);
		}
		int lo = min > xs[0] ? near(xs, n, min, true) : 0;
		int hi = max < xs[n - 1] ? near(xs, n, max, false) : n - 1;
		if (lo < 0 || hi < 0 || hi >= n || lo > hi || hi - lo + 1 < view.minSamples) {
			return DecimatedTrace.ineligible(view);
		}
		boolean loIn = inside(view, xs[lo], ys[lo]);
		boolean hiIn = inside(view, xs[hi], ys[hi]);
		if (nanMarker(view, loIn, xs[lo], ys[lo]) || nanMarker(view, hiIn, xs[hi], ys[hi])) {
			return DecimatedTrace.ineligible(view);
		}
		int a = loIn ? lo : lo + 1;
		int b = hiIn ? hi : hi - 1;
		if (b - a < 1 || !interiorInside(view, xs, pyramid, a, b, tmp)) {
			return DecimatedTrace.ineligible(view);
		}
		int lCount = loIn ? 0 : straight(view, xs[lo], ys[lo], xs[a], ys[a], tmp);
		double lx = tmp[0];
		double ly = tmp[1];
		int rCount = hiIn ? 0 : straight(view, xs[b], ys[b], xs[hi], ys[hi], tmp);
		if (lCount < 0 || rCount < 0) {
			return DecimatedTrace.ineligible(view);
		}
		columns(view, xs, pyramid, a, b, lCount > 0, lx, ly, rCount > 0, tmp[0], tmp[1], out, tmp);
		return new DecimatedTrace(view, DecimatedTrace.DECIMATED, out.toArray(), a, b);
	}

	/** Port of {@code Trace.nearBinarySearchX} over the packed x column. */
	static int near(double[] xs, int n, double key, boolean left) {
		int low = 0;
		int high = n - 1;
		while (low <= high) {
			int mid = low + ((high - low) >> 1);
			int cmp = Double.compare(xs[mid], key);
			if (cmp < 0) {
				if (mid < n - 1 && key < xs[mid + 1]) {
					return left ? mid : mid + 1;
				}
				low = mid + 1;
			} else if (cmp > 0) {
				if (mid > 0 && key > xs[mid - 1]) {
					return left ? mid - 1 : mid;
				}
				high = mid - 1;
			} else {
				return mid;
			}
		}
		return -(low + 1);
	}

	/** {@code Range.inRange}: inclusive, either bound order. */
	static boolean inRange(double lower, double upper, double v) {
		return lower <= upper ? v >= lower && v <= upper : v >= upper && v <= lower;
	}

	private static boolean inside(TraceView v, double x, double y) {
		return inRange(v.xLower, v.xUpper, x) && inRange(v.yLower, v.yUpper, y);
	}

	/** The loop draws a NaN marker for a NaN sample whose x is in range. */
	private static boolean nanMarker(TraceView v, boolean in, double x, double y) {
		return !in && y != y && inRange(v.xLower, v.xUpper, x);
	}

	private static boolean interiorInside(TraceView v, double[] xs, PackedMinMaxPyramid p, int a, int b,
			double[] tmp) {
		if (!inRange(v.xLower, v.xUpper, xs[a]) || !inRange(v.xLower, v.xUpper, xs[b])) {
			return false;
		}
		if (v.x.log && xs[a] <= 0) {
			return false;
		}
		p.rangeMinMax(a, b, tmp);
		double lo = tmp[0];
		double hi = tmp[1];
		if (lo != lo || hi != hi || (v.y.log && lo <= 0)) {
			return false;
		}
		return inRange(v.yLower, v.yUpper, lo) && inRange(v.yLower, v.yUpper, hi);
	}

	/**
	 * Port of {@code Trace.getStraightLineIntersection(dp1, dp2)} for samples without errors: writes the first
	 * intersection to {@code tmp[0], tmp[1]} and returns the count (0..2), or -1 where the loop would overflow its
	 * two-element result.
	 */
	static int straight(TraceView v, double x1, double y1, double x2, double y2, double[] tmp) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		int count = 0;
		if (dy != 0.0) {
			double x = (v.yLower - y1) * dx / dy + x1;
			count = take(v, x, v.yLower, x1, y1, x2, y2, tmp, count, false);
			x = (v.yUpper - y1) * dx / dy + x1;
			count = take(v, x, v.yUpper, x1, y1, x2, y2, tmp, count, false);
		}
		if (count == 2) {
			return 2;
		}
		if (dx != 0.0) {
			double y = (v.xLower - x1) * dy / dx + y1;
			count = take(v, v.xLower, y, x1, y1, x2, y2, tmp, count, true);
			if (count >= 0) {
				y = (v.xUpper - x1) * dy / dx + y1;
				count = take(v, v.xUpper, y, x1, y1, x2, y2, tmp, count, true);
			}
		}
		return count;
	}

	private static int take(TraceView v, double x, double y, double x1, double y1, double x2, double y2,
			double[] tmp, int count, boolean dedup) {
		if (count < 0 || !evalDP(v, x, y, x1, y1, x2, y2)) {
			return count;
		}
		if (dedup) {
			for (int i = 0; i < count; i++) {
				if (same(tmp[2 * i], x) && same(tmp[2 * i + 1], y)) {
					return count;
				}
			}
		}
		if (count == 2) {
			return -1;
		}
		tmp[2 * count] = x;
		tmp[2 * count + 1] = y;
		return count + 1;
	}

	/** Port of {@code Trace.evalDP}. */
	private static boolean evalDP(TraceView v, double x, double y, double x1, double y1, double x2, double y2) {
		if (!inside(v, x, y) || !inRange(x1, x2, x) || !inRange(y1, y2, y)) {
			return false;
		}
		return !(same(x, x1) && same(y, y1)) && !(same(x, x2) && same(y, y2));
	}

	/** {@code Sample.equals} on one coordinate: equal bit patterns (NaN canonical). */
	private static boolean same(double a, double b) {
		return Double.compare(a, b) == 0;
	}

	private static void columns(TraceView v, double[] xs, PackedMinMaxPyramid p, int a, int b, boolean hasL,
			double lx, double ly, boolean hasR, double rx, double ry, PolylineBuilder out, double[] tmp) {
		AxisMapping mx = v.x;
		AxisMapping my = v.y;
		double[] ys = p.data();
		out.reset();
		int i = hasL ? a : a + 1;
		int colX = hasL ? mx.position(lx) : mx.position(xs[a]);
		int y1 = hasL ? my.position(ly) : my.position(ys[a]);
		int rX = hasR ? mx.position(rx) : 0;
		out.add(colX, y1);
		while (true) {
			int j = i <= b && mx.position(xs[i]) == colX ? lastInColumn(xs, mx, i, b, colX) : i - 1;
			boolean rawDone = j == b;
			if (rawDone && (!hasR || rX == colX)) {
				return;
			}
			flush(p, my, colX, y1, i, j, out, tmp);
			if (rawDone) {
				out.add(rX, my.position(ry));
				return;
			}
			i = j + 1;
			colX = mx.position(xs[i]);
			y1 = my.position(ys[i]);
			out.add(colX, y1);
			i++;
		}
	}

	/** Region flush of one finished column: interior min and max (if any) and the column's last point. */
	private static void flush(PackedMinMaxPyramid p, AxisMapping my, int colX, int y1, int i, int j,
			PolylineBuilder out, double[] tmp) {
		if (j < i) {
			return;
		}
		int first = p.firstDiff(i, j, y1, my);
		if (first < 0) {
			return;
		}
		int yn = my.position(p.data()[j]);
		int last = j > i ? p.lastDiff(i, j - 1, yn, my) : -1;
		if (last >= first) {
			p.rangeMinMax(first, last, tmp);
			int pa = my.position(tmp[0]);
			int pb = my.position(tmp[1]);
			out.add(colX, Math.min(pa, pb));
			out.add(colX, Math.max(pa, pb));
		}
		out.add(colX, yn);
	}

	/** Last index in {@code [i, b]} whose pixel x is {@code colX}, given {@code xs[i]} maps to it (gallop + bisect). */
	static int lastInColumn(double[] xs, AxisMapping mx, int i, int b, int colX) {
		int good = i;
		int step = 1;
		int bad = -1;
		while (true) {
			int probe = good + step;
			if (probe > b) {
				probe = b;
				if (probe == good) {
					return good;
				}
				if (mx.position(xs[probe]) == colX) {
					return probe;
				}
				bad = probe;
				break;
			}
			if (mx.position(xs[probe]) != colX) {
				bad = probe;
				break;
			}
			good = probe;
			step <<= 1;
		}
		while (bad - good > 1) {
			int mid = good + ((bad - good) >> 1);
			if (mx.position(xs[mid]) == colX) {
				good = mid;
			} else {
				bad = mid;
			}
		}
		return good;
	}
}
