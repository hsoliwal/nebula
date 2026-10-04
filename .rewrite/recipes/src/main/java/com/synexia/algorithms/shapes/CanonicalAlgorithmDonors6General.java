// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import com.synexia.algorithms.core.ProgressAlgorithm;
import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Generic monitor-first donors used to classify the large practical middle of coding-problem
 * corpora: scans, counters, elementary data-structure machines, rewrites and transforms.
 */
public final class CanonicalAlgorithmDonors6General {
    private CanonicalAlgorithmDonors6General() {}

    public record ScanSummary(long min, long max, long sum, int minIndex, int maxIndex) {}

    public record FrequencyTable(long[] values, long[] counts) {
        public FrequencyTable {
            values = Objects.requireNonNull(values, "values").clone();
            counts = Objects.requireNonNull(counts, "counts").clone();
            if (values.length != counts.length) throw new IllegalArgumentException("length");
        }
        @Override public long[] values() { return values.clone(); }
        @Override public long[] counts() { return counts.clone(); }
    }

    public record MachineTrace(byte[] operations, long[] values) {
        public MachineTrace {
            operations = Objects.requireNonNull(operations, "operations").clone();
            values = Objects.requireNonNull(values, "values").clone();
            if (operations.length != values.length) throw new IllegalArgumentException("trace lengths");
            for (byte op : operations) if (op < 0 || op > 2) {
                throw new IllegalArgumentException("operation must be 0=push/add, 1=pop/remove, 2=peek");
            }
        }
        @Override public byte[] operations() { return operations.clone(); }
        @Override public long[] values() { return values.clone(); }
    }

    public record MachineResult(long[] outputs, byte[] produced) {
        public MachineResult {
            outputs = Objects.requireNonNull(outputs, "outputs").clone();
            produced = Objects.requireNonNull(produced, "produced").clone();
            if (outputs.length != produced.length) throw new IllegalArgumentException("result lengths");
        }
        @Override public long[] outputs() { return outputs.clone(); }
        @Override public byte[] produced() { return produced.clone(); }
    }

    public record NextList(int[] next, int head) {
        public NextList {
            next = Objects.requireNonNull(next, "next").clone();
            if (head < -1 || head >= next.length) throw new IllegalArgumentException("head");
            for (int n : next) if (n < -1 || n >= next.length) throw new IllegalArgumentException("next");
        }
        @Override public int[] next() { return next.clone(); }
    }

    public record NextListResult(int[] next, int head) {
        public NextListResult {
            next = Objects.requireNonNull(next, "next").clone();
        }
        @Override public int[] next() { return next.clone(); }
    }

    public record LongMatrix(long[][] values) {
        public LongMatrix {
            Objects.requireNonNull(values, "values");
            values = Arrays.stream(values)
                    .map(row -> Objects.requireNonNull(row, "row").clone())
                    .toArray(long[][]::new);
            if (values.length > 0) {
                int width = values[0].length;
                for (long[] row : values) if (row.length != width) {
                    throw new IllegalArgumentException("matrix must be rectangular");
                }
            }
        }
        @Override public long[][] values() {
            return Arrays.stream(values).map(long[]::clone).toArray(long[][]::new);
        }
    }

    public record DecimalPair(String left, String right) {
        public DecimalPair {
            left = decimal(left, "left");
            right = decimal(right, "right");
        }
    }

    public record PhaseTrace(int[] phase, int phases) {
        public PhaseTrace {
            phase = Objects.requireNonNull(phase, "phase").clone();
            if (phases < 1) throw new IllegalArgumentException("phases");
            for (int p : phase) if (p < 0 || p >= phases) throw new IllegalArgumentException("phase");
        }
        @Override public int[] phase() { return phase.clone(); }
    }

