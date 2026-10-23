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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Brief rule 24, hidden viewports (user 2026-10-20: "if the viewport becomes visible from hidden it
 * should paint; while it is hidden the model may make changes and the viewport is dirty and renders
 * on becoming visible"). For each way a Grid is hidden (an unselected tab, its own visibility, an
 * empty size, scrolled out of a ScrolledComposite) the gate is:
 * <ul>
 * <li>hide, apply 10^4 model deltas (texts, roots and children added, items disposed, expand and
 * collapse, row heights; for a VIRTUAL Grid item counts and clears);</li>
 * <li>while hidden: no native work ({@link Grid#nativeOps}: invalidations, scroll bar updates,
 * measuring GCs), no paint, and for the VIRTUAL Grid no SetData request;</li>
 * <li>shown: exactly one paint pass, the window equals the Grid's paint, and the rows on screen
 * equal those of a Grid built with the same model that was never hidden (the differential proves
 * such a Grid equal to stock).</li>
 * </ul>
 * In the original mode the same scripts run and the native work stock does while hidden is logged.
 */
public class GridHiddenViewport_Test {
	private static final Logger LOG = Logger.getLogger("org.eclipse.nebula.widgets.grid");
	private static final int DELTAS = 10_000;

	private Display display;
	private Shell shell;
	private Shell referenceShell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(10, 10, 760, 560);
		referenceShell = new Shell(display);
		referenceShell.setBounds(20, 20, 760, 560);
	}

	@After
	public void tearDown() {
		shell.dispose();
		referenceShell.dispose();
	}

	@Test
	public void unselectedTab() {
		shell.setLayout(new FillLayout());
		final CTabFolder folder = new CTabFolder(shell, SWT.NONE);
		final CTabItem gridTab = new CTabItem(folder, SWT.NONE);
		gridTab.setText("grid");
		final Composite page = new Composite(folder, SWT.NONE);
		page.setLayout(new FillLayout());
		gridTab.setControl(page);
		final CTabItem otherTab = new CTabItem(folder, SWT.NONE);
		otherTab.setText("other");
		otherTab.setControl(new Text(folder, SWT.MULTI));
		folder.setSelection(gridTab);
		final Grid grid = treeGrid(page, SWT.NONE);
		gate("unselected tab", grid, g -> folder.setSelection(otherTab), g -> folder.setSelection(gridTab), this::treeDeltas);
	}

	@Test
	public void ownVisibility() {
		final Grid grid = treeGrid(shell, SWT.NONE);
		grid.setBounds(5, 5, 700, 480);
		gate("setVisible(false)", grid, g -> g.setVisible(false), g -> g.setVisible(true), this::treeDeltas);
	}

	@Test
	public void emptySize() {
		final Grid grid = treeGrid(shell, SWT.NONE);
		grid.setBounds(5, 5, 700, 480);
		gate("zero size", grid, g -> g.setSize(0, 0), g -> g.setSize(700, 480), this::treeDeltas);
	}

	@Test
	public void scrolledOutOfView() {
		final ScrolledComposite scrolled = new ScrolledComposite(shell, SWT.V_SCROLL);
		scrolled.setBounds(5, 5, 720, 500);
		final Composite content = new Composite(scrolled, SWT.NONE);
		content.setSize(700, 2_000);
		scrolled.setContent(content);
		final Grid grid = treeGrid(content, SWT.NONE);
		grid.setBounds(0, 0, 700, 480);
		gate("scrolled out of view", grid, g -> scrolled.setOrigin(0, 1_200), g -> scrolled.setOrigin(0, 0), this::treeDeltas);
	}

	@Test
	public void virtualGridAsksNoDataWhileHidden() {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 700, 480);
		columns(grid);
		final int[] asked = new int[1];
		grid.addListener(SWT.SetData, e -> {
			asked[0]++;
			final GridItem item = (GridItem) e.item;
			final int row = grid.indexOf(item);
			item.setText(0, "v" + row + "." + generation);
			item.setText(1, "w" + row);
		});
		grid.setItemCount(50_000);
		final int[] askedWhileHidden = new int[1];
		gate("virtual, setVisible(false)", grid, g -> {
			g.setVisible(false);
			askedWhileHidden[0] = asked[0];
		}, g -> {
			assertEquals("SetData requests while hidden", askedWhileHidden[0], asked[0]);
			g.setVisible(true);
		}, this::virtualDeltas);
	}

	/* the text generation the virtual deltas change (every clear asks for the new texts) */
	private int generation;

	private void gate(final String how, final Grid grid, final Consumer<Grid> hide, final Consumer<Grid> show,
			final java.util.function.BiConsumer<Grid, Random> deltas) {
		shell.open();
		flush();
		grid.setTopIndex(400);
		flush();
		hide.accept(grid);
		flush();
		final int nativeBefore = grid.nativeOps;
		final int paintsBefore = grid.paintPasses;
		final int patchesBefore = grid.hiddenPatches;
		final long started = System.nanoTime();
		deltas.accept(grid, new Random(0x41DL));
		final long micros = (System.nanoTime() - started) / 1_000;
		flush();
		final int nativeWhileHidden = grid.nativeOps - nativeBefore;
		final int paintsWhileHidden = grid.paintPasses - paintsBefore;
		show.accept(grid);
		flush();
		final int paintsOnShow = grid.paintPasses - paintsBefore - paintsWhileHidden;
		LOG.info("GridHiddenViewport_Test: " + how + " (" + (GridViewport.ENABLED ? "viewport" : "original") + "): " + DELTAS
				+ " deltas in " + micros + " us; while hidden native ops " + nativeWhileHidden + ", paints " + paintsWhileHidden
				+ ", coalesced " + grid.hiddenDeltas + "; on show paint passes " + paintsOnShow + ", window patches "
				+ (grid.hiddenPatches - patchesBefore));
		if (GridViewport.ENABLED) {
			assertEquals(how + ": native ops while hidden", 0, nativeWhileHidden);
			assertEquals(how + ": paints while hidden", 0, paintsWhileHidden);
			assertEquals(how + ": paint passes on show", 1, paintsOnShow);
			assertEquals(how + ": window patches on show", 1, grid.hiddenPatches - patchesBefore);
		} else {
			assertEquals(how + ": paints while hidden", 0, paintsWhileHidden);
			assertTrue(how + ": painted on show", paintsOnShow >= 1);
		}
		assertScreenIsPaint(grid, how);
		assertEqualsNeverHidden(grid, how);
	}

	private Grid treeGrid(final Composite parent, final int style) {
		final Grid grid = new Grid(parent, style | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		columns(grid);
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

	private static void columns(final Grid grid) {
		for (int c = 0; c < 2; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setWidth(200);
			column.setTree(c == 0 && (grid.getStyle() & SWT.VIRTUAL) == 0);
		}
	}

	private void treeDeltas(final Grid grid, final Random random) {
		for (int step = 0; step < DELTAS; step++) {
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

	private void virtualDeltas(final Grid grid, final Random random) {
		for (int step = 0; step < DELTAS; step++) {
			final int kind = random.nextInt(100);
			final int count = grid.getItemCount();
			if (kind < 70) {
				grid.clear(random.nextInt(count), false);
			} else if (kind < 85) {
				grid.setItemCount(count + 1 + random.nextInt(10));
			} else if (kind < 99) {
				grid.setItemCount(Math.max(1_000, count - 1 - random.nextInt(10)));
			} else {
				generation++;
				grid.clearAll(false);
			}
		}
	}

	/* the rows on screen and the pixels equal those of a Grid with the same model that was never hidden */
	private void assertEqualsNeverHidden(final Grid grid, final String how) {
		referenceShell.setLayout(null);
		final Grid reference = new Grid(referenceShell, grid.getStyle() & (SWT.VIRTUAL | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL));
		reference.setSize(grid.getSize());
		reference.setHeaderVisible(grid.getHeaderVisible());
		reference.setLinesVisible(grid.getLinesVisible());
		columns(reference);
		if ((grid.getStyle() & SWT.VIRTUAL) != 0) {
			reference.addListener(SWT.SetData, e -> {
				final GridItem item = (GridItem) e.item;
				final int row = reference.indexOf(item);
				item.setText(0, "v" + row + "." + generation);
				item.setText(1, "w" + row);
			});
			reference.setItemCount(grid.getItemCount());
		} else {
			final Map<GridItem, GridItem> copies = new IdentityHashMap<>();
			for (final GridItem item : grid.getItems()) {
				final GridItem parent = item.getParentItem();
				final GridItem copy = parent == null ? new GridItem(reference, SWT.NONE) : new GridItem(copies.get(parent), SWT.NONE);
				copy.setText(0, item.getText(0));
				copy.setText(1, item.getText(1));
				if (item.getHeight() != grid.getItemHeight()) {
					copy.setHeight(item.getHeight());
				}
				copies.put(item, copy);
			}
			for (final GridItem item : grid.getItems()) {
				copies.get(item).setExpanded(item.isExpanded());
			}
		}
		referenceShell.open();
		flush();
		reference.setTopIndex(grid.getTopIndex());
		flush();
		assertEquals(how + ": top row", grid.getTopIndex(), reference.getTopIndex());
		assertEquals(how + ": bottom row", grid.getBottomIndex(), reference.getBottomIndex());
		for (int row = grid.getTopIndex(); row <= grid.getBottomIndex(); row++) {
			assertEquals(how + ": row " + row, grid.getItem(row).getText(0) + "|" + grid.getItem(row).getText(1),
					reference.getItem(row).getText(0) + "|" + reference.getItem(row).getText(1));
		}
		final ImageData shown = GridScrollPaint_Test.print(grid);
		final ImageData fresh = GridScrollPaint_Test.print(reference);
		if (!GridScrollPaint_Test.sameClientArea(grid, shown, fresh)) {
			GridViewportDifferential_Test.save(shown, "hidden-shown");
			GridViewportDifferential_Test.save(fresh, "hidden-never-hidden");
			throw new AssertionError(how + ": the shown Grid paints unlike a Grid never hidden ("
					+ GridViewportDifferential_Test.difference(fresh, shown) + ")");
		}
		reference.dispose();
	}

	private void assertScreenIsPaint(final Grid grid, final String how) {
		final ImageData now = GridScrollPaint_Test.screen(grid);
		final ImageData painted = GridScrollPaint_Test.print(grid);
		if (!GridScrollPaint_Test.sameClientArea(grid, now, painted)) {
			GridViewportDifferential_Test.save(now, "hidden-now");
			GridViewportDifferential_Test.save(painted, "hidden-painted");
			throw new AssertionError(how + ": the window after the show differs from the Grid's paint ("
					+ GridViewportDifferential_Test.difference(painted, now) + ")");
		}
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain paints and timers
		}
	}
}
