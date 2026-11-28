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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Leaf G7, one model with deltas: a change of one row's data or height inside the window repaints
 * that row (from that row down for a height), and a change outside the window or on a hidden row
 * repaints nothing, since the rows on screen and the row-based scroll bar do not move. After every
 * change the window equals the Grid's own full paint, in both modes; the paint counts hold in the
 * viewport mode (the original mode repaints the Grid, as stock).
 * <p>
 * Items added to a shown Grid leave the scroll values to the next paint, and the setters that
 * follow each add join that paint: in the viewport mode the native work of a loop of adds and
 * setters does not grow with its length, the Grid's row getters stay exact during the loop, and
 * after the paint the scroll bars and the window equal a full recomputation and a full repaint.
 * </p>
 */
public class GridRowDelta_Test {
	private Display display;
	private Shell shell;
	private Grid grid;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(10, 10, 460, 420);
		// the focus stays here: a focused Grid paints a focus rectangle that is not part of print()
		new Text(shell, SWT.SINGLE).setBounds(5, 390, 60, 22);
		grid = new Grid(shell, SWT.MULTI | SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 440, 370);
		grid.setHeaderVisible(true);
		for (int c = 0; c < 3; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setWidth(120);
			column.setTree(c == 0);
		}
		for (int r = 0; r < 600; r++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			root.setText(0, "root " + r);
			root.setText(1, "value " + r);
			for (int k = 0; k < 2; k++) {
				new GridItem(root, SWT.NONE).setText(0, "child " + r + "." + k);
			}
			root.setExpanded(r % 2 == 0);
		}
		shell.open();
		GridTestUtil.awaitGridPaints(display, grid, 1);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void dataChangesOffScreenPaintNothingAndOnScreenPaintTheRow() {
		final GridItem far = grid.getItem(grid.getItemCount() - 10);
		assertDelta("text off screen", () -> far.setText(1, "changed"), 0, 1);
		assertDelta("background off screen", () -> far.setBackground(1, display.getSystemColor(SWT.COLOR_YELLOW)), 0, 1);
		final GridItem hidden = grid.getRootItem(1).getItem(0);
		assertDelta("text of a hidden row", () -> hidden.setText(1, "hidden changed"), 0, 1);
		// row 6 is root 2 (root 1, rows 3 to 5, is collapsed: rows 4 and 5 are hidden)
		final GridItem shown = grid.getItem(6);
		assertDelta("text on screen", () -> shown.setText(1, "visible changed"), 1, 1);
		assertDelta("background on screen", () -> shown.setBackground(2, display.getSystemColor(SWT.COLOR_CYAN)), 1, 1);
		assertDelta("checked on screen", () -> shown.setChecked(true), 1, 1);
		// the changes are there when their rows come into view
		grid.getRootItem(1).setExpanded(true);
		assertScreenIsPaint("after expanding the hidden row's parent");
		grid.showItem(far);
		assertScreenIsPaint("after showing the far row");
		assertEquals("changed", far.getText(1));
	}

	@Test
	public void heightChangesOffScreenRepaintNothingAndKeepTheScrollBarsCurrent() {
		final int bottom = grid.getBottomIndex();
		final GridItem far = grid.getItem(grid.getItemCount() - 10);
		// the first height that differs switches the Grid to variable heights: the scroll bars change
		// shape (stock repaints for that), so the deltas start with the second
		grid.getItem(grid.getItemCount() - 1).setHeight(20);
		flush();
		assertDelta("height off screen", () -> far.setHeight(40), 0, 1);
		assertScrollBarsCurrent("height off screen");
		assertEquals(bottom, grid.getBottomIndex());
		final GridItem hidden = grid.getRootItem(1).getItem(1);
		assertDelta("height of a hidden row", () -> hidden.setHeight(30), 0, 1);
		assertScrollBarsCurrent("height of a hidden row");
		grid.showItem(far);
		assertScreenIsPaint("after showing the taller far row");
		assertEquals(40, far.getHeight());
		grid.setTopIndex(0);
		flush();
		final GridItem shown = grid.getItem(3);
		assertDelta("height on screen", () -> shown.setHeight(45), 1, 1);
		assertScrollBarsCurrent("height on screen");
		assertTrue("the bottom row moved up", grid.getBottomIndex() < bottom);
		grid.getRootItem(1).setExpanded(true);
		assertScreenIsPaint("after expanding the taller hidden row's parent");
	}

