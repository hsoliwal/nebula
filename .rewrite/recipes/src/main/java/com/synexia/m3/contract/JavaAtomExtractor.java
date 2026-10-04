package com.synexia.m3.contract;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * JDK-tree file atomizer for type/member replacement candidates.
 *
 * <p>Method bodies are deliberately not recursively atomized here; a method/constructor is one
 * behavior atom. This keeps the first replacement boundary mechanically understandable.</p>
 */
public final class JavaAtomExtractor {
    private static final Set<String> SKIP_DIRECTORIES = Set.of(
            ".git", "target", "node_modules", ".idea", ".settings", ".gradle");

    private static final List<String> COMPOSITION_OPTIONS = List.of("-proc:none", "--source", "21");

    /**
     * Caller-revalidated input roots for one composition request. These are binding values, never
     * proof of recipe, toolchain, dependency or contract admission. The caller must still verify
     * the actual inputs and every semantic/caller dependency on each mechanical pass.
     */
    public record CompositionBinding(String recipeRoot, String contractRoot, String toolchainRoot) {
        public CompositionBinding {
            requireRoot(recipeRoot, "recipeRoot");
            requireRoot(contractRoot, "contractRoot");
            requireRoot(toolchainRoot, "toolchainRoot");
        }

        private static void requireRoot(String value, String name) {
            if (value == null || value.length() != 64
                    || !value.chars().allMatch(c -> c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) {
                throw new IllegalArgumentException(name + " must be lowercase SHA-256");
            }
        }
    }

    /**
     * Creates an opt-in, in-process composition session. Existing uncached entry points are unchanged.
     * The request budget bounds repeated work; orchestration still owns pass/fixed-point/cycle exits.
     */
    public CompositionSession newCompositionSession(
            int maxEntries, long maxRetainedSourceCharacters, long maxRequests) {
        return new CompositionSession(this, maxEntries, maxRetainedSourceCharacters, maxRequests);
    }

    /**
     * Bounded reuse of immutable lossless source compositions, not attributed ASTs or recipe results.
     *
     * <p>Keys retain the complete validated source String and use exact equality, not just a digest
     * or filesystem timestamp. Path, source, recipe/contract/toolchain roots or actual parser
     * configuration changes always miss. No file contents are read by the session; file-backed
     * callers must re-read/verify their current immutable snapshot before calling. No static cache,
     * disk persistence, compiler tree, context or verification receipt is retained.</p>
     *
     * <p>The two retention limits count entries and input UTF-16 characters, not measured heap bytes;
     * compositions also retain atom/segment metadata and text. Oversized and unsupported inputs are
     * never retained. Calls are serialized for deterministic budgets/counters and safe eviction.
     * Closing releases retained values and rejects further work.</p>
     */
    public static final class CompositionSession implements AutoCloseable {
        public record Statistics(long requests, long hits, long extractionCalls, long evictions,
                                 long rejectedAdmissions, int entries, long retainedSourceCharacters) {}

        private record Key(String path, String source, CompositionBinding binding, String parserIdentity) {}
        private final JavaAtomExtractor extractor;
        private final int maxEntries;
        private final long maxRetainedSourceCharacters;
        private final long maxRequests;
        private final java.util.LinkedHashMap<Key, JavaFileComposition> entries =
                new java.util.LinkedHashMap<>(16, 0.75f, true);
        private long requests;
        private long hits;
        private long extractionCalls;
        private long evictions;
        private long rejectedAdmissions;
        private long retainedSourceCharacters;
        private boolean closed;

        private CompositionSession(JavaAtomExtractor extractor, int maxEntries,
                long maxRetainedSourceCharacters, long maxRequests) {
            if (maxEntries < 1 || maxRetainedSourceCharacters < 1 || maxRequests < 1) {
                throw new IllegalArgumentException("positive composition session budgets required");
            }
            this.extractor = extractor;
            this.maxEntries = maxEntries;
            this.maxRetainedSourceCharacters = maxRetainedSourceCharacters;
            this.maxRequests = maxRequests;
        }

        public JavaFileComposition compose(Path path, String source, CompositionBinding binding)
                throws IOException {
            return compose(path, source, binding, com.synexia.job.IProgressMonitor.noop());
        }

