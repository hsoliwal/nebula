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
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * GridItem.setItemCount, clear(int, boolean) and clearAll(boolean) (G4 step 2, an additive API)
 * against SWT's TreeItem, their contract donor: seeded scripts run on a Tree and a Grid of the
 * same style, and every step answers alike and raises the same SetData and Dispose events. The
 * widgets are never shown, so data is requested only by reads, where the two coincide. A Dispose
 * listener sits on every item the script has fetched (a virtual Tree has no object for the
 * others), and each item gets an id when first fetched, in the same order on both sides.
 */
public class GridTreeItemContract_Test {
	private static final int ROOTS = 4, STEPS = 400;

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void virtualScriptsMatchTheTree() {
		for (int seed = 0; seed < 6; seed++) {
			run(seed, SWT.VIRTUAL);
		}
	}

	@Test
	public void plainScriptsMatchTheTree() {
		for (int seed = 10; seed < 14; seed++) {
			run(seed, SWT.NONE);
		}
	}

	@Test
	public void checkScriptsMatchTheTree() {
		for (int seed = 20; seed < 24; seed++) {
			run(seed, SWT.VIRTUAL | SWT.CHECK);
			run(seed + 100, SWT.CHECK);
		}
	}

	@Test
	public void negativeCountIsZeroAndDisposedReceiverThrows() {
		final Grid grid = new Grid(shell, SWT.VIRTUAL);
		final GridItem root = new GridItem(grid, SWT.NONE);
		root.setItemCount(3);
		assertEquals(3, root.getItemCount());
		assertEquals(4, grid.getItemCount());
		root.setItemCount(-5);
		assertEquals(0, root.getItemCount());
		assertEquals(1, grid.getItemCount());
		root.dispose();
		try {
			root.setItemCount(1);
			fail("disposed");
		} catch (final org.eclipse.swt.SWTException expected) {
			assertEquals(SWT.ERROR_WIDGET_DISPOSED, expected.code);
		}
	}

	private void run(final int seed, final int style) {
		final Side tree = new TreeSide(new Tree(shell, style), style);
		final Side grid = new GridSide(new Grid(shell, style), style);
		final Random random = new Random(0x7E1AL + seed);
		final StringBuilder log = new StringBuilder("seed " + seed + " style " + Integer.toHexString(style) + "\n");
		for (int step = 0; step < STEPS; step++) {
			final int[] op = { random.nextInt(11), random.nextInt(1 << 20), random.nextInt(1 << 20),
					random.nextInt(1 << 20) };
			log.append(step).append(": ").append(java.util.Arrays.toString(op)).append('\n');
			final String expected = tree.apply(op);
			final String actual = grid.apply(op);
			if (!expected.equals(actual)) {
				fail("step " + step + " differs\n  tree: " + expected + "\n  grid: " + actual + "\n" + log);
			}
			log.append("   = ").append(expected.length() > 200 ? expected.substring(0, 200) : expected).append('\n');
		}
		assertEquals("final structure\n" + log, tree.dump(), grid.dump());
		tree.dispose();
		grid.dispose();
	}

	/** One widget under the script; items are TreeItems or GridItems. */
	private abstract static class Side {
		private final StringBuilder events = new StringBuilder();
		private int ids;
		final boolean virtual, check;

		Side(final int style) {
			virtual = (style & SWT.VIRTUAL) != 0;
			check = (style & SWT.CHECK) != 0;
		}

		abstract Item root(int index);

		abstract int count(Item item);

		abstract Item child(Item item, int index);

		abstract void setCount(Item item, int count);

		abstract void clear(Item item, int index, boolean all);

		abstract void clearAll(Item item, boolean all);

		abstract String text(Item item);

		abstract void setText(Item item, String text);

		abstract boolean checked(Item item);

		abstract void setChecked(Item item, boolean checked);

		abstract Item parentOf(Item item);

		abstract int indexOf(Item parent, Item item);

		abstract boolean modeFixesStock();

		abstract void dispose();

		/* the id of an item, given when the script first holds it */
		final String id(final Item item) {
			if (item.getData("id") == null) {
				item.setData("id", "#" + ids++);
				item.addListener(SWT.Dispose, e -> events.append("Dispose(").append(item.getData("id")).append(") "));
			}
			return (String) item.getData("id");
		}

