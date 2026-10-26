// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.m3.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/** JDK compiler AST admission; binary donor signatures below are test-only. */
final class GwtAttributedJavaInventoryTest {
    private static final Path SIGNATURE_JAR = testOnlySignatureJar();
    private static final List<GwtAttributedJavaInventory.ClasspathJar> JARS = List.of(
            new GwtAttributedJavaInventory.ClasspathJar(SIGNATURE_JAR, digest(SIGNATURE_JAR)));
    private static final String PREFIX = "package demo;\n"
            + "import com.google.gwt.user.client.ui.Label;\n"
            + "import com.google.gwt.user.client.ui.Button;\n"
            + "public final class Orders { public static void build() {\n";
    private static final String SUFFIX = "\n} }\n";

    private static GwtAttributedJavaInventory.Inventory inspect(String java) throws IOException {
        byte[] bytes = java.getBytes(StandardCharsets.UTF_8);
        return GwtAttributedJavaInventory.inspect(new WebWidgetUiBinderProjection.SourceReceipt(
                "demo/Orders.java", digest(bytes)), bytes, JARS);
    }

    private static String classWith(String statements) { return PREFIX + statements + SUFFIX; }

    @Test void attributedConstructorOwnersProduceExactOriginalUiBinderGrammar() throws Exception {
        var inventory = inspect(classWith("Label title = new Label(\"Café & 🚀\");\n"
                + "Button save = new Button(\"Save\");"));
        String xml = inventory.candidateXml().uiBinderXml();
        assertEquals(2, inventory.declarations().size());
        assertEquals(2, inventory.candidateXml().rowCount());
        assertTrue(xml.contains("<g:Label ui:field=\"title\" text=\"Café &amp; 🚀\"/>"));
        assertTrue(xml.contains("<g:Button ui:field=\"save\" text=\"Save\" enabled=\"true\"/>"));
        assertEquals(1, inventory.classpathDigests().size());
        assertEquals(digest(SIGNATURE_JAR), inventory.classpathDigests().getFirst());
        assertTrue(inventory.runtimeQualificationRequired());
    }

    @Test void sourceWrittenConstructorCannotHideBehindJavacSyntheticConstructor() {
        String withConstructor = PREFIX.replace("public static void build()",
                "public Orders() { System.exit(0); } public static void build()")
                + "Label title = new Label(\"Hello\");" + SUFFIX;
        assertThrows(IllegalArgumentException.class, () -> inspect(withConstructor));
        assertThrows(IllegalArgumentException.class, () -> inspect(PREFIX.replace(
                "public final class Orders {", "public final class Orders { static { System.exit(0); }")
                + "Label title = new Label(\"Hello\");" + SUFFIX));
    }

    @Test void sourceBodyIsClosedToAllOtherStatementsAndEffects() {
        for (String statement : new String[] {
                "Label title = new Label(\"One\"); System.out.println(\"side effect\");",
                "Label title = new Label(\"One\"); title.setText(\"Two\");",
                "Button save = new Button(\"Save\", () -> System.exit(0));",
                "Label title = new Label(System.getProperty(\"name\"));",
                "String titleText = \"One\"; Label title = new Label(titleText);",
                "for (int i=0;i<1;i++) { Label title=new Label(\"One\"); }",
                "var title = new Label(\"One\");",
                "return;"
        }) assertThrows(IllegalArgumentException.class, () -> inspect(classWith(statement)));
    }

    @Test void otherTypesAndUnqualifiedJavaSourceAreNotMappedByNames() {
        for (String code : new String[] {
                "package demo; public final class Orders { public static void build()"
                        + " { String title = new String(\"Hello\"); } }",
                classWith("Button save = new Button(\"<b>Unsafe</b>\");"),
                classWith("Button save = new Button(\"Fish & chips\");"),
                classWith("Label title = new Label(\"One\") { };"),
                classWith("Label title = new Label();"),
                PREFIX.replace("import com.google.gwt.user.client.ui.Label;",
                        "import com.google.gwt.user.client.ui.*;")
                        + "Label title = new Label(\"One\");" + SUFFIX,
                PREFIX.replace("public final class Orders", "@Deprecated public final class Orders")
                        + "Label title = new Label(\"One\");" + SUFFIX,
                PREFIX.replace("package demo;", "package foreign;")
                        + "Label title = new Label(\"One\");" + SUFFIX
        }) assertThrows(IllegalArgumentException.class, () -> inspect(code));
    }

