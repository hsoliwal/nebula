// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.corpus.ChallengeBalancedEvidence;
import com.synexia.algorithms.corpus.ChallengeCategoryCapabilityIndex;
import com.synexia.algorithms.corpus.ChallengeCategoryCatalog;
import com.synexia.algorithms.corpus.ChallengePlatform;
import com.synexia.algorithms.corpus.ChallengeSearchIndex;
import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Balanced platform-local queries over the one canonical challenge posting index. */
final class M3PlatformScopedCategorySearch {
    record Result(List<ChallengeSearchIndex.Hit> hits, String root) {
        Result {
            hits = List.copyOf(Objects.requireNonNull(hits, "hits"));
            if (root == null || !root.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("search root");
            }
        }
    }

    private static final List<ChallengePlatform> PLATFORMS =
            List.of(
                    ChallengePlatform.LEETCODE,
                    ChallengePlatform.HACKERRANK,
                    ChallengePlatform.GEEKSFORGEEKS);

    private M3PlatformScopedCategorySearch() {}

    static Result evaluate(
            String shape, String category, String term, String repository, int limit) {
        String checkedCategory = Objects.requireNonNull(category, "category").strip();
        if (checkedCategory.isEmpty() || limit < 1 || limit > 100_000) {
            throw new IllegalArgumentException("category or limit");
        }
        AlgorithmShape requested =
                Objects.requireNonNull(shape, "shape").equals("ALL")
                        ? null
                        : AlgorithmShape.valueOf(shape);
        String checkedTerm = Objects.requireNonNull(term, "term");
        String checkedRepository = Objects.requireNonNull(repository, "repository");
        List<String> evidence = new ArrayList<>();
        evidence.add("M3_PLATFORM_SCOPED_CATEGORY_SEARCH_V3");
        evidence.add(ChallengeCategoryCatalog.root());
        evidence.add(ChallengeCategoryCapabilityIndex.canonical().root());
        evidence.add(checkedCategory.toLowerCase(Locale.ROOT));
        evidence.add(shape);
        evidence.add(checkedTerm);
        evidence.add(checkedRepository);
        evidence.add(Integer.toString(limit));

        EnumMap<ChallengePlatform, List<ChallengeSearchIndex.Hit>> local =
                new EnumMap<>(ChallengePlatform.class);
        int matchedCategories = 0;
        int supportedPlatforms = 0;
        ChallengeCategoryCapabilityIndex categoryIndex =
                ChallengeCategoryCapabilityIndex.canonical();
        for (ChallengePlatform platform : PLATFORMS) {
            checkCanceled();
            var found = categoryIndex.find(platform, checkedCategory);
            if (found.isEmpty()) continue;
            matchedCategories++;
            ChallengeCategoryCapabilityIndex.Row indexed = found.orElseThrow();
            ChallengeCategoryCatalog.Category row = indexed.category();
            if (requested != null && !indexed.capabilityMask().contains(requested)) continue;
            supportedPlatforms++;
            Set<AlgorithmShape> shapes =
                    requested == null
                            ? Set.copyOf(indexed.capabilityMask().shapes())
                            : Set.of(requested);
            if (shapes.isEmpty()) {
                evidence.add(platform.name() + "|" + row.id() + "|NO_SHAPES");
                local.put(platform, List.of());
                continue;
            }
            ChallengeSearchIndex.Evaluation evaluation =
                    evaluatePlatform(
                            platform,
                            shapes,
                            checkedTerm,
                            checkedRepository,
                            limit);
            evidence.add(platform.name() + "|" + row.id() + "|" + evaluation.root());
            local.put(platform, evaluation.hits());
        }
        if (matchedCategories == 0) {
            throw new IllegalArgumentException("unknown challenge category: " + category);
        }
        if (supportedPlatforms == 0) {
            throw new IllegalArgumentException("shape is not mapped by selected category");
        }
        List<ChallengeSearchIndex.Hit> hits =
                ChallengeBalancedEvidence.balance(local, limit);
        addBalanceEvidence(evidence, local, hits, limit);
        return new Result(hits, digest(evidence));
    }

