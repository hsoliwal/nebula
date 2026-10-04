// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Metadata-only category registry for the three requested practice surfaces.
 *
 * <p>The registry stores labels and source index URLs only. It deliberately does not copy challenge
 * statements, editorials, submissions, or solution bodies. Platform taxonomies are not assumed to
 * be identical: only explicitly supported rows are published. The pinned Java source corpus remains
 * owned by {@link JavaProblemCorpus}; a category row is not source custody.</p>
 */
public final class CompetitiveProblemCategoryCatalog {
    public enum Platform {
        LEETCODE,
        HACKERRANK,
        GEEKSFORGEEKS
    }

    public record Category(Platform platform, String key, String label, String officialUrl) {
        public Category {
            Objects.requireNonNull(platform, "platform");
            key = requiredKey(key);
            label = required(label, "label");
            officialUrl = required(officialUrl, "officialUrl");
            if (!officialUrl.startsWith("https://")) {
                throw new IllegalArgumentException("officialUrl must use https");
            }
        }

        public String stableKey() {
            return platform.name() + "\t" + key;
        }

        /** Normalized Synexia comparison category; never a claim of platform equivalence. */
        public CompetitiveProblemCategory normalizedCategory() {
            return normalized(key);
        }
    }

    /**
     * Original six cross-site keys retained for source compatibility.
     *
     * <p>Use {@link #commonCategories()} for the complete normalized intersection.</p>
     */
    private static final List<String> LEGACY_COMMON_KEYS = List.of(
            "ARRAYS", "STRINGS", "SEARCHING", "SORTING", "GRAPH", "DYNAMIC_PROGRAMMING");

    private static final String LEETCODE_PROBLEMSET = "https://leetcode.com/problemset/";
    private static final String HACKERRANK_ALGORITHMS =
            "https://www.hackerrank.com/domains/algorithms";
    private static final String HACKERRANK_DATA_STRUCTURES =
            "https://www.hackerrank.com/domains/data-structures";
    private static final String GFG_PRACTICE =
            "https://www.geeksforgeeks.org/practice-problems";

    private static final List<Category> CATEGORIES = build();
    private static final Set<String> LEGACY_COMMON_KEY_SET = Set.copyOf(LEGACY_COMMON_KEYS);
    private static final Indexes INDEXES = Indexes.build(CATEGORIES);

    private CompetitiveProblemCategoryCatalog() {}

    public static List<Category> all() {
        return CATEGORIES;
    }

    public static List<Category> byPlatform(final Platform platform) {
        return INDEXES.byPlatform().get(
                Objects.requireNonNull(platform, "platform"));
    }

    public static List<Category> byCategory(final CompetitiveProblemCategory category) {
        return INDEXES.byCategory().get(
                Objects.requireNonNull(category, "category"));
    }

    public static List<Category> byPlatformAndCategory(
            final Platform platform,
            final CompetitiveProblemCategory category) {
        return INDEXES.byPlatformAndCategory()
                .get(Objects.requireNonNull(platform, "platform"))
                .get(Objects.requireNonNull(category, "category"));
    }

    /** Historical API: returns exactly the original six common string keys. */
    public static Set<String> commonKeys() {
        return LEGACY_COMMON_KEY_SET;
    }

    public static Set<String> supportedKeys(final Platform platform) {
        return INDEXES.supportedKeys().get(
                Objects.requireNonNull(platform, "platform"));
    }

    public static Set<CompetitiveProblemCategory> supportedCategories(final Platform platform) {
        return INDEXES.supportedCategories().get(
                Objects.requireNonNull(platform, "platform"));
    }

    /** Complete normalized intersection actually represented on all three source surfaces. */
    public static Set<CompetitiveProblemCategory> commonCategories() {
        return INDEXES.commonCategories();
    }

    public static boolean supports(
            final Platform platform,
            final CompetitiveProblemCategory category) {
        return !byPlatformAndCategory(platform, category).isEmpty();
    }

