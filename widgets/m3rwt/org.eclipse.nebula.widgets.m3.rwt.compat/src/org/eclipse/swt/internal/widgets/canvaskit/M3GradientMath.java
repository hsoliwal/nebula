/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.widgets.canvaskit;

/**
 * Pure arithmetic for drawing SWT patterns with canvas gradients: the repeated gradient over a shape's bounds, its
 * colour stops, the alpha bands and image tiles. No allocation besides the returned arrays; float and int only, so
 * the class also compiles under GWT. Catalogue: interval projection (dot product), floor/ceil period cover.
 */
public final class M3GradientMath {

	/** More periods than this are not laid out; the gradient is clamped at its end points instead. */
	static final int MAX_PERIODS = 256;
	/** Alpha bands per gradient period when the two alpha values differ. */
	static final int BANDS_PER_PERIOD = 16;
	/** Upper bound of alpha bands per shape. */
	static final int MAX_BANDS = 256;
	/** Floats per band: four corner points (x, y) and the band's alpha. */
	static final int BAND_FLOATS = 9;

	private M3GradientMath () {
	}

	/**
	 * The canvas gradient that draws an SWT gradient pattern over {@code box} = {x0, y0, x1, y1}: SWT repeats the
	 * gradient outside its end points (GDI+ tile, cairo repeat), so the canvas gradient spans the whole periods that
	 * cover the box.
	 *
	 * @return {gx1, gy1, gx2, gy2, periods, firstPeriod}; periods is 1 and the end points are the pattern's own
	 *         when the cover needs more than {@link #MAX_PERIODS} periods or the gradient has no length
	 */
	static float[] gradient (float x1, float y1, float x2, float y2, float[] box) {
		float dx = x2 - x1, dy = y2 - y1;
		float length2 = dx * dx + dy * dy;
		if (length2 == 0) return new float[] {x1, y1, x2, y2, 1, 0};
		float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
		for (int corner = 0; corner < 4; corner++) {
			float px = (corner & 1) == 0 ? box[0] : box[2];
			float py = (corner & 2) == 0 ? box[1] : box[3];
			float t = ((px - x1) * dx + (py - y1) * dy) / length2;
			min = Math.min (min, t);
			max = Math.max (max, t);
		}
		int first = (int) Math.floor (min);
		int last = (int) Math.ceil (max);
		if (last <= first) last = first + 1;
		if (last - first > MAX_PERIODS) return new float[] {x1, y1, x2, y2, 1, 0};
		return new float[] {x1 + dx * first, y1 + dy * first, x1 + dx * last, y1 + dy * last, last - first, first};
	}

	/**
	 * Colour stops of {@code periods} repeated periods: pairs (offset, colour) with colour 0 for colour 1 and 1 for
	 * colour 2; each period runs from colour 1 to colour 2 and the next starts again at colour 1 (hard edge).
	 */
	static float[] stops (int periods) {
		float[] stops = new float[periods * 4];
		for (int k = 0; k < periods; k++) {
			stops[k * 4] = k / (float) periods;
			stops[k * 4 + 1] = 0;
			stops[k * 4 + 2] = (k + 1) / (float) periods;
			stops[k * 4 + 3] = 1;
		}
		return stops;
	}

