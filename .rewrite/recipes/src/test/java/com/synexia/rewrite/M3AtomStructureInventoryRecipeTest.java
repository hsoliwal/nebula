// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.openrewrite.DataTable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3AtomStructureInventoryRecipeTest {
    @Test
    void exactNormalizationRemovesCommentsButPreservesLiteralContent() {
        String first = "int value = 1; // comment\nString text = \"a b\";";
        String second = "int value=1; /* other */ String text=\"a b\";";

        assertEquals(
                M3AtomStructureInventoryRecipe.normalizeExact(first),
                M3AtomStructureInventoryRecipe.normalizeExact(second));
        assertNotEquals(
                M3AtomStructureInventoryRecipe.normalizeSimilarity(first),
                M3AtomStructureInventoryRecipe.normalizeSimilarity(
                        "long different = 2; String text = \"c d\";"));
        assertEquals(
                M3AtomStructureInventoryRecipe.hashComponents("a", "b").length(),
                64);
        assertEquals(
                M3AtomStructureInventoryRecipe.simHash64("int value = 1;"),
                M3AtomStructureInventoryRecipe.simHash64("int renamed = 9;"));
    }

    @Test
    void recipeEmitsHierarchyAndNeverChangesTheSource() {
        SourceFile source = JavaParser.fromJavaVersion()
                .build()
                .parse(
                        """
                        package demo;
                        /** A documented API. */
                        public interface Port { void run(); }
                        final class Impl implements Port {
                            private final int value = 1;
                            /** Executes the operation. */
                            public void run() { int local = value; }
                        }
                        """)
                .findFirst()
                .orElseThrow();

        M3AtomStructureInventoryRecipe recipe = new M3AtomStructureInventoryRecipe(
                "repo", "project", "module", "lib:a:1,lib:b:1");
        RecipeRun run = recipe.run(
                new InMemoryLargeSourceSet(List.of(source)),
                new InMemoryExecutionContext());

        assertEquals(0, run.getChangeset().size());
        assertFalse(recipe.replacementAuthority());
        Map<String, Integer> tables = run.getDataTables().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getName(),
                        entry -> entry.getValue().size()));
        assertTrue(run.getDataTables().keySet().stream()
                .anyMatch(table -> table.getDisplayName().contains("M3 atom inventory")));
        assertTrue(run.getDataTables().keySet().stream()
                .anyMatch(table -> table.getDisplayName().contains("M3 atom structure hierarchy")));
        assertTrue(tables.values().stream().mapToInt(Integer::intValue).sum() >= 10);

        String atomTableName = new M3AtomStructureInventoryRecipe.AtomTable(recipe).getName();
        List<M3AtomStructureInventoryRecipe.AtomRow> atoms = run.getDataTableRows(atomTableName);
        assertTrue(atoms.size() >= 5);
        assertTrue(atoms.stream().allMatch(atom -> atom.getCompositionSha256().matches("[0-9a-f]{64}")));
        assertTrue(atoms.stream().anyMatch(atom -> atom.getSimHash64() != 0L));
        M3AtomStructureInventoryRecipe.AtomRow first = atoms.get(0);
        assertEquals(
                M3AtomStructureInventoryRecipe.hashComponents(
                        "atom-composition-v1", first.getNormalizedComposition()),
                first.getCompositionSha256());
    }
}

