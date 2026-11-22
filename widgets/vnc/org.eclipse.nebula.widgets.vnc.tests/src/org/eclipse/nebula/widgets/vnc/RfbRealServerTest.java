/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assume;
import org.junit.Test;

/**
 * Real x11vnc server interoperability, not a scripted peer.
 *
 * <p>CI runs an actual X11 screen-scraping server on loopback inside the
 * same Xvfb/Openbox display. This test verifies that a server-produced RFB
 * framebuffer contains a native SWT window with its expected color.</p>
 */
public final class RfbRealServerTest {
    @Test
    public void realX11ServerDeliversNativeWindowPixels() throws Exception {
        String specifiedPort = System.getenv("NEBULA_RFB_REAL_SERVER_PORT");
        Assume.assumeTrue("Opt-in live RFB server is not configured", specifiedPort != null);
        int port = Integer.parseInt(specifiedPort);
        assertTrue("Invalid real server port", port >= 1 && port <= 65535);

        Display display = Display.getDefault();
        assertSame("Real server acceptance must use SWT's owning event thread",
                display.getThread(), Thread.currentThread());

        Shell shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
        shell.setText("Nebula RFB live-server contract");
        shell.setBackground(display.getSystemColor(SWT.COLOR_RED));
        shell.setBounds(140, 120, 250, 220);
        AtomicReference<RfbClient.Frame> latest = new AtomicReference<>();
        AtomicReference<IOException> failure = new AtomicReference<>();
        AtomicReference<String> serverTitle = new AtomicReference<>();
        RfbClient client = new RfbClient(new RfbClient.Listener() {
            @Override
            public void connected(String title, int width, int height) {
                serverTitle.set(title);
            }

            @Override
            public void frame(RfbClient.Frame snapshot) {
                latest.set(snapshot);
            }

            @Override
            public void disconnected(IOException cause) {
                if (cause != null) {
                    failure.compareAndSet(null, cause);
                }
            }
        });

        try {
            shell.open();
            shell.forceActive();
            // Let the window manager settle before asking x11vnc to scrape it.
            long settledUntil = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(350);
            while (System.nanoTime() < settledUntil) {
                pump(display);
                Thread.sleep(10);
            }
            Point center = shell.toDisplay(shell.getClientArea().width / 2,
                    shell.getClientArea().height / 2);
            assertTrue("Unexpected negative SWT screen coordinates", center.x >= 0 && center.y >= 0);

            client.connectAsync("127.0.0.1", port, null, false);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
            boolean matched = false;
            int sampled = -1;
            int receivedWidth = -1;
            int receivedHeight = -1;
            while (System.nanoTime() < deadline) {
                pump(display);
                RfbClient.Frame frame = latest.get();
                if (frame != null && center.x < frame.width() && center.y < frame.height()) {
                    receivedWidth = frame.width();
                    receivedHeight = frame.height();
                    sampled = frame.rgbForDisplay()[center.y * frame.width() + center.x];
                    if (sampled == 0xff0000) {
                        matched = true;
                        break;
                    }
                }
                Thread.sleep(25);
            }
            assertNull("Real RFB server disconnected unexpectedly", failure.get());
            assertNotNull("No RFB ServerInit was received", serverTitle.get());
            assertTrue("Real server provided no native-window framebuffer; last pixel "
                    + Integer.toHexString(sampled) + ", size " + receivedWidth + "x"
                    + receivedHeight, matched);
            System.out.println("NEBULA_RFB_REAL_SERVER=PASSED " +
                    receivedWidth + "x" + receivedHeight + " server=" + serverTitle.get());
        } finally {
            client.close();
            shell.dispose();
        }
    }

    private static void pump(Display display) {
        while (display.readAndDispatch()) {
            // Native UI thread: process pending focus, paint and layout work.
        }
    }
}
