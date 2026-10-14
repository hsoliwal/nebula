/*******************************************************************************
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0, available at
 * https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.nebula.widgets.grid.internal.DefaultColumnFooterRenderer;
import org.eclipse.nebula.widgets.grid.internal.DefaultColumnHeaderRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Real-widget regression. Requires a native display; no skipped/stubbed pass is admissible. */
public class GridPaintPreparation_Test {
    private Display display;
    private Shell shell;
    private CountingGrid grid;
    private CountingData data;
    private final List<String> renders = new ArrayList<>();
    private int setDataCalls;
    private GridColumn[] columns;

    @Before public void setUp() {
        display = Display.getDefault();
        shell = new Shell(display);
        data = new CountingData(display);
        grid = new CountingGrid(data, shell);
        grid.setHeaderVisible(true);
        grid.setFooterVisible(true);
        grid.setSize(360, 220);
        columns = new GridColumn[4];
        for (int index = 0; index < columns.length; index++) {
            final int columnIndex = index;
            GridColumn column = columns[index] = new GridColumn(grid, SWT.NONE);
            column.setText("column " + index);
            column.setFooterText("footer " + index);
            column.setWidth(120);
            column.setCellRenderer(new DefaultCellRenderer() {
                @Override public void paint(GC gc, Object value) {
                    renders.add("body:" + columnIndex);
                    super.paint(gc, value);
                }
            });
            column.setHeaderRenderer(new DefaultColumnHeaderRenderer() {
                @Override public void paint(GC gc, Object value) {
                    renders.add("header:" + columnIndex);
                    super.paint(gc, value);
                }
            });
            column.setFooterRenderer(new DefaultColumnFooterRenderer() {
                @Override public void paint(GC gc, Object value) {
                    renders.add("footer:" + columnIndex);
                    super.paint(gc, value);
                }
            });
        }
        grid.addListener(SWT.SetData, event -> {
            setDataCalls++;
            GridItem item = (GridItem) event.item;
            item.setText("row " + event.index);
        });
        grid.setItemCount(4096);
        shell.setSize(390, 260);
        // Settle Grid's existing scrollbar geometry without needing the event loop.
        paint(grid.getClientArea());
        grid.getItem(0).setRowSpan(0, 1);
        grid.setTopIndex(2048);
        assertEquals("far viewport fixture", 2048, grid.getTopIndex());
        resetObservations();
    }

    @After public void tearDown() {
        if (shell != null && !shell.isDisposed()) shell.dispose();
    }

    @Test public void headerDamageDoesNotPrepareOrMaterializeBodyRows() {
        Rectangle area = grid.getClientArea();
        assertNoBodyPreparation(new Rectangle(0, 0, area.width, grid.getHeaderHeight()));
        assertTrue(renders.stream().anyMatch(value -> value.startsWith("header:")));
    }

    @Test public void footerDamageDoesNotPrepareOrMaterializeBodyRows() {
        Rectangle area = grid.getClientArea();
        assertNoBodyPreparation(new Rectangle(0, area.height - grid.getFooterHeight(),
                area.width, grid.getFooterHeight()));
        assertTrue(renders.stream().anyMatch(value -> value.startsWith("footer:")));
    }

    @Test public void emptyDamageDoesNotPrepareOrMaterializeRows() {
        assertNoBodyPreparation(new Rectangle(20, 80, 0, 0));
        assertTrue(renders.isEmpty());
    }

    @Test public void repeatedHeaderPaintsKeepTheSameSparseFacadesAndSelection() {
        GridItem selected = grid.getItem(2048);
        grid.setSelection(new GridItem[] {selected});
        Rectangle area = grid.getClientArea();
        resetObservations();
        int count = grid.virtualMaterializedItemCount();
        for (int index = 0; index < 32; index++) {
            paint(new Rectangle(0, 0, area.width, grid.getHeaderHeight()));
        }
        assertEquals(0, data.rowSpanCalls);
        assertEquals(count, grid.virtualMaterializedItemCount());
        assertSame(selected, grid.getItem(2048));
        assertArrayEquals(new GridItem[] {selected}, grid.getSelection());
    }

