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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Idle prefetch: rows below the viewport are populated before they are scrolled in. */
public class GridPrefetch_Test {

	private Display display;
	private Shell shell;
	private Grid grid;
	private final int[] setData = new int[100_000];

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL);
		grid.setBounds(0, 0, 400, 300);
		new GridColumn(grid, SWT.NONE).setWidth(200);
		grid.addListener(SWT.SetData, e -> {
			setData[e.index]++;
			((GridItem) e.item).setText("row " + e.index);
		});
		grid.setItemCount(setData.length);
		grid.setPrefetchRows(30);
		shell.setSize(420, 340);
		shell.open();
		flush();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void rowsBelowTheViewportArePopulatedWhileIdle() {
		final int bottom = grid.getBottomIndex();
		assertTrue("prefetched " + grid.prefetchedRows, grid.prefetchedRows >= 30);
		for (int r = bottom + 1; r <= bottom + 30; r++) {
			assertEquals("row " + r + " populated once", 1, setData[r]);
		}
		final int[] before = setData.clone();
		grid.setTopIndex(bottom + 1);
		grid.update();
		for (int r = bottom + 1; r <= bottom + 12; r++) {
			assertEquals("no SetData when row " + r + " scrolls in", before[r], setData[r]);
		}
	}

	@Test
	public void zeroDisablesPrefetch() {
		grid.setPrefetchRows(0);
		final int bottom = grid.getBottomIndex();
		grid.setTopIndex(5000);
		grid.update();
		flush();
		assertEquals(0, setData[5000 + 60]);
		assertTrue(bottom > 0);
	}

	@Test
	public void bothSidesShareTheRemainingResidencyCapacity() {
		prepareMiddleViewport();
		final GridItem retained = grid.getItem(100);
		final int before = grid.virtualMaterializedItemCount();
		final int bottom = grid.getBottomIndex();
		final int top = grid.getTopIndex();
		grid.setItemResidency(Grid.RESIDENCY_EVICT_COLD, before + 2);

		queuePrefetch(2);
		flush();

		assertEquals("one capacity shared by below and above rows", before + 2,
				grid.virtualMaterializedItemCount());
		assertEquals(1, setData[bottom + 1]);
		assertEquals(1, setData[bottom + 2]);
		assertEquals("no capacity remains for the upper side", 0, setData[top - 1]);
		assertSame(retained, grid.getItem(100));
	}

	@Test
	public void setDataMaterializationConsumesTheRemainingCapacity() {
		prepareMiddleViewport();
		final int before = grid.virtualMaterializedItemCount();
		final int first = grid.getBottomIndex() + 1;
		final GridItem[] added = new GridItem[1];
		grid.setItemResidency(Grid.RESIDENCY_EVICT_COLD, before + 2);
		grid.addListener(SWT.SetData, event -> {
			if (event.index == first) {
				added[0] = grid.getItem(190);
				added[0].setData("application identity");
				added[0].getText(0);
			}
		});

		queuePrefetch(4);
		flush();

		assertEquals("callback-created rows share the capacity", before + 2,
				grid.virtualMaterializedItemCount());
		assertEquals("prefetch stops after the callback consumes the last slot", 0, setData[first + 1]);
		assertSame(added[0], grid.getItem(190));
		assertEquals("application identity", added[0].getData());
	}

	@Test
	public void queuedPrefetchStopsAfterTheGridBecomesATree() {
		prepareMiddleViewport();
		final int before = grid.prefetchedRows;
		final GridItem root = grid.getItem(0);
		final GridItem[] child = new GridItem[1];
		grid.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				grid.removePaintListener(this);
				// Grid's earlier paint listener has queued prefetch, but it cannot run yet.
				child[0] = new GridItem(root, SWT.NONE);
			}
		});

		queuePrefetch(4);
		flush();

		assertEquals("flat prefetch does not run against the tree owner", before, grid.prefetchedRows);
		assertSame(child[0], root.getItem(0));
	}

	@Test
	public void setDataCanDisablePrefetch() {
		prepareMiddleViewport();
		final int first = grid.getBottomIndex() + 1;
		final int before = grid.prefetchedRows;
		grid.addListener(SWT.SetData, event -> {
			if (event.index == first) {
				grid.setPrefetchRows(0);
			}
		});

		queuePrefetch(4);
		flush();

		assertEquals("the disabling callback is the last prefetch", before + 1, grid.prefetchedRows);
		assertEquals(0, setData[first + 1]);
		assertEquals(0, grid.getPrefetchRows());
	}

	@Test
	public void setDataCanReplaceThePrefetchRequest() {
		prepareMiddleViewport();
		final int first = grid.getBottomIndex() + 1;
		final int before = grid.prefetchedRows;
		grid.addListener(SWT.SetData, event -> {
			if (event.index == first) {
				grid.setPrefetchRows(1);
			}
		});

		queuePrefetch(4);
		flush();

		assertEquals("the old request stops at the callback", before + 1, grid.prefetchedRows);
		assertEquals(0, setData[first + 1]);
		assertEquals(1, grid.getPrefetchRows());
	}

	@Test
	public void setDataCanDisposeTheGrid() {
		prepareMiddleViewport();
		final int first = grid.getBottomIndex() + 1;
		grid.addListener(SWT.SetData, event -> {
			if (event.index == first) {
				grid.dispose();
			}
		});

		queuePrefetch(4);
		flush();

		assertTrue(grid.isDisposed());
		assertEquals("no next-row callback after disposal", 0, setData[first + 1]);
	}

	@Test
	public void setDataCanChangeTheGridToATree() {
		prepareMiddleViewport();
		final int first = grid.getBottomIndex() + 1;
		final int before = grid.prefetchedRows;
		final GridItem[] child = new GridItem[1];
		grid.addListener(SWT.SetData, event -> {
			if (event.index == first) {
				child[0] = new GridItem(grid.getItem(0), SWT.NONE);
			}
		});

		queuePrefetch(4);
		flush();

		assertEquals("the old sparse owner is not used after the callback", before + 1, grid.prefetchedRows);
		assertSame(child[0], grid.getItem(0).getItem(0));
	}

	@Test
	public void setDataCanShrinkTheLogicalRows() {
		prepareMiddleViewport();
		final int first = grid.getBottomIndex() + 1;
		grid.addListener(SWT.SetData, event -> {
			if (event.index == first) {
				grid.setItemCount(first + 1);
			}
		});

		queuePrefetch(4);
		flush();

		assertEquals(first + 1, grid.getItemCount());
		assertEquals("no access beyond the callback's new extent", 0, setData[first + 1]);
	}

	@Test
	public void setDataCanMoveTheViewport() {
		prepareMiddleViewport();
		final int first = grid.getBottomIndex() + 1;
		grid.addListener(SWT.SetData, event -> {
			if (event.index == first) {
				grid.setTopIndex(150);
			}
		});

		queuePrefetch(4);
		flush();

		assertEquals(150, grid.getTopIndex());
		assertEquals("the old viewport is abandoned at the callback", 0, setData[first + 1]);
		assertEquals("the next paint serves the new viewport", 1, setData[grid.getBottomIndex() + 1]);
	}

	private void prepareMiddleViewport() {
		grid.setPrefetchRows(0);
		grid.setItemCount(200);
		grid.setTopIndex(100);
		grid.update();
		flush();
		assertEquals(100, grid.getTopIndex());
	}

	private void queuePrefetch(final int rows) {
		grid.setPrefetchRows(rows);
		final int before = grid.paintPasses;
		grid.redraw();
		grid.update();
		// Stop at the first paint, leaving its queued idle request to the caller.
		final long deadline = System.nanoTime() + 5_000_000_000L;
		while (grid.paintPasses == before && System.nanoTime() < deadline) {
			if (!display.readAndDispatch()) {
				Thread.yield();
			}
		}
		assertTrue("the paint queues the idle request", grid.paintPasses > before);
	}

	private void flush() {
		GridSwtScreenshotCapture.awaitStableCounter(display, () -> grid.paintPasses, 1,
				"prefetch Grid paint did not settle");
	}
}
