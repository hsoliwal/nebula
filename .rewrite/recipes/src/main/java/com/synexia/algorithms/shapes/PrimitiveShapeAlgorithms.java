// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import com.synexia.algorithms.core.IAlgorithm;
import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

/**
 * CPU reference kernels for canonical computational shapes.
 *
 * <p>All hot loops are primitive-array based and poll the supplied progress monitor.
 * These implementations are the semantic oracle for native and TornadoVM backends.
 */
public final class PrimitiveShapeAlgorithms {

    private PrimitiveShapeAlgorithms() {}

    public static CanonicalShape<PrimitiveInputs.LongSearch, Integer> binarySearch() {
        return shape(descriptor(
                "search.binary.long", AlgorithmPurpose.FIND_ONE, AlgorithmShape.BINARY_SEARCH,
                "O(log n)", "O(1)", false, false),
                (input, monitor) -> {
                    long[] values = input.values();
                    int low = 0;
                    int high = values.length - 1;
                    monitor.beginTask("binary-search", Math.max(1, 64 - Integer.numberOfLeadingZeros(values.length + 1)));
                    try {
                        while (low <= high) {
                            monitor.checkCanceled();
                            int mid = (low + high) >>> 1;
                            long value = values[mid];
                            monitor.worked(1);
                            if (value < input.key()) low = mid + 1;
                            else if (value > input.key()) high = mid - 1;
                            else return mid;
                        }
                        return -(low + 1);
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.LongSearch, Integer> interpolationSearch() {
        return shape(descriptor(
                "search.interpolation.long", AlgorithmPurpose.FIND_ONE,
                AlgorithmShape.INTERPOLATION_SEARCH, "O(log log n) average; O(n) worst", "O(1)",
                false, false),
                (input, monitor) -> {
                    long[] values = input.values();
                    int low = 0;
                    int high = values.length - 1;
                    monitor.beginTask("interpolation-search", Math.max(1, values.length));
                    try {
                        while (low <= high) {
                            monitor.checkCanceled();
                            long lowValue = values[low];
                            long highValue = values[high];
                            long key = input.key();
                            if (key < lowValue || key > highValue || lowValue == highValue) break;

                            double fraction = ((double) key - (double) lowValue)
                                    / ((double) highValue - (double) lowValue);
                            int span = high - low;
                            int offset = (int) Math.max(0, Math.min(span, fraction * span));
                            int position = low + offset;
                            long value = values[position];
                            monitor.worked(1);
                            if (value < key) low = position + 1;
                            else if (value > key) high = position - 1;
                            else break;
                        }
                        return exactOrderedResult(values, input.key());
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.LongTopK, long[]> topK() {
        return shape(descriptor(
                "select.topk.long", AlgorithmPurpose.TOP_K, AlgorithmShape.HEAP_SELECT,
                "O(n log k)", "O(k)", false, false),
                (input, monitor) -> {
                    long[] values = input.values();
                    int k = input.k();
                    if (k == 0) return new long[0];
                    long[] heap = new long[k];
                    int size = 0;
                    monitor.beginTask("top-k", values.length);
                    try {
                        for (long value : values) {
                            monitor.checkCanceled();
                            if (size < k) {
                                heap[size] = value;
                                siftUpMin(heap, size++);
                            } else if (value > heap[0]) {
                                heap[0] = value;
                                siftDownMin(heap, size, 0);
                            }
                            monitor.worked(1);
                        }
                        Arrays.sort(heap);
                        reverse(heap);
                        return heap;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.LongSortedInsert, long[]> sortedInsert() {
        return shape(descriptor(
                "order.insert.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.SORTED_INSERT,
                "O(log n + n)", "O(n)", false, false),
                (input, monitor) -> {
                    long[] values = input.values();
                    monitor.beginTask("sorted-insert", values.length + 1L);
                    try {
                        int low = 0, high = values.length;
                        while (low < high) {
                            monitor.checkCanceled();
                            int mid = (low + high) >>> 1;
                            if (values[mid] <= input.value()) low = mid + 1;
                            else high = mid;
                        }
                        long[] result = new long[values.length + 1];
                        System.arraycopy(values, 0, result, 0, low);
                        result[low] = input.value();
                        System.arraycopy(values, low, result, low + 1, values.length - low);
                        monitor.worked(values.length + 1L);
                        return result;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.LongMembership, Boolean> membership() {
        return shape(descriptor(
                "membership.hash.long", AlgorithmPurpose.MEMBERSHIP, AlgorithmShape.HASH_MEMBERSHIP,
                "O(n) build / O(1) expected lookup", "O(n)", false, false),
                (input, monitor) -> {
                    long[] values = input.values();
                    int capacity = tableCapacity(values.length);
                    long[] table = new long[capacity];
                    byte[] used = new byte[capacity];
                    int mask = capacity - 1;
                    monitor.beginTask("hash-membership", values.length + 1L);
                    try {
                        for (long value : values) {
                            monitor.checkCanceled();
                            int slot = mix64(value) & mask;
                            while (used[slot] != 0 && table[slot] != value) slot = (slot + 1) & mask;
                            used[slot] = 1;
                            table[slot] = value;
                            monitor.worked(1);
                        }
                        int slot = mix64(input.value()) & mask;
                        while (used[slot] != 0) {
                            monitor.checkCanceled();
                            if (table[slot] == input.value()) return true;
                            slot = (slot + 1) & mask;
                        }
                        monitor.worked(1);
                        return false;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<long[], long[]> deduplicate() {
        return shape(descriptor(
                "deduplicate.sort-unique.long", AlgorithmPurpose.DEDUPLICATE, AlgorithmShape.SORT_UNIQUE,
                "O(n log n)", "O(n)", false, false),
                (input, monitor) -> {
                    long[] values = Objects.requireNonNull(input, "input").clone();
                    monitor.beginTask("deduplicate", values.length + 1L);
                    try {
                        monitor.checkCanceled();
                        Arrays.sort(values);
                        if (values.length == 0) return values;
                        int write = 1;
                        for (int read = 1; read < values.length; read++) {
                            monitor.checkCanceled();
                            if (values[read] != values[write - 1]) values[write++] = values[read];
                            monitor.worked(1);
                        }
                        return Arrays.copyOf(values, write);
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.IntCsrGraph, int[]> breadthFirstSearch() {
        return shape(descriptor(
                "graph.bfs.csr-int", AlgorithmPurpose.TRAVERSE, AlgorithmShape.BFS,
                "O(V+E)", "O(V)", false, false),
                (input, monitor) -> {
                    int[] offsets = input.offsets();
                    int[] edges = input.edges();
                    int vertices = offsets.length - 1;
                    int[] order = new int[vertices];
                    int[] queue = new int[vertices];
                    byte[] seen = new byte[vertices];
                    int head = 0, tail = 0, count = 0;
                    queue[tail++] = input.source();
                    seen[input.source()] = 1;
                    monitor.beginTask("bfs", (long) vertices + edges.length);
                    try {
                        while (head < tail) {
                            monitor.checkCanceled();
                            int vertex = queue[head++];
                            order[count++] = vertex;
                            monitor.worked(1);
                            for (int i = offsets[vertex]; i < offsets[vertex + 1]; i++) {
                                int next = edges[i];
                                if (seen[next] == 0) {
                                    seen[next] = 1;
                                    queue[tail++] = next;
                                }
                                monitor.worked(1);
                            }
                        }
                        return Arrays.copyOf(order, count);
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.WeightedIntCsrGraph, long[]> dijkstra() {
        return shape(descriptor(
                "graph.dijkstra.csr-int-long", AlgorithmPurpose.SHORTEST_PATH, AlgorithmShape.DIJKSTRA,
                "O((V+E) log V)", "O(V)", false, false),
                (input, monitor) -> {
                    int[] offsets = input.offsets();
                    int[] edges = input.edges();
                    long[] weights = input.weights();
                    int vertices = offsets.length - 1;
                    long[] distances = new long[vertices];
                    Arrays.fill(distances, Long.MAX_VALUE);
                    distances[input.source()] = 0L;
                    LongIntMinHeap heap = new LongIntMinHeap(Math.max(4, vertices));
                    heap.add(0L, input.source());
                    monitor.beginTask("dijkstra", (long) vertices + edges.length);
                    try {
                        while (!heap.isEmpty()) {
                            monitor.checkCanceled();
                            long distance = heap.minKey();
                            int vertex = heap.minValue();
                            heap.removeMin();
                            if (distance != distances[vertex]) continue;
                            monitor.worked(1);
                            for (int i = offsets[vertex]; i < offsets[vertex + 1]; i++) {
                                int next = edges[i];
                                long candidate = saturatedAdd(distance, weights[i]);
                                if (candidate < distances[next]) {
                                    distances[next] = candidate;
                                    heap.add(candidate, next);
                                }
                                monitor.worked(1);
                            }
                        }
                        return distances;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.IntDag, int[]> topologicalSort() {
        return shape(descriptor(
                "graph.topological.csr-int", AlgorithmPurpose.DEPENDENCY_ORDER, AlgorithmShape.TOPOLOGICAL_SORT,
                "O(V+E)", "O(V)", false, false),
                (input, monitor) -> {
                    int[] offsets = input.offsets();
                    int[] edges = input.edges();
                    int vertices = offsets.length - 1;
                    int[] indegree = new int[vertices];
                    for (int edge : edges) indegree[edge]++;
                    int[] queue = new int[vertices];
                    int head = 0, tail = 0;
                    for (int v = 0; v < vertices; v++) if (indegree[v] == 0) queue[tail++] = v;
                    int[] order = new int[vertices];
                    int count = 0;
                    monitor.beginTask("topological-sort", (long) vertices + edges.length);
                    try {
                        while (head < tail) {
                            monitor.checkCanceled();
                            int vertex = queue[head++];
                            order[count++] = vertex;
                            monitor.worked(1);
                            for (int i = offsets[vertex]; i < offsets[vertex + 1]; i++) {
                                int next = edges[i];
                                if (--indegree[next] == 0) queue[tail++] = next;
                                monitor.worked(1);
                            }
                        }
                        if (count != vertices) throw new IllegalArgumentException("graph contains a cycle");
                        return order;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.LongWindow, Long> maxWindowSum() {
        return shape(descriptor(
                "window.max-sum.long", AlgorithmPurpose.RUNNING_RANGE, AlgorithmShape.SLIDING_WINDOW,
                "O(n)", "O(1)", false, false),
                (input, monitor) -> {
                    long[] values = input.values();
                    int width = input.width();
                    monitor.beginTask("sliding-window", values.length);
                    try {
                        long sum = 0L;
                        for (int i = 0; i < width; i++) {
                            monitor.checkCanceled();
                            sum += values[i];
                            monitor.worked(1);
                        }
                        long best = sum;
                        for (int i = width; i < values.length; i++) {
                            monitor.checkCanceled();
                            sum += values[i] - values[i - width];
                            if (sum > best) best = sum;
                            monitor.worked(1);
                        }
                        return best;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.StringPattern, Integer> kmp() {
        return shape(descriptor(
                "sequence.kmp.string", AlgorithmPurpose.SEQUENCE_MATCH, AlgorithmShape.KMP,
                "O(n+m)", "O(m)", false, false),
                (input, monitor) -> {
                    String text = input.text();
                    String pattern = input.pattern();
                    if (pattern.isEmpty()) return 0;
                    int[] prefix = new int[pattern.length()];
                    monitor.beginTask("kmp", (long) text.length() + pattern.length());
                    try {
                        for (int i = 1, j = 0; i < pattern.length(); i++) {
                            monitor.checkCanceled();
                            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = prefix[j - 1];
                            if (pattern.charAt(i) == pattern.charAt(j)) j++;
                            prefix[i] = j;
                            monitor.worked(1);
                        }
                        for (int i = 0, j = 0; i < text.length(); i++) {
                            monitor.checkCanceled();
                            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = prefix[j - 1];
                            if (text.charAt(i) == pattern.charAt(j)) j++;
                            if (j == pattern.length()) return i - j + 1;
                            monitor.worked(1);
                        }
                        return -1;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.StringPair, Integer> editDistance() {
        return shape(descriptor(
                "difference.edit-distance.string", AlgorithmPurpose.DIFFERENCE, AlgorithmShape.DYNAMIC_PROGRAMMING,
                "O(n*m)", "O(m)", false, false),
                (input, monitor) -> {
                    String left = input.left();
                    String right = input.right();
                    if (right.length() > left.length()) {
                        String swap = left; left = right; right = swap;
                    }
                    int[] previous = new int[right.length() + 1];
                    int[] current = new int[right.length() + 1];
                    for (int j = 0; j <= right.length(); j++) previous[j] = j;
                    monitor.beginTask("edit-distance", left.length());
                    try {
                        for (int i = 1; i <= left.length(); i++) {
                            monitor.checkCanceled();
                            current[0] = i;
                            for (int j = 1; j <= right.length(); j++) {
                                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                                current[j] = Math.min(
                                        Math.min(current[j - 1] + 1, previous[j] + 1),
                                        previous[j - 1] + cost);
                            }
                            int[] swap = previous; previous = current; current = swap;
                            monitor.worked(1);
                        }
                        return previous[right.length()];
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.LongPair, long[]> orderedMerge() {
        return shape(descriptor(
                "merge.ordered.long", AlgorithmPurpose.MERGE, AlgorithmShape.ORDERED_MERGE,
                "O(n+m)", "O(n+m)", false, false),
                (input, monitor) -> {
                    long[] left = input.left();
                    long[] right = input.right();
                    long[] result = new long[left.length + right.length];
                    int i = 0, j = 0, k = 0;
                    monitor.beginTask("ordered-merge", result.length);
                    try {
                        while (i < left.length && j < right.length) {
                            monitor.checkCanceled();
                            result[k++] = left[i] <= right[j] ? left[i++] : right[j++];
                            monitor.worked(1);
                        }
                        while (i < left.length) { result[k++] = left[i++]; monitor.worked(1); }
                        while (j < right.length) { result[k++] = right[j++]; monitor.worked(1); }
                        return result;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.Rendezvous, String> rendezvousHash() {
        return shape(descriptor(
                "partition.rendezvous.string", AlgorithmPurpose.PARTITIONING, AlgorithmShape.RENDEZVOUS_HASH,
                "O(nodes)", "O(1)", true, false),
                (input, monitor) -> {
                    String bestNode = null;
                    long bestScore = 0L;
                    boolean first = true;
                    monitor.beginTask("rendezvous-hash", input.nodes().length);
                    try {
                        for (String node : input.nodes()) {
                            monitor.checkCanceled();
                            long score = fnv1a64(input.key() + "\u0000" + node);
                            if (first || Long.compareUnsigned(score, bestScore) > 0) {
                                first = false;
                                bestScore = score;
                                bestNode = node;
                            }
                            monitor.worked(1);
                        }
                        return bestNode;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.StateTransition, Integer> stateTable() {
        return shape(descriptor(
                "state.table.int", AlgorithmPurpose.STATE_TRANSITION, AlgorithmShape.STATE_TABLE,
                "O(1)", "O(1)", false, false),
                (input, monitor) -> {
                    monitor.beginTask("state-transition", 1);
                    try {
                        monitor.checkCanceled();
                        int[][] table = input.table();
                        if (input.state() < 0 || input.state() >= table.length
                                || table[input.state()] == null
                                || input.event() < 0 || input.event() >= table[input.state()].length) {
                            throw new IllegalArgumentException("invalid state/event");
                        }
                        monitor.worked(1);
                        return table[input.state()][input.event()];
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<long[], Long> reduceSum() {
        return shape(descriptor(
                "reduce.sum.long", AlgorithmPurpose.ACCUMULATION, AlgorithmShape.REDUCE,
                "O(n)", "O(1)", true, false),
                (input, monitor) -> {
                    long[] values = Objects.requireNonNull(input, "input");
                    long sum = 0L;
                    monitor.beginTask("reduce-sum", values.length);
                    try {
                        for (long value : values) {
                            monitor.checkCanceled();
                            sum += value;
                            monitor.worked(1);
                        }
                        return sum;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.LongTransform, long[]> affineTransform() {
        return shape(descriptor(
                "map.affine.long", AlgorithmPurpose.TRANSFORMATION, AlgorithmShape.MAP,
                "O(n)", "O(n)", true, true),
                (input, monitor) -> {
                    long[] source = input.values();
                    long[] result = new long[source.length];
                    monitor.beginTask("affine-transform", source.length);
                    try {
                        for (int i = 0; i < source.length; i++) {
                            monitor.checkCanceled();
                            result[i] = source[i] * input.multiplier() + input.addend();
                            monitor.worked(1);
                        }
                        return result;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<PrimitiveInputs.LongPartition, int[]> partitionIndices() {
        return shape(descriptor(
                "partition.indices.long", AlgorithmPurpose.FAN_OUT, AlgorithmShape.PARTITION,
                "O(n)", "O(n)", true, true),
                (input, monitor) -> {
                    long[] values = input.values();
                    int[] side = new int[values.length];
                    monitor.beginTask("partition", values.length);
                    try {
                        for (int i = 0; i < values.length; i++) {
                            monitor.checkCanceled();
                            side[i] = values[i] < input.pivot() ? 0 : 1;
                            monitor.worked(1);
                        }
                        return side;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<long[], long[]> sortAscending() {
        return shape(descriptor(
                "sort.ascending.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.QUICK_SORT,
                "O(n log n)", "O(n)", true, false),
                (input, monitor) -> {
                    long[] values = Objects.requireNonNull(input, "input").clone();
                    monitor.beginTask("sort-ascending", values.length);
                    try {
                        monitor.checkCanceled();
                        Arrays.sort(values);
                        monitor.worked(values.length);
                        monitor.checkCanceled();
                        return values;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<long[], long[]> inclusiveScan() {
        return shape(descriptor(
                "scan.inclusive-sum.long", AlgorithmPurpose.ACCUMULATION, AlgorithmShape.PREFIX_SCAN,
                "O(n)", "O(n)", true, false),
                (input, monitor) -> {
                    long[] values = Objects.requireNonNull(input, "input");
                    long[] result = new long[values.length];
                    long sum = 0L;
                    monitor.beginTask("inclusive-scan", values.length);
                    try {
                        for (int i = 0; i < values.length; i++) {
                            monitor.checkCanceled();
                            sum += values[i];
                            result[i] = sum;
                            monitor.worked(1);
                        }
                        return result;
                    } finally {
                        monitor.done();
                    }
                });
    }

    private static AlgorithmDescriptor descriptor(
            String id, AlgorithmPurpose purpose, AlgorithmShape shape,
            String time, String space, boolean parallel, boolean tornado) {
        EnumSet<PatternView> views = switch (purpose) {
            case FIND_ONE, TOP_K, MEMBERSHIP, DEDUPLICATE, SEQUENCE_MATCH, DIFFERENCE, MERGE ->
                    EnumSet.of(PatternView.STRATEGY, PatternView.DAG_NODE);
            case TRAVERSE, REACHABILITY, SHORTEST_PATH, DEPENDENCY_ORDER ->
                    EnumSet.of(PatternView.VISITOR, PatternView.ITERATOR, PatternView.DAG_NODE);
            case RUNNING_RANGE -> EnumSet.of(PatternView.TEMPLATE_METHOD, PatternView.AGGREGATOR, PatternView.GAME_LOOP);
            case MAINTAIN_ORDER -> EnumSet.of(PatternView.RESEQUENCER, PatternView.STRATEGY);
            case PARTITIONING -> EnumSet.of(PatternView.SHARDER, PatternView.ROUTER, PatternView.DAG_NODE);
            case STATE_TRANSITION -> EnumSet.of(PatternView.STATE, PatternView.GAME_LOOP, PatternView.ECS_SYSTEM);
            case ACCUMULATION -> EnumSet.of(PatternView.AGGREGATOR, PatternView.DAG_NODE);
            case TRANSFORMATION -> EnumSet.of(PatternView.STRATEGY, PatternView.DAG_NODE, PatternView.ECS_SYSTEM);
            case FAN_OUT -> EnumSet.of(PatternView.SPLITTER, PatternView.DAG_NODE, PatternView.JOB_SYSTEM);
            default -> EnumSet.of(PatternView.STRATEGY);
        };
        boolean nativeFriendly = purpose == AlgorithmPurpose.FIND_ONE
                || purpose == AlgorithmPurpose.ACCUMULATION
                || purpose == AlgorithmPurpose.TRANSFORMATION
                || purpose == AlgorithmPurpose.FAN_OUT;
        return new AlgorithmDescriptor(
                id, purpose, shape, views, time, space,
                true, parallel, nativeFriendly, tornado);
    }

    private static <I, O> CanonicalShape<I, O> shape(
            AlgorithmDescriptor descriptor, IAlgorithm.MonitoredFunction<I, O> function) {
        return new MonitoredCanonicalShape<>(descriptor, function);
    }

    private record MonitoredCanonicalShape<I, O>(
            AlgorithmDescriptor descriptor,
            IAlgorithm.MonitoredFunction<I, O> function) implements CanonicalShape<I, O> {
        private MonitoredCanonicalShape {
            Objects.requireNonNull(descriptor, "descriptor");
            Objects.requireNonNull(function, "function");
        }
        @Override public O execute(I input) { return function.apply(input, IProgressMonitor.noop()); }
        @Override public O execute(I input, IProgressMonitor monitor) {
            return function.apply(input, ProgressMonitors.nonNull(monitor));
        }
    }

    private static int exactOrderedResult(long[] values, long key) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (values[mid] < key) low = mid + 1;
            else high = mid;
        }
        return low < values.length && values[low] == key ? low : -(low + 1);
    }

    private static void siftUpMin(long[] heap, int index) {
        long value = heap[index];
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (heap[parent] <= value) break;
            heap[index] = heap[parent];
            index = parent;
        }
        heap[index] = value;
    }

    private static void siftDownMin(long[] heap, int size, int index) {
        long value = heap[index];
        int half = size >>> 1;
        while (index < half) {
            int child = (index << 1) + 1;
            int right = child + 1;
            if (right < size && heap[right] < heap[child]) child = right;
            if (heap[child] >= value) break;
            heap[index] = heap[child];
            index = child;
        }
        heap[index] = value;
    }

    private static void reverse(long[] values) {
        for (int i = 0, j = values.length - 1; i < j; i++, j--) {
            long value = values[i]; values[i] = values[j]; values[j] = value;
        }
    }

    private static int tableCapacity(int size) {
        if (size < 0 || size > (1 << 29)) {
            throw new IllegalArgumentException("input too large");
        }
        int needed = Math.max(2, size * 2);
        int capacity = 1;
        while (capacity < needed) capacity <<= 1;
        return capacity;
    }

    private static int mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        value ^= value >>> 33;
        return (int) value;
    }

    private static long saturatedAdd(long left, long right) {
        if (left == Long.MAX_VALUE) return Long.MAX_VALUE;
        long value = left + right;
        return value < left ? Long.MAX_VALUE : value;
    }

    private static long fnv1a64(String text) {
        long hash = 0xcbf29ce484222325L;
        for (byte value : text.getBytes(StandardCharsets.UTF_8)) {
            hash ^= value & 0xffL;
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static final class LongIntMinHeap {
        private long[] keys;
        private int[] values;
        private int size;

        LongIntMinHeap(int initial) {
            keys = new long[initial];
            values = new int[initial];
        }

        boolean isEmpty() { return size == 0; }
        long minKey() { return keys[0]; }
        int minValue() { return values[0]; }

        void add(long key, int value) {
            ensure(size + 1);
            int index = size++;
            while (index > 0) {
                int parent = (index - 1) >>> 1;
                if (keys[parent] <= key) break;
                keys[index] = keys[parent];
                values[index] = values[parent];
                index = parent;
            }
            keys[index] = key;
            values[index] = value;
        }

        void removeMin() {
            int newSize = --size;
            if (newSize == 0) return;
            long key = keys[newSize];
            int value = values[newSize];
            int index = 0;
            int half = newSize >>> 1;
            while (index < half) {
                int child = (index << 1) + 1;
                int right = child + 1;
                if (right < newSize && keys[right] < keys[child]) child = right;
                if (keys[child] >= key) break;
                keys[index] = keys[child];
                values[index] = values[child];
                index = child;
            }
            keys[index] = key;
            values[index] = value;
        }

        private void ensure(int needed) {
            if (needed <= keys.length) return;
            int capacity = Math.max(needed, keys.length << 1);
            keys = Arrays.copyOf(keys, capacity);
            values = Arrays.copyOf(values, capacity);
        }
    }
}
