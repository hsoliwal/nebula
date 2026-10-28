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
import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * A heat map: a grid of cells coloured by a value through a palette. The values are one <code>float</code> lane in
 * row-major order; the palette is a lookup table of 256 colours derived from its stops; cells are integers (row,
 * column, cell width and height). There is no object per cell. Painting covers only the cells inside the clip and
 * draws runs of equally coloured cells with one fill each; a value change repaints its cell when the cell is on
 * screen and nothing otherwise.
 * While the map is hidden (its own Hide, a zero size, a hidden ancestor) value changes only update the lane: no
 * repaint, no scroll bar update; on becoming visible it updates the scroll bars and paints once. A resize repaints
 * only the newly exposed strips (NO_REDRAW_RESIZE), or scrolls when the origin is clamped.
 * <p>
 * Styles: <code>H_SCROLL</code>, <code>V_SCROLL</code>, <code>BORDER</code>. Events: <code>Selection</code> when the
 * selected cell changes (<code>Event.x</code> the column, <code>Event.y</code> the row).
 * </p>
 * <p>
 * Startup switch: <code>-Dswt.widget.mode.M3HeatMap=original</code> paints every cell of the grid on every paint,
 * as a plain implementation would; the default (viewport) paints the clip only.
 * </p>
 */
public class M3HeatMap extends Canvas {

