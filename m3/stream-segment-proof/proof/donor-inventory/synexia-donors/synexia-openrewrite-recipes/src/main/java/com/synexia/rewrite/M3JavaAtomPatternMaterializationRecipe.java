// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

/**
 * Materializes deterministic semantic atom identities and structural pattern labels in Java source.
 *
 * <p>This is deliberately not a blind "split every method" transform. The mature M3 atom law is
 * that every behavioral leaf has an explicit semantic identity and admitted composition grammar.
 * Existing type/method declaration boundaries are materialized as stable atoms, and executable
 * statement/control leaves receive deterministic compiler-inert leaf identities. Physical helper
 * extraction remains a separate source-sealed transform and is never inferred from line count.</p>
 *
 * <p>The only source delta produced here is compiler-inert block-comment metadata immediately
 * preceding Java types, methods and admitted statement/control leaves. No signature, annotation,
 * import, expression semantics, control-flow, exception, state, resource or bytecode surface is
 * changed.</p>
 */
public final class M3JavaAtomPatternMaterializationRecipe extends Recipe {
    static final String MARKER = "M3-ATOM";
    static final String LEAF_MARKER = "M3-LEAF";

    private final String sourceFilePattern;

    public M3JavaAtomPatternMaterializationRecipe() {
        this("**/*.java");
    }

    @JsonCreator
    public M3JavaAtomPatternMaterializationRecipe(
            @JsonProperty("sourceFilePattern") String sourceFilePattern) {
        this.sourceFilePattern = checked(sourceFilePattern);
    }

    @Override
    public String getDisplayName() {
        return "Materialize M3 Java semantic atoms and structural patterns";
    }

