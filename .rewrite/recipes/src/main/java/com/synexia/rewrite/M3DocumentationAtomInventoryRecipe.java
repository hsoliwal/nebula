// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static java.util.Collections.emptyList;

import com.synexia.rewrite.sealed.SealHash;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

/**
 * Read-only documentation atom inventory.
 *
 * <p>Markdown and AsciiDoc are treated as document structure rather than Java/text source. The
 * recipe emits heading, paragraph and fenced-code atoms without rewriting prose.</p>
 */
public final class M3DocumentationAtomInventoryRecipe
        extends ScanningRecipe<M3DocumentationAtomInventoryRecipe.Accumulator> {

    private static final String MARKDOWN_BACKTICK_FENCE =
            Character.toString(96).repeat(3);

    private final transient DocumentationAtomTable table = new DocumentationAtomTable(this);

    @Override
    public String getDisplayName() {
        return "M3 documentation atom inventory";
    }

    @Override
    public String getDescription() {
        return "Inventories Markdown and AsciiDoc headings, paragraphs, and fenced code as "
                + "content-addressed document atoms. It never applies Java AST transformations.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "documentation",
                "markdown",
                "asciidoc",
                "atom",
                "pattern",
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

    @Override
    public Accumulator getInitialValue(final ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(final Tree tree, final ExecutionContext ctx) {
                if (!(tree instanceof SourceFile source)) {
                    return tree;
                }
                stopAfterPreVisit();
                final String path = source.getSourcePath().toString().replace('\\', '/');
                final M3SourceSpecificity specificity = M3SourceSpecificity.classify(path);
                if (!specificity.documentation()) {
                    return tree;
                }
                acc.rows.addAll(parse(path, specificity, source.printAll()));
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            final Accumulator acc, final ExecutionContext ctx) {
        acc.rows.stream()
                .sorted(Comparator.comparing(DocumentationAtom::sourcePath)
                        .thenComparingInt(DocumentationAtom::ordinal))
                .forEach(row -> table.insertRow(ctx, row));
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public boolean mutationAuthority() {
        return false;
    }

    public boolean sourceCopyAuthority() {
        return false;
    }

    static List<DocumentationAtom> parse(
            final String path,
            final M3SourceSpecificity specificity,
            final String source) {
        final String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        final String[] lines = normalized.split("\n", -1);
        final ArrayList<DocumentationAtom> atoms = new ArrayList<>();
        final StringBuilder paragraph = new StringBuilder();
        int ordinal = 0;
        boolean fence = false;
        String fenceToken = "";
        int fenceStart = -1;
        final StringBuilder fenced = new StringBuilder();

        for (int index = 0; index < lines.length; index++) {
            final String line = lines[index];
            final String stripped = line.stripLeading();
            if (fence) {
                fenced.append(line).append('\n');
                if (closesFence(stripped, fenceToken)) {
                    atoms.add(atom(
                            path,
                            specificity,
                            ++ordinal,
                            "CODE_FENCE",
                            0,
                            fenceStart,
                            index + 1,
                            fenced.toString()));
                    fenced.setLength(0);
                    fence = false;
                    fenceToken = "";
                    fenceStart = -1;
                }
                continue;
            }

            final String opened = fenceToken(stripped, specificity);
            if (!opened.isEmpty()) {
                ordinal = flushParagraph(
                        atoms, path, specificity, ordinal, paragraph, index);
                fence = true;
                fenceToken = opened;
                fenceStart = index + 1;
                fenced.append(line).append('\n');
                continue;
            }

            final int headingLevel = headingLevel(stripped, specificity);
            if (headingLevel > 0) {
                ordinal = flushParagraph(
                        atoms, path, specificity, ordinal, paragraph, index);
                atoms.add(atom(
                        path,
                        specificity,
                        ++ordinal,
                        "HEADING",
                        headingLevel,
                        index + 1,
                        index + 1,
                        line));
                continue;
            }

            if (line.isBlank()) {
                ordinal = flushParagraph(
                        atoms, path, specificity, ordinal, paragraph, index);
                continue;
            }

            if (!paragraph.isEmpty()) {
                paragraph.append('\n');
            }
            paragraph.append(line);
        }

        if (fence && !fenced.isEmpty()) {
            atoms.add(atom(
                    path,
                    specificity,
                    ++ordinal,
                    "CODE_FENCE_UNCLOSED",
                    0,
                    fenceStart,
                    lines.length,
                    fenced.toString()));
        }
        flushParagraph(atoms, path, specificity, ordinal, paragraph, lines.length);
        return List.copyOf(atoms);
    }

    private static int flushParagraph(
            final List<DocumentationAtom> atoms,
            final String path,
            final M3SourceSpecificity specificity,
            final int currentOrdinal,
            final StringBuilder paragraph,
            final int endLineExclusive) {
        if (paragraph.isEmpty()) {
            return currentOrdinal;
        }
        final int lines = 1 + (int) paragraph.chars().filter(ch -> ch == '\n').count();
        final int startLine = Math.max(1, endLineExclusive - lines + 1);
        final int ordinal = currentOrdinal + 1;
        atoms.add(atom(
                path,
                specificity,
                ordinal,
                "PARAGRAPH",
                0,
                startLine,
                Math.max(startLine, endLineExclusive),
                paragraph.toString()));
        paragraph.setLength(0);
        return ordinal;
    }

    private static DocumentationAtom atom(
            final String path,
            final M3SourceSpecificity specificity,
            final int ordinal,
            final String atomKind,
            final int level,
            final int startLine,
            final int endLine,
            final String text) {
        return new DocumentationAtom(
                path,
                specificity.name(),
                ordinal,
                atomKind,
                level,
                startLine,
                endLine,
                text.length(),
                tokenCount(text),
                SealHash.text(text),
                "DOCUMENT_STRUCTURE_ONLY",
                false);
    }

    private static int tokenCount(final String text) {
        final String stripped = text.strip();
        return stripped.isEmpty() ? 0 : stripped.split("\\s+").length;
    }

    private static int headingLevel(
            final String stripped, final M3SourceSpecificity specificity) {
        if (specificity == M3SourceSpecificity.MARKDOWN) {
            int count = 0;
            while (count < stripped.length() && stripped.charAt(count) == '#') {
                count++;
            }
            return count > 0
                            && count <= 6
                            && count < stripped.length()
                            && Character.isWhitespace(stripped.charAt(count))
                    ? count
                    : 0;
        }
        if (specificity == M3SourceSpecificity.ASCIIDOC) {
            int count = 0;
            while (count < stripped.length() && stripped.charAt(count) == '=') {
                count++;
            }
            return count > 0
                            && count <= 6
                            && count < stripped.length()
                            && Character.isWhitespace(stripped.charAt(count))
                    ? count
                    : 0;
        }
        return 0;
    }

    private static String fenceToken(
            final String stripped, final M3SourceSpecificity specificity) {
        if (specificity == M3SourceSpecificity.MARKDOWN) {
            if (stripped.startsWith(MARKDOWN_BACKTICK_FENCE)) {
                return MARKDOWN_BACKTICK_FENCE;
            }
            if (stripped.startsWith("~~~")) {
                return "~~~";
            }
        } else if (specificity == M3SourceSpecificity.ASCIIDOC && stripped.equals("----")) {
            return "----";
        }
        return "";
    }

    private static boolean closesFence(final String stripped, final String token) {
        return stripped.startsWith(token);
    }

    static final class Accumulator {
        private final List<DocumentationAtom> rows = new ArrayList<>();
    }

    public static final class DocumentationAtomTable extends DataTable<DocumentationAtom> {
        DocumentationAtomTable(final org.openrewrite.Recipe recipe) {
            super(
                    recipe,
                    "M3 documentation atoms",
                    "Content-addressed Markdown/AsciiDoc heading, paragraph and fenced-code atoms.");
        }
    }

    public record DocumentationAtom(
            @Column(displayName = "Source path", description = "Repository-relative document path.")
                    String sourcePath,
            @Column(displayName = "Source specificity", description = "Markdown or AsciiDoc.")
                    String sourceSpecificity,
            @Column(displayName = "Ordinal", description = "Stable atom order within the document.")
                    int ordinal,
            @Column(displayName = "Atom kind", description = "Heading, paragraph, or code fence.")
                    String atomKind,
            @Column(displayName = "Heading level", description = "Heading depth, otherwise zero.")
                    int headingLevel,
            @Column(displayName = "Start line", description = "1-based inclusive start line.")
                    int startLine,
            @Column(displayName = "End line", description = "1-based inclusive end line.")
                    int endLine,
            @Column(displayName = "Characters", description = "Atom character count.")
                    int characters,
            @Column(displayName = "Tokens", description = "Whitespace-token estimate.")
                    int tokens,
            @Column(displayName = "Content hash", description = "Content-addressed source seal.")
                    String contentHash,
            @Column(displayName = "Disposition", description = "Permitted processing lane.")
                    String disposition,
            @Column(displayName = "Mutation authority", description = "Always false here.")
                    boolean mutationAuthority) {}
}
