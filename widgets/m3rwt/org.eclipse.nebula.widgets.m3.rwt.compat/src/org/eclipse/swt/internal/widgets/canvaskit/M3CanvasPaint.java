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
package org.eclipse.swt.internal.widgets.canvaskit;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.graphics.GCAdapter;
import org.eclipse.swt.widgets.Canvas;

/**
 * How a Canvas repaints on RWT, as delta views (user rule 24): partial redraws within one request accumulate into
 * one damage rectangle (RWT 4.8.0 kept only the last one, so earlier damage never reached the browser), and while
 * the canvas or an ancestor is hidden nothing is painted or sent; the damage waits and is painted once when the
 * canvas is rendered visible again. Design: docs/m3/rwt/RWT_GC_COMPAT.md.
 */
public final class M3CanvasPaint {

	private M3CanvasPaint () {
	}

	/**
	 * Called by {@code Canvas.repaint}.
	 *
	 * @return the rectangle to paint now (the new damage united with damage not yet sent), or {@code null} if the
	 *         canvas is hidden and the damage was recorded instead
	 */
	public static Rectangle damage (Canvas canvas, GCAdapter adapter, Rectangle rectangle) {
		if (!canvas.isVisible ()) {
			adapter.setHiddenDamage (union (adapter.getHiddenDamage (), rectangle));
			return null;
		}
		Rectangle result = rectangle;
		if (adapter.getForceRedraw () && adapter.getPaintRect () != null) result = union (result, adapter.getPaintRect ());
		if (adapter.getHiddenDamage () != null) {
			result = union (result, adapter.getHiddenDamage ());
			adapter.setHiddenDamage (null);
		}
		return result;
	}

	/** Called by the canvas renderer: a canvas that became visible paints the damage recorded while it was hidden. */
	static void paintIfShown (Canvas canvas) {
		GCAdapter adapter = canvas.getAdapter (GCAdapter.class);
		Rectangle hidden = adapter.getHiddenDamage ();
		if (hidden != null && canvas.isVisible ()) canvas.redraw (hidden.x, hidden.y, hidden.width, hidden.height, false);
	}

	/** {@code Canvas.scroll}: the union of source and destination, or {@code null} if the area is empty. */
	public static Rectangle scrollDamage (int destX, int destY, int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) return null;
		return union (new Rectangle (x, y, width, height), new Rectangle (destX, destY, width, height));
	}

	/** Bounding union; {@code a} may be {@code null}. */
	static Rectangle union (Rectangle a, Rectangle b) {
		if (a == null) return new Rectangle (b.x, b.y, b.width, b.height);
		int x = Math.min (a.x, b.x), y = Math.min (a.y, b.y);
		int right = Math.max (a.x + a.width, b.x + b.width), bottom = Math.max (a.y + a.height, b.y + b.height);
		return new Rectangle (x, y, right - x, bottom - y);
	}
}
