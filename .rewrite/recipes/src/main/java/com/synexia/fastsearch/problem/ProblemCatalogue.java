// SPDX-License-Identifier: Apache-2.0
package com.synexia.fastsearch.problem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable bitmap index for problem metadata.
 *
 * <p>Token constraints intersect. Category constraints are ORed together and then intersected with
 * the text result. Optional source constraints are a precomputed bitmap intersection. Returned order
 * is stable by source, external id and title.</p>
 */
public final class ProblemCatalogue {
    private static final Pattern TOKEN_BREAK = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final List<ProblemDescriptor> entries;
    private final Map<String, BitSet> tokenIndex;
    private final PrefixNode prefixIndex;
    private final Map<Integer, List<IndexedToken>> fuzzyVocabulary;
    private final Map<ProblemCategory, BitSet> categoryIndex;
    private final Map<ProblemSource, BitSet> sourceIndex;
    private final String root;

    private record IndexedToken(int[] codePoints, BitSet postings) {}

    /** Match mechanics captured by an immutable prepared metadata query. */
    public enum MatchMode {
        EXACT,
        PREFIX
    }

    /**
     * Immutable candidate bitmap bound to one catalogue content root.
     *
     * <p>The bitmap is never exposed. A prepared query may therefore be safely reused across
     * result limits without redoing source/category/token intersections. It grants no donor-source
     * or replacement authority.</p>
     */
    public static final class PreparedQuery {
        private final String catalogueRoot;
        private final MatchMode mode;
        private final ProblemSource source;
        private final List<ProblemCategory> categories;
        private final List<String> tokens;
        private final BitSet candidates;
        private final String root;

        private PreparedQuery(
                String catalogueRoot,
                MatchMode mode,
                ProblemSource source,
                Set<ProblemCategory> categories,
                List<String> tokens,
                BitSet candidates) {
            if (catalogueRoot == null || !catalogueRoot.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("catalogueRoot");
            }
            this.catalogueRoot = catalogueRoot;
            this.mode = Objects.requireNonNull(mode, "mode");
            this.source = source;
            EnumSet<ProblemCategory> stableCategories = EnumSet.noneOf(ProblemCategory.class);
            if (categories != null) stableCategories.addAll(categories);
            this.categories = List.copyOf(stableCategories);
            this.tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
            this.candidates = (BitSet) Objects.requireNonNull(candidates, "candidates").clone();
            this.root = preparedQueryRoot(
                    catalogueRoot,
                    mode,
                    source,
                    this.categories,
                    this.tokens,
                    this.candidates);
        }

        public String catalogueRoot() {
            return catalogueRoot;
        }

        public MatchMode mode() {
            return mode;
        }

        /** Nullable when no source constraint was prepared. */
        public ProblemSource source() {
            return source;
        }

        public List<ProblemCategory> categories() {
            return categories;
        }

        public List<String> tokens() {
            return tokens;
        }

        public int candidateCount() {
            return candidates.cardinality();
        }

        public String root() {
            return root;
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean replacementAuthority() {
            return false;
        }
    }

    /**
     * Immutable fuzzy candidate ordering bound to one catalogue content root.
     *
     * <p>Edit-distance work is paid once at preparation time. Repeated result limits reuse the
     * ranked entry/distance arrays without rerunning token distance or posting intersections.</p>
     */
    public static final class PreparedFuzzyQuery {
        private final String catalogueRoot;
        private final ProblemSource source;
        private final List<ProblemCategory> categories;
        private final List<String> tokens;
        private final int maxEdits;
        private final int[] rankedEntries;
        private final int[] totalDistances;
        private final String root;

