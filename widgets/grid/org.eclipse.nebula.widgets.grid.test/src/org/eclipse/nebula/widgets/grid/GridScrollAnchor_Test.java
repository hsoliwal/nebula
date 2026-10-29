/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * This program and the accompanying materials are made available under the
 * Apache License 2.0 or the Eclipse Public License 2.0.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import org.eclipse.nebula.widgets.grid.internal.IScrollBarProxy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Regression contracts for pending tree scroll anchors and the existing Grid row-query owner. */
public class GridScrollAnchor_Test {
    private Display display;
    private Shell shell;

    /** Creates an isolated shell on the SWT thread. */
    @Before
    public void setUp() {
        display = Display.getDefault();
        shell = new Shell(display);
        shell.setBounds(10, 10, 340, 260);
    }

    /** Releases the shell and every Grid even when a contract fails. */
    @After
    public void tearDown() {
        if (shell != null && !shell.isDisposed()) shell.dispose();
    }

    /** A collapse must settle a stale native rank before either query caches it. */
    @Test
    public void collapsedEagerAndSparseRowsStayQueryableBeforePaint() {
        for (boolean sparse : new boolean[] {false, true}) {
            Fixture fixture = fixture(100, sparse);
            Grid grid = fixture.grid;
            grid.setTopIndex(80);
            grid.getBottomIndex();
            fixture.parent.setExpanded(false);
            int materialized = grid.virtualMaterializedItemCount();
            int requests = fixture.requests[0];
            assertEquals(0, grid.getTopIndex());
            assertEquals(101, grid.getBottomIndex());
            int[] range = new int[2];
            grid.getViewportItemRange(range, 0);
            assertEquals(0, range[0]);
            assertEquals(102, range[1]);
            assertEquals(materialized, grid.virtualMaterializedItemCount());
            assertEquals(requests, fixture.requests[0]);
            grid.dispose();
        }
    }

    /** Frozen queries retain valid logical anchors and perform no scrollbar work. */
    @Test
    public void frozenQueryCacheIsReplacedWhenTheScrollbarsSettle() {
        Fixture fixture = fixture(100, true);
        Grid grid = fixture.grid;
        grid.setTopIndex(80);
        grid.freezePlanes(Grid.PLANE_SCROLLBARS);
        try {
            fixture.parent.setExpanded(false);
            int nativeBefore = grid.nativeOps;
            int requestsBefore = fixture.requests[0];
            assertEquals(101, grid.getTopIndex());
            assertEquals(101, grid.getBottomIndex());
            assertEquals(nativeBefore, grid.nativeOps);
            assertEquals(requestsBefore, fixture.requests[0]);
        } finally {
            grid.thawPlanes(Grid.PLANE_SCROLLBARS);
        }
        assertEquals(0, grid.getTopIndex());
        assertEquals(101, grid.getBottomIndex());
        grid.redraw();
        grid.update();
        settle();
        assertEquals(0, grid.getTopIndex());
        assertEquals(101, grid.getBottomIndex());
    }

    /** A deferred anchor at a cold last child uses indexed geometry without creating an item. */
    @Test
    public void frozenOutOfDomainAnchorDoesNotMaterializeItsColdEndpoint() {
        Fixture fixture = fixture(100, true);
        Grid grid = fixture.grid;
        GridItem tail = grid.getRootItem(1);
        tail.setItemCount(5);
        tail.setExpanded(true);
        grid.setTopIndex(80);
        grid.freezePlanes(Grid.PLANE_SCROLLBARS);
        try {
            fixture.parent.setExpanded(false);
            int nativeBefore = grid.nativeOps;
            int requestsBefore = fixture.requests[0];
            int materializedBefore = grid.virtualMaterializedItemCount();
            assertEquals(106, grid.getTopIndex());
            assertEquals(106, grid.getBottomIndex());
            assertEquals(materializedBefore, grid.virtualMaterializedItemCount());
            assertEquals(requestsBefore, fixture.requests[0]);
            assertEquals(nativeBefore, grid.nativeOps);
        } finally {
            grid.thawPlanes(Grid.PLANE_SCROLLBARS);
        }
    }

