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

import java.util.ArrayDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.rap.json.JsonArray;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.graphics.FontUtil;
import org.eclipse.swt.internal.graphics.GCOperation;
import org.eclipse.swt.internal.graphics.GCOperation.DrawArc;
import org.eclipse.swt.internal.graphics.GCOperation.DrawLine;
import org.eclipse.swt.internal.graphics.GCOperation.DrawPath;
import org.eclipse.swt.internal.graphics.GCOperation.DrawPolyline;
import org.eclipse.swt.internal.graphics.GCOperation.DrawRectangle;
import org.eclipse.swt.internal.graphics.GCOperation.FillGradientRectangle;
import org.eclipse.swt.internal.graphics.GCOperation.SetClipping;
import org.eclipse.swt.internal.graphics.GCOperation.SetProperty;
import org.eclipse.swt.internal.graphics.GCOperation.SetTransform;
import org.eclipse.swt.internal.graphics.ImageFactory;
import org.eclipse.swt.internal.graphics.M3GCOperation;
import org.eclipse.swt.widgets.Control;

/**
 * The part of the canvas operation writer that handles the operations added for M3: patterns, the fill rule and
 * the replay of recorded images. It keeps the drawing state the client has been told (one operation per property)
 * so that it can restore the client's tracked state after a {@code save}/{@code restore} group. All arithmetic lives
 * in {@link M3GradientMath}. Design: docs/m3/rwt/RWT_GC_COMPAT.md.
 */
final class M3GCWriterSupport {

	private static final Logger LOG = Logger.getLogger ("org.eclipse.swt.internal.widgets.canvaskit");
	private static final GCOperation[] NONE = new GCOperation[0];
	private static final int PROPERTIES = SetProperty.FONT + 1;
	/** Image-pattern fills with more tiles than this are filled with the background colour instead. */
	static final int MAX_TILES = 1024;

	private static final class Frame {
		final float[] base;
		final int alpha;
		final GCOperation[] outer;
		final SetTransform outerTransform;
		final M3GCOperation.SetPattern outerFill;
		final M3GCOperation.SetPattern outerStroke;
		final boolean outerEvenOdd;
		boolean clipOpen;

		Frame (float[] base, int alpha, GCOperation[] outer, SetTransform outerTransform, M3GCOperation.SetPattern outerFill, M3GCOperation.SetPattern outerStroke, boolean outerEvenOdd) {
			this.base = base;
			this.alpha = alpha;
			this.outer = outer;
			this.outerTransform = outerTransform;
			this.outerFill = outerFill;
			this.outerStroke = outerStroke;
			this.outerEvenOdd = outerEvenOdd;
		}
	}

	private final Control control;
	private final GCOperation[] current = new GCOperation[PROPERTIES];
	private SetTransform transform;
	private M3GCOperation.SetPattern fillPattern;
	private M3GCOperation.SetPattern strokePattern;
	private boolean evenOdd = true;
	private final ArrayDeque<Frame> frames = new ArrayDeque<> ();
	/** Operations this class returned for the writer to write; they pass through unchanged when they come back. */
	private final ArrayDeque<GCOperation> own = new ArrayDeque<> ();
	private M3GCOperation.SetPattern shapePattern;
	private float[] bands;

	M3GCWriterSupport (Control control) {
		this.control = control;
	}

	/**
	 * @return {@code null} if the writer writes the operation itself; otherwise the operations to write instead
	 *         (possibly none)
	 */
	GCOperation[] handle (GCOperation operation, JsonArray out) {
		if (!own.isEmpty () && own.peekFirst () == operation) {
			own.pollFirst ();
			observe (operation);
			return null;
		}
		if (operation instanceof M3GCOperation) return handleM3 ((M3GCOperation) operation, out);
		Frame frame = frames.peekFirst ();
		if (frame != null && operation instanceof SetTransform) {
			return mine (new SetTransform (M3GradientMath.multiply (frame.base, ((SetTransform) operation).elements)));
		}
		if (frame != null && operation instanceof SetProperty && ((SetProperty) operation).id == SetProperty.ALPHA) {
			int alpha = ((Integer) ((SetProperty) operation).value).intValue ();
			return mine (new SetProperty (SetProperty.ALPHA, (alpha * frame.alpha + 127) / 255));
		}
		if (frame != null && operation instanceof SetClipping) frame.clipOpen = !((SetClipping) operation).isReset ();
		observe (operation);
		return null;
	}

