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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Viewport of a {@link CalendarCombo}, aspect H (hidden gating, docs/m3/every-widget). The stock combo opens its
 * calendar in an ON_TOP shell ({@code showCalendar}) and closes it ({@code kill}) on a move, resize or disposal of
 * the combo control, on a move, resize, deactivation, focus loss, close or iconify of the parent shell and on clicks
 * and keys outside, but not when the combo or an ancestor hides: the drop-down, with its CalendarComposite and an
 * open month picker, stays on screen over the hidden combo, and an open request on a hidden combo opens it there.
 * <p>
 * Here the drop-down closes through the stock kill, an open month picker first and the focus left alone, when the
 * combo hides (its own {@code SWT.Hide}), when an ancestor up to the shell hides ({@code SWT.Hide} of each ancestor,
 * watched only while the drop-down is open) or when the combo stops being part-visible ({@code SWT.PartHidden} of the
 * M3 SWT fork, which also covers zero sizes and container pages); and {@code showCalendar}/{@code openCalendar} on a
 * hidden combo open nothing.
 * </p>
 * <p>
 * {@code -Dswt.widget.mode.CalendarCombo=original} (or the global {@code swt.widget.mode=original}) keeps the stock
 * combo.
 * </p>
 */
final class CalendarComboViewport {

	/** True unless the startup mode of CalendarCombo is original. */
	static final boolean ENABLED = WidgetMode.of("CalendarCombo", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private final CalendarCombo combo;
	private final Listener listener = this::handleEvent;
	/* the ancestors' Hide while the drop-down is open; null on the M3 SWT fork, whose SWT.PartHidden covers them */
	private final HideWatch ancestors;

	/** Drop-downs closed because the combo or an ancestor hid (tests and benches). */
	int closes;
	/** Open requests that opened nothing because the combo was hidden (tests and benches). */
	int refused;

	CalendarComboViewport(final CalendarCombo combo) {
		this.combo = combo;
		// CalendarCombo.addListener forwards to the combo control: the composite's own events go through m3AddOwnListener
		combo.m3AddOwnListener(SWT.Hide, listener);
		combo.m3AddOwnListener(SWT.Dispose, listener);
		if (HideWatch.PART_HIDDEN >= 0) {
			combo.addListener(HideWatch.PART_HIDDEN, listener); // the combo control is part-hidden with the combo
			ancestors = null;
		} else {
			ancestors = new HideWatch(listener);
		}
	}

	/** @return false, and the open request opens nothing, while the combo is hidden */
	boolean mayOpen() {
		if (HideWatch.shown(combo)) {
			return true;
		}
		refused++;
		return false;
	}

	/** {@link CalendarCombo#showCalendar()} opened the drop-down: watch the ancestors (stock SWT). */
	void opened() {
		if (ancestors != null) {
			ancestors.arm(combo);
		}
	}

	/** The stock kill closed the drop-down. */
	void closed() {
		if (ancestors != null) {
			ancestors.disarm();
		}
	}

	/** @return true while the ancestors' Hide is watched (tests) */
	boolean isWatching() {
		return ancestors != null && ancestors.isArmed();
	}

	private void handleEvent(final Event event) {
		if (event.type == SWT.Dispose) {
			closed();
			return;
		}
		// SWT.Hide of the combo or an ancestor, or SWT.PartHidden: the drop-down must not stay over a hidden combo
		if (!combo.isDisposed() && combo.m3CalendarOpen()) {
			closes++;
			combo.m3CloseHidden();
		}
		closed();
	}
}
