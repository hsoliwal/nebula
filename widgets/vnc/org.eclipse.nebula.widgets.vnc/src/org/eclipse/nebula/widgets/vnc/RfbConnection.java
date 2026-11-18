/*
 * SPDX-License-Identifier: EPL-2.0
 *
 * Protocol design reference: RFB protocol specification and Eclipse TmL VNC.
 * This implementation is new; it does not contain copied TmL sources.
 */
package org.eclipse.nebula.widgets.vnc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/** RFB 3.3/3.7/3.8 transport; all reads and framebuffer writes run off the SWT thread. */
final class RfbConnection implements AutoCloseable {
    interface Listener {
        void changed(RfbFramebuffer framebuffer);
        void bell();
        void clipboard(String text);
        void failed(IOException exception);
    }

    private static final int MAX_TEXT = 1_048_576;
    private static final int MAX_NAME = 65_536;
    private static final int MAX_RECTS = 8_192;

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Listener listener;
    private final Object writeLock = new Object();
    private final RfbFramebuffer framebuffer;
    private final String serverName;
    private volatile boolean closed;

    RfbConnection(Socket socket, char[] password, Listener listener) throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.listener = Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(password, "password");
        socket.setSoTimeout(10_000);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        int minor = negotiateProtocol();
        negotiateSecurity(minor, password);
        out.writeByte(1); // ClientInit: share the desktop
        out.flush();

