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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

/**
 * Rows of different heights (20 to 56 pixels, wrapped text) scrolled smoothly: a timer moves the
 * vertical scroll bar one row per frame and sends the {@code SWT.Selection} a wheel notch or an
 * arrow click sends. In the viewport mode (the default) each step moves the pixels that stay on
 * screen by the row index's pixel distance between the old and the new top row and paints only the
 * row that comes into view; the stock Grid ({@code -Dswt.widget.mode.Grid=original}) repaints the
 * whole body when rows differ in height.
 * <p>
 * The time per step goes to the {@code org.eclipse.nebula.snippets.grid} logger. With
 * {@code -Dnebula.snippet.screenshot=<file.png>} the snippet scrolls 300 steps, saves the Grid's
 * window to that file and exits.
 * </p>
 *
 * For a list of all Nebula Grid example snippets see
 * http://www.eclipse.org/nebula/widgets/grid/snippets.php
 */
public class GridSnippetVariableHeightScroll {
	private static final Logger LOG = Logger.getLogger("org.eclipse.nebula.snippets.grid");

	private static final int ROWS = 10_000;
	private static final int FRAME_MS = 16;
	private static final int STEPS = 300;

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Grid: variable row heights, smooth scroll");
		shell.setLayout(new FillLayout());

		Grid grid = new Grid(shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setHeaderVisible(true);
		GridColumn number = new GridColumn(grid, SWT.NONE);
		number.setText("#");
		number.setWidth(60);
		GridColumn text = new GridColumn(grid, SWT.NONE);
		text.setText("Text");
		text.setWidth(260);
		text.setWordWrap(true);
		for (int r = 0; r < ROWS; r++) {
			GridItem item = new GridItem(grid, SWT.NONE);
			int lines = 1 + r % 3;
			item.setText(0, Integer.toString(r));
			StringBuilder words = new StringBuilder("Row " + r);
			for (int l = 1; l < lines; l++) {
				words.append("\nline ").append(l + 1).append(" of ").append(lines);
			}
			item.setText(1, words.toString());
			item.setHeight(2 + 18 * lines);
		}

		shell.setSize(380, 460);
		shell.open();
		settle(display);

		ScrollBar bar = grid.getVerticalBar();
		long[] elapsed = { 0 };
		int[] steps = { 0 };
		boolean screenshotMode = System.getProperty("nebula.snippet.screenshot") != null;
		Runnable step = new Runnable() {
			@Override
			public void run() {
				if (grid.isDisposed() || steps[0] >= STEPS && screenshotMode) {
					return;
				}
				long start = System.nanoTime();
				int next = bar.getSelection() + 1 > bar.getMaximum() - bar.getThumb() ? 0 : bar.getSelection() + 1;
				bar.setSelection(next);
				// what a wheel notch or an arrow click sends: the Grid scrolls its body
				bar.notifyListeners(SWT.Selection, new Event());
				grid.update();
				elapsed[0] += System.nanoTime() - start;
				if (++steps[0] % 100 == 0) {
					LOG.info(() -> String.format("%d scroll steps, %d us per step (scroll and paint)",
							Integer.valueOf(steps[0]), Long.valueOf(elapsed[0] / 1_000 / steps[0])));
				}
				display.timerExec(FRAME_MS, this);
			}
		};
		display.timerExec(FRAME_MS, step);

		if (screenshotMode) {
			while (steps[0] < STEPS && !shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			settle(display);
			save(shell, System.getProperty("nebula.snippet.screenshot"));
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
