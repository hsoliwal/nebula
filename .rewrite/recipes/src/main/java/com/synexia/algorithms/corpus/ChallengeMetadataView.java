// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Normalized metadata-only view over the existing pinned challenge/catalogue owners.
 *
 * <p>This class does not fetch or copy problem statements or solutions. Difficulty and complexity
 * are explicit UNKNOWN values until independently pinned metadata is added; challenge donor source
 * remains INVENTORY_ONLY evidence and never receives replacement authority.</p>
 */
public final class ChallengeMetadataView {
    public enum Difficulty { UNKNOWN, EASY, MEDIUM, HARD }
    public enum Complexity {
        UNKNOWN, CONSTANT, LOGARITHMIC, LINEAR, N_LOG_N, QUADRATIC, POLYNOMIAL, EXPONENTIAL
    }
    public enum DonorDecision { INVENTORY_ONLY, REUSE, WRAP, ADAPT, PORT, NEW }

    public record Entry(
            String stableId,
            String title,
            ChallengePlatform platform,
            ChallengeProblem.Status status,
            String algorithmShape,
            List<String> categoryIds,
            List<String> donorRepositories,
            String language,
            Difficulty difficulty,
            Complexity timeComplexity,
            Complexity spaceComplexity,
            DonorDecision donorDecision,
            String root) {
        public Entry {
            stableId = text(stableId, "stableId");
            title = text(title, "title");
            platform = Objects.requireNonNull(platform, "platform");
            status = Objects.requireNonNull(status, "status");
            algorithmShape = Objects.requireNonNullElse(algorithmShape, "").strip();
            categoryIds = stable(categoryIds, "categoryIds");
            donorRepositories = stable(donorRepositories, "donorRepositories");
            language = text(language, "language");
            difficulty = Objects.requireNonNull(difficulty, "difficulty");
            timeComplexity = Objects.requireNonNull(timeComplexity, "timeComplexity");
            spaceComplexity = Objects.requireNonNull(spaceComplexity, "spaceComplexity");
            donorDecision = Objects.requireNonNull(donorDecision, "donorDecision");

            CatalogueDigest digest = new CatalogueDigest("SYNEXIA_CHALLENGE_METADATA_V1")
                    .text(stableId)
                    .text(title)
                    .text(platform.name())
                    .text(status.name())
                    .text(algorithmShape)
                    .text(language)
                    .text(difficulty.name())
                    .text(timeComplexity.name())
                    .text(spaceComplexity.name())
                    .text(donorDecision.name());
            categoryIds.forEach(digest::text);
            donorRepositories.forEach(digest::text);
            String expected = digest.finish();
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) throw new IllegalArgumentException("challenge metadata root mismatch");
        }

        public boolean sourceCopyAuthority() { return false; }
        public boolean replacementAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    private static final List<Entry> ENTRIES = build();
    private static final Map<String, Entry> BY_ID = byId();
    private static final String ROOT = root(ENTRIES);

    private ChallengeMetadataView() {}

    public static List<Entry> all() {
        return ENTRIES;
    }

    public static Entry require(String stableId) {
        Entry value = BY_ID.get(Objects.requireNonNull(stableId, "stableId"));
        if (value == null) throw new IllegalArgumentException("unknown challenge metadata: " + stableId);
        return value;
    }

    public static String root() {
        return ROOT;
    }

    /** Fast category lookup reuses the canonical precomputed shape/platform posting index. */
    public static List<Entry> byCategory(
            ChallengePlatform platform, String category, int limit) {
        if (limit < 1 || limit > 100_000) throw new IllegalArgumentException("limit");
        ChallengeCategoryCatalog.Category resolved =
                ChallengeCategoryCatalog.find(
                                Objects.requireNonNull(platform, "platform"),
                                Objects.requireNonNull(category, "category"))
                        .orElse(null);
        if (resolved == null || resolved.shapes().isEmpty()) return List.of();
        ChallengeSearchIndex.Query query = new ChallengeSearchIndex.Query(
                Set.of(platform),
                Set.copyOf(resolved.shapes()),
                Set.of(),
                Set.of(),
                List.of(),
                List.of(),
                limit);
        return metadata(ChallengeSearchIndex.canonical().search(query));
    }

    public static List<Entry> byShape(AlgorithmShape shape, int limit) {
        if (limit < 1 || limit > 100_000) throw new IllegalArgumentException("limit");
        ChallengeSearchIndex.Query query = new ChallengeSearchIndex.Query(
                Set.of(),
                Set.of(Objects.requireNonNull(shape, "shape")),
                Set.of(),
                Set.of(),
                List.of(),
                List.of(),
                limit);
        return metadata(ChallengeSearchIndex.canonical().search(query));
    }

    public static List<Entry> searchTerms(List<String> terms, int limit) {
        if (limit < 1 || limit > 100_000) throw new IllegalArgumentException("limit");
        ChallengeSearchIndex.Query query = new ChallengeSearchIndex.Query(
                Set.of(), Set.of(), Set.of(), Set.of(),
                List.copyOf(Objects.requireNonNull(terms, "terms")),
                List.of(), limit);
        return metadata(ChallengeSearchIndex.canonical().search(query));
    }

    private static List<Entry> metadata(List<ChallengeSearchIndex.Hit> hits) {
        ArrayList<Entry> result = new ArrayList<>(hits.size());
        for (ChallengeSearchIndex.Hit hit : hits) {
            result.add(require(hit.problem().id().stableId()));
        }
        return List.copyOf(result);
    }

    private static List<Entry> build() {
        ArrayList<Entry> result = new ArrayList<>();
        for (ChallengeProblem problem : ChallengeCatalog.all()) {
            String shape = problem.shape().map(Enum::name).orElse("");
            List<String> categories = problem.shape()
                    .map(value -> ChallengeCategoryCatalog.forShape(value).stream()
                            .filter(category -> category.platform() == problem.id().platform())
                            .map(ChallengeCategoryCatalog.Category::id)
                            .sorted()
                            .toList())
                    .orElse(List.of());
            TreeSet<String> repositories = new TreeSet<>();
            for (CorpusSourceEntry source : problem.sources()) repositories.add(source.repository());
            result.add(new Entry(
                    problem.id().stableId(),
                    problem.title(),
                    problem.id().platform(),
                    problem.status(),
                    shape,
                    categories,
                    List.copyOf(repositories),
                    "JAVA",
                    Difficulty.UNKNOWN,
                    Complexity.UNKNOWN,
                    Complexity.UNKNOWN,
                    DonorDecision.INVENTORY_ONLY,
                    ""));
        }
        result.sort(Comparator.comparing(Entry::stableId));
        return List.copyOf(result);
    }

    private static Map<String, Entry> byId() {
        LinkedHashMap<String, Entry> result = new LinkedHashMap<>();
        for (Entry entry : ENTRIES) {
            if (result.putIfAbsent(entry.stableId(), entry) != null) {
                throw new ExceptionInInitializerError("duplicate challenge metadata id " + entry.stableId());
            }
        }
        return Map.copyOf(result);
    }

    private static String root(List<Entry> entries) {
        CatalogueDigest digest = new CatalogueDigest("SYNEXIA_CHALLENGE_METADATA_CATALOG_V1");
        entries.forEach(entry -> digest.text(entry.root()));
        return digest.finish();
    }

    private static List<String> stable(List<String> values, String field) {
        TreeSet<String> result = new TreeSet<>();
        for (String value : Objects.requireNonNull(values, field)) result.add(text(value, field));
        return List.copyOf(result);
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }
}
