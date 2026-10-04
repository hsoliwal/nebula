/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0, https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Real-widget geometry oracle: enumerate actual pixel hit-testing, not projection formulas. */
public class GridViewportCoordinates_Test {
    private Display display;
    private Shell shell;
    private Grid grid;
    private GridColumn[] columns;

    @Before public void setUp() {
        display = Display.getDefault();
        shell = new Shell(display);
        grid = new Grid(shell, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        grid.setSize(363, 220);
        grid.setHeaderVisible(true);
        columns = new GridColumn[8];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = new GridColumn(grid, SWT.NONE);
            columns[i].setWidth(80 + 17 * i);
        }
        for (int i = 0; i < 40; i++) new GridItem(grid, SWT.NONE).setText("row " + i);
        shell.setSize(400, 260);
        shell.open();
        flush();
    }

    @After public void tearDown() { if (shell != null && !shell.isDisposed()) shell.dispose(); }

    @Test public void pixelScrollingMatchesEveryHitTestedColumn() {
        for (int offset : new int[] {0, 1, 79, 80, 81, 177, 400, 700, 2000}) {
            scroll(offset);
            assertGeometry();
        }
        grid.showColumn(columns[7]); flush();
        assertTrue(Arrays.asList(grid.getVisibleRange().getColumns()).contains(columns[7]));
        assertGeometry();
    }

    @Test public void columnScrollingMatchesTheSamePixelOracle() {
        grid.setColumnScrolling(true); flush();
        for (int offset = 0; offset < columns.length; offset++) { scroll(offset); assertGeometry(); }
    }

    @Test public void hiddenZeroWidthAndReorderedColumnsKeepIdentityAndOrder() {
        columns[1].setVisible(false);
        columns[4].setWidth(0);
        grid.setColumnOrder(new int[] {7, 3, 1, 0, 6, 4, 5, 2}); flush();
        for (int offset : new int[] {0, 70, 200, 400, 2000}) { scroll(offset); assertGeometry(); }
        columns[1].setVisible(true); columns[4].setWidth(120); flush(); assertGeometry();
    }

    @Test public void fixedPlaneOccludesScrolledColumnsWithoutLosingPinnedReferences() {
        columns[0].setFixed(true); columns[1].setFixed(true); flush();
        for (int offset : new int[] {1, 100, 250, 600, 2000}) {
            scroll(offset); assertGeometry();
            List<GridColumn> visible = Arrays.asList(grid.getVisibleRange().getColumns());
            assertTrue(visible.contains(columns[0])); assertTrue(visible.contains(columns[1]));
        }
    }

    @Test public void rowHeaderAndResizeUseActualRemainingViewport() {
        grid.setRowHeaderVisible(true, 53); flush();
        for (int offset : new int[] {0, 1, 3, 7}) { scroll(offset); assertGeometry(); }
        grid.setSize(730, 260); shell.setSize(770, 310); flush(); assertGeometry();
        grid.setSize(160, 160); flush(); assertGeometry();
    }

    @Test public void projectionHandlesExactEdgesEmptyWindowsAndFixedNonPrefixColumns() {
        for (GridColumn column : columns) column.setWidth(100);
        List<GridColumn> list = Arrays.asList(columns);
        assertArrayEquals(new GridColumn[] {columns[1], columns[2]},
                GridViewportProjection.visibleColumns(list, 100, 0, 200, false));
        assertEquals(1, GridViewportProjection.startColumnIndex(list, 100));
        assertEquals(2, GridViewportProjection.endColumnIndex(list, 1, -100, 200));
        assertEquals(columns.length, GridViewportProjection.startColumnIndex(list, 800));
        assertEquals(0, GridViewportProjection.visibleColumns(list, 0, 70, 70, false).length);
        columns[3].setFixed(true);
        assertArrayEquals(new GridColumn[] {columns[3], columns[5], columns[6]},
                GridViewportProjection.visibleColumns(list, 400, 0, 300, true));
        columns[0].setWidth(0); columns[1].setVisible(false);
        assertEquals(2, GridViewportProjection.startColumnIndex(list, 0));
    }

    @Test public void widenedCumulativeWidthsDoNotWrapAndQueriesDoNotMutateModel() {
        shell.dispose(); shell = new Shell(display); grid = new Grid(shell, SWT.NONE);
        GridColumn a = new GridColumn(grid, SWT.NONE); a.setWidth(1_500_000_000);
        GridColumn b = new GridColumn(grid, SWT.NONE); b.setWidth(1_500_000_000);
        GridColumn c = new GridColumn(grid, SWT.NONE); c.setWidth(50);
        List<GridColumn> list = new ArrayList<>(List.of(a, b, c));
        List<GridColumn> before = List.copyOf(list);
        assertEquals(1, GridViewportProjection.startColumnIndex(list, 2_000_000_000));
        assertArrayEquals(new GridColumn[] {b},
                GridViewportProjection.visibleColumns(list, 2_000_000_000, 0, 200, false));
        assertEquals(before, list); assertEquals(1_500_000_000, a.getWidth());
        a.setVisible(false); b.setVisible(false); c.setVisible(false);
        assertEquals(0, GridViewportProjection.visibleColumns(list, 0, 0, 200, false).length);
    }

    private void scroll(int selection) {
        grid.getHorizontalBar().setSelection(selection);
        grid.getHorizontalBar().notifyListeners(SWT.Selection, new Event());
        flush();
    }

    private void assertGeometry() {
        Set<GridColumn> hits = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int x = 0; x < grid.getClientArea().width; x++) {
            GridColumn hit = grid.getColumn(new Point(x, 1));
            if (hit != null) hits.add(hit);
        }
        List<GridColumn> expected = new ArrayList<>();
        for (int index : grid.getColumnOrder()) if (hits.contains(columns[index])) expected.add(columns[index]);
        assertArrayEquals("range must match actual pixel hit tests in display order",
                expected.toArray(new GridColumn[0]), grid.getVisibleRange().getColumns());
    }

    private void flush() {
        grid.redraw(); grid.update();
        while (display.readAndDispatch()) { /* process real native layout/paint events */ }
        grid.update();
    }
}
