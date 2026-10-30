/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.nebula.widgets.grid.internal.DefaultColumnHeaderRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Test;

/** Native Grid mode-transition, renderer-state and selected-column lifetime contracts. */
public class GridCellSelectionMode_Test {
    private Shell shell;
    private Grid grid;
    private GridColumnGroup group;

    /** Releases the native widgets created by each test. */
    @After
    public void tearDown() {
        if (shell != null && !shell.isDisposed()) shell.dispose();
    }

    /** Clearing cells must also release the derived selected-column references. */
    @Test
    public void disablingReleasesDerivedColumnSelection() throws Exception {
        create(false, false);
        grid.selectCell(new Point(1, 0));
        assertEquals(1, selectedColumns().size());
        grid.setCellSelectionEnabled(false);
        assertEquals(0, grid.getCellSelectionCount());
        assertTrue("derived columns remain after disabling cell selection", selectedColumns().isEmpty());
    }

    /** Re-enabling an empty cell selection must not restore its former header highlight. */
    @Test
    public void reenableDoesNotRestoreFormerHeaderSelection() throws Exception {
        create(false, false);
        grid.selectCell(new Point(1, 0));
        settle();
        assertTrue(grid.getColumn(1).getHeaderRenderer().isSelected());
        grid.setCellSelectionEnabled(false);
        grid.setCellSelectionEnabled(true);
        settle();
        assertEquals(0, grid.getCellSelectionCount());
        assertFalse("re-enabled empty selection highlights former column",
                grid.getColumn(1).getHeaderRenderer().isSelected());
    }

    /** The ordinary header painter must explicitly reset its previous selected state. */
    @Test
    public void disablingResetsOrdinaryHeaderRenderer() throws Exception {
        create(false, false);
        grid.selectCell(new Point(1, 0));
        settle();
        assertTrue(grid.getColumn(1).getHeaderRenderer().isSelected());
        grid.setCellSelectionEnabled(false);
        settle();
        assertFalse("disabled ordinary header remains selected",
                grid.getColumn(1).getHeaderRenderer().isSelected());
    }

    /** The existing mixed-group frozen-header fallback must reset its renderer as well. */
    @Test
    public void disablingResetsFixedHeaderRenderer() throws Exception {
        create(true, true);
        grid.selectCell(new Point(0, 0));
        settle();
        grid.getHorizontalBar().setSelection(80);
        grid.getHorizontalBar().notifyListeners(SWT.Selection, new Event());
        settle();
        assertTrue(grid.getColumn(0).getHeaderRenderer().isSelected());
        grid.setCellSelectionEnabled(false);
        settle();
        assertFalse("disabled fixed header remains selected",
                grid.getColumn(0).getHeaderRenderer().isSelected());
    }

    /** A group header must follow the empty current selection after a mode transition. */
    @Test
    public void disablingResetsGroupedHeaderSelection() throws Exception {
        create(true, false);
        for (int column = 0; column < grid.getColumnCount(); ++column) {
            grid.selectCell(new Point(column, 0));
        }
        settle();
        assertTrue(group.getHeaderRenderer().isSelected());
        grid.setCellSelectionEnabled(false);
        settle();
        assertFalse("disabled group header remains selected", group.getHeaderRenderer().isSelected());
    }

    /** A subsequently removed column cannot remain rooted by the cleared selection cache. */
    @Test
    public void removingFormerlySelectedColumnLeavesNoSelectionReference() throws Exception {
        create(false, false);
        GridColumn removed = grid.getColumn(1);
        grid.selectCell(new Point(1, 0));
        grid.setCellSelectionEnabled(false);
        removed.dispose();
        assertTrue(removed.isDisposed());
        assertFalse("disposed column retained by disabled selection", selectedColumns().contains(removed));
        assertEquals(0, grid.getCellSelectionCount());
    }

    /** Mode changes preserve existing row/cell clearing behavior and emit no selection events. */
    @Test
    public void repeatedModeChangesPreservePublicSelectionAndEventSemantics() throws Exception {
        create(false, false);
        AtomicInteger selectionEvents = new AtomicInteger();
        grid.addListener(SWT.Selection, event -> selectionEvents.incrementAndGet());
        grid.addListener(SWT.DefaultSelection, event -> selectionEvents.incrementAndGet());
        grid.selectCell(new Point(1, 0));
        grid.setCellSelectionEnabled(false);
        grid.select(2);
        grid.setCellSelectionEnabled(false);
        assertArrayEquals(new int[] { 2 }, grid.getSelectionIndices());
        assertEquals(0, grid.getCellSelectionCount());
        grid.setCellSelectionEnabled(true);
        assertEquals(0, grid.getSelectionCount());
        grid.selectCell(new Point(2, 1));
        grid.setCellSelectionEnabled(true);
        assertArrayEquals(new Point[] { new Point(2, 1) }, grid.getCellSelection());
        assertEquals("programmatic mode changes must not synthesize selection events", 0, selectionEvents.get());
    }

