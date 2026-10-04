// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import com.synexia.rewrite.sealed.SealHash;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.tree.ParseError;

/**
 * Inventories bounded Java declarations while retaining every supplied source as visible evidence.
 * Pattern: Visitor; role: read-only declaration observer. Existing typed class/method rows remain
 * compatible; unresolved declarations receive lexical names and blocking residue. Fields,
 * initializer blocks, lambdas, anonymous classes and enum values are emitted as individually
 * addressable read-only atoms. No AST node is replaced and no source is reparsed. This does not
 * prove complete AST mapping,
 * symbol/effect/dependency resolution, behavior or canonical mutation admission.
 */
public final class M3RecipeFirstJavaAtomInventoryRecipe extends Recipe {
    /** Host may adapt its existing ProgressMonitor with monitor::isCanceled. */
    public static final String CANCELLATION_KEY = "com.synexia.m3.sourceVisibility.canceled";
    /** Host may adapt its existing ProgressMonitor with monitor::worked. */
    public static final String PROGRESS_KEY = "com.synexia.m3.sourceVisibility.worked";

    private final transient M3RecipeFirstJavaAtomInventoryTable table =
            new M3RecipeFirstJavaAtomInventoryTable(this);
    private final transient M3RecipeFirstSourceCoverageTable coverage =
            new M3RecipeFirstSourceCoverageTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory Java atoms for M3 recipe-first transformation";
    }

    @Override
    public String getDescription() {
        return "Emit class, method, field, initializer, lambda, anonymous-class and enum-value "
                + "atoms with contract-surface classification before repository-wide transformation, "
                + "retaining unsupported and unresolved source residue.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("synexia", "m3", "m3scale", "recipe-first", "atom", "inventory", "read-only");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                BooleanSupplier canceled = ctx.getMessage(CANCELLATION_KEY);
                LongConsumer progress = ctx.getMessage(PROGRESS_KEY);
                M3RecipeFirstSourceCoverageTable.checkCanceled(canceled);
                String path = source.getSourcePath().normalize().toString().replace('\\', '/');
                String hash = SealHash.text(source.printAll());
                M3SourceKind kind = M3SourceKind.classify(path);
                var atoms = new ArrayList<M3RecipeFirstJavaAtomInventoryTable.Row>();
                int[] counts = new int[5];
                M3RecipeFirstSourceCoverageTable.Disposition disposition;
                if (source instanceof ParseError
                        || source.getMarkers().findFirst(ParseExceptionResult.class).isPresent()) {
                    disposition = M3RecipeFirstSourceCoverageTable.Disposition.PARSE_ERROR;
                } else if (source instanceof J.CompilationUnit unit && kind == M3SourceKind.JAVA) {
                    new JavaIsoVisitor<ExecutionContext>() {
                        @Override
                        public J preVisit(J java, ExecutionContext context) {
                            M3RecipeFirstSourceCoverageTable.checkCanceled(canceled);
                            if (java.getMarkers().findFirst(ParseExceptionResult.class).isPresent()) counts[4]++;
                            if (progress != null) progress.accept(1L);
                            return java;
                        }

                        @Override
                        public J.ClassDeclaration visitClassDeclaration(
                                J.ClassDeclaration declaration, ExecutionContext context) {
                            counts[0]++;
                            JavaType.FullyQualified type = TypeUtils.asFullyQualified(declaration.getType());
                            boolean unresolved = type == null;
                            if (unresolved) counts[2]++;
                            atoms.add(new M3RecipeFirstJavaAtomInventoryTable.Row(path,
                                    unresolved ? lexicalOwner() : type.getFullyQualifiedName(),
                                    "CLASS", declaration.getSimpleName(), contractSurface(declaration), false,
                                    unresolved ? "HOLD_UNRESOLVED_DECLARATION" : "CLASSIFY_THEN_RECIPE",
                                    "READ_ONLY_EVIDENCE"));
                            return super.visitClassDeclaration(declaration, context);
                        }

                        @Override
                        public J.MethodDeclaration visitMethodDeclaration(
                                J.MethodDeclaration method, ExecutionContext context) {
                            counts[1]++;
                            Object owner = getCursor().getPathAsStream().skip(1)
                                    .filter(value -> value instanceof J.ClassDeclaration
                                            || value instanceof J.NewClass creation && creation.getBody() != null)
                                    .findFirst().orElse(null);
                            JavaType.FullyQualified type = owner instanceof J.ClassDeclaration declaration
                                    ? TypeUtils.asFullyQualified(declaration.getType()) : null;
                            JavaType.Method methodType = method.getMethodType();
                            boolean unresolved = type == null || !TypeUtils.isWellFormedType(methodType)
                                    || !type.getFullyQualifiedName().equals(methodType.getDeclaringType().getFullyQualifiedName())
                                    || methodType.getThrownExceptions().stream().anyMatch(thrown -> !TypeUtils.isWellFormedType(thrown));
                            if (unresolved) counts[2]++;
                            boolean nativeMethod = method.hasModifier(J.Modifier.Type.Native);
                            atoms.add(new M3RecipeFirstJavaAtomInventoryTable.Row(path,
                                    type == null ? lexicalOwner() : type.getFullyQualifiedName(),
                                    "METHOD", method.getSimpleName(), contractSurface(method), nativeMethod,
                                    unresolved ? "HOLD_UNRESOLVED_DECLARATION"
                                            : nativeMethod ? "JNI_INVENTORY_THEN_RECIPE"
                                            : "CATALOGUE_COMPARE_THEN_RECIPE", "READ_ONLY_EVIDENCE"));
                            return super.visitMethodDeclaration(method, context);
                        }

                        @Override
                        public J.VariableDeclarations visitVariableDeclarations(
                                J.VariableDeclarations declarations, ExecutionContext context) {
                            var parent = getCursor().getParentTreeCursor();
                            boolean field = parent.getValue() instanceof J.ClassDeclaration
                                    || parent.getValue() instanceof J.Block
                                        && (parent.getParentTreeCursor().getValue() instanceof J.ClassDeclaration
                                            || parent.getParentTreeCursor().getValue() instanceof J.NewClass);
                            if (field) {
                                for (J.VariableDeclarations.NamedVariable variable : declarations.getVariables()) {
                                    atoms.add(new M3RecipeFirstJavaAtomInventoryTable.Row(
                                            path, ownerType(), "FIELD", variable.getSimpleName(),
                                            contractSurface(declarations), false,
                                            "CATALOGUE_COMPARE_THEN_RECIPE", "READ_ONLY_EVIDENCE"));
                                }
                            }
                            return super.visitVariableDeclarations(declarations, context);
                        }

                        @Override
                        public J.Block visitBlock(J.Block block, ExecutionContext context) {
                            var parent = getCursor().getParentTreeCursor();
                            if (parent.getValue() instanceof J.Block
                                    && (parent.getParentTreeCursor().getValue() instanceof J.ClassDeclaration
                                        || parent.getParentTreeCursor().getValue() instanceof J.NewClass)) {
                                atoms.add(new M3RecipeFirstJavaAtomInventoryTable.Row(
                                        path, ownerType(), "INITIALIZER", "<initializer>",
                                        "INTERNAL_OR_PACKAGE", false,
                                        "EFFECT_INVENTORY_THEN_RECIPE", "READ_ONLY_EVIDENCE"));
                            }
                            return super.visitBlock(block, context);
                        }

                        @Override
                        public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext context) {
                            atoms.add(new M3RecipeFirstJavaAtomInventoryTable.Row(
                                    path, ownerType(), "LAMBDA", "<lambda>",
                                    "INTERNAL_OR_PACKAGE", false,
                                    "EFFECT_INVENTORY_THEN_RECIPE", "READ_ONLY_EVIDENCE"));
                            return super.visitLambda(lambda, context);
                        }

                        @Override
                        public J.NewClass visitNewClass(J.NewClass creation, ExecutionContext context) {
                            if (creation.getBody() != null) {
                                atoms.add(new M3RecipeFirstJavaAtomInventoryTable.Row(
                                        path, ownerType(), "ANONYMOUS_CLASS", "<anonymous>",
                                        "INTERNAL_OR_PACKAGE", false,
                                        "CATALOGUE_COMPARE_THEN_RECIPE", "READ_ONLY_EVIDENCE"));
                            }
                            return super.visitNewClass(creation, context);
                        }

                        @Override
                        public J.EnumValue visitEnumValue(J.EnumValue value, ExecutionContext context) {
                            atoms.add(new M3RecipeFirstJavaAtomInventoryTable.Row(
                                    path, ownerType(), "ENUM_VALUE", value.getName().getSimpleName(),
                                    "PUBLIC_API", false,
                                    "CONTRACT_INVENTORY_THEN_RECIPE", "READ_ONLY_EVIDENCE"));
                            return super.visitEnumValue(value, context);
                        }

                        private String ownerType() {
                            Object owner = getCursor().getPathAsStream().skip(1)
                                    .filter(value -> value instanceof J.ClassDeclaration
                                            || value instanceof J.NewClass creation && creation.getBody() != null)
                                    .findFirst().orElse(null);
                            if (owner instanceof J.ClassDeclaration declaration) {
                                JavaType.FullyQualified type = TypeUtils.asFullyQualified(declaration.getType());
                                if (type != null) return type.getFullyQualifiedName();
                            }
                            return lexicalOwner();
                        }

                        private String lexicalOwner() {
                            List<String> names = new ArrayList<>();
                            getCursor().getPathAsStream().forEach(value -> {
                                if (value instanceof J.ClassDeclaration declaration) names.add(declaration.getSimpleName());
                                else if (value instanceof J.NewClass creation && creation.getBody() != null) names.add("<anonymous>");
                            });
                            Collections.reverse(names);
                            return "<unresolved>:" + String.join("/", names);
                        }
                    }.visit(unit, ctx);
                    if (counts[4] != 0) {
                        atoms.replaceAll(atom -> new M3RecipeFirstJavaAtomInventoryTable.Row(
                                atom.sourcePath(), atom.ownerType(), atom.atomKind(), atom.atomName(),
                                atom.contractSurface(), atom.nativeAtom(), "HOLD_PARSE_ERROR", atom.authority()));
                    }
                    disposition = counts[4] != 0
                            ? M3RecipeFirstSourceCoverageTable.Disposition.PARSE_ERROR
                            : counts[2] != 0
                            ? M3RecipeFirstSourceCoverageTable.Disposition.UNRESOLVED_DECLARATION_ATTRIBUTION
                            : counts[3] != 0
                                    ? M3RecipeFirstSourceCoverageTable.Disposition.PARTIAL_JAVA_ATOM_MAPPING
                                    : M3RecipeFirstSourceCoverageTable.Disposition.JAVA_DECLARATIONS_MAPPED;
                } else {
                    disposition = kind == M3SourceKind.JAVA
                            ? M3RecipeFirstSourceCoverageTable.Disposition.JAVA_NOT_PARSED
                            : M3RecipeFirstSourceCoverageTable.Disposition.UNSUPPORTED_SOURCE;
                }
                M3RecipeFirstSourceCoverageTable.checkCanceled(canceled);
                var row = new M3RecipeFirstSourceCoverageTable.Row(path, hash, kind.name(),
                        source.getClass().getName(), counts[0], counts[1], counts[2], counts[3], disposition);
                // Buffer a file until its read-only scan succeeds. A partial/canceled scan is not success.
                atoms.forEach(atom -> table.insertRow(ctx, atom));
                coverage.insertRow(ctx, row);
                return source;
            }
        };
    }

    static String contractSurface(J.ClassDeclaration declaration) {
        if (declaration.hasModifier(J.Modifier.Type.Public)) return "PUBLIC_API";
        if (declaration.hasModifier(J.Modifier.Type.Protected)) return "PROTECTED_API";
        return "INTERNAL_OR_PACKAGE";
    }

    static String contractSurface(J.MethodDeclaration method) {
        if (method.hasModifier(J.Modifier.Type.Native)) return "JNI_CONTRACT";
        if (method.hasModifier(J.Modifier.Type.Public)) return "PUBLIC_API";
        if (method.hasModifier(J.Modifier.Type.Protected)) return "PROTECTED_API";
        return "INTERNAL_OR_PACKAGE";
    }

    static String contractSurface(J.VariableDeclarations declarations) {
        if (declarations.hasModifier(J.Modifier.Type.Public)) return "PUBLIC_API";
        if (declarations.hasModifier(J.Modifier.Type.Protected)) return "PROTECTED_API";
        return "INTERNAL_OR_PACKAGE";
    }

    static Set<String> supportedAtomKinds() {
        return Set.of("CLASS", "METHOD", "FIELD", "INITIALIZER", "LAMBDA", "ANONYMOUS_CLASS", "ENUM_VALUE");
    }
}
