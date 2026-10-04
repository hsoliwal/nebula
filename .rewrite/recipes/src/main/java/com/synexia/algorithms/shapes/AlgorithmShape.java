// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

/** Canonical mechanical shape. Many domain patterns can map to the same shape. */
public enum AlgorithmShape {
    BINARY_SEARCH,
    EXPONENTIAL_SEARCH,
    GALLOPING_SEARCH,
    HEAP_SELECT,
    QUICKSELECT,
    SORTED_INSERT,
    HASH_MEMBERSHIP,
    PAIR_SUM_HASH,
    SORT_UNIQUE,
    TWO_POINTER,
    FAST_SLOW_POINTER,
    PARTITION_WALK,
    MERGE_WALK,
    BFS,
    DFS,
    BIDIRECTIONAL_SEARCH,
    BEST_FIRST,
    DIJKSTRA,
    TOPOLOGICAL_SORT,
    UNION_FIND,
    CONNECTED_COMPONENTS,
    GRID_CONNECTED_COMPONENTS,
    MAX_FLOW,
    SLIDING_WINDOW,
    MONOTONIC_QUEUE,
    MONOTONIC_STACK,
    PREFIX_SCAN,
    DIFFERENCE_SCAN,
    KADANE,
    LONGEST_INCREASING_SUBSEQUENCE,
    SUBSEQUENCE,
    KMP,
    BOYER_MOORE_HORSPOOL,
    DYNAMIC_PROGRAMMING,
    MEMOIZATION,
    BACKTRACKING,
    BRANCH_AND_BOUND,
    GREEDY,
    CONSTRAINT_SEARCH,
    ORDERED_MERGE,
    INTERVAL_MERGE,
    INTERVAL_JOIN,
    SWEEP_LINE,
    INTERVAL_INDEX,
    COVERAGE,
    BIPARTITE_MATCHING,
    ASSIGNMENT,
    STABLE_MATCHING,
    RANKED_MATCHING,
    LRU,
    PRIORITY_QUEUE,
    BOUNDED_RETRY,
    TOKEN_BUCKET,
    RENDEZVOUS_HASH,
    STATE_TABLE,
    REDUCE,
    MAP,
    PARTITION,
    MULTIWAY_MERGE,

    // Sorting / array structure
    INSERTION_SORT,
    MERGE_SORT,
    QUICK_SORT,
    HEAP_SORT,
    COUNTING_SORT,
    RADIX_SORT,
    DUTCH_FLAG,
    BOYER_MOORE_MAJORITY,

    // Range / indexing
    FENWICK_TREE,
    SEGMENT_TREE,
    SPARSE_TABLE,

    // String / text
    TRIE,
    RABIN_KARP,
    Z_ALGORITHM,
    MANACHER,
    AHO_CORASICK,

    // Trees
    TREE_TRAVERSAL,
    LOWEST_COMMON_ANCESTOR,
    BINARY_LIFTING,
    EULER_TOUR,

    // Graph families not represented above
    A_STAR,
    BELLMAN_FORD,
    FLOYD_WARSHALL,
    ZERO_ONE_BFS,
    PRIM_MST,
    KRUSKAL_MST,
    STRONGLY_CONNECTED_COMPONENTS,
    BRIDGES_ARTICULATION,
    EULERIAN_PATH,
    MIN_COST_MAX_FLOW,

    // Specialized dynamic-programming recurrence families
    LONGEST_COMMON_SUBSEQUENCE,
    COIN_CHANGE,
    KNAPSACK_01,
    INTERVAL_DP,
    BITMASK_DP,
    DIGIT_DP,

    // Number theory
    GCD_EUCLID,
    SIEVE,
    MODULAR_EXPONENTIATION,
    EXTENDED_GCD,
    CHINESE_REMAINDER,
    MILLER_RABIN,

    // Bit-oriented kernels
    XOR_BASIS,
    BITSET_DP,

    // Computational geometry
    ORIENTATION,
    CONVEX_HULL,

    // Generic production shapes used by broad problem corpora
    LINEAR_SCAN,
    FREQUENCY_COUNT,
    FREQUENCY_L1_DISTANCE,
    STACK_MACHINE,
    QUEUE_MACHINE,
    LINKED_LIST_REWRITE,
    MATRIX_SCAN,
    STRING_TRANSFORM,
    BIT_MANIPULATION,
    ARITHMETIC,
    CONCURRENCY_COORDINATION,
    SIMULATION,

    // Additional distinct sorting mechanics present in donor corpora
    BUBBLE_SORT,
    SELECTION_SORT,
    SHELL_SORT,
    BUCKET_SORT,

    // Ordered-search shapes appended to preserve existing enum ordinals.
    INTERPOLATION_SEARCH,
    JUMP_SEARCH,
    FIBONACCI_SEARCH,
    TERNARY_SEARCH
}
