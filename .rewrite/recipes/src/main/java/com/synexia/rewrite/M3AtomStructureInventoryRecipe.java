// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;

/**
 * Candidate-only, one-pass Java atom and hierarchy inventory.
 *
 * <p>The recipe does not mutate a source tree. It emits two DataTables: one for normalized
 * atom facts and one for the parent/child structure that composes those facts. The host owns
 * DataTable serialization and later admission.</p>
 */
public final class M3AtomStructureInventoryRecipe extends Recipe {
    private static final String EMITTED_PREFIX = M3AtomStructureInventoryRecipe.class.getName();
    private static final int MAX_SOURCE_CHARS = 8 * 1024 * 1024;
    private static final int MAX_COMPOSITION_CHARS = 4096;
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
            "int", "interface", "long", "native", "new", "package", "private", "protected",
            "public", "record", "return", "sealed", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "non-sealed", "permits", "var", "yield", "true", "false", "null");

    @Option(
            displayName = "Repository",
            description = "Stable repository identity used as the hierarchy root.",
            example = "hsoliwal/com.synexia",
            required = false)
    private final String repository;

    @Option(
            displayName = "Project",
            description = "Stable project identity below the repository.",
            example = "com.synexia",
            required = false)
    private final String project;

    @Option(
            displayName = "Module",
            description = "Stable module identity below the project.",
            example = "synexia-openrewrite-recipes",
            required = false)
    private final String module;

    @Option(
            displayName = "Libraries",
            description = "Comma-separated explicit library identities; dependency inference is not guessed.",
            example = "org.openrewrite:rewrite-core:8.17.1,org.openrewrite:rewrite-java:8.17.1",
            required = false)
    private final String libraries;

    private final transient AtomTable atomTable = new AtomTable(this);
    private final transient StructureTable structureTable = new StructureTable(this);

    public M3AtomStructureInventoryRecipe() {
        this("", "", "", "");
    }

    @JsonCreator
    public M3AtomStructureInventoryRecipe(
            @JsonProperty("repository") String repository,
            @JsonProperty("project") String project,
            @JsonProperty("module") String module,
            @JsonProperty("libraries") String libraries) {
        this.repository = cleanOption(repository);
        this.project = cleanOption(project);
        this.module = cleanOption(module);
        this.libraries = cleanOption(libraries);
    }

    @Override
    public String getDisplayName() {
        return "Inventory M3 atoms and structure hashes";
    }

    @Override
    public String getDescription() {
        return "Emits candidate-only atom, similarity, logic, composition, structure, and "
                + "repository-to-member hierarchy rows in one Java AST traversal without source mutation.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "atom",
                "structure",
                "logic-hash",
                "similarity-hash",
                "hierarchy",
                "inventory-first",
                "candidate-only",
                "read-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    public boolean replacementAuthority() {
        return false;
    }

    public String getRepository() {
        return repository;
    }

    public String getProject() {
        return project;
    }

    public String getModule() {
        return module;
    }

    public String getLibraries() {
        return libraries;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final Deque<Frame> frames = new ArrayDeque<>();
            private String sourcePath;
            private int ordinal;

            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit compilationUnit, ExecutionContext context) {
                sourcePath = normalizePath(compilationUnit.getSourcePath());
                ordinal = 0;
                frames.clear();

                String repositoryName = valueOr(repository, "<unspecified-repository>");
                String projectName = valueOr(project, "<unspecified-project>");
                String moduleName = valueOr(module, "<unspecified-module>");
                String packageName = compilationUnit.getPackageDeclaration() == null
                        ? "<default>"
                        : valueOr(compilationUnit.getPackageDeclaration().getPackageName(), "<default>");

                String repositoryId = stableId("REPOSITORY", repositoryName);
                String projectId = stableId("PROJECT", repositoryId, projectName);
                String moduleId = stableId("MODULE", projectId, moduleName);
                String packageId = stableId("PACKAGE", moduleId, packageName);

                emitIdentity(context, repositoryId, "", "REPOSITORY", repositoryName, 0);
                emitIdentity(context, projectId, repositoryId, "PROJECT", projectName, 1);
                emitIdentity(context, moduleId, projectId, "MODULE", moduleName, 2);
                for (String library : parseLibraries(libraries)) {
                    emitIdentity(
                            context,
                            stableId("LIBRARY", moduleId, library),
                            moduleId,
                            "LIBRARY",
                            library,
                            3);
                }
                emitIdentity(context, packageId, moduleId, "PACKAGE", packageName, 3);

                String source = bounded(compilationUnit.printAll(), MAX_SOURCE_CHARS);
                String fileId = stableId("FILE", packageId, sourcePath);
                String fileAtomId = stableId("ATOM", fileId, "FILE");
                emitAtom(
                        context,
                        new AtomRow(
                                sourcePath,
                                fileAtomId,
                                fileId,
                                "FILE",
                                sourcePath,
                                normalized(source),
                                compositionHash(source),
                                atomHash("FILE", source),
                                similarityHash(source),
                                simHash64(source),
                                logicHash(source)));
                Frame file = new Frame(fileId, packageId, "FILE", sourcePath, fileAtomId, 4);
                frames.push(file);
                emitComments(context, compilationUnit, file);

                J.CompilationUnit result = super.visitCompilationUnit(compilationUnit, context);
                frames.pop();
                emitFrame(context, file);
                return result;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration classDeclaration, ExecutionContext context) {
                Frame parent = currentFrame();
                String kind = classKind(classDeclaration);
                String symbol = classDeclaration.getSimpleName();
                String nodeId = nextId(kind, parent.nodeId, symbol);
                String atomId = stableId("ATOM", nodeId, kind);
                String source = typeComposition(classDeclaration);
                emitAtom(
                        context,
                        new AtomRow(
                                sourcePath,
                                atomId,
                                nodeId,
                                kind,
                                symbol,
                                normalized(source),
                                compositionHash(source),
                                atomHash(kind, source),
                                similarityHash(source),
                                simHash64(source),
                                logicHash(source)));

                Frame type = new Frame(nodeId, parent.nodeId, kind, symbol, atomId, parent.depth + 1);
                emitComments(context, classDeclaration, type);
                frames.push(type);
                J.ClassDeclaration result = super.visitClassDeclaration(classDeclaration, context);
                frames.pop();
                String structureHash = emitFrame(context, type);
                parent.children.add(childReference(type, structureHash));
                return result;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext context) {
                Frame parent = currentFrame();
                String symbol = method.getSimpleName();
                String nodeId = nextId("METHOD", parent.nodeId, symbol);
                String atomId = stableId("ATOM", nodeId, "METHOD");
                String source = bounded(method.printTrimmed(getCursor()), MAX_SOURCE_CHARS);
                String body = method.getBody() == null
                        ? "<no-body>"
                        : bounded(method.getBody().printTrimmed(getCursor()), MAX_SOURCE_CHARS);
                emitAtom(
                        context,
                        new AtomRow(
                                sourcePath,
                                atomId,
                                nodeId,
                                "METHOD",
                                symbol,
                                normalized(source),
                                compositionHash(source),
                                atomHash("METHOD", source),
                                similarityHash(source),
                                simHash64(source),
                                logicHash(body)));

                Frame member = new Frame(nodeId, parent.nodeId, "METHOD", symbol, atomId, parent.depth + 1);
                emitComments(context, method, member);
                frames.push(member);
                J.MethodDeclaration result = super.visitMethodDeclaration(method, context);
                frames.pop();
                String structureHash = emitFrame(context, member);
                parent.children.add(childReference(member, structureHash));
                return result;
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(
                    J.VariableDeclarations.NamedVariable variable, ExecutionContext context) {
                if (!variable.isField(getCursor())) {
                    return super.visitVariable(variable, context);
                }
                Frame parent = currentFrame();
                String symbol = variable.getSimpleName();
                String nodeId = nextId("FIELD", parent.nodeId, symbol);
                String atomId = stableId("ATOM", nodeId, "FIELD");
                String source = bounded(variable.printTrimmed(getCursor()), MAX_SOURCE_CHARS);
                String initializer = variable.getInitializer() == null
                        ? "<no-initializer>"
                        : bounded(variable.getInitializer().printTrimmed(getCursor()), MAX_SOURCE_CHARS);
                emitAtom(
                        context,
                        new AtomRow(
                                sourcePath,
                                atomId,
                                nodeId,
                                "FIELD",
                                symbol,
                                normalized(source),
                                compositionHash(source),
                                atomHash("FIELD", source),
                                similarityHash(source),
                                simHash64(source),
                                logicHash(initializer)));

                Frame member = new Frame(nodeId, parent.nodeId, "FIELD", symbol, atomId, parent.depth + 1);
                emitComments(context, variable, member);
                J.VariableDeclarations.NamedVariable result = super.visitVariable(variable, context);
                String structureHash = emitFrame(context, member);
                parent.children.add(childReference(member, structureHash));
                return result;
            }

            private Frame currentFrame() {
                Frame frame = frames.peek();
                if (frame == null) {
                    throw new IllegalStateException("Java AST node visited without an enclosing file");
                }
                return frame;
            }

            private String nextId(String kind, String parentId, String symbol) {
                ordinal++;
                return stableId(kind, sourcePath, parentId, symbol, Integer.toString(ordinal));
            }

            private void emitComments(ExecutionContext context, J node, Frame owner) {
                for (Comment comment : node.getComments()) {
                    String text = bounded(comment.printComment(getCursor()), MAX_SOURCE_CHARS);
                    if (text.isBlank()) {
                        continue;
                    }
                    String nodeId = nextId("DOC", owner.nodeId, "comment");
                    String atomId = stableId("ATOM", nodeId, "DOC");
                    emitAtom(
                            context,
                            new AtomRow(
                                    sourcePath,
                                    atomId,
                                    nodeId,
                                    "DOC",
                                    "comment",
                                    normalized(text),
                                    compositionHash(text),
                                    atomHash("DOC", text),
                                    similarityHash(text),
                                    simHash64(text),
                                    logicHash(text)));
                    Frame doc = new Frame(nodeId, owner.nodeId, "DOC", "comment", atomId, owner.depth + 1);
                    String structureHash = emitFrame(context, doc);
                    owner.children.add(childReference(doc, structureHash));
                }
            }

            private void emitIdentity(
                    ExecutionContext context,
                    String nodeId,
                    String parentId,
                    String kind,
                    String symbol,
                    int depth) {
                emitFrame(context, new Frame(nodeId, parentId, kind, symbol, "", depth));
            }

            private void emitAtom(ExecutionContext context, AtomRow row) {
                synchronized (context) {
                    String key = EMITTED_PREFIX + ".atom." + row.getAtomId();
                    if (Boolean.TRUE.equals(context.getMessage(key))) {
                        return;
                    }
                    context.putMessage(key, Boolean.TRUE);
                    atomTable.insertRow(context, row);
                }
            }

            private String emitFrame(ExecutionContext context, Frame frame) {
                String composition = String.join("|", frame.children);
                String compositionHash = hashComponents("composition-v1", composition);
                String structureHash = hashComponents(
                        "structure-v1",
                        frame.kind,
                        frame.parentId,
                        frame.symbol,
                        compositionHash,
                        composition);
                StructureRow row = new StructureRow(
                        sourcePath,
                        frame.nodeId,
                        frame.parentId,
                        frame.kind,
                        frame.symbol,
                        frame.atomId,
                        bounded(composition, MAX_COMPOSITION_CHARS),
                        compositionHash,
                        structureHash,
                        frame.depth);
                synchronized (context) {
                    String key = EMITTED_PREFIX + ".structure." + frame.nodeId;
                    if (Boolean.TRUE.equals(context.getMessage(key))) {
                        return structureHash;
                    }
                    context.putMessage(key, Boolean.TRUE);
                    structureTable.insertRow(context, row);
                }
                return structureHash;
            }
        };
    }

    public static String normalizeExact(String source) {
        return normalized(source);
    }

    public static String normalizeSimilarity(String source) {
        return tokenize(source, true);
    }

    public static String hashComponents(String... values) {
        MessageDigest digest = sha256();
        for (String value : values) {
            byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
            digest.update((byte) (bytes.length >>> 24));
            digest.update((byte) (bytes.length >>> 16));
            digest.update((byte) (bytes.length >>> 8));
            digest.update((byte) bytes.length);
            digest.update(bytes);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String atomHash(String kind, String source) {
        return hashComponents("atom-v1", kind, normalized(source));
    }

    private static String compositionHash(String source) {
        return hashComponents("atom-composition-v1", normalized(source));
    }

    private static String similarityHash(String source) {
        return hashComponents("similarity-v1", tokenize(source, true));
    }

    /** Candidate-only 64-bit SimHash over the generalized token shape. */
    public static long simHash64(String source) {
        String shape = tokenize(source, true);
        if (shape.isEmpty()) {
            return 0L;
        }
        int[] weights = new int[Long.SIZE];
        for (String token : shape.split(String.valueOf('\u001f'), -1)) {
            long mixed = mix64(fnv64(token));
            for (int bit = 0; bit < Long.SIZE; bit++) {
                weights[bit] += ((mixed >>> bit) & 1L) == 0L ? -1 : 1;
            }
        }
        long result = 0L;
        for (int bit = 0; bit < Long.SIZE; bit++) {
            if (weights[bit] >= 0) {
                result |= 1L << bit;
            }
        }
        return result;
    }

    private static long fnv64(String value) {
        long hash = 0xcbf29ce484222325L;
        for (byte current : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= Byte.toUnsignedLong(current);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed = (mixed ^ (mixed >>> 30)) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }

    private static String logicHash(String source) {
        return hashComponents("logic-v1", normalized(source));
    }

    private static String normalized(String source) {
        return tokenize(source, false);
    }

    private static String tokenize(String source, boolean generalized) {
        String input = Objects.requireNonNullElse(source, "");
        StringBuilder result = new StringBuilder(Math.min(input.length(), MAX_SOURCE_CHARS));
        int index = 0;
        while (index < input.length()) {
            char current = input.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (current == '/' && index + 1 < input.length()) {
                char next = input.charAt(index + 1);
                if (next == '/') {
                    index += 2;
                    while (index < input.length() && input.charAt(index) != '\n') {
                        index++;
                    }
                    continue;
                }
                if (next == '*') {
                    index += 2;
                    while (index + 1 < input.length()
                            && !(input.charAt(index) == '*' && input.charAt(index + 1) == '/')) {
                        index++;
                    }
                    index = Math.min(index + 2, input.length());
                    continue;
                }
            }
            if (current == '"' || current == '\'') {
                int start = index;
                char quote = current;
                boolean textBlock = quote == '"'
                        && index + 2 < input.length()
                        && input.startsWith("\"\"\"", index);
                index += textBlock ? 3 : 1;
                String closing = textBlock ? "\"\"\"" : String.valueOf(quote);
                while (index < input.length()) {
                    if (input.charAt(index) == '\\' && index + 1 < input.length()) {
                        index += 2;
                    } else if (input.startsWith(closing, index)) {
                        index += closing.length();
                        break;
                    } else {
                        index++;
                    }
                }
                appendToken(result, generalized ? "<literal>" : input.substring(start, index));
                continue;
            }
            if (Character.isJavaIdentifierStart(current)) {
                int start = index++;
                while (index < input.length() && Character.isJavaIdentifierPart(input.charAt(index))) {
                    index++;
                }
                String token = input.substring(start, index);
                appendToken(result, generalized && !JAVA_KEYWORDS.contains(token) ? "<id>" : token);
                continue;
            }
            if (Character.isDigit(current)) {
                int start = index++;
                while (index < input.length()) {
                    char next = input.charAt(index);
                    if (!Character.isLetterOrDigit(next) && next != '.' && next != '_') {
                        break;
                    }
                    index++;
                }
                appendToken(result, generalized ? "<number>" : input.substring(start, index));
                continue;
            }
            appendToken(result, String.valueOf(current));
            index++;
        }
        return result.toString();
    }

    private static void appendToken(StringBuilder result, String token) {
        if (result.length() > 0) {
            result.append('\u001f');
        }
        result.append(token);
    }

    private static String typeComposition(J.ClassDeclaration declaration) {
        StringBuilder result = new StringBuilder();
        result.append(declaration.getKind().name()).append(' ')
                .append(declaration.getSimpleName());
        if (declaration.getExtends() != null) {
            result.append(" extends ").append(declaration.getExtends());
        }
        if (declaration.getImplements() != null && !declaration.getImplements().isEmpty()) {
            result.append(" implements ");
            for (var implemented : declaration.getImplements()) {
                result.append(implemented).append(',');
            }
        }
        return result.toString();
    }

    private static String classKind(J.ClassDeclaration declaration) {
        return declaration.getKind().name().toUpperCase(Locale.ROOT).contains("INTERFACE")
                ? "INTERFACE"
                : "IMPLEMENTATION";
    }

    private static List<String> parseLibraries(String raw) {
        TreeSet<String> sorted = new TreeSet<>();
        for (String candidate : Objects.requireNonNullElse(raw, "").split(",")) {
            String value = candidate.strip();
            if (!value.isEmpty()) {
                sorted.add(value);
            }
        }
        return List.copyOf(sorted);
    }

    private static String stableId(String kind, String... values) {
        String[] all = new String[values.length + 1];
        all[0] = kind;
        System.arraycopy(values, 0, all, 1, values.length);
        return hashComponents(all);
    }

    private static String cleanOption(String value) {
        return Objects.requireNonNullElse(value, "").strip();
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String normalizePath(Path path) {
        return path == null ? "<unknown-source>" : path.toString().replace('\\', '/');
    }

    private static String bounded(String value, int maximum) {
        String checked = Objects.requireNonNullElse(value, "");
        return checked.length() <= maximum
                ? checked
                : checked.substring(0, maximum) + "\n<TRUNCATED>";
    }

    private static String childReference(Frame child, String structureHash) {
        return child.kind + ":" + child.nodeId + ":" + child.atomId + ":" + structureHash;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static final class Frame {
        private final String nodeId;
        private final String parentId;
        private final String kind;
        private final String symbol;
        private final String atomId;
        private final int depth;
        private final List<String> children = new ArrayList<>();

        private Frame(
                String nodeId,
                String parentId,
                String kind,
                String symbol,
                String atomId,
                int depth) {
            this.nodeId = nodeId;
            this.parentId = parentId;
            this.kind = kind;
            this.symbol = symbol;
            this.atomId = atomId;
            this.depth = depth;
        }
    }

    public static final class AtomTable extends DataTable<AtomRow> {
        AtomTable(Recipe recipe) {
            super(recipe, "M3 atom inventory", "Normalized atom facts and precomputed hashes.");
        }
    }

    public static final class StructureTable extends DataTable<StructureRow> {
        StructureTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 atom structure hierarchy",
                    "Candidate-only parent/child composition and repository hierarchy.");
        }
    }

    public static final class AtomRow {
        @Column(displayName = "Source path", description = "Repository-relative source path.")
        private final String sourcePath;
        @Column(displayName = "Atom ID", description = "Stable atom identity.")
        private final String atomId;
        @Column(displayName = "Owner node ID", description = "Structure node containing the atom.")
        private final String ownerNodeId;
        @Column(displayName = "Atom kind", description = "FILE, TYPE, METHOD, FIELD, or DOC.")
        private final String atomKind;
        @Column(displayName = "Symbol", description = "Source symbol or documentation marker.")
        private final String symbol;
        @Column(displayName = "Normalized composition", description = "Bounded normalized token composition.")
        private final String normalizedComposition;
        @Column(displayName = "Composition SHA-256", description = "Normalized composition hash for this atom.")
        private final String compositionSha256;
        @Column(displayName = "Atom SHA-256", description = "Exact normalized atom hash.")
        private final String atomSha256;
        @Column(displayName = "Similarity SHA-256", description = "Generalized token-shape hash.")
        private final String similaritySha256;
        @Column(displayName = "SimHash-64", description = "Candidate-only generalized token-shape SimHash.")
        private final long simHash64;
        @Column(displayName = "Logic SHA-256", description = "Executable/body or initializer hash.")
        private final String logicSha256;
        @Column(displayName = "Authority", description = "Always candidate-only.")
        private final String authority = "CANDIDATE_ONLY";
        @Column(displayName = "Replacement authority", description = "Always false.")
        private final boolean replacementAuthority = false;

        AtomRow(
                String sourcePath,
                String atomId,
                String ownerNodeId,
                String atomKind,
                String symbol,
                String normalizedComposition,
                String compositionSha256,
                String atomSha256,
                String similaritySha256,
                long simHash64,
                String logicSha256) {
            this.sourcePath = required(sourcePath, "sourcePath");
            this.atomId = hashRequired(atomId, "atomId");
            this.ownerNodeId = hashRequired(ownerNodeId, "ownerNodeId");
            this.atomKind = required(atomKind, "atomKind");
            this.symbol = required(symbol, "symbol");
            this.normalizedComposition = bounded(normalizedComposition, MAX_COMPOSITION_CHARS);
            this.compositionSha256 = hashRequired(compositionSha256, "compositionSha256");
            this.atomSha256 = hashRequired(atomSha256, "atomSha256");
            this.similaritySha256 = hashRequired(similaritySha256, "similaritySha256");
            this.simHash64 = simHash64;
            this.logicSha256 = hashRequired(logicSha256, "logicSha256");
        }

        public String getSourcePath() { return sourcePath; }
        public String getAtomId() { return atomId; }
        public String getOwnerNodeId() { return ownerNodeId; }
        public String getAtomKind() { return atomKind; }
        public String getSymbol() { return symbol; }
        public String getNormalizedComposition() { return normalizedComposition; }
        public String getCompositionSha256() { return compositionSha256; }
        public String getAtomSha256() { return atomSha256; }
        public String getSimilaritySha256() { return similaritySha256; }
        public long getSimHash64() { return simHash64; }
        public String getLogicSha256() { return logicSha256; }
        public String getAuthority() { return authority; }
        public boolean isReplacementAuthority() { return replacementAuthority; }
    }

    public static final class StructureRow {
        @Column(displayName = "Source path", description = "Repository-relative source path.")
        private final String sourcePath;
        @Column(displayName = "Node ID", description = "Stable hierarchy node identity.")
        private final String nodeId;
        @Column(displayName = "Parent ID", description = "Stable parent node identity.")
        private final String parentId;
        @Column(displayName = "Node kind", description = "Repository, project, module, package, file, type, or member.")
        private final String nodeKind;
        @Column(displayName = "Symbol", description = "Hierarchy symbol.")
        private final String symbol;
        @Column(displayName = "Atom ID", description = "Linked atom identity, if any.")
        private final String atomId;
        @Column(displayName = "Child references", description = "Ordered child atom/structure references.")
        private final String childReferences;
        @Column(displayName = "Composition SHA-256", description = "Hash of ordered child references.")
        private final String compositionSha256;
        @Column(displayName = "Structure SHA-256", description = "Hash of node identity and composition.")
        private final String structureSha256;
        @Column(displayName = "Depth", description = "Hierarchy depth.")
        private final int depth;
        @Column(displayName = "Authority", description = "Always candidate-only.")
        private final String authority = "CANDIDATE_ONLY";
        @Column(displayName = "Replacement authority", description = "Always false.")
        private final boolean replacementAuthority = false;

        StructureRow(
                String sourcePath,
                String nodeId,
                String parentId,
                String nodeKind,
                String symbol,
                String atomId,
                String childReferences,
                String compositionSha256,
                String structureSha256,
                int depth) {
            this.sourcePath = required(sourcePath, "sourcePath");
            this.nodeId = hashRequired(nodeId, "nodeId");
            this.parentId = parentId == null ? "" : parentId;
            this.nodeKind = required(nodeKind, "nodeKind");
            this.symbol = required(symbol, "symbol");
            this.atomId = atomId == null ? "" : atomId;
            this.childReferences = bounded(childReferences, MAX_COMPOSITION_CHARS);
            this.compositionSha256 = hashRequired(compositionSha256, "compositionSha256");
            this.structureSha256 = hashRequired(structureSha256, "structureSha256");
            if (depth < 0) {
                throw new IllegalArgumentException("depth");
            }
            this.depth = depth;
        }

        public String getSourcePath() { return sourcePath; }
        public String getNodeId() { return nodeId; }
        public String getParentId() { return parentId; }
        public String getNodeKind() { return nodeKind; }
        public String getSymbol() { return symbol; }
        public String getAtomId() { return atomId; }
        public String getChildReferences() { return childReferences; }
        public String getCompositionSha256() { return compositionSha256; }
        public String getStructureSha256() { return structureSha256; }
        public int getDepth() { return depth; }
        public String getAuthority() { return authority; }
        public boolean isReplacementAuthority() { return replacementAuthority; }
    }

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (checked.isBlank() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String hashRequired(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }
}


