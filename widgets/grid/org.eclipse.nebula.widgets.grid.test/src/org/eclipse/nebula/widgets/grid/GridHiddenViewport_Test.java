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
import static org.junit.Assume.assumeTrue;

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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
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
 * <li>shown: exactly one paint pass of the Grid's own and one window patch, the window equals the
 * Grid's paint, and the rows on screen equal those of a Grid built with the same model that was
 * never hidden (the differential proves such a Grid equal to stock).</li>
 * </ul>
 * In the original mode the same scripts run and the native work stock does while hidden is logged.
 * <p>
 * The pass count measures the Grid's own work. Every pass on show counts as a Grid pass, except a
 * pass that is wholly contained in a parent's own invalidation. Such a parent-originated pass must
 * meet all three conditions:
 * </p>
 * <ol>
 * <li>The Grid issued no native work since its previous pass, and the pass applied no window
 * patch.</li>
 * <li>Its clip lies within the strip of the Grid's client area that the parent's own redraw covers
 * on this platform. The strip is computed from the parent's geometry.</li>
 * <li>The parent painted its own area outside the Grid since the Grid's previous pass. So there is
 * at most one such pass per parent redraw.</li>
 * </ol>
 * <p>
 * Any other extra pass still fails: native work after the show, a patch in a second pass, a clip
 * outside the strip, or a strip expose that the parent did not paint. The message names every
 * pass. Two sets of tests check this:
 * </p>
 * <ul>
 * <li>The {@code unselectedTabCounts...} tests inject extra work after the show on a real
 * CTabFolder and require that the count fails. Some of them first repaint the folder's tab area,
 * so that (c) holds. One fails only (b). Where the strip is not empty (GTK3), one fails only (a)
 * and one fails only (c).</li>
 * <li>The {@code showPasses...} tests drive the classification with synthetic paint events and
 * the Grid's counters moved by hand, on every platform. Each case fails exactly one condition,
 * including a patch applied in a second pass, and one case meets all three.</li>
 * </ul>
 * <p>
 * <b>Platform note, GTK3: parent-originated exposes.</b> {@code CTabFolder.setSelection} defers
 * its tab-strip redraw: {@code showItem} calls {@code updateFolder(REDRAW_TABS)}, which runs
 * {@code redrawTabs()} from {@code asyncExec}. {@code redrawTabs()} invalidates
 * {@code redraw(0, 0, size.x, -trim.y - marginHeight + 1, false)} (stock SWT 3.135
 * {@code CTabFolder.java:2268}). That rectangle is one row deeper than the tab strip, so its last
 * row is the first row of the client area, where the selected page and its Grid lie.
 * </p>
 * <ul>
 * <li>On win32 a redraw with {@code all == false} keeps child windows out: SWT sets
 * {@code RDW_ALLCHILDREN} only for {@code all}. The strip is empty there, and the Grid paints once.</li>
 * <li>On GTK3 SWT's child {@code GdkWindow}s are client-side. {@code Control.redrawWidget} passes
 * the rectangle to {@code gdk_window_invalidate_rect} (stock {@code Control.java:4721-4740}). GDK
 * adds it to the native parent's update area, and GTK renders every widget that intersects that
 * area, children included.</li>
 * </ul>
 * <p>
 * So on GTK3 the Grid gets a second paint pass of one row ({@code 0,0 Wx1}). It issues no native
 * work and applies no patch in that pass. It cannot skip the pass: GTK3 starts the invalidated
 * region blank, and the row must be painted again. A plain {@code Canvas} in the same page gets the
 * same second paint, and a page shown without {@code CTabFolder.setSelection} paints once. The
 * contract counts only Grid-originated work, so this pass is the parent's, not the Grid's. On
 * other platforms (cocoa and GTK4 are not measured) the strip is empty, and any extra pass fails.
 * </p>
 */
