/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/**
 * Real native-screen test. This must run with an actual SWT display and window
 * manager; a headless decoder test cannot qualify its pixel or JNI lifecycle.
 */
public final class VncCanvasNativeTest {
    private static final int[] EXPECTED = {0xff0000, 0x00ff00, 0x0000ff, 0xffffff};
    private static final int[] PATCHED = {0x00ffff, 0x00ff00, 0x0000ff, 0xffffff};

    @Test
    public void nativeCanvasRendersDecodedPixelsAndDisposesItsResources() throws Exception {
        // Tycho's SWT UI-harness invokes tests on its Display thread.
        // JUnit @Test(timeout=...) moves the body to a worker and can
        // deadlock against Display.syncExec while JUnit waits on that worker.
        // Keep explicit bounded socket/frame deadlines inside the test
        // instead of using JUnit's separate timeout thread.
        Display display = Display.getDefault();
        AtomicReference<Throwable> failed = new AtomicReference<>();
        if (Display.getCurrent() == display) {
            runOnDisplayThread(display);
            return;
        }
        display.syncExec(() -> {
            try {
                runOnDisplayThread(display);
            } catch (Throwable error) {
                failed.set(error);
            }
        });
        Throwable error = failed.get();
        if (error instanceof Exception exception) {
            throw exception;
        }
        if (error instanceof Error fatal) {
            throw fatal;
        }
    }

