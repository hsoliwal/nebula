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
package org.eclipse.nebula.visualization.internal.xygraph.decimation;

/**
 * Immutable key of one decimated view: the data snapshot, both pixel mappings and both axis ranges
 * ({@code getRange()}, which the trace uses for clipping). Two equal keys produce the same polyline.
 */
public final class TraceView {

	public final PackedColumns columns;
	public final AxisMapping x;
	public final AxisMapping y;
	public final double xLower;
	public final double xUpper;
	public final double yLower;
	public final double yUpper;
	/** minimum number of samples in the visible index range for the decimated path */
	public final int minSamples;

	public TraceView(PackedColumns columns, AxisMapping x, AxisMapping y, double xLower, double xUpper, double yLower,
			double yUpper, int minSamples) {
		this.columns = columns;
		this.x = x;
		this.y = y;
		this.xLower = xLower;
		this.xUpper = xUpper;
		this.yLower = yLower;
		this.yUpper = yUpper;
		this.minSamples = minSamples;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TraceView)) {
			return false;
		}
		TraceView v = (TraceView) o;
		return columns == v.columns && x.equals(v.x) && y.equals(v.y) && Double.compare(xLower, v.xLower) == 0
				&& Double.compare(xUpper, v.xUpper) == 0 && Double.compare(yLower, v.yLower) == 0
				&& Double.compare(yUpper, v.yUpper) == 0 && minSamples == v.minSamples;
	}

	@Override
	public int hashCode() {
		int h = System.identityHashCode(columns);
		h = h * 31 + x.hashCode();
		h = h * 31 + y.hashCode();
		h = h * 31 + Double.hashCode(xLower);
		h = h * 31 + Double.hashCode(xUpper);
		h = h * 31 + Double.hashCode(yLower);
		return h * 31 + Double.hashCode(yUpper);
	}
}