		final String path(final Item item) {
			final Item parent = parentOf(item);
			return parent == null ? (String) item.getData("root") : path(parent) + "." + indexOf(parent, item);
		}

		final void setData(final Event event) {
			final Item item = (Item) event.item;
			final String path = path(item);
			// the original mode gives a root its row, stock (D33): compared in the default mode only
			final String index = parentOf(item) == null && !modeFixesStock() ? "-" : String.valueOf(event.index);
			events.append("SetData(").append(id(item)).append(' ').append(path).append(" index ").append(index)
					.append(") ");
			setText(item, "t" + path);
		}

		final void named(final Item root, final int index) {
			root.setData("root", "R" + index);
			id(root);
		}

		/* a random item: a root, then down through random children */
		private Item pick(final int a) {
			Item item = root(a % ROOTS);
			int bits = a >> 2;
			while ((bits & 3) != 0 && count(item) > 0) {
				final int n = count(item);
				item = child(item, (bits >> 2) % n);
				bits >>= 7;
			}
			id(item);
			return item;
		}

		final String apply(final int[] op) {
			final Item item = pick(op[1]);
			final String id = id(item);
			String answer;
			try {
				answer = run(op[0], item, op[2], op[3]);
			} catch (final IllegalArgumentException | org.eclipse.swt.SWTException e) {
				answer = "threw " + e.getClass().getSimpleName() + " " + e.getMessage();
			}
			final String result = id + " " + answer + " | " + (modeFixesStock() ? events.toString() : sortDisposes());
			events.setLength(0);
			return result;
		}

		/*
		 * The original mode disposes descendants in stock's order (D34): compared as a set there.
		 * SetData events of a step all come before its disposal, so they keep their order.
		 */
		private String sortDisposes() {
			final StringBuilder kept = new StringBuilder();
			final java.util.List<String> disposes = new java.util.ArrayList<>();
			for (final String token : events.toString().split("(?<=\\) )")) {
				if (token.startsWith("Dispose(")) {
					disposes.add(token);
				} else {
					kept.append(token);
				}
			}
			java.util.Collections.sort(disposes);
			return kept + String.join("", disposes);
		}

		private String run(final int kind, final Item item, final int b, final int c) {
			switch (kind) {
			case 0:
			case 1:
				setCount(item, b % 4 == 0 ? b % 40 : b % 9 - 1);
				return "count " + count(item);
			case 2:
				return "text " + text(item);
			case 3: {
				final int n = count(item);
				// now and then one past the last child: ERROR_INVALID_RANGE
				clear(item, c % 7 == 0 ? n : n == 0 ? 0 : b % n, (c & 1) != 0);
				return "cleared";
			}
			case 4:
				clearAll(item, (c & 1) != 0);
				return "cleared all";
			case 5:
				return "count " + count(item);
			case 6:
				if (parentOf(item) != null && (b & 1) == 0) {
					final String id = id(item);
					item.dispose();
					return "disposed " + id;
				}
				return "kept";
			case 7:
				if (virtual && !modeFixesStock()) {
					// stock: a text set is no data, and SetData overwrites it (D35)
					return "skipped (D35)";
				}
				setText(item, "s" + b % 100);
				return "set";
			case 8:
				if (!modeFixesStock()) {
					return "skipped (stock getItem range, D30)";
				}
				final int n = count(item);
				child(item, (b & 1) == 0 ? n : -1 - b % 3);
				return "no exception";
			case 9:
				if (!check) {
					// a Tree without SWT.CHECK ignores setChecked; a Grid keeps it (outside this API)
					return "no check style";
				}
				if (virtual && !modeFixesStock()) {
					// stock: a check state set is no data (D35)
					return "skipped (D35)";
				}
				setChecked(item, (b & 1) == 0);
				return "checked " + checked(item);
			default:
				return check ? "checked " + checked(item) : "no check style";
			}
		}

		/* every item's count and text, depth first (materializes and requests all) */
		final String dump() {
			final StringBuilder out = new StringBuilder();
			for (int r = 0; r < ROOTS; r++) {
				dump(root(r), 0, out);
			}
			return out.toString();
		}

