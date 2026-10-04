// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.api;

import com.synexia.algorithms.corpus.ChallengeCategoryCatalog;
import com.synexia.algorithms.corpus.ChallengePlatform;
import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Precompute-first metadata search over the canonical LeetCode/HackerRank/GeeksforGeeks taxonomy.
 *
 * <p>Exact and prefix queries execute entirely on frozen alias geometry. Only aliases that survive
 * the conservative edit-distance length bound enter the Java/JNI parity-verified residual lane.
 * Results nominate categories and algorithm shapes for donor review/recipe work; they never grant
 * challenge-body copy authority or target-source mutation authority.</p>
 */
public final class ChallengeCategoryFastSearch {
    private static final double MIN_FUZZY_SCORE = 0.45d;
    private static final CompiledAliasIndex INDEX =
            CompiledAliasIndex.compile(ChallengeCategoryCatalog.categories());

    public record Hit(
            ChallengePlatform platform,
            String categoryId,
            String displayName,
            List<AlgorithmShape> shapes,
            String taxonomySource,
            double score,
            int editDistance,
            NativeSearch.Backend backend,
            String evidenceSha256) {
        public Hit {
            platform = Objects.requireNonNull(platform, "platform");
            categoryId = Objects.requireNonNull(categoryId, "categoryId");
            displayName = Objects.requireNonNull(displayName, "displayName");
            shapes = List.copyOf(Objects.requireNonNull(shapes, "shapes"));
            taxonomySource = Objects.requireNonNull(taxonomySource, "taxonomySource");
            backend = Objects.requireNonNull(backend, "backend");
            if (evidenceSha256 == null || !evidenceSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("evidenceSha256");
            }
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean mutationAuthority() {
            return false;
        }
    }

    public record IndexSnapshot(
            int categories,
            int aliases,
            int lengthBuckets,
            String categoryRoot,
            String root) {
        public IndexSnapshot {
            if (categories < 1 || aliases < categories || lengthBuckets < 1) {
                throw new IllegalArgumentException("invalid category index counts");
            }
            if (categoryRoot == null || !categoryRoot.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("categoryRoot");
            }
            if (root == null || !root.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("root");
            }
        }
    }

    public IndexSnapshot snapshot() {
        return INDEX.snapshot();
    }

    public List<Hit> search(String platform, String query, int limit) {
        if (limit < 1 || limit > 1_000) {
            throw new IllegalArgumentException("limit must be in [1,1000]");
        }
        String normalized = FuzzyScorer.normalize(query);
        if (normalized.isEmpty()) {
            return List.of();
        }

        ChallengePlatform requested = parsePlatform(platform);
        TreeMap<String, Hit> best = new TreeMap<>();
        List<AliasEntry> exact = INDEX.exact(requested, normalized);
        for (AliasEntry entry : exact) {
            accept(best, entry, 1.0d, 0, NativeSearch.Backend.JAVA,
                    evidence("EXACT", normalized, List.of(entry)));
        }
        if (!exact.isEmpty()) {
            return ranked(best, limit);
        }

        List<AliasEntry> prefix = INDEX.prefix(requested, normalized);
        for (AliasEntry entry : prefix) {
            int distance = entry.normalizedAlias().length() - normalized.length();
            double edit = editScore(normalized.length(), entry.normalizedAlias().length(), distance);
            accept(best, entry, Math.max(0.90d, edit), distance, NativeSearch.Backend.JAVA,
                    evidence("PREFIX", normalized, List.of(entry)));
        }
        if (best.size() >= limit) {
            return ranked(best, limit);
        }

        Set<String> alreadyScored = new HashSet<>();
        prefix.forEach(entry -> alreadyScored.add(entry.key()));
        List<AliasEntry> residual = INDEX.residual(requested, normalized, alreadyScored);
        if (!residual.isEmpty()) {
            String[] labels = residual.stream().map(AliasEntry::normalizedAlias).toArray(String[]::new);
            NativeSearch.VerifiedBatch batch =
                    NativeSearch.verifiedLevenshteinBatch(normalized, labels);
            int[] distances = batch.distances();
            for (int i = 0; i < residual.size(); i++) {
                AliasEntry entry = residual.get(i);
                int distance = distances[i];
                double score =
                        editScore(normalized.length(), entry.normalizedAlias().length(), distance);
                if (score < MIN_FUZZY_SCORE) {
                    continue;
                }
                accept(best, entry, score, distance, batch.backend(), batch.evidenceSha256());
            }
        }
        return ranked(best, limit);
    }

    private static void accept(
            Map<String, Hit> best,
            AliasEntry entry,
            double score,
            int distance,
            NativeSearch.Backend backend,
            String evidence) {
        ChallengeCategoryCatalog.Category row = entry.category();
        Hit candidate = new Hit(
                row.platform(),
                row.id(),
                row.displayName(),
                row.shapes(),
                row.source(),
                score,
                distance,
                backend,
                evidence);
        String key = row.platform().name() + ":" + row.id();
        Hit previous = best.get(key);
        if (previous == null || better(candidate, previous)) {
            best.put(key, candidate);
        }
    }

    private static List<Hit> ranked(Map<String, Hit> best, int limit) {
        return best.values().stream()
                .sorted(Comparator.comparingDouble(Hit::score).reversed()
                        .thenComparingInt(Hit::editDistance)
                        .thenComparing(hit -> hit.platform().name())
                        .thenComparing(Hit::categoryId))
                .limit(limit)
                .toList();
    }

    private static double editScore(int queryLength, int candidateLength, int distance) {
        return Math.max(
                0.0d,
                1.0d - ((double) distance / Math.max(1, Math.max(queryLength, candidateLength))));
    }

    private static boolean better(Hit left, Hit right) {
        int score = Double.compare(left.score(), right.score());
        return score > 0 || (score == 0 && left.editDistance() < right.editDistance());
    }

    private static ChallengePlatform parsePlatform(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("ALL")) {
            return null;
        }
        ChallengePlatform platform = ChallengePlatform.from(value);
        if (platform == ChallengePlatform.OTHER) {
            throw new IllegalArgumentException("unsupported challenge platform: " + value);
        }
        return platform;
    }

