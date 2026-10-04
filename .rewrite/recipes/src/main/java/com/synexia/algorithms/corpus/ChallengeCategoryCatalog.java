// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Stable taxonomy projection from public coding-challenge categories to Synexia's canonical
 * mechanical algorithm shapes.
 *
 * <p>The catalogue is deliberately evidence-only. A category match never imports a challenge
 * statement, editorial, judge harness, or donor solution body, and it never grants rewrite
 * authority. Consumers must still prove the target source contract and behaviour through the
 * normal M3 admission gates.</p>
 */
public final class ChallengeCategoryCatalog {
    public static final String AUTHORITY = "TAXONOMY_ONLY";

    public record Category(
            ChallengePlatform platform,
            String id,
            String displayName,
            List<String> aliases,
            List<AlgorithmShape> shapes,
            String source,
            String authority) {

        public Category {
            platform = Objects.requireNonNull(platform, "platform");
            if (platform == ChallengePlatform.OTHER) {
                throw new IllegalArgumentException("catalogue category requires a concrete platform");
            }
            id = identifier(id);
            displayName = required(displayName, "displayName");
            source = required(source, "source");
            authority = required(authority, "authority");

            TreeSet<String> stableAliases = new TreeSet<>();
            stableAliases.add(displayName);
            stableAliases.add(id);
            if (aliases != null) {
                for (String alias : aliases) stableAliases.add(required(alias, "alias"));
            }
            aliases = List.copyOf(stableAliases);

            TreeSet<AlgorithmShape> stableShapes =
                    new TreeSet<>(Comparator.comparing(Enum::name));
            if (shapes != null) stableShapes.addAll(shapes);
            shapes = List.copyOf(stableShapes);
        }

        public boolean mapsTo(AlgorithmShape shape) {
            return shapes.contains(Objects.requireNonNull(shape, "shape"));
        }
    }

    private static final String LEETCODE_SOURCE =
            "LeetCode problem tags and official study-plan taxonomy";
    private static final String HACKERRANK_ALGORITHMS_SOURCE =
            "HackerRank Algorithms subdomains";
    private static final String HACKERRANK_STRUCTURES_SOURCE =
            "HackerRank Data Structures subdomains";
    private static final String GFG_SOURCE =
            "GeeksforGeeks Practice topic taxonomy";

    private static final List<Category> CATEGORIES = build();
    private static final Map<ChallengePlatform, List<Category>> BY_PLATFORM = byPlatform();
    private static final Map<AlgorithmShape, List<Category>> BY_SHAPE = byShape();
    private static final Map<String, Category> LOOKUP = lookup();
    private static final String ROOT = root(CATEGORIES);

    private ChallengeCategoryCatalog() {}

    public static List<Category> categories() {
        return CATEGORIES;
    }

    public static List<Category> byPlatform(ChallengePlatform platform) {
        return BY_PLATFORM.getOrDefault(
                Objects.requireNonNull(platform, "platform"),
                List.of());
    }

    public static List<Category> forShape(AlgorithmShape shape) {
        return BY_SHAPE.getOrDefault(Objects.requireNonNull(shape, "shape"), List.of());
    }

    public static Optional<Category> find(ChallengePlatform platform, String label) {
        Objects.requireNonNull(platform, "platform");
        if (platform == ChallengePlatform.OTHER || label == null || label.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LOOKUP.get(key(platform, label)));
    }

    public static String root() {
        return ROOT;
    }