        private PreparedFuzzyQuery(
                String catalogueRoot,
                ProblemSource source,
                Set<ProblemCategory> categories,
                List<String> tokens,
                int maxEdits,
                int[] rankedEntries,
                int[] totalDistances) {
            if (catalogueRoot == null || !catalogueRoot.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("catalogueRoot");
            }
            if (maxEdits < 0 || maxEdits > 2) throw new IllegalArgumentException("maxEdits");
            this.catalogueRoot = catalogueRoot;
            this.source = source;
            EnumSet<ProblemCategory> stableCategories = EnumSet.noneOf(ProblemCategory.class);
            if (categories != null) stableCategories.addAll(categories);
            this.categories = List.copyOf(stableCategories);
            this.tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
            this.maxEdits = maxEdits;
            this.rankedEntries = Objects.requireNonNull(rankedEntries, "rankedEntries").clone();
            this.totalDistances =
                    Objects.requireNonNull(totalDistances, "totalDistances").clone();
            if (this.rankedEntries.length != this.totalDistances.length) {
                throw new IllegalArgumentException("fuzzy candidate arrays");
            }
            for (int index = 0; index < this.rankedEntries.length; index++) {
                if (this.rankedEntries[index] < 0 || this.totalDistances[index] < 0) {
                    throw new IllegalArgumentException("fuzzy candidate value");
                }
            }
            this.root = preparedFuzzyRoot(
                    catalogueRoot,
                    source,
                    this.categories,
                    this.tokens,
                    maxEdits,
                    this.rankedEntries,
                    this.totalDistances);
        }

        public String catalogueRoot() {
            return catalogueRoot;
        }

        public ProblemSource source() {
            return source;
        }

        public List<ProblemCategory> categories() {
            return categories;
        }

        public List<String> tokens() {
            return tokens;
        }

        public int maxEdits() {
            return maxEdits;
        }

        public int candidateCount() {
            return rankedEntries.length;
        }

        public String root() {
            return root;
        }

        public boolean sourceCopyAuthority() {
            return false;
        }

        public boolean replacementAuthority() {
            return false;
        }
    }

    /** Token trie; every path stores the union of postings below that prefix. */
    private static final class PrefixNode {
        private final Map<Integer, PrefixNode> children = new HashMap<>();
        private final BitSet postings = new BitSet();
    }

    /** Metadata-only candidate; totalEditDistance sums the best match for each query token. */
    public record FuzzyCandidate(ProblemDescriptor problem, int totalEditDistance) {
        public FuzzyCandidate {
            problem = Objects.requireNonNull(problem, "problem");
            if (totalEditDistance < 0) throw new IllegalArgumentException("totalEditDistance");
        }
    }

    public ProblemCatalogue(List<ProblemDescriptor> descriptors) {
        Objects.requireNonNull(descriptors, "descriptors");
        Map<String, ProblemDescriptor> unique = new LinkedHashMap<>();
        descriptors.stream()
                .sorted(Comparator.comparing(ProblemDescriptor::canonicalKey))
                .forEach(descriptor -> {
                    ProblemDescriptor previous =
                            unique.putIfAbsent(descriptor.canonicalKey(), descriptor);
                    if (previous != null) {
                        throw new IllegalArgumentException(
                                "duplicate problem " + descriptor.canonicalKey());
                    }
                });
        this.entries = List.copyOf(unique.values());
        this.tokenIndex = buildTokenIndex(entries);
        this.prefixIndex = buildPrefixIndex(tokenIndex);
        this.fuzzyVocabulary = buildFuzzyVocabulary(tokenIndex);
        this.categoryIndex = buildCategoryIndex(entries);
        this.sourceIndex = buildSourceIndex(entries);
        this.root = catalogueRoot(entries);
    }

    public int size() {
        return entries.size();
    }

    public String root() {
        return root;
    }

    public List<ProblemDescriptor> byCategory(ProblemCategory category) {
        return select(
                copy(categoryIndex.get(Objects.requireNonNull(category, "category"))),
                Integer.MAX_VALUE);
    }

    public List<ProblemDescriptor> bySource(ProblemSource source) {
        return select(
                copy(sourceIndex.get(Objects.requireNonNull(source, "source"))),
                Integer.MAX_VALUE);
    }

    public List<ProblemDescriptor> search(
            String text,
            Set<ProblemCategory> categories,
            int limit) {
        return search(null, text, categories, limit);
    }

    public List<ProblemDescriptor> search(
            ProblemSource source,
            String text,
            Set<ProblemCategory> categories,
            int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit");
        List<String> queryTokens = tokens(text);
        return select(candidates(MatchMode.EXACT, source, categories, queryTokens), limit);
    }

    public PreparedQuery prepare(
            String text,
            Set<ProblemCategory> categories) {
        return prepare(null, text, categories);
    }

    public PreparedQuery prepare(
            ProblemSource source,
            String text,
            Set<ProblemCategory> categories) {
        return prepare(MatchMode.EXACT, source, text, categories);
    }

