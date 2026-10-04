// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.JavaType;
/** Candidate transformation; independent compile/test/runtime gates remain mandatory. */
public final class M3StringIndexOfContainsRecipe extends M3BoundedJdkRecipe {
    public M3StringIndexOfContainsRecipe() { this(null, null); }
    @JsonCreator public M3StringIndexOfContainsRecipe(
            @JsonProperty("sourcePath") String sourcePath,
            @JsonProperty("maxChangesPerFile") Integer maxChangesPerFile) {
        super(sourcePath, maxChangesPerFile);
    }
    @Override public String getDisplayName() { return "M3 normalize String search-presence predicates"; }
    @Override public String getDescription() { return "Replaces only -1/nonnegative String.indexOf(String) predicates; excludes char, offset and positional comparisons."; }
    @Override protected Visitor newVisitor() {
        return new Visitor() {
            private final MethodMatcher search = new MethodMatcher("java.lang.String indexOf(java.lang.String)");
            @Override public J visitBinary(J.Binary binary, ExecutionContext context) {
                J visited = super.visitBinary(binary, context);
                if (!(visited instanceof J.Binary current) || current.getType() != JavaType.Primitive.Boolean
                        || !admitted(current, context)) return visited;
                var comparison = M3JdkExpressionSupport.comparison(current.getOperator());
                if (comparison == null) return current;
                Expression call = current.getLeft().unwrap();
                Long constant = M3JdkExpressionSupport.integerLiteral(current.getRight());
                if (!(call instanceof J.MethodInvocation)) {
                    call = current.getRight().unwrap();
                    constant = M3JdkExpressionSupport.integerLiteral(current.getLeft());
                    comparison = M3JdkExpressionLaws.reversed(comparison);
                }
                if (!(call instanceof J.MethodInvocation invocation) || !search.matches(invocation)
                        || invocation.getSelect() == null || invocation.getArguments().size() != 1
                        || constant == null) return current;
                var polarity = M3JdkExpressionLaws.searchPredicate(comparison, constant);
                if (polarity == M3JdkExpressionLaws.Polarity.NONE) return current;
                String template = (polarity == M3JdkExpressionLaws.Polarity.NEGATIVE ? "!" : "")
                        + "#{any(java.lang.String)}.contains(#{any(java.lang.String)})";
                return replace(current, template, context, invocation.getSelect(), invocation.getArguments().getFirst());
            }
        };
    }
}
