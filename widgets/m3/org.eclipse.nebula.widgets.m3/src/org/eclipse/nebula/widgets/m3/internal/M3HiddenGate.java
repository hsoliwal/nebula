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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

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
 * On SWT with part visibility (rule 25: SWT.PartVisible / SWT.PartHidden, which cover ancestors, tabs, stack pages,
 * collapsed sections, zero size and minimized shells) the gate listens to those events and never probes.
 * </p>
 * <p>
 * Create the gate in the widget constructor before the widget adds its own Paint listener, so the patch is applied
 * before the paint.
 * </p>
 */
public final class M3HiddenGate {

	/*
	 * Part visibility (brief rule 25): SWT.PartVisible / SWT.PartHidden, Control.isPartVisible() and
	 * Control.setPartHidden(boolean), present in the M3 SWT fork (docs/m3/visibility/PART_VISIBLE.md) and absent in
	 * stock SWT 3.135.0. Bound once by reflection so the bundle runs on both; -1 and null when absent.
	 */
	private static final int PART_VISIBLE, PART_HIDDEN;
	private static final MethodHandle IS_PART_VISIBLE, SET_PART_HIDDEN;

	static {
		int visible = -1, hidden = -1;
		MethodHandle is = null, set = null;
		try {
			visible = SWT.class.getField("PartVisible").getInt(null); //$NON-NLS-1$
			hidden = SWT.class.getField("PartHidden").getInt(null); //$NON-NLS-1$
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			is = lookup.findVirtual(Control.class, "isPartVisible", MethodType.methodType(boolean.class)); //$NON-NLS-1$
			set = lookup.findVirtual(Control.class, "setPartHidden", MethodType.methodType(void.class, boolean.class)); //$NON-NLS-1$
		} catch (ReflectiveOperationException | RuntimeException e) {
			visible = hidden = -1;
			is = set = null;
		}
		PART_VISIBLE = visible;
		PART_HIDDEN = hidden;
		IS_PART_VISIBLE = is;
		SET_PART_HIDDEN = set;
	}

	/** @return true when the running SWT has the part visibility events (rule 25) */
	public static boolean hasPartVisibility() {
		return PART_HIDDEN >= 0;
	}

	/**
	 * Reports that a container hid or showed content by other means than setVisible or a zero size (a drawn
	 * collapsible section, a drawn stack page, an overlay): Control.setPartHidden on SWT with part visibility, nothing
	 * otherwise (the gates of the content then rely on Hide/Show, zero size and one probe).
	 */
	public static void setPartHidden(Control content, boolean hidden) {
		if (SET_PART_HIDDEN != null) {
			try {
				SET_PART_HIDDEN.invoke(content, hidden);
			} catch (Throwable e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private static boolean isPartVisible(Control control) {
		try {
			return (boolean) IS_PART_VISIBLE.invoke(control);
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		}
	}

	private final Control control;
	private final Runnable reveal;
	private boolean hidden, dirty, zero;
	/* true while the reveal action runs: the control is becoming visible (a Show event arrives before the window is
	 * shown, so a probe would still see it hidden) */
	private boolean revealing;

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
		if (PART_HIDDEN >= 0) {
			control.addListener(PART_HIDDEN, e -> hidden = true);
			control.addListener(PART_VISIBLE, e -> {
				hidden = false;
				reveal(null);
			});
			hidden = !isPartVisible(control);
		}
	}

	/**
	 * @return true when a delta may do native work now; false when the control is hidden, in which case the delta has
	 *         been folded into the dirty window and the control renders when it becomes visible
	 */
	public boolean visible() {
		if (revealing) {
			return true;
		}
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
		if (revealing) {
			return true;
		}
		if (PART_HIDDEN >= 0) {
			return true;
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
			revealing = true;
			try {
				reveal.run();
			} finally {
				revealing = false;
			}
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
