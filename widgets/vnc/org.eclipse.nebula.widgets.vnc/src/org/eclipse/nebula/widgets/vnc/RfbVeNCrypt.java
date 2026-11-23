/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Strict VeNCrypt v0.2 pre-TLS negotiation.
 * Only X509None (260) and X509Vnc (261) are permitted. Anonymous TLS and
 * unencrypted/Plain subtypes must never be selected implicitly.
 * The caller owns upgrading the transport and its certificate validation.
 */
final class RfbVeNCrypt {
    static final int SECURITY_TYPE = 19;
    static final int X509_NONE = 260;
    static final int X509_VNC = 261;

    private RfbVeNCrypt() { }

    static int negotiate(DataInputStream input, DataOutputStream output,
            boolean passwordPresent) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        int major = input.readUnsignedByte();
        int minor = input.readUnsignedByte();
        if (major != 0 || minor < 2) {
            throw new IOException("Unsupported VeNCrypt version: " + major + "." + minor);
        }
        output.writeByte(0);
        output.writeByte(2);
        output.flush();
        if (input.readUnsignedByte() != 0) {
            throw new IOException("Server rejected VeNCrypt version 0.2");
        }
        int count = input.readUnsignedByte();
        if (count == 0) {
            throw new IOException("VeNCrypt server offered no subtypes");
        }
        int required = passwordPresent ? X509_VNC : X509_NONE;
        boolean offered = false;
        for (int i = 0; i < count; i++) {
            int subtype = input.readInt();
            if (subtype == required) {
                offered = true;
            }
        }
        if (!offered) {
            throw new IOException("No acceptable certificate-authenticated VeNCrypt subtype");
        }
        output.writeInt(required);
        output.flush();
        if (input.readUnsignedByte() != 1) {
            throw new IOException("Server rejected VeNCrypt X509 subtype");
        }
        return required;
    }
}
