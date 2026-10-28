// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.m3.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;

/** The pre-index bound must use physical CEN records, never trust EOCD counts. */
final class GwtJarCentralDirectoryPreflightTest {

    @Test void forgedSmallEocdCannotHide12001PhysicalEntries() throws Exception {
        Path jar = archive(12_001);
        try {
            forgeCount(jar, 1);
            assertPreflightRefuses(jar, "JAR_CENTRAL_DIRECTORY_COUNT_MISMATCH");
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    @Test void understatedTwoEntryArchiveRefusesBeforeJarFileIndex() throws Exception {
        Path jar = archive(2);
        try {
            forgeCount(jar, 1);
            assertPreflightRefuses(jar, "JAR_CENTRAL_DIRECTORY_COUNT_MISMATCH");
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    @Test void invalidCentralHeaderSignatureRefusesBeforeJarFileIndex() throws Exception {
        Path jar = archive(2);
        try {
            byte[] bytes = Files.readAllBytes(jar);
            int at = find(bytes, new byte[]{0x50, 0x4b, 0x01, 0x02});
            assertTrue(at >= 0);
            bytes[at] ^= 1;
            Files.write(jar, bytes);
            assertPreflightRefuses(jar, "JAR_CENTRAL_DIRECTORY_HEADER");
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    @Test void oversizedVariableHeaderCannotEscapeDeclaredDirectory() throws Exception {
        Path jar = archive(1);
        try {
            byte[] bytes = Files.readAllBytes(jar);
            int at = find(bytes, new byte[]{0x50, 0x4b, 0x01, 0x02});
            assertTrue(at >= 0);
            bytes[at + 28] = (byte) 255;
            bytes[at + 29] = (byte) 255;
            Files.write(jar, bytes);
            assertPreflightRefuses(jar, "JAR_CENTRAL_DIRECTORY_BOUNDS");
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    @Test void exactPhysicalCountAdmitsBoundaryWithoutJarFileAllocation() throws Exception {
        for (int count : new int[]{1, 2, 256, 12_000}) {
            Path jar = archive(count);
            try {
                var preflight = method();
                // Null return: the private pre-index check completed normally.
                assertEquals(null, preflight.invoke(null, jar));
            } finally {
                Files.deleteIfExists(jar);
            }
        }
    }

    private static Path archive(int entries) throws IOException {
        Path jar = Files.createTempFile("m3-physical-central-directory-", ".jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
            for (int i = 0; i < entries; i++) {
                output.putNextEntry(new JarEntry("path/item" + i + ".bin"));
                output.write(i & 255);
                output.closeEntry();
            }
        }
        return jar;
    }

    private static void forgeCount(Path archive, int claimed) throws IOException {
        byte[] bytes = Files.readAllBytes(archive);
        int at = find(bytes, new byte[]{0x50, 0x4b, 0x05, 0x06});
        assertTrue(at >= 0);
        for (int offset : new int[] {8, 10}) {
            bytes[at + offset] = (byte) claimed;
            bytes[at + offset + 1] = (byte) (claimed >>> 8);
        }
        Files.write(archive, bytes);
    }

    private static int find(byte[] data, byte[] magic) {
        for (int i = 0; i <= data.length - magic.length; i++) {
            boolean match = true;
            for (int j = 0; j < magic.length; j++) {
                if (data[i + j] != magic[j]) { match = false; break; }
            }
            if (match) return i;
        }
        return -1;
    }

    private static Method method() throws ReflectiveOperationException {
        Method method = GwtAttributedJavaInventory.class
                .getDeclaredMethod("preflightZipDirectory", Path.class);
        method.setAccessible(true);
        return method;
    }

    private static void assertPreflightRefuses(Path jar, String signature) throws Exception {
        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> method().invoke(null, jar));
        assertTrue(thrown.getCause() instanceof IllegalArgumentException);
        assertTrue(thrown.getCause().getMessage().contains(signature),
                "Expected " + signature + "; got " + thrown.getCause());
    }
}
