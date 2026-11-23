/*
 * SPDX-License-Identifier: EPL-2.0
 *
 * Protocol design reference: RFB protocol specification and Eclipse TmL VNC.
 * This implementation is new; it does not contain copied TmL sources.
 */
package org.eclipse.nebula.widgets.vnc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

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
    private static final int MAX_COMPRESSED = 64 * 1024 * 1024;
    private static final int[] ENCODINGS = {16, 6, 5, 2, 1, 0, -223, -224};

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Listener listener;
    private final Object writeLock = new Object();
    private final RfbFramebuffer framebuffer;
    private final String serverName;
    private final Inflater zlib = new Inflater();
    private final Inflater zrle = new Inflater();
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
        // RFB 3.7 with None also omits SecurityResult. RFB 3.8 sends it.
        if (security == 2 || minor >= 8) {
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
    static byte[] vncChallengeResponse(byte[] challenge, char[] password) throws IOException {
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
            out.writeShort(ENCODINGS.length);
            for (int encoding : ENCODINGS) out.writeInt(encoding);
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
        boolean resized = false;
        for (int i = 0; i < count; i++) {
            int x = in.readUnsignedShort();
            int y = in.readUnsignedShort();
            int w = in.readUnsignedShort();
            int h = in.readUnsignedShort();
            int encoding = in.readInt();
            if (encoding == -224) break; // LastRect sentinel
            switch (encoding) {
                case 0 -> framebuffer.readRaw(in, x, y, w, h);
                case 1 -> framebuffer.copyRect(x, y, w, h,
                        in.readUnsignedShort(), in.readUnsignedShort());
                case 2 -> readRre(x, y, w, h);
                case 5 -> readHextile(x, y, w, h);
                case 6 -> readZlib(x, y, w, h);
                case 16 -> readZrle(x, y, w, h);
                case -223 -> {
                    if (x != 0 || y != 0) throw new IOException("Invalid DesktopSize update");
                    framebuffer.resize(w, h);
                    resized = true;
                }
                default -> throw new IOException("Unrequested RFB encoding " + encoding);
            }
        }
        listener.changed(framebuffer);
        requestUpdate(!resized);
    }

    private void validateRectangle(int x, int y, int width, int height) throws IOException {
        if (width < 1 || height < 1 || (long) x + width > framebuffer.width()
                || (long) y + height > framebuffer.height()) {
            throw new IOException("Invalid RFB encoded rectangle");
        }
    }

    /** Decode RRE atomically, publishing one bounded damage interval only after validation. */
    private void readRre(int x, int y, int w, int h) throws IOException {
        validateRectangle(x, y, w, h);
        long count = Integer.toUnsignedLong(in.readInt());
        if (count > (long) w * h) throw new IOException("RRE subrectangle count exceeds tile");
        int[] tile = new int[w * h];
        Arrays.fill(tile, readColor());
        for (long i = 0; i < count; i++) {
            int color = readColor();
            int rx = in.readUnsignedShort(), ry = in.readUnsignedShort();
            int rw = in.readUnsignedShort(), rh = in.readUnsignedShort();
            if (rw < 1 || rh < 1 || (long) rx + rw > w || (long) ry + rh > h) {
                throw new IOException("RRE subrectangle outside tile");
            }
            for (int row = ry; row < ry + rh; row++) {
                Arrays.fill(tile, row * w + rx, row * w + rx + rw, color);
            }
        }
        framebuffer.writePixels(x, y, w, h, tile);
    }

    /** Zlib and ZRLE each retain their own stream dictionary across rectangles. */
    private byte[] inflateRectangle(Inflater inflater, int limit) throws IOException {
        int length = checkedLength(in.readInt(), MAX_COMPRESSED, "compressed rectangle");
        byte[] compressed = readBytes(length);
        if (!inflater.needsInput()) throw new IOException("Previous RFB zlib segment was not consumed");
        inflater.setInput(compressed);
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limit, 8192));
        byte[] buffer = new byte[8192];
        try {
            while (true) {
                int read = inflater.inflate(buffer);
                if (read > 0) {
                    if (read > limit - output.size()) throw new IOException("RFB decompression exceeds bounds");
                    output.write(buffer, 0, read);
                } else if (inflater.needsInput() || inflater.finished()) {
                    break;
                } else if (inflater.needsDictionary()) {
                    throw new IOException("Unexpected RFB zlib dictionary");
                } else {
                    throw new IOException("RFB compressed stream did not progress");
                }
            }
        } catch (DataFormatException malformed) {
            throw new IOException("Invalid RFB compressed stream", malformed);
        }
        if (inflater.getRemaining() != 0) throw new IOException("Trailing RFB compressed bytes");
        return output.toByteArray();
    }

    private void readZlib(int x, int y, int w, int h) throws IOException {
        validateRectangle(x, y, w, h);
        byte[] decompressed = inflateRectangle(zlib, w * h * 4);
        if (decompressed.length != w * h * 4) throw new EOFException("Truncated RFB Zlib rectangle");
        int[] pixels = new int[w * h];
        for (int i = 0, at = 0; i < pixels.length; i++, at += 4) {
            pixels[i] = 0xff000000 | (decompressed[at + 2] & 0xff) << 16
                    | (decompressed[at + 1] & 0xff) << 8 | (decompressed[at] & 0xff);
        }
        framebuffer.writePixels(x, y, w, h, pixels);
    }

    /** Compact ZRLE pixels omit the unused fourth byte in our 32-bit format. */
    private static int compactPixel(DataInputStream input) throws IOException {
        int blue = input.readUnsignedByte(), green = input.readUnsignedByte(), red = input.readUnsignedByte();
        return 0xff000000 | red << 16 | green << 8 | blue;
    }

    private static int runLength(DataInputStream input, int remaining) throws IOException {
        int count = 1, next;
        do {
            next = input.readUnsignedByte();
            count += next;
            if (count > remaining) throw new IOException("ZRLE run exceeds tile");
        } while (next == 255);
        return count;
    }

    /** ZRLE tiles: raw, solid, packed palette, plain RLE and palette RLE. */
    private void readZrle(int x, int y, int w, int h) throws IOException {
        validateRectangle(x, y, w, h);
        long tiles = ((w + 63L) / 64) * ((h + 63L) / 64);
        int limit = Math.toIntExact(Math.min(8L * w * h + 1024L * tiles,
                8L * RfbFramebuffer.MAX_PIXELS));
        byte[] expanded = inflateRectangle(zrle, limit);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(expanded));
        for (int ty = 0; ty < h; ty += 64) {
            for (int tx = 0; tx < w; tx += 64) {
                int tw = Math.min(64, w - tx), th = Math.min(64, h - ty);
                int encoding = input.readUnsignedByte();
                int paletteSize = encoding & 0x7f;
                boolean rle = (encoding & 0x80) != 0;
                if (!rle && paletteSize > 16) throw new IOException("ZRLE palette too large");
                int[] palette = new int[paletteSize];
                for (int i = 0; i < paletteSize; i++) palette[i] = compactPixel(input);
                int[] tile = new int[tw * th];
                if (!rle && paletteSize == 0) {
                    for (int i = 0; i < tile.length; i++) tile[i] = compactPixel(input);
                } else if (!rle && paletteSize == 1) {
                    Arrays.fill(tile, palette[0]);
                } else if (!rle) {
                    int bits = paletteSize <= 2 ? 1 : paletteSize <= 4 ? 2 : 4;
                    int mask = (1 << bits) - 1;
                    for (int row = 0; row < th; row++) {
                        int packed = 0;
                        for (int col = 0; col < tw; col++) {
                            if ((col * bits & 7) == 0) packed = input.readUnsignedByte();
                            int index = packed >>> (8 - bits - (col * bits & 7)) & mask;
                            if (index >= paletteSize) throw new IOException("ZRLE invalid palette index");
                            tile[row * tw + col] = palette[index];
                        }
                    }
                } else {
                    int at = 0;
                    while (at < tile.length) {
                        int color, count;
                        if (paletteSize == 0) {
                            color = compactPixel(input);
                            count = runLength(input, tile.length - at);
                        } else {
                            int index = input.readUnsignedByte();
                            if ((index & 0x7f) >= paletteSize) throw new IOException("ZRLE invalid RLE palette index");
                            color = palette[index & 0x7f];
                            count = (index & 0x80) == 0 ? 1 : runLength(input, tile.length - at);
                        }
                        Arrays.fill(tile, at, at + count, color);
                        at += count;
                    }
                }
                framebuffer.writePixels(x + tx, y + ty, tw, th, tile);
            }
        }
        if (input.available() != 0) throw new IOException("Trailing ZRLE tile bytes");
    }

    /**
     * Hextile is built tile-by-tile on the CPU, then published as bounded
     * rectangle writes. Colors persist between tiles as required by RFB.
     */
    private void readHextile(int x, int y, int width, int height) throws IOException {
        if (width < 0 || height < 0 || (long) x + width > framebuffer.width()
                || (long) y + height > framebuffer.height()) {
            throw new IOException("Invalid Hextile rectangle");
        }
        int background = 0xff000000;
        int foreground = 0xff000000;
        for (int ty = y; ty < y + height; ty += 16) {
            int th = Math.min(16, y + height - ty);
            for (int tx = x; tx < x + width; tx += 16) {
                int tw = Math.min(16, x + width - tx);
                int subencoding = in.readUnsignedByte();
                if ((subencoding & ~31) != 0) throw new IOException("Invalid Hextile flags");
                if ((subencoding & 1) != 0) {
                    framebuffer.readRaw(in, tx, ty, tw, th);
                    continue;
                }
                if ((subencoding & 2) != 0) background = readColor();
                if ((subencoding & 4) != 0) foreground = readColor();
                int[] tile = new int[tw * th];
                Arrays.fill(tile, background);
                if ((subencoding & 8) != 0) {
                    int subrectangles = in.readUnsignedByte();
                    for (int i = 0; i < subrectangles; i++) {
                        int color = (subencoding & 16) != 0 ? readColor() : foreground;
                        int position = in.readUnsignedByte();
                        int size = in.readUnsignedByte();
                        int sx = position >>> 4;
                        int sy = position & 15;
                        int sw = (size >>> 4) + 1;
                        int sh = (size & 15) + 1;
                        if (sx + sw > tw || sy + sh > th) {
                            throw new IOException("Hextile subrectangle outside tile");
                        }
                        for (int row = sy; row < sy + sh; row++) {
                            Arrays.fill(tile, row * tw + sx, row * tw + sx + sw, color);
                        }
                    }
                }
                framebuffer.writePixels(tx, ty, tw, th, tile);
            }
        }
    }

    private int readColor() throws IOException {
        int b = in.readUnsignedByte();
        int g = in.readUnsignedByte();
        int r = in.readUnsignedByte();
        in.readUnsignedByte(); // padding (32-bit true-colour)
        return 0xff000000 | r << 16 | g << 8 | b;
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
        zlib.end();
        zrle.end();
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing on shutdown must never prevent SWT disposal.
        }
    }
}
