/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others.
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
package org.eclipse.nebula.widgets.grid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;

/**
 * Mouse gestures on a Grid's column headers, column group headers and row headers for the selection
 * tests: where a click hits (found by the Grid's own hit tests once the Grid has painted), and the
 * events of a press, a drag and a release as the event loop delivers them (the pointer moves to a
 * press point before the button goes down there).
 */
final class GridHeaderGestures {

	private GridHeaderGestures() {
	}

	/* paint and dispatch as the event loop would */
	static void settle(final Grid grid) {
		grid.update();
		while (grid.getDisplay().readAndDispatch()) {
			// drain
		}
	}

	private static int firstVisibleColumn(final Grid grid) {
		for (final int col : grid.getColumnOrder()) {
			if (grid.getColumn(col).getVisible()) {
				return col;
			}
		}
		return -1;
	}

	/* where a click hits row {@code row}'s header once the row is shown (scrolled to when asked), or null */
	static Point rowHeader(final Grid grid, final int row, final boolean scroll) {
		final int col = firstVisibleColumn(grid);
		if (col < 0 || row < 0 || row >= grid.getItemCount()) {
			return null;
		}
		final GridItem item = grid.getItem(row);
		final long deadline = System.nanoTime() + 1_000_000_000L;
		do {
			if (scroll) {
				grid.showItem(item);
			}
			settle(grid);
			final Rectangle bounds = item.getBounds(col);
			final Point point = new Point(Math.max(1, grid.getRowHeaderWidth() / 2), bounds.y + bounds.height / 2);
			if (bounds.height > 0 && point.x <= grid.getRowHeaderWidth() && grid.getItem(point) == item) {
				return point;
			}
		} while (System.nanoTime() < deadline);
		return null;
	}

	/*
	 * where a click hits column {@code col}'s header once the column is shown: its group's header when
	 * {@code group} (null for a column without a group), else the column's own header
	 */
	static Point columnHeader(final Grid grid, final int col, final boolean group) {
		final GridColumn column = grid.getColumn(col);
		if (!column.getVisible() || group && column.getColumnGroup() == null) {
			return null;
		}
		final long deadline = System.nanoTime() + 1_000_000_000L;
		do {
			grid.showColumn(column);
			settle(grid);
			final Rectangle bounds = column.getBounds();
			final int y = group ? Math.max(1, grid.getGroupHeaderHeight() / 2) : bounds.y + bounds.height / 2;
			final Point point = new Point(bounds.x + Math.min(8, bounds.width / 2), y);
			if (bounds.width > 0 && point.x >= 0 && point.x < grid.getClientArea().width && point.y > 0
					&& point.y < grid.getHeaderHeight() && grid.getColumn(point) == column) {
				return point;
			}
		} while (System.nanoTime() < deadline);
		return null;
	}

	/* a point below the Grid at x, where the Grid hit-tests no row (a drag past the last row shown) */
	static Point below(final Grid grid, final int x) {
		final Point point = new Point(x, grid.getClientArea().height + 10);
		for (int i = 0; i < 50 && grid.getItem(point) != null; i++) {
			point.y += grid.getItemHeight() + 1;
		}
		return point;
	}

	static void mouse(final Grid grid, final int type, final Point point, final int stateMask) {
		final Event e = new Event();
		e.button = type == SWT.MouseMove ? 0 : 1;
		e.x = point.x;
		e.y = point.y;
		e.stateMask = stateMask;
		grid.notifyListeners(type, e);
	}

	/* the pointer comes to the point (no button), then the button goes down there */
	static void press(final Grid grid, final Point point, final int stateMask) {
		mouse(grid, SWT.MouseMove, point, stateMask & ~SWT.BUTTON_MASK);
		mouse(grid, SWT.MouseDown, point, stateMask);
	}

	/* the pointer moves to the point with the button down */
	static void drag(final Grid grid, final Point point, final int stateMask) {
		mouse(grid, SWT.MouseMove, point, stateMask | SWT.BUTTON1);
	}

	static void release(final Grid grid, final Point point, final int stateMask) {
		mouse(grid, SWT.MouseUp, point, stateMask | SWT.BUTTON1);
	}

	static void click(final Grid grid, final Point point, final int stateMask) {
		press(grid, point, stateMask);
		release(grid, point, stateMask);
	}
}
