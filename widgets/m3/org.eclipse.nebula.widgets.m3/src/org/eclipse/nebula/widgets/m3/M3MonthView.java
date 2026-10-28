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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;

import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.M3HiddenGate;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A month view: one or more month grids side by side and stacked, with a selected day or interval, flagged days,
 * unselectable days and today (the SWT counterpart of SwingX <code>JXMonthView</code>, re-derived from its documented
 * behaviour). Days are epoch days (<code>LocalDate.toEpochDay()</code>), passed as <code>long</code>.
 * <p>
 * The model is ints and sorted long lanes: the first shown month, the selection interval, flagged and unselectable
 * days (binary search, no objects per day). Every model change is a delta: selecting repaints the old and new
 * selected cells (an interval: the union of the old and new cell bands), flagging repaints one cell, and a change to a
 * day that is not shown repaints nothing; paging months repaints the grids. Cell geometry is measured once per font.
 * While hidden, changes do no native work and the view paints once when shown.
 * </p>
 * <p>
 * Styles: <code>SINGLE</code> (default) or <code>MULTI</code> (an interval by shift-click or shift-arrows),
 * <code>BORDER</code>. Events: <code>Selection</code> (<code>Event.index</code> the selected epoch day) and
 * <code>DefaultSelection</code> on double-click or Enter. Startup switch:
 * <code>-Dswt.widget.mode.M3MonthView=original</code> repaints the whole view on every change.
 * </p>
 */
public class M3MonthView extends Canvas {

