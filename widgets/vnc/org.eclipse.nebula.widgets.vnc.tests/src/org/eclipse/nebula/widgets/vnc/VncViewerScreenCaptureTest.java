/*
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/** Native DISPLAY_COPY_AREA proof, not an offscreen GC or Control.print test. */
public final class VncViewerScreenCaptureTest {
    private static Rectangle bounds(VncViewer viewer) throws Exception {
        Field field = VncViewer.class.getDeclaredField("displayedBounds");
        field.setAccessible(true);
        return (Rectangle) field.get(viewer);
    }
    private static boolean awaitPixels(Display display, VncViewer viewer, RGB left, RGB right)
            throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (System.nanoTime() < deadline) {
            while (display.readAndDispatch()) { }
            display.update();
            Rectangle frame = bounds(viewer), area = viewer.getClientArea();
            if (frame.width > 8 && frame.height > 8 && area.width > 0 && area.height > 0) {
                Point point = viewer.toDisplay(area.x, area.y);
                Image capture = new Image(display, area.width, area.height);
                try {
                    GC gc = new GC(display);
                    try {
                        gc.copyArea(capture, point.x, point.y);
                    } finally {
                        gc.dispose();
                    }
                    ImageData image = capture.getImageData();
                    int x0 = frame.x - area.x + frame.width / 4;
                    int x1 = frame.x - area.x + frame.width * 3 / 4;
                    int y = frame.y - area.y + frame.height / 2;
                    if (x0 >= 0 && x1 < image.width && y >= 0 && y < image.height
                            && left.equals(image.palette.getRGB(image.getPixel(x0, y)))
                            && right.equals(image.palette.getRGB(image.getPixel(x1, y)))) {
                        return true;
                    }
                } finally {
                    capture.dispose();
                }
            }
            Thread.sleep(20);
        }
        return false;
    }
    @Test(timeout = 20000)
    public void screenPaintsChangedAndUnchangedFramebufferPlanes() throws Exception {
        Display display = Display.getCurrent();
        boolean ownsDisplay = display == null;
        if (ownsDisplay) display = new Display();
        Shell shell = null;
        try {
            shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
            shell.setLayout(new FillLayout());
            shell.setSize(340, 260);
            VncViewer viewer = new VncViewer(shell, SWT.NONE);
            RfbFramebuffer framebuffer = new RfbFramebuffer(16, 12);
            int[] pixels = new int[16 * 12];
            for (int y = 0; y < 12; y++) {
                Arrays.fill(pixels, y * 16, y * 16 + 8, 0xffcc0000);
                Arrays.fill(pixels, y * 16 + 8, (y + 1) * 16, 0xff0033ff);
            }
            framebuffer.writePixels(0, 0, 16, 12, pixels);
            Field field = VncViewer.class.getDeclaredField("framebuffer");
            field.setAccessible(true);
            field.set(viewer, framebuffer);
            shell.open();
            viewer.redraw();
            assertTrue("real display failed initial red/blue pixels",
                    awaitPixels(display, viewer, new RGB(204, 0, 0), new RGB(0, 51, 255)));
            int[] patch = new int[8 * 12];
            Arrays.fill(patch, 0xff00aa11);
            framebuffer.writePixels(0, 0, 8, 12, patch);
            viewer.redraw();
            assertTrue("real display lost changed/unchanged planes",
                    awaitPixels(display, viewer, new RGB(0, 170, 17), new RGB(0, 51, 255)));
        } finally {
            if (shell != null && !shell.isDisposed()) shell.dispose();
            if (ownsDisplay && display != null && !display.isDisposed()) display.dispose();
        }
    }
}
