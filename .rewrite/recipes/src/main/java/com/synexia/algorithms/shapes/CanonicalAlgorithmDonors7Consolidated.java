// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import com.synexia.algorithms.core.ProgressAlgorithm;
import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Consolidated donors derived from comparing multiple independent Java problem implementations.
 *
 * <p>These donors preserve the union of observed behaviors as explicit policies instead of keeping
 * one source file per repository or silently dropping variant semantics.
 */
public final class CanonicalAlgorithmDonors7Consolidated {
    private CanonicalAlgorithmDonors7Consolidated() {}

    public enum DuplicateIndexPolicy {
        /** Preserve the first index observed for a value. */
        FIRST_SEEN,
        /** Preserve the most recent index observed for a value. */
        LATEST_SEEN
    }

    public enum NoSolutionPolicy {
        RETURN_NEGATIVE_ONES,
        RETURN_ZEROES,
        RETURN_EMPTY,
        THROW
    }

    public record PairSumInput(long[] values, long target, DuplicateIndexPolicy duplicateIndexPolicy) {
        public PairSumInput {
            values = Objects.requireNonNull(values, "values").clone();
            duplicateIndexPolicy = Objects.requireNonNull(duplicateIndexPolicy, "duplicateIndexPolicy");
        }

        public PairSumInput(long[] values, long target) {
            this(values, target, DuplicateIndexPolicy.LATEST_SEEN);
        }

        @Override
        public long[] values() {
            return values.clone();
        }
    }

    public record MultisetDistanceInput(long[] left, long[] right) {
        public MultisetDistanceInput {
            left = Objects.requireNonNull(left, "left").clone();
            right = Objects.requireNonNull(right, "right").clone();
        }

        @Override public long[] left() { return left.clone(); }
        @Override public long[] right() { return right.clone(); }
    }

    public record PairSumResult(boolean found, int firstIndex, int secondIndex) {
        public PairSumResult {
            if (found) {
                if (firstIndex < 0 || secondIndex < 0 || firstIndex == secondIndex) {
                    throw new IllegalArgumentException("invalid found pair");
                }
            } else if (firstIndex != -1 || secondIndex != -1) {
                throw new IllegalArgumentException("missing pair must use -1/-1");
            }
        }

        public static PairSumResult found(int firstIndex, int secondIndex) {
            return new PairSumResult(true, firstIndex, secondIndex);
        }

        public static PairSumResult missing() {
            return new PairSumResult(false, -1, -1);
        }

        /**
         * Compatibility projection for donor implementations whose no-solution contracts differ.
         */
        public int[] indices(NoSolutionPolicy policy) {
            Objects.requireNonNull(policy, "policy");
            if (found) return new int[] {firstIndex, secondIndex};
            return switch (policy) {
                case RETURN_NEGATIVE_ONES -> new int[] {-1, -1};
                case RETURN_ZEROES -> new int[] {0, 0};
                case RETURN_EMPTY -> new int[0];
                case THROW -> throw new IllegalArgumentException("No two-sum solution");
            };
        }
    }

    /**
     * One-pass complement lookup with primitive open addressing.
     *
     * <p>This is the shared invariant found in the compared Java implementations of LeetCode #1.
     * The caller input is never modified. The map stores primitive long keys and int indexes,
     * avoiding per-entry boxing/object allocation.
     */
    public static CanonicalAlgorithmDonors.Donor<PairSumInput, PairSumResult> pairSumHash() {
        return donor(
                "lookup.pair-sum-complement.long",
                AlgorithmPurpose.FIND_ONE,
                AlgorithmShape.PAIR_SUM_HASH,
                "O(n) expected",
                "O(n) primitive arrays",
                (input, supplied) -> {
                    long[] values = input.values();
                    PrimitiveLongIntMap seen = new PrimitiveLongIntMap(values.length);
                    IProgressMonitor monitor = start(
                            supplied, "pair-sum-complement", Math.max(1, values.length));
                    try {
                        for (int i = 0; i < values.length; i++) {
                            monitor.checkCanceled();
                            long value = values[i];

                            long complement;
                            try {
                                complement = Math.subtractExact(input.target(), value);
                            } catch (ArithmeticException impossibleComplement) {
                                put(seen, value, i, input.duplicateIndexPolicy());
                                monitor.worked(1);
                                continue;
                            }

                            int prior = seen.getOrDefault(complement, -1);
                            if (prior >= 0) {
                                monitor.worked(1);
                                return PairSumResult.found(prior, i);
                            }

                            put(seen, value, i, input.duplicateIndexPolicy());
                            monitor.worked(1);
                        }
                        return PairSumResult.missing();
                    } finally {
                        monitor.done();
                    }
                });
    }


    /**
     * L1 distance between two multisets represented as primitive long tokens.
     *
     * <p>This is the shared invariant in the pulled HackerRank Making Anagrams implementations:
     * count each token on both sides and sum the absolute frequency differences.
     */
    public static CanonicalAlgorithmDonors.Donor<MultisetDistanceInput, Long> frequencyL1Distance() {
        return donor(
                "difference.multiset-frequency-l1.long",
                AlgorithmPurpose.DIFFERENCE,
                AlgorithmShape.FREQUENCY_L1_DISTANCE,
                "O(n+m) expected",
                "O(unique tokens) primitive arrays",
                (input, supplied) -> {
                    long[] left = input.left();
                    long[] right = input.right();
                    long requested = (long) left.length + right.length;
                    int expected = requested > Integer.MAX_VALUE
                            ? Integer.MAX_VALUE
                            : (int) requested;
                    PrimitiveLongIntMap counts = new PrimitiveLongIntMap(expected);
                    IProgressMonitor monitor = start(
                            supplied,
                            "frequency-l1-distance",
                            Math.max(1L, requested));
                    try {
                        for (long token : left) {
                            monitor.checkCanceled();
                            counts.addTo(token, 1);
                            monitor.worked(1);
                        }
                        for (long token : right) {
                            monitor.checkCanceled();
                            counts.addTo(token, -1);
                            monitor.worked(1);
                        }
                        return counts.absoluteValueSum();
                    } finally {
                        monitor.done();
                    }
                });
    }

