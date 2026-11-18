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

public class RfbHextileTest {
    @Test
    public void decodesBackgroundAndColoredSubrectangle() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            ExecutorService thread = Executors.newSingleThreadExecutor();
            try {
                Future<?> peer = thread.submit(() -> {
                    try (Socket socket = server.accept()) {
                        socket.setSoTimeout(10_000);
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
                        out.flush();
                        in.skipNBytes(12);
                        out.writeByte(1);
                        out.writeByte(1);
                        out.flush();
                        assertEquals(1, in.readUnsignedByte());
                        out.writeInt(0);
                        out.flush();
                        assertEquals(1, in.readUnsignedByte());
                        out.writeShort(2);
                        out.writeShort(1);
                        out.write(new byte[16]);
                        out.writeInt(0); // empty desktop name
                        out.flush();
                        in.skipNBytes(20); // SetPixelFormat
                        assertEquals(2, in.readUnsignedByte());
                        in.readUnsignedByte();
                        int count = in.readUnsignedShort();
                        boolean hextile = false;
                        for (int i = 0; i < count; i++) {
                            hextile |= in.readInt() == 5;
                        }
                        org.junit.Assert.assertTrue("Hextile encoding advertised", hextile);
                        in.skipNBytes(10); // first full framebuffer update request
                        out.writeByte(0);
                        out.writeByte(0);
                        out.writeShort(1);  // one rectangle
                        out.writeShort(0);
                        out.writeShort(0);
                        out.writeShort(2);
                        out.writeShort(1);
                        out.writeInt(5); // Hextile
                        out.writeByte(26); // BackgroundSpecified | AnySubrects | SubrectsColored
                        out.write(new byte[] {0, 0, (byte) 255, 0}); // background red
                        out.writeByte(1); // one colored subrect
                        out.write(new byte[] {0, (byte) 255, 0, 0}); // green
                        out.writeByte(0x10); // x=1, y=0
                        out.writeByte(0);    // width=1, height=1
                        out.flush();
                        in.skipNBytes(10); // incremental update request
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                try (Socket socket = new Socket("127.0.0.1", server.getLocalPort());
                     RfbConnection client = new RfbConnection(socket, new char[0],
                         new RfbConnection.Listener() {
                             @Override public void changed(RfbFramebuffer frame) { }
                             @Override public void bell() { }
                             @Override public void clipboard(String text) { }
                             @Override public void failed(java.io.IOException exception) { }
                         })) {
                    client.readPacket();
                    assertArrayEquals(new int[] {0xffff0000, 0xff00ff00},
                            client.framebuffer().snapshot().pixels());
                }
                peer.get(10, TimeUnit.SECONDS);
            } finally {
                thread.shutdownNow();
            }
        }
    }
}
