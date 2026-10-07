// SPDX-License-Identifier: Apache-2.0
package com.synexia.ai.m3scale;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Parse-only JDK atomizer. It never loads or executes analyzed classes. */
public final class M3JavaAtomizer {
    private static final Set<String> KEYWORDS = Set.of(
            "abstract","assert","boolean","break","byte","case","catch","char","class","const","continue",
            "default","do","double","else","enum","extends","final","finally","float","for","goto","if",
            "implements","import","instanceof","int","interface","long","native","new","package","private",
            "protected","public","return","short","static","strictfp","super","switch","synchronized","this",
            "throw","throws","transient","try","void","volatile","while","record","sealed","permits","non-sealed",
            "var","yield","true","false","null");

    private M3JavaAtomizer() {}

    public static M3FileFacts analyze(String moduleId, String logicalPath, byte[] bytes, long maxFileBytes) {
        if (bytes.length > maxFileBytes) throw new IllegalArgumentException("file byte budget");
        String source = strictUtf8(bytes);
        String exact = M3Hash.sha256(bytes);
        List<String> logicTokens = logicTokens(source);
        String logic = M3Hash.canonical("M3_LOGIC_V1", logicTokens);
        long simHash = M3Hash.simHash(similarityTokens(logicTokens));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            String empty = M3Hash.canonical("M3_UNAVAILABLE", List.of(logicalPath));
            return new M3FileFacts(moduleId, logicalPath, "", bytes.length, exact, empty, empty, empty, logic,
                    simHash, false, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of("JDK_COMPILER_UNAVAILABLE"));
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Scanner scanner = new Scanner();
        try {
            parseJava21(logicalPath, source, diagnostics, (task, tree) -> {
                scanner.packageName = tree.getPackageName() == null ? "" : tree.getPackageName().toString();
                scanner.scan(tree, null);
            });
        } catch (RuntimeException | java.io.IOException exception) {
            scanner.diagnostics.add("PARSER_EXCEPTION:" + exception.getClass().getSimpleName());
        }
        diagnostics.getDiagnostics().stream().map(M3JavaAtomizer::diagnosticId).forEach(scanner.diagnostics::add);
        boolean parsed = scanner.diagnostics.stream().noneMatch(x -> x.startsWith("ERROR:"))
                && scanner.diagnostics.stream().noneMatch(x -> x.startsWith("PARSER_EXCEPTION:"));
        String contract = M3Hash.canonical("M3_FILE_CONTRACT_V1", concat(
                List.of("package=" + scanner.packageName), List.copyOf(scanner.declarations)));
        String api = M3Hash.canonical("M3_FILE_API_V1", concat(
                List.of("package=" + scanner.packageName), List.copyOf(scanner.apiDeclarations)));
        String structure = M3Hash.canonical("M3_STRUCTURE_V1", scanner.structure);
        return new M3FileFacts(moduleId, logicalPath, scanner.packageName, bytes.length, exact, contract, api, structure,
                logic, simHash, parsed, List.copyOf(scanner.declaredTypes), List.copyOf(scanner.declarations),
                List.copyOf(scanner.apiDeclarations), List.copyOf(scanner.imports), List.copyOf(scanner.calls),
                List.copyOf(scanner.referencedTypes), List.copyOf(scanner.diagnostics));
    }

