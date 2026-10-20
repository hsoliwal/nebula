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
import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Toasts: transient notifications stacked in the bottom-right corner of a shell, each with a title, a message and a
 * level, dismissed on click or after their time (the behaviours catalogue "toasts"; Nebula's Opal Notifier opens one
 * shell per notification, this draws every toast in one overlay).
 * <p>
 * Toasts are lanes (id, level, expiry, title, message) in one drawn overlay shell; there is no shell or control per
 * toast. One timer runs for the earliest expiry only. Adding or removing a toast repaints the overlay once (it is small
 * and its cards move); the pointer resting on the overlay pauses expiry. While no toast is shown the overlay shell is
 * hidden; while the owner shell is minimized or hidden, toasts still expire but nothing is painted.
 * </p>
 */
public final class M3Toasts {

	/** Level: information. */
	public static final int INFO = 0;
	/** Level: warning. */
	public static final int WARNING = 1;
	/** Level: error. */
	public static final int ERROR = 2;

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3Toasts"); //$NON-NLS-1$

	private static final int WIDTH = 300, CARD = 54, GAP = 6, MARGIN = 12, MAX = 64;

	private final Shell owner;
	private Shell overlay;
	private Canvas canvas;
	private int count, nextId = 1;
	private int[] ids = new int[4], levels = new int[4];
	private long[] expiries = new long[4];
	private String[] titles = new String[4], messages = new String[4];
	private boolean paused;
	private long pausedAt;
	private final Runnable expire = this::expire;

