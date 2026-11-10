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
package org.eclipse.nebula.widgets.calendarcombo;

import org.eclipse.nebula.widgets.calendarcombo.internal.HideWatch;
import org.eclipse.nebula.widgets.calendarcombo.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Viewport of a {@link CalendarComposite}, aspect H (hidden gating, docs/m3/every-widget). While the mouse is held on
 * a month arrow, the stock calendar starts a Thread that sleeps 300 ms (120 ms after ten steps) and then flips the
 * month through {@code Display.syncExec}, for as long as the button is held: also while the calendar is hidden, and a
 * repeat paused by moving off the arrows keeps its thread sleeping after the calendar is disposed, because only a
 * flip notices the disposal.
 * <p>
 * Here no thread is started: a Display timer runs the stock thread's loop body ({@link CalendarComposite#m3ArrowStep()})
 * on the Display thread with the stock delays, and stops when the button is released ({@code killArrowThread}) or the
 * calendar is disposed. A step due while the calendar is hidden (its own or an ancestor's visibility, a zero size, a
 * minimized shell, or {@code SWT.PartHidden} on the M3 SWT fork) parks the repeat instead: no flip and no timer. The
 * next Show, Resize, Paint or {@code SWT.PartVisible} after the reveal, or the release of the button if it comes
 * first, catches up once: it takes, at once, the parked step and every step the stock thread would have taken in the
 * hidden time (same delays, same speed switch), so the calendar shows the month the stock one would, and the repeat
 * goes on with the stock phase.
 * </p>
 * <p>
 * {@code -Dswt.widget.mode.CalendarComposite=original} (or the global {@code swt.widget.mode=original}) keeps the
 * stock thread.
 * </p>
 */
final class CalendarCompositeViewport {

	/** True unless the startup mode of CalendarComposite is original. */
	static final boolean ENABLED = WidgetMode.of("CalendarComposite", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private final CalendarComposite composite;
	private final Display display;
	private final Runnable ticker = this::tick;
	private final Listener listener = this::handleEvent;
	private boolean armed;
	private boolean parked;
	private long parkedAt;

	/** Arrow-repeat steps taken by the timer while the calendar was shown (tests and benches). */
	int steps;
	/** Timer firings that found the calendar hidden and parked the repeat (tests and benches). */
	int parks;
	/** Steps taken at once by a catch-up after a reveal or a release (tests and benches). */
	int caughtUp;

	CalendarCompositeViewport(final CalendarComposite composite) {
		this.composite = composite;
		display = composite.getDisplay();
		composite.addListener(SWT.Show, listener);
		composite.addListener(SWT.Resize, listener);
		// an ancestor shown again: SWT sends no Show to the child, but the first paint after the reveal arrives
		composite.addListener(SWT.Paint, listener);
		if (HideWatch.PART_VISIBLE >= 0) {
			composite.addListener(HideWatch.PART_VISIBLE, listener);
		}
		composite.addListener(SWT.Dispose, listener);
	}

	/** {@code runArrowThread} set up the stock state of a repeat: the timer takes the place of its thread. */
	void arrowStarted() {
		parked = false;
		arm(composite.m3ArrowDelay());
	}

	/** {@code killArrowThread} is about to end the repeat: a parked repeat first takes the steps of its hidden time. */
	void arrowStopping() {
		if (parked) {
			parked = false;
			catchUp();
		}
		disarm();
	}

	/** @return true while a step is due on the timer */
	boolean isArmed() {
		return armed;
	}

	/** @return true while the repeat is parked because the calendar is hidden */
	boolean isParked() {
		return parked;
	}

	private void tick() {
		armed = false;
		if (composite.isDisposed() || composite.m3ArrowDelay() < 0) {
			return; // released or disposed: the stock thread would leave its loop
		}
		if (!HideWatch.shown(composite)) {
			parked = true;
			parkedAt = now();
			parks++;
			return;
		}
		steps++;
		arm(composite.m3ArrowStep());
	}

	private void handleEvent(final Event event) {
		if (event.type == SWT.Dispose) {
			disarm();
			parked = false;
			return;
		}
		if (parked && !composite.isDisposed() && HideWatch.shown(composite)) {
			parked = false;
			arm(catchUp());
		}
	}

	/*
	 * The parked step and every step the stock thread would have taken since, at once (the stock loop sleeps the delay
	 * each step returns); returns the time left to the next step, or -1 when the repeat ended.
	 */
	private int catchUp() {
		long elapsed = now() - parkedAt;
		int next = composite.m3ArrowStep();
		caughtUp++;
		while (next >= 0 && elapsed >= next) {
			elapsed -= next;
			next = composite.m3ArrowStep();
			caughtUp++;
		}
		return next < 0 ? -1 : (int) (next - elapsed);
	}

	private void arm(final int delay) {
		if (delay < 0 || armed || composite.isDisposed()) {
			return;
		}
		armed = true;
		display.timerExec(delay, ticker);
	}

	private void disarm() {
		if (armed) {
			armed = false;
			if (!display.isDisposed()) {
				display.timerExec(-1, ticker);
			}
		}
	}

	private static long now() {
		return System.nanoTime() / 1_000_000L;
	}
}