    private static String evidence(String lane, String query, List<AliasEntry> entries) {
        MessageDigest digest = sha256();
        frame(digest, "SYNEXIA_CHALLENGE_CATEGORY_FAST_SEARCH_V2");
        frame(digest, ChallengeCategoryCatalog.root());
        frame(digest, lane);
        frame(digest, query);
        for (AliasEntry entry : entries) {
            frame(digest, entry.category().platform().name());
            frame(digest, entry.category().id());
            frame(digest, entry.normalizedAlias());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private record AliasEntry(
            ChallengeCategoryCatalog.Category category,
            String alias,
            String normalizedAlias) {
        AliasEntry {
            category = Objects.requireNonNull(category, "category");
            alias = Objects.requireNonNull(alias, "alias");
            normalizedAlias = Objects.requireNonNull(normalizedAlias, "normalizedAlias");
            if (normalizedAlias.isEmpty()) {
                throw new IllegalArgumentException("normalizedAlias");
            }
        }

        String key() {
            return category.platform().name() + ":" + category.id() + ":" + normalizedAlias;
        }
    }

    private static final class CompiledAliasIndex {
        private final Map<ChallengePlatform, Map<String, List<AliasEntry>>> exact;
        private final Map<ChallengePlatform, NavigableMap<String, List<AliasEntry>>> ordered;
        private final Map<ChallengePlatform, NavigableMap<Integer, List<AliasEntry>>> byLength;
        private final IndexSnapshot snapshot;

        private CompiledAliasIndex(
                Map<ChallengePlatform, Map<String, List<AliasEntry>>> exact,
                Map<ChallengePlatform, NavigableMap<String, List<AliasEntry>>> ordered,
                Map<ChallengePlatform, NavigableMap<Integer, List<AliasEntry>>> byLength,
                IndexSnapshot snapshot) {
            this.exact = exact;
            this.ordered = ordered;
            this.byLength = byLength;
            this.snapshot = snapshot;
        }

        static CompiledAliasIndex compile(List<ChallengeCategoryCatalog.Category> categories) {
            EnumMap<ChallengePlatform, Map<String, List<AliasEntry>>> exact =
                    new EnumMap<>(ChallengePlatform.class);
            EnumMap<ChallengePlatform, NavigableMap<String, List<AliasEntry>>> ordered =
                    new EnumMap<>(ChallengePlatform.class);
            EnumMap<ChallengePlatform, NavigableMap<Integer, List<AliasEntry>>> lengths =
                    new EnumMap<>(ChallengePlatform.class);
            int aliasCount = 0;
            int bucketCount = 0;

            for (ChallengePlatform platform : ChallengePlatform.values()) {
                if (platform == ChallengePlatform.OTHER) {
                    continue;
                }
                TreeMap<String, List<AliasEntry>> exactRows = new TreeMap<>();
                TreeMap<String, List<AliasEntry>> orderedRows = new TreeMap<>();
                TreeMap<Integer, List<AliasEntry>> lengthRows = new TreeMap<>();
                for (ChallengeCategoryCatalog.Category category : categories) {
                    if (category.platform() != platform) {
                        continue;
                    }
                    for (String alias : category.aliases()) {
                        String normalized = FuzzyScorer.normalize(alias);
                        if (normalized.isEmpty()) {
                            continue;
                        }
                        AliasEntry entry = new AliasEntry(category, alias, normalized);
                        exactRows.computeIfAbsent(normalized, ignored -> new ArrayList<>()).add(entry);
                        orderedRows.computeIfAbsent(normalized, ignored -> new ArrayList<>()).add(entry);
                        lengthRows.computeIfAbsent(normalized.length(), ignored -> new ArrayList<>())
                                .add(entry);
                        aliasCount++;
                    }
                }
                freezeLists(exactRows);
                freezeLists(orderedRows);
                freezeLists(lengthRows);
                exact.put(platform, Collections.unmodifiableMap(exactRows));
                ordered.put(platform, Collections.unmodifiableNavigableMap(orderedRows));
                lengths.put(platform, Collections.unmodifiableNavigableMap(lengthRows));
                bucketCount += lengthRows.size();
            }

            String root = indexRoot(categories, aliasCount, bucketCount);
            return new CompiledAliasIndex(
                    Collections.unmodifiableMap(exact),
                    Collections.unmodifiableMap(ordered),
                    Collections.unmodifiableMap(lengths),
                    new IndexSnapshot(
                            categories.size(),
                            aliasCount,
                            bucketCount,
                            ChallengeCategoryCatalog.root(),
                            root));
        }

        IndexSnapshot snapshot() {
            return snapshot;
        }

        List<AliasEntry> exact(ChallengePlatform requested, String query) {
            ArrayList<AliasEntry> result = new ArrayList<>();
            for (ChallengePlatform platform : platforms(requested)) {
                result.addAll(exact.get(platform).getOrDefault(query, List.of()));
            }
            result.sort(aliasOrder());
            return List.copyOf(result);
        }

        List<AliasEntry> prefix(ChallengePlatform requested, String query) {
            ArrayList<AliasEntry> result = new ArrayList<>();
            for (ChallengePlatform platform : platforms(requested)) {
                NavigableMap<String, List<AliasEntry>> map = ordered.get(platform);
                for (Map.Entry<String, List<AliasEntry>> entry :
                        map.tailMap(query, true).entrySet()) {
                    if (!entry.getKey().startsWith(query)) {
                        break;
                    }
                    result.addAll(entry.getValue());
                }
            }
            result.sort(aliasOrder());
            return List.copyOf(result);
        }

        List<AliasEntry> residual(
                ChallengePlatform requested, String query, Set<String> excluded) {
            ArrayList<AliasEntry> result = new ArrayList<>();
            int queryLength = query.length();
            for (ChallengePlatform platform : platforms(requested)) {
                for (Map.Entry<Integer, List<AliasEntry>> bucket :
                        byLength.get(platform).entrySet()) {
                    if (!lengthCanReachThreshold(queryLength, bucket.getKey())) {
                        continue;
                    }
                    for (AliasEntry entry : bucket.getValue()) {
                        if (!excluded.contains(entry.key())) {
                            result.add(entry);
                        }
                    }
                }
            }
            result.sort(aliasOrder());
            return List.copyOf(result);
        }

        private static boolean lengthCanReachThreshold(int queryLength, int candidateLength) {
            int maximum = Math.max(queryLength, candidateLength);
            int permittedDistance =
                    (int) Math.floor((1.0d - MIN_FUZZY_SCORE) * maximum);
            return Math.abs(queryLength - candidateLength) <= permittedDistance;
        }

        private static List<ChallengePlatform> platforms(ChallengePlatform requested) {
            return requested == null
                    ? List.of(
                            ChallengePlatform.LEETCODE,
                            ChallengePlatform.HACKERRANK,
                            ChallengePlatform.GEEKSFORGEEKS)
                    : List.of(requested);
        }

        private static Comparator<AliasEntry> aliasOrder() {
            return Comparator.comparing((AliasEntry entry) -> entry.category().platform().name())
                    .thenComparing(entry -> entry.category().id())
                    .thenComparing(AliasEntry::normalizedAlias)
                    .thenComparing(AliasEntry::alias);
        }

        private static <K extends Comparable<? super K>> void freezeLists(
                Map<K, List<AliasEntry>> values) {
            values.replaceAll((ignored, rows) -> {
                ArrayList<AliasEntry> stable = new ArrayList<>(rows);
                stable.sort(aliasOrder());
                return List.copyOf(stable);
            });
        }

        private static String indexRoot(
                List<ChallengeCategoryCatalog.Category> categories,
                int aliases,
                int buckets) {
            MessageDigest digest = sha256();
            frame(digest, "SYNEXIA_CHALLENGE_CATEGORY_ALIAS_INDEX_V1");
            frame(digest, ChallengeCategoryCatalog.root());
            frame(digest, Integer.toString(categories.size()));
            frame(digest, Integer.toString(aliases));
            frame(digest, Integer.toString(buckets));
            for (ChallengeCategoryCatalog.Category category : categories) {
                frame(digest, category.platform().name());
                frame(digest, category.id());
                for (String alias : category.aliases()) {
                    frame(digest, FuzzyScorer.normalize(alias));
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        }
    }
}
