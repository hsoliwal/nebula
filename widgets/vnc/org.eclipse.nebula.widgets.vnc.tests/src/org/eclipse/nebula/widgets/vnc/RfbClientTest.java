/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class RfbClientTest {

    @Test
    public void decodesRawAndOverlappingCopyRectFromRfb38() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CountDownLatch updated = new CountDownLatch(1);
            AtomicReference<RfbClient.Frame> result = new AtomicReference<>();
            Future<?> peer = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    DataInputStream input = new DataInputStream(
                            new BufferedInputStream(socket.getInputStream()));
                    DataOutputStream output = new DataOutputStream(
                            new BufferedOutputStream(socket.getOutputStream()));
                    output.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
                    output.flush();
                    byte[] clientBanner = new byte[12];
                    input.readFully(clientBanner);
                    assertEquals("RFB 003.008\n",
                            new String(clientBanner, StandardCharsets.US_ASCII));
                    output.writeByte(1); // one offered security type
                    output.writeByte(1); // None
                    output.flush();
                    assertEquals(1, input.readUnsignedByte());
                    output.writeInt(0); // SecurityResult success
                    output.flush();
                    assertEquals(1, input.readUnsignedByte()); // shared desktop
                    output.writeShort(3);
                    output.writeShort(2);
                    output.write(new byte[16]);
                    output.writeInt(4);
                    output.write("test".getBytes(StandardCharsets.ISO_8859_1));
                    output.flush();

                    // Client set-pixel-format, set-encodings and initial update.
                    input.skipNBytes(20);
                    assertEquals(2, input.readUnsignedByte()); // SetEncodings
                    input.skipNBytes(1);
                    int count = input.readUnsignedShort();
                    assertEquals(11, count);
                    input.skipNBytes(count * 4L);
                    assertEquals(3, input.readUnsignedByte());
                    input.skipNBytes(9);

                    output.writeByte(0); // FramebufferUpdate
                    output.writeByte(0);
                    output.writeShort(65535); // LastRect ends the update; count is a placeholder
                    writeRect(output, 0, 0, 3, 1, 0);
                    writeRgb(output, 0x112233);
                    writeRgb(output, 0x445566);
                    writeRgb(output, 0x778899);
                    writeRect(output, 1, 1, 2, 1, 1);
                    output.writeShort(0); // copy source x
                    output.writeShort(0); // copy source y
                    writeRect(output, 0, 0, 0, 0, -224); // LastRect marker
                    output.flush();
                    updated.await(3, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
            RfbClient client = new RfbClient(new RfbClient.Listener() {
                @Override
                public void frame(RfbClient.Frame frame) {
                    if (frame.sequence() >= 2) {
                        result.set(frame);
                        updated.countDown();
                    }
                }
            });
            try {
                client.connectAsync("127.0.0.1", server.getLocalPort(), null, false);
                assertTrue("Expected a decoded framebuffer", updated.await(5, TimeUnit.SECONDS));
                RfbClient.Frame actual = result.get();
                assertEquals(3, actual.width());
                assertEquals(2, actual.height());
                assertEquals("test", actual.title());
                assertArrayEquals(new int[] {
                        0x112233, 0x445566, 0x778899,
                        0, 0x112233, 0x445566
                }, actual.rgbCopy());
                int[] external = actual.rgbCopy();
                external[0] = 0;
                assertEquals(0x112233, actual.rgbCopy()[0]);
                peer.get(5, TimeUnit.SECONDS);
            } finally {
                client.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void decodesHextileAndDesktopResizeFromRfb33() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CountDownLatch updated = new CountDownLatch(1);
            AtomicReference<RfbClient.Frame> result = new AtomicReference<>();
            Future<?> peer = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    DataInputStream input = new DataInputStream(
                            new BufferedInputStream(socket.getInputStream()));
                    DataOutputStream output = new DataOutputStream(
                            new BufferedOutputStream(socket.getOutputStream()));

                    output.write("RFB 003.003\n".getBytes(StandardCharsets.US_ASCII));
                    output.flush();
                    byte[] clientBanner = new byte[12];
                    input.readFully(clientBanner);
                    assertEquals("RFB 003.003\n",
                            new String(clientBanner, StandardCharsets.US_ASCII));
                    output.writeInt(1); // None, old RFB 3.3 security type
                    output.flush();
                    assertEquals(1, input.readUnsignedByte()); // shared desktop

                    output.writeShort(2);
                    output.writeShort(2);
                    output.write(new byte[16]);
                    output.writeInt(7);
                    output.write("hextile".getBytes(StandardCharsets.ISO_8859_1));
                    output.flush();

                    input.skipNBytes(20); // SetPixelFormat
                    assertEquals(2, input.readUnsignedByte());
                    input.skipNBytes(1);
                    int count = input.readUnsignedShort();
                    assertEquals(11, count);
                    input.skipNBytes(count * 4L);
                    assertEquals(3, input.readUnsignedByte()); // initial refresh
                    input.skipNBytes(9);

                    output.writeByte(0); // FramebufferUpdate
                    output.writeByte(0);
                    output.writeShort(2); // Hextile + desktop resize
                    writeRect(output, 0, 0, 2, 2, 5);
                    output.writeByte(14); // background, foreground, subrectangles
                    writeRgb(output, 0x102030); // background
                    writeRgb(output, 0x445566); // foreground
                    output.writeByte(1); // one subrectangle
                    output.writeByte(0x10); // x=1, y=0
                    output.writeByte(0x01); // width=1, height=2
                    writeRect(output, 1, 1, 3, 2, -223); // DesktopSize x/y ignored per RFC
                    output.flush();
                    assertTrue("Client did not decode Hextile", updated.await(3, TimeUnit.SECONDS));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
            RfbClient client = new RfbClient(new RfbClient.Listener() {
                @Override
                public void frame(RfbClient.Frame frame) {
                    if (frame.sequence() >= 2) {
                        result.set(frame);
                        updated.countDown();
                    }
                }
            });
            try {
                client.connectAsync("127.0.0.1", server.getLocalPort(), null, false);
                assertTrue("Expected resized framebuffer", updated.await(5, TimeUnit.SECONDS));
                RfbClient.Frame actual = result.get();
                assertEquals(3, actual.width());
                assertEquals(2, actual.height());
                assertArrayEquals(new int[] {
                        0x102030, 0x445566, 0,
                        0x102030, 0x445566, 0
                }, actual.rgbCopy());
                peer.get(5, TimeUnit.SECONDS);
            } finally {
                client.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void vncChallengeRejectsInvalidLengthAndNonBytePassword() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> RfbClient.vncResponse(new byte[15], new char[] {'a'}));
        assertThrows(java.io.IOException.class,
                () -> RfbClient.vncResponse(new byte[16], new char[] {'\u0100'}));
        assertEquals(16, RfbClient.vncResponse(new byte[16], new char[0]).length);
    }

    private static void writeRect(DataOutputStream output,
            int x, int y, int w, int h, int encoding) throws Exception {
        output.writeShort(x);
        output.writeShort(y);
        output.writeShort(w);
        output.writeShort(h);
        output.writeInt(encoding);
    }

    private static void writeRgb(DataOutputStream output, int rgb) throws Exception {
        output.writeByte(rgb);
        output.writeByte(rgb >>> 8);
        output.writeByte(rgb >>> 16);
        output.writeByte(0);
    }
}
