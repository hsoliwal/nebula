/*
 * Copyright (c) 2026 Synexia contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package org.eclipse.nebula.widgets.opal.gradientchooser;

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
 * Visible X11/GTK screen pixels, not a control-local GC or an artificial Paint.
 * The existing color/stop and native Paint tests remain independent.
 */
public final class GradientChooserDisplayScreenTest {
    private static final RGB RED = new RGB(204, 0, 0);
    private static final RGB BLUE = new RGB(0, 51, 255);

    private static ImageData visiblePixels(GradientChooser chooser) {
        Rectangle client = chooser.getClientArea();
        assertTrue("no visible GradientChooser client", client.width > 0 && client.height > 0);
        Point origin = chooser.toDisplay(client.x, client.y);
        Image image = new Image(chooser.getDisplay(), client.width, client.height);
        try {
            GC screen = new GC(chooser.getDisplay());
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

    private static RGB at(ImageData frame, int x, int y) {
        assertTrue("sample outside actual screenshot: " + x + "," + y,
                x >= 0 && x < frame.width && y >= 0 && y < frame.height);
        return frame.palette.getRGB(frame.getPixel(x, y));
    }

    private static void assertVisibleStops(GradientChooser chooser, RGB left, RGB right) {
        ImageData frame = visiblePixels(chooser);
        Rectangle bar = chooser.getBarBounds();
        int y = bar.y + bar.height / 2;
        int leftX = chooser.xOf(0.125f);
        int rightX = chooser.xOf(0.875f);
        assertEquals("visible left constant-color gradient segment", left, at(frame, leftX, y));
        assertEquals("visible right constant-color gradient segment", right, at(frame, rightX, y));
    }

    @Test
    public void realDisplayShowsBothColorPlanesAfterStopReplacement() {
        Display display = Display.getCurrent();
        boolean ownsDisplay = display == null;
        if (ownsDisplay) display = new Display();
        Shell shell = null;
        try {
            shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
            shell.setBounds(36, 36, 330, 175);
            GradientChooser chooser = new GradientChooser(shell, SWT.NONE);
            chooser.setBounds(20, 20, 250, 55);
            chooser.setStops(new float[] { 0f, 0.25f, 0.75f, 1f },
                    new RGB[] { RED, RED, BLUE, BLUE });
            List<Rectangle> paints = TestSupport.recordPaints(chooser);
            shell.open();
            TestSupport.awaitNativePaint(chooser, paints, 1, "gradient never painted on native screen");
            assertEquals("model left color", 0xcc0000, chooser.colorAt(0.125f));
            assertEquals("model right color", 0x0033ff, chooser.colorAt(0.875f));
            assertVisibleStops(chooser, RED, BLUE);

            paints.clear();
            chooser.setStops(new float[] { 0f, 0.25f, 0.75f, 1f },
                    new RGB[] { BLUE, BLUE, RED, RED });
            TestSupport.awaitNativePaint(chooser, paints, 1, "changed gradient never repainted natively");
            assertVisibleStops(chooser, BLUE, RED);
        } finally {
            if (shell != null && !shell.isDisposed()) shell.dispose();
            if (ownsDisplay && display != null && !display.isDisposed()) display.dispose();
        }
    }
}
