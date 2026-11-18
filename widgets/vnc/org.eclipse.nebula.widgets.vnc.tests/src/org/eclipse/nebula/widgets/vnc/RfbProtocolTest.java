/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/** Bounded, deterministic protocol tests; no sockets or live VNC server. */
public final class RfbProtocolTest {
    private static byte[] update() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(0); out.writeByte(0); out.writeShort(1);
        out.writeShort(0); out.writeShort(0); out.writeShort(2); out.writeShort(1);
        out.writeInt(0); // Raw
        out.write(new byte[] {0x11,0x22,0x33,0, (byte) 0xff,0,0,0});
        return bytes.toByteArray();
    }

    private static byte[] scriptedServer(int security, int width, int height, byte[] updates)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
        out.writeByte(1); out.writeByte(security);
        out.writeInt(0); // success result
        out.writeShort(width); out.writeShort(height);
        out.write(new byte[16]); // server pixel format
        out.writeInt(4);
        out.write("demo".getBytes(StandardCharsets.US_ASCII));
        out.write(updates);
        return bytes.toByteArray();
    }

    @Test
    public void rawAndOverlapPreservePixelsAndProtectSnapshots() throws IOException {
        RfbFramebuffer framebuffer = new RfbFramebuffer(4, 3);
        byte[] raw = {1,2,3,0,4,5,6,0,7,8,9,0,10,11,12,0};
        framebuffer.readRaw(new DataInputStream(new ByteArrayInputStream(raw)),0,0,4,1);
        assertEquals(0xff030201, framebuffer.snapshot().argb()[0]);
        framebuffer.copyRect(0, 1, 4, 1, 0, 0);
        framebuffer.copyRect(0, 2, 4, 1, 0, 1);
        assertEquals(0xff030201, framebuffer.snapshot().argb()[8]);
        framebuffer.copyRect(1, 0, 3, 1, 0, 0);
        assertEquals(0xff030201, framebuffer.snapshot().argb()[1]);
        int[] snapshot = framebuffer.snapshot().argb();
        snapshot[0] = 0;
        assertEquals(0xff030201, framebuffer.snapshot().argb()[0]);
        assertThrows(IllegalArgumentException.class, () -> framebuffer.copyRect(3,2,2,1,0,0));
        assertThrows(IllegalArgumentException.class, () -> new RfbFramebuffer(8192,8192));
    }

    @Test
    public void protocolWireUpdatesAndExplicitSecurityRefusal() throws IOException {
        byte[] bytes = scriptedServer(1, 2, 1, update());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (RfbClient client = RfbClient.open(new ByteArrayInputStream(bytes), output, true)) {
            assertEquals("demo", client.desktopName());
            assertEquals(0xff332211, client.nextFrame().argb()[0]);
            assertEquals(0xff0000ff, client.currentFrame().argb()[1]);
            client.sendPointer(1, 0, 1);
            client.sendKey(true, 0x61);
            assertTrue(output.size() > 30);
        }
        assertThrows(IOException.class,
                () -> RfbClient.open(new ByteArrayInputStream(bytes), new ByteArrayOutputStream(), false));
        byte[] incompatible = scriptedServer(2, 2, 1, update());
        assertThrows(IOException.class,
                () -> RfbClient.open(new ByteArrayInputStream(incompatible), new ByteArrayOutputStream(), true));
    }
}
