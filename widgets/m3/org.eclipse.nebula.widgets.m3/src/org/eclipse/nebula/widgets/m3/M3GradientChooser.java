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

import java.util.Arrays;

import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.M3HiddenGate;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A gradient chooser: a gradient bar defined by colour stops at positions from 0 to 1, with stop markers below it;
 * click under the bar to add a stop, drag a marker to move it, double-click it to change its colour, Delete to remove
 * the selected stop (the SWT counterpart of SwingX <code>JXGradientChooser</code>, re-derived from its documented
 * behaviour). {@link #toLookupTable(int)} samples the gradient, for example as an {@link M3HeatMap} palette.
 * <p>
 * Stops are two sorted lanes (float position, packed RGB int) plus a lazily filled Color lane. Every change is a
 * delta: moving, recolouring, adding or removing stop i repaints only the bar and marker strip between its neighbours
 * (the two gradient segments that change); selecting repaints the old and new markers. A width change stretches the
 * whole gradient (a full repaint by nature). While hidden, changes only update the lanes and the chooser paints once
 * when shown.
 * </p>
 * <p>
 * Styles: <code>BORDER</code>. Events: <code>Modify</code> when the stops change (<code>Event.index</code> the stop),
 * <code>Selection</code> when the selected stop changes. Startup switch:
 * <code>-Dswt.widget.mode.M3GradientChooser=original</code> repaints everything on every change.
 * </p>
 */
public class M3GradientChooser extends Canvas {

	static final boolean VIEWPORT = WidgetMode.of("M3GradientChooser", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final int PAD = 6, BAR = 24, MARK = 10;

	private final M3HiddenGate gate;
	private float[] positions = { 0f, 1f };
	private int[] colors = { 0x000000, 0xffffff };
	private Color[] swatches = new Color[2];
	private int count = 2, selected = -1, dragging = -1;
	private final int[] triangle = new int[6];

	/**
	 * Creates a chooser with a black-to-white gradient.
	 *
	 * @param parent the parent
	 * @param style BORDER
	 */
	public M3GradientChooser(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);
		gate = new M3HiddenGate(this, null);
		addListener(SWT.Paint, this::paint);
		addListener(SWT.MouseDown, this::mouseDown);
		addListener(SWT.MouseMove, e -> {
			if (dragging >= 0 && (e.stateMask & SWT.BUTTON1) != 0) {
				moveStop(dragging, positionAt(e.x));
			}
		});
		addListener(SWT.MouseUp, e -> dragging = -1);
		addListener(SWT.MouseDoubleClick, e -> {
			int stop = stopAt(e.x, e.y);
			if (stop >= 0) {
				ColorDialog dialog = new ColorDialog(getShell());
				dialog.setRGB(getStopColor(stop));
				RGB rgb = dialog.open();
				if (rgb != null) {
					setStopColor(stop, rgb);
				}
			}
		});
		addListener(SWT.KeyDown, e -> {
			if ((e.keyCode == SWT.DEL || e.keyCode == SWT.BS) && selected >= 0 && count > 2) {
				removeStop(selected);
			}
		});
		addListener(SWT.Traverse, e -> e.doit = e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS);
		addListener(SWT.Dispose, e -> disposeSwatches(0, swatches.length));
	}

	/**
	 * Replaces all stops.
	 *
	 * @param stopPositions positions from 0 to 1, ascending
	 * @param stopColors one colour per position
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if an array or colour is null; ERROR_INVALID_ARGUMENT -
	 *                if fewer than 2 stops, lengths differ, or positions are not ascending within 0..1
	 */
	public void setStops(float[] stopPositions, RGB[] stopColors) {
		checkWidget();
		if (stopPositions == null || stopColors == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (stopPositions.length < 2 || stopPositions.length != stopColors.length) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		for (int i = 0; i < stopPositions.length; i++) {
			if (stopColors[i] == null) {
				SWT.error(SWT.ERROR_NULL_ARGUMENT);
			}
			if (!(stopPositions[i] >= 0 && stopPositions[i] <= 1) || i > 0 && stopPositions[i] < stopPositions[i - 1]) {
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}
		}
		disposeSwatches(0, swatches.length);
		count = stopPositions.length;
		positions = stopPositions.clone();
		colors = new int[count];
		swatches = new Color[count];
		for (int i = 0; i < count; i++) {
			colors[i] = pack(stopColors[i]);
		}
		selected = -1;
		if (showing()) {
			M3Counters.fullRepaints++;
			redraw();
		}
		modified(-1);
	}

	/** @return the number of stops (at least 2) */
	public int getStopCount() {
		checkWidget();
		return count;
	}

	/** @return a stop's position from 0 to 1 */
	public float getStopPosition(int stop) {
		checkStop(stop);
		return positions[stop];
	}

	/** @return a stop's colour */
	public RGB getStopColor(int stop) {
		checkStop(stop);
		return unpack(colors[stop]);
	}

	/**
	 * Adds a stop at a position (after any stop at the same position).
	 *
	 * @return the index of the new stop
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if the position is outside 0..1; ERROR_NULL_ARGUMENT
	 *                - if the colour is null
	 */
	public int addStop(float position, RGB color) {
		checkWidget();
		if (!(position >= 0 && position <= 1)) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		if (color == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		int at = 0;
		while (at < count && positions[at] <= position) {
			at++;
		}
		if (count == positions.length) {
			int n = count * 2;
			positions = Arrays.copyOf(positions, n);
			colors = Arrays.copyOf(colors, n);
			swatches = Arrays.copyOf(swatches, n);
		}
		System.arraycopy(positions, at, positions, at + 1, count - at);
		System.arraycopy(colors, at, colors, at + 1, count - at);
		System.arraycopy(swatches, at, swatches, at + 1, count - at);
		positions[at] = position;
		colors[at] = pack(color);
		swatches[at] = null;
		count++;
		if (selected >= at) {
			selected++;
		}
		damageAround(at, position);
		modified(at);
		return at;
	}

	/**
	 * Removes a stop.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if not a stop; ERROR_INVALID_ARGUMENT - if only two stops
	 *                are left
	 */
	public void removeStop(int stop) {
		checkStop(stop);
		if (count <= 2) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		float position = positions[stop];
		disposeSwatches(stop, stop + 1);
		System.arraycopy(positions, stop + 1, positions, stop, count - stop - 1);
		System.arraycopy(colors, stop + 1, colors, stop, count - stop - 1);
		System.arraycopy(swatches, stop + 1, swatches, stop, count - stop - 1);
		count--;
		swatches[count] = null;
		if (selected == stop) {
			selected = -1;
		} else if (selected > stop) {
			selected--;
		}
		// the merged segment spans the removed stop's neighbours, which now sit at stop - 1 and stop
		damageRange(stop > 0 ? positions[stop - 1] : 0f, stop < count ? positions[stop] : 1f, position);
		modified(stop);
	}

	/**
	 * Moves a stop, clamped between its neighbours so the order stays.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if not a stop
	 */
	public void moveStop(int stop, float position) {
		checkStop(stop);
		float low = stop > 0 ? positions[stop - 1] : 0f, high = stop < count - 1 ? positions[stop + 1] : 1f;
		float next = Math.max(low, Math.min(high, position));
		if (next == positions[stop]) {
			return;
		}
		float old = positions[stop];
		positions[stop] = next;
		damageRange(low, high, old);
		modified(stop);
	}

	/**
	 * Changes a stop's colour.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if not a stop; ERROR_NULL_ARGUMENT - if null
	 */
	public void setStopColor(int stop, RGB color) {
		checkStop(stop);
		if (color == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		int packed = pack(color);
		if (packed == colors[stop]) {
			return;
		}
		colors[stop] = packed;
		disposeSwatches(stop, stop + 1);
		damageAround(stop, positions[stop]);
		modified(stop);
	}

	/** Selects a stop (its marker is drawn filled), or -1 for none; repaints the old and new markers. */
	public void setSelectedStop(int stop) {
		checkWidget();
		if (stop < -1 || stop >= count) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
		if (stop == selected) {
			return;
		}
		int old = selected;
		selected = stop;
		repaintMarker(old);
		repaintMarker(stop);
		notifyListeners(SWT.Selection, new Event());
	}

	/** @return the selected stop, or -1 */
	public int getSelectedStop() {
		checkWidget();
		return selected;
	}

	/**
	 * @return the gradient colour at a position, as 0xRRGGBB (linear between the enclosing stops)
	 */
	public int colorAt(float position) {
		checkWidget();
		if (position <= positions[0]) {
			return colors[0];
		}
		for (int i = 1; i < count; i++) {
			if (position <= positions[i]) {
				float span = positions[i] - positions[i - 1];
				float t = span <= 0 ? 1f : (position - positions[i - 1]) / span;
				return mix(colors[i - 1], colors[i], t);
			}
		}
		return colors[count - 1];
	}

	/**
	 * Samples the gradient at evenly spaced positions.
	 *
	 * @param size the number of samples, at least 2
	 * @return packed 0xRRGGBB colours
	 */
	public int[] toLookupTable(int size) {
		checkWidget();
		if (size < 2) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		int[] table = new int[size];
		for (int i = 0; i < size; i++) {
			table[i] = colorAt(i / (float) (size - 1));
		}
		return table;
	}

	/** @return the gradient bar in client coordinates */
	public Rectangle getBarBounds() {
		checkWidget();
		Rectangle client = getClientArea();
		return new Rectangle(client.x + PAD, client.y + PAD, Math.max(1, client.width - 2 * PAD), BAR);
	}

	/** @return the client x of a stop position */
	public int xOf(float position) {
		checkWidget();
		Rectangle bar = getBarBounds();
		return bar.x + Math.round(position * (bar.width - 1));
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		Rectangle trim = computeTrim(0, 0, wHint == SWT.DEFAULT ? 240 : wHint, hHint == SWT.DEFAULT ? 2 * PAD + BAR + MARK + 2 : hHint);
		return new Point(trim.width, trim.height);
	}

	private void checkStop(int stop) {
		checkWidget();
		if (stop < 0 || stop >= count) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
	}

	private static int pack(RGB rgb) {
		return (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
	}

	private static RGB unpack(int rgb) {
		return new RGB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
	}

	private static int mix(int a, int b, float t) {
		int red = Math.round(((a >> 16) & 0xff) * (1 - t) + ((b >> 16) & 0xff) * t);
		int green = Math.round(((a >> 8) & 0xff) * (1 - t) + ((b >> 8) & 0xff) * t);
		int blue = Math.round((a & 0xff) * (1 - t) + (b & 0xff) * t);
		return (red << 16) | (green << 8) | blue;
	}

	private float positionAt(int x) {
		Rectangle bar = getBarBounds();
		return Math.max(0f, Math.min(1f, (x - bar.x) / (float) Math.max(1, bar.width - 1)));
	}

	private int stopAt(int x, int y) {
		Rectangle bar = getBarBounds();
		if (y < bar.y + bar.height || y > bar.y + bar.height + MARK + 2) {
			return -1;
		}
		int best = -1, distance = MARK;
		for (int i = 0; i < count; i++) {
			int d = Math.abs(xOf(positions[i]) - x);
			if (d <= distance) {
				best = i;
				distance = d;
			}
		}
		return best;
	}

	private void mouseDown(Event event) {
		if (event.button != 1) {
			return;
		}
		forceFocus();
		int stop = stopAt(event.x, event.y);
		Rectangle bar = getBarBounds();
		if (stop < 0 && event.y >= bar.y + bar.height && event.y <= bar.y + bar.height + MARK + 2) {
			float position = positionAt(event.x);
			stop = addStop(position, unpack(colorAt(position)));
		}
		if (stop >= 0) {
			setSelectedStop(stop);
			dragging = stop;
		}
	}

	private void modified(int stop) {
		Event event = new Event();
		event.index = stop;
		notifyListeners(SWT.Modify, event);
	}

	private boolean showing() {
		return !VIEWPORT || gate.probe();
	}

	/* the segments touching a stop change: from its left neighbour to its right neighbour */
	private void damageAround(int stop, float extra) {
		damageRange(stop > 0 ? positions[stop - 1] : 0f, stop < count - 1 ? positions[stop + 1] : 1f, extra);
	}

	/* repaints the bar and marker strip between two positions (widened by an old position and the marker size) */
	private void damageRange(float from, float to, float extra) {
		if (!showing()) {
			return;
		}
		if (!VIEWPORT) {
			M3Counters.fullRepaints++;
			redraw();
			return;
		}
		float low = Math.min(from, extra), high = Math.max(to, extra);
		Rectangle bar = getBarBounds();
		int x0 = low <= 0 ? 0 : xOf(low) - MARK, x1 = high >= 1 ? getClientArea().width : xOf(high) + MARK + 1;
		redraw(x0, bar.y, x1 - x0, bar.height + MARK + 3, false);
	}

	private void repaintMarker(int stop) {
		if (stop < 0 || !showing()) {
			return;
		}
		Rectangle bar = getBarBounds();
		int x = xOf(positions[stop]);
		if (VIEWPORT) {
			redraw(x - MARK, bar.y + bar.height, 2 * MARK + 1, MARK + 3, false);
		} else {
			M3Counters.fullRepaints++;
			redraw();
		}
	}

	private Color swatch(int stop) {
		if (swatches[stop] == null) {
			int rgb = colors[stop];
			swatches[stop] = new Color(getDisplay(), (rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
		}
		return swatches[stop];
	}

	private void disposeSwatches(int from, int to) {
		for (int i = from; i < to && i < swatches.length; i++) {
			if (swatches[i] != null) {
				swatches[i].dispose();
				swatches[i] = null;
			}
		}
	}

	private void paint(Event event) {
		GC gc = event.gc;
		gc.setBackground(getBackground());
		gc.fillRectangle(event.x, event.y, event.width, event.height);
		Rectangle bar = getBarBounds();
		int clipLeft = event.x, clipRight = event.x + event.width;
		int first = xOf(positions[0]), last = xOf(positions[count - 1]);
		if (first > bar.x && clipLeft < first) {
			gc.setBackground(swatch(0));
			gc.fillRectangle(bar.x, bar.y, first - bar.x, bar.height);
		}
		for (int i = 0; i + 1 < count; i++) {
			int x0 = xOf(positions[i]), x1 = xOf(positions[i + 1]);
			if (x1 <= x0 || x1 < clipLeft || x0 > clipRight) {
				continue;
			}
			gc.setForeground(swatch(i));
			gc.setBackground(swatch(i + 1));
			gc.fillGradientRectangle(x0, bar.y, x1 - x0 + 1, bar.height, false);
		}
		if (last < bar.x + bar.width - 1 && clipRight > last) {
			gc.setBackground(swatch(count - 1));
			gc.fillRectangle(last + 1, bar.y, bar.x + bar.width - 1 - last, bar.height);
		}
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		gc.drawRectangle(bar.x - 1, bar.y - 1, bar.width + 1, bar.height + 1);
		int top = bar.y + bar.height + 2;
		for (int i = 0; i < count; i++) {
			int x = xOf(positions[i]);
			if (x + MARK < clipLeft || x - MARK > clipRight) {
				continue;
			}
			triangle[0] = x;
			triangle[1] = top;
			triangle[2] = x - MARK / 2;
			triangle[3] = top + MARK;
			triangle[4] = x + MARK / 2;
			triangle[5] = top + MARK;
			gc.setBackground(i == selected ? getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION) : swatch(i));
			gc.fillPolygon(triangle);
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
			gc.drawPolygon(triangle);
		}
	}
}
