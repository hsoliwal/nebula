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
package org.eclipse.nebula.widgets.geomap.internal;

import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Whether a control that shows a map is shown, and the events that report a change, for the hidden gating of
 * {@code GeoMap} and {@code MapFigure} (aspect H, docs/m3/every-widget). A control is shown when it is not disposed, it
 * and its ancestors are visible, it has a non-zero size, its shell is not minimized and, on the M3 SWT fork,
 * {@code Control.isPartVisible()} holds (looked up by reflection; absent on stock SWT).
 * <p>
 * A new instance adds one listener: to the control for SWT.Hide, SWT.Resize and SWT.Paint (the paint is the reveal:
 * SWT sends SWT.Show before the window is visible and nothing to a child whose ancestor shows again); on the M3 fork
 * for SWT.PartHidden and SWT.PartVisible, which also report hidden ancestors and a minimized shell; on stock SWT to
 * every ancestor for SWT.Hide and to the shell for SWT.Iconify, so the owner learns at the moment the map is hidden
 * and not at its next tile. {@link #dispose()} removes all of them.
 * </p>
 */
public final class GeoMapVisibility {

	/* SWT.PartVisible, SWT.PartHidden and Control.isPartVisible of the M3 SWT fork; -1 and absent on stock SWT */
	private static final int PART_VISIBLE = partEvent("PartVisible"); //$NON-NLS-1$
	private static final int PART_HIDDEN = partEvent("PartHidden"); //$NON-NLS-1$
	private static final Method IS_PART_VISIBLE = partVisibleMethod();

	private final Control control;
	private final Listener listener;
	/* the ancestors (SWT.Hide) and, last, the shell (SWT.Iconify) on stock SWT; empty on the M3 fork */
	private final Control[] ancestors;

	/**
	 * Adds the listener to the control (and on stock SWT to its ancestors and shell).
	 *
	 * @param control the control that shows the map
	 * @param listener the owner's listener, called for the events above (and SWT.Dispose of the control)
	 */
	public GeoMapVisibility(final Control control, final Listener listener) {
		this.control = control;
		this.listener = listener;
		control.addListener(SWT.Hide, listener);
		control.addListener(SWT.Resize, listener);
		control.addListener(SWT.Paint, listener);
		control.addListener(SWT.Dispose, listener);
		if (PART_VISIBLE >= 0) {
			control.addListener(PART_VISIBLE, listener);
			control.addListener(PART_HIDDEN, listener);
			ancestors = new Control[0];
			return;
		}
		ancestors = ancestors(control);
		for (final Control ancestor : ancestors) {
			ancestor.addListener(SWT.Hide, listener);
		}
		if (ancestors.length > 0) {
			ancestors[ancestors.length - 1].addListener(SWT.Iconify, listener);
		}
	}

	/* the parent chain up to and including the control's own shell (a dialog shell's parent shell is not followed) */
	private static Control[] ancestors(final Control control) {
		int depth = 0;
		for (Control parent = control.getParent(); parent != null; parent = next(parent)) {
			depth++;
		}
		final Control[] chain = new Control[depth];
		Control parent = control.getParent();
		for (int i = 0; i < depth; i++, parent = next(parent)) {
			chain[i] = parent;
		}
		return chain;
	}

	private static Control next(final Control parent) {
		return parent instanceof Shell ? null : parent.getParent();
	}

	/** @return true while the control is shown (see the class comment) */
	public boolean shown() {
		return shown(control);
	}

	/** @return the control */
	public Control getControl() {
		return control;
	}

	/** Removes the listener from the control (when not disposed), its ancestors and its shell. */
	public void dispose() {
		if (!control.isDisposed()) {
			control.removeListener(SWT.Hide, listener);
			control.removeListener(SWT.Resize, listener);
			control.removeListener(SWT.Paint, listener);
			control.removeListener(SWT.Dispose, listener);
			if (PART_VISIBLE >= 0) {
				control.removeListener(PART_VISIBLE, listener);
				control.removeListener(PART_HIDDEN, listener);
			}
		}
		for (final Control ancestor : ancestors) {
			if (!ancestor.isDisposed()) {
				ancestor.removeListener(SWT.Hide, listener);
				ancestor.removeListener(SWT.Iconify, listener);
			}
		}
	}

	/**
	 * @param control a control
	 * @return true when the control is not disposed, visible with its ancestors, of a non-zero size, its shell not
	 *         minimized and, on the M3 SWT fork, part-visible
	 */
	public static boolean shown(final Control control) {
		if (control.isDisposed() || !control.isVisible()) {
			return false; // the control or an ancestor is hidden (setVisible, tab and stack pages)
		}
		final Point size = control.getSize();
		if (size.x <= 0 || size.y <= 0 || control.getShell().getMinimized()) {
			return false;
		}
		if (IS_PART_VISIBLE != null) {
			try {
				return (Boolean) IS_PART_VISIBLE.invoke(control);
			} catch (final ReflectiveOperationException e) {
				return true;
			}
		}
		return true;
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
