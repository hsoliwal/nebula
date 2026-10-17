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

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import org.eclipse.nebula.widgets.grid.internal.ScrollBarProxyAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Pending scroll requests must survive first-paint timing without crossing stale callback state. */
public class GridTopIndexStartup_Test {
	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
	}

	@After
	public void tearDown() {
		if (!shell.isDisposed()) shell.dispose();
		while (display.readAndDispatch()) { }
	}

	private static boolean fixes() { return GridViewport.UPSTREAM_FIXES; }

	private Grid flat(final int count) {
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setSize(200, 200);
		new GridColumn(grid, SWT.NONE).setWidth(100);
		for (int row = 0; row < count; row++) {
			new GridItem(grid, SWT.NONE).setText("row " + row);
		}
		return grid;
	}

	@Test
	public void firstTopRequestUsesCurrentRowsWithoutPaintingOrSelectionEvents() {
		final Grid grid = flat(40);
		final int[] selections = { 0 };
		grid.addListener(SWT.Selection, event -> selections[0]++);
		grid.getVerticalBar().addListener(SWT.Selection, event -> selections[0]++);
		assertFalse(grid.getVerticalBar().getVisible());
		final GridItem target = grid.getItem(12);
		grid.setTopIndex(12);
		assertEquals(fixes() ? 12 : 0, grid.getTopIndex());
		assertSame(target, grid.getItem(12));
		assertEquals(0, selections[0]);
		assertFalse("no paint or shell open is a prerequisite", shell.getVisible());
	}

	@Test
	public void collapsedTreeSelectionRevealsTheRequestedRootBeforeFirstPaint() {
		final Grid grid = flat(0);
		for (int row = 0; row < 20; row++) {
			final GridItem root = new GridItem(grid, SWT.NONE);
			for (int child = 0; child < 3; child++) new GridItem(root, SWT.NONE);
		}
		grid.setSelection(new int[] { 4, 8, 24 });
		grid.setTopIndex(12);
		assertEquals(fixes() ? 12 : 0, grid.getTopIndex());
		grid.showSelection();
		assertEquals(fixes() ? 4 : 0, grid.getTopIndex());
		assertArrayEquals(new int[] { 4, 8, 24 }, grid.getSelectionIndices());
	}

	@Test
	public void pendingGrowthRefreshesTheRangeEvenWhenTheBarIsAlreadyVisible() throws Exception {
		final Grid grid = flat(40);
		openAndSettle(grid);
		final int oldLimit = grid.getVerticalBar().getMaximum() - grid.getVerticalBar().getThumb();
		grid.setItemCount(200);
		grid.setTopIndex(150);
		assertEquals(fixes() ? 150 : oldLimit, grid.getTopIndex());
	}

	@Test
	public void pendingResizeCanShowTheBarWithoutASecondPaint() throws Exception {
		final Grid grid = flat(40);
		grid.setSize(200, 1200);
		shell.setSize(240, 1250);
		shell.open();
		settle(grid);
		assertFalse(grid.getVerticalBar().getVisible());
		grid.setSize(200, 200);
		grid.setTopIndex(20);
		assertEquals(fixes() ? 20 : 0, grid.getTopIndex());
	}

	@Test
	public void latestResizeCallbackRequestWinsAfterGeometrySettles() {
		final Grid grid = flat(60);
		final int[] resizes = { 0 };
		grid.addListener(SWT.Resize, event -> {
			resizes[0]++;
			grid.setTopIndex(20);
		});
		grid.setTopIndex(12);
		assertEquals(fixes() ? 20 : 0, grid.getTopIndex());
		assertEquals(fixes(), resizes[0] > 0);
		assertTrue("native geometry settles without recursive resize loops", resizes[0] < 8);
	}

	@Test
	public void disposedTargetIsNotReplacedByTheNextRowAtItsAddress() {
		final Grid grid = flat(40);
		final GridItem target = grid.getItem(12);
		final GridItem following = grid.getItem(13);
		grid.getVerticalBar().addListener(SWT.Show, event -> target.dispose());
		grid.setTopIndex(12);
		assertEquals(fixes(), target.isDisposed());
		assertEquals(0, grid.getTopIndex());
		assertSame(fixes() ? following : target, grid.getItem(12));
	}

	@Test
	public void movedTargetDoesNotScrollAnUnrelatedReplacementRow() {
		final Grid grid = flat(40);
		final GridItem target = grid.getItem(12);
		grid.getVerticalBar().addListener(SWT.Show, event -> new GridItem(grid, SWT.NONE, 0));
		grid.setTopIndex(12);
		assertFalse(target.isDisposed());
		assertSame(target, grid.getItem(fixes() ? 13 : 12));
		assertEquals(0, grid.getTopIndex());
	}

	@Test
	public void shrinkingRowsDuringShowRecomputesVisibilityAndCancelsRemovedTarget() {
		final Grid grid = flat(40);
		grid.getVerticalBar().addListener(SWT.Show, event -> grid.setItemCount(5));
		grid.setTopIndex(12);
		assertEquals(fixes() ? 5 : 40, grid.getItemCount());
		assertFalse(grid.getVerticalBar().getVisible());
		assertEquals(0, grid.getTopIndex());
	}

	@Test
	public void showListenerCanDisposeTheOwnerWithoutStaleGeometryAccess() {
		final Grid grid = flat(40);
		grid.getVerticalBar().addListener(SWT.Show, event -> grid.dispose());
		grid.setTopIndex(12);
		assertEquals(fixes(), grid.isDisposed());
	}

	@Test
	public void resizeListenerCanDisposeTheOwnerWithoutStaleGeometryAccess() {
		final Grid grid = flat(40);
		grid.addListener(SWT.Resize, event -> grid.dispose());
		grid.setTopIndex(12);
		assertEquals(fixes(), grid.isDisposed());
	}

	@Test
	public void showListenerCanDisposeOnlyTheNativeBarWithoutStaleGeometryAccess() {
		final Grid grid = flat(40);
		final ScrollBar bar = grid.getVerticalBar();
		bar.addListener(SWT.Show, event -> bar.dispose());
		grid.setTopIndex(12);
		assertEquals(fixes(), bar.isDisposed());
		assertFalse(grid.isDisposed());
		grid.setTopIndex(20);
		assertEquals("later requests cannot touch the retired native adapter", 0, grid.getTopIndex());
	}

	@Test
	public void showResizeUsesTheNewClientAreaForBothBarsAndTheRowRange() {
		final Grid grid = flat(60);
		grid.getColumn(0).setWidth(400);
		grid.getVerticalBar().addListener(SWT.Show, event -> grid.setSize(500, 500));
		grid.setTopIndex(20);
		assertEquals(fixes() ? 500 : 200, grid.getSize().x);
		assertEquals(fixes() ? 500 : 200, grid.getSize().y);
		assertFalse("the widened client fits the column", grid.getHorizontalBar().getVisible());
		assertEquals(fixes() ? 20 : 0, grid.getTopIndex());
		if (fixes()) {
			assertEquals((grid.getClientArea().height + 1) / (grid.getItemHeight() + 1),
					grid.getVerticalBar().getThumb());
		}
	}

	@Test
	public void immediateResizeQueryClampsAnEarlierStartupScrollWhenAllRowsFit() {
		final Grid grid = flat(20);
		grid.setTopIndex(4);
		assertEquals(fixes() ? 4 : 0, grid.getTopIndex());
		grid.setSize(500, 700);
		assertEquals(0, grid.getTopIndex());
		assertFalse(grid.getVerticalBar().getVisible());
	}

	@Test
	public void retiringNativeBarPreservesAListenerInstalledExternalProxy() {
		final Grid grid = flat(60);
		final Canvas external = new Canvas(shell, SWT.V_SCROLL);
		external.setSize(40, 200);
		final ScrollBarProxyAdapter replacement = new ScrollBarProxyAdapter(external.getVerticalBar());
		final ScrollBar nativeBar = grid.getVerticalBar();
		nativeBar.addListener(SWT.Show, event -> {
			nativeBar.dispose();
			grid.setlVerticalScrollBarProxy(replacement);
		});
		grid.setTopIndex(12);
		if (fixes()) assertSame(replacement, grid.getVerticalScrollBarProxy());
		grid.setTopIndex(20);
		assertEquals(fixes() ? 20 : 0, grid.getTopIndex());
		if (fixes()) assertSame(replacement, grid.getVerticalScrollBarProxy());
	}

	@Test
	public void pendingViewportNotificationCannotEraseNewRowInvalidationDuringPaint() throws Exception {
		final Grid grid = flat(40);
		final boolean[] changed = { false };
		grid.getVerticalBar().addListener(SWT.Show, event -> grid.setTopIndex(12));
		grid.addViewportChangeListener(() -> {
			if (!changed[0] && grid.getTopIndex() == 12) {
				changed[0] = true;
				grid.setItemCount(200);
			}
		});
		openAndSettle(grid);
		assertTrue("the viewport callback must execute", changed[0]);
		assertEquals(200, grid.getItemCount());
		assertEquals("paint retains the newer scrollbar invalidation", 200, grid.getVerticalBar().getMaximum());
		assertEquals(12, grid.getTopIndex());
	}

	@Test
	public void unchangedPrepaintRequestsReuseSettledScrollbarGeometry() {
		final Grid grid = new Grid(shell, SWT.NONE);
		grid.setSize(200, 200);
		new GridColumn(grid, SWT.NONE).setWidth(100);
		for (int row = 0; row < 60; row++) new GridItem(grid, SWT.NONE);
		final Canvas external = new Canvas(shell, SWT.V_SCROLL);
		external.setSize(40, 200);
		final int[] rangeCalls = { 0 };
		grid.setlVerticalScrollBarProxy(new ScrollBarProxyAdapter(external.getVerticalBar()) {
			@Override
			public void setValues(int selection, int minimum, int maximum, int thumb, int increment, int pageIncrement) {
				rangeCalls[0]++;
				super.setValues(selection, minimum, maximum, thumb, increment, pageIncrement);
			}
		});
		grid.setTopIndex(10);
		final int initial = rangeCalls[0];
		grid.setTopIndex(11);
		grid.setTopIndex(12);
		assertEquals(12, grid.getTopIndex());
		assertEquals("unchanged requests do not reconfigure ranges", initial, rangeCalls[0]);
		if (fixes()) assertTrue(initial > 0);
	}

	@Test
	public void nestedScrollbarFreezeRetainsTheLatestRequestUntilFinalThaw() {
		final Grid grid = flat(60);
		grid.freezePlanes(Grid.PLANE_SCROLLBARS);
		grid.freezePlanes(Grid.PLANE_SCROLLBARS);
		grid.setTopIndex(12);
		grid.setTopIndex(20);
		assertFalse(grid.getVerticalBar().getVisible());
		assertEquals(0, grid.getTopIndex());
		grid.thawPlanes(Grid.PLANE_SCROLLBARS);
		assertFalse(grid.getVerticalBar().getVisible());
		assertEquals(0, grid.getTopIndex());
		grid.thawPlanes(Grid.PLANE_SCROLLBARS);
		assertEquals(fixes() ? 20 : 0, grid.getTopIndex());
	}

	@Test
	public void freezeDuringResizeDefersTheRequestUntilTheListenerThaws() {
		final Grid grid = flat(60);
		final int[] freezes = { 0 };
		grid.addListener(SWT.Resize, event -> {
			if (freezes[0]++ == 0) grid.freezePlanes(Grid.PLANE_SCROLLBARS);
		});
		grid.setTopIndex(12);
		assertEquals(0, grid.getTopIndex());
		if (fixes()) {
			grid.thawPlanes(Grid.PLANE_SCROLLBARS);
			assertEquals(12, grid.getTopIndex());
		} else {
			assertEquals(0, freezes[0]);
		}
	}

	@Test
	public void hugeVirtualStartupScrollKeepsFacadeCountBounded() {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setSize(200, 200);
		new GridColumn(grid, SWT.NONE).setWidth(100);
		final int[] dataCalls = { 0 };
		grid.addListener(SWT.SetData, event -> dataCalls[0]++);
		grid.setItemCount(10_000_000);
		assertEquals(0, grid.virtualMaterializedItemCount());
		grid.setTopIndex(9_000_000);
		assertEquals(fixes() ? 9_000_000 : 0, grid.getTopIndex());
		assertEquals(10_000_000, grid.getItemCount());
		assertTrue("10-million logical rows retain at most the target and existing extent endpoints",
				grid.virtualMaterializedItemCount() <= 3);
		final GridItem target = grid.getItem(9_000_000);
		grid.setTopIndex(9_000_000);
		assertSame(target, grid.getItem(9_000_000));
		assertEquals("geometry does not invoke data callbacks", 0, dataCalls[0]);
	}

	@Test
	public void firstPaintMatchesTheSameScrollAfterNativeGeometryHasSettled() throws Exception {
		if ("gtk".equals(SWT.getPlatform())) {
			assertEquals("full native pixel proof requires deterministic GTK scrollbars", "0",
					System.getenv("GTK_OVERLAY_SCROLLING"));
		}
		final Grid grid = flat(100);
		grid.setBounds(0, 0, 390, 280);
		grid.getColumn(0).setWidth(360);
		grid.setHeaderVisible(true);
		grid.getColumn(0).setText("Startup viewport - stable row addresses");
		for (int row = 0; row < 100; row++) grid.getItem(row).setText("Viewport row " + row + " - logical scroll request");
		grid.setTopIndex(25);
		shell.setBounds(50, 50, 430, 340);
		shell.open();
		settle(grid);
		final String configured = System.getProperty("nebula.topindex.evidence.dir");
		final Path evidence = configured == null ? Files.createTempDirectory("nebula-top-index-") : Path.of(configured);
		Files.createDirectories(evidence);
		final Path firstPath = evidence.resolve("top-index-first-paint.png");
		final Path settledPath = evidence.resolve("top-index-settled-request.png");
		assertEquals(GridSwtScreenshotCapture.Method.DISPLAY_COPY_AREA,
				GridSwtScreenshotCapture.captureScreenControl(grid, firstPath).method());
		grid.setTopIndex(fixes() ? 25 : 0);
		settle(grid);
		assertEquals(GridSwtScreenshotCapture.Method.DISPLAY_COPY_AREA,
				GridSwtScreenshotCapture.captureScreenControl(grid, settledPath).method());
		final ImageData first = new ImageLoader().load(firstPath.toString())[0];
		final ImageData settled = new ImageLoader().load(settledPath.toString())[0];
		assertEquals(settled.width, first.width);
		assertEquals(settled.height, first.height);
		assertTrue(first.width > 300 && first.height > 200);
		final HashSet<Integer> colors = new HashSet<>();
		long differences = 0;
		for (int y = 0; y < first.height; y++) {
			for (int x = 0; x < first.width; x++) {
				final int pixel = first.getPixel(x, y);
				colors.add(pixel);
				if (pixel != settled.getPixel(x, y)) differences++;
			}
		}
		assertTrue("native capture cannot be blank or flat", colors.size() > 4);
		assertEquals("native startup scroll pixel mismatch", 0, differences);
	}

	private void openAndSettle(final Grid grid) throws Exception {
		shell.setBounds(40, 40, 450, 350);
		shell.open();
		settle(grid);
	}

	private void settle(final Grid grid) throws InterruptedException {
		grid.redraw();
		for (int round = 0; round < 8; round++) {
			while (display.readAndDispatch()) { }
			grid.update();
			display.update();
			Thread.sleep(10);
		}
	}
}
