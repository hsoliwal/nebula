// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class NebulaM3JavaBeforeJniPolicyTest {
    @Test
    void existingNativeDeclarationRequiresFullJavaOracleEvidenceAndNoAuthority() {
        String source =
                """
                package p;
                final class Candidate {
                    private static native int nativeValue();
                    static int find(java.util.List<String> values, String value) {
                        for (String candidate : values) {
                            if (values.contains(value)) return candidate.length();
                        }
                        return -1;
                    }
                }
                """;
        var review = NebulaM3JavaBeforeJniPolicy.review(
                facts("widgets/demo/src/p/Candidate.java", source));

        assertEquals(
                NebulaM3JavaBeforeJniPolicy.Decision.REVIEW_EXISTING_NATIVE_DECLARATION,
                review.decision());
        assertTrue(review.javaOracleRequired());
        assertTrue(review.differentialCorpusRequired());
        assertTrue(review.lifecycleFallbackRequired());
        assertTrue(review.setupIncludedBenchmarkRequired());
        assertFalse(review.nativeExecutionAuthority());
        assertFalse(review.promotionAuthority());
    }

    @Test
    void repeatedSearchLoopIsOnlyAProfileCandidateBeforeJni() {
        String source =
                """
                package p;
                final class Candidate {
                    static int find(java.util.List<String> values, String value) {
                        for (String candidate : values) {
                            if (values.contains(value)) return candidate.length();
                        }
                        return -1;
                    }
                }
                """;
        var review = NebulaM3JavaBeforeJniPolicy.review(
                facts("widgets/demo/src/p/Candidate.java", source));

        assertEquals(
                NebulaM3JavaBeforeJniPolicy.Decision.PROFILE_JAVA_HOT_PATH_BEFORE_JNI,
                review.decision());
        assertTrue(review.javaOracleRequired());
        assertTrue(review.differentialCorpusRequired());
        assertTrue(review.lifecycleFallbackRequired());
        assertTrue(review.setupIncludedBenchmarkRequired());
        assertFalse(review.nativeExecutionAuthority());
        assertFalse(review.promotionAuthority());
    }

    @Test
    void ordinaryJavaWithoutHotPrimitiveHasNoNativeAction() {
        String source =
                """
                package p;
                final class Candidate {
                    static int answer() { return 42; }
                }
                """;
        var review = NebulaM3JavaBeforeJniPolicy.review(
                facts("widgets/demo/src/p/Candidate.java", source));

        assertEquals(NebulaM3JavaBeforeJniPolicy.Decision.NO_NATIVE_ACTION, review.decision());
        assertTrue(review.javaOracleRequired());
        assertFalse(review.differentialCorpusRequired());
        assertFalse(review.lifecycleFallbackRequired());
        assertFalse(review.setupIncludedBenchmarkRequired());
        assertFalse(review.nativeExecutionAuthority());
        assertFalse(review.promotionAuthority());
    }

    private static NebulaM3InventoryRecipe.SourceFacts facts(String path, String source) {
        return NebulaM3InventoryRecipe.analyze(parse(Path.of(path), source));
    }

    private static J.CompilationUnit parse(Path path, String source) {
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(path, source)),
                                null,
                                new InMemoryExecutionContext())) {
            List<SourceFile> files = parsed.toList();
            assertEquals(1, files.size());
            return (J.CompilationUnit) files.getFirst();
        }
    }
}
