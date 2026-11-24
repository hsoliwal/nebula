/*
 * SPDX-License-Identifier: EPL-2.0
 *
 * The existing Nebula VNC Canvas and SWT GC/Image native presentation contract.
 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/**
 * Actual SWT Image/GC native-pixel acceptance for the existing VncViewer.
 * No socket, synthetic RFB frame dispatch, substitute renderer or screen
 * capture is used. This is an offscreen GC proof, NOT real window pixels.
 */
public final class VncViewerNativeImageTest {
    private static final int WIDTH = 8, HEIGHT = 6;
    private static final int BACKGROUND = 0xff112233;
    private static final int CORNER = 0xffe31122;
    private static final int RIGHT = 0xff3366dd;
    private static final int FIRST_PATCH = 0xff00aa11;
    private static final int SECOND_PATCH = 0xffcd2700;

    /** Inject only the detached CPU framebuffer, not a connection or OS handle. */
    private static void attach(VncViewer viewer, RfbFramebuffer framebuffer) throws Exception {
        Field field = VncViewer.class.getDeclaredField("framebuffer");
        field.setAccessible(true);
        field.set(viewer, framebuffer);
    }

    /** Inspect the original SWT Image owner without adding public/test hooks. */
    private static Image retained(VncViewer viewer) throws Exception {
        Field field = VncViewer.class.getDeclaredField("cachedImage");
        field.setAccessible(true);
        return (Image) field.get(viewer);
    }

    private static long retainedRevision(VncViewer viewer) throws Exception {
        Field field = VncViewer.class.getDeclaredField("cachedRevision");
        field.setAccessible(true);
        return field.getLong(viewer);
    }

    private static void paint(VncViewer viewer, Display display, Image target) {
        GC gc = new GC(target);
        try {
            gc.setBackground(display.getSystemColor(SWT.COLOR_MAGENTA));
            gc.fillRectangle(0, 0, WIDTH, HEIGHT);
            Event event = new Event();
            event.gc = gc;
            event.x = 0;
            event.y = 0;
            event.width = WIDTH;
            event.height = HEIGHT;
            // Trigger the original Canvas Paint listener and real GC image
            // transport, not the private paintFrame method or a fake painter.
            viewer.notifyListeners(SWT.Paint, event);
            assertFalse("caller GC must remain borrowed", gc.isDisposed());
        } finally {
            gc.dispose();
        }
    }

    private static void pixel(Image image, int x, int y, int argb) {
        ImageData data = image.getImageData();
        RGB actual = data.palette.getRGB(data.getPixel(x, y));
        RGB expected = new RGB((argb >>> 16) & 255, (argb >>> 8) & 255, argb & 255);
        assertEquals("native SWT image at " + x + "," + y, expected, actual);
    }

