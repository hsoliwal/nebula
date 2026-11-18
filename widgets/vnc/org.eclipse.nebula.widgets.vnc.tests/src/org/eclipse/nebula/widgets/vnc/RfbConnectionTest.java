/*
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
                        assertEquals(3, encodings);
                        for (int i = 0; i < encodings; i++) in.readInt();
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
}
