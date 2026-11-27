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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;

import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;

/**
 * A seeded selection script for a {@code SWT.VIRTUAL} Grid and the trace it leaves: after every step
 * the selection count, index and indices, the focus row, the item count and the Selection and
 * DefaultSelection events (type, item row, detail, state mask); every few steps the selected items'
 * rows in {@code getSelection()} order and sampled {@code isSelected} answers. Two Grids that behave
 * alike leave the same trace. Public API only, so it also runs against earlier Grid builds.
 */
final class GridSelectionScript {

	private final Grid grid;
	private final List<String> events = new ArrayList<>();
	final List<String> trace = new ArrayList<>();
	/* run after every step (the row selection test checks its lane there) */
	Runnable afterStep = () -> {
	};
	private boolean permuted;
	/*
	 * whether rows were inserted or removed: the earlier item list maps a selected item through the
	 * model row its slot cached when the item was first read, which such changes leave stale, so a
	 * row permutation is only scripted before them (see the row selection's PR notes)
	 */
	private boolean structural;
	/*
	 * cell steps as well (the cell selection scripts): cells, columns, cell clicks and drags; the
	 * rows a step removes or permutes are read first, so both Grids have their items (an earlier
	 * build made them in its range loops; see the cell selection's PR notes)
	 */
	boolean cellOps;
	/* whether a cell step may set a column span (spans turn the range loops back to items) */
	boolean spans;
	/*
	 * cell steps take header gestures as well (the header and drag scripts): clicks on column, column
	 * group and row headers, row-header and column-header drags (the Grid needs its row header shown,
	 * and a column group for the column-header drags)
	 */
	boolean headerOps;
	/* whether the Grid has had a FocusIn (see takeFocus) */
	private boolean focusedIn;

	GridSelectionScript(final Grid grid) {
		this.grid = grid;
		// added after the Grid's own FocusIn listener, so Grid.onFocusIn has run when this is set
		grid.addListener(SWT.FocusIn, e -> focusedIn = true);
		grid.addListener(SWT.Selection, e -> events.add(describe("S", e)));
		grid.addListener(SWT.DefaultSelection, e -> events.add(describe("D", e)));
		grid.addListener(SWT.SetData, e -> ((GridItem) e.item).setText(0, "r" + e.index));
	}

	private String describe(final String type, final Event e) {
		final int row = e.item instanceof GridItem && !e.item.isDisposed() ? grid.indexOf((GridItem) e.item) : -1;
		return type + row + "/" + e.detail + "/" + e.stateMask;
	}

	void run(final long seed, final int steps) {
		takeFocus();
		for (int k = 0; k < steps; k++) {
			final Random r = new Random(seed * 7919 + k);
			String op;
			try {
				op = step(r);
			} catch (final RuntimeException failure) {
				op = "EXC " + failure.getClass().getSimpleName();
			}
			afterStep.run();
			observe(seed, k, op);
		}
	}

	/*
	 * The first FocusIn makes row 0 the focus row of a Grid that has none (Grid.onFocusIn, as in the
	 * earlier releases). Under X11 it comes when the window manager activates the newly opened shell,
	 * at a time the X server picks: one Grid could take it before the first observation and the other
	 * after it (a CI run traced focus=-1 for one Grid and focus=0 for the other at step 0, after a
	 * deselect). The Grid is given the focus and the events are dispatched until the Grid has had its
	 * FocusIn (bounded wait, logged if it does not come), so that every Grid takes it before the first
	 * step. isFocusControl() cannot tell: SWT GTK marks the shell active as soon as open() asks for the
	 * activation, and then answers the window's focus widget as the focus control while the window
	 * has not received the focus yet. On win32 the FocusIn comes inside open().
	 */
	private void takeFocus() {
		if (grid.isDisposed()) {
			return;
		}
		grid.setFocus();
		final long deadline = System.nanoTime() + 2_000_000_000L;
		while (!focusedIn && !grid.isDisposed() && System.nanoTime() < deadline) {
			if (!grid.getDisplay().readAndDispatch()) {
				LockSupport.parkNanos(5_000_000L);
			}
		}
		if (!focusedIn) {
			System.out.println("GRID_SELECTION_SCRIPT the Grid had no FocusIn within 2 s");
		}
		settle();
	}