	private GCOperation[] handleM3 (M3GCOperation operation, JsonArray out) {
		if (operation instanceof M3GCOperation.SetPattern) {
			M3GCOperation.SetPattern pattern = (M3GCOperation.SetPattern) operation;
			M3GCOperation.SetPattern kept = pattern.kind == M3GCOperation.SetPattern.NONE ? null : pattern;
			if (pattern.foreground) strokePattern = kept; else fillPattern = kept;
		} else if (operation instanceof M3GCOperation.SetFillRule) {
			evenOdd = ((M3GCOperation.SetFillRule) operation).evenOdd;
		} else if (operation instanceof M3GCOperation.BeginReplay) {
			beginReplay ((M3GCOperation.BeginReplay) operation, out);
		} else if (operation instanceof M3GCOperation.EndReplay) {
			return endReplay (out);
		}
		return NONE;
	}

	private void beginReplay (M3GCOperation.BeginReplay operation, JsonArray out) {
		Frame enclosing = frames.peekFirst ();
		float[] base = enclosing == null ? operation.transform : M3GradientMath.multiply (enclosing.base, operation.transform);
		frames.push (new Frame (base, alpha (), current.clone (), transform, fillPattern, strokePattern, evenOdd));
		fillPattern = null;
		strokePattern = null;
		evenOdd = true;
		out.add (new JsonArray ().add ("save"));
		JsonArray set = new JsonArray ().add ("setTransform");
		for (float element : base) set.add (element);
		out.add (set);
		transform = new SetTransform (base);
		out.add (new JsonArray ().add ("beginPath"));
		out.add (new JsonArray ().add ("rect").add (operation.clipX).add (operation.clipY).add (operation.clipWidth).add (operation.clipHeight));
		out.add (new JsonArray ().add ("clip"));
	}

	private GCOperation[] endReplay (JsonArray out) {
		Frame frame = frames.poll ();
		if (frame == null) return NONE;
		if (frame.clipOpen) out.add (new JsonArray ().add ("restore"));
		out.add (new JsonArray ().add ("restore"));
		fillPattern = frame.outerFill;
		strokePattern = frame.outerStroke;
		evenOdd = frame.outerEvenOdd;
		ArrayDeque<GCOperation> resync = new ArrayDeque<> ();
		for (int id = 0; id < PROPERTIES; id++) {
			if (current[id] != frame.outer[id]) resync.add (frame.outer[id] != null ? frame.outer[id] : initial (id));
		}
		if (transform != frame.outerTransform) resync.add (frame.outerTransform != null ? frame.outerTransform : new SetTransform (new float[] {1, 0, 0, 1, 0, 0}));
		for (GCOperation operation : resync) own.add (operation);
		return resync.toArray (NONE);
	}

	/**
	 * Starts a drawing operation: when a pattern applies, writes the gradient (or the clip group of an image
	 * pattern) and returns how many times the writer must write the operation (alpha bands); otherwise 1.
	 */
	int beginShape (GCOperation operation, JsonArray out) {
		shapePattern = patternFor (operation);
		bands = null;
		if (shapePattern == null) return 1;
		float[] box = bounds (operation);
		out.add (new JsonArray ().add ("save"));
		if (shapePattern.kind == M3GCOperation.SetPattern.IMAGE) return 1;
		float[] gradient = M3GradientMath.gradient (shapePattern.x1, shapePattern.y1, shapePattern.x2, shapePattern.y2, box);
		JsonArray create = new JsonArray ().add ("createLinearGradient");
		for (int i = 0; i < 4; i++) create.add (gradient[i]);
		out.add (create);
		float[] stops = M3GradientMath.stops ((int) gradient[4]);
		for (int i = 0; i < stops.length; i += 2) {
			out.add (new JsonArray ().add ("addColorStop").add (stops[i]).add (rgb (stops[i + 1] == 0 ? shapePattern.argb1 : shapePattern.argb2)));
		}
		out.add (new JsonArray ().add (shapePattern.foreground ? "strokeStyle" : "fillStyle").add ("linearGradient"));
		int alpha1 = shapePattern.argb1 >>> 24, alpha2 = shapePattern.argb2 >>> 24;
		if (alpha1 == alpha2) {
			if (alpha1 != 255) out.add (new JsonArray ().add ("globalAlpha").add (M3GradientMath.globalAlpha (alpha (), alpha1)));
			return 1;
		}
		bands = M3GradientMath.bands (gradient, box, alpha1, alpha2);
		return bands.length / M3GradientMath.BAND_FLOATS;
	}

