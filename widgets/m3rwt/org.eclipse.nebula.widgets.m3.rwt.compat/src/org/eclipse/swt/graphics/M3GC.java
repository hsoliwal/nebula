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
package org.eclipse.swt.graphics;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.graphics.GCAdapter;
import org.eclipse.swt.internal.graphics.GCOperation;
import org.eclipse.swt.internal.graphics.M3GCOperation;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * The GC methods that RWT 4.8.0 lacks, called from the patched {@link GC}: patterns, fill rule, copyArea, drawing
 * on images and drawing recorded images. Design: docs/m3/rwt/RWT_GC_COMPAT.md.
 */
final class M3GC {

	private static final Logger LOG = Logger.getLogger ("org.eclipse.swt.graphics");
	private static volatile boolean copyAreaLogged;

	private M3GC () {
	}

	/** The delegate of {@code new GC (image)}. */
	static GCDelegate imageDelegate (Image image) {
		return M3DisplayLists.openGC (image);
	}

	static void setPattern (GCDelegate delegate, boolean foreground, Pattern pattern) {
		M3GCOperation.SetPattern operation = pattern == null ? M3GCOperation.SetPattern.none (foreground) : pattern.toOperation (foreground);
		add (delegate, operation);
		if (delegate instanceof M3ImageGC) ((M3ImageGC) delegate).remember (operation);
	}

	static void setFillRule (GCDelegate delegate, int rule) {
		M3GCOperation.SetFillRule operation = new M3GCOperation.SetFillRule (rule == SWT.FILL_EVEN_ODD);
		add (delegate, operation);
		if (delegate instanceof M3ImageGC) ((M3ImageGC) delegate).remember (operation);
	}

	/** Closes the segment of a GC on an image; other delegates need nothing. */
	static void dispose (GCDelegate delegate) {
		if (delegate instanceof M3ImageGC) ((M3ImageGC) delegate).end ();
	}

	/**
	 * Draws a recorded image by replaying its display list.
	 *
	 * @return false if the image has no display list (RWT then draws its pixels as before)
	 */
	static boolean drawRecorded (GCDelegate delegate, Image image, Rectangle src, Rectangle dest) {
		if (!M3DisplayLists.isRecorded (image)) return false;
		GCAdapter sink = delegate instanceof ControlGC ? ((ControlGC) delegate).getGCAdapter () : null;
		if (sink == null) return true;
		Rectangle source = src.width < 0 ? image.getBounds () : src;
		Rectangle target = dest.width < 0 ? new Rectangle (dest.x, dest.y, source.width, source.height) : dest;
		M3DisplayLists.replay (sink, delegate.getTransform (), image, source, target);
		return true;
	}

	/** {@code copyArea (image, x, y)}: a recorded source is copied as a display list; a Control has no server pixels. */
	static void copyArea (GCDelegate delegate, Drawable drawable, Image image, int x, int y) {
		if (drawable instanceof Image && M3DisplayLists.isRecorded ((Image) drawable)) {
			M3DisplayLists.copy ((Image) drawable, image, x, y);
		} else if (!copyAreaLogged) {
			copyAreaLogged = true;
			LOG.log (Level.FINE, "GC.copyArea (Image, int, int) leaves the image unchanged on RWT: the server keeps no pixels of a {0}", drawable);
		}
	}

	/**
	 * {@code copyArea (srcX, srcY, width, height, destX, destY, paint)} on a Control: the destination, and with
	 * {@code paint} the source, are redrawn after the current event, so the paint listener draws the moved content.
	 */
	static void copyArea (Drawable drawable, int srcX, int srcY, int width, int height, int destX, int destY, boolean paint) {
		if (!(drawable instanceof Control) || width <= 0 || height <= 0) return;
		Control control = (Control) drawable;
		Display display = control.getDisplay ();
		display.asyncExec (() -> {
			if (control.isDisposed ()) return;
			control.redraw (destX, destY, width, height, false);
			if (paint) control.redraw (srcX, srcY, width, height, false);
		});
	}

	private static void add (GCDelegate delegate, GCOperation operation) {
		if (!(delegate instanceof ControlGC)) return;
		GCAdapter adapter = ((ControlGC) delegate).getGCAdapter ();
		if (adapter != null) adapter.addGCOperation (operation);
	}
}
