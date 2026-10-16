// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** JUnit trust gate for the exact Java-LST publisher recipe and its fixed point. */
final class NebulaM3DagEvidenceAtomicRecipeTest {
    private static final String BEFORE =
            "/org/eclipse/nebula/m3/rewrite/exact/dag-atomic-evidence/"
                    + "NebulaM3RecipeDagManifestCli.before.java.txt";

    @Test
    void pinsExactJavaSourcesAndScope() {
        var recipe = new NebulaM3DagEvidenceAtomicRecipe();
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.matches(Path.of(NebulaM3DagEvidenceAtomicRecipe.PATH)));
        assertTrue(recipe.matches(Path.of(NebulaM3DagEvidenceAtomicRecipe.MODULE_PATH)));
        assertFalse(recipe.matches(Path.of("src/main/java/example/Other.java")));
        assertTrue(recipe.getTags().contains("candidate-only"));
        assertEquals(recipe.expectedBeforeSha256(),
                NebulaM3ExactJavaSnapshotRecipe.sha256(resource(BEFORE)));
        assertEquals(recipe.expectedAfterSha256(),
                NebulaM3ExactJavaSnapshotRecipe.sha256(
                        resource(NebulaM3DagEvidenceAtomicRecipe.POSTIMAGE)));
    }

    @Test
    void transformsOneStructuredJavaFileAndReplaysToFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile source = java(
                NebulaM3DagEvidenceAtomicRecipe.PATH, resource(BEFORE), context);
        var first = new NebulaM3DagEvidenceAtomicRecipe()
                .run(new InMemoryLargeSourceSet(List.of(source)), context);

        assertTrue(errors.isEmpty(), errors.toString());
        var results = first.getChangeset().getAllResults();
        assertEquals(1, results.size());
        SourceFile after = results.getFirst().getAfter();
        assertTrue(after instanceof J.CompilationUnit);
        assertEquals(resource(NebulaM3DagEvidenceAtomicRecipe.POSTIMAGE), after.printAll());

        var secondErrors = new ArrayList<Throwable>();
        var second = new NebulaM3DagEvidenceAtomicRecipe().run(
                new InMemoryLargeSourceSet(List.of(after)),
                new InMemoryExecutionContext(secondErrors::add));
        assertTrue(secondErrors.isEmpty(), secondErrors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void refusesDriftAndIgnoresUnrelatedJava() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile drift = java(NebulaM3DagEvidenceAtomicRecipe.PATH,
                resource(BEFORE) + "// changed\n", context);
        var rejected = new NebulaM3DagEvidenceAtomicRecipe()
                .run(new InMemoryLargeSourceSet(List.of(drift)), context);
        assertTrue(rejected.getChangeset().getAllResults().isEmpty());
        assertFalse(errors.isEmpty());

        var unrelatedErrors = new ArrayList<Throwable>();
        var unrelatedContext = new InMemoryExecutionContext(unrelatedErrors::add);
        SourceFile other = java("somewhere/Other.java", resource(BEFORE), unrelatedContext);
        var noChange = new NebulaM3DagEvidenceAtomicRecipe().run(
                new InMemoryLargeSourceSet(List.of(other)), unrelatedContext);
        assertTrue(noChange.getChangeset().getAllResults().isEmpty());
        assertTrue(unrelatedErrors.isEmpty(), unrelatedErrors.toString());
    }

    private static SourceFile java(
            String path, String text, InMemoryExecutionContext context) {
        List<SourceFile> files = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), text)), null, context)
                .toList();
        assertEquals(1, files.size());
        assertTrue(files.getFirst() instanceof J.CompilationUnit);
        return files.getFirst();
    }

    private static String resource(String path) {
        try (var input = NebulaM3DagEvidenceAtomicRecipeTest.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("missing recipe resource: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