    /**
     * Finds metadata whose external ID or title has a token beginning with each query token.
     * Query normalization, source/category filters, ordering and limit match {@link #search}.
     * Empty text retains the usual filter-only behavior; this is read-only donor evidence.
     */
    public List<ProblemDescriptor> searchPrefix(
            String text, Set<ProblemCategory> categories, int limit) {
        return searchPrefix(null, text, categories, limit);
    }

    public List<ProblemDescriptor> searchPrefix(
            ProblemSource source, String text, Set<ProblemCategory> categories, int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit");
        List<String> queryTokens = tokens(text);
        return select(candidates(MatchMode.PREFIX, source, categories, queryTokens), limit);
    }

    public PreparedQuery preparePrefix(
            String text,
            Set<ProblemCategory> categories) {
        return preparePrefix(null, text, categories);
    }

    public PreparedQuery preparePrefix(
            ProblemSource source,
            String text,
            Set<ProblemCategory> categories) {
        return prepare(MatchMode.PREFIX, source, text, categories);
    }

    /** Execute one prepared exact/prefix query with a new result limit. */
    public List<ProblemDescriptor> execute(PreparedQuery prepared, int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit");
        PreparedQuery checked = Objects.requireNonNull(prepared, "prepared");
        if (!root.equals(checked.catalogueRoot)) {
            throw new IllegalArgumentException("prepared query belongs to a different catalogue");
        }
        return select(checked.candidates, limit);
    }

    /**
     * Execute one prepared exact/prefix query against a source posting without rebuilding token or
     * category intersections.
     *
     * <p>A source-bound prepared query may only be replayed for that same source. A source-agnostic
     * query can be reused across all source lanes.</p>
     */
    public List<ProblemDescriptor> execute(
            PreparedQuery prepared,
            ProblemSource source,
            int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit");
        PreparedQuery checked = Objects.requireNonNull(prepared, "prepared");
        if (!root.equals(checked.catalogueRoot)) {
            throw new IllegalArgumentException("prepared query belongs to a different catalogue");
        }
        ProblemSource checkedSource = Objects.requireNonNull(source, "source");
        requireCompatiblePreparedSource(checked.source, checkedSource);
        BitSet scoped = (BitSet) checked.candidates.clone();
        scoped.and(copy(sourceIndex.get(checkedSource)));
        return select(scoped, limit);
    }

    /**
     * Candidate-only typo lookup over title and external-ID tokens. Each distinct query token must
     * be within maxEdits of an indexed token; hits rank by the sum of their closest distances.
     * Equal scores retain canonical source/ID/title order. No result grants replacement authority.
     */
    public List<FuzzyCandidate> searchFuzzy(
            String text, Set<ProblemCategory> categories, int maxEdits, int limit) {
        return searchFuzzy(null, text, categories, maxEdits, limit);
    }

    public List<FuzzyCandidate> searchFuzzy(
            ProblemSource source,
            String text,
            Set<ProblemCategory> categories,
            int maxEdits,
            int limit) {
        return execute(prepareFuzzy(source, text, categories, maxEdits), limit);
    }

    public PreparedFuzzyQuery prepareFuzzy(
            String text,
            Set<ProblemCategory> categories,
            int maxEdits) {
        return prepareFuzzy(null, text, categories, maxEdits);
    }

