// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * FILE-local, exact-Java-LST hardening of the Drools recipe-DAG scheduler projection.
 *
 * <p>Requires independently verified predecessor completion and a SHA-256-shaped proof receipt;
 * readiness alone cannot unlock the next phase. Candidate-only, no widget or JNI changes.</p>
 */
public final class NebulaM3DroolsCompletionGateRecipe extends NebulaM3ExactJavaSnapshotRecipe {
    public static final String PATH =
            "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3OrchestratorPlans.java";
    public static final String MODULE_PATH =
            "src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3OrchestratorPlans.java";
    public static final String BEFORE_SHA256 = "6d5a9a84a6253cc9028361098c51f5917fe0d8285149bc56bcfb45b08b2262c1";
    public static final String AFTER_SHA256 = "4970d9a95b3ddbe90604665295c32ff8fa1788871a202a7477ec05870d791e93";
    public static final String POSTIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/drools-completion-gate/"
                    + "NebulaM3OrchestratorPlans.after.java.txt";

    @Override public String getDisplayName() {
        return "Require proven completion before Drools recipe dependency readiness";
    }

    @Override public String getDescription() {
        return "Replay the exact reviewed Java-21 scheduler projection change while rejecting "
                + "unreviewed source drift and reaching an unchanged second pass.";
    }

    @Override public Set<String> getTags() {
        return Set.of("m3", "nebula", "drools", "recipe-dag", "jdk21",
                "file-local", "behavior-contract-preserving", "recipe-first", "candidate-only");
    }

    @Override protected String repositoryPath() { return PATH; }
    @Override protected String moduleRelativePath() { return MODULE_PATH; }
    @Override protected String beforeSha256() { return BEFORE_SHA256; }
    @Override protected String afterSha256() { return AFTER_SHA256; }
    @Override protected String afterResource() { return POSTIMAGE; }
}
