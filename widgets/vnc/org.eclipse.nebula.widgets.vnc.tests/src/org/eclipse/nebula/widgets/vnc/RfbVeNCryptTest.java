/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Test;

/** Authenticated VeNCrypt 0.2 subtype selection; no external VNC server. */
public class RfbVeNCryptTest {

    @Test public void choosesCertificateOnlyX509None() throws Exception {
        ByteArrayOutputStream outbound = new ByteArrayOutputStream();
        int subtype = RfbVeNCrypt.negotiate(input(1, 257, 260, 261),
                new DataOutputStream(outbound), false);
        assertEquals(260, subtype);
        assertArrayEquals(new byte[] {0,2,0,0,1,4}, outbound.toByteArray());
    }

    @Test public void choosesX509VncWhenPasswordIsRequired() throws Exception {
        ByteArrayOutputStream outbound = new ByteArrayOutputStream();
        int subtype = RfbVeNCrypt.negotiate(input(1, 257, 260, 261),
                new DataOutputStream(outbound), true);
        assertEquals(261, subtype);
        assertArrayEquals(new byte[] {0,2,0,0,1,5}, outbound.toByteArray());
    }

    @Test public void neverSelectsAnonymousOrUnauthenticatedSubtype() throws Exception {
        assertThrows(IOException.class, () -> negotiate(input(1, 256, 257, 258, 259), false));
        assertThrows(IOException.class, () -> negotiate(input(1, 260), true));
        assertThrows(IOException.class, () -> negotiate(input(1, 261), false));
    }

    @Test public void rejectsVersionFailureAndEmptyOffer() throws Exception {
        assertThrows(IOException.class, () -> negotiate(
                new DataInputStream(new ByteArrayInputStream(new byte[] {0,1})), false));
        assertThrows(IOException.class, () -> negotiate(
                new DataInputStream(new ByteArrayInputStream(new byte[] {0,2,1})), false));
        assertThrows(IOException.class, () -> negotiate(
                new DataInputStream(new ByteArrayInputStream(new byte[] {0,2,0,0})), false));
    }

    @Test public void rejectsSubtypeAckAndTruncatedOffer() throws Exception {
        assertThrows(IOException.class, () -> negotiate(input(0, 260), false));
        assertThrows(IOException.class, () -> negotiate(
                new DataInputStream(new ByteArrayInputStream(new byte[] {0,2,0,2,0,0,1,4})), false));
    }

    private static int negotiate(DataInputStream stream, boolean password) throws IOException {
        return RfbVeNCrypt.negotiate(stream,
                new DataOutputStream(new ByteArrayOutputStream()), password);
    }

    private static DataInputStream input(int finalAck, int... offers) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        out.writeByte(0); out.writeByte(2); // version 0.2
        out.writeByte(0); // accepts version
        out.writeByte(offers.length);
        for (int subtype : offers) out.writeInt(subtype);
        out.writeByte(finalAck);
        return new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()));
    }
}
