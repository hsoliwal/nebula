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
package org.eclipse.swt.internal.graphics;

import org.eclipse.swt.graphics.Image;

/**
 * GC operations that RWT 4.8.0 does not have: patterns, the fill rule and the replay of a recorded image.
 * They travel through {@link GCAdapter} like RWT's own operations and are written by the canvas operation
 * writer. Design: docs/m3/rwt/RWT_GC_COMPAT.md.
 */
public abstract class M3GCOperation extends GCOperation {

	M3GCOperation () {
	}

	/**
	 * Sets or clears the background (fill) or foreground (stroke) pattern. Colours are packed as
	 * {@code alpha << 24 | red << 16 | green << 8 | blue}.
	 */
	public static final class SetPattern extends M3GCOperation {
		/** No pattern: fills and strokes use the solid colours again. */
		public static final int NONE = 0;
		/** Linear gradient from (x1, y1) with colour 1 to (x2, y2) with colour 2, repeated outside. */
		public static final int GRADIENT = 1;
		/** The image tiled from (0, 0). */
		public static final int IMAGE = 2;

		public final boolean foreground;
		public final int kind;
		public final float x1;
		public final float y1;
		public final float x2;
		public final float y2;
		public final int argb1;
		public final int argb2;
		public final Image image;

		public SetPattern (boolean foreground, int kind, float x1, float y1, float x2, float y2, int argb1, int argb2, Image image) {
			this.foreground = foreground;
			this.kind = kind;
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.argb1 = argb1;
			this.argb2 = argb2;
			this.image = image;
		}

		/** @return a operation that clears the pattern of the given side */
		public static SetPattern none (boolean foreground) {
			return new SetPattern (foreground, NONE, 0, 0, 0, 0, 0, 0, null);
		}
	}

	/** Sets the fill rule for polygon and path fills: even-odd (SWT's default) or non-zero winding. */
	public static final class SetFillRule extends M3GCOperation {
		public final boolean evenOdd;

		public SetFillRule (boolean evenOdd) {
			this.evenOdd = evenOdd;
		}
	}

	/**
	 * Starts the replay of a recorded image: the operations up to the matching {@link EndReplay} are drawn with
	 * {@code transform} (absolute, already composed with the target's transform) and clipped to the source
	 * rectangle of the image, in image coordinates.
	 */
	public static final class BeginReplay extends M3GCOperation {
		public final float[] transform;
		public final int clipX;
		public final int clipY;
		public final int clipWidth;
		public final int clipHeight;

		public BeginReplay (float[] transform, int clipX, int clipY, int clipWidth, int clipHeight) {
			this.transform = transform;
			this.clipX = clipX;
			this.clipY = clipY;
			this.clipWidth = clipWidth;
			this.clipHeight = clipHeight;
		}
	}

	/** Ends the innermost {@link BeginReplay}; the target's own drawing state applies again. */
	public static final class EndReplay extends M3GCOperation {
		public EndReplay () {
		}
	}
}
