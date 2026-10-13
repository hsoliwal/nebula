/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid.bench;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

/**
 * Before/after bench for the Grid model leaf: 1M rows x 100 columns, {@code SWT.VIRTUAL}.
 *
 * <p>Usage: {@code GridModelBench <label> <driver-class> <rows> <columns> <out.tsv>}. The driver
 * is {@code ClassicDriver} (application-side index + {@code setItemCount} + {@code clearAll}, runs
 * on the unmodified and the new Grid) or {@code PackedDriver} (RowPermutation, new Grid only).
 * One TSV row per metric: label, driver, metric, value, unit.</p>
 */
public final class GridModelBench {

	/** Sort/filter/group strategy under test. */
	public interface Driver {
		void install(Grid grid, int rows);

		/** Model row shown at view index {@code index}. */
		int modelRow(int index);

		void sort(int[] keys, boolean descending);

		void filter(int[] keys, int modulus);

		void clear();

		/** Groups rows by {@code keys[m] % groups}; returns the group count. */
		int group(int[] keys, int groups);
	}

	private static final int FRAMES = 200;

	public static void main(final String[] args) throws Exception {
		final String label = args[0];
		final Driver driver = (Driver) Class.forName(args[1]).getDeclaredConstructor().newInstance();
		final int rows = Integer.parseInt(args[2]);
		final int columns = Integer.parseInt(args[3]);
		final Path out = Path.of(args[4]);
		final String driverName = driver.getClass().getSimpleName();
		final PrintWriter tsv = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND));
		final Report report = (metric, value, unit) -> {
			tsv.println(label + "\t" + driverName + "\t" + metric + "\t" + value + "\t" + unit);
			tsv.flush();
			System.out.println(label + " " + driverName + " " + metric + "=" + value + " " + unit);
		};

		final int[] keys = new int[rows];
		final Random random = new Random(20261013L);
		for (int m = 0; m < rows; m++) {
			keys[m] = random.nextInt(1 << 20);
		}

		final Display display = Display.getDefault();
		final Shell shell = new Shell(display);
		shell.setSize(1280, 800);
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setHeaderVisible(true);
		grid.setBounds(0, 0, 1260, 760);
		for (int c = 0; c < columns; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("C" + c);
			column.setWidth(80);
		}
		final int[] setData = new int[1];
		grid.addListener(SWT.SetData, e -> {
			setData[0]++;
			final GridItem item = (GridItem) e.item;
			final int m = driver.modelRow(e.index);
			for (int c = 0; c < columns; c++) {
				item.setText(c, cellText(m, c, keys));
			}
		});
		final long[] lastPaint = new long[1];
		grid.addListener(SWT.Paint, e -> lastPaint[0] = System.nanoTime());
		grid.setItemCount(rows);
		driver.install(grid, rows);

		final long open0 = System.nanoTime();
		shell.open();
		while (lastPaint[0] == 0L) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		report.put("first_paint", ms(lastPaint[0] - open0), "ms");
		drain(display);
		report.put("materialized_after_first_paint", materialized(grid), "items");

		timed(report, "sort_asc", () -> driver.sort(keys, false), display, grid);
		timed(report, "sort_desc", () -> driver.sort(keys, true), display, grid);
		final long alloc0 = allocated();
		driver.sort(keys, false);
		report.put("sort_repeat_allocated", allocated() - alloc0, "bytes");
		drain(display);
		timed(report, "filter_mod3", () -> driver.filter(keys, 3), display, grid);
		report.put("view_rows_after_filter", grid.getItemCount(), "rows");
		drain(display);
		final long g0 = System.nanoTime();
		final int groups = driver.group(keys, 1000);
		grid.update();
		final long g1 = System.nanoTime();
		report.put("group_1000", groups < 0 ? "NOT_IMPLEMENTED" : ms(g1 - g0), groups < 0 ? "-" : "ms");
		drain(display);
		timed(report, "clear_view", driver::clear, display, grid);
		report.put("materialized_after_model_ops", materialized(grid), "items");

		scroll(report, "vscroll", grid, display, true);
		scroll(report, "hscroll", grid, display, false);
		report.put("materialized_after_scroll", materialized(grid), "items");
		report.put("setdata_events", setData[0], "events");

		System.gc();
		System.gc();
		final Runtime rt = Runtime.getRuntime();
		report.put("heap_used_after_gc", (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024), "MiB");
		report.put("private_bytes", privateBytes() / (1024 * 1024), "MiB");
		shell.dispose();
		display.dispose();
		tsv.close();
	}

	private static String cellText(final int m, final int c, final int[] keys) {
		return c == 0 ? Integer.toString(keys[m]) : "r" + m + "c" + c;
	}

	private static void timed(final Report report, final String metric, final Runnable op, final Display display,
			final Grid grid) {
		drain(display);
		final long t0 = System.nanoTime();
		op.run();
		grid.update();
		final long t1 = System.nanoTime();
		report.put(metric, ms(t1 - t0), "ms");
		drain(display);
	}

	private static void scroll(final Report report, final String name, final Grid grid, final Display display,
			final boolean vertical) {
		final long[] frames = new long[FRAMES];
		final ScrollBar bar = vertical ? grid.getVerticalBar() : grid.getHorizontalBar();
		final int max = Math.max(1, bar.getMaximum() - bar.getThumb());
		final int step = Math.max(1, vertical ? Math.min(37, max / FRAMES) : Math.min(41, max / FRAMES));
		drain(display);
		for (int i = 0; i < FRAMES; i++) {
			final int target = Math.min(max, (i + 1) * step);
			final long t0 = System.nanoTime();
			if (vertical) {
				grid.setTopIndex(Math.min(grid.getItemCount() - 1, target));
			} else {
				bar.setSelection(target);
				bar.notifyListeners(SWT.Selection, new Event());
			}
			grid.update();
			frames[i] = System.nanoTime() - t0;
		}
		Arrays.sort(frames);
		report.put(name + "_p50", ms(frames[FRAMES / 2]), "ms");
		report.put(name + "_p95", ms(frames[(FRAMES * 95) / 100]), "ms");
		report.put(name + "_max", ms(frames[FRAMES - 1]), "ms");
		drain(display);
	}

	private static void drain(final Display display) {
		while (display.readAndDispatch()) {
			// drain
		}
	}

	private static String ms(final long nanos) {
		return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1e6);
	}

	private static long allocated() {
		final java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		return ((com.sun.management.ThreadMXBean) bean).getThreadAllocatedBytes(Thread.currentThread().getId());
	}

	private static int materialized(final Grid grid) {
		try {
			final Method m = Grid.class.getDeclaredMethod("virtualMaterializedItemCount");
			m.setAccessible(true);
			return (Integer) m.invoke(grid);
		} catch (final ReflectiveOperationException e) {
			return -1;
		}
	}

	private static long privateBytes() {
		final long pid = ProcessHandle.current().pid();
		try {
			final Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command",
					"(Get-Process -Id " + pid + ").PrivateMemorySize64").redirectErrorStream(true).start();
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				final String line = r.readLine();
				p.waitFor();
				return Long.parseLong(line.trim());
			}
		} catch (final Exception e) {
			return -1L;
		}
	}

	@FunctionalInterface
	interface Report {
		void put(String metric, Object value, String unit);
	}
}
