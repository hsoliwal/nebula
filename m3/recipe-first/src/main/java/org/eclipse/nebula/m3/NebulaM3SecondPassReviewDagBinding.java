// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.List;
import java.util.Objects;

/**
 * Exact authority-free Nebula binding to the saved Synexia second-pass review DAG candidate.
 *
 * <p>The upstream DAG is content-addressed and candidate-only. Nebula executes the same four
 * review owners through a local declarative wrapper, while the pinned Synexia plan remains the
 * topology/projection authority. Hosted proof must close before this binding can be promoted.</p>
 */
public final class NebulaM3SecondPassReviewDagBinding {
    public static final String UPSTREAM_REPOSITORY = "hsoliwal/com.synexia";
    public static final String UPSTREAM_BRANCH = "feat/m3-second-pass-review-dag-20261005";
    public static final String UPSTREAM_COMMIT =
            "5369fdc8c076b998b0dd39c7c67c85d11a4b2d8f";
    public static final int UPSTREAM_PR = 8973;

    public static final String PLAN_CLASS =
            "com.synexia.m3.recipe.M3SecondPassReviewDagPlan";
    public static final String DAG_ID = "m3_second_pass_review";
    public static final String LOCAL_RECIPE =
            "org.eclipse.nebula.m3.SecondPassReviewDag";

    public static final String PLAN_BLOB =
            "a98b490872fc8adbeb3e509fda31e478a5dc1f37";
    public static final String PLAN_TEST_BLOB =
            "7a29baa55eb240c5ae9d1999818f6c83fb813c38";
    public static final String TASK_CRATE_BLOB =
            "985711efd34426bbdc898f419e1fcdb7fdc42921";
    public static final String WORKFLOW_BLOB =
            "3f0820fff9119c204f20ebc8a57af60484e98f51";

    public static final String HOSTED_PROOF = "PENDING_HOSTED_PROOF";
    public static final String SCOPE = "FILE";
    public static final String GOAL = "DRY_RUN";

    public static final List<String> ATOM_IDS =
            List.of(
                    "00-structural-signals",
                    "10-atom-pattern-signal-chain",
                    "20-challenge-donor-planner",
                    "30-jni-contract-inventory");

    public static final List<String> RECIPE_OWNERS =
            List.of(
                    "com.synexia.rewrite.M3CodeSignalTriggerRecipe",
                    "com.synexia.rewrite.M3AtomPatternSignalChain",
                    "com.synexia.M3ProblemRecipePlanner",
                    "com.synexia.rewrite.M3JniContractInventoryRecipe");

    public static final List<String> MAVEN_PROFILES =
            List.of(
                    "m3-code-signal-review",
                    "m3-atom-pattern-signal-chain",
                    "m3-problem-recipe-planner",
                    "m3-jni-contract-inventory");

    public static final List<String> REQUIRED_GATES =
            List.of("DIFF", "LINT", "COMPILE", "TEST", "FIXED_POINT");

    public static final List<String> CHALLENGE_ORDER =
            List.of("LeetCode", "HackerRank", "GeeksforGeeks");

    private NebulaM3SecondPassReviewDagBinding() {}

    public static boolean camelProjection() {
        return true;
    }

    public static boolean airflowProjection() {
        return true;
    }

    public static boolean droolsAdmission() {
        return true;
    }

    public static boolean internalSignalLeafFanOut() {
        return false;
    }

    public static boolean sourceMutationAuthority() {
        return false;
    }

    public static boolean semanticEquivalenceAuthority() {
        return false;
    }

    public static boolean replacementAuthority() {
        return false;
    }

    public static boolean nativeExecutionAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static String tsv() {
        return "upstream_repository\tupstream_branch\tupstream_commit\tupstream_pr"
                + "\tplan_class\tdag_id\tlocal_recipe\tplan_blob\tplan_test_blob"
                + "\ttask_crate_blob\tworkflow_blob\tatom_ids\trecipe_owners\tmaven_profiles"
                + "\tscope\tgoal\tchallenge_order\tcamel\tairflow\tdrools"
                + "\tinternal_signal_leaf_fanout\tsource_mutation\tsemantic_equivalence"
                + "\treplacement\tnative_execution\tpromotion\thosted_proof\n"
                + UPSTREAM_REPOSITORY
                + "\t"
                + UPSTREAM_BRANCH
                + "\t"
                + UPSTREAM_COMMIT
                + "\t"
                + UPSTREAM_PR
                + "\t"
                + PLAN_CLASS
                + "\t"
                + DAG_ID
                + "\t"
                + LOCAL_RECIPE
                + "\t"
                + PLAN_BLOB
                + "\t"
                + PLAN_TEST_BLOB
                + "\t"
                + TASK_CRATE_BLOB
                + "\t"
                + WORKFLOW_BLOB
                + "\t"
                + String.join(",", ATOM_IDS)
                + "\t"
                + String.join(",", RECIPE_OWNERS)
                + "\t"
                + String.join(",", MAVEN_PROFILES)
                + "\t"
                + SCOPE
                + "\t"
                + GOAL
                + "\t"
                + String.join(",", CHALLENGE_ORDER)
                + "\ttrue\ttrue\ttrue\tfalse\tfalse\tfalse\tfalse\tfalse\tfalse\t"
                + HOSTED_PROOF
                + "\n";
    }

    public static void requireExact(
            String repository,
            String branch,
            String commit,
            int pr,
            String planClass,
            String dagId,
            String localRecipe) {
        if (!UPSTREAM_REPOSITORY.equals(Objects.requireNonNull(repository, "repository"))
                || !UPSTREAM_BRANCH.equals(Objects.requireNonNull(branch, "branch"))
                || !UPSTREAM_COMMIT.equals(Objects.requireNonNull(commit, "commit"))
                || UPSTREAM_PR != pr
                || !PLAN_CLASS.equals(Objects.requireNonNull(planClass, "planClass"))
                || !DAG_ID.equals(Objects.requireNonNull(dagId, "dagId"))
                || !LOCAL_RECIPE.equals(Objects.requireNonNull(localRecipe, "localRecipe"))) {
            throw new IllegalArgumentException("Nebula M3 second-pass review DAG binding drift");
        }
    }
}
