// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.Objects;

/** Exact authority-free binding to the reusable Synexia second-pass signal recipe. */
public final class NebulaM3SecondPassBinding {
    public static final String UPSTREAM_REPOSITORY = "hsoliwal/com.synexia";
    public static final String UPSTREAM_BRANCH = "develop";
    public static final String UPSTREAM_COMMIT =
            "daaab09a1b91fe8344c1ea97359342b52739bf38";
    public static final int UPSTREAM_PR = 8925;
    public static final String UPSTREAM_PR_HEAD =
            "5f63a7a6a4541d055df5071de22d5edf9ee23c7a";
    public static final String INTEGRATED_PRS = "8891,8897,8915";
    public static final String RECIPE =
            "com.synexia.rewrite.M3SecondPassAtomPatternRecipe";
    public static final String CANONICAL_CATALOG =
            "com.synexia.m3.recipe.M3SecondPassRecipeDagCatalog";
    public static final String STATE_MODE = "SHARED_JVM_COMPOSITE";
    public static final boolean EXTERNAL_LEAF_FAN_OUT = false;
    public static final int PASS_BUDGET = 2;
    public static final String HOSTED_PROOF = "MERGED_PR_HEAD_RUNS_QUEUED";

    private NebulaM3SecondPassBinding() {}

    public static boolean sourceMutationAuthority() {
        return false;
    }

    public static boolean replacementAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static String tsv() {
        return "upstream_repository\tupstream_branch\tupstream_commit\tupstream_pr"
                + "\tupstream_pr_head\tintegrated_prs"
                + "\trecipe_entrypoint\tcanonical_catalog\tstate_mode"
                + "\texternal_leaf_fanout\tpass_budget\tsource_mutation\treplacement"
                + "\tpromotion\thosted_proof\n"
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
                + CANONICAL_CATALOG
                + "\t"
                + STATE_MODE
                + "\t"
                + EXTERNAL_LEAF_FAN_OUT
                + "\t"
                + PASS_BUDGET
                + "\tfalse\tfalse\tfalse\t"
                + HOSTED_PROOF
                + "\n";
    }

    public static void requireExact(
            String repository, String branch, String commit, String recipe, int passBudget) {
        if (!UPSTREAM_REPOSITORY.equals(Objects.requireNonNull(repository, "repository"))
                || !UPSTREAM_BRANCH.equals(Objects.requireNonNull(branch, "branch"))
                || !UPSTREAM_COMMIT.equals(Objects.requireNonNull(commit, "commit"))
                || !RECIPE.equals(Objects.requireNonNull(recipe, "recipe"))
                || PASS_BUDGET != passBudget) {
            throw new IllegalArgumentException("Nebula M3 second-pass binding drift");
        }
    }
}
