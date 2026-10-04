// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative Java-owned Python atomizer for the recipe-first lane.
 *
 * <p>Recognizes class, def and async-def suites using indentation and a bounded lexical scan of the
 * declaration header. It never imports or executes Python. Decorators plus the declaration header
 * form the immutable contract slice; recipes may independently replace only bounded bodies.</p>
 */
public final class M3PythonAtomizer {
    private static final Pattern DEFINITION =
            Pattern.compile("^(?:async\\s+def|def|class)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b");
    private static final int MAX_HEADER_CHARS = 256 * 1024;

    private M3PythonAtomizer() {}

    public static List<M3PythonAtom> analyze(String sourcePath, String source) {
        String path = M3OpenRewriteTranspiler.normalizeSourcePathOrDirectory(sourcePath);
        if (M3SourceKind.classify(path) != M3SourceKind.PYTHON) {
            throw new IllegalArgumentException("Python source required");
        }
        Objects.requireNonNull(source, "source");
        if (source.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("NUL-bearing Python source rejected");
        }

        List<Line> lines = lines(source);
        ArrayList<Seed> seeds = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            Line line = lines.get(lineIndex);
            String trimmed = line.text().stripLeading();
            Matcher matcher = DEFINITION.matcher(trimmed);
            if (!matcher.find()) continue;

            int indentation = indentationWidth(line.text());
            int declarationOffset = line.start() + (line.text().length() - trimmed.length());
            Header header = header(source, declarationOffset);
            int startOffset = line.start();
            for (int prior = lineIndex - 1; prior >= 0; prior--) {
                Line decorator = lines.get(prior);
                if (decorator.text().isBlank()) break;
                String decoratorText = decorator.text().stripLeading();
                if (!decoratorText.startsWith("@")
                        || indentationWidth(decorator.text()) != indentation) {
                    break;
                }
                startOffset = decorator.start();
            }
            M3PythonAtom.Kind kind =
                    trimmed.startsWith("class ")
                            ? M3PythonAtom.Kind.CLASS
                            : trimmed.startsWith("async ")
                                    ? M3PythonAtom.Kind.ASYNC_FUNCTION
                                    : M3PythonAtom.Kind.FUNCTION;
            seeds.add(
                    new Seed(
                            kind,
                            matcher.group(1),
                            indentation,
                            lineIndex,
                            startOffset,
                            header.endOffset(),
                            header.bodyStartOffset(),
                            Integer.MAX_VALUE));
        }

        for (int index = 0; index < seeds.size(); index++) {
            Seed seed = seeds.get(index);
            int headerLineIndex =
                    lineIndexContaining(lines, seed.headerEndOffset() - 1);
            Line headerLine = lines.get(headerLineIndex);
            String sameLineTail =
                    source.substring(seed.headerEndOffset(), headerLine.end());
            boolean oneLineSuite =
                    !sameLineTail.strip().isEmpty()
                            && !sameLineTail.stripLeading().startsWith("#");
            int end =
                    oneLineSuite
                            ? headerLine.endWithTerminator()
                            : source.length();
            if (!oneLineSuite) {
                for (int lineIndex = headerLineIndex + 1;
                        lineIndex < lines.size();
                        lineIndex++) {
                    Line line = lines.get(lineIndex);
                    String trimmed = line.text().strip();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    if (indentationWidth(line.text()) <= seed.indentation()) {
                        end = line.start();
                        break;
                    }
                }
            }
            seeds.set(index, seed.withEnd(end));
        }

