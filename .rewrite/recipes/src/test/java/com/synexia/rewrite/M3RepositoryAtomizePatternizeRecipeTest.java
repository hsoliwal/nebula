// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

final class M3RepositoryAtomizePatternizeRecipeTest {
    @Test
    void composesReadOnlyInventoryAndPatternOwnersInSerialOrder() {
        M3RepositoryAtomizePatternizeRecipe recipe =
                new M3RepositoryAtomizePatternizeRecipe(
                        "synexia-indexstring/src/**/*.java",
                        "mindex jni precompute filesystem search");

        List<Recipe> children = recipe.getRecipeList();
        assertEquals(3, children.size());
        assertInstanceOf(M3RecipeFirstJavaAtomInventoryRecipe.class, children.get(0));

        M3FrameworkRecipePatternReviewRecipe patterns =
                assertInstanceOf(M3FrameworkRecipePatternReviewRecipe.class, children.get(1));
        assertEquals("synexia-indexstring/src/**/*.java", patterns.getSourceFilePattern());
        assertEquals("mindex jni precompute filesystem search", patterns.getProblemTerm());

        M3MechanicalDonorShapeReviewRecipe donors =
                assertInstanceOf(M3MechanicalDonorShapeReviewRecipe.class, children.get(2));
        assertEquals("ALL", donors.getShape());

        assertFalse(recipe.mutationAuthority());
        assertFalse(recipe.sourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
        assertTrue(recipe.getTags().contains("read-only"));
        assertEquals(1, recipe.maxCycles());
    }

    @Test
    void defaultScopeTargetsTheMavenSuppliedJavaSourceSet() {
        M3RepositoryAtomizePatternizeRecipe recipe = new M3RepositoryAtomizePatternizeRecipe();
        assertEquals("**/*.java", recipe.sourceFilePattern());
        assertTrue(recipe.problemTerm().contains("mindex"));
        assertTrue(recipe.problemTerm().contains("filesystem"));
    }
}