	static final boolean VIEWPORT = WidgetMode.of("M3HeatMap", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3HeatMap"); //$NON-NLS-1$

	private static final int LUT_SIZE = 256;

	private int rows, columns;
	private float[] values = new float[0];
	private float min = 0f, max = 1f;
	private boolean autoRange = true;
	private int[] lut = new int[LUT_SIZE];
	private final Color[] colors = new Color[LUT_SIZE];
	private int cellWidth = 8, cellHeight = 8;
	private int originX, originY;
	private int selectedRow = -1, selectedColumn = -1;
	private final M3HiddenGate gate;

	/**
	 * Creates a heat map with an empty grid and a blue-to-red palette.
	 *
	 * @param parent the parent
	 * @param style H_SCROLL, V_SCROLL, BORDER
	 */
	public M3HeatMap(Composite parent, int style) {
		super(parent, style | SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED | (VIEWPORT ? SWT.NO_REDRAW_RESIZE : 0));
		gate = new M3HiddenGate(this, this::updateScrollBars);
		setPalette(new RGB[] { new RGB(0, 0, 160), new RGB(0, 200, 220), new RGB(255, 230, 0), new RGB(200, 0, 0) });
		Listener listener = this::handle;
		addListener(SWT.Paint, listener);
		addListener(SWT.Resize, listener);
		addListener(SWT.MouseDown, listener);
		addListener(SWT.KeyDown, listener);
		addListener(SWT.Dispose, e -> disposeColors());
		ScrollBar h = getHorizontalBar(), v = getVerticalBar();
		if (h != null) {
			h.addListener(SWT.Selection, e -> scrollTo(h.getSelection(), originY));
		}
		if (v != null) {
			v.addListener(SWT.Selection, e -> scrollTo(originX, v.getSelection()));
		}
	}

	private void handle(Event event) {
		switch (event.type) {
		case SWT.Paint:
			paint(event.gc, event.x, event.y, event.width, event.height);
			break;
		case SWT.Resize:
			updateScrollBars();
			break;
		case SWT.MouseDown:
			if (event.button == 1) {
				Point cell = getCell(new Point(event.x, event.y));
				if (cell != null) {
					setSelection(cell.y, cell.x);
				}
			}
			break;
		case SWT.KeyDown:
			key(event.keyCode);
			break;
		default:
			break;
		}
	}

	private void key(int keyCode) {
		if (selectedRow < 0) {
			return;
		}
		int row = selectedRow, column = selectedColumn;
		switch (keyCode) {
		case SWT.ARROW_UP:
			row--;
			break;
		case SWT.ARROW_DOWN:
			row++;
			break;
		case SWT.ARROW_LEFT:
			column--;
			break;
		case SWT.ARROW_RIGHT:
			column++;
			break;
		default:
			return;
		}
		if (row >= 0 && row < rows && column >= 0 && column < columns) {
			setSelection(row, column);
			showCell(row, column);
		}
	}

	/**
	 * Sets the grid size. Values inside the new grid are kept; new cells are zero.
	 *
	 * @param rows at least 0
	 * @param columns at least 0
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if a size is negative
	 */
	public void setGridSize(int rows, int columns) {
		checkWidget();
		if (rows < 0 || columns < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		float[] next = new float[rows * columns];
		int copyRows = Math.min(rows, this.rows), copyColumns = Math.min(columns, this.columns);
		for (int r = 0; r < copyRows; r++) {
			System.arraycopy(values, r * this.columns, next, r * columns, copyColumns);
		}
		this.rows = rows;
		this.columns = columns;
		values = next;
		if (selectedRow >= rows || selectedColumn >= columns) {
			selectedRow = selectedColumn = -1;
		}
		rangeChanged();
		updateScrollBars();
		repaint();
	}

	/** @return the number of rows */
	public int getRowCount() {
		checkWidget();
		return rows;
	}

	/** @return the number of columns */
	public int getColumnCount() {
		checkWidget();
		return columns;
	}

	/**
	 * Replaces every value.
	 *
	 * @param rowMajor <code>rows * columns</code> values, row by row
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the array is null</li>
	 *                <li>ERROR_INVALID_ARGUMENT - if its length is not rows * columns</li>
	 *                </ul>
	 */
	public void setValues(float[] rowMajor) {
		checkWidget();
		if (rowMajor == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (rowMajor.length != rows * columns) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		System.arraycopy(rowMajor, 0, values, 0, rowMajor.length);
		rangeChanged();
		repaint();
	}

	/**
	 * Sets one value and repaints its cell when it is on screen.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if the cell is outside the grid
	 */
	public void setValue(int row, int column, float value) {
		checkWidget();
		checkCell(row, column);
		values[row * columns + column] = value;
		if (autoRange && (value < min || value > max)) {
			rangeChanged();
			repaint();
			return;
		}
		if (!showing()) {
			return;
		}
		Rectangle bounds = getCellBounds(row, column);
		Rectangle client = getClientArea();
		if (bounds.intersects(client)) {
			repaint(bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}

	/**
	 * @return the value of a cell
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if the cell is outside the grid
	 */
	public float getValue(int row, int column) {
		checkWidget();
		checkCell(row, column);
		return values[row * columns + column];
	}

	/**
	 * Repaints the cells of a row range that are on screen (a delta from the model).
	 *
	 * @param fromRow first row, inclusive
	 * @param toRow last row, inclusive
	 */
	public void update(int fromRow, int toRow) {
		checkWidget();
		if (rows == 0 || columns == 0) {
			return;
		}
		fromRow = Math.max(0, fromRow);
		toRow = Math.min(rows - 1, toRow);
		if (fromRow > toRow) {
			return;
		}
		if (autoRange) {
			rangeChanged();
		}
		if (!showing()) {
			return;
		}
		Rectangle client = getClientArea();
		int top = Math.max(client.y, fromRow * cellHeight - originY);
		int bottom = Math.min(client.y + client.height, (toRow + 1) * cellHeight - originY);
		if (top < bottom) {
			repaint(client.x, top, client.width, bottom - top);
		}
	}

	/**
	 * Sets the value range mapped onto the palette and turns automatic ranging off.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if min is not below max or a bound is NaN
	 */
	public void setRange(float min, float max) {
		checkWidget();
		if (!(min < max)) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.min = min;
		this.max = max;
		autoRange = false;
		repaint();
	}

	/** @return the lower bound of the range */
	public float getMinimum() {
		checkWidget();
		return min;
	}

	/** @return the upper bound of the range */
	public float getMaximum() {
		checkWidget();
		return max;
	}

	/**
	 * Turns automatic ranging (the range follows the smallest and largest value) on or off.
	 */
	public void setAutoRange(boolean autoRange) {
		checkWidget();
		this.autoRange = autoRange;
		rangeChanged();
		repaint();
	}

	/** @return whether the range follows the values */
	public boolean isAutoRange() {
		checkWidget();
		return autoRange;
	}

	private void rangeChanged() {
		if (!autoRange) {
			return;
		}
		float lo = Float.POSITIVE_INFINITY, hi = Float.NEGATIVE_INFINITY;
		for (int i = 0, n = rows * columns; i < n; i++) {
			float v = values[i];
			lo = Math.min(lo, v);
			hi = Math.max(hi, v);
		}
		if (lo < hi) {
			min = lo;
			max = hi;
		} else if (lo == hi) {
			min = lo;
			max = lo + 1f;
		} else {
			min = 0f;
			max = 1f;
		}
	}

	/**
	 * Sets the palette: at least two colour stops, spread evenly over the range; the lookup table is derived once.
	 *
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the array or a stop is null</li>
	 *                <li>ERROR_INVALID_ARGUMENT - if there are fewer than two stops</li>
	 *                </ul>
	 */
	public void setPalette(RGB[] stops) {
		checkWidget();
		if (stops == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (stops.length < 2) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		for (RGB stop : stops) {
			if (stop == null) {
				SWT.error(SWT.ERROR_NULL_ARGUMENT);
			}
		}
		int[] next = new int[LUT_SIZE];
		int segments = stops.length - 1;
		for (int i = 0; i < LUT_SIZE; i++) {
			float position = i * (float) segments / (LUT_SIZE - 1);
			int segment = Math.min(segments - 1, (int) position);
			float t = position - segment;
			RGB a = stops[segment], b = stops[segment + 1];
			int red = Math.round(a.red + (b.red - a.red) * t);
			int green = Math.round(a.green + (b.green - a.green) * t);
			int blue = Math.round(a.blue + (b.blue - a.blue) * t);
			next[i] = (red << 16) | (green << 8) | blue;
		}
		lut = next;
		disposeColors();
		repaint();
	}

	/* the colour of a LUT index, created on first use and kept until the palette changes or the map is disposed */
	private Color color(int index) {
		Color color = colors[index];
		if (color == null) {
			int rgb = lut[index];
			color = colors[index] = new Color(getDisplay(), (rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
		}
		return color;
	}

	private void disposeColors() {
		for (int i = 0; i < colors.length; i++) {
			if (colors[i] != null) {
				colors[i].dispose();
				colors[i] = null;
			}
		}
	}

	/**
	 * @return the lookup table index of a value (0..255) under the current range
	 */
	public int indexOf(float value) {
		checkWidget();
		return lutIndex(value);
	}

	private int lutIndex(float value) {
		if (Float.isNaN(value)) {
			return 0;
		}
		float t = (value - min) / (max - min);
		if (t <= 0f) {
			return 0;
		}
		if (t >= 1f) {
			return LUT_SIZE - 1;
		}
		return (int) (t * (LUT_SIZE - 1) + 0.5f);
	}

	/**
	 * @return the colour of a value as a packed <code>0xRRGGBB</code> int
	 */
	public int colorOf(float value) {
		checkWidget();
		return lut[lutIndex(value)];
	}

	/**
	 * Sets the cell size in pixels.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if a size is below 1
	 */
	public void setCellSize(int width, int height) {
		checkWidget();
		if (width < 1 || height < 1) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		cellWidth = width;
		cellHeight = height;
		updateScrollBars();
		repaint();
	}

	/** @return the cell size in pixels */
	public Point getCellSize() {
		checkWidget();
		return new Point(cellWidth, cellHeight);
	}

	/**
	 * @param point a point in the receiver's coordinates
	 * @return the cell under the point as (column, row), or <code>null</code> outside the grid
	 */
	public Point getCell(Point point) {
		checkWidget();
		if (point == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		int x = point.x + originX, y = point.y + originY;
		if (x < 0 || y < 0) {
			return null;
		}
		int column = x / cellWidth, row = y / cellHeight;
		return row < rows && column < columns ? new Point(column, row) : null;
	}

	/**
	 * @return the bounds of a cell in the receiver's coordinates (it may be off screen)
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if the cell is outside the grid
	 */
	public Rectangle getCellBounds(int row, int column) {
		checkWidget();
		checkCell(row, column);
		return new Rectangle(column * cellWidth - originX, row * cellHeight - originY, cellWidth, cellHeight);
	}

	/**
	 * Selects a cell, or nothing with (-1, -1), and sends a Selection event when the selection changes.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if the cell is outside the grid and not (-1, -1)
	 */
	public void setSelection(int row, int column) {
		checkWidget();
		if (!(row == -1 && column == -1)) {
			checkCell(row, column);
		}
		if (row == selectedRow && column == selectedColumn) {
			return;
		}
		int oldRow = selectedRow, oldColumn = selectedColumn;
		selectedRow = row;
		selectedColumn = column;
		repaintCell(oldRow, oldColumn);
		repaintCell(row, column);
		Event event = new Event();
		event.x = column;
		event.y = row;
		notifyListeners(SWT.Selection, event);
	}

	/** @return the selected cell as (column, row), or (-1, -1) */
	public Point getSelection() {
		checkWidget();
		return new Point(selectedColumn, selectedRow);
	}

	private void repaintCell(int row, int column) {
		if (row < 0 || column < 0) {
			return;
		}
		Rectangle bounds = getCellBounds(row, column);
		if (bounds.intersects(getClientArea())) {
			repaint(bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}

	/**
	 * Scrolls so that a cell is on screen.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if the cell is outside the grid
	 */
	public void showCell(int row, int column) {
		checkWidget();
		checkCell(row, column);
		Rectangle client = getClientArea();
		int x = originX, y = originY;
		int cellX = column * cellWidth, cellY = row * cellHeight;
		if (cellX < x) {
			x = cellX;
		} else if (cellX + cellWidth > x + client.width) {
			x = cellX + cellWidth - client.width;
		}
		if (cellY < y) {
			y = cellY;
		} else if (cellY + cellHeight > y + client.height) {
			y = cellY + cellHeight - client.height;
		}
		scrollTo(x, y);
	}

	/** @return the origin: the grid pixel at the top left of the client area */
	public Point getOrigin() {
		checkWidget();
		return new Point(originX, originY);
	}

	/** Scrolls the grid so that the pixel (x, y) is at the top left; clamped to the grid. */
	public void setOrigin(int x, int y) {
		checkWidget();
		scrollTo(x, y);
	}

	private void scrollTo(int x, int y) {
		Rectangle client = getClientArea();
		x = Math.max(0, Math.min(x, Math.max(0, columns * cellWidth - client.width)));
		y = Math.max(0, Math.min(y, Math.max(0, rows * cellHeight - client.height)));
		if (x == originX && y == originY) {
			return;
		}
		int dx = originX - x, dy = originY - y;
		originX = x;
		originY = y;
		if (!showing()) {
			return;
		}
		ScrollBar h = getHorizontalBar(), v = getVerticalBar();
		if (h != null) {
			h.setSelection(x);
		}
		if (v != null) {
			v.setSelection(y);
		}
		if (VIEWPORT) {
			scroll(dx, dy, 0, 0, client.width, client.height, false);
		} else {
			repaint();
		}
	}

	/* a hidden map does no native work: deltas fold into the gate; on reveal the scroll bars are updated and the map paints once */
	private boolean showing() {
		return !VIEWPORT || gate.probe();
	}

	private void repaint() {
		if (showing()) {
			M3Counters.fullRepaints++;
			redraw();
		}
	}

	private void repaint(int x, int y, int width, int height) {
		if (showing()) {
			redraw(x, y, width, height, false);
		}
	}

	private void updateScrollBars() {
		if (!showing()) {
			return;
		}
		Rectangle client = getClientArea();
		int width = columns * cellWidth, height = rows * cellHeight;
		ScrollBar h = getHorizontalBar(), v = getVerticalBar();
		if (h != null) {
			h.setMaximum(Math.max(1, width));
			h.setThumb(Math.max(1, Math.min(width, client.width)));
			h.setIncrement(cellWidth);
			h.setPageIncrement(Math.max(cellWidth, client.width));
		}
		if (v != null) {
			v.setMaximum(Math.max(1, height));
			v.setThumb(Math.max(1, Math.min(height, client.height)));
			v.setIncrement(cellHeight);
			v.setPageIncrement(Math.max(cellHeight, client.height));
		}
		scrollTo(originX, originY);
	}

	private void paint(GC gc, int clipX, int clipY, int clipWidth, int clipHeight) {
		Rectangle client = getClientArea();
		gc.setBackground(getBackground());
		gc.fillRectangle(clipX, clipY, clipWidth, clipHeight);
		if (rows == 0 || columns == 0) {
			return;
		}
		int firstRow, lastRow, firstColumn, lastColumn;
		if (VIEWPORT) {
			firstRow = Math.max(0, (clipY + originY) / cellHeight);
			lastRow = Math.min(rows - 1, (clipY + clipHeight - 1 + originY) / cellHeight);
			firstColumn = Math.max(0, (clipX + originX) / cellWidth);
			lastColumn = Math.min(columns - 1, (clipX + clipWidth - 1 + originX) / cellWidth);
		} else {
			firstRow = 0;
			lastRow = rows - 1;
			firstColumn = 0;
			lastColumn = columns - 1;
		}
		if (LOG.isTraceEnabled()) {
			final int fr = firstRow, lr = lastRow, fc = firstColumn, lc = lastColumn;
			LOG.trace(() -> "paint rows " + fr + ".." + lr + " columns " + fc + ".." + lc); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		// runs of equal colour, one fill each; colours come from a per-LUT-index cache, so a paint allocates nothing
		int current = -1;
		for (int row = firstRow; row <= lastRow; row++) {
			int y = row * cellHeight - originY;
			int base = row * columns;
			int runStart = firstColumn;
			int runIndex = lutIndex(values[base + firstColumn]);
			int runColor = lut[runIndex];
			for (int column = firstColumn + 1; column <= lastColumn + 1; column++) {
				int index = column <= lastColumn ? lutIndex(values[base + column]) : -1;
				int rgb = index >= 0 ? lut[index] : -1;
				if (rgb != runColor) {
					if (runColor != current) {
						gc.setBackground(color(runIndex));
						current = runColor;
					}
					gc.fillRectangle(runStart * cellWidth - originX, y, (column - runStart) * cellWidth, cellHeight);
					runStart = column;
					runColor = rgb;
					runIndex = index;
				}
			}
		}
		if (selectedRow >= 0 && selectedRow >= firstRow && selectedRow <= lastRow && selectedColumn >= firstColumn
				&& selectedColumn <= lastColumn) {
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
			gc.setLineWidth(2);
			gc.drawRectangle(selectedColumn * cellWidth - originX + 1, selectedRow * cellHeight - originY + 1, cellWidth - 2,
					cellHeight - 2);
		}
		gc.setBackground(getBackground());
		int right = columns * cellWidth - originX, bottom = rows * cellHeight - originY;
		if (right < client.x + client.width) {
			gc.fillRectangle(right, client.y, client.x + client.width - right, client.height);
		}
		if (bottom < client.y + client.height) {
			gc.fillRectangle(client.x, bottom, client.width, client.y + client.height - bottom);
		}
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		int width = wHint != SWT.DEFAULT ? wHint : columns * cellWidth;
		int height = hHint != SWT.DEFAULT ? hHint : rows * cellHeight;
		Rectangle trim = computeTrim(0, 0, width, height);
		return new Point(trim.width, trim.height);
	}

	private void checkCell(int row, int column) {
		if (row < 0 || row >= rows || column < 0 || column >= columns) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
	}

	/** @return a copy of the lookup table (256 packed <code>0xRRGGBB</code> ints) */
	public int[] getLookupTable() {
		checkWidget();
		return Arrays.copyOf(lut, lut.length);
	}
}
