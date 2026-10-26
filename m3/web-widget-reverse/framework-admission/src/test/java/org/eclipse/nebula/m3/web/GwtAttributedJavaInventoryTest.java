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
