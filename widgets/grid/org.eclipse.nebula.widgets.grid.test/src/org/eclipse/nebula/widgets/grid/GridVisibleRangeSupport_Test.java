/*******************************************************************************
 * Copyright (c) 2026 Eclipse Nebula contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.grid.Grid.GridVisibleRange;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeSupport.RangeChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * UI/runtime proof for GridVisibleRangeSupport.
 *
 * <p>The PNG files are diagnostic evidence only. Assertions are based on the
 * logical visible range and range-change events so theme/font rasterization
 * differences do not make the test pixel-fragile.</p>
 */
public class GridVisibleRangeSupport_Test {

	private Display display;
	private Shell shell;
	private Grid grid;
	private GridColumn[] columns;
	private final List<RangeChangedEvent> events = new ArrayList<>();

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		grid = new Grid(shell, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		grid.setSize(360, 220);

		columns = new GridColumn[8];
		for (int column = 0; column < columns.length; column++) {
			GridColumn gridColumn = columns[column] = new GridColumn(grid, SWT.NONE);
			gridColumn.setText("column " + column);
			gridColumn.setWidth(120);
		}

		for (int row = 0; row < 240; row++) {
			GridItem item = new GridItem(grid, SWT.NONE);
			for (int column = 0; column < columns.length; column++) {
				item.setText(column, "r" + row + " c" + column);
			}
		}

		GridVisibleRangeSupport support = GridVisibleRangeSupport.createFor(grid);
		support.addRangeChangeListener(events::add);

		shell.setSize(380, 260);
		shell.open();
		flushPaint();
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	@Test
	public void testVisibleRangeTracksRenderedViewportAndProducesScreenshots() throws Exception {
		GridVisibleRange top = grid.getVisibleRange();
		assertTrue("top scene must expose rows", top.getItems().length > 0);
		assertTrue("top scene must be a viewport, not the whole model",
				top.getItems().length < grid.getItemCount());
		assertTrue("top scene must expose columns", top.getColumns().length > 0);
		assertTrue("top scene must be horizontally bounded",
				top.getColumns().length < grid.getColumnCount());
		assertSame(grid.getItem(grid.getTopIndex()), top.getItems()[0]);
		snapshot("01-top");

		events.clear();
		grid.setTopIndex(120);
		flushPaint();
		GridVisibleRange middle = grid.getVisibleRange();
		assertTrue("vertical scroll must advance the logical viewport", grid.getTopIndex() >= 100);
		assertSame(grid.getItem(grid.getTopIndex()), middle.getItems()[0]);
		assertNotSame(top.getItems()[0], middle.getItems()[0]);
		assertTrue("paint-driven support must publish a row-range delta", hasRowDelta(events));
		snapshot("02-middle");

		events.clear();
		GridColumn firstVisibleBefore = middle.getColumns()[0];
		grid.showColumn(columns[7]);
		flushPaint();
		GridVisibleRange horizontal = grid.getVisibleRange();
		assertTrue("rightmost column must be visible after showColumn",
				containsIdentity(horizontal.getColumns(), columns[7]));
		assertNotSame("horizontal viewport must advance from its initial first column",
				firstVisibleBefore, horizontal.getColumns()[0]);
		assertTrue("paint-driven support must publish a column-range delta", hasColumnDelta(events));
		snapshot("03-horizontal");

		int rowsBeforeResize = horizontal.getItems().length;
		int columnsBeforeResize = horizontal.getColumns().length;
		grid.setSize(760, 420);
		shell.setSize(780, 460);
		flushPaint();
		GridVisibleRange resized = grid.getVisibleRange();
		assertTrue("larger viewport should not expose fewer rows",
				resized.getItems().length >= rowsBeforeResize);
		assertTrue("larger viewport should not expose fewer columns",
				resized.getColumns().length >= columnsBeforeResize);
		snapshot("04-resized");

		ScrollBar vertical = grid.getVerticalBar();
		ScrollBar horizontalBar = grid.getHorizontalBar();
		assertTrue("large model must require vertical scrolling", vertical != null && vertical.getVisible());
		assertTrue("wide model must require horizontal scrolling", horizontalBar != null && horizontalBar.getVisible());
	}

	private boolean hasRowDelta(List<RangeChangedEvent> changes) {
		for (RangeChangedEvent event : changes) {
			if (event.addedRows.length != 0 || event.removedRows.length != 0) return true;
		}
		return false;
	}

	private boolean hasColumnDelta(List<RangeChangedEvent> changes) {
		for (RangeChangedEvent event : changes) {
			if (event.addedColumns.length != 0 || event.removedColumns.length != 0) return true;
		}
		return false;
	}

	private static boolean containsIdentity(GridColumn[] values, GridColumn target) {
		for (GridColumn value : values) {
			if (value == target) return true;
		}
		return false;
	}

	private void flushPaint() {
		grid.redraw();
		grid.update();
		while (display.readAndDispatch()) {
			// drain real SWT paint/scroll work
		}
	}

	private void snapshot(String name) throws IOException {
		int width = Math.max(1, grid.getSize().x);
		int height = Math.max(1, grid.getSize().y);
		Image image = new Image(display, width, height);
		GC gc = new GC(grid);
		try {
			gc.copyArea(image, 0, 0);
			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] { image.getImageData() };
			Path directory = Path.of("target", "m3-visible-range-screenshots");
			Files.createDirectories(directory);
			loader.save(directory.resolve(name + ".png").toString(), SWT.IMAGE_PNG);
		} finally {
			gc.dispose();
			image.dispose();
		}
	}
}
