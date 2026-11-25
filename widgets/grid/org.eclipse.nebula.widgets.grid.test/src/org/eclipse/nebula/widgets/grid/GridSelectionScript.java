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

	GridSelectionScript(final Grid grid) {
		this.grid = grid;
		grid.addListener(SWT.Selection, e -> events.add(describe("S", e)));
		grid.addListener(SWT.DefaultSelection, e -> events.add(describe("D", e)));
		grid.addListener(SWT.SetData, e -> ((GridItem) e.item).setText(0, "r" + e.index));
	}

	private String describe(final String type, final Event e) {
		final int row = e.item instanceof GridItem && !e.item.isDisposed() ? grid.indexOf((GridItem) e.item) : -1;
		return type + row + "/" + e.detail + "/" + e.stateMask;
	}

	void run(final long seed, final int steps) {
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
			grid.remove(a);
			return "remove " + a;
		case 14:
			if (n == 0) {
				return "noop";
			}
			grid.remove(a, Math.min(n - 1, a + r.nextInt(5)));
			return "remove " + a + "..";
		case 15: {
			final int[] ix = indices(r, n);
			for (int i = 0; i < ix.length; i++) {
				ix[i] = Math.min(ix[i], n - 1);
			}
			if (n == 0 || ix.length == 0) {
				return "noop";
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

	private String key(final int keyCode, final Random r) {
		final int[] masks = { 0, SWT.MOD2, SWT.MOD1, SWT.MOD1 | SWT.MOD2 };
		final Event e = new Event();
		e.keyCode = keyCode;
		e.stateMask = masks[r.nextInt(masks.length)];
		grid.notifyListeners(SWT.KeyDown, e);
		return "key " + keyCode + "/" + e.stateMask;
	}

	private String click(final Random r) {
		final int n = grid.getItemCount();
		if (n == 0) {
			return "noop";
		}
		final int row = Math.min(n - 1, grid.getTopIndex() + r.nextInt(8));
		final Rectangle bounds = grid.getItem(row).getBounds(0);
		final int[] masks = { 0, SWT.MOD2, SWT.MOD1, SWT.MOD1 | SWT.MOD2 };
		final Event down = new Event();
		down.button = 1;
		down.x = bounds.x + 4;
		down.y = bounds.y + bounds.height / 2;
		down.stateMask = masks[r.nextInt(masks.length)];
		grid.notifyListeners(SWT.MouseDown, down);
		final Event up = new Event();
		up.button = 1;
		up.x = down.x;
		up.y = down.y;
		up.stateMask = down.stateMask | SWT.BUTTON1;
		grid.notifyListeners(SWT.MouseUp, up);
		return "click " + row + "/" + down.stateMask;
	}

	private void observe(final long seed, final int k, final String op) {
		// paint as the event loop would: the scroll bars catch up with deferred row changes (a
		// removed row without an item defers them to the paint, one with an item updates at once)
		if (!grid.isDisposed() && !Boolean.getBoolean("grid.selection.script.unpainted")) {
			grid.update();
			while (grid.getDisplay().readAndDispatch()) {
				// drain
			}
		}
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
