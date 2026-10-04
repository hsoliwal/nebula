// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * Candidate for integral even/odd predicates, including negative values.
 *
 * <p>Bit-parity reference: TheAlgorithms/Java IsEven by Bama Charan Chhandogi,
 * revision 550a7ed124324a9c9ad3ff664123dc41ffdbba7d (MIT). Attribution and full
 * license are packaged under META-INF/synexia/donors/thealgorithms-java/.
 * The typed OpenRewrite admission/visitor is independent Synexia integration.</p>
 */
public final class M3IntegralParityRecipe extends M3BoundedJdkRecipe {
    public M3IntegralParityRecipe() { this(null, null); }

    @JsonCreator
    public M3IntegralParityRecipe(@JsonProperty("sourcePath") String sourcePath,
            @JsonProperty("maxChangesPerFile") Integer maxChangesPerFile) {
        super(sourcePath, maxChangesPerFile);
    }

    @Override public String getDisplayName() { return "M3 normalize integral parity predicates"; }
    @Override public String getDescription() {
        return "Replaces primitive int/long remainder by literal 2 or -2 compared with zero by a low-bit test; preserves evaluation and rejects sign-dependent oddness tests.";
    }

    @Override protected Visitor newVisitor() {
        return new Visitor() {
            @Override public J visitBinary(J.Binary binary, ExecutionContext context) {
                J visited = super.visitBinary(binary, context);
                if (!(visited instanceof J.Binary current) || current.getType() != JavaType.Primitive.Boolean
                        || !admitted(current, context)
                        || (current.getOperator() != J.Binary.Type.Equal
                        && current.getOperator() != J.Binary.Type.NotEqual)) return visited;
                Expression operand = current.getLeft().unwrap();
                Long zero = M3JdkExpressionSupport.integerLiteral(current.getRight());
                if (!(operand instanceof J.Binary)) {
                    operand = current.getRight().unwrap();
                    zero = M3JdkExpressionSupport.integerLiteral(current.getLeft());
                }
                if (zero == null || zero != 0 || !(operand instanceof J.Binary remainder)
                        || remainder.getOperator() != J.Binary.Type.Modulo) return current;
                JavaType type = remainder.getType();
                if ((type != JavaType.Primitive.Int && type != JavaType.Primitive.Long)
                        || remainder.getLeft().getType() != type) return current;
                Long divisor = M3JdkExpressionSupport.integerLiteral(remainder.getRight());
                if (divisor == null || (divisor != 2 && divisor != -2)) return current;
                boolean wide = type == JavaType.Primitive.Long;
                String template = "(#{any(" + (wide ? "long" : "int") + ")} & " + (wide ? "1L" : "1") + ") "
                        + (current.getOperator() == J.Binary.Type.Equal ? "==" : "!=") + " 0";
                return replace(current, template, context, remainder.getLeft());
            }
        };
    }
}
