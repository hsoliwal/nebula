// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.JavaType;
/** Candidate transformation; independent compile/test/runtime gates remain mandatory. */
public final class M3IntegralIdentityRecipe extends M3BoundedJdkRecipe {
    public M3IntegralIdentityRecipe() { this(null, null); }
    @JsonCreator public M3IntegralIdentityRecipe(
            @JsonProperty("sourcePath") String sourcePath,
            @JsonProperty("maxChangesPerFile") Integer maxChangesPerFile) {
        super(sourcePath, maxChangesPerFile);
    }
    @Override public String getDisplayName() { return "M3 remove type-preserving integral identities"; }
    @Override public String getDescription() { return "Removes neutral int/long arithmetic and bitwise operands without removing evaluation or changing primitive width."; }
    @Override protected Visitor newVisitor() {
        return new Visitor() {
            @Override public J visitBinary(J.Binary binary, ExecutionContext context) {
                J visited = super.visitBinary(binary, context);
                if (!(visited instanceof J.Binary current) || !admitted(current, context)
                        || (current.getType() != JavaType.Primitive.Int
                            && current.getType() != JavaType.Primitive.Long)) return visited;
                var operator = M3JdkExpressionSupport.integralOperator(current.getOperator());
                if (operator == null) return current;
                Expression operand = current.getLeft();
                Long constant = M3JdkExpressionSupport.integerLiteral(current.getRight());
                boolean literalOnLeft = false;
                if (constant == null || !M3JdkExpressionLaws.integralIdentity(operator, false, constant)) {
                    operand = current.getRight();
                    constant = M3JdkExpressionSupport.integerLiteral(current.getLeft());
                    literalOnLeft = true;
                }
                if (constant == null || operand.getType() != current.getType()
                        || !M3JdkExpressionLaws.integralIdentity(operator, literalOnLeft, constant)) return current;
                String type = current.getType() == JavaType.Primitive.Int ? "int" : "long";
                return replace(current, "(#{any(" + type + ")})", context, operand);
            }
        };
    }
}
