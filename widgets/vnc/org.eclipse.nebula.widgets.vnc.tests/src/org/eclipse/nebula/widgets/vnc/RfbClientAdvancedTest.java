/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
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
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.junit.Test;

/** Scripted end-to-end RFB server tests for additional codecs and direct TLS. */
public class RfbClientAdvancedTest {
    @Test public void rreEndToEnd() throws Exception {
        byte[] payload = bytes(d -> {
            d.writeInt(1); pixel(d, 0x112233);
            pixel(d, 0xaabbcc); d.writeShort(1); d.writeShort(0);
            d.writeShort(1); d.writeShort(1);
        });
        runPeer(2, payload, new int[] {0x112233, 0xaabbcc});
    }

    @Test public void trleEndToEnd() throws Exception {
        byte[] payload = bytes(d -> {
            d.writeByte(2);
            compact(d, 0x112233); compact(d, 0xaabbcc);
            d.writeByte(0x40);
        });
        runPeer(15, payload, new int[] {0x112233, 0xaabbcc});
    }

    @Test public void zrleEndToEnd() throws Exception {
        byte[] tile = bytes(d -> {
            d.writeByte(2);
            compact(d, 0x112233); compact(d, 0xaabbcc);
            d.writeByte(0x40);
        });
        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        Deflater deflater = new Deflater();
        try (DeflaterOutputStream stream = new DeflaterOutputStream(zipped, deflater, 512, true)) {
            stream.write(tile);
            stream.flush();
            byte[] payload = bytes(d -> {
                d.writeInt(zipped.size());
                d.write(zipped.toByteArray());
            });
            runPeer(16, payload, new int[] {0x112233, 0xaabbcc});
        }
    }

    @Test public void tlsDoesNotDowngradeToPlaintext() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> peer = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    socket.getOutputStream().write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            });
            CountDownLatch done = new CountDownLatch(1);
            AtomicReference<IOException> failure = new AtomicReference<>();
            RfbClient client = new RfbClient(new RfbClient.Listener() {
                @Override public void disconnected(IOException cause) {
                    failure.set(cause);
                    done.countDown();
                }
            });
            try {
                client.connectTlsAsync("127.0.0.1", server.getLocalPort(), null);
                assertTrue("TLS handshake must reject a plaintext peer",
                        done.await(6, TimeUnit.SECONDS));
                assertNotNull("TLS must not downgrade", failure.get());
                peer.get(6, TimeUnit.SECONDS);
            } finally {
                client.close();
                executor.shutdownNow();
            }
        }
    }

    @Test public void noneSecurityDoesNotOverrideRequestedPassword() throws Exception {
        assertFailsBeforeSecuritySelection(false);
    }

    @Test public void vencryptCannotDowngradeToLegacySecurity() throws Exception {
        assertFailsBeforeSecuritySelection(true);
    }

    private static void assertFailsBeforeSecuritySelection(boolean vencrypt) throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CountDownLatch disconnected = new CountDownLatch(1);
            AtomicReference<IOException> failure = new AtomicReference<>();
            Future<?> peer = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                    byte[] banner = new byte[12];
                    in.readFully(banner);
                    assertEquals("RFB 003.008\n",
                            new String(banner, StandardCharsets.US_ASCII));
                    out.writeByte(1); out.writeByte(1); // only None security
                    out.flush();
                    // The client must reject the offer and close, not select 1.
                    assertEquals(-1, in.read());
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
            RfbClient client = new RfbClient(new RfbClient.Listener() {
                @Override public void disconnected(IOException cause) {
                    failure.set(cause);
                    disconnected.countDown();
                }
            });
            try {
                if (vencrypt) {
                    client.connectVeNCryptAsync("127.0.0.1", server.getLocalPort(), null);
                } else {
                    client.connectAsync("127.0.0.1", server.getLocalPort(),
                            new char[] {'t', 'e', 's', 't'}, false);
                }
                assertTrue("Must refuse unauthenticated fallback",
                        disconnected.await(6, TimeUnit.SECONDS));
                assertNotNull(failure.get());
                peer.get(6, TimeUnit.SECONDS);
            } finally {
                client.close();
                executor.shutdownNow();
            }
        }
    }

    private static void runPeer(int encoding, byte[] payload, int[] expected) throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CountDownLatch arrived = new CountDownLatch(1);
            AtomicReference<RfbClient.Frame> snapshot = new AtomicReference<>();
            Future<?> peer = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                    byte[] banner = new byte[12];
                    in.readFully(banner);
                    assertEquals("RFB 003.008\n", new String(banner, StandardCharsets.US_ASCII));
                    out.writeByte(1); out.writeByte(1); out.flush();
                    assertEquals(1, in.readUnsignedByte());
                    out.writeInt(0); out.flush();
                    assertEquals(1, in.readUnsignedByte());
                    out.writeShort(2); out.writeShort(1);
                    out.write(new byte[16]);
                    out.writeInt(4);
                    out.write("test".getBytes(StandardCharsets.ISO_8859_1));
                    out.flush();

                    in.skipNBytes(20);
                    assertEquals(2, in.readUnsignedByte());
                    in.skipNBytes(1);
                    int n = in.readUnsignedShort();
                    assertEquals(7, n);
                    int[] negotiated = new int[n];
                    for (int i = 0; i < n; i++) negotiated[i] = in.readInt();
                    assertTrue(Arrays.stream(negotiated).anyMatch(value -> value == encoding));
                    assertEquals(3, in.readUnsignedByte());
                    in.skipNBytes(9);

                    out.writeByte(0); out.writeByte(0); out.writeShort(1);
                    out.writeShort(0); out.writeShort(0); out.writeShort(2); out.writeShort(1);
                    out.writeInt(encoding);
                    out.write(payload);
                    out.flush();
                    if (!arrived.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("RFB client did not publish decoded frame");
                    }
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
            RfbClient client = new RfbClient(new RfbClient.Listener() {
                @Override public void frame(RfbClient.Frame frame) {
                    if (frame.sequence() >= 2) {
                        snapshot.set(frame);
                        arrived.countDown();
                    }
                }
            });
            try {
                client.connectAsync("127.0.0.1", server.getLocalPort(), null, false);
                assertTrue("RFB frame not received", arrived.await(6, TimeUnit.SECONDS));
                assertArrayEquals(expected, snapshot.get().rgbCopy());
                peer.get(6, TimeUnit.SECONDS);
            } finally {
                client.close();
                executor.shutdownNow();
            }
        }
    }

    private interface Write { void write(DataOutputStream stream) throws IOException; }

    private static byte[] bytes(Write writer) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        writer.write(out);
        out.flush();
        return buffer.toByteArray();
    }

    private static void compact(DataOutputStream out, int rgb) throws IOException {
        out.writeByte(rgb);
        out.writeByte(rgb >>> 8);
        out.writeByte(rgb >>> 16);
    }

    private static void pixel(DataOutputStream out, int rgb) throws IOException {
        compact(out, rgb);
        out.writeByte(0);
    }
}
