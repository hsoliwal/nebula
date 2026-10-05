// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class NebulaM3SecondPassReviewDagBindingTest {
    @Test
    void exactBindingIsPinnedAndAuthorityFree() {
        assertEquals(
                "hsoliwal/com.synexia",
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_REPOSITORY);
        assertEquals(
                "feat/m3-second-pass-review-dag-20261005",
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_BRANCH);
        assertEquals(
                "5369fdc8c076b998b0dd39c7c67c85d11a4b2d8f",
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_COMMIT);
        assertEquals(8973, NebulaM3SecondPassReviewDagBinding.UPSTREAM_PR);
        assertEquals(
                "com.synexia.m3.recipe.M3SecondPassReviewDagPlan",
                NebulaM3SecondPassReviewDagBinding.PLAN_CLASS);
        assertEquals(
                "m3_second_pass_review",
                NebulaM3SecondPassReviewDagBinding.DAG_ID);
        assertEquals(
                "org.eclipse.nebula.m3.SecondPassReviewDag",
                NebulaM3SecondPassReviewDagBinding.LOCAL_RECIPE);

        assertEquals("FILE", NebulaM3SecondPassReviewDagBinding.SCOPE);
        assertEquals("DRY_RUN", NebulaM3SecondPassReviewDagBinding.GOAL);
        assertEquals(4, NebulaM3SecondPassReviewDagBinding.ATOM_IDS.size());
        assertEquals(4, NebulaM3SecondPassReviewDagBinding.RECIPE_OWNERS.size());
        assertEquals(4, NebulaM3SecondPassReviewDagBinding.MAVEN_PROFILES.size());
        assertEquals(
                List.of("LeetCode", "HackerRank", "GeeksforGeeks"),
                NebulaM3SecondPassReviewDagBinding.CHALLENGE_ORDER);

        assertTrue(NebulaM3SecondPassReviewDagBinding.camelProjection());
        assertTrue(NebulaM3SecondPassReviewDagBinding.airflowProjection());
        assertTrue(NebulaM3SecondPassReviewDagBinding.droolsAdmission());
        assertFalse(NebulaM3SecondPassReviewDagBinding.internalSignalLeafFanOut());
        assertFalse(NebulaM3SecondPassReviewDagBinding.sourceMutationAuthority());
        assertFalse(NebulaM3SecondPassReviewDagBinding.semanticEquivalenceAuthority());
        assertFalse(NebulaM3SecondPassReviewDagBinding.replacementAuthority());
        assertFalse(NebulaM3SecondPassReviewDagBinding.nativeExecutionAuthority());
        assertFalse(NebulaM3SecondPassReviewDagBinding.promotionAuthority());
        assertEquals(
                "PENDING_HOSTED_PROOF",
                NebulaM3SecondPassReviewDagBinding.HOSTED_PROOF);
    }

    @Test
    void machineLedgerMatchesTheJavaOwnerExactly() throws Exception {
        Path repository = repositoryRoot();
        Path ledger =
                repository.resolve(
                        "m3/catalogue/second-pass-review-dag-binding.tsv");

        assertTrue(Files.isRegularFile(ledger));
        assertEquals(
                NebulaM3SecondPassReviewDagBinding.tsv(),
                Files.readString(ledger));
    }

    @Test
    void declarativeRecipePreservesTheExactFourAtomOrder() throws Exception {
        Path repository = repositoryRoot();
        String yaml =
                Files.readString(
                        repository.resolve(
                                "m3/recipe-first/src/main/resources/META-INF/rewrite/"
                                        + "nebula-m3-second-pass-review-dag.yml"));

        assertTrue(
                yaml.contains(
                        "name: "
                                + NebulaM3SecondPassReviewDagBinding.LOCAL_RECIPE));
        int previous = -1;
        for (String owner : NebulaM3SecondPassReviewDagBinding.RECIPE_OWNERS) {
            int current = yaml.indexOf("  - " + owner);
            assertTrue(current > previous, owner);
            previous = current;
        }
        assertFalse(yaml.contains("rewrite:run"));
        assertFalse(yaml.contains("promotionAuthority"));
    }

    @Test
    void rootPomPolicyAndConvergencePlanWireTheReadOnlyDag() throws Exception {
        Path repository = repositoryRoot();
        String pom = Files.readString(repository.resolve("pom.xml"));
        String policy =
                Files.readString(
                        repository.resolve(".m3/atom-pattern/policy.properties"));
        String plan =
                Files.readString(repository.resolve("m3/convergence/PLAN.tsv"));

        assertTrue(pom.contains("<id>m3-second-pass-review-dag</id>"));
        assertTrue(
                pom.contains(
                        "<recipe>"
                                + NebulaM3SecondPassReviewDagBinding.LOCAL_RECIPE
                                + "</recipe>"));
        assertTrue(pom.contains("<artifactId>synexia-openrewrite-recipes</artifactId>"));
        assertTrue(pom.contains("<artifactId>nebula-m3-recipe-first</artifactId>"));

        assertTrue(
                policy.contains(
                        "java.secondPassReviewDagRecipe="
                                + NebulaM3SecondPassReviewDagBinding.LOCAL_RECIPE));
        assertTrue(
                policy.contains(
                        "java.secondPassReviewDagUpstreamCommit="
                                + NebulaM3SecondPassReviewDagBinding.UPSTREAM_COMMIT));
        assertTrue(
                policy.contains(
                        "java.secondPassReviewDagPlanClass="
                                + NebulaM3SecondPassReviewDagBinding.PLAN_CLASS));
        assertTrue(policy.contains("java.secondPassReviewDagMutation=false"));
        assertTrue(policy.contains("java.secondPassReviewDagPromotion=false"));

        assertTrue(
                plan.contains(
                        "4\tSECOND_PASS_REVIEW_DAG\t"
                                + NebulaM3SecondPassReviewDagBinding.LOCAL_RECIPE
                                + "\tFILE\tREAD_ONLY\t"));
        assertTrue(plan.contains("8\tFILE_FIXED_POINT\t"));
    }

    @Test
    void exactBindingFailsClosedOnDrift() {
        NebulaM3SecondPassReviewDagBinding.requireExact(
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_REPOSITORY,
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_BRANCH,
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_COMMIT,
                NebulaM3SecondPassReviewDagBinding.UPSTREAM_PR,
                NebulaM3SecondPassReviewDagBinding.PLAN_CLASS,
                NebulaM3SecondPassReviewDagBinding.DAG_ID,
                NebulaM3SecondPassReviewDagBinding.LOCAL_RECIPE);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3SecondPassReviewDagBinding.requireExact(
                                NebulaM3SecondPassReviewDagBinding.UPSTREAM_REPOSITORY,
                                NebulaM3SecondPassReviewDagBinding.UPSTREAM_BRANCH,
                                "0".repeat(40),
                                NebulaM3SecondPassReviewDagBinding.UPSTREAM_PR,
                                NebulaM3SecondPassReviewDagBinding.PLAN_CLASS,
                                NebulaM3SecondPassReviewDagBinding.DAG_ID,
                                NebulaM3SecondPassReviewDagBinding.LOCAL_RECIPE));
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("widgets"))
                    && Files.isDirectory(current.resolve("m3"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Nebula repository root not found");
    }
}
