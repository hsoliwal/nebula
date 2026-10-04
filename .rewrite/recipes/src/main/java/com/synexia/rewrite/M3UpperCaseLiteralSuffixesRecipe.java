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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/** Canonicalize Java numeric literal suffixes without changing their values or types. */
public final class M3UpperCaseLiteralSuffixesRecipe extends Recipe {
    @Option(
            displayName = "Source file pattern",
            description = "Glob relative to the project root. Use an exact path for one-file M3 execution.",
            required = false)
    private final String sourceFilePattern;

    public M3UpperCaseLiteralSuffixesRecipe() {
        this(M3MechanicalJavaRecipe.DEFAULT_SOURCE_FILE_PATTERN);
    }

    @JsonCreator
    public M3UpperCaseLiteralSuffixesRecipe(String sourceFilePattern) {
        this.sourceFilePattern = pattern(sourceFilePattern);
    }

    @Override
    public String getDisplayName() {
        return "M3 upper-case numeric literal suffixes";
    }

    @Override
    public String getDescription() {
        return "Changes only lower-case Java long/float/double literal suffixes to their upper-case spelling.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("synexia", "m3", "mechanical", "literal", "class-backed");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> visitor = new JavaIsoVisitor<>() {
            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext context) {
                J.Literal visited = super.visitLiteral(literal, context);
                String valueSource = visited.getValueSource();
                Character lowerSuffix = lowerSuffix(visited.getType());
                if (valueSource == null || valueSource.isEmpty() || lowerSuffix == null) {
                    return visited;
                }
                int suffixIndex = valueSource.length() - 1;
                if (valueSource.charAt(suffixIndex) != lowerSuffix.charValue()) {
                    return visited;
                }
                String canonical = valueSource.substring(0, suffixIndex)
                        + Character.toUpperCase(lowerSuffix.charValue());
                return visited.withValueSource(canonical);
            }
        };
        return Preconditions.check(new FindSourceFiles(sourceFilePattern).getVisitor(), visitor);
    }

    public String getSourceFilePattern() {
        return sourceFilePattern;
    }

    private static Character lowerSuffix(JavaType.Primitive primitive) {
        if (primitive == JavaType.Primitive.Long) {
            return 'l';
        }
        if (primitive == JavaType.Primitive.Float) {
            return 'f';
        }
        if (primitive == JavaType.Primitive.Double) {
            return 'd';
        }
        return null;
    }

    private static String pattern(String value) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        return value.strip();
    }
}
