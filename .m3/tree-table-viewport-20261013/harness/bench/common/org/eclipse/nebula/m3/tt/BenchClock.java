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
package org.eclipse.nebula.m3.tt;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.widgets.Display;

/** Frame timing, heap and process-memory recorder shared by the widget benches. */
public final class BenchClock {
	private final List<String> lines = new ArrayList<>();
	private final String image;

	public BenchClock(String image) {
		this.image = image;
		lines.add("image\tmetric\tvalue\tunit");
	}

	public void put(String metric, double value, String unit) {
		lines.add(image + "\t" + metric + "\t" + String.format("%.3f", value) + "\t" + unit);
		System.out.println(image + " " + metric + " " + String.format("%.3f", value) + " " + unit);
	}

	public static double ms(long nanos) {
		return nanos / 1_000_000.0;
	}

	/** Records p50/p95/max of the given frame durations (nanoseconds). */
	public void frames(String metric, long[] nanos) {
		long[] sorted = nanos.clone();
		Arrays.sort(sorted);
		put(metric + ".p50", ms(sorted[(sorted.length - 1) / 2]), "ms");
		put(metric + ".p95", ms(sorted[(int) Math.ceil(sorted.length * 0.95) - 1]), "ms");
		put(metric + ".max", ms(sorted[sorted.length - 1]), "ms");
		put(metric + ".frames", sorted.length, "count");
	}

	/** Java heap in use after two GC requests, and process private (committed) bytes. */
	public void memory(String label) {
		System.gc();
		System.gc();
		Runtime rt = Runtime.getRuntime();
		put(label + ".heapUsed", (rt.totalMemory() - rt.freeMemory()) / 1048576.0, "MiB");
		java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		if (os instanceof com.sun.management.OperatingSystemMXBean) {
			long committed = ((com.sun.management.OperatingSystemMXBean) os).getCommittedVirtualMemorySize();
			put(label + ".processPrivate", committed / 1048576.0, "MiB");
		}
	}

	public static void pump(Display display) {
		while (display.readAndDispatch()) {
			// drain
		}
	}

	public void write(String path) throws IOException {
		Files.write(Paths.get(path), lines, StandardCharsets.UTF_8);
	}
}
