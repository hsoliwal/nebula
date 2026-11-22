/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import java.io.ByteArrayInputStream;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pure Java, no-JUnit/no-Maven smoke gate for the exact RFB product sources.
 * This complements, but never replaces, Tycho/SWT native acceptance.
 */
public final class HeadlessRfbContracts {
    private HeadlessRfbContracts() { }

    public static void main(String[] args) throws Exception {
        tightFourKDesktopAsLegalRects();
        tilePaletteAndRre();
        vencryptCertificateOnly();
        richCursorPixelsAndBounds();
        boundedAtomicQueue();
        legacyVncResponse();
        inputEventsOverLoopback();
        System.out.println("NEBULA_RFB_HEADLESS=PASSED 7");
    }

    private static void tightFourKDesktopAsLegalRects() throws IOException {
        int stride = 4096;
        int[] frame = new int[stride * 2];
        byte[] red = {(byte) 0x80, 0x11, 0x22, 0x33};
        byte[] green = {(byte) 0x80, 0x44, 0x55, 0x66};
        try (RfbTightDecoder decoder = new RfbTightDecoder()) {
            decoder.decode(input(red), frame, stride, 0, 0, 2048, 2);
            decoder.decode(input(green), frame, stride, 2048, 0, 2048, 2);
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < stride; col++) {
                    check(frame[row * stride + col] ==
                            (col < 2048 ? 0x112233 : 0x445566),
                            "Incorrect split 4K Tight framebuffer");
                }
            }
            boolean rejected = false;
            try {
                decoder.decode(input(red), frame, stride, 0, 0, 2049, 2);
            } catch (IOException expected) {
                rejected = true;
            }
            check(rejected, "A single >2048px Tight rectangle must be rejected");
        }
    }

    private static void tilePaletteAndRre() throws IOException {
        int[] paletteFrame = new int[2];
        byte[] packedTile = {2, 0x33, 0x22, 0x11, 0x66, 0x55, 0x44, 0x40};
        int[] rreFrame = new int[1];
        byte[] rre = {0, 0, 0, 0, 0x33, 0x22, 0x11, 0};
        try (RfbTileDecoder decoder = new RfbTileDecoder()) {
            decoder.decodeTrle(input(packedTile), paletteFrame, 2, 0, 0, 2, 1);
            decoder.decodeRre(input(rre), rreFrame, 1, 0, 0, 1, 1);
        }
        check(Arrays.equals(paletteFrame, new int[] {0x112233, 0x445566}),
                "TRLE packed palette incorrect");
        check(rreFrame[0] == 0x112233, "RRE background incorrect");
    }

    private static void vencryptCertificateOnly() throws IOException {
        // Server: v0.2 accepted, one offered subtype X509None=260, final ACK.
        byte[] offer = {0, 2, 0, 1, 0, 0, 1, 4, 1};
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        int selected = RfbVeNCrypt.negotiate(input(offer),
                new DataOutputStream(response), false);
        check(selected == RfbVeNCrypt.X509_NONE, "VeNCrypt subtype mismatch");
        check(Arrays.equals(response.toByteArray(), new byte[] {0, 2, 0, 0, 1, 4}),
                "VeNCrypt version/subtype output mismatch");
        boolean rejected = false;
        try {
            RfbVeNCrypt.negotiate(input(offer),
                    new DataOutputStream(new ByteArrayOutputStream()), true);
        } catch (IOException expected) {
            rejected = true;
        }
        check(rejected, "VeNCrypt must not downgrade requested VNC authentication");
    }

    private static void richCursorPixelsAndBounds() throws IOException {
        byte[] packed = {0, 0, (byte) 255, 0, 0, (byte) 255, 0, 0, (byte) 0xc0};
        RfbCursorDecoder.Shape shape =
                RfbCursorDecoder.readRich(input(packed), 1, 0, 2, 1);
        check(shape.width() == 2 && shape.hotspotX() == 1,
                "RichCursor size/hotspot invalid");
        check(Arrays.equals(shape.argbCopy(), new int[] {0xffff0000, 0xff00ff00}),
                "RichCursor RGB/opacity invalid");
        boolean rejected = false;
        try {
            RfbCursorDecoder.readRich(input(packed), 0, 0, 513, 1);
        } catch (IOException expected) {
            rejected = true;
        }
        check(rejected, "Oversized RichCursor must fail closed");
    }

    private static void boundedAtomicQueue() {
        AtomicInteger bytes = new AtomicInteger();
        check(RfbClient.reserveQueueBytes(bytes, 4 * 1024 * 1024),
                "A full 4MiB queue reservation must be supported");
        check(!RfbClient.reserveQueueBytes(bytes, 1),
                "Queue must reject excess bytes");
        check(bytes.get() == 4 * 1024 * 1024,
                "Queue budget must not increase on rejection");
        check(!RfbClient.reserveQueueBytes(bytes, -1),
                "Negative queue packet length must be rejected");
    }

    private static void legacyVncResponse() throws IOException {
        byte[] challenge = new byte[16];
        byte[] answer = RfbClient.vncResponse(challenge, new char[] {'s','e','c','r','e','t'});
        check(answer.length == 16, "Legacy VNC challenge-response length");
        Arrays.fill(answer, (byte) 0);
    }

    /** Tests actual RFB 3.8 packets without SWT, Tycho, or external servers. */
    private static void inputEventsOverLoopback() throws Exception {
        try (ServerSocket server = new ServerSocket(
                    0, 1, InetAddress.getByName("127.0.0.1"))) {
            ExecutorService peerPool = Executors.newSingleThreadExecutor();
            CountDownLatch connected = new CountDownLatch(1);
            Future<?> peer = peerPool.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                    byte[] banner = new byte[12];
                    in.readFully(banner);
                    check(Arrays.equals(banner,
                            "RFB 003.008\n".getBytes(StandardCharsets.US_ASCII)),
                            "Client RFB version mismatch");
                    out.writeByte(1); // one supported security method
                    out.writeByte(1); // None; permitted on loopback only
                    out.flush();
                    check(in.readUnsignedByte() == 1, "No-security selection");
                    out.writeInt(0); // successful SecurityResult
                    out.flush();
                    check(in.readUnsignedByte() == 1, "Shared desktop flag");
                    out.writeShort(2);
                    out.writeShort(1);
                    out.write(new byte[16]); // server's pixel format
                    out.writeInt(0); // empty desktop name
                    out.flush();

                    in.skipNBytes(20); // client SetPixelFormat
                    check(in.readUnsignedByte() == 2, "SetEncodings type");
                    in.skipNBytes(1);
                    int codecs = in.readUnsignedShort();
                    check(codecs >= 7 && codecs <= 24, "Invalid encoding count");
                    in.skipNBytes(4L * codecs);
                    check(in.readUnsignedByte() == 3, "Initial FramebufferUpdateRequest");
                    in.skipNBytes(9);

                    check(in.readUnsignedByte() == 5, "PointerEvent type");
                    check(in.readUnsignedByte() == 1, "Pointer button mask");
                    check(in.readUnsignedShort() == 1 && in.readUnsignedShort() == 0,
                            "Pointer coordinates");
                    check(in.readUnsignedByte() == 4, "KeyEvent down type");
                    check(in.readUnsignedByte() == 1, "KeyEvent down state");
                    in.skipNBytes(2);
                    check(in.readInt() == 0x41, "KeyEvent down keysym");
                    check(in.readUnsignedByte() == 4, "KeyEvent up type");
                    check(in.readUnsignedByte() == 0, "KeyEvent up state");
                    in.skipNBytes(2);
                    check(in.readInt() == 0x41, "KeyEvent up keysym");
                    check(in.readUnsignedByte() == 6, "ClientCutText type");
                    in.skipNBytes(3);
                    int length = in.readInt();
                    check(length == 4, "Latin-1 clipboard byte length");
                    byte[] clipboard = new byte[length];
                    in.readFully(clipboard);
                    check("café".equals(new String(clipboard,
                            StandardCharsets.ISO_8859_1)),
                            "Latin-1 clipboard wire contents");
                } catch (Exception error) {
                    throw new AssertionError("RFB scripted loopback peer failed", error);
                }
            });
            try (RfbClient client = new RfbClient(new RfbClient.Listener() {
                @Override
                public void connected(String title, int width, int height) {
                    check(width == 2 && height == 1, "ServerInit dimensions");
                    connected.countDown();
                }
            })) {
                client.connectAsync("127.0.0.1", server.getLocalPort(), null, false);
                check(connected.await(5, TimeUnit.SECONDS), "RFB loopback handshake timeout");
                check(!client.pointer(1, 2, 0), "Pointer outside framebuffer accepted");
                check(client.pointer(1, 1, 0), "Pointer packet rejected");
                check(!client.key(true, 0), "Invalid zero keysym accepted");
                check(client.key(true, 0x41) && client.key(false, 0x41),
                        "Valid key packet rejected");
                check(client.sendClipboard("café"), "Latin-1 clipboard rejected");
                boolean refused = false;
                try {
                    client.sendClipboard("\u20ac");
                } catch (IllegalArgumentException expected) {
                    refused = true;
                }
                check(refused, "Unsupported legacy clipboard Unicode accepted");
                peer.get(6, TimeUnit.SECONDS);
            } finally {
                peerPool.shutdownNow();
            }
        }
    }

    private static DataInputStream input(byte[] bytes) {
        return new DataInputStream(new ByteArrayInputStream(bytes));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
