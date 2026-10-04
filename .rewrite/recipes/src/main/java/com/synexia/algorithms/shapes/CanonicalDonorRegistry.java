// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import com.synexia.algorithms.core.ProgressAlgorithm;
import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Executable representative for every canonical computational shape.
 *
 * <p>This is the dynamic bridge used by corpus tooling. Production code should normally use the
 * typed donor or an enterprise template, while discovery/analysis code can resolve by
 * {@link AlgorithmShape}. Startup fails if any shape lacks an implementation.
 */
public final class CanonicalDonorRegistry {

    public record Entry(
            AlgorithmDescriptor descriptor,
            ProgressAlgorithm<Object, Object> algorithm) {

        public Entry {
            Objects.requireNonNull(descriptor, "descriptor");
            Objects.requireNonNull(algorithm, "algorithm");
        }

        public Object execute(Object input, IProgressMonitor monitor) {
            return algorithm.execute(input, ProgressMonitors.nonNull(monitor));
        }
    }

    private static final Map<AlgorithmShape, Entry> ENTRIES = build();

    private CanonicalDonorRegistry() {}

    public static Map<AlgorithmShape, Entry> all() {
        return ENTRIES;
    }

    public static Entry require(AlgorithmShape shape) {
        Entry entry = ENTRIES.get(Objects.requireNonNull(shape, "shape"));
        if (entry == null) throw new IllegalArgumentException("no donor for shape " + shape);
        return entry;
    }

    public static boolean supports(AlgorithmShape shape) {
        return ENTRIES.containsKey(Objects.requireNonNull(shape, "shape"));
    }