    private static List<Category> build() {
        ArrayList<Category> rows = new ArrayList<>();

        // LeetCode: broad stable tags/pattern families used for problem navigation and study plans.
        add(rows, ChallengePlatform.LEETCODE, "array", "Array", LEETCODE_SOURCE,
                shapes(AlgorithmShape.LINEAR_SCAN, AlgorithmShape.TWO_POINTER,
                        AlgorithmShape.SLIDING_WINDOW, AlgorithmShape.PREFIX_SCAN,
                        AlgorithmShape.KADANE), "arrays");
        add(rows, ChallengePlatform.LEETCODE, "string", "String", LEETCODE_SOURCE,
                shapes(AlgorithmShape.STRING_TRANSFORM, AlgorithmShape.KMP,
                        AlgorithmShape.RABIN_KARP, AlgorithmShape.Z_ALGORITHM,
                        AlgorithmShape.MANACHER), "strings");
        add(rows, ChallengePlatform.LEETCODE, "hash-table", "Hash Table", LEETCODE_SOURCE,
                shapes(AlgorithmShape.HASH_MEMBERSHIP, AlgorithmShape.FREQUENCY_COUNT,
                        AlgorithmShape.PAIR_SUM_HASH), "hashing", "hash map", "hash set");
        add(rows, ChallengePlatform.LEETCODE, "linked-list", "Linked List", LEETCODE_SOURCE,
                shapes(AlgorithmShape.LINKED_LIST_REWRITE, AlgorithmShape.FAST_SLOW_POINTER),
                "linked lists");
        add(rows, ChallengePlatform.LEETCODE, "stack", "Stack", LEETCODE_SOURCE,
                shapes(AlgorithmShape.STACK_MACHINE, AlgorithmShape.MONOTONIC_STACK));
        add(rows, ChallengePlatform.LEETCODE, "queue", "Queue", LEETCODE_SOURCE,
                shapes(AlgorithmShape.QUEUE_MACHINE, AlgorithmShape.BFS,
                        AlgorithmShape.MONOTONIC_QUEUE), "deque");
        add(rows, ChallengePlatform.LEETCODE, "heap-priority-queue", "Heap / Priority Queue",
                LEETCODE_SOURCE,
                shapes(AlgorithmShape.PRIORITY_QUEUE, AlgorithmShape.HEAP_SELECT,
                        AlgorithmShape.HEAP_SORT, AlgorithmShape.MULTIWAY_MERGE),
                "heap", "priority queue");
        add(rows, ChallengePlatform.LEETCODE, "tree", "Tree", LEETCODE_SOURCE,
                shapes(AlgorithmShape.TREE_TRAVERSAL, AlgorithmShape.DFS, AlgorithmShape.BFS,
                        AlgorithmShape.LOWEST_COMMON_ANCESTOR, AlgorithmShape.BINARY_LIFTING,
                        AlgorithmShape.EULER_TOUR), "binary tree");
        add(rows, ChallengePlatform.LEETCODE, "graph", "Graph", LEETCODE_SOURCE,
                shapes(AlgorithmShape.BFS, AlgorithmShape.DFS, AlgorithmShape.DIJKSTRA,
                        AlgorithmShape.TOPOLOGICAL_SORT, AlgorithmShape.UNION_FIND,
                        AlgorithmShape.CONNECTED_COMPONENTS,
                        AlgorithmShape.STRONGLY_CONNECTED_COMPONENTS,
                        AlgorithmShape.BRIDGES_ARTICULATION, AlgorithmShape.BELLMAN_FORD,
                        AlgorithmShape.FLOYD_WARSHALL, AlgorithmShape.PRIM_MST,
                        AlgorithmShape.KRUSKAL_MST, AlgorithmShape.MAX_FLOW),
                "graph theory");
        add(rows, ChallengePlatform.LEETCODE, "binary-search", "Binary Search", LEETCODE_SOURCE,
                shapes(AlgorithmShape.BINARY_SEARCH, AlgorithmShape.EXPONENTIAL_SEARCH,
                        AlgorithmShape.GALLOPING_SEARCH, AlgorithmShape.INTERPOLATION_SEARCH),
                "binarysearch");
        add(rows, ChallengePlatform.LEETCODE, "two-pointers", "Two Pointers", LEETCODE_SOURCE,
                shapes(AlgorithmShape.TWO_POINTER, AlgorithmShape.FAST_SLOW_POINTER),
                "two pointer");
        add(rows, ChallengePlatform.LEETCODE, "sliding-window", "Sliding Window", LEETCODE_SOURCE,
                shapes(AlgorithmShape.SLIDING_WINDOW, AlgorithmShape.MONOTONIC_QUEUE),
                "window");
        add(rows, ChallengePlatform.LEETCODE, "prefix-sum", "Prefix Sum", LEETCODE_SOURCE,
                shapes(AlgorithmShape.PREFIX_SCAN, AlgorithmShape.DIFFERENCE_SCAN),
                "prefix sums", "cumulative sum");
        add(rows, ChallengePlatform.LEETCODE, "dynamic-programming", "Dynamic Programming",
                LEETCODE_SOURCE,
                shapes(AlgorithmShape.DYNAMIC_PROGRAMMING, AlgorithmShape.MEMOIZATION,
                        AlgorithmShape.LONGEST_COMMON_SUBSEQUENCE,
                        AlgorithmShape.LONGEST_INCREASING_SUBSEQUENCE,
                        AlgorithmShape.COIN_CHANGE, AlgorithmShape.KNAPSACK_01,
                        AlgorithmShape.INTERVAL_DP, AlgorithmShape.BITMASK_DP,
                        AlgorithmShape.DIGIT_DP), "dp");
        add(rows, ChallengePlatform.LEETCODE, "greedy", "Greedy", LEETCODE_SOURCE,
                shapes(AlgorithmShape.GREEDY));
        add(rows, ChallengePlatform.LEETCODE, "backtracking", "Backtracking", LEETCODE_SOURCE,
                shapes(AlgorithmShape.BACKTRACKING, AlgorithmShape.CONSTRAINT_SEARCH));
        add(rows, ChallengePlatform.LEETCODE, "bit-manipulation", "Bit Manipulation",
                LEETCODE_SOURCE,
                shapes(AlgorithmShape.BIT_MANIPULATION, AlgorithmShape.XOR_BASIS,
                        AlgorithmShape.BITSET_DP), "bitwise");
        add(rows, ChallengePlatform.LEETCODE, "trie", "Trie", LEETCODE_SOURCE,
                shapes(AlgorithmShape.TRIE, AlgorithmShape.AHO_CORASICK), "prefix tree");
        add(rows, ChallengePlatform.LEETCODE, "union-find", "Union Find", LEETCODE_SOURCE,
                shapes(AlgorithmShape.UNION_FIND), "disjoint set", "dsu");
        add(rows, ChallengePlatform.LEETCODE, "segment-tree", "Segment Tree", LEETCODE_SOURCE,
                shapes(AlgorithmShape.SEGMENT_TREE));
        add(rows, ChallengePlatform.LEETCODE, "fenwick-tree", "Fenwick Tree", LEETCODE_SOURCE,
                shapes(AlgorithmShape.FENWICK_TREE), "binary indexed tree", "bit tree");
        add(rows, ChallengePlatform.LEETCODE, "sorting", "Sorting", LEETCODE_SOURCE,
                sortingShapes(), "sort");
        add(rows, ChallengePlatform.LEETCODE, "intervals", "Intervals", LEETCODE_SOURCE,
                shapes(AlgorithmShape.INTERVAL_MERGE, AlgorithmShape.INTERVAL_JOIN,
                        AlgorithmShape.SWEEP_LINE, AlgorithmShape.INTERVAL_INDEX,
                        AlgorithmShape.COVERAGE),
                "interval", "merge intervals");
        add(rows, ChallengePlatform.LEETCODE, "range-queries", "Range Queries", LEETCODE_SOURCE,
                shapes(AlgorithmShape.SEGMENT_TREE, AlgorithmShape.FENWICK_TREE,
                        AlgorithmShape.SPARSE_TABLE),
                "range query", "range-query");
        add(rows, ChallengePlatform.LEETCODE, "mathematics", "Math / Geometry", LEETCODE_SOURCE,
                shapes(AlgorithmShape.ARITHMETIC, AlgorithmShape.GCD_EUCLID,
                        AlgorithmShape.SIEVE, AlgorithmShape.MODULAR_EXPONENTIATION,
                        AlgorithmShape.EXTENDED_GCD, AlgorithmShape.CHINESE_REMAINDER,
                        AlgorithmShape.MILLER_RABIN, AlgorithmShape.ORIENTATION,
                        AlgorithmShape.CONVEX_HULL),
                "math", "mathematics", "geometry", "computational geometry");
        add(rows, ChallengePlatform.LEETCODE, "concurrency", "Concurrency", LEETCODE_SOURCE,
                shapes(AlgorithmShape.CONCURRENCY_COORDINATION),
                "multithreading", "thread coordination");
        add(rows, ChallengePlatform.LEETCODE, "simulation", "Simulation", LEETCODE_SOURCE,
                shapes(AlgorithmShape.SIMULATION));

        // HackerRank Algorithms domains.
        add(rows, ChallengePlatform.HACKERRANK, "warmup", "Warmup", HACKERRANK_ALGORITHMS_SOURCE,
                shapes(AlgorithmShape.LINEAR_SCAN, AlgorithmShape.ARITHMETIC));
        add(rows, ChallengePlatform.HACKERRANK, "implementation", "Implementation",
                HACKERRANK_ALGORITHMS_SOURCE,
                shapes(AlgorithmShape.SIMULATION, AlgorithmShape.MATRIX_SCAN,
                        AlgorithmShape.STRING_TRANSFORM));
        add(rows, ChallengePlatform.HACKERRANK, "strings", "Strings",
                HACKERRANK_ALGORITHMS_SOURCE,
                shapes(AlgorithmShape.STRING_TRANSFORM, AlgorithmShape.KMP, AlgorithmShape.TRIE,
                        AlgorithmShape.RABIN_KARP, AlgorithmShape.Z_ALGORITHM,
                        AlgorithmShape.AHO_CORASICK, AlgorithmShape.SUBSEQUENCE,
                        AlgorithmShape.FREQUENCY_COUNT), "string");
        add(rows, ChallengePlatform.HACKERRANK, "sorting", "Sorting",
                HACKERRANK_ALGORITHMS_SOURCE, sortingShapes(), "sort");
        add(rows, ChallengePlatform.HACKERRANK, "search", "Search",
                HACKERRANK_ALGORITHMS_SOURCE,
                shapes(AlgorithmShape.BINARY_SEARCH, AlgorithmShape.EXPONENTIAL_SEARCH,
                        AlgorithmShape.GALLOPING_SEARCH, AlgorithmShape.LINEAR_SCAN,
                        AlgorithmShape.BFS, AlgorithmShape.DFS, AlgorithmShape.DIJKSTRA));
        add(rows, ChallengePlatform.HACKERRANK, "graph-theory", "Graph Theory",
                HACKERRANK_ALGORITHMS_SOURCE,
                shapes(AlgorithmShape.BFS, AlgorithmShape.DFS, AlgorithmShape.DIJKSTRA,
                        AlgorithmShape.TOPOLOGICAL_SORT, AlgorithmShape.UNION_FIND,
                        AlgorithmShape.CONNECTED_COMPONENTS, AlgorithmShape.PRIM_MST,
                        AlgorithmShape.KRUSKAL_MST, AlgorithmShape.STRONGLY_CONNECTED_COMPONENTS,
                        AlgorithmShape.BELLMAN_FORD, AlgorithmShape.FLOYD_WARSHALL,
                        AlgorithmShape.MAX_FLOW), "graphs", "graph");
        add(rows, ChallengePlatform.HACKERRANK, "greedy", "Greedy",
                HACKERRANK_ALGORITHMS_SOURCE, shapes(AlgorithmShape.GREEDY));
        add(rows, ChallengePlatform.HACKERRANK, "dynamic-programming", "Dynamic Programming",
                HACKERRANK_ALGORITHMS_SOURCE,
                shapes(AlgorithmShape.DYNAMIC_PROGRAMMING, AlgorithmShape.MEMOIZATION,
                        AlgorithmShape.LONGEST_COMMON_SUBSEQUENCE,
                        AlgorithmShape.LONGEST_INCREASING_SUBSEQUENCE,
                        AlgorithmShape.KNAPSACK_01, AlgorithmShape.COIN_CHANGE,
                        AlgorithmShape.INTERVAL_DP, AlgorithmShape.BITMASK_DP,
                        AlgorithmShape.DIGIT_DP), "dp");
        add(rows, ChallengePlatform.HACKERRANK, "constructive-algorithms",
                "Constructive Algorithms", HACKERRANK_ALGORITHMS_SOURCE,
                shapes(AlgorithmShape.GREEDY, AlgorithmShape.SIMULATION), "constructive");
        add(rows, ChallengePlatform.HACKERRANK, "bit-manipulation", "Bit Manipulation",
                HACKERRANK_ALGORITHMS_SOURCE,
                shapes(AlgorithmShape.BIT_MANIPULATION, AlgorithmShape.XOR_BASIS,
                        AlgorithmShape.BITSET_DP), "bitwise");
        add(rows, ChallengePlatform.HACKERRANK, "recursion", "Recursion",
                HACKERRANK_ALGORITHMS_SOURCE,
                shapes(AlgorithmShape.DFS, AlgorithmShape.BACKTRACKING,
                        AlgorithmShape.MEMOIZATION, AlgorithmShape.TREE_TRAVERSAL));
        add(rows, ChallengePlatform.HACKERRANK, "game-theory", "Game Theory",
                HACKERRANK_ALGORITHMS_SOURCE, List.of());
        add(rows, ChallengePlatform.HACKERRANK, "np-complete", "NP Complete",
                HACKERRANK_ALGORITHMS_SOURCE,
                shapes(AlgorithmShape.CONSTRAINT_SEARCH, AlgorithmShape.BRANCH_AND_BOUND,
                        AlgorithmShape.BACKTRACKING), "np-complete problems");
        add(rows, ChallengePlatform.HACKERRANK, "debugging", "Debugging",
                HACKERRANK_ALGORITHMS_SOURCE, List.of());

        // HackerRank Data Structures domains.
        add(rows, ChallengePlatform.HACKERRANK, "arrays", "Arrays",
                HACKERRANK_STRUCTURES_SOURCE,
                shapes(AlgorithmShape.LINEAR_SCAN, AlgorithmShape.TWO_POINTER,
                        AlgorithmShape.SLIDING_WINDOW, AlgorithmShape.PREFIX_SCAN,
                        AlgorithmShape.DIFFERENCE_SCAN, AlgorithmShape.FREQUENCY_COUNT), "array");
        add(rows, ChallengePlatform.HACKERRANK, "linked-lists", "Linked Lists",
                HACKERRANK_STRUCTURES_SOURCE,
                shapes(AlgorithmShape.LINKED_LIST_REWRITE, AlgorithmShape.FAST_SLOW_POINTER),
                "linked list");
        add(rows, ChallengePlatform.HACKERRANK, "trees", "Trees",
                HACKERRANK_STRUCTURES_SOURCE,
                shapes(AlgorithmShape.TREE_TRAVERSAL, AlgorithmShape.DFS, AlgorithmShape.BFS,
                        AlgorithmShape.LOWEST_COMMON_ANCESTOR, AlgorithmShape.BINARY_LIFTING,
                        AlgorithmShape.EULER_TOUR), "tree");
        add(rows, ChallengePlatform.HACKERRANK, "balanced-trees", "Balanced Trees",
                HACKERRANK_STRUCTURES_SOURCE, shapes(AlgorithmShape.TREE_TRAVERSAL),
                "balanced tree");
        add(rows, ChallengePlatform.HACKERRANK, "stacks", "Stacks",
                HACKERRANK_STRUCTURES_SOURCE,
                shapes(AlgorithmShape.STACK_MACHINE, AlgorithmShape.MONOTONIC_STACK), "stack");
        add(rows, ChallengePlatform.HACKERRANK, "queues", "Queues",
                HACKERRANK_STRUCTURES_SOURCE,
                shapes(AlgorithmShape.QUEUE_MACHINE, AlgorithmShape.BFS,
                        AlgorithmShape.MONOTONIC_QUEUE), "queue", "deque");
        add(rows, ChallengePlatform.HACKERRANK, "heap", "Heap",
                HACKERRANK_STRUCTURES_SOURCE,
                shapes(AlgorithmShape.PRIORITY_QUEUE, AlgorithmShape.HEAP_SELECT,
                        AlgorithmShape.HEAP_SORT), "priority queue");
        add(rows, ChallengePlatform.HACKERRANK, "disjoint-set", "Disjoint Set",
                HACKERRANK_STRUCTURES_SOURCE, shapes(AlgorithmShape.UNION_FIND),
                "union find", "dsu");
        add(rows, ChallengePlatform.HACKERRANK, "multiple-choice", "Multiple Choice",
                HACKERRANK_STRUCTURES_SOURCE, List.of());
        add(rows, ChallengePlatform.HACKERRANK, "trie", "Trie",
                HACKERRANK_STRUCTURES_SOURCE,
                shapes(AlgorithmShape.TRIE, AlgorithmShape.AHO_CORASICK), "prefix tree");
        add(rows, ChallengePlatform.HACKERRANK, "advanced", "Advanced",
                HACKERRANK_STRUCTURES_SOURCE,
                shapes(AlgorithmShape.SEGMENT_TREE, AlgorithmShape.FENWICK_TREE,
                        AlgorithmShape.SPARSE_TABLE, AlgorithmShape.BINARY_LIFTING));

        // GeeksforGeeks Practice topic filter.
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "mathematics", "Mathematics", GFG_SOURCE,
                shapes(AlgorithmShape.ARITHMETIC, AlgorithmShape.GCD_EUCLID,
                        AlgorithmShape.SIEVE, AlgorithmShape.MODULAR_EXPONENTIATION,
                        AlgorithmShape.EXTENDED_GCD, AlgorithmShape.CHINESE_REMAINDER,
                        AlgorithmShape.MILLER_RABIN), "math");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "recursion", "Recursion", GFG_SOURCE,
                shapes(AlgorithmShape.DFS, AlgorithmShape.BACKTRACKING,
                        AlgorithmShape.MEMOIZATION));
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "bit-magic", "Bit Magic", GFG_SOURCE,
                shapes(AlgorithmShape.BIT_MANIPULATION, AlgorithmShape.XOR_BASIS,
                        AlgorithmShape.BITSET_DP), "bit manipulation");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "arrays", "Arrays", GFG_SOURCE,
                shapes(AlgorithmShape.LINEAR_SCAN, AlgorithmShape.TWO_POINTER,
                        AlgorithmShape.SLIDING_WINDOW, AlgorithmShape.PREFIX_SCAN,
                        AlgorithmShape.KADANE), "array");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "matrix", "Matrix", GFG_SOURCE,
                shapes(AlgorithmShape.MATRIX_SCAN), "matrices");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "strings", "Strings", GFG_SOURCE,
                shapes(AlgorithmShape.STRING_TRANSFORM, AlgorithmShape.KMP,
                        AlgorithmShape.RABIN_KARP, AlgorithmShape.Z_ALGORITHM,
                        AlgorithmShape.MANACHER, AlgorithmShape.AHO_CORASICK), "string");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "searching", "Searching", GFG_SOURCE,
                shapes(AlgorithmShape.BINARY_SEARCH, AlgorithmShape.EXPONENTIAL_SEARCH,
                        AlgorithmShape.GALLOPING_SEARCH, AlgorithmShape.INTERPOLATION_SEARCH,
                        AlgorithmShape.JUMP_SEARCH, AlgorithmShape.FIBONACCI_SEARCH,
                        AlgorithmShape.TERNARY_SEARCH, AlgorithmShape.LINEAR_SCAN),
                "search");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "sorting", "Sorting", GFG_SOURCE,
                sortingShapes(), "sort");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "hashing", "Hashing", GFG_SOURCE,
                shapes(AlgorithmShape.HASH_MEMBERSHIP, AlgorithmShape.FREQUENCY_COUNT,
                        AlgorithmShape.PAIR_SUM_HASH), "hash table");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "two-pointers", "Two Pointers", GFG_SOURCE,
                shapes(AlgorithmShape.TWO_POINTER, AlgorithmShape.FAST_SLOW_POINTER),
                "two pointer");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "sliding-window", "Sliding Window",
                GFG_SOURCE,
                shapes(AlgorithmShape.SLIDING_WINDOW, AlgorithmShape.MONOTONIC_QUEUE), "window");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "prefix-sum", "Prefix Sum", GFG_SOURCE,
                shapes(AlgorithmShape.PREFIX_SCAN, AlgorithmShape.DIFFERENCE_SCAN),
                "prefix sums");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "linked-list", "Linked List", GFG_SOURCE,
                shapes(AlgorithmShape.LINKED_LIST_REWRITE, AlgorithmShape.FAST_SLOW_POINTER),
                "linked lists");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "stack", "Stack", GFG_SOURCE,
                shapes(AlgorithmShape.STACK_MACHINE, AlgorithmShape.MONOTONIC_STACK));
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "queue", "Queue", GFG_SOURCE,
                shapes(AlgorithmShape.QUEUE_MACHINE, AlgorithmShape.BFS,
                        AlgorithmShape.MONOTONIC_QUEUE), "queues");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "deque", "Deque", GFG_SOURCE,
                shapes(AlgorithmShape.QUEUE_MACHINE, AlgorithmShape.MONOTONIC_QUEUE));
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "tree", "Tree", GFG_SOURCE,
                shapes(AlgorithmShape.TREE_TRAVERSAL, AlgorithmShape.DFS, AlgorithmShape.BFS,
                        AlgorithmShape.LOWEST_COMMON_ANCESTOR, AlgorithmShape.BINARY_LIFTING,
                        AlgorithmShape.EULER_TOUR), "trees");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "binary-search-tree",
                "Binary Search Tree", GFG_SOURCE,
                shapes(AlgorithmShape.TREE_TRAVERSAL, AlgorithmShape.BINARY_SEARCH), "bst");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "heap", "Heap", GFG_SOURCE,
                shapes(AlgorithmShape.PRIORITY_QUEUE, AlgorithmShape.HEAP_SELECT,
                        AlgorithmShape.HEAP_SORT), "priority queue");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "graph", "Graph", GFG_SOURCE,
                shapes(AlgorithmShape.BFS, AlgorithmShape.DFS, AlgorithmShape.DIJKSTRA,
                        AlgorithmShape.TOPOLOGICAL_SORT, AlgorithmShape.UNION_FIND,
                        AlgorithmShape.CONNECTED_COMPONENTS, AlgorithmShape.PRIM_MST,
                        AlgorithmShape.KRUSKAL_MST, AlgorithmShape.STRONGLY_CONNECTED_COMPONENTS,
                        AlgorithmShape.BRIDGES_ARTICULATION, AlgorithmShape.BELLMAN_FORD,
                        AlgorithmShape.FLOYD_WARSHALL, AlgorithmShape.MAX_FLOW),
                "graph theory");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "greedy", "Greedy", GFG_SOURCE,
                shapes(AlgorithmShape.GREEDY));
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "backtracking", "Backtracking", GFG_SOURCE,
                shapes(AlgorithmShape.BACKTRACKING, AlgorithmShape.CONSTRAINT_SEARCH));
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "dynamic-programming",
                "Dynamic Programming", GFG_SOURCE,
                shapes(AlgorithmShape.DYNAMIC_PROGRAMMING, AlgorithmShape.MEMOIZATION,
                        AlgorithmShape.LONGEST_COMMON_SUBSEQUENCE,
                        AlgorithmShape.LONGEST_INCREASING_SUBSEQUENCE,
                        AlgorithmShape.COIN_CHANGE, AlgorithmShape.KNAPSACK_01,
                        AlgorithmShape.INTERVAL_DP, AlgorithmShape.BITMASK_DP,
                        AlgorithmShape.DIGIT_DP), "dp");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "trie", "Trie", GFG_SOURCE,
                shapes(AlgorithmShape.TRIE, AlgorithmShape.AHO_CORASICK), "prefix tree");
        add(rows, ChallengePlatform.GEEKSFORGEEKS, "segment-tree", "Segment Tree", GFG_SOURCE,
                shapes(AlgorithmShape.SEGMENT_TREE));

        rows.sort(Comparator.comparing((Category row) -> row.platform().name())
                .thenComparing(Category::id));
        rejectDuplicates(rows);
        return List.copyOf(rows);
    }

    private static List<AlgorithmShape> sortingShapes() {
        return shapes(
                AlgorithmShape.INSERTION_SORT,
                AlgorithmShape.MERGE_SORT,
                AlgorithmShape.QUICK_SORT,
                AlgorithmShape.HEAP_SORT,
                AlgorithmShape.COUNTING_SORT,
                AlgorithmShape.RADIX_SORT,
                AlgorithmShape.BUBBLE_SORT,
                AlgorithmShape.SELECTION_SORT,
                AlgorithmShape.SHELL_SORT,
                AlgorithmShape.BUCKET_SORT,
                AlgorithmShape.SORT_UNIQUE);
    }

    private static List<AlgorithmShape> shapes(AlgorithmShape... shapes) {
        return List.of(shapes);
    }

    private static void add(
            List<Category> rows,
            ChallengePlatform platform,
            String id,
            String displayName,
            String source,
            List<AlgorithmShape> shapes,
            String... aliases) {
        rows.add(new Category(
                platform,
                id,
                displayName,
                List.of(aliases),
                shapes,
                source,
                AUTHORITY));
    }

    private static void rejectDuplicates(List<Category> rows) {
        LinkedHashSet<String> identities = new LinkedHashSet<>();
        for (Category row : rows) {
            String identity = row.platform().name() + ":" + row.id();
            if (!identities.add(identity)) {
                throw new IllegalStateException("duplicate challenge category: " + identity);
            }
        }
    }

    private static Map<ChallengePlatform, List<Category>> byPlatform() {
        EnumMap<ChallengePlatform, List<Category>> result =
                new EnumMap<>(ChallengePlatform.class);
        for (ChallengePlatform platform : ChallengePlatform.values()) {
            if (platform == ChallengePlatform.OTHER) continue;
            result.put(platform, CATEGORIES.stream()
                    .filter(row -> row.platform() == platform)
                    .toList());
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<AlgorithmShape, List<Category>> byShape() {
        EnumMap<AlgorithmShape, List<Category>> result = new EnumMap<>(AlgorithmShape.class);
        for (AlgorithmShape shape : AlgorithmShape.values()) {
            List<Category> rows = CATEGORIES.stream()
                    .filter(row -> row.mapsTo(shape))
                    .toList();
            if (!rows.isEmpty()) result.put(shape, rows);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Category> lookup() {
        LinkedHashMap<String, Category> result = new LinkedHashMap<>();
        for (Category row : CATEGORIES) {
            for (String alias : row.aliases()) {
                String key = key(row.platform(), alias);
                Category previous = result.putIfAbsent(key, row);
                if (previous != null && previous != row) {
                    throw new IllegalStateException(
                            "challenge category alias collision: " + row.platform() + ":" + alias);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static String key(ChallengePlatform platform, String label) {
        return platform.name() + ":" + normalize(label);
    }

    private static String normalize(String value) {
        String source = required(value, "label").toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(source.length());
        boolean separator = false;
        for (int index = 0; index < source.length(); index++) {
            char valueChar = source.charAt(index);
            if (Character.isLetterOrDigit(valueChar)) {
                if (separator && !result.isEmpty()) result.append('-');
                result.append(valueChar);
                separator = false;
            } else {
                separator = true;
            }
        }
        return result.toString();
    }

    private static String identifier(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) throw new IllegalArgumentException("id");
        return normalized;
    }

    private static String required(String value, String field) {
        String normalized = Objects.toString(value, "").strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field);
        return normalized;
    }

    private static String root(List<Category> rows) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "SYNEXIA_CHALLENGE_CATEGORY_CATALOG_V1");
            for (Category row : rows) {
                update(digest, row.platform().name());
                update(digest, row.id());
                update(digest, row.displayName());
                for (String alias : row.aliases()) update(digest, alias);
                for (AlgorithmShape shape : row.shapes()) update(digest, shape.name());
                update(digest, row.source());
                update(digest, row.authority());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