    public static Category require(final Platform platform, final String key) {
        final Platform checkedPlatform = Objects.requireNonNull(platform, "platform");
        final String checked = requiredKey(key);
        final Category category = INDEXES.byStableKey().get(checkedPlatform.name() + "\t" + checked);
        if (category == null) {
            throw new IllegalArgumentException(
                    "unknown competitive-problem category: "
                            + checkedPlatform
                            + ":"
                            + checked);
        }
        return category;
    }

    public static Category require(
            final Platform platform,
            final CompetitiveProblemCategory category) {
        final List<Category> matches = byPlatformAndCategory(platform, category);
        if (matches.size() != 1) {
            throw new IllegalArgumentException(
                    "unsupported or ambiguous competitive-problem category: "
                            + platform + ":" + category);
        }
        return matches.getFirst();
    }


    private record Indexes(
            Map<Platform, List<Category>> byPlatform,
            Map<CompetitiveProblemCategory, List<Category>> byCategory,
            Map<Platform, Map<CompetitiveProblemCategory, List<Category>>> byPlatformAndCategory,
            Map<String, Category> byStableKey,
            Map<Platform, Set<String>> supportedKeys,
            Map<Platform, Set<CompetitiveProblemCategory>> supportedCategories,
            Set<CompetitiveProblemCategory> commonCategories) {

        private static Indexes build(List<Category> categories) {
            EnumMap<Platform, ArrayList<Category>> byPlatformMutable =
                    lists(Platform.class);
            EnumMap<CompetitiveProblemCategory, ArrayList<Category>> byCategoryMutable =
                    lists(CompetitiveProblemCategory.class);
            EnumMap<Platform, EnumMap<CompetitiveProblemCategory, ArrayList<Category>>>
                    facetsMutable = new EnumMap<>(Platform.class);
            EnumMap<Platform, LinkedHashSet<String>> keysMutable =
                    new EnumMap<>(Platform.class);
            EnumMap<Platform, EnumSet<CompetitiveProblemCategory>> categoriesMutable =
                    new EnumMap<>(Platform.class);
            LinkedHashMap<String, Category> byStableKeyMutable = new LinkedHashMap<>();

            for (Platform platform : Platform.values()) {
                facetsMutable.put(platform, lists(CompetitiveProblemCategory.class));
                keysMutable.put(platform, new LinkedHashSet<>());
                categoriesMutable.put(
                        platform,
                        EnumSet.noneOf(CompetitiveProblemCategory.class));
            }

            for (Category row : categories) {
                byPlatformMutable.get(row.platform()).add(row);
                CompetitiveProblemCategory normalized = row.normalizedCategory();
                byCategoryMutable.get(normalized).add(row);
                facetsMutable.get(row.platform()).get(normalized).add(row);
                keysMutable.get(row.platform()).add(row.key());
                categoriesMutable.get(row.platform()).add(normalized);
                if (byStableKeyMutable.putIfAbsent(row.stableKey(), row) != null) {
                    throw new ExceptionInInitializerError(
                            "duplicate stable category key: " + row.stableKey());
                }
            }

            EnumSet<CompetitiveProblemCategory> common =
                    EnumSet.allOf(CompetitiveProblemCategory.class);
            common.remove(CompetitiveProblemCategory.OTHER);
            for (Platform platform : Platform.values()) {
                common.retainAll(categoriesMutable.get(platform));
            }

            EnumMap<Platform, Map<CompetitiveProblemCategory, List<Category>>> frozenFacets =
                    new EnumMap<>(Platform.class);
            for (Platform platform : Platform.values()) {
                frozenFacets.put(
                        platform,
                        freezeLists(
                                CompetitiveProblemCategory.class,
                                facetsMutable.get(platform)));
            }

            EnumMap<Platform, Set<String>> frozenKeys = new EnumMap<>(Platform.class);
            EnumMap<Platform, Set<CompetitiveProblemCategory>> frozenCategories =
                    new EnumMap<>(Platform.class);
            for (Platform platform : Platform.values()) {
                frozenKeys.put(platform, Set.copyOf(keysMutable.get(platform)));
                frozenCategories.put(
                        platform,
                        Set.copyOf(categoriesMutable.get(platform)));
            }

            return new Indexes(
                    freezeLists(Platform.class, byPlatformMutable),
                    freezeLists(
                            CompetitiveProblemCategory.class,
                            byCategoryMutable),
                    Collections.unmodifiableMap(frozenFacets),
                    Map.copyOf(byStableKeyMutable),
                    Collections.unmodifiableMap(frozenKeys),
                    Collections.unmodifiableMap(frozenCategories),
                    Set.copyOf(common));
        }

        private static <E extends Enum<E>>
                EnumMap<E, ArrayList<Category>> lists(Class<E> type) {
            EnumMap<E, ArrayList<Category>> result = new EnumMap<>(type);
            for (E value : type.getEnumConstants()) {
                result.put(value, new ArrayList<>());
            }
            return result;
        }

        private static <E extends Enum<E>> Map<E, List<Category>> freezeLists(
                Class<E> type,
                EnumMap<E, ArrayList<Category>> source) {
            EnumMap<E, List<Category>> result = new EnumMap<>(type);
            source.forEach((key, values) -> result.put(key, List.copyOf(values)));
            return Collections.unmodifiableMap(result);
        }
    }

