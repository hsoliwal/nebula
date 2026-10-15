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

/** Exact Java-LST replay and fail-closed test for Drools predecessor completion. */
final class NebulaM3DroolsCompletionGateRecipeTest {
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/drools-completion-gate/";
    private static final String BEFORE = ROOT + "NebulaM3OrchestratorPlans.before.java.txt";

    @Test
    void exactHashesPathsAndScopeAreSealed() {
        var recipe = new NebulaM3DroolsCompletionGateRecipe();
        assertEquals(recipe.BEFORE_SHA256,
                NebulaM3ExactJavaSnapshotRecipe.sha256(resource(BEFORE)));
        assertEquals(recipe.AFTER_SHA256,
                NebulaM3ExactJavaSnapshotRecipe.sha256(resource(recipe.POSTIMAGE)));
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("recipe-first"));
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.matches(Path.of(recipe.PATH)));
        assertTrue(recipe.matches(Path.of(recipe.MODULE_PATH)));
        assertFalse(recipe.matches(Path.of("unrelated/NebulaM3OrchestratorPlans.java")));
    }

    @Test
    void structuredJavaCandidateReachesUnchangedSecondPass() {
        var recipe = new NebulaM3DroolsCompletionGateRecipe();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile before = parse(recipe.PATH, resource(BEFORE), context);

        var first = recipe.run(new InMemoryLargeSourceSet(List.of(before)), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile post = changes.getFirst().getAfter();
        assertTrue(post instanceof J.CompilationUnit);
        assertEquals(resource(recipe.POSTIMAGE), post.printAll());
        assertTrue(post.printAll().contains("complete == true, proofSha256 matches"));

        var secondErrors = new ArrayList<Throwable>();
        var second = new NebulaM3DroolsCompletionGateRecipe().run(
                new InMemoryLargeSourceSet(List.of(post)),
                new InMemoryExecutionContext(secondErrors::add));
        assertTrue(secondErrors.isEmpty(), secondErrors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void sourceDriftAndUnrelatedFilesCannotReceiveMutation() {
        var recipe = new NebulaM3DroolsCompletionGateRecipe();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile drift = parse(recipe.PATH, resource(BEFORE) + "// unexpected\n", context);
        String immutable = drift.printAll();
        var rejected = recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context);
        assertTrue(rejected.getChangeset().getAllResults().isEmpty());
        assertFalse(errors.isEmpty());
        assertEquals(immutable, drift.printAll());

        var otherErrors = new ArrayList<Throwable>();
        var otherContext = new InMemoryExecutionContext(otherErrors::add);
        SourceFile unrelated = parse(
                "unrelated/NebulaM3OrchestratorPlans.java",
                resource(BEFORE),
                otherContext);
        var noChange = recipe.run(
                new InMemoryLargeSourceSet(List.of(unrelated)),
                otherContext);
        assertTrue(otherErrors.isEmpty(), otherErrors.toString());
        assertTrue(noChange.getChangeset().getAllResults().isEmpty());
    }

    private static SourceFile parse(
            String path, String body, InMemoryExecutionContext context) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), body)),
                        null,
                        context)
                .toList();
        assertEquals(1, parsed.size());
        assertTrue(parsed.getFirst() instanceof J.CompilationUnit);
        return parsed.getFirst();
    }

    private static String resource(String name) {
        try (var input =
                NebulaM3DroolsCompletionGateRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) throw new IllegalStateException("missing source " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
