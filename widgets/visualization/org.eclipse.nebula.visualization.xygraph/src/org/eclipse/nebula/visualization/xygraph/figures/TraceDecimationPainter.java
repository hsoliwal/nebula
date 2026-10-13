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
package org.eclipse.nebula.visualization.xygraph.figures;

import java.util.List;

import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.nebula.visualization.internal.xygraph.decimation.AxisMapping;
import org.eclipse.nebula.visualization.internal.xygraph.decimation.DecimatedTrace;
import org.eclipse.nebula.visualization.internal.xygraph.decimation.PackedColumns;
import org.eclipse.nebula.visualization.internal.xygraph.decimation.TraceDecimator;
import org.eclipse.nebula.visualization.internal.xygraph.decimation.TraceView;
import org.eclipse.nebula.visualization.xygraph.dataprovider.IDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.ISample;
import org.eclipse.nebula.visualization.xygraph.dataprovider.PackedXYDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.Sample;
import org.eclipse.nebula.visualization.xygraph.linearscale.AbstractScale;
import org.eclipse.nebula.visualization.xygraph.linearscale.LinearScale;
import org.eclipse.nebula.visualization.xygraph.linearscale.Range;
import org.eclipse.swt.widgets.Display;

/**
 * UI-thread side of the decimated trace paint: captures the view from the axes, asks the trace's
 * {@link TraceDecimator} for the published polyline and keeps the hot-sample range for
 * {@link Trace#getHotSampleList()}. Only {@link PackedXYDataProvider} data is decimated; every other provider, and
 * every view the decimator declares ineligible, is painted by the trace's per-sample loop.
 */
final class TraceDecimationPainter {

	/** Returned while the first view of new data is still being computed: draw nothing for this frame. */
	static final PointList NOTHING = new PointList(0);

