/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A Grid never paints a row before its data is there: right after a scroll (without running the
 * event loop) the printed Grid equals the Grid printed after the event loop settled, and every row
 * on screen shows its text. Covers a flat {@code SWT.VIRTUAL} Grid (rows answered by
 * {@code SWT.SetData} during the paint) and a tree Grid, in whatever mode the run selects.
 */
public class GridScrollPaint_Test {
	/** The package's logger (brief rule 22: java.util.logging only). */
	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("org.eclipse.nebula.widgets.grid");

	private Display display;
	private Shell shell;
	private int setData;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(10, 10, 420, 420);
		new Text(shell, SWT.SINGLE).setBounds(5, 390, 60, 22);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	private Grid grid(final int style) {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | style);
		grid.setBounds(5, 5, 400, 370);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(false);
		for (int c = 0; c < 3; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setWidth(110);
			column.setTree(c == 0 && (style & SWT.VIRTUAL) == 0);
		}
		return grid;
	}

	/*
	 * runs the event loop until it has been idle for 200 ms and no Grid waits for a drag frame
	 * (frame-coalesced drag paints use timers, which a test desktop may fire late)
	 */
	private void settle() {
		long idleSince = System.currentTimeMillis();
		final long deadline = idleSince + 5000;
		while (System.currentTimeMillis() - idleSince < 200 || dragFramePending() && System.currentTimeMillis() < deadline) {
			if (display.readAndDispatch()) {
				idleSince = System.currentTimeMillis();
			} else {
				try {
					Thread.sleep(5);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	private boolean dragFramePending() {
		for (final org.eclipse.swt.widgets.Control child : shell.getChildren()) {
			if (child instanceof Grid && ((Grid) child).dragFramePending()) {
				return true;
			}
		}
		return false;
	}

	/* the window's pixels as they are (a copy of the Grid's client area, no paint) */
	static ImageData screen(final Grid grid) {
		final Point size = grid.getSize();
		final Image image = new Image(grid.getDisplay(), size.x, size.y);
		try {
			final GC gc = new GC(grid);
			try {
				gc.copyArea(image, 0, 0);
			} finally {
				gc.dispose();
			}
			return image.getImageData();
		} finally {
			image.dispose();
		}
	}

	/* the Grid's own paint (deterministic, unlike Control.print) */
	static ImageData print(final Grid grid) {
		final Point size = grid.getSize();
		final Image image = new Image(grid.getDisplay(), size.x, size.y);
		try {
			GridRowPermutation_Test.paint(grid, image);
			return image.getImageData();
		} finally {
			image.dispose();
		}
	}

	/* rows on screen that show no pixel other than the background in their first column */
	private static int blankRows(final Grid grid, final ImageData image) {
		final int header = grid.getHeaderVisible() ? grid.getHeaderHeight() : 0;
		final int row = grid.getItemHeight() + 1;
		final RGB background = grid.getBackground().getRGB();
		final int rows = Math.min((grid.getClientArea().height - header) / row, visibleRowsBelowTop(grid));
		int blank = 0;
		for (int r = 0; r < rows; r++) {
			boolean ink = false;
			for (int y = header + r * row; y < header + (r + 1) * row - 1 && !ink; y++) {
				for (int x = 30; x < 100 && !ink; x++) {
					final RGB rgb = image.palette.getRGB(image.getPixel(x, y));
					ink = rgb.red + rgb.green + rgb.blue < 300 && !rgb.equals(background);
				}
			}
			blank += ink ? 0 : 1;
		}
		return blank;
	}

	private static int visibleRowsBelowTop(final Grid grid) {
		int rows = 0;
		for (GridItem item = grid.getItem(grid.getTopIndex()); item != null && rows < 100; item = grid.getNextVisibleItem(item)) {
			rows++;
		}
		return rows;
	}

	/* the Grid's settled pixels before the current step */
	private ImageData previous;
	/* steps whose window showed the new rows right away, and steps that still showed the old rows */
	private int immediate, deferred;

	/*
	 * Right after a scroll, before the event loop runs, the window (its pixels as they are, copied
	 * without a paint) shows either the rows before the scroll or the rows after it: never a mix,
	 * never a row without its data. Once the event loop settled, the window shows exactly what the
	 * Grid paints: no stale rows are left on screen.
	 */
	private void assertPaintedLikeSettled(final Grid grid, final String step) {
		final int before = setData;
		final ImageData now = screen(grid);
		final int answered = setData - before;
		final int blankNow = blankRows(grid, now);
		settle();
		final ImageData onScreen = screen(grid);
		final ImageData settled = print(grid);
		if (!sameClientArea(grid, onScreen, settled)) {
			GridViewportDifferential_Test.save(onScreen, "scroll-screen");
			GridViewportDifferential_Test.save(settled, "scroll-settled");
			fail(step + ": the settled window differs from the Grid's paint ("
					+ GridViewportDifferential_Test.difference(onScreen, settled) + ")");
		}
		final boolean isNew = sameClientArea(grid, now, settled);
		final boolean isOld = previous != null && sameClientArea(grid, now, previous);
		if (!isNew && !isOld || blankNow > 0) {
			GridViewportDifferential_Test.save(now, "scroll-now");
			GridViewportDifferential_Test.save(settled, "scroll-settled");
			fail(step + ": the window right after the scroll is neither the old nor the settled Grid ("
					+ GridViewportDifferential_Test.difference(now, settled) + "), blank rows " + blankNow
					+ ", SetData answered meanwhile " + answered);
		}
		immediate += isNew ? 1 : 0;
		deferred += isNew ? 0 : 1;
		assertEquals(step + ": blank rows after settling", 0, blankRows(grid, settled));
		previous = settled;
	}

	static boolean sameClientArea(final Grid grid, final ImageData a, final ImageData b) {
		final org.eclipse.swt.graphics.Rectangle area = grid.getClientArea();
		final int dx = (grid.getSize().x - grid.getClientArea().width - (grid.getVerticalBar().isVisible() ? grid.getVerticalBar().getSize().x : 0)) / 2;
		for (int y = area.y; y < area.y + area.height; y++) {
			for (int x = area.x; x < area.x + area.width; x++) {
				if (a.getPixel(x + dx, y + dx) != b.getPixel(x + dx, y + dx)) {
					return false;
				}
			}
		}
		return true;
	}

	private void scrollScript(final Grid grid) {
		shell.open();
		settle();
		previous = screen(grid);
		final ScrollBar bar = grid.getVerticalBar();
		final int[] tops = { 40, 41, 60, 400, 399, 1200, 7, 5000, 5013, 2 };
		for (final int top : tops) {
			grid.setTopIndex(Math.min(top, grid.getItemCount() - 1));
			assertPaintedLikeSettled(grid, "setTopIndex " + top);
		}
		// steps of one row, of more than half a page (the blit's exposed strip reaches past its source),
		// and jumps of more than a page
		for (final int value : new int[] { 3, 9, 26, 27, 250, 251, 1800, 30, 47, 46 }) {
			bar.setSelection(Math.min(value, bar.getMaximum()));
			final Event drag = new Event();
			drag.detail = value % 2 == 0 ? SWT.DRAG : SWT.NONE;
			bar.notifyListeners(SWT.Selection, drag);
			assertPaintedLikeSettled(grid, "scroll bar " + value + " detail " + drag.detail);
		}
		for (final int key : new int[] { SWT.PAGE_DOWN, SWT.PAGE_DOWN, SWT.ARROW_DOWN, SWT.PAGE_UP, SWT.END, SWT.HOME }) {
			final Event event = new Event();
			event.keyCode = key;
			event.doit = true;
			grid.notifyListeners(SWT.KeyDown, event);
			assertPaintedLikeSettled(grid, "key " + key);
		}
		LOG.info("GridScrollPaint_Test: " + (immediate + deferred) + " scroll steps, new rows shown right away "
				+ immediate + ", old rows until the event loop ran " + deferred + ", no mixed or blank window");
	}

	@Test
	public void virtualRowsArePaintedWithTheirDataRightAfterAScroll() {
		final Grid grid = grid(SWT.VIRTUAL);
		grid.addListener(SWT.SetData, e -> {
			setData++;
			final GridItem item = (GridItem) e.item;
			for (int c = 0; c < 3; c++) {
				item.setText(c, "row " + e.index + " / " + c);
			}
		});
		grid.setItemCount(20_000);
		scrollScript(grid);
		assertTrue("SetData answered", setData > 0);
	}

	/*
	 * A blit scroll moves the body's pixels. Its first pixel row also shows the header's overflow
	 * right of the last column (Grid.HEADER_OVERFLOW, as the stock Grid paints it), which a move
	 * must neither carry into the body nor cover with row pixels; the classic tree column's dotted
	 * branch lines take their phase from the absolute pixel row, which an odd move would shift.
	 * Right after each blit scroll, before the event loop runs, and once settled, the window shows
	 * exactly the Grid's paint.
	 */
	@Test
	public void blitScrollKeepsTheHeaderOverflowInPlace() {
		blitScript(false);
	}

	@Test
	public void blitScrollKeepsTheBranchLinePhase() {
		blitScript(true);
	}

	/* a custom empty column header that paints three rows below its bounds: a wider band to keep */
	@Test
	public void blitScrollKeepsACustomHeaderOverflowInPlace() {
		overflowing = true;
		blitScript(false);
	}

	private boolean overflowing;

	private void blitScript(final boolean treeColumn) {
		final Grid grid = grid(SWT.NONE);
		grid.getColumn(0).setTree(treeColumn);
		if (overflowing) {
			grid.setEmptyColumnHeaderRenderer(new GridDifferentialDriver.OverflowingEmptyHeader());
		}
		for (int r = 0; r < 2000; r++) {
			new GridItem(grid, SWT.NONE).setText(0, "row " + r);
		}
		scrollScript(grid, "tree column " + treeColumn, true);
	}

	/*
	 * G5: rows of different heights (odd and even moves) and a tree with collapsed and expanded
	 * parents scroll by a blit of the row index's pixel delta in the viewport mode; the original
	 * mode repaints the body. Either way the window shows exactly the Grid's paint.
	 */
	@Test
	public void blitScrollWithVariableRowHeights() {
		final Grid grid = grid(SWT.NONE);
		grid.getColumn(0).setTree(false);
		for (int r = 0; r < 2000; r++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			item.setText(0, "row " + r);
			item.setHeight(14 + r % 3 * 7);
		}
		scrollScript(grid, "variable heights", GridViewport.ENABLED);
	}

	@Test
	public void blitScrollInATree() {
		final Grid grid = grid(SWT.NONE);
		// an even row extent: the classic tree column's dotted lines allow even moves only
		grid.setItemHeight(19);
		for (int r = 0; r < 300; r++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			root.setText(0, "root " + r);
			for (int c = 0; c < r % 5; c++) {
				new GridItem(root, SWT.NONE).setText(0, "child " + r + "." + c);
			}
			root.setExpanded(r % 3 != 0);
		}
		scrollScript(grid, "tree", GridViewport.ENABLED);
	}

	private void scrollScript(final Grid grid, final String label, final boolean expectBlits) {
		shell.open();
		settle();
		final ScrollBar bar = grid.getVerticalBar();
		final int blits = grid.blitScrollCount;
		// rows up and down by one and by several (odd and even), a jump of more than a page (no blit)
		final int[] values = { 1, 6, 5, 2, 30, 29, 40, 42, 38 };
		for (final int value : values) {
			final int before = grid.blitScrollCount;
			bar.setSelection(value);
			bar.notifyListeners(SWT.Selection, new Event());
			if (grid.blitScrollCount > before) {
				assertScreenIsPaint(grid, label + ", scroll bar " + value + ", right after the blit");
			}
			settle();
			assertScreenIsPaint(grid, label + ", scroll bar " + value + ", settled");
		}
		LOG.info("GridScrollPaint_Test: " + (grid.blitScrollCount - blits) + " of " + values.length
				+ " scroll steps were blits (" + label + "), window identical to the Grid's paint");
		if (expectBlits) {
			assertTrue("blit scrolls", grid.blitScrollCount > blits);
		}
	}

	private static void assertScreenIsPaint(final Grid grid, final String step) {
		final ImageData now = screen(grid);
		final ImageData painted = print(grid);
		if (!sameClientArea(grid, now, painted)) {
			GridViewportDifferential_Test.save(now, "blit-now");
			GridViewportDifferential_Test.save(painted, "blit-painted");
			fail(step + ": the window differs from the Grid's paint ("
					+ GridViewportDifferential_Test.difference(now, painted) + ")");
		}
	}

	@Test
	public void treeRowsArePaintedRightAfterAScroll() {
		final Grid grid = grid(SWT.NONE);
		for (int r = 0; r < 600; r++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			root.setText(0, "root " + r);
			for (int c = 0; r % 3 == 0 && c < 12; c++) {
				new GridItem(root, SWT.NONE).setText(0, "child " + r + "." + c);
			}
			root.setExpanded(r % 6 == 0);
		}
		scrollScript(grid);
	}
}
