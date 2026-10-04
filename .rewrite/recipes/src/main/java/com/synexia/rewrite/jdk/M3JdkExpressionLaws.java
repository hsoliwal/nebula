// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

/** Small, dependency-free admission laws shared by recipes and differential tests. */
public final class M3JdkExpressionLaws {
    public enum Comparison { EQ, NE, LT, LE, GT, GE }
    public enum Polarity { NONE, POSITIVE, NEGATIVE }
    public enum Operator { ADD, SUBTRACT, MULTIPLY, DIVIDE, OR, XOR, AND, SHL, SHR, USHR }

    private M3JdkExpressionLaws() {}

    /** String.length is nonnegative; only predicates equivalent to zero/nonzero are admitted. */
    public static Polarity lengthPredicate(Comparison comparison, long constant) {
        java.util.Objects.requireNonNull(comparison, "comparison");
        if (constant == 0) {
            return switch (comparison) {
                case EQ, LE -> Polarity.POSITIVE;
                case NE, GT -> Polarity.NEGATIVE;
                default -> Polarity.NONE;
            };
        }
        if (constant == 1) {
            return switch (comparison) {
                case LT -> Polarity.POSITIVE;
                case GE -> Polarity.NEGATIVE;
                default -> Polarity.NONE;
            };
        }
        return Polarity.NONE;
    }

    /** String.indexOf(String) returns -1 or a nonnegative UTF-16 index. */
    public static Polarity searchPredicate(Comparison comparison, long constant) {
        java.util.Objects.requireNonNull(comparison, "comparison");
        if (constant == -1) {
            return switch (comparison) {
                case NE, GT -> Polarity.POSITIVE;
                case EQ, LE -> Polarity.NEGATIVE;
                default -> Polarity.NONE;
            };
        }
        if (constant == 0) {
            return switch (comparison) {
                case GE -> Polarity.POSITIVE;
                case LT -> Polarity.NEGATIVE;
                default -> Polarity.NONE;
            };
        }
        return Polarity.NONE;
    }

    public static Comparison reversed(Comparison comparison) {
        return switch (java.util.Objects.requireNonNull(comparison, "comparison")) {
            case EQ -> Comparison.EQ;
            case NE -> Comparison.NE;
            case LT -> Comparison.GT;
            case LE -> Comparison.GE;
            case GT -> Comparison.LT;
            case GE -> Comparison.LE;
        };
    }

    /** Requires identical int/long operand and result types at the LST admission boundary. */
    public static boolean integralIdentity(Operator operator, boolean literalOnLeft, long literal) {
        return switch (java.util.Objects.requireNonNull(operator, "operator")) {
            case ADD, OR, XOR -> literal == 0;
            case MULTIPLY -> literal == 1;
            case AND -> literal == -1;
            case SUBTRACT, SHL, SHR, USHR -> !literalOnLeft && literal == 0;
            case DIVIDE -> !literalOnLeft && literal == 1;
        };
    }

    /** A Java char literal for exactly one UTF-16 unit, including NUL and isolated surrogates. */
    public static String charLiteral(char value) {
        String escaped = switch (value) {
            case '\b' -> "\\b";
            case '\t' -> "\\t";
            case '\n' -> "\\n";
            case '\f' -> "\\f";
            case '\r' -> "\\r";
            case '\'' -> "\\'";
            case '\\' -> "\\\\";
            default -> value >= 32 && value <= 126
                    ? Character.toString(value)
                    : value < 32 || value == 127
                            ? String.format(java.util.Locale.ROOT, "\\%03o", (int) value)
                            : String.format(java.util.Locale.ROOT, "\\u%04x", (int) value);
        };
        return "'" + escaped + "'";
    }
}
