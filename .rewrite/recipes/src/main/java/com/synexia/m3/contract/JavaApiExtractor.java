package com.synexia.m3.contract;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * JDK compiler-tree source contract extractor.
 *
 * <p>Parsing does not require type attribution or project classpath resolution. It captures declared
 * source contracts before mechanical refactors so file-local transformations can prove that public
 * and protected APIs were not silently removed.</p>
 */
public final class JavaApiExtractor {
    private static final Set<String> SKIP_DIRECTORIES = Set.of(
            ".git", "target", "node_modules", ".idea", ".settings", ".gradle");

    public JavaApiSnapshot snapshot(Path repositoryRoot) throws IOException {
        Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot").toAbsolutePath().normalize();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JDK compiler is required; a JRE-only runtime is insufficient");
        }

        List<Path> files = collectJavaFiles(root);
        List<ApiMember> members = new ArrayList<>();
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(null, Locale.ROOT, java.nio.charset.StandardCharsets.UTF_8)) {
            for (Path file : files) {
                var units = fileManager.getJavaFileObjects(file.toFile());
                JavacTask task = (JavacTask) compiler.getTask(
                        null,
                        fileManager,
                        diagnostic -> {
                            if (diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR) {
                                throw new IllegalArgumentException(
                                        "Java parse error in " + file + ": " + diagnostic.getMessage(Locale.ROOT));
                            }
                        },
                        List.of("-proc:none"),
                        null,
                        units);
                for (CompilationUnitTree unit : task.parse()) {
                    String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
                    new ContractScanner(
                            normalize(root.relativize(file)),
                            packageName,
                            members).scan(unit, null);
                }
            }
        }
        return new JavaApiSnapshot(members);
    }

    private static List<Path> collectJavaFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                if (!directory.equals(root)
                        && SKIP_DIRECTORIES.contains(directory.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (attributes.isRegularFile() && file.getFileName().toString().endsWith(".java")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        files.sort(Comparator.comparing(path -> normalize(root.relativize(path))));
        return List.copyOf(files);
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static final class ContractScanner extends TreeScanner<Void, Void> {
        private final String sourcePath;
        private final String packageName;
        private final List<ApiMember> output;
        private final Deque<TypeContext> owners = new ArrayDeque<>();

        private ContractScanner(String sourcePath, String packageName, List<ApiMember> output) {
            this.sourcePath = sourcePath;
            this.packageName = packageName;
            this.output = output;
        }

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            boolean nestedInInterface = !owners.isEmpty() && owners.peek().interfaceLike();
            ApiVisibility declaredVisibility = visibility(tree.getModifiers(), nestedInInterface);
            if (declaredVisibility == null) return null;
            ApiVisibility visibility = owners.isEmpty()
                    ? declaredVisibility
                    : effectiveVisibility(owners.peek().visibility(), declaredVisibility);

            String simpleName = tree.getSimpleName().toString();
            String owner = owners.isEmpty()
                    ? qualify(packageName, simpleName)
                    : owners.peek().qualifiedName() + "." + simpleName;
            boolean interfaceLike = tree.getKind() == Tree.Kind.INTERFACE
                    || tree.getKind() == Tree.Kind.ANNOTATION_TYPE;

            output.add(new ApiMember(
                    sourcePath,
                    owner,
                    ApiMemberKind.TYPE,
                    visibility,
                    typeSignature(tree, owner)));
            owners.push(new TypeContext(owner, visibility, interfaceLike));

            for (Tree member : tree.getMembers()) {
                scan(member, null);
            }

            owners.pop();
            return null;
        }

        @Override
        public Void visitMethod(MethodTree tree, Void unused) {
            if (owners.isEmpty()) return null;
            TypeContext context = owners.peek();
            ApiVisibility declaredVisibility = visibility(tree.getModifiers(), context.interfaceLike());
            if (declaredVisibility == null) return null;
            ApiVisibility visibility = effectiveVisibility(context.visibility(), declaredVisibility);

            boolean constructor = tree.getReturnType() == null;
            output.add(new ApiMember(
                    sourcePath,
                    context.qualifiedName(),
                    constructor ? ApiMemberKind.CONSTRUCTOR : ApiMemberKind.METHOD,
                    visibility,
                    methodSignature(tree, constructor)));
            return null;
        }

        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
            if (owners.isEmpty()) return null;
            TypeContext context = owners.peek();
            ApiVisibility declaredVisibility = visibility(tree.getModifiers(), context.interfaceLike());
            if (declaredVisibility == null) return null;
            ApiVisibility visibility = effectiveVisibility(context.visibility(), declaredVisibility);
            output.add(new ApiMember(
                    sourcePath,
                    context.qualifiedName(),
                    ApiMemberKind.FIELD,
                    visibility,
                    fieldSignature(tree)));
            return null;
        }

        private static String typeSignature(ClassTree tree, String owner) {
            StringBuilder value = new StringBuilder();
            String modifiers = modifierSignature(tree.getModifiers());
            if (!modifiers.isBlank()) value.append(modifiers).append(' ');
            value.append(tree.getKind().name()).append(' ').append(owner);
            if (!tree.getTypeParameters().isEmpty()) {
                value.append('<').append(joinTypeParameters(tree.getTypeParameters())).append('>');
            }
            if (tree.getExtendsClause() != null) {
                value.append(" extends ").append(normalizeType(tree.getExtendsClause()));
            }
            if (!tree.getImplementsClause().isEmpty()) {
                value.append(" implements ")
                        .append(tree.getImplementsClause().stream()
                                .map(JavaApiExtractor.ContractScanner::normalizeType)
                                .collect(java.util.stream.Collectors.joining(",")));
            }
            if (!tree.getPermitsClause().isEmpty()) {
                value.append(" permits ")
                        .append(tree.getPermitsClause().stream()
                                .map(JavaApiExtractor.ContractScanner::normalizeType)
                                .collect(java.util.stream.Collectors.joining(",")));
            }
            return value.toString();
        }

        private static String methodSignature(MethodTree tree, boolean constructor) {
            StringBuilder value = new StringBuilder();
            String modifiers = modifierSignature(tree.getModifiers());
            if (!modifiers.isBlank()) value.append(modifiers).append(' ');
            if (!tree.getTypeParameters().isEmpty()) {
                value.append('<').append(joinTypeParameters(tree.getTypeParameters())).append("> ");
            }
            if (!constructor) value.append(normalizeType(tree.getReturnType())).append(' ');
            value.append(constructor ? "<init>" : tree.getName()).append('(');
            value.append(tree.getParameters().stream()
                    .map(parameter -> normalizeType(parameter.getType()))
                    .collect(java.util.stream.Collectors.joining(",")));
            value.append(')');
            if (!tree.getThrows().isEmpty()) {
                value.append(" throws ")
                        .append(tree.getThrows().stream()
                                .map(JavaApiExtractor.ContractScanner::normalizeType)
                                .collect(java.util.stream.Collectors.joining(",")));
            }
            return value.toString();
        }

        private static String fieldSignature(VariableTree tree) {
            String modifiers = modifierSignature(tree.getModifiers());
            return (modifiers.isBlank() ? "" : modifiers + " ")
                    + normalizeType(tree.getType()) + " " + tree.getName();
        }

        private static String modifierSignature(ModifiersTree modifiers) {
            List<String> values = new ArrayList<>();
            modifiers.getFlags().stream()
                    .filter(flag -> flag != Modifier.PUBLIC
                            && flag != Modifier.PROTECTED
                            && flag != Modifier.PRIVATE)
                    .map(flag -> flag.name().toLowerCase(Locale.ROOT).replace('_', '-'))
                    .sorted()
                    .forEach(values::add);
            modifiers.getAnnotations().stream()
                    .map(Object::toString)
                    .map(ContractScanner::normalizeWhitespace)
                    .sorted()
                    .forEach(values::add);
            return String.join(" ", values);
        }

        private static String joinTypeParameters(List<? extends TypeParameterTree> parameters) {
            return parameters.stream()
                    .map(Object::toString)
                    .map(JavaApiExtractor.ContractScanner::normalizeWhitespace)
                    .collect(java.util.stream.Collectors.joining(","));
        }

        private static String normalizeType(Tree tree) {
            return tree == null ? "" : normalizeWhitespace(tree.toString());
        }

        private static String normalizeWhitespace(String value) {
            return value.replaceAll("\\s+", " ").trim();
        }

        private static ApiVisibility effectiveVisibility(
                ApiVisibility ownerVisibility,
                ApiVisibility declaredVisibility) {
            if (ownerVisibility == ApiVisibility.PACKAGE) return ApiVisibility.PACKAGE;
            if (ownerVisibility == ApiVisibility.PROTECTED && declaredVisibility == ApiVisibility.PUBLIC) {
                return ApiVisibility.PROTECTED;
            }
            return declaredVisibility;
        }

        private static ApiVisibility visibility(ModifiersTree modifiers, boolean implicitPublic) {
            Set<Modifier> flags = modifiers.getFlags();
            if (flags.contains(Modifier.PRIVATE)) return null;
            if (flags.contains(Modifier.PUBLIC) || implicitPublic) return ApiVisibility.PUBLIC;
            if (flags.contains(Modifier.PROTECTED)) return ApiVisibility.PROTECTED;
            return ApiVisibility.PACKAGE;
        }

        private static String qualify(String packageName, String simpleName) {
            return packageName.isBlank() ? simpleName : packageName + "." + simpleName;
        }

        private record TypeContext(
                String qualifiedName,
                ApiVisibility visibility,
                boolean interfaceLike) {
        }
    }
}
