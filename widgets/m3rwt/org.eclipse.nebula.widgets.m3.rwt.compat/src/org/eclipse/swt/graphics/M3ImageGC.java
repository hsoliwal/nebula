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

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.graphics.GCAdapter;
import org.eclipse.swt.internal.graphics.M3GCOperation;

/**
 * The delegate of a GC that draws on an {@link Image}. RWT keeps no pixels on the server, so the operations are
 * recorded into the image's display list ({@link M3DisplayLists}) with the same state logic as a Control GC, and
 * replayed where the image is drawn with {@link GC#drawImage}. Each GC is one segment of the list, wrapped in
 * {@code BeginReplay} / {@code EndReplay}, so state never leaks from one GC to the next.
 */
final class M3ImageGC extends ControlGC {

	private static final float[] IDENTITY = {1, 0, 0, 1, 0, 0};

	private final Image image;
	private final GCAdapter list;
	private boolean clipOpen;
	private M3GCOperation.SetPattern backgroundPattern;
	private M3GCOperation.SetPattern foregroundPattern;
	private M3GCOperation.SetFillRule fillRule;
	private boolean ended;

	M3ImageGC (Image image, GCAdapter list) {
		super (image.getDevice ().getSystemFont (), image.getDevice ().getSystemColor (SWT.COLOR_WHITE), image.getDevice ().getSystemColor (SWT.COLOR_BLACK));
		this.image = image;
		this.list = list;
		beginSegment ();
	}

	@Override
	GCAdapter getGCAdapter () {
		return ended ? null : list;
	}

	@Override
	Font getDefaultFont () {
		return image.getDevice ().getSystemFont ();
	}

	@Override
	Rectangle getDrawableBounds () {
		return image.getBounds ();
	}

	@Override
	void setClipping (Rectangle rectangle) {
		super.setClipping (rectangle);
		clipOpen = rectangle != null;
	}

	@Override
	void setClipping (Path path) {
		super.setClipping (path);
		clipOpen = path != null;
	}

	@Override
	void drawRectangle (Rectangle bounds, boolean fill) {
		if (fill && coversOpaque (bounds)) {
			list.clearGCOperations ();
			beginSegment ();
		}
		super.drawRectangle (bounds, fill);
	}

	/** Remembers the pattern and fill rule operations so that a compaction can restate them. */
	void remember (M3GCOperation operation) {
		if (operation instanceof M3GCOperation.SetPattern) {
			M3GCOperation.SetPattern pattern = (M3GCOperation.SetPattern) operation;
			M3GCOperation.SetPattern kept = pattern.kind == M3GCOperation.SetPattern.NONE ? null : pattern;
			if (pattern.foreground) foregroundPattern = kept; else backgroundPattern = kept;
		} else if (operation instanceof M3GCOperation.SetFillRule) {
			fillRule = (M3GCOperation.SetFillRule) operation;
		}
	}

	boolean isEnded () {
		return ended;
	}

	/** Closes this GC's segment; called when the GC is disposed. */
	void end () {
		if (!ended) {
			list.addGCOperation (new M3GCOperation.EndReplay ());
			ended = true;
		}
	}

	/**
	 * An opaque fill that covers the whole image, unclipped and untransformed, hides everything recorded before
	 * it (the usual first call of a double buffer), so the list restarts with this segment's current state.
	 */
	private boolean coversOpaque (Rectangle bounds) {
		Rectangle size = image.getBounds ();
		if (clipOpen || getAlpha () != 255 || !java.util.Arrays.equals (getTransform (), IDENTITY)) return false;
		if (backgroundPattern != null && !isOpaqueGradient (backgroundPattern)) return false;
		return bounds.x <= 0 && bounds.y <= 0 && bounds.x + bounds.width >= size.width && bounds.y + bounds.height >= size.height;
	}

	private static boolean isOpaqueGradient (M3GCOperation.SetPattern pattern) {
		return pattern.kind == M3GCOperation.SetPattern.GRADIENT && pattern.argb1 >>> 24 == 0xFF && pattern.argb2 >>> 24 == 0xFF;
	}

	private void beginSegment () {
		Rectangle size = image.getBounds ();
		list.addGCOperation (new M3GCOperation.BeginReplay (IDENTITY.clone (), 0, 0, size.width, size.height));
		M3DisplayLists.restate (list, this);
		if (backgroundPattern != null) list.addGCOperation (backgroundPattern);
		if (foregroundPattern != null) list.addGCOperation (foregroundPattern);
		if (fillRule != null) list.addGCOperation (fillRule);
	}
}
