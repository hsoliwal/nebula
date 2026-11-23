/*
 * SPDX-License-Identifier: EPL-2.0
 * RFB wire fixtures grounded in IETF RFC 6143 and Sequoyah's encoding families.
 */
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Deflater;

import org.junit.Test;

public class RfbAdvancedEncodingsTest {
    private static final int RED = 0xffff0000, GREEN = 0xff00ff00, BLUE = 0xff0000ff;

    private interface Probe { void check(RfbConnection connection) throws Exception; }

    /** Real loopback sockets keep the original RfbConnection/API under test. */
    private static void exchange(int protocol, byte[] updates, int packets, Probe probe) throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            ExecutorService worker = Executors.newSingleThreadExecutor();
            try {
                Future<?> peer = worker.submit(() -> {
                    try (Socket socket = server.accept()) {
                        socket.setSoTimeout(5000);
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                        output.write(("RFB 003.00" + protocol + "\n").getBytes(StandardCharsets.US_ASCII));
                        byte[] greeting = new byte[12]; input.readFully(greeting);
                        assertEquals("RFB 003.00" + protocol + "\n",
                                new String(greeting, StandardCharsets.US_ASCII));
                        if (protocol == 3) output.writeInt(1);
                        else {
                            output.writeByte(1); output.writeByte(1); output.flush();
                            assertEquals(1, input.readUnsignedByte());
                            if (protocol == 8) output.writeInt(0);
                        }
                        output.flush();
                        assertEquals(1, input.readUnsignedByte()); // ClientInit
                        output.writeShort(3); output.writeShort(2); output.write(new byte[16]);
                        output.writeInt(0); output.flush();
                        assertEquals(0, input.readUnsignedByte()); input.skipNBytes(19);
                        assertEquals(2, input.readUnsignedByte()); input.skipNBytes(1);
                        int count = input.readUnsignedShort();
                        assertEquals(8, count);
                        input.skipNBytes(count * 4L);
                        input.skipNBytes(10); // initial full update request
                        output.write(updates); output.flush();
                        for (int i = 0; i < packets; i++) {
                            byte[] request = new byte[10]; input.readFully(request);
                            assertEquals(3, request[0]);
                        }
                    } catch (Exception failure) {
                        throw new RuntimeException(failure);
                    }
                });
                try (Socket socket = new Socket("127.0.0.1", server.getLocalPort());
                        RfbConnection connection = new RfbConnection(socket, new char[0],
                                new RfbConnection.Listener() {
                                    @Override public void changed(RfbFramebuffer frame) { }
                                    @Override public void bell() { }
                                    @Override public void clipboard(String text) { }
                                    @Override public void failed(IOException failure) { }
                                })) {
                    probe.check(connection);
                }
                peer.get(10, TimeUnit.SECONDS);
            } finally {
                worker.shutdownNow();
            }
        }
    }

    private static void packet(DataOutputStream out, int count) throws IOException {
        out.writeByte(0); out.writeByte(0); out.writeShort(count);
    }

    private static void rect(DataOutputStream out, int x, int y, int w, int h, int encoding) throws IOException {
        out.writeShort(x); out.writeShort(y); out.writeShort(w); out.writeShort(h); out.writeInt(encoding);
    }

    private static void pixel(DataOutputStream out, int argb) throws IOException {
        out.writeByte(argb & 255); out.writeByte(argb >>> 8 & 255);
        out.writeByte(argb >>> 16 & 255); out.writeByte(0);
    }

    private static void compact(DataOutputStream out, int argb) throws IOException {
        out.writeByte(argb & 255); out.writeByte(argb >>> 8 & 255);
        out.writeByte(argb >>> 16 & 255);
    }

    private static byte[] zip(byte[] bytes) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(result)) {
            deflater.write(bytes);
        }
        return result.toByteArray();
    }

    @Test public void protocol37NoneHasNoSecurityResult() throws Exception {
        exchange(7, new byte[0], 0, client -> {
            assertEquals(3, client.framebuffer().width());
            assertEquals(2, client.framebuffer().height());
        });
    }

    @Test public void protocol33NoneRetainsItsOriginalWireHandshake() throws Exception {
        exchange(3, new byte[0], 0, client -> assertEquals(3, client.framebuffer().width()));
    }

    @Test public void zlibKeepsOneDictionaryAcrossIncrementalUpdates() throws Exception {
        ByteArrayOutputStream continuous = new ByteArrayOutputStream();
        byte[] first, second;
        try (DeflaterOutputStream zip = new DeflaterOutputStream(
                continuous, new Deflater(), 512, true)) {
            zip.write(new byte[] {0, 0, (byte) 255, 0}); zip.flush();
            first = continuous.toByteArray();
            zip.write(new byte[] {0, (byte) 255, 0, 0}); zip.flush();
            byte[] both = continuous.toByteArray();
            second = Arrays.copyOfRange(both, first.length, both.length);
        }
        ByteArrayOutputStream messages = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(messages);
        packet(out, 1); rect(out, 0, 0, 1, 1, 6); out.writeInt(first.length); out.write(first);
        packet(out, 1); rect(out, 1, 0, 1, 1, 6); out.writeInt(second.length); out.write(second);
        exchange(8, messages.toByteArray(), 2, client -> {
            client.readPacket(); client.readPacket();
            assertArrayEquals(new int[] {RED, GREEN, 0xff000000},
                    Arrays.copyOf(client.framebuffer().snapshot().pixels(), 3));
        });
    }

    @Test public void rreZlibAndZrlePreserveTheSameFrameOwner() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        packet(out, 1); rect(out, 0, 0, 3, 2, 2); // RRE
        out.writeInt(1); pixel(out, BLUE); pixel(out, RED);
        out.writeShort(1); out.writeShort(0); out.writeShort(1); out.writeShort(2);
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream rawOut = new DataOutputStream(raw);
        pixel(rawOut, RED); pixel(rawOut, GREEN); pixel(rawOut, BLUE);
        byte[] zlib = zip(raw.toByteArray());
        packet(out, 1); rect(out, 0, 0, 3, 1, 6);
        out.writeInt(zlib.length); out.write(zlib);
        ByteArrayOutputStream tiles = new ByteArrayOutputStream();
        DataOutputStream tileOut = new DataOutputStream(tiles);
        tileOut.writeByte(0); compact(tileOut, BLUE); compact(tileOut, GREEN); compact(tileOut, RED);
        byte[] zrle = zip(tiles.toByteArray());
        packet(out, 1); rect(out, 0, 1, 3, 1, 16);
        out.writeInt(zrle.length); out.write(zrle);
        exchange(8, bytes.toByteArray(), 3, client -> {
            RfbFramebuffer owner = client.framebuffer();
            client.readPacket();
            assertArrayEquals(new int[] {BLUE, RED, BLUE, BLUE, RED, BLUE}, owner.snapshot().pixels());
            client.readPacket();
            assertArrayEquals(new int[] {RED, GREEN, BLUE, BLUE, RED, BLUE}, owner.snapshot().pixels());
            client.readPacket();
            assertArrayEquals(new int[] {RED, GREEN, BLUE, BLUE, GREEN, RED}, owner.snapshot().pixels());
            assertSame(owner, client.framebuffer());
        });
    }

    @Test public void zrlePackedPaletteAndPaletteRuns() throws Exception {
        for (int mode = 0; mode < 2; mode++) {
            ByteArrayOutputStream tile = new ByteArrayOutputStream();
            DataOutputStream tileOut = new DataOutputStream(tile);
            if (mode == 0) {
                tileOut.writeByte(2); compact(tileOut, RED); compact(tileOut, GREEN);
                tileOut.writeByte(0x40); tileOut.writeByte(0xc0); // 010 / 110
            } else {
                tileOut.writeByte(130); compact(tileOut, RED); compact(tileOut, GREEN);
                tileOut.writeByte(0x80); tileOut.writeByte(2); // three red
                tileOut.writeByte(0x81); tileOut.writeByte(2); // three green
            }
            byte[] zrle = zip(tile.toByteArray());
            ByteArrayOutputStream messages = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(messages);
            packet(out, 1); rect(out, 0, 0, 3, 2, 16);
            out.writeInt(zrle.length); out.write(zrle);
            final int variant = mode;
            exchange(8, messages.toByteArray(), 1, client -> {
                client.readPacket();
                assertArrayEquals(variant == 0
                        ? new int[] {RED, GREEN, RED, GREEN, GREEN, RED}
                        : new int[] {RED, RED, RED, GREEN, GREEN, GREEN},
                        client.framebuffer().snapshot().pixels());
            });
        }
    }

    @Test public void desktopResizeAndLastRectUseFullRefresh() throws Exception {
        ByteArrayOutputStream messages = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(messages);
        packet(out, 2); rect(out, 0, 0, 4, 3, -223); rect(out, 0, 0, 0, 0, -224);
        exchange(8, messages.toByteArray(), 1, client -> {
            client.readPacket();
            assertEquals(4, client.framebuffer().width());
            assertEquals(3, client.framebuffer().height());
        });
    }

    @Test public void maliciousRreSubrectangleCountIsRejectedBeforeMutation() throws Exception {
        ByteArrayOutputStream messages = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(messages);
        packet(out, 1); rect(out, 0, 0, 3, 2, 2); out.writeInt(Integer.MAX_VALUE);
        // This packet fails before sending an incremental update request.
        exchange(8, messages.toByteArray(), 0, client -> {
            assertThrows(IOException.class, client::readPacket);
            assertArrayEquals(new int[6], java.util.Arrays.stream(
                    client.framebuffer().snapshot().pixels()).map(v -> v & 0xffffff).toArray());
        });
    }

    @Test public void rrePublishesExactExistingDamageWithoutASecondFramebuffer() throws Exception {
        ByteArrayOutputStream messages = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(messages);
        packet(out, 1);
        rect(out, 1, 0, 2, 1, 2);
        out.writeInt(1); pixel(out, BLUE);
        pixel(out, RED); out.writeShort(0); out.writeShort(0);
        out.writeShort(1); out.writeShort(1);
        exchange(8, messages.toByteArray(), 1, connection -> {
            RfbFramebuffer frame = connection.framebuffer();
            long seen = frame.snapshotSince(-1).revision();
            connection.readPacket();
            RfbFramebuffer.FrameUpdate patch = frame.snapshotSince(seen);
            assertFalse(patch.complete());
            assertEquals(1, patch.x());
            assertEquals(0, patch.y());
            assertArrayEquals(new int[] {RED, BLUE}, patch.pixels());
            assertSame(frame, connection.framebuffer());
        });
    }

    @Test public void malformedRreSubrectangleDoesNotPublishPartialBackground() throws Exception {
        ByteArrayOutputStream messages = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(messages);
        packet(out, 1); rect(out, 0, 0, 3, 2, 2);
        out.writeInt(1); pixel(out, BLUE);
        pixel(out, RED); out.writeShort(2); out.writeShort(1);
        out.writeShort(2); out.writeShort(2); // subrect extends outside 3x2
        exchange(8, messages.toByteArray(), 0, connection -> {
            RfbFramebuffer frame = connection.framebuffer();
            long seen = frame.revision();
            assertThrows(IOException.class, connection::readPacket);
            assertEquals(seen, frame.revision());
            assertArrayEquals(new int[] {0xff000000, 0xff000000, 0xff000000,
                    0xff000000, 0xff000000, 0xff000000}, frame.snapshot().pixels());
        });
    }
    @Test public void trailingZrleBytesDoNotPublishDecodedPixelsOrDamage() throws Exception {
        ByteArrayOutputStream tile = new ByteArrayOutputStream();
        DataOutputStream tileOut = new DataOutputStream(tile);
        tileOut.writeByte(1); compact(tileOut, RED);
        tileOut.writeByte(99); // malformed extra byte after an otherwise valid solid tile
        byte[] compressed = zip(tile.toByteArray());
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(message);
        packet(out, 1); rect(out, 0, 0, 3, 2, 16);
        out.writeInt(compressed.length); out.write(compressed);
        exchange(8, message.toByteArray(), 0, client -> {
            RfbFramebuffer frame = client.framebuffer();
            long previousRevision = frame.revision();
            int[] originalPixels = frame.snapshot().pixels();
            assertThrows(IOException.class, client::readPacket);
            assertEquals("malformed ZRLE cannot publish a partial tile",
                    previousRevision, frame.revision());
            assertArrayEquals("malformed ZRLE must retain original pixels",
                    originalPixels, frame.snapshot().pixels());
        });
    }

    @Test public void twoZrleTilesCommitExactlyOneFullRectangleDamage() throws Exception {
        ByteArrayOutputStream tiles = new ByteArrayOutputStream();
        DataOutputStream tileOut = new DataOutputStream(tiles);
        tileOut.writeByte(1); compact(tileOut, RED); // 64 x 2
        tileOut.writeByte(1); compact(tileOut, GREEN); // 1 x 2
        byte[] compressed = zip(tiles.toByteArray());
        ByteArrayOutputStream messages = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(messages);
        packet(out, 1); rect(out, 0, 0, 65, 2, -223); // negotiated DesktopSize
        packet(out, 1); rect(out, 0, 0, 65, 2, 16);
        out.writeInt(compressed.length); out.write(compressed);
        exchange(8, messages.toByteArray(), 2, client -> {
            RfbFramebuffer frame = client.framebuffer();
            client.readPacket(); // resize to 65 x 2
            long previousRevision = frame.revision();
            client.readPacket(); // two tiles should be one atomic RFB rectangle
            assertEquals(previousRevision + 1, frame.revision());
            int[] pixels = frame.snapshot().pixels();
            assertEquals(130, pixels.length);
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 65; col++) {
                    assertEquals("tile pixel " + row + "," + col,
                            col < 64 ? RED : GREEN, pixels[row * 65 + col]);
                }
            }
        });
    }

    @Test public void malformedSecondHextileTileCannotPublishFirstTile() throws Exception {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(message);
        packet(out, 1); rect(out, 0, 0, 17, 2, -223); // make two 16-column tiles
        packet(out, 1); rect(out, 0, 0, 17, 2, 5);
        out.writeByte(2); pixel(out, RED); // first 16x2 tile decodes successfully
        out.writeByte(0x20); // second tile has reserved Hextile flag
        exchange(8, message.toByteArray(), 1, client -> {
            client.readPacket();
            RfbFramebuffer frame = client.framebuffer();
            long originalRevision = frame.revision();
            int[] originalPixels = frame.snapshot().pixels();
            assertThrows(IOException.class, client::readPacket);
            assertEquals("no partial first tile or damage revision",
                    originalRevision, frame.revision());
            assertArrayEquals(originalPixels, frame.snapshot().pixels());
        });
    }

    @Test public void validTwoHextileTilesPublishExactlyOneDamageRevision() throws Exception {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(message);
        packet(out, 1); rect(out, 0, 0, 17, 2, -223);
        packet(out, 1); rect(out, 0, 0, 17, 2, 5);
        out.writeByte(2); pixel(out, RED); // 16x2
        out.writeByte(2); pixel(out, GREEN); // 1x2
        exchange(8, message.toByteArray(), 2, client -> {
            client.readPacket();
            RfbFramebuffer frame = client.framebuffer();
            long before = frame.revision();
            client.readPacket();
            assertEquals(before + 1, frame.revision());
            int[] pixels = frame.snapshot().pixels();
            assertEquals(34, pixels.length);
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 17; col++) {
                    assertEquals("pixel " + row + "," + col,
                            col < 16 ? RED : GREEN, pixels[row * 17 + col]);
                }
            }
        });
    }

    @Test public void rawHextileTileAndSolidTileUseOriginalPixelFormat() throws Exception {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(message);
        packet(out, 1); rect(out, 0, 0, 17, 1, -223);
        packet(out, 1); rect(out, 0, 0, 17, 1, 5);
        out.writeByte(1); // 16 raw 32-bit little endian B,G,R,pad pixels
        for (int i = 0; i < 16; i++) pixel(out, RED);
        out.writeByte(2); pixel(out, GREEN); // final 1x1 solid tile
        exchange(8, message.toByteArray(), 2, client -> {
            client.readPacket();
            RfbFramebuffer frame = client.framebuffer();
            long before = frame.revision();
            client.readPacket();
            assertEquals(before + 1, frame.revision());
            int[] pixels = frame.snapshot().pixels();
            assertEquals(17, pixels.length);
            for (int i = 0; i < 17; i++) {
                assertEquals(i < 16 ? RED : GREEN, pixels[i]);
            }
        });
    }

}
