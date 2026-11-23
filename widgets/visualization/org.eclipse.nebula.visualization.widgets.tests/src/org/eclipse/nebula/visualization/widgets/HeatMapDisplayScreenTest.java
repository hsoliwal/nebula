/*
 * Copyright (c) 2026 Synexia contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package org.eclipse.nebula.visualization.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/**
 * Native display-surface acceptance for visible HeatMap cells. Uses GC(Display),
 * not GC(Control), Control.print, synthetic Paint events or a reference PNG.
 */
public final class HeatMapDisplayScreenTest {
    private static final RGB RED = new RGB(204, 0, 0);
    private static final RGB BLUE = new RGB(0, 51, 255);

    private static ImageData visiblePixels(HeatMap map) {
        Rectangle client = map.getClientArea();
        assertTrue("no visible HeatMap client", client.width > 0 && client.height > 0);
        Point origin = map.toDisplay(client.x, client.y);
        Image image = new Image(map.getDisplay(), client.width, client.height);
        try {
            GC screen = new GC(map.getDisplay());
            try {
                screen.copyArea(image, origin.x, origin.y);
            } finally {
                screen.dispose();
            }
            return image.getImageData();
        } finally {
            image.dispose();
        }
    }

    private static void assertCell(ImageData image, int column, int row, RGB expected) {
        int x = column * 64 + 32;
        int y = row * 48 + 24;
        assertTrue("cell center outside actual screenshot: " + x + "," + y,
                x < image.width && y < image.height);
        RGB actual = image.palette.getRGB(image.getPixel(x, y));
        assertEquals("real screen pixel at cell " + row + "," + column, expected, actual);
    }

    @Test
    public void realDisplayShowsChangedAndUnchangedCellPlanes() {
        Display display = Display.getCurrent();
        boolean ownsDisplay = display == null;
        if (ownsDisplay) display = new Display();
        Shell shell = null;
        try {
            shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
            shell.setBounds(36, 36, 290, 220);
            HeatMap map = new HeatMap(shell, SWT.NONE);
            map.setBounds(20, 20, 150, 120);
            map.setGridSize(2, 2);
            map.setCellSize(64, 48);
            map.setPalette(new RGB[] { RED, BLUE });
            map.setRange(0f, 1f);
            map.setValues(new float[] { 0f, 1f, 1f, 0f });
            List<Rectangle> paints = TestSupport.recordPaints(map);
            shell.open();
            TestSupport.awaitNativePaint(map, paints, 1, "initial HeatMap never painted on native screen");
            assertEquals("source model red", 0xcc0000, map.colorOf(0f));
            assertEquals("source model blue", 0x0033ff, map.colorOf(1f));
            ImageData initial = visiblePixels(map);
            assertCell(initial, 0, 0, RED);
            assertCell(initial, 1, 0, BLUE);
            assertCell(initial, 0, 1, BLUE);
            assertCell(initial, 1, 1, RED);

            paints.clear();
            map.setValue(0, 0, 1f);
            TestSupport.awaitNativePaint(map, paints, 1, "changed HeatMap cell never repainted natively");
            ImageData changed = visiblePixels(map);
            assertCell(changed, 0, 0, BLUE);
            assertCell(changed, 1, 0, BLUE);
            assertCell(changed, 0, 1, BLUE);
            assertCell(changed, 1, 1, RED);
        } finally {
            if (shell != null && !shell.isDisposed()) shell.dispose();
            if (ownsDisplay && display != null && !display.isDisposed()) display.dispose();
        }
    }
}
