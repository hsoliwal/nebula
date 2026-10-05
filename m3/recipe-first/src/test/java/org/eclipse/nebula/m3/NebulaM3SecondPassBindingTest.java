// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NebulaM3SecondPassBindingTest {
    @Test
    void exactBindingIsAuthorityFreeAndContentIsPinned() {
        assertEquals("hsoliwal/com.synexia", NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY);
        assertEquals("develop", NebulaM3SecondPassBinding.UPSTREAM_BRANCH);
        assertEquals(
                "1cd108647f1eb1d5c288d917acbf7e8e635c8ce9",
                NebulaM3SecondPassBinding.UPSTREAM_COMMIT);
        assertEquals(8925, NebulaM3SecondPassBinding.UPSTREAM_PR);
        assertEquals(
                "5f63a7a6a4541d055df5071de22d5edf9ee23c7a",
                NebulaM3SecondPassBinding.UPSTREAM_PR_HEAD);
        assertEquals("8891,8897,8915", NebulaM3SecondPassBinding.INTEGRATED_PRS);

        assertEquals(
                "com.synexia.rewrite.M3AtomPatternSignalChainRecipe",
                NebulaM3SecondPassBinding.RECIPE);
        assertEquals(
                "com.synexia.rewrite.M3AtomPatternSignalChain",
                NebulaM3SecondPassBinding.NAMED_RECIPE);
        assertEquals(
                "com.synexia.rewrite.M3RepositoryAtomizePatternizeRecipe",
                NebulaM3SecondPassBinding.REPOSITORY_RECIPE);

        assertEquals(
                "975cddc7d5365c6483f56f5b9c551387c5af40eb",
                NebulaM3SecondPassBinding.RECIPE_BLOB);
        assertEquals(
                "1f06981c5b12373770df0f2bcecf0fc7b3fbdd5b",
                NebulaM3SecondPassBinding.NAMED_RECIPE_BLOB);
        assertEquals(
                "83cf30e8243f8e4822e45909e77f3479a7b742c3",
                NebulaM3SecondPassBinding.REPOSITORY_RECIPE_BLOB);
        assertEquals(
                "60a401e0ff6540b9d3e9f104ee5771a7a26e4683",
                NebulaM3SecondPassBinding.LEXICAL_MASK_BLOB);
        assertEquals(
                "5e18c97ec6d3762b1f9450d271a92e0012666d8c",
                NebulaM3SecondPassBinding.TORTURE_FIXTURE_BLOB);
        assertEquals(
                "8ee2012f765f077fb4d811ed053196e6ce049daa",
                NebulaM3SecondPassBinding.PROOF_WORKFLOW_BLOB);

        assertEquals(4, NebulaM3SecondPassBinding.PASS_BUDGET);
        assertEquals(3, NebulaM3SecondPassBinding.REQUIRED_FIXED_POINT_PASSES);
        assertEquals(
                "com.synexia.rewrite.M3AtomPatternSignalChain",
                NebulaM3SecondPassBinding.CANONICAL_CATALOG);
        assertEquals(
                "SCANNING_RECIPE_INTERNAL_FANOUT",
                NebulaM3SecondPassBinding.STATE_MODE);
        assertFalse(NebulaM3SecondPassBinding.EXTERNAL_LEAF_FAN_OUT);
        assertEquals(
                "PENDING_EXACT_COMMIT_GREEN_PROOF",
                NebulaM3SecondPassBinding.HOSTED_PROOF);
        assertFalse(NebulaM3SecondPassBinding.sourceMutationAuthority());
        assertFalse(NebulaM3SecondPassBinding.semanticEquivalenceAuthority());
        assertFalse(NebulaM3SecondPassBinding.replacementAuthority());
        assertFalse(NebulaM3SecondPassBinding.promotionAuthority());
    }

    @Test
    void checkedInBindingLedgerMatchesJavaOwnerExactly() throws Exception {
        Path repository = repositoryRoot();
        Path ledger = repository.resolve("m3/catalogue/second-pass-recipe-binding.tsv");

        assertTrue(Files.isRegularFile(ledger));
        assertEquals(NebulaM3SecondPassBinding.tsv(), Files.readString(ledger));
    }

    @Test
    void exactBindingFailsClosedOnAnyDrift() {
        NebulaM3SecondPassBinding.requireExact(
                NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY,
                NebulaM3SecondPassBinding.UPSTREAM_BRANCH,
                NebulaM3SecondPassBinding.UPSTREAM_COMMIT,
                NebulaM3SecondPassBinding.RECIPE,
                NebulaM3SecondPassBinding.PASS_BUDGET);

        NebulaM3SecondPassBinding.requireCanonicalIntegration(
                NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY,
                NebulaM3SecondPassBinding.UPSTREAM_BRANCH,
                NebulaM3SecondPassBinding.UPSTREAM_COMMIT,
                NebulaM3SecondPassBinding.UPSTREAM_PR,
                NebulaM3SecondPassBinding.UPSTREAM_PR_HEAD,
                NebulaM3SecondPassBinding.RECIPE,
                NebulaM3SecondPassBinding.PASS_BUDGET);

        NebulaM3SecondPassBinding.requireCurrentBlobs(
                NebulaM3SecondPassBinding.RECIPE_BLOB,
                NebulaM3SecondPassBinding.NAMED_RECIPE_BLOB,
                NebulaM3SecondPassBinding.REPOSITORY_RECIPE_BLOB,
                NebulaM3SecondPassBinding.LEXICAL_MASK_BLOB,
                NebulaM3SecondPassBinding.TORTURE_FIXTURE_BLOB,
                NebulaM3SecondPassBinding.PROOF_WORKFLOW_BLOB);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3SecondPassBinding.requireExact(
                                NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY,
                                NebulaM3SecondPassBinding.UPSTREAM_BRANCH,
                                "0".repeat(40),
                                NebulaM3SecondPassBinding.RECIPE,
                                NebulaM3SecondPassBinding.PASS_BUDGET));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3SecondPassBinding.requireCanonicalIntegration(
                                NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY,
                                NebulaM3SecondPassBinding.UPSTREAM_BRANCH,
                                NebulaM3SecondPassBinding.UPSTREAM_COMMIT,
                                NebulaM3SecondPassBinding.UPSTREAM_PR + 1,
                                NebulaM3SecondPassBinding.UPSTREAM_PR_HEAD,
                                NebulaM3SecondPassBinding.RECIPE,
                                NebulaM3SecondPassBinding.PASS_BUDGET));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3SecondPassBinding.requireCanonicalIntegration(
                                NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY,
                                NebulaM3SecondPassBinding.UPSTREAM_BRANCH,
                                NebulaM3SecondPassBinding.UPSTREAM_COMMIT,
                                NebulaM3SecondPassBinding.UPSTREAM_PR,
                                "0".repeat(40),
                                NebulaM3SecondPassBinding.RECIPE,
                                NebulaM3SecondPassBinding.PASS_BUDGET));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3SecondPassBinding.requireCurrentBlobs(
                                "0".repeat(40),
                                NebulaM3SecondPassBinding.NAMED_RECIPE_BLOB,
                                NebulaM3SecondPassBinding.REPOSITORY_RECIPE_BLOB,
                                NebulaM3SecondPassBinding.LEXICAL_MASK_BLOB,
                                NebulaM3SecondPassBinding.TORTURE_FIXTURE_BLOB,
                                NebulaM3SecondPassBinding.PROOF_WORKFLOW_BLOB));
    }

    @Test
    void policyConvergencePlanAndRootProfileCarryTheExactReadOnlyGate() throws Exception {
        Path repository = repositoryRoot();
        String policy =
                Files.readString(repository.resolve(".m3/atom-pattern/policy.properties"));
        String plan = Files.readString(repository.resolve("m3/convergence/PLAN.tsv"));
        String pom = Files.readString(repository.resolve("pom.xml"));

        assertPolicy(policy, "java.secondPassRecipe", NebulaM3SecondPassBinding.RECIPE);
        assertPolicy(
                policy,
                "java.secondPassNamedRecipe",
                NebulaM3SecondPassBinding.NAMED_RECIPE);
        assertPolicy(
                policy,
                "java.secondPassRepositoryRecipe",
                NebulaM3SecondPassBinding.REPOSITORY_RECIPE);
        assertPolicy(
                policy,
                "java.secondPassPassBudget",
                Integer.toString(NebulaM3SecondPassBinding.PASS_BUDGET));
        assertPolicy(
                policy,
                "java.secondPassRequiredFixedPointPasses",
                Integer.toString(NebulaM3SecondPassBinding.REQUIRED_FIXED_POINT_PASSES));
        assertPolicy(
                policy,
                "java.secondPassUpstreamCommit",
                NebulaM3SecondPassBinding.UPSTREAM_COMMIT);
        assertPolicy(
                policy,
                "java.secondPassUpstreamBranch",
                NebulaM3SecondPassBinding.UPSTREAM_BRANCH);
        assertPolicy(
                policy,
                "java.secondPassUpstreamPr",
                Integer.toString(NebulaM3SecondPassBinding.UPSTREAM_PR));
        assertPolicy(
                policy,
                "java.secondPassUpstreamPrHead",
                NebulaM3SecondPassBinding.UPSTREAM_PR_HEAD);
        assertPolicy(
                policy,
                "java.secondPassIntegratedPrs",
                NebulaM3SecondPassBinding.INTEGRATED_PRS);
        assertPolicy(
                policy,
                "java.secondPassRecipeBlob",
                NebulaM3SecondPassBinding.RECIPE_BLOB);
        assertPolicy(
                policy,
                "java.secondPassNamedRecipeBlob",
                NebulaM3SecondPassBinding.NAMED_RECIPE_BLOB);
        assertPolicy(
                policy,
                "java.secondPassRepositoryRecipeBlob",
                NebulaM3SecondPassBinding.REPOSITORY_RECIPE_BLOB);
        assertPolicy(
                policy,
                "java.secondPassLexicalMaskBlob",
                NebulaM3SecondPassBinding.LEXICAL_MASK_BLOB);
        assertPolicy(
                policy,
                "java.secondPassTortureFixtureBlob",
                NebulaM3SecondPassBinding.TORTURE_FIXTURE_BLOB);
        assertPolicy(
                policy,
                "java.secondPassProofWorkflowBlob",
                NebulaM3SecondPassBinding.PROOF_WORKFLOW_BLOB);
        assertPolicy(
                policy,
                "java.secondPassHostedProof",
                NebulaM3SecondPassBinding.HOSTED_PROOF);
        assertPolicy(
                policy,
                "java.secondPassCanonicalCatalog",
                NebulaM3SecondPassBinding.CANONICAL_CATALOG);
        assertPolicy(
                policy,
                "java.secondPassStateMode",
                NebulaM3SecondPassBinding.STATE_MODE);
        assertPolicy(policy, "java.secondPassExternalLeafFanOut", "false");
        assertPolicy(policy, "java.secondPassMutation", "false");
        assertPolicy(policy, "java.secondPassSemanticEquivalence", "false");
        assertPolicy(policy, "java.secondPassReplacement", "false");
        assertPolicy(policy, "java.secondPassPromotion", "false");

        assertTrue(
                plan.contains(
                        "3\tSECOND_PASS_SIGNAL_CHAIN\t"
                                + "com.synexia:"
                                + NebulaM3SecondPassBinding.RECIPE
                                + "\tFILE\tREAD_ONLY\t"));
        assertTrue(plan.contains("passBudget=4, fixedPointPasses=3"));
        assertTrue(plan.contains("7\tFILE_FIXED_POINT\t"));

        assertTrue(
                pom.contains(
                        "<recipe>"
                                + NebulaM3SecondPassBinding.REPOSITORY_RECIPE
                                + "</recipe>"));
        assertFalse(pom.contains("com.synexia.rewrite.M3NebulaAtomizePatternizeRecipe"));
    }

    private static void assertPolicy(String policy, String key, String value) {
        assertTrue(policy.contains(key + "=" + value), key);
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
