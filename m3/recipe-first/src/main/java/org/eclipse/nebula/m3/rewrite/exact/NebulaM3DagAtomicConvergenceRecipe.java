// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Two ordered, independently tested OpenRewrite atoms: Maven admission, then Java publication.
 *
 * <p>This DAG never executes a filesystem writer or transfers a backport. It only produces
 * reviewed source candidates, with fixed-point behavior proved by JUnit.</p>
 */
public final class NebulaM3DagAtomicConvergenceRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Converge Nebula M3 DAG publication Maven and Java atoms";
    }

    @Override
    public String getDescription() {
        return "Replay the exact Maven XML profile, then the exact Java 21 DAG evidence publisher.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "nebula", "maven", "openrewrite",
                "atomization", "documentation", "recipe-dag", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new NebulaM3DagAtomicMavenProfileRecipe(),
                new NebulaM3DagEvidenceAtomicRecipe());
    }
}
