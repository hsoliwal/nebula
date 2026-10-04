// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Read-only live-reactor catalogue. Maven supplies the source/classpath model; M3 inventories
 * semantic atoms and pinned pattern/donor-shape evidence before any absorption recipe is allowed.
 */
public final class M3RepositoryAtomizePatternizeRecipe extends Recipe {
    private final String sourceFilePattern;
    private final String problemTerm;

    public M3RepositoryAtomizePatternizeRecipe() {
        this("**/*.java",
                "mindex minex jni filesystem precompute search regex index persistence lifecycle");
    }

    @JsonCreator
    public M3RepositoryAtomizePatternizeRecipe(String sourceFilePattern, String problemTerm) {
        this.sourceFilePattern = checked(sourceFilePattern, "sourceFilePattern");
        this.problemTerm = checked(problemTerm, "problemTerm");
    }

    @Override public String getDisplayName() {
        return "M3 live repository atomize and patternize catalogue";
    }

    @Override public String getDescription() {
        return "Runs read-only semantic atom inventory, framework/pattern review and pinned mechanical "
                + "donor-shape evidence across the Maven-supplied live source set before bounded absorption.";
    }

    @Override public Set<String> getTags() {
        return Set.of("synexia", "m3", "recipe-first", "maven", "atom", "pattern",
                "repository", "read-only", "candidate-only");
    }

    @Override public int maxCycles() { return 1; }

    @Override public List<Recipe> getRecipeList() {
        return List.of(
                new M3RecipeFirstJavaAtomInventoryRecipe(),
                new M3FrameworkRecipePatternReviewRecipe(sourceFilePattern, problemTerm),
                new M3MechanicalDonorShapeReviewRecipe("ALL"));
    }

    public String sourceFilePattern() { return sourceFilePattern; }
    public String problemTerm() { return problemTerm; }
    public boolean mutationAuthority() { return false; }
    public boolean sourceCopyAuthority() { return false; }
    public boolean promotionAuthority() { return false; }

    private static String checked(String value, String field) {
        String result = Objects.toString(value, "").strip();
        if (result.isEmpty() || result.length() > 4096 || result.indexOf('\0') >= 0
                || result.indexOf('\r') >= 0 || result.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return result;
    }
}