	static final boolean VIEWPORT = WidgetMode.of("M3MonthView", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final int PAD = 4, GAP = 12;

	private final boolean multi;
	private final M3HiddenGate gate;
	/* first shown month as epoch day of its first day; grid shape */
	private long firstMonth;
	private int columns = 1, rows = 1, firstDayOfWeek = 1;
	private long selectionStart = Long.MIN_VALUE, selectionEnd = Long.MIN_VALUE, anchor = Long.MIN_VALUE, today;
	private long[] flagged = new long[0], unselectable = new long[0];
	private int flaggedCount, unselectableCount;
	/* geometry measured once per font: cell size, header height, month box size */
	private int cellWidth = -1, cellHeight, headerHeight, boxWidth, boxHeight;
	private String[] weekdayNames = new String[7];
	private final int[] dayWidths = new int[32];

	/**
	 * Creates a month view showing the current month with no selection.
	 *
	 * @param parent the parent
	 * @param style SINGLE or MULTI, BORDER
	 */
	public M3MonthView(Composite parent, int style) {
		super(parent, (style & ~(SWT.SINGLE | SWT.MULTI)) | SWT.DOUBLE_BUFFERED | (VIEWPORT ? SWT.NO_REDRAW_RESIZE : 0));
		multi = (style & SWT.MULTI) != 0;
		gate = new M3HiddenGate(this, null);
		today = LocalDate.now().toEpochDay();
		firstMonth = LocalDate.ofEpochDay(today).withDayOfMonth(1).toEpochDay();
		addListener(SWT.Paint, this::paint);
		addListener(SWT.MouseDown, this::mouse);
		addListener(SWT.MouseDoubleClick, e -> notifyListeners(SWT.DefaultSelection, new Event()));
		addListener(SWT.KeyDown, this::key);
		addListener(SWT.Traverse, e -> e.doit = e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS || e.detail == SWT.TRAVERSE_RETURN);
	}

	/**
	 * Shows the month containing a day as the first month.
	 *
	 * @param epochDay any day of the month
	 */
	public void setFirstDisplayedDay(long epochDay) {
		checkWidget();
		long month = LocalDate.ofEpochDay(epochDay).withDayOfMonth(1).toEpochDay();
		if (month != firstMonth) {
			firstMonth = month;
			fullRepaint();
		}
	}

	/** @return the epoch day of the first day of the first shown month */
	public long getFirstDisplayedDay() {
		checkWidget();
		return firstMonth;
	}

	/** @return the epoch day of the last day of the last shown month */
	public long getLastDisplayedDay() {
		checkWidget();
		return LocalDate.ofEpochDay(firstMonth).plusMonths((long) columns * rows).toEpochDay() - 1;
	}

	/**
	 * Sets how many months are shown, side by side and stacked.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if a value is less than 1
	 */
	public void setMonthsShown(int columns, int rows) {
		checkWidget();
		if (columns < 1 || rows < 1) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.columns = columns;
		this.rows = rows;
		fullRepaint();
	}

	/**
	 * Sets the first day of the week.
	 *
	 * @param dayOfWeek 1 (Monday) to 7 (Sunday), as java.time.DayOfWeek
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if out of range
	 */
	public void setFirstDayOfWeek(int dayOfWeek) {
		checkWidget();
		if (dayOfWeek < 1 || dayOfWeek > 7) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		firstDayOfWeek = dayOfWeek;
		cellWidth = -1;
		fullRepaint();
	}

	/** @return the first day of the week, 1 (Monday) to 7 (Sunday) */
	public int getFirstDayOfWeek() {
		checkWidget();
		return firstDayOfWeek;
	}

	/** Sets the day drawn as today and repaints the old and new today cells. */
	public void setToday(long epochDay) {
		checkWidget();
		long old = today;
		today = epochDay;
		repaintDays(old, old);
		repaintDays(epochDay, epochDay);
	}

	/** @return the day drawn as today */
	public long getToday() {
		checkWidget();
		return today;
	}

	/**
	 * Selects one day (unselectable days are refused) and repaints the old and new selected cells.
	 *
	 * @return true when the day was selected
	 */
	public boolean setSelection(long epochDay) {
		checkWidget();
		return select(epochDay, epochDay, epochDay);
	}

	/**
	 * Selects an interval (MULTI only; SINGLE selects its start). Unselectable days inside the interval stay drawn as
	 * unselectable.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if from is after to
	 */
	public void setSelectionInterval(long from, long to) {
		checkWidget();
		if (from > to) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		select(from, multi ? to : from, from);
	}

	/** Clears the selection. */
	public void clearSelection() {
		checkWidget();
		long start = selectionStart, end = selectionEnd;
		selectionStart = selectionEnd = anchor = Long.MIN_VALUE;
		repaintDays(start, end);
	}

	/** @return the first selected day, or Long.MIN_VALUE */
	public long getSelectionStart() {
		checkWidget();
		return selectionStart;
	}

	/** @return the last selected day, or Long.MIN_VALUE */
	public long getSelectionEnd() {
		checkWidget();
		return selectionEnd;
	}

	/** @return true when a day is inside the selection */
	public boolean isSelected(long epochDay) {
		checkWidget();
		return selectionStart != Long.MIN_VALUE && epochDay >= selectionStart && epochDay <= selectionEnd;
	}

	/** Flags or unflags a day (drawn bold); repaints its cell when shown. */
	public void setFlagged(long epochDay, boolean flag) {
		checkWidget();
		int at = Arrays.binarySearch(flagged, 0, flaggedCount, epochDay);
		if ((at >= 0) == flag) {
			return;
		}
		if (flag) {
			flagged = insert(flagged, flaggedCount++, -at - 1, epochDay);
		} else {
			System.arraycopy(flagged, at + 1, flagged, at, flaggedCount - at - 1);
			flaggedCount--;
		}
		repaintDays(epochDay, epochDay);
	}

	/** @return true when the day is flagged */
	public boolean isFlagged(long epochDay) {
		checkWidget();
		return Arrays.binarySearch(flagged, 0, flaggedCount, epochDay) >= 0;
	}

	/** Makes a day unselectable (drawn struck through) or selectable again; repaints its cell when shown. */
	public void setUnselectable(long epochDay, boolean value) {
		checkWidget();
		int at = Arrays.binarySearch(unselectable, 0, unselectableCount, epochDay);
		if ((at >= 0) == value) {
			return;
		}
		if (value) {
			unselectable = insert(unselectable, unselectableCount++, -at - 1, epochDay);
		} else {
			System.arraycopy(unselectable, at + 1, unselectable, at, unselectableCount - at - 1);
			unselectableCount--;
		}
		repaintDays(epochDay, epochDay);
	}

	/** @return true when the day cannot be selected */
	public boolean isUnselectable(long epochDay) {
		checkWidget();
		return Arrays.binarySearch(unselectable, 0, unselectableCount, epochDay) >= 0;
	}

	/**
	 * @return the cell of a shown day in client coordinates, or null when the day is not shown
	 */
	public Rectangle getDayBounds(long epochDay) {
		checkWidget();
		geometry();
		return dayBounds(epochDay);
	}

	/** @return the epoch day under a point, or Long.MIN_VALUE */
	public long getDayAt(int x, int y) {
		checkWidget();
		geometry();
		int column = x / (boxWidth + GAP), row = y / (boxHeight + GAP);
		if (column >= columns || row >= rows || x < 0 || y < 0) {
			return Long.MIN_VALUE;
		}
		int bx = x - column * (boxWidth + GAP) - PAD, by = y - row * (boxHeight + GAP) - headerHeight - cellHeight;
		if (bx < 0 || by < 0 || bx >= 7 * cellWidth || by >= 6 * cellHeight) {
			return Long.MIN_VALUE;
		}
		long month = LocalDate.ofEpochDay(firstMonth).plusMonths((long) row * columns + column).toEpochDay();
		long day = gridStart(month) + (long) (by / cellHeight) * 7 + bx / cellWidth;
		LocalDate first = LocalDate.ofEpochDay(month);
		return day >= month && day < first.plusMonths(1).toEpochDay() ? day : Long.MIN_VALUE;
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		geometry();
		int width = columns * boxWidth + (columns - 1) * GAP, height = rows * boxHeight + (rows - 1) * GAP;
		Rectangle trim = computeTrim(0, 0, wHint == SWT.DEFAULT ? width : wHint, hHint == SWT.DEFAULT ? height : hHint);
		return new Point(trim.width, trim.height);
	}

	private static long[] insert(long[] lane, int count, int at, long value) {
		long[] target = count == lane.length ? Arrays.copyOf(lane, Math.max(8, count * 2)) : lane;
		System.arraycopy(target, at, target, at + 1, count - at);
		target[at] = value;
		return target;
	}

	private boolean select(long from, long to, long newAnchor) {
		if (from == to && isUnselectable(from)) {
			return false;
		}
		long oldStart = selectionStart, oldEnd = selectionEnd;
		selectionStart = from;
		selectionEnd = to;
		anchor = newAnchor;
		if (oldStart != from || oldEnd != to) {
			if (oldStart != Long.MIN_VALUE) {
				repaintDays(oldStart, oldEnd);
			}
			repaintDays(from, to);
			Event event = new Event();
			event.index = (int) from;
			notifyListeners(SWT.Selection, event);
		}
		return true;
	}

	private boolean showing() {
		return !VIEWPORT || gate.probe();
	}

	private void fullRepaint() {
		if (showing()) {
			M3Counters.fullRepaints++;
			redraw();
		}
	}

	/* repaints the shown cells of a day range: one rectangle per touched week row (clipped to the shown months) */
	private void repaintDays(long from, long to) {
		if (from == Long.MIN_VALUE || !showing()) {
			return;
		}
		if (!VIEWPORT) {
			M3Counters.fullRepaints++;
			redraw();
			return;
		}
		geometry();
		long first = Math.max(from, firstMonth), last = Math.min(to, getLastDisplayedDay());
		if (last - first > 62) {
			// an interval longer than two months covers most shown cells: one damage of the client area
			redraw();
			return;
		}
		for (long day = first; day <= last; day++) {
			Rectangle cell = dayBounds(day);
			if (cell != null) {
				redraw(cell.x, cell.y, cell.width, cell.height, false);
			}
		}
	}

	/* the first day of the 6 x 7 grid of a month (the week row containing the 1st) */
	private long gridStart(long month) {
		int dow = LocalDate.ofEpochDay(month).getDayOfWeek().getValue();
		return month - ((dow - firstDayOfWeek + 7) % 7);
	}

	private Rectangle dayBounds(long day) {
		if (day < firstMonth || day > getLastDisplayedDay()) {
			return null;
		}
		LocalDate date = LocalDate.ofEpochDay(day);
		LocalDate first = LocalDate.ofEpochDay(firstMonth);
		int index = (date.getYear() - first.getYear()) * 12 + date.getMonthValue() - first.getMonthValue();
		long month = date.withDayOfMonth(1).toEpochDay();
		int offset = (int) (day - gridStart(month));
		int column = index % columns, row = index / columns;
		int x = column * (boxWidth + GAP) + PAD + (offset % 7) * cellWidth;
		int y = row * (boxHeight + GAP) + headerHeight + cellHeight + (offset / 7) * cellHeight;
		return new Rectangle(x, y, cellWidth, cellHeight);
	}

	/* cell and box geometry from the font, measured once per font or first day of week */
	private void geometry() {
		if (cellWidth >= 0) {
			return;
		}
		GC gc = new GC(this);
		Point digits = gc.textExtent("00"); //$NON-NLS-1$
		int widest = digits.x;
		for (int i = 0; i < 7; i++) {
			DayOfWeek dow = DayOfWeek.of((firstDayOfWeek - 1 + i) % 7 + 1);
			weekdayNames[i] = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).substring(0, 2);
			widest = Math.max(widest, gc.textExtent(weekdayNames[i]).x);
		}
		for (int d = 1; d < DAY_LABELS.length; d++) {
			dayWidths[d] = gc.textExtent(DAY_LABELS[d]).x;
		}
		int fontHeight = gc.getFontMetrics().getHeight();
		gc.dispose();
		M3Counters.measures++;
		cellWidth = widest + 2 * PAD;
		cellHeight = fontHeight + PAD;
		headerHeight = fontHeight + 2 * PAD;
		boxWidth = 7 * cellWidth + 2 * PAD;
		boxHeight = headerHeight + 7 * cellHeight + PAD;
	}

