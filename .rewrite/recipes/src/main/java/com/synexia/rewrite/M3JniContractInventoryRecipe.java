// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

/**
 * Inventories Java native declarations before any MIndex/M3 JNI ownership migration.
 *
 * <p>The recipe never renames Java native declarations or C/C++ symbols. It derives the JVM
 * descriptor plus both JNI short and long symbol candidates from attributed Java types. Native
 * source correlation remains a companion repository gate because the Java LST is not a C/C++
 * semantic model.</p>
 */
public final class M3JniContractInventoryRecipe
        extends ScanningRecipe<M3JniContractInventoryRecipe.State> {

    private final transient M3JniContractInventoryTable table =
            new M3JniContractInventoryTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory JNI contracts for M3 migration";
    }

    @Override
    public String getDescription() {
        return "Inventory Java native declarations, JVM descriptors, candidate M3 ownership and "
                + "JNI short/long linkage names without changing Java or native source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia", "m3", "m3lang", "mindex", "jni", "native", "inventory", "read-only");
    }

    @Override
    public State getInitialValue(ExecutionContext ctx) {
        return new State();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext ctx) {
                if (!method.hasModifier(J.Modifier.Type.Native)) {
                    return super.visitMethodDeclaration(method, ctx);
                }
                J.ClassDeclaration owner =
                        getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                JavaType.FullyQualified ownerType =
                        TypeUtils.asFullyQualified(owner.getType());
                if (ownerType == null) {
                    throw new IllegalStateException(
                            "native method requires declaring-type attribution: "
                                    + method.getSimpleName());
                }
                J.CompilationUnit unit =
                        getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                String declaringType = ownerType.getFullyQualifiedName();
                String overloadKey = declaringType + "#" + method.getSimpleName();
                String methodDescriptor = descriptor(method);
                String shortSymbol =
                        jniShortSymbol(declaringType, method.getSimpleName());
                state.nativeNameCounts.merge(overloadKey, 1, Integer::sum);
                state.pending.add(
                        new Pending(
                                unit.getSourcePath().normalize().toString(),
                                declaringType,
                                candidateM3Type(declaringType),
                                method.getSimpleName(),
                                methodDescriptor,
                                method.printTrimmed(),
                                method.hasModifier(J.Modifier.Type.Static),
                                overloadKey,
                                shortSymbol,
                                jniLongSymbol(shortSymbol, methodDescriptor)));
                return super.visitMethodDeclaration(method, ctx);
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(State state, ExecutionContext ctx) {
        synchronized (state) {
            if (state.exported) {
                return List.of();
            }
            state.pending.stream()
                    .sorted(
                            Comparator.comparing(Pending::declaringType)
                                    .thenComparing(Pending::methodName)
                                    .thenComparing(Pending::descriptor))
                    .forEach(
                            pending -> {
                                int overloads =
                                        state.nativeNameCounts.getOrDefault(
                                                pending.overloadKey(), 1);
                                boolean overloaded = overloads > 1;
                                table.insertRow(
                                        ctx,
                                        new M3JniContractInventoryTable.Row(
                                                pending.sourcePath(),
                                                pending.declaringType(),
                                                pending.candidateM3Type(),
                                                pending.methodName(),
                                                pending.descriptor(),
                                                pending.javaDeclaration(),
                                                pending.staticMethod(),
                                                overloads,
                                                pending.jniShortSymbol(),
                                                pending.jniLongSymbol(),
                                                overloaded
                                                        ? pending.jniLongSymbol()
                                                        : pending.jniShortSymbol(),
                                                pending.methodName() + pending.descriptor(),
                                                overloaded
                                                        ? "LONG_NAME_OR_REGISTER_NATIVES_PROOF_REQUIRED"
                                                        : "SHORT_NAME_OR_REGISTER_NATIVES_PROOF_REQUIRED",
                                                "READ_ONLY_EVIDENCE"));
                            });
            state.exported = true;
            return List.of();
        }
    }

    static String descriptor(J.MethodDeclaration method) {
        JavaType.Method methodType = method.getMethodType();
        if (methodType == null) {
            throw new IllegalStateException(
                    "native method requires method-type attribution: "
                            + method.getSimpleName());
        }
        if (method.getReturnTypeExpression() == null
                || method.getReturnTypeExpression().getType() == null) {
            throw new IllegalStateException(
                    "native method requires return-type attribution: "
                            + method.getSimpleName());
        }
        StringBuilder out = new StringBuilder("(");
        for (JavaType parameter : methodType.getParameterTypes()) {
            out.append(descriptor(parameter));
        }
        return out.append(')')
                .append(descriptor(method.getReturnTypeExpression().getType()))
                .toString();
    }

    static String descriptor(JavaType type) {
        if (type instanceof JavaType.Primitive primitive) {
            return switch (primitive) {
                case Boolean -> "Z";
                case Byte -> "B";
                case Char -> "C";
                case Double -> "D";
                case Float -> "F";
                case Int -> "I";
                case Long -> "J";
                case Short -> "S";
                case Void -> "V";
                case String -> "Ljava/lang/String;";
                case None, Null ->
                        throw new IllegalArgumentException(
                                "unsupported JNI primitive descriptor: " + primitive);
            };
        }
        if (type instanceof JavaType.Array array) {
            return "[" + descriptor(array.getElemType());
        }
        if (type instanceof JavaType.GenericTypeVariable variable) {
            List<JavaType> bounds = variable.getBounds();
            return bounds.isEmpty()
                    ? "Ljava/lang/Object;"
                    : descriptor(bounds.getFirst());
        }
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        if (fullyQualified != null) {
            return "L" + fullyQualified.getFullyQualifiedName().replace('.', '/') + ";";
        }
        throw new IllegalArgumentException(
                "unsupported JNI attributed type: " + type.getClass().getName());
    }

    static String jniShortSymbol(String declaringType, String methodName) {
        String internalName = declaringType.replace('.', '/');
        return "Java_" + escapeJni(internalName) + "_" + escapeJni(methodName);
    }

    static String jniLongSymbol(String shortSymbol, String descriptor) {
        int close = descriptor.indexOf(')');
        if (!descriptor.startsWith("(") || close < 1) {
            throw new IllegalArgumentException("invalid JVM method descriptor: " + descriptor);
        }
        return shortSymbol + "__" + escapeJni(descriptor.substring(1, close));
    }

    static String escapeJni(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if ((ch >= 'A' && ch <= 'Z')
                    || (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9')) {
                out.append(ch);
            } else if (ch == '/') {
                out.append('_');
            } else if (ch == '_') {
                out.append("_1");
            } else if (ch == ';') {
                out.append("_2");
            } else if (ch == '[') {
                out.append("_3");
            } else {
                out.append("_0");
                String hex = Integer.toHexString(ch);
                out.append("0".repeat(4 - hex.length())).append(hex);
            }
        }
        return out.toString();
    }

    private static String candidateM3Type(String declaringType) {
        int packageSeparator = declaringType.lastIndexOf('.');
        String classPart =
                packageSeparator < 0
                        ? declaringType
                        : declaringType.substring(packageSeparator + 1);
        for (String segment : classPart.split("\\$")) {
            if (M3MIndexNamePolicy.isMIndexSimpleName(segment)) {
                return M3MIndexNamePolicy.canonicalFullyQualifiedName(declaringType);
            }
        }
        return "";
    }

    record Pending(
            String sourcePath,
            String declaringType,
            String candidateM3Type,
            String methodName,
            String descriptor,
            String javaDeclaration,
            boolean staticMethod,
            String overloadKey,
            String jniShortSymbol,
            String jniLongSymbol) {}

    /** One source-set accumulator. No native or Java source is mutated. */
    public static final class State {
        private final Map<String, Integer> nativeNameCounts = new ConcurrentHashMap<>();
        private final Collection<Pending> pending = new ConcurrentLinkedQueue<>();
        private boolean exported;
    }
}