	/**
	 * Alpha bands across the gradient (perpendicular strips), each with the alpha of its middle, so that a gradient
	 * whose two alpha values differ can be drawn without per-stop alpha. Band corners extend past {@code box}.
	 *
	 * @param gradient the result of {@link #gradient}
	 * @return {@link #BAND_FLOATS} floats per band
	 */
	static float[] bands (float[] gradient, float[] box, int alpha1, int alpha2) {
		int periods = (int) gradient[4];
		int perPeriod = Math.max (1, Math.min (BANDS_PER_PERIOD, MAX_BANDS / periods));
		int count = periods * perPeriod;
		float gx = gradient[2] - gradient[0], gy = gradient[3] - gradient[1];
		float length = (float) Math.sqrt (gx * gx + gy * gy);
		float nx = length == 0 ? 0 : -gy / length, ny = length == 0 ? 1 : gx / length;
		float reach = (float) Math.hypot (box[2] - box[0], box[3] - box[1]) + (float) Math.hypot (box[0] - gradient[0], box[1] - gradient[1]) + 1;
		float[] bands = new float[count * BAND_FLOATS];
		for (int band = 0; band < count; band++) {
			float u0 = band / (float) count, u1 = (band + 1) / (float) count;
			// widen the outermost bands so that the pad regions beyond the gradient are covered too
			float s0 = band == 0 ? -reach / Math.max (length, 1) : u0;
			float s1 = band == count - 1 ? 1 + reach / Math.max (length, 1) : u1;
			int o = band * BAND_FLOATS;
			corner (bands, o, gradient, gx, gy, s0, nx, ny, -reach);
			corner (bands, o + 2, gradient, gx, gy, s1, nx, ny, -reach);
			corner (bands, o + 4, gradient, gx, gy, s1, nx, ny, reach);
			corner (bands, o + 6, gradient, gx, gy, s0, nx, ny, reach);
			float within = ((band % perPeriod) + 0.5f) / perPeriod;
			bands[o + 8] = Math.round (alpha1 + (alpha2 - alpha1) * within);
		}
		return bands;
	}

	private static void corner (float[] bands, int offset, float[] gradient, float gx, float gy, float s, float nx, float ny, float r) {
		bands[offset] = gradient[0] + gx * s + nx * r;
		bands[offset + 1] = gradient[1] + gy * s + ny * r;
	}

	/** The canvas {@code globalAlpha} for a pattern alpha under the GC alpha, both 0..255, rounded to 2 decimals. */
	static float globalAlpha (int gcAlpha, int patternAlpha) {
		return Math.round (gcAlpha * patternAlpha / 650.25f) / 100f;
	}

	/** Bounding box {x0, y0, x1, y1} of interleaved x, y coordinates. */
	static float[] extent (int[] points) {
		float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		for (int i = 0; i + 1 < points.length; i += 2) include (box, points[i], points[i + 1]);
		return empty (box);
	}

	/** Bounding box {x0, y0, x1, y1} of interleaved x, y coordinates. */
	static float[] extent (float[] points) {
		float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		for (int i = 0; i + 1 < points.length; i += 2) include (box, points[i], points[i + 1]);
		return empty (box);
	}

	private static void include (float[] box, float x, float y) {
		box[0] = Math.min (box[0], x);
		box[1] = Math.min (box[1], y);
		box[2] = Math.max (box[2], x);
		box[3] = Math.max (box[3], y);
	}

	private static float[] empty (float[] box) {
		if (box[0] > box[2]) return new float[] {0, 0, 0, 0};
		return box;
	}

	/**
	 * Tiles of an image pattern tiled from (0, 0) that meet {@code box}.
	 *
	 * @return {firstColumn, firstRow, columns, rows}
	 */
	static int[] tiles (float[] box, int width, int height) {
		int column = (int) Math.floor (box[0] / width), row = (int) Math.floor (box[1] / height);
		int columns = Math.max (0, (int) Math.ceil (box[2] / width) - column);
		int rows = Math.max (0, (int) Math.ceil (box[3] / height) - row);
		return new int[] {column, row, columns, rows};
	}

	/** Affine product {@code a × b} in SWT/canvas element order (m11, m12, m21, m22, dx, dy): b is applied first. */
	public static float[] multiply (float[] a, float[] b) {
		return new float[] {
			a[0] * b[0] + a[2] * b[1],
			a[1] * b[0] + a[3] * b[1],
			a[0] * b[2] + a[2] * b[3],
			a[1] * b[2] + a[3] * b[3],
			a[0] * b[4] + a[2] * b[5] + a[4],
			a[1] * b[4] + a[3] * b[5] + a[5]};
	}
}
