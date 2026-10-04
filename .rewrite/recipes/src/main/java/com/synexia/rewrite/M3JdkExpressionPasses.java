// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.rewrite.jdk.M3BoundedJdkRecipe;
import com.synexia.rewrite.jdk.M3IntegralIdentityRecipe;
import com.synexia.rewrite.jdk.M3IntegralParityRecipe;
import com.synexia.rewrite.jdk.M3PrimitiveBooleanRecipe;
import com.synexia.rewrite.jdk.M3ShiftDistanceRecipe;
import com.synexia.rewrite.jdk.M3SingleCharacterSearchRecipe;
import com.synexia.rewrite.jdk.M3StringEmptyEqualityRecipe;
import com.synexia.rewrite.jdk.M3StringIndexOfContainsRecipe;
import com.synexia.rewrite.jdk.M3StringLengthZeroRecipe;
import com.synexia.rewrite.jdk.M3StringPrefixRecipe;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Explicit candidate plan. Does not extend IOP admission or alter existing mechanical defaults. */
public final class M3JdkExpressionPasses {
    public enum Category {
        STRING_EMPTY, STRING_SEARCH, BOOLEAN, INTEGER_BITWISE,
        STRING_PREFIX, INTEGER_PARITY, SHIFT_DISTANCE
    }
    private M3JdkExpressionPasses() {}

    public static List<M3TranspilePass> forFile(String sourcePath) {
        return forFile(sourcePath, EnumSet.of(Category.STRING_EMPTY, Category.STRING_SEARCH,
                Category.BOOLEAN, Category.INTEGER_BITWISE));
    }

    /** Category selection narrows recipe candidates, not the behavioral contract or proof gates. */
    public static List<M3TranspilePass> forFile(String sourcePath, Set<Category> categories) {
        String path = M3BoundedJdkRecipe.normalizePath(Objects.requireNonNull(sourcePath, "sourcePath"));
        if (path.isEmpty()) throw new IllegalArgumentException("exact sourcePath required");
        Set<Category> selected = Set.copyOf(Objects.requireNonNull(categories, "categories"));
        List<M3TranspilePass> passes = new ArrayList<>();
        if (selected.contains(Category.STRING_EMPTY)) {
            passes.add(new M3TranspilePass("jdk-01-string-length-zero", new M3StringLengthZeroRecipe(path, 1)));
            passes.add(new M3TranspilePass("jdk-02-string-empty-equality", new M3StringEmptyEqualityRecipe(path, 1)));
        }
        if (selected.contains(Category.STRING_SEARCH)) {
            passes.add(new M3TranspilePass("jdk-03-string-search-predicate", new M3StringIndexOfContainsRecipe(path, 1)));
            passes.add(new M3TranspilePass("jdk-04-single-character-search", new M3SingleCharacterSearchRecipe(path, 1)));
        }
        if (selected.contains(Category.BOOLEAN)) {
            passes.add(new M3TranspilePass("jdk-05-primitive-boolean", new M3PrimitiveBooleanRecipe(path, 1)));
        }
        if (selected.contains(Category.INTEGER_BITWISE)) {
            passes.add(new M3TranspilePass("jdk-06-integral-identity", new M3IntegralIdentityRecipe(path, 1)));
        }
        if (selected.contains(Category.STRING_PREFIX)) {
            passes.add(new M3TranspilePass("jdk-07-string-prefix", new M3StringPrefixRecipe(path, 1)));
        }
        if (selected.contains(Category.INTEGER_PARITY)) {
            passes.add(new M3TranspilePass("jdk-08-integral-parity", new M3IntegralParityRecipe(path, 1)));
        }
        if (selected.contains(Category.SHIFT_DISTANCE)) {
            passes.add(new M3TranspilePass("jdk-09-shift-distance", new M3ShiftDistanceRecipe(path, 1)));
        }
        return List.copyOf(passes);
    }

    /**
     * Explicit category preview under the existing IOP pattern-class admission hook.
     * This plan is candidate-only and does not grant promotion authority or change default profiles.
     */
    public static List<M3TranspilePass> forIopFile(String sourcePath, Set<Category> categories) {
        List<M3TranspilePass> leaves = forFile(sourcePath, categories);
        return leaves.stream().map(pass -> new M3TranspilePass(pass.id(),
                // The leaf owns exact literal matching, including brackets. Do not turn the path into a glob.
                new M3IopPatternScopedRecipe("**", pass.recipe()))).toList();
    }
}
