/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Synexia contributors - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.visualization.xygraph.dataprovider;

import org.eclipse.nebula.visualization.internal.xygraph.decimation.PackedColumns;
import org.eclipse.nebula.visualization.xygraph.linearscale.Range;
import org.eclipse.swt.SWT;

/**
 * A chronological data provider that keeps its samples in two packed {@code double[]} columns (x and y) instead of
 * one {@link ISample} object per sample.
 * <p>
 * {@link #getSample(int)} creates a {@link Sample} only when it is called. Appends are amortized O(1);
 * {@link #getXDataMinMax(boolean)} and {@link #getYDataMinMax(boolean)} are O(1) from running extremes and return
 * the same ranges {@link AbstractDataProvider#getDataRange(boolean, boolean)} would compute. A {@code Trace} drawing
 * this provider as a line reduces the visible range per pixel column on worker threads (see the trace's
 * documentation); every other caller sees an ordinary {@link IDataProvider}.
 * <p>
 * The x values are expected to be non-decreasing. If an appended x is smaller than its predecessor or is NaN,
 * {@link #isChronological()} returns false from then on and traces fall back to drawing every sample.
 * <p>
 * Writers are serialized by this object's monitor; readers never block. Listeners are notified on the calling thread,
 * as {@link CircularBufferDataProvider} does.
 *
 * @since 3.2
 */
public class PackedXYDataProvider extends AbstractDataProvider {

	private volatile PackedColumns columns = PackedColumns.EMPTY;
	private double xMin = Double.POSITIVE_INFINITY;
	private double xMax = Double.NEGATIVE_INFINITY;
	private double xPosMin = Double.POSITIVE_INFINITY;
	private double yMin = Double.POSITIVE_INFINITY;
	private double yMax = Double.NEGATIVE_INFINITY;
	private double yPosMin = Double.POSITIVE_INFINITY;

	public PackedXYDataProvider() {
		super(true);
	}

	/**
	 * Replaces all samples with copies of {@code xValues[0 .. count)} and {@code yValues[0 .. count)} and notifies
	 * the listeners once.
	 *
	 * @param xValues
	 *            the x values
	 * @param yValues
	 *            the y values
	 * @param count
	 *            number of samples to copy
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if an array is null</li>
	 *    <li>ERROR_INVALID_RANGE - if count is negative or larger than an array</li>
	 * </ul>
	 */
	public void setData(double[] xValues, double[] yValues, int count) {
		synchronized (this) {
			checkCount(xValues, yValues, 0, count);
			double[] x = new double[Math.max(count, 16)];
			double[] y = new double[x.length];
			System.arraycopy(xValues, 0, x, 0, count);
			System.arraycopy(yValues, 0, y, 0, count);
			resetStats();
			boolean sorted = accumulate(x, y, 0, count, true);
			columns = new PackedColumns(x, y, count, sorted, columns.generation + 1);
		}
		fireDataChange();
	}

	/**
	 * Appends one sample and notifies the listeners.
	 *
	 * @param x
	 *            the x value
	 * @param y
	 *            the y value
	 */
	public void append(double x, double y) {
		synchronized (this) {
			PackedColumns c = ensureCapacity(1);
			c.x[c.size] = x;
			c.y[c.size] = y;
			publish(c, 1);
		}
		fireDataChange();
	}

	/**
	 * Appends {@code count} samples from {@code xValues[offset ..]} and {@code yValues[offset ..]} and notifies the
	 * listeners once.
	 *
	 * @param xValues
	 *            the x values
	 * @param yValues
	 *            the y values
	 * @param offset
	 *            index of the first sample in both arrays
	 * @param count
	 *            number of samples
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if an array is null</li>
	 *    <li>ERROR_INVALID_RANGE - if offset or count lie outside an array</li>
	 * </ul>
	 */
	public void append(double[] xValues, double[] yValues, int offset, int count) {
		synchronized (this) {
			checkCount(xValues, yValues, offset, count);
			PackedColumns c = ensureCapacity(count);
			System.arraycopy(xValues, offset, c.x, c.size, count);
			System.arraycopy(yValues, offset, c.y, c.size, count);
			publish(c, count);
		}
		fireDataChange();
	}

	/** Removes all samples and notifies the listeners. */
	public void clear() {
		synchronized (this) {
			resetStats();
			columns = new PackedColumns(new double[16], new double[16], 0, true, columns.generation + 1);
		}
		fireDataChange();
	}

