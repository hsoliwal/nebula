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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.nebula.widgets.grid.Grid.GridVisibleRange;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeSupport.RangeChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
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
		assertTrue("range delta must report the column that actually left the viewport",
				hasRemovedColumn(events, firstVisibleBefore));
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
            if (event.addedRows.length != 0 || event.removedRows.length != 0) {
                return true;
            }
		}
		return false;
	}

	private boolean hasColumnDelta(List<RangeChangedEvent> changes) {
		for (RangeChangedEvent event : changes) {
            if (event.addedColumns.length != 0 || event.removedColumns.length != 0) {
                return true;
            }
		}
		return false;
	}

	private boolean hasRemovedColumn(List<RangeChangedEvent> changes, GridColumn target) {
		for (RangeChangedEvent event : changes) {
            if (containsIdentity(event.removedColumns, target)) {
                return true;
            }
		}
		return false;
	}

	@Test
	public void testViewportDamageSeparatesVerticalBodyFromChrome() {
		Rectangle client = new Rectangle(0, 0, 360, 220);
		Rectangle vertical = GridViewportDamage.scrollDamage(client, 28, 24, false);
		assertTrue("vertical damage must start below the header", vertical.y == 28);
		assertTrue("vertical damage must stop above the footer", vertical.height == 168);
		assertTrue("vertical damage retains full body width", vertical.width == 360);

		Rectangle horizontal = GridViewportDamage.scrollDamage(client, 28, 24, true);
		assertTrue("horizontal scroll must invalidate the full viewport plane",
				horizontal.equals(client));

		assertTrue(GridViewportDamage.intersectsHeader(new Rectangle(0, 0, 100, 10), client, 28));
		assertTrue(!GridViewportDamage.intersectsHeader(new Rectangle(0, 40, 100, 10), client, 28));
		assertTrue(GridViewportDamage.intersectsFooter(new Rectangle(0, 210, 100, 10), client, 24));
		assertTrue(!GridViewportDamage.intersectsFooter(new Rectangle(0, 100, 100, 10), client, 24));
	}

	@Test
	public void testPaintDagSeparatesHeaderBodyFooterAndFixedPlanes() {
		Rectangle client = new Rectangle(0, 0, 360, 220);

		int header = GridPaintDAG.plan(
				new Rectangle(0, 0, 360, 20), client, 28, 24, true, true, true, false);
		assertTrue(GridPaintDAG.includes(header, GridPaintDAG.BACKGROUND));
		assertTrue(GridPaintDAG.includes(header, GridPaintDAG.HEADER));
		assertTrue(!GridPaintDAG.includes(header, GridPaintDAG.BODY));
		assertTrue(!GridPaintDAG.includes(header, GridPaintDAG.FIXED));
		assertTrue(!GridPaintDAG.includes(header, GridPaintDAG.FOOTER));

		int body = GridPaintDAG.plan(
				new Rectangle(0, 60, 360, 80), client, 28, 24, true, true, true, false);
		assertTrue(GridPaintDAG.includes(body, GridPaintDAG.BODY));
		assertTrue(GridPaintDAG.includes(body, GridPaintDAG.FIXED));
		assertTrue(!GridPaintDAG.includes(body, GridPaintDAG.HEADER));
		assertTrue(!GridPaintDAG.includes(body, GridPaintDAG.FOOTER));

		int footer = GridPaintDAG.plan(
				new Rectangle(0, 205, 360, 15), client, 28, 24, true, true, true, false);
		assertTrue(GridPaintDAG.includes(footer, GridPaintDAG.FOOTER));
		assertTrue(!GridPaintDAG.includes(footer, GridPaintDAG.BODY));

		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.FIXED, GridPaintDAG.BODY));
		assertTrue(GridPaintDAG.dependsOn(GridPaintDAG.HEADER, GridPaintDAG.BACKGROUND));
	}

	@Test
	public void testTransformPaintAtomComposesWithoutNativeResources() {
		GridTransform transform = GridTransform.translate(10, 20)
				.then(GridTransform.scale(2, 3));
		Rectangle mapped = transform.mapBounds(new Rectangle(1, 2, 3, 4));
		assertEquals(new Rectangle(22, 66, 6, 12), mapped);
		assertEquals(new Rectangle(1, 2, 3, 4),
				GridTransform.IDENTITY.mapBounds(new Rectangle(1, 2, 3, 4)));

		GridTransform quarterTurn = GridTransform.rotate((float)(Math.PI / 2d));
		assertEquals(new Rectangle(-6, 1, 4, 3),
				quarterTurn.mapBounds(new Rectangle(1, 2, 3, 4)));
		assertEquals(1f, quarterTurn.determinant(), 0.0001f);
		assertTrue(GridTransform.translate(3, 4).isTranslationOnly());
		assertTrue(!GridTransform.shear(1, 0).isTranslationOnly());

		GridTransform composite = GridTransform.translate(40, -10)
				.then(GridTransform.scale(2, 4));
		GridTransform restored = composite.then(composite.inverse());
		assertTrue(restored.isIdentity());
		assertSame(GridTransform.IDENTITY, GridTransform.translate(0, 0));
		assertSame(GridTransform.IDENTITY, GridTransform.scale(1, 1));
	}

	@Test
	public void testCanonicalQuadrantsDoNotSnapAdjacentAngles() {
		for (int quadrant = -4; quadrant <= 4; quadrant++) {
			float radians = (float)(quadrant * (Math.PI / 2d));
			GridTransform exact = GridTransform.rotate(radians);
			assertEquals(1f, exact.determinant(), 0f);
			assertTrue(exact.then(exact.inverse()).isIdentity());
			if ((quadrant & 3) == 0) {
				assertSame(GridTransform.IDENTITY, exact);
			}
			for (float adjacent : new float[] {Math.nextDown(radians), Math.nextUp(radians)}) {
				GridTransform ordinary = GridTransform.rotate(adjacent);
				assertEquals((float)Math.cos(adjacent), ordinary.m11, 0f);
				assertEquals((float)Math.sin(adjacent), ordinary.m12, 0f);
			}
		}
	}

	@Test
	public void testGcProxyRestoresClippingAfterPlanePaint() {
		GC gc = new GC(grid);
		try {
			Rectangle original = gc.getClipping();
			Rectangle requested = new Rectangle(
					original.x + 2, original.y + 3,
					Math.max(1, original.width / 2), Math.max(1, original.height / 2));
			Rectangle expected = original.intersection(requested);
			try (GridGCProxy proxy = GridGCProxy.wrap(gc).clip(requested)) {
				assertEquals(expected, proxy.gc().getClipping());
			}
			assertEquals(original, gc.getClipping());
		} finally {
			gc.dispose();
		}
	}

	@Test
	public void testAtomizedViewportProjectionAndSelectionPatterns() {
		List<GridColumn> orderedColumns = Arrays.asList(columns);
		GridColumn[] projected = GridViewportProjection.visibleColumns(
				orderedColumns, 0, columns.length - 1);
		assertTrue("projection atom must retain visible columns",
				projected.length == columns.length);
		assertSame(columns[0], GridViewportProjection.columnAt(
				orderedColumns, 1, false, 0, false, 0));

		List<GridItem> atomItems = List.of(grid.getItem(0), grid.getItem(1), grid.getItem(2));
		List<GridItem> selected = new ArrayList<>();
		GridSelectionAtom.selectRange(atomItems, selected, 0, 2);
		assertTrue(selected.size() == 3);
		GridSelectionAtom.selectIndices(atomItems, selected, new int[] { 0, 2, 2 });
		assertTrue("selection atom must remain duplicate-stable", selected.size() == 3);
		GridSelectionAtom.deselectRange(atomItems, selected, 1, 2);
		assertTrue(selected.size() == 1);
		assertSame(atomItems.get(0), selected.get(0));
	}

	private static boolean containsIdentity(GridColumn[] values, GridColumn target) {
		for (GridColumn value : values) {
            if (value == target) {
                return true;
            }
		}
		return false;
	}

	private void flushPaint() {
		// GTK may defer invalidation until its next frame-clock tick. Wait for an
		// actual Paint event before asserting the paint-driven range publication.
		boolean[] painted = { false };
		org.eclipse.swt.widgets.Listener observed = event -> painted[0] = true;
		grid.addListener(SWT.Paint, observed);
		try {
			grid.redraw();
			long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
			do {
				grid.update();
				while (display.readAndDispatch()) { /* drain real native events */ }
                if (!painted[0]) {
                    java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L);
                }
			} while (!painted[0] && System.nanoTime() < deadline && !Thread.currentThread().isInterrupted());
			assertTrue("actual SWT Paint must complete before range assertions", painted[0]);
		} finally {
			grid.removeListener(SWT.Paint, observed);
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
