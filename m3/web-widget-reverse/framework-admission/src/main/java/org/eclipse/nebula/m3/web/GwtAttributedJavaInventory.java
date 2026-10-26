// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.m3.web;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * JDK 21 javac-attributed, source-digest-sealed static GWT widget inventory.
 *
 * <p>Unlike text or regex scanners, this recognizes an exact, single-method
 * Java source AST, checks constructor owner symbols and constant arguments,
 * and refuses all unexamined Java statements and expression effects.
 * Only original UiBinder candidate metadata is emitted; no donor code executes
 * and no runtime/ABI/UI equivalence is conferred.</p>
 */
public final class GwtAttributedJavaInventory {
    private static final int MAX_SOURCE_BYTES = 1_048_576;
    private static final int MAX_JARS = 8;
    private static final long MAX_JAR_BYTES = 100L * 1024 * 1024;
    private static final String LABEL = "com.google.gwt.user.client.ui.Label";
    private static final String BUTTON = "com.google.gwt.user.client.ui.Button";

    private GwtAttributedJavaInventory() { }

    /** Pinned donor binary: it cannot inherit the caller's process classpath. */
    public record ClasspathJar(Path path, String sha256) {
        public ClasspathJar {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(sha256, "sha256");
            if (!sha256.matches("[0-9a-f]{64}")) throw refuse("JAR_DIGEST_REQUIRED");
            if (!path.toString().endsWith(".jar")) throw refuse("JAR_BINARY_REQUIRED");
        }
    }

    /** Immutable proof of attributed syntax and subsequent candidate projection. */
    public record Inventory(WebWidgetUiBinderProjection.SourceReceipt source,
                            List<String> classpathDigests,
                            List<WebWidgetUiBinderProjection.Declaration> declarations,
                            WebWidgetUiBinderProjection.Projection candidateXml) {
        public Inventory {
            Objects.requireNonNull(source, "source");
            classpathDigests = List.copyOf(classpathDigests);
            declarations = List.copyOf(declarations);
            Objects.requireNonNull(candidateXml, "candidateXml");
            if (declarations.size() != candidateXml.rowCount()) {
                throw refuse("INVENTORY_ROW_MISMATCH");
            }
        }

        public boolean runtimeQualificationRequired() { return true; }
    }