    /** Shared, resource-scoped parse boundary for SQLite facts and portable component packs. */
    static void parseJava21(String logicalPath, String source, DiagnosticCollector<JavaFileObject> diagnostics,
            java.util.function.BiConsumer<JavacTask, CompilationUnitTree> visitor) throws java.io.IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new java.io.IOException("JDK_COMPILER_UNAVAILABLE");
        try (var manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "-Xlint:none", "--source", "21"), null, List.of(new SourceObject(logicalPath, source)));
            for (CompilationUnitTree tree : task.parse()) visitor.accept(task, tree);
        }
    }

    private static String strictUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException exception) {
            throw new IllegalArgumentException("invalid UTF-8", exception);
        }
    }

    private static String diagnosticId(Diagnostic<? extends JavaFileObject> diagnostic) {
        return diagnostic.getKind().name() + ":" + diagnostic.getCode() + ":"
                + diagnostic.getLineNumber() + ":" + diagnostic.getColumnNumber();
    }

    private static List<String> concat(List<String> left, List<String> right) {
        ArrayList<String> out = new ArrayList<>(left.size() + right.size());
        out.addAll(left); out.addAll(right); return out;
    }

    /** Token projection preserves Java keywords/operators/literal values while alpha-renaming identifiers. */
    static List<String> logicTokens(String source) {
        ArrayList<String> out = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                i = skipLineComment(source, i); continue;
            }
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                i = skipBlockComment(source, i); continue;
            }
            if (c == '"' || c == '\'') {
                i = projectQuotedToken(source, i, out); continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                i = projectIdentifier(source, i, out); continue;
            }
            if (Character.isDigit(c)) {
                i = projectNumber(source, i, out); continue;
            }
            String op = longestOperator(source, i);
            out.add(op); i += op.length();
        }
        return List.copyOf(out);
    }

    // Stateless lexical atoms: preserve the existing projection, including malformed-input handling.
    // No new parser, retained state, or per-token wrapper is introduced by this extraction.
    private static int skipLineComment(String source, int i) {
        i += 2;
        while (i < source.length() && source.charAt(i) != '\n') i++;
        return i;
    }

    private static int skipBlockComment(String source, int i) {
        i += 2;
        while (i + 1 < source.length() && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) i++;
        return Math.min(source.length(), i + 2);
    }

    private static int projectQuotedToken(String source, int i, List<String> out) {
        char quote = source.charAt(i);
        int start = i++;
        boolean escape = false;
        while (i < source.length()) {
            char x = source.charAt(i++);
            if (escape) { escape = false; continue; }
            if (x == '\\') { escape = true; continue; }
            if (x == quote) break;
        }
        String literal = source.substring(start, Math.min(i, source.length()));
        out.add((quote == '"' ? "STR:" : "CHAR:") + M3Hash.sha256(literal).substring(0, 16));
        return i;
    }

    private static int projectIdentifier(String source, int i, List<String> out) {
        int start = i++;
        while (i < source.length() && Character.isJavaIdentifierPart(source.charAt(i))) i++;
        String word = source.substring(start, i);
        out.add(KEYWORDS.contains(word) ? word : "ID");
        return i;
    }

    private static int projectNumber(String source, int i, List<String> out) {
        int start = i++;
        while (i < source.length()) {
            char x = source.charAt(i);
            if (Character.isLetterOrDigit(x) || x == '_' || x == '.' || x == '+' || x == '-') i++; else break;
        }
        out.add("NUM:" + source.substring(start, i).toLowerCase(Locale.ROOT));
        return i;
    }

    private static List<String> similarityTokens(List<String> logic) {
        return logic.stream().map(token -> token.startsWith("NUM:") ? "NUM"
                : token.startsWith("STR:") ? "STR" : token.startsWith("CHAR:") ? "CHAR" : token).toList();
    }

    private static String longestOperator(String source, int index) {
        String[] operators = {">>>=","<<=",">>=","...","::","->","==","!=","<=",">=","&&","||","++","--","+=","-=","*=","/=","%=","&=","|=","^=","<<",">>",">>>"};
        for (String op : operators) if (source.startsWith(op, index)) return op;
        return Character.toString(source.charAt(index));
    }

    private static final class SourceObject extends SimpleJavaFileObject {
        private final String source;
        SourceObject(String logicalPath, String source) {
            super(URI.create("string:///M3_" + M3Hash.sha256(logicalPath).substring(0, 16) + ".java"), Kind.SOURCE);
            this.source = source;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
    }

    private static final class Scanner extends TreeScanner<Void, Void> {
        String packageName = "";
        final TreeSet<String> declaredTypes = new TreeSet<>();
        final TreeSet<String> declarations = new TreeSet<>();
        final TreeSet<String> apiDeclarations = new TreeSet<>();
        final TreeSet<String> imports = new TreeSet<>();
        final TreeSet<String> calls = new TreeSet<>();
        final TreeSet<String> referencedTypes = new TreeSet<>();
        final TreeSet<String> diagnostics = new TreeSet<>();
        final ArrayList<String> structure = new ArrayList<>();
        final Deque<String> typeStack = new ArrayDeque<>();
        int methodDepth;

        @Override public Void scan(Tree tree, Void unused) {
            if (tree == null) return null;
            structure.add(tree.getKind().name());
            return super.scan(tree, unused);
        }

        @Override public Void visitImport(ImportTree node, Void unused) {
            String value = node.getQualifiedIdentifier().toString();
            imports.add((node.isStatic() ? "static " : "") + value);
            referencedTypes.add(value);
            return super.visitImport(node, unused);
        }

        @Override public Void visitClass(ClassTree node, Void unused) {
            String name = node.getSimpleName().toString();
            String fqcn = packageName.isEmpty() ? name : packageName + "." + name;
            declaredTypes.add(fqcn);
            String declaration = "TYPE:" + modifiers(node.getModifiers().getFlags()) + ":" + node.getKind().name()
                    + ":" + name + ":extends=" + (node.getExtendsClause() == null ? "" : node.getExtendsClause())
                    + ":implements=" + node.getImplementsClause();
            declarations.add(declaration);
            if (isApi(node.getModifiers().getFlags())) apiDeclarations.add(declaration);
            if (node.getExtendsClause() != null) referencedTypes.add(node.getExtendsClause().toString());
            node.getImplementsClause().forEach(x -> referencedTypes.add(x.toString()));
            typeStack.push(fqcn);
            try { return super.visitClass(node, unused); } finally { typeStack.pop(); }
        }

        @Override public Void visitMethod(MethodTree node, Void unused) {
            String owner = typeStack.isEmpty() ? packageName : typeStack.peek();
            String parameters = node.getParameters().stream().map(v -> v.getType().toString()).reduce((a,b)->a+","+b).orElse("");
            String result = node.getReturnType() == null ? "<init>" : node.getReturnType().toString();
            String declaration = "METHOD:" + modifiers(node.getModifiers().getFlags()) + ":" + owner + "#" + node.getName()
                    + "(" + parameters + "):" + result + ":throws=" + node.getThrows();
            declarations.add(declaration);
            if (isApi(node.getModifiers().getFlags())) apiDeclarations.add(declaration);
            if (node.getReturnType() != null) referencedTypes.add(node.getReturnType().toString());
            node.getParameters().forEach(v -> referencedTypes.add(v.getType().toString()));
            node.getThrows().forEach(x -> referencedTypes.add(x.toString()));
            methodDepth++;
            try { return super.visitMethod(node, unused); } finally { methodDepth--; }
        }

        @Override public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
            // Implicit lambda parameters have no declared type and are not class fields.
            // Field-initializer lambdas must establish the same local scope as method bodies.
            methodDepth++;
            try { return super.visitLambdaExpression(node, unused); } finally { methodDepth--; }
        }

        @Override public Void visitVariable(VariableTree node, Void unused) {
            if (!typeStack.isEmpty() && methodDepth == 0) {
                String declaration = "FIELD:" + modifiers(node.getModifiers().getFlags()) + ":" + typeStack.peek() + "#"
                        + node.getName() + ":" + node.getType();
                declarations.add(declaration);
                if (isApi(node.getModifiers().getFlags())) apiDeclarations.add(declaration);
                referencedTypes.add(node.getType().toString());
            }
            return super.visitVariable(node, unused);
        }

        @Override public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            calls.add("CALL:" + node.getMethodSelect());
            return super.visitMethodInvocation(node, unused);
        }

        @Override public Void visitNewClass(NewClassTree node, Void unused) {
            calls.add("NEW:" + node.getIdentifier()); referencedTypes.add(node.getIdentifier().toString());
            return super.visitNewClass(node, unused);
        }

        @Override public Void visitMemberReference(MemberReferenceTree node, Void unused) {
            calls.add("REF:" + node.getQualifierExpression() + "::" + node.getName());
            return super.visitMemberReference(node, unused);
        }

        @Override public Void visitBinary(BinaryTree node, Void unused) {
            structure.add("OP:" + node.getKind().name()); return super.visitBinary(node, unused);
        }
        @Override public Void visitUnary(UnaryTree node, Void unused) {
            structure.add("OP:" + node.getKind().name()); return super.visitUnary(node, unused);
        }
        @Override public Void visitLiteral(LiteralTree node, Void unused) {
            Object value = node.getValue(); structure.add("LITERAL:" + (value == null ? "NULL" : value.getClass().getSimpleName()));
            return super.visitLiteral(node, unused);
        }

        private static String modifiers(Set<Modifier> modifiers) {
            return modifiers.stream().map(Modifier::name).sorted().reduce((a,b)->a+","+b).orElse("");
        }
        private static boolean isApi(Set<Modifier> modifiers) {
            return modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED);
        }
    }
}
