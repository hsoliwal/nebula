/*
 * SPDX-License-Identifier: EPL-2.0
 * Native on-screen acceptance; keep this separate from headless protocol tests.
 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
            ExecutorService peer = Executors.newSingleThreadExecutor();
            Display display = new Display();
            try {
                Shell shell = new Shell(display);
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
                    double scale = Math.min(area.width / 2.0, (double) area.height);
                    int dw = (int) Math.round(2 * scale), dh = (int) Math.round(scale);
                    int captureWidth = Math.min(100, dw), captureHeight = Math.min(32, dh);
                    Point origin = viewer.toDisplay(area.x + (area.width - captureWidth) / 2,
                            area.y + (area.height - captureHeight) / 2);
                    Image capture = new Image(display, captureWidth, captureHeight);
                    try {
                        boolean visible = false;
                        while (!visible && System.nanoTime() < deadline) {
                            viewer.redraw(); viewer.update();
                            while (display.readAndDispatch()) { /* flush SWT paint */ }
                            GC screen = new GC(display);
                            try { screen.copyArea(capture, origin.x, origin.y); }
                            finally { screen.dispose(); }
                            ImageData pixels = capture.getImageData();
                            org.eclipse.swt.graphics.RGB left = pixels.palette.getRGB(
                                    pixels.getPixel(captureWidth / 4, captureHeight / 2));
                            org.eclipse.swt.graphics.RGB right = pixels.palette.getRGB(
                                    pixels.getPixel(captureWidth * 3 / 4, captureHeight / 2));
                            visible = left.red == 255 && left.green == 0 && left.blue == 0
                                    && right.red == 0 && right.green == 255 && right.blue == 0;
                            if (!visible) {
                                display.timerExec(30, () -> { });
                                display.sleep();
                            }
                        }
                        assertTrue("SWT visible-screen pixels did not match the RFB frame", visible);
                    } finally {
                        capture.dispose();
                    }
                    sender.get(5, TimeUnit.SECONDS);
                } finally {
                    shell.dispose();
                }
            } finally {
                display.dispose();
                peer.shutdownNow();
            }
        }
    }
}