	@Test
	public void rowColoursOffScreenPaintNothingAndOnScreenPaintTheRow() {
		// whole-row and row-header colours change one row: off screen nothing paints, on screen the row
		grid.setRowHeaderVisible(true);
		flush();
		final GridItem far = grid.getItem(grid.getItemCount() - 10);
		assertBodyPaints("row background off screen", () -> far.setBackground(display.getSystemColor(SWT.COLOR_YELLOW)), 0, 1);
		assertBodyPaints("row foreground off screen", () -> far.setForeground(display.getSystemColor(SWT.COLOR_RED)), 0, 1);
		assertBodyPaints("header background off screen", () -> far.setHeaderBackground(display.getSystemColor(SWT.COLOR_GREEN)), 0, 1);
		final GridItem shown = grid.getItem(6);
		assertBodyPaints("row background on screen", () -> shown.setBackground(display.getSystemColor(SWT.COLOR_CYAN)), 1, 1);
		assertBodyPaints("row foreground on screen", () -> shown.setForeground(display.getSystemColor(SWT.COLOR_BLUE)), 1, 1);
		assertBodyPaints("header background on screen", () -> shown.setHeaderBackground(display.getSystemColor(SWT.COLOR_MAGENTA)), 1, 1);
		assertBodyPaints("header foreground on screen", () -> shown.setHeaderForeground(display.getSystemColor(SWT.COLOR_WHITE)), 1, 1);
		assertEquals(display.getSystemColor(SWT.COLOR_YELLOW), far.getBackground(1));
	}

	@Test
	public void addsWithSettersOnAShownGridIssueNoNativeWorkPerItem() {
		// each add owes the whole client to the next paint, and so do its setters: no invalidation
		// or scroll bar update of their own, so the native work does not grow with the adds
		final int few = addRootsWithSetters(50);
		assertScrollBarsCurrent("after 50 adds with setters");
		assertSameAsFullRepaint("after 50 adds with setters");
		final int many = addRootsWithSetters(500);
		assertScrollBarsCurrent("after 500 more adds with setters");
		assertSameAsFullRepaint("after 500 more adds with setters");
		if (GridViewport.ENABLED) {
			assertTrue("native work of 50 adds with setters: " + few, few >= 1 && few <= 2);
			assertEquals("native work of 500 adds with setters (50: " + few + ")", few, many);
		}
		final GridItem last = grid.getItem(grid.getItemCount() - 1);
		grid.showItem(last);
		flush();
		assertSameAsFullRepaint("the added rows in view");
		assertEquals("added 499", last.getText(0));
		assertEquals(18 + (499 & 3), last.getHeight());
		assertTrue(last.getChecked());
		assertEquals(display.getSystemColor(SWT.COLOR_YELLOW), last.getBackground(1));
	}

	@Test
	public void rowGettersStayExactWhileAddsWithSettersWaitForThePaint() {
		// the Grid's own row getters settle the scroll values they read, so they stay exact between
		// an add and its paint; rows appended below the window move neither its top nor its bottom
		flush();
		final int top = grid.getTopIndex();
		final int bottom = grid.getBottomIndex();
		final int count = grid.getItemCount();
		final int roots = grid.getRootItemCount();
		final Color yellow = display.getSystemColor(SWT.COLOR_YELLOW);
		for (int i = 0; i < 60; i++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			item.setText(0, "added " + i);
			item.setBackground(1, yellow);
			item.setChecked(true);
			item.setHeight(18 + (i & 3));
			final String step = "after add " + i;
			assertEquals(step, count + i + 1, grid.getItemCount());
			assertEquals(step, roots + i + 1, grid.getRootItemCount());
			assertSame(step, item, grid.getItem(count + i));
			assertEquals(step, count + i, grid.indexOf(item));
			assertEquals(step + ": top row", top, grid.getTopIndex());
			assertEquals(step + ": bottom row", bottom, grid.getBottomIndex());
		}
		flush();
		assertEquals("top row after the paint", top, grid.getTopIndex());
		assertEquals("bottom row after the paint", bottom, grid.getBottomIndex());
		assertScrollBarsCurrent("after the adds");
		assertSameAsFullRepaint("after the adds");
	}

	@Test
	public void aDeltaMadeDuringAPaintIsPaintedByTheNextPaint() {
		// a paint consumes the invalidation owed to it as it starts: an add made while it runs (here
		// by a later paint listener) is owed again, and another paint shows it
		flush();
		final GridItem[] madeInPaint = new GridItem[1];
		final Listener addInPaint = e -> {
			if (madeInPaint[0] == null) {
				madeInPaint[0] = new GridItem(grid, SWT.NONE, 0);
				madeInPaint[0].setText(0, "added during a paint");
			}
		};
		grid.addListener(SWT.Paint, addInPaint);
		final int paints = grid.paintPasses;
		try {
			new GridItem(grid, SWT.NONE, 0).setText(0, "added before the paint");
			flush();
		} finally {
			grid.removeListener(SWT.Paint, addInPaint);
		}
		assertNotNull("the paint listener ran", madeInPaint[0]);
		assertSame(madeInPaint[0], grid.getItem(0));
		assertTrue("the add made during a paint is painted by another one (" + (grid.paintPasses - paints) + " paints)",
				grid.paintPasses - paints >= 2);
		assertSameAsFullRepaint("after an add made during a paint");
	}

