// SPDX-License-Identifier: Apache-2.0
package com.synexia.chrome2api;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Immutable, content-addressed candidate plan emitted to the existing Chrome2api backend. */
public record Chrome2ApiRecipePlan(
        String schema,
        String taskId,
        String capabilityId,
        Mode mode,
        String recipeClassName,
        List<String> targetPaths,
        List<String> atomIds,
        List<String> donorRepositories,
        String requirementsSha256,
        boolean canonicalMutationAuthority,
        boolean promotionAuthority) {

    public static final String SCHEMA = "SYNEXIA_CHROME2API_RECIPE_PLAN_V1";

    public Chrome2ApiRecipePlan {
        schema = required(schema, "schema");
        if (!SCHEMA.equals(schema)) throw new IllegalArgumentException("unsupported schema");
        taskId = required(taskId, "taskId");
        capabilityId = required(capabilityId, "capabilityId");
        mode = Objects.requireNonNull(mode, "mode");
        recipeClassName = required(recipeClassName, "recipeClassName");
        targetPaths = stable(targetPaths, "targetPaths");
        atomIds = stable(atomIds, "atomIds");
        donorRepositories = stable(donorRepositories, "donorRepositories");
        requirementsSha256 = sha256(requirementsSha256);
        if (canonicalMutationAuthority || promotionAuthority) {
            throw new IllegalArgumentException("Chrome2api plans cannot grant authority");
        }
    }

    public enum Mode {
        REUSE_OR_COMPOSE,
        CREATE_OR_IMPROVE
    }

    public boolean candidateOnly() {
        return !canonicalMutationAuthority && !promotionAuthority;
    }

    private static List<String> stable(List<String> values, String field) {
        Objects.requireNonNull(values, field);
        return values.stream()
                .map(value -> required(value, field + " entry"))
                .distinct()
                .sorted()
                .toList();
    }

    private static String sha256(String value) {
        String normalized = required(value, "requirementsSha256").toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("requirementsSha256 must be a 64-hex SHA-256");
        }
        return normalized;
    }

    private static String required(String value, String field) {
        String normalized = Objects.toString(value, "").strip();
        if (normalized.isEmpty() || normalized.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return normalized;
    }
}