	private void mouse(Event event) {
		if (event.button != 1) {
			return;
		}
		forceFocus();
		long day = getDayAt(event.x, event.y);
		if (day != Long.MIN_VALUE) {
			if (multi && (event.stateMask & SWT.SHIFT) != 0 && anchor != Long.MIN_VALUE) {
				select(Math.min(anchor, day), Math.max(anchor, day), anchor);
			} else {
				select(day, day, day);
			}
			return;
		}
		geometry();
		if (event.y < headerHeight && event.x < boxWidth / 3) {
			setFirstDisplayedDay(LocalDate.ofEpochDay(firstMonth).minusMonths(1).toEpochDay());
		} else if (event.y < headerHeight && event.x > columns * (boxWidth + GAP) - GAP - boxWidth / 3) {
			setFirstDisplayedDay(LocalDate.ofEpochDay(firstMonth).plusMonths(1).toEpochDay());
		}
	}

	private void key(Event event) {
		long focus = selectionStart == Long.MIN_VALUE ? today : selectionEnd == anchor ? selectionStart : selectionEnd;
		long next;
		switch (event.keyCode) {
		case SWT.ARROW_LEFT:
			next = focus - 1;
			break;
		case SWT.ARROW_RIGHT:
			next = focus + 1;
			break;
		case SWT.ARROW_UP:
			next = focus - 7;
			break;
		case SWT.ARROW_DOWN:
			next = focus + 7;
			break;
		case SWT.PAGE_UP:
			next = LocalDate.ofEpochDay(focus).minusMonths(1).toEpochDay();
			break;
		case SWT.PAGE_DOWN:
			next = LocalDate.ofEpochDay(focus).plusMonths(1).toEpochDay();
			break;
		default:
			if (event.character == '\r') {
				notifyListeners(SWT.DefaultSelection, new Event());
			}
			return;
		}
		if (multi && (event.stateMask & SWT.SHIFT) != 0 && anchor != Long.MIN_VALUE) {
			select(Math.min(anchor, next), Math.max(anchor, next), anchor);
		} else {
			select(next, next, next);
		}
		if (next < firstMonth || next > getLastDisplayedDay()) {
			setFirstDisplayedDay(next < firstMonth ? next : LocalDate.ofEpochDay(next).minusMonths((long) columns * rows - 1).toEpochDay());
		}
	}

