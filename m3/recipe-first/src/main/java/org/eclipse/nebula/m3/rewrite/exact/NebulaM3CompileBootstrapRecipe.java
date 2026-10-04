// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Repairs the two actual compilation blockers before running the existing full recipe crate.
 * This composes the existing exact-Java engine; it is not another parser or rewrite engine.
 * Each leaf remains source-pinned and one-cycle. Repository-wide atomization is a later gate.
 */
public final class NebulaM3CompileBootstrapRecipe extends Recipe {
    static final String RESOURCE = "/org/eclipse/nebula/m3/rewrite/exact/compile-bootstrap/";

    static List<NebulaM3ExactJavaSnapshotRecipe> snapshots() {
        return List.of(new ReviewTable(), new ReviewComposition());
    }

    @Override public String getDisplayName() { return "Restore Nebula recipe-crate compilation"; }
    @Override public String getDescription() {
        return "Restores the existing ordered review composition and declared pass-order accessor "
                + "from exact preimages before compiling and executing the full atom-pattern crate.";
    }
    @Override public Set<String> getTags() { return Set.of("m3", "recipe-first", "exact-source", "bootstrap"); }
    @Override public int maxCycles() { return 1; }
    @Override public List<Recipe> getRecipeList() {
        return snapshots().stream().map(Recipe.class::cast).toList();
    }

    public static final class ReviewTable extends NebulaM3ExactJavaSnapshotRecipe {
        @Override protected String repositoryPath() { return "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/NebulaM3FastSearchReviewTable.java"; }
        @Override protected String moduleRelativePath() { return "src/main/java/org/eclipse/nebula/m3/NebulaM3FastSearchReviewTable.java"; }
        @Override protected String beforeSha256() { return "8873857934e27684ed66c8de97a7232a4011a69c9e8363d1d911c233999f8e71"; }
        @Override protected String afterSha256() { return "aaaeb0e63a6a15382e3303c960d24e6da8a56b751800fb9ca82d75543837ed57"; }
        @Override protected String afterResource() { return RESOURCE + "NebulaM3FastSearchReviewTable.java.after.txt"; }
        @Override public String getDisplayName() { return "Restore compiled Nebula ReviewTable"; }
        @Override public String getDescription() {
            return "Replays one exact reviewed compile repair, preserving existing public signatures.";
        }
    }

    public static final class ReviewComposition extends NebulaM3ExactJavaSnapshotRecipe {
        @Override protected String repositoryPath() { return "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/NebulaM3RepositoryReviewRecipe.java"; }
        @Override protected String moduleRelativePath() { return "src/main/java/org/eclipse/nebula/m3/NebulaM3RepositoryReviewRecipe.java"; }
        @Override protected String beforeSha256() { return "1c74e3420b318a7836da9de9f29529ce3f2daf34ea82d5bb018bdc8f6db97037"; }
        @Override protected String afterSha256() { return "f8e27fcf0e15766ed53fcfcf9747a63d4c21e351b47c8595a2e25eff493ac077"; }
        @Override protected String afterResource() { return RESOURCE + "NebulaM3RepositoryReviewRecipe.java.after.txt"; }
        @Override public String getDisplayName() { return "Restore compiled Nebula ReviewComposition"; }
        @Override public String getDescription() {
            return "Replays one exact reviewed compile repair, preserving existing public signatures.";
        }
    }
}
