// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.m3.web;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;

/** Deterministic test-only counterexample for caller-owned source-byte-array drift. */
public final class GwtImmutableSourceSnapshotTest {
    @Test
    void sourceBytesCannotDriftBetweenAttributionAndProjection() throws Exception {
        Path root = Files.createTempDirectory("m3-source-bytes-counterexample-");
        try {
            Path archive = signatures(root);
            String source = "package demo; import com.google.gwt.user.client.ui.Label; "
                    + "public final class Orders { public static void build() { "
                    + "Label title = new Label(\"Hello\"); } }";
            byte[] mutableInput = source.getBytes(StandardCharsets.UTF_8);
            var receipt = new WebWidgetUiBinderProjection.SourceReceipt(
                    "demo/Orders.java", sha(mutableInput));
            var jar = new GwtAttributedJavaInventory.ClasspathJar(archive,
                    sha(Files.readAllBytes(archive)));
            Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
            Set<Path> preexisting = activeSnapshotDirectories(tempDir);
            var result = CompletableFuture.supplyAsync(() -> {
                try {
                    return GwtAttributedJavaInventory.inspect(
                            receipt, mutableInput, List.of(jar));
                } catch (IOException checked) {
                    throw new CompletionException(checked);
                }
            });
            long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            boolean analyzerReached = false;
            while (System.nanoTime() < limit && !result.isDone()) {
                Set<Path> current = activeSnapshotDirectories(tempDir);
                current.removeAll(preexisting);
                if (!current.isEmpty()) {
                    analyzerReached = true;
                    break;
                }
                Thread.sleep(1);
            }
            if (!analyzerReached) {
                throw new AssertionError("Timed out waiting for private JAR snapshot stage");
            }
            int changedAt = source.indexOf("Hello");
            if (changedAt < 0 || mutableInput[changedAt] != 'H') {
                throw new AssertionError("Missing original mutable-source sentinel");
            }
            // This happens after the source SHA and decode but before the
            // original implementation projects the same mutable array again.
            mutableInput[changedAt] = 'J';
            var analyzed = result.get(15, TimeUnit.SECONDS);
            if (!analyzed.source().sha256().equals(receipt.sha256())) {
                throw new AssertionError("Output source receipt changed");
            }
            if (analyzed.declarations().size() != 1
                    || !analyzed.declarations().getFirst().literalText().equals("Hello")
                    || !analyzed.candidateXml().uiBinderXml().contains("Hello")) {
                throw new AssertionError("Attributed source drifted from first verified bytes");
            }
            if (mutableInput[changedAt] != 'J') {
                throw new AssertionError("Fixture failed to mutate caller-owned array");
            }
            System.out.println("PASS 5 source-byte snapshot/receipt coherence assertions");
            System.out.println("SOURCE_SHA256 " + receipt.sha256());
            System.out.println("ORIGINAL_LITERAL Hello, MUTATED_CALLER_LITERAL Jello");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static Set<Path> activeSnapshotDirectories(Path tempDir) throws IOException {
        try (var paths = Files.list(tempDir)) {
            return new HashSet<>(paths.filter(p -> p.getFileName().toString()
                    .startsWith("m3-gwt-ast-classpath-")).toList());
        }
    }

    private static Path signatures(Path root) throws IOException {
        Path sourceDir = root.resolve("src/com/google/gwt/user/client/ui");
        Path classes = root.resolve("classes");
        Files.createDirectories(sourceDir);
        Files.createDirectories(classes);
        Path label = sourceDir.resolve("Label.java");
        Files.writeString(label,
                "package com.google.gwt.user.client.ui; "
                        + "public class Label { public Label(String text) {} }",
                StandardCharsets.UTF_8);
        int status = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
                "-d", classes.toString(), label.toString());
        if (status != 0) throw new AssertionError("Test-only signature compile failed");
        Path jar = root.resolve("test-only-gwt-signatures.jar");
        try (var zip = new JarOutputStream(Files.newOutputStream(jar))) {
            var entry = new JarEntry("com/google/gwt/user/client/ui/Label.class");
            entry.setTime(0L);
            zip.putNextEntry(entry);
            zip.write(Files.readAllBytes(classes.resolve(entry.getName())));
            zip.closeEntry();
        }
        return jar;
    }

    private static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
