/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Standalone RFB 3.3/3.7/3.8 client with a single framebuffer reader and a
 * bounded asynchronous outgoing queue. No SWT objects are accessed by its
 * I/O threads. Designed for embedding by {@link VncCanvas} or other widgets.
 *
 * Supported encodings: Raw, CopyRect, Hextile and DesktopSize. Only requested
 * encodings are accepted. Wire pixels use negotiated 32-bit little-endian
 * true color (8 bits each of red, green and blue).
 *
 * RFB "None" and DES-based VNC authentication provide no transport privacy.
 * Connections outside loopback require an explicit opt-in; use SSH tunnelling
 * or another secured transport when crossing a trust boundary.
 */
public final class RfbClient implements AutoCloseable {

    public interface Listener {
        default void connected(String desktopName, int width, int height) { }
        default void frame(Frame frame) { }
        default void bell() { }
        default void clipboard(String text) { }
        default void disconnected(IOException cause) { }
    }

    /**
     * Read-only snapshot. The framebuffer is cloned by the decoder before
     * publication. The returned public pixel array is a defensive copy.
     */
    public static final class Frame {
        private final int width;
        private final int height;
        private final int[] rgb;
        private final String title;
        private final long sequence;

        private Frame(int width, int height, int[] rgb, String title, long sequence) {
            this.width = width;
            this.height = height;
            this.rgb = rgb.clone();
            this.title = title;
            this.sequence = sequence;
        }

        public int width() { return width; }
        public int height() { return height; }
        public String title() { return title; }
        public long sequence() { return sequence; }
        public int[] rgbCopy() { return rgb.clone(); }

        // Widget-side read-only use. No pixels array escapes to public callers.
        int[] rgbForDisplay() { return rgb; }
    }

    private static final int MAX_PIXELS = 16_777_216;
    private static final int MAX_TITLE_BYTES = 65_536;
    private static final int MAX_CLIPBOARD_BYTES = 1_048_576;
    private static final int MAX_QUEUE_SIZE = 256;
    private static final int ENCODING_RAW = 0;
    private static final int ENCODING_COPY_RECT = 1;
    private static final int ENCODING_HEXTILE = 5;
    private static final int ENCODING_DESKTOP_SIZE = -223;

    private final Listener listener;
    private final BlockingQueue<byte[]> outbound = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile Socket socket;
    private volatile Thread readerThread;
    private volatile Thread writerThread;
    private volatile int width;
    private volatile int height;
    private int[] framebuffer;
    private String title = "";
    private long sequence;

