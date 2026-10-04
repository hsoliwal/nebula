// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/** Makes the JLS 15.19 literal shift-distance mask explicit without dropping operand evaluation. */
public final class M3ShiftDistanceRecipe extends M3BoundedJdkRecipe {
    public M3ShiftDistanceRecipe() { this(null, null); }

    @JsonCreator
    public M3ShiftDistanceRecipe(@JsonProperty("sourcePath") String sourcePath,
            @JsonProperty("maxChangesPerFile") Integer maxChangesPerFile) {
        super(sourcePath, maxChangesPerFile);
    }

    @Override public String getDisplayName() { return "M3 normalize integral literal shift distances"; }
    @Override public String getDescription() {
        return "Masks literal shift distances to five bits for int and six bits for long; preserves the shift, primitive width and one evaluation of its left operand.";
    }

    @Override protected Visitor newVisitor() {
        return new Visitor() {
            @Override public J visitBinary(J.Binary binary, ExecutionContext context) {
                J visited = super.visitBinary(binary, context);
                if (!(visited instanceof J.Binary current) || !admitted(current, context)) return visited;
                String operator = switch (current.getOperator()) {
                    case LeftShift -> "<<";
                    case RightShift -> ">>";
                    case UnsignedRightShift -> ">>>";
                    default -> null;
                };
                JavaType type = current.getType();
                if (operator == null || (type != JavaType.Primitive.Int && type != JavaType.Primitive.Long)
                        || current.getLeft().getType() != type) return current;
                Long distance = M3JdkExpressionSupport.integerLiteral(current.getRight());
                if (distance == null) return current;
                boolean wide = type == JavaType.Primitive.Long;
                long masked = distance & (wide ? 63 : 31);
                if (distance == masked) return current;
                String template = "(#{any(" + (wide ? "long" : "int") + ")} " + operator + " " + masked + ")";
                return replace(current, template, context, current.getLeft());
            }
        };
    }
}