    @Test
    public void fullFrameThenPartialNativeImagesAndCopyRectKeepOneRetainedImage() throws Exception {
        Display display = Display.getCurrent();
        boolean ownsDisplay = display == null;
        if (ownsDisplay) display = new Display();
        Shell shell = null;
        try {
            shell = new Shell(display);
            VncViewer viewer = new VncViewer(shell, SWT.NONE);
            viewer.setSize(WIDTH, HEIGHT);
            RfbFramebuffer framebuffer = new RfbFramebuffer(WIDTH, HEIGHT);
            int[] initial = new int[WIDTH * HEIGHT];
            Arrays.fill(initial, BACKGROUND);
            initial[0] = CORNER;
            initial[WIDTH - 1] = RIGHT;
            framebuffer.writePixels(0, 0, WIDTH, HEIGHT, initial);
            attach(viewer, framebuffer);

            Image target = new Image(display, WIDTH, HEIGHT);
            try {
                paint(viewer, display, target);
                Image owner = retained(viewer);
                assertTrue("initial native image must exist", owner != null);
                assertFalse(owner.isDisposed());
                assertEquals(framebuffer.revision(), retainedRevision(viewer));
                pixel(target, 0, 0, CORNER);
                pixel(target, WIDTH - 1, 0, RIGHT);
                pixel(target, 2, 2, BACKGROUND);

                // One CPU dirty rectangle must patch the same SWT native Image,
                // not allocate a fresh full-screen Image or lose other pixels.
                framebuffer.writePixels(3, 2, 2, 1, new int[] {FIRST_PATCH, SECOND_PATCH});
                paint(viewer, display, target);
                assertSame("partial update retains SWT image ownership", owner, retained(viewer));
                assertEquals(framebuffer.revision(), retainedRevision(viewer));
                pixel(target, 3, 2, FIRST_PATCH);
                pixel(target, 4, 2, SECOND_PATCH);
                pixel(target, 0, 0, CORNER);
                pixel(target, 7, 0, RIGHT);
                pixel(target, 2, 2, BACKGROUND);

                // CopyRect changes only the destination; the existing native
                // cached image must again receive only that patch.
                framebuffer.copyRect(4, 3, 2, 1, 3, 2);
                paint(viewer, display, target);
                assertSame("CopyRect keeps retained native image", owner, retained(viewer));
                assertEquals(framebuffer.revision(), retainedRevision(viewer));
                pixel(target, 4, 3, FIRST_PATCH);
                pixel(target, 5, 3, SECOND_PATCH);
                pixel(target, 6, 3, BACKGROUND);
                pixel(target, 3, 2, FIRST_PATCH);

                // The original widget owns the cached Image, but not the
                // caller's GC/Image proof surface or detached CPU framebuffer.
                viewer.disconnect();
                assertTrue("disconnection disposes cached image", owner.isDisposed());
                assertNull(retained(viewer));
                assertFalse("caller image must remain live", target.isDisposed());
                assertEquals(FIRST_PATCH, framebuffer.snapshot().pixels()[2 * WIDTH + 3]);
            } finally {
                target.dispose();
            }
        } finally {
            if (shell != null && !shell.isDisposed()) shell.dispose();
            if (ownsDisplay && display != null && !display.isDisposed()) display.dispose();
        }
    }
    /**
     * A scaled remote two-color framebuffer must not blend adjoining cells.
     * This uses the real SWT.Paint listener and a native offscreen Image/GC.
     * It deliberately does not claim actual GC(Display) screen pixels.
     */
    @Test
    public void scaledRfbPixelsStayExactAndBorrowedInterpolationIsRestored() throws Exception {
        Display display = Display.getCurrent();
        boolean ownsDisplay = display == null;
        if (ownsDisplay) display = new Display();
        Shell shell = null;
        try {
            shell = new Shell(display);
            VncViewer viewer = new VncViewer(shell, SWT.NONE);
            viewer.setSize(220, 160);
            RfbFramebuffer framebuffer = new RfbFramebuffer(2, 1);
            framebuffer.writePixels(0, 0, 2, 1, new int[] {0xffff0000, 0xff00ff00});
            attach(viewer, framebuffer);
            Image target = new Image(display, 220, 160);
            try {
                GC gc = new GC(target);
                try {
                    gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
                    gc.fillRectangle(0, 0, 220, 160);
                    gc.setInterpolation(SWT.HIGH);
                    Event event = new Event();
                    event.gc = gc;
                    event.x = 0;
                    event.y = 0;
                    event.width = 220;
                    event.height = 160;
                    viewer.notifyListeners(SWT.Paint, event);
                    assertEquals("caller interpolation must be restored",
                            SWT.HIGH, gc.getInterpolation());
                    assertFalse("caller GC must not be disposed", gc.isDisposed());
                } finally {
                    gc.dispose();
                }
                // Two source cells, fitted to 220x110, centered at y=25.
                // The source-cell boundary must remain pure red/green.
                pixel(target, 109, 80, 0xffff0000);
                pixel(target, 110, 80, 0xff00ff00);
                pixel(target, 0, 80, 0xffff0000);
                pixel(target, 219, 80, 0xff00ff00);
            } finally {
                target.dispose();
            }
        } finally {
            if (shell != null && !shell.isDisposed()) shell.dispose();
            if (ownsDisplay && display != null && !display.isDisposed()) display.dispose();
        }
    }

}
