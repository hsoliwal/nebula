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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Cell selection over many columns: 50,000 rows x 4 columns, all 200,000 cells selected with
 * {@link Grid#selectAllCells()}, then single cells toggled in the middle of that selection, as a
 * user's ctrl+click would. In the viewport mode (the default) the Grid keeps the selected cells in
 * a hash set with per-column counts, so a toggle and a membership test cost O(1); the stock Grid
 * ({@code -Dswt.widget.mode.Grid=original}) scans its list of selected cells.
 * <p>
 * The times go to the {@code org.eclipse.nebula.snippets.grid} logger. With
 * {@code -Dnebula.snippet.screenshot=<file.png>} the snippet saves the Grid's window to that file and
 * exits.
 * </p>
 *
 * For a list of all Nebula Grid example snippets see
 * http://www.eclipse.org/nebula/widgets/grid/snippets.php
 */
public class GridSnippetCellSelection200k {
	private static final Logger LOG = Logger.getLogger("org.eclipse.nebula.snippets.grid");

	private static final int ROWS = 50_000;
	private static final int COLUMNS = 4;
	private static final int TOGGLES = 200;

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Grid: 200,000 selected cells");
		shell.setLayout(new FillLayout());

		Grid grid = new Grid(shell, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setHeaderVisible(true);
		grid.setRowHeaderVisible(true);
		grid.setCellSelectionEnabled(true);
		for (int c = 0; c < COLUMNS; c++) {
			GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("Column " + c);
			column.setWidth(90);
		}
		for (int r = 0; r < ROWS; r++) {
			GridItem item = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < COLUMNS; c++) {
				item.setText(c, r + ":" + c);
			}
		}

		long start = System.nanoTime();
		grid.selectAllCells();
		long selectMillis = (System.nanoTime() - start) / 1_000_000;
		LOG.info(() -> String.format("selectAllCells: %,d cells in %d ms",
				Integer.valueOf(grid.getCellSelectionCount()), Long.valueOf(selectMillis)));

		// ctrl+click in the middle of the selection: off, then on again
		start = System.nanoTime();
		for (int i = 0; i < TOGGLES; i++) {
			Point cell = new Point(i % COLUMNS, ROWS / 2 + i);
			grid.deselectCell(cell);
			grid.selectCell(cell);
		}
		long toggleMicros = (System.nanoTime() - start) / 1_000 / (2 * TOGGLES);
		LOG.info(() -> String.format("one cell toggle inside %,d selected cells: %d us",
				Integer.valueOf(grid.getCellSelectionCount()), Long.valueOf(toggleMicros)));

		// leave a visible pattern: every third cell of the first rows deselected
		for (int r = 0; r < 40; r++) {
			for (int c = 0; c < COLUMNS; c++) {
				if ((r + c) % 3 == 0) {
					grid.deselectCell(new Point(c, r));
				}
			}
		}
		LOG.info(() -> String.format("%,d cells selected", Integer.valueOf(grid.getCellSelectionCount())));

		shell.setSize(460, 420);
		shell.open();
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
