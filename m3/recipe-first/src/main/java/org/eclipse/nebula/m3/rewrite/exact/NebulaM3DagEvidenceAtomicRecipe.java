// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * Exact Java-LST repair for the Nebula recipe-DAG evidence publisher.
 *
 * <p>Reuses the canonical one-file OpenRewrite source seal. No scheduler, widget, public SWT
 * surface or backport source is changed, and this recipe grants no promotion authority.</p>
 */
public final class NebulaM3DagEvidenceAtomicRecipe extends NebulaM3ExactJavaSnapshotRecipe {
    public static final String PATH =
            "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3RecipeDagManifestCli.java";
    public static final String MODULE_PATH =
            "src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3RecipeDagManifestCli.java";
    public static final String BEFORE_SHA256 =
            "99e2ef5813f059ca48206e6021d9610af2db2d61958ed61f131fbd3aa0e9f666";
    public static final String AFTER_SHA256 =
            "7bad32921368032cf42ee7297653e425afe96965634aafe9212abff67fce1c2c";
    public static final String POSTIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/dag-atomic-evidence/"
                    + "NebulaM3RecipeDagManifestCli.after.java.txt";

    @Override
    public String getDisplayName() {
        return "Atomically publish Nebula M3 recipe-DAG evidence files";
    }

    @Override
    public String getDescription() {
        return "Replays the SHA-256 pinned Java 21 change that atomically replaces complete "
                + "scheduler/evidence files and refuses symlinked or special output targets.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "nebula", "java21", "openrewrite", "file-local",
                "recipe-first", "documentation", "candidate-only");
    }

    @Override
    protected String repositoryPath() {
        return PATH;
    }

    @Override
    protected String moduleRelativePath() {
        return MODULE_PATH;
    }

    @Override
    protected String beforeSha256() {
        return BEFORE_SHA256;
    }

    @Override
    protected String afterSha256() {
        return AFTER_SHA256;
    }

    @Override
    protected String afterResource() {
        return POSTIMAGE;
    }
}
