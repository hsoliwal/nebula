// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

/** Adds the M3 exact-source-file fence to one external OpenRewrite leaf recipe. */
final class M3ScopedRecipe extends Recipe {
    private final String sourceFilePattern;
    private final Recipe delegate;

    M3ScopedRecipe(String sourceFilePattern, Recipe delegate) {
        this.sourceFilePattern = M3OpenRewriteTranspiler.normalizeSourcePath(sourceFilePattern);
        this.delegate = java.util.Objects.requireNonNull(delegate, "delegate");
        if (!delegate.getRecipeList().isEmpty()) {
            throw new IllegalArgumentException("M3ScopedRecipe requires a leaf recipe");
        }
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public String getDescription() {
        return "M3 exact-file fence around " + delegate.getName();
    }

    @Override
    public Set<String> getTags() {
        return Set.of("synexia", "m3", "scoped", "candidate");
    }

    @Override
    public int maxCycles() {
        return Math.min(1, Math.max(1, delegate.maxCycles()));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindSourceFiles(sourceFilePattern).getVisitor(), delegate.getVisitor());
    }
}