    public record SimulationTrace(int initialState, int[] events, int[][] transitions) {
        public SimulationTrace {
            events = Objects.requireNonNull(events, "events").clone();
            Objects.requireNonNull(transitions, "transitions");
            transitions = Arrays.stream(transitions)
                    .map(row -> Objects.requireNonNull(row, "row").clone())
                    .toArray(int[][]::new);
            if (initialState < 0 || initialState >= transitions.length) {
                throw new IllegalArgumentException("initialState");
            }
            for (int[] row : transitions) for (int state : row) {
                if (state < 0 || state >= transitions.length) {
                    throw new IllegalArgumentException("transition target");
                }
            }
            for (int event : events) {
                if (event < 0 || transitions.length == 0 || event >= transitions[0].length) {
                    throw new IllegalArgumentException("event");
                }
            }
        }
        @Override public int[] events() { return events.clone(); }
        @Override public int[][] transitions() {
            return Arrays.stream(transitions).map(int[]::clone).toArray(int[][]::new);
        }
    }

    public static CanonicalAlgorithmDonors.Donor<long[], ScanSummary> linearScan() {
        return donor("scan.summary.long", AlgorithmPurpose.ACCUMULATION, AlgorithmShape.LINEAR_SCAN,
                "O(n)", "O(1)", (input, supplied) -> {
            long[] a = Objects.requireNonNull(input, "input");
            IProgressMonitor m = start(supplied, "linear-scan", Math.max(1, a.length));
            try {
                if (a.length == 0) return new ScanSummary(0, 0, 0, -1, -1);
                long min = a[0], max = a[0], sum = 0;
                int minIndex = 0, maxIndex = 0;
                for (int i = 0; i < a.length; i++) {
                    m.checkCanceled();
                    long value = a[i];
                    sum = Math.addExact(sum, value);
                    if (value < min) { min = value; minIndex = i; }
                    if (value > max) { max = value; maxIndex = i; }
                    m.worked(1);
                }
                return new ScanSummary(min, max, sum, minIndex, maxIndex);
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[], FrequencyTable> frequencyCount() {
        return donor("count.frequency.long", AlgorithmPurpose.ACCUMULATION, AlgorithmShape.FREQUENCY_COUNT,
                "O(n log n)", "O(n) clone", (input, supplied) -> {
            long[] a = Objects.requireNonNull(input, "input").clone();
            IProgressMonitor m = start(supplied, "frequency-count", Math.max(1, a.length));
            try {
                Arrays.sort(a);
                if (a.length == 0) return new FrequencyTable(new long[0], new long[0]);
                long[] values = new long[a.length];
                long[] counts = new long[a.length];
                int size = 0;
                for (long value : a) {
                    m.checkCanceled();
                    if (size == 0 || values[size - 1] != value) {
                        values[size] = value;
                        counts[size] = 1;
                        size++;
                    } else {
                        counts[size - 1]++;
                    }
                    m.worked(1);
                }
                return new FrequencyTable(Arrays.copyOf(values, size), Arrays.copyOf(counts, size));
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<MachineTrace, MachineResult> stackMachine() {
        return donor("machine.stack.long", AlgorithmPurpose.STATE_TRANSITION, AlgorithmShape.STACK_MACHINE,
                "O(n)", "O(n)", (input, supplied) -> {
            byte[] op = input.operations();
            long[] values = input.values();
            long[] stack = new long[op.length];
            int size = 0;
            long[] out = new long[op.length];
            byte[] produced = new byte[op.length];
            IProgressMonitor m = start(supplied, "stack-machine", Math.max(1, op.length));
            try {
                for (int i = 0; i < op.length; i++) {
                    m.checkCanceled();
                    switch (op[i]) {
                        case 0 -> stack[size++] = values[i];
                        case 1 -> {
                            if (size > 0) { out[i] = stack[--size]; produced[i] = 1; }
                        }
                        case 2 -> {
                            if (size > 0) { out[i] = stack[size - 1]; produced[i] = 1; }
                        }
                        default -> throw new IllegalStateException();
                    }
                    m.worked(1);
                }
                return new MachineResult(out, produced);
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<MachineTrace, MachineResult> queueMachine() {
        return donor("machine.queue.long", AlgorithmPurpose.STATE_TRANSITION, AlgorithmShape.QUEUE_MACHINE,
                "O(n)", "O(n)", (input, supplied) -> {
            byte[] op = input.operations();
            long[] values = input.values();
            long[] queue = new long[Math.max(1, op.length)];
            int head = 0, tail = 0, size = 0;
            long[] out = new long[op.length];
            byte[] produced = new byte[op.length];
            IProgressMonitor m = start(supplied, "queue-machine", Math.max(1, op.length));
            try {
                for (int i = 0; i < op.length; i++) {
                    m.checkCanceled();
                    switch (op[i]) {
                        case 0 -> {
                            queue[tail] = values[i];
                            tail = (tail + 1) % queue.length;
                            size++;
                        }
                        case 1 -> {
                            if (size > 0) {
                                out[i] = queue[head];
                                produced[i] = 1;
                                head = (head + 1) % queue.length;
                                size--;
                            }
                        }
                        case 2 -> {
                            if (size > 0) { out[i] = queue[head]; produced[i] = 1; }
                        }
                        default -> throw new IllegalStateException();
                    }
                    m.worked(1);
                }
                return new MachineResult(out, produced);
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<NextList, NextListResult> linkedListReverse() {
        return donor("list.reverse.next-array", AlgorithmPurpose.TRANSFORMATION, AlgorithmShape.LINKED_LIST_REWRITE,
                "O(n)", "O(n) clone", (input, supplied) -> {
            int[] next = input.next();
            byte[] seen = new byte[next.length];
            int previous = -1, current = input.head();
            IProgressMonitor m = start(supplied, "linked-list-rewrite", Math.max(1, next.length));
            try {
                while (current >= 0) {
                    m.checkCanceled();
                    if (seen[current] != 0) throw new IllegalArgumentException("cycle");
                    seen[current] = 1;
                    int following = next[current];
                    next[current] = previous;
                    previous = current;
                    current = following;
                    m.worked(1);
                }
                return new NextListResult(next, previous);
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<LongMatrix, long[]> matrixScan() {
        return donor("matrix.spiral.long", AlgorithmPurpose.TRAVERSE, AlgorithmShape.MATRIX_SCAN,
                "O(rows*cols)", "O(rows*cols)", (input, supplied) -> {
            long[][] a = input.values();
            int rows = a.length, cols = rows == 0 ? 0 : a[0].length;
            long[] out = new long[Math.multiplyExact(rows, cols)];
            IProgressMonitor m = start(supplied, "matrix-scan", Math.max(1, out.length));
            try {
                int top = 0, bottom = rows - 1, left = 0, right = cols - 1, k = 0;
                while (top <= bottom && left <= right) {
                    m.checkCanceled();
                    for (int c = left; c <= right; c++) { out[k++] = a[top][c]; m.worked(1); }
                    top++;
                    for (int r = top; r <= bottom; r++) { out[k++] = a[r][right]; m.worked(1); }
                    right--;
                    if (top <= bottom) {
                        for (int c = right; c >= left; c--) { out[k++] = a[bottom][c]; m.worked(1); }
                        bottom--;
                    }
                    if (left <= right) {
                        for (int r = bottom; r >= top; r--) { out[k++] = a[r][left]; m.worked(1); }
                        left++;
                    }
                }
                return out;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<String, String> stringTransform() {
        return donor("string.reverse-codepoints", AlgorithmPurpose.TRANSFORMATION, AlgorithmShape.STRING_TRANSFORM,
                "O(n)", "O(n)", (input, supplied) -> {
            String text = Objects.requireNonNull(input, "input");
            int[] cps = text.codePoints().toArray();
            IProgressMonitor m = start(supplied, "string-transform", Math.max(1, cps.length));
            try {
                for (int i = 0, j = cps.length - 1; i < j; i++, j--) {
                    m.checkCanceled();
                    int t = cps[i]; cps[i] = cps[j]; cps[j] = t;
                    m.worked(1);
                }
                if ((cps.length & 1) != 0) m.worked(1);
                return new String(cps, 0, cps.length);
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[], Long> bitManipulation() {
        return donor("bit.xor-fold.long", AlgorithmPurpose.ACCUMULATION, AlgorithmShape.BIT_MANIPULATION,
                "O(n)", "O(1)", (input, supplied) -> {
            long[] values = Objects.requireNonNull(input, "input");
            long result = 0;
            IProgressMonitor m = start(supplied, "bit-manipulation", Math.max(1, values.length));
            try {
                for (long value : values) {
                    m.checkCanceled();
                    result ^= value;
                    m.worked(1);
                }
                return result;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<DecimalPair, String> arithmetic() {
        return donor("arithmetic.decimal-add", AlgorithmPurpose.ACCUMULATION, AlgorithmShape.ARITHMETIC,
                "O(n)", "O(n)", (input, supplied) -> {
            String a = input.left(), b = input.right();
            IProgressMonitor m = start(supplied, "arithmetic", Math.max(1, Math.max(a.length(), b.length())));
            try {
                StringBuilder out = new StringBuilder(Math.max(a.length(), b.length()) + 1);
                int i = a.length() - 1, j = b.length() - 1, carry = 0;
                while (i >= 0 || j >= 0 || carry != 0) {
                    m.checkCanceled();
                    int x = i >= 0 ? a.charAt(i--) - '0' : 0;
                    int y = j >= 0 ? b.charAt(j--) - '0' : 0;
                    int sum = x + y + carry;
                    out.append((char) ('0' + (sum % 10)));
                    carry = sum / 10;
                    m.worked(1);
                }
                return out.reverse().toString();
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<PhaseTrace, Boolean> concurrencyCoordination() {
        return donor("concurrency.phase-order.trace", AlgorithmPurpose.DEPENDENCY_ORDER,
                AlgorithmShape.CONCURRENCY_COORDINATION, "O(n)", "O(1)", (input, supplied) -> {
            int[] phase = input.phase();
            IProgressMonitor m = start(supplied, "concurrency-coordination", Math.max(1, phase.length));
            try {
                for (int i = 0; i < phase.length; i++) {
                    m.checkCanceled();
                    if (phase[i] != i % input.phases()) return false;
                    m.worked(1);
                }
                return true;
            } finally { m.done(); }
        });
    }


    public static CanonicalAlgorithmDonors.Donor<SimulationTrace, int[]> simulation() {
        return donor("simulation.state-trace", AlgorithmPurpose.STATE_TRANSITION, AlgorithmShape.SIMULATION,
                "O(events)", "O(events)", (input, supplied) -> {
            int[][] table = input.transitions();
            int[] events = input.events();
            int[] states = new int[events.length + 1];
            states[0] = input.initialState();
            IProgressMonitor m = start(supplied, "simulation", Math.max(1, events.length));
            try {
                for (int i = 0; i < events.length; i++) {
                    m.checkCanceled();
                    int state = states[i];
                    int event = events[i];
                    if (event >= table[state].length) throw new IllegalArgumentException("event");
                    states[i + 1] = table[state][event];
                    m.worked(1);
                }
                return states;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[], long[]> bubbleSort() {
        return donor("sort.bubble.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.BUBBLE_SORT,
                "O(n^2)", "O(n) clone", (input, supplied) -> {
            long[] a = Objects.requireNonNull(input, "input").clone();
            IProgressMonitor m = start(supplied, "bubble-sort",
                    Math.max(1L, (long) a.length * Math.max(1, a.length - 1) / 2));
            try {
                for (int end = a.length - 1; end > 0; end--) {
                    boolean changed = false;
                    for (int i = 0; i < end; i++) {
                        m.checkCanceled();
                        if (a[i] > a[i + 1]) {
                            long t = a[i]; a[i] = a[i + 1]; a[i + 1] = t; changed = true;
                        }
                        m.worked(1);
                    }
                    if (!changed) break;
                }
                return a;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[], long[]> selectionSort() {
        return donor("sort.selection.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.SELECTION_SORT,
                "O(n^2)", "O(n) clone", (input, supplied) -> {
            long[] a = Objects.requireNonNull(input, "input").clone();
            IProgressMonitor m = start(supplied, "selection-sort",
                    Math.max(1L, (long) a.length * Math.max(1, a.length - 1) / 2));
            try {
                for (int i = 0; i < a.length; i++) {
                    int min = i;
                    for (int j = i + 1; j < a.length; j++) {
                        m.checkCanceled();
                        if (a[j] < a[min]) min = j;
                        m.worked(1);
                    }
                    if (min != i) { long t = a[i]; a[i] = a[min]; a[min] = t; }
                }
                return a;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<long[], long[]> shellSort() {
        return donor("sort.shell.long", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.SHELL_SORT,
                "O(n^2) worst-case", "O(n) clone", (input, supplied) -> {
            long[] a = Objects.requireNonNull(input, "input").clone();
            IProgressMonitor m = start(supplied, "shell-sort", IProgressMonitor.UNKNOWN);
            try {
                for (int gap = a.length >>> 1; gap > 0; gap >>>= 1) {
                    for (int i = gap; i < a.length; i++) {
                        m.checkCanceled();
                        long value = a[i];
                        int j = i;
                        while (j >= gap && a[j - gap] > value) {
                            a[j] = a[j - gap];
                            j -= gap;
                            m.worked(1);
                        }
                        a[j] = value;
                        m.worked(1);
                    }
                }
                return a;
            } finally { m.done(); }
        });
    }

    public static CanonicalAlgorithmDonors.Donor<int[], int[]> bucketSort() {
        return donor("sort.bucket.int", AlgorithmPurpose.MAINTAIN_ORDER, AlgorithmShape.BUCKET_SORT,
                "O(n + buckets + local sorts)", "O(n+buckets)", (input, supplied) -> {
            int[] a = Objects.requireNonNull(input, "input").clone();
            IProgressMonitor m = start(supplied, "bucket-sort", Math.max(1, a.length));
            try {
                if (a.length < 2) return a;
                int min = a[0], max = a[0];
                for (int value : a) { min = Math.min(min, value); max = Math.max(max, value); }
                int bucketCount = Math.max(1, (int) Math.sqrt(a.length));
                long range = (long) max - min + 1L;
                int[] sizes = new int[bucketCount];
                for (int value : a) {
                    m.checkCanceled();
                    int bucket = (int) Math.min(bucketCount - 1,
                            ((long) value - min) * bucketCount / range);
                    sizes[bucket]++;
                    m.worked(1);
                }
                int[][] buckets = new int[bucketCount][];
                for (int i = 0; i < bucketCount; i++) buckets[i] = new int[sizes[i]];
                Arrays.fill(sizes, 0);
                for (int value : a) {
                    int bucket = (int) Math.min(bucketCount - 1,
                            ((long) value - min) * bucketCount / range);
                    buckets[bucket][sizes[bucket]++] = value;
                }
                int write = 0;
                for (int[] bucket : buckets) {
                    Arrays.sort(bucket);
                    for (int value : bucket) a[write++] = value;
                }
                return a;
            } finally { m.done(); }
        });
    }

    public static java.util.List<AlgorithmDescriptor> descriptors() {
        return java.util.List.of(
                linearScan().descriptor(), frequencyCount().descriptor(), stackMachine().descriptor(),
                queueMachine().descriptor(), linkedListReverse().descriptor(), matrixScan().descriptor(),
                stringTransform().descriptor(), bitManipulation().descriptor(), arithmetic().descriptor(),
                concurrencyCoordination().descriptor(), simulation().descriptor(), bubbleSort().descriptor(),
                selectionSort().descriptor(), shellSort().descriptor(), bucketSort().descriptor());
    }

    private static <I, O> CanonicalAlgorithmDonors.Donor<I, O> donor(
            String id, AlgorithmPurpose purpose, AlgorithmShape shape, String time, String space,
            ProgressAlgorithm<I, O> algorithm) {
        return new CanonicalAlgorithmDonors.Donor<>(
                new AlgorithmDescriptor(id, purpose, shape,
                        EnumSet.of(PatternView.STRATEGY, PatternView.TEMPLATE_METHOD, PatternView.DAG_NODE),
                        time, space, true, false, true, false),
                algorithm);
    }

    private static IProgressMonitor start(IProgressMonitor supplied, String name, long total) {
        IProgressMonitor m = ProgressMonitors.nonNull(supplied);
        m.beginTask(name, total);
        m.checkCanceled();
        return m;
    }

    private static String decimal(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isEmpty()) throw new IllegalArgumentException(label);
        for (int i = 0; i < value.length(); i++) if (!Character.isDigit(value.charAt(i))) {
            throw new IllegalArgumentException(label + " must be unsigned decimal");
        }
        return value;
    }
}
