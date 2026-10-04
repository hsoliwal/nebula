// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
}
