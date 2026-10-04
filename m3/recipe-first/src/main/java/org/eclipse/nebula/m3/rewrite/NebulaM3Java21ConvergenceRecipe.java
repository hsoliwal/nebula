// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite;

import org.eclipse.nebula.m3.rewrite.convergence.NebulaNebulaM3FileConvergenceRecipeDag;
import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** Single trusted OpenRewrite entry point for Nebula FILE-local Java 21 convergence. */
public final class NebulaM3Java21ConvergenceRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "M3 Nebula Java 21 multi-pass FILE convergence";
    }

    @Override
    public String getDescription() {
        return "Inventories, atomizes, patternizes/IOP-types and documents admitted Nebula "
                + "Java 21 FILE-local leaves through one deterministic OpenRewrite DAG.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "java21",
                "multi-pass",
                "convergence",
                "inventory",
                "atomization",
                "patternization",
                "iop",
                "documentation",
                "file-local",
                "behavior-contract-preserving");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return NebulaM3FileConvergenceRecipeDag.atoms().stream()
                .map(NebulaM3FileConvergenceRecipeDag.Atom::recipe)
                .toList();
    }
}
