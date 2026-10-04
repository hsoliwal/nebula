// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.fastsearch.problem.ProblemCatalogueTsv;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

class M3AlgorithmAtomRecipeCrateRecipeTest {
    @Test
    void recipeCreatesOnlyAuditManifestAndThenReachesFixedPoint() {
        String source =
                """
                package demo;
                class Searcher {
                    int find(int[] values, int key) {
                        return java.util.Arrays.binarySearch(values, key);
                    }
                    native int nativeLookup(int key);
                }
                """;
        SourceFile javaSource =
                javaSource(
                        "src/main/java/demo/Searcher.java",
                        source);
        PlainText recipeCatalogue =
                PlainText.builder()
                        .sourcePath(Path.of("m3-recipe-catalogue.tsv"))
                        .text(
                                M3RecipeCatalogueTsv.HEADER
                                        + "\n"
                                        + "algorithm.shape.binary_search\t"
                                        + "com.synexia.rewrite.BinarySearchMechanicalRecipe\t"
                                        + "a".repeat(64)
                                        + "\t"
                                        + "b".repeat(64)
                                        + "\tm3,recipe-first\n")
                        .build();
        PlainText problemCatalogue =
                PlainText.builder()
                        .sourcePath(Path.of("problem-catalogue.tsv"))
                        .text(problemTsv())
                        .build();
        String output = "m3-algorithm-atom-recipe-crates.tsv";
        var recipe =
                new M3AlgorithmAtomRecipeCrateRecipe(12, output);
        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(
                                List.of(
                                        javaSource,
                                        recipeCatalogue,
                                        problemCatalogue)),
                        context(),
                        1);
        var changes = first.getChangeset().getAllResults();

        assertEquals(1, changes.size());
        SourceFile manifest = changes.getFirst().getAfter();
        assertEquals(Path.of(output), manifest.getSourcePath());
        String text = manifest.printAll();
        assertTrue(
                text.contains(
                        "#M3_ALGORITHM_ATOM_RECIPE_CRATE_PLAN_V2"));
        assertTrue(text.contains("BINDING"));
        assertTrue(text.contains("BINARY_SEARCH"));
        assertTrue(text.contains("nativeDeclarations"));
        assertTrue(text.contains("nativeDonorCatalogueRoot"));
        assertTrue(text.contains("nativeDonorReviewRoot"));
        String bindingHeader =
                text.lines().filter(line -> line.startsWith("recordType\tserialOrdinal")).findFirst().orElseThrow();
        String bindingRow =
                text.lines().filter(line -> line.startsWith("BINDING\t")).findFirst().orElseThrow();
        String[] headerCells = bindingHeader.split("\\t", -1);
        String[] bindingCells = bindingRow.split("\\t", -1);
        assertEquals(headerCells.length, bindingCells.length);
        assertTrue(bindingCells[indexOf(headerCells, "capabilityPlanRoot")].matches("[0-9a-f]{64}"));
        assertTrue(bindingCells[indexOf(headerCells, "nativeDonorCatalogueRoot")].matches("[0-9a-f]{64}"));
        assertTrue(bindingCells[indexOf(headerCells, "nativeDonorReviewRoot")].matches("[0-9a-f]{64}"));
        assertTrue(
                text.lines()
                        .anyMatch(
                                line ->
                                        line.startsWith("FILE\t0\t")
                                                && line.contains("\t1\t")));
        assertFalse(recipe.sourceMutationAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
        assertTrue(recipe.getTags().contains("jni"));
        assertTrue(recipe.getTags().contains("leetcode"));
        assertTrue(recipe.getTags().contains("hackerrank"));
        assertTrue(recipe.getTags().contains("geeksforgeeks"));

        var secondInputs = new ArrayList<SourceFile>();
        secondInputs.add(javaSource);
        secondInputs.add(recipeCatalogue);
        secondInputs.add(problemCatalogue);
        secondInputs.add(manifest);
        var second =
                recipe.run(
                        new InMemoryLargeSourceSet(secondInputs),
                        context(),
                        1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void driftedManifestFailsClosed() {
        String source =
                "class Searcher { int find(int[] a, int x) { "
                        + "return java.util.Arrays.binarySearch(a, x); } }";
        SourceFile javaSource =
                javaSource("src/Searcher.java", source);
        PlainText drifted =
                PlainText.builder()
                        .sourcePath(
                                Path.of(
                                        "m3-algorithm-atom-recipe-crates.tsv"))
                        .text("stale\n")
                        .build();

        AssertionError failure = assertThrows(
                AssertionError.class,
                () ->
                        new M3AlgorithmAtomRecipeCrateRecipe(
                                        5,
                                        "m3-algorithm-atom-recipe-crates.tsv")
                                .run(
                                        new InMemoryLargeSourceSet(
                                                List.of(
                                                        javaSource,
                                                        drifted)),
                                        context(),
                                        1));
        assertEquals(IllegalStateException.class, failure.getCause().getClass());
        assertEquals("algorithm atom recipe-crate manifest drift: m3-algorithm-atom-recipe-crates.tsv",
                failure.getCause().getMessage());
        assertEquals("stale\n", drifted.getText());
    }

    @Test
    void invalidManifestPathFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3AlgorithmAtomRecipeCrateRecipe(
                                20, "../outside.tsv"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3AlgorithmAtomRecipeCrateRecipe(
                                0, ""));
    }

    private static int indexOf(String[] headers, String name) {
        for (int index = 0; index < headers.length; index++) {
            if (headers[index].equals(name)) return index;
        }
        throw new AssertionError("missing manifest column: " + name);
    }

    private static SourceFile javaSource(
            String path, String source) {
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(
                                Parser.Input.fromString(
                                        Path.of(path), source)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static String problemTsv() {
        return ProblemCatalogueTsv.HEADER
                + "\n"
                + "LEETCODE\tlc-binary\tBinary Search\t"
                + "https://leetcode.com/problems/binary-search/\t"
                + "SEARCH\tO(log n)\ttrue\tmetadata-only\n"
                + "HACKERRANK\thr-search\tIce Cream Parlor\t"
                + "https://www.hackerrank.com/challenges/icecream-parlor/problem\t"
                + "SEARCH\tO(log n)\ttrue\tmetadata-only\n"
                + "GEEKSFORGEEKS\tgfg-binary\tBinary Search\t"
                + "https://www.geeksforgeeks.org/problems/binary-search/1\t"
                + "SEARCH\tO(log n)\ttrue\tmetadata-only\n";
    }
}
