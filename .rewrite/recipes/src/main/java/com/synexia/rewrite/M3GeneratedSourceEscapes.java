// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Objects;

/**
 * Canonical escaping for Java source emitted by M3 recipe/scaffold generators.
 *
 * <p>This class is deliberately small and package-local. Generated source must never depend on
 * ad-hoc escaping copies in individual recipe writers.</p>
 */
final class M3GeneratedSourceEscapes {
    private M3GeneratedSourceEscapes() {}

    static String javaString(String value) {
        String input = Objects.requireNonNull(value, "value");
        StringBuilder out = new StringBuilder(input.length() + 16);
        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            switch (current) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\b' -> out.append("\\b");
                case '\t' -> out.append("\\t");
                case '\n' -> out.append("\\n");
                case '\f' -> out.append("\\f");
                case '\r' -> out.append("\\r");
                default -> {
                    if (Character.isISOControl(current)) {
                        appendOctal(out, current);
                    } else {
                        out.append(current);
                    }
                }
            }
        }
        return out.toString();
    }

    static String javadoc(String value) {
        String input = Objects.requireNonNull(value, "value").replace("*/", "* /");
        StringBuilder out = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            if (Character.isISOControl(current)) {
                out.append(' ');
            } else {
                out.append(current);
            }
        }
        return out.toString();
    }

    private static void appendOctal(StringBuilder out, char value) {
        out.append('\\')
                .append((char) ('0' + ((value >>> 6) & 7)))
                .append((char) ('0' + ((value >>> 3) & 7)))
                .append((char) ('0' + (value & 7)));
    }
}
