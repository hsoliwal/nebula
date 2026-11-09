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
import org.eclipse.swt.widgets.Shell;

/**
 * Viewport of a {@link CustomCombo} (and of {@link FlatCalendarCombo}), aspect H (hidden gating,
 * docs/m3/every-widget). The stock combo hides its list drop-down when the combo itself is hidden
 * ({@code setVisible(false)}), but not when an ancestor hides: the ON_TOP drop-down shell stays on screen over the
 * hidden combo, and {@code dropDown(false)} cannot close it any more, because it returns at once while the combo is
 * not visible.
 * <p>
 * Here the drop-down shell is hidden directly, as the stock {@code setVisible(false)} does, when the combo hides, when
 * an ancestor up to the shell hides ({@code SWT.Hide} of each ancestor, watched only while the drop-down is open) or
 * when the combo stops being part-visible ({@code SWT.PartHidden} of the M3 SWT fork, which also covers zero sizes);
 * and a drop on a combo that is visible but not part-visible (fork) opens nothing, like the stock drop on an invisible
 * combo.
 * </p>
 * <p>
 * {@code -Dswt.widget.mode.CustomCombo=original} (or the global {@code swt.widget.mode=original}) keeps the stock
 * combo.
 * </p>
 */
final class CustomComboViewport {

	/** True unless the startup mode of CustomCombo is original. */
	static final boolean ENABLED = WidgetMode.of("CustomCombo", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private final CustomCombo combo;
	private final Listener listener = this::handleEvent;
	/* the ancestors' Hide while the drop-down is open; null on the M3 SWT fork, whose SWT.PartHidden covers them */
	private final HideWatch ancestors;
	/* the drop-down shell whose Hide ends the watch (stock SWT) */
	private Shell watchedPopup;

	/** Drop-downs closed because the combo or an ancestor hid (tests and benches). */
	int closes;
	/** Drops that opened nothing because the combo was not part-visible (tests and benches). */
	int refused;

	CustomComboViewport(final CustomCombo combo) {
		this.combo = combo;
		combo.addListener(SWT.Hide, listener);
		combo.addListener(SWT.Dispose, listener);
		if (HideWatch.PART_HIDDEN >= 0) {
			combo.addListener(HideWatch.PART_HIDDEN, listener);
			ancestors = null;
		} else {
			ancestors = new HideWatch(listener);
		}
	}

	/** @return false, and the drop opens nothing, while the combo is hidden */
	boolean mayOpen() {
		if (HideWatch.shown(combo)) {
			return true;
		}
		refused++;
		return false;
	}

	/** {@code dropDown(true)} showed the drop-down: watch the ancestors until it hides (stock SWT). */
	void opened() {
		if (ancestors == null) {
			return;
		}
		ancestors.arm(combo);
		final Shell popup = combo.popup;
		if (popup != watchedPopup) {
			watchedPopup = popup;
			popup.addListener(SWT.Hide, listener);
		}
	}

	/** @return true while the ancestors' Hide is watched (tests) */
	boolean isWatching() {
		return ancestors != null && ancestors.isArmed();
	}

	private void handleEvent(final Event event) {
		if (event.type == SWT.Dispose) {
			if (ancestors != null) {
				ancestors.disarm();
			}
			return;
		}
		if (event.widget == watchedPopup) {
			ancestors.disarm(); // the drop-down closed
			return;
		}
		// SWT.Hide of the combo or an ancestor, or SWT.PartHidden
		final Shell popup = combo.popup;
		if (popup != null && !popup.isDisposed() && popup.getVisible()) {
			closes++;
			popup.setVisible(false);
		}
		if (ancestors != null) {
			ancestors.disarm();
		}
	}
}
