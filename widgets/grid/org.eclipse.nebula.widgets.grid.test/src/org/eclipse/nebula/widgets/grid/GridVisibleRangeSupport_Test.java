/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.nebula.widgets.grid.Grid.GridVisibleRange;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeSupport.RangeChangedEvent;
import org.eclipse.nebula.widgets.grid.GridVisibleRangeSupport.VisibleRangeChangedListener;
import org.eclipse.swt.SWT;
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
    private GridVisibleRangeSupport support;
    private Method calculate;
    private Field published;

    @Before
    public void setUp() throws Exception {
        display = Display.getDefault();
        shell = new Shell(display);
        grid = new Grid(shell, SWT.H_SCROLL | SWT.V_SCROLL);
        grid.setBounds(0, 0, 250, 160);
        for (int i = 0; i < 6; i++) {
            GridColumn column = new GridColumn(grid, SWT.NONE);
            column.setWidth(100);
        }
        for (int i = 0; i < 60; i++) {
            GridItem item = new GridItem(grid, SWT.NONE);
            item.setText(0, "row-" + i);
        }
        shell.setSize(280, 210);
        shell.open();
        flushPaint();
        assertTrue("The fixture must contain visible rows", grid.getVisibleRange().getItems().length > 0);
        assertTrue("The fixture must be scrollable", grid.getVisibleRange().getItems().length < 60);
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

    private void flushPaint() {
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
