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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

/**
 * Measurements of the optimization pass (C:/m3-superset/tools/OPTIMIZATION_PASS.md) for the Grid: one operation at
 * one size per JVM, median of the repetitions, wall time and bytes allocated on the UI thread, plus the Grid's own
 * counters (body paints, rows culled by the paint clip, items made). Runs on the hidden desktop.
 * <p>
 * Usage: {@code GridOptimizationBench <operation> <size> <out.tsv>}; operations: {@code expand-eager},
 * {@code expand-sparse}, {@code row-height}, {@code frozen-hscroll}, {@code cell-toggle}, {@code page-scroll}.
 * Each result is one TSV line: mode, operation, size, metric, value, unit.
 * </p>
 */
public final class GridOptimizationBench {
	private static final Logger LOG = Logger.getLogger("org.eclipse.nebula.widgets.grid");
	private static final int REPS = 5;

	private final Display display = new Display();
	private final Shell shell = new Shell(display);
	private final String operation;
	private final int size;
	private final Path out;

	private GridOptimizationBench(final String operation, final int size, final Path out) {
		this.operation = operation;
		this.size = size;
		this.out = out;
		shell.setBounds(10, 10, 640, 520);
		final Button sink = new Button(shell, SWT.PUSH);
		sink.setBounds(600, 490, 10, 10);
		shell.open();
		sink.setFocus();
	}

	public static void main(final String[] args) throws IOException {
		final GridOptimizationBench bench = new GridOptimizationBench(args[0], Integer.parseInt(args[1]), Paths.get(args[2]));
		try {
			bench.run();
		} finally {
			bench.display.dispose();
		}
	}

	private void run() throws IOException {
		switch (operation) {
		case "expand-eager":
			expand(false);
			break;
		case "expand-sparse":
			expand(true);
			break;
		case "row-height":
			rowHeight();
			break;
		case "frozen-hscroll":
			frozenHorizontalScroll();
			break;
		case "cell-toggle":
			cellToggle();
			break;
		case "page-scroll":
			pageScroll();
			break;
		default:
			throw new IllegalArgumentException(operation);
		}
	}

	/* one root with size children (eager items or setItemCount rows), expanded and collapsed */
	private void expand(final boolean sparse) throws IOException {
		final Grid grid = grid(sparse ? SWT.VIRTUAL : SWT.NONE, 1);
		grid.getColumn(0).setTree(true);
		if (sparse) {
			grid.addListener(SWT.SetData, e -> ((GridItem) e.item).setText("row " + e.index));
		}
		final long build = System.nanoTime();
		final GridItem root = new GridItem(grid, SWT.NONE);
		root.setText("root");
		if (sparse) {
			root.setItemCount(size);
		} else {
			for (int i = 0; i < size; i++) {
				new GridItem(root, SWT.NONE).setText("row " + i);
			}
		}
		new GridItem(grid, SWT.NONE).setText("after");
		flush();
		report("build", (System.nanoTime() - build) / 1_000_000, "ms");
		final long[][] expand = new long[2][REPS];
		final long[][] collapse = new long[2][REPS];
		for (int r = 0; r < REPS; r++) {
			measure(expand, r, () -> root.setExpanded(true), grid);
			measure(collapse, r, () -> root.setExpanded(false), grid);
		}
		report("expand", median(expand[0]) / 1_000, "us");
		report("expand_alloc", median(expand[1]), "bytes");
		report("collapse", median(collapse[0]) / 1_000, "us");
		report("collapse_alloc", median(collapse[1]), "bytes");
		report("items_made", grid.virtualMaterializedItemCount(), "items");
	}

	/* size flat rows: the Grid's item height for all rows, and one row's height off and on screen */
	private void rowHeight() throws IOException {
		final Grid grid = grid(SWT.NONE, 3);
		for (int i = 0; i < size; i++) {
			new GridItem(grid, SWT.NONE).setText("row " + i);
		}
		flush();
		final long[][] all = new long[2][REPS];
		final long[][] off = new long[2][REPS];
		final long[][] on = new long[2][REPS];
		final GridItem far = grid.getItem(size - 2);
		final GridItem near = grid.getItem(2);
		for (int r = 0; r < REPS; r++) {
			final int h = 18 + r % 2 * 4;
			measure(all, r, () -> grid.setItemHeight(h), grid);
			measure(off, r, () -> far.setHeight(h + 7), grid);
			measure(on, r, () -> near.setHeight(h + 9), grid);
		}
		report("set_item_height", median(all[0]) / 1_000, "us");
		report("set_item_height_alloc", median(all[1]), "bytes");
		report("row_height_off_screen", median(off[0]) / 1_000, "us");
		report("row_height_on_screen", median(on[0]) / 1_000, "us");
	}

