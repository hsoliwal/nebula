// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * Exact FILE-scoped OpenRewrite hardening of the final Nebula proof-receipt CLI.
 *
 * <p>Only a receipt bound to the current canonical M3 recipe DAG may enter the final
 * manual-transfer review lane. Preserves the five-argument CLI contract, Java 21 and
 * all widget contracts. A stale source preimage is rejected; a second pass is unchanged.</p>
 */
public final class NebulaM3ProofReceiptDagIdentityRecipe extends NebulaM3ExactJavaSnapshotRecipe {
    public static final String PATH =
            "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3ProofReceiptCli.java";
    public static final String MODULE_PATH =
            "src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3ProofReceiptCli.java";
    public static final String BEFORE_SHA256 =
            "fdc2a52161f0a532dfdfcc154055f4c0f2b0489bc76fd0095de472f1d377af3a";
    public static final String AFTER_SHA256 =
            "3cb951f7f586aefc34f28b66c3c7577520b98988fd2e1adb1258f0aa5b52ad3f";
    public static final String POSTIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/proof-receipt-dag/"
                    + "NebulaM3ProofReceiptCli.after.java.txt";

    @Override public String getDisplayName() {
        return "Require canonical Nebula recipe DAG root in final proof receipt";
    }

    @Override public String getDescription() {
        return "Replays the exact Java 21 CLI identity gate with preimage drift rejection, "
                + "structured Java parsing, and a second-pass fixed point.";
    }

    @Override public Set<String> getTags() {
        return Set.of("m3", "nebula", "proof", "recipe-dag", "jdk21",
                "file-local", "recipe-first", "candidate-only", "contract-preserving");
    }

    @Override protected String repositoryPath() { return PATH; }
    @Override protected String moduleRelativePath() { return MODULE_PATH; }
    @Override protected String beforeSha256() { return BEFORE_SHA256; }
    @Override protected String afterSha256() { return AFTER_SHA256; }
    @Override protected String afterResource() { return POSTIMAGE; }
}