    /** Re-enabling empty selection must not restore a classic renderer selection highlight. */
    @Test
    public void reenabledSelectionMatchesFreshUnselectedScreenPixels() throws Exception {
        create(false, false);
        // Exercise the existing selectable renderer through its public extension point.
        // The native look intentionally paints only hot/pressed/normal header states.
        grid.getColumn(1).setHeaderRenderer(new DefaultColumnHeaderRenderer());
        grid.selectCell(new Point(1, 0));
        settle();
        assertTrue(grid.getColumn(1).getHeaderRenderer().isSelected());
        grid.setCellSelectionEnabled(false);
        settle();
        grid.setCellSelectionEnabled(true);
        settle();
        String configured = System.getProperty("nebula.selection.mode.evidence.dir");
        Path evidence = configured == null ? Files.createTempDirectory("grid-selection-mode-") : Path.of(configured);
        Files.createDirectories(evidence);
        Path actualPath = evidence.resolve("reenabled-after-selection.png");
        Path expectedPath = evidence.resolve("reenabled-fresh-unselected.png");
        assertEquals(GridSwtScreenshotCapture.Method.DISPLAY_COPY_AREA,
                GridSwtScreenshotCapture.captureScreenControl(grid, actualPath).method());
        // Rebuild the independent reference through existing public selection operations.
        grid.deselectAllCells();
        settle();
        assertEquals(GridSwtScreenshotCapture.Method.DISPLAY_COPY_AREA,
                GridSwtScreenshotCapture.captureScreenControl(grid, expectedPath).method());
        ImageData actual = new ImageLoader().load(actualPath.toString())[0];
        ImageData expected = new ImageLoader().load(expectedPath.toString())[0];
        assertEquals(expected.width, actual.width);
        assertEquals(expected.height, actual.height);
        assertTrue(actual.width > 250 && actual.height > 150);
        HashSet<Integer> colors = new HashSet<>();
        long differences = 0;
        for (int y = 0; y < actual.height; ++y) {
            for (int x = 0; x < actual.width; ++x) {
                int pixel = actual.getPixel(x, y);
                colors.add(Integer.valueOf(pixel));
                if (pixel != expected.getPixel(x, y)) ++differences;
            }
        }
        assertTrue("visible capture must not be blank", colors.size() > 4);
        assertEquals("selection mode pixel mismatch", 0, differences);
    }

    private void create(boolean grouped, boolean fixed) throws Exception {
        shell = new Shell(Display.getDefault());
        shell.setLayout(new FillLayout());
        grid = new Grid(shell, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        grid.setHeaderVisible(true);
        grid.setCellSelectionEnabled(true);
        group = grouped ? new GridColumnGroup(grid, SWT.NONE) : null;
        if (group != null) group.setText("Selection group");
        for (int index = 0; index < 3; ++index) {
            GridColumn column = group == null ? new GridColumn(grid, SWT.NONE) : new GridColumn(group, SWT.NONE);
            column.setText("Column " + index);
            column.setWidth(index == 0 ? 110 : 180);
            column.setFixed(fixed && index == 0);
        }
        for (int row = 0; row < 6; ++row) {
            GridItem item = new GridItem(grid, SWT.NONE);
            for (int column = 0; column < 3; ++column) item.setText(column, "Cell " + row + "," + column);
        }
        shell.setBounds(30, 30, 370, 250);
        shell.open();
        settle();
    }

    private void settle() throws InterruptedException {
        grid.redraw();
        grid.update();
        Display display = shell.getDisplay();
        for (int pass = 0; pass < 12; ++pass) {
            while (display.readAndDispatch()) { }
            grid.update();
            Thread.sleep(10);
        }
    }

    @SuppressWarnings("unchecked")
    private List<GridColumn> selectedColumns() throws Exception {
        Field field = Grid.class.getDeclaredField("selectedColumns");
        field.setAccessible(true);
        return (List<GridColumn>) field.get(grid);
    }
}