	/* size rows, 30 columns, the first frozen: one horizontal scroll step of a column width */
	private void frozenHorizontalScroll() throws IOException {
		final Grid grid = grid(SWT.NONE, 30);
		grid.getColumn(0).setFixed(true);
		for (int i = 0; i < size; i++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			item.setText(0, "row " + i);
			item.setText(1, "value " + i);
		}
		flush();
		final ScrollBar bar = grid.getHorizontalBar();
		final int steps = 20;
		final long[][] step = new long[2][steps];
		final int paints = grid.bodyPaintCount;
		for (int s = 0; s < steps; s++) {
			final int selection = (s % 10 + 1) * 37;
			measure(step, s, () -> {
				bar.setSelection(selection);
				bar.notifyListeners(SWT.Selection, new Event());
			}, grid);
		}
		report("hscroll_step", median(step[0]) / 1_000, "us");
		report("hscroll_step_alloc", median(step[1]), "bytes");
		report("body_paints_per_step", (grid.bodyPaintCount - paints) / steps, "paints");
		report("culled_rows_last_paint", grid.paintCulledRows, "rows");
	}

	/* size cells selected (size / 4 rows x 4 columns): one cell deselected and selected again */
	private void cellToggle() throws IOException {
		final Grid grid = grid(SWT.MULTI, 4);
		grid.setCellSelectionEnabled(true);
		final int rows = size / 4;
		for (int i = 0; i < rows; i++) {
			new GridItem(grid, SWT.NONE).setText("row " + i);
		}
		flush();
		long t = System.nanoTime();
		grid.selectAllCells();
		report("select_all_cells", (System.nanoTime() - t) / 1_000_000, "ms");
		final int toggles = 50;
		final long[][] toggle = new long[2][toggles];
		for (int i = 0; i < toggles; i++) {
			final Point cell = new Point(i % 4, rows / 2 + i);
			measure(toggle, i, () -> {
				grid.deselectCell(cell);
				grid.selectCell(cell);
			}, grid);
		}
		report("cell_toggle", median(toggle[0]) / 1_000, "us");
		report("cell_toggle_alloc", median(toggle[1]), "bytes");
		t = System.nanoTime();
		grid.deselectAllCells();
		report("deselect_all_cells", (System.nanoTime() - t) / 1_000_000, "ms");
	}

	/* size flat rows: one page down by the scroll bar, as a page-down click */
	private void pageScroll() throws IOException {
		final Grid grid = grid(SWT.NONE, 3);
		for (int i = 0; i < size; i++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			item.setText(0, "row " + i);
			item.setText(1, "value " + i);
		}
		flush();
		final ScrollBar bar = grid.getVerticalBar();
		final int steps = 20;
		final long[][] page = new long[2][steps];
		final int start = size / 2;
		for (int s = 0; s < steps; s++) {
			final int selection = start + s * bar.getThumb();
			measure(page, s, () -> {
				bar.setSelection(selection);
				bar.notifyListeners(SWT.Selection, new Event());
			}, grid);
		}
		report("page_scroll", median(page[0]) / 1_000, "us");
		report("page_scroll_alloc", median(page[1]), "bytes");
		report("culled_rows_last_paint", grid.paintCulledRows, "rows");
	}

	private Grid grid(final int style, final int columns) {
		final Grid grid = new Grid(shell, style | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 590, 480);
		grid.setHeaderVisible(true);
		for (int c = 0; c < columns; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setWidth(100);
		}
		return grid;
	}

	/* one repetition: the operation, then its paint; time in [0][rep], bytes allocated in [1][rep] */
	private void measure(final long[][] into, final int rep, final Runnable op, final Grid grid) {
		flush();
		final long a0 = allocated();
		final long t0 = System.nanoTime();
		op.run();
		grid.update();
		into[0][rep] = System.nanoTime() - t0;
		into[1][rep] = allocated() - a0;
		flush();
	}

	private static long allocated() {
		return ((com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean()).getCurrentThreadAllocatedBytes();
	}

	private static long median(final long[] values) {
		final long[] sorted = values.clone();
		Arrays.sort(sorted);
		return sorted[sorted.length / 2];
	}

	private void report(final String metric, final long value, final String unit) throws IOException {
		final String line = (GridViewport.ENABLED ? "viewport" : "original") + "\t" + operation + "\t" + size + "\t"
				+ metric + "\t" + value + "\t" + unit;
		LOG.info(line);
		Files.write(out, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
				StandardOpenOption.APPEND);
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain paints and timers
		}
	}
}
