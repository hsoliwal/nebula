package com.synexia.m3.contract;

import java.util.Objects;

/** One normalized source-level API contract atom. */
public record ApiMember(
        String sourcePath,
        String owner,
        ApiMemberKind kind,
        ApiVisibility visibility,
        String signature) {

    public ApiMember {
        sourcePath = required(sourcePath, "sourcePath");
        owner = required(owner, "owner");
        kind = Objects.requireNonNull(kind, "kind");
        visibility = Objects.requireNonNull(visibility, "visibility");
        signature = normalize(required(signature, "signature"));
    }

    public String fingerprint() {
        return ContractHashing.sha256(
                owner + "\n" + kind + "\n" + visibility + "\n" + signature);
    }

    public boolean externalContract() {
        return visibility == ApiVisibility.PUBLIC || visibility == ApiVisibility.PROTECTED;
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String required(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}
