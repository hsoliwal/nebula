// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.util.List;
import java.util.Objects;

/** Exhaustive projection from canonical computational shape to enterprise template style. */
public final class ProblemTemplateCatalog {
    private ProblemTemplateCatalog() {}

    public static TemplateStyle style(AlgorithmShape shape) {
        Objects.requireNonNull(shape, "shape");
        return switch (shape) {
            case BINARY_SEARCH, EXPONENTIAL_SEARCH, GALLOPING_SEARCH, INTERPOLATION_SEARCH,
                    JUMP_SEARCH, FIBONACCI_SEARCH, TERNARY_SEARCH, QUICKSELECT ->
                    s(EnterpriseTemplateKind.SEARCH, "find", "locate", "threshold");
            case HEAP_SELECT, BOYER_MOORE_MAJORITY ->
                    s(EnterpriseTemplateKind.RANK, "rank", "top", "select");
            case SORTED_INSERT ->
                    s(EnterpriseTemplateKind.ORDER, "maintain-order", "insert");
            case HASH_MEMBERSHIP ->
                    s(EnterpriseTemplateKind.MEMBERSHIP, "contains", "lookup", "validate");
            case PAIR_SUM_HASH ->
                    s(EnterpriseTemplateKind.MATCH, "pair", "find-pair", "match-sum");
            case SORT_UNIQUE ->
                    s(EnterpriseTemplateKind.DEDUPLICATE, "deduplicate", "canonicalize");
            case TWO_POINTER, FAST_SLOW_POINTER, PARTITION_WALK, MERGE_WALK ->
                    s(EnterpriseTemplateKind.POINTER_WALK, "pair", "scan", "reconcile");
            case BFS, DFS, BIDIRECTIONAL_SEARCH, BEST_FIRST, CONNECTED_COMPONENTS ->
                    s(EnterpriseTemplateKind.TRAVERSE, "traverse", "discover", "explore");
            case DIJKSTRA, A_STAR, BELLMAN_FORD, FLOYD_WARSHALL, ZERO_ONE_BFS ->
                    s(EnterpriseTemplateKind.ROUTE, "route", "shortest", "cheapest");
            case TOPOLOGICAL_SORT ->
                    s(EnterpriseTemplateKind.DEPENDENCY_ORDER, "order-dependencies", "plan");
            case UNION_FIND, STRONGLY_CONNECTED_COMPONENTS, BRIDGES_ARTICULATION,
                    GRID_CONNECTED_COMPONENTS ->
                    s(EnterpriseTemplateKind.CONNECTIVITY, "connect", "group", "component", "regions");
            case MAX_FLOW, MIN_COST_MAX_FLOW ->
                    s(EnterpriseTemplateKind.FLOW, "allocate-flow", "capacity", "throughput");
            case SLIDING_WINDOW, MONOTONIC_QUEUE ->
                    s(EnterpriseTemplateKind.WINDOW, "window", "rolling", "recent");
            case MONOTONIC_STACK ->
                    s(EnterpriseTemplateKind.STACK_QUEUE, "next-greater", "span", "boundary");
            case PREFIX_SCAN, DIFFERENCE_SCAN ->
                    s(EnterpriseTemplateKind.SCAN, "cumulative", "difference", "range-update");
            case KADANE, LONGEST_INCREASING_SUBSEQUENCE, SUBSEQUENCE, LONGEST_COMMON_SUBSEQUENCE ->
                    s(EnterpriseTemplateKind.SEQUENCE, "sequence", "trend", "alignment");
            case KMP, BOYER_MOORE_HORSPOOL, RABIN_KARP, Z_ALGORITHM, MANACHER, AHO_CORASICK ->
                    s(EnterpriseTemplateKind.TEXT_INDEX, "find-text", "detect", "match-text");
            case DYNAMIC_PROGRAMMING, MEMOIZATION, BRANCH_AND_BOUND, CONSTRAINT_SEARCH,
                    COIN_CHANGE, KNAPSACK_01, INTERVAL_DP, BITMASK_DP, DIGIT_DP ->
                    s(EnterpriseTemplateKind.OPTIMIZE, "optimize", "plan", "score");
            case BACKTRACKING ->
                    s(EnterpriseTemplateKind.BACKTRACK, "enumerate", "solve", "search-space");
            case GREEDY ->
                    s(EnterpriseTemplateKind.GREEDY, "choose", "allocate", "schedule");
            case ORDERED_MERGE, MULTIWAY_MERGE ->
                    s(EnterpriseTemplateKind.MERGE, "merge", "fan-in", "reconcile");
            case INTERVAL_MERGE, INTERVAL_JOIN, SWEEP_LINE, INTERVAL_INDEX, COVERAGE ->
                    s(EnterpriseTemplateKind.INTERVAL, "availability", "overlap", "coverage");
            case BIPARTITE_MATCHING, ASSIGNMENT, STABLE_MATCHING, RANKED_MATCHING ->
                    s(EnterpriseTemplateKind.MATCH, "match", "assign", "dispatch");
            case LRU ->
                    s(EnterpriseTemplateKind.CACHE, "cache", "retain-hot");
            case PRIORITY_QUEUE ->
                    s(EnterpriseTemplateKind.SCHEDULE, "schedule", "prioritize");
            case BOUNDED_RETRY ->
                    s(EnterpriseTemplateKind.RESILIENCE, "retry", "recover");
            case TOKEN_BUCKET ->
                    s(EnterpriseTemplateKind.RATE_LIMIT, "throttle", "rate-limit");
            case RENDEZVOUS_HASH ->
                    s(EnterpriseTemplateKind.SHARD, "shard", "place", "distribute");
            case STATE_TABLE ->
                    s(EnterpriseTemplateKind.STATE, "transition", "workflow-state");
            case REDUCE ->
                    s(EnterpriseTemplateKind.AGGREGATE, "aggregate", "sum", "reduce");
            case MAP ->
                    s(EnterpriseTemplateKind.TRANSFORM, "transform", "map");
            case PARTITION, DUTCH_FLAG ->
                    s(EnterpriseTemplateKind.PARTITION, "partition", "classify");
            case INSERTION_SORT, MERGE_SORT, QUICK_SORT, HEAP_SORT, COUNTING_SORT, RADIX_SORT ->
                    s(EnterpriseTemplateKind.SORT, "sort", "order");
            case FENWICK_TREE, SEGMENT_TREE, SPARSE_TABLE ->
                    s(EnterpriseTemplateKind.RANGE_QUERY, "range-query", "aggregate-range");
            case TRIE ->
                    s(EnterpriseTemplateKind.TEXT_INDEX, "prefix", "autocomplete", "dictionary");
            case TREE_TRAVERSAL, LOWEST_COMMON_ANCESTOR, BINARY_LIFTING, EULER_TOUR ->
                    s(EnterpriseTemplateKind.TREE, "hierarchy", "ancestor", "tree-query");
            case PRIM_MST, KRUSKAL_MST ->
                    s(EnterpriseTemplateKind.CONNECTIVITY, "minimum-network", "connect-cheapest");
            case EULERIAN_PATH ->
                    s(EnterpriseTemplateKind.ROUTE, "visit-every-edge", "route");
            case GCD_EUCLID, SIEVE, MODULAR_EXPONENTIATION, EXTENDED_GCD,
                    CHINESE_REMAINDER, MILLER_RABIN ->
                    s(EnterpriseTemplateKind.NUMBER_THEORY, "numeric", "validate-number");
            case XOR_BASIS, BITSET_DP ->
                    s(EnterpriseTemplateKind.BIT, "bit-optimize", "compact-state");
            case ORIENTATION, CONVEX_HULL ->
                    s(EnterpriseTemplateKind.GEOMETRY, "spatial", "boundary", "orientation");
            case LINEAR_SCAN ->
                    s(EnterpriseTemplateKind.SCAN, "scan", "summarize", "inspect");
            case FREQUENCY_COUNT ->
                    s(EnterpriseTemplateKind.AGGREGATE, "count", "histogram", "frequency");
            case FREQUENCY_L1_DISTANCE ->
                    s(EnterpriseTemplateKind.DIFFERENCE, "compare-bags", "frequency-difference", "reconcile");
            case STACK_MACHINE, QUEUE_MACHINE ->
                    s(EnterpriseTemplateKind.STACK_QUEUE, "push", "pop", "buffer");
            case LINKED_LIST_REWRITE ->
                    s(EnterpriseTemplateKind.TRANSFORM, "rewrite-list", "reverse", "relink");
            case MATRIX_SCAN ->
                    s(EnterpriseTemplateKind.TRAVERSE, "scan-matrix", "grid", "visit-cells");
            case STRING_TRANSFORM ->
                    s(EnterpriseTemplateKind.TRANSFORM, "transform-text", "normalize", "rewrite");
            case BIT_MANIPULATION ->
                    s(EnterpriseTemplateKind.BIT, "bit-transform", "xor", "mask");
            case ARITHMETIC ->
                    s(EnterpriseTemplateKind.ARITHMETIC, "calculate", "numeric-transform", "decimal");
            case CONCURRENCY_COORDINATION ->
                    s(EnterpriseTemplateKind.CONCURRENCY, "coordinate", "phase", "sequence-workers");
            case SIMULATION ->
                    s(EnterpriseTemplateKind.STATE, "simulate", "step", "apply-events");
            case BUBBLE_SORT, SELECTION_SORT, SHELL_SORT, BUCKET_SORT ->
                    s(EnterpriseTemplateKind.SORT, "sort", "order");
        };
    }

    private static TemplateStyle s(EnterpriseTemplateKind kind, String... verbs) {
        return new TemplateStyle(kind, List.of(verbs), "encode -> canonical donor -> decode");
    }
}
