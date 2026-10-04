// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

/**
 * Typed class-backed wrapper for one canonical M3 pass, admitted only for explicit IOP pattern
 * source atoms.
 *
 * <p>Path admission is necessary but not sufficient. Class hooks must also prove that every
 * top-level class in the file is an explicit pattern owner or declared pattern participant.</p>
 */
public final class M3IopGuardedRecipe extends Recipe {
    @Option(
            displayName = "Pass id",
            description = "One canonical M3 mechanical pass id.",
            example = "01-upper-case-literal-suffixes")
    private final String passId;

    @Option(
            displayName = "Source file pattern",
            description = "Additional source glob. IOP production-source admission remains mandatory.",
            required = false)
    private final String sourceFilePattern;

    public M3IopGuardedRecipe() {
        this("01-upper-case-literal-suffixes", M3MechanicalJavaRecipe.DEFAULT_SOURCE_FILE_PATTERN);
    }

    @JsonCreator
    public M3IopGuardedRecipe(String passId, String sourceFilePattern) {
        if (passId == null || passId.isBlank() || passId.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("passId required");
        }
        if (sourceFilePattern == null
                || sourceFilePattern.isBlank()
                || sourceFilePattern.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        this.passId = canonicalPassId(passId.strip());
        this.sourceFilePattern = sourceFilePattern.strip();
    }

    @Override
    public String getDisplayName() {
        return "M3 IOP-only guarded mechanical pass";
    }

    @Override
    public String getDescription() {
        return "Runs one admitted canonical M3 OpenRewrite pass only when the Java source belongs "
                + "to the Synexia IOP production-source boundary and every top-level class is "
                + "admitted by deterministic IOP pattern class hooks.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "iop",
                "pattern",
                "guarded",
                "class-hooked",
                "class-backed",
                "iop-only",
                "pattern-only");
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
        return Preconditions.check(patternOnly, delegate().getVisitor());
    }

    public String getPassId() {
        return passId;
    }

    public String getSourceFilePattern() {
        return sourceFilePattern;
    }

    private Recipe delegate() {
        return switch (passId) {
            case "01-upper-case-literal-suffixes" ->
                    new M3UpperCaseLiteralSuffixesRecipe(sourceFilePattern);
            case "02-modifier-order" ->
                    new M3ModifierOrderRecipe(sourceFilePattern);
            case "03-negated-equality" ->
                    new M3NegatedEqualityRecipe(sourceFilePattern);
            case "04-remove-unused-imports" ->
                    new M3MechanicalJavaRecipe(sourceFilePattern, true, false, false);
            case "05-order-imports" ->
                    new M3MechanicalJavaRecipe(sourceFilePattern, false, true, false);
            case "06-auto-format" ->
                    new M3MechanicalJavaRecipe(sourceFilePattern, false, false, true);
            default -> throw new IllegalStateException("pass escaped constructor validation: " + passId);
        };
    }

    private static String canonicalPassId(String value) {
        return switch (value) {
            case "01-upper-case-literal-suffixes",
                    "02-modifier-order",
                    "03-negated-equality",
                    "04-remove-unused-imports",
                    "05-order-imports",
                    "06-auto-format" -> value;
            default -> throw new IllegalArgumentException("unknown canonical M3 mechanical pass: " + value);
        };
    }
}