	@Test
	public void addsAroundAScrollLoseNoInvalidation() {
		// a scroll right after an unpainted add paints what is owed first (a blit moves painted
		// pixels only), and an add right after a scroll is invalidated again
		flush();
		new GridItem(grid, SWT.NONE, 0).setText(0, "added before a scroll");
		grid.setTopIndex(grid.indexOf(grid.getRootItem(3)));
		flush();
		assertSameAsFullRepaint("a scroll after an unpainted add");
		final int blits = grid.blitScrollCount;
		grid.setTopIndex(grid.indexOf(grid.getRootItem(5)));
		new GridItem(grid, SWT.NONE, 0).setText(0, "added after a scroll");
		grid.getItem(grid.getTopIndex()).setText(1, "changed after a scroll");
		flush();
		assertSameAsFullRepaint("an add right after a scroll" + (grid.blitScrollCount > blits ? " (blit)" : ""));
	}

	@Test
	public void setRedrawAroundAddsLosesNoInvalidation() {
		// what turning drawing off and on repaints is the platform's: the first delta after it asks
		// for an invalidation again instead of merging into one made while drawing was off
		flush();
		final int deltas = grid.hiddenDeltas;
		grid.setRedraw(false);
		final Color yellow = display.getSystemColor(SWT.COLOR_YELLOW);
		for (int i = 0; i < 40; i++) {
			final GridItem item = new GridItem(grid, SWT.NONE, 0);
			item.setText(0, "added while drawing is off " + i);
			item.setBackground(1, yellow);
			item.setChecked(true);
			item.setHeight(18 + (i & 3));
		}
		grid.setRedraw(true);
		final int before = grid.nativeOps;
		new GridItem(grid, SWT.NONE, 0).setText(0, "added after setRedraw(true)");
		if (GridViewport.ENABLED) {
			assertEquals("the first add after setRedraw(true) invalidates", before + 1, grid.nativeOps);
		}
		flush();
		assertEquals("drawing turned off is not hidden", deltas, grid.hiddenDeltas);
		assertScrollBarsCurrent("after setRedraw(true)");
		assertSameAsFullRepaint("after setRedraw(true)");
	}

	@Test
	public void addsWithSettersOnAHiddenGridStayHiddenDeltas() {
		// hidden, every delta stays a hidden delta with no native work, as before; the show paints
		// one window patch
		flush();
		grid.setVisible(false);
		flush();
		final int deltas = grid.hiddenDeltas;
		final int patches = grid.hiddenPatches;
		final int ops = grid.nativeOps;
		final int merged = grid.mergedInvalidations;
		final Color yellow = display.getSystemColor(SWT.COLOR_YELLOW);
		final int adds = 40;
		for (int i = 0; i < adds; i++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			item.setText(0, "added while hidden " + i);
			item.setBackground(1, yellow);
			item.setChecked(true);
			item.setHeight(18 + (i & 3));
		}
		if (GridViewport.ENABLED) {
			assertEquals("native work while hidden", 0, grid.nativeOps - ops);
			assertEquals("hidden deltas: each add and each of its four setters", 5 * adds, grid.hiddenDeltas - deltas);
			assertEquals("merged while hidden", 0, grid.mergedInvalidations - merged);
		}
		grid.setVisible(true);
		flush();
		if (GridViewport.ENABLED) {
			assertEquals("window patches on show", 1, grid.hiddenPatches - patches);
		}
		assertScrollBarsCurrent("after the show");
		assertSameAsFullRepaint("after the show");
	}

	@Test
	public void afterAddsArePaintedASetterRepaintsJustItsRow() {
		// once the adds are painted the scroll values are settled again: a setter on a row on screen
		// repaints that row, one off screen nothing (the row delta is unchanged)
		addRootsWithSetters(30);
		final GridItem shown = grid.getItem(6);
		final int paintsBefore = grid.rowDeltaPaints;
		assertBodyPaints("text on screen after painted adds", () -> shown.setText(1, "visible changed"), 1, 1);
		final GridItem far = grid.getItem(grid.getItemCount() - 5);
		final int skipsBefore = grid.rowDeltaSkips;
		assertBodyPaints("text of an added row off screen", () -> far.setText(1, "far changed"), 0, 1);
		if (GridViewport.ENABLED) {
			assertEquals("row deltas painted", 1, grid.rowDeltaPaints - paintsBefore);
			assertEquals("row deltas skipped", 1, grid.rowDeltaSkips - skipsBefore);
		}
	}

