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
package org.eclipse.nebula.visualization.widgets;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Random;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assume;
import org.junit.Test;

/**
 * Measurements of HeatMap. Wall time (median of 5 rounds) and bytes allocated on the UI thread per operation;
 * prints one line per measurement: <code>bench &lt;widget&gt; &lt;operation&gt; &lt;size&gt; &lt;value&gt; &lt;unit&gt;</code>.
 * Opt-in: <code>-Dnebula.bench=true</code>.
 */
public class HeatMapBench {

	static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

	Display display;

	@Test
	public void measure() {
		Assume.assumeTrue("-Dnebula.bench=true is not set", Boolean.getBoolean("nebula.bench"));
		display = Display.getDefault();
		for (int side : new int[] { 316, 1000 }) {
			heatMap(side);
		}
	}

	static void print(String widget, String operation, long size, double value, String unit) {
		System.out.println("bench " + widget + " " + operation + " " + size + " " + String.format(java.util.Locale.ROOT, "%.1f", value) + " " + unit);
	}

	/* a full repaint of the client area and one cell update of a side x side heat map */
	void heatMap(int side) {
		Shell shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(0, 0, 640, 480);
		shell.setLayout(new FillLayout());
		HeatMap map = new HeatMap(shell, SWT.H_SCROLL | SWT.V_SCROLL);
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
		print("HeatMap", "paint-client-area", cells, median(time) / 1000.0, "us");
		print("HeatMap", "paint-client-area-alloc", cells, median(bytes), "bytes");
		long a = THREADS.getCurrentThreadAllocatedBytes();
		long start = System.nanoTime();
		map.setValues(values);
		print("HeatMap", "setValues", cells, (System.nanoTime() - start) / 1000.0, "us");
		print("HeatMap", "setValues-alloc", cells, THREADS.getCurrentThreadAllocatedBytes() - a, "bytes");
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