    public PreparedFuzzyQuery prepareFuzzy(
            ProblemSource source,
            String text,
            Set<ProblemCategory> categories,
            int maxEdits) {
        if (maxEdits < 0 || maxEdits > 2) throw new IllegalArgumentException("maxEdits");
        if (text != null && text.length() > 256) throw new IllegalArgumentException("text");
        List<String> queryTokens = tokens(text);
        if (queryTokens.size() > 8) throw new IllegalArgumentException("query token count");

        BitSet candidateBits = new BitSet(entries.size());
        candidateBits.set(0, entries.size());
        if (source != null) candidateBits.and(copy(sourceIndex.get(source)));
        if (categories != null && !categories.isEmpty()) {
            candidateBits.and(categoryPostings(categories));
        }

        int[] totals = new int[entries.size()];
        // A fresh match bitmap marks which scratch slots belong to each query token.
        int[] closest = new int[entries.size()];
        for (String queryToken : queryTokens) {
            int[] queryPoints = queryToken.codePoints().toArray();
            BitSet tokenMatches = new BitSet(entries.size());
            for (int length = Math.max(0, queryPoints.length - maxEdits);
                    length <= queryPoints.length + maxEdits;
                    length++) {
                for (IndexedToken indexed : fuzzyVocabulary.getOrDefault(length, List.of())) {
                    int distance = boundedDistance(queryPoints, indexed.codePoints(), maxEdits);
                    if (distance > maxEdits) continue;
                    BitSet postings = indexed.postings();
                    for (int entry = postings.nextSetBit(0);
                            entry >= 0;
                            entry = entry == Integer.MAX_VALUE ? -1 : postings.nextSetBit(entry + 1)) {
                        if (candidateBits.get(entry)) {
                            if (tokenMatches.get(entry)) {
                                closest[entry] = Math.min(closest[entry], distance);
                            } else {
                                tokenMatches.set(entry);
                                closest[entry] = distance;
                            }
                        }
                    }
                }
            }
            candidateBits.and(tokenMatches);
            if (candidateBits.isEmpty()) break;
            for (int entry = candidateBits.nextSetBit(0);
                    entry >= 0;
                    entry = entry == Integer.MAX_VALUE ? -1 : candidateBits.nextSetBit(entry + 1)) {
                totals[entry] += closest[entry];
            }
        }

        List<Integer> ranked = new ArrayList<>(candidateBits.cardinality());
        for (int entry = candidateBits.nextSetBit(0);
                entry >= 0;
                entry = entry == Integer.MAX_VALUE ? -1 : candidateBits.nextSetBit(entry + 1)) {
            ranked.add(entry);
        }
        ranked.sort(Comparator.<Integer>comparingInt(entry -> totals[entry])
                .thenComparingInt(Integer::intValue));
        int[] rankedEntries = new int[ranked.size()];
        int[] totalDistances = new int[ranked.size()];
        for (int index = 0; index < ranked.size(); index++) {
            rankedEntries[index] = ranked.get(index);
            totalDistances[index] = totals[rankedEntries[index]];
        }
        return new PreparedFuzzyQuery(
                root,
                source,
                categories,
                queryTokens,
                maxEdits,
                rankedEntries,
                totalDistances);
    }

    public List<FuzzyCandidate> execute(PreparedFuzzyQuery prepared, int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit");
        PreparedFuzzyQuery checked = Objects.requireNonNull(prepared, "prepared");
        if (!root.equals(checked.catalogueRoot)) {
            throw new IllegalArgumentException(
                    "prepared fuzzy query belongs to a different catalogue");
        }
        int count = Math.min(limit, checked.rankedEntries.length);
        ArrayList<FuzzyCandidate> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(new FuzzyCandidate(
                    entries.get(checked.rankedEntries[index]),
                    checked.totalDistances[index]));
        }
        return List.copyOf(result);
    }

    /**
     * Execute one prepared fuzzy ranking against a source lane without recomputing edit distance or
     * token postings.
     */
    public List<FuzzyCandidate> execute(
            PreparedFuzzyQuery prepared,
            ProblemSource source,
            int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit");
        PreparedFuzzyQuery checked = Objects.requireNonNull(prepared, "prepared");
        if (!root.equals(checked.catalogueRoot)) {
            throw new IllegalArgumentException(
                    "prepared fuzzy query belongs to a different catalogue");
        }
        ProblemSource checkedSource = Objects.requireNonNull(source, "source");
        requireCompatiblePreparedSource(checked.source, checkedSource);
        ArrayList<FuzzyCandidate> result =
                new ArrayList<>(Math.min(limit, checked.rankedEntries.length));
        for (int index = 0;
                index < checked.rankedEntries.length && result.size() < limit;
                index++) {
            ProblemDescriptor descriptor = entries.get(checked.rankedEntries[index]);
            if (descriptor.source() == checkedSource) {
                result.add(new FuzzyCandidate(
                        descriptor,
                        checked.totalDistances[index]));
            }
        }
        return List.copyOf(result);
    }

    private static void requireCompatiblePreparedSource(
            ProblemSource preparedSource,
            ProblemSource requestedSource) {
        if (preparedSource != null && preparedSource != requestedSource) {
            throw new IllegalArgumentException(
                    "prepared query is source-bound to " + preparedSource.name());
        }
    }

