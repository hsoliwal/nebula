/*******************************************************************************
 * Copyright (c) 2026 Synexia and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.opal.gradientchooser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.junit.Test;

/** Fail-closed pixel evidence: screenshot IO cannot replace a graphics mismatch. */
public final class TestSupportPixelDiagnosticsTest {
    private static final String PIXEL_DIR = "m3.test.pixels.dir";

    private static ImageData pixel(int rgb) {
        ImageData result = new ImageData(1, 1, 24,
                new PaletteData(0xff0000, 0x00ff00, 0x0000ff));
        result.setPixel(0, 0, rgb);
        return result;
    }

    @Test
    public void badScreenshotDirectoryPreservesTheOriginalPixelDifference() throws Exception {
        Path fileNotDirectory = Files.createTempFile("gradient-pixel-diagnostics-", ".tmp");
        String previous = System.getProperty(PIXEL_DIR);
        try {
            System.setProperty(PIXEL_DIR, fileNotDirectory.toString());
            AssertionError mismatch = assertThrows(AssertionError.class, () ->
                    TestSupport.assertSamePixels("gradient-bad-path",
                            pixel(0xff0000), pixel(0x0000ff)));
            assertTrue(mismatch.getMessage(), mismatch.getMessage()
                    .contains("first difference at (0,0)"));
            assertEquals("save failure is retained as suppressed diagnostic",
                    1, mismatch.getSuppressed().length);
            assertTrue(mismatch.getSuppressed()[0] instanceof UncheckedIOException);
        } finally {
            if (previous == null) System.clearProperty(PIXEL_DIR);
            else System.setProperty(PIXEL_DIR, previous);
            Files.deleteIfExists(fileNotDirectory);
        }
    }

    @Test
    public void identicalPixelsDoNotAttemptAnyScreenshotWrite() throws Exception {
        Path fileNotDirectory = Files.createTempFile("gradient-same-pixels-", ".tmp");
        String previous = System.getProperty(PIXEL_DIR);
        try {
            System.setProperty(PIXEL_DIR, fileNotDirectory.toString());
            TestSupport.assertSamePixels("gradient-same", pixel(0x123456), pixel(0x123456));
        } finally {
            if (previous == null) System.clearProperty(PIXEL_DIR);
            else System.setProperty(PIXEL_DIR, previous);
            Files.deleteIfExists(fileNotDirectory);
        }
    }

    @Test
    public void aPixelDifferenceStillFailsAndWritesTwoPortablePNGDiagnostics() throws Exception {
        Path directory = Files.createTempDirectory("gradient-pixel-report-");
        String previous = System.getProperty(PIXEL_DIR);
        try {
            System.setProperty(PIXEL_DIR, directory.toString());
            AssertionError mismatch = assertThrows(AssertionError.class, () ->
                    TestSupport.assertSamePixels("gradient-portable",
                            pixel(0x00ff00), pixel(0x000000)));
            assertTrue(mismatch.getMessage().contains("first difference at (0,0)"));
            assertEquals("no screenshot IO error on writable path",
                    0, mismatch.getSuppressed().length);
            assertTrue(Files.size(directory.resolve("pixels-gradient-portable-expected.png")) > 0);
            assertTrue(Files.size(directory.resolve("pixels-gradient-portable-actual.png")) > 0);
        } finally {
            if (previous == null) System.clearProperty(PIXEL_DIR);
            else System.setProperty(PIXEL_DIR, previous);
            Files.deleteIfExists(directory.resolve("pixels-gradient-portable-expected.png"));
            Files.deleteIfExists(directory.resolve("pixels-gradient-portable-actual.png"));
            Files.deleteIfExists(directory);
        }
    }
}
