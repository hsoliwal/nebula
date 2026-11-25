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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
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
		// these tests read the screen: the shell stays above other applications' windows, which
		// would be captured in its place
		shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
		// The positions below are client coordinates. The shell gets a client area of this size on every
		// platform: with a fixed outer size the window frame of Windows left the Grid one pixel outside it
		Rectangle trim = shell.computeTrim(0, 0, 420, 420);
		shell.setBounds(10, 10, trim.width, trim.height);
		new Text(shell, SWT.SINGLE).setBounds(5, 390, 60, 22);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	private Grid grid(final int style) {
		return configure(new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | style), style);
	}

	private static Grid configure(final Grid grid, final int style) {
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

	/* opens the shell and returns once the screen shows the Grid */
	private void show(final Grid grid) {
		shell.open();
		settle();
		awaitShown(grid);
	}

	/*
	 * Waits until the screen shows the Grid as it paints itself. The desktop draws a window that has
	 * just opened in a transition (scaled, blended frames) for some hundred milliseconds; those frames
	 * are not the Grid's pixels, and a comparison during them fails for every row. Only pumps events.
	 */
	static void awaitShown(final Grid grid) {
		final long deadline = System.currentTimeMillis() + 5000;
		while (!sameNativeContent(capture(grid), print(grid))) {
			if (System.currentTimeMillis() > deadline) {
				fail("the screen never showed the Grid's paint: another window covers the shell, or the desktop still animates it ("
						+ GridViewportDifferential_Test.difference(screen(grid), print(grid)) + ")");
			}
			if (!grid.getDisplay().readAndDispatch()) {
				try {
					Thread.sleep(20);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	/*
	 * Pump native events until the Grid's body paints are stable for 200 ms
	 * and no drag frame is queued. Other desktop events cannot extend a Grid
	 * paint-quiescence interval. The total five-second limit still fails closed.
	 */
	private void settle() {
		final long started = System.nanoTime();
		long bodyPaintIdleSince = started;
		long observedBodyPaints = bodyPaintTotal();
		int dispatched = 0;
		for (;;) {
			final long now = System.nanoTime();
			if (now - bodyPaintIdleSince >= 200_000_000L && !dragFramePending()) {
				return;
			}
			if (now - started >= 5_000_000_000L) {
				fail("Grid body paints did not settle within five seconds; dispatched "
						+ dispatched + " SWT events, body paints " + observedBodyPaints
						+ ", quiet " + (now - bodyPaintIdleSince) / 1_000_000L
						+ " ms, drag pending " + dragFramePending());
			}
			if (display.readAndDispatch()) {
				dispatched++;
			} else {
				try {
					Thread.sleep(5);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			final long currentBodyPaints = bodyPaintTotal();
			if (currentBodyPaints != observedBodyPaints) {
				observedBodyPaints = currentBodyPaints;
				bodyPaintIdleSince = System.nanoTime();
			}
		}
	}

	/** Count scrolling-body paints on this shell without adding SWT listeners. */
	private long bodyPaintTotal() {
		long paints = 0;
		for (final org.eclipse.swt.widgets.Control child : shell.getChildren()) {
			if (child instanceof Grid) {
				paints += ((Grid) child).bodyPaintCount;
			}
		}
		return paints;
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
			GridSwtScreenshotCapture.screenBounds(grid);
			final GC gc = new GC(grid.getDisplay());
			try {
				final Point origin = grid.toDisplay(0, 0);
				gc.copyArea(image, origin.x, origin.y);
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

	/** Native capture and its parent-relative scrollbar geometry, sampled at the same time. */
	private static GridSwtScreenshotCapture.ScrollPaintFrame capture(final Grid grid) {
		return GridSwtScreenshotCapture.captureScrollPaintFrame(grid, () -> screen(grid));
	}

	private static boolean sameNativeContent(final GridSwtScreenshotCapture.ScrollPaintFrame screen,
			final ImageData painted) {
		return GridSwtScreenshotCapture.sameScrollPaintedContent(screen, painted, screen);
	}

	/* the Grid's settled pixels before the current step */
	private ImageData previous;
	private GridSwtScreenshotCapture.ScrollPaintFrame previousFrame;
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
		final GridSwtScreenshotCapture.ScrollPaintFrame nowFrame = capture(grid);
		final ImageData now = nowFrame.pixels;
		final int answered = setData - before;
		final int blankNow = blankRows(grid, now);
		settle();
		final GridSwtScreenshotCapture.ScrollPaintFrame settledFrame = capture(grid);
		final ImageData onScreen = settledFrame.pixels;
		final ImageData settled = print(grid);
		if (!sameNativeContent(settledFrame, settled)) {
			GridViewportDifferential_Test.save(onScreen, "scroll-screen");
			GridViewportDifferential_Test.save(settled, "scroll-settled");
			fail(step + ": the settled window differs from the Grid's paint ("
					+ GridViewportDifferential_Test.difference(onScreen, settled) + ")");
		}
		final boolean isNew = GridSwtScreenshotCapture.sameScrollPaintedContent(nowFrame, settled, settledFrame);
		final boolean isOld = previous != null
				&& GridSwtScreenshotCapture.sameScrollPaintedContent(nowFrame, previous, previousFrame);
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
		previousFrame = settledFrame;
	}

	static boolean sameClientArea(final Grid grid, final ImageData a, final ImageData b) {
		final org.eclipse.swt.graphics.Rectangle area = grid.getClientArea();
		// Both images start at client (0, 0). GTK overlay scrollbars do not consume
		// client width; subtracting their size again produces negative pixel indices.
		assertTrue("captured client must fit both images", area.x >= 0 && area.y >= 0
				&& (long) area.x + area.width <= Math.min(a.width, b.width)
				&& (long) area.y + area.height <= Math.min(a.height, b.height));
		for (int y = area.y; y < area.y + area.height; y++) {
			for (int x = area.x; x < area.x + area.width; x++) {
				if (a.getPixel(x, y) != b.getPixel(x, y)) {
					return false;
				}
			}
		}
		return true;
	}

	private void scrollScript(final Grid grid) {
		show(grid);
		previousFrame = capture(grid);
		previous = previousFrame.pixels;
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

	/* twelve columns of 90 pixels, the first one frozen, 2000 rows */
	private Grid frozenColumnGrid() {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 400, 370);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		for (int c = 0; c < 12; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setWidth(90);
			column.setFixed(c == 0);
		}
		for (int r = 0; r < 2000; r++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 12; c++) {
				item.setText(c, r + ":" + c);
			}
		}
		return grid;
	}

	/*
	 * Optimization pass, frozen columns (nebula#696, #165): a horizontal scroll moves the pixels
	 * right of the frozen column and repaints the exposed strip and the header row. Steps to the
	 * right and left by odd and even amounts, one across the overlay's activation and one of more
	 * than the scrolled width (no blit); the window equals the Grid's paint right after each blit (the
	 * viewport mode; the original mode repaints, as stock) and once settled, in both modes.
	 */
	@Test
	public void horizontalBlitUnderAFrozenColumn() {
		final Grid grid = frozenColumnGrid();
		show(grid);
		final ScrollBar bar = grid.getHorizontalBar();
		final int blits = grid.hBlitScrollCount;
		final int[] values = { 37, 74, 73, 200, 160, 10, 0, 150, 151, 700 };
		for (final int value : values) {
			final int before = grid.hBlitScrollCount;
			bar.setSelection(value);
			bar.notifyListeners(SWT.Selection, new Event());
			if (grid.hBlitScrollCount > before) {
				assertScreenIsPaint(grid, "hbar " + value + ", right after the blit");
			}
			settle();
			assertScreenIsPaint(grid, "hbar " + value + ", settled");
		}
		LOG.info("GridScrollPaint_Test: " + (grid.hBlitScrollCount - blits) + " of " + values.length
				+ " horizontal steps were blits (frozen column), window identical to the Grid's paint");
		if (GridViewport.ENABLED) {
			assertTrue("horizontal blit scrolls", grid.hBlitScrollCount > blits);
		}
	}

	private void scrollScript(final Grid grid, final String label, final boolean expectBlits) {
		show(grid);
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
		final GridSwtScreenshotCapture.ScrollPaintFrame frame = capture(grid);
		final ImageData now = frame.pixels;
		final ImageData painted = print(grid);
		if (!sameNativeContent(frame, painted)) {
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

	/*
	 * A paint that is pending when a scroll arrives shows the rows of the new position; the pixels it
	 * painted must not be moved again. Whatever was invalid before the scroll (the whole Grid, one cell),
	 * the settled window shows exactly the Grid's paint.
	 */
	@Test
	public void repaintPendingAtAScrollIsNotMoved() {
		final Grid grid = grid(SWT.NONE);
		grid.getColumn(0).setTree(false);
		for (int r = 0; r < 2000; r++) {
			new GridItem(grid, SWT.NONE).setText(0, "row " + r);
		}
		show(grid);
		final ScrollBar bar = grid.getVerticalBar();
		final int[] values = { 1, 6, 5, 2, 9, 3 };
		for (int i = 0; i < values.length; i++) {
			final boolean whole = i % 2 == 0;
			if (whole) {
				grid.redraw();
			} else {
				grid.redraw(40, grid.getHeaderHeight() + 3 * (grid.getItemHeight() + 1), 60, 30, false);
			}
			bar.setSelection(values[i]);
			bar.notifyListeners(SWT.Selection, new Event());
			final String step = "scroll bar " + values[i] + (whole ? " with the Grid invalid" : " with one cell invalid");
			settle();
			assertScreenIsPaint(grid, step + ", settled");
		}
	}

	@Test
	public void repaintPendingAtAHorizontalScrollIsNotMoved() {
		final Grid grid = frozenColumnGrid();
		show(grid);
		final ScrollBar bar = grid.getHorizontalBar();
		final int[] values = { 37, 74, 73, 200, 160, 10 };
		for (int i = 0; i < values.length; i++) {
			final boolean whole = i % 2 == 0;
			if (whole) {
				grid.redraw();
			} else {
				grid.redraw(150, grid.getHeaderHeight() + 3 * (grid.getItemHeight() + 1), 60, 30, false);
			}
			bar.setSelection(values[i]);
			bar.notifyListeners(SWT.Selection, new Event());
			final String step = "hbar " + values[i] + (whole ? " with the Grid invalid" : " with one cell invalid");
			settle();
			assertScreenIsPaint(grid, step + ", settled");
		}
	}

	/*
	 * A scroll while the pointer is over the Grid changes the hovered row, and a hover change
	 * invalidates the client area. A test cannot place the pointer, so this Grid's hover refresh
	 * invalidates the way a hover change does. The scroll is still a blit, and the window shows
	 * exactly the Grid's paint right after it and once settled.
	 */
	@Test
	public void hoverChangeAtAScrollKeepsTheBlitAndTheWindowExact() {
		final Grid grid = configure(new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL) {
			@Override
			protected void refreshHoverState() {
				redraw();
			}
		}, SWT.NONE);
		grid.getColumn(0).setTree(false);
		for (int r = 0; r < 2000; r++) {
			new GridItem(grid, SWT.NONE).setText(0, "row " + r);
		}
		show(grid);
		final ScrollBar bar = grid.getVerticalBar();
		for (final int value : new int[] { 1, 6, 5, 2, 9, 3 }) {
			final int blits = grid.blitScrollCount;
			bar.setSelection(value);
			bar.notifyListeners(SWT.Selection, new Event());
			assertEquals("scroll bar " + value + " under a hover change: blit scrolls", blits + 1, grid.blitScrollCount);
			assertScreenIsPaint(grid, "scroll bar " + value + " under a hover change, right after the blit");
			settle();
			assertScreenIsPaint(grid, "scroll bar " + value + " under a hover change, settled");
		}
	}

	@Test
	public void measuredNativeChromeNeverMasksGridContentOrWrongGeometry() {
		final org.eclipse.swt.graphics.PaletteData palette =
				new org.eclipse.swt.graphics.PaletteData(0xff0000, 0x00ff00, 0x0000ff);
		final ImageData pixels = new ImageData(100, 80, 24, palette);
		final ImageData painted = new ImageData(100, 80, 24, palette);
		final GridSwtScreenshotCapture.ScrollPaintBar v = new GridSwtScreenshotCapture.ScrollPaintBar(
				new Rectangle(94, 0, 6, 74), new Rectangle(94, 5, 6, 20));
		final GridSwtScreenshotCapture.ScrollPaintBar h = new GridSwtScreenshotCapture.ScrollPaintBar(
				new Rectangle(0, 74, 94, 6), new Rectangle(6, 74, 20, 6));
		final GridSwtScreenshotCapture.ScrollPaintFrame frame =
				new GridSwtScreenshotCapture.ScrollPaintFrame(pixels,
						new Rectangle(0, 0, 100, 80), true, true, v, v, h, h);
		pixels.setPixel(99, 40, 0x123456); // measured native vertical track
		pixels.setPixel(94, 40, 0x123456); // exact first masked right pixel
		pixels.setPixel(40, 79, 0x123456); // measured native horizontal track
		pixels.setPixel(40, 74, 0x123456); // exact first masked bottom pixel
		assertTrue(GridSwtScreenshotCapture.sameScrollPaintedContent(frame, painted, frame));
		pixels.setPixel(93, 40, 0xabcdef); // last interior pixel
		assertTrue(!GridSwtScreenshotCapture.sameScrollPaintedContent(frame, painted, frame));
		pixels.setPixel(93, 40, 0);
		pixels.setPixel(40, 73, 0xabcdef); // last interior row
		assertTrue(!GridSwtScreenshotCapture.sameScrollPaintedContent(frame, painted, frame));
		pixels.setPixel(40, 73, 0);
		final GridSwtScreenshotCapture.ScrollPaintFrame plain =
				new GridSwtScreenshotCapture.ScrollPaintFrame(pixels,
						new Rectangle(0, 0, 100, 80), false, false, null, null, null, null);
		assertTrue(!GridSwtScreenshotCapture.sameScrollPaintedContent(plain, painted, plain));
		boolean refused = false;
		try {
			new GridSwtScreenshotCapture.ScrollPaintFrame(pixels,
						new Rectangle(0, 0, 100, 80), true, true,
						new GridSwtScreenshotCapture.ScrollPaintBar(new Rectangle(93, 0, 6, 74),
								new Rectangle(93, 5, 6, 20)), v, h, h);
		} catch (final AssertionError expected) {
			refused = true;
		}
		assertTrue("undocked native scrollbar must be refused", refused);
		refused = false;
		try {
			GridSwtScreenshotCapture.scrollPaintContentClip(frame, plain);
		} catch (final AssertionError expected) {
			refused = true;
		}
		assertTrue("GTK3/native visibility drift must be refused", refused);
	}
	@Test
	public void nativeChromeAnimatedWidthsHaveOneHalfOpenContentClip() {
		final org.eclipse.swt.graphics.PaletteData palette =
				new org.eclipse.swt.graphics.PaletteData(0xff0000, 0x00ff00, 0x0000ff);
		final ImageData pixels = new ImageData(100, 80, 24, palette);
		final ImageData painted = new ImageData(100, 80, 24, palette);
		final Rectangle client = new Rectangle(0, 0, 100, 80);
		final GridSwtScreenshotCapture.ScrollPaintBar verticalBefore =
				new GridSwtScreenshotCapture.ScrollPaintBar(new Rectangle(96, 0, 4, 74),
						new Rectangle(96, 6, 4, 20));
		final GridSwtScreenshotCapture.ScrollPaintBar verticalAfter =
				new GridSwtScreenshotCapture.ScrollPaintBar(new Rectangle(94, 0, 6, 74),
						new Rectangle(94, 6, 6, 20));
		final GridSwtScreenshotCapture.ScrollPaintBar horizontalBefore =
				new GridSwtScreenshotCapture.ScrollPaintBar(new Rectangle(0, 74, 96, 6),
						new Rectangle(6, 74, 20, 6));
		final GridSwtScreenshotCapture.ScrollPaintBar horizontalAfter =
				new GridSwtScreenshotCapture.ScrollPaintBar(new Rectangle(0, 74, 94, 6),
						new Rectangle(6, 74, 20, 6));
		final GridSwtScreenshotCapture.ScrollPaintFrame widened =
				new GridSwtScreenshotCapture.ScrollPaintFrame(pixels, client, true, true,
						verticalBefore, verticalAfter, horizontalBefore, horizontalAfter);
		final GridSwtScreenshotCapture.ScrollPaintFrame narrow =
				new GridSwtScreenshotCapture.ScrollPaintFrame(pixels, client, true, true,
						verticalBefore, verticalBefore, horizontalBefore, horizontalBefore);
		final Rectangle clip = GridSwtScreenshotCapture.scrollPaintContentClip(widened, narrow);
		assertEquals("union must be measured once", new Rectangle(0, 0, 94, 74), clip);
		pixels.setPixel(95, 0, 0x101010);
		assertTrue(GridSwtScreenshotCapture.sameScrollPaintedContent(widened, painted, narrow));
		pixels.setPixel(93, 73, 0x202020);
		assertTrue(!GridSwtScreenshotCapture.sameScrollPaintedContent(widened, painted, narrow));
		pixels.setPixel(93, 73, 0);
		boolean refused = false;
		try {
			new GridSwtScreenshotCapture.ScrollPaintBar(new Rectangle(94, 0, 6, 74),
						new Rectangle(93, 6, 6, 20));
		} catch (final AssertionError expected) {
			refused = true;
		}
		assertTrue("native thumb outside track must fail", refused);
		// This otherwise perfectly valid full-height 6px bar isolates the GTK3
		// platform gate; a short track must not pass this test for another reason.
		final GridSwtScreenshotCapture.ScrollPaintBar nonGtkValidBar =
				new GridSwtScreenshotCapture.ScrollPaintBar(
						new Rectangle(94, 0, 6, 80), new Rectangle(94, 6, 6, 20));
		refused = false;
		try {
			new GridSwtScreenshotCapture.ScrollPaintFrame(pixels, client, false, false,
						nonGtkValidBar, nonGtkValidBar, null, null);
		} catch (final AssertionError expected) {
			refused = true;
		}
		assertTrue("only GTK3 may carry a chrome exclusion", refused);
	}

	@Test
	public void insetScrollbarOwnsNoGridContentPixelsAndStyleDriftIsRefused() {
		final org.eclipse.swt.graphics.PaletteData palette =
				new org.eclipse.swt.graphics.PaletteData(0xff0000, 0x00ff00, 0x0000ff);
		final ImageData pixels = new ImageData(100, 80, 24, palette);
		final ImageData painted = new ImageData(100, 80, 24, palette);
		final Rectangle client = new Rectangle(0, 0, 100, 80);
		final GridSwtScreenshotCapture.ScrollPaintBar verticalInset =
				new GridSwtScreenshotCapture.ScrollPaintBar(new Rectangle(100, 0, 6, 80),
						new Rectangle(100, 20, 6, 20));
		final GridSwtScreenshotCapture.ScrollPaintBar horizontalInset =
				new GridSwtScreenshotCapture.ScrollPaintBar(new Rectangle(0, 80, 100, 6),
						new Rectangle(10, 80, 30, 6));
		final GridSwtScreenshotCapture.ScrollPaintFrame inset =
				new GridSwtScreenshotCapture.ScrollPaintFrame(pixels, client, true, false,
						verticalInset, verticalInset, horizontalInset, horizontalInset);
		assertEquals("inset scrollbar cannot take a content pixel", client,
				GridSwtScreenshotCapture.scrollPaintContentClip(inset, inset));
		pixels.setPixel(99, 20, 0x123456);
		assertTrue(!GridSwtScreenshotCapture.sameScrollPaintedContent(inset, painted, inset));
		pixels.setPixel(99, 20, 0);
		pixels.setPixel(20, 79, 0x123456);
		assertTrue(!GridSwtScreenshotCapture.sameScrollPaintedContent(inset, painted, inset));
		pixels.setPixel(20, 79, 0);
		final GridSwtScreenshotCapture.ScrollPaintBar verticalOverlay =
				new GridSwtScreenshotCapture.ScrollPaintBar(new Rectangle(94, 0, 6, 80),
						new Rectangle(94, 20, 6, 20));
		boolean refused = false;
		try {
			new GridSwtScreenshotCapture.ScrollPaintFrame(pixels, client, true, false,
						verticalInset, verticalOverlay, null, null);
		} catch (final AssertionError expected) {
			refused = true;
		}
		assertTrue("an inset-to-overlay transition within a capture is not stable", refused);
		final GridSwtScreenshotCapture.ScrollPaintFrame over =
				new GridSwtScreenshotCapture.ScrollPaintFrame(pixels, client, true, true,
						verticalOverlay, verticalOverlay, null, null);
		refused = false;
		try {
			GridSwtScreenshotCapture.scrollPaintContentClip(inset, over);
		} catch (final AssertionError expected) {
			refused = true;
		}
		assertTrue("an inset-to-overlay transition between captures is not comparable", refused);
		refused = false;
		try {
			new GridSwtScreenshotCapture.ScrollPaintFrame(pixels, client, true, false,
						new GridSwtScreenshotCapture.ScrollPaintBar(new Rectangle(99, 0, 6, 80),
								new Rectangle(99, 20, 6, 20)),
						verticalInset, null, null);
		} catch (final AssertionError expected) {
			refused = true;
		}
		assertTrue("a partially intruding native track cannot be treated as an inset", refused);
	}

	/** Native GTK3 may report a mapped 1x1 overlay allocation and a larger slider. */
	@Test
	public void collapsedNativeGtkOverlayNeverAuthorizesAnUnmeasuredPixelExclusion() {
		final org.eclipse.swt.graphics.PaletteData palette =
				new org.eclipse.swt.graphics.PaletteData(0xff0000, 0x00ff00, 0x0000ff);
		final ImageData nativePixels = new ImageData(100, 80, 24, palette);
		final ImageData modelPaint = new ImageData(100, 80, 24, palette);
		final Rectangle client = new Rectangle(0, 0, 100, 80);
		final GridSwtScreenshotCapture.ScrollPaintBar unmeasuredVertical =
				GridSwtScreenshotCapture.ScrollPaintBar.unmeasuredGtk3Overlay(
						new Rectangle(94, 0, 1, 1), new Rectangle(94, 0, 1, 145));
		final GridSwtScreenshotCapture.ScrollPaintBar unmeasuredHorizontal =
				GridSwtScreenshotCapture.ScrollPaintBar.unmeasuredGtk3Overlay(
						new Rectangle(0, 74, 1, 1), new Rectangle(0, 74, 147, 1));
		final GridSwtScreenshotCapture.ScrollPaintFrame uncertain =
				new GridSwtScreenshotCapture.ScrollPaintFrame(nativePixels, client,
						true, true, unmeasuredVertical, unmeasuredVertical,
						unmeasuredHorizontal, unmeasuredHorizontal);
		assertEquals("unmeasurable native painted extent cannot hide any content",
				client, GridSwtScreenshotCapture.scrollPaintContentClip(uncertain, uncertain));
		nativePixels.setPixel(99, 40, 0x102030);
		assertTrue("rightmost body pixel must not be masked",
				!GridSwtScreenshotCapture.sameScrollPaintedContent(uncertain, modelPaint, uncertain));
		nativePixels.setPixel(99, 40, 0);
		nativePixels.setPixel(40, 79, 0x102030);
		assertTrue("bottommost body pixel must not be masked",
				!GridSwtScreenshotCapture.sameScrollPaintedContent(uncertain, modelPaint, uncertain));
		nativePixels.setPixel(40, 79, 0);
		assertTrue(GridSwtScreenshotCapture.sameScrollPaintedContent(uncertain, modelPaint, uncertain));
		final GridSwtScreenshotCapture.ScrollPaintBar measured =
				new GridSwtScreenshotCapture.ScrollPaintBar(
						new Rectangle(94, 0, 6, 80), new Rectangle(94, 6, 6, 20));
		final GridSwtScreenshotCapture.ScrollPaintFrame qualified =
				new GridSwtScreenshotCapture.ScrollPaintFrame(nativePixels, client,
						true, true, measured, measured, null, null);
		boolean refused = false;
		try {
			GridSwtScreenshotCapture.scrollPaintContentClip(uncertain, qualified);
		} catch (final AssertionError expected) {
			refused = true;
		}
		assertTrue("unknown and measured native ownership cannot be mixed", refused);
	}

	@Test
	public void swtScrollbarModeIsAuthoritativeForNativePixelExclusion() {
		final org.eclipse.swt.graphics.PaletteData palette =
				new org.eclipse.swt.graphics.PaletteData(0xff0000, 0x00ff00, 0x0000ff);
		final ImageData actual = new ImageData(100, 80, 24, palette);
		final ImageData expected = new ImageData(100, 80, 24, palette);
		final Rectangle client = new Rectangle(0, 0, 100, 80);
		final GridSwtScreenshotCapture.ScrollPaintBar overlay =
				new GridSwtScreenshotCapture.ScrollPaintBar(
						new Rectangle(94, 0, 6, 80), new Rectangle(94, 10, 6, 20));
		final GridSwtScreenshotCapture.ScrollPaintBar inset =
				new GridSwtScreenshotCapture.ScrollPaintBar(
						new Rectangle(100, 0, 6, 80), new Rectangle(100, 10, 6, 20));
		// Matching geometry is not permission to crop when SWT explicitly reports NONE.
		boolean refused = false;
		try {
			new GridSwtScreenshotCapture.ScrollPaintFrame(actual, client,
					true, false, overlay, overlay, null, null);
		} catch (final AssertionError expectedRefusal) {
			refused = true;
		}
		assertTrue("SWT.NONE cannot authorize an overlay mask", refused);
		final GridSwtScreenshotCapture.ScrollPaintFrame over =
				new GridSwtScreenshotCapture.ScrollPaintFrame(actual, client,
					true, true, overlay, overlay, null, null);
		final GridSwtScreenshotCapture.ScrollPaintFrame inside =
				new GridSwtScreenshotCapture.ScrollPaintFrame(actual, client,
					true, false, inset, inset, null, null);
		actual.setPixel(99, 25, 0x123456);
		assertTrue("measured overlay owns its right strip",
				GridSwtScreenshotCapture.sameScrollPaintedContent(over, expected, over));
		assertTrue("SWT.NONE retains the same pixel as Grid content",
				!GridSwtScreenshotCapture.sameScrollPaintedContent(inside, expected, inside));
		actual.setPixel(99, 25, 0);
		refused = false;
		try {
			GridSwtScreenshotCapture.scrollPaintContentClip(over, inside);
		} catch (final AssertionError expectedRefusal) {
			refused = true;
		}
		assertTrue("incompatible native modes cannot be joined", refused);
		refused = false;
		try {
			new GridSwtScreenshotCapture.ScrollPaintFrame(actual, client,
					true, true, inset, inset, null, null);
		} catch (final AssertionError expectedRefusal) {
			refused = true;
		}
		assertTrue("SWT overlay mode cannot claim inset geometry", refused);
		// GTK4/Win32/Cocoa must keep all pixels, regardless of their own overlay style.
		final GridSwtScreenshotCapture.ScrollPaintFrame nonGtk =
				new GridSwtScreenshotCapture.ScrollPaintFrame(actual, client,
					false, false, null, null, null, null);
		actual.setPixel(99, 25, 0x654321);
		assertTrue("other platforms compare all client pixels",
				!GridSwtScreenshotCapture.sameScrollPaintedContent(nonGtk, expected, nonGtk));
		actual.setPixel(99, 25, 0);
		final GridSwtScreenshotCapture.ScrollPaintBar horizontalOverlay =
				new GridSwtScreenshotCapture.ScrollPaintBar(
						new Rectangle(0, 74, 100, 6), new Rectangle(10, 74, 20, 6));
		refused = false;
		try {
			new GridSwtScreenshotCapture.ScrollPaintFrame(actual, client,
					true, false, null, null, horizontalOverlay, horizontalOverlay);
		} catch (final AssertionError expectedRefusal) {
			refused = true;
		}
		assertTrue("SWT.NONE cannot authorize horizontal native masking", refused);
		final GridSwtScreenshotCapture.ScrollPaintFrame noBarOverlay =
				new GridSwtScreenshotCapture.ScrollPaintFrame(actual, client,
					true, true, null, null, null, null);
		final GridSwtScreenshotCapture.ScrollPaintFrame noBarInset =
				new GridSwtScreenshotCapture.ScrollPaintFrame(actual, client,
					true, false, null, null, null, null);
		refused = false;
		try {
			GridSwtScreenshotCapture.scrollPaintContentClip(noBarOverlay, noBarInset);
		} catch (final AssertionError expectedRefusal) {
			refused = true;
		}
		assertTrue("mode comparison must reject drift even with no native bars", refused);
		refused = false;
		try {
			new GridSwtScreenshotCapture.ScrollPaintFrame(actual, client,
					false, true, null, null, null, null);
		} catch (final AssertionError expectedRefusal) {
			refused = true;
		}
		assertTrue("non-GTK3 must never claim GTK3 exclusion authority", refused);
	}
}
