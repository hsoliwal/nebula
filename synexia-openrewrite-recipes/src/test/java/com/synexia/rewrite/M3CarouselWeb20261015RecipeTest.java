// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.CreateTextFile;

/** Executes the real recipe; compilation and browser behavior are separate gates. */
final class M3CarouselWeb20261015RecipeTest {
    private static final String CRATE = "carousel-web-forward-20261015";
    private static final String ROOT = "/com/synexia/rewrite/carousel-web-proof/";
    private record Sealed(String kind, String path, String hash, String resource) { }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(e -> { throw new AssertionError(e); });
    }
    private static String resource(String name) {
        try (var stream = M3CarouselWeb20261015RecipeTest.class.getResourceAsStream(name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new AssertionError(e); }
    }
    private static List<Sealed> sealed() {
        return resource(ROOT + "targets.tsv").lines().filter(l -> !l.isBlank() && !l.startsWith("#"))
                .map(l -> { String[] c = l.split("\t", -1); assertEquals(4, c.length);
                    return new Sealed(c[0], c[1], c[2], c[3]); }).toList();
    }
    private static Map<String,String> expected() {
        Map<String,String> result = new TreeMap<>();
        for (Sealed row : sealed()) assertNull(result.put(row.path(), resource(ROOT + row.resource())));
        return result;
    }
    private static SourceFile source(Sealed row, String text) {
        if (!row.kind().equals("java")) return PlainText.builder().id(Tree.randomId()).sourcePath(Path.of(row.path())).text(text).build();
        var parser = JavaParser.fromJavaVersion().build();
        var values = parser.parseInputs(List.of(Parser.Input.fromString(Path.of(row.path()), text)), null, context()).toList();
        assertEquals(1, values.size()); assertInstanceOf(J.CompilationUnit.class, values.getFirst());
        assertEquals(text, values.getFirst().printAll()); return values.getFirst();
    }
    private static List<SourceFile> sources() {
        return sealed().stream().map(r -> source(r, resource(ROOT + r.resource()))).toList();
    }
    private static Recipe named() {
        return Environment.builder().scanRuntimeClasspath("com.synexia.rewrite").build()
                .activateRecipes("com.synexia.rewrite.M3CarouselWebForward20261015");
    }
    @Test void exactNamedRecipeProducesTypedOwnersAndAssets() throws Exception {
        assertExactNamedGeneration(System.getProperty("m3.recipe.generatedDirectory"));
    }
    @Test void exactNamedRecipeWorksWithoutExportDestination() throws Exception {
        assertExactNamedGeneration(null);
    }
    private static void assertExactNamedGeneration(String target) throws Exception {
        var output = named().run(new InMemoryLargeSourceSet(List.of()), context()).getChangeset().getAllResults();
        Map<String,String> actual = new TreeMap<>();
        for (var change : output) {
            assertNull(change.getBefore()); var after = change.getAfter(); assertNotNull(after);
            String path = after.getSourcePath().toString().replace('\\', '/');
            assertNull(actual.put(path, after.printAll()));
            if (path.endsWith(".java")) assertInstanceOf(J.CompilationUnit.class, after);
        }
        assertEquals(expected(), actual);
        // Export only for explicitly configured qualification runs.
        if (target == null) return;
        Path root = Path.of(target).toAbsolutePath().normalize();
        for (var entry : actual.entrySet()) {
            Path destination = root.resolve(entry.getKey()).normalize(); assertTrue(destination.startsWith(root));
            assertFalse(Files.exists(destination)); Files.createDirectories(destination.getParent());
            Files.writeString(destination, entry.getValue(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE_NEW);
        }
    }
    @Test void sealedPostimagesReachFixedPoint() {
        assertTrue(named().run(new InMemoryLargeSourceSet(sources()), context()).getChangeset().getAllResults().isEmpty());
    }
    @Test void allReviewedBytesMatchTheirSha256Seals() throws Exception {
        for (Sealed row : sealed()) {
            assertEquals(row.hash(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(resource(ROOT + row.resource()).getBytes(StandardCharsets.UTF_8))), row.path());
        }
    }
    @Test void refusesEachForeignJavaOwner() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        for (Sealed row : sealed()) if (row.kind().equals("java")) {
            var inventory = recipe.getInitialValue(context());
            recipe.getScanner(inventory).visit(source(row, resource(ROOT + row.resource()) + "\n// foreign\n"), context());
            assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, context()), row.path());
        }
    }
    @Test void refusesEachForeignStructuredTextOwner() {
        var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        for (Sealed row : sealed()) if (row.kind().equals("text")) {
            var inventory = recipe.getInitialValue(context());
            recipe.getScanner(inventory).visit(source(row, resource(ROOT + row.resource()) + "\nforeign\n"), context());
            assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, context()), row.path());
        }
    }
    @Test void duplicateTypedOwnersRefuseBeforeGeneration() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        for (Sealed row : sealed()) if (row.kind().equals("java")) {
            var inventory = recipe.getInitialValue(context()); var file = source(row, resource(ROOT + row.resource()));
            recipe.getScanner(inventory).visit(file, context()); recipe.getScanner(inventory).visit(file, context());
            assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, context()), row.path());
        }
    }
    @Test void webAssetsUseExistingCreateTextFileAndPreserveExistingContent() {
        int count = 0;
        for (Sealed row : sealed()) if (row.kind().equals("asset")) {
            count++; var foreign = source(row, "caller-owned content\n");
            var recipe = new CreateTextFile(resource(ROOT + row.resource()), row.path(), false);
            assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(foreign)), context()).getChangeset().getAllResults().isEmpty());
            assertEquals("caller-owned content\n", foreign.printAll());
        }
        assertTrue(count > 0);
    }
    @Test void sealedGwtLinkerNamesAreJavaIdentifiers() throws Exception {
        int names = 0;
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        for (Sealed row : sealed()) if (row.path().endsWith(".gwt.xml")) {
            var xml = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(
                    resource(ROOT + row.resource()).getBytes(StandardCharsets.UTF_8)));
            for (String tag : List.of("define-linker", "add-linker")) {
                var nodes = xml.getElementsByTagName(tag);
                for (int i = 0; i < nodes.getLength(); i++) {
                    String name = ((org.w3c.dom.Element) nodes.item(i)).getAttribute("name");
                    assertTrue(javax.lang.model.SourceVersion.isIdentifier(name)
                            && !javax.lang.model.SourceVersion.isKeyword(name), row.path() + ": " + name);
                    names++;
                }
            }
        }
        assertEquals(4, names);
        assertFalse(javax.lang.model.SourceVersion.isIdentifier("geometry-single"));
    }
}