	void beginPass (int pass, JsonArray out) {
		if (bands == null) return;
		int b = pass * M3GradientMath.BAND_FLOATS;
		out.add (new JsonArray ().add ("save"));
		out.add (new JsonArray ().add ("beginPath"));
		out.add (new JsonArray ().add ("moveTo").add (bands[b]).add (bands[b + 1]));
		for (int i = 2; i < 8; i += 2) out.add (new JsonArray ().add ("lineTo").add (bands[b + i]).add (bands[b + i + 1]));
		out.add (new JsonArray ().add ("closePath"));
		out.add (new JsonArray ().add ("clip"));
		out.add (new JsonArray ().add ("globalAlpha").add (M3GradientMath.globalAlpha (alpha (), (int) bands[b + 8])));
	}

	/** Ends one pass: applies the fill rule and, for an image pattern, turns the fill into a clip and tiles the image. */
	void endPass (int pass, int start, GCOperation operation, JsonArray out) {
		if (isFill (operation) && (operation instanceof DrawPolyline || operation instanceof DrawPath) && evenOdd) {
			int index = lastIndexOf (out, start, "fill");
			if (index >= 0) out.set (index, new JsonArray ().add ("fill").add ("evenodd"));
		}
		if (shapePattern != null && shapePattern.kind == M3GCOperation.SetPattern.IMAGE) tile (start, operation, out);
		if (bands != null) out.add (new JsonArray ().add ("restore"));
	}

	/** Ends the operation; returns the operations that restore the client's tracked state after the pattern group. */
	GCOperation[] endShape (GCOperation operation, JsonArray out) {
		if (shapePattern == null) return NONE;
		out.add (new JsonArray ().add ("restore"));
		int colour = shapePattern.foreground ? SetProperty.FOREGROUND : SetProperty.BACKGROUND;
		shapePattern = null;
		bands = null;
		return mine (current (colour), current (SetProperty.ALPHA));
	}

	private void tile (int start, GCOperation operation, JsonArray out) {
		Image image = shapePattern.image;
		Rectangle size = image.getBounds ();
		float[] box = bounds (operation);
		int[] grid = M3GradientMath.tiles (box, size.width, size.height);
		long count = (long) grid[2] * grid[3];
		int index = lastIndexOf (out, start, "fill");
		if (index < 0) return;
		if (count > MAX_TILES) {
			LOG.log (Level.FINE, "image pattern needs {0} tiles; filled with the background colour", count);
			return;
		}
		JsonArray tail = new JsonArray ();
		for (int i = index + 1; i < out.size (); i++) tail.add (out.get (i));
		JsonArray clip = new JsonArray ().add ("clip");
		if ((operation instanceof DrawPolyline || operation instanceof DrawPath) && evenOdd) clip.add ("evenodd");
		out.set (index, clip);
		JsonArray insert = new JsonArray ();
		String path = ImageFactory.getImagePath (image);
		for (int row = 0; row < grid[3]; row++) {
			for (int column = 0; column < grid[2]; column++) {
				insert.add (new JsonArray ().add ("drawImage").add (path).add ((grid[0] + column) * size.width).add ((grid[1] + row) * size.height));
			}
		}
		for (int i = 0; i < tail.size (); i++) insert.add (tail.get (i));
		int position = index + 1;
		for (int i = 0; i < insert.size (); i++, position++) {
			if (position < out.size ()) out.set (position, insert.get (i)); else out.add (insert.get (i));
		}
	}

	private M3GCOperation.SetPattern patternFor (GCOperation operation) {
		if (operation instanceof FillGradientRectangle) return null;
		if (isFill (operation)) return fillPattern;
		if (isStroke (operation)) return strokePattern != null && strokePattern.kind == M3GCOperation.SetPattern.GRADIENT ? strokePattern : null;
		return null;
	}

	static boolean isFill (GCOperation operation) {
		if (operation instanceof DrawRectangle) return ((DrawRectangle) operation).fill;
		if (operation instanceof DrawArc) return ((DrawArc) operation).fill;
		if (operation instanceof DrawPolyline) return ((DrawPolyline) operation).fill;
		if (operation instanceof DrawPath) return ((DrawPath) operation).fill;
		return false;
	}

