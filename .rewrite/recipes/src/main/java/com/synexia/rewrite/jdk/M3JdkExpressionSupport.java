// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

final class M3JdkExpressionSupport {
    private M3JdkExpressionSupport() {}

    /**
     * OpenRewrite 8.17 separates escaped surrogate units from literal values for serialization.
     * Recover only one quoted surrogate with exact metadata; do not decode arbitrary source text.
     */
    static Character singleUtf16Unit(J.Literal literal) {
        if (literal.getType() != JavaType.Primitive.String
                || !(literal.getValue() instanceof String value)) return null;
        var escapes = literal.getUnicodeEscapes();
        if (escapes == null || escapes.isEmpty()) return value.length() == 1 ? value.charAt(0) : null;
        if (escapes.size() != 1 || !"\"\"".equals(literal.getValueSource())
                || !"\"\"".equals(value)) return null;
        var escape = escapes.getFirst();
        if (escape.getValueSourceIndex() != 1 || escape.getCodePoint().length() != 4) return null;
        int decoded = 0;
        for (int i = 0; i < 4; i++) {
            int digit = Character.digit(escape.getCodePoint().charAt(i), 16);
            if (digit < 0) return null;
            decoded = (decoded << 4) | digit;
        }
        return Character.isSurrogate((char) decoded) ? (char) decoded : null;
    }

    static M3JdkExpressionLaws.Comparison comparison(J.Binary.Type type) {
        return switch (type) {
            case Equal -> M3JdkExpressionLaws.Comparison.EQ;
            case NotEqual -> M3JdkExpressionLaws.Comparison.NE;
            case LessThan -> M3JdkExpressionLaws.Comparison.LT;
            case LessThanOrEqual -> M3JdkExpressionLaws.Comparison.LE;
            case GreaterThan -> M3JdkExpressionLaws.Comparison.GT;
            case GreaterThanOrEqual -> M3JdkExpressionLaws.Comparison.GE;
            default -> null;
        };
    }

    static Long integerLiteral(Expression expression) {
        Expression unwrapped = expression.unwrap();
        if (unwrapped instanceof J.Literal literal) {
            if (literal.getType() == JavaType.Primitive.Int && literal.getValue() instanceof Integer value) {
                return value.longValue();
            }
            if (literal.getType() == JavaType.Primitive.Long && literal.getValue() instanceof Long value) return value;
        }
        if (unwrapped instanceof J.Unary unary && unary.getOperator() == J.Unary.Type.Negative
                && unary.getExpression().unwrap() instanceof J.Literal literal) {
            // Only the nonnegative magnitudes needed by admission laws; avoid MIN_VALUE double negation.
            if (literal.getType() == JavaType.Primitive.Int && literal.getValue() instanceof Integer value && value >= 0) return -(long) value;
            if (literal.getType() == JavaType.Primitive.Long && literal.getValue() instanceof Long value && value >= 0) return -value;
        }
        return null;
    }

    static Boolean booleanLiteral(Expression expression) {
        return expression.unwrap() instanceof J.Literal literal
                && literal.getType() == JavaType.Primitive.Boolean
                && literal.getValue() instanceof Boolean value ? value : null;
    }

    static M3JdkExpressionLaws.Operator integralOperator(J.Binary.Type type) {
        return switch (type) {
            case Addition -> M3JdkExpressionLaws.Operator.ADD;
            case Subtraction -> M3JdkExpressionLaws.Operator.SUBTRACT;
            case Multiplication -> M3JdkExpressionLaws.Operator.MULTIPLY;
            case Division -> M3JdkExpressionLaws.Operator.DIVIDE;
            case BitOr -> M3JdkExpressionLaws.Operator.OR;
            case BitXor -> M3JdkExpressionLaws.Operator.XOR;
            case BitAnd -> M3JdkExpressionLaws.Operator.AND;
            case LeftShift -> M3JdkExpressionLaws.Operator.SHL;
            case RightShift -> M3JdkExpressionLaws.Operator.SHR;
            case UnsignedRightShift -> M3JdkExpressionLaws.Operator.USHR;
            default -> null;
        };
    }
}
