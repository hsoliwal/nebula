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
package org.eclipse.nebula.widgets.timeline;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.NativeGraphicsSource;
import org.eclipse.draw2d.UpdateManager;
import org.eclipse.nebula.widgets.timeline.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Viewport of a {@link TimelineComposite}, aspect H (hidden gating, docs/m3/every-widget). The stock timeline keeps
 * working while nobody can see it: every model change schedules the {@link TimelineDataBinding} refresh job (300 ms),
 * which runs on a worker thread and syncExecs viewer refresh and update into the UI thread, rebuilding figures; every
 * figure change then posts a Draw2D update request that lays out the figure tree and repaints the damage of the hidden
 * canvas (the LightweightSystem's root figure reports itself showing, so DeferredUpdateManager filters nothing), and
 * every canvas resize lays the tree out at once. Here, while the timeline is hidden (its own or an ancestor's
 * visibility, a zero size, a minimized shell, or SWT.PartHidden on the M3 SWT fork):
 * <ul>
 * <li>the binding job is neither scheduled nor run ({@link #admit(TimelineComposite, Job)} answers its
 * {@code shouldSchedule} and {@code shouldRun}); the model changes stay queued in the binding as stock queues them;</li>
 * <li>the Draw2D update cycle is parked ({@link TimelineUpdateManager}): no update request, no layout, no paint.</li>
 * </ul>
 * The reveal (the first Paint, Resize, Deiconify or SWT.PartVisible that finds the timeline shown) resumes both
 * once, Draw2D first (its layout sets the time axis width, TracksLayer.layout, that the refresh reads), then the
 * binding job, so the timeline shows what a timeline that stayed shown shows. Direct viewer calls ({@code refresh},
 * {@code setSelection}) keep their synchronous figure work; only the layout and painting that follow them wait.
 * <p>
 * {@code -Dswt.widget.mode.TimelineComposite=original} (or the global {@code swt.widget.mode=original}) keeps the
 * stock behaviour.
 * </p>
 */
final class TimelineViewport implements Listener {

	/** True unless the startup mode of TimelineComposite is original. */
	static final boolean ENABLED = WidgetMode.of("TimelineComposite", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	/* SWT.PartVisible, SWT.PartHidden and Control.isPartVisible of the M3 SWT fork (brief rule 25); -1 and null on stock SWT */
	private static final int PART_VISIBLE = partEvent("PartVisible"); //$NON-NLS-1$
	private static final int PART_HIDDEN = partEvent("PartHidden"); //$NON-NLS-1$
	private static final Method IS_PART_VISIBLE = partVisibleMethod();
	private static final Object[] NO_ARGUMENTS = {};
	private static final Control[] NONE = {};
	private static final int[] CONTROL_EVENTS = { SWT.Hide, SWT.Show };
	private static final int[] SHELL_EVENTS = { SWT.Hide, SWT.Show, SWT.Iconify, SWT.Deiconify };

	private final TimelineComposite control;
	private final Canvas canvas;
	private TimelineUpdateManager manager;
	/* stock SWT only: the timeline and its ancestors up to the shell, watched for Hide and Show */
	private Control[] watched = NONE;

	/* written on the UI thread by the events below; read on any thread by the binding job gate */
	private volatile boolean hidden;
	private volatile boolean disposed;

	/* binding jobs refused while hidden, scheduled once by the reveal; two buffers, so the reveal schedules outside the lock */
	private Job[] parkedJobs = new Job[1];
	private Job[] revealJobs = new Job[1];
	private int parkedJobCount;

	/** Binding job requests refused while hidden (tests and benches). */
	volatile int jobsParked;
	/** Binding jobs scheduled by a reveal (tests and benches). */
	int jobsResumed;
	/** Hidden-to-shown transitions (tests and benches). */
	int reveals;

	TimelineViewport(final TimelineComposite control, final Canvas canvas) {
		this.control = control;
		this.canvas = canvas;
		// added before the LightweightSystem's own listeners: the gate is current when Draw2D handles the same event
		canvas.addListener(SWT.Paint, this);
		canvas.addListener(SWT.Resize, this);
		control.addListener(SWT.Dispose, this);
		if (PART_VISIBLE >= 0 && PART_HIDDEN >= 0) {
			canvas.addListener(PART_VISIBLE, this);
			canvas.addListener(PART_HIDDEN, this);
		} else {
			watch();
		}
		hidden = !shown();
	}

	/**
	 * Gives the LightweightSystem of the canvas the gated update manager, with the graphics source the LightweightSystem
	 * gives a double-buffered canvas (TimelineComposite creates its canvas SWT.DOUBLE_BUFFERED; a canvas without it would
	 * need Draw2D's package-private BufferedGraphicsSource, so it keeps the stock manager). The stock manager is
	 * disposed: the one request it queued (LightweightSystem.setControl revalidates the root) then finds nothing to do.
	 */
	void install(final LightweightSystem system) {
		if ((canvas.getStyle() & SWT.DOUBLE_BUFFERED) == 0) {
			return;
		}
		final UpdateManager stock = system.getUpdateManager();
		manager = new TimelineUpdateManager(this, canvas);
		manager.setGraphicsSource(new NativeGraphicsSource(canvas));
		system.setUpdateManager(manager);
		stock.dispose();
	}

	/** {@link #admitJob(Job)} of the timeline's viewport; true in the original mode. */
	static boolean admit(final TimelineComposite timeline, final Job job) {
		final TimelineViewport viewport = timeline.viewport;
		return viewport == null || viewport.admitJob(job);
	}

	/**
	 * The binding job gate, any thread ({@code ViewerRefreshJob.shouldSchedule} and {@code shouldRun}).
	 *
	 * @return false while the timeline is hidden (the job is kept and scheduled once by the reveal) or disposed
	 */
	boolean admitJob(final Job job) {
		if (disposed) {
			return false;
		}
		if (!hidden) {
			return true;
		}
		synchronized (this) {
			if (!hidden) {
				return true;
			}
			jobsParked++;
			if (!isParked(job)) {
				park(job);
			}
			return false;
		}
	}

	/** @return true while the timeline is hidden (UI thread for Draw2D, any thread for the binding job gate) */
	boolean isHidden() {
		return hidden;
	}

	/** @return the gated update manager, null before {@link #install(LightweightSystem)} */
	TimelineUpdateManager manager() {
		return manager;
	}

	@Override
	public void handleEvent(final Event event) {
		if (event.type == SWT.Dispose) {
			release();
		} else if (event.type != SWT.Paint || hidden) {
			// a shown timeline pays one field read per paint
			refresh(event.type == SWT.Paint ? event : null);
		}
	}

	/* UI thread: the gate after an event; the first event that finds the timeline shown again resumes the parked work */
	private void refresh(final Event paint) {
		if (disposed || canvas.isDisposed()) {
			return;
		}
		if (watched != NONE && moved()) {
			watch(); // setParent moved the timeline under other ancestors
		}
		if (!shown()) {
			hidden = true;
		} else if (hidden) {
			reveal(paint);
		}
	}

	private void reveal(final Event paint) {
		final Job[] jobs;
		final int count;
		synchronized (this) {
			hidden = false;
			jobs = parkedJobs;
			count = parkedJobCount;
			parkedJobs = revealJobs;
			revealJobs = jobs;
			parkedJobCount = 0;
		}
		reveals++;
		if (manager != null) {
			manager.resume(paint != null && covers(paint)); // Draw2D first: its layout sets the time axis width
		}
		for (int i = 0; i < count; i++) {
			final Job job = jobs[i];
			jobs[i] = null;
			jobsResumed++;
			job.schedule(); // catch up once: the stock refresh of everything the model changed while hidden
		}
	}

	/* a Paint event of the whole canvas repaints every figure: what was damaged while hidden needs no extra redraw */
	private boolean covers(final Event paint) {
		final Rectangle area = canvas.getClientArea();
		return paint.x <= area.x && paint.y <= area.y && paint.x + paint.width >= area.x + area.width
				&& paint.y + paint.height >= area.y + area.height;
	}

	private boolean shown() {
		if (!canvas.isVisible()) {
			return false; // the timeline or an ancestor is hidden (setVisible, tab and stack pages, a hidden shell)
		}
		final Point size = canvas.getSize();
		if (size.x <= 0 || size.y <= 0 || canvas.getShell().getMinimized()) {
			return false;
		}
		if (IS_PART_VISIBLE != null) {
			try {
				return (Boolean) IS_PART_VISIBLE.invoke(canvas, NO_ARGUMENTS);
			} catch (final ReflectiveOperationException | RuntimeException e) {
				return true;
			}
		}
		return true;
	}

	private boolean isParked(final Job job) {
		for (int i = 0; i < parkedJobCount; i++) {
			if (parkedJobs[i] == job) {
				return true;
			}
		}
		return false;
	}

	private void park(final Job job) {
		if (parkedJobCount == parkedJobs.length) {
			parkedJobs = Arrays.copyOf(parkedJobs, parkedJobCount * 2);
			revealJobs = new Job[parkedJobs.length];
		}
		parkedJobs[parkedJobCount++] = job;
	}

	/* stock SWT sends Hide and Show only to the control whose visibility changes: watch the timeline and its ancestors */
	private void watch() {
		unwatch();
		int count = 0;
		for (Control c = control; c != null; c = c instanceof Shell ? null : c.getParent()) {
			count++;
		}
		final Control[] chain = new Control[count];
		int i = 0;
		for (Control c = control; c != null; c = c instanceof Shell ? null : c.getParent()) {
			chain[i++] = c;
			listen(c, true);
		}
		watched = chain;
	}

	private void unwatch() {
		for (final Control c : watched) {
			if (!c.isDisposed()) {
				listen(c, false);
			}
		}
		watched = NONE;
	}

	private void listen(final Control c, final boolean add) {
		final int[] types = c instanceof Shell ? SHELL_EVENTS : CONTROL_EVENTS;
		for (final int type : types) {
			if (add) {
				c.addListener(type, this);
			} else {
				c.removeListener(type, this);
			}
		}
	}

	private boolean moved() {
		int i = 0;
		for (Control c = control; c != null; c = c instanceof Shell ? null : c.getParent()) {
			if (i == watched.length || watched[i] != c) {
				return true;
			}
			i++;
		}
		return i != watched.length;
	}

	/* the timeline is disposed: the ancestors lose this listener, parked jobs are dropped, later binding jobs refused */
	private void release() {
		disposed = true;
		unwatch();
		synchronized (this) {
			Arrays.fill(parkedJobs, 0, parkedJobCount, null);
			parkedJobCount = 0;
		}
	}

	private static int partEvent(final String name) {
		try {
			return SWT.class.getField(name).getInt(null);
		} catch (final ReflectiveOperationException | RuntimeException e) {
			return -1;
		}
	}

	private static Method partVisibleMethod() {
		try {
			return Control.class.getMethod("isPartVisible"); //$NON-NLS-1$
		} catch (final ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