    private PreparedQuery prepare(
            MatchMode mode,
            ProblemSource source,
            String text,
            Set<ProblemCategory> categories) {
        List<String> queryTokens = tokens(text);
        return new PreparedQuery(
                root,
                mode,
                source,
                categories,
                queryTokens,
                candidates(mode, source, categories, queryTokens));
    }

    private BitSet candidates(
            MatchMode mode,
            ProblemSource source,
            Set<ProblemCategory> categories,
            List<String> queryTokens) {
        BitSet result = new BitSet(entries.size());
        result.set(0, entries.size());
        if (source != null) {
            result.and(copy(sourceIndex.get(source)));
        }

        for (String token : queryTokens) {
            BitSet matching = mode == MatchMode.EXACT
                    ? tokenIndex.get(token)
                    : prefixPostings(token);
            if (matching == null) {
                result.clear();
                return result;
            }
            result.and(matching);
            if (result.isEmpty()) return result;
        }

        if (categories != null && !categories.isEmpty()) {
            result.and(categoryPostings(categories));
        }
        return result;
    }

    private BitSet prefixPostings(String token) {
        PrefixNode node = prefixIndex;
        for (int codePoint : token.codePoints().toArray()) {
            node = node.children.get(codePoint);
            if (node == null) return null;
        }
        return node.postings;
    }

    private BitSet categoryPostings(Set<ProblemCategory> categories) {
        BitSet combined = new BitSet(entries.size());
        categories.stream()
                .filter(Objects::nonNull)
                .sorted()
                .forEach(category -> {
                    BitSet bits = categoryIndex.get(category);
                    if (bits != null) combined.or(bits);
                });
        return combined;
    }