    @Test void commentsAndCodeLookingStringsDoNotExecute() throws Exception {
        var inv = inspect(classWith("// new Button(\"not executable\");\n"
                + "Label title = new Label(\"System.exit(1)\");"));
        assertEquals(1, inv.declarations().size());
        assertTrue(inv.candidateXml().uiBinderXml().contains("System.exit(1)"));
    }

    @Test void sourceDigestAndCanonicalBinaryClasspathMustMatchActualBytes() throws Exception {
        byte[] bytes = classWith("Label title = new Label(\"One\");")
                .getBytes(StandardCharsets.UTF_8);
        var wrong = new WebWidgetUiBinderProjection.SourceReceipt("demo/Orders.java", "0".repeat(64));
        assertThrows(IllegalArgumentException.class, () -> GwtAttributedJavaInventory.inspect(
                wrong, bytes, JARS));
        var good = new WebWidgetUiBinderProjection.SourceReceipt("demo/Orders.java", digest(bytes));
        assertThrows(IllegalArgumentException.class, () -> GwtAttributedJavaInventory.inspect(
                good, bytes, List.of()));
        assertThrows(IllegalArgumentException.class, () -> GwtAttributedJavaInventory.inspect(
                good, bytes, List.of(JARS.getFirst(), JARS.getFirst())));
        var forged = new GwtAttributedJavaInventory.ClasspathJar(SIGNATURE_JAR, "0".repeat(64));
        assertThrows(IllegalArgumentException.class, () -> GwtAttributedJavaInventory.inspect(
                good, bytes, List.of(forged)));
    }

    @Test void invalidUtf8AndOversizeInputsRefuseBeforeCompiling() throws Exception {
        byte[] invalid = {(byte) 0xc3, (byte) 0x28};
        assertThrows(IllegalArgumentException.class, () -> GwtAttributedJavaInventory.inspect(
                new WebWidgetUiBinderProjection.SourceReceipt("demo/Orders.java", digest(invalid)),
                invalid, JARS));
        byte[] large = new byte[1_048_577];
        assertThrows(IllegalArgumentException.class, () -> GwtAttributedJavaInventory.inspect(
                new WebWidgetUiBinderProjection.SourceReceipt("demo/Orders.java", digest(large)),
                large, JARS));
    }

