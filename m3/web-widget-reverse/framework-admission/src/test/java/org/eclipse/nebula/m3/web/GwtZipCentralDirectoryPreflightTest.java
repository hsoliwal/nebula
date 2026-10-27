// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.m3.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;

/** Regression: central-directory counts must be verified before JarFile indexes them. */
final class GwtZipCentralDirectoryPreflightTest {
    private static final Method PREFLIGHT = preflightMethod();

    @Test void forgedSmallEocdCannotHide12001Entries() throws Exception {
        Path jar = jar(12_001);
        try {
            patchEocdCount(jar, 1);
            refuse(jar, "JAR_CENTRAL_DIRECTORY_COUNT_MISMATCH");
        } finally { Files.deleteIfExists(jar); }
    }

    @Test void missingOrOverreportedEntriesRefuseBeforeIndexing() throws Exception {
        Path jar = jar(1);
        try {
            patchEocdCount(jar, 0);
            refuse(jar, "JAR_CENTRAL_DIRECTORY_COUNT_MISMATCH");
            patchEocdCount(jar, 2);
            refuse(jar, "JAR_CENTRAL_DIRECTORY_COUNT_MISMATCH");
        } finally { Files.deleteIfExists(jar); }
    }

    @Test void validStandardAndCommentedJarRemainAdmitted() throws Exception {
        Path plain = jar(2);
        Path commented = Files.createTempFile("m3-zip-comment-", ".jar");
        try {
            assertDoesNotThrow(() -> preflight(plain));
            try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(commented))) {
                out.setComment("EOCD with legal ZIP comment");
                out.putNextEntry(new JarEntry("com/example/Widget.class"));
                out.write(5);
                out.closeEntry();
            }
            assertDoesNotThrow(() -> preflight(commented));
        } finally {
            Files.deleteIfExists(plain);
            Files.deleteIfExists(commented);
        }
    }

    @Test void corruptCentralHeaderAndDiskIdentityRefuse() throws Exception {
        Path badHeader = jar(1), badDisk = jar(1);
        try {
            patchCentral(badHeader, 0, 0x51);
            refuse(badHeader, "JAR_CENTRAL_DIRECTORY_HEADER");
            patchCentral(badDisk, 34, 1);
            refuse(badDisk, "JAR_SPANNED_DIRECTORY_UNSUPPORTED");
        } finally {
            Files.deleteIfExists(badHeader);
            Files.deleteIfExists(badDisk);
        }
    }

    @Test void forgedCentralLengthAndZip64SentinelRefuse() throws Exception {
        Path small = jar(1), zip64 = jar(1);
        try {
            byte[] bytes = Files.readAllBytes(small);
            int end = eocd(bytes), claimed = u32(bytes, end + 12);
            write32(bytes, end + 12, claimed - 1);
            Files.write(small, bytes);
            refuse(small, "JAR_CENTRAL_DIRECTORY_BOUNDS");
            byte[] sentinel = Files.readAllBytes(zip64);
            write32(sentinel, u32(sentinel, eocd(sentinel) + 16) + 20, -1);
            Files.write(zip64, sentinel);
            refuse(zip64, "JAR_ZIP64_UNSUPPORTED");
        } finally {
            Files.deleteIfExists(small);
            Files.deleteIfExists(zip64);
        }
    }

    private static Path jar(int count) throws Exception {
        Path path = Files.createTempFile("m3-zip-real-count-", ".jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
            for (int i = 0; i < count; i++) {
                out.putNextEntry(new JarEntry("pkg/Row" + i + ".bin"));
                out.write(i & 255);
                out.closeEntry();
            }
        }
        return path;
    }

    private static void patchEocdCount(Path path, int count) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        int end = eocd(bytes);
        write16(bytes, end + 8, count);
        write16(bytes, end + 10, count);
        Files.write(path, bytes);
    }

    private static void patchCentral(Path path, int offset, int value) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        int start = u32(bytes, eocd(bytes) + 16);
        bytes[start + offset] = (byte) value;
        Files.write(path, bytes);
    }

    private static int eocd(byte[] bytes) {
        for (int i = bytes.length - 22; i >= 0; i--) {
            if (bytes[i] == 0x50 && bytes[i + 1] == 0x4b
                    && bytes[i + 2] == 0x05 && bytes[i + 3] == 0x06
                    && i + 22 + u16(bytes, i + 20) == bytes.length) return i;
        }
        throw new AssertionError("fixture missing ZIP end record");
    }

    private static int u16(byte[] b, int index) {
        return (b[index] & 255) | ((b[index + 1] & 255) << 8);
    }

    private static int u32(byte[] b, int index) {
        return u16(b, index) | (u16(b, index + 2) << 16);
    }

    private static void write16(byte[] b, int index, int n) {
        b[index] = (byte) n;
        b[index + 1] = (byte) (n >>> 8);
    }

    private static void write32(byte[] b, int index, int n) {
        write16(b, index, n);
        write16(b, index + 2, n >>> 16);
    }

    private static void refuse(Path jar, String reason) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> preflight(jar));
        assertTrue(failure.getMessage().contains(reason), failure.getMessage());
    }

    private static void preflight(Path file) {
        try {
            PREFLIGHT.invoke(null, file);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException original) throw original;
            throw new IllegalStateException("unexpected preflight checked exception", failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("private preflight API moved", failure);
        }
    }

    private static Method preflightMethod() {
        try {
            Method m = GwtAttributedJavaInventory.class.getDeclaredMethod(
                    "preflightZipDirectory", Path.class);
            m.setAccessible(true);
            return m;
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
}