    static Result evaluateShape(
            String shape, String term, String repository, int limit) {
        if (limit < 1 || limit > 100_000) {
            throw new IllegalArgumentException("limit");
        }
        AlgorithmShape requested = AlgorithmShape.valueOf(Objects.requireNonNull(shape, "shape"));
        String checkedTerm = Objects.requireNonNull(term, "term");
        String checkedRepository = Objects.requireNonNull(repository, "repository");
        List<String> evidence = new ArrayList<>();
        evidence.add("M3_PLATFORM_SCOPED_SHAPE_SEARCH_V1");
        evidence.add(requested.name());
        evidence.add(checkedTerm);
        evidence.add(checkedRepository);
        evidence.add(Integer.toString(limit));

        EnumMap<ChallengePlatform, List<ChallengeSearchIndex.Hit>> local =
                new EnumMap<>(ChallengePlatform.class);
        for (ChallengePlatform platform : PLATFORMS) {
            checkCanceled();
            ChallengeSearchIndex.Evaluation evaluation =
                    evaluatePlatform(
                            platform,
                            Set.of(requested),
                            checkedTerm,
                            checkedRepository,
                            limit);
            evidence.add(platform.name() + "|" + evaluation.root());
            local.put(platform, evaluation.hits());
        }
        List<ChallengeSearchIndex.Hit> hits =
                ChallengeBalancedEvidence.balance(local, limit);
        addBalanceEvidence(evidence, local, hits, limit);
        return new Result(hits, digest(evidence));
    }

    private static ChallengeSearchIndex.Evaluation evaluatePlatform(
            ChallengePlatform platform,
            Set<AlgorithmShape> shapes,
            String term,
            String repository,
            int limit) {
        ChallengeSearchIndex.Query query =
                new ChallengeSearchIndex.Query(
                        Set.of(platform),
                        shapes,
                        Set.of(),
                        Set.of(),
                        term.isEmpty() ? List.of() : List.of(term),
                        repository.isEmpty() ? List.of() : List.of(repository),
                        limit);
        ChallengeSearchIndex.Evaluation evaluation =
                ChallengeSearchIndex.canonical().evaluate(query);
        checkCanceled();
        return evaluation;
    }

    private static void addBalanceEvidence(
            List<String> evidence,
            Map<ChallengePlatform, List<ChallengeSearchIndex.Hit>> local,
            List<ChallengeSearchIndex.Hit> selected,
            int limit) {
        long availablePlatforms =
                PLATFORMS.stream()
                        .filter(platform -> !local.getOrDefault(platform, List.of()).isEmpty())
                        .count();
        long representedPlatforms =
                selected.stream()
                        .map(hit -> hit.problem().id().platform())
                        .distinct()
                        .count();
        long required = Math.min((long) limit, availablePlatforms);
        if (representedPlatforms < required) {
            throw new IllegalStateException("balanced challenge search failed source-spread invariant");
        }
        evidence.add("AVAILABLE_PLATFORMS=" + availablePlatforms);
        evidence.add("REPRESENTED_PLATFORMS=" + representedPlatforms);
        for (ChallengeSearchIndex.Hit hit : selected) {
            evidence.add("H|" + hit.problem().id().stableId());
        }
    }

    private static void checkCanceled() {
        if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException(
                    "M3 challenge search interrupted");
        }
    }

    private static String digest(List<String> values) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                hash.update((byte) (bytes.length >>> 24));
                hash.update((byte) (bytes.length >>> 16));
                hash.update((byte) (bytes.length >>> 8));
                hash.update((byte) bytes.length);
                hash.update(bytes);
            }
            return HexFormat.of().formatHex(hash.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