	private int row(final Random r, final int n) {
		return n == 0 ? 0 : r.nextInt(n);
	}

	private int[] indices(final Random r, final int n) {
		final int[] result = new int[r.nextInt(6)];
		for (int i = 0; i < result.length; i++) {
			// out of range and repeated indices on purpose
			result[i] = r.nextInt(10) == 0 ? n + r.nextInt(3) : r.nextInt(3) == 0 && i > 0 ? result[i - 1] : row(r, n);
		}
		return result;
	}

	private String step(final Random r) {
		if (cellOps && r.nextInt(3) != 0) {
			return cellStep(r);
		}
		final int n = grid.getItemCount();
		final int op = r.nextInt(31);
		if (op >= 12 && op <= 17 || op >= 28) {
			structural = true;
		}
		final int a = row(r, n);
		final int b = Math.min(n - 1, a + r.nextInt(40));
		switch (op) {
		case 0:
			grid.select(a);
			return "select " + a;
		case 1:
			grid.select(a, b);
			return "select " + a + ".." + b;
		case 2: {
			final int[] ix = indices(r, n);
			grid.select(ix);
			return "select " + Arrays.toString(ix);
		}
		case 3:
			grid.selectAll();
			return "selectAll";
		case 4:
			grid.deselect(a);
			return "deselect " + a;
		case 5:
			grid.deselect(a, b);
			return "deselect " + a + ".." + b;
		case 6: {
			final int[] ix = indices(r, n);
			grid.deselect(ix);
			return "deselect " + Arrays.toString(ix);
		}
		case 7:
			grid.deselectAll();
			return "deselectAll";
		case 8:
			grid.setSelection(a);
			return "setSelection " + a;
		case 9:
			grid.setSelection(a, b);
			return "setSelection " + a + ".." + b;
		case 10: {
			final int[] ix = indices(r, n);
			grid.setSelection(ix);
			return "setSelection " + Arrays.toString(ix);
		}
		case 11: {
			if (n == 0) {
				return "noop";
			}
			final GridItem[] its = new GridItem[1 + r.nextInt(3)];
			for (int i = 0; i < its.length; i++) {
				its[i] = r.nextBoolean() && i > 0 ? its[i - 1] : grid.getItem(row(r, n));
			}
			grid.setSelection(its);
			return "setSelection items";
		}
		case 12: {
			final int at = n == 0 ? 0 : r.nextInt(n + 1);
			new GridItem(grid, SWT.NONE, at).setText(0, "new");
			return "insert " + at;
		}
		case 13:
			if (n == 0) {
				return "noop";
			}
			read(a, a);
			grid.remove(a);
			return "remove " + a;
		case 14:
			if (n == 0) {
				return "noop";
			}
			final int last = Math.min(n - 1, a + r.nextInt(5));
			read(a, last);
			grid.remove(a, last);
			return "remove " + a + "..";
		case 15: {
			final int[] ix = indices(r, n);
			for (int i = 0; i < ix.length; i++) {
				ix[i] = Math.min(ix[i], n - 1);
			}
			if (n == 0 || ix.length == 0) {
				return "noop";
			}
			for (final int i : ix) {
				read(i, i);
			}
			grid.remove(ix);
			return "remove " + Arrays.toString(ix);
		}
		case 16:
			if (n == 0) {
				return "noop";
			}
			grid.getItem(a).dispose();
			return "dispose " + a;
		case 17: {
			final int count = Math.max(0, n + r.nextInt(41) - 20);
			grid.setItemCount(count);
			return "setItemCount " + count;
		}
		case 18:
			if (n == 0) {
				return "noop";
			}
			if (r.nextBoolean()) {
				grid.clear(a, false);
			} else {
				grid.clearAll(true);
			}
			return "clear";
		case 19:
			return key(r.nextBoolean() ? SWT.ARROW_DOWN : SWT.ARROW_UP, r);
		case 20:
			return key(r.nextBoolean() ? SWT.END : SWT.HOME, r);
		case 21:
			return key(r.nextBoolean() ? SWT.PAGE_DOWN : SWT.PAGE_UP, r);
		case 22: {
			final Event e = new Event();
			e.character = ' ';
			e.stateMask = r.nextBoolean() ? SWT.MOD1 : 0;
			grid.notifyListeners(SWT.KeyDown, e);
			return "space " + e.stateMask;
		}
		case 23:
			return click(r);
		case 24:
			grid.setTopIndex(a);
			return "top " + a;
		case 25:
			grid.showSelection();
			return "showSelection";
		case 26:
			if (n > 0) {
				grid.getItem(a).getText(0);
			}
			return "read " + a;
		case 27:
			if (permuted || !structural && r.nextInt(4) == 0) {
				permuted = !permuted;
				final RowPermutation permutation = permuted ? new RowPermutation(n) : null;
				if (permutation != null) {
					final int[] keys = new int[n];
					for (int i = 0; i < n; i++) {
						keys[i] = n - i;
					}
					permutation.sortByIntKeys(keys, false);
				}
				if (cellOps) {
					for (final Point cell : grid.getCellSelection()) {
						read(cell.y, cell.y);
					}
				}
				grid.setRowPermutation(permutation);
				return "permutation " + permuted;
			}
			return "noop";
		case 28:
			grid.disposeAllItems();
			grid.setItemCount(50 + r.nextInt(200));
			return "disposeAll";
		case 29:
			if (n == 0) {
				return "noop";
			}
			read(a, b);
			grid.remove(a, b);
			return "remove " + a + ".." + b;
		default:
			if (r.nextInt(8) == 0 && n > 0) {
				new GridItem(grid.getItem(a), SWT.NONE).setText(0, "child");
				return "child " + a;
			}
			return "noop";
		}
	}