    /**
     * Native setter callbacks may query rows while scrollbar settlement is in progress.
     *
     * @throws ReflectiveOperationException if the existing scrollbar owner cannot be observed
     */
    @Test
    public void reentrantScrollbarSetterQueriesNeverStartAnotherSettlement() throws ReflectiveOperationException {
        Fixture fixture = fixture(100, true);
        Grid grid = fixture.grid;
        grid.setTopIndex(80);
        fixture.parent.setExpanded(false);
        Field bar = Grid.class.getDeclaredField("vScroll");
        bar.setAccessible(true);
        Object original = bar.get(grid);
        int[] depth = {0};
        int[] calls = {0};
        IScrollBarProxy observed = IScrollBarProxy.class.cast(Proxy.newProxyInstance(
                IScrollBarProxy.class.getClassLoader(), new Class<?>[] {IScrollBarProxy.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("setValues")) {
                        assertEquals(1, ++depth[0]);
                        try {
                            calls[0]++;
                            int top = grid.getTopIndex();
                            int bottom = grid.getBottomIndex();
                            assertTrue(top == 0 || top == 101);
                            assertTrue(bottom >= top && bottom <= 101);
                        } finally {
                            depth[0]--;
                        }
                    }
                    return method.invoke(original, arguments);
                }));
        bar.set(grid, observed);
        try {
            assertEquals(0, grid.getTopIndex());
            assertEquals(101, grid.getBottomIndex());
            assertTrue("the native scrollbar setter must execute", calls[0] > 0);
        } finally {
            bar.set(grid, original);
        }
    }

    /** A failed external scrollbar snapshot must leave the next query able to settle. */
    @Test
    public void failedScrollbarSnapshotDoesNotLatchTheReentrancyGuard() {
        Grid grid = new Grid(shell, SWT.VIRTUAL);
        grid.setBounds(0, 0, 300, 220);
        new GridColumn(grid, SWT.NONE).setWidth(200);
        grid.setItemCount(100);
        shell.open();
        settle();
        IScrollBarProxy original = grid.getVerticalScrollBarProxy();
        boolean[] failOnce = {true};
        IScrollBarProxy observed = IScrollBarProxy.class.cast(Proxy.newProxyInstance(
                IScrollBarProxy.class.getClassLoader(), new Class<?>[] {IScrollBarProxy.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getSelection") && failOnce[0]) {
                        failOnce[0] = false;
                        throw new IllegalStateException("one failed external scrollbar snapshot");
                    }
                    return method.invoke(original, arguments);
                }));
        grid.setlVerticalScrollBarProxy(observed);
        grid.setItemCount(101);
        int nativeBefore = grid.nativeOps;
        assertThrows(IllegalStateException.class, grid::getTopIndex);
        assertEquals(0, grid.getTopIndex());
        assertTrue("a subsequent query must retry native settlement", grid.nativeOps > nativeBefore);
    }

    /** A pending million-child collapse must answer geometry without visiting cold items. */
    @Test
    public void collapsedMillionChildrenDoNotMaterializeDuringQueries() {
        Fixture fixture = fixture(1_000_000, true);
        Grid grid = fixture.grid;
        grid.setTopIndex(990_000);
        fixture.parent.setExpanded(false);
        int materialized = grid.virtualMaterializedItemCount();
        int requests = fixture.requests[0];
        assertEquals(0, grid.getTopIndex());
        assertEquals(1_000_001, grid.getBottomIndex());
        assertEquals(materialized, grid.virtualMaterializedItemCount());
        assertEquals(requests, fixture.requests[0]);
        assertTrue("paint and explicit scrolling retain a bounded set of facades", materialized < 64);
    }

    /**
     * An explicit hidden or out-of-range range endpoint remains an invalid argument.
     *
     * @throws ReflectiveOperationException if the existing viewport cannot be observed
     */
    @Test
    public void explicitInvalidRangeArgumentsKeepTheirFailureContract() throws ReflectiveOperationException {
        Fixture fixture = fixture(100, true);
        Grid grid = fixture.grid;
        fixture.parent.setExpanded(false);
        Field field = Grid.class.getDeclaredField("viewport");
        field.setAccessible(true);
        GridViewport viewport = (GridViewport) field.get(grid);
        assertThrows(IllegalArgumentException.class, () -> viewport.rowRange(1, 200, false, false));
        assertThrows(IllegalArgumentException.class, () -> viewport.rowRange(-2, 200, false, false));
        assertThrows(IllegalArgumentException.class, () -> viewport.rowRange(102, 200, false, false));
        assertThrows(IllegalArgumentException.class, () -> viewport.rowRange(1, 1));
        assertThrows(IllegalArgumentException.class, () -> viewport.rowRange(0, 100));
        assertThrows(IllegalArgumentException.class, () -> viewport.rowRange(101, 0));
    }

    /** Empty nonvirtual Grids keep the existing zero bounds and do not request data. */
    @Test
    public void emptyNonvirtualGridRetainsItsExistingBounds() {
        Grid grid = new Grid(shell, SWT.V_SCROLL);
        grid.setBounds(0, 0, 300, 220);
        int[] requests = {0};
        grid.addListener(SWT.SetData, event -> requests[0]++);
        assertEquals(0, grid.getTopIndex());
        assertEquals(0, grid.getBottomIndex());
        grid.setItemCount(20);
        grid.setItemCount(0);
        assertEquals(0, grid.getTopIndex());
        assertEquals(0, grid.getBottomIndex());
        assertEquals(0, requests[0]);
    }

    private Fixture fixture(int count, boolean sparse) {
        Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL);
        grid.setBounds(0, 0, 300, 220);
        new GridColumn(grid, SWT.NONE).setWidth(200);
        int[] requests = {0};
        grid.addListener(SWT.SetData, event -> {
            requests[0]++;
            ((GridItem) event.item).setText("row " + event.index);
        });
        GridItem parent = new GridItem(grid, SWT.NONE);
        new GridItem(grid, SWT.NONE);
        if (sparse) parent.setItemCount(count);
        else for (int index = 0; index < count; index++) new GridItem(parent, SWT.NONE);
        parent.setExpanded(true);
        shell.open();
        settle();
        return new Fixture(grid, parent, requests);
    }

    private void settle() {
        while (display.readAndDispatch()) {
            // Drain original SWT paints and layout.
        }
    }

    private static final class Fixture {
        final Grid grid;
        final GridItem parent;
        final int[] requests;
        Fixture(Grid grid, GridItem parent, int[] requests) {
            this.grid = grid;
            this.parent = parent;
            this.requests = requests;
        }
    }
}