	/**
	 * @param owner the shell whose bottom-right corner holds the toasts
	 */
	public M3Toasts(Shell owner) {
		if (owner == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		this.owner = owner;
		owner.addListener(SWT.Resize, e -> locate());
		owner.addListener(SWT.Move, e -> locate());
		owner.addListener(SWT.Dispose, e -> {
			owner.getDisplay().timerExec(-1, expire);
			if (overlay != null && !overlay.isDisposed()) {
				overlay.dispose();
			}
		});
	}

	/**
	 * Shows a toast.
	 *
	 * @param title the title, never null
	 * @param message the message, never null
	 * @param level INFO, WARNING or ERROR
	 * @param millis how long it stays, at least 1
	 * @return the toast id
	 */
	public int show(String title, String message, int level, int millis) {
		if (title == null || message == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (level < INFO || level > ERROR || millis < 1) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		if (count == MAX) {
			remove(0);
		}
		if (count == ids.length) {
			int n = count * 2;
			ids = Arrays.copyOf(ids, n);
			levels = Arrays.copyOf(levels, n);
			expiries = Arrays.copyOf(expiries, n);
			titles = Arrays.copyOf(titles, n);
			messages = Arrays.copyOf(messages, n);
		}
		int id = nextId++;
		ids[count] = id;
		levels[count] = level;
		expiries[count] = now() + millis;
		titles[count] = title;
		messages[count] = message;
		count++;
		LOG.debug(() -> "toast " + id + ": " + title); //$NON-NLS-1$ //$NON-NLS-2$
		changed();
		return id;
	}

	/**
	 * Dismisses a toast.
	 *
	 * @return true when it was shown
	 */
	public boolean dismiss(int id) {
		for (int i = 0; i < count; i++) {
			if (ids[i] == id) {
				remove(i);
				changed();
				return true;
			}
		}
		return false;
	}

	/** @return the number of toasts shown */
	public int getCount() {
		return count;
	}

	/** @return the overlay shell (created with the first toast), or null */
	public Shell getOverlay() {
		return overlay;
	}

	/** @return the id of the toast under an overlay point, or 0 */
	public int toastAt(int x, int y) {
		int index = (y - GAP) / (CARD + GAP);
		return x >= 0 && x < WIDTH && y >= GAP && index >= 0 && index < count && (y - GAP) % (CARD + GAP) < CARD ? ids[count - 1 - index] : 0;
	}

	private long now() {
		return System.nanoTime() / 1_000_000L;
	}

	private void remove(int index) {
		int tail = count - index - 1;
		System.arraycopy(ids, index + 1, ids, index, tail);
		System.arraycopy(levels, index + 1, levels, index, tail);
		System.arraycopy(expiries, index + 1, expiries, index, tail);
		System.arraycopy(titles, index + 1, titles, index, tail);
		System.arraycopy(messages, index + 1, messages, index, tail);
		count--;
		titles[count] = messages[count] = null;
	}

	/* one overlay update per change: size, position, one repaint; then the timer for the earliest expiry */
	private void changed() {
		if (owner.isDisposed()) {
			return;
		}
		if (count == 0) {
			if (overlay != null && !overlay.isDisposed()) {
				overlay.setVisible(false);
			}
			owner.getDisplay().timerExec(-1, expire);
			return;
		}
		ensureOverlay();
		locate();
		if (owner.isVisible() && !owner.getMinimized()) {
			overlay.setVisible(true);
			canvas.redraw();
			M3Counters.fullRepaints++;
		}
		schedule();
	}

	private void schedule() {
		if (paused || count == 0) {
			return;
		}
		long earliest = Long.MAX_VALUE;
		for (int i = 0; i < count; i++) {
			earliest = Math.min(earliest, expiries[i]);
		}
		owner.getDisplay().timerExec((int) Math.max(1, Math.min(Integer.MAX_VALUE, earliest - now())), expire);
	}

	private void expire() {
		if (owner.isDisposed() || paused) {
			return;
		}
		long time = now();
		boolean removed = false;
		for (int i = count - 1; i >= 0; i--) {
			if (expiries[i] <= time) {
				remove(i);
				removed = true;
			}
		}
		if (removed) {
			changed();
		} else {
			schedule();
		}
	}

	private void ensureOverlay() {
		if (overlay != null && !overlay.isDisposed()) {
			return;
		}
		overlay = new Shell(owner, SWT.ON_TOP | SWT.TOOL | SWT.NO_TRIM | SWT.NO_FOCUS);
		overlay.setLayout(new org.eclipse.swt.layout.FillLayout());
		canvas = new Canvas(overlay, SWT.DOUBLE_BUFFERED | SWT.NO_FOCUS);
		canvas.addListener(SWT.Paint, this::paint);
		canvas.addListener(SWT.MouseUp, e -> {
			int id = toastAt(e.x, e.y);
			if (id != 0) {
				dismiss(id);
			}
		});
		canvas.addListener(SWT.MouseEnter, e -> {
			paused = true;
			pausedAt = now();
			owner.getDisplay().timerExec(-1, expire);
		});
		canvas.addListener(SWT.MouseExit, e -> {
			long held = now() - pausedAt;
			paused = false;
			for (int i = 0; i < count; i++) {
				expiries[i] += held;
			}
			schedule();
		});
	}

	private void locate() {
		if (overlay == null || overlay.isDisposed() || count == 0) {
			return;
		}
		Rectangle client = owner.getClientArea();
		int height = GAP + count * (CARD + GAP);
		Point corner = owner.toDisplay(client.x + client.width - WIDTH - MARGIN, client.y + client.height - height - MARGIN);
		overlay.setBounds(corner.x, corner.y, WIDTH, height);
	}

	private void paint(Event event) {
		GC gc = event.gc;
		gc.setBackground(owner.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		gc.fillRectangle(event.x, event.y, event.width, event.height);
		int fontHeight = gc.getFontMetrics().getHeight();
		for (int slot = 0; slot < count; slot++) {
			int i = count - 1 - slot;
			int y = GAP + slot * (CARD + GAP);
			if (y > event.y + event.height || y + CARD < event.y) {
				continue;
			}
			int accent = levels[i] == ERROR ? SWT.COLOR_RED : levels[i] == WARNING ? SWT.COLOR_DARK_YELLOW : SWT.COLOR_LIST_SELECTION;
			gc.setBackground(owner.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			gc.fillRectangle(0, y, WIDTH, CARD);
			gc.setBackground(owner.getDisplay().getSystemColor(accent));
			gc.fillRectangle(0, y, 4, CARD);
			gc.setForeground(owner.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
			gc.drawRectangle(0, y, WIDTH - 1, CARD - 1);
			gc.setForeground(owner.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
			gc.drawText(titles[i], 12, y + 6, true);
			gc.drawText(messages[i], 12, y + 8 + fontHeight, true);
		}
	}
}
