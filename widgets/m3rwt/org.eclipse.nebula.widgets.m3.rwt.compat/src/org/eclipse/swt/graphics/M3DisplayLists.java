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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.graphics.FontUtil;
import org.eclipse.swt.internal.graphics.GCAdapter;
import org.eclipse.swt.internal.graphics.GCOperation;
import org.eclipse.swt.internal.graphics.GCOperation.SetProperty;
import org.eclipse.swt.internal.graphics.M3GCOperation;
import org.eclipse.swt.internal.widgets.canvaskit.M3GradientMath;

/**
 * The display lists of images drawn with {@code new GC (image)}: one list of GC operations per image, kept as long
 * as the image is reachable. A list starts with the image's own pixels (one {@code DrawImage}) followed by one
 * segment per GC; an opaque fill over the whole image restarts it (see {@link M3ImageGC}).
 */
final class M3DisplayLists {

	private static final Logger LOG = Logger.getLogger ("org.eclipse.swt.graphics");
	/** Lists longer than this are reported once (FINE would hide a leak; WARNING is the right level). */
	static final int WARN_OPERATIONS = 1 << 16;

	private static final class Entry {
		final GCAdapter list = new GCAdapter ();
		M3ImageGC open;
		boolean warned;
	}

	private static final Map<Image, Entry> LISTS = Collections.synchronizedMap (new WeakHashMap<> ());

	private M3DisplayLists () {
	}

	/** Creates the delegate of a GC on the image; like SWT, an image can have only one GC at a time. */
	static M3ImageGC openGC (Image image) {
		Entry entry = LISTS.computeIfAbsent (image, M3DisplayLists::newEntry);
		if (entry.open != null && !entry.open.isEnded ()) SWT.error (SWT.ERROR_INVALID_ARGUMENT);
		entry.open = new M3ImageGC (image, entry.list);
		return entry.open;
	}

	private static Entry newEntry (Image image) {
		Entry entry = new Entry ();
		Rectangle size = image.getBounds ();
		entry.list.addGCOperation (new GCOperation.DrawImage (image, new Rectangle (0, 0, size.width, size.height), new Rectangle (0, 0, size.width, size.height), true));
		return entry;
	}

	/** @return whether the image has a display list, i.e. a GC has drawn on it */
	static boolean isRecorded (Image image) {
		return LISTS.containsKey (image);
	}

	/** @return a copy of the image's operations, or an empty array */
	static GCOperation[] operations (Image image) {
		Entry entry = LISTS.get (image);
		if (entry == null) return new GCOperation[0];
		GCOperation[] operations = entry.list.getGCOperations ();
		if (operations.length > WARN_OPERATIONS && !entry.warned) {
			entry.warned = true;
			LOG.log (Level.WARNING, () -> "display list of " + image + " has " + operations.length + " operations; the image is drawn on repeatedly without a full opaque fill");
		}
		return operations;
	}

	/**
	 * Appends the replay of {@code image} to {@code sink}: a group that maps the source rectangle of the image onto
	 * the destination rectangle (with {@code transform}, the drawing GC's current transform) and clips to it.
	 */
	static void replay (GCAdapter sink, float[] transform, Image image, Rectangle src, Rectangle dest) {
		float sx = dest.width / (float) src.width;
		float sy = dest.height / (float) src.height;
		float[] local = {sx, 0, 0, sy, dest.x - src.x * sx, dest.y - src.y * sy};
		sink.addGCOperation (new M3GCOperation.BeginReplay (M3GradientMath.multiply (transform, local), src.x, src.y, src.width, src.height));
		appendBalanced (sink, operations (image));
		sink.addGCOperation (new M3GCOperation.EndReplay ());
	}

	/** {@code copyArea (target, x, y)} from a recorded image: the target shows the source moved by (-x, -y). */
	static void copy (Image source, Image target, int x, int y) {
		GCOperation[] operations = operations (source);
		Entry entry = LISTS.computeIfAbsent (target, M3DisplayLists::newEntry);
		Rectangle size = target.getBounds ();
		entry.list.clearGCOperations ();
		entry.list.addGCOperation (new M3GCOperation.BeginReplay (new float[] {1, 0, 0, 1, -x, -y}, x, y, size.width, size.height));
		appendBalanced (entry.list, operations);
		entry.list.addGCOperation (new M3GCOperation.EndReplay ());
	}

	/** Copies the operations and closes segments that are still open (a GC that is not disposed yet). */
	private static void appendBalanced (GCAdapter sink, GCOperation[] operations) {
		int open = 0;
		for (GCOperation operation : operations) {
			if (operation instanceof M3GCOperation.BeginReplay) open++;
			else if (operation instanceof M3GCOperation.EndReplay) open--;
			sink.addGCOperation (operation);
		}
		for (; open > 0; open--) sink.addGCOperation (new M3GCOperation.EndReplay ());
	}

	/** Appends the GC's current colours, font, alpha and line attributes, so a segment does not depend on earlier ones. */
	static void restate (GCAdapter list, ControlGC gc) {
		list.addGCOperation (new SetProperty (SetProperty.FOREGROUND, gc.getForeground ().getRGB ()));
		list.addGCOperation (new SetProperty (SetProperty.BACKGROUND, gc.getBackground ().getRGB ()));
		FontData data = FontUtil.getData (gc.getFont ());
		FontData copy = new FontData (data.getName (), data.getHeight (), data.getStyle ());
		copy.setLocale (data.getLocale ());
		list.addGCOperation (new SetProperty (copy));
		list.addGCOperation (new SetProperty (SetProperty.ALPHA, gc.getAlpha ()));
		list.addGCOperation (new SetProperty (SetProperty.LINE_WIDTH, gc.getLineWidth ()));
		list.addGCOperation (new SetProperty (SetProperty.LINE_CAP, gc.getLineCap ()));
		list.addGCOperation (new SetProperty (SetProperty.LINE_JOIN, gc.getLineJoin ()));
		if (gc.getLineStyle () != SWT.LINE_CUSTOM) {
			list.addGCOperation (new SetProperty (SetProperty.LINE_STYLE, gc.getLineStyle ()));
		} else if (gc.getLineDash () != null) {
			list.addGCOperation (new SetProperty (SetProperty.LINE_DASH, gc.getLineDash ()));
		}
	}
}
