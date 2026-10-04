// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Canonical M3 promotion gate vocabulary shared by problem and repository recipe work. */
public enum M3RecipeGate {
    SOURCE_REVIEW,
    RECIPE_DRY_RUN,
    SECOND_PASS_FIXED_POINT,
    API_CONTRACT,
    COMPATIBILITY,
    COMPILE,
    STATIC_ANALYSIS,
    UNIT_TEST,
    INTEGRATION_TEST,
    DIFFERENTIAL,
    JAVA_ORACLE,
    NATIVE_ABI,
    SERVICE_LIFECYCLE,
    SERIALIZATION_COMPATIBILITY,
    BENCHMARK;

    private static final Map<String, M3RecipeGate> ALIASES = Map.ofEntries(
            Map.entry("BALANCED_CHALLENGE_EVIDENCE", SOURCE_REVIEW),
            Map.entry("PINNED_DONOR_PROVENANCE", SOURCE_REVIEW),
            Map.entry("SERIAL_DONOR_FILE_REVIEW", SOURCE_REVIEW),
            Map.entry("SERIAL_DONOR_ATOM_REVIEW", SOURCE_REVIEW),
            Map.entry("RUNTIME_IF_REQUIRED", SERVICE_LIFECYCLE),
            Map.entry("COMPILER", COMPILE),
            Map.entry("FORMAT_AND_STATIC_ANALYSIS", STATIC_ANALYSIS),
            Map.entry("UNIT_TESTS", UNIT_TEST),
            Map.entry("INTEGRATION_TESTS", INTEGRATION_TEST),
            Map.entry("API_AND_CONTRACT_TESTS", API_CONTRACT),
            Map.entry("DIFFERENTIAL_PROOF", DIFFERENTIAL),
            Map.entry("NATIVE_PARITY_IF_DECLARED", NATIVE_ABI),
            Map.entry("BENCHMARK_IF_PERFORMANCE_CLAIM", BENCHMARK));

    public static M3RecipeGate parse(String value) {
        String key = Objects.requireNonNull(value, "value").strip().toUpperCase(Locale.ROOT);
        if (key.isEmpty() || key.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("gate");
        }
        M3RecipeGate alias = ALIASES.get(key);
        if (alias != null) return alias;
        return valueOf(key);
    }
}
