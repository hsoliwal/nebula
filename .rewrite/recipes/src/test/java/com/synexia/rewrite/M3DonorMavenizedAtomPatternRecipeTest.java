// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

class M3DonorMavenizedAtomPatternRecipeTest {
    @Test
    void bundleReusesExistingAtomAndAlgorithmOwnersWithoutMutationAuthority() {
        var recipe =
                new M3DonorMavenizedAtomPatternRecipe(
                        "dmikushin/gpu-grep",
                        "gpu-grep",
                        "gpu-grep",
                        "",
                        25);

        List<Recipe> children = recipe.getRecipeList();
        assertEquals(2, children.size());
        assertInstanceOf(M3AtomStructureInventoryRecipe.class, children.get(0));
        assertInstanceOf(M3AlgorithmAtomRecipeCrateRecipe.class, children.get(1));
        assertEquals("dmikushin/gpu-grep", recipe.getRepository());
        assertEquals("gpu-grep", recipe.getProject());
        assertEquals("gpu-grep", recipe.getModule());
        assertEquals(25, recipe.getEvidenceLimit());
        assertFalse(recipe.sourceMutationAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void evidenceLimitAndUnsafeOptionsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3DonorMavenizedAtomPatternRecipe(
                        "repo", "project", "module", "", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3DonorMavenizedAtomPatternRecipe(
                        "repo\nforged", "project", "module", "", 20));
    }
}
