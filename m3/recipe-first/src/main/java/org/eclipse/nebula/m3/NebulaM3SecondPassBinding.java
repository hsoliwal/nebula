// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.Objects;

/** Exact authority-free binding to the reusable Synexia second-pass signal recipe. */
public final class NebulaM3SecondPassBinding {
    public static final String UPSTREAM_REPOSITORY = "hsoliwal/com.synexia";
    public static final String UPSTREAM_BRANCH = "feat/m3-second-pass-signal-chain-20261004";
    public static final String UPSTREAM_COMMIT =
            "630d3529f42f3ff6bde92da02b505153fdb35efb";
    public static final int UPSTREAM_PR = 8891;
    public static final String RECIPE =
            "com.synexia.rewrite.M3SecondPassAtomPatternRecipe";
    public static final int PASS_BUDGET = 2;
    public static final String HOSTED_PROOF = "WORKFLOW_STARTUP_FAILURE_NO_JOBS";

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
                + "\trecipe_entrypoint\tpass_budget\tsource_mutation\treplacement"
                + "\tpromotion\thosted_proof\n"
                + UPSTREAM_REPOSITORY
                + "\t"
                + UPSTREAM_BRANCH
                + "\t"
                + UPSTREAM_COMMIT
                + "\t"
                + UPSTREAM_PR
                + "\t"
                + RECIPE
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