    @Test void compilerAndProjectionEnforceDirectRowAndUnicodeBudgets() throws Exception {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < 256; i++) rows.append("Label r").append(i)
                .append("=new Label(\"Row").append(i).append("\");\n");
        assertEquals(256, inspect(classWith(rows.toString())).declarations().size());
        assertThrows(IllegalArgumentException.class,
                () -> inspect(classWith(rows + "Label overflow=new Label(\"Too many\");")));
        assertThrows(IllegalArgumentException.class,
                () -> inspect(classWith("Label title=new Label(\"Bad  whitespace\");")));
        assertThrows(IllegalArgumentException.class,
                () -> inspect(classWith("Label title=new Label(\"Unpaired\\uD800\");")));
    }

    @Test void repeatedAttributedAnalysisIsStableAcrossWorkers() throws Exception {
        String text = classWith("Label title = new Label(\"Orders\");");
        var first = inspect(text);
        assertEquals(first, inspect(text));
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<java.util.concurrent.Callable<GwtAttributedJavaInventory.Inventory>>();
            for (int i = 0; i < 16; i++) tasks.add(() -> inspect(text));
            for (var future : executor.invokeAll(tasks)) assertEquals(first, future.get());
        }
    }

    @Test void verifiedBinarySnapshotsAreCleanedAfterSuccessAndRefusal() throws Exception {
        long initial = snapshotDirectoryCount();
        inspect(classWith("Label title = new Label(\"Hello\");"));
        assertEquals(initial, snapshotDirectoryCount());
        assertThrows(IllegalArgumentException.class, () -> inspect(classWith(
                "Label title = new Label(System.getProperty(\"effect\"));")));
        assertEquals(initial, snapshotDirectoryCount());

        Path forged = Files.createTempFile("m3-untrusted-gwt-", ".jar");
        try {
            Files.write(forged, Files.readAllBytes(SIGNATURE_JAR));
            Files.writeString(forged, "unsealed suffix", java.nio.file.StandardOpenOption.APPEND);
            byte[] javaSource = classWith("Label title = new Label(\"Hello\");")
                    .getBytes(StandardCharsets.UTF_8);
            var receipt = new WebWidgetUiBinderProjection.SourceReceipt(
                    "demo/Orders.java", digest(javaSource));
            var falseReceipt = new GwtAttributedJavaInventory.ClasspathJar(
                    forged, digest(SIGNATURE_JAR));
            assertThrows(IllegalArgumentException.class, () ->
                    GwtAttributedJavaInventory.inspect(receipt, javaSource, List.of(falseReceipt)));
            assertEquals(initial, snapshotDirectoryCount());
        } finally {
            Files.deleteIfExists(forged);
        }
    }

    @Test void rejectsCorruptAndNoncanonicalClasspathArchives() throws Exception {
        Path broken = Files.createTempFile("m3-invalid-archive-", ".jar");
        Files.writeString(broken, "not a ZIP archive");
        expectArchiveRefusal(broken, "JAR_INVALID_ARCHIVE");
        Path traversal = archiveWithOne("../escaping.class", new byte[] {1});
        expectArchiveRefusal(traversal, "JAR_NON_CANONICAL_OR_DUPLICATE_ENTRY");
        Path backslash = archiveWithOne("com\\forged.class", new byte[] {1});
        expectArchiveRefusal(backslash, "JAR_NON_CANONICAL_OR_DUPLICATE_ENTRY");
        Path empty = Files.createTempFile("m3-empty-archive-", ".jar");
        try (var out = new JarOutputStream(Files.newOutputStream(empty))) { out.flush(); }
        expectArchiveRefusal(empty, "EMPTY_JAR");
    }

    @Test void rejectsLargeCompressedEntryAndExtremeCompressionRatio() throws Exception {
        byte[] zeros = new byte[16 * 1024 * 1024 + 1];
        Path tooLarge = archiveWithOne("compressed-over-budget.bin", zeros);
        expectArchiveRefusal(tooLarge, "JAR_ENTRY_SIZE_OR_RATIO_BUDGET");
        Path extremeRatio = archiveWithOne("highly-compressible.bin", new byte[2 * 1024 * 1024]);
        expectArchiveRefusal(extremeRatio, "JAR_ENTRY_SIZE_OR_RATIO_BUDGET");
    }

    @Test void rejectsStoredEntryWithCorruptedPayloadCrc() throws Exception {
        Path jar = Files.createTempFile("m3-bad-archive-crc-", ".jar");
        byte[] payload = "CRC_ORIGINAL_SENTINEL".getBytes(StandardCharsets.US_ASCII);
        var crc = new java.util.zip.CRC32();
        crc.update(payload);
        try (var stream = new JarOutputStream(Files.newOutputStream(jar))) {
            var entry = new JarEntry("payload.bin");
            entry.setMethod(JarEntry.STORED);
            entry.setSize(payload.length);
            entry.setCompressedSize(payload.length);
            entry.setCrc(crc.getValue());
            stream.putNextEntry(entry);
            stream.write(payload);
            stream.closeEntry();
        }
        byte[] all = Files.readAllBytes(jar);
        int at = -1;
        for (int i = 0; i <= all.length - payload.length; i++) {
            boolean same = true;
            for (int j = 0; j < payload.length; j++) {
                if (all[i + j] != payload[j]) { same = false; break; }
            }
            if (same) { at = i; break; }
        }
        assertTrue(at >= 0);
        all[at] ^= 1; // preserve central-directory CRC, alter actual data
        Files.write(jar, all);
        expectArchiveRefusal(jar, "JAR_ENTRY_CRC_MISMATCH");
    }

    @Test void rejectsPathologicalJarDirectoryEntryCount() throws Exception {
        Path crowd = Files.createTempFile("m3-too-many-entries-", ".jar");
        try (var stream = new JarOutputStream(Files.newOutputStream(crowd))) {
            for (int i = 0; i <= 12_000; i++) {
                stream.putNextEntry(new JarEntry("class-" + i + ".class"));
                stream.write(1);
                stream.closeEntry();
            }
        }
        expectArchiveRefusal(crowd, "JAR_ENTRY_COUNT_BUDGET");
    }

    private static Path archiveWithOne(String name, byte[] payload) throws IOException {
        Path jar = Files.createTempFile("m3-archive-candidate-", ".jar");
        try (var stream = new JarOutputStream(Files.newOutputStream(jar))) {
            stream.putNextEntry(new JarEntry(name));
            stream.write(payload);
            stream.closeEntry();
        }
        return jar;
    }

    private static void expectArchiveRefusal(Path jar, String reason) throws Exception {
        long before = snapshotDirectoryCount();
        try {
            byte[] bytes = classWith("Label title = new Label(\"Hello\");")
                    .getBytes(StandardCharsets.UTF_8);
            var source = new WebWidgetUiBinderProjection.SourceReceipt(
                    "demo/Orders.java", digest(bytes));
            var binary = new GwtAttributedJavaInventory.ClasspathJar(jar, digest(jar));
            var failure = assertThrows(IllegalArgumentException.class, () ->
                    GwtAttributedJavaInventory.inspect(source, bytes, List.of(binary)));
            assertTrue(failure.getMessage().contains(reason));
            assertEquals(before, snapshotDirectoryCount());
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    private static long snapshotDirectoryCount() throws IOException {
        try (var children = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return children.filter(path -> path.getFileName().toString()
                    .startsWith("m3-gwt-ast-classpath-")).count();
        }
    }

    private static Path testOnlySignatureJar() {
        try {
            Path root = Files.createTempDirectory("m3-typed-gwt-test-only-");
            Path source = root.resolve("src/com/google/gwt/user/client/ui");
            Path classes = root.resolve("classes");
            Files.createDirectories(source);
            Files.createDirectories(classes);
            Path label = source.resolve("Label.java");
            Path button = source.resolve("Button.java");
            Files.writeString(label, "package com.google.gwt.user.client.ui; "
                    + "public class Label { public Label(String text) {} "
                    + "public void setText(String text) {} }\n", StandardCharsets.UTF_8);
            Files.writeString(button, "package com.google.gwt.user.client.ui; "
                    + "public class Button { public Button(String html) {} "
                    + "public Button(String html, Runnable handler) {} }\n", StandardCharsets.UTF_8);
            int status = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "--release", "21", "-proc:none", "-d", classes.toString(),
                    label.toString(), button.toString());
            if (status != 0) throw new AssertionError("test-only typed fixture did not compile");
            Path jar = root.resolve("test-only-gwt-signatures.jar");
            try (var stream = new JarOutputStream(Files.newOutputStream(jar))) {
                for (String name : new String[] {
                        "com/google/gwt/user/client/ui/Label.class",
                        "com/google/gwt/user/client/ui/Button.class"}) {
                    var entry = new JarEntry(name);
                    entry.setTime(0L);
                    stream.putNextEntry(entry);
                    stream.write(Files.readAllBytes(classes.resolve(name)));
                    stream.closeEntry();
                }
            }
            jar.toFile().deleteOnExit();
            return jar;
        } catch (IOException io) { throw new ExceptionInInitializerError(io); }
    }

    private static String digest(Path path) {
        try { return digest(Files.readAllBytes(path)); }
        catch (IOException failure) { throw new ExceptionInInitializerError(failure); }
    }

    private static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
