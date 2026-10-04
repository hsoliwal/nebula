// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
/** Candidate transformation; independent compile/test/runtime gates remain mandatory. */
public final class M3PrimitiveBooleanRecipe extends M3BoundedJdkRecipe {
    public M3PrimitiveBooleanRecipe() { this(null, null); }
    @JsonCreator public M3PrimitiveBooleanRecipe(
            @JsonProperty("sourcePath") String sourcePath,
            @JsonProperty("maxChangesPerFile") Integer maxChangesPerFile) {
        super(sourcePath, maxChangesPerFile);
    }
    @Override public String getDisplayName() { return "M3 simplify primitive-boolean expressions"; }
    @Override public String getDescription() { return "Simplifies literal equality, opposite-branch ternaries and double negation only for primitive boolean operands."; }
    @Override protected Visitor newVisitor() {
        return new Visitor() {
            @Override public J visitBinary(J.Binary binary, ExecutionContext context) {
                J visited = super.visitBinary(binary, context);
                if (!(visited instanceof J.Binary current) || !admitted(current, context)
                        || (current.getOperator() != J.Binary.Type.Equal
                            && current.getOperator() != J.Binary.Type.NotEqual)) return visited;
                Expression operand = current.getLeft();
                Boolean literal = M3JdkExpressionSupport.booleanLiteral(current.getRight());
                if (literal == null) {
                    operand = current.getRight();
                    literal = M3JdkExpressionSupport.booleanLiteral(current.getLeft());
                }
                if (literal == null || operand.getType() != JavaType.Primitive.Boolean) return current;
                boolean positive = literal == (current.getOperator() == J.Binary.Type.Equal);
                return simplify(current, operand, positive, context);
            }
            @Override public J visitTernary(J.Ternary ternary, ExecutionContext context) {
                J visited = super.visitTernary(ternary, context);
                if (!(visited instanceof J.Ternary current) || !admitted(current, context)
                        || current.getCondition().getType() != JavaType.Primitive.Boolean) return visited;
                Boolean yes = M3JdkExpressionSupport.booleanLiteral(current.getTruePart());
                Boolean no = M3JdkExpressionSupport.booleanLiteral(current.getFalsePart());
                if (yes == null || no == null || yes.equals(no)) return current;
                return simplify(current, current.getCondition(), yes, context);
            }
            @Override public J visitUnary(J.Unary unary, ExecutionContext context) {
                J visited = super.visitUnary(unary, context);
                if (!(visited instanceof J.Unary current) || !admitted(current, context)
                        || current.getOperator() != J.Unary.Type.Not
                        || !(current.getExpression().unwrap() instanceof J.Unary inner)
                        || inner.getOperator() != J.Unary.Type.Not
                        || inner.getExpression().getType() != JavaType.Primitive.Boolean) return visited;
                return simplify(current, inner.getExpression(), true, context);
            }
            private J simplify(Expression before, Expression operand, boolean positive, ExecutionContext context) {
                // Boolean equivalence does not preserve flow-scoped pattern bindings. Removing
                // equality/ternary structure can shadow a field or local beyond this expression.
                // Keep that candidate unchanged until a binding-aware wider rewrite is admitted.
                PatternBindingFinder bindings = new PatternBindingFinder();
                bindings.visit(operand, null);
                if (bindings.found) return before;
                return replace(before, positive ? "(#{any(boolean)})" : "!(#{any(boolean)})", context, operand);
            }
        };
    }

    /** Conservative LST-only gate: unfamiliar instanceof right-hand forms also remain unchanged. */
    private static final class PatternBindingFinder extends JavaVisitor<Void> {
        private boolean found;

        @Override public J visitInstanceOf(J.InstanceOf instanceOf, Void unused) {
            if (instanceOf.getPattern() != null || !(instanceOf.getClazz() instanceof TypeTree)) {
                found = true;
                return instanceOf;
            }
            return found ? instanceOf : super.visitInstanceOf(instanceOf, unused);
        }
    }
}