    @Test public void defaultVisualizerDistantBodyUsesSparseSpanFrontier() {
        Grid indexed = new Grid(shell, SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);
        indexed.setSize(360, 220);
        GridColumn column = new GridColumn(indexed, SWT.NONE);
        column.setWidth(160);
        GridColumn hidden = new GridColumn(indexed, SWT.NONE);
        hidden.setWidth(160);
        hidden.setVisible(false);
        indexed.setItemCount(1_000_000);

        // Settle existing scrollbar geometry at the start of the logical model.
        paint(indexed, indexed.getClientArea());

        GridItem spanOwner = indexed.getItem(499_995);
        spanOwner.setRowSpan(1, 100);
        assertEquals("a hidden-column span must not change the historical paint frontier",
                500_000, indexed.rowSpanPaintStart(500_000));

        spanOwner.setRowSpan(0, 10);
        assertEquals(499_995, indexed.rowSpanPaintStart(500_000));
        assertEquals(1, indexed.rowSpanIndexOwnerCount());

        // Once spanning is active, direct mutation through the built-in public
        // DataVisualizer must keep the sparse owner exact as well.
        indexed.getDataVisualizer().setRowSpan(spanOwner, 0, 0);
        assertEquals(0, indexed.rowSpanIndexOwnerCount());
        indexed.getDataVisualizer().setRowSpan(spanOwner, 0, 10);
        assertEquals(1, indexed.rowSpanIndexOwnerCount());

        indexed.setTopIndex(500_000);
        assertEquals(500_000, indexed.getTopIndex());

        int before = indexed.virtualMaterializedItemCount();
        paint(indexed, indexed.getClientArea());
        int after = indexed.virtualMaterializedItemCount();

        assertEquals(1, indexed.rowSpanIndexOwnerCount());
        assertTrue("distant body paint must materialize only a bounded viewport frontier: " + (after - before),
                after - before < 128);
        assertSame(spanOwner, indexed.getItem(499_995));
    }

    @Test public void ordinaryBodyStillPreparesSpansAndRestoresTheClip() {
        Rectangle area = grid.getClientArea();
        Rectangle body = new Rectangle(0, grid.getHeaderHeight(), area.width,
                area.height - grid.getHeaderHeight() - grid.getFooterHeight());
        ImageData image = paint(body);
        assertTrue("body span preparation remains admitted", data.rowSpanCalls > 0);
        assertTrue("actual body renderer callbacks", renders.stream().anyMatch(value -> value.startsWith("body:")));
        assertTrue("chrome must remain outside body damage", renders.stream().noneMatch(value -> !value.startsWith("body:")));
        assertTrue(image.width > 0 && image.height > 0);
    }

    @Test public void fixedBodyAndChromeRetainTheirPaintOrder() {
        columns[0].setFixed(true);
        grid.getHorizontalBar().setSelection(150);
        grid.getHorizontalBar().notifyListeners(SWT.Selection, new Event());
        resetObservations();
        paint(grid.getClientArea());
        int firstChrome = -1;
        int lastBody = -1;
        int firstFooter = -1;
        int lastHeader = -1;
        for (int index = 0; index < renders.size(); index++) {
            String entry = renders.get(index);
            if (entry.startsWith("body:")) lastBody = index;
            else if (firstChrome == -1) firstChrome = index;
            if (entry.startsWith("header:")) lastHeader = index;
            if (entry.startsWith("footer:") && firstFooter == -1) firstFooter = index;
        }
        assertTrue("body renderers must execute", lastBody >= 0);
        assertTrue("body/fixed planes precede chrome", firstChrome > lastBody);
        assertTrue("headers precede footers", firstFooter > lastHeader && lastHeader >= 0);
        assertTrue("frozen first column is still rendered", renders.contains("body:0"));
    }

    private void assertNoBodyPreparation(Rectangle clip) {
        int count = grid.virtualMaterializedItemCount();
        paint(clip);
        assertEquals("no span-prefix queries outside body damage", 0, data.rowSpanCalls);
        assertEquals("no row projection outside body damage", 0, grid.topQueries);
        assertEquals("no new sparse facade outside body damage", count, grid.virtualMaterializedItemCount());
        assertEquals("no SetData callback outside body damage", 0, setDataCalls);
        assertTrue(renders.stream().noneMatch(value -> value.startsWith("body:")));
    }

    private ImageData paint(Rectangle clip) {
        return paint(grid, clip);
    }

    private ImageData paint(Grid target, Rectangle clip) {
        Image image = new Image(display, 400, 280);
        GC gc = new GC(image);
        try {
            gc.setClipping(clip);
            Rectangle actualClip = gc.getClipping();
            Event event = new Event();
            event.gc = gc;
            event.x = clip.x;
            event.y = clip.y;
            event.width = clip.width;
            event.height = clip.height;
            target.notifyListeners(SWT.Paint, event);
            assertEquals("paint must restore incoming clipping", actualClip, gc.getClipping());
            return image.getImageData();
        } finally {
            gc.dispose();
            image.dispose();
        }
    }

    private void resetObservations() {
        data.rowSpanCalls = 0;
        grid.topQueries = 0;
        setDataCalls = 0;
        renders.clear();
    }

    private static class CountingData extends GridItemDataVisualizer {
        int rowSpanCalls;
        CountingData(Display display) {
            super(display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_BLACK), null);
        }
        @Override public int getRowSpan(GridItem item, int column) {
            rowSpanCalls++;
            return super.getRowSpan(item, column);
        }
    }

    private static class CountingGrid extends Grid {
        int topQueries;
        CountingGrid(DataVisualizer data, Shell shell) {
            super(data, shell, SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
        }
        @Override public int getTopIndex() {
            topQueries++;
            return super.getTopIndex();
        }
    }
}
