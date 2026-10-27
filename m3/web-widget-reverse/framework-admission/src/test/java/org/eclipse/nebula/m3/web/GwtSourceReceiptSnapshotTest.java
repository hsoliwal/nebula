// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.m3.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.AbstractList;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/** Source-byte receipt must bind the identical immutable Java that javac analyzed. */
final class GwtSourceReceiptSnapshotTest {
    @Test void mutationAfterHashCannotPoisonOriginalSourceReceipt() throws Exception {
        Path work = Files.createTempDirectory("m3-gwt-source-snapshot-");
        try {
            Path sourceFolder = work.resolve("src/com/google/gwt/user/client/ui");
            Path classes = work.resolve("classes");
            Files.createDirectories(sourceFolder);
            Files.createDirectories(classes);
            Path label = sourceFolder.resolve("Label.java");
            Path button = sourceFolder.resolve("Button.java");
            Files.writeString(label, "package com.google.gwt.user.client.ui; "
                    + "public class Label { public Label(String literal) {} }\n");
            Files.writeString(button, "package com.google.gwt.user.client.ui; "
                    + "public class Button { public Button(String literal) {} }\n");
            int compiled = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "--release", "21", "-proc:none", "-Xlint:all", "-Werror", "-d",
                    classes.toString(), label.toString(), button.toString());
            assertEquals(0, compiled);
            Path jar = work.resolve("test-only-gwt.jar");
            try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
                for (String className : List.of(
                        "com/google/gwt/user/client/ui/Label.class",
                        "com/google/gwt/user/client/ui/Button.class")) {
                    var entry = new JarEntry(className);
                    entry.setTime(0);
                    output.putNextEntry(entry);
                    output.write(Files.readAllBytes(classes.resolve(className)));
                    output.closeEntry();
                }
            }
            String original = "package demo; import com.google.gwt.user.client.ui.Label; "
                    + "public final class Orders { public static void build() { "
                    + "Label title = new Label(\"Before\"); }}";
            byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
            String sourceDigest = sha(bytes);
            var receipt = new WebWidgetUiBinderProjection.SourceReceipt(
                    "demo/Orders.java", sourceDigest);
            var jarReceipt = new GwtAttributedJavaInventory.ClasspathJar(jar, sha(Files.readAllBytes(jar)));
            int mutationOffset = original.indexOf("Before");
            // List.copyOf(jars) takes its snapshot only AFTER the original
            // input digest and javac source text have been computed. The old
            // implementation then passed this modified caller array to the
            // later UiBinder projection and spuriously refused the receipt.
            List<GwtAttributedJavaInventory.ClasspathJar> mutatingClasspath = new AbstractList<>() {
                private boolean mutated;
                @Override public int size() { return 1; }
                @Override public GwtAttributedJavaInventory.ClasspathJar get(int index) {
                    if (index != 0) throw new IndexOutOfBoundsException(index);
                    if (!mutated) {
                        System.arraycopy("Mutate".getBytes(StandardCharsets.UTF_8), 0,
                                bytes, mutationOffset, 6);
                        mutated = true;
                    }
                    return jarReceipt;
                }
            };
            var inspected = GwtAttributedJavaInventory.inspect(receipt, bytes, mutatingClasspath);
            assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("Mutate"),
                    "fixture actually mutated the caller's source array");
            assertEquals("Before", inspected.declarations().getFirst().literalText());
            assertEquals(sourceDigest, inspected.source().sha256());
            assertEquals(sourceDigest, inspected.candidateXml().sourceSha256());
        } finally {
            try (var paths = Files.walk(work)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