	/* cell steps read the rows a removal takes first: both Grids then have their items */
	private void read(final int from, final int to) {
		if (cellOps) {
			for (int row = Math.max(0, from); row <= to && row < grid.getItemCount(); row++) {
				grid.getItem(row);
			}
		}
	}

	private Point cell(final Random r, final int n, final int m) {
		// now and then a cell out of range on purpose
		return new Point(r.nextInt(12) == 0 ? m : row(r, m), r.nextInt(12) == 0 ? n : row(r, n));
	}

	private Point[] cells(final Random r, final int n, final int m) {
		final Point[] result = new Point[r.nextInt(6)];
		for (int i = 0; i < result.length; i++) {
			// repeated cells on purpose
			result[i] = i > 0 && r.nextInt(4) == 0 ? new Point(result[i - 1].x, result[i - 1].y) : cell(r, n, m);
		}
		return result;
	}

	private String cellStep(final Random r) {
		if (headerOps && r.nextInt(3) == 0) {
			return headerStep(r);
		}
		final int n = grid.getItemCount();
		final int m = grid.getColumnCount();
		final Point p = cell(r, n, m);
		switch (r.nextInt(19)) {
		case 0:
			grid.selectCell(p);
			return "selectCell " + p;
		case 1:
			grid.deselectCell(p);
			return "deselectCell " + p;
		case 2: {
			final Point[] ps = cells(r, n, m);
			grid.selectCells(ps);
			return "selectCells " + Arrays.toString(ps);
		}
		case 3: {
			final Point[] ps = cells(r, n, m);
			grid.deselectCells(ps);
			return "deselectCells " + Arrays.toString(ps);
		}
		case 4:
			grid.setCellSelection(p);
			return "setCellSelection " + p;
		case 5: {
			final Point[] ps = cells(r, n, m);
			grid.setCellSelection(ps);
			return "setCellSelection " + Arrays.toString(ps);
		}
		case 6:
			grid.selectAllCells();
			return "selectAllCells";
		case 7:
			grid.deselectAllCells();
			return "deselectAllCells";
		case 8:
			if (m == 0) {
				return "noop";
			}
			grid.selectColumn(p.x % m);
			return "selectColumn " + p.x % m;
		case 9:
		case 10:
		case 11:
			return clickCell(r, false);
		case 12: {
			final int[] keys = { SWT.ARROW_LEFT, SWT.ARROW_RIGHT, SWT.ARROW_UP, SWT.ARROW_DOWN, SWT.HOME, SWT.END };
			return key(keys[r.nextInt(keys.length)], r);
		}
		case 13:
			return clickCell(r, true);
		case 14:
			if (m <= 1) {
				return "noop";
			}
			grid.getColumn(p.x % m).dispose();
			return "disposeColumn " + p.x % m;
		case 15: {
			if (m >= 6) {
				return "noop";
			}
			final int at = r.nextInt(m + 1);
			new GridColumn(grid, SWT.NONE, at).setWidth(50 + 10 * r.nextInt(4));
			return "newColumn " + at;
		}
		case 16: {
			final int[] order = grid.getColumnOrder();
			for (int i = order.length - 1; i > 0; i--) {
				final int j = r.nextInt(i + 1);
				final int t = order[i];
				order[i] = order[j];
				order[j] = t;
			}
			grid.setColumnOrder(order);
			return "columnOrder " + Arrays.toString(order);
		}
		case 17: {
			if (m == 0) {
				return "noop";
			}
			final GridColumn column = grid.getColumn(p.x % m);
			if (r.nextBoolean()) {
				column.setVisible(!column.getVisible());
				return "visible " + p.x % m + " " + column.getVisible();
			}
			column.setCellSelectionEnabled(!column.getCellSelectionEnabled());
			return "cellSelectable " + p.x % m + " " + column.getCellSelectionEnabled();
		}
		default:
			if (spans && n > 0 && m > 1 && r.nextInt(3) == 0) {
				final int row = row(r, n);
				final int column = r.nextInt(m);
				grid.getItem(row).setColumnSpan(column, 1);
				return "span " + column + "," + row;
			}
			return "noop";
		}
	}

