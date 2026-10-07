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
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Property-gated visual regression lane for the distilled Grid viewport.
 *
 * <p>The scenarios are original Nebula tests distilled from the supplied
 * Java2s SWT/SWT-2D/Swing behavioral catalogues. Donor source is not copied.</p>
 */
public class GridViewportScreenshotRegression_Test {

	private static final String ENABLED = "nebula.grid.viewport.screenshotRegression";
	private static final String OUTPUT = "nebula.grid.viewport.screenshots";
	private static final String NATIVE_CAPTURE = "nebula.grid.viewport.screenshots.native";
	private static final int LOGICAL_ROWS = 1_000_000;

	private Display display;
	private Shell shell;
	private Grid grid;
	private GridColumn[] columns;
	private final int[] setDataCount = { 0 };

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setText("Nebula Grid viewport screenshot regression");
		shell.setLayout(new FillLayout());
		shell.setSize(1000, 700);
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
		if (display != null && !display.isDisposed()) {
			while (display.readAndDispatch()) {
				// Flush native dispose/redraw work.
			}
		}
	}

	@Test
	public void testGridViewportScreenshotRegression() throws Exception {
		assumeTrue("Grid viewport screenshot lane disabled", Boolean.getBoolean(ENABLED));

		final Path output = Path.of(System.getProperty(
				OUTPUT, "target/screenshots/grid-viewport"));
		Files.createDirectories(output);

		createScene();
		shell.open();
		drainEvents(150);

		capture("grid-top", output);

		grid.setTopIndex(LOGICAL_ROWS / 2);
		grid.setSelection(LOGICAL_ROWS / 2, LOGICAL_ROWS / 2 + 2);
		grid.showSelection();
		drainEvents(150);
		capture("grid-middle-selection", output);

		grid.setTopIndex(LOGICAL_ROWS - 1);
		drainEvents(150);
		capture("grid-end", output);

		grid.setTopIndex(LOGICAL_ROWS / 2);
		grid.showColumn(columns[columns.length - 1]);
		drainEvents(150);
		capture("grid-horizontal-header", output);

		assertTrue(Files.size(output.resolve("grid-top.png")) > 0);
		assertTrue(Files.size(output.resolve("grid-middle-selection.png")) > 0);
		assertTrue(Files.size(output.resolve("grid-end.png")) > 0);
		assertTrue(Files.size(output.resolve("grid-horizontal-header.png")) > 0);
		assertTrue("one million logical rows must not imply one million GridItem facades",
				grid.virtualMaterializedItemCount() < 1_000);
	}

	private void createScene() {
		grid = new Grid(shell, SWT.VIRTUAL | SWT.CHECK | SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		grid.setColumnScrolling(false);

		columns = new GridColumn[4];
		for (int index = 0; index < columns.length; index++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("Column " + index);
			column.setWidth(260);
			columns[index] = column;
		}

		grid.addListener(SWT.SetData, event -> {
			setDataCount[0]++;
			final GridItem item = (GridItem) event.item;
			final int row = event.index;
			item.setText(0, "row " + row);
			item.setText(1, "group " + (row >>> 10));
			item.setText(2, "hex " + Integer.toHexString(row));
			item.setText(3, "mask " + (row & 63));
			if ((row & 31) == 0) {
				item.setChecked(true);
			}
		});
		grid.setItemCount(LOGICAL_ROWS);
	}

	private void capture(final String name, final Path output) throws Exception {
		shell.layout(true, true);
		grid.redraw();
		grid.update();
		drainEvents(80);

		final Path png = output.resolve(name + ".png");
		final GridSwtScreenshotCapture.Result capture =
				GridSwtScreenshotCapture.captureControl(grid, png);

		final StringBuilder text = new StringBuilder(512);
		text.append("scenario=").append(name).append('\n');
		text.append("platform=").append(SWT.getPlatform()).append('\n');
		text.append("logicalRows=").append(grid.getItemCount()).append('\n');
		text.append("topIndex=").append(grid.getTopIndex()).append('\n');
		text.append("selectionCount=").append(grid.getSelectionCount()).append('\n');
		text.append("materializedItems=")
				.append(grid.virtualMaterializedItemCount()).append('\n');
		text.append("setDataCount=").append(setDataCount[0]).append('\n');
		text.append("bounds=").append(grid.getBounds()).append('\n');
		text.append("client=").append(grid.getClientArea()).append('\n');
		text.append("screenshot=").append(capture.path().getFileName()).append('\n');
		text.append("screenshot.captureMethod=").append(capture.method()).append('\n');
		text.append("screenshot.sha256=").append(capture.sha256()).append('\n');
		appendScrollBar(text, "h", grid.getHorizontalBar());
		appendScrollBar(text, "v", grid.getVerticalBar());

		if (Boolean.getBoolean(NATIVE_CAPTURE)) {
			final Path nativePng = output.resolve(name + "-native.png");
			try {
				final GridSwtScreenshotCapture.Result nativeCapture =
						GridSwtScreenshotCapture.captureNativeShell(grid, nativePng);
				text.append("nativeScreenshot=")
						.append(nativeCapture.path().getFileName()).append('\n');
				text.append("nativeScreenshot.captureMethod=")
						.append(nativeCapture.method()).append('\n');
				text.append("nativeScreenshot.sha256=")
						.append(nativeCapture.sha256()).append('\n');
			} catch (final RuntimeException | IOException unavailable) {
				text.append("nativeScreenshot.error=")
						.append(unavailable.getClass().getName())
						.append(": ").append(String.valueOf(unavailable.getMessage()))
						.append('\n');
			}
		}

		Files.writeString(
				output.resolve(name + ".txt"), text, StandardCharsets.UTF_8);
	}

	private static void appendScrollBar(
			final StringBuilder text, final String prefix, final ScrollBar bar) {
		if (bar == null || bar.isDisposed()) {
			text.append(prefix).append(".scrollbar=<none>\n");
			return;
		}
		text.append(prefix).append(".selection=").append(bar.getSelection()).append('\n');
		text.append(prefix).append(".minimum=").append(bar.getMinimum()).append('\n');
		text.append(prefix).append(".maximum=").append(bar.getMaximum()).append('\n');
		text.append(prefix).append(".thumb=").append(bar.getThumb()).append('\n');
		text.append(prefix).append(".visible=").append(bar.getVisible()).append('\n');
	}

	private void drainEvents(final long millis) throws InterruptedException {
		final long deadline = System.currentTimeMillis() + millis;
		do {
			while (display.readAndDispatch()) {
				// Drain async layout, redraw and viewport publication work.
			}
			Thread.sleep(5);
		} while (System.currentTimeMillis() < deadline);
	}
}
