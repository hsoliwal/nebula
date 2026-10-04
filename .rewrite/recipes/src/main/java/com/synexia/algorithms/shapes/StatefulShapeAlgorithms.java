// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import com.synexia.algorithms.core.IAlgorithm;
import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

/** Executable reference shapes whose mechanics are stateful or involve multiple runs. */
public final class StatefulShapeAlgorithms {
    private StatefulShapeAlgorithms() {}

    public static CanonicalShape<StatefulInputs.Reachability, Boolean> reachability() {
        return shape(descriptor(
                "graph.reachability.bfs", AlgorithmPurpose.REACHABILITY, AlgorithmShape.BFS,
                "O(V+E)", "O(V)", false, false),
                (input, monitor) -> {
                    PrimitiveInputs.IntCsrGraph graph = input.graph();
                    int[] offsets = graph.offsets();
                    int[] edges = graph.edges();
                    int vertices = offsets.length - 1;
                    byte[] seen = new byte[vertices];
                    int[] queue = new int[vertices];
                    int head = 0, tail = 0;
                    queue[tail++] = graph.source();
                    seen[graph.source()] = 1;
                    monitor.beginTask("reachability", (long) vertices + edges.length);
                    try {
                        while (head < tail) {
                            monitor.checkCanceled();
                            int vertex = queue[head++];
                            if (vertex == input.target()) return true;
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
                        return false;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<StatefulInputs.CacheTrace, StatefulInputs.CacheTraceResult> lruTrace() {
        return shape(descriptor(
                "cache.lru", AlgorithmPurpose.CACHE, AlgorithmShape.LRU,
                "O(1) expected/op", "O(capacity)", false, false),
                (input, monitor) -> {
                    PackedLongLru cache = new PackedLongLru(input.capacity());
                    byte[] operations = input.operations();
                    long[] keys = input.keys();
                    long[] values = input.values();
                    long[] outputs = new long[operations.length];
                    byte[] hits = new byte[operations.length];
                    monitor.beginTask("lru-cache", operations.length);
                    try {
                        for (int i = 0; i < operations.length; i++) {
                            monitor.checkCanceled();
                            if (operations[i] == 1) {
                                cache.put(keys[i], values[i]);
                            } else {
                                int node = cache.find(keys[i]);
                                if (node >= 0) {
                                    outputs[i] = cache.valueAt(node);
                                    hits[i] = 1;
                                    cache.touch(node);
                                }
                            }
                            monitor.worked(1);
                        }
                        return new StatefulInputs.CacheTraceResult(outputs, hits);
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<StatefulInputs.PriorityJobs, long[]> prioritySchedule() {
        return shape(descriptor(
                "schedule.priority-queue", AlgorithmPurpose.SCHEDULING, AlgorithmShape.PRIORITY_QUEUE,
                "O(n log n)", "O(n)", false, false),
                (input, monitor) -> {
                    long[] ids = input.jobIds();
                    long[] priorities = input.priorities();
                    LongPairMaxHeap heap = new LongPairMaxHeap(ids.length);
                    monitor.beginTask("priority-schedule", ids.length * 2L);
                    try {
                        for (int i = 0; i < ids.length; i++) {
                            monitor.checkCanceled();
                            heap.add(priorities[i], ids[i]);
                            monitor.worked(1);
                        }
                        long[] result = new long[ids.length];
                        for (int i = 0; i < result.length; i++) {
                            monitor.checkCanceled();
                            result[i] = heap.maxValue();
                            heap.removeMax();
                            monitor.worked(1);
                        }
                        return result;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static <T> CanonicalShape<T, T> retryIdentity(int maxRetries) {
        if (maxRetries < 0) throw new IllegalArgumentException("maxRetries");
        IAlgorithm<T, T> identity = IAlgorithm.identity();
        return shape(descriptor(
                "retry.bounded", AlgorithmPurpose.RETRY, AlgorithmShape.BOUNDED_RETRY,
                "O(attempts)", "O(1)", false, false),
                (input, monitor) -> identity.retry(maxRetries).execute(input, monitor));
    }

    public static CanonicalShape<StatefulInputs.TokenBucketTrace, byte[]> tokenBucket() {
        return shape(descriptor(
                "rate.token-bucket", AlgorithmPurpose.RATE_LIMITING, AlgorithmShape.TOKEN_BUCKET,
                "O(n)", "O(1)", false, false),
                (input, monitor) -> {
                    long[] requests = input.requestNanos();
                    byte[] allowed = new byte[requests.length];
                    long tokens = input.capacity();
                    long lastRefill = requests.length == 0 ? 0L : requests[0];
                    monitor.beginTask("token-bucket", requests.length);
                    try {
                        for (int i = 0; i < requests.length; i++) {
                            monitor.checkCanceled();
                            long now = requests[i];
                            long periods = (now - lastRefill) / input.refillPeriodNanos();
                            if (periods > 0L) {
                                long refill;
                                try {
                                    refill = Math.multiplyExact(periods, input.refillTokens());
                                } catch (ArithmeticException overflow) {
                                    refill = Long.MAX_VALUE;
                                }
                                tokens = Math.min(input.capacity(), saturatedAdd(tokens, refill));
                                lastRefill += periods * input.refillPeriodNanos();
                            }
                            if (tokens > 0L) {
                                tokens--;
                                allowed[i] = 1;
                            }
                            monitor.worked(1);
                        }
                        return allowed;
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static CanonicalShape<StatefulInputs.LongRuns, long[]> multiwayMerge() {
        return shape(descriptor(
                "fanin.multiway-merge", AlgorithmPurpose.FAN_IN, AlgorithmShape.MULTIWAY_MERGE,
                "O(n log k)", "O(k+n)", false, false),
                (input, monitor) -> {
                    long[][] runs = input.runs();
                    int total = 0;
                    for (long[] run : runs) total = Math.addExact(total, run.length);
                    long[] result = new long[total];
                    RunMinHeap heap = new RunMinHeap(Math.max(1, runs.length));
                    int[] positions = new int[runs.length];
                    for (int run = 0; run < runs.length; run++) {
                        if (runs[run].length > 0) heap.add(runs[run][0], run);
                    }
                    monitor.beginTask("multiway-merge", total);
                    try {
                        int write = 0;
                        while (!heap.isEmpty()) {
                            monitor.checkCanceled();
                            int run = heap.minRun();
                            result[write++] = heap.minValue();
                            heap.removeMin();
                            int next = ++positions[run];
                            if (next < runs[run].length) heap.add(runs[run][next], run);
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
        return new AlgorithmDescriptor(
                id, purpose, shape,
                EnumSet.of(PatternView.STRATEGY, PatternView.TEMPLATE_METHOD, PatternView.DAG_NODE),
                time, space, true, parallel, false, tornado);
    }

    private static <I, O> CanonicalShape<I, O> shape(
            AlgorithmDescriptor descriptor, IAlgorithm.MonitoredFunction<I, O> function) {
        return new MonitoredShape<>(descriptor, function);
    }

    private record MonitoredShape<I, O>(
            AlgorithmDescriptor descriptor,
            IAlgorithm.MonitoredFunction<I, O> function) implements CanonicalShape<I, O> {
        private MonitoredShape {
            Objects.requireNonNull(descriptor, "descriptor");
            Objects.requireNonNull(function, "function");
        }
        @Override public O execute(I input) { return function.apply(input, IProgressMonitor.noop()); }
        @Override public O execute(I input, IProgressMonitor monitor) {
            return function.apply(input, ProgressMonitors.nonNull(monitor));
        }
    }

    private static long saturatedAdd(long left, long right) {
        long value = left + right;
        if (((left ^ value) & (right ^ value)) < 0) return Long.MAX_VALUE;
        return value;
    }

    /** Primitive LRU: open-address key->node table plus index-linked recency list. */
    private static final class PackedLongLru {
        private static final int TOMBSTONE = -1;
        private final int capacity;
        private final long[] keys;
        private final long[] values;
        private final int[] previous;
        private final int[] next;
        private final byte[] active;
        private final int[] free;
        private int freeTop;
        private int size;
        private int head = -1;
        private int tail = -1;
        private final int[] table;
        private final int mask;

        PackedLongLru(int capacity) {
            if (capacity < 1 || capacity > (1 << 29)) {
                throw new IllegalArgumentException("LRU capacity out of range");
            }
            this.capacity = capacity;
            keys = new long[capacity];
            values = new long[capacity];
            previous = new int[capacity];
            next = new int[capacity];
            active = new byte[capacity];
            free = new int[capacity];
            Arrays.fill(previous, -1);
            Arrays.fill(next, -1);
            for (int i = capacity - 1; i >= 0; i--) free[freeTop++] = i;
            int tableCapacity = 1;
            int target = Math.max(2, capacity * 2);
            while (tableCapacity < target) tableCapacity <<= 1;
            table = new int[tableCapacity];
            mask = table.length - 1;
        }

        int find(long key) {
            int slot = mix(key) & mask;
            while (true) {
                int marker = table[slot];
                if (marker == 0) return -1;
                if (marker > 0) {
                    int node = marker - 1;
                    if (active[node] != 0 && keys[node] == key) return node;
                }
                slot = (slot + 1) & mask;
            }
        }

        long valueAt(int node) { return values[node]; }

        void put(long key, long value) {
            int node = find(key);
            if (node >= 0) {
                values[node] = value;
                touch(node);
                return;
            }
            if (size == capacity) {
                node = tail;
                removeTable(keys[node]);
                unlink(node);
            } else {
                node = free[--freeTop];
                size++;
            }
            active[node] = 1;
            keys[node] = key;
            values[node] = value;
            insertTable(key, node);
            linkHead(node);
        }

        void touch(int node) {
            if (node == head) return;
            unlink(node);
            linkHead(node);
        }

        private void linkHead(int node) {
            previous[node] = -1;
            next[node] = head;
            if (head >= 0) previous[head] = node;
            head = node;
            if (tail < 0) tail = node;
        }

        private void unlink(int node) {
            int p = previous[node];
            int n = next[node];
            if (p >= 0) next[p] = n; else head = n;
            if (n >= 0) previous[n] = p; else tail = p;
            previous[node] = -1;
            next[node] = -1;
        }

        private void insertTable(long key, int node) {
            int slot = mix(key) & mask;
            int tombstone = -1;
            while (table[slot] != 0) {
                if (table[slot] == TOMBSTONE && tombstone < 0) tombstone = slot;
                slot = (slot + 1) & mask;
            }
            table[tombstone >= 0 ? tombstone : slot] = node + 1;
        }

        private void removeTable(long key) {
            int slot = mix(key) & mask;
            while (table[slot] != 0) {
                int marker = table[slot];
                if (marker > 0 && keys[marker - 1] == key) {
                    table[slot] = TOMBSTONE;
                    active[marker - 1] = 0;
                    return;
                }
                slot = (slot + 1) & mask;
            }
        }

        private static int mix(long value) {
            value ^= value >>> 33;
            value *= 0xff51afd7ed558ccdl;
            value ^= value >>> 33;
            value *= 0xc4ceb9fe1a85ec53l;
            value ^= value >>> 33;
            return (int) value;
        }
    }

    private static final class LongPairMaxHeap {
        private final long[] priorities;
        private final long[] values;
        private int size;

        LongPairMaxHeap(int capacity) {
            priorities = new long[capacity];
            values = new long[capacity];
        }

        void add(long priority, long value) {
            int index = size++;
            while (index > 0) {
                int parent = (index - 1) >>> 1;
                if (compare(priorities[parent], values[parent], priority, value) >= 0) break;
                priorities[index] = priorities[parent];
                values[index] = values[parent];
                index = parent;
            }
            priorities[index] = priority;
            values[index] = value;
        }

        long maxValue() { return values[0]; }

        void removeMax() {
            int n = --size;
            if (n == 0) return;
            long priority = priorities[n], value = values[n];
            int index = 0;
            while (true) {
                int left = (index << 1) + 1;
                if (left >= n) break;
                int right = left + 1;
                int child = right < n
                        && compare(priorities[right], values[right], priorities[left], values[left]) > 0
                        ? right : left;
                if (compare(priorities[child], values[child], priority, value) <= 0) break;
                priorities[index] = priorities[child];
                values[index] = values[child];
                index = child;
            }
            priorities[index] = priority;
            values[index] = value;
        }

        private static int compare(long p1, long v1, long p2, long v2) {
            int byPriority = Long.compare(p1, p2);
            return byPriority != 0 ? byPriority : -Long.compare(v1, v2);
        }
    }

    private static final class RunMinHeap {
        private final long[] values;
        private final int[] runs;
        private int size;

        RunMinHeap(int capacity) {
            values = new long[capacity];
            runs = new int[capacity];
        }

        boolean isEmpty() { return size == 0; }
        long minValue() { return values[0]; }
        int minRun() { return runs[0]; }

        void add(long value, int run) {
            int index = size++;
            while (index > 0) {
                int parent = (index - 1) >>> 1;
                if (compare(values[parent], runs[parent], value, run) <= 0) break;
                values[index] = values[parent];
                runs[index] = runs[parent];
                index = parent;
            }
            values[index] = value;
            runs[index] = run;
        }

        void removeMin() {
            int n = --size;
            if (n == 0) return;
            long value = values[n];
            int run = runs[n];
            int index = 0;
            while (true) {
                int left = (index << 1) + 1;
                if (left >= n) break;
                int right = left + 1;
                int child = right < n && compare(values[right], runs[right], values[left], runs[left]) < 0
                        ? right : left;
                if (compare(values[child], runs[child], value, run) >= 0) break;
                values[index] = values[child];
                runs[index] = runs[child];
                index = child;
            }
            values[index] = value;
            runs[index] = run;
        }

        private static int compare(long v1, int r1, long v2, int r2) {
            int value = Long.compare(v1, v2);
            return value != 0 ? value : Integer.compare(r1, r2);
        }
    }
}