	/*
	 * A click on a cell the script picks from its own random sequence (a visible column; the row and
	 * the column are shown and the click goes where the Grid hit-tests the cell), or a drag from that
	 * cell to a cell at most three rows away in the same view (the top row is pinned so that both are
	 * shown, never read from the Grid's scroll state).
	 */
	private String clickCell(final Random r, final boolean drag) {
		final int n = grid.getItemCount();
		final int[] visible = new int[grid.getColumnCount()];
		int v = 0;
		for (int c = 0; c < visible.length; c++) {
			if (grid.getColumn(c).getVisible()) {
				visible[v++] = c;
			}
		}
		if (n == 0 || v == 0) {
			return "noop";
		}
		final int row = r.nextInt(n);
		final int col = visible[r.nextInt(v)];
		final int[] masks = { 0, SWT.MOD2, SWT.MOD1, SWT.MOD1 | SWT.MOD2 };
		final int stateMask = masks[r.nextInt(masks.length)];
		final int toRow = Math.max(0, Math.min(n - 1, row + r.nextInt(7) - 3));
		final int toCol = visible[r.nextInt(v)];
		if (drag) {
			final int window = Math.max(1, grid.getVisibleGridHeight() / (grid.getItemHeight() + 1));
			final int target = Math.max(0, Math.min(Math.min(row, toRow) - 1, n - window));
			if (pinTopAt(target) < 0) {
				return "drag " + row + " top not pinned";
			}
			grid.showColumn(grid.getColumn(col));
		}
		final Point point = shown(row, col, !drag);
		if (point == null) {
			return (drag ? "drag " : "click ") + col + "," + row + " not shown";
		}
		final Event down = new Event();
		down.button = 1;
		down.x = point.x;
		down.y = point.y;
		down.stateMask = stateMask;
		grid.notifyListeners(SWT.MouseDown, down);
		String moved = "";
		if (drag) {
			final Point to = shown(toRow, toCol, false);
			if (to != null) {
				final Event move = new Event();
				move.x = to.x;
				move.y = to.y;
				move.stateMask = stateMask | SWT.BUTTON1;
				grid.notifyListeners(SWT.MouseMove, move);
				moved = " to " + toCol + "," + toRow;
			} else {
				moved = " to not shown";
			}
		}
		final Event up = new Event();
		up.button = 1;
		up.x = down.x;
		up.y = down.y;
		up.stateMask = down.stateMask | SWT.BUTTON1;
		grid.notifyListeners(SWT.MouseUp, up);
		return (drag ? "drag " : "click ") + col + "," + row + "/" + down.stateMask + moved;
	}