	private static void checkCount(double[] x, double[] y, int offset, int count) {
		if (x == null || y == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (offset < 0 || count < 0 || offset > x.length - count || offset > y.length - count) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
	}

	private PackedColumns ensureCapacity(int extra) {
		PackedColumns c = columns;
		int need = c.size + extra;
		if (need <= c.x.length) {
			return c;
		}
		int cap = Math.max(need, Math.max(16, c.x.length + (c.x.length >> 1)));
		double[] x = new double[cap];
		double[] y = new double[cap];
		System.arraycopy(c.x, 0, x, 0, c.size);
		System.arraycopy(c.y, 0, y, 0, c.size);
		return new PackedColumns(x, y, c.size, c.sorted, c.generation);
	}

	private void publish(PackedColumns c, int added) {
		boolean sorted = accumulate(c.x, c.y, c.size, c.size + added, c.sorted);
		columns = new PackedColumns(c.x, c.y, c.size + added, sorted, c.generation);
	}

	private void resetStats() {
		xMin = Double.POSITIVE_INFINITY;
		xMax = Double.NEGATIVE_INFINITY;
		xPosMin = Double.POSITIVE_INFINITY;
		yMin = Double.POSITIVE_INFINITY;
		yMax = Double.NEGATIVE_INFINITY;
		yPosMin = Double.POSITIVE_INFINITY;
	}

	/** Folds {@code [from, to)} into the running extremes; returns whether x stays non-decreasing and NaN free. */
	private boolean accumulate(double[] x, double[] y, int from, int to, boolean sorted) {
		for (int i = from; i < to; i++) {
			double xv = x[i];
			double yv = y[i];
			if (sorted && (xv != xv || (i > 0 && xv < x[i - 1]))) {
				sorted = false;
			}
			xMin = xMin > xv ? xv : xMin;
			xMax = xMax < xv ? xv : xMax;
			xPosMin = xv > 0 && xPosMin > xv ? xv : xPosMin;
			yMin = yMin > yv ? yv : yMin;
			yMax = yMax < yv ? yv : yMax;
			yPosMin = yv > 0 && yPosMin > yv ? yv : yPosMin;
		}
		return sorted;
	}

	@Override
	public int getSize() {
		return columns.size;
	}

	/** @return a new {@link Sample} for the index, or null outside {@code [0, getSize())} */
	@Override
	public ISample getSample(int index) {
		PackedColumns c = columns;
		if (index < 0 || index >= c.size) {
			return null;
		}
		return new Sample(c.x[index], c.y[index]);
	}

	/**
	 * Returns the x value at the index without creating a sample object.
	 *
	 * @param index
	 *            the sample index
	 * @return the x value
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the size - 1 (inclusive)</li>
	 * </ul>
	 */
	public double getXValue(int index) {
		PackedColumns c = columns;
		if (index < 0 || index >= c.size) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
		return c.x[index];
	}

	/**
	 * Returns the y value at the index without creating a sample object.
	 *
	 * @param index
	 *            the sample index
	 * @return the y value
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the size - 1 (inclusive)</li>
	 * </ul>
	 */
	public double getYValue(int index) {
		PackedColumns c = columns;
		if (index < 0 || index >= c.size) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
		return c.y[index];
	}

	@Override
	public boolean isChronological() {
		return super.isChronological() && columns.sorted;
	}

	@Override
	public synchronized Range getXDataMinMax(boolean positiveOnly) {
		if (getSize() <= 0) {
			return null;
		}
		xDataMinMax = range(positiveOnly, xMin, xMax, xPosMin);
		return xDataMinMax;
	}

	@Override
	public synchronized Range getYDataMinMax(boolean positiveOnly) {
		if (getSize() <= 0) {
			return null;
		}
		yDataMinMax = range(positiveOnly, yMin, yMax, yPosMin);
		return yDataMinMax;
	}

	/** Same result as {@link AbstractDataProvider#getDataRange(boolean, boolean)} for samples without errors. */
	private static Range range(boolean positiveOnly, double min, double max, double posMin) {
		if (!positiveOnly) {
			return new Range(min, max);
		}
		double hi = max < 0 ? 0 : max;
		return new Range(posMin, hi < posMin ? posMin : hi);
	}

	@Override
	public Range getDataRange(boolean positiveOnly, boolean isXAxis, int lowerBound) {
		if (lowerBound != 0 || getSize() <= 0) {
			return super.getDataRange(positiveOnly, isXAxis, lowerBound);
		}
		synchronized (this) {
			return isXAxis ? range(positiveOnly, xMin, xMax, xPosMin) : range(positiveOnly, yMin, yMax, yPosMin);
		}
	}

	/**
	 * The current immutable column snapshot. Renderers read it without locking.
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public PackedColumns snapshot() {
		return columns;
	}
}
