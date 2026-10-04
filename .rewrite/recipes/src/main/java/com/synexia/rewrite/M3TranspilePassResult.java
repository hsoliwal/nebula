// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Immutable evidence emitted after one file-local OpenRewrite pass. */
public record M3TranspilePassResult(
        int ordinal,
        String passId,
        String recipeName,
        String sourcePath,
        String beforeSha256,
        String afterSha256,
        boolean changed) {

    public M3TranspilePassResult {
        if (ordinal < 1) {
            throw new IllegalArgumentException("ordinal must be positive");
        }
        passId = text(passId, "passId");
        recipeName = text(recipeName, "recipeName");
        sourcePath = text(sourcePath, "sourcePath");
        beforeSha256 = hash(beforeSha256, "beforeSha256");
        afterSha256 = hash(afterSha256, "afterSha256");
        if (changed == beforeSha256.equals(afterSha256)) {
            throw new IllegalArgumentException("changed flag must match content hashes");
        }
    }

    private static String text(String value, String field) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return value.strip();
    }

    private static String hash(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }
}