    private static void runOnDisplayThread(Display display) throws Exception {
        assertEquals("Native test did not run on the SWT UI thread",
                display.getThread(), Thread.currentThread());
        ExecutorService peers = Executors.newSingleThreadExecutor();
        CountDownLatch releasePeer = new CountDownLatch(1);
        CountDownLatch requestPatch = new CountDownLatch(1);
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Future<?> serverTask = peers.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(10000);
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    output.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
                    output.flush();
                    byte[] version = new byte[12];
                    input.readFully(version);
                    assertEquals("RFB 003.008\n",
                            new String(version, StandardCharsets.US_ASCII));
                    output.writeByte(1); output.writeByte(1); output.flush();
                    assertEquals(1, input.readUnsignedByte());
                    output.writeInt(0); output.flush();
                    assertEquals(1, input.readUnsignedByte());
                    output.writeShort(2); output.writeShort(2);
                    output.write(new byte[16]);
                    output.writeInt(6);
                    output.write("native".getBytes(StandardCharsets.ISO_8859_1));
                    output.flush();

                    input.skipNBytes(20);
                    assertEquals(2, input.readUnsignedByte());
                    input.skipNBytes(1);
                    int n = input.readUnsignedShort();
                    assertEquals(11, n);
                    input.skipNBytes(4L * n);
                    assertEquals(3, input.readUnsignedByte());
                    input.skipNBytes(9);
                    output.writeByte(0);
                    output.writeByte(0);
                    output.writeShort(1);
                    output.writeShort(0); output.writeShort(0);
                    output.writeShort(2); output.writeShort(2);
                    output.writeInt(0); // raw
                    for (int rgb : EXPECTED) {
                        output.writeByte(rgb);
                        output.writeByte(rgb >>> 8);
                        output.writeByte(rgb >>> 16);
                        output.writeByte(0);
                    }
                    output.flush();
                    if (requestPatch.await(8, TimeUnit.SECONDS)) {
                        // The next incremental request must precede the patch.
                        assertEquals(3, input.readUnsignedByte());
                        input.skipNBytes(9);
                        output.writeByte(0);
                        output.writeByte(0);
                        output.writeShort(1);
                        output.writeShort(0); output.writeShort(0);
                        output.writeShort(1); output.writeShort(1);
                        output.writeInt(0); // Raw single-pixel damage
                        output.writeByte(0xff); // blue
                        output.writeByte(0xff); // green
                        output.writeByte(0x00); // red
                        output.writeByte(0x00); // pad
                        output.flush();
                    }
                    releasePeer.await(10, TimeUnit.SECONDS);
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });

            Shell shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
            shell.setLayout(new FillLayout());
            shell.setSize(240, 240);
            shell.setLocation(50, 50);
            VncCanvas canvas = new VncCanvas(shell, SWT.NONE);
            AtomicReference<IOException> failure = new AtomicReference<>();
            canvas.setSessionListener(new VncCanvas.Listener() {
                @Override public void disconnected(IOException error) {
                    if (error != null) {
                        failure.set(error);
                    }
                }
            });
            try {
                shell.open();
                shell.forceActive();
                canvas.connect("127.0.0.1", server.getLocalPort(), null, false);

                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
                int stable = 0;
                int[] actual = new int[4];
                while (System.nanoTime() < deadline && stable < 2) {
                    while (display.readAndDispatch()) {
                        // The SWT event thread owns the Canvas and all images.
                    }
                    if (canvas.getFrame() != null && canvas.getFrame().sequence() >= 2) {
                        actual = captureFromScreen(display, canvas);
                        stable = Arrays.equals(EXPECTED, actual) ? stable + 1 : 0;
                    }
                    Thread.sleep(50);
                }
                // PROBE ONLY
                System.out.println("PROBE vnc shell=" + shell.getBounds() + " client=" + shell.getClientArea()
                        + " canvas=" + canvas.getBounds() + " canvasClient=" + canvas.getClientArea()
                        + " origin=" + canvas.toDisplay(0, 0) + " frame=" + (canvas.getFrame() == null ? "null"
                        : canvas.getFrame().width() + "x" + canvas.getFrame().height() + " seq=" + canvas.getFrame().sequence())
                        + " actual=" + Arrays.toString(actual) + " stable=" + stable + " visible=" + shell.isVisible()
                        + " active=" + display.getActiveShell() + " shells=" + display.getShells().length
                        + " monitor=" + display.getPrimaryMonitor().getBounds());
                probeSaveScreen(display, shell.getBounds(), "vnc-shell-screen");
                probeSavePrint(canvas, "vnc-canvas-print");
                assertNull("RFB I/O failed", failure.get());
                assertArrayEquals("Real SWT screen must show remote pixels",
                        EXPECTED, actual);
                assertEquals("Require two settled matching captures", 2, stable);

                requestPatch.countDown();
                long updateDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                stable = 0;
                while (System.nanoTime() < updateDeadline && stable < 2) {
                    while (display.readAndDispatch()) {
                        // Deliver the incremental one-pixel update.
                    }
                    if (canvas.getFrame() != null && canvas.getFrame().sequence() >= 3) {
                        actual = captureFromScreen(display, canvas);
                        stable = Arrays.equals(PATCHED, actual) ? stable + 1 : 0;
                    }
                    Thread.sleep(50);
                }
                assertNull("RFB peer failed during partial update", failure.get());
                assertArrayEquals("Native image patch must replace only changed pixels",
                        PATCHED, actual);
                assertEquals(2, stable);
            } finally {
                requestPatch.countDown();
                releasePeer.countDown();
                if (!canvas.isDisposed()) {
                    canvas.disconnect();
                }
                shell.dispose();
            }
            serverTask.get(5, TimeUnit.SECONDS);
            assertTrue("SWT Canvas still alive after Shell disposal", canvas.isDisposed());
        } finally {
            requestPatch.countDown();
            releasePeer.countDown();
            peers.shutdownNow();
        }
    }

    /* PROBE ONLY */
    private static void probeSaveScreen(Display display, org.eclipse.swt.graphics.Rectangle area, String name) {
        Image shot = new Image(display, Math.max(1, area.width), Math.max(1, area.height));
        GC screen = new GC(display);
        try {
            screen.copyArea(shot, area.x, area.y);
        } finally {
            screen.dispose();
        }
        probeSave(shot.getImageData(), name);
        shot.dispose();
    }

    private static void probeSavePrint(org.eclipse.swt.widgets.Control control, String name) {
        Point size = control.getSize();
        Image shot = new Image(control.getDisplay(), Math.max(1, size.x), Math.max(1, size.y));
        GC gc = new GC(shot);
        try {
            System.out.println("PROBE vnc print=" + control.print(gc));
        } finally {
            gc.dispose();
        }
        probeSave(shot.getImageData(), name);
        shot.dispose();
    }

    private static void probeSave(ImageData data, String name) {
        org.eclipse.swt.graphics.ImageLoader loader = new org.eclipse.swt.graphics.ImageLoader();
        loader.data = new ImageData[] {data};
        String dir = System.getProperty("m3.test.pixels.dir", System.getProperty("java.io.tmpdir"));
        new java.io.File(dir).mkdirs();
        loader.save(dir + "/probe-" + name + ".png", SWT.IMAGE_PNG);
    }

    private static int[] captureFromScreen(Display display, VncCanvas canvas) {
        Point size = canvas.getSize();
        Point origin = canvas.toDisplay(0, 0);
        Image screenshot = new Image(display, size.x, size.y);
        try {
            GC screen = new GC(display);
            try {
                screen.copyArea(screenshot, origin.x, origin.y);
            } finally {
                screen.dispose();
            }
            ImageData data = screenshot.getImageData();
            int[][] samples = {
                    {size.x / 4, size.y / 4},
                    {3 * size.x / 4, size.y / 4},
                    {size.x / 4, 3 * size.y / 4},
                    {3 * size.x / 4, 3 * size.y / 4}
            };
            int[] values = new int[samples.length];
            for (int i = 0; i < samples.length; i++) {
                int encoded = data.getPixel(samples[i][0], samples[i][1]);
                RGB rgb = data.palette.getRGB(encoded);
                values[i] = (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
            }
            return values;
        } finally {
            screenshot.dispose();
        }
    }
}
