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

/** FILE-local, structured-LST proof for the exact final-receipt DAG identity recipe. */
final class NebulaM3ProofReceiptDagIdentityRecipeTest {
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/proof-receipt-dag/";
    private static final String BEFORE =
            ROOT + "NebulaM3ProofReceiptCli.before.java.txt";

    @Test
    void sourceHashesPathsAndAuthorityArePinned() {
        var recipe = new NebulaM3ProofReceiptDagIdentityRecipe();
        assertEquals(
                NebulaM3ProofReceiptDagIdentityRecipe.BEFORE_SHA256,
                NebulaM3ExactJavaSnapshotRecipe.sha256(resource(BEFORE)));
        assertEquals(
                NebulaM3ProofReceiptDagIdentityRecipe.AFTER_SHA256,
                NebulaM3ExactJavaSnapshotRecipe.sha256(
                        resource(NebulaM3ProofReceiptDagIdentityRecipe.POSTIMAGE)));
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("recipe-first"));
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.matches(Path.of(NebulaM3ProofReceiptDagIdentityRecipe.PATH)));
        assertTrue(recipe.matches(Path.of(NebulaM3ProofReceiptDagIdentityRecipe.MODULE_PATH)));
        assertFalse(recipe.matches(Path.of("unrelated/NebulaM3ProofReceiptCli.java")));
    }

    @Test
    void exactJavaPreimageReplaysAndSecondRunIsUnchanged() {
        var recipe = new NebulaM3ProofReceiptDagIdentityRecipe();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile before = parse(
                NebulaM3ProofReceiptDagIdentityRecipe.PATH, resource(BEFORE), context);

        var first = recipe.run(new InMemoryLargeSourceSet(List.of(before)), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var results = first.getChangeset().getAllResults();
        assertEquals(1, results.size());
        SourceFile post = results.getFirst().getAfter();
        assertTrue(post instanceof J.CompilationUnit);
        assertEquals(resource(NebulaM3ProofReceiptDagIdentityRecipe.POSTIMAGE), post.printAll());
        assertTrue(post.printAll().contains("final proof receipt DAG identity mismatch"));

        var secondErrors = new ArrayList<Throwable>();
        var second = new NebulaM3ProofReceiptDagIdentityRecipe().run(
                new InMemoryLargeSourceSet(List.of(post)),
                new InMemoryExecutionContext(secondErrors::add));
        assertTrue(secondErrors.isEmpty(), secondErrors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void driftFailsClosedAndUnrelatedCompilationUnitsAreUnchanged() {
        var recipe = new NebulaM3ProofReceiptDagIdentityRecipe();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile drift = parse(
                NebulaM3ProofReceiptDagIdentityRecipe.PATH,
                resource(BEFORE) + "// unexpected\n",
                context);
        String exact = drift.printAll();
        var rejected = recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context);
        assertTrue(rejected.getChangeset().getAllResults().isEmpty());
        assertFalse(errors.isEmpty());
        assertEquals(exact, drift.printAll());

        var unrelatedErrors = new ArrayList<Throwable>();
        var unrelatedContext = new InMemoryExecutionContext(unrelatedErrors::add);
        SourceFile unrelated = parse(
                "unrelated/NebulaM3ProofReceiptCli.java", resource(BEFORE), unrelatedContext);
        var unchanged = recipe.run(
                new InMemoryLargeSourceSet(List.of(unrelated)), unrelatedContext);
        assertTrue(unrelatedErrors.isEmpty(), unrelatedErrors.toString());
        assertTrue(unchanged.getChangeset().getAllResults().isEmpty());
    }

    private static SourceFile parse(
            String path, String body, InMemoryExecutionContext context) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
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
                NebulaM3ProofReceiptDagIdentityRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) throw new IllegalStateException("missing source " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