        int width = in.readUnsignedShort();
        int height = in.readUnsignedShort();
        in.skipNBytes(16); // ServerInit pixel format; replaced below
        int nameSize = checkedLength(in.readInt(), MAX_NAME, "desktop name");
        serverName = new String(readBytes(nameSize), StandardCharsets.UTF_8);
        framebuffer = new RfbFramebuffer(width, height);
        configurePixelFormat();
        configureEncodings();
        requestUpdate(false);
        socket.setSoTimeout(0); // idle sessions remain connected until explicitly closed
    }

    RfbFramebuffer framebuffer() {
        return framebuffer;
    }

    String serverName() {
        return serverName;
    }

    private byte[] readBytes(int size) throws IOException {
        byte[] bytes = new byte[size];
        in.readFully(bytes);
        return bytes;
    }

    private static int checkedLength(int value, int maximum, String label) throws IOException {
        if (value < 0 || value > maximum) {
            throw new IOException("Invalid " + label + " length");
        }
        return value;
    }

    private int negotiateProtocol() throws IOException {
        String version = new String(readBytes(12), StandardCharsets.US_ASCII);
        if (!version.matches("RFB 003\\.00[3-8]\\n")) {
            throw new IOException("Unsupported RFB protocol banner");
        }
        int offered = Integer.parseInt(version.substring(8, 11));
        int minor = offered >= 8 ? 8 : (offered >= 7 ? 7 : 3);
        out.write(("RFB 003." + String.format("%03d", minor) + "\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
        return minor;
    }

    private void negotiateSecurity(int minor, char[] password) throws IOException {
        int security;
        if (minor == 3) {
            security = in.readInt();
            if (security == 0) {
                throw new IOException("RFB security rejected: " + readReason());
            }
        } else {
            int numberOfTypes = in.readUnsignedByte();
            if (numberOfTypes == 0) {
                throw new IOException("RFB security rejected: " + readReason());
            }
            byte[] offered = readBytes(numberOfTypes);
            boolean none = false, vnc = false;
            for (byte type : offered) {
                none |= (type & 0xff) == 1;
                vnc |= (type & 0xff) == 2;
            }
            security = password.length > 0 && vnc ? 2 : (none ? 1 : (vnc ? 2 : -1));
            if (security < 0) {
                throw new IOException("RFB server offers no supported security type");
            }
            out.writeByte(security);
            out.flush();
        }

        if (security == 2) {
            if (password.length == 0) {
                throw new IOException("RFB server requires a VNC password");
            }
            byte[] challenge = readBytes(16);
            out.write(vncChallengeResponse(challenge, password));
            out.flush();
        } else if (security != 1) {
            throw new IOException("Unsupported RFB security type " + security);
        }

        // Protocol 3.3 with None security has no SecurityResult message.
        if (minor != 3 || security != 1) {
            int result = in.readInt();
            if (result != 0) {
                String detail = minor >= 8 ? readReason() : "authentication failed";
                throw new IOException("RFB authentication failed: " + detail);
            }
        }
    }

    private String readReason() throws IOException {
        int size = checkedLength(in.readInt(), MAX_TEXT, "error text");
        return new String(readBytes(size), StandardCharsets.UTF_8);
    }

    /** Legacy VNC challenge/response; use only inside an SSH/TLS tunnel. */
    private static byte[] vncChallengeResponse(byte[] challenge, char[] password) throws IOException {
        byte[] key = new byte[8];
        try {
            for (int i = 0; i < Math.min(password.length, key.length); i++) {
                key[i] = (byte) (Integer.reverse(password[i] & 0xff) >>> 24);
            }
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"));
            return cipher.doFinal(challenge);
        } catch (GeneralSecurityException exception) {
            throw new IOException("VNC challenge authentication is unavailable", exception);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    private void configurePixelFormat() throws IOException {
        synchronized (writeLock) {
            out.writeByte(0); // SetPixelFormat
            out.write(new byte[3]);
            out.writeByte(32); // bits per pixel
            out.writeByte(24); // depth
            out.writeByte(0);  // little endian
            out.writeByte(1);  // true colour
            out.writeShort(255); // max red
            out.writeShort(255); // max green
            out.writeShort(255); // max blue
            out.writeByte(16); // shift red
            out.writeByte(8);  // shift green
            out.writeByte(0);  // shift blue
            out.write(new byte[3]);
            out.flush();
        }
    }

    private void configureEncodings() throws IOException {
        synchronized (writeLock) {
            out.writeByte(2); // SetEncodings
            out.writeByte(0);
            out.writeShort(3);
            out.writeInt(1);    // CopyRect
            out.writeInt(0);    // Raw
            out.writeInt(-223); // DesktopSize
            out.flush();
        }
    }

    private void requestUpdate(boolean incremental) throws IOException {
        synchronized (writeLock) {
            if (closed) return;
            out.writeByte(3);
            out.writeByte(incremental ? 1 : 0);
            out.writeShort(0);
            out.writeShort(0);
            out.writeShort(framebuffer.width());
            out.writeShort(framebuffer.height());
            out.flush();
        }
    }

    void readLoop() {
        try {
            while (!closed) {
                readPacket();
            }
        } catch (EOFException eof) {
            if (!closed) listener.failed(new IOException("RFB server disconnected", eof));
        } catch (IOException | IllegalArgumentException error) {
            if (!closed) listener.failed(error instanceof IOException ? (IOException) error
                    : new IOException("Invalid RFB framebuffer update", error));
        }
    }

    /** One packet, exposed to package tests; only the receiver thread calls it. */
    void readPacket() throws IOException {
        int message = in.readUnsignedByte();
        switch (message) {
            case 0 -> readFramebufferUpdate();
            case 1 -> {
                in.readUnsignedByte(); // padding
                in.readUnsignedShort(); // first colour
                int count = in.readUnsignedShort();
                in.skipNBytes((long) count * 6); // colour map entries (unused in true-colour)
            }
            case 2 -> listener.bell();
            case 3 -> {
                in.skipNBytes(3);
                int length = checkedLength(in.readInt(), MAX_TEXT, "clipboard");
                listener.clipboard(new String(readBytes(length), StandardCharsets.ISO_8859_1));
            }
            default -> throw new IOException("Unsupported RFB server message " + message);
        }
    }

    private void readFramebufferUpdate() throws IOException {
        in.readUnsignedByte(); // padding
        int count = in.readUnsignedShort();
        if (count > MAX_RECTS) throw new IOException("Too many RFB rectangles");
        for (int i = 0; i < count; i++) {
            int x = in.readUnsignedShort();
            int y = in.readUnsignedShort();
            int w = in.readUnsignedShort();
            int h = in.readUnsignedShort();
            int encoding = in.readInt();
            switch (encoding) {
                case 0 -> framebuffer.readRaw(in, x, y, w, h);
                case 1 -> framebuffer.copyRect(x, y, w, h,
                        in.readUnsignedShort(), in.readUnsignedShort());
                case -223 -> {
                    if (x != 0 || y != 0) throw new IOException("Invalid DesktopSize update");
                    framebuffer.resize(w, h);
                }
                default -> throw new IOException("Unrequested RFB encoding " + encoding);
            }
        }
        listener.changed(framebuffer);
        requestUpdate(true);
    }

    void sendPointer(int x, int y, int buttonMask) throws IOException {
        synchronized (writeLock) {
            if (closed) return;
            out.writeByte(5);
            out.writeByte(buttonMask & 0xff);
            out.writeShort(Math.max(0, Math.min(x, framebuffer.width() - 1)));
            out.writeShort(Math.max(0, Math.min(y, framebuffer.height() - 1)));
            out.flush();
        }
    }

    void sendKey(int keysym, boolean down) throws IOException {
        synchronized (writeLock) {
            if (closed) return;
            out.writeByte(4);
            out.writeByte(down ? 1 : 0);
            out.writeShort(0);
            out.writeInt(keysym);
            out.flush();
        }
    }

    void sendClipboard(String text) throws IOException {
        Objects.requireNonNull(text, "text");
        byte[] data = text.getBytes(StandardCharsets.ISO_8859_1);
        if (data.length > MAX_TEXT) throw new IOException("RFB clipboard exceeds limit");
        synchronized (writeLock) {
            if (closed) return;
            out.writeByte(6);
            out.write(new byte[3]);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        }
    }

    @Override
    public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing on shutdown must never prevent SWT disposal.
        }
    }
}
