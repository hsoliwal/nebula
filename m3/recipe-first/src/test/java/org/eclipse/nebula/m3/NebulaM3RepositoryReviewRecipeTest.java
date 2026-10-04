// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.eclipse.nebula.m3.review.NebulaM3RepositoryReviewRecipeDag;
import org.junit.jupiter.api.Test;

final class NebulaM3RepositoryReviewRecipeTest {
    @Test
    void reviewDagIsSerialReadOnlyAndOneCycle() {
        assertEquals(
                List.of(
                        "repository-inventory",
                        "fast-search-challenge-donor-review",
                        "java-before-jni-review"),
                NebulaM3RepositoryReviewRecipeDag.atoms().stream()
                        .map(NebulaM3RepositoryReviewRecipeDag.Atom::id)
                        .toList());
        assertEquals(
                List.of(
                        NebulaM3RepositoryReviewRecipeDag.Phase.INVENTORY,
                        NebulaM3RepositoryReviewRecipeDag.Phase.CHALLENGE_DONOR_REVIEW,
                        NebulaM3RepositoryReviewRecipeDag.Phase.JAVA_BEFORE_JNI_REVIEW),
                NebulaM3RepositoryReviewRecipeDag.atoms().stream()
                        .map(NebulaM3RepositoryReviewRecipeDag.Atom::phase)
                        .toList());
        assertTrue(
                NebulaM3RepositoryReviewRecipeDag.atoms().stream()
                        .allMatch(atom -> atom.recipe().maxCycles() == 1));
        assertTrue(
                NebulaM3RepositoryReviewRecipeDag.atoms().stream()
                        .allMatch(atom -> atom.recipe().getTags().contains("read-only")));

        NebulaM3RepositoryReviewRecipe recipe = new NebulaM3RepositoryReviewRecipe();
        assertEquals(3, recipe.getRecipeList().size());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("read-only"));
        assertTrue(recipe.getTags().contains("recipe-dag"));
    }
    @Test
    void reviewDagProducesNoSourceChanges() {
        String source =
                """
                package p;
                import java.util.List;
                final class Candidate {
                    static int find(List<String> values, String value) {
                        for (String candidate : values) {
                            if (values.contains(value)) return values.indexOf(value);
                        }
                        return -1;
                    }
                }
                """;
        SourceFile parsed;
        try (var sources =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of("widgets/demo/src/p/Candidate.java"),
                                                source)),
                                null,
                                new InMemoryExecutionContext())) {
            List<SourceFile> files = sources.toList();
            assertEquals(1, files.size());
            parsed = files.getFirst();
        }

        var run =
                new NebulaM3RepositoryReviewRecipe()
                        .run(
                                new InMemoryLargeSourceSet(List.of(parsed)),
                                new InMemoryExecutionContext());

        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }

}