	/*
	 * A header gesture the script picks from its own random sequence, with shift and ctrl: a click on a
	 * column header or on a column group header, a click on a row header, a press on a row header
	 * dragged to another shown row, past the last row, into the header or (after a scroll) to the top
	 * row of another view, or a press on a column group header dragged to a column header.
	 */
	private String headerStep(final Random r) {
		final int n = grid.getItemCount();
		final int[] visible = new int[grid.getColumnCount()];
		int v = 0;
		for (int c = 0; c < visible.length; c++) {
			if (grid.getColumn(c).getVisible()) {
				visible[v++] = c;
			}
		}
		if (n == 0 || v == 0) {
			return "noop";
		}
		final int[] masks = { 0, SWT.MOD2, SWT.MOD1, SWT.MOD1 | SWT.MOD2 };
		final int stateMask = masks[r.nextInt(masks.length)];
		final int col = visible[r.nextInt(v)];
		final int toCol = visible[r.nextInt(v)];
		final int row = r.nextInt(n);
		final int how = r.nextInt(4);
		final int toRow = Math.max(0, Math.min(n - 1, row + r.nextInt(7) - 3));
		final int scrollTo = r.nextInt(n);
		switch (r.nextInt(6)) {
		case 0:
		case 1: {
			final boolean group = r.nextBoolean();
			final Point point = GridHeaderGestures.columnHeader(grid, col, group);
			if (point == null) {
				return (group ? "group header " : "header ") + col + " none";
			}
			GridHeaderGestures.click(grid, point, stateMask);
			return (group ? "group header " : "header ") + col + "/" + stateMask;
		}
		case 2: {
			final Point point = GridHeaderGestures.rowHeader(grid, row, true);
			if (point == null) {
				return "row header " + row + " not shown";
			}
			GridHeaderGestures.click(grid, point, stateMask);
			return "row header " + row + "/" + stateMask;
		}
		case 3:
		case 4:
			return rowHeaderDrag(row, how, toRow, scrollTo, stateMask);
		default: {
			final Point press = GridHeaderGestures.columnHeader(grid, col, true);
			if (press == null) {
				return "column drag " + col + " no group";
			}
			GridHeaderGestures.press(grid, press, stateMask);
			final Point to = GridHeaderGestures.columnHeader(grid, toCol, false);
			if (to != null) {
				GridHeaderGestures.drag(grid, to, stateMask);
			}
			GridHeaderGestures.release(grid, to != null ? to : press, stateMask);
			return "column drag " + col + "/" + stateMask + " to " + (to != null ? String.valueOf(toCol) : "not shown");
		}
		}
	}

	/*
	 * a press on row {@code row}'s header (the top row pinned just above it), dragged to: 0 row toRow
	 * (shown with it), 1 below the Grid, 2 into the column header, 3 the top row of the view at
	 * scrollTo (the Grid scrolled there while the button is down); then released
	 */
	private String rowHeaderDrag(final int row, final int how, final int toRow, final int scrollTo,
			final int stateMask) {
		final int n = grid.getItemCount();
		final int window = Math.max(1, grid.getVisibleGridHeight() / (grid.getItemHeight() + 1));
		final int top = Math.max(0, Math.min((how == 0 ? Math.min(row, toRow) : row) - 1, n - window));
		if (pinTopAt(top) < 0) {
			return "row drag " + row + " top not pinned";
		}
		final Point press = GridHeaderGestures.rowHeader(grid, row, false);
		if (press == null) {
			return "row drag " + row + " not shown";
		}
		GridHeaderGestures.press(grid, press, stateMask);
		Point to = null;
		String where;
		switch (how) {
		case 0:
			to = GridHeaderGestures.rowHeader(grid, toRow, false);
			where = "row " + toRow;
			break;
		case 1:
			to = GridHeaderGestures.below(grid, press.x);
			where = "below";
			break;
		case 2:
			to = new Point(press.x, Math.max(1, grid.getHeaderHeight() / 2));
			where = "header";
			break;
		default:
			final int target = Math.max(0, Math.min(scrollTo, n - window));
			where = "top " + target;
			if (pinTopAt(target) >= 0) {
				to = GridHeaderGestures.rowHeader(grid, target, false);
			}
			break;
		}
		if (to != null) {
			GridHeaderGestures.drag(grid, to, stateMask);
		} else {
			where += " not shown";
		}
		GridHeaderGestures.release(grid, to != null ? to : press, stateMask);
		return "row drag " + row + "/" + stateMask + " to " + where;
	}

