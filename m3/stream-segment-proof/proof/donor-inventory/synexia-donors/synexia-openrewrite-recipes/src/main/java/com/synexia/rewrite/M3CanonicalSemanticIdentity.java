// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.StringJoiner;
import org.openrewrite.java.tree.JavaType;

/** Canonical, fail-closed semantic identity normalization shared by M3 evidence projections. */
final class M3CanonicalSemanticIdentity {
    private M3CanonicalSemanticIdentity() {}

    static boolean comparableMethod(JavaType.Method method) {
        JavaType.Method resolved = M3SemanticAttribution.resolvedMethod(method);
        if (resolved == null) {
            return false;
        }
        if (!resolved.getParameterTypes().stream().allMatch(M3CanonicalSemanticIdentity::comparableType)) {
            return false;
        }
        return resolved.isConstructor() || comparableType(resolved.getReturnType());
    }

    static boolean comparableType(JavaType type) {
        if (!M3SemanticAttribution.isResolvedType(type)) {
            return false;
        }
        if (type instanceof JavaType.Primitive) {
            return true;
        }
        if (type instanceof JavaType.Array array) {
            return comparableType(array.getElemType());
        }
        if (type instanceof JavaType.Parameterized parameterized) {
            return M3SemanticAttribution.fullyQualified(parameterized) != null
                    && parameterized.getTypeParameters().stream()
                            .allMatch(M3CanonicalSemanticIdentity::comparableType);
        }
        return M3SemanticAttribution.fullyQualified(type) != null;
    }

    static String codeType(JavaType type) {
        if (!comparableType(type)) {
            throw new IllegalArgumentException("comparable Java type required");
        }
        return M3SemanticAttribution.typeName(type);
    }

    static String graphType(JavaType type) {
        if (!comparableType(type)) {
            throw new IllegalArgumentException("comparable Java type required");
        }
        if (type instanceof JavaType.Primitive primitive) {
            return primitive.getKeyword();
        }
        if (type instanceof JavaType.Array array) {
            return graphType(array.getElemType()) + "[]";
        }
        if (type instanceof JavaType.Parameterized parameterized) {
            StringJoiner out =
                    new StringJoiner(",", parameterized.getFullyQualifiedName() + "<", ">");
            parameterized.getTypeParameters().forEach(parameter -> out.add(graphType(parameter)));
            return out.toString();
        }
        JavaType.FullyQualified fullyQualified = M3SemanticAttribution.fullyQualified(type);
        if (fullyQualified == null) {
            throw new IllegalStateException("comparable type lost attribution");
        }
        return fullyQualified.getFullyQualifiedName();
    }

    static String codeMethodIdentity(JavaType.Method method) {
        if (!comparableMethod(method)) {
            throw new IllegalArgumentException("comparable Java method required");
        }
        return M3SemanticAttribution.methodKey(method);
    }

    static String graphMethodIdentity(JavaType.Method method) {
        JavaType.Method resolved = M3SemanticAttribution.resolvedMethod(method);
        if (resolved == null || !comparableMethod(resolved)) {
            throw new IllegalArgumentException("comparable Java method required");
        }
        StringJoiner parameters = new StringJoiner(",", "(", ")");
        resolved.getParameterTypes().forEach(type -> parameters.add(graphType(type)));
        return "M|"
                + M3SemanticAttribution.typeName(resolved.getDeclaringType())
                + "#"
                + resolved.getName()
                + parameters;
    }

    static String canonicalCodeType(String codeIdentity) {
        return "T|" + requireComparableText(codeIdentity, "code type identity");
    }

    static String canonicalGraphType(String graphIdentity) {
        String value = requireComparableText(graphIdentity, "graph type identity");
        if (!value.startsWith("T|") || value.length() == 2) {
            throw new IllegalArgumentException("invalid graph type identity: " + value);
        }
        return value;
    }

    static String canonicalCodeMethod(String codeIdentity) {
        String value = requireComparableText(codeIdentity, "code method identity");
        int open = value.indexOf('(');
        int close = value.lastIndexOf("):");
        if (open <= 0 || close <= open) {
            throw new IllegalArgumentException("invalid code method identity: " + value);
        }
        String head = value.substring(0, open);
        String parameters = eraseParameterizedArguments(value.substring(open + 1, close));
        return "M|" + head + "(" + parameters + ")";
    }