	/*
	 * Adds n roots at the end between two paints, each followed by setText, setBackground,
	 * setChecked and setHeight, and lets the paint they owe run. Returns the native work the Grid
	 * issued during the adds.
	 */
	private int addRootsWithSetters(final int n) {
		flush();
		final Color yellow = display.getSystemColor(SWT.COLOR_YELLOW);
		final int before = grid.nativeOps;
		for (int i = 0; i < n; i++) {
			final GridItem item = new GridItem(grid, SWT.NONE);
			item.setText(0, "added " + i);
			item.setBackground(1, yellow);
			item.setChecked(true);
			item.setHeight(18 + (i & 3));
		}
		final int ops = grid.nativeOps - before;
		flush();
		return ops;
	}

	/* applies the change; the viewport mode paints the body paintsIfDelta times, the original mode stockPaints */
	private void assertBodyPaints(final String step, final Runnable change, final int paintsIfDelta, final int stockPaints) {
		flush();
		final int before = grid.bodyPaintCount;
		change.run();
		grid.update();
		flush();
		assertEquals(step + ": body paints", GridViewport.ENABLED ? paintsIfDelta : stockPaints, grid.bodyPaintCount - before);
		assertSameAsFullRepaint(step);
	}

	/* the window after the change equals the window after a full repaint (screen to screen, no print) */
	private void assertSameAsFullRepaint(final String step) {
		final int[] incremental = windowPixels();
		grid.redraw();
		grid.update();
		flush();
		assertTrue(step + ": the window equals a full repaint", java.util.Arrays.equals(incremental, windowPixels()));
	}

	private int[] windowPixels() {
		final org.eclipse.swt.graphics.Rectangle area = grid.getClientArea();
		final org.eclipse.swt.graphics.Image shot = new org.eclipse.swt.graphics.Image(display, area.width, area.height);
		final org.eclipse.swt.graphics.GC gc = new org.eclipse.swt.graphics.GC(grid);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		final ImageData data = shot.getImageData();
		shot.dispose();
		final int[] all = new int[area.width * area.height];
		for (int y = 0; y < area.height; y++) {
			data.getPixels(0, y, area.width, all, y * area.width);
		}
		return all;
	}

	/* applies the change; the viewport mode paints the body paintsIfDelta times, the original mode stockPaints */
	private void assertDelta(final String step, final Runnable change, final int paintsIfDelta, final int stockPaints) {
		flush();
		final int before = grid.bodyPaintCount;
		final int deltas = grid.rowDeltaPaints + grid.rowDeltaSkips;
		change.run();
		grid.update();
		flush();
		assertEquals(step + ": body paints (window " + grid.getTopIndex() + ".." + grid.getBottomIndex() + ", row deltas painted "
				+ grid.rowDeltaPaints + " skipped " + grid.rowDeltaSkips + ")", GridViewport.ENABLED ? paintsIfDelta : stockPaints,
				grid.bodyPaintCount - before);
		if (GridViewport.ENABLED) {
			assertEquals(step + ": handled as a row delta", deltas + 1, grid.rowDeltaPaints + grid.rowDeltaSkips);
		}
		assertScreenIsPaint(step);
	}

	/* the scroll bars equal what a full recomputation gives (a delta must not leave them stale) */
	private void assertScrollBarsCurrent(final String step) {
		final String now = bars();
		grid.setScrollValuesObsolete();
		grid.update();
		flush();
		assertEquals(step + ": scroll bars", bars(), now);
	}

	private String bars() {
		final ScrollBar v = grid.getVerticalBar();
		final ScrollBar h = grid.getHorizontalBar();
		return v.isVisible() + " " + v.getSelection() + "/" + v.getMaximum() + "/" + v.getThumb() + " | " + h.isVisible()
				+ " " + h.getSelection() + "/" + h.getMaximum() + "/" + h.getThumb() + " | top " + grid.getTopIndex();
	}

	private void assertScreenIsPaint(final String step) {
		flush();
		final ImageData now = GridScrollPaint_Test.screen(grid);
		final ImageData painted = GridScrollPaint_Test.print(grid);
		if (!GridScrollPaint_Test.sameClientArea(grid, now, painted)) {
			GridViewportDifferential_Test.save(now, "rowdelta-now");
			GridViewportDifferential_Test.save(painted, "rowdelta-painted");
			throw new AssertionError(step + ": the window differs from the Grid's paint");
		}
	}

	/*
	 * Drains events until the Grid's paints stayed quiet for 200 ms. A plain readAndDispatch drain
	 * returns before GTK3's frame-clock paint, so the body paint counts and the screen were read
	 * before the paint (see GridTestUtil).
	 */
	private void flush() {
		GridTestUtil.awaitGridPaints(display, grid, 0);
	}
}