	/* where the Grid hit-tests cell (col, row) once it is shown (scrolled to when asked), or null */
	private Point shown(final int row, final int col, final boolean scroll) {
		final GridItem item = grid.getItem(row);
		final long deadline = System.nanoTime() + 1_000_000_000L;
		do {
			if (scroll) {
				grid.showItem(item);
				grid.showColumn(grid.getColumn(col));
			}
			settle();
			final Rectangle bounds = item.getBounds(col);
			final Point candidate = new Point(bounds.x + Math.min(4, bounds.width / 2), bounds.y + bounds.height / 2);
			final Point hit = bounds.width > 0 ? grid.getCell(candidate) : null;
			if (hit != null && hit.x == col && hit.y == row) {
				return candidate;
			}
		} while (System.nanoTime() < deadline);
		return null;
	}

	/* scrolls to top row target and waits until the Grid answers it three times in a row; -1 if not */
	private int pinTopAt(final int target) {
		final long deadline = System.nanoTime() + 1_000_000_000L;
		int stable = 0;
		while (stable < 3 && System.nanoTime() < deadline) {
			if (grid.getTopIndex() != target) {
				grid.setTopIndex(target);
			}
			settle();
			stable = grid.getTopIndex() == target ? stable + 1 : 0;
		}
		return stable >= 3 ? target : -1;
	}

	private String key(final int keyCode, final Random r) {
		final int[] masks = { 0, SWT.MOD2, SWT.MOD1, SWT.MOD1 | SWT.MOD2 };
		final Event e = new Event();
		e.keyCode = keyCode;
		e.stateMask = masks[r.nextInt(masks.length)];
		String pinned = "";
		if (keyCode == SWT.PAGE_UP || keyCode == SWT.PAGE_DOWN) {
			// Page Up/Down start from the top/bottom index, which follows the native scroll bar's
			// asynchronous state: pin it from the script's random sequence first
			final int top = pinTop(r);
			if (top < 0) {
				return "key " + keyCode + "/" + e.stateMask + " top not pinned";
			}
			pinned = " top " + top;
		}
		grid.notifyListeners(SWT.KeyDown, e);
		return "key " + keyCode + "/" + e.stateMask + pinned;
	}

	/*
	 * Scrolls to a top row drawn from the script's random sequence among those that need no clamp,
	 * and paints and dispatches until the Grid answers that top index on three reads in a row
	 * (bounded wait); returns the row, or -1 when it does not settle there.
	 */
	private int pinTop(final Random r) {
		final int n = grid.getItemCount();
		final int window = Math.max(1, grid.getVisibleGridHeight() / (grid.getItemHeight() + 1));
		final int target = n > window ? r.nextInt(n - window + 1) : 0;
		final long deadline = System.nanoTime() + 1_000_000_000L;
		int stable = 0;
		while (stable < 3 && System.nanoTime() < deadline) {
			if (grid.getTopIndex() != target) {
				grid.setTopIndex(target);
			}
			settle();
			stable = grid.getTopIndex() == target ? stable + 1 : 0;
		}
		return stable >= 3 ? target : -1;
	}