    static String canonicalGraphMethod(String graphIdentity) {
        String value = requireComparableText(graphIdentity, "graph method identity");
        if (!value.startsWith("M|")) {
            throw new IllegalArgumentException("invalid graph method identity: " + value);
        }
        int open = value.indexOf('(');
        int close = value.lastIndexOf(')');
        if (open <= 2 || close <= open || close != value.length() - 1) {
            throw new IllegalArgumentException("invalid graph method identity: " + value);
        }
        String head = value.substring(0, open);
        String parameters = normalizeGraphTypeSequence(
                eraseParameterizedArguments(value.substring(open + 1, close)));
        return head + "(" + parameters + ")";
    }

    static String canonicalCodeField(String codeIdentity) {
        String value = requireComparableText(codeIdentity, "code field identity");
        int colon = value.lastIndexOf(':');
        if (colon <= 0 || colon == value.length() - 1 || value.indexOf('#') < 1) {
            throw new IllegalArgumentException("invalid code field identity: " + value);
        }
        return "F|" + value.substring(0, colon + 1)
                + eraseParameterizedArguments(value.substring(colon + 1));
    }

    static String canonicalGraphField(String graphIdentity) {
        String value = requireComparableText(graphIdentity, "graph field identity");
        if (!value.startsWith("F|")) {
            throw new IllegalArgumentException("invalid graph field identity: " + value);
        }
        String body = value.substring(2);
        if (body.startsWith("T|")) {
            body = body.substring(2);
        }
        int colon = body.lastIndexOf(':');
        if (colon <= 0 || colon == body.length() - 1 || body.indexOf('#') < 1) {
            throw new IllegalArgumentException("invalid graph field identity: " + value);
        }
        return "F|" + body.substring(0, colon + 1)
                + normalizeGraphType(eraseParameterizedArguments(body.substring(colon + 1)));
    }

    static String eraseParameterizedArguments(String value) {
        String checked = requireComparableSequence(value, "type list");
        StringBuilder out = new StringBuilder(checked.length());
        int depth = 0;
        for (int index = 0; index < checked.length(); index++) {
            char current = checked.charAt(index);
            if (current == '<') {
                depth++;
                continue;
            }
            if (current == '>') {
                if (depth == 0) {
                    throw new IllegalArgumentException("unbalanced parameterized type: " + checked);
                }
                depth--;
                continue;
            }
            if (depth == 0 && !Character.isWhitespace(current)) {
                out.append(current);
            }
        }
        if (depth != 0) {
            throw new IllegalArgumentException("unbalanced parameterized type: " + checked);
        }
        return out.toString();
    }

    private static String normalizeGraphTypeSequence(String value) {
        if (value.isEmpty()) {
            return value;
        }
        StringJoiner normalized = new StringJoiner(",");
        for (String type : value.split(",", -1)) {
            normalized.add(normalizeGraphType(type));
        }
        return normalized.toString();
    }

    private static String normalizeGraphType(String value) {
        String checked = requireComparableText(value, "graph type");
        int arrayDimensions = 0;
        while (checked.endsWith("[]")) {
            checked = checked.substring(0, checked.length() - 2);
            arrayDimensions++;
        }
        String base = "String".equals(checked) ? "java.lang.String" : checked;
        return base + "[]".repeat(arrayDimensions);
    }

    private static String requireComparableSequence(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        String checked = value.strip();
        if (checked.indexOf(' ') >= 0
                || checked.contains("<unresolved")
                || checked.contains("<recursive>")) {
            throw new IllegalArgumentException("non-comparable " + name + ": " + value);
        }
        return checked;
    }

    private static String requireComparableText(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        String checked = value.strip();
        if (checked.isEmpty()
                || checked.indexOf(' ') >= 0
                || checked.contains("<unresolved")
                || checked.contains("<recursive>")) {
            throw new IllegalArgumentException("non-comparable " + name + ": " + value);
        }
        return checked;
    }
}
