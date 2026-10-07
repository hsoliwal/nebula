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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.nebula.widgets.grid.Grid.GridVisibleRange;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeSupport.RangeChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
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

	private static final String SCREEN_CAPTURE = "nebula.grid.viewport.screenshots.screen";
	private static final String NATIVE_CAPTURE = "nebula.grid.viewport.screenshots.native";

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
		shell.setLocation(30, 40);
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
		assertTrue("viewport signal must publish row-range delta before paint", hasRowDelta(events));
		int verticalEventsBeforePaint = events.size();
		flushPaint();
		assertEquals("paint fallback must not duplicate an already-published row delta",
				verticalEventsBeforePaint, events.size());
		GridVisibleRange middle = grid.getVisibleRange();
		assertTrue("vertical scroll must advance the logical viewport", grid.getTopIndex() >= 100);
		assertSame(grid.getItem(grid.getTopIndex()), middle.getItems()[0]);
		assertNotSame(top.getItems()[0], middle.getItems()[0]);
		snapshot("02-middle");

		events.clear();
		GridColumn firstVisibleBefore = middle.getColumns()[0];
		grid.showColumn(columns[7]);
		assertTrue("viewport signal must publish column-range delta before paint", hasColumnDelta(events));
		int horizontalEventsBeforePaint = events.size();
		flushPaint();
		assertEquals("paint fallback must not duplicate an already-published column delta",
				horizontalEventsBeforePaint, events.size());
		GridVisibleRange horizontal = grid.getVisibleRange();
		assertTrue("rightmost column must be visible after showColumn",
				containsIdentity(horizontal.getColumns(), columns[7]));
		assertNotSame("horizontal viewport must advance from its initial first column",
				firstVisibleBefore, horizontal.getColumns()[0]);
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
	public void testVirtualMillionRowScreenshotsStaySparseAcrossDistantViewport() throws Exception {
		grid.dispose();
		grid = new Grid(shell, SWT.VIRTUAL | SWT.CHECK | SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		grid.setSize(760, 420);

		columns = new GridColumn[4];
		for (int column = 0; column < columns.length; column++) {
			GridColumn gridColumn = columns[column] = new GridColumn(grid, SWT.NONE);
			gridColumn.setText("column " + column);
			gridColumn.setWidth(220);
		}

		final int[] setDataCount = { 0 };
		grid.addListener(SWT.SetData, event -> {
			setDataCount[0]++;
			GridItem item = (GridItem)event.item;
			int row = event.index;
			item.setText(0, "row " + row);
			item.setText(1, "group " + (row >>> 10));
			item.setText(2, "hex " + Integer.toHexString(row));
			item.setText(3, "mask " + (row & 63));
			if ((row & 31) == 0) {
				item.setChecked(true);
			}
		});

		shell.setSize(780, 460);
		grid.setItemCount(1_000_000);
		flushPaint();
		assertEquals(1_000_000, grid.getItemCount());
		assertTrue("top viewport must keep million-row facade residency bounded",
				grid.virtualMaterializedItemCount() < 256);
		snapshot("05-virtual-million-top");

		grid.setTopIndex(500_000);
		grid.setSelection(500_000, 500_002);
		flushPaint();
		assertTrue("distant scroll must reach the logical midpoint", grid.getTopIndex() >= 500_000);
		assertTrue("distant viewport must keep million-row facade residency bounded",
				grid.virtualMaterializedItemCount() < 256);
		assertTrue("visible data demand must stay far below logical row count",
				setDataCount[0] < 1_000);
		snapshot("06-virtual-million-middle");
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
		try {
			new GridTransform(Float.NaN, 0, 0, 1, 0, 0);
			fail("non-finite transform elements must be rejected");
		} catch (IllegalArgumentException expected) {
			// expected
		}
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
		// GTK may defer invalidation until its next frame-clock tick. Paint is still
		// followed by bounded event draining so the frame is presented before screen capture.
		boolean[] painted = { false };
		org.eclipse.swt.widgets.Listener observed = event -> painted[0] = true;
		grid.addListener(SWT.Paint, observed);
		try {
			grid.redraw();
			long started = System.nanoTime();
			long deadline = started + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
			long presented = started + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(120);
			do {
				grid.update();
				while (display.readAndDispatch()) { /* drain real native events */ }
                if (!painted[0] || System.nanoTime() < presented) {
                    java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L);
                }
			} while ((!painted[0] || System.nanoTime() < presented) && System.nanoTime() < deadline && !Thread.currentThread().isInterrupted());
			assertTrue("actual SWT Paint must complete before range assertions", painted[0]);
		} finally {
			grid.removeListener(SWT.Paint, observed);
		}
	}


	@Test
	public void testScreenCaptureFreshnessBoundsAndGraphicsLifetime() throws Exception {
		org.junit.Assume.assumeTrue(Boolean.getBoolean(SCREEN_CAPTURE));
		assertEquals("gtk", SWT.getPlatform());
		assertEquals("x11", System.getenv("GDK_BACKEND"));
		assertTrue(!"1".equals(System.getenv("SWT_GTK4")));
		Path output = Path.of("target", "m3-visible-range-screenshots");
		String expectedGc = System.getProperty("nebula.swt.gc.sha256", "");
		assertTrue("Missing pinned SWT receiver hash", expectedGc.matches("[0-9a-f]{64}"));
		try (var input = GC.class.getResourceAsStream("GC.class")) {
			assertTrue("Cannot inspect loaded SWT GC", input != null);
			String actualGc = java.util.HexFormat.of().formatHex(
					java.security.MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
			assertEquals("Tycho loaded a different SWT GC", expectedGc, actualGc);
			Files.createDirectories(output);
			Files.writeString(output.resolve("swt-receiver.properties"),
					"gcClassSha256=" + actualGc + "\nresource=" + GC.class.getResource("GC.class") + "\n");
		}
		int[] color = { SWT.COLOR_RED };
		grid.addListener(SWT.Paint, event -> {
			event.gc.setBackground(display.getSystemColor(color[0]));
			event.gc.fillRectangle(32, 40, 64, 32);
		});
		flushPaint();
		GridSwtScreenshotCapture.Result first = GridSwtScreenshotCapture.captureScreenControl(
				grid, output.resolve("capture-red.png"));
		Rectangle firstBounds = GridSwtScreenshotCapture.screenBounds(grid);
		color[0] = SWT.COLOR_BLUE;
		shell.setLocation(70, 80);
		flushPaint();
		GridSwtScreenshotCapture.Result second = GridSwtScreenshotCapture.captureScreenControl(
				grid, output.resolve("capture-blue.png"));
		Rectangle secondBounds = GridSwtScreenshotCapture.screenBounds(grid);
		assertTrue(!firstBounds.equals(secondBounds));
		int border = grid.getBorderWidth();
		for (String name : new String[] {"red", "blue"}) {
			org.eclipse.swt.graphics.ImageData data = new org.eclipse.swt.graphics.ImageLoader()
					.load(output.resolve("capture-" + name + ".png").toString())[0];
			org.eclipse.swt.graphics.RGB expected = display.getSystemColor(
					name.equals("red") ? SWT.COLOR_RED : SWT.COLOR_BLUE).getRGB();
			assertEquals(expected, data.palette.getRGB(data.getPixel(40 + border, 48 + border)));
		}
		grid.setVisible(false);
		try {
			GridSwtScreenshotCapture.screenBounds(grid);
			fail("hidden capture must fail");
		} catch (IllegalArgumentException expected) { /* required rejection */ }
		grid.setVisible(true);
		org.eclipse.swt.graphics.Point original = grid.getLocation();
		grid.setLocation(-1, original.y);
		try {
			GridSwtScreenshotCapture.screenBounds(grid);
			fail("ancestor-clipped capture must fail");
		} catch (IllegalArgumentException expected) { /* required rejection */ }
		grid.setLocation(original);
		flushPaint();
		// Warm all paths before turning on the ownership observer.
		for (int path = 0; path < 3; path++) capturePath(path, output.resolve("repeat.png"));
		boolean tracking = display.isTracking();
		display.setTracking(true);
		int failures = 0;
		try {
			java.util.Set<Object> baseline = trackedGraphics();
			org.eclipse.swt.graphics.Image retained = new org.eclipse.swt.graphics.Image(display, 4, 4);
			GC retainedGc = null;
			try {
				assertEquals(baseline.size() + 1, trackedGraphics().size());
				retainedGc = new GC(retained);
				assertEquals(baseline.size() + 2, trackedGraphics().size());
			} finally {
				if (retainedGc != null) retainedGc.dispose();
				retained.dispose();
			}
			assertEquals(baseline, trackedGraphics());
			for (int iteration = 0; iteration < 16; iteration++) {
				for (int path = 0; path < 3; path++) {
					capturePath(path, output.resolve("repeat.png"));
					assertEquals(baseline, trackedGraphics());
				}
			}
			for (int path = 0; path < 3; path++) {
				try {
					capturePath(path, output); // Existing directory: deterministic PNG write failure.
					fail("PNG write to a directory must fail");
				} catch (org.eclipse.swt.SWTException expected) { failures++; }
				assertEquals(baseline, trackedGraphics());
			}
		} finally { display.setTracking(tracking); }
		assertEquals(3, failures);
		Files.writeString(output.resolve("screen-contract.properties"),
				"captureMethod=DISPLAY_COPY_AREA\nmanualQaRequired=false\n"
				+ "red.sha256=" + first.sha256() + "\nblue.sha256=" + second.sha256() + "\n"
				+ "marker.x=" + (32 + border) + "\nmarker.y=" + (40 + border) + "\n"
				+ "marker.width=64\nmarker.height=32\n"
				+ "successfulCaptures=48\nwriteFailures=3\npositiveLeakControls=2\nretainedGraphicsDelta=0\n");
	}

	private void capturePath(int path, Path output) throws IOException {
		switch (path) {
			case 0: GridSwtScreenshotCapture.captureScreenControl(grid, output); break;
			case 1: GridSwtScreenshotCapture.captureControl(grid, output); break;
			case 2: GridSwtScreenshotCapture.captureNativeShell(grid, output); break;
			default: throw new IllegalArgumentException("capture path");
		}
	}

	private java.util.Set<Object> trackedGraphics() {
		java.util.Set<Object> result = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		for (Object value : display.getDeviceData().objects) {
			if (value instanceof GC || value instanceof org.eclipse.swt.graphics.Image) result.add(value);
		}
		return result;
	}

	private void snapshot(String name) throws IOException {
		Path directory = Path.of("target", "m3-visible-range-screenshots");
		Files.createDirectories(directory);
		GridSwtScreenshotCapture.Result screen = Boolean.getBoolean(SCREEN_CAPTURE)
				? GridSwtScreenshotCapture.captureScreenControl(grid, directory.resolve(name + "-screen.png")) : null;
		Path png = directory.resolve(name + ".png");
		GridSwtScreenshotCapture.Result capture =
				GridSwtScreenshotCapture.captureControl(grid, png);

		GridVisibleRange visible = grid.getVisibleRange();
		StringBuilder evidence = new StringBuilder(512);
		evidence.append("scenario=").append(name).append('\n');
		evidence.append("platform=").append(SWT.getPlatform()).append('\n');
		evidence.append("logicalRows=").append(grid.getItemCount()).append('\n');
		evidence.append("materializedItems=")
				.append(grid.virtualMaterializedItemCount()).append('\n');
		evidence.append("topIndex=").append(grid.getTopIndex()).append('\n');
		evidence.append("visibleRows=").append(visible.getItems().length).append('\n');
		evidence.append("visibleColumns=").append(visible.getColumns().length).append('\n');
		evidence.append("selectionCount=").append(grid.getSelectionCount()).append('\n');
		evidence.append("captureMethod=").append(capture.method()).append('\n');
		evidence.append("screenshot=").append(capture.path().getFileName()).append('\n');
		evidence.append("screenshot.sha256=").append(capture.sha256()).append('\n');
		appendScrollBarEvidence(evidence, "h", grid.getHorizontalBar());
		appendScrollBarEvidence(evidence, "v", grid.getVerticalBar());

		if (Boolean.getBoolean(SCREEN_CAPTURE)) {
			assertEquals("gtk", SWT.getPlatform());
			assertEquals("x11", System.getenv("GDK_BACKEND"));
			assertTrue(!"1".equals(System.getenv("SWT_GTK4")));
			org.eclipse.swt.graphics.ImageData pixels = new org.eclipse.swt.graphics.ImageLoader()
					.load(screen.path().toString())[0];
			evidence.append("screen.captureMethod=").append(screen.method()).append('\n');
			evidence.append("screen.screenshot=").append(screen.path().getFileName()).append('\n');
			evidence.append("screen.sha256=").append(screen.sha256()).append('\n');
			evidence.append("screen.bounds=").append(GridSwtScreenshotCapture.screenBounds(grid)).append('\n');
			evidence.append("screen.dpi=").append(display.getDPI()).append('\n');
			evidence.append("screen.backend=").append(System.getenv("GDK_BACKEND")).append('\n');
			evidence.append("screen.pixelWidth=").append(pixels.width).append('\n');
			evidence.append("screen.pixelHeight=").append(pixels.height).append('\n');
		}

		if (Boolean.getBoolean(NATIVE_CAPTURE)) {
			Path nativePng = directory.resolve(name + "-native.png");
			try {
				GridSwtScreenshotCapture.Result nativeCapture =
						GridSwtScreenshotCapture.captureNativeShell(grid, nativePng);
				evidence.append("nativeScreenshot=")
						.append(nativeCapture.path().getFileName()).append('\n');
				evidence.append("nativeScreenshot.captureMethod=")
						.append(nativeCapture.method()).append('\n');
				evidence.append("nativeScreenshot.sha256=")
						.append(nativeCapture.sha256()).append('\n');
			} catch (RuntimeException | IOException unavailable) {
				evidence.append("nativeScreenshot.error=")
						.append(unavailable.getClass().getName())
						.append(": ").append(String.valueOf(unavailable.getMessage()))
						.append('\n');
			}
		}

		Files.writeString(
				directory.resolve(name + ".txt"), evidence, StandardCharsets.UTF_8);
	}

	private static void appendScrollBarEvidence(
			StringBuilder evidence, String prefix, ScrollBar bar) {
		if (bar == null || bar.isDisposed()) {
			evidence.append(prefix).append(".scrollbar=<none>\n");
			return;
		}
		evidence.append(prefix).append(".selection=").append(bar.getSelection()).append('\n');
		evidence.append(prefix).append(".minimum=").append(bar.getMinimum()).append('\n');
		evidence.append(prefix).append(".maximum=").append(bar.getMaximum()).append('\n');
		evidence.append(prefix).append(".thumb=").append(bar.getThumb()).append('\n');
		evidence.append(prefix).append(".visible=").append(bar.getVisible()).append('\n');
	}

}
