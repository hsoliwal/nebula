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
package org.eclipse.nebula.widgets.m3.tests;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Random;

import org.eclipse.nebula.widgets.m3.M3HeatMap;
import org.eclipse.nebula.widgets.m3.M3StackLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assume;
import org.junit.Test;

/**
 * Optimization pass measurements of the M3 widgets (C:/m3-superset/tools/OPTIMIZATION_PASS.md): wall time (median of
 * 5 rounds) and bytes allocated on the UI thread per operation, at the sizes the pass asks for. Prints one line per
 * measurement: <code>bench &lt;widget&gt; &lt;operation&gt; &lt;size&gt; &lt;value&gt; &lt;unit&gt;</code>. Opt-in:
 * <code>-Dm3.bench=true</code>.
 */
public class M3OptimizationBench {

	static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

	Display display;

	@Test
	public void measure() {
		Assume.assumeTrue("-Dm3.bench=true is not set", Boolean.getBoolean("m3.bench"));
		display = Display.getDefault();
		for (int children : new int[] { 1_000, 5_000 }) {
			stackChildren(children, false);
			stackChildren(children, true);
		}
		for (int pages : new int[] { 100_000, 1_000_000 }) {
			stackPages(pages);
		}
		for (int side : new int[] { 316, 1000 }) {
			heatMap(side);
		}
	}

	static void print(String widget, String operation, long size, double value, String unit) {
		System.out.println("bench " + widget + " " + operation + " " + size + " " + String.format(java.util.Locale.ROOT, "%.1f", value) + " " + unit);
	}

	/* layout after the top control changes, StackLayout versus M3StackLayout over n live children */
	void stackChildren(int n, boolean m3) {
		Shell shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(0, 0, 400, 300);
		shell.setLayout(new FillLayout());
		Composite composite = new Composite(shell, SWT.NONE);
		StackLayout layout = m3 ? new M3StackLayout() : new StackLayout();
		composite.setLayout(layout);
		Control[] children = new Control[n];
		for (int i = 0; i < n; i++) {
			Label label = new Label(composite, SWT.NONE);
			label.setText("child " + i);
			children[i] = label;
		}
		shell.open();
		layout.topControl = children[0];
		composite.layout();
		drain();
		long[] time = new long[5], bytes = new long[5];
		Random random = new Random(1);
		for (int k = 0; k < time.length; k++) {
			layout.topControl = children[random.nextInt(n)];
			long a = THREADS.getCurrentThreadAllocatedBytes();
			long start = System.nanoTime();
			composite.layout();
			time[k] = System.nanoTime() - start;
			bytes[k] = THREADS.getCurrentThreadAllocatedBytes() - a;
			drain();
		}
		String widget = m3 ? "M3StackLayout" : "StackLayout";
		print(widget, "layout-after-top-change", n, median(time) / 1000.0, "us");
		print(widget, "layout-after-top-change-alloc", n, median(bytes), "bytes");
		shell.dispose();
	}

	/* showPage of a random page among n declared pages */
	void stackPages(int n) {
		Shell shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(0, 0, 400, 300);
		shell.setLayout(new FillLayout());
		Composite composite = new Composite(shell, SWT.NONE);
		M3StackLayout layout = new M3StackLayout();
		composite.setLayout(layout);
		layout.setPages(composite, n, (parent, index) -> {
			Label label = new Label(parent, SWT.NONE);
			label.setText("page " + index);
			return label;
		});
		shell.open();
		Random random = new Random(2);
		for (int i = 0; i < 64; i++) {
			layout.showPage(random.nextInt(n));
		}
		drain();
		long[] time = new long[5], bytes = new long[5], hit = new long[5];
		for (int k = 0; k < time.length; k++) {
			int page = random.nextInt(n);
			long a = THREADS.getCurrentThreadAllocatedBytes();
			long start = System.nanoTime();
			layout.showPage(page);
			time[k] = System.nanoTime() - start;
			bytes[k] = THREADS.getCurrentThreadAllocatedBytes() - a;
			start = System.nanoTime();
			layout.showPage(page);
			hit[k] = System.nanoTime() - start;
			drain();
		}
		print("M3StackLayout", "showPage-miss", n, median(time) / 1000.0, "us");
		print("M3StackLayout", "showPage-miss-alloc", n, median(bytes), "bytes");
		print("M3StackLayout", "showPage-resident", n, median(hit) / 1000.0, "us");
		print("M3StackLayout", "resident-controls", n, layout.getResidentCount(), "controls");
		shell.dispose();
	}

	/* a full repaint of the client area and one cell update of a side x side heat map */
	void heatMap(int side) {
		Shell shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(0, 0, 640, 480);
		shell.setLayout(new FillLayout());
		M3HeatMap map = new M3HeatMap(shell, SWT.H_SCROLL | SWT.V_SCROLL);
		map.setCellSize(4, 4);
		map.setGridSize(side, side);
		float[] values = new float[side * side];
		Random random = new Random(3);
		for (int i = 0; i < values.length; i++) {
			values[i] = random.nextFloat();
		}
		map.setValues(values);
		shell.open();
		drain();
		long[] time = new long[5], bytes = new long[5];
		for (int k = 0; k < time.length; k++) {
			map.setOrigin(random.nextInt(side * 4), random.nextInt(side * 4));
			drain();
			long a = THREADS.getCurrentThreadAllocatedBytes();
			long start = System.nanoTime();
			map.redraw();
			map.update();
			time[k] = System.nanoTime() - start;
			bytes[k] = THREADS.getCurrentThreadAllocatedBytes() - a;
		}
		long cells = (long) side * side;
		print("M3HeatMap", "paint-client-area", cells, median(time) / 1000.0, "us");
		print("M3HeatMap", "paint-client-area-alloc", cells, median(bytes), "bytes");
		long a = THREADS.getCurrentThreadAllocatedBytes();
		long start = System.nanoTime();
		map.setValues(values);
		print("M3HeatMap", "setValues", cells, (System.nanoTime() - start) / 1000.0, "us");
		print("M3HeatMap", "setValues-alloc", cells, THREADS.getCurrentThreadAllocatedBytes() - a, "bytes");
		shell.dispose();
	}

	void drain() {
		while (display.readAndDispatch()) {
			// dispatch everything queued
		}
	}

	static long median(long[] values) {
		long[] sorted = values.clone();
		Arrays.sort(sorted);
		return sorted[sorted.length / 2];
	}
}