    public static java.util.List<AlgorithmDescriptor> descriptors() {
        return java.util.List.of(
                pairSumHash().descriptor(),
                frequencyL1Distance().descriptor());
    }

    private static void put(
            PrimitiveLongIntMap map,
            long key,
            int index,
            DuplicateIndexPolicy duplicateIndexPolicy) {
        if (duplicateIndexPolicy == DuplicateIndexPolicy.FIRST_SEEN) {
            map.putIfAbsent(key, index);
        } else {
            map.put(key, index);
        }
    }

    private static <I, O> CanonicalAlgorithmDonors.Donor<I, O> donor(
            String id,
            AlgorithmPurpose purpose,
            AlgorithmShape shape,
            String time,
            String space,
            ProgressAlgorithm<I, O> algorithm) {
        return new CanonicalAlgorithmDonors.Donor<>(
                new AlgorithmDescriptor(
                        id,
                        purpose,
                        shape,
                        EnumSet.of(
                                PatternView.STRATEGY,
                                PatternView.TEMPLATE_METHOD,
                                PatternView.DAG_NODE),
                        time,
                        space,
                        true,
                        false,
                        true,
                        false),
                algorithm);
    }

    private static IProgressMonitor start(
            IProgressMonitor supplied,
            String name,
            long total) {
        IProgressMonitor monitor = ProgressMonitors.nonNull(supplied);
        monitor.beginTask(name, total);
        monitor.checkCanceled();
        return monitor;
    }

    /**
     * Minimal primitive long->int open-addressed map specialized for immutable numeric keys.
     */
    private static final class PrimitiveLongIntMap {
        private static final float LOAD_FACTOR = 0.60f;

        private long[] keys;
        private int[] values;
        private byte[] used;
        private int mask;
        private int size;
        private int threshold;

        PrimitiveLongIntMap(int expectedSize) {
            int needed = Math.max(4, expectedSize <= 0 ? 4 : expectedSize);
            int capacity = 1;
            while (capacity < needed / LOAD_FACTOR && capacity < (1 << 30)) capacity <<= 1;
            keys = new long[capacity];
            values = new int[capacity];
            used = new byte[capacity];
            mask = capacity - 1;
            threshold = Math.max(1, (int) (capacity * LOAD_FACTOR));
        }

        int getOrDefault(long key, int defaultValue) {
            int slot = slot(key);
            while (used[slot] != 0) {
                if (keys[slot] == key) return values[slot];
                slot = (slot + 1) & mask;
            }
            return defaultValue;
        }

        void putIfAbsent(long key, int value) {
            ensureCapacity();
            int slot = slot(key);
            while (used[slot] != 0) {
                if (keys[slot] == key) return;
                slot = (slot + 1) & mask;
            }
            used[slot] = 1;
            keys[slot] = key;
            values[slot] = value;
            size++;
        }

        void addTo(long key, int delta) {
            ensureCapacity();
            int slot = slot(key);
            while (used[slot] != 0) {
                if (keys[slot] == key) {
                    values[slot] = Math.addExact(values[slot], delta);
                    return;
                }
                slot = (slot + 1) & mask;
            }
            used[slot] = 1;
            keys[slot] = key;
            values[slot] = delta;
            size++;
        }

        long absoluteValueSum() {
            long sum = 0L;
            for (int i = 0; i < values.length; i++) {
                if (used[i] == 0) continue;
                sum = Math.addExact(sum, Math.abs((long) values[i]));
            }
            return sum;
        }

        void put(long key, int value) {
            ensureCapacity();
            int slot = slot(key);
            while (used[slot] != 0) {
                if (keys[slot] == key) {
                    values[slot] = value;
                    return;
                }
                slot = (slot + 1) & mask;
            }
            used[slot] = 1;
            keys[slot] = key;
            values[slot] = value;
            size++;
        }

        private void ensureCapacity() {
            if (size + 1 <= threshold) return;
            rehash(keys.length << 1);
        }

        private void rehash(int newCapacity) {
            long[] oldKeys = keys;
            int[] oldValues = values;
            byte[] oldUsed = used;

            keys = new long[newCapacity];
            values = new int[newCapacity];
            used = new byte[newCapacity];
            mask = newCapacity - 1;
            threshold = Math.max(1, (int) (newCapacity * LOAD_FACTOR));
            size = 0;

            for (int i = 0; i < oldKeys.length; i++) {
                if (oldUsed[i] == 0) continue;
                put(oldKeys[i], oldValues[i]);
            }
        }

        private int slot(long key) {
            long z = key;
            z ^= z >>> 33;
            z *= 0xff51afd7ed558ccdL;
            z ^= z >>> 33;
            z *= 0xc4ceb9fe1a85ec53L;
            z ^= z >>> 33;
            return ((int) z) & mask;
        }
    }
}
