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
