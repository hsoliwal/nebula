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
package org.eclipse.nebula.widgets.grid;

import java.util.Random;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Diagnostic only (no assertions): logs where the paints of two GTK3 observations come from.
 * <ul>
 * <li>A Grid in a CTabFolder page shown again: the paint events of the Grid, a plain Canvas in the
 * same place, the page and the folder, with their clips, in time order. An asyncExec marker posted
 * right after the show runs after the folder's own deferred update.</li>
 * <li>A resize of a plain Canvas (with and without NO_REDRAW_RESIZE) and of the Grid: the clip of
 * each paint per resize step against the client area.</li>
 * </ul>
 */
public class GridGtkPaintOrigin_Test {
	private static final Logger LOG = Logger.getLogger("org.eclipse.nebula.widgets.grid");

	private Display display;
	private Shell shell;
	private StringBuilder trace;
	private long t0;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(10, 10, 760, 560);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void tabShowsGrid() {
		tab("grid in tab", true, true);
	}

	@Test
	public void tabShowsCanvas() {
		tab("canvas in tab", false, true);
	}

	@Test
	public void pageShownWithoutTheFolderUpdate() {
		tab("grid, page shown directly", true, false);
	}

	@Test
	public void resizePaints() {
		shell.setLayout(null);
		final int[] styles = { SWT.NO_REDRAW_RESIZE | SWT.DOUBLE_BUFFERED | SWT.H_SCROLL | SWT.V_SCROLL,
				SWT.NO_REDRAW_RESIZE | SWT.DOUBLE_BUFFERED, SWT.DOUBLE_BUFFERED | SWT.H_SCROLL | SWT.V_SCROLL };
		final String[] names = { "canvas NO_REDRAW_RESIZE+scroll", "canvas NO_REDRAW_RESIZE", "canvas redraw-resize+scroll" };
		for (int s = 0; s < styles.length; s++) {
			final Canvas canvas = canvas(shell, styles[s]);
			canvas.setBounds(5, 5, 600, 400);
			resizeSteps(names[s], canvas);
			canvas.dispose();
		}
		new Text(shell, SWT.SINGLE).setBounds(5, 500, 60, 22);
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 600, 400);
		grid.setHeaderVisible(true);
		grid.setFooterVisible(true);
		grid.setLinesVisible(true);
		for (int c = 0; c < 9; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setFooterText("f" + c);
			column.setWidth(80 + c % 3 * 10);
		}
		for (int r = 0; r < 2_000; r++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 9; c++) {
				item.setText(c, r + ":" + c);
			}
		}
		record(grid, "grid");
		final int deltas = grid.resizeDeltas;
		final int full = grid.resizeFullRepaints;
		resizeSteps("grid", grid);
		LOG.info("GridGtkPaintOrigin_Test: grid (" + mode() + ") planned edge bands " + (grid.resizeDeltas - deltas)
				+ ", planned whole repaints " + (grid.resizeFullRepaints - full));
	}

	private void resizeSteps(final String name, final Control control) {
		shell.open();
		pump();
		final int[][] sizes = { { 647, 400 }, { 647, 441 }, { 586, 441 }, { 586, 388 }, { 633, 429 } };
		final StringBuilder log = new StringBuilder();
		for (final int[] size : sizes) {
			final String steps = during(() -> control.setSize(size[0], size[1]));
			final Rectangle client = ((Composite) control).getClientArea();
			log.append(" | to ").append(size[0]).append('x').append(size[1]).append(" client ").append(client.width)
					.append('x').append(client.height).append(": ").append(steps);
		}
		LOG.info("GridGtkPaintOrigin_Test: resize " + name + " (" + mode() + ")" + log);
	}

	private void tab(final String name, final boolean useGrid, final boolean viaFolder) {
		shell.setLayout(new FillLayout());
		final CTabFolder folder = new CTabFolder(shell, SWT.NONE);
		final CTabItem mainTab = new CTabItem(folder, SWT.NONE);
		mainTab.setText("grid");
		final Composite page = new Composite(folder, SWT.NONE);
		page.setLayout(new FillLayout());
		mainTab.setControl(page);
		final CTabItem otherTab = new CTabItem(folder, SWT.NONE);
		otherTab.setText("other");
		final Text text = new Text(folder, SWT.MULTI);
		otherTab.setControl(text);
		folder.setSelection(mainTab);
		final Control shown = useGrid ? treeGrid(page) : canvas(page, SWT.DOUBLE_BUFFERED | SWT.H_SCROLL | SWT.V_SCROLL);
		if (useGrid) {
			record(shown, "grid");
		}
		record(page, "page");
		record(folder, "folder");
		record(text, "text");
		shell.open();
		pump();
		if (useGrid) {
			((Grid) shown).setTopIndex(400);
			pump();
		}
		if (viaFolder) {
			folder.setSelection(otherTab);
		} else {
			page.setVisible(false);
			text.setVisible(true);
		}
		pump();
		if (useGrid) {
			treeDeltas((Grid) shown, new Random(0x41DL));
			pump();
		}
		final String show = during(() -> {
			if (viaFolder) {
				folder.setSelection(mainTab);
			} else {
				page.setVisible(true);
				text.setVisible(false);
				folder.redraw();
			}
		});
		final Rectangle client = folder.getClientArea();
		final String where = "folder size " + folder.getSize().x + "x" + folder.getSize().y + ", client area " + client.x + ","
				+ client.y + " " + client.width + "x" + client.height + ", tab height " + folder.getTabHeight() + ", "
				+ (useGrid ? "grid" : "canvas") + " bounds " + shown.getBounds() + ", focus " + focusName(shown, text, folder);
		LOG.info("GridGtkPaintOrigin_Test: " + name + " (" + mode() + "), show: " + show + " | " + where);
		if (!viaFolder) {
			final int w = folder.getSize().x;
			final String overlap = during(() -> folder.redraw(0, 0, w, client.y + 1, false));
			final String above = during(() -> folder.redraw(0, 0, w, client.y, false));
			LOG.info("GridGtkPaintOrigin_Test: " + name + " (" + mode() + "), folder.redraw(0,0," + w + "," + (client.y + 1)
					+ ",false): " + overlap + " | folder.redraw(0,0," + w + "," + client.y + ",false): " + above);
		}
	}

	private String focusName(final Control shown, final Text text, final CTabFolder folder) {
		final Control focus = display.getFocusControl();
		return focus == shown ? "shown" : focus == text ? "text" : focus == folder ? "folder" : String.valueOf(focus);
	}

	private Grid treeGrid(final Composite parent) {
		final Grid grid = new Grid(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		for (int c = 0; c < 2; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setWidth(200);
			column.setTree(c == 0);
		}
		for (int r = 0; r < 3_000; r++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			root.setText(0, "root " + r);
			root.setText(1, "value " + r);
			for (int k = 0; k < 2; k++) {
				new GridItem(root, SWT.NONE).setText(0, "child " + r + "." + k);
			}
			root.setExpanded(r % 3 != 0);
		}
		return grid;
	}

	/* the deltas of GridHiddenViewport_Test (same seed and mix), 10^4 of them */
	private static void treeDeltas(final Grid grid, final Random random) {
		for (int step = 0; step < 10_000; step++) {
			final int kind = random.nextInt(100);
			final int count = grid.getItemCount();
			if (kind < 55) {
				grid.getItem(random.nextInt(count)).setText(random.nextInt(2), "t" + step);
			} else if (kind < 65) {
				new GridItem(grid, SWT.NONE).setText(0, "new root " + step);
			} else if (kind < 75) {
				final GridItem root = grid.getRootItem(random.nextInt(grid.getRootItemCount()));
				new GridItem(root, SWT.NONE).setText(0, "new child " + step);
			} else if (kind < 85) {
				if (count > 1_000) {
					grid.getItem(random.nextInt(count)).dispose();
				}
			} else if (kind < 95) {
				final GridItem root = grid.getRootItem(random.nextInt(grid.getRootItemCount()));
				root.setExpanded(!root.isExpanded());
			} else {
				grid.getItem(random.nextInt(count)).setHeight(18 + random.nextInt(20));
			}
		}
	}

	private Canvas canvas(final Composite parent, final int style) {
		final Canvas canvas = new Canvas(parent, style);
		canvas.addListener(SWT.Paint, e -> {
			e.gc.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			e.gc.fillRectangle(e.x, e.y, e.width, e.height);
		});
		record(canvas, "canvas");
		return canvas;
	}

	private void record(final Control control, final String name) {
		for (final int type : new int[] { SWT.Paint, SWT.Resize, SWT.Show, SWT.Hide, SWT.FocusIn, SWT.FocusOut }) {
			control.addListener(type, e -> {
				if (trace == null) {
					return;
				}
				trace.append((System.nanoTime() - t0) / 1_000_000).append("ms ").append(name).append(' ');
				switch (type) {
				case SWT.Paint:
					trace.append("paint ").append(e.x).append(',').append(e.y).append(' ').append(e.width).append('x')
							.append(e.height);
					break;
				case SWT.Resize:
					trace.append("resize ").append(control.getSize().x).append('x').append(control.getSize().y);
					break;
				default:
					trace.append(type == SWT.Show ? "show" : type == SWT.Hide ? "hide" : type == SWT.FocusIn ? "focusIn" : "focusOut");
				}
				if (control instanceof Grid) {
					final Grid grid = (Grid) control;
					trace.append(" [passes ").append(grid.paintPasses).append(" ops ").append(grid.nativeOps).append(" patches ")
							.append(grid.hiddenPatches).append(']');
				}
				trace.append("; ");
			});
		}
	}

	/* runs the action and records the events until 300 ms pass without one (3 s at most) */
	private String during(final Runnable action) {
		trace = new StringBuilder();
		t0 = System.nanoTime();
		action.run();
		trace.append((System.nanoTime() - t0) / 1_000_000).append("ms returned; ");
		// posted after the action's own asyncExec runnables, so it runs right after them
		display.asyncExec(() -> {
			if (trace != null) {
				trace.append((System.nanoTime() - t0) / 1_000_000).append("ms asyncExec marker; ");
			}
		});
		pump();
		final String result = trace.toString();
		trace = null;
		return result;
	}

	private void pump() {
		final long start = System.nanoTime();
		long quietSince = start;
		int seen = trace == null ? 0 : trace.length();
		while (System.nanoTime() - start < 3_000_000_000L && System.nanoTime() - quietSince < 300_000_000L) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (final InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			final int now = trace == null ? 0 : trace.length();
			if (now != seen) {
				seen = now;
				quietSince = System.nanoTime();
			}
		}
	}

	private static String mode() {
		return GridViewport.ENABLED ? "viewport" : "original";
	}
}
