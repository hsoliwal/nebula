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
package org.eclipse.nebula.widgets.m3.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Scrollable;

/**
 * Hidden viewports (brief rule 24, user 2026-10-20: "if the viewport becomes visible from hidden it should paint;
 * while it is hidden the model may make changes and the viewport is dirty and renders on becoming visible").
 * <p>
 * A widget asks {@link #visible()} before the native work of a model delta (a redraw, a measure, a child move). While
 * the control is hidden (its own Hide event, a zero size, or a hidden ancestor found by one probe) the answer is
 * false and the delta is folded into a coalesced dirty flag: O(1), no native call after the first. When the control
 * becomes visible (its Show event, a resize from zero, or the first paint after an ancestor was shown) the gate runs
 * the widget's reveal action once, the one window patch against the current model, and repaints the client area
 * once (a paint that already covers the client area is that one paint).
 * </p>
 * <p>
 * Create the gate in the widget constructor before the widget adds its own Paint listener, so the patch is applied
 * before the paint.
 * </p>
 */
public final class M3HiddenGate {

	private final Control control;
	private final Runnable reveal;
	private boolean hidden, dirty, zero;

	/**
	 * @param control the viewport control
	 * @param reveal the window patch to apply when the control becomes visible with pending deltas (re-measure,
	 *            re-layout), or null; the gate repaints after it
	 */
	public M3HiddenGate(Control control, Runnable reveal) {
		this.control = control;
		this.reveal = reveal;
		control.addListener(SWT.Hide, e -> hidden = true);
		control.addListener(SWT.Show, e -> {
			hidden = false;
			reveal(null);
		});
		control.addListener(SWT.Resize, e -> {
			Point size = control.getSize();
			boolean wasZero = zero;
			zero = size.x <= 0 || size.y <= 0;
			if (wasZero && !zero) {
				reveal(null);
			}
		});
		control.addListener(SWT.Paint, this::paint);
	}

	/**
	 * @return true when a delta may do native work now; false when the control is hidden, in which case the delta has
	 *         been folded into the dirty window and the control renders when it becomes visible
	 */
	public boolean visible() {
		if (hidden || zero || dirty) {
			dirty = true;
			M3Counters.hiddenDeltas++;
			return false;
		}
		return true;
	}

	/**
	 * Like {@link #visible()}, but also probes the ancestors once (Control.isVisible) and folds the delta when an
	 * ancestor is hidden; use before expensive native work (measuring, creating, moving children).
	 *
	 * @return true when the control is visible
	 */
	public boolean probe() {
		if (!visible()) {
			return false;
		}
		M3Counters.probes++;
		if (!control.isVisible()) {
			hidden = dirty = true;
			M3Counters.hiddenDeltas++;
			return false;
		}
		return true;
	}

	/** @return true while deltas are pending for the next reveal */
	public boolean isDirty() {
		return dirty;
	}

	private void paint(Event event) {
		hidden = false;
		reveal(event);
	}

	private void reveal(Event paint) {
		if (!dirty || zero) {
			return;
		}
		dirty = false;
		M3Counters.reveals++;
		if (reveal != null) {
			reveal.run();
		}
		Point size = control.getSize();
		int width = size.x, height = size.y;
		if (control instanceof Scrollable) {
			org.eclipse.swt.graphics.Rectangle client = ((Scrollable) control).getClientArea();
			width = client.width;
			height = client.height;
		}
		boolean covered = paint != null && paint.x <= 0 && paint.y <= 0 && paint.x + paint.width >= width
				&& paint.y + paint.height >= height;
		if (!covered) {
			control.redraw();
		}
	}
}