    private static List<Category> build() {
        final LinkedHashSet<Category> result = new LinkedHashSet<>();

        // LeetCode topic vocabulary. Generic problemset URL is used for categories where this
        // metadata class does not need to depend on an unstable filtered-list slug.
        add(result, Platform.LEETCODE, "ARRAYS", "Array", "https://leetcode.com/problem-list/array/");
        add(result, Platform.LEETCODE, "STRINGS", "String", "https://leetcode.com/problem-list/string/");
        add(result, Platform.LEETCODE, "SEARCHING", "Binary Search",
                "https://leetcode.com/problem-list/binary-search/");
        add(result, Platform.LEETCODE, "SORTING", "Sorting",
                "https://leetcode.com/problem-list/sorting/");
        add(result, Platform.LEETCODE, "GRAPH", "Graph",
                "https://leetcode.com/problem-list/graph/");
        add(result, Platform.LEETCODE, "DYNAMIC_PROGRAMMING", "Dynamic Programming",
                "https://leetcode.com/problem-list/dynamic-programming/");
        add(result, Platform.LEETCODE, "TWO_POINTERS", "Two Pointers", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "SLIDING_WINDOW", "Sliding Window", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "HASHING", "Hash Table", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "LINKED_LIST", "Linked List", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "STACK_QUEUE", "Stack / Queue", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "TREES", "Tree", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "HEAPS", "Heap / Priority Queue", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "INTERVALS", "Intervals", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "RANGE_QUERIES", "Segment Tree / Range Query",
                LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "GREEDY", "Greedy", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "BACKTRACKING", "Backtracking", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "TRIES", "Trie", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "BIT_MANIPULATION", "Bit Manipulation",
                LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "MATHEMATICS", "Math / Geometry", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "CONCURRENCY", "Concurrency", LEETCODE_PROBLEMSET);
        add(result, Platform.LEETCODE, "SIMULATION", "Simulation", LEETCODE_PROBLEMSET);

        // HackerRank publishes algorithm and data-structure subdomains separately.
        add(result, Platform.HACKERRANK, "ARRAYS", "Arrays", HACKERRANK_DATA_STRUCTURES);
        add(result, Platform.HACKERRANK, "STRINGS", "Strings", HACKERRANK_ALGORITHMS);
        add(result, Platform.HACKERRANK, "SEARCHING", "Search", HACKERRANK_ALGORITHMS);
        add(result, Platform.HACKERRANK, "SORTING", "Sorting", HACKERRANK_ALGORITHMS);
        add(result, Platform.HACKERRANK, "GRAPH", "Graph Theory", HACKERRANK_ALGORITHMS);
        add(result, Platform.HACKERRANK, "DYNAMIC_PROGRAMMING", "Dynamic Programming",
                HACKERRANK_ALGORITHMS);
        add(result, Platform.HACKERRANK, "LINKED_LIST", "Linked Lists",
                HACKERRANK_DATA_STRUCTURES);
        add(result, Platform.HACKERRANK, "STACK_QUEUE", "Stacks / Queues",
                HACKERRANK_DATA_STRUCTURES);
        add(result, Platform.HACKERRANK, "TREES", "Trees / Balanced Trees",
                HACKERRANK_DATA_STRUCTURES);
        add(result, Platform.HACKERRANK, "HEAPS", "Heap", HACKERRANK_DATA_STRUCTURES);
        add(result, Platform.HACKERRANK, "GREEDY", "Greedy", HACKERRANK_ALGORITHMS);
        add(result, Platform.HACKERRANK, "TRIES", "Trie", HACKERRANK_DATA_STRUCTURES);
        add(result, Platform.HACKERRANK, "BIT_MANIPULATION", "Bit Manipulation",
                HACKERRANK_ALGORITHMS);

        // GeeksforGeeks current practice topic surface.
        add(result, Platform.GEEKSFORGEEKS, "ARRAYS", "Arrays", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "STRINGS", "Strings", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "SEARCHING", "Searching", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "SORTING", "Sorting", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "GRAPH", "Graph", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "DYNAMIC_PROGRAMMING", "Dynamic Programming",
                GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "TWO_POINTERS", "Two Pointers", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "SLIDING_WINDOW", "Sliding Window", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "HASHING", "Hashing", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "LINKED_LIST", "Linked List", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "STACK_QUEUE", "Stack / Queue / Deque",
                GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "TREES", "Tree / Binary Search Tree", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "HEAPS", "Heap", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "RANGE_QUERIES", "Segment Tree", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "GREEDY", "Greedy", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "BACKTRACKING", "Backtracking", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "TRIES", "Trie", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "BIT_MANIPULATION", "Bit Magic", GFG_PRACTICE);
        add(result, Platform.GEEKSFORGEEKS, "MATHEMATICS", "Mathematics", GFG_PRACTICE);

        final List<Category> ordered =
                result.stream().sorted(Comparator.comparing(Category::stableKey)).toList();
        if (ordered.size() != result.size()) {
            throw new ExceptionInInitializerError("duplicate category metadata");
        }
        for (Platform platform : Platform.values()) {
            final EnumSet<CompetitiveProblemCategory> seen =
                    EnumSet.noneOf(CompetitiveProblemCategory.class);
            for (Category row : ordered) {
                if (row.platform() == platform && !seen.add(row.normalizedCategory())) {
                    throw new ExceptionInInitializerError(
                            "duplicate normalized category: "
                                    + platform + ":" + row.normalizedCategory());
                }
            }
        }
        return List.copyOf(ordered);
    }

    private static void add(
            final Set<Category> rows,
            final Platform platform,
            final String key,
            final String label,
            final String url) {
        if (!rows.add(new Category(platform, key, label, url))) {
            throw new ExceptionInInitializerError(
                    "duplicate category key: " + platform + ":" + key);
        }
    }

    private static CompetitiveProblemCategory normalized(final String key) {
        return switch (requiredKey(key)) {
            case "SEARCHING" -> CompetitiveProblemCategory.SEARCH;
            case "GRAPH" -> CompetitiveProblemCategory.GRAPHS;
            default -> CompetitiveProblemCategory.valueOf(requiredKey(key));
        };
    }

    private static String requiredKey(final String value) {
        final String normalized = required(value, "key").toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("invalid category key: " + value);
        }
        return normalized;
    }

    private static String required(final String value, final String label) {
        final String checked = Objects.requireNonNull(value, label).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(label);
        }
        return checked;
    }
}