	/*
	 * A click on a row the script picks from its own random sequence (never from the top index,
	 * which follows the native scroll bar's asynchronous state): the row is shown, the Grid's
	 * geometry settles, and the click goes where the Grid hit-tests that row.
	 */
	private String click(final Random r) {
		final int n = grid.getItemCount();
		if (n == 0) {
			return "noop";
		}
		final int row = r.nextInt(n);
		final int[] masks = { 0, SWT.MOD2, SWT.MOD1, SWT.MOD1 | SWT.MOD2 };
		final int stateMask = masks[r.nextInt(masks.length)];
		final GridItem item = grid.getItem(row);
		Point point = null;
		final long deadline = System.nanoTime() + 2_000_000_000L;
		do {
			// shown again on every pass: a late native scroll can move the row out of view
			grid.showItem(item);
			settle();
			final Rectangle bounds = item.getBounds(0);
			final Point candidate = new Point(bounds.x + 4, bounds.y + bounds.height / 2);
			if (grid.getItem(candidate) == item) {
				point = candidate;
			}
		} while (point == null && System.nanoTime() < deadline);
		if (point == null) {
			return "click " + row + " not shown";
		}
		final Event down = new Event();
		down.button = 1;
		down.x = point.x;
		down.y = point.y;
		down.stateMask = stateMask;
		grid.notifyListeners(SWT.MouseDown, down);
		final Event up = new Event();
		up.button = 1;
		up.x = down.x;
		up.y = down.y;
		up.stateMask = down.stateMask | SWT.BUTTON1;
		grid.notifyListeners(SWT.MouseUp, up);
		return "click " + row + "/" + down.stateMask;
	}

	/* paint and dispatch as the event loop would */
	private void settle() {
		if (!grid.isDisposed() && !Boolean.getBoolean("grid.selection.script.unpainted")) {
			grid.update();
			while (grid.getDisplay().readAndDispatch()) {
				// drain
			}
		}
	}

	private void observe(final long seed, final int k, final String op) {
		// paint as the event loop would: the scroll bars catch up with deferred row changes (a
		// removed row without an item defers them to the paint, one with an item updates at once)
		settle();
		final StringBuilder b = new StringBuilder();
		b.append(k).append(' ').append(op).append(" | n=").append(grid.getItemCount());
		probe(b, " count=", () -> Integer.valueOf(grid.getSelectionCount()));
		probe(b, " index=", () -> Integer.valueOf(grid.getSelectionIndex()));
		probe(b, " indices=", () -> Arrays.toString(grid.getSelectionIndices()));
		probe(b, " focus=", () -> {
			final GridItem focus = grid.getFocusItem();
			return Integer.valueOf(focus == null || focus.isDisposed() ? -1 : grid.indexOf(focus));
		});
		b.append(" events=").append(events);
		events.clear();
		if (grid.getCellSelectionEnabled()) {
			probe(b, " cells=", () -> grid.getCellSelectionCount() + Arrays.toString(grid.getCellSelection()));
			if (cellOps) {
				probe(b, " focusCell=", () -> String.valueOf(grid.getFocusCell()));
			}
		}
		if (k % 7 == 6) {
			probe(b, " selection=", () -> {
				final GridItem[] selection = grid.getSelection();
				final int[] rows = new int[selection.length];
				for (int i = 0; i < rows.length; i++) {
					rows[i] = grid.indexOf(selection[i]);
				}
				return Arrays.toString(rows);
			});
			final Random sample = new Random(seed * 31 + k);
			final int n = grid.getItemCount();
			for (int i = 0; i < 4 && n > 0; i++) {
				final int row = sample.nextInt(n);
				probe(b, " sel(" + row + ")=", () -> Boolean.valueOf(grid.isSelected(row)));
			}
		}
		trace.add(b.toString());
	}

	/* one observation; a failure is part of the trace (the same failure must happen on both) */
	private static void probe(final StringBuilder b, final String label, final java.util.function.Supplier<Object> value) {
		b.append(label);
		try {
			b.append(value.get());
		} catch (final RuntimeException failure) {
			b.append("EXC ").append(failure.getClass().getSimpleName());
		}
	}
}
