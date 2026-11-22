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
import java.util.Arrays;
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
        System.out.println("NEBULA_RFB_HEADLESS=PASSED 6");
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

    private static DataInputStream input(byte[] bytes) {
        return new DataInputStream(new ByteArrayInputStream(bytes));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
