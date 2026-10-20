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
import org.eclipse.swt.internal.graphics.M3GCOperation;

/**
 * A pattern used while drawing: a linear gradient between two points, or an image that is tiled. Set it on a
 * {@link GC} with {@link GC#setBackgroundPattern(Pattern)} (fills) or {@link GC#setForegroundPattern(Pattern)}
 * (strokes).
 * <p>
 * Outside its two end points a gradient repeats, as on the desktop platforms. In the browser the colour stops
 * of a gradient carry no alpha; two different alpha values are therefore drawn as bands of constant alpha. See
 * {@code docs/m3/rwt/RWT_GC_COMPAT.md}.
 * </p>
 * <p>
 * Application code must explicitly invoke the {@link #dispose()} method to release a pattern it created.
 * </p>
 *
 * @see GC#setBackgroundPattern(Pattern)
 * @see GC#setForegroundPattern(Pattern)
 */
public class Pattern extends Resource {

	final int kind;
	final float x1;
	final float y1;
	final float x2;
	final float y2;
	final int argb1;
	final int argb2;
	final Image image;

	/**
	 * Constructs a new pattern that tiles the given image.
	 *
	 * @param device the device on which to allocate the pattern, or {@code null} for the current display
	 * @param image the image that the pattern draws
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the device is null and there is no current device, or the image is null</li>
	 *    <li>ERROR_INVALID_ARGUMENT - if the image has been disposed</li>
	 * </ul>
	 */
	public Pattern (Device device, Image image) {
		super (checkDevice (device));
		if (image == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		if (image.isDisposed ()) SWT.error (SWT.ERROR_INVALID_ARGUMENT);
		kind = M3GCOperation.SetPattern.IMAGE;
		x1 = y1 = x2 = y2 = 0;
		argb1 = argb2 = 0;
		this.image = image;
	}

	/**
	 * Constructs a new opaque linear gradient from (x1, y1) in colour 1 to (x2, y2) in colour 2.
	 *
	 * @param device the device on which to allocate the pattern, or {@code null} for the current display
	 * @param x1 the x coordinate of the starting corner of the gradient
	 * @param y1 the y coordinate of the starting corner of the gradient
	 * @param x2 the x coordinate of the ending corner of the gradient
	 * @param y2 the y coordinate of the ending corner of the gradient
	 * @param color1 the starting colour of the gradient
	 * @param color2 the ending colour of the gradient
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the device is null and there is no current device, or a colour is null</li>
	 *    <li>ERROR_INVALID_ARGUMENT - if a colour has been disposed</li>
	 * </ul>
	 */
	public Pattern (Device device, float x1, float y1, float x2, float y2, Color color1, Color color2) {
		this (device, x1, y1, x2, y2, color1, 0xFF, color2, 0xFF);
	}

	/**
	 * Constructs a new linear gradient from (x1, y1) in colour 1 with alpha 1 to (x2, y2) in colour 2 with alpha 2.
	 *
	 * @param device the device on which to allocate the pattern, or {@code null} for the current display
	 * @param x1 the x coordinate of the starting corner of the gradient
	 * @param y1 the y coordinate of the starting corner of the gradient
	 * @param x2 the x coordinate of the ending corner of the gradient
	 * @param y2 the y coordinate of the ending corner of the gradient
	 * @param color1 the starting colour of the gradient
	 * @param alpha1 the starting alpha value of the gradient (only the low 8 bits are used)
	 * @param color2 the ending colour of the gradient
	 * @param alpha2 the ending alpha value of the gradient (only the low 8 bits are used)
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the device is null and there is no current device, or a colour is null</li>
	 *    <li>ERROR_INVALID_ARGUMENT - if a colour has been disposed</li>
	 * </ul>
	 */
	public Pattern (Device device, float x1, float y1, float x2, float y2, Color color1, int alpha1, Color color2, int alpha2) {
		super (checkDevice (device));
		if (color1 == null || color2 == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		if (color1.isDisposed () || color2.isDisposed ()) SWT.error (SWT.ERROR_INVALID_ARGUMENT);
		kind = M3GCOperation.SetPattern.GRADIENT;
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		argb1 = pack (color1, alpha1);
		argb2 = pack (color2, alpha2);
		image = null;
	}

	static int pack (Color color, int alpha) {
		return (alpha & 0xFF) << 24 | color.getRed () << 16 | color.getGreen () << 8 | color.getBlue ();
	}

	M3GCOperation.SetPattern toOperation (boolean foreground) {
		return new M3GCOperation.SetPattern (foreground, kind, x1, y1, x2, y2, argb1, argb2, image);
	}

	/**
	 * Returns a string containing a concise, human-readable description of the receiver.
	 *
	 * @return a string representation of the receiver
	 */
	@Override
	public String toString () {
		if (isDisposed ()) return "Pattern {*DISPOSED*}";
		if (image != null) return "Pattern {image}";
		return "Pattern {" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ", #" + Integer.toHexString (argb1) + ", #" + Integer.toHexString (argb2) + "}";
	}
}
