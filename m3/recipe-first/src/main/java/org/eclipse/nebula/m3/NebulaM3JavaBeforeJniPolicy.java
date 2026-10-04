// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.Objects;

/** Fail-closed Java-before-JNI review policy; it grants no native execution authority. */
public final class NebulaM3JavaBeforeJniPolicy {
    public enum Decision {
        NO_NATIVE_ACTION,
        PROFILE_JAVA_HOT_PATH_BEFORE_JNI,
        REVIEW_EXISTING_NATIVE_DECLARATION
    }

    public record Review(
            Decision decision,
            String reason,
            boolean javaOracleRequired,
            boolean differentialCorpusRequired,
            boolean lifecycleFallbackRequired,
            boolean setupIncludedBenchmarkRequired,
            boolean nativeExecutionAuthority,
            boolean promotionAuthority) {
        public Review {
            decision = Objects.requireNonNull(decision, "decision");
            reason = Objects.requireNonNull(reason, "reason").strip();
            if (reason.isEmpty()) {
                throw new IllegalArgumentException("reason");
            }
            if (!javaOracleRequired || nativeExecutionAuthority || promotionAuthority) {
                throw new IllegalArgumentException(
                        "Nebula native review cannot bypass the Java oracle or gain authority");
            }
        }
    }

    private NebulaM3JavaBeforeJniPolicy() {
        throw new AssertionError("No instances");
    }

    public static Review review(NebulaM3InventoryRecipe.SourceFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.nativeMethodCount() > 0) {
            return candidate(
                    Decision.REVIEW_EXISTING_NATIVE_DECLARATION,
                    "EXISTING_NATIVE_DECLARATION_REQUIRES_PARITY_LIFECYCLE_FALLBACK_AND_BENCHMARK");
        }
        if (facts.loopCount() > 0 && !"NONE".equals(facts.fastSearchSignal())) {
            return candidate(
                    Decision.PROFILE_JAVA_HOT_PATH_BEFORE_JNI,
                    "SEARCH_LOOP_IS_ONLY_A_PROFILE_CANDIDATE_NOT_NATIVE_AUTHORITY");
        }
        return new Review(
                Decision.NO_NATIVE_ACTION,
                "NO_PROVEN_HOT_PRIMITIVE",
                true,
                false,
                false,
                false,
                false,
                false);
    }

    private static Review candidate(Decision decision, String reason) {
        return new Review(
                decision,
                reason,
                true,
                true,
                true,
                true,
                false,
                false);
    }
}
