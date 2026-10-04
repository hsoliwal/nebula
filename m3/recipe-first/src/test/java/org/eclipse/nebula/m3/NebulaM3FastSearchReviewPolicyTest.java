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

final class NebulaM3FastSearchReviewPolicyTest {
    @Test
    void everyCategoryUsesTheRequiredFourPassSerialReview() {
        assertEquals(
                NebulaM3FastSearchReviewPolicy.Category.values().length * 4,
                NebulaM3FastSearchReviewPolicy.allPasses().size());

        for (NebulaM3FastSearchReviewPolicy.Category category :
                NebulaM3FastSearchReviewPolicy.Category.values()) {
            List<NebulaM3FastSearchReviewPolicy.ReviewPass> passes =
                    NebulaM3FastSearchReviewPolicy.passes(category);
            assertEquals(4, passes.size());
            assertEquals(
                    List.of(
                            NebulaM3FastSearchReviewPolicy.EvidenceSource.LEETCODE,
                            NebulaM3FastSearchReviewPolicy.EvidenceSource.HACKERRANK,
                            NebulaM3FastSearchReviewPolicy.EvidenceSource.GEEKSFORGEEKS,
                            NebulaM3FastSearchReviewPolicy.EvidenceSource.GITHUB_DONOR),
                    passes.stream()
                            .map(NebulaM3FastSearchReviewPolicy.ReviewPass::source)
                            .toList());
            assertEquals(List.of(1, 2, 3, 4), passes.stream()
                    .map(NebulaM3FastSearchReviewPolicy.ReviewPass::ordinal)
                    .toList());

            for (int index = 0; index < 3; index++) {
                var pass = passes.get(index);
                assertEquals("REFERENCE_ONLY", pass.disposition());
                assertTrue(pass.donorRepository().isEmpty());
                assertTrue(pass.revision().isEmpty());
                assertTrue(pass.license().isEmpty());
            }
            var donor = passes.get(3);
            assertFalse(donor.donorRepository().isEmpty());
            assertTrue(donor.revision().matches("[0-9a-f]{40}"));
            assertFalse(donor.license().isEmpty());
            assertTrue(donor.reference().contains(donor.donorRepository()));
            assertTrue(donor.reference().contains(donor.revision()));
        }
    }

    @Test
    void fileSignalsMapToCandidateCategoriesWithoutGrantingImplementationAuthority() {
        String source =
                """
                package p;
                import java.util.Collections;
                import java.util.List;
                final class Candidate {
                    static int find(List<String> values, String value) {
                        values.sort(null);
                        if (values.contains(value)) {
                            return values.indexOf(value)
                                    + Collections.binarySearch(values, value);
                        }
                        return -1;
                    }
                }
                """;

        NebulaM3InventoryRecipe.SourceFacts facts =
                NebulaM3InventoryRecipe.analyze(
                        parse(Path.of("widgets/demo/src/p/Candidate.java"), source));

        assertEquals(
                List.of(
                        NebulaM3FastSearchReviewPolicy.Category.BINARY_SEARCH,
                        NebulaM3FastSearchReviewPolicy.Category.ADAPTIVE_ORDER,
                        NebulaM3FastSearchReviewPolicy.Category.PREFIX_FUZZY_SEARCH,
                        NebulaM3FastSearchReviewPolicy.Category.PRIMITIVE_LOOKUP),
                NebulaM3FastSearchReviewPolicy.categoriesFor(facts));
        assertTrue(
                NebulaM3FastSearchReviewPolicy.renderTsv()
                        .contains(
                                "BINARY_SEARCH\t4\tGITHUB_DONOR\t"
                                        + "https://github.com/vigna/fastutil/commit/"));
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