        ArrayList<M3PythonAtom> atoms = new ArrayList<>(seeds.size());
        for (Seed seed : seeds) {
            Seed parent = parent(seeds, seed);
            String qualifiedName =
                    parent == null
                            ? seed.name()
                            : qualifiedName(seeds, parent) + "." + seed.name();
            boolean leaf =
                    seeds.stream()
                            .noneMatch(
                                    candidate ->
                                            candidate != seed
                                                    && candidate.startOffset()
                                                            >= seed.bodyStartOffset()
                                                    && candidate.startOffset() < seed.endOffset()
                                                    && candidate.indentation()
                                                            > seed.indentation());
            boolean external =
                    !seed.name().startsWith("_")
                            && (parent == null
                                    || parent.kind() == M3PythonAtom.Kind.CLASS
                                            && !parent.name().startsWith("_"));
            String contract =
                    normalizeNewlines(
                            source.substring(seed.startOffset(), seed.headerEndOffset()));
            String body = source.substring(seed.bodyStartOffset(), seed.endOffset());
            atoms.add(
                    new M3PythonAtom(
                            path,
                            seed.kind(),
                            qualifiedName,
                            seed.name(),
                            seed.indentation(),
                            seed.startOffset(),
                            seed.headerEndOffset(),
                            seed.bodyStartOffset(),
                            seed.endOffset(),
                            external,
                            leaf,
                            sha256(contract),
                            sha256(body),
                            ""));
        }
        atoms.sort(
                Comparator.comparingInt(M3PythonAtom::startOffset)
                        .thenComparing(M3PythonAtom::qualifiedName));
        return List.copyOf(atoms);
    }

    public static String contractFingerprint(List<M3PythonAtom> atoms) {
        StringBuilder material =
                new StringBuilder("M3-PYTHON-CONTRACT-FINGERPRINT/1\n");
        Objects.requireNonNull(atoms, "atoms").stream()
                .sorted(
                        Comparator.comparingInt(M3PythonAtom::startOffset)
                                .thenComparing(M3PythonAtom::qualifiedName))
                .forEach(
                        atom ->
                                material.append(atom.kind())
                                        .append('\t')
                                        .append(atom.qualifiedName())
                                        .append('\t')
                                        .append(atom.contractSha256())
                                        .append('\n'));
        return sha256(material.toString());
    }

    static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static Header header(String source, int declarationOffset) {
        int depth = 0;
        Quote quote = Quote.NONE;
        boolean escaped = false;
        boolean comment = false;
        int limit = Math.min(source.length(), declarationOffset + MAX_HEADER_CHARS);
        for (int index = declarationOffset; index < limit; index++) {
            char ch = source.charAt(index);
            char next = index + 1 < limit ? source.charAt(index + 1) : '\0';
            char next2 = index + 2 < limit ? source.charAt(index + 2) : '\0';

            if (comment) {
                if (ch == '\n' || ch == '\r') comment = false;
                continue;
            }
            if (quote != Quote.NONE) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (quote == Quote.TRIPLE_SINGLE
                        && ch == '\''
                        && next == '\''
                        && next2 == '\'') {
                    quote = Quote.NONE;
                    index += 2;
                } else if (quote == Quote.TRIPLE_DOUBLE
                        && ch == '"'
                        && next == '"'
                        && next2 == '"') {
                    quote = Quote.NONE;
                    index += 2;
                } else if (quote == Quote.SINGLE && ch == '\'') {
                    quote = Quote.NONE;
                } else if (quote == Quote.DOUBLE && ch == '"') {
                    quote = Quote.NONE;
                }
                continue;
            }

            if (ch == '#') {
                comment = true;
            } else if (ch == '\'' && next == '\'' && next2 == '\'') {
                quote = Quote.TRIPLE_SINGLE;
                index += 2;
            } else if (ch == '"' && next == '"' && next2 == '"') {
                quote = Quote.TRIPLE_DOUBLE;
                index += 2;
            } else if (ch == '\'') {
                quote = Quote.SINGLE;
            } else if (ch == '"') {
                quote = Quote.DOUBLE;
            } else if (ch == '(' || ch == '[' || ch == '{') {
                depth++;
            } else if (ch == ')' || ch == ']' || ch == '}') {
                depth = Math.max(0, depth - 1);
            } else if (ch == ':' && depth == 0) {
                int end = index + 1;
                int body = end;
                if (body < source.length() && source.charAt(body) == '\r') body++;
                if (body < source.length() && source.charAt(body) == '\n') body++;
                return new Header(end, body);
            }
        }
        throw new IllegalArgumentException(
                "Python declaration header is not bounded by ':'");
    }

    private static Seed parent(List<Seed> seeds, Seed child) {
        Seed selected = null;
        for (Seed candidate : seeds) {
            if (candidate == child
                    || candidate.startOffset() >= child.startOffset()
                    || candidate.endOffset() <= child.startOffset()
                    || candidate.indentation() >= child.indentation()) {
                continue;
            }
            if (selected == null || candidate.indentation() > selected.indentation()) {
                selected = candidate;
            }
        }
        return selected;
    }

    private static String qualifiedName(List<Seed> seeds, Seed seed) {
        Seed parent = parent(seeds, seed);
        return parent == null
                ? seed.name()
                : qualifiedName(seeds, parent) + "." + seed.name();
    }

    private static int lineIndexContaining(
            List<Line> lines, int offset) {
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            if (offset >= line.start()
                    && offset < Math.max(line.endWithTerminator(), line.start() + 1)) {
                return index;
            }
        }
        throw new IllegalArgumentException(
                "Python header offset is outside source lines");
    }

    private static int indentationWidth(String line) {
        int width = 0;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == ' ') {
                width++;
            } else if (ch == '\t') {
                width = ((width / 8) + 1) * 8;
            } else {
                break;
            }
        }
        return width;
    }

    private static List<Line> lines(String source) {
        ArrayList<Line> result = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < source.length(); index++) {
            char ch = source.charAt(index);
            if (ch != '\n' && ch != '\r') continue;
            int end = index;
            int terminated = index + 1;
            if (ch == '\r'
                    && terminated < source.length()
                    && source.charAt(terminated) == '\n') {
                terminated++;
                index++;
            }
            result.add(
                    new Line(
                            start,
                            end,
                            terminated,
                            source.substring(start, end)));
            start = terminated;
        }
        if (start < source.length() || source.isEmpty()) {
            result.add(
                    new Line(
                            start,
                            source.length(),
                            source.length(),
                            source.substring(start)));
        }
        return List.copyOf(result);
    }

    private static String normalizeNewlines(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private enum Quote {
        NONE,
        SINGLE,
        DOUBLE,
        TRIPLE_SINGLE,
        TRIPLE_DOUBLE
    }

    private record Header(int endOffset, int bodyStartOffset) {}

    private record Line(
            int start, int end, int endWithTerminator, String text) {}

    private record Seed(
            M3PythonAtom.Kind kind,
            String name,
            int indentation,
            int declarationLine,
            int startOffset,
            int headerEndOffset,
            int bodyStartOffset,
            int endOffset) {

        private Seed withEnd(int end) {
            return new Seed(
                    kind,
                    name,
                    indentation,
                    declarationLine,
                    startOffset,
                    headerEndOffset,
                    bodyStartOffset,
                    end);
        }
    }
}
