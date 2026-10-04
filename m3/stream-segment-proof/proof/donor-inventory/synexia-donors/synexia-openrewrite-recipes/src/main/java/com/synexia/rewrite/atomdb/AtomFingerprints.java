// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.atomdb;

import com.synexia.rewrite.sealed.SealHash;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

/**
 * Versioned, candidate-only fingerprints of immutable Java trees and textual evidence.
 * Java logic retains names, literals, operators and declaration flags. Only LST Space/comments
 * are discarded; no type attribution, alpha-renaming or behavioral equivalence is inferred.
 */
public final class AtomFingerprints {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}_]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern HEADING = Pattern.compile("^ {0,3}(#{1,6})(?:\\s+|$).*");
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*([-+*]|[0-9]{1,9}[.)])\\s+.*");
    private static final Pattern FENCE = Pattern.compile("^ {0,3}(`{3,}|~{3,})(.*)$");
    private static final PrintOutputCapture.MarkerPrinter NO_DIAGNOSTICS =
            new PrintOutputCapture.MarkerPrinter() { };

    private AtomFingerprints() { }

    public record Fingerprint(String sourceHash, String logicHash, String structureHash, String simHash) {
        public Fingerprint {
            SealHash.require(sourceHash);
            SealHash.require(logicHash);
            SealHash.require(structureHash);
            if (simHash == null || !simHash.matches("[0-9a-f]{16}")) {
                throw new IllegalArgumentException("INVALID_SIMHASH64");
            }
        }
    }

    /**
     * Source identity is the exact input printer output, including its prefix and comments.
     * Balanced shape events and framed leaf spelling bind the space-free printer unambiguously.
     * The normalized copy is used only inside this call; the supplied tree remains unchanged.
     * Unknown/unparsed Java nodes must use {@link #opaque(String)} instead.
     */
    public static Fingerprint javaTree(J tree) {
        Objects.requireNonNull(tree, "tree");
        if (!(tree instanceof SourceFile)) {
            throw new IllegalArgumentException("JAVA_FINGERPRINT_SOURCE_CURSOR_REQUIRED");
        }
        return javaTree(tree, new Cursor(new Cursor(null, Cursor.ROOT_VALUE), tree));
    }

    /**
     * Fingerprint a nested node with the original visitor's current cursor and its actual
     * SourceFile ancestry. An immutable copy may reuse its original node's cursor. Detached
     * nodes have no source/printer context and are rejected instead of inventing a source owner.
     */
    public static Fingerprint javaTree(J tree, Cursor cursor) {
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(cursor, "cursor");
        if (!(cursor.getValue() instanceof J) || cursor.firstEnclosing(SourceFile.class) == null) {
            throw new IllegalArgumentException("JAVA_FINGERPRINT_SOURCE_CURSOR_REQUIRED");
        }
        Cursor parent = cursor.getParent();
        if (parent == null) parent = new Cursor(null, Cursor.ROOT_VALUE);
        String source = tree instanceof SourceFile file ? file.printAll() : tree.print(cursor);
        Events events = new Events();
        J normalized = new JavaIsoVisitor<Events>() {
            @Override
            public J preVisit(J node, Events state) {
                if (node instanceof J.Unknown || node instanceof J.Unknown.Source) {
                    throw new IllegalArgumentException("UNPARSED_JAVA_FINGERPRINT");
                }
                String category = category(node);
                state.shape.add("ENTER:" + category);
                state.logic.add("ENTER:" + category);
                state.features.add(category);
                if (node instanceof J.Identifier identifier) {
                    state.logic.add("NAME:" + identifier.getSimpleName());
                } else if (node instanceof J.Literal literal) {
                    // Preserve source spelling, including escaped strings and Java text blocks.
                    state.logic.add("LITERAL:" + literal.getValueSource());
                } else if (node instanceof J.Modifier modifier) {
                    state.logic.add("KEYWORD:" + modifier.getKeyword());
                }
                return node;
            }

            @Override
            public J postVisit(J node, Events state) {
                String category = category(node);
                state.shape.add("EXIT:" + category);
                state.logic.add("EXIT:" + category);
                return node;
            }

            @Override
            public Space visitSpace(Space space, Space.Location location, Events state) {
                return Space.EMPTY;
            }
        }.visitNonNull(tree, events, parent);
        PrintOutputCapture<Integer> printed = new PrintOutputCapture<>(0, NO_DIAGNOSTICS);
        new JavaPrinter<Integer>().visit(normalized, printed, parent);
        return new Fingerprint(
                SealHash.text(source),
                SealHash.frame("ATOM-JAVA-LOGIC/1", framed(events.logic), printed.getOut()),
                SealHash.frame("ATOM-JAVA-STRUCTURE/1", framed(events.shape)),
                simHash(events.features));
    }

    /**
     * Document source identity preserves the exact supplied text. Logic normalizes line endings
     * and whitespace outside code fences; fenced contents remain exact after newline normalization.
     * Structure observes bounded Markdown ATX headings, paragraphs, lists, pipe rows and fences
     * in source order, omitting prose/code values. Other syntax, including RST, is paragraph evidence;
     * this is not a complete Markdown parser or documentation equivalence proof.
     */
    public static Fingerprint document(String text) {
        Objects.requireNonNull(text, "text");
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        List<String> shape = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        boolean paragraph = false;
        char fenceCharacter = 0;
        int fenceLength = 0;
        shape.add("ENTER:DOCUMENT");
        for (String line : normalized.split("\n", -1)) {
            Matcher fence = FENCE.matcher(line);
            if (fenceCharacter != 0) {
                lines.add(line);
                if (fence.matches() && fence.group(1).charAt(0) == fenceCharacter
                        && fence.group(1).length() >= fenceLength && fence.group(2).isBlank()) {
                    shape.add("EXIT:CODE_FENCE");
                    fenceCharacter = 0;
                } else {
                    shape.add("CODE_LINE");
                }
                continue;
            }
            String clean = WHITESPACE.matcher(line).replaceAll(" ").trim();
            lines.add(clean);
            Matcher heading = HEADING.matcher(line);
            Matcher item = LIST_ITEM.matcher(line);
            String role;
            if (fence.matches()) {
                role = "ENTER:CODE_FENCE";
                fenceCharacter = fence.group(1).charAt(0);
                fenceLength = fence.group(1).length();
            } else if (clean.isEmpty()) {
                role = "PARAGRAPH_BREAK";
            } else if (heading.matches()) {
                role = "HEADING:" + heading.group(1).length();
            } else if (item.matches()) {
                role = "LIST_ITEM:" + (Character.isDigit(item.group(1).charAt(0)) ? "ORDERED" : "BULLET");
            } else if (clean.startsWith("|") && clean.endsWith("|") && clean.length() > 1) {
                long columns = clean.chars().filter(value -> value == '|').count() - 1;
                role = "PIPE_ROW:" + columns;
            } else {
                role = "PARAGRAPH";
            }
            if (!role.equals("PARAGRAPH") || !paragraph) shape.add(role);
            paragraph = role.equals("PARAGRAPH");
        }
        if (fenceCharacter != 0) shape.add("UNCLOSED:CODE_FENCE");
        shape.add("EXIT:DOCUMENT");
        return new Fingerprint(SealHash.text(text),
                SealHash.frame("ATOM-DOCUMENT-LOGIC/1", String.join("\n", lines)),
                SealHash.frame("ATOM-DOCUMENT-STRUCTURE/1", framed(shape)),
                simHash(words(text)));
    }

    /** Exact-text fallback: no language normalization or executable structure is claimed. */
    public static Fingerprint opaque(String text) {
        Objects.requireNonNull(text, "text");
        return new Fingerprint(SealHash.text(text),
                SealHash.frame("ATOM-OPAQUE-LOGIC/1", text),
                SealHash.frame("ATOM-OPAQUE-STRUCTURE/1", text),
                simHash(words(text)));
    }

    /**
     * ADAPT of cognix-rag live.SimHash: FNV-1a over UTF-16 code units, unigram weight 1,
     * adjacent-pair weight 2, and ties set to zero. Features are already extracted by the caller;
     * their order and exact spelling are retained. The result is a shortlist signal, not proof.
     */
    public static String simHash(List<String> features) {
        Objects.requireNonNull(features, "features");
        long[] weights = new long[64];
        String previous = null;
        for (String feature : features) {
            Objects.requireNonNull(feature, "feature");
            accumulate(weights, feature, 1);
            if (previous != null) accumulate(weights, previous + "\u0000" + feature, 2);
            previous = feature;
        }
        long bits = 0L;
        for (int bit = 0; bit < 64; bit++) {
            if (weights[bit] > 0) bits |= 1L << bit;
        }
        return String.format(Locale.ROOT, "%016x", bits);
    }

    private static void accumulate(long[] weights, String feature, int magnitude) {
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < feature.length(); index++) {
            hash ^= feature.charAt(index);
            hash *= 0x100000001b3L;
        }
        for (int bit = 0; bit < 64; bit++) {
            weights[bit] += ((hash >>> bit) & 1L) == 1 ? magnitude : -magnitude;
        }
    }

    private static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        Matcher matcher = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) words.add(matcher.group());
        return words;
    }

    private static String category(J node) {
        String name = node.getClass().getName().substring(J.class.getName().length() + 1);
        if (node instanceof J.Binary value) return name + ':' + value.getOperator();
        if (node instanceof J.AssignmentOperation value) return name + ':' + value.getOperator();
        if (node instanceof J.Unary value) return name + ':' + value.getOperator();
        if (node instanceof J.Primitive value) return name + ':' + value.getType();
        if (node instanceof J.Literal value) return name + ':' + value.getType();
        if (node instanceof J.Modifier value) return name + ':' + value.getType();
        if (node instanceof J.ClassDeclaration value) return name + ':' + value.getKind();
        if (node instanceof J.ClassDeclaration.Kind value) return name + ':' + value.getType();
        if (node instanceof J.Case value) return name + ':' + value.getType();
        if (node instanceof J.Wildcard value) return name + ':' + value.getBound();
        if (node instanceof J.Block value) return name + ":static=" + value.isStatic();
        if (node instanceof J.Import value) return name + ":static=" + value.isStatic();
        if (node instanceof J.MethodDeclaration value) return name + ":constructor=" + value.isConstructor();
        if (node instanceof J.Yield value) return name + ":implicit=" + value.isImplicit();
        return name;
    }

    private static String framed(List<String> events) {
        StringBuilder framed = new StringBuilder();
        for (String event : events) framed.append(event.length()).append(':').append(event);
        return framed.toString();
    }

    private static final class Events {
        private final List<String> logic = new ArrayList<>();
        private final List<String> shape = new ArrayList<>();
        private final List<String> features = new ArrayList<>();
    }
}

