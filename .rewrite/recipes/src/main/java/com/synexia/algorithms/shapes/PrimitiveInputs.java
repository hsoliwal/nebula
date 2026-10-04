// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import java.util.Arrays;
import java.util.Objects;

/** Packed/primitive request types for canonical kernels. */
public final class PrimitiveInputs {
    private PrimitiveInputs() {}

    public record LongSearch(long[] values, long key) {
        public LongSearch {
            values = copy(values);
        }
        @Override public long[] values() { return values.clone(); }
    }

    public record LongTopK(long[] values, int k) {
        public LongTopK {
            values = copy(values);
            if (k < 0 || k > values.length) throw new IllegalArgumentException("invalid k");
        }
        @Override public long[] values() { return values.clone(); }
    }

    public record LongSortedInsert(long[] values, long value) {
        public LongSortedInsert { values = copy(values); }
        @Override public long[] values() { return values.clone(); }
    }

    public record LongMembership(long[] values, long value) {
        public LongMembership { values = copy(values); }
        @Override public long[] values() { return values.clone(); }
    }

    public record LongWindow(long[] values, int width) {
        public LongWindow {
            values = copy(values);
            if (width < 1 || width > values.length) throw new IllegalArgumentException("invalid width");
        }
        @Override public long[] values() { return values.clone(); }
    }

    public record LongPair(long[] left, long[] right) {
        public LongPair {
            left = copy(left);
            right = copy(right);
        }
        @Override public long[] left() { return left.clone(); }
        @Override public long[] right() { return right.clone(); }
    }

    public record StringPattern(String text, String pattern) {
        public StringPattern {
            text = Objects.requireNonNull(text, "text");
            pattern = Objects.requireNonNull(pattern, "pattern");
        }
    }

    public record StringPair(String left, String right) {
        public StringPair {
            left = Objects.requireNonNull(left, "left");
            right = Objects.requireNonNull(right, "right");
        }
    }

    public record LongTransform(long[] values, long multiplier, long addend) {
        public LongTransform { values = copy(values); }
        @Override public long[] values() { return values.clone(); }
    }

    public record LongPartition(long[] values, long pivot) {
        public LongPartition { values = copy(values); }
        @Override public long[] values() { return values.clone(); }
    }

    public record StateTransition(int state, int event, int[][] table) {
        public StateTransition {
            Objects.requireNonNull(table, "table");
            table = Arrays.stream(table).map(row -> row == null ? null : row.clone()).toArray(int[][]::new);
        }
        @Override public int[][] table() {
            return Arrays.stream(table).map(row -> row == null ? null : row.clone()).toArray(int[][]::new);
        }
    }

    public record Rendezvous(String key, String[] nodes) {
        public Rendezvous {
            key = Objects.requireNonNull(key, "key");
            nodes = Objects.requireNonNull(nodes, "nodes").clone();
            if (nodes.length == 0) throw new IllegalArgumentException("nodes must not be empty");
            for (String node : nodes) Objects.requireNonNull(node, "node");
        }
        @Override public String[] nodes() { return nodes.clone(); }
    }

    public record IntCsrGraph(int[] offsets, int[] edges, int source) {
        public IntCsrGraph {
            offsets = copy(offsets);
            edges = copy(edges);
            validateCsr(offsets, edges);
            if (source < 0 || source >= offsets.length - 1) throw new IllegalArgumentException("source");
        }
        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
    }

    public record WeightedIntCsrGraph(int[] offsets, int[] edges, long[] weights, int source) {
        public WeightedIntCsrGraph {
            offsets = copy(offsets);
            edges = copy(edges);
            weights = copy(weights);
            validateCsr(offsets, edges);
            if (weights.length != edges.length) throw new IllegalArgumentException("weights");
            if (source < 0 || source >= offsets.length - 1) throw new IllegalArgumentException("source");
            for (long weight : weights) if (weight < 0L) throw new IllegalArgumentException("negative weight");
        }
        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
        @Override public long[] weights() { return weights.clone(); }
    }

    public record IntDag(int[] offsets, int[] edges) {
        public IntDag {
            offsets = copy(offsets);
            edges = copy(edges);
            validateCsr(offsets, edges);
        }
        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] edges() { return edges.clone(); }
    }

    private static void validateCsr(int[] offsets, int[] edges) {
        if (offsets.length < 2 || offsets[0] != 0 || offsets[offsets.length - 1] != edges.length) {
            throw new IllegalArgumentException("invalid CSR offsets");
        }
        for (int i = 1; i < offsets.length; i++) {
            if (offsets[i] < offsets[i - 1]) throw new IllegalArgumentException("offsets not monotonic");
        }
        int vertices = offsets.length - 1;
        for (int edge : edges) if (edge < 0 || edge >= vertices) throw new IllegalArgumentException("edge");
    }

    private static long[] copy(long[] value) {
        return Objects.requireNonNull(value, "value").clone();
    }

    private static int[] copy(int[] value) {
        return Objects.requireNonNull(value, "value").clone();
    }
}