    /**
     * Parse and type-check only a single, public final Java class whose sole
     * method is public static void build() and contains 1..256 variable
     * declarations constructed by new GWT Label(String) or Button(String).
     * The binary classpath is an explicit, SHA-256 verified list of JARs.
     * Annotation processing, implicit compilation and sourcepath discovery
     * are disabled. Constructor/method bodies are never executed.
     */
    public static Inventory inspect(WebWidgetUiBinderProjection.SourceReceipt receipt,
                                    byte[] sourceUtf8, List<ClasspathJar> jars)
            throws IOException {
        Objects.requireNonNull(receipt, "receipt");
        Objects.requireNonNull(sourceUtf8, "sourceUtf8");
        Objects.requireNonNull(jars, "jars");
        if (sourceUtf8.length == 0 || sourceUtf8.length > MAX_SOURCE_BYTES) {
            throw refuse("SOURCE_BUDGET");
        }
        if (!sha256(sourceUtf8).equals(receipt.sha256())) throw refuse("SOURCE_DIGEST_DRIFT");
        String source;
        try {
            source = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(sourceUtf8)).toString();
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("SOURCE_INVALID_UTF8", invalid);
        }
        if (jars.isEmpty() || jars.size() > MAX_JARS) throw refuse("BINARY_CLASSPATH_REQUIRED");
        var classpath = new ArrayList<Path>();
        for (ClasspathJar jar : jars) {
            Path path = jar.path();
            if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    || Files.size(path) > MAX_JAR_BYTES || Files.size(path) == 0) {
                throw refuse("JAR_FILE_BUDGET_OR_SYMLINK");
            }
            if (!sha256(path).equals(jar.sha256())) throw refuse("JAR_DIGEST_DRIFT");
            Path canonical = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (classpath.contains(canonical)) throw refuse("DUPLICATE_BINARY_JAR");
            classpath.add(canonical);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK21_COMPILER_REQUIRED");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            manager.setLocationFromPaths(StandardLocation.CLASS_PATH, classpath);
            manager.setLocationFromPaths(StandardLocation.SOURCE_PATH, List.of());
            String relativePath = receipt.relativePath();
            var sourceObject = new SimpleJavaFileObject(
                    URI.create("string:///" + relativePath), JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return source;
                }
            };
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "21", "-proc:none", "-implicit:none", "-Xlint:all", "-Werror"),
                    null, List.of(sourceObject));
            var units = new ArrayList<CompilationUnitTree>();
            task.parse().forEach(units::add);
            if (units.size() != 1) throw refuse("SINGLE_SOURCE_UNIT_REQUIRED");
            // javac analyze() injects a default constructor into class ASTs.
            // Snapshot the original, source-written members before attribution
            // or an unexpected handwritten constructor could be overlooked.
            List<? extends Tree> originalMembers = units.getFirst().getTypeDecls().size() == 1
                    && units.getFirst().getTypeDecls().getFirst() instanceof ClassTree clazz
                    ? List.copyOf(clazz.getMembers()) : List.of();
            // The analyzer may replace a source-written `var` type node with
            // its inferred type. Refuse inference before that information is lost.
            for (Tree member : originalMembers) {
                if (member instanceof MethodTree parsedMethod && parsedMethod.getBody() != null) {
                    for (StatementTree parsedStatement : parsedMethod.getBody().getStatements()) {
                        if (parsedStatement instanceof VariableTree parsedVariable
                                && (parsedVariable.getType() == null
                                    || parsedVariable.getType().toString().equals("var"))) {
                            throw refuse("EXPLICIT_SOURCE_TYPE_REQUIRED");
                        }
                    }
                }
            }
            task.analyze();
            if (diagnostics.getDiagnostics().stream().anyMatch(d ->
                    d.getKind() == Diagnostic.Kind.ERROR)) throw refuse("ATTRIBUTION_DIAGNOSTIC");
            List<WebWidgetUiBinderProjection.Declaration> rows = readExactClass(
                    units.getFirst(), Trees.instance(task), relativePath, originalMembers);
            var candidate = WebWidgetUiBinderProjection.project(receipt, sourceUtf8, rows);
            return new Inventory(receipt, jars.stream().map(ClasspathJar::sha256).toList(),
                    rows, candidate);
        } catch (RuntimeException failure) {
            if (failure instanceof IllegalArgumentException) throw failure;
            throw new IllegalArgumentException("JAVA_SOURCE_AST_UNQUALIFIED", failure);
        }
    }

    private static List<WebWidgetUiBinderProjection.Declaration> readExactClass(
            CompilationUnitTree unit, Trees trees, String relativePath,
            List<? extends Tree> originalMembers) {
        if (!unit.getPackageAnnotations().isEmpty() || unit.getTypeDecls().size() != 1) {
            throw refuse("ONE_CLASS_ONLY");
        }
        for (ImportTree imp : unit.getImports()) {
            if (imp.isStatic() || !List.of(LABEL, BUTTON).contains(
                    imp.getQualifiedIdentifier().toString())) throw refuse("UNKNOWN_IMPORT");
        }
        if (!(unit.getTypeDecls().getFirst() instanceof ClassTree owner)
                || owner.getKind() != Tree.Kind.CLASS || !owner.getModifiers().getAnnotations().isEmpty()
                || !owner.getModifiers().getFlags().equals(Set.of(Modifier.PUBLIC, Modifier.FINAL))
                || owner.getExtendsClause() != null || !owner.getImplementsClause().isEmpty()
                || !owner.getTypeParameters().isEmpty() || !owner.getPermitsClause().isEmpty()
                || !pathMatchesPackage(unit, owner, relativePath)) {
            throw refuse("PLAIN_PUBLIC_FINAL_CLASS_REQUIRED");
        }
        if (originalMembers.size() != 1 || !(originalMembers.getFirst() instanceof MethodTree method)
                || !method.getName().contentEquals("build") || method.getBody() == null
                || method.getReturnType() == null || !method.getReturnType().toString().equals("void")
                || !method.getModifiers().getFlags().equals(Set.of(Modifier.PUBLIC, Modifier.STATIC))
                || !method.getModifiers().getAnnotations().isEmpty() || !method.getParameters().isEmpty()
                || method.getReceiverParameter() != null || !method.getThrows().isEmpty()
                || !method.getTypeParameters().isEmpty() || method.getDefaultValue() != null) {
            throw refuse("SINGLE_STATIC_BUILD_METHOD_REQUIRED");
        }
        var statements = method.getBody().getStatements();
        if (statements.isEmpty() || statements.size() > 256) throw refuse("ROW_BUDGET");
        var rows = new ArrayList<WebWidgetUiBinderProjection.Declaration>();
        for (StatementTree statement : statements) {
            if (!(statement instanceof VariableTree variable) || variable.getInitializer() == null
                    || variable.getType() == null || variable.getType().toString().equals("var")
                    || !variable.getModifiers().getFlags().isEmpty()
                    || !variable.getModifiers().getAnnotations().isEmpty()
                    || !(variable.getInitializer() instanceof NewClassTree constructor)
                    || constructor.getClassBody() != null || constructor.getEnclosingExpression() != null
                    || !constructor.getTypeArguments().isEmpty() || constructor.getArguments().size() != 1
                    || !(constructor.getArguments().getFirst() instanceof LiteralTree literal)
                    || literal.getKind() != Tree.Kind.STRING_LITERAL) {
                throw refuse("ONLY_LITERAL_NEW_WIDGET_VARIABLES");
            }
            TreePath path = TreePath.getPath(unit, constructor);
            Element element = trees.getElement(path);
            if (!(element instanceof ExecutableElement executable)
                    || element.getKind() != ElementKind.CONSTRUCTOR
                    || !(executable.getEnclosingElement() instanceof TypeElement type)
                    || executable.getParameters().size() != 1
                    || !executable.getParameters().getFirst().asType().toString().equals("java.lang.String")) {
                throw refuse("EXACT_CONSTRUCTOR_ATTRIBUTION_REQUIRED");
            }
            String fqcn = type.getQualifiedName().toString();
            if (!fqcn.equals(LABEL) && !fqcn.equals(BUTTON)) throw refuse("FOREIGN_CONSTRUCTOR");
            var variableType = trees.getTypeMirror(TreePath.getPath(unit, variable.getType()));
            if (variableType == null || !variableType.toString().equals(fqcn)) {
                throw refuse("EXACT_VARIABLE_TYPE_REQUIRED");
            }
            String text = (String) literal.getValue();
            // GWT Button(String) is HTML-capable; admit only inert ASCII text
            // while retaining the explicit browser/runtime equivalence hold.
            if (fqcn.equals(BUTTON) && !text.matches("[A-Za-z0-9_. -]{1,256}")) {
                throw refuse("BUTTON_HTML_NOT_PLAIN_TEXT_PROVEN");
            }
            rows.add(new WebWidgetUiBinderProjection.Declaration(
                    WebWidgetAdmission.Framework.GWT, fqcn,
                    variable.getName().toString(), text, true, 0));
        }
        return List.copyOf(rows);
    }

    private static boolean pathMatchesPackage(CompilationUnitTree unit, ClassTree owner,
                                              String relativePath) {
        String suffix = (unit.getPackageName() == null ? ""
                : unit.getPackageName().toString().replace('.', '/') + "/")
                + owner.getSimpleName() + ".java";
        return relativePath.equals(suffix) || relativePath.endsWith("/" + suffix);
    }

    private static IllegalArgumentException refuse(String reason) {
        return new IllegalArgumentException("M3_GWT_AST_" + reason);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[16_384];
                for (int n; (n = input.read(buffer)) != -1;) {
                    digest.update(buffer, 0, n);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
