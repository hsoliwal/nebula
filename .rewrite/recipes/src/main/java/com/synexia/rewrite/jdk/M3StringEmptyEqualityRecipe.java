// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.MethodMatcher;
/** Candidate transformation; independent compile/test/runtime gates remain mandatory. */
public final class M3StringEmptyEqualityRecipe extends M3BoundedJdkRecipe {
    public M3StringEmptyEqualityRecipe() { this(null, null); }
    @JsonCreator public M3StringEmptyEqualityRecipe(
            @JsonProperty("sourcePath") String sourcePath,
            @JsonProperty("maxChangesPerFile") Integer maxChangesPerFile) {
        super(sourcePath, maxChangesPerFile);
    }
    @Override public String getDisplayName() { return "M3 use String.isEmpty for empty-literal equality"; }
    @Override public String getDescription() { return "Replaces String.equals or equalsIgnoreCase against an empty literal; does not reverse null-safe literal receivers."; }
    @Override protected Visitor newVisitor() {
        return new Visitor() {
            private final MethodMatcher equals = new MethodMatcher("java.lang.String equals(java.lang.Object)");
            private final MethodMatcher ignoreCase = new MethodMatcher("java.lang.String equalsIgnoreCase(java.lang.String)");
            @Override public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J visited = super.visitMethodInvocation(method, context);
                if (!(visited instanceof J.MethodInvocation current) || !admitted(current, context)
                        || current.getSelect() == null || current.getArguments().size() != 1
                        || (!equals.matches(current) && !ignoreCase.matches(current))) return visited;
                if (!(current.getArguments().getFirst().unwrap() instanceof J.Literal literal)
                        || !"".equals(literal.getValue())) return current;
                return replace(current, "#{any(java.lang.String)}.isEmpty()", context, current.getSelect());
            }
        };
    }
}
