// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.contract;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Lossless source-order composition over the canonical JDK member atoms.
 *
 * <p>This is a source partition, not an execution DAG: source order must never be interpreted as
 * permission to reorder evaluation. Bodies retain their complete control, exception and aliasing
 * behavior. Unclassified spans include type shells, imports and comments and are never rewrite
 * candidates. Shared canonical AST nodes may index these occurrence spans; hashes are provenance,
 * not equivalence or promotion proof.</p>
 */
public final class JavaFileComposition {
    public enum Status { COMPLETE, UNSUPPORTED_PARSE, UNSUPPORTED_OVERLAP, UNSUPPORTED_LANGUAGE }
    public enum Kind { ATOM, FIELD_GROUP, UNCLASSIFIED_SOURCE }

    /** Offsets are UTF-16 code-unit positions, exactly as in {@link CodeAtom}. */
    public record Segment(int ordinal, int start, int end, Kind kind,
                          List<CodeAtom> atoms, String text, String id) {
        public Segment {
            if (ordinal < 0 || start < 0 || end < start) throw new IllegalArgumentException("span");
            kind = Objects.requireNonNull(kind, "kind");
            atoms = List.copyOf(atoms);
            text = Objects.requireNonNull(text, "text");
            id = Objects.requireNonNull(id, "id");
            if (text.length() != end - start) throw new IllegalArgumentException("span text");
        }
    }

    private final String sourcePath;
    private final String sourceSha256;
    private final Status status;
    private final List<CodeAtom> atoms;
    private final List<Segment> segments;
    private final String root;

    private JavaFileComposition(String path, String source, List<CodeAtom> inventory, Status status) {
        sourcePath = path;
        sourceSha256 = ContractHashing.sha256(source);
        this.status = status;
        atoms = List.copyOf(inventory);
        var parts = new ArrayList<Segment>();
        int cursor = 0;
        if (status == Status.COMPLETE) {
            List<CodeAtom> members = atoms.stream().filter(a -> a.kind() != CodeAtomKind.TYPE)
                    .sorted(Comparator.comparingLong(CodeAtom::startOffset)
                            .thenComparingLong(CodeAtom::endOffset)).toList();
            for (int i = 0; i < members.size();) {
                CodeAtom first = members.get(i++);
                int start = Math.toIntExact(first.startOffset());
                int end = Math.toIntExact(first.endOffset());
                if (start < cursor) throw new IllegalArgumentException("overlapping atoms");
                if (cursor < start) add(parts, source, cursor, start, Kind.UNCLASSIFIED_SOURCE, List.of());
                var group = new ArrayList<CodeAtom>();
                group.add(first);
                while (i < members.size() && members.get(i).startOffset() < end) {
                    CodeAtom next = members.get(i++);
                    if (first.kind() != CodeAtomKind.FIELD || next.kind() != CodeAtomKind.FIELD
                            || next.startOffset() != start || !next.owner().equals(first.owner())) {
                        throw new IllegalArgumentException("overlapping atoms");
                    }
                    group.add(next);
                    end = Math.max(end, Math.toIntExact(next.endOffset()));
                }
                add(parts, source, start, end, group.size() == 1 ? Kind.ATOM : Kind.FIELD_GROUP, group);
                cursor = end;
            }
        }
        if (cursor < source.length() || parts.isEmpty()) {
            add(parts, source, cursor, source.length(), Kind.UNCLASSIFIED_SOURCE, List.of());
        }
        segments = List.copyOf(parts);
        var roots = new ArrayList<String>();
        roots.add(path);
        roots.add(sourceSha256);
        roots.add(status.name());
        atoms.forEach(a -> roots.add(atomKey(a)));
        segments.forEach(s -> roots.add(s.id()));
        root = framedHash("JAVA_FILE_COMPOSITION/1", roots);
    }

    static JavaFileComposition create(String path, String source, List<CodeAtom> atoms) {
        for (CodeAtom atom : atoms) {
            if (!atom.sourcePath().equals(path) || atom.endOffset() > source.length()
                    || !atom.sha256().equals(ContractHashing.sha256(source.substring(
                            Math.toIntExact(atom.startOffset()), Math.toIntExact(atom.endOffset()))))) {
                throw new IllegalArgumentException("atom source mismatch");
            }
        }
        try {
            return new JavaFileComposition(path, source, atoms, Status.COMPLETE);
        } catch (IllegalArgumentException unsupported) {
            if (!"overlapping atoms".equals(unsupported.getMessage())) throw unsupported;
            return unsupported(path, source, Status.UNSUPPORTED_OVERLAP);
        }
    }

    static JavaFileComposition unsupported(String path, String source, Status status) {
        if (status == Status.COMPLETE) throw new IllegalArgumentException("unsupported status");
        return new JavaFileComposition(path, source, List.of(), status);
    }

    private void add(List<Segment> output, String source, int start, int end,
                     Kind kind, List<CodeAtom> group) {
        String text = source.substring(start, end);
        var identity = new ArrayList<>(List.of(sourcePath, sourceSha256,
                Integer.toString(output.size()), Integer.toString(start), Integer.toString(end), kind.name()));
        group.forEach(a -> identity.add(atomKey(a)));
        output.add(new Segment(output.size(), start, end, kind, group, text,
                framedHash("JAVA_FILE_SEGMENT/1", identity)));
    }

    private static String atomKey(CodeAtom atom) {
        return framedHash("JAVA_FILE_ATOM_OCCURRENCE/1", List.of(atom.sourcePath(), atom.owner(),
                atom.kind().name(), atom.name(), Long.toString(atom.startOffset()),
                Long.toString(atom.endOffset()), atom.sha256()));
    }

    private static String framedHash(String domain, List<String> values) {
        StringBuilder encoded = new StringBuilder(domain).append('\n');
        values.forEach(v -> encoded.append(v.length()).append(':').append(v));
        return ContractHashing.sha256(encoded.toString());
    }

    public String sourcePath() { return sourcePath; }
    public String sourceSha256() { return sourceSha256; }
    public Status status() { return status; }
    public List<CodeAtom> atoms() { return atoms; }
    public List<Segment> segments() { return segments; }
    public String root() { return root; }
    public boolean sourceMutationAuthority() { return false; }
    public boolean promotionAuthority() { return false; }

    /** Reassembles retained pieces in source order, including all unsupported/unclassified text. */
    public String reconstruct() {
        StringBuilder source = new StringBuilder();
        segments.forEach(s -> source.append(s.text()));
        String result = source.toString();
        if (!sourceSha256.equals(ContractHashing.sha256(result))) {
            throw new IllegalStateException("composition reconstruction mismatch");
        }
        return result;
    }
}
