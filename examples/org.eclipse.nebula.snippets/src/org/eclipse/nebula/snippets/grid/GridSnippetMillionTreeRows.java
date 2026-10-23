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
package org.eclipse.nebula.snippets.grid;

import java.util.logging.Logger;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * A virtual tree Grid with 1,000,000 rows: 1,000 roots, each given 999 children by
 * {@link GridItem#setItemCount(int)}. The texts come from an {@code SWT.SetData} listener when a
 * row is first shown. In the viewport mode (the default) a child that nobody has read is a row of
 * the Grid's row tree and has no {@code GridItem}; {@code -Dswt.widget.mode.Grid=original} runs the
 * stock Grid, which creates every child up front.
 * <p>
 * The build time, the heap used and the number of {@code SetData} requests go to the
 * {@code org.eclipse.nebula.snippets.grid} logger. With {@code -Dnebula.snippet.screenshot=<file.png>}
 * the snippet saves the Grid's window to that file and exits.
 * </p>
 *
 * For a list of all Nebula Grid example snippets see
 * http://www.eclipse.org/nebula/widgets/grid/snippets.php
 */
public class GridSnippetMillionTreeRows {
	private static final Logger LOG = Logger.getLogger("org.eclipse.nebula.snippets.grid");

	private static final int ROOTS = 1000;
	private static final int CHILDREN = 999;

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Grid: 1,000,000 tree rows from GridItem.setItemCount");
		shell.setLayout(new FillLayout());

		Grid grid = new Grid(shell, SWT.BORDER | SWT.VIRTUAL | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setHeaderVisible(true);
		GridColumn rowColumn = new GridColumn(grid, SWT.NONE);
		rowColumn.setTree(true);
		rowColumn.setText("Row");
		rowColumn.setWidth(220);
		GridColumn valueColumn = new GridColumn(grid, SWT.NONE);
		valueColumn.setText("Value");
		valueColumn.setWidth(120);

		int[] requests = { 0 };
		grid.addListener(SWT.SetData, event -> {
			requests[0]++;
			GridItem item = (GridItem) event.item;
			GridItem parent = item.getParentItem();
			if (parent == null) {
				// a root: event.index is its index among the roots
				item.setText(0, "Root " + event.index);
				item.setText(1, CHILDREN + " children");
			} else {
				item.setText(0, "Item " + parent.getData() + "." + event.index);
				item.setText(1, Integer.toString(event.index * 37 % 1000));
			}
		});

		long heapBefore = usedHeap();
		long start = System.nanoTime();
		for (int r = 0; r < ROOTS; r++) {
			GridItem root = new GridItem(grid, SWT.NONE);
			root.setData(Integer.valueOf(r));
			root.setItemCount(CHILDREN);
		}
		grid.getItem(0).setExpanded(true);
		grid.getRootItem(2).setExpanded(true);
		long buildMillis = (System.nanoTime() - start) / 1_000_000;
		long heapMegabytes = (usedHeap() - heapBefore) >> 20;
		LOG.info(() -> String.format("%,d rows (%d roots x %d children) built in %d ms, heap +%d MB",
				Integer.valueOf(grid.getItemCount()), Integer.valueOf(ROOTS), Integer.valueOf(CHILDREN),
				Long.valueOf(buildMillis), Long.valueOf(heapMegabytes)));

		shell.setSize(420, 480);
		shell.open();
		settle(display);
		LOG.info(() -> "SetData requests after the first paint: " + requests[0]);

		// a row far down: only the rows on screen are asked for
		grid.setTopIndex(grid.getItemCount() / 2);
		settle(display);
		LOG.info(() -> "SetData requests after jumping to the middle: " + requests[0]);
		grid.setTopIndex(0);
		settle(display);

		String screenshot = System.getProperty("nebula.snippet.screenshot");
		if (screenshot != null) {
			save(shell, screenshot);
			display.dispose();
			return;
		}
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	private static long usedHeap() {
		Runtime runtime = Runtime.getRuntime();
		for (int i = 0; i < 3; i++) {
			System.gc();
		}
		return runtime.totalMemory() - runtime.freeMemory();
	}

	/* runs the event loop until it has been idle for 200 ms */
	private static void settle(Display display) {
		long quietSince = System.currentTimeMillis();
		while (System.currentTimeMillis() - quietSince < 200) {
			if (display.readAndDispatch()) {
				quietSince = System.currentTimeMillis();
			} else {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	/* saves the shell's client area, as the screen shows it, to a PNG file */
	private static void save(Shell shell, String file) {
		Rectangle area = shell.getClientArea();
		Image image = new Image(shell.getDisplay(), area.width, area.height);
		GC gc = new GC(shell);
		try {
			gc.copyArea(image, 0, 0);
			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] { image.getImageData() };
			loader.save(file, SWT.IMAGE_PNG);
			LOG.info(() -> "saved " + file);
		} finally {
			gc.dispose();
			image.dispose();
		}
	}
}
