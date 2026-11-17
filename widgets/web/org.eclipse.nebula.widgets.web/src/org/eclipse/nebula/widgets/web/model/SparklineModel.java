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
 *******************************************************************************/
package org.eclipse.nebula.widgets.web.model;

import java.util.Arrays;

import static org.eclipse.nebula.widgets.web.model.WebStates.*;

/**
 * A scrollable sparkline: each point is an integer value lane, not a child Control.
 * Only the visible points and overscan receive rectangles and display-list ink.
 * Reusable primitive arrays retain resident identities across filter changes so
 * retired points cannot remain clickable after a scroll or filter update.
 */
public class SparklineModel extends WebModel implements SparklineApi {
	private static final int POINT_SIZE = 8;
	private int[] residentIds = new int[32];
	private int[] points = new int[64];
	private int previousCount;
	private int minimum, maximum;
	private boolean rangeDirty = true;

	public SparklineModel() {
		super(CONCEPT, WebStates.role("img"), 0, RECT, V_DEFAULT);
		itemExtent = 12;
	}

	public static SparklineModel sample() {
		SparklineModel model = new SparklineModel();
		model.load(SAMPLE);
		return model;
	}

	@Override
	public int aspects() {
		// The shared SWT short-blit direction is left-to-right; RTL needs full damage.
		return A_R | A_G | (rtl ? 0 : A_D);
	}

