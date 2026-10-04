// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.OrderImports;
import org.openrewrite.java.RemoveUnusedImports;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.java.tree.J;

/**
 * Bounded class-backed mechanical Java recipe for M3.
 *
 * <p>The recipe deliberately composes only Apache-2.0 OpenRewrite core/rewrite-java classes. It
 * produces a candidate source transformation; M3 diff/lint/compile/test/runtime gates remain the
 * authority for promotion.</p>
 */
public final class M3MechanicalJavaRecipe extends Recipe {
    public static final String DEFAULT_SOURCE_FILE_PATTERN = "**/*.java";

    @Option(
            displayName = "Source file pattern",
            description = "Glob relative to the project root. Use an exact path for one-file M3 execution.",
            required = false,
            example = "src/main/java/com/acme/Foo.java")
    private final String sourceFilePattern;

    @Option(
            displayName = "Remove unused imports",
            description = "Run the Apache-2.0 core RemoveUnusedImports recipe.",
            required = false)
    private final boolean removeUnusedImports;

    @Option(
            displayName = "Order imports",
            description = "Run the Apache-2.0 core OrderImports recipe without implicit unused-import removal.",
            required = false)
    private final boolean orderImports;

    @Option(
            displayName = "Auto format",
            description = "Run the Apache-2.0 core AutoFormat recipe after structural cleanup.",
            required = false)
    private final boolean autoFormat;

    /** OpenRewrite/Maven activation constructor with conservative deterministic defaults. */
    public M3MechanicalJavaRecipe() {
        this(DEFAULT_SOURCE_FILE_PATTERN, true, true, true);
    }

    /** Direct Java hook for generated/configured recipe plans and declarative option binding. */
    @JsonCreator
    public M3MechanicalJavaRecipe(
            String sourceFilePattern,
            boolean removeUnusedImports,
            boolean orderImports,
            boolean autoFormat) {
        if (sourceFilePattern == null || sourceFilePattern.isBlank() || sourceFilePattern.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        this.sourceFilePattern = sourceFilePattern.strip();
        this.removeUnusedImports = removeUnusedImports;
        this.orderImports = orderImports;
        this.autoFormat = autoFormat;
    }

    @Override
    public String getDisplayName() {
        return "M3 mechanical Java cleanup";
    }

    @Override
    public String getDescription() {
        return "Runs bounded Apache-2.0 OpenRewrite core Java cleanup behind an explicit source-file fence. "
                + "The resulting patch is an M3 candidate, not behavior-equivalence proof.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("synexia", "m3", "iop", "mechanical", "class-backed");
    }

    /** M3 owns multi-pass orchestration; this recipe performs one bounded rewrite cycle. */
    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> cleanup = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit compilationUnit, ExecutionContext context) {
                J.CompilationUnit visited = super.visitCompilationUnit(compilationUnit, context);
                if (removeUnusedImports) {
                    doAfterVisit(new RemoveUnusedImports().getVisitor());
                }
                if (orderImports) {
                    doAfterVisit(new OrderImports(false).getVisitor());
                }
                if (autoFormat) {
                    doAfterVisit(new AutoFormat(null, null).getVisitor());
                }
                return visited;
            }
        };
        return Preconditions.check(new FindSourceFiles(sourceFilePattern).getVisitor(), cleanup);
    }

    public String getSourceFilePattern() {
        return sourceFilePattern;
    }

    public boolean isRemoveUnusedImports() {
        return removeUnusedImports;
    }

    public boolean isOrderImports() {
        return orderImports;
    }

    public boolean isAutoFormat() {
        return autoFormat;
    }
}
