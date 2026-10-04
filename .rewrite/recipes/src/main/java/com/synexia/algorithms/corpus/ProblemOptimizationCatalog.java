// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mechanical optimization projection from competitive-problem shapes to Synexia execution lanes.
 *
 * <p>The catalogue describes reusable mechanics and candidate target surfaces. It never grants
 * source replacement authority and every native-eligible plan retains an explicit Java fallback.</p>
 */
public final class ProblemOptimizationCatalog {
    public enum Kernel {
        DIRECT_ADDRESS,
        ORDERED_RANK_SEARCH,
        LINEAR_SCAN,
        PREFIX_SCAN,
        SLIDING_WINDOW,
        HASH_TABLE,
        BITMAP_POSTINGS,
        BIT_PACKING,
        SORT_SELECT,
        HEAP_PRIORITY,
        TRIE,
        AHO_CORASICK,
        KMP,
        ROLLING_HASH,
        RANGE_FENWICK,
        RANGE_SEGMENT,
        SPARSE_TABLE,
        GRAPH_TRAVERSAL,
        UNION_FIND,
        DAG_TOPOLOGY,
        INTERVAL_SWEEP,
        TREE_INDEX,
        DP_TABLE,
        MEMO_TABLE,
        BOUNDED_SEARCH,
        MATRIX_GRID,
        QUEUE_STACK,
        LINKED_REWRITE,
        NUMERIC,
        SIMULATION,
        RATE_LIMIT
    }

    public enum Surface {
        INDEX_STRING,
        MINDEX_COLLECTIONS,
        MINDEX_AST,
        MINDEX_DAG,
        MINDEX_SVG_VIEWER,
        CPULLM_M3,
        GENERAL_RUNTIME
    }

    public enum NativeLane {
        JAVA_ONLY,
        JAVA_PRIMARY_JNI_OPTIONAL,
        JNI_CANDIDATE
    }

    public record Plan(
            AlgorithmShape shape,
            CompetitiveProblemCategory category,
            List<Kernel> kernels,
            List<Surface> surfaces,
            NativeLane nativeLane,
            List<OptimizationDonorCatalog.Role> donorRoles,
            String javaFallback,
            String rationale,
            String root) {

        public Plan {
            shape = Objects.requireNonNull(shape, "shape");
            category = Objects.requireNonNull(category, "category");

            final EnumSet<Kernel> stableKernels = EnumSet.noneOf(Kernel.class);
            stableKernels.addAll(Objects.requireNonNull(kernels, "kernels"));
            if (stableKernels.isEmpty()) {
                throw new IllegalArgumentException("OPTIMIZATION_KERNEL_REQUIRED");
            }
            kernels = List.copyOf(stableKernels);

            final EnumSet<Surface> stableSurfaces = EnumSet.noneOf(Surface.class);
            stableSurfaces.addAll(Objects.requireNonNull(surfaces, "surfaces"));
            if (stableSurfaces.isEmpty()) {
                throw new IllegalArgumentException("OPTIMIZATION_SURFACE_REQUIRED");
            }
            surfaces = List.copyOf(stableSurfaces);

            nativeLane = Objects.requireNonNull(nativeLane, "nativeLane");

            final EnumSet<OptimizationDonorCatalog.Role> roles =
                    EnumSet.noneOf(OptimizationDonorCatalog.Role.class);
            roles.addAll(Objects.requireNonNull(donorRoles, "donorRoles"));
            donorRoles = List.copyOf(roles);

            javaFallback = text(javaFallback, "javaFallback");
            rationale = text(rationale, "rationale");
            if (nativeLane != NativeLane.JAVA_ONLY && javaFallback.isBlank()) {
                throw new IllegalArgumentException("NATIVE_PLAN_REQUIRES_JAVA_FALLBACK");
            }

            final CatalogueDigest digest = new CatalogueDigest("SYNEXIA_PROBLEM_OPTIMIZATION_V1")
                    .text(shape.name())
                    .text(category.name())
                    .text(nativeLane.name())
                    .text(javaFallback)
                    .text(rationale);
            kernels.forEach(value -> digest.text(value.name()));
            surfaces.forEach(value -> digest.text(value.name()));
            donorRoles.forEach(value -> digest.text(value.name()));
            final String expected = digest.finish();
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("OPTIMIZATION_PLAN_ROOT_MISMATCH");
            }
        }