    private static Map<AlgorithmShape, Entry> build() {
        EnumMap<AlgorithmShape, Entry> values = new EnumMap<>(AlgorithmShape.class);
        put(values, CanonicalAlgorithmDonors4Graph.aStar()); // A_STAR
        put(values, CanonicalAlgorithmDonors3.ahoCorasickCounts()); // AHO_CORASICK
        put(values, CanonicalAlgorithmDonors2.assignment()); // ASSIGNMENT
        put(values, CanonicalAlgorithmDonors.subsetSumBacktracking()); // BACKTRACKING
        put(values, CanonicalAlgorithmDonors4Graph.bellmanFord()); // BELLMAN_FORD
        put(values, CanonicalAlgorithmDonors2.bestFirst()); // BEST_FIRST
        put(values, PrimitiveShapeAlgorithms.breadthFirstSearch()); // BFS
        put(values, CanonicalAlgorithmDonors.bidirectionalReachability()); // BIDIRECTIONAL_SEARCH
        put(values, CanonicalAlgorithmDonors3.binaryLifting()); // BINARY_LIFTING
        put(values, PrimitiveShapeAlgorithms.binarySearch()); // BINARY_SEARCH
        put(values, PrimitiveShapeAlgorithms.interpolationSearch()); // INTERPOLATION_SEARCH
        put(values, CanonicalAlgorithmDonors.bipartiteMatching()); // BIPARTITE_MATCHING
        put(values, CanonicalAlgorithmDonors5MathDp.bitmaskDp()); // BITMASK_DP
        put(values, CanonicalAlgorithmDonors5MathDp.bitsetDp()); // BITSET_DP
        put(values, StatefulShapeAlgorithms.retryIdentity(2)); // BOUNDED_RETRY
        put(values, CanonicalAlgorithmDonors3.majorityVote()); // BOYER_MOORE_MAJORITY
        put(values, CanonicalAlgorithmDonors.branchAndBoundKnapsack()); // BRANCH_AND_BOUND
        put(values, CanonicalAlgorithmDonors4Graph.bridges()); // BRIDGES_ARTICULATION
        put(values, CanonicalAlgorithmDonors5MathDp.chineseRemainder()); // CHINESE_REMAINDER
        put(values, CanonicalAlgorithmDonors5MathDp.coinChange()); // COIN_CHANGE
        put(values, CanonicalAlgorithmDonors.connectedComponents()); // CONNECTED_COMPONENTS
        put(values, CanonicalAlgorithmDonors8Grid.gridConnectedComponents()); // GRID_CONNECTED_COMPONENTS
        put(values, CanonicalAlgorithmDonors2.nQueensConstraintSearch()); // CONSTRAINT_SEARCH
        put(values, CanonicalAlgorithmDonors5MathDp.convexHull()); // CONVEX_HULL
        put(values, CanonicalAlgorithmDonors3.countingSort()); // COUNTING_SORT
        put(values, CanonicalAlgorithmDonors.coveredLength()); // COVERAGE
        put(values, CanonicalAlgorithmDonors.depthFirstSearch()); // DFS
        put(values, CanonicalAlgorithmDonors2.differenceScan()); // DIFFERENCE_SCAN
        put(values, CanonicalAlgorithmDonors5MathDp.digitDp()); // DIGIT_DP
        put(values, PrimitiveShapeAlgorithms.dijkstra()); // DIJKSTRA
        put(values, CanonicalAlgorithmDonors3.dutchFlag()); // DUTCH_FLAG
        put(values, PrimitiveShapeAlgorithms.editDistance()); // DYNAMIC_PROGRAMMING
        put(values, CanonicalAlgorithmDonors3.eulerTour()); // EULER_TOUR
        put(values, CanonicalAlgorithmDonors4Graph.eulerianPath()); // EULERIAN_PATH
        put(values, CanonicalAlgorithmDonors.exponentialSearch()); // EXPONENTIAL_SEARCH
        put(values, CanonicalAlgorithmDonors5MathDp.extendedGcd()); // EXTENDED_GCD
        put(values, CanonicalAlgorithmDonors.fastSlowCycle()); // FAST_SLOW_POINTER
        put(values, CanonicalAlgorithmDonors3.fenwickRangeSum()); // FENWICK_TREE
        put(values, CanonicalAlgorithmDonors4Graph.floydWarshall()); // FLOYD_WARSHALL
        put(values, CanonicalAlgorithmDonors2.gallopingSearch()); // GALLOPING_SEARCH
        put(values, CanonicalAlgorithmDonors2.jumpSearch()); // JUMP_SEARCH
        put(values, CanonicalAlgorithmDonors2.fibonacciSearch()); // FIBONACCI_SEARCH
        put(values, CanonicalAlgorithmDonors5MathDp.gcdEuclid()); // GCD_EUCLID
        put(values, CanonicalAlgorithmDonors.greedyIntervalSchedule()); // GREEDY
        put(values, PrimitiveShapeAlgorithms.membership()); // HASH_MEMBERSHIP
        put(values, CanonicalAlgorithmDonors7Consolidated.pairSumHash()); // PAIR_SUM_HASH
        put(values, PrimitiveShapeAlgorithms.topK()); // HEAP_SELECT
        put(values, CanonicalAlgorithmDonors3.heapSort()); // HEAP_SORT
        put(values, CanonicalAlgorithmDonors3.insertionSort()); // INSERTION_SORT
        put(values, CanonicalAlgorithmDonors5MathDp.intervalDp()); // INTERVAL_DP
        put(values, CanonicalAlgorithmDonors2.intervalIndex()); // INTERVAL_INDEX
        put(values, CanonicalAlgorithmDonors2.intervalJoin()); // INTERVAL_JOIN
        put(values, CanonicalAlgorithmDonors.mergeIntervals()); // INTERVAL_MERGE
        put(values, CanonicalAlgorithmDonors.kadane()); // KADANE
        put(values, PrimitiveShapeAlgorithms.kmp()); // KMP
        put(values, CanonicalAlgorithmDonors3.horspool()); // BOYER_MOORE_HORSPOOL
        put(values, CanonicalAlgorithmDonors5MathDp.zeroOneKnapsack()); // KNAPSACK_01
        put(values, CanonicalAlgorithmDonors4Graph.kruskalMst()); // KRUSKAL_MST
        put(values, CanonicalAlgorithmDonors5MathDp.longestCommonSubsequence()); // LONGEST_COMMON_SUBSEQUENCE
        put(values, CanonicalAlgorithmDonors.lisLength()); // LONGEST_INCREASING_SUBSEQUENCE
        put(values, CanonicalAlgorithmDonors3.lowestCommonAncestor()); // LOWEST_COMMON_ANCESTOR
        put(values, StatefulShapeAlgorithms.lruTrace()); // LRU
        put(values, CanonicalAlgorithmDonors3.manacher()); // MANACHER
        put(values, PrimitiveShapeAlgorithms.affineTransform()); // MAP
        put(values, CanonicalAlgorithmDonors2.maxFlow()); // MAX_FLOW
        put(values, CanonicalAlgorithmDonors2.memoizedFibonacci()); // MEMOIZATION
        put(values, CanonicalAlgorithmDonors3.mergeSort()); // MERGE_SORT
        put(values, CanonicalAlgorithmDonors2.mergeWalkIntersection()); // MERGE_WALK
        put(values, CanonicalAlgorithmDonors5MathDp.millerRabin()); // MILLER_RABIN
        put(values, CanonicalAlgorithmDonors4Graph.minCostMaxFlow()); // MIN_COST_MAX_FLOW
        put(values, CanonicalAlgorithmDonors5MathDp.modularExponentiation()); // MODULAR_EXPONENTIATION
        put(values, CanonicalAlgorithmDonors.slidingMaximum()); // MONOTONIC_QUEUE
        put(values, CanonicalAlgorithmDonors.nextGreaterIndices()); // MONOTONIC_STACK
        put(values, StatefulShapeAlgorithms.multiwayMerge()); // MULTIWAY_MERGE
        put(values, PrimitiveShapeAlgorithms.orderedMerge()); // ORDERED_MERGE
        put(values, CanonicalAlgorithmDonors5MathDp.orientation()); // ORIENTATION
        put(values, PrimitiveShapeAlgorithms.partitionIndices()); // PARTITION
        put(values, CanonicalAlgorithmDonors2.partitionWalk()); // PARTITION_WALK
        put(values, CanonicalAlgorithmDonors.prefixScan()); // PREFIX_SCAN
        put(values, CanonicalAlgorithmDonors4Graph.primMst()); // PRIM_MST
        put(values, StatefulShapeAlgorithms.prioritySchedule()); // PRIORITY_QUEUE
        put(values, CanonicalAlgorithmDonors3.quickSort()); // QUICK_SORT
        put(values, CanonicalAlgorithmDonors.quickSelect()); // QUICKSELECT
        put(values, CanonicalAlgorithmDonors3.rabinKarp()); // RABIN_KARP
        put(values, CanonicalAlgorithmDonors3.radixSort()); // RADIX_SORT
        put(values, CanonicalAlgorithmDonors2.rankedMatching()); // RANKED_MATCHING
        put(values, PrimitiveShapeAlgorithms.reduceSum()); // REDUCE
        put(values, PrimitiveShapeAlgorithms.rendezvousHash()); // RENDEZVOUS_HASH
        put(values, CanonicalAlgorithmDonors3.segmentTreeRangeMin()); // SEGMENT_TREE
        put(values, CanonicalAlgorithmDonors5MathDp.sieve()); // SIEVE
        put(values, PrimitiveShapeAlgorithms.maxWindowSum()); // SLIDING_WINDOW
        put(values, PrimitiveShapeAlgorithms.deduplicate()); // SORT_UNIQUE
        put(values, PrimitiveShapeAlgorithms.sortedInsert()); // SORTED_INSERT
        put(values, CanonicalAlgorithmDonors3.sparseTableRangeMin()); // SPARSE_TABLE
        put(values, CanonicalAlgorithmDonors.stableMatching()); // STABLE_MATCHING
        put(values, PrimitiveShapeAlgorithms.stateTable()); // STATE_TABLE
        put(values, CanonicalAlgorithmDonors4Graph.stronglyConnectedComponents()); // STRONGLY_CONNECTED_COMPONENTS
        put(values, CanonicalAlgorithmDonors2.subsequence()); // SUBSEQUENCE
        put(values, CanonicalAlgorithmDonors.maxOverlap()); // SWEEP_LINE
        put(values, StatefulShapeAlgorithms.tokenBucket()); // TOKEN_BUCKET
        put(values, PrimitiveShapeAlgorithms.topologicalSort()); // TOPOLOGICAL_SORT
        put(values, CanonicalAlgorithmDonors3.treePreorder()); // TREE_TRAVERSAL
        put(values, CanonicalAlgorithmDonors3.trieMembership()); // TRIE
        put(values, CanonicalAlgorithmDonors.twoPointerPair()); // TWO_POINTER
        put(values, CanonicalAlgorithmDonors2.unionFind()); // UNION_FIND
        put(values, CanonicalAlgorithmDonors2.ternarySearch()); // TERNARY_SEARCH
        put(values, CanonicalAlgorithmDonors5MathDp.xorBasis()); // XOR_BASIS
        put(values, CanonicalAlgorithmDonors3.zAlgorithm()); // Z_ALGORITHM
        put(values, CanonicalAlgorithmDonors4Graph.zeroOneBfs()); // ZERO_ONE_BFS
        put(values, CanonicalAlgorithmDonors6General.linearScan()); // LINEAR_SCAN
        put(values, CanonicalAlgorithmDonors6General.frequencyCount()); // FREQUENCY_COUNT
        put(values, CanonicalAlgorithmDonors7Consolidated.frequencyL1Distance()); // FREQUENCY_L1_DISTANCE
        put(values, CanonicalAlgorithmDonors6General.stackMachine()); // STACK_MACHINE
        put(values, CanonicalAlgorithmDonors6General.queueMachine()); // QUEUE_MACHINE
        put(values, CanonicalAlgorithmDonors6General.linkedListReverse()); // LINKED_LIST_REWRITE
        put(values, CanonicalAlgorithmDonors6General.matrixScan()); // MATRIX_SCAN
        put(values, CanonicalAlgorithmDonors6General.stringTransform()); // STRING_TRANSFORM
        put(values, CanonicalAlgorithmDonors6General.bitManipulation()); // BIT_MANIPULATION
        put(values, CanonicalAlgorithmDonors6General.arithmetic()); // ARITHMETIC
        put(values, CanonicalAlgorithmDonors6General.concurrencyCoordination()); // CONCURRENCY_COORDINATION
        put(values, CanonicalAlgorithmDonors6General.simulation()); // SIMULATION
        put(values, CanonicalAlgorithmDonors6General.bubbleSort()); // BUBBLE_SORT
        put(values, CanonicalAlgorithmDonors6General.selectionSort()); // SELECTION_SORT
        put(values, CanonicalAlgorithmDonors6General.shellSort()); // SHELL_SORT
        put(values, CanonicalAlgorithmDonors6General.bucketSort()); // BUCKET_SORT
        if (values.size() != AlgorithmShape.values().length) {
            throw new ExceptionInInitializerError(
                    "canonical donor registry incomplete: "
                            + values.size() + "/" + AlgorithmShape.values().length);
        }
        return Map.copyOf(values);
    }

    private static <I, O> void put(
            EnumMap<AlgorithmShape, Entry> values,
            CanonicalShape<I, O> shape) {
        ProgressAlgorithm<I, O> algorithm = shape::execute;
        put(values, shape.descriptor(), algorithm);
    }

    private static <I, O> void put(
            EnumMap<AlgorithmShape, Entry> values,
            CanonicalAlgorithmDonors.Donor<I, O> donor) {
        put(values, donor.descriptor(), donor);
    }

    private static <I, O> void put(
            EnumMap<AlgorithmShape, Entry> values,
            CanonicalAlgorithmDonors2.Donor<I, O> donor) {
        put(values, donor.descriptor(), donor);
    }

    @SuppressWarnings("unchecked")
    private static <I, O> void put(
            EnumMap<AlgorithmShape, Entry> values,
            AlgorithmDescriptor descriptor,
            ProgressAlgorithm<I, O> algorithm) {
        Entry entry = new Entry(
                descriptor,
                (ProgressAlgorithm<Object, Object>) (ProgressAlgorithm<?, ?>) algorithm);
        if (values.putIfAbsent(descriptor.shape(), entry) != null) {
            throw new ExceptionInInitializerError(
                    "duplicate canonical donor for " + descriptor.shape());
        }
    }
}
