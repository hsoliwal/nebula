/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.Test;

/** JDK 21 CPU-only PNG/JPEG admission and dimensional decode tests. */
public class RemoteFrameDecodeTest {
    @Test public void pngDecodesExactArgbIncludingAlpha() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x80f02010);
        image.setRGB(1, 0, 0xff112233);
        image.setRGB(0, 1, 0x00000000);
        image.setRGB(1, 1, 0xffffffff);
        byte[] bytes = encode(image, "png");
        RemoteFrameDecode.Pixels pixels = RemoteFrameDecode.decode(RemoteFrameDecode.Format.PNG, bytes);
        assertEquals(2, pixels.width());
        assertEquals(2, pixels.height());
        assertArrayEquals(new int[] { 0x80f02010, 0xff112233, 0x00000000, 0xffffffff }, pixels.argb());
    }

    @Test public void jpegDecodesPixelsWithoutImplicitTransparency() throws Exception {
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 3; y++) for (int x = 0; x < 4; x++) image.setRGB(x, y, 0xff335577);
        RemoteFrameDecode.Pixels pixels = RemoteFrameDecode.decode(RemoteFrameDecode.Format.JPEG,
                encode(image, "jpg"));
        assertEquals(12, pixels.argb().length);
        for (int pixel : pixels.argb()) assertEquals(0xff, pixel >>> 24);
    }

    @Test public void formatMismatchFailsBeforePublication() throws Exception {
        byte[] png = encode(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png");
        assertThrows(IllegalArgumentException.class,
                () -> RemoteFrameDecode.decode(RemoteFrameDecode.Format.JPEG, png));
        byte[] jpeg = encode(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpg");
        assertThrows(IllegalArgumentException.class,
                () -> RemoteFrameDecode.decode(RemoteFrameDecode.Format.PNG, jpeg));
    }

    @Test public void refusesTooLargeDeclaredPngBeforeAllocation() throws Exception {
        byte[] png = encode(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png");
        // IHDR width and height fields at offsets 16..23. The reader checks dimensions before decode.
        png[16] = 0x00; png[17] = 0x00; png[18] = 0x20; png[19] = 0x01; // 8193 wide
        assertThrows(IOException.class, () -> RemoteFrameDecode.decode(RemoteFrameDecode.Format.PNG, png));
    }

    @Test public void rejectsBrokenEnvelopeAndCorruptFrame() throws Exception {
        byte[] bytes = new byte[] { (byte) 0x89, 'P','N','G',13,10,26,10,0,0,0,0 };
        assertThrows(IOException.class, () -> RemoteFrameDecode.decode(RemoteFrameDecode.Format.PNG, bytes));
        assertThrows(IllegalArgumentException.class,
                () -> RemoteFrameDecode.decode(RemoteFrameDecode.Format.PNG, new byte[1]));
        assertThrows(IllegalArgumentException.class,
                () -> RemoteFrameDecode.decode(RemoteFrameDecode.Format.JPEG, new byte[8]));
        byte[] huge = new byte[RemoteFrameDecode.MAX_COMPRESSED_BYTES + 1];
        assertThrows(IllegalArgumentException.class,
                () -> RemoteFrameDecode.validateEnvelope(RemoteFrameDecode.Format.PNG, huge));
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, bytes));
        return bytes.toByteArray();
    }
}
