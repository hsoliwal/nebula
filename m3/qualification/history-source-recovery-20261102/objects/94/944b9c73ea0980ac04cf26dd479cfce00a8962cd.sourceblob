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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.nebula.widgets.grid.Grid.GridVisibleRange;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeSupport.RangeChangedEvent;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeSupport.VisibleRangeChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Actual SWT/OSGi integration, not substituted Grid/SWT implementations.
 * Private method access makes callback/exception tests deterministic; the paint
 * test separately exercises the real SWT Paint registration. This suite pins
 * historical event semantics, not the separate opt-in removed-column repair.
 */
public class GridVisibleRangeSupport_Test {
    private Display display;
    private Shell shell;
    private Grid grid;
    private GridColumn[] columns;
    private final List<RangeChangedEvent> events = new ArrayList<>();
    private GridVisibleRangeSupport support;
    private Method calculate;
    private Field published;

    @Before
    public void setUp() throws Exception {
        display = Display.getDefault();
        shell = new Shell(display);
        grid = new Grid(shell, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        grid.setHeaderVisible(true);
        grid.setLinesVisible(true);
        grid.setBounds(0, 0, 360, 220);
        columns = new GridColumn[8];
        for (int i = 0; i < columns.length; i++) {
            GridColumn column = columns[i] = new GridColumn(grid, SWT.NONE);
            column.setText("column " + i);
            column.setWidth(120);
        }
        for (int row = 0; row < 240; row++) {
            GridItem item = new GridItem(grid, SWT.NONE);
            for (int column = 0; column < columns.length; column++) {
                item.setText(column, "r" + row + " c" + column);
            }
        }
        shell.setSize(390, 270);
        shell.open();
        flushPaint();
        assertTrue("The fixture must contain visible rows", grid.getVisibleRange().getItems().length > 0);
        assertTrue("The fixture must be scrollable", grid.getVisibleRange().getItems().length < grid.getItemCount());
        support = GridVisibleRangeSupport.createFor(grid);
        calculate = GridVisibleRangeSupport.class.getDeclaredMethod("calculateChange");
        calculate.setAccessible(true);
        published = GridVisibleRangeSupport.class.getDeclaredField("oldRange");
        published.setAccessible(true);
    }

    @After
    public void tearDown() {
        if (shell != null && !shell.isDisposed()) {
            shell.dispose();
        }
        // Display.getDefault() is shared with the existing Nebula test bundle.
    }

    @Test
    public void testInitialRangeAndNoop() throws Exception {
        List<RangeChangedEvent> events = new ArrayList<>();
        support.addRangeChangeListener(events::add);
        GridVisibleRange previous = snapshot();
        calculate();
        assertEquals(1, events.size());
        assertEvent(previous, events.get(0));
        assertSame(events.get(0).visibleRange, snapshot());
        calculate();
        assertEquals("An unchanged range must not emit another event", 1, events.size());
    }

    @Test
    public void testVerticalScrollDeltas() throws Exception {
        List<RangeChangedEvent> events = new ArrayList<>();
        support.addRangeChangeListener(events::add);
        calculate();
        GridVisibleRange previous = snapshot();
        events.clear();
        grid.setTopIndex(15);
        assertTrue("The test must actually scroll", grid.getTopIndex() > 0);
        calculate();
        assertEquals(1, events.size());
        assertEvent(previous, events.get(0));
        assertTrue(events.get(0).removedRows.length > 0);
        assertTrue(events.get(0).addedRows.length > 0);
    }

    @Test
    public void testHorizontalScrollDeltas() throws Exception {
        List<RangeChangedEvent> events = new ArrayList<>();
        support.addRangeChangeListener(events::add);
        calculate();
        GridVisibleRange previous = snapshot();
        events.clear();
        ScrollBar bar = grid.getHorizontalBar();
        assertNotNull(bar);
        bar.setSelection(120);
        bar.notifyListeners(SWT.Selection, new Event());
        assertTrue("The test must actually scroll", bar.getSelection() > 0);
        calculate();
        assertEquals(1, events.size());
        assertEvent(previous, events.get(0));
        assertTrue(events.get(0).addedColumns.length > 0);
        assertTrue(events.get(0).removedColumns.length > 0);
    }

    @Test
    public void testListenerOrderAndSharedEvent() throws Exception {
        List<Integer> order = new ArrayList<>();
        List<RangeChangedEvent> events = new ArrayList<>();
        GridVisibleRange previous = snapshot();
        support.addRangeChangeListener(event -> {
            order.add(1);
            events.add(event);
            assertSame("Publish occurs after all listeners", previous, snapshotUnchecked());
        });
        support.addRangeChangeListener(event -> {
            order.add(2);
            events.add(event);
            assertSame(previous, snapshotUnchecked());
        });
        calculate();
        assertEquals(Arrays.asList(1, 2), order);
        assertSame(events.get(0), events.get(1));
        assertSame(grid, events.get(0).getSource());
        assertSame(events.get(0).visibleRange, snapshot());
    }

    @Test
    public void testListenerFailureRetainsSnapshot() throws Exception {
        GridVisibleRange previous = snapshot();
        IllegalStateException expected = new IllegalStateException("listener-sentinel");
        VisibleRangeChangedListener throwing = event -> { throw expected; };
        List<RangeChangedEvent> later = new ArrayList<>();
        support.addRangeChangeListener(throwing);
        support.addRangeChangeListener(later::add);
        try {
            calculate();
            fail("The listener failure must propagate");
        } catch (IllegalStateException actual) {
            assertSame(expected, actual);
        }
        assertTrue(later.isEmpty());
        assertSame(previous, snapshot());
        support.removeRangeChangeListener(throwing);
        calculate();
        assertEquals(1, later.size());
        assertEvent(previous, later.get(0));
    }

    @Test
    public void testRemovingLastListenerRetainsSnapshot() throws Exception {
        List<RangeChangedEvent> events = new ArrayList<>();
        VisibleRangeChangedListener listener = events::add;
        support.addRangeChangeListener(listener);
        calculate();
        GridVisibleRange previous = snapshot();
        support.removeRangeChangeListener(listener);
        grid.setTopIndex(15);
        calculate();
        assertEquals(1, events.size());
        assertSame(previous, snapshot());
        support.addRangeChangeListener(listener);
        calculate();
        assertEquals(2, events.size());
        assertEvent(previous, events.get(1));
    }


    @Test
    public void testVisualViewportScenesProduceScreenshots() throws Exception {
        grid.setHeaderVisible(true);
        grid.setLinesVisible(true);
        for (int column = 0; column < grid.getColumnCount(); column++) {
            GridColumn gridColumn = grid.getColumn(column);
            gridColumn.setText("column " + column);
            gridColumn.setWidth(110);
        }
        for (int row = 0; row < grid.getItemCount(); row++) {
            GridItem item = grid.getItem(row);
            for (int column = 0; column < grid.getColumnCount(); column++) {
                item.setText(column, "r" + row + " c" + column);
            }
        }

        grid.setSize(360, 220);
        shell.setSize(390, 270);
        flushPaint();

        GridVisibleRange top = grid.getVisibleRange();
        assertTrue("top screenshot must show a bounded row viewport",
                top.getItems().length > 0 && top.getItems().length < grid.getItemCount());
        assertTrue("top screenshot must show a bounded column viewport",
                top.getColumns().length > 0 && top.getColumns().length < grid.getColumnCount());
        assertSame(grid.getItem(grid.getTopIndex()), top.getItems()[0]);
        snapshot("01-top");

        grid.setTopIndex(30);
        flushPaint();
        GridVisibleRange middle = grid.getVisibleRange();
        assertTrue("vertical screenshot must advance the logical viewport", grid.getTopIndex() > 0);
        assertSame(grid.getItem(grid.getTopIndex()), middle.getItems()[0]);
        snapshot("02-middle");

        ScrollBar horizontal = grid.getHorizontalBar();
        assertNotNull(horizontal);
        horizontal.setSelection(180);
        horizontal.notifyListeners(SWT.Selection, new Event());
        flushPaint();
        GridVisibleRange shifted = grid.getVisibleRange();
        assertTrue("horizontal screenshot must retain visible columns", shifted.getColumns().length > 0);
        assertTrue("horizontal scrollbar must move", horizontal.getSelection() > 0);
        snapshot("03-horizontal");

        int rowsBeforeResize = shifted.getItems().length;
        int columnsBeforeResize = shifted.getColumns().length;
        grid.setSize(620, 340);
        shell.setSize(650, 390);
        flushPaint();
        GridVisibleRange resized = grid.getVisibleRange();
        assertTrue("larger screenshot viewport must not expose fewer rows",
                resized.getItems().length >= rowsBeforeResize);
        assertTrue("larger screenshot viewport must not expose fewer columns",
                resized.getColumns().length >= columnsBeforeResize);
        snapshot("04-resized");
    }

@Test
	public void testVisibleRangeTracksRenderedViewportAndProducesScreenshots() throws Exception {
		events.clear();
		support.addRangeChangeListener(events::add);
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

    @Test
    public void testPaintDrivesVisibleRange() throws Exception {
        List<RangeChangedEvent> events = new ArrayList<>();
        GridVisibleRange[] previous = { snapshot() };
        support.addRangeChangeListener(event -> {
            assertEvent(previous[0], event);
            previous[0] = event.visibleRange;
            events.add(event);
        });
        grid.redraw();
        flushPaint();
        assertTrue("Real SWT Paint must invoke the registered support", !events.isEmpty());
        int count = events.size();
        grid.redraw();
        flushPaint();
        assertEquals("Stationary repaint must not emit a difference", count, events.size());
    }


private boolean hasRowDelta(List<RangeChangedEvent> changes) {
		for (RangeChangedEvent event : changes) {
			if (event.addedRows.length != 0 || event.removedRows.length != 0) return true;
		}
		return false;
	}

private boolean hasColumnDelta(List<RangeChangedEvent> changes) {
		for (RangeChangedEvent event : changes) {
			if (event.addedColumns.length != 0 || event.removedColumns.length != 0) return true;
		}
		return false;
	}

private boolean hasRemovedColumn(List<RangeChangedEvent> changes, GridColumn target) {
		for (RangeChangedEvent event : changes) {
			if (containsIdentity(event.removedColumns, target)) return true;
		}
		return false;
	}

private static boolean containsIdentity(GridColumn[] values, GridColumn target) {
		for (GridColumn value : values) {
			if (value == target) return true;
		}
		return false;
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

    private void flushPaint() {
        grid.redraw();
        grid.update();
        for (int i = 0; i < 10000; i++) {
            if (!display.readAndDispatch()) {
                return;
            }
        }
        fail("SWT event queue did not quiesce within the fixture budget");
    }

    private GridVisibleRange snapshot() throws IllegalAccessException {
        return (GridVisibleRange) published.get(support);
    }

    private GridVisibleRange snapshotUnchecked() {
        try {
            return snapshot();
        } catch (IllegalAccessException failure) {
            throw new AssertionError(failure);
        }
    }

    private void calculate() throws Exception {
        try {
            calculate.invoke(support);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new AssertionError(cause);
        }
    }

    private void assertEvent(GridVisibleRange previous, RangeChangedEvent event) {
        assertSame(grid, event.getSource());
        Delta<GridItem> rows = legacy(previous.getItems(), event.visibleRange.getItems());
        Delta<GridColumn> columns = legacy(previous.getColumns(), event.visibleRange.getColumns());
        assertReferences(rows.added.toArray(), event.addedRows);
        assertReferences(rows.removed.toArray(), event.removedRows);
        assertReferences(columns.added.toArray(), event.addedColumns);
        // Preserve the historical ignored-return toArray behavior exactly. The
        // documented removed-column repair is deliberately a different recipe.
        GridColumn[] legacyRemoved = new GridColumn[columns.removed.size()];
        columns.added.toArray(legacyRemoved);
        assertReferences(legacyRemoved, event.removedColumns);
    }

    private static <T> Delta<T> legacy(T[] before, T[] after) {
        List<T> removed = new ArrayList<>(Arrays.asList(before));
        List<T> added = new ArrayList<>(Arrays.asList(after));
        Iterator<T> iterator = added.iterator();
        while (iterator.hasNext()) {
            if (removed.remove(iterator.next())) {
                iterator.remove();
            }
        }
        return new Delta<>(removed, added);
    }

    private static void assertReferences(Object[] expected, Object[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertSame("Reference/order at " + i, expected[i], actual[i]);
        }
    }

    private static final class Delta<T> {
        final List<T> removed;
        final List<T> added;
        Delta(List<T> removed, List<T> added) {
            this.removed = removed;
            this.added = added;
        }
    }
}
