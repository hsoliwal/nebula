// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

/** Replace only comment-free !(a == b) / !(a != b) forms with the equivalent binary operator. */
public final class M3NegatedEqualityRecipe extends Recipe {
    @Option(
            displayName = "Source file pattern",
            description = "Glob relative to the project root. Use an exact path for one-file M3 execution.",
            required = false)
    private final String sourceFilePattern;

    public M3NegatedEqualityRecipe() {
        this(M3MechanicalJavaRecipe.DEFAULT_SOURCE_FILE_PATTERN);
    }

    @JsonCreator
    public M3NegatedEqualityRecipe(String sourceFilePattern) {
        this.sourceFilePattern = pattern(sourceFilePattern);
    }

    @Override
    public String getDisplayName() {
        return "M3 normalize negated equality";
    }

    @Override
    public String getDescription() {
        return "Converts comment-free !(a == b) to a != b and !(a != b) to a == b.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("synexia", "m3", "mechanical", "boolean", "class-backed");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaVisitor<ExecutionContext> visitor = new JavaVisitor<>() {
            @Override
            public J visitUnary(J.Unary unary, ExecutionContext context) {
                J visited = super.visitUnary(unary, context);
                if (!(visited instanceof J.Unary current)
                        || current.getOperator() != J.Unary.Type.Not) {
                    return visited;
                }

                Expression expression = current.getExpression();
                if (!(expression instanceof J.Parentheses<?> parentheses)
                        || !(parentheses.getTree() instanceof J.Binary binary)
                        || !commentFree(parentheses, binary)) {
                    return current;
                }

                J.Binary.Type opposite;
                if (binary.getOperator() == J.Binary.Type.Equal) {
                    opposite = J.Binary.Type.NotEqual;
                } else if (binary.getOperator() == J.Binary.Type.NotEqual) {
                    opposite = J.Binary.Type.Equal;
                } else {
                    return current;
                }

                return binary.withOperator(opposite).withPrefix(current.getPrefix());
            }
        };
        return Preconditions.check(new FindSourceFiles(sourceFilePattern).getVisitor(), visitor);
    }

    public String getSourceFilePattern() {
        return sourceFilePattern;
    }

    private static boolean commentFree(J.Parentheses<?> parentheses, J.Binary binary) {
        AtomicBoolean comments = new AtomicBoolean();
        new JavaVisitor<AtomicBoolean>() {
            @Override
            public Space visitSpace(
                    Space space,
                    Space.Location location,
                    AtomicBoolean found) {
                if (!space.getComments().isEmpty()) {
                    found.set(true);
                }
                return super.visitSpace(space, location, found);
            }
        }.visit(parentheses, comments);
        return !comments.get() && binary.getPrefix().getComments().isEmpty();
    }

    private static String pattern(String value) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        return value.strip();
    }
}
