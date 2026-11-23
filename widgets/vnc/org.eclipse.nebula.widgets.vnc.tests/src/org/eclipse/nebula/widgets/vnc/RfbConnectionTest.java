/*
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class RfbConnectionTest {
    @Test
    public void vncPasswordUsesBitReversedDesKey() throws Exception {
        byte[] challenge = new byte[16];
        for (int i = 0; i < challenge.length; i++) challenge[i] = (byte) i;
        // Independent OpenSSL DES-ECB vector for "secret", challenge 00..0f.
        assertArrayEquals(new byte[] {
                (byte) 0xee, 0x22, 0x53, (byte) 0x9f,
                0x33, (byte) 0xa5, (byte) 0x98, 0x3e,
                (byte) 0xc1, 0x2f, (byte) 0x9c, 0x2e,
                (byte) 0xdb, (byte) 0xc9, (byte) 0x95, (byte) 0xdd
        }, RfbConnection.vncChallengeResponse(challenge, "secret".toCharArray()));
    }

    @Test
    public void loopbackRfb38NegotiatesAndReceivesRawPixels() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            ExecutorService threads = Executors.newSingleThreadExecutor();
            try {
                Future<?> peer = threads.submit(() -> {
                    try (Socket socket = server.accept()) {
                        socket.setSoTimeout(10_000);
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
                        out.flush();
                        byte[] version = new byte[12];
                        in.readFully(version);
                        assertArrayEquals("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII), version);
                        out.writeByte(1); // number of security types
                        out.writeByte(1); // None
                        out.flush();
                        assertEquals(1, in.readUnsignedByte());
                        out.writeInt(0); // SecurityResult OK
                        out.flush();
                        assertEquals(1, in.readUnsignedByte()); // ClientInit shared
                        out.writeShort(2);
                        out.writeShort(1);
                        out.write(new byte[16]); // initial server pixel format
                        out.writeInt(4);
                        out.write("test".getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        byte[] setFormat = new byte[20];
                        in.readFully(setFormat);
                        assertEquals(0, setFormat[0]);
                        assertEquals(32, setFormat[4]);
                        assertEquals(2, in.readUnsignedByte()); // SetEncodings
                        in.readUnsignedByte(); // pad
                        int encodings = in.readUnsignedShort();
                        assertEquals(8, encodings);
                        java.util.Set<Integer> advertised = new java.util.HashSet<>();
                        for (int i = 0; i < encodings; i++) advertised.add(in.readInt());
                        org.junit.Assert.assertTrue(advertised.containsAll(
                                java.util.List.of(16, 6, 5, 2, 1, 0, -223, -224)));
                        byte[] firstRequest = new byte[10];
                        in.readFully(firstRequest);
                        assertEquals(3, firstRequest[0]);
                        assertEquals(0, firstRequest[1]); // full refresh
                        out.writeByte(0); // FramebufferUpdate
                        out.writeByte(0);
                        out.writeShort(1);
                        out.writeShort(0);
                        out.writeShort(0);
                        out.writeShort(2);
                        out.writeShort(1);
                        out.writeInt(0); // Raw
                        out.write(new byte[] {
                                0, 0, (byte) 255, 0,
                                0, (byte) 255, 0, 0
                        });
                        out.flush();
                        byte[] incremental = new byte[10];
                        in.readFully(incremental);
                        assertEquals(3, incremental[0]);
                        assertEquals(1, incremental[1]);
                    } catch (Exception failure) {
                        throw new RuntimeException(failure);
                    }
                });
                try (Socket socket = new Socket("127.0.0.1", server.getLocalPort());
                        RfbConnection connection = new RfbConnection(socket, new char[0],
                                new RfbConnection.Listener() {
                                    @Override public void changed(RfbFramebuffer f) { }
                                    @Override public void bell() { }
                                    @Override public void clipboard(String text) { }
                                    @Override public void failed(java.io.IOException error) { }
                                })) {
                    assertEquals("test", connection.serverName());
                    connection.readPacket();
                    assertArrayEquals(new int[] {0xffff0000, 0xff00ff00},
                            connection.framebuffer().snapshot().pixels());
                }
                peer.get(10, TimeUnit.SECONDS);
            } finally {
                threads.shutdownNow();
            }
        }
    }
    /**
     * A deterministic original-RFB socket fixture. It blocks halfway through
     * a packet until close() interrupts the input, without a real network.
     */
    @Test
    public void uiCloseInterruptsActivePacketAndRefusesConcurrentReaders() throws Exception {
        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        DataOutputStream greeting = new DataOutputStream(wire);
        greeting.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
        greeting.writeByte(1); // one offered authentication scheme
        greeting.writeByte(1); // None
        greeting.writeInt(0);  // successful 3.8 SecurityResult
        greeting.writeShort(1);
        greeting.writeShort(1);
        greeting.write(new byte[16]);
        greeting.writeInt(0);  // zero-length server name
        greeting.writeByte(0); // FramebufferUpdate begins
        greeting.writeByte(0); // padding, but rectangle count never arrives
        byte[] message = wire.toByteArray();
        CountDownLatch readIsBlocked = new CountDownLatch(1);
        CountDownLatch closedSocket = new CountDownLatch(1);

        Socket source = new Socket() {
            private int position;
            private final InputStream bytes = new InputStream() {
                @Override
                public int read() throws IOException {
                    if (position < message.length) return message[position++] & 0xff;
                    readIsBlocked.countDown();
                    try {
                        if (!closedSocket.await(5, TimeUnit.SECONDS)) {
                            throw new IOException("Socket did not close");
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IOException("RFB reader interrupted", interrupted);
                    }
                    return -1;
                }
            };
            private final OutputStream output = new ByteArrayOutputStream();

            @Override public InputStream getInputStream() { return bytes; }
            @Override public OutputStream getOutputStream() { return output; }
            @Override public void setSoTimeout(int timeout) { }
            @Override public void close() { closedSocket.countDown(); }
        };

        ExecutorService readers = Executors.newSingleThreadExecutor();
        try (RfbConnection connection = new RfbConnection(source, new char[0],
                new RfbConnection.Listener() {
                    @Override public void changed(RfbFramebuffer framebuffer) { }
                    @Override public void bell() { }
                    @Override public void clipboard(String text) { }
                    @Override public void failed(IOException failure) { }
                })) {
            Future<?> active = readers.submit(() ->
                    assertThrows(IOException.class, connection::readPacket));
            assertTrue("decoder must reach the incomplete packet",
                    readIsBlocked.await(5, TimeUnit.SECONDS));
            assertThrows("a second reader must not enter the same decoder",
                    IOException.class, connection::readPacket);
            long begin = System.nanoTime();
            connection.close();
            assertTrue("UI disconnect waited for socket decoding",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin) < 1500);
            active.get(5, TimeUnit.SECONDS);
            connection.close();
            assertThrows("closed decoders must refuse new packets",
                    IOException.class, connection::readPacket);
        } finally {
            closedSocket.countDown();
            readers.shutdownNow();
        }
    }

}
