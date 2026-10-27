// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.m3.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/** Regression tests for source bytes as immutable attributed-input evidence. */
final class GwtSourceBytesOwnershipTest {
    private static final String SOURCE = "package demo;\n"
            + "import com.google.gwt.user.client.ui.Label;\n"
            + "public final class Orders { public static void build() { "
            + "Label title = new Label(\"Hello\"); } }\n";

    @Test void mutationDuringClasspathSnapshotDoesNotChangeAttributedSource() throws Exception {
        Path root = Files.createTempDirectory("m3-gwt-source-array-test-");
        try {
            Path jar = signatures(root);
            byte[] input = SOURCE.getBytes(StandardCharsets.UTF_8);
            String sourceSha = sha(input);
            int mutationOffset = SOURCE.indexOf("Hello");
            var binary = new GwtAttributedJavaInventory.ClasspathJar(jar, sha(Files.readAllBytes(jar)));
            List<GwtAttributedJavaInventory.ClasspathJar> triggered =
                    new AbstractList<GwtAttributedJavaInventory.ClasspathJar>() {
                        private boolean changed;
                        @Override public int size() { return 1; }
                        @Override public GwtAttributedJavaInventory.ClasspathJar get(int index) {
                            if (index != 0) throw new IndexOutOfBoundsException(index);
                            if (!changed) {
                                input[mutationOffset] = 'J'; // "Hello" becomes "Jello".
                                changed = true;
                            }
                            return binary;
                        }
                    };
            var receipt = new WebWidgetUiBinderProjection.SourceReceipt("demo/Orders.java", sourceSha);
            var inventory = GwtAttributedJavaInventory.inspect(receipt, input, triggered);
            assertFalse(sourceSha.equals(sha(input)), "the caller's array really changed");
            assertEquals(sourceSha, inventory.source().sha256(), "verified identity survives caller mutation");
            assertTrue(inventory.candidateXml().uiBinderXml().contains("text=\"Hello\""),
                    "projection must use the same original bytes as javac attribution");
            assertFalse(inventory.candidateXml().uiBinderXml().contains("Jello"),
                    "mutable caller data must not leak into the output");
            assertEquals(List.of(binary.sha256()), inventory.classpathDigests());
        } finally {
            deleteTree(root);
        }
    }

    @Test void anUnchangedSourceRetainsExactCandidateOutput() throws Exception {
        Path root = Files.createTempDirectory("m3-gwt-source-stable-test-");
        try {
            Path jar = signatures(root);
            byte[] input = SOURCE.getBytes(StandardCharsets.UTF_8);
            byte[] before = input.clone();
            var receipt = new WebWidgetUiBinderProjection.SourceReceipt("demo/Orders.java", sha(input));
            var binary = new GwtAttributedJavaInventory.ClasspathJar(jar, sha(Files.readAllBytes(jar)));
            var result = GwtAttributedJavaInventory.inspect(receipt, input, List.of(binary));
            assertTrue(Arrays.equals(before, input), "inspection must not mutate caller arrays");
            assertEquals(1, result.declarations().size());
            assertTrue(result.candidateXml().uiBinderXml().contains("text=\"Hello\""));
        } finally {
            deleteTree(root);
        }
    }

    @Test void malformedSourceDigestStillRefuses() throws Exception {
        byte[] bytes = SOURCE.getBytes(StandardCharsets.UTF_8);
        var receipt = new WebWidgetUiBinderProjection.SourceReceipt("demo/Orders.java", "0".repeat(64));
        assertThrows(IllegalArgumentException.class, () ->
                GwtAttributedJavaInventory.inspect(receipt, bytes, List.of()));
    }

    @Test void overBudgetInputRefusesBeforeAnySourceSnapshot() throws Exception {
        byte[] oversized = new byte[1_048_577];
        var receipt = new WebWidgetUiBinderProjection.SourceReceipt("demo/Orders.java", "0".repeat(64));
        var rejection = assertThrows(IllegalArgumentException.class, () ->
                GwtAttributedJavaInventory.inspect(receipt, oversized, List.of()));
        assertTrue(rejection.getMessage().contains("SOURCE_BUDGET"));
    }

    @Test void theOriginalCallerByteArrayStillBelongsToTheCaller() throws Exception {
        byte[] bytes = SOURCE.getBytes(StandardCharsets.UTF_8);
        String expected = sha(bytes);
        byte[] independent = bytes.clone();
        independent[SOURCE.indexOf("Hello")] = 'J';
        assertEquals(expected, sha(bytes));
        assertFalse(Arrays.equals(bytes, independent));
    }

    private static Path signatures(Path root) throws Exception {
        Path src = root.resolve("src/com/google/gwt/user/client/ui");
        Path classes = root.resolve("classes");
        Files.createDirectories(src);
        Files.createDirectories(classes);
        Path java = src.resolve("Label.java");
        Files.writeString(java, "package com.google.gwt.user.client.ui; "
                + "public class Label { public Label(String text) {} }\n", StandardCharsets.UTF_8);
        int status = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
                "-d", classes.toString(), java.toString());
        assertEquals(0, status, "first-party test signatures must compile");
        Path jar = root.resolve("typed-source-test-only.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
            var entry = new JarEntry("com/google/gwt/user/client/ui/Label.class");
            entry.setTime(0L);
            output.putNextEntry(entry);
            output.write(Files.readAllBytes(classes.resolve(entry.getName())));
            output.closeEntry();
        }
        return jar;
    }

    private static void deleteTree(Path directory) throws Exception {
        try (var walk = Files.walk(directory)) {
            for (var path : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