    private static String preparedQueryRoot(
            String catalogueRoot,
            MatchMode mode,
            ProblemSource source,
            List<ProblemCategory> categories,
            List<String> queryTokens,
            BitSet candidates) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "SYNEXIA_PROBLEM_PREPARED_QUERY_V1");
            frame(digest, catalogueRoot);
            frame(digest, mode.name());
            frame(digest, source == null ? "" : source.name());
            categories.forEach(category -> frame(digest, category.name()));
            queryTokens.forEach(token -> frame(digest, token));
            frame(digest, Integer.toString(candidates.cardinality()));
            for (int index = candidates.nextSetBit(0);
                    index >= 0;
                    index = index == Integer.MAX_VALUE ? -1 : candidates.nextSetBit(index + 1)) {
                frame(digest, Integer.toString(index));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String preparedFuzzyRoot(
            String catalogueRoot,
            ProblemSource source,
            List<ProblemCategory> categories,
            List<String> queryTokens,
            int maxEdits,
            int[] rankedEntries,
            int[] totalDistances) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "SYNEXIA_PROBLEM_PREPARED_FUZZY_QUERY_V1");
            frame(digest, catalogueRoot);
            frame(digest, source == null ? "" : source.name());
            categories.forEach(category -> frame(digest, category.name()));
            queryTokens.forEach(token -> frame(digest, token));
            frame(digest, Integer.toString(maxEdits));
            frame(digest, Integer.toString(rankedEntries.length));
            for (int index = 0; index < rankedEntries.length; index++) {
                frame(digest, Integer.toString(rankedEntries[index]));
                frame(digest, Integer.toString(totalDistances[index]));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static int boundedDistance(int[] left, int[] right, int maxEdits) {
        if (Math.abs(left.length - right.length) > maxEdits) return maxEdits + 1;
        if (maxEdits == 0) return Arrays.equals(left, right) ? 0 : 1;
        int[] previous = new int[right.length + 1];
        int[] current = new int[right.length + 1];
        for (int column = 0; column <= right.length; column++) previous[column] = column;
        for (int row = 1; row <= left.length; row++) {
            current[0] = row;
            int rowMinimum = current[0];
            for (int column = 1; column <= right.length; column++) {
                int substitution = previous[column - 1]
                        + (left[row - 1] == right[column - 1] ? 0 : 1);
                current[column] = Math.min(substitution,
                        Math.min(previous[column] + 1, current[column - 1] + 1));
                rowMinimum = Math.min(rowMinimum, current[column]);
            }
            if (rowMinimum > maxEdits) return maxEdits + 1;
            int[] swapped = previous;
            previous = current;
            current = swapped;
        }
        return Math.min(previous[right.length], maxEdits + 1);
    }

    private List<ProblemDescriptor> select(BitSet bits, int limit) {
        if (bits == null || bits.isEmpty()) return List.of();
        List<ProblemDescriptor> result = new ArrayList<>(Math.min(bits.cardinality(), limit));
        for (int index = bits.nextSetBit(0);
                index >= 0 && result.size() < limit;
                index = index == Integer.MAX_VALUE ? -1 : bits.nextSetBit(index + 1)) {
            result.add(entries.get(index));
        }
        return List.copyOf(result);
    }

    private static Map<String, BitSet> buildTokenIndex(List<ProblemDescriptor> entries) {
        Map<String, BitSet> index = new HashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            ProblemDescriptor descriptor = entries.get(i);
            for (String token : tokens(descriptor.externalId() + " " + descriptor.title())) {
                index.computeIfAbsent(token, ignored -> new BitSet(entries.size())).set(i);
            }
        }
        return Map.copyOf(index);
    }

    private static PrefixNode buildPrefixIndex(Map<String, BitSet> tokens) {
        PrefixNode root = new PrefixNode();
        tokens.forEach((token, postings) -> {
            PrefixNode node = root;
            for (int codePoint : token.codePoints().toArray()) {
                node = node.children.computeIfAbsent(codePoint, ignored -> new PrefixNode());
                node.postings.or(postings);
            }
        });
        return root;
    }

    private static Map<Integer, List<IndexedToken>> buildFuzzyVocabulary(
            Map<String, BitSet> tokenIndex) {
        Map<Integer, List<IndexedToken>> byLength = new HashMap<>();
        tokenIndex.forEach((token, postings) -> {
            int[] points = token.codePoints().toArray();
            byLength.computeIfAbsent(points.length, ignored -> new ArrayList<>())
                    .add(new IndexedToken(points, postings));
        });
        byLength.replaceAll((ignored, tokens) -> List.copyOf(tokens));
        return Map.copyOf(byLength);
    }

    private static Map<ProblemCategory, BitSet> buildCategoryIndex(
            List<ProblemDescriptor> entries) {
        Map<ProblemCategory, BitSet> index = new EnumMap<>(ProblemCategory.class);
        for (int i = 0; i < entries.size(); i++) {
            for (ProblemCategory category : entries.get(i).categories()) {
                index.computeIfAbsent(category, ignored -> new BitSet(entries.size())).set(i);
            }
        }
        return Map.copyOf(index);
    }

    private static Map<ProblemSource, BitSet> buildSourceIndex(
            List<ProblemDescriptor> entries) {
        Map<ProblemSource, BitSet> index = new EnumMap<>(ProblemSource.class);
        for (int i = 0; i < entries.size(); i++) {
            index.computeIfAbsent(
                            entries.get(i).source(),
                            ignored -> new BitSet(entries.size()))
                    .set(i);
        }
        return Map.copyOf(index);
    }

    private static List<String> tokens(String text) {
        String normalized = Normalizer.normalize(
                        Objects.toString(text, ""), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .strip();
        if (normalized.isEmpty()) return List.of();
        return TOKEN_BREAK.splitAsStream(normalized)
                .filter(token -> !token.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private static String catalogueRoot(List<ProblemDescriptor> entries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "SYNEXIA_PROBLEM_CATALOGUE_V1");
            for (ProblemDescriptor descriptor : entries) {
                frame(digest, descriptor.source().name());
                frame(digest, descriptor.externalId());
                frame(digest, descriptor.title());
                frame(digest, descriptor.uri().toString());
                frame(
                        digest,
                        descriptor.categories().stream()
                                .map(Enum::name)
                                .sorted()
                                .toList()
                                .toString());
                frame(digest, descriptor.asymptoticTarget());
                frame(digest, Boolean.toString(descriptor.evidenceOnly()));
                frame(digest, descriptor.licenseNote());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) ':');
        digest.update(bytes);
        digest.update((byte) '\n');
    }

    private static BitSet copy(BitSet value) {
        return value == null ? new BitSet() : (BitSet) value.clone();
    }
}
