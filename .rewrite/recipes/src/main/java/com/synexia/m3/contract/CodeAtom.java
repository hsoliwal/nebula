package com.synexia.m3.contract;

import java.util.Objects;

/**
 * Deterministic file-local source atom.
 *
 * <p>Offsets identify the exact source span; SHA-256 makes replacement/proof cheap without retaining
 * full source text in the inventory.</p>
 */
public record CodeAtom(
        String sourcePath,
        String owner,
        CodeAtomKind kind,
        String name,
        long startOffset,
        long endOffset,
        String sha256) {

    public CodeAtom {
        sourcePath = required(sourcePath, "sourcePath");
        owner = required(owner, "owner");
        kind = Objects.requireNonNull(kind, "kind");
        name = required(name, "name");
        sha256 = required(sha256, "sha256");
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("invalid source atom range");
        }
    }

    public long length() {
        return endOffset - startOffset;
    }

    private static String required(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}
