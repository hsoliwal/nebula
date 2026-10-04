// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Conservative policy for automatic promotion of donor source code into the Apache-2.0 Synexia tree.
 *
 * <p>This is a project admission policy, not a legal opinion. Non-permissive or unknown licenses
 * remain valid inputs for inspection, benchmarking, algorithm-shape discovery and clean-room
 * reimplementation, but they are not automatically admitted as source-code donors.
 */
public final class DonorLicensePromotionPolicy {

    public enum Decision {
        PERMISSIVE_AUTO_PROMOTION,
        RECIPROCAL_INSPECTION_ONLY,
        UNKNOWN_REVIEW_REQUIRED
    }

    public record Assessment(String declaredLicense, Decision decision, String rationale) {
        public Assessment {
            declaredLicense = Objects.toString(declaredLicense, "").strip();
            decision = Objects.requireNonNull(decision, "decision");
            rationale = Objects.requireNonNull(rationale, "rationale");
        }

        public boolean automaticCodePromotionAllowed() {
            return decision == Decision.PERMISSIVE_AUTO_PROMOTION;
        }
    }

    private static final Set<String> PERMISSIVE = Set.of(
            "MIT",
            "APACHE-2.0",
            "BSD-2-CLAUSE",
            "BSD-3-CLAUSE",
            "BSD-3-CLAUSE-CLEAR",
            "ISC",
            "0BSD",
            "UNLICENSE");

    private static final Set<String> RECIPROCAL_PREFIXES = Set.of(
            "GPL-",
            "AGPL-",
            "LGPL-",
            "MPL-",
            "EPL-",
            "CDDL-",
            "CC-BY-SA-");

    private DonorLicensePromotionPolicy() {}

    public static Assessment assess(String declaredLicense) {
        String raw = Objects.toString(declaredLicense, "").strip();
        if (raw.isEmpty() || raw.toUpperCase(Locale.ROOT).startsWith("UNVERIFIED")) {
            return new Assessment(
                    raw,
                    Decision.UNKNOWN_REVIEW_REQUIRED,
                    "license provenance is absent or unverified");
        }

        String normalized = raw.toUpperCase(Locale.ROOT);
        if (PERMISSIVE.contains(normalized)) {
            return new Assessment(
                    raw,
                    Decision.PERMISSIVE_AUTO_PROMOTION,
                    "reviewed permissive license may proceed to behavioral and benchmark proof");
        }

        for (String prefix : RECIPROCAL_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return new Assessment(
                        raw,
                        Decision.RECIPROCAL_INSPECTION_ONLY,
                        "reciprocal/share-alike source is inspection-only for automatic Apache-2.0 promotion");
            }
        }

        if (normalized.startsWith("CC-BY-")) {
            // Even non-SA Creative Commons licenses are not automatically treated as software-code
            // promotion licenses by this pipeline; require explicit review instead.
            return new Assessment(
                    raw,
                    Decision.UNKNOWN_REVIEW_REQUIRED,
                    "Creative Commons source requires explicit code-license review");
        }

        return new Assessment(
                raw,
                Decision.UNKNOWN_REVIEW_REQUIRED,
                "license is not on the automatic permissive allow-list");
    }
}
