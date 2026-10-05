// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

/** Fail-closed OpenRewrite attribution policy shared by semantic inventory recipes. */
final class M3SemanticAttribution {
    private M3SemanticAttribution() {}

    static JavaType.FullyQualified fullyQualified(JavaType type) {
        return TypeUtils.asFullyQualified(type);
    }

    static JavaType.Method resolvedMethod(JavaType.Method method) {
        return isResolvedMethod(method) ? method : null;
    }

    static JavaType.Method overriddenMethod(JavaType.Method method) {
        JavaType.Method resolved = resolvedMethod(method);
        if (resolved == null) {
            return null;
        }
        return TypeUtils.findOverriddenMethod(resolved)
                .map(M3SemanticAttribution::resolvedMethod)
                .orElse(null);
    }

    static boolean isResolvedMethod(JavaType.Method method) {
        if (method == null
                || fullyQualified(method.getDeclaringType()) == null
                || !isResolvedType(method.getDeclaringType())) {
            return false;
        }
        if (!method.isConstructor() && !isResolvedType(method.getReturnType())) {
            return false;
        }
        return method.getParameterTypes().stream()
                .allMatch(M3SemanticAttribution::isResolvedType);
    }

    static boolean isResolvedType(JavaType type) {
        if (type == null || type instanceof JavaType.Unknown) {
            return false;
        }
        if (type instanceof JavaType.Primitive primitive) {
            return primitive != JavaType.Primitive.None && primitive != JavaType.Primitive.Null;
        }
        // Class evidence is nominal identity, not proof that every member is attributed.
        if (type instanceof JavaType.Class) {
            return true;
        }
        // Reuse the pinned donor's composite-type walk, including generic bounds and cycles.
        // Identity membership avoids recursive structural equals on self-referential types.
        Set<JavaType> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return TypeUtils.isWellFormedType(type, seen)
                && seen.stream().noneMatch(component ->
                        component == JavaType.Primitive.None
                                || component == JavaType.Primitive.Null);
    }

    static String methodKey(JavaType.Method method) {
        JavaType.Method resolved = resolvedMethod(method);
        if (resolved == null) {
            throw new IllegalArgumentException("method attribution required");
        }
        String owner = fullyQualified(resolved.getDeclaringType()).getFullyQualifiedName();
        String parameters = parameterTypes(resolved);
        String returnType = resolved.isConstructor() ? owner : typeName(resolved.getReturnType());
        return owner
                + "#"
                + resolved.getName()
                + "("
                + parameters
                + "):"
                + returnType;
    }

    static String parameterTypes(JavaType.Method method) {
        JavaType.Method resolved = resolvedMethod(method);
        if (resolved == null) {
            throw new IllegalArgumentException("method attribution required");
        }
        return resolved.getParameterTypes().stream()
                .map(M3SemanticAttribution::typeName)
                .collect(Collectors.joining(","));
    }

    static String returnType(JavaType.Method method) {
        JavaType.Method resolved = resolvedMethod(method);
        if (resolved == null) {
            return "<unresolved>";
        }
        if (resolved.isConstructor()) {
            return fullyQualified(resolved.getDeclaringType()).getFullyQualifiedName();
        }
        return typeName(resolved.getReturnType());
    }

    static String typeName(JavaType type) {
        if (!isResolvedType(type)) {
            return "<unresolved>";
        }
        if (type instanceof JavaType.Primitive primitive) {
            return switch (primitive) {
                case Boolean -> "boolean";
                case Byte -> "byte";
                case Char -> "char";
                case Double -> "double";
                case Float -> "float";
                case Int -> "int";
                case Long -> "long";
                case Short -> "short";
                case Void -> "void";
                case String -> "java.lang.String";
                case None, Null -> throw new IllegalStateException("unresolved primitive");
            };
        }
        if (type instanceof JavaType.Array array) {
            return typeName(array.getElemType()) + "[]";
        }
        JavaType.FullyQualified fullyQualified = fullyQualified(type);
        if (fullyQualified != null) {
            return fullyQualified.getFullyQualifiedName();
        }
        String rendered = type.toString();
        return rendered == null || rendered.isBlank() ? "<unresolved>" : rendered;
    }
}
