// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import java.util.Set;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

/**
 * Class-backed M3 serial mechanical pipeline whose mutation authority is restricted to explicit IOP pattern code.
 */
public final class M3IopOnlySerialMechanicalJavaRecipe extends Recipe {
    @Option(
            displayName = "Source file pattern",
            description = "Additional file glob; IOP source admission remains mandatory.",
            required = false,
            example = "synexia-iop/src/main/java/com/synexia/iop/**/*.java")
    private final String sourceFilePattern;

    public M3IopOnlySerialMechanicalJavaRecipe() {
        this(M3MechanicalJavaRecipe.DEFAULT_SOURCE_FILE_PATTERN);
    }

    @JsonCreator
    public M3IopOnlySerialMechanicalJavaRecipe(String sourceFilePattern) {
        if (sourceFilePattern == null || sourceFilePattern.isBlank() || sourceFilePattern.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        this.sourceFilePattern = sourceFilePattern.strip();
    }

    @Override
    public String getDisplayName() {
        return "M3 IOP-only serial mechanical Java pipeline";
    }

    @Override
    public String getDescription() {
        return "Runs the canonical M3 OpenRewrite pass sequence only on admitted Synexia IOP pattern Java sources. "
                + "Recipe output remains a candidate until M3 diff/lint/compile/test/runtime promotion.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia", "m3", "iop", "pattern", "pattern-only", "mechanical", "serial",
                "class-hooked", "class-backed", "iop-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return M3IopFullScaleMutationAuthority.canonicalPasses(sourceFilePattern);
    }

    public String getSourceFilePattern() {
        return sourceFilePattern;
    }
}
