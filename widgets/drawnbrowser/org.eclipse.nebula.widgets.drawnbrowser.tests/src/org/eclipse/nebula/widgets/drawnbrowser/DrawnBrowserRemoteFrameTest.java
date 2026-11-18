/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Contributors to the Nebula project.
 */
package org.eclipse.nebula.widgets.drawnbrowser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/** SWT acceptance candidate for explicit remote presentation on the original Canvas. */
public class DrawnBrowserRemoteFrameTest {
    @Test
    public void remoteFramePaintHandoffAndNativeHtmlReturn() {
        Display display = Display.getDefault();
        Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
        try {
            shell.setLayout(new FillLayout());
            DrawnBrowser browser = new DrawnBrowser(shell, SWT.NONE);
            browser.setText("<p>Local HTML document</p>");
            shell.setBounds(40, 40, 320, 240);
            shell.open();
            settle(display, browser);
            List<String> painted = new ArrayList<>();
            browser.setRemoteFramePaintListener((epoch, revision) -> painted.add(epoch + ":" + revision));
            ImageData pixels = solidRed();
            assertTrue(browser.presentRemoteFrame(7, 1, 400, 200, pixels));
            assertNull("paint callback not completed yet", browser.remoteCssPoint(7, 1, 160, 120));
            browser.redraw();
            settle(display, browser);
            assertTrue("exact paint revision must be reported", painted.contains("7:1"));
            Point center = browser.remoteCssPoint(7, 1, 160, 120);
            assertNotNull(center);
            assertEquals(new Point(200, 100), center);
            assertNull("letterbox gutter", browser.remoteCssPoint(7, 1, 160, 0));
            assertFalse("old revision", browser.presentRemoteFrame(7, 1, 400, 200, pixels));
            assertTrue(browser.presentRemoteFrame(7, 2, 400, 200, pixels));
            assertNull("a new image must repaint", browser.remoteCssPoint(7, 1, 160, 120));
            browser.redraw();
            settle(display, browser);
            assertTrue(painted.contains("7:2"));
            browser.clearRemoteFrame();
            assertNull(browser.remoteCssPoint(7, 2, 160, 120));
            assertFalse("retired transport epoch", browser.presentRemoteFrame(7, 3, 400, 200, pixels));
            assertTrue("new session", browser.presentRemoteFrame(8, 0, 400, 200, pixels));
            browser.setText("<p>Back to local HTML</p>");
            settle(display, browser);
            assertTrue(browser.getText().contains("Back to local HTML"));
        } finally {
            shell.dispose();
        }
    }

    private static ImageData solidRed() {
        ImageData data = new ImageData(200, 100, 24,
                new PaletteData(0x00ff0000, 0x0000ff00, 0x000000ff));
        for (int y = 0; y < data.height; y++) {
            for (int x = 0; x < data.width; x++) data.setPixel(x, y, 0x00ff0000);
        }
        return data;
    }

    private static void settle(Display display, DrawnBrowser browser) {
        long end = System.nanoTime() + 150_000_000L;
        do {
            while (display.readAndDispatch()) { }
            browser.update();
            try { Thread.sleep(2); } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
        } while (System.nanoTime() < end);
    }
}
