/*
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.vnc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Client-side RFB 3.8 session on caller-supplied, already-connected streams.
 * The host owns TLS/SSH, host identity, credentials, and connection policy.
 * Only RFB security type None is supported and must be explicitly enabled.
 * The client never opens sockets, discovers hosts, or forwards input implicitly.
 */
public final class RfbClient implements AutoCloseable {
    private static final byte[] RFB38 = "RFB 003.008\n".getBytes(StandardCharsets.US_ASCII);
    private final DataInputStream input;
    private final DataOutputStream output;
    private final Object outputLock = new Object();
    private final RfbFramebuffer framebuffer;
    private final String desktopName;
    private volatile boolean closed;

    private RfbClient(InputStream in, OutputStream out, boolean allowNone) throws IOException {
        input = new DataInputStream(Objects.requireNonNull(in, "in"));
        output = new DataOutputStream(Objects.requireNonNull(out, "out"));
        byte[] version = new byte[12];
        input.readFully(version);
        if (!Arrays.equals(version, RFB38)) {
            throw new IOException("unsupported RFB version; only 3.8 is implemented");
        }
        synchronized (outputLock) {
            output.write(RFB38);
            output.flush();
        }
        int count = input.readUnsignedByte();
        if (count == 0) {
            throw new IOException("server refused RFB security negotiation");
        }
        boolean serverOffersNone = false;
        for (int i = 0; i < count; i++) {
            serverOffersNone |= input.readUnsignedByte() == 1;
        }
        if (!allowNone || !serverOffersNone) {
            throw new IOException("RFB None security not explicitly approved or not offered");
        }
        synchronized (outputLock) {
            output.writeByte(1);
            output.flush();
        }
        int securityResult = input.readInt();
        if (securityResult != 0) {
            throw new IOException("RFB security handshake failed");
        }
        synchronized (outputLock) {
            output.writeByte(1); // shared desktop
            output.flush();
        }
        int width = input.readUnsignedShort();
        int height = input.readUnsignedShort();
        byte[] serverPixelFormat = new byte[16];
        input.readFully(serverPixelFormat);
        long nameLength = Integer.toUnsignedLong(input.readInt());
        if (nameLength > 4096) {
            throw new IOException("RFB server name exceeds limit");
        }
        byte[] name = new byte[(int) nameLength];
        input.readFully(name);
        desktopName = new String(name, StandardCharsets.UTF_8);
        try {
            framebuffer = new RfbFramebuffer(width, height);
        } catch (IllegalArgumentException e) {
            throw new IOException("invalid RFB server framebuffer", e);
        }
        synchronized (outputLock) {
            output.writeByte(0); // SetPixelFormat
            output.write(new byte[3]);
            output.writeByte(32); // bpp
            output.writeByte(24); // depth
            output.writeByte(0); // little endian
            output.writeByte(1); // true colour
            output.writeShort(255); // red max
            output.writeShort(255); // green max
            output.writeShort(255); // blue max
            output.writeByte(16); // red shift
            output.writeByte(8); // green shift
            output.writeByte(0); // blue shift
            output.write(new byte[3]);
            output.writeByte(2); // SetEncodings
            output.writeByte(0);
            output.writeShort(3);
            output.writeInt(0); // Raw
            output.writeInt(1); // CopyRect
            output.writeInt(-223); // DesktopSize
            requestUpdateLocked(false);
        }
    }

    /** Establish the RFB handshake only after the caller explicitly chooses its security policy. */
    public static RfbClient open(InputStream in, OutputStream out, boolean allowUnauthenticatedNone)
            throws IOException {
        return new RfbClient(in, out, allowUnauthenticatedNone);
    }

    public String desktopName() {
        return desktopName;
    }

    public RfbFramebuffer.Frame currentFrame() {
        return framebuffer.snapshot();
    }

    /** Read the next completed framebuffer update; Bell and clipboard messages are ignored. */
    public RfbFramebuffer.Frame nextFrame() throws IOException {
        while (!closed) {
            int message = input.readUnsignedByte();
            if (message == 2) { // Bell
                continue;
            }
            if (message == 3) { // ServerCutText: never auto-install remote clipboard
                input.skipNBytes(3);
                long length = Integer.toUnsignedLong(input.readInt());
                if (length > 65_536) {
                    throw new IOException("RFB clipboard exceeds limit");
                }
                input.skipNBytes(length);
                continue;
            }
            if (message != 0) {
                throw new IOException("unsupported RFB server message: " + message);
            }
            input.readUnsignedByte(); // padding
            int count = input.readUnsignedShort();
            if (count > 4096) {
                throw new IOException("too many RFB update rectangles");
            }
            boolean resized = false;
            try {
                for (int i = 0; i < count; i++) {
                    int x = input.readUnsignedShort();
                    int y = input.readUnsignedShort();
                    int w = input.readUnsignedShort();
                    int h = input.readUnsignedShort();
                    int encoding = input.readInt();
                    switch (encoding) {
                        case 0 -> framebuffer.readRaw(input, x, y, w, h);
                        case 1 -> framebuffer.copyRect(x, y, w, h,
                                input.readUnsignedShort(), input.readUnsignedShort());
                        case -223 -> {
                            if (x != 0 || y != 0) {
                                throw new IOException("invalid RFB DesktopSize origin");
                            }
                            framebuffer.resize(w, h);
                            resized = true;
                        }
                        default -> throw new IOException("unsupported RFB encoding: " + encoding);
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new IOException("invalid RFB rectangle", e);
            }
            synchronized (outputLock) {
                requestUpdateLocked(!resized);
            }
            return framebuffer.snapshot();
        }
        throw new IOException("RFB session closed");
    }

    public void sendPointer(int x, int y, int buttons) throws IOException {
        if (x < 0 || y < 0 || x >= framebuffer.width() || y >= framebuffer.height()
                || (buttons & ~0xff) != 0) {
            throw new IllegalArgumentException("invalid RFB pointer event");
        }
        synchronized (outputLock) {
            requireOpen();
            output.writeByte(5);
            output.writeByte(buttons);
            output.writeShort(x);
            output.writeShort(y);
            output.flush();
        }
    }

    public void sendKey(boolean pressed, int keysym) throws IOException {
        synchronized (outputLock) {
            requireOpen();
            output.writeByte(4);
            output.writeByte(pressed ? 1 : 0);
            output.writeShort(0);
            output.writeInt(keysym);
            output.flush();
        }
    }

    private void requestUpdateLocked(boolean incremental) throws IOException {
        requireOpen();
        output.writeByte(3);
        output.writeByte(incremental ? 1 : 0);
        output.writeShort(0);
        output.writeShort(0);
        output.writeShort(framebuffer.width());
        output.writeShort(framebuffer.height());
        output.flush();
    }

    private void requireOpen() throws IOException {
        if (closed) {
            throw new IOException("RFB session closed");
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        try {
            input.close();
        } finally {
            output.close();
        }
    }
}