        public boolean substitutionAuthority() {
            return false;
        }

        public boolean nativeEligible() {
            return nativeLane != NativeLane.JAVA_ONLY;
        }

        public List<OptimizationDonorCatalog.Donor> donors() {
            final ArrayList<OptimizationDonorCatalog.Donor> result = new ArrayList<>();
            for (OptimizationDonorCatalog.Role role : donorRoles) {
                result.addAll(OptimizationDonorCatalog.byRole(role));
            }
            return result.stream()
                    .distinct()
                    .sorted(Comparator.comparing(OptimizationDonorCatalog.Donor::repository))
                    .toList();
        }

        /**
         * Native C/C++ systems mechanics relevant to this plan's kernels.
         *
         * <p>These matches are review evidence only; they do not authorize dependency addition,
         * source copying, substitution or native execution.</p>
         */
        public List<NativeMechanicsDonorCatalog.Match> nativeMechanicsDonors() {
            return NativeMechanicsDonorCatalog.matches(kernels);
        }
    }

    private static final Map<AlgorithmShape, Plan> BY_SHAPE = build();
    private static final String ROOT = root(BY_SHAPE);

    private ProblemOptimizationCatalog() {}

    public static Plan plan(final AlgorithmShape shape) {
        final Plan plan = BY_SHAPE.get(Objects.requireNonNull(shape, "shape"));
        if (plan == null) {
            throw new IllegalStateException("MISSING_OPTIMIZATION_PLAN:" + shape);
        }
        return plan;
    }

    public static List<Plan> all() {
        return BY_SHAPE.values().stream()
                .sorted(Comparator.comparing(value -> value.shape().name()))
                .toList();
    }

    public static List<Plan> byCategory(final CompetitiveProblemCategory category) {
        Objects.requireNonNull(category, "category");
        return all().stream().filter(plan -> plan.category() == category).toList();
    }

    public static List<Plan> nativeCandidates() {
        return all().stream().filter(Plan::nativeEligible).toList();
    }

    public static String root() {
        return ROOT;
    }

    private static Map<AlgorithmShape, Plan> build() {
        final EnumMap<AlgorithmShape, Plan> values = new EnumMap<>(AlgorithmShape.class);
        for (AlgorithmShape shape : AlgorithmShape.values()) {
            values.put(shape, planFor(shape));
        }
        if (values.size() != AlgorithmShape.values().length) {
            throw new ExceptionInInitializerError("INCOMPLETE_ALGORITHM_SHAPE_OPTIMIZATION_MAP");
        }
        return Map.copyOf(values);
    }

    private static Plan planFor(final AlgorithmShape shape) {
        final CompetitiveProblemCategory category = CompetitiveProblemCategory.fromShape(shape);
        final List<Kernel> kernels = kernels(shape);
        final List<Surface> surfaces = surfaces(category, kernels);
        final NativeLane nativeLane = nativeLane(shape, kernels);
        final List<OptimizationDonorCatalog.Role> donorRoles = donorRoles(kernels);
        return new Plan(
                shape,
                category,
                kernels,
                surfaces,
                nativeLane,
                donorRoles,
                "Java 21 primitive/indexed implementation",
                rationale(category, kernels, nativeLane),
                "");
    }

    private static List<Kernel> kernels(final AlgorithmShape shape) {
        return switch (shape) {
            case BINARY_SEARCH, EXPONENTIAL_SEARCH, GALLOPING_SEARCH, INTERPOLATION_SEARCH,
                    JUMP_SEARCH, FIBONACCI_SEARCH, TERNARY_SEARCH ->
                    List.of(Kernel.ORDERED_RANK_SEARCH);
            case LINEAR_SCAN, PARTITION_WALK, MERGE_WALK, SUBSEQUENCE,
                    FREQUENCY_L1_DISTANCE ->
                    List.of(Kernel.LINEAR_SCAN);
            case PREFIX_SCAN, DIFFERENCE_SCAN, KADANE ->
                    List.of(Kernel.PREFIX_SCAN, Kernel.LINEAR_SCAN);
            case TWO_POINTER, FAST_SLOW_POINTER ->
                    List.of(Kernel.LINEAR_SCAN, Kernel.DIRECT_ADDRESS);
            case SLIDING_WINDOW ->
                    List.of(Kernel.SLIDING_WINDOW, Kernel.LINEAR_SCAN);
            case MONOTONIC_QUEUE, MONOTONIC_STACK, STACK_MACHINE, QUEUE_MACHINE ->
                    List.of(Kernel.QUEUE_STACK);
            case HASH_MEMBERSHIP, PAIR_SUM_HASH, FREQUENCY_COUNT, RENDEZVOUS_HASH ->
                    List.of(Kernel.HASH_TABLE, Kernel.DIRECT_ADDRESS);
            case LRU ->
                    List.of(Kernel.HASH_TABLE, Kernel.LINKED_REWRITE);
            case HEAP_SELECT, PRIORITY_QUEUE, HEAP_SORT ->
                    List.of(Kernel.HEAP_PRIORITY);
            case QUICKSELECT, SORTED_INSERT, SORT_UNIQUE, INSERTION_SORT, MERGE_SORT,
                    QUICK_SORT, COUNTING_SORT, RADIX_SORT, DUTCH_FLAG,
                    BOYER_MOORE_MAJORITY, BUBBLE_SORT, SELECTION_SORT, SHELL_SORT,
                    BUCKET_SORT, ORDERED_MERGE, MULTIWAY_MERGE ->
                    List.of(Kernel.SORT_SELECT);
            case KMP, Z_ALGORITHM, MANACHER ->
                    List.of(Kernel.KMP, Kernel.ORDERED_RANK_SEARCH);
            case BOYER_MOORE_HORSPOOL ->
                    List.of(Kernel.LINEAR_SCAN, Kernel.DIRECT_ADDRESS);
            case RABIN_KARP ->
                    List.of(Kernel.ROLLING_HASH, Kernel.HASH_TABLE);
            case TRIE ->
                    List.of(Kernel.TRIE, Kernel.DIRECT_ADDRESS);
            case AHO_CORASICK ->
                    List.of(Kernel.AHO_CORASICK, Kernel.TRIE);
            case FENWICK_TREE ->
                    List.of(Kernel.RANGE_FENWICK, Kernel.PREFIX_SCAN);
            case SEGMENT_TREE ->
                    List.of(Kernel.RANGE_SEGMENT);
            case SPARSE_TABLE ->
                    List.of(Kernel.SPARSE_TABLE, Kernel.ORDERED_RANK_SEARCH);
            case BFS, DFS, BIDIRECTIONAL_SEARCH, BEST_FIRST, DIJKSTRA, A_STAR,
                    BELLMAN_FORD, FLOYD_WARSHALL, ZERO_ONE_BFS, CONNECTED_COMPONENTS,
                    GRID_CONNECTED_COMPONENTS, MAX_FLOW, BIPARTITE_MATCHING,
                    ASSIGNMENT, STABLE_MATCHING, RANKED_MATCHING, PRIM_MST,
                    KRUSKAL_MST, STRONGLY_CONNECTED_COMPONENTS, BRIDGES_ARTICULATION,
                    EULERIAN_PATH, MIN_COST_MAX_FLOW ->
                    List.of(Kernel.GRAPH_TRAVERSAL);
            case UNION_FIND ->
                    List.of(Kernel.UNION_FIND, Kernel.DIRECT_ADDRESS);
            case TOPOLOGICAL_SORT ->
                    List.of(Kernel.DAG_TOPOLOGY, Kernel.GRAPH_TRAVERSAL);
            case INTERVAL_MERGE, INTERVAL_JOIN, SWEEP_LINE, INTERVAL_INDEX, COVERAGE ->
                    List.of(Kernel.INTERVAL_SWEEP, Kernel.ORDERED_RANK_SEARCH);
            case TREE_TRAVERSAL, LOWEST_COMMON_ANCESTOR, BINARY_LIFTING, EULER_TOUR ->
                    List.of(Kernel.TREE_INDEX, Kernel.DIRECT_ADDRESS);
            case DYNAMIC_PROGRAMMING, LONGEST_INCREASING_SUBSEQUENCE,
                    LONGEST_COMMON_SUBSEQUENCE, COIN_CHANGE, KNAPSACK_01, INTERVAL_DP,
                    BITMASK_DP, DIGIT_DP, BITSET_DP ->
                    List.of(Kernel.DP_TABLE);
            case MEMOIZATION ->
                    List.of(Kernel.MEMO_TABLE, Kernel.HASH_TABLE);
            case BACKTRACKING, BRANCH_AND_BOUND, CONSTRAINT_SEARCH ->
                    List.of(Kernel.BOUNDED_SEARCH);
            case BIT_MANIPULATION, XOR_BASIS ->
                    List.of(Kernel.BIT_PACKING, Kernel.BITMAP_POSTINGS);
            case MATRIX_SCAN ->
                    List.of(Kernel.MATRIX_GRID, Kernel.LINEAR_SCAN);
            case ORIENTATION, CONVEX_HULL ->
                    List.of(Kernel.NUMERIC, Kernel.SORT_SELECT);
            case GCD_EUCLID, SIEVE, MODULAR_EXPONENTIATION, EXTENDED_GCD,
                    CHINESE_REMAINDER, MILLER_RABIN, ARITHMETIC ->
                    List.of(Kernel.NUMERIC);
            case LINKED_LIST_REWRITE ->
                    List.of(Kernel.LINKED_REWRITE);
            case STRING_TRANSFORM ->
                    List.of(Kernel.LINEAR_SCAN, Kernel.DIRECT_ADDRESS);
            case CONCURRENCY_COORDINATION, SIMULATION, STATE_TABLE ->
                    List.of(Kernel.SIMULATION);
            case BOUNDED_RETRY, TOKEN_BUCKET ->
                    List.of(Kernel.RATE_LIMIT);
            case REDUCE, MAP, PARTITION ->
                    List.of(Kernel.LINEAR_SCAN);
            case GREEDY ->
                    List.of(Kernel.SORT_SELECT, Kernel.LINEAR_SCAN);
        };
    }

    private static List<Surface> surfaces(
            final CompetitiveProblemCategory category,
            final List<Kernel> kernels) {
        final EnumSet<Surface> surfaces = EnumSet.of(Surface.CPULLM_M3);
        switch (category) {
            case STRINGS, SEARCH, HASHING, TRIES, BIT_MANIPULATION ->
                    surfaces.add(Surface.INDEX_STRING);
            case ARRAYS, SORTING, TWO_POINTERS, SLIDING_WINDOW, LINKED_LIST,
                    STACK_QUEUE, HEAPS, RANGE_QUERIES ->
                    surfaces.add(Surface.MINDEX_COLLECTIONS);
            case TREES, BACKTRACKING, DYNAMIC_PROGRAMMING ->
                    surfaces.add(Surface.MINDEX_AST);
            case GRAPHS -> surfaces.add(Surface.MINDEX_DAG);
            case INTERVALS, MATHEMATICS, SIMULATION ->
                    surfaces.add(Surface.MINDEX_SVG_VIEWER);
            case GREEDY, CONCURRENCY, OTHER -> surfaces.add(Surface.GENERAL_RUNTIME);
        }
        if (kernels.contains(Kernel.TRIE)
                || kernels.contains(Kernel.AHO_CORASICK)
                || kernels.contains(Kernel.KMP)
                || kernels.contains(Kernel.ROLLING_HASH)) {
            surfaces.add(Surface.INDEX_STRING);
        }
        return List.copyOf(surfaces);
    }

    private static NativeLane nativeLane(
            final AlgorithmShape shape, final List<Kernel> kernels) {
        if (shape == AlgorithmShape.KMP
                || shape == AlgorithmShape.Z_ALGORITHM
                || shape == AlgorithmShape.BINARY_SEARCH
                || shape == AlgorithmShape.EXPONENTIAL_SEARCH
                || shape == AlgorithmShape.GALLOPING_SEARCH
                || shape == AlgorithmShape.INTERPOLATION_SEARCH
                || shape == AlgorithmShape.JUMP_SEARCH
                || shape == AlgorithmShape.FIBONACCI_SEARCH
                || shape == AlgorithmShape.TERNARY_SEARCH) {
            return NativeLane.JAVA_PRIMARY_JNI_OPTIONAL;
        }
        if (kernels.stream().anyMatch(kernel ->
                kernel == Kernel.DIRECT_ADDRESS
                        || kernel == Kernel.BITMAP_POSTINGS
                        || kernel == Kernel.BIT_PACKING
                        || kernel == Kernel.TRIE
                        || kernel == Kernel.AHO_CORASICK
                        || kernel == Kernel.ROLLING_HASH)) {
            return NativeLane.JNI_CANDIDATE;
        }
        if (kernels.stream().anyMatch(kernel ->
                kernel == Kernel.LINEAR_SCAN
                        || kernel == Kernel.PREFIX_SCAN
                        || kernel == Kernel.SLIDING_WINDOW
                        || kernel == Kernel.RANGE_FENWICK
                        || kernel == Kernel.RANGE_SEGMENT
                        || kernel == Kernel.MATRIX_GRID)) {
            return NativeLane.JAVA_PRIMARY_JNI_OPTIONAL;
        }
        return NativeLane.JAVA_ONLY;
    }

    private static List<OptimizationDonorCatalog.Role> donorRoles(
            final List<Kernel> kernels) {
        final EnumSet<OptimizationDonorCatalog.Role> roles =
                EnumSet.of(OptimizationDonorCatalog.Role.ALGORITHM_REFERENCE);
        if (kernels.stream().anyMatch(kernel ->
                kernel == Kernel.HASH_TABLE
                        || kernel == Kernel.DIRECT_ADDRESS
                        || kernel == Kernel.HEAP_PRIORITY
                        || kernel == Kernel.QUEUE_STACK)) {
            roles.add(OptimizationDonorCatalog.Role.PRIMITIVE_COLLECTIONS);
        }
        if (kernels.stream().anyMatch(kernel ->
                kernel == Kernel.BITMAP_POSTINGS || kernel == Kernel.BIT_PACKING)) {
            roles.add(OptimizationDonorCatalog.Role.BITMAP_POSTINGS);
        }
        if (kernels.stream().anyMatch(kernel ->
                kernel == Kernel.KMP
                        || kernel == Kernel.ROLLING_HASH
                        || kernel == Kernel.TRIE
                        || kernel == Kernel.AHO_CORASICK)) {
            roles.add(OptimizationDonorCatalog.Role.BOUNDED_REGEX);
        }
        return List.copyOf(roles);
    }

    private static String rationale(
            final CompetitiveProblemCategory category,
            final List<Kernel> kernels,
            final NativeLane nativeLane) {
        return category.name()
                + " -> "
                + kernels.stream().map(Enum::name).sorted().toList()
                + "; "
                + nativeLane.name()
                + "; candidate indexes reject/prune only; final verifier remains authoritative";
    }

    private static String root(final Map<AlgorithmShape, Plan> plans) {
        final CatalogueDigest digest =
                new CatalogueDigest("SYNEXIA_PROBLEM_OPTIMIZATION_CATALOG_V1");
        plans.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                .forEach(entry -> digest
                        .text(entry.getKey().name())
                        .text(entry.getValue().root()));
        digest.text(OptimizationDonorCatalog.root());
        digest.text(ChallengeCategoryCatalog.root());
        return digest.finish();
    }

    private static String text(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("INVALID_" + field.toUpperCase(java.util.Locale.ROOT));
        }
        return checked;
    }

    private static String sha256(final String value, final String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("INVALID_" + field.toUpperCase(java.util.Locale.ROOT));
        }
        return value;
    }
}
