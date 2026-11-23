/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * CPU-only PNG/JPEG decoder for a previously authenticated browser-frame stream.
 * A private in-memory ImageInputStream avoids ImageIO's optional disk cache.
 * SWT Image and native handles are owned only by the receiving DrawnBrowser.
 */
public final class RemoteFrameDecode {
    public static final int MAX_COMPRESSED_BYTES = 8 * 1024 * 1024;
    public static final int MAX_AXIS = 8192;
    public static final long MAX_PIXELS = 16_777_216L;

    public enum Format { PNG, JPEG }

    /** One independently allocated pixel lane; the caller must not mutate it. */
    public record Pixels(int width, int height, int[] argb) { }

    private RemoteFrameDecode() { }

    public static Pixels decode(Format format, byte[] compressed) throws IOException {
        validateEnvelope(format, compressed);
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(compressed))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported browser image encoding");
            ImageReader reader = readers.next();
            try {
                String actual = reader.getFormatName();
                if (!(format == Format.PNG && "png".equalsIgnoreCase(actual)
                        || format == Format.JPEG && ("jpeg".equalsIgnoreCase(actual)
                                || "jpg".equalsIgnoreCase(actual)))) {
                    throw new IOException("Frame encoding does not match declared format");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                BufferedImage bitmap = reader.read(0);
                if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
                    throw new IOException("Incomplete browser image");
                }
                int[] pixels = new int[Math.multiplyExact(width, height)];
                bitmap.getRGB(0, 0, width, height, pixels, 0, width);
                return new Pixels(width, height, pixels);
            } catch (RuntimeException exception) {
                throw new IOException("Invalid browser image", exception);
            } finally {
                reader.dispose();
            }
        }
    }

    public static void validateEnvelope(Format format, byte[] bytes) {
        if (format == null || bytes == null || bytes.length < 8
                || bytes.length > MAX_COMPRESSED_BYTES) {
            throw new IllegalArgumentException("Invalid compressed browser frame");
        }
        if (format == Format.PNG) {
            int[] magic = { 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
            for (int i = 0; i < magic.length; i++) {
                if ((bytes[i] & 0xff) != magic[i]) throw new IllegalArgumentException("Invalid PNG signature");
            }
        } else if ((bytes[0] & 0xff) != 0xff || (bytes[1] & 0xff) != 0xd8
                || (bytes[bytes.length - 2] & 0xff) != 0xff
                || (bytes[bytes.length - 1] & 0xff) != 0xd9) {
            throw new IllegalArgumentException("Invalid JPEG markers");
        }
    }

    private static void validateDimensions(int width, int height) throws IOException {
        if (width < 1 || height < 1 || width > MAX_AXIS || height > MAX_AXIS
                || (long) width * height > MAX_PIXELS) {
            throw new IOException("Browser image exceeds decoded pixel budget");
        }
    }
}
