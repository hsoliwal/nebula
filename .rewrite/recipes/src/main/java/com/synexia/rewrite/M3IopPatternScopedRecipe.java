// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

/**
 * Internal hard fence around one external OpenRewrite leaf for the canonical M3 IOP pattern lane.
 *
 * <p>The delegate may run only when the source matches the configured file pattern and every
 * top-level class is admitted by {@link M3IopPatternClassHooks}. This wrapper never grants
 * mutation authority by itself; only {@link M3IopPatternOnlySerialMechanicalJavaRecipe} is the
 * named pipeline entry point.</p>
 */
final class M3IopPatternScopedRecipe extends Recipe {
    private final String sourceFilePattern;
    private final Recipe delegate;

    M3IopPatternScopedRecipe(String sourceFilePattern, Recipe delegate) {
        if (sourceFilePattern == null
                || sourceFilePattern.isBlank()
                || sourceFilePattern.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        this.sourceFilePattern = sourceFilePattern.strip();
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        if (!delegate.getRecipeList().isEmpty()) {
            throw new IllegalArgumentException("M3IopPatternScopedRecipe requires a leaf recipe");
        }
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public String getDescription() {
        return "M3 IOP pattern-only class hook around " + delegate.getName();
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "iop",
                "pattern",
                "pattern-only",
                "class-hooked",
                "guarded",
                "scoped",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> patternOnly = new JavaIsoVisitor<>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit compilationUnit, ExecutionContext context) {
                J.CompilationUnit visited = super.visitCompilationUnit(compilationUnit, context);
                return M3IopPatternClassHooks.admits(visited)
                        ? SearchResult.found(visited)
                        : visited;
            }
        };

        TreeVisitor<?, ExecutionContext> guarded =
                Preconditions.check(patternOnly, delegate.getVisitor());
        return Preconditions.check(
                new FindSourceFiles(sourceFilePattern).getVisitor(),
                guarded);
    }

    String sourceFilePattern() {
        return sourceFilePattern;
    }

    Recipe delegate() {
        return delegate;
    }
}
