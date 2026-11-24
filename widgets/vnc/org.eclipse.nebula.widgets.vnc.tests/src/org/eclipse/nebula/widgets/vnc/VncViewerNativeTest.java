/*
 * SPDX-License-Identifier: EPL-2.0
 * Native on-screen acceptance; keep this separate from headless protocol tests.
 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/** Proves what SWT's native desktop actually painted, not only Control.print. */
public class VncViewerNativeTest {
    @Test public void capturesDecodedRfbPixelsFromTheVisibleScreen() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CountDownLatch captureComplete = new CountDownLatch(1);
            ExecutorService peer = Executors.newSingleThreadExecutor();
            // Tycho's SWT UI harness (useUIHarness) already owns the Display
            // and runs this test on its thread; only a plain JUnit run creates one.
            Display display = Display.getCurrent();
            boolean ownsDisplay = display == null;
            if (ownsDisplay) display = new Display();
            try {
                Shell shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
                try {
                    shell.setLayout(new FillLayout());
                    VncViewer viewer = new VncViewer(shell, SWT.NONE);
                    shell.setSize(220, 160);
                    shell.open();
                    Future<?> sender = peer.submit(() -> {
                        try (Socket socket = server.accept()) {
                            socket.setSoTimeout(5000);
                            DataInputStream input = new DataInputStream(socket.getInputStream());
                            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                            output.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
                            input.skipNBytes(12);
                            output.writeByte(1); output.writeByte(1); output.flush();
                            assertEquals(1, input.readUnsignedByte());
                            output.writeInt(0); output.flush();
                            assertEquals(1, input.readUnsignedByte());
                            output.writeShort(2); output.writeShort(1); output.write(new byte[16]);
                            output.writeInt(0); output.flush();
                            input.skipNBytes(20);
                            assertEquals(2, input.readUnsignedByte()); input.skipNBytes(1);
                            input.skipNBytes(input.readUnsignedShort() * 4L);
                            input.skipNBytes(10);
                            output.writeByte(0); output.writeByte(0); output.writeShort(1);
                            output.writeShort(0); output.writeShort(0);
                            output.writeShort(2); output.writeShort(1); output.writeInt(0);
                            output.write(new byte[] { 0, 0, (byte) 255, 0, 0, (byte) 255, 0, 0 });
                            output.writeByte(3); output.write(new byte[3]); output.writeInt(5);
                            output.write("ready".getBytes(StandardCharsets.US_ASCII));
                            output.flush();
                            input.skipNBytes(10);
                            // Do not turn end-of-stream into a false native-paint fence.
                            // The RFB desktop stays live until actual screen sampling finishes.
                            if (!captureComplete.await(15, TimeUnit.SECONDS)) {
                                throw new IOException("native screen capture never completed");
                            }
                        } catch (Exception error) {
                            throw new RuntimeException(error);
                        }
                    });
                    viewer.connect("127.0.0.1", server.getLocalPort(), null);
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    while (!"ready".equals(viewer.getRemoteClipboardText())
                            && System.nanoTime() < deadline) {
                        if (!display.readAndDispatch()) {
                            display.timerExec(20, () -> { });
                            display.sleep();
                        }
                    }
                    assertEquals("RFB frame/clipboard did not arrive", "ready", viewer.getRemoteClipboardText());
                    Rectangle area = viewer.getClientArea();
                    assertTrue("SWT viewer has no client area", area.width > 0 && area.height > 0);
                    Field frameField = VncViewer.class.getDeclaredField("displayedBounds");
                    frameField.setAccessible(true);
                    Point origin = viewer.toDisplay(area.x, area.y);
                    Image capture = new Image(display, area.width, area.height);
                    try {
                        boolean visible = false;
                        String observed = "no painted frame bounds";
                        // Clipboard arrival proves protocol receipt, not GUI presentation.
                        long paintDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                        while (!visible && System.nanoTime() < paintDeadline) {
                            viewer.redraw();
                            viewer.update();
                            for (int dispatched = 0; dispatched < 1000
                                    && display.readAndDispatch(); dispatched++) { /* flush SWT paint */ }
                            display.update();
                            Rectangle frame = (Rectangle) frameField.get(viewer);
                            if (frame.width >= 4 && frame.height >= 1) {
                                int x0 = frame.x - area.x + frame.width / 4;
                                int x1 = frame.x - area.x + frame.width * 3 / 4;
                                int y = frame.y - area.y + frame.height / 2;
                                if (x0 >= 0 && x1 < area.width && y >= 0 && y < area.height) {
                                    GC screen = new GC(display);
                                    try { screen.copyArea(capture, origin.x, origin.y); }
                                    finally { screen.dispose(); }
                                    ImageData pixels = capture.getImageData();
                                    org.eclipse.swt.graphics.RGB left = pixels.palette.getRGB(pixels.getPixel(x0, y));
                                    org.eclipse.swt.graphics.RGB right = pixels.palette.getRGB(pixels.getPixel(x1, y));
                                    visible = left.red == 255 && left.green == 0 && left.blue == 0
                                            && right.red == 0 && right.green == 255 && right.blue == 0;
                                    observed = "left=" + left + " right=" + right + " bounds=" + frame
                                            + " status=" + viewer.getConnectionStatus();
                                } else {
                                    observed = "painted bounds outside native client area: " + frame;
                                }
                            }
                            if (!visible) {
                                display.timerExec(30, () -> { });
                                display.sleep();
                            }
                        }
                        assertTrue("SWT visible-screen RFB pixels mismatch; " + observed, visible);
                    } finally {
                        capture.dispose();
                    }
                    captureComplete.countDown();
                    sender.get(5, TimeUnit.SECONDS);
                } finally {
                    captureComplete.countDown();
                    shell.dispose();
                }
            } finally {
                if (ownsDisplay && !display.isDisposed()) display.dispose();
                peer.shutdownNow();
            }
        }
    }
}
