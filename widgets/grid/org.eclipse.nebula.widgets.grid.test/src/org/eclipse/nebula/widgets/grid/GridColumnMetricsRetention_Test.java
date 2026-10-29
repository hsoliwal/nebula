/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Real Grid lifetime regression; checks retained references without a timing-dependent GC request. */
public class GridColumnMetricsRetention_Test {

    private Shell shell;
    private Grid grid;

    /** Creates the original owning widgets on the SWT display thread. */
    @Before
    public void setUp() {
        shell = new Shell(Display.getDefault());
        grid = new Grid(shell, SWT.H_SCROLL | SWT.V_SCROLL);
        grid.setSize(480, 240);
        for (int i = 0; i < 8; ++i) {
            GridColumn column = new GridColumn(grid, SWT.NONE);
            column.setWidth(40 + i * 10);
        }
    }

    /** Releases all native widget resources owned by the fixture. */
    @After
    public void tearDown() {
        if (shell != null && !shell.isDisposed()) shell.dispose();
    }

    /** Removed trailing columns must leave the retained lane after its rebuild. */
    @Test
    public void shrinkReleasesDisposedTailColumns() throws Exception {
        GridColumnMetrics metrics = grid.columnMetrics();
        List<GridColumn> before = columns();
        assertTrue(metrics.ready(before));
        for (int i = before.size() - 1; i >= 3; --i) before.get(i).dispose();
        assertCurrentGeometryAndNoTail(metrics);
        assertSame(before.get(0), grid.getColumn(0));
        assertEquals(3, grid.getColumnCount());
    }

    /** Removing an interior column and then reordering retains only the live identities. */
    @Test
    public void reorderedSurvivorsKeepWidthsAndReleaseRemovedColumns() throws Exception {
        GridColumnMetrics metrics = grid.columnMetrics();
        assertTrue(metrics.ready(columns()));
        grid.getColumn(2).dispose();
        grid.getColumn(4).dispose();
        grid.setColumnOrder(new int[]{5, 3, 1, 4, 2, 0});
        assertCurrentGeometryAndNoTail(metrics);
        GridColumn first = grid.getColumn(grid.getColumnOrder()[0]);
        first.setWidth(137);
        assertCurrentGeometryAndNoTail(metrics);
    }

    /** Empty geometry must release every old object while retaining reusable primitive capacity. */
    @Test
    public void emptyRebuildReleasesEveryFormerColumn() throws Exception {
        GridColumnMetrics metrics = grid.columnMetrics();
        assertTrue(metrics.ready(columns()));
        for (GridColumn column : grid.getColumns()) column.dispose();
        assertCurrentGeometryAndNoTail(metrics);
        assertEquals(0, metrics.startColumnIndex(0));
        assertEquals(0, metrics.visibleColumns(0, 0, 400, false).length);
    }

    /** Reusing capacity for new columns must not resurrect disposed column identities. */
    @Test
    public void repeatedShrinkAndRegrowPreserveLiveOrder() throws Exception {
        GridColumnMetrics metrics = grid.columnMetrics();
        assertTrue(metrics.ready(columns()));
        for (int round = 0; round < 12; ++round) {
            while (grid.getColumnCount() > 2) grid.getColumn(grid.getColumnCount() - 1).dispose();
            assertCurrentGeometryAndNoTail(metrics);
            for (int i = 0; i < 3 + round % 4; ++i) {
                GridColumn column = new GridColumn(grid, SWT.NONE);
                column.setWidth(17 + round + i);
            }
            assertCurrentGeometryAndNoTail(metrics);
        }
    }

    private List<GridColumn> columns() {
        List<GridColumn> result = new ArrayList<>();
        for (int index : grid.getColumnOrder()) result.add(grid.getColumn(index));
        return result;
    }

    private void assertCurrentGeometryAndNoTail(GridColumnMetrics metrics) throws Exception {
        List<GridColumn> live = columns();
        assertTrue(metrics.ready(live));
        int x = 0;
        for (int i = 0; i < live.size(); ++i) {
            GridColumn column = live.get(i);
            assertEquals(i, metrics.positionOf(column));
            assertEquals(x, metrics.headerX(column, 0, false, 0));
            assertSame(column, metrics.columnAt(x, false, 0, false, 0));
            x += column.getWidth();
        }
        assertEquals(x, metrics.originX(null, 0, false, 0));
        // Inspect the retained root directly: a public behavior-only check would
        // miss this leak, and System.gc()/WeakReference polling is nondeterministic.
        Field field = GridColumnMetrics.class.getDeclaredField("columns");
        field.setAccessible(true);
        GridColumn[] retained = (GridColumn[])field.get(metrics);
        for (int i = 0; i < live.size(); ++i) assertSame(live.get(i), retained[i]);
        for (int i = live.size(); i < retained.length; ++i) {
            assertNull("retained removed column at slot " + i, retained[i]);
        }
    }
}
