// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Read-only donor analysis bundle for a Mavenized absorbed donor image.
 *
 * <p>The bundle deliberately composes existing owners instead of creating a parallel atomizer or
 * algorithm classifier. Structural atoms/hashes come from {@link M3AtomStructureInventoryRecipe};
 * method-level algorithm/problem/donor/recipe patterns come from
 * {@link M3AlgorithmAtomRecipeCrateRecipe}. Neither child recipe grants replacement or promotion
 * authority.</p>
 */
public final class M3DonorMavenizedAtomPatternRecipe extends Recipe {
    private final String repository;
    private final String project;
    private final String module;
    private final String libraries;
    private final Integer evidenceLimit;

    public M3DonorMavenizedAtomPatternRecipe() {
        this("", "", "", "", 20);
    }

    @JsonCreator
    public M3DonorMavenizedAtomPatternRecipe(
            @JsonProperty("repository") String repository,
            @JsonProperty("project") String project,
            @JsonProperty("module") String module,
            @JsonProperty("libraries") String libraries,
            @JsonProperty("evidenceLimit") Integer evidenceLimit) {
        this.repository = clean(repository);
        this.project = clean(project);
        this.module = clean(module);
        this.libraries = clean(libraries);
        this.evidenceLimit = Objects.requireNonNullElse(evidenceLimit, 20);
        if (this.evidenceLimit < 1 || this.evidenceLimit > 1000) {
            throw new IllegalArgumentException("evidenceLimit");
        }
    }

    @Override
    public String getDisplayName() {
        return "M3 Mavenized donor atom and pattern inventory";
    }

    @Override
    public String getDescription() {
        return "Runs the existing structural atom inventory and method-level algorithm recipe-crate "
                + "inventory over a Mavenized absorbed donor source tree without changing donor source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "donor",
                "maven",
                "atom",
                "pattern",
                "inventory-first",
                "recipe-first",
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
                new M3AtomStructureInventoryRecipe(
                        repository,
                        project,
                        module,
                        libraries),
                new M3AlgorithmAtomRecipeCrateRecipe(
                        evidenceLimit,
                        ""));
    }

    public String getRepository() {
        return repository;
    }

    public String getProject() {
        return project;
    }

    public String getModule() {
        return module;
    }

    public String getLibraries() {
        return libraries;
    }

    public Integer getEvidenceLimit() {
        return evidenceLimit;
    }

    public boolean sourceMutationAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    private static String clean(String value) {
        String result = Objects.toString(value, "").strip();
        if (result.indexOf('\0') >= 0
                || result.indexOf('\n') >= 0
                || result.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("recipe option");
        }
        return result;
    }
}