	private void paint(Event event) {
		geometry();
		GC gc = event.gc;
		Rectangle clip = new Rectangle(event.x, event.y, event.width, event.height);
		gc.setBackground(getBackground());
		gc.fillRectangle(clip);
		LocalDate first = LocalDate.ofEpochDay(firstMonth);
		for (int index = 0; index < columns * rows; index++) {
			int column = index % columns, row = index / columns;
			int bx = column * (boxWidth + GAP), by = row * (boxHeight + GAP);
			if (!clip.intersects(bx, by, boxWidth, boxHeight)) {
				continue;
			}
			LocalDate monthDate = first.plusMonths(index);
			paintMonth(gc, clip, bx, by, monthDate);
		}
	}

	private void paintMonth(GC gc, Rectangle clip, int bx, int by, LocalDate monthDate) {
		gc.setForeground(getForeground());
		if (clip.intersects(bx, by, boxWidth, headerHeight)) {
			String title = monthDate.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + monthDate.getYear(); //$NON-NLS-1$
			Point extent = gc.textExtent(title);
			gc.drawText(title, bx + (boxWidth - extent.x) / 2, by + PAD, true);
			if (monthDate.toEpochDay() == firstMonth) {
				gc.drawText("<", bx + PAD, by + PAD, true); //$NON-NLS-1$
			}
			if (monthDate.toEpochDay() == LocalDate.ofEpochDay(firstMonth).plusMonths((long) columns * rows - 1).toEpochDay()) {
				gc.drawText(">", bx + boxWidth - PAD - gc.textExtent(">").x, by + PAD, true); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		int top = by + headerHeight;
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
		for (int i = 0; i < 7; i++) {
			gc.drawText(weekdayNames[i], bx + PAD + i * cellWidth + PAD, top, true);
		}
		long month = monthDate.toEpochDay(), end = monthDate.plusMonths(1).toEpochDay(), start = gridStart(month);
		for (int cell = 0; cell < 42; cell++) {
			long day = start + cell;
			int x = bx + PAD + (cell % 7) * cellWidth, y = top + cellHeight + (cell / 7) * cellHeight;
			if (day < month || day >= end || !clip.intersects(x, y, cellWidth, cellHeight)) {
				continue;
			}
			paintDay(gc, day, (int) (day - month) + 1, x, y);
		}
	}

	private void paintDay(GC gc, long day, int dayOfMonth, int x, int y) {
		boolean selected = isSelected(day);
		if (selected) {
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
			gc.fillRectangle(x, y, cellWidth, cellHeight);
		}
		gc.setForeground(getDisplay().getSystemColor(selected ? SWT.COLOR_LIST_SELECTION_TEXT : SWT.COLOR_WIDGET_FOREGROUND));
		String label = DAY_LABELS[dayOfMonth];
		boolean flag = Arrays.binarySearch(flagged, 0, flaggedCount, day) >= 0;
		int tx = x + cellWidth - PAD - dayWidths[dayOfMonth];
		gc.drawText(label, tx, y + PAD / 2, true);
		if (flag) {
			gc.drawText(label, tx + 1, y + PAD / 2, true);
		}
		if (Arrays.binarySearch(unselectable, 0, unselectableCount, day) >= 0) {
			gc.drawLine(x + 2, y + cellHeight / 2, x + cellWidth - 3, y + cellHeight / 2);
		}
		if (day == today) {
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
			gc.drawRectangle(x, y, cellWidth - 1, cellHeight - 1);
		}
	}

	/* interned day-of-month labels: painting a cell allocates no string */
	private static final String[] DAY_LABELS = new String[32];
	static {
		for (int i = 0; i < DAY_LABELS.length; i++) {
			DAY_LABELS[i] = Integer.toString(i);
		}
	}
}