    @Override
    public String getDescription() {
        return "Adds deterministic compiler-inert M3 atom identities and structural pattern labels "
                + "to Java type/method declarations without changing executable behavior.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "openrewrite",
                "java",
                "atom",
                "pattern",
                "materialization",
                "contract-preserving",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> visitor =
                new JavaIsoVisitor<>() {
                    private String sourcePath = "";
                    private final java.util.Map<String, Integer> statementOccurrences =
                            new java.util.HashMap<>();

                    @Override
                    public J.CompilationUnit visitCompilationUnit(
                            J.CompilationUnit unit,
                            ExecutionContext context) {
                        sourcePath =
                                unit.getSourcePath()
                                        .normalize()
                                        .toString()
                                        .replace('\\', '/');
                        statementOccurrences.clear();
                        return super.visitCompilationUnit(unit, context);
                    }

                    @Override
                    public J.ClassDeclaration visitClassDeclaration(
                            J.ClassDeclaration declaration,
                            ExecutionContext context) {
                        J.ClassDeclaration visited =
                                super.visitClassDeclaration(declaration, context);
                        String owner = lexicalClassOwner(visited.getSimpleName());
                        String identity =
                                atomId(
                                        "TYPE",
                                        sourcePath,
                                        owner,
                                        typeIdentity(visited));
                        String marker =
                                marker(
                                        "TYPE",
                                        identity,
                                        M3RecipeFirstJavaAtomInventoryRecipe.contractSurface(visited),
                                        typePatterns(visited));
                        return withMarker(visited, marker);
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(
                            J.MethodDeclaration method,
                            ExecutionContext context) {
                        J.MethodDeclaration visited =
                                super.visitMethodDeclaration(method, context);
                        J.ClassDeclaration owner =
                                getCursor().firstEnclosing(J.ClassDeclaration.class);
                        String ownerName =
                                owner == null
                                        ? "<top-level>"
                                        : lexicalClassOwner(owner.getSimpleName());
                        String identity =
                                atomId(
                                        "METHOD",
                                        sourcePath,
                                        ownerName,
                                        methodIdentity(visited));
                        String marker =
                                marker(
                                        "METHOD",
                                        identity,
                                        M3RecipeFirstJavaAtomInventoryRecipe.contractSurface(visited),
                                        methodPatterns(visited));
                        return withMarker(visited, marker);
                    }

                    @Override
                    public Statement visitStatement(
                            Statement statement,
                            ExecutionContext context) {
                        Statement visited = super.visitStatement(statement, context);
                        if (visited instanceof J.ClassDeclaration
                                || visited instanceof J.MethodDeclaration
                                || visited instanceof J.Block) {
                            return visited;
                        }

                        Object parent = getCursor().getParentTreeCursor().getValue();
                        if (parent instanceof J.MethodDeclaration
                                || parent instanceof J.ForLoop.Control
                                || parent instanceof J.ForEachLoop.Control
                                || parent instanceof J.Try.Resource
                                || parent instanceof J.Lambda) {
                            return visited;
                        }

                        J.MethodDeclaration method =
                                getCursor().firstEnclosing(J.MethodDeclaration.class);
                        J.ClassDeclaration owner =
                                getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (method == null
                                && !(visited instanceof J.VariableDeclarations)) {
                            return visited;
                        }

                        String ownerName =
                                owner == null
                                        ? "<top-level>"
                                        : lexicalClassOwner(owner.getSimpleName());
                        String methodName =
                                method == null ? "<type-body>" : methodIdentity(method);
                        String canonical = canonicalStatementText(visited);
                        String occurrenceKey =
                                String.join(
                                        "\u001f",
                                        sourcePath,
                                        ownerName,
                                        methodName,
                                        visited.getClass().getName(),
                                        canonical);
                        int occurrence =
                                statementOccurrences.merge(
                                        occurrenceKey,
                                        1,
                                        Integer::sum);
                        String identity =
                                atomId(
                                        "LEAF",
                                        sourcePath,
                                        ownerName,
                                        methodName,
                                        statementKind(visited),
                                        Integer.toString(occurrence),
                                        canonical);
                        String leaf =
                                leafMarker(
                                        statementKind(visited),
                                        identity,
                                        statementPatterns(visited));
                        return withLeafMarker(visited, leaf);
                    }

                    private String lexicalClassOwner(String current) {
                        List<String> names = new ArrayList<>();
                        getCursor()
                                .getPathAsStream()
                                .filter(J.ClassDeclaration.class::isInstance)
                                .map(J.ClassDeclaration.class::cast)
                                .map(J.ClassDeclaration::getSimpleName)
                                .forEach(names::add);
                        java.util.Collections.reverse(names);
                        if (names.isEmpty() || !names.get(names.size() - 1).equals(current)) {
                            names.add(current);
                        }
                        return String.join("$", names);
                    }
                };

        return Preconditions.check(
                new FindSourceFiles(sourceFilePattern).getVisitor(),
                visitor);
    }

    public String getSourceFilePattern() {
        return sourceFilePattern;
    }

    public boolean executableBehaviorMutationAuthority() {
        return false;
    }

    public boolean sourceMetadataMutationAuthority() {
        return true;
    }

    public boolean sourceCopyAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    private static Statement withLeafMarker(
            Statement statement,
            String marker) {
        return statement.withComments(
                updatedLeafComments(
                        statement.getComments(),
                        marker,
                        statement.getPrefix().getWhitespace()));
    }

    private static List<Comment> updatedLeafComments(
            List<Comment> comments,
            String marker,
            String prefixWhitespace) {
        ArrayList<Comment> result = new ArrayList<>(comments.size() + 1);
        boolean found = false;
        boolean changed = false;
        String expected = " " + marker + " ";
        for (Comment comment : comments) {
            if (comment instanceof TextComment text
                    && text.getText().contains(LEAF_MARKER)) {
                if (!found) {
                    Comment replacement =
                            text.getText().equals(expected)
                                    ? text
                                    : text.withText(expected);
                    result.add(replacement);
                    changed |= replacement != text;
                    found = true;
                } else {
                    changed = true;
                }
            } else {
                result.add(comment);
            }
        }
        if (!found) {
            String suffix = "\n" + indentation(prefixWhitespace);
            result.add(new TextComment(true, expected, suffix, Markers.EMPTY));
            changed = true;
        }
        return changed ? List.copyOf(result) : comments;
    }

    private static String leafMarker(
            String kind,
            String id,
            Set<String> patterns) {
        return LEAF_MARKER
                + " kind="
                + kind
                + " id="
                + id
                + " contract=INTERNAL_OR_PACKAGE patterns="
                + String.join(",", patterns);
    }

    private static String canonicalStatementText(Statement statement) {
        return statement
                .printTrimmed()
                .replaceAll(
                        "(?s)/\\*\\s*M3-(?:ATOM|LEAF)\\b.*?\\*/\\s*",
                        "")
                .strip();
    }

    private static String statementKind(Statement statement) {
        if (statement instanceof J.VariableDeclarations) return "LOCAL_DECLARATION";
        if (statement instanceof J.ForLoop
                || statement instanceof J.ForEachLoop
                || statement instanceof J.WhileLoop
                || statement instanceof J.DoWhileLoop) return "ITERATION";
        if (statement instanceof J.If) return "BRANCH";
        if (statement instanceof J.Switch) return "SWITCH";
        if (statement instanceof J.Try) return "TRY";
        if (statement instanceof J.Synchronized) return "SYNCHRONIZED";
        if (statement instanceof J.Return) return "RETURN";
        if (statement instanceof J.Throw) return "THROW";
        if (statement instanceof J.Break) return "BREAK";
        if (statement instanceof J.Continue) return "CONTINUE";
        if (statement instanceof J.Assignment
                || statement instanceof J.AssignmentOperation) return "MUTATION";
        if (statement instanceof J.MethodInvocation) return "INVOCATION";
        if (statement instanceof J.NewClass) return "CONSTRUCTION";
        if (statement instanceof J.Assert) return "ASSERTION";
        if (statement instanceof J.Case) return "CASE";
        return statement.getClass().getSimpleName().toUpperCase(java.util.Locale.ROOT);
    }

    private static Set<String> statementPatterns(Statement statement) {
        TreeSet<String> patterns = new TreeSet<>();
        if (statement instanceof J.ForLoop
                || statement instanceof J.ForEachLoop
                || statement instanceof J.WhileLoop
                || statement instanceof J.DoWhileLoop) {
            patterns.add("ITERATION");
        }
        if (statement instanceof J.If
                || statement instanceof J.Switch
                || statement instanceof J.Case) {
            patterns.add("BRANCHING");
        }
        if (statement instanceof J.Try || statement instanceof J.Throw) {
            patterns.add("EXCEPTION_BOUNDARY");
        }
        if (statement instanceof J.Synchronized) {
            patterns.add("SYNCHRONIZATION");
        }
        if (statement instanceof J.Return) {
            patterns.add("RETURN");
        }
        if (statement instanceof J.Break || statement instanceof J.Continue) {
            patterns.add("CONTROL_TRANSFER");
        }
        if (statement instanceof J.VariableDeclarations) {
            patterns.add("LOCAL_DECLARATION");
        }
        if (statement instanceof J.Assignment
                || statement instanceof J.AssignmentOperation) {
            patterns.add("MUTATION");
        }
        if (statement instanceof J.MethodInvocation) {
            patterns.add("INVOCATION");
        }
        if (statement instanceof J.NewClass) {
            patterns.add("CONSTRUCTION");
        }
        if (statement instanceof J.Assert) {
            patterns.add("ASSERTION");
        }
        if (patterns.isEmpty()) {
            patterns.add("STATEMENT");
        }
        return java.util.Collections.unmodifiableSet(
                new LinkedHashSet<>(patterns));
    }

    private static J.ClassDeclaration withMarker(
            J.ClassDeclaration declaration,
            String marker) {
        return declaration.withComments(
                updatedComments(
                        declaration.getComments(),
                        marker,
                        declaration.getPrefix().getWhitespace()));
    }

    private static J.MethodDeclaration withMarker(
            J.MethodDeclaration method,
            String marker) {
        return method.withComments(
                updatedComments(
                        method.getComments(),
                        marker,
                        method.getPrefix().getWhitespace()));
    }

    private static List<Comment> updatedComments(
            List<Comment> comments,
            String marker,
            String prefixWhitespace) {
        ArrayList<Comment> result = new ArrayList<>(comments.size() + 1);
        boolean found = false;
        boolean changed = false;
        String expected = " " + marker + " ";
        for (Comment comment : comments) {
            if (comment instanceof TextComment text
                    && text.getText().contains(MARKER)) {
                if (!found) {
                    Comment replacement = text.getText().equals(expected)
                            ? text
                            : text.withText(expected);
                    result.add(replacement);
                    changed |= replacement != text;
                    found = true;
                } else {
                    changed = true;
                }
            } else {
                result.add(comment);
            }
        }
        if (!found) {
            String suffix = "\n" + indentation(prefixWhitespace);
            result.add(new TextComment(true, expected, suffix, Markers.EMPTY));
            changed = true;
        }
        return changed ? List.copyOf(result) : comments;
    }

    private static String marker(
            String kind,
            String id,
            String contract,
            Set<String> patterns) {
        return MARKER
                + " kind="
                + kind
                + " id="
                + id
                + " contract="
                + contract
                + " patterns="
                + String.join(",", patterns);
    }

    private static String typeIdentity(J.ClassDeclaration declaration) {
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(declaration.getType());
        if (type != null && !type.getFullyQualifiedName().isBlank()) {
            return type.getFullyQualifiedName();
        }
        return declaration.getSimpleName() + ":" + declaration.getKind().name();
    }

    private static String methodIdentity(J.MethodDeclaration method) {
        JavaType.Method type = method.getMethodType();
        if (type != null && TypeUtils.isWellFormedType(type)) {
            return type.toString();
        }
        return method.getSimpleName()
                + "/"
                + method.getParameters().size()
                + ":"
                + (method.getReturnTypeExpression() == null
                        ? "<constructor>"
                        : method.getReturnTypeExpression().toString());
    }

    private static Set<String> typePatterns(J.ClassDeclaration declaration) {
        TreeSet<String> patterns = new TreeSet<>();
        if (declaration.getExtends() != null) {
            patterns.add("INHERITANCE");
        }
        if (declaration.getImplements() != null
                && !declaration.getImplements().isEmpty()) {
            patterns.add("INTERFACE_IMPLEMENTATION");
        }
        if (!declaration.getLeadingAnnotations().isEmpty()) {
            patterns.add("ANNOTATED_TYPE");
        }
        if (declaration.getKind() == J.ClassDeclaration.Kind.Type.Record) {
            patterns.add("RECORD");
        }
        if (declaration.hasModifier(J.Modifier.Type.Sealed)
                || declaration.getPermits() != null
                        && !declaration.getPermits().isEmpty()) {
            patterns.add("SEALED_HIERARCHY");
        }
        if (patterns.isEmpty()) {
            patterns.add("PLAIN_TYPE");
        }
        return java.util.Collections.unmodifiableSet(new LinkedHashSet<>(patterns));
    }

    private static Set<String> methodPatterns(J.MethodDeclaration method) {
        TreeSet<String> patterns = new TreeSet<>();
        if (method.getBody() != null) {
            new JavaIsoVisitor<Set<String>>() {
                @Override
                public J.ForLoop visitForLoop(J.ForLoop loop, Set<String> found) {
                    found.add("ITERATION");
                    return super.visitForLoop(loop, found);
                }

                @Override
                public J.ForEachLoop visitForEachLoop(
                        J.ForEachLoop loop,
                        Set<String> found) {
                    found.add("ITERATION");
                    return super.visitForEachLoop(loop, found);
                }

                @Override
                public J.WhileLoop visitWhileLoop(
                        J.WhileLoop loop,
                        Set<String> found) {
                    found.add("ITERATION");
                    return super.visitWhileLoop(loop, found);
                }

                @Override
                public J.DoWhileLoop visitDoWhileLoop(
                        J.DoWhileLoop loop,
                        Set<String> found) {
                    found.add("ITERATION");
                    return super.visitDoWhileLoop(loop, found);
                }

                @Override
                public J.If visitIf(J.If condition, Set<String> found) {
                    found.add("BRANCHING");
                    return super.visitIf(condition, found);
                }

                @Override
                public J.Switch visitSwitch(J.Switch switchStatement, Set<String> found) {
                    found.add("BRANCHING");
                    return super.visitSwitch(switchStatement, found);
                }

                @Override
                public J.Try visitTry(J.Try tryStatement, Set<String> found) {
                    found.add("EXCEPTION_BOUNDARY");
                    return super.visitTry(tryStatement, found);
                }

                @Override
                public J.Throw visitThrow(J.Throw thrown, Set<String> found) {
                    found.add("EXCEPTION_BOUNDARY");
                    return super.visitThrow(thrown, found);
                }

                @Override
                public J.Synchronized visitSynchronized(
                        J.Synchronized synchronizedStatement,
                        Set<String> found) {
                    found.add("SYNCHRONIZATION");
                    return super.visitSynchronized(synchronizedStatement, found);
                }

                @Override
                public J.Lambda visitLambda(J.Lambda lambda, Set<String> found) {
                    found.add("LAMBDA");
                    return super.visitLambda(lambda, found);
                }

                @Override
                public J.InstanceOf visitInstanceOf(
                        J.InstanceOf instanceOf,
                        Set<String> found) {
                    found.add("TYPE_TEST");
                    return super.visitInstanceOf(instanceOf, found);
                }

                @Override
                public J.MethodInvocation visitMethodInvocation(
                        J.MethodInvocation invocation,
                        Set<String> found) {
                    String name = invocation.getSimpleName();
                    if ("stream".equals(name)
                            || "parallelStream".equals(name)) {
                        found.add("STREAM_PIPELINE");
                    }
                    JavaType.Method methodType = invocation.getMethodType();
                    if (methodType != null
                            && TypeUtils.isOfClassType(
                                    methodType.getDeclaringType(),
                                    "java.util.Optional")) {
                        found.add("OPTIONAL_FLOW");
                    }
                    if ("builder".equals(name) || "build".equals(name)) {
                        found.add("BUILDER_CANDIDATE");
                    }
                    if (Set.of("of", "create", "newInstance", "getInstance")
                            .contains(name)) {
                        found.add("FACTORY_CANDIDATE");
                    }
                    return super.visitMethodInvocation(invocation, found);
                }
            }.visit(method.getBody(), patterns);
        }
        if (method.hasModifier(J.Modifier.Type.Native)) {
            patterns.add("JNI_BOUNDARY");
        }
        if (patterns.isEmpty()) {
            patterns.add("STRAIGHT_LINE");
        }
        return java.util.Collections.unmodifiableSet(new LinkedHashSet<>(patterns));
    }

    private static String indentation(String whitespace) {
        if (whitespace == null || whitespace.isEmpty()) {
            return "";
        }
        int newline = Math.max(whitespace.lastIndexOf('\n'), whitespace.lastIndexOf('\r'));
        return newline < 0 ? whitespace : whitespace.substring(newline + 1);
    }

    private static String atomId(String... parts) {
        return sha256(String.join("\u001f", parts));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static String checked(String value) {
        String result = Objects.toString(value, "").strip();
        if (result.isEmpty()
                || result.length() > 4096
                || result.indexOf('\0') >= 0
                || result.indexOf('\r') >= 0
                || result.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern");
        }
        return result;
    }
}
