// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

/** Opt-in recipe composition; never attached to an existing source-mutating lifecycle. */
public final class M3JdkExpressionRecipes extends Recipe {
    @Option(displayName = "Exact source path", required = false,
            description = "Literal repository-relative Java path. Empty selects Java source files.")
    private final String sourcePath;
    @Option(displayName = "Maximum changes per leaf per file", required = false,
            description = "Each of six independent leaves defaults to one expression replacement.", example = "1")
    private final int maxChangesPerFile;

    public M3JdkExpressionRecipes() { this(null, null); }
    @JsonCreator public M3JdkExpressionRecipes(@JsonProperty("sourcePath") String sourcePath,
            @JsonProperty("maxChangesPerFile") Integer maxChangesPerFile) {
        var validated = new M3StringLengthZeroRecipe(sourcePath, maxChangesPerFile);
        this.sourcePath = validated.getSourcePath();
        this.maxChangesPerFile = validated.getMaxChangesPerFile();
    }
    public String getSourcePath() { return sourcePath; }
    public int getMaxChangesPerFile() { return maxChangesPerFile; }
    @Override public String getDisplayName() { return "M3 bounded JDK expression candidates"; }
    @Override public String getDescription() {
        return "Composes six exact-path, method-body, type-aware expression recipes as opt-in candidates.";
    }
    @Override public Set<String> getTags() { return Set.of("synexia", "m3", "jdk", "candidate-only"); }
    @Override public int maxCycles() { return 1; }

    @Override public boolean equals(Object other) {
        return other instanceof M3JdkExpressionRecipes that
                && sourcePath.equals(that.sourcePath) && maxChangesPerFile == that.maxChangesPerFile;
    }
    @Override public int hashCode() { return Objects.hash(getName(), sourcePath, maxChangesPerFile); }
    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3StringLengthZeroRecipe(sourcePath, maxChangesPerFile),
                new M3StringEmptyEqualityRecipe(sourcePath, maxChangesPerFile),
                new M3StringIndexOfContainsRecipe(sourcePath, maxChangesPerFile),
                new M3SingleCharacterSearchRecipe(sourcePath, maxChangesPerFile),
                new M3PrimitiveBooleanRecipe(sourcePath, maxChangesPerFile),
                new M3IntegralIdentityRecipe(sourcePath, maxChangesPerFile));
    }
}
