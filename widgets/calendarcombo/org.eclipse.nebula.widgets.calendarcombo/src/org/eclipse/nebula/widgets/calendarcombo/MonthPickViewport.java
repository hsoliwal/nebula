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
 * Viewport of a {@link MonthPick}, aspect H (hidden gating, docs/m3/every-widget). While the mouse is held above or
 * below the month list, the stock picker starts a Thread per direction that sleeps 200 ms and then posts a one-month
 * scroll through {@code Display.asyncExec}, for as long as its flag is set: also while the picker is hidden, and after
 * the picker is disposed (released outside the list, the usual end of a scroll), when the thread keeps posting a
 * runnable every 200 ms for the life of the application, because nothing clears its flag.
 * <p>
 * Here no thread is started: a Display timer per direction runs the stock thread's loop on the Display thread (a
 * scroll after each 200 ms, again while the flag is set), and both stop when the picker is disposed. A scroll due
 * while the picker is hidden (its own or an ancestor's visibility, a zero size, a minimized shell, or
 * {@code SWT.PartHidden} on the M3 SWT fork) parks the direction instead. The next Show, Resize, Paint or
 * {@code SWT.PartVisible} after the reveal catches up once: the parked scroll and, while the flag is still set, every
 * scroll of the hidden time, at once; the timer then goes on with the stock phase.
 * </p>
 * <p>
 * {@code -Dswt.widget.mode.MonthPick=original} (or the global {@code swt.widget.mode=original}) keeps the stock
 * threads.
 * </p>
 */
final class MonthPickViewport {

	/** True unless the startup mode of MonthPick is original. */
	static final boolean ENABLED = WidgetMode.of("MonthPick", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	/* direction bits of armed and parked */
	private static final int ABOVE = 1;
	private static final int BELOW = 2;

	private final MonthPick pick;
	private final Display display;
	private final Runnable aboveTicker = () -> tick(ABOVE);
	private final Runnable belowTicker = () -> tick(BELOW);
	private final Listener listener = this::handleEvent;
	private int armed;
	private int parked;
	private long aboveParkedAt;
	private long belowParkedAt;

	/** Scrolls taken by the timers while the picker was shown (tests and benches). */
	int steps;
	/** Timer firings that found the picker hidden and parked a direction (tests and benches). */
	int parks;
	/** Scrolls taken at once by a catch-up after a reveal (tests and benches). */
	int caughtUp;

	MonthPickViewport(final MonthPick pick) {
		this.pick = pick;
		display = pick.getDisplay();
		pick.addListener(SWT.Show, listener);
		pick.addListener(SWT.Resize, listener);
		pick.addListener(SWT.Paint, listener);
		if (HideWatch.PART_VISIBLE >= 0) {
			pick.addListener(HideWatch.PART_VISIBLE, listener);
		}
		pick.addListener(SWT.Dispose, listener);
	}

	/**
	 * @param up the direction ({@code runAbove} or {@code runBelow})
	 * @return true while the direction's timer is due or parked: the stock thread of that direction would be alive
	 */
	boolean scrolling(final boolean up) {
		return ((armed | parked) & (up ? ABOVE : BELOW)) != 0;
	}

	/**
	 * {@code runAbove} or {@code runBelow} set the stock flag of a direction: its timer takes the place of the thread.
	 *
	 * @param up the direction
	 */
	void scrollStarted(final boolean up) {
		if (!scrolling(up)) {
			arm(up ? ABOVE : BELOW, pick.m3SleepTime());
		}
	}

	private void tick(final int bit) {
		armed &= ~bit;
		if (pick.isDisposed()) {
			return;
		}
		if (!HideWatch.shown(pick)) {
			parked |= bit;
			if (bit == ABOVE) {
				aboveParkedAt = now();
			} else {
				belowParkedAt = now();
			}
			parks++;
			return;
		}
		final boolean up = bit == ABOVE;
		steps++;
		pick.m3ScrollOneMonth(up); // the stock thread posts a scroll after every sleep
		if (pick.m3Scrolling(up)) {
			arm(bit, pick.m3SleepTime()); // and sleeps again while its flag is set
		}
	}

	private void handleEvent(final Event event) {
		if (event.type == SWT.Dispose) {
			cancel(ABOVE, aboveTicker);
			cancel(BELOW, belowTicker);
			parked = 0;
			return;
		}
		if (parked != 0 && !pick.isDisposed() && HideWatch.shown(pick)) {
			final int bits = parked;
			parked = 0;
			if ((bits & ABOVE) != 0) {
				catchUp(ABOVE, aboveParkedAt);
			}
			if ((bits & BELOW) != 0) {
				catchUp(BELOW, belowParkedAt);
			}
		}
	}

	/* the parked scroll and, while the flag is set, every scroll of the hidden time; then the stock phase goes on */
	private void catchUp(final int bit, final long since) {
		final boolean up = bit == ABOVE;
		final int sleep = pick.m3SleepTime();
		long elapsed = now() - since;
		pick.m3ScrollOneMonth(up);
		caughtUp++;
		while (pick.m3Scrolling(up) && elapsed >= sleep) {
			elapsed -= sleep;
			pick.m3ScrollOneMonth(up);
			caughtUp++;
		}
		if (pick.m3Scrolling(up)) {
			arm(bit, (int) (sleep - elapsed));
		}
	}

	private void arm(final int bit, final int delay) {
		if ((armed & bit) != 0 || pick.isDisposed()) {
			return;
		}
		armed |= bit;
		display.timerExec(delay, bit == ABOVE ? aboveTicker : belowTicker);
	}

	private void cancel(final int bit, final Runnable ticker) {
		if ((armed & bit) != 0) {
			armed &= ~bit;
			if (!display.isDisposed()) {
				display.timerExec(-1, ticker);
			}
		}
	}

	private static long now() {
		return System.nanoTime() / 1_000_000L;
	}
}
