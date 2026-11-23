/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.drawnbrowser;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.eclipse.nebula.widgets.drawnbrowser.internal.RemoteFrameDecode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/** Real SWT presentation acceptance; requires a native display/Tycho test lane. */
public class DrawnBrowserRemoteFramePresenterTest {
    @Test public void workerDecodedFramePublishesOnlyThroughTheExistingCanvas() throws Exception {
        Display display = Display.getDefault();
        Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
        try {
            shell.setLayout(new FillLayout());
            DrawnBrowser browser = new DrawnBrowser(shell, SWT.NONE);
            browser.setText("<p>Preserved local HTML</p>");
            shell.setBounds(35, 35, 320, 240);
            shell.open();
            RemoteFramePresenter presenter = new RemoteFramePresenter(browser);
            List<String> painted = new ArrayList<>();
            browser.setRemoteFramePaintListener((epoch, revision) -> painted.add(epoch + ":" + revision));
            byte[] image = png();
            assertTrue(presenter.offer(8, 1, 200, 100, RemoteFrameDecode.Format.PNG, image));
            assertFalse(presenter.offer(8, 1, 200, 100, RemoteFrameDecode.Format.PNG, image));
            assertNull("cannot input before native paint", browser.remoteCssPoint(8, 1, 160, 120));
            pumpUntil(display, browser, () -> painted.contains("8:1"));
            assertNotNull(browser.remoteCssPoint(8, 1, 160, 120));
            assertNull("letterbox margin", browser.remoteCssPoint(8, 1, 160, 0));
            presenter.endSession();
            assertNull(browser.remoteCssPoint(8, 1, 160, 120));
            assertFalse(presenter.offer(8, 2, 200, 100, RemoteFrameDecode.Format.PNG, image));
            assertTrue(presenter.offer(9, 0, 200, 100, RemoteFrameDecode.Format.PNG, image));
            pumpUntil(display, browser, () -> painted.contains("9:0"));
            presenter.close();
            assertFalse(presenter.offer(10, 0, 200, 100, RemoteFrameDecode.Format.PNG, image));
            assertTrue(browser.getText().contains("Preserved local HTML"));
        } finally {
            shell.dispose();
        }
    }

    @Test public void aCanvasHasOneSessionOwnerAndCloseIsIdempotent() {
        Display display = Display.getDefault();
        Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
        try {
            shell.setLayout(new FillLayout());
            DrawnBrowser browser = new DrawnBrowser(shell, SWT.NONE);
            int originalDisposeListeners = browser.getListeners(SWT.Dispose).length;
            RemoteFramePresenter first = new RemoteFramePresenter(browser);
            assertEquals(originalDisposeListeners + 1, browser.getListeners(SWT.Dispose).length);
            assertThrows(IllegalStateException.class, () -> new RemoteFramePresenter(browser));
            first.close();
            first.close(); // AutoCloseable contract; does not clear another session
            assertEquals("closed presenter must not leak widget listeners",
                    originalDisposeListeners, browser.getListeners(SWT.Dispose).length);
            RemoteFramePresenter second = new RemoteFramePresenter(browser);
            assertThrows(IllegalStateException.class, () -> new RemoteFramePresenter(browser));
            second.close();
            assertEquals(originalDisposeListeners, browser.getListeners(SWT.Dispose).length);
        } finally {
            shell.dispose();
        }
    }

    @Test public void disposeRetiresPresenterAndCloseCanBeCalledAgain() {
        Display display = Display.getDefault();
        Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
        RemoteFramePresenter presenter = new RemoteFramePresenter(
                new DrawnBrowser(shell, SWT.NONE));
        shell.dispose();
        presenter.close(); // native dispose already closed the decode executor
        assertFalse(presenter.offer(1, 0, 100, 100, RemoteFrameDecode.Format.PNG, new byte[0]));
    }

    @Test public void reattachedPresenterReportsRetiredCanvasEpoch() throws Exception {
        Display display = Display.getDefault();
        Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
        try {
            shell.setLayout(new FillLayout());
            DrawnBrowser browser = new DrawnBrowser(shell, SWT.NONE);
            shell.setBounds(35, 35, 320, 240);
            shell.open();
            byte[] image = png();
            RemoteFramePresenter original = new RemoteFramePresenter(browser);
            List<String> painted = new ArrayList<>();
            browser.setRemoteFramePaintListener((epoch, revision) -> painted.add(epoch + ":" + revision));
            assertTrue(original.offer(40, 1, 200, 100, RemoteFrameDecode.Format.PNG, image));
            pumpUntil(display, browser, () -> painted.contains("40:1"));
            original.close();
            RemoteFramePresenter replacement = new RemoteFramePresenter(browser);
            List<RemoteFramePresenter.Failure> errors = new ArrayList<>();
            replacement.setFailureListener(errors::add);
            // 40 was explicitly retired on the original Canvas. The new
            // presenter must not mistake its own acceptance for a native paint.
            assertTrue(replacement.offer(40, 2, 200, 100, RemoteFrameDecode.Format.PNG, image));
            pumpUntil(display, browser, () -> !errors.isEmpty());
            assertEquals(40, errors.getFirst().epoch());
            assertEquals(2, errors.getFirst().revision());
            assertFalse(painted.contains("40:2"));
            assertTrue(replacement.offer(41, 0, 200, 100, RemoteFrameDecode.Format.PNG, image));
            pumpUntil(display, browser, () -> painted.contains("41:0"));
            replacement.close();
        } finally {
            shell.dispose();
        }
    }

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            image.setRGB(x, y, 0xff3271af);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", bytes));
        return bytes.toByteArray();
    }

    private static void pumpUntil(Display display, DrawnBrowser browser, java.util.function.BooleanSupplier done)
            throws Exception {
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (!done.getAsBoolean() && System.nanoTime() < deadline) {
            while (display.readAndDispatch()) { }
            browser.update();
            Thread.sleep(2);
        }
        assertTrue("expected SWT paint callback", done.getAsBoolean());
    }
}
