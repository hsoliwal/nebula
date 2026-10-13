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
 * Immutable copy of the parameters that {@code LinearScale.getValuePosition(value, false)} reads, and the same
 * arithmetic in the same order, so a worker thread maps values to pixels exactly as the axis would on the UI thread.
 * <p>
 * Shared with the GWT/RWT client renderer: int and double state only; the only long is the return value of
 * {@link Math#round(double)}, cast at once, exactly as the receiver does.
 */
public final class AxisMapping {

	final boolean log;
	final boolean horizontal;
	final double rangeMin;
	final double rangeMax;
	final int length;
	final int margin;
	final int boundsX;
	final int boundsY;
	/** linear: magnitude, min/f, (max/f - min/f); log: log10(min), log10(max) - log10(min) */
	private final double f;
	private final double minOverF;
	private final double t;
	private final double logMin;
	private final double logSpan;
	private final double l;

	/**
	 * @param log         {@code isLogScaleEnabled()}
	 * @param horizontal  {@code isHorizontal()}
	 * @param rangeMin    {@code getLocalRange().getLower()}
	 * @param rangeMax    {@code getLocalRange().getUpper()}
	 * @param length      scale length (tick length + 2 * margin)
	 * @param margin      {@code getMargin(false)} after the tick update
	 * @param boundsX     {@code getBounds().x}
	 * @param boundsY     {@code getBounds().y}
	 */
	public AxisMapping(boolean log, boolean horizontal, double rangeMin, double rangeMax, int length, int margin,
			int boundsX, int boundsY) {
		this.log = log;
		this.horizontal = horizontal;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		this.length = length;
		this.margin = margin;
		this.boundsX = boundsX;
		this.boundsY = boundsY;
		this.l = length - 2 * margin;
		double fm = Math.max(Math.abs(rangeMin), Math.abs(rangeMax));
		double maxF = rangeMax / fm;
		double minF = rangeMin / fm;
		this.f = fm;
		this.minOverF = minF;
		this.t = maxF - minF;
		this.logMin = Math.log10(rangeMin);
		this.logSpan = Math.log10(rangeMax) - logMin;
	}

	/** Same as {@code LinearScale.getValuePosition(value, false)} for the captured state. */
	public int position(double value) {
		double pixelsToStart;
		if (log) {
			pixelsToStart = value <= 0 ? margin : ((Math.log10(value) - logMin) / logSpan * l) + margin;
		} else {
			pixelsToStart = ((value / f - minOverF) / t * l) + margin;
		}
		double abs = horizontal ? pixelsToStart + boundsX : length - pixelsToStart + boundsY;
		return (int) Math.round(abs);
	}

	/** True if this mapping was captured from exactly these parameters (lets the UI reuse it without allocating). */
	public boolean matches(boolean log, boolean horizontal, double rangeMin, double rangeMax, int length, int margin,
			int boundsX, int boundsY) {
		return this.log == log && this.horizontal == horizontal && Double.compare(this.rangeMin, rangeMin) == 0
				&& Double.compare(this.rangeMax, rangeMax) == 0 && this.length == length && this.margin == margin
				&& this.boundsX == boundsX && this.boundsY == boundsY;
	}

	/** Pixel positions only move one way with the value (needed for block min/max tests). */
	public boolean isMonotone() {
		return l > 0 && (log ? rangeMin > 0 && rangeMax > 0 && logSpan != 0 : t != 0 && f > 0)
				&& !Double.isNaN(t) && !Double.isInfinite(t);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AxisMapping)) {
			return false;
		}
		AxisMapping m = (AxisMapping) o;
		return log == m.log && horizontal == m.horizontal && Double.compare(rangeMin, m.rangeMin) == 0
				&& Double.compare(rangeMax, m.rangeMax) == 0 && length == m.length && margin == m.margin
				&& boundsX == m.boundsX && boundsY == m.boundsY;
	}

	@Override
	public int hashCode() {
		int h = (log ? 1 : 0) | (horizontal ? 2 : 0);
		h = h * 31 + Double.hashCode(rangeMin);
		h = h * 31 + Double.hashCode(rangeMax);
		h = h * 31 + length;
		h = h * 31 + margin;
		h = h * 31 + boundsX;
		return h * 31 + boundsY;
	}
}