	@Override
	public void load(String sample) {
		String[] values = sample == null || sample.isBlank() ? new String[0] : sample.split(";", -1);
		int[] parsed = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			parsed[i] = WebStates.parse(values[i]);
		}
		load(parsed);
	}

	@Override
	public void load(int[] values) {
		if (values == null) {
			throw new NullPointerException("values");
		}
		clear();
		scroll = 0;
		setLogicalCount(values.length);
		System.arraycopy(values, 0, value, 0, values.length);
		rangeDirty = true;
		relayout(-1, D_LOAD);
	}

	@Override
	public int add(String label, String detail) {
		rangeDirty = true;
		return super.add(label, detail);
	}

	@Override
	public void remove(int item) {
		rangeDirty = true;
		super.remove(item);
	}

	@Override
	public void clear() {
		rangeDirty = true;
		super.clear();
	}

	@Override
	public void setLogicalCount(int n) {
		rangeDirty = true;
		super.setLogicalCount(n);
	}

	@Override
	public void setValue(int item, int v) {
		check(item);
		int old = value[item], next = clampValue(item, v);
		if (!rangeDirty) {
			if ((old == minimum && next > old) || (old == maximum && next < old)) {
				// An old extremum moved inward; another point may own that bound.
				rangeDirty = true;
			} else {
				minimum = Math.min(minimum, next);
				maximum = Math.max(maximum, next);
			}
		}
		value[item] = next;
		relayout(item, D_VALUE);
	}

	@Override
	public int minValue() {
		ensureRange();
		return minimum;
	}

	@Override
	public int maxValue() {
		ensureRange();
		return maximum;
	}

	private void ensureRange() {
		if (!rangeDirty) {
			return;
		}
		minimum = maximum = count == 0 ? 0 : value[0];
		for (int i = 1; i < count; i++) {
			minimum = Math.min(minimum, value[i]);
			maximum = Math.max(maximum, value[i]);
		}
		rangeDirty = false;
	}

	@Override
	public String accessibleName(int item) {
		if (item < 0) {
			return "Sparkline, " + count + " points, " + minValue() + " to " + maxValue();
		}
		int index = checked(item);
		return (text[index] == null ? "Point " + (index + 1) : text[index]) + ": " + value[index];
	}

	@Override
	public int viewLength() {
		return width;
	}

	@Override
	public boolean scrollsHorizontally() {
		return true;
	}

	@Override
	protected void arrange() {
		for (int i = 0; i < previousCount; i++) {
			setRect(residentIds[i], 0, 0, 0, 0);
		}
		project();
		int n = residentCount();
		if (residentIds.length < n) {
			residentIds = Arrays.copyOf(residentIds, n);
		}
		for (int p = window[2]; p < window[3]; p++) {
			residentIds[p - window[2]] = itemAt(p);
		}
		previousCount = n;
		doLayout();
		if (rtl) {
			for (int i = 0; i < previousCount; i++) {
				int b = residentIds[i] * 4;
				rects[b] = width - rects[b] - rects[b + 2];
			}
		}
	}

	@Override
	protected void doLayout() {
		ensureRange();
		int top = 4;
		int bottom = Math.max(top, height - 5);
		long span = (long) maximum - minimum;
		for (int p = window[2]; p < window[3]; p++) {
			int item = itemAt(p);
			int y = span == 0 ? (top + bottom) / 2
					: bottom - (int) (((long) value[item] - minimum) * (bottom - top) / span);
			int x = (int) ((long) p * itemExtent - scroll) + itemExtent / 2;
			setRect(item, x - POINT_SIZE / 2, y - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
		}
	}

	@Override
	protected void emit(WebPaint paint) {
		int color = theme.slot((widgetFlags & DISABLED) != 0 ? WebTheme.MUTED : WebTheme.ACCENT);
		int needed = previousCount * 2;
		if (points.length < needed) {
			points = Arrays.copyOf(points, needed);
		}
		int segment = 0;
		for (int p = window[2]; p < window[3]; p++) {
			int item = itemAt(p);
			if ((flags[item] & HIDDEN) != 0) {
				paintSegment(paint, segment, color);
				segment = 0;
				continue;
			}
			int b = item * 4;
			points[segment * 2] = rects[b] + POINT_SIZE / 2;
			points[segment * 2 + 1] = rects[b + 1] + POINT_SIZE / 2;
			segment++;
		}
		paintSegment(paint, segment, color);
		if (focus >= 0 && focus < count && (flags[focus] & HIDDEN) == 0 && rects[focus * 4 + 2] > 0) {
			int b = focus * 4;
			paint.ovalLine(rects[b] - 2, rects[b + 1] - 2, POINT_SIZE + 4, POINT_SIZE + 4, theme.slot(WebTheme.FG));
		}
	}

	private void paintSegment(WebPaint paint, int count, int color) {
		if (count > 1) {
			paint.poly(points, 0, count, color);
		}
		if (count > 0) {
			int last = (count - 1) * 2;
			paint.oval(points[last] - 3, points[last + 1] - 3, 6, 6, color);
		}
	}

	@Override
	public int hitTest(int x, int y) {
		if (x < 0 || x >= width || y < 0 || y >= height || itemExtent <= 0) {
			return -1;
		}
		long position = (scroll + (rtl ? width - 1L - x : x)) / itemExtent;
		if (position < 0 || position >= logicalCount()) {
			return -1;
		}
		int item = itemAt((int) position);
		return (flags[item] & (DISABLED | HIDDEN)) != 0 ? -1 : item;
	}

	private int positionOf(int item) {
		int lo = 0, hi = logicalCount() - 1;
		while (lo <= hi) {
			int mid = lo + (hi - lo) / 2;
			int id = itemAt(mid);
			if (id == item) {
				return mid;
			}
			if (id < item) {
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}
		return -1;
	}

	@Override
	protected boolean moveFocus(int direction) {
		int p = focus < 0 ? (direction > 0 ? -1 : logicalCount()) : positionOf(focus);
		if (p < 0 && focus >= 0) {
			p = direction > 0 ? -1 : logicalCount();
		}
		for (p += direction; p >= 0 && p < logicalCount(); p += direction) {
			int item = itemAt(p);
			if ((flags[item] & (DISABLED | HIDDEN)) == 0) {
				setFocus(item);
				return true;
			}
		}
		return false;
	}

	@Override
	protected int nextEnabled(int from, int direction) {
		int n = logicalCount();
		int start = from < 0 ? -1 : from >= count ? n : positionOf(from);
		if (start < 0 && from >= 0) {
			start = direction > 0 ? -1 : n;
		}
		for (int p = start + direction; p >= 0 && p < n; p += direction) {
			int item = itemAt(p);
			if ((flags[item] & (DISABLED | HIDDEN)) == 0) {
				return item;
			}
		}
		return -1;
	}

	@Override
	public void setFocus(int item) {
		super.setFocus(item);
		int position = item < 0 ? -1 : positionOf(item);
		if (position >= 0) {
			long left = (long) position * itemExtent;
			if (left < scroll) {
				scrollTo(left);
			} else if (left + itemExtent > scroll + width) {
				scrollTo(left + itemExtent - width);
			}
		}
	}

	@Override
	protected void onWheel(int delta) {
		scrollTo(scroll + (long) delta * itemExtent);
	}
}