		private void dump(final Item item, final int depth, final StringBuilder out) {
			out.append(depth).append(' ').append(text(item)).append(" (").append(count(item)).append(")\n");
			for (int i = 0; i < count(item); i++) {
				dump(child(item, i), depth + 1, out);
			}
		}
	}

	private static final class TreeSide extends Side {
		private final Tree tree;

		TreeSide(final Tree tree, final int style) {
			super(style);
			this.tree = tree;
			for (int r = 0; r < ROOTS; r++) {
				named(new TreeItem(tree, SWT.NONE), r);
			}
			tree.addListener(SWT.SetData, this::setData);
		}

		@Override
		Item root(final int index) {
			return tree.getItem(index);
		}

		@Override
		int count(final Item item) {
			return ((TreeItem) item).getItemCount();
		}

		@Override
		Item child(final Item item, final int index) {
			return ((TreeItem) item).getItem(index);
		}

		@Override
		void setCount(final Item item, final int count) {
			((TreeItem) item).setItemCount(count);
		}

		@Override
		void clear(final Item item, final int index, final boolean all) {
			((TreeItem) item).clear(index, all);
		}

		@Override
		void clearAll(final Item item, final boolean all) {
			((TreeItem) item).clearAll(all);
		}

		@Override
		String text(final Item item) {
			return ((TreeItem) item).getText(0);
		}

		@Override
		void setText(final Item item, final String text) {
			((TreeItem) item).setText(0, text);
		}

		@Override
		boolean checked(final Item item) {
			return ((TreeItem) item).getChecked();
		}

		@Override
		void setChecked(final Item item, final boolean checked) {
			((TreeItem) item).setChecked(checked);
		}

		@Override
		Item parentOf(final Item item) {
			return ((TreeItem) item).getParentItem();
		}

		@Override
		int indexOf(final Item parent, final Item item) {
			return ((TreeItem) parent).indexOf((TreeItem) item);
		}

		@Override
		boolean modeFixesStock() {
			return GridTestUtil.fixesUpstreamDefects();
		}

		@Override
		void dispose() {
			tree.dispose();
		}
	}

	private static final class GridSide extends Side {
		private final Grid grid;

		GridSide(final Grid grid, final int style) {
			super(style);
			this.grid = grid;
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setTree(true);
			column.setWidth(120);
			for (int r = 0; r < ROOTS; r++) {
				named(new GridItem(grid, SWT.NONE), r);
			}
			grid.addListener(SWT.SetData, this::setData);
		}

		/*
		 * The original mode keeps stock's child getters, which request no data (D32); the script then
		 * requests the receiver's data where TreeItem's getters do, so the new API is compared alike
		 */
		private void emulateTreeRequest(final Item item) {
			if (virtual && !modeFixesStock()) {
				((GridItem) item).getText(0);
			}
		}

		@Override
		Item root(final int index) {
			return grid.getRootItem(index);
		}

		@Override
		int count(final Item item) {
			emulateTreeRequest(item);
			return ((GridItem) item).getItemCount();
		}

		@Override
		Item child(final Item item, final int index) {
			if (index >= 0) {
				emulateTreeRequest(item);
			}
			return ((GridItem) item).getItem(index);
		}

		@Override
		void setCount(final Item item, final int count) {
			((GridItem) item).setItemCount(count);
		}

		@Override
		void clear(final Item item, final int index, final boolean all) {
			((GridItem) item).clear(index, all);
		}

		@Override
		void clearAll(final Item item, final boolean all) {
			((GridItem) item).clearAll(all);
		}

		@Override
		String text(final Item item) {
			return ((GridItem) item).getText(0);
		}

		@Override
		void setText(final Item item, final String text) {
			((GridItem) item).setText(0, text);
		}

		@Override
		boolean checked(final Item item) {
			return ((GridItem) item).getChecked(0);
		}

		@Override
		void setChecked(final Item item, final boolean checked) {
			((GridItem) item).setChecked(0, checked);
		}

		@Override
		Item parentOf(final Item item) {
			return ((GridItem) item).getParentItem();
		}

		@Override
		int indexOf(final Item parent, final Item item) {
			return ((GridItem) parent).indexOf((GridItem) item);
		}

		@Override
		boolean modeFixesStock() {
			return GridTestUtil.fixesUpstreamDefects();
		}

		@Override
		void dispose() {
			grid.dispose();
		}
	}
}