        public synchronized JavaFileComposition compose(Path path, String source,
                CompositionBinding binding, com.synexia.job.IProgressMonitor monitor) throws IOException {
            if (closed) throw new IllegalStateException("composition session is closed");
            Objects.requireNonNull(monitor, "monitor").checkCanceled();
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(binding, "binding");
            String portable = normalize(compositionPath(path));
            if (requests == maxRequests) throw new IllegalStateException("composition request budget exhausted");
            requests++;
            Key key = new Key(portable, source, binding, compositionParserIdentity());
            JavaFileComposition cached = entries.get(key);
            if (cached != null) {
                monitor.checkCanceled();
                hits++;
                return cached;
            }
            extractionCalls++;
            JavaFileComposition result = extractor.compose(Path.of(portable), source, monitor);
            monitor.checkCanceled();
            if (result.status() != JavaFileComposition.Status.COMPLETE
                    || source.length() > maxRetainedSourceCharacters) {
                rejectedAdmissions++;
                return result;
            }
            while (!entries.isEmpty() && (entries.size() == maxEntries
                    || source.length() > maxRetainedSourceCharacters - retainedSourceCharacters)) {
                var oldest = entries.entrySet().iterator();
                retainedSourceCharacters -= oldest.next().getKey().source().length();
                oldest.remove();
                evictions++;
            }
            entries.put(key, result);
            retainedSourceCharacters += source.length();
            return result;
        }

        public synchronized Statistics statistics() {
            return new Statistics(requests, hits, extractionCalls, evictions, rejectedAdmissions,
                    entries.size(), retainedSourceCharacters);
        }

        @Override public synchronized void close() {
            entries.clear();
            retainedSourceCharacters = 0;
            closed = true;
        }
    }

    private static Path compositionPath(Path sourcePath) {
        Path path = Path.of(Objects.requireNonNull(sourcePath, "sourcePath").toString()
                .replace('\\', '/')).normalize();
        if (path.isAbsolute() || path.startsWith("..") || path.toString().isBlank()) {
            throw new IllegalArgumentException("repository-relative source path required");
        }
        return path;
    }

    private static String compositionParserIdentity() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler is required");
        // An in-process session cannot cross JVM/class-loader upgrades. Include the actual fixed
        // options, compiler module and runtime identity rather than trusting a caller label alone.
        return "JAVA_FILE_COMPOSITION/1|UTF-8|Locale.ROOT|" + COMPOSITION_OPTIONS
                + '|' + compiler.getClass().getName() + '|' + compiler.getClass().getModule().getDescriptor().toNameAndVersion()
                + '|' + Runtime.version();
    }

    /**
     * Inventories one immutable source snapshot using the existing scanner and preserves every
     * source position in a lossless composition. No class loading, attribution or source editing.
     * Parse errors retain the entire input as typed unsupported evidence, never a partial inventory.
     */
    public JavaFileComposition compose(Path sourcePath, String source) throws IOException {
        return compose(sourcePath, source, com.synexia.job.IProgressMonitor.noop());
    }

