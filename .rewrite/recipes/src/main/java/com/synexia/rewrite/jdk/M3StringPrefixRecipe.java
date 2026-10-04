// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/** Prefix-only search candidate; preserves receiver/needle evaluation order and null failure. */
public final class M3StringPrefixRecipe extends M3BoundedJdkRecipe {
    public M3StringPrefixRecipe() { this(null, null); }

    @JsonCreator
    public M3StringPrefixRecipe(@JsonProperty("sourcePath") String sourcePath,
            @JsonProperty("maxChangesPerFile") Integer maxChangesPerFile) {
        super(sourcePath, maxChangesPerFile);
    }

    @Override public String getDisplayName() { return "M3 normalize String prefix predicates"; }
    @Override public String getDescription() {
        return "Replaces attributed String.indexOf(String) equality/inequality to zero with startsWith; excludes offset, char and lastIndexOf searches.";
    }

    @Override protected Visitor newVisitor() {
        return new Visitor() {
            private final MethodMatcher search = new MethodMatcher("java.lang.String indexOf(java.lang.String)");

            @Override public J visitBinary(J.Binary binary, ExecutionContext context) {
                J visited = super.visitBinary(binary, context);
                if (!(visited instanceof J.Binary current) || current.getType() != JavaType.Primitive.Boolean
                        || !admitted(current, context)
                        || (current.getOperator() != J.Binary.Type.Equal
                        && current.getOperator() != J.Binary.Type.NotEqual)) return visited;
                Expression call = current.getLeft().unwrap();
                Long zero = M3JdkExpressionSupport.integerLiteral(current.getRight());
                if (!(call instanceof J.MethodInvocation)) {
                    call = current.getRight().unwrap();
                    zero = M3JdkExpressionSupport.integerLiteral(current.getLeft());
                }
                if (zero == null || zero != 0 || !(call instanceof J.MethodInvocation invocation)
                        || !search.matches(invocation) || invocation.getSelect() == null
                        || invocation.getArguments().size() != 1) return current;
                String template = (current.getOperator() == J.Binary.Type.NotEqual ? "!" : "")
                        + "#{any(java.lang.String)}.startsWith(#{any(java.lang.String)})";
                return replace(current, template, context, invocation.getSelect(), invocation.getArguments().getFirst());
            }
        };
    }
}
