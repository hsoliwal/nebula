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
package org.eclipse.nebula.widgets.gallery.tests;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import org.eclipse.nebula.widgets.gallery.DefaultGalleryGroupRenderer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assume;
import org.junit.Test;

/**
 * Optimization pass measurement of the Gallery selection (leaf G-SEL-1): one virtual group of n items; selectAll,
 * getSelection and deselectAll, wall time and bytes allocated on the UI thread, stock or this Gallery (whichever is on
 * the class path, in whichever startup mode). Prints <code>bench Gallery &lt;operation&gt; &lt;n&gt; &lt;value&gt;
 * &lt;unit&gt; &lt;mode&gt;</code>. Opt-in: <code>-Dnebula.gallery.bench=true</code>, sizes with
 * <code>-Dnebula.gallery.bench.counts=10000,100000</code>.
 */
public class GallerySelectionBench {

	static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

	@Test
	public void measure() {
		Assume.assumeTrue("-Dnebula.gallery.bench=true is not set", Boolean.getBoolean("nebula.gallery.bench"));
		Display display = Display.getDefault();
		int[] counts = Arrays.stream(System.getProperty("nebula.gallery.bench.counts", "10000,100000").split(",")).mapToInt(Integer::parseInt).toArray();
		for (int n : counts) {
			measure(display, n);
		}
	}

	void measure(Display display, int n) {
		Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds(40, 40, 640, 480);
		shell.setLayout(new FillLayout());
		Gallery gallery = new Gallery(shell, SWT.V_SCROLL | SWT.MULTI | SWT.VIRTUAL);
		DefaultGalleryGroupRenderer groupRenderer = new DefaultGalleryGroupRenderer();
		groupRenderer.setItemHeight(56);
		groupRenderer.setItemWidth(72);
		gallery.setGroupRenderer(groupRenderer);
		gallery.setItemRenderer(new DefaultGalleryItemRenderer());
		gallery.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item.getParentItem() == null) {
				item.setText("group");
				item.setItemCount(n);
			} else {
				item.setText("item " + e.index);
			}
		});
		gallery.setItemCount(1);
		gallery.getItem(0).setExpanded(true);
		shell.open();
		drain(display);
		/* materialize every facade first, so the measurement is the selection, not SetData */
		GalleryItem group = gallery.getItem(0);
		for (int i = 0; i < n; i++) {
			group.getItem(i);
		}
		String mode = GalleryScaleOracleTest.mode();
		long a = THREADS.getCurrentThreadAllocatedBytes(), start = System.nanoTime();
		gallery.selectAll();
		long selectAll = System.nanoTime() - start, selectAllBytes = THREADS.getCurrentThreadAllocatedBytes() - a;
		start = System.nanoTime();
		int count = gallery.getSelection().length;
		long getSelection = System.nanoTime() - start;
		a = THREADS.getCurrentThreadAllocatedBytes();
		start = System.nanoTime();
		gallery.deselectAll();
		long deselectAll = System.nanoTime() - start, deselectBytes = THREADS.getCurrentThreadAllocatedBytes() - a;
		print("selectAll", n, selectAll / 1e6, "ms", mode);
		print("selectAll-alloc", n, selectAllBytes, "bytes", mode);
		print("getSelection", n, getSelection / 1e6, "ms", mode);
		print("selected", n, count, "items", mode);
		print("deselectAll", n, deselectAll / 1e6, "ms", mode);
		print("deselectAll-alloc", n, deselectBytes, "bytes", mode);
		shell.dispose();
	}

	static void print(String operation, int n, double value, String unit, String mode) {
		System.out.println("bench Gallery " + operation + " " + n + " " + String.format(java.util.Locale.ROOT, "%.1f", value) + " " + unit + " " + mode);
	}

	static void drain(Display display) {
		while (display.readAndDispatch()) {
			// dispatch everything queued
		}
	}
}