    public RfbClient(Listener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Open from a worker. The caller's password is copied and is never held
     * after authentication. The caller should also clear its own char[].
     *
     * @param allowUnencryptedRemote explicit opt-in to non-loopback plaintext
     */
    public void connectAsync(String host, int port, char[] password, boolean allowUnencryptedRemote) {
        Objects.requireNonNull(host, "host");
        if (host.isBlank() || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid RFB address or port");
        }
        if (closed.get() || !started.compareAndSet(false, true)) {
            throw new IllegalStateException("RfbClient is closed or already started");
        }
        char[] secret = password == null ? new char[0] : password.clone();
        readerThread = new Thread(() -> {
            IOException problem = null;
            try {
                InetAddress address = InetAddress.getByName(host);
                if (!address.isLoopbackAddress() && !allowUnencryptedRemote) {
                    throw new IOException("Unencrypted remote RFB requires explicit opt-in; use an SSH tunnel");
                }
                Socket connection = new Socket();
                socket = connection;
                if (closed.get()) {
                    return;
                }
                connection.connect(new InetSocketAddress(address, port), 10_000);
                connection.setTcpNoDelay(true);
                DataInputStream input = new DataInputStream(
                        new BufferedInputStream(connection.getInputStream()));
                DataOutputStream output = new DataOutputStream(
                        new BufferedOutputStream(connection.getOutputStream()));
                authenticate(input, output, secret);
                initialize(input, output);
                if (closed.get()) {
                    return;
                }
                writerThread = new Thread(() -> writeLoop(output), "Nebula-RFB-writer");
                writerThread.setDaemon(true);
                writerThread.start();
                listener.connected(title, width, height);
                publish();
                readLoop(input);
            } catch (IOException e) {
                if (!closed.get()) {
                    problem = e;
                }
            } catch (RuntimeException e) {
                if (!closed.get()) {
                    problem = new IOException("Unexpected RFB client error", e);
                }
            } finally {
                Arrays.fill(secret, '\0');
                close();
                listener.disconnected(problem);
            }
        }, "Nebula-RFB-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void authenticate(DataInputStream input, DataOutputStream output,
            char[] password) throws IOException {
        byte[] banner = new byte[12];
        input.readFully(banner);
        String version = new String(banner, StandardCharsets.US_ASCII);
        if (!version.matches("RFB 003\\.[0-9]{3}\\n")) {
            throw new IOException("Unsupported RFB protocol banner");
        }
        int offered = Integer.parseInt(version.substring(8, 11));
        int minor = offered >= 8 ? 8 : offered >= 7 ? 7 : 3;
        output.write(("RFB 003.00" + minor + "\n").getBytes(StandardCharsets.US_ASCII));
        output.flush();

        int security;
        if (minor == 3) {
            security = input.readInt();
            if (security == 0) {
                throw new IOException("RFB handshake refused: " + readText(input, MAX_TITLE_BYTES));
            }
        } else {
            int count = input.readUnsignedByte();
            if (count == 0) {
                throw new IOException("RFB handshake refused: " + readText(input, MAX_TITLE_BYTES));
            }
            byte[] methods = new byte[count];
            input.readFully(methods);
            boolean none = false;
            boolean vnc = false;
            for (byte method : methods) {
                none |= method == 1;
                vnc |= method == 2;
            }
            if (vnc && password.length > 0) {
                security = 2;
            } else if (none) {
                security = 1;
            } else {
                throw new IOException("Server requires unsupported RFB security or a password");
            }
            output.writeByte(security);
            output.flush();
        }

        if (security == 2) {
            if (password.length == 0) {
                throw new IOException("RFB VNC authentication requires a password");
            }
            byte[] challenge = new byte[16];
            input.readFully(challenge);
            byte[] response = vncResponse(challenge, password);
            output.write(response);
            output.flush();
            Arrays.fill(response, (byte) 0);
            Arrays.fill(challenge, (byte) 0);
        } else if (security != 1) {
            throw new IOException("Unsupported RFB security scheme: " + security);
        }

        // RFB 3.7 with security None has no SecurityResult.
        if (minor == 8 || security == 2) {
            int result = input.readInt();
            if (result != 0) {
                String reason = minor == 8 ? readText(input, MAX_TITLE_BYTES) : "authentication rejected";
                throw new IOException("RFB security failed: " + reason);
            }
        }
    }

    static byte[] vncResponse(byte[] challenge, char[] password) throws IOException {
        if (challenge.length != 16) {
            throw new IllegalArgumentException("VNC challenge must have 16 bytes");
        }
        byte[] key = new byte[8];
        for (int i = 0; i < Math.min(8, password.length); i++) {
            if (password[i] > 255) {
                throw new IOException("VNC authentication only supports single-byte characters");
            }
            key[i] = (byte) (Integer.reverse(password[i] & 0xff) >>> 24);
        }
        try {
            Cipher des = Cipher.getInstance("DES/ECB/NoPadding");
            des.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"));
            return des.doFinal(challenge);
        } catch (GeneralSecurityException e) {
            throw new IOException("VNC authentication unavailable", e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    private void initialize(DataInputStream input, DataOutputStream output) throws IOException {
        output.writeByte(1); // shared desktop
        output.flush();
        int w = input.readUnsignedShort();
        int h = input.readUnsignedShort();
        validateSize(w, h);
        input.skipNBytes(16); // server's pixel format
        title = readText(input, MAX_TITLE_BYTES);
        width = w;
        height = h;
        framebuffer = new int[w * h];

        // SetPixelFormat: 32bpp, depth 24, little-endian, true-color RGB888.
        output.writeByte(0);
        output.write(new byte[3]);
        output.writeByte(32);
        output.writeByte(24);
        output.writeByte(0);
        output.writeByte(1);
        output.writeShort(255);
        output.writeShort(255);
        output.writeShort(255);
        output.writeByte(16);
        output.writeByte(8);
        output.writeByte(0);
        output.write(new byte[3]);

        output.writeByte(2); // SetEncodings
        output.writeByte(0);
        output.writeShort(4);
        output.writeInt(ENCODING_HEXTILE);
        output.writeInt(ENCODING_COPY_RECT);
        output.writeInt(ENCODING_RAW);
        output.writeInt(ENCODING_DESKTOP_SIZE);
        output.write(requestUpdate(false));
        output.flush();
    }

    private void readLoop(DataInputStream input) throws IOException {
        while (!closed.get()) {
            int message = input.readUnsignedByte();
            switch (message) {
                case 0 -> readFramebufferUpdate(input);
                case 1 -> { // SetColorMapEntries: consumed; true-color is requested.
                    input.readUnsignedByte();
                    input.readUnsignedShort();
                    int count = input.readUnsignedShort();
                    input.skipNBytes(6L * count);
                }
                case 2 -> listener.bell();
                case 3 -> {
                    input.skipNBytes(3);
                    listener.clipboard(readText(input, MAX_CLIPBOARD_BYTES));
                }
                default -> throw new IOException("Unsupported server-to-client message: " + message);
            }
        }
    }

    private void readFramebufferUpdate(DataInputStream input) throws IOException {
        input.readUnsignedByte(); // padding
        int rectangles = input.readUnsignedShort();
        for (int r = 0; r < rectangles; r++) {
            int x = input.readUnsignedShort();
            int y = input.readUnsignedShort();
            int w = input.readUnsignedShort();
            int h = input.readUnsignedShort();
            int encoding = input.readInt();

            if (encoding == ENCODING_DESKTOP_SIZE) {
                resize(w, h);
                continue;
            }
            validateRect(x, y, w, h);
            switch (encoding) {
                case ENCODING_RAW -> decodeRaw(input, x, y, w, h);
                case ENCODING_COPY_RECT -> decodeCopy(input, x, y, w, h);
                case ENCODING_HEXTILE -> decodeHextile(input, x, y, w, h);
                default -> throw new IOException("Unrequested RFB encoding: " + encoding);
            }
        }
        publish();
        enqueue(requestUpdate(true));
    }

    private void decodeRaw(DataInputStream input, int x, int y, int w, int h)
            throws IOException {
        for (int row = 0; row < h; row++) {
            int offset = (y + row) * width + x;
            for (int col = 0; col < w; col++) {
                framebuffer[offset + col] = readRgb(input);
            }
        }
    }

    private void decodeCopy(DataInputStream input, int x, int y, int w, int h)
            throws IOException {
        int sourceX = input.readUnsignedShort();
        int sourceY = input.readUnsignedShort();
        validateRect(sourceX, sourceY, w, h);
        int[] copy = new int[w * h];
        for (int row = 0; row < h; row++) {
            System.arraycopy(framebuffer, (sourceY + row) * width + sourceX,
                    copy, row * w, w);
        }
        for (int row = 0; row < h; row++) {
            System.arraycopy(copy, row * w, framebuffer,
                    (y + row) * width + x, w);
        }
    }

    private void decodeHextile(DataInputStream input, int x, int y, int w, int h)
            throws IOException {
        int background = 0;
        int foreground = 0;
        for (int top = y; top < y + h; top += 16) {
            for (int left = x; left < x + w; left += 16) {
                int tw = Math.min(16, x + w - left);
                int th = Math.min(16, y + h - top);
                int flags = input.readUnsignedByte();
                if ((flags & ~31) != 0) {
                    throw new IOException("Invalid Hextile subencoding flags");
                }
                if ((flags & 1) != 0) {
                    decodeRaw(input, left, top, tw, th);
                    continue;
                }
                if ((flags & 2) != 0) {
                    background = readRgb(input);
                }
                if ((flags & 4) != 0) {
                    foreground = readRgb(input);
                }
                fill(left, top, tw, th, background);
                if ((flags & 8) != 0) {
                    int count = input.readUnsignedByte();
                    for (int i = 0; i < count; i++) {
                        int color = (flags & 16) != 0 ? readRgb(input) : foreground;
                        int pos = input.readUnsignedByte();
                        int size = input.readUnsignedByte();
                        int sx = pos >>> 4;
                        int sy = pos & 15;
                        int sw = (size >>> 4) + 1;
                        int sh = (size & 15) + 1;
                        if (sx + sw > tw || sy + sh > th) {
                            throw new IOException("Hextile subrectangle exceeds tile");
                        }
                        fill(left + sx, top + sy, sw, sh, color);
                    }
                } else if ((flags & 16) != 0) {
                    throw new IOException("Invalid Hextile colored-subrect flag");
                }
            }
        }
    }

    private void fill(int x, int y, int w, int h, int rgb) {
        for (int row = 0; row < h; row++) {
            int start = (y + row) * width + x;
            Arrays.fill(framebuffer, start, start + w, rgb);
        }
    }

    private void resize(int w, int h) throws IOException {
        validateSize(w, h);
        int[] next = new int[w * h];
        int copyRows = Math.min(height, h);
        int copyCols = Math.min(width, w);
        for (int row = 0; row < copyRows; row++) {
            System.arraycopy(framebuffer, row * width, next, row * w, copyCols);
        }
        width = w;
        height = h;
        framebuffer = next;
    }

    private void publish() {
        listener.frame(new Frame(width, height, framebuffer, title, ++sequence));
    }

    private static int readRgb(DataInputStream in) throws IOException {
        int blue = in.readUnsignedByte();
        int green = in.readUnsignedByte();
        int red = in.readUnsignedByte();
        in.readUnsignedByte(); // 32-bit pixel's high byte
        return (red << 16) | (green << 8) | blue;
    }

    private static String readText(DataInputStream input, int maxBytes) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > maxBytes) {
            throw new IOException("Invalid RFB text length: " + length);
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    private static void validateSize(int w, int h) throws IOException {
        if (w <= 0 || h <= 0 || (long) w * h > MAX_PIXELS) {
            throw new IOException("Invalid RFB framebuffer dimensions: " + w + "x" + h);
        }
    }

    private void validateRect(int x, int y, int w, int h) throws IOException {
        if (w <= 0 || h <= 0 || x >= width || y >= height
                || (long) x + w > width || (long) y + h > height) {
            throw new IOException("Invalid RFB rectangle");
        }
    }

    private byte[] requestUpdate(boolean incremental) {
        byte[] request = new byte[10];
        request[0] = 3;
        request[1] = (byte) (incremental ? 1 : 0);
        request[6] = (byte) (width >>> 8);
        request[7] = (byte) width;
        request[8] = (byte) (height >>> 8);
        request[9] = (byte) height;
        return request;
    }

    public boolean requestRefresh() {
        return enqueue(requestUpdate(false));
    }

    public boolean pointer(int mask, int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return false;
        }
        return enqueue(new byte[] {5, (byte) (mask & 255),
                (byte) (x >>> 8), (byte) x, (byte) (y >>> 8), (byte) y});
    }

    public boolean key(boolean down, int keysym) {
        if (keysym == 0) {
            return false;
        }
        return enqueue(new byte[] {4, (byte) (down ? 1 : 0), 0, 0,
                (byte) (keysym >>> 24), (byte) (keysym >>> 16),
                (byte) (keysym >>> 8), (byte) keysym});
    }

    public boolean sendClipboard(String text) {
        Objects.requireNonNull(text, "text");
        byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
        if (bytes.length > MAX_CLIPBOARD_BYTES) {
            throw new IllegalArgumentException("Clipboard exceeds 1 MiB");
        }
        ByteArrayOutputStream raw = new ByteArrayOutputStream(8 + bytes.length);
        try (DataOutputStream data = new DataOutputStream(raw)) {
            data.writeInt(6 << 24); // type 6 + three padding bytes
            data.writeInt(bytes.length);
            data.write(bytes);
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        return enqueue(raw.toByteArray());
    }

    private boolean enqueue(byte[] packet) {
        return started.get() && !closed.get() && outbound.offer(packet);
    }

    private void writeLoop(DataOutputStream output) {
        try {
            while (!closed.get()) {
                byte[] packet = outbound.take();
                output.write(packet);
                output.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            close();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Socket current = socket;
            if (current != null) {
                try {
                    current.close();
                } catch (IOException ignored) {
                    // The peer may already have closed the connection.
                }
            }
            Thread writer = writerThread;
            if (writer != null) {
                writer.interrupt();
            }
        }
    }
}
