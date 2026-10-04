// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

/** Adapts the existing immutable problem catalog into the serial review input without editing it. */
public final class ProblemAdapterReviewBridge {
    private ProblemAdapterReviewBridge() {}

    public static List<SerialProblemReview.SourceFile> fromCatalog(List<ProblemAdapter> adapters) {
        Objects.requireNonNull(adapters, "adapters");
        return adapters.stream().map(ProblemAdapterReviewBridge::fromAdapter).toList();
    }

    public static SerialProblemReview.SourceFile fromAdapter(ProblemAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        CorpusSourceEntry source = adapter.source();
        String generatedAtom = ProblemAdapterSourceGenerator.sourceFor(adapter);
        String sourceId = adapter.id();
        return new SerialProblemReview.SourceFile(
                sourceId,
                source.platform(),
                source.repository(),
                source.commit(),
                source.path(),
                ProblemAdapterIndex.problemKey(adapter),
                reviewCategoryKey(source.platform(), adapter.classification().shape()),
                sha256(generatedAtom),
                SerialProblemReview.CONTRACT_SHA256,
                "");
    }

    /**
     * Platform-aware review key derived from the one canonical cross-site taxonomy.
     *
     * <p>The normalized category is resolved first from {@link CompetitiveProblemCategory}; the
     * platform catalogue then supplies the exact external key used by review coverage. Unsupported
     * platform/category combinations fail closed as {@code UNMAPPED} instead of inventing category
     * equivalence.</p>
     */
    public static String reviewCategoryKey(String platform, AlgorithmShape shape) {
        if (shape == null) return "UNMAPPED";
        CompetitiveProblemCategory category =
                CompetitiveCapabilityPlanIndex.canonical().require(shape).category();
        if (category == CompetitiveProblemCategory.OTHER) return "UNMAPPED";

        final CompetitiveProblemCategoryCatalog.Platform sourcePlatform;
        try {
            sourcePlatform =
                    CompetitiveProblemCategoryCatalog.Platform.valueOf(
                            Objects.requireNonNull(platform, "platform")
                                    .strip()
                                    .toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return "UNMAPPED";
        }

        if (!CompetitiveProblemCategoryCatalog.supports(sourcePlatform, category)) {
            return "UNMAPPED";
        }
        return CompetitiveProblemCategoryCatalog.require(sourcePlatform, category).key();
    }

    static String categoryFor(String platform, AlgorithmShape shape) {
        return reviewCategoryKey(platform, shape);
    }

    /**
     * Historical platform-neutral projection retained for package-local callers.
     *
     * <p>Legacy public review keys use SEARCHING and GRAPH while all other normalized category
     * names are already the canonical key spelling.</p>
     */
    static String categoryFor(AlgorithmShape shape) {
        if (shape == null) return "UNMAPPED";
        CompetitiveProblemCategory category =
                CompetitiveCapabilityPlanIndex.canonical().require(shape).category();
        return switch (category) {
            case SEARCH -> "SEARCHING";
            case GRAPHS -> "GRAPH";
            case OTHER -> "UNMAPPED";
            default -> category.name();
        };
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
