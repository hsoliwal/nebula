// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Canonical M3 recipe-first normalization lattice for a Mavenized project/repository.
 *
 * <p>The recipe is inventory/patternization only. Actual mutation recipes remain separately
 * source-sealed and verification-gated. This bundle makes the required hierarchy explicit for
 * every module without copying configuration into each child POM.</p>
 */
public final class M3HierarchicalAtomPatternRecipe extends Recipe {
    private final String sourceFilePattern;
    private final String problemTerm;

    public M3HierarchicalAtomPatternRecipe() {
        this(
                "**",
                "mindex minex jni filesystem precompute search regex index persistence lifecycle");
    }

    @JsonCreator
    public M3HierarchicalAtomPatternRecipe(
            @JsonProperty("sourceFilePattern") final String sourceFilePattern,
            @JsonProperty("problemTerm") final String problemTerm) {
        this.sourceFilePattern = checked(sourceFilePattern, "sourceFilePattern");
        this.problemTerm = optional(problemTerm);
    }

    @Override
    public String getDisplayName() {
        return "M3 hierarchical Mavenized atom-pattern lattice";
    }

    @Override
    public String getDescription() {
        return "Routes each source by specificity, inventories documentation separately from code, "
                + "atomizes/patternizes the Maven-supplied source set, and emits the mandatory "
                + "FILE -> PACKAGE -> MODULE -> PROJECT -> REPOSITORY promotion plan before absorption.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "maven",
                "openrewrite",
                "recipe-first",
                "atom",
                "pattern",
                "hierarchical",
                "donor",
                "absorption",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3HierarchicalAtomPatternPlanRecipe(),
                new M3DocumentationAtomInventoryRecipe(),
                new M3RepositoryAtomizePatternizeRecipe(sourceFilePattern, problemTerm));
    }

    public String sourceFilePattern() {
        return sourceFilePattern;
    }

    public String problemTerm() {
        return problemTerm;
    }

    public boolean mutationAuthority() {
        return false;
    }

    public boolean sourceCopyAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    private static String checked(final String value, final String field) {
        final String result = Objects.toString(value, "").strip();
        if (result.isEmpty()
                || result.length() > 4096
                || result.indexOf('\0') >= 0
                || result.indexOf('\r') >= 0
                || result.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return result;
    }

    private static String optional(final String value) {
        final String result = Objects.requireNonNullElse(value, "").strip();
        if (result.length() > 4096
                || result.indexOf('\0') >= 0
                || result.indexOf('\r') >= 0
                || result.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("problemTerm");
        }
        return result;
    }
}
