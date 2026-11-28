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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Test;

/**
 * Differential test of the header and drag gestures on a virtual Grid whose cells are blocks: the
 * same gestures run on a Grid whose cells are blocks, on one whose cells are the point list of the
 * earlier releases (the oracle, whose range loops make an item per row) and, for the fixed scenario,
 * on a Grid that is not virtual, and all leave the same trace after every gesture: the cells in
 * order, the focus cell and item, the Selection and DefaultSelection events (row of event.item,
 * detail, state mask), the selected rows (count, indices, getSelection) and sampled isSelected and
 * isCellSelected answers. The fixed scenario clicks column and column group headers and row headers
 * with shift and ctrl, drags row headers down past the last row, up after a scroll and back to the
 * anchor, drags across column headers both ways, and ctrl+drags over an earlier selection, with a
 * hidden and an unselectable column; the seeded scripts mix such gestures with the cell steps of the
 * cell selection scripts (cells, columns removed, inserted, moved, hidden, made unselectable, rows
 * removed and inserted, keys, cell clicks and drags) in SINGLE and MULTI.
 */
public class GridHeaderDragSelectionDifferential_Test {

	private static final int ROWS = 400;
	private static final int STEPS = 250;
	private Shell shell;

	@After
	public void tearDown() {
		Grid.cellSelectionByBlocks = true;
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	/* a Grid of 5 columns (1 and 2 in a column group) with row and column headers */
	private Grid grid(final boolean blocks, final boolean virtual, final int style) {
		Grid.cellSelectionByBlocks = blocks;
		shell = new Shell(Display.getDefault());
		shell.setSize(520, 360);
		final Grid grid = new Grid(shell, (virtual ? SWT.VIRTUAL : SWT.NONE) | SWT.V_SCROLL | SWT.H_SCROLL | style);
		Grid.cellSelectionByBlocks = true;
		grid.setBounds(0, 0, 500, 300);
		grid.setHeaderVisible(true);
		grid.setRowHeaderVisible(true);
		final GridColumnGroup group = new GridColumnGroup(grid, SWT.NONE);
		group.setText("group");
		for (int c = 0; c < 5; c++) {
			final GridColumn column = c == 1 || c == 2 ? new GridColumn(group, SWT.NONE) : new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setWidth(70);
		}
		grid.setCellSelectionEnabled(true);
		return grid;
	}

	private static String describe(final Grid grid, final String type, final Event e) {
		final int row = e.item instanceof GridItem && !e.item.isDisposed() ? grid.indexOf((GridItem) e.item) : -1;
		return type + row + "/" + e.detail + "/" + e.stateMask;
	}

	/* what the Grid answers, as text */
	private static String answers(final Grid grid, final List<String> events) {
		final StringBuilder b = new StringBuilder();
		b.append("cells=").append(grid.getCellSelectionCount()).append(Arrays.toString(grid.getCellSelection()));
		b.append(" focusCell=").append(grid.getFocusCell());
		final GridItem focus = grid.getFocusItem();
		b.append(" focus=").append(focus == null || focus.isDisposed() ? -1 : grid.indexOf(focus));
		b.append(" events=").append(events);
		events.clear();
		b.append(" count=").append(grid.getSelectionCount());
		b.append(" indices=").append(Arrays.toString(grid.getSelectionIndices()));
		final GridItem[] selection = grid.getSelection();
		final int[] rows = new int[selection.length];
		for (int i = 0; i < rows.length; i++) {
			rows[i] = grid.indexOf(selection[i]);
		}
		b.append(" selection=").append(Arrays.toString(rows));
		final int n = grid.getItemCount();
		for (int i = 0; i < 6 && n > 0; i++) {
			final int row = (int) ((i * 7919L + 13) % n);
			b.append(" sel(").append(row).append(")=").append(grid.isSelected(row));
			b.append(grid.isCellSelected(new Point(i % 5, row)) ? "+" : "-");
		}
		return b.toString();
	}

	private static Point rowHeader(final Grid grid, final int row) {
		final Point point = GridHeaderGestures.rowHeader(grid, row, false);
		assertTrue("row " + row + " header not shown", point != null);
		return point;
	}

	private static Point header(final Grid grid, final int col, final boolean group) {
		final Point point = GridHeaderGestures.columnHeader(grid, col, group);
		assertTrue("column " + col + " header not shown", point != null);
		return point;
	}

	/* scrolls to top row top and waits until the Grid answers it */
	private static void top(final Grid grid, final int top) {
		final long deadline = System.nanoTime() + 5_000_000_000L;
		do {
			grid.setTopIndex(top);
			GridHeaderGestures.settle(grid);
		} while (grid.getTopIndex() != top && System.nanoTime() < deadline);
		assertEquals("top index", top, grid.getTopIndex());
	}

	/* the fixed scenario on one Grid: the answers after every gesture */
	private List<String> scenario(final boolean blocks, final boolean virtual, final int style) {
		final Grid grid = grid(blocks, virtual, style);
		final List<String> events = new ArrayList<>();
		grid.addListener(SWT.Selection, e -> events.add(describe(grid, "S", e)));
		grid.addListener(SWT.DefaultSelection, e -> events.add(describe(grid, "D", e)));
		if (virtual) {
			grid.addListener(SWT.SetData, e -> ((GridItem) e.item).setText(0, "r" + e.index));
			grid.setItemCount(ROWS);
		} else {
			for (int i = 0; i < ROWS; i++) {
				new GridItem(grid, SWT.NONE).setText(0, "r" + i);
			}
		}
		shell.open();
		top(grid, 0);
		final List<String> trace = new ArrayList<>();
		final int last = ROWS - 1;
		final int window = Math.max(1, grid.getVisibleGridHeight() / (grid.getItemHeight() + 1));
		final int[] masks = { 0, SWT.MOD1, SWT.MOD2, SWT.MOD1 | SWT.MOD2 };

		// column headers, with each state mask, twice (ctrl on a selected column takes it back)
		for (final int mask : masks) {
			for (int k = 0; k < 2; k++) {
				GridHeaderGestures.click(grid, header(grid, 3, false), mask);
				trace.add("header 3/" + mask + " " + answers(grid, events));
			}
		}
		// the column group header, then a drag across the column headers right, then left
		for (final int mask : masks) {
			GridHeaderGestures.press(grid, header(grid, 1, true), mask);
			trace.add("group/" + mask + " " + answers(grid, events));
			GridHeaderGestures.drag(grid, header(grid, 4, false), mask);
			trace.add("group drag 4/" + mask + " " + answers(grid, events));
			GridHeaderGestures.drag(grid, header(grid, 0, false), mask);
			trace.add("group drag 0/" + mask + " " + answers(grid, events));
			GridHeaderGestures.release(grid, header(grid, 0, false), mask);
			trace.add("group release/" + mask + " " + answers(grid, events));
		}
		// row headers: a click, ctrl+clicks, shift+click down and, after a scroll, up
		top(grid, 0);
		for (final int mask : masks) {
			GridHeaderGestures.click(grid, rowHeader(grid, 4), mask);
			trace.add("row 4/" + mask + " " + answers(grid, events));
			GridHeaderGestures.click(grid, rowHeader(grid, 2), mask);
			trace.add("row 2/" + mask + " " + answers(grid, events));
		}
		GridHeaderGestures.click(grid, rowHeader(grid, 5), 0);
		top(grid, last - window + 1);
		GridHeaderGestures.click(grid, rowHeader(grid, last - 1), SWT.MOD2);
		trace.add("row shift down " + answers(grid, events));
		GridHeaderGestures.click(grid, rowHeader(grid, last - 3), 0);
		top(grid, 0);
		GridHeaderGestures.click(grid, rowHeader(grid, 3), SWT.MOD2);
		trace.add("row shift up " + answers(grid, events));
		// row-header drags: down past the last row, back up to a shown row, back to the anchor
		for (final int mask : new int[] { 0, SWT.MOD1 }) {
			top(grid, 0);
			final Point press = rowHeader(grid, 6);
			GridHeaderGestures.press(grid, press, mask);
			trace.add("drag press/" + mask + " " + answers(grid, events));
			GridHeaderGestures.drag(grid, GridHeaderGestures.below(grid, press.x), mask);
			trace.add("drag below/" + mask + " " + answers(grid, events));
			GridHeaderGestures.drag(grid, rowHeader(grid, last - 4), mask);
			trace.add("drag back/" + mask + " " + answers(grid, events));
			top(grid, 0);
			GridHeaderGestures.drag(grid, rowHeader(grid, 6), mask);
			trace.add("drag anchor/" + mask + " " + answers(grid, events));
			GridHeaderGestures.drag(grid, rowHeader(grid, 2), mask);
			trace.add("drag above anchor/" + mask + " " + answers(grid, events));
			GridHeaderGestures.release(grid, rowHeader(grid, 2), mask);
			trace.add("drag release/" + mask + " " + answers(grid, events));
		}
		// a row-header drag up after a scroll, over the earlier selection with ctrl, then into the header
		for (final int mask : new int[] { 0, SWT.MOD1 }) {
			top(grid, last - window + 1);
			GridHeaderGestures.press(grid, rowHeader(grid, last - 2), mask);
			top(grid, ROWS / 2);
			GridHeaderGestures.drag(grid, rowHeader(grid, ROWS / 2 + 1), mask);
			trace.add("drag up/" + mask + " " + answers(grid, events));
			final Point into = new Point(rowHeader(grid, ROWS / 2 + 1).x, Math.max(1, grid.getHeaderHeight() / 2));
			GridHeaderGestures.drag(grid, into, mask);
			trace.add("drag header/" + mask + " " + answers(grid, events));
			GridHeaderGestures.release(grid, into, mask);
			trace.add("drag up release/" + mask + " " + answers(grid, events));
		}
		// a ctrl+drag over cells selected by column: rows partly selected split the drag's rows
		top(grid, 0);
		GridHeaderGestures.click(grid, header(grid, 3, false), 0);
		GridHeaderGestures.click(grid, rowHeader(grid, 8), SWT.MOD1);
		top(grid, ROWS / 2);
		GridHeaderGestures.press(grid, rowHeader(grid, ROWS / 2 + 3), SWT.MOD1);
		top(grid, 0);
		GridHeaderGestures.drag(grid, rowHeader(grid, 1), SWT.MOD1);
		GridHeaderGestures.release(grid, rowHeader(grid, 1), SWT.MOD1);
		trace.add("ctrl drag up over a column " + answers(grid, events));
		// a hidden and an unselectable column
		grid.getColumn(0).setVisible(false);
		grid.getColumn(3).setCellSelectionEnabled(false);
		top(grid, 0);
		GridHeaderGestures.click(grid, header(grid, 3, false), 0);
		trace.add("unselectable header " + answers(grid, events));
		GridHeaderGestures.click(grid, header(grid, 3, false), SWT.MOD1);
		trace.add("unselectable ctrl header " + answers(grid, events));
		final Point press = rowHeader(grid, 3);
		GridHeaderGestures.press(grid, press, 0);
		GridHeaderGestures.drag(grid, GridHeaderGestures.below(grid, press.x), 0);
		GridHeaderGestures.release(grid, GridHeaderGestures.below(grid, press.x), 0);
		trace.add("hidden column drag " + answers(grid, events));
		GridHeaderGestures.click(grid, header(grid, 1, true), SWT.MOD1);
		trace.add("hidden column group " + answers(grid, events));
		shell.dispose();
		return trace;
	}

	private void compareScenario(final int style) {
		final List<String> oracle = scenario(false, true, style);
		final List<String> blocks = scenario(true, true, style);
		final List<String> plain = scenario(true, false, style);
		assertEquals(oracle.size(), blocks.size());
		for (int i = 0; i < oracle.size(); i++) {
			assertEquals("step " + i, oracle.get(i), blocks.get(i));
			assertEquals("not virtual, step " + i, oracle.get(i), plain.get(i));
		}
	}

	@Test
	public void multiSelectionGesturesMatchThePointList() {
		compareScenario(SWT.MULTI);
	}

	@Test
	public void singleSelectionGesturesMatchThePointList() {
		compareScenario(SWT.SINGLE);
	}

	private List<String> script(final boolean blocks, final int style, final boolean spans, final long seed) {
		return GridSelectionScript.trace(() -> {
			final Grid grid = grid(blocks, true, style);
			final GridSelectionScript script = new GridSelectionScript(grid);
			script.cellOps = true;
			script.headerOps = true;
			script.spans = spans;
			if (blocks) {
				script.afterStep = grid::checkCellSelection;
			}
			grid.setItemCount(300);
			shell.open();
			return script;
		}, seed, STEPS);
	}

	private void compareScripts(final int style, final boolean spans, final int seeds) {
		// the original mode keeps the stock point list on both sides: a seed suffices
		final int runs = GridTestUtil.fixesUpstreamDefects() ? seeds : 1;
		for (long seed = 1; seed <= runs; seed++) {
			final List<String> oracle = script(false, style, spans, seed);
			final List<String> cells = script(true, style, spans, seed);
			for (int i = 0; i < oracle.size(); i++) {
				assertEquals("seed " + seed + " step " + i, oracle.get(i), cells.get(i));
			}
		}
	}

	@Test
	public void multiSelectionHeaderScriptsMatchThePointList() {
		compareScripts(SWT.MULTI, false, 6);
	}

	@Test
	public void singleSelectionHeaderScriptsMatchThePointList() {
		compareScripts(SWT.SINGLE, false, 2);
	}

	@Test
	public void headerScriptsWithColumnSpansMatchThePointList() {
		compareScripts(SWT.MULTI, true, 2);
	}
}