public class GridHiddenViewport_Test {
	private static final Logger LOG = Logger.getLogger("org.eclipse.nebula.widgets.grid");
	private static final int DELTAS = 10_000;
	/*
	 * Whether a parent's redraw with all == false reaches child controls (see the class comment):
	 * GTK3, whose child GdkWindows are client-side. Not win32. Cocoa and GTK4 are not measured, and
	 * there the strip stays empty, so the count stays exact.
	 */
	private static final boolean PARENT_REDRAW_REACHES_CHILDREN = "gtk".equals(SWT.getPlatform())
			&& !"1".equals(System.getenv("SWT_GTK4"));

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
		new TabCase().gate("unselected tab", null);
	}

	/*
	 * The parent allowance admits a parent's own expose and nothing else. Each test below injects
	 * extra work after the show has settled. The Grid pass count must fail on it. A test "with a tab
	 * paint" first repaints the folder's tab area in the same turn (it ends at the client area, so it
	 * exposes no part of the Grid) and requires that the injected pass followed that paint. So
	 * condition (c) holds there, and only the other conditions can reject the pass. The showPasses...
	 * tests below check each condition alone with synthetic events, on every platform.
	 */

	@Test
	public void unselectedTabCountsAWholeGridRedrawAfterTheShow() {
		assertGridPassCountFails("a whole Grid redraw", false, (tab, row) -> tab.grid.redraw());
	}

	@Test
	public void unselectedTabCountsAWholeGridRedrawWithATabPaint() {
		// (c) holds: a Grid-originated whole repaint in the frame of a folder tab paint is still counted, by (a) and (b)
		assertGridPassCountFails("a whole Grid redraw with a tab paint", true, (tab, row) -> tab.grid.redraw());
	}

	@Test
	public void unselectedTabCountsAGridRowRedrawOutsideTheStrip() {
		assertGridPassCountFails("a Grid redraw of a row outside the strip", false, (tab, row) -> {
			final Rectangle client = tab.grid.getClientArea();
			tab.grid.redraw(0, client.height / 2, client.width, tab.grid.getItemHeight(), false);
		});
	}

	@Test
	public void unselectedTabCountsAGridRedrawInsideTheStrip() {
		// (b) and (c) hold where the strip is not empty (GTK3): only the Grid's native work (a) rejects the pass
		assertGridPassCountFails("a Grid redraw of the strip with a tab paint", true,
				(tab, row) -> tab.grid.redraw(row.x, row.y, row.width, row.height, false));
	}

	@Test
	public void unselectedTabCountsAParentExposeOutsideTheStrip() {
		// (a) and (c) hold: no native work of the Grid, a tab paint; only the clip outside the strip (b) rejects the pass
		assertGridPassCountFails("a page redraw of a Grid row outside the strip with a tab paint", true, (tab, row) -> {
			final Rectangle client = tab.grid.getClientArea();
			final Rectangle outside = display.map(tab.grid, tab.page,
					new Rectangle(0, client.height / 2, client.width, tab.grid.getItemHeight()));
			tab.page.redraw(outside.x, outside.y, outside.width, outside.height, true);
		});
	}

	@Test
	public void unselectedTabCountsAStripExposeTheFolderDidNotPaint() {
		// (a) and, where the strip is not empty (GTK3), (b) hold; only the missing folder tab paint (c) rejects the pass
		assertGridPassCountFails("a page redraw of the strip", false, (tab, row) -> {
			final Rectangle inPage = display.map(tab.grid, tab.page, row);
			tab.page.redraw(inPage.x, inPage.y, inPage.width, inPage.height, true);
		});
	}

	/* the allowance is one pass per parent redraw, not one pass in all */
	@Test
	public void unselectedTabAllowsOneParentPassPerTabStripRedraw() {
		final TabCase tab = new TabCase();
		tab.gate("unselected tab, then two tab-strip redraws", g -> {
			// CTabItem.setText defers redrawTabs() as setSelection does: updateFolder(UPDATE_TAB_HEIGHT | REDRAW_TABS)
			tab.otherTab.setText("other 1");
			flush(g, g.paintPasses);
			tab.otherTab.setText("other 2");
			flush(g, g.paintPasses);
		});
	}

	/* a Grid in the page of a CTabFolder; the gate hides it behind the other tab and shows it with setSelection */
	private final class TabCase {
		final CTabFolder folder;
		final CTabItem gridTab;
		final CTabItem otherTab;
		final Composite page;
		final Grid grid;
		final TabStripExpose expose;

		TabCase() {
			shell.setLayout(new FillLayout());
			folder = new CTabFolder(shell, SWT.NONE);
			gridTab = new CTabItem(folder, SWT.NONE);
			gridTab.setText("grid");
			page = new Composite(folder, SWT.NONE);
			page.setLayout(new FillLayout());
			gridTab.setControl(page);
			otherTab = new CTabItem(folder, SWT.NONE);
			otherTab.setText("other");
			otherTab.setControl(new Text(folder, SWT.MULTI));
			folder.setSelection(gridTab);
			grid = treeGrid(page, SWT.NONE);
			expose = new TabStripExpose(folder);
		}

		void gate(final String how, final Consumer<Grid> afterShow) {
			GridHiddenViewport_Test.this.gate(how, grid, g -> folder.setSelection(otherTab), g -> folder.setSelection(gridTab),
					GridHiddenViewport_Test.this::treeDeltas, expose, afterShow);
		}
	}

	/* extra work after the show; row is the strip, or where the strip is empty the Grid's first client row */
	private interface Injection {
		void inject(TabCase tab, Rectangle row);
	}

	/*
	 * The injection after the show must fail the Grid pass count (a viewport-mode contract). With
	 * withTabPaint the folder first repaints its tab area, which ends at the client area and so
	 * exposes no part of the Grid. update() delivers that paint before the injection, on every
	 * platform. The injected pass must then have followed a folder tab paint: condition (c) holds.
	 */
	private void assertGridPassCountFails(final String what, final boolean withTabPaint, final Injection injection) {
		assumeTrue("the exact paint pass count is a viewport-mode contract", GridViewport.ENABLED);
		final TabCase tab = new TabCase();
		final String how = "unselected tab, then " + what;
		try {
			tab.gate(how, g -> {
				final Rectangle strip = tab.expose.strip(g);
				final Rectangle row = strip.isEmpty() ? new Rectangle(0, 0, g.getClientArea().width, 1) : strip;
				if (withTabPaint) {
					tab.folder.redraw(0, 0, tab.folder.getSize().x, tab.folder.getClientArea().y, false);
					tab.folder.update();
				}
				injection.inject(tab, row);
				// the injected expose paints the Grid on every platform: wait for that pass
				flush(g, g.paintPasses + 1);
			});
		} catch (final AssertionError expected) {
			final String message = String.valueOf(expected.getMessage());
			assertTrue(how + ": failed on another check than the Grid pass count: " + message,
					message.contains("Grid paint passes on show"));
			if (withTabPaint) {
				assertTrue(how + ": the injected pass did not follow a folder tab paint, so condition (c) did not hold: " + message,
						passesOnShow.lastParentPaints >= 1);
			}
			LOG.info("GridHiddenViewport_Test: " + how + ": the Grid pass count failed as required: " + message);
			return;
		}
		fail(how + ": the Grid pass count accepted the extra work");
	}

	/*
	 * ShowPasses on its own, on every platform: synthetic paint events, and the Grid's native work and
	 * patch counters moved by hand. The parent's strip is 0,0 100x1, and a parent paint above y = 10
	 * covers the parent's own area. Each case first gives the Grid its own pass, which applies the
	 * patch. One case meets all three conditions; each other case fails exactly one of them.
	 */

	@Test
	public void showPassesExcludeAPassThatMeetsAllThreeConditions() {
		final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		final ShowPasses passes = syntheticShow(grid);
		passes.parentPainted(syntheticPaint(0, 0, 200, 10));
		passes.handleEvent(syntheticPaint(0, 0, 100, 1));
		assertSyntheticPasses("all three conditions", passes, 1, 1);
	}

	@Test
	public void showPassesCountAPassAfterNativeWork() {
		// fails (a) only: native work of the Grid since its previous pass
		final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		final ShowPasses passes = syntheticShow(grid);
		passes.parentPainted(syntheticPaint(0, 0, 200, 10));
		grid.nativeOps++;
		passes.handleEvent(syntheticPaint(0, 0, 100, 1));
		assertSyntheticPasses("native work before the pass", passes, 2, 0);
	}

	@Test
	public void showPassesCountAPassThatAppliesAPatch() {
		// fails (a) only: a window patch applied in a second pass
		final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		final ShowPasses passes = syntheticShow(grid);
		passes.parentPainted(syntheticPaint(0, 0, 200, 10));
		grid.hiddenPatches++;
		passes.handleEvent(syntheticPaint(0, 0, 100, 1));
		assertSyntheticPasses("a patch in the second pass", passes, 2, 0);
	}

	@Test
	public void showPassesCountAPassReachingOutsideTheStrip() {
		// fails (b) only: the clip covers the strip and one row more
		final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		final ShowPasses passes = syntheticShow(grid);
		passes.parentPainted(syntheticPaint(0, 0, 200, 10));
		passes.handleEvent(syntheticPaint(0, 0, 100, 2));
		assertSyntheticPasses("a clip reaching outside the strip", passes, 2, 0);
	}

	@Test
	public void showPassesCountAPassWithoutAParentPaint() {
		// fails (c) only: the parent painted only inside the Grid's area, not its own
		final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		final ShowPasses passes = syntheticShow(grid);
		passes.parentPainted(syntheticPaint(0, 20, 100, 1));
		passes.handleEvent(syntheticPaint(0, 0, 100, 1));
		assertSyntheticPasses("no parent paint of its own area", passes, 2, 0);
	}

	@Test
	public void showPassesAllowOneParentPassPerParentPaint() {
		// two strip passes after one parent paint: the second fails (c)
		final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		final ShowPasses passes = syntheticShow(grid);
		passes.parentPainted(syntheticPaint(0, 0, 200, 10));
		passes.handleEvent(syntheticPaint(0, 0, 100, 1));
		passes.handleEvent(syntheticPaint(0, 0, 100, 1));
		assertSyntheticPasses("two strip passes after one parent paint", passes, 2, 1);
	}

	/* a show of the Grid with a synthetic parent expose; the Grid's own first pass has applied the patch */
	private ShowPasses syntheticShow(final Grid grid) {
		final Composite parent = new Composite(shell, SWT.NONE);
		final ParentExpose expose = new ParentExpose() {
			@Override
			public Control parent() {
				return parent;
			}

			@Override
			public Rectangle strip(final Grid g) {
				return new Rectangle(0, 0, 100, 1);
			}

			@Override
			public boolean ownPaint(final Event paint) {
				return paint.y < 10;
			}

			@Override
			public String toString() {
				return "synthetic parent, own area above y = 10";
			}
		};
		final ShowPasses passes = new ShowPasses(grid, expose);
		passes.start();
		grid.nativeOps++;
		grid.hiddenPatches++;
		passes.handleEvent(syntheticPaint(0, 0, 200, 100));
		assertSyntheticPasses("the Grid's own pass", passes, 1, 0);
		return passes;
	}

	private static Event syntheticPaint(final int x, final int y, final int width, final int height) {
		final Event paint = new Event();
		paint.x = x;
		paint.y = y;
		paint.width = width;
		paint.height = height;
		return paint;
	}

	private static void assertSyntheticPasses(final String what, final ShowPasses passes, final int gridPasses, final int parentPasses) {
		assertEquals(what + ": Grid passes (" + passes + ")", gridPasses, passes.gridPasses);
		assertEquals(what + ": parent passes (" + passes + ")", parentPasses, passes.parentPasses);
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

	/* the paint passes of the latest gate's show */
	private ShowPasses passesOnShow;

	private void gate(final String how, final Grid grid, final Consumer<Grid> hide, final Consumer<Grid> show,
			final java.util.function.BiConsumer<Grid, Random> deltas) {
		gate(how, grid, hide, show, deltas, null, null);
	}

	/*
	 * expose: a parent whose own redraw reaches into the Grid, or null; afterShow: extra work once the
	 * show's paints have settled, counted with them, or null
	 */
	private void gate(final String how, final Grid grid, final Consumer<Grid> hide, final Consumer<Grid> show,
			final java.util.function.BiConsumer<Grid, Random> deltas, final ParentExpose expose, final Consumer<Grid> afterShow) {
		shell.open();
		flush(grid, 1);
		final int paintsBeforeScroll = grid.paintPasses;
		grid.setTopIndex(400);
		flush(grid, paintsBeforeScroll + 1);
		hide.accept(grid);
		flush(grid, 0);
		final int nativeBefore = grid.nativeOps;
		final int paintsBefore = grid.paintPasses;
		final int patchesBefore = grid.hiddenPatches;
		final long started = System.nanoTime();
		deltas.accept(grid, new Random(0x41DL));
		final long micros = (System.nanoTime() - started) / 1_000;
		// no paint is expected: the quiet interval gives any paint while hidden the time to show up
		flush(grid, 0);
		final int nativeWhileHidden = grid.nativeOps - nativeBefore;
		final int paintsWhileHidden = grid.paintPasses - paintsBefore;
		// each paint the show brings: its clip, and whether it is the Grid's own pass or a parent's expose
		final ShowPasses passes = new ShowPasses(grid, expose);
		passesOnShow = passes;
		passes.start();
		show.accept(grid);
		flush(grid, paintsBefore + paintsWhileHidden + 1);
		if (afterShow != null) {
			afterShow.accept(grid);
		}
		passes.stop();
		final int paintsOnShow = grid.paintPasses - paintsBefore - paintsWhileHidden;
		LOG.info("GridHiddenViewport_Test: " + how + " (" + (GridViewport.ENABLED ? "viewport" : "original") + "): " + DELTAS
				+ " deltas in " + micros + " us; while hidden native ops " + nativeWhileHidden + ", paints " + paintsWhileHidden
				+ ", coalesced " + grid.hiddenDeltas + "; on show paint passes " + paintsOnShow + " (" + passes
				+ "), window patches " + (grid.hiddenPatches - patchesBefore));
		if (GridViewport.ENABLED) {
			assertEquals(how + ": native ops while hidden", 0, nativeWhileHidden);
			assertEquals(how + ": paints while hidden", 0, paintsWhileHidden);
			assertEquals(how + ": Grid paint passes on show (" + passes + ")", 1, passes.gridPasses);
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
		flush(reference, 1);
		final int referencePaints = reference.paintPasses;
		reference.setTopIndex(grid.getTopIndex());
		flush(reference, referencePaints + 1);
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

	/* a parent whose own redraw can reach into the Grid (see the class comment) */
	private interface ParentExpose {
		/** @return the parent */
		Control parent();

		/**
		 * @param grid the Grid
		 * @return the strip of the Grid's client area that the parent's own redraw covers, in Grid
		 *         coordinates; empty where that redraw does not reach child controls
		 */
		Rectangle strip(Grid grid);

		/**
		 * @param paint a paint event of the parent
		 * @return true when the paint covers the parent's own area outside the Grid, which only the
		 *         parent's own redraw paints
		 */
		boolean ownPaint(Event paint);
	}

	/*
	 * The tab strip of a CTabFolder. redrawTabs() (stock SWT 3.135 CTabFolder.java:2261-2270)
	 * invalidates its tab strip and one row more, which is the first row of the client area:
	 * redraw(0, 0, size.x, -trim.y - marginHeight + 1, false) for tabs on top, and the mirror
	 * rectangle for tabs at the bottom. Its trim is the renderer's body trim. computeTrim(0, 0, 0, 0)
	 * returns that trim plus the height of wrapped top-right controls, and this folder has none. So
	 * the strip follows the folder's renderer, tab height and margins, not a fixed row.
	 */
	private static final class TabStripExpose implements ParentExpose {
		private final CTabFolder folder;

		TabStripExpose(final CTabFolder folder) {
			this.folder = folder;
		}

		@Override
		public Control parent() {
			return folder;
		}

		/* the rectangle redrawTabs() invalidates, in folder coordinates */
		Rectangle redrawTabsArea() {
			final Point size = folder.getSize();
			final Rectangle trim = folder.computeTrim(0, 0, 0, 0);
			if (folder.getTabPosition() == SWT.BOTTOM) {
				final int h = trim.height + trim.y - folder.marginHeight;
				return new Rectangle(0, size.y - h - 1, size.x, h + 1);
			}
			return new Rectangle(0, 0, size.x, -trim.y - folder.marginHeight + 1);
		}

		/* the folder's area between its edge and its client area on the tab side, in folder coordinates */
		Rectangle tabArea() {
			final Point size = folder.getSize();
			final Rectangle client = folder.getClientArea();
			if (folder.getTabPosition() == SWT.BOTTOM) {
				final int bottom = client.y + client.height;
				return new Rectangle(0, bottom, size.x, size.y - bottom);
			}
			return new Rectangle(0, 0, size.x, client.y);
		}

		@Override
		public Rectangle strip(final Grid grid) {
			if (!PARENT_REDRAW_REACHES_CHILDREN) {
				return new Rectangle(0, 0, 0, 0);
			}
			return folder.getDisplay().map(folder, grid, redrawTabsArea()).intersection(grid.getClientArea());
		}

		@Override
		public boolean ownPaint(final Event paint) {
			return new Rectangle(paint.x, paint.y, paint.width, paint.height).intersects(tabArea());
		}

		@Override
		public String toString() {
			final Rectangle client = folder.getClientArea();
			return "redrawTabs " + text(redrawTabsArea()) + " of folder " + folder.getSize().x + "x" + folder.getSize().y
					+ ", client area " + text(client) + ", tab height " + folder.getTabHeight();
		}
	}

	/*
	 * The paint passes a show brings. Each pass is the Grid's own unless it is wholly contained in a
	 * parent's own invalidation: no native work of the Grid since its previous pass and no patch
	 * applied, a clip within the parent's strip, and a paint of the parent's own area since the
	 * Grid's previous pass (one such pass per parent redraw). This listener is added after the Grid's
	 * own paint listener, so a pass's patch and its native work are counted when it runs.
	 */
	private static final class ShowPasses implements Listener {
		private final Grid grid;
		private final ParentExpose expose;
		private final Listener parentPaint;
		private final StringBuilder passes = new StringBuilder();
		private int ops;
		private int patches;
		private int parentPaints;
		private int parentPaintsSeen;
		private Rectangle strip = new Rectangle(0, 0, 0, 0);
		/* passes that count as the Grid's own work */
		int gridPasses;
		/* passes wholly contained in a parent's own invalidation */
		int parentPasses;
		/* the parent paints the latest pass followed */
		int lastParentPaints;

		ShowPasses(final Grid grid, final ParentExpose expose) {
			this.grid = grid;
			this.expose = expose;
			parentPaint = this::parentPainted;
		}

		/* a paint event of the parent */
		void parentPainted(final Event e) {
			if (expose.ownPaint(e)) {
				parentPaints++;
			}
		}

		void start() {
			ops = grid.nativeOps;
			patches = grid.hiddenPatches;
			grid.addListener(SWT.Paint, this);
			if (expose != null) {
				expose.parent().addListener(SWT.Paint, parentPaint);
			}
		}

		void stop() {
			grid.removeListener(SWT.Paint, this);
			if (expose != null) {
				expose.parent().removeListener(SWT.Paint, parentPaint);
			}
		}

		@Override
		public void handleEvent(final Event e) {
			final Rectangle clip = new Rectangle(e.x, e.y, e.width, e.height);
			final int opsSince = grid.nativeOps - ops;
			final int patchesSince = grid.hiddenPatches - patches;
			final int parentPaintsSince = parentPaints - parentPaintsSeen;
			if (expose != null) {
				strip = expose.strip(grid);
			}
			// (a) no native work of the Grid since its previous pass, and no patch applied in this pass
			final boolean noGridWork = opsSince == 0 && patchesSince == 0;
			// (b) the clip lies within the parent's strip
			final boolean inStrip = within(clip, strip);
			// (c) the parent painted its own area since the Grid's previous pass
			final boolean afterParentPaint = parentPaintsSince > 0;
			final boolean parent = noGridWork && inStrip && afterParentPaint;
			lastParentPaints = parentPaintsSince;
			if (parent) {
				parentPasses++;
			} else {
				gridPasses++;
			}
			final Rectangle client = grid.getClientArea();
			passes.append(passes.length() == 0 ? "" : "; ").append(text(clip)).append(parent ? " parent" : " grid")
					.append(" [native ops ").append(opsSince).append(", patches ").append(patchesSince)
					.append(", parent paints ").append(parentPaintsSince).append(", client ").append(client.width).append('x')
					.append(client.height).append(']');
			ops = grid.nativeOps;
			patches = grid.hiddenPatches;
			parentPaintsSeen = parentPaints;
		}

		private static boolean within(final Rectangle clip, final Rectangle area) {
			return !area.isEmpty() && clip.x >= area.x && clip.y >= area.y && clip.x + clip.width <= area.x + area.width
					&& clip.y + clip.height <= area.y + area.height;
		}

		@Override
		public String toString() {
			if (expose == null) {
				return "Grid " + gridPasses + ", parent " + parentPasses + ": " + passes + "; no parent expose";
			}
			return "Grid " + gridPasses + ", parent " + parentPasses + ": " + passes + "; strip "
					+ (!strip.isEmpty() ? text(strip)
							: PARENT_REDRAW_REACHES_CHILDREN ? "empty" : "empty, a parent's redraw does not reach child controls here")
					+ " (" + expose + ")";
		}
	}

	private static String text(final Rectangle r) {
		return r.x + "," + r.y + " " + r.width + "x" + r.height;
	}

	/*
	 * Drains events until the Grid painted minimumPaintPasses times in total and then stayed quiet.
	 * A plain readAndDispatch drain returns before GTK3's frame-clock paint (see GridTestUtil).
	 */
	private void flush(final Grid grid, final int minimumPaintPasses) {
		GridTestUtil.awaitGridPaints(display, grid, minimumPaintPasses);
	}
}
