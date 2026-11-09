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
package org.eclipse.nebula.widgets.datechooser.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Hidden state of a control for the viewports of this bundle (aspect H, docs/m3/every-widget): whether a control is
 * shown, and the part visibility of the M3 SWT fork ({@code SWT.PartVisible}, {@code SWT.PartHidden},
 * {@code Control.isPartVisible()}), looked up by reflection so the bundle runs on stock SWT as well.
 * <p>
 * Stock SWT sends {@code SWT.Hide} only to the control whose own visibility changes: a combo whose parent, tab page or
 * shell hides hears nothing. An instance therefore watches the Hide events of a control's ancestors up to and
 * including its shell while it is armed (while a drop-down is open), with one listener and one reused array, and
 * forgets them when disarmed. On the M3 fork, {@code SWT.PartHidden} tells the control itself (ancestors, zero size,
 * minimized shell, containers that hide their pages), and the viewports do not arm a watch.
 * </p>
 */
public final class HideWatch {

	/** {@code SWT.PartVisible} of the M3 SWT fork; -1 on stock SWT. */
	public static final int PART_VISIBLE = event("PartVisible"); //$NON-NLS-1$

	/** {@code SWT.PartHidden} of the M3 SWT fork; -1 on stock SWT. */
	public static final int PART_HIDDEN = event("PartHidden"); //$NON-NLS-1$

	/* Control.isPartVisible of the M3 SWT fork, (Control)boolean; null on stock SWT */
	private static final MethodHandle IS_PART_VISIBLE = partVisible();

	private final Listener listener;
	private Composite[] hooked;
	private int count;

	/**
	 * @param listener receives the {@code SWT.Hide} event of every watched ancestor
	 */
	public HideWatch(final Listener listener) {
		this.listener = listener;
	}

	/**
	 * @param control a control
	 * @return true when the control and its ancestors are visible, it has a size, its shell is not minimized and, on
	 *         the M3 SWT fork, it is part-visible
	 */
	public static boolean shown(final Control control) {
		if (control.isDisposed() || !control.isVisible()) {
			return false; // the control or an ancestor is hidden (setVisible, tab and stack pages)
		}
		if (IS_PART_VISIBLE != null) {
			try {
				return (boolean) IS_PART_VISIBLE.invokeExact(control);
			} catch (final Throwable e) {
				// the stock checks below
			}
		}
		final Point size = control.getSize();
		return size.x > 0 && size.y > 0 && !control.getShell().getMinimized();
	}

	/**
	 * Watches {@code SWT.Hide} on every ancestor of the control up to and including its shell, in place of an earlier
	 * watch.
	 *
	 * @param control the control whose ancestors are watched
	 */
	public void arm(final Control control) {
		disarm();
		for (Composite parent = control.getParent(); parent != null; parent = parent.getParent()) {
			if (hooked == null) {
				hooked = new Composite[8];
			} else if (count == hooked.length) {
				hooked = Arrays.copyOf(hooked, count * 2);
			}
			hooked[count++] = parent;
			parent.addListener(SWT.Hide, listener);
			if (parent instanceof Shell) {
				break;
			}
		}
	}

	/** Stops watching the ancestors. */
	public void disarm() {
		for (int i = 0; i < count; i++) {
			if (!hooked[i].isDisposed()) {
				hooked[i].removeListener(SWT.Hide, listener);
			}
			hooked[i] = null;
		}
		count = 0;
	}

	/** @return true while ancestors are watched */
	public boolean isArmed() {
		return count > 0;
	}

	private static int event(final String name) {
		try {
			return SWT.class.getField(name).getInt(null);
		} catch (final ReflectiveOperationException | RuntimeException e) {
			return -1;
		}
	}

	private static MethodHandle partVisible() {
		try {
			return MethodHandles.publicLookup().findVirtual(Control.class, "isPartVisible", //$NON-NLS-1$
					MethodType.methodType(boolean.class));
		} catch (final ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
