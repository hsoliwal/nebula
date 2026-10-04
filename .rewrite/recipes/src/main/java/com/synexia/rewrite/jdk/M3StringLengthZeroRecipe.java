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
public final class M3StringLengthZeroRecipe extends M3BoundedJdkRecipe {
    public M3StringLengthZeroRecipe() { this(null, null); }
    @JsonCreator public M3StringLengthZeroRecipe(
            @JsonProperty("sourcePath") String sourcePath,
            @JsonProperty("maxChangesPerFile") Integer maxChangesPerFile) {
        super(sourcePath, maxChangesPerFile);
    }
    @Override public String getDisplayName() { return "M3 use String.isEmpty for zero-length predicates"; }
    @Override public String getDescription() { return "Replaces admitted String.length comparisons with isEmpty or its negation, preserving receiver evaluation."; }
    @Override protected Visitor newVisitor() {
        return new Visitor() {
            private final MethodMatcher length = new MethodMatcher("java.lang.String length()");
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
                if (!(call instanceof J.MethodInvocation invocation) || !length.matches(invocation)
                        || invocation.getSelect() == null || constant == null) return current;
                var polarity = M3JdkExpressionLaws.lengthPredicate(comparison, constant);
                if (polarity == M3JdkExpressionLaws.Polarity.NONE) return current;
                String template = (polarity == M3JdkExpressionLaws.Polarity.NEGATIVE ? "!" : "")
                        + "#{any(java.lang.String)}.isEmpty()";
                return replace(current, template, context, invocation.getSelect());
            }
        };
    }
}
