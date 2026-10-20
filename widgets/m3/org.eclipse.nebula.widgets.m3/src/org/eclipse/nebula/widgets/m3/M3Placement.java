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
package org.eclipse.nebula.widgets.m3;

/**
 * Toolkit-neutral placement of a floating box (popup, hover card, popover) next to an anchor rectangle inside bounding
 * rectangle (the monitor's client area): the preferred side, flipped to the opposite side when the box does not fit
 * and the opposite side has more room, then shifted along the cross axis to stay inside the bounds (the placement,
 * flip and shift steps of Floating UI, MIT, re-derived as plain integer arithmetic). No SWT or AWT types, no
 * allocation: SWT ({@link M3InfoPopup}) and Swing share it.
 */
public final class M3Placement {

	/** Side: below the anchor. */
	public static final int BOTTOM = 0;
	/** Side: above the anchor. */
	public static final int TOP = 1;
	/** Side: right of the anchor. */
	public static final int RIGHT = 2;
	/** Side: left of the anchor. */
	public static final int LEFT = 3;

	private M3Placement() {
	}

	/**
	 * Places a box.
	 *
	 * @param ax anchor x
	 * @param ay anchor y
	 * @param aw anchor width
	 * @param ah anchor height
	 * @param width box width
	 * @param height box height
	 * @param bx bounds x
	 * @param by bounds y
	 * @param bw bounds width
	 * @param bh bounds height
	 * @param side the preferred side: BOTTOM, TOP, RIGHT or LEFT
	 * @param gap the distance between anchor and box
	 * @param out receives x, y and the side used (length at least 3)
	 */
	public static void place(int ax, int ay, int aw, int ah, int width, int height, int bx, int by, int bw, int bh, int side, int gap,
			int[] out) {
		boolean vertical = side == BOTTOM || side == TOP;
		int used = side;
		if (vertical) {
			int below = by + bh - (ay + ah + gap), above = ay - gap - by;
			if (side == BOTTOM && height > below && above > below) {
				used = TOP;
			} else if (side == TOP && height > above && below > above) {
				used = BOTTOM;
			}
			out[1] = used == BOTTOM ? ay + ah + gap : ay - gap - height;
			out[0] = shift(ax, width, bx, bw);
			out[1] = clamp(out[1], height, by, bh);
		} else {
			int right = bx + bw - (ax + aw + gap), left = ax - gap - bx;
			if (side == RIGHT && width > right && left > right) {
				used = LEFT;
			} else if (side == LEFT && width > left && right > left) {
				used = RIGHT;
			}
			out[0] = used == RIGHT ? ax + aw + gap : ax - gap - width;
			out[1] = shift(ay, height, by, bh);
			out[0] = clamp(out[0], width, bx, bw);
		}
		out[2] = used;
	}

	/* start-aligned with the anchor, shifted back inside the bounds (the start edge wins when the box is larger) */
	private static int shift(int start, int size, int boundsStart, int boundsSize) {
		return clamp(start, size, boundsStart, boundsSize);
	}

	private static int clamp(int position, int size, int boundsStart, int boundsSize) {
		int max = boundsStart + boundsSize - size;
		return Math.max(boundsStart, Math.min(position, max));
	}

	/**
	 * Packs an anchor id: a kind (header, body, footer, or any small number), a row and a column.
	 *
	 * @param kind 0..255
	 * @param row any int (-1 for a header or footer)
	 * @param column 0..65535
	 * @return the packed id, never -1
	 */
	public static long anchor(int kind, int row, int column) {
		return ((long) (kind & 0xff) << 56) | ((long) (column & 0xffff) << 32) | (row & 0xffffffffL);
	}

	/** @return the kind of a packed anchor */
	public static int kindOf(long anchor) {
		return (int) (anchor >>> 56) & 0xff;
	}

	/** @return the row of a packed anchor */
	public static int rowOf(long anchor) {
		return (int) anchor;
	}

	/** @return the column of a packed anchor */
	public static int columnOf(long anchor) {
		return (int) (anchor >>> 32) & 0xffff;
	}
}