	private static final ClassValue<Boolean> PLAIN_MAPPING = new ClassValue<Boolean>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			return Boolean.valueOf(plain(type));
		}
	};

	private final Trace trace;
	private TraceDecimator decimator;
	private TraceView lastView;
	private DecimatedTrace shown;
	private DecimatedTrace hot;
	private IDataProvider provider;
	private int generation = -1;
	private Runnable late;

	TraceDecimationPainter(Trace trace) {
		this.trace = trace;
	}

	/**
	 * @return the polyline to draw, {@link #NOTHING}, or null when the per-sample loop must paint this frame
	 */
	PointList polyline(IDataProvider dataProvider, Axis xAxis, Axis yAxis) {
		hot = null;
		if (!TraceDecimator.ENABLED || !(dataProvider instanceof PackedXYDataProvider)) {
			return null;
		}
		PackedXYDataProvider p = (PackedXYDataProvider) dataProvider;
		PackedColumns cols = p.snapshot();
		if (!p.isChronological() || cols.size < TraceDecimator.DEFAULT_MIN_SAMPLES) {
			return null;
		}
		if (dataProvider != provider || cols.generation != generation) {
			reset();
			provider = dataProvider;
			generation = cols.generation;
		}
		TraceView view = capture(cols, xAxis, yAxis);
		if (view == null) {
			return null;
		}
		if (decimator == null) {
			decimator = new TraceDecimator();
		}
		DecimatedTrace d = decimator.request(view, TraceDecimator.DEFAULT_WAIT_MS, lateRepaint());
		if (d == null) {
			d = shown;
			if (d == null) {
				return NOTHING;
			}
		}
		if (d.status != DecimatedTrace.DECIMATED) {
			return null;
		}
		shown = d;
		hot = d;
		PointList pl = (PointList) d.uiPolyline;
		if (pl == null) {
			pl = new PointList(d.points);
			d.uiPolyline = pl;
		}
		return pl;
	}

	/** Fills the (cleared) hot list from the last decimated paint, once, on demand. */
	void materializeHot(List<ISample> list) {
		DecimatedTrace d = hot;
		if (d == null) {
			return;
		}
		hot = null;
		PackedColumns c = d.view.columns;
		for (int i = d.hotFrom; i <= d.hotTo; i++) {
			list.add(new Sample(c.x[i], c.y[i]));
		}
	}

	void reset() {
		if (decimator != null) {
			decimator.reset();
		}
		shown = null;
		hot = null;
		lastView = null;
		provider = null;
		generation = -1;
	}

	/** Timings of the last worker task (pyramid ns, decimation ns), or null. */
	long[] lastTimings() {
		return decimator == null ? null : decimator.lastTimings();
	}

	private Runnable lateRepaint() {
		Runnable r = late;
		if (r == null) {
			Display display = Display.getCurrent();
			r = () -> {
				if (display != null && !display.isDisposed()) {
					display.asyncExec(trace::repaint);
				}
			};
			late = r;
		}
		return r;
	}

	private TraceView capture(PackedColumns cols, Axis xAxis, Axis yAxis) {
		TraceView prev = lastView;
		AxisMapping mx = mapping(xAxis, prev == null ? null : prev.x);
		AxisMapping my = mapping(yAxis, prev == null ? null : prev.y);
		if (mx == null || my == null) {
			return null;
		}
		Range xr = xAxis.getRange();
		Range yr = yAxis.getRange();
		if (prev != null && prev.columns == cols && prev.x == mx && prev.y == my
				&& Double.compare(prev.xLower, xr.getLower()) == 0 && Double.compare(prev.xUpper, xr.getUpper()) == 0
				&& Double.compare(prev.yLower, yr.getLower()) == 0 && Double.compare(prev.yUpper, yr.getUpper()) == 0) {
			return prev;
		}
		TraceView v = new TraceView(cols, mx, my, xr.getLower(), xr.getUpper(), yr.getLower(), yr.getUpper(),
				TraceDecimator.DEFAULT_MIN_SAMPLES);
		lastView = v;
		return v;
	}

	/**
	 * Captures the axis mapping; null if the axis class changes the mapping methods or the copy disagrees with the
	 * axis on the probe values (fail closed: the per-sample loop paints).
	 */
	private static AxisMapping mapping(Axis axis, AxisMapping prev) {
		if (!PLAIN_MAPPING.get(axis.getClass()).booleanValue()) {
			return null;
		}
		int probe = axis.getValuePosition(0, false);
		Range r = axis.getLocalRange();
		int margin = axis.getMargin(false);
		int length = axis.getTickLength() + 2 * margin;
		Rectangle b = axis.getBounds();
		boolean log = axis.isLogScaleEnabled();
		boolean horizontal = axis.isHorizontal();
		if (prev != null && prev.matches(log, horizontal, r.getLower(), r.getUpper(), length, margin, b.x, b.y)) {
			return prev;
		}
		AxisMapping m = new AxisMapping(log, horizontal, r.getLower(), r.getUpper(), length, margin, b.x, b.y);
		if (m.position(0) != probe || m.position(r.getLower()) != axis.getValuePosition(r.getLower(), false)
				|| m.position(r.getUpper()) != axis.getValuePosition(r.getUpper(), false)) {
			return null;
		}
		return m;
	}

	private static boolean plain(Class<?> c) {
		try {
			return declaredInScale(c, "getValuePosition", double.class, boolean.class) //$NON-NLS-1$
					&& declaredInScale(c, "getValuePrecisePosition", double.class, boolean.class) //$NON-NLS-1$
					&& declaredInScale(c, "getLocalRange") && declaredInScale(c, "getMargin", boolean.class) //$NON-NLS-1$ //$NON-NLS-2$
					&& declaredInScale(c, "getTickLength") && declaredInScale(c, "isLogScaleEnabled") //$NON-NLS-1$ //$NON-NLS-2$
					&& declaredInScale(c, "isHorizontal") && declaredInScale(c, "updateTick"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (NoSuchMethodException | SecurityException e) {
			return false;
		}
	}

	private static boolean declaredInScale(Class<?> c, String name, Class<?>... params) throws NoSuchMethodException {
		Class<?> d = c.getMethod(name, params).getDeclaringClass();
		return d == LinearScale.class || d == AbstractScale.class;
	}
}
