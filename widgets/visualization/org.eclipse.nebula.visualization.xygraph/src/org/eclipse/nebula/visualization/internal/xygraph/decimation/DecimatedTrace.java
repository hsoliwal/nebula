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
 * Immutable result of decimating one {@link TraceView}: either the exact vertex list of the trace's polyline plus the
 * index range of the in-range samples, or {@link #INELIGIBLE} when the view needs the per-sample paint loop.
 */
public final class DecimatedTrace {

	public static final int INELIGIBLE = 0;
	public static final int DECIMATED = 1;

	public final TraceView view;
	public final int status;
	/** {@code x0, y0, x1, y1, ...}; never modified after publication */
	public final int[] points;
	/** first and last index of the samples inside both axis ranges (the trace's hot samples) */
	public final int hotFrom;
	public final int hotTo;
	/** UI-thread cache of the toolkit polyline wrapper for {@link #points}; set once on the UI thread */
	public Object uiPolyline;

	DecimatedTrace(TraceView view, int status, int[] points, int hotFrom, int hotTo) {
		this.view = view;
		this.status = status;
		this.points = points;
		this.hotFrom = hotFrom;
		this.hotTo = hotTo;
	}

	static DecimatedTrace ineligible(TraceView view) {
		return new DecimatedTrace(view, INELIGIBLE, null, 0, -1);
	}
}