	static boolean isStroke (GCOperation operation) {
		if (operation instanceof DrawLine) return true;
		return (operation instanceof DrawRectangle || operation instanceof DrawArc || operation instanceof DrawPolyline || operation instanceof DrawPath) && !isFill (operation);
	}

	/** Bounds of a drawing operation as {x0, y0, x1, y1}; strokes grow by half the line width plus one pixel. */
	float[] bounds (GCOperation operation) {
		float[] box;
		if (operation instanceof DrawRectangle) {
			DrawRectangle r = (DrawRectangle) operation;
			box = new float[] {r.x, r.y, r.x + r.width, r.y + r.height};
		} else if (operation instanceof DrawArc) {
			DrawArc a = (DrawArc) operation;
			box = new float[] {a.x, a.y, a.x + a.width, a.y + a.height};
		} else if (operation instanceof DrawLine) {
			DrawLine l = (DrawLine) operation;
			box = new float[] {Math.min (l.x1, l.x2), Math.min (l.y1, l.y2), Math.max (l.x1, l.x2), Math.max (l.y1, l.y2)};
		} else if (operation instanceof DrawPolyline) {
			box = M3GradientMath.extent (((DrawPolyline) operation).points);
		} else {
			box = M3GradientMath.extent (((DrawPath) operation).points);
		}
		if (isStroke (operation)) {
			float grow = lineWidth () / 2f + 1;
			box[0] -= grow;
			box[1] -= grow;
			box[2] += grow;
			box[3] += grow;
		}
		return box;
	}

	private void observe (GCOperation operation) {
		if (operation instanceof SetProperty) {
			SetProperty property = (SetProperty) operation;
			int id = property.id == SetProperty.LINE_DASH ? SetProperty.LINE_STYLE : property.id;
			if (id >= 0 && id < PROPERTIES) current[id] = operation;
		} else if (operation instanceof SetTransform) {
			transform = (SetTransform) operation;
		}
	}

	private GCOperation[] mine (GCOperation... operations) {
		for (GCOperation operation : operations) own.add (operation);
		return operations;
	}

	private GCOperation current (int id) {
		return current[id] != null ? current[id] : initial (id);
	}

	private int alpha () {
		GCOperation operation = current[SetProperty.ALPHA];
		return operation == null ? 255 : ((Integer) ((SetProperty) operation).value).intValue ();
	}

	private int lineWidth () {
		GCOperation operation = current[SetProperty.LINE_WIDTH];
		return operation == null ? 1 : Math.max (1, ((Integer) ((SetProperty) operation).value).intValue ());
	}

	/** The state the writer's {@code init} call gives the client, as operations. */
	private GCOperation initial (int id) {
		switch (id) {
			case SetProperty.FOREGROUND: return new SetProperty (SetProperty.FOREGROUND, control.getForeground ().getRGB ());
			case SetProperty.BACKGROUND: return new SetProperty (SetProperty.BACKGROUND, control.getBackground ().getRGB ());
			case SetProperty.ALPHA: return new SetProperty (SetProperty.ALPHA, 255);
			case SetProperty.LINE_WIDTH: return new SetProperty (SetProperty.LINE_WIDTH, 1);
			case SetProperty.LINE_CAP: return new SetProperty (SetProperty.LINE_CAP, SWT.CAP_FLAT);
			case SetProperty.LINE_JOIN: return new SetProperty (SetProperty.LINE_JOIN, SWT.JOIN_MITER);
			case SetProperty.LINE_STYLE: return new SetProperty (SetProperty.LINE_STYLE, SWT.LINE_SOLID);
			default: return new SetProperty (FontUtil.getData (control.getFont ()));
		}
	}

	private static JsonArray rgb (int argb) {
		return new JsonArray ().add (argb >>> 16 & 0xFF).add (argb >>> 8 & 0xFF).add (argb & 0xFF).add (255);
	}

	private static int lastIndexOf (JsonArray out, int start, String name) {
		for (int i = out.size () - 1; i >= start; i--) {
			JsonArray operation = out.get (i).asArray ();
			if (name.equals (operation.get (0).asString ())) return i;
		}
		return -1;
	}
}
