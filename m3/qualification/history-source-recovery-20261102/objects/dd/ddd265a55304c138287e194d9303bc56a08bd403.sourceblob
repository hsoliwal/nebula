// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.Objects;

/** Exact authority-free binding to the canonical Synexia atom/pattern signal chain. */
public final class NebulaM3SecondPassBinding {
    public static final String UPSTREAM_REPOSITORY = "hsoliwal/com.synexia";
    public static final String UPSTREAM_BRANCH = "develop";
    public static final String UPSTREAM_COMMIT =
            "1cd108647f1eb1d5c288d917acbf7e8e635c8ce9";
    public static final int UPSTREAM_PR = 8925;
    public static final String UPSTREAM_PR_HEAD =
            "5f63a7a6a4541d055df5071de22d5edf9ee23c7a";
    public static final String INTEGRATED_PRS = "8891,8897,8915";

    public static final String RECIPE =
            "com.synexia.rewrite.M3AtomPatternSignalChainRecipe";
    public static final String NAMED_RECIPE =
            "com.synexia.rewrite.M3AtomPatternSignalChain";
    public static final String REPOSITORY_RECIPE =
            "com.synexia.rewrite.M3RepositoryAtomizePatternizeRecipe";

    public static final String RECIPE_BLOB =
            "975cddc7d5365c6483f56f5b9c551387c5af40eb";
    public static final String NAMED_RECIPE_BLOB =
            "1f06981c5b12373770df0f2bcecf0fc7b3fbdd5b";
    public static final String REPOSITORY_RECIPE_BLOB =
            "83cf30e8243f8e4822e45909e77f3479a7b742c3";
    public static final String LEXICAL_MASK_BLOB =
            "60a401e0ff6540b9d3e9f104ee5771a7a26e4683";
    public static final String TORTURE_FIXTURE_BLOB =
            "5e18c97ec6d3762b1f9450d271a92e0012666d8c";
    public static final String PROOF_WORKFLOW_BLOB =
            "8ee2012f765f077fb4d811ed053196e6ce049daa";

    public static final String CANONICAL_CATALOG = NAMED_RECIPE;
    public static final String STATE_MODE = "SCANNING_RECIPE_INTERNAL_FANOUT";
    public static final boolean EXTERNAL_LEAF_FAN_OUT = false;
    public static final int PASS_BUDGET = 4;
    public static final int REQUIRED_FIXED_POINT_PASSES = 3;
    public static final String HOSTED_PROOF = "PENDING_EXACT_COMMIT_GREEN_PROOF";

    private NebulaM3SecondPassBinding() {}

    public static boolean sourceMutationAuthority() {
        return false;
    }

    public static boolean semanticEquivalenceAuthority() {
        return false;
    }

    public static boolean replacementAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static String tsv() {
        return "upstream_repository\tupstream_branch\tupstream_commit\tintegration_pr"
                + "\tintegration_pr_head\tintegrated_prs"
                + "\trecipe_entrypoint\tnamed_recipe\trepository_entrypoint"
                + "\trecipe_blob\tnamed_recipe_blob\trepository_recipe_blob"
                + "\tlexical_mask_blob\ttorture_fixture_blob\tproof_workflow_blob"
                + "\tstate_mode\texternal_leaf_fanout\tpass_budget"
                + "\trequired_fixed_point_passes\tsource_mutation\tsemantic_equivalence"
                + "\treplacement\tpromotion\thosted_proof\n"
                + UPSTREAM_REPOSITORY
                + "\t"
                + UPSTREAM_BRANCH
                + "\t"
                + UPSTREAM_COMMIT
                + "\t"
                + UPSTREAM_PR
                + "\t"
                + UPSTREAM_PR_HEAD
                + "\t"
                + INTEGRATED_PRS
                + "\t"
                + RECIPE
                + "\t"
                + NAMED_RECIPE
                + "\t"
                + REPOSITORY_RECIPE
                + "\t"
                + RECIPE_BLOB
                + "\t"
                + NAMED_RECIPE_BLOB
                + "\t"
                + REPOSITORY_RECIPE_BLOB
                + "\t"
                + LEXICAL_MASK_BLOB
                + "\t"
                + TORTURE_FIXTURE_BLOB
                + "\t"
                + PROOF_WORKFLOW_BLOB
                + "\t"
                + STATE_MODE
                + "\t"
                + EXTERNAL_LEAF_FAN_OUT
                + "\t"
                + PASS_BUDGET
                + "\t"
                + REQUIRED_FIXED_POINT_PASSES
                + "\tfalse\tfalse\tfalse\tfalse\t"
                + HOSTED_PROOF
                + "\n";
    }

    public static void requireExact(
            String repository, String branch, String commit, String recipe, int passBudget) {
        requireCanonicalIntegration(
                repository,
                branch,
                commit,
                UPSTREAM_PR,
                UPSTREAM_PR_HEAD,
                recipe,
                passBudget);
    }

    public static void requireCanonicalIntegration(
            String repository,
            String branch,
            String commit,
            int pullRequest,
            String pullRequestHead,
            String recipe,
            int passBudget) {
        if (!UPSTREAM_REPOSITORY.equals(Objects.requireNonNull(repository, "repository"))
                || !UPSTREAM_BRANCH.equals(Objects.requireNonNull(branch, "branch"))
                || !UPSTREAM_COMMIT.equals(Objects.requireNonNull(commit, "commit"))
                || UPSTREAM_PR != pullRequest
                || !UPSTREAM_PR_HEAD.equals(
                        Objects.requireNonNull(pullRequestHead, "pullRequestHead"))
                || !RECIPE.equals(Objects.requireNonNull(recipe, "recipe"))
                || PASS_BUDGET != passBudget) {
            throw new IllegalArgumentException("Nebula M3 second-pass binding drift");
        }
    }

    public static void requireCurrentBlobs(
            String recipeBlob,
            String namedRecipeBlob,
            String repositoryRecipeBlob,
            String lexicalMaskBlob,
            String tortureFixtureBlob,
            String proofWorkflowBlob) {
        if (!RECIPE_BLOB.equals(Objects.requireNonNull(recipeBlob, "recipeBlob"))
                || !NAMED_RECIPE_BLOB.equals(
                        Objects.requireNonNull(namedRecipeBlob, "namedRecipeBlob"))
                || !REPOSITORY_RECIPE_BLOB.equals(
                        Objects.requireNonNull(repositoryRecipeBlob, "repositoryRecipeBlob"))
                || !LEXICAL_MASK_BLOB.equals(
                        Objects.requireNonNull(lexicalMaskBlob, "lexicalMaskBlob"))
                || !TORTURE_FIXTURE_BLOB.equals(
                        Objects.requireNonNull(tortureFixtureBlob, "tortureFixtureBlob"))
                || !PROOF_WORKFLOW_BLOB.equals(
                        Objects.requireNonNull(proofWorkflowBlob, "proofWorkflowBlob"))) {
            throw new IllegalArgumentException("Nebula M3 second-pass blob custody drift");
        }
    }
}
