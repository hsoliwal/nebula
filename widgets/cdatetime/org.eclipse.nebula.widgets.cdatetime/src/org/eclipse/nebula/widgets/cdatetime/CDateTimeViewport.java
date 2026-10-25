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
package org.eclipse.nebula.widgets.cdatetime;

import org.eclipse.nebula.widgets.cdatetime.internal.HideWatch;
import org.eclipse.nebula.widgets.cdatetime.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Viewport of a {@link CDateTime}, aspect H (hidden gating, docs/m3/every-widget). The stock CDateTime (a cwt
 * BaseCombo) closes its drop-down on a move or resize of the combo and on the deactivation of the drop-down shell,
 * but not when the combo or an ancestor hides: the ON_TOP drop-down, with its picker, stays on screen over the hidden
 * combo, and {@code setOpen(true)} on a hidden combo opens it there. Every selection or calendar change refreshes the
 * picker ({@code updatePicker}: DatePicker, AnalogTimePicker or DiscreteTimePicker {@code updateView}, which re-texts
 * the day, month and year buttons and redraws, or repaints the clock), also while the combo is hidden and while the
 * drop-down is closed: each close refreshes the picker that is being shrunk and disposed.
 * <p>
 * Here the drop-down closes when the combo hides (its own {@code SWT.Hide}), when an ancestor up to the shell hides
 * ({@code SWT.Hide} of each ancestor, watched only while the drop-down is open) or when the combo stops being
 * part-visible ({@code SWT.PartHidden} of the M3 SWT fork, which also covers zero sizes): the drop-down shell goes at
 * once and the stock close follows. {@code setOpen(true)} on a hidden combo opens nothing. The picker is not refreshed
 * while the drop-down is closed (the next open builds a new picker from the model) or while the combo is hidden; a
 * SIMPLE combo whose refresh was held refreshes its picker once on the next Show, Resize, Paint or
 * {@code SWT.PartVisible}, so it shows what the stock one does.
 * </p>
 * <p>
 * {@code -Dswt.widget.mode.CDateTime=original} (or the global {@code swt.widget.mode=original}) keeps the stock
 * CDateTime.
 * </p>
 */
final class CDateTimeViewport {

	/** True unless the startup mode of CDateTime is original. */
	static final boolean ENABLED = WidgetMode.of("CDateTime", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private final CDateTime cdt;
	private final Listener listener = this::handleEvent;
	/* the ancestors' Hide while the drop-down is open; null on the M3 SWT fork, whose SWT.PartHidden covers them */
	private final HideWatch ancestors;
	/* a SIMPLE combo's picker refresh was held while hidden */
	private boolean pickerStale;

	/** Drop-downs closed because the combo or an ancestor hid (tests and benches). */
	int closes;
	/** Open requests that opened nothing because the combo was hidden (tests and benches). */
	int refused;
	/** Picker refreshes held because the drop-down was closed or the combo hidden (tests and benches). */
	int held;
	/** Held picker refreshes made once after a reveal (tests and benches). */
	int caughtUp;

	CDateTimeViewport(final CDateTime cdt) {
		this.cdt = cdt;
		cdt.addListener(SWT.Hide, listener);
		cdt.addListener(SWT.Show, listener);
		cdt.addListener(SWT.Resize, listener);
		// an ancestor shown again: SWT sends no Show to the child, but the first paint after the reveal arrives
		cdt.addListener(SWT.Paint, listener);
		cdt.addListener(SWT.Dispose, listener);
		if (HideWatch.PART_HIDDEN >= 0) {
			cdt.addListener(HideWatch.PART_HIDDEN, listener);
			cdt.addListener(HideWatch.PART_VISIBLE, listener);
			ancestors = null;
		} else {
			ancestors = new HideWatch(listener);
		}
	}

	/** @return false, and {@code setOpen(true)} opens nothing, while the combo is hidden */
	boolean mayOpen() {
		if (HideWatch.shown(cdt)) {
			return true;
		}
		refused++;
		return false;
	}

	/** {@code setOpen} ran: watch the ancestors while the drop-down is open (stock SWT). */
	void openChanged() {
		if (ancestors == null) {
			return;
		}
		if (cdt.isOpen()) {
			ancestors.arm(cdt);
		} else {
			ancestors.disarm();
		}
	}

	/**
	 * {@code updatePicker} is about to refresh the picker.
	 *
	 * @return true when the refresh is held: the drop-down is closed or the combo hidden
	 */
	boolean holdPickerRefresh() {
		if (cdt.picker == null) {
			return false; // nothing to refresh: the stock method does nothing either
		}
		final boolean simple = (cdt.style & CDT.SIMPLE) != 0;
		if ((simple || cdt.isOpen()) && HideWatch.shown(cdt)) {
			pickerStale = false;
			return false;
		}
		held++;
		pickerStale = simple; // a closed drop-down's picker is disposed, the next open builds a new one
		return true;
	}

	/** @return true while the ancestors' Hide is watched (tests) */
	boolean isWatching() {
		return ancestors != null && ancestors.isArmed();
	}

	private void handleEvent(final Event event) {
		switch (event.type) {
		case SWT.Dispose:
			if (ancestors != null) {
				ancestors.disarm();
			}
			return;
		case SWT.Show:
		case SWT.Resize:
		case SWT.Paint:
			revealed();
			return;
		case SWT.Hide:
			hidden();
			return;
		default:
			if (event.type == HideWatch.PART_HIDDEN) {
				hidden();
			} else {
				revealed();
			}
		}
	}

	private void hidden() {
		if (!cdt.isDisposed() && cdt.isOpen()) {
			closes++;
			cdt.m3CloseHidden();
		}
		if (ancestors != null) {
			ancestors.disarm();
		}
	}

	private void revealed() {
		if (pickerStale && !cdt.isDisposed() && HideWatch.shown(cdt)) {
			pickerStale = false;
			caughtUp++;
			cdt.m3RefreshPicker();
		}
	}
}
