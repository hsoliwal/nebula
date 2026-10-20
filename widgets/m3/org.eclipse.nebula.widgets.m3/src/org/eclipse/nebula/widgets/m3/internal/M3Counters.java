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

/**
 * Delta gate counters of the M3 widgets (brief rule 24, "all delta based view" and "resize and layout change the
 * port"): how often a widget repainted its whole client area, re-planned its whole layout, or moved, created or
 * released a child. The differential and resize-sweep tests read them; a model delta or a width-only resize must
 * leave {@link #fullRepaints} and {@link #fullLayouts} unchanged. Plain fields: widgets run on the UI thread only.
 */
public final class M3Counters {

	/** whole-client-area repaints requested by a widget (redraw() without a rectangle) */
	public static long fullRepaints;
	/** layout passes that re-planned every item, slot or page, as opposed to an incremental pass */
	public static long fullLayouts;
	/** layout passes of any kind */
	public static long layouts;
	/** child controls moved or resized by a layout */
	public static long moves;
	/** child controls or items created on demand */
	public static long creates;
	/** child controls or items released */
	public static long releases;

	private M3Counters() {
	}

	/** zeroes every counter */
	public static void reset() {
		fullRepaints = fullLayouts = layouts = moves = creates = releases = 0;
	}

	/** @return the counters as one line, for test messages and logs */
	public static String line() {
		return "fullRepaints=" + fullRepaints + " fullLayouts=" + fullLayouts + " layouts=" + layouts + " moves=" + moves //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ " creates=" + creates + " releases=" + releases; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