    public JavaFileComposition compose(Path sourcePath, String source,
            com.synexia.job.IProgressMonitor monitor) throws IOException {
        Path path = Path.of(Objects.requireNonNull(sourcePath, "sourcePath").toString()
                .replace('\\', '/')).normalize();
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(monitor, "monitor").checkCanceled();
        if (path.isAbsolute() || path.startsWith("..") || path.toString().isBlank()) {
            throw new IllegalArgumentException("repository-relative source path required");
        }
        // Hashing uses UTF-8; reject malformed UTF-16 rather than silently replacing surrogates.
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i == source.length() || !Character.isLowSurrogate(source.charAt(i))) {
                    throw new IllegalArgumentException("malformed UTF-16 source");
                }
            } else if (Character.isLowSurrogate(c)) throw new IllegalArgumentException("malformed UTF-16 source");
        }
        String portable = normalize(path);
        if (!portable.endsWith(".java")) {
            return JavaFileComposition.unsupported(portable, source,
                    JavaFileComposition.Status.UNSUPPORTED_LANGUAGE);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler is required");
        var diagnostics = new javax.tools.DiagnosticCollector<javax.tools.JavaFileObject>();
        var input = new javax.tools.SimpleJavaFileObject(
                java.net.URI.create("string:///Composition.java"), javax.tools.JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
        };
        List<CodeAtom> atoms = new ArrayList<>();
        try (var manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    COMPOSITION_OPTIONS, null, List.of(input));
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            var units = new ArrayList<CompilationUnitTree>();
            task.parse().forEach(units::add);
            if (diagnostics.getDiagnostics().stream()
                    .noneMatch(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR)) {
                for (CompilationUnitTree unit : units) {
                    monitor.checkCanceled();
                    new AtomScanner(portable, unit, source, positions, atoms).scan(unit, null);
                }
            }
        }
        monitor.checkCanceled();
        if (diagnostics.getDiagnostics().stream()
                .anyMatch(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR)) {
            return JavaFileComposition.unsupported(portable, source,
                    JavaFileComposition.Status.UNSUPPORTED_PARSE);
        }
        return JavaFileComposition.create(portable, source, atoms);
    }

    public List<CodeAtom> extract(Path repositoryRoot) throws IOException {
        Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot").toAbsolutePath().normalize();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler is required");

        List<CodeAtom> atoms = new ArrayList<>();
        try (StandardJavaFileManager manager =
                     compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            for (Path file : collectJavaFiles(root)) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                var units = manager.getJavaFileObjects(file.toFile());
                JavacTask task = (JavacTask) compiler.getTask(
                        null, manager, null, List.of("-proc:none"), null, units);
                Trees trees = Trees.instance(task);
                SourcePositions positions = trees.getSourcePositions();
                for (CompilationUnitTree unit : task.parse()) {
                    new AtomScanner(
                            normalize(root.relativize(file)),
                            unit,
                            source,
                            positions,
                            atoms).scan(unit, null);
                }
            }
        }
        atoms.sort(Comparator
                .comparing(CodeAtom::sourcePath)
                .thenComparingLong(CodeAtom::startOffset)
                .thenComparing(CodeAtom::kind)
                .thenComparing(CodeAtom::name));
        return List.copyOf(atoms);
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

    private static final class AtomScanner extends TreeScanner<Void, Void> {
        private final String sourcePath;
        private final CompilationUnitTree unit;
        private final String source;
        private final SourcePositions positions;
        private final List<CodeAtom> output;
        private final Deque<String> owners = new ArrayDeque<>();

        private AtomScanner(
                String sourcePath,
                CompilationUnitTree unit,
                String source,
                SourcePositions positions,
                List<CodeAtom> output) {
            this.sourcePath = sourcePath;
            this.unit = unit;
            this.source = source;
            this.positions = positions;
            this.output = output;
        }

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
            String owner = owners.isEmpty()
                    ? qualify(packageName, tree.getSimpleName().toString())
                    : owners.peek() + "." + tree.getSimpleName();
            add(tree, owner, CodeAtomKind.TYPE, tree.getSimpleName().toString());
            owners.push(owner);
            int initializer = 0;
            for (Tree member : tree.getMembers()) {
                if (member instanceof ClassTree nested) {
                    scan(nested, null);
                } else if (member instanceof MethodTree method) {
                    boolean constructor = method.getReturnType() == null;
                    add(method, owner,
                            constructor ? CodeAtomKind.CONSTRUCTOR : CodeAtomKind.METHOD,
                            constructor ? "<init>" : method.getName().toString());
                } else if (member instanceof VariableTree field) {
                    add(field, owner, CodeAtomKind.FIELD, field.getName().toString());
                } else if (member instanceof BlockTree block) {
                    initializer++;
                    add(block, owner, CodeAtomKind.INITIALIZER,
                            (block.isStatic() ? "<clinit>#" : "<init-block>#") + initializer);
                }
            }
            owners.pop();
            return null;
        }

        private void add(Tree tree, String owner, CodeAtomKind kind, String name) {
            long start = positions.getStartPosition(unit, tree);
            long end = positions.getEndPosition(unit, tree);
            if (start < 0 || end < start || end > source.length()) return;
            String atomSource = source.substring(Math.toIntExact(start), Math.toIntExact(end));
            output.add(new CodeAtom(
                    sourcePath, owner, kind, name, start, end, ContractHashing.sha256(atomSource)));
        }

        private static String qualify(String packageName, String simpleName) {
            return packageName.isBlank() ? simpleName : packageName + "." + simpleName;
        }
    }
}

