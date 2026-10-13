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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Per-trace front end of the decimation workers: one task at a time per trace, a small cache of published views,
 * and the pyramid of the latest data snapshot (extended in place for appends).
 * <p>
 * The UI thread calls {@link #request(TraceView, int, Runnable)} from paint only, so nothing is computed for a trace
 * that is not painted (hidden widget, hidden figure, collapsed parent). A result that arrives after the UI stopped
 * waiting triggers {@code onLate} once (the trace schedules one repaint).
 */
public final class TraceDecimator {

	/** Visible samples below which the per-sample loop is cheaper than a worker round trip. */
	public static final int DEFAULT_MIN_SAMPLES = Integer.getInteger(
			"org.eclipse.nebula.visualization.xygraph.decimation.minSamples", 4096).intValue(); //$NON-NLS-1$
	/** Longest time the UI thread waits for a view before drawing the previous one (ms). */
	public static final int DEFAULT_WAIT_MS = Integer.getInteger(
			"org.eclipse.nebula.visualization.xygraph.decimation.waitMillis", 250).intValue(); //$NON-NLS-1$
	/** {@code false} disables the decimated path for every trace (diagnostics and A/B runs). */
	public static final boolean ENABLED = !"off".equals(System.getProperty( //$NON-NLS-1$
			"org.eclipse.nebula.visualization.xygraph.decimation")); //$NON-NLS-1$

	private static final int CACHE = 4;
	private static volatile ExecutorService pool;

	private final DecimatedTrace[] cache = new DecimatedTrace[CACHE];
	private int cacheNext;
	private final PolylineBuilder builder = new PolylineBuilder();
	private final double[] tmp = new double[4];
	private PackedMinMaxPyramid pyramid;
	private PackedColumns pyramidColumns;
	private TraceView pendingView;
	private DecimatedTrace pendingResult;
	private boolean pendingLate;
	private Runnable pendingLateAction;
	private long pyramidNanos;
	private long decimateNanos;

	private static ExecutorService pool() {
		ExecutorService p = pool;
		if (p == null) {
			synchronized (TraceDecimator.class) {
				p = pool;
				if (p == null) {
					int n = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
					ThreadFactory f = r -> {
						Thread t = new Thread(r, "nebula-xygraph-decimation"); //$NON-NLS-1$
						t.setDaemon(true);
						t.setPriority(Thread.NORM_PRIORITY - 1);
						return t;
					};
					p = Executors.newFixedThreadPool(n, f);
					pool = p;
				}
			}
		}
		return p;
	}

	/**
	 * Returns the published result for {@code view}, computing it on a worker and waiting up to {@code waitMillis}.
	 * Returns null if the worker did not finish in time; {@code onLate} then runs on the worker thread once the
	 * result is published.
	 */
	public DecimatedTrace request(TraceView view, int waitMillis, Runnable onLate) {
		synchronized (this) {
			DecimatedTrace hit = lookup(view);
			if (hit != null) {
				return hit;
			}
			if (!view.equals(pendingView)) {
				pendingView = view;
				pendingResult = null;
				pendingLate = false;
				pool().execute(() -> compute(view));
			}
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMillis);
			while (pendingResult == null && view.equals(pendingView)) {
				long left = deadline - System.nanoTime();
				if (left <= 0) {
					pendingLate = true;
					pendingLateAction = onLate;
					return null;
				}
				try {
					TimeUnit.NANOSECONDS.timedWait(this, left);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
			return lookup(view);
		}
	}

	private DecimatedTrace lookup(TraceView view) {
		for (DecimatedTrace d : cache) {
			if (d != null && d.view.equals(view)) {
				return d;
			}
		}
		return null;
	}

	/** Worker side; one task per trace at a time (the builder, pyramid and scratch belong to this decimator). */
	private void compute(TraceView view) {
		synchronized (this) {
			if (!view.equals(pendingView)) {
				return; // superseded by a newer view before it started
			}
		}
		DecimatedTrace result;
		synchronized (builder) {
			long t0 = System.nanoTime();
			PackedMinMaxPyramid p = pyramidFor(view.columns);
			long t1 = System.nanoTime();
			result = TraceDecimation.decimate(view, p, builder, tmp);
			long t2 = System.nanoTime();
			pyramidNanos = t1 - t0;
			decimateNanos = t2 - t1;
		}
		Runnable late = null;
		synchronized (this) {
			cache[cacheNext] = result;
			cacheNext = (cacheNext + 1) % CACHE;
			if (view.equals(pendingView)) {
				pendingResult = result;
				if (pendingLate) {
					late = pendingLateAction;
				}
				pendingView = null;
				pendingLate = false;
				pendingLateAction = null;
			}
			notifyAll();
		}
		if (late != null) {
			late.run();
		}
	}

	private PackedMinMaxPyramid pyramidFor(PackedColumns c) {
		PackedMinMaxPyramid p = pyramid;
		PackedColumns pc = pyramidColumns;
		if (p != null && pc == c) {
			return p;
		}
		if (p != null && pc != null && pc.generation == c.generation && c.size >= pc.size) {
			p = p.extend(c.y, c.size);
		} else {
			p = PackedMinMaxPyramid.build(c.y, c.size);
		}
		pyramid = p;
		pyramidColumns = c;
		return p;
	}

	/** Drops cached views and the pyramid (data replaced or trace disposed). */
	public synchronized void reset() {
		for (int i = 0; i < CACHE; i++) {
			cache[i] = null;
		}
		synchronized (builder) {
			pyramid = null;
			pyramidColumns = null;
		}
	}

	/** Nanoseconds of the last pyramid build/extension and the last decimation (diagnostics). */
	public long[] lastTimings() {
		synchronized (builder) {
			return new long[] { pyramidNanos, decimateNanos };
		}
	}
}
