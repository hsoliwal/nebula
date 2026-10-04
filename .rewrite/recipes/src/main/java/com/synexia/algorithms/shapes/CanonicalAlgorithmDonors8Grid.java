// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import com.synexia.algorithms.core.ProgressAlgorithm;
import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Canonical superset for implicit-grid connected-component problems such as Number of Islands.
 *
 * <p>The pulled Java donors use DFS, BFS and union-find variants, and differ on whether they mutate
 * the input grid. This donor preserves those variants as explicit policies over one row-major
 * primitive representation.
 */
public final class CanonicalAlgorithmDonors8Grid {
    private CanonicalAlgorithmDonors8Grid() {}

    public enum Strategy {
        DEPTH_FIRST,
        BREADTH_FIRST,
        UNION_FIND
    }

    public enum MutationPolicy {
        /** Do not modify the caller-provided cell array. */
        PRESERVE,
        /** Replace every visited land cell with the configured water value. */
        SINK_LAND_TO_WATER
    }

    /**
     * Row-major primitive grid.
     *
     * <p>For {@link MutationPolicy#PRESERVE}, the constructor snapshots the caller array. For
     * {@link MutationPolicy#SINK_LAND_TO_WATER}, the caller array is retained intentionally so the
     * observed mutation contract of several donor implementations can be reproduced.
     */
    public static final class GridInput {
        private final int rows;
        private final int cols;
        private final byte[] cells;
        private final byte landValue;
        private final byte waterValue;
        private final Strategy strategy;
        private final MutationPolicy mutationPolicy;

        public GridInput(
                int rows,
                int cols,
                byte[] cells,
                Strategy strategy,
                MutationPolicy mutationPolicy) {
            this(rows, cols, cells, (byte) 1, (byte) 0, strategy, mutationPolicy);
        }

        public GridInput(
                int rows,
                int cols,
                byte[] cells,
                byte landValue,
                byte waterValue,
                Strategy strategy,
                MutationPolicy mutationPolicy) {
            if (rows < 0 || cols < 0) throw new IllegalArgumentException("negative grid size");
            long size = (long) rows * cols;
            if (size > Integer.MAX_VALUE) throw new IllegalArgumentException("grid too large");
            Objects.requireNonNull(cells, "cells");
            if (cells.length != (int) size) throw new IllegalArgumentException("cell length");
            if (landValue == waterValue) throw new IllegalArgumentException("land == water");
            this.rows = rows;
            this.cols = cols;
            this.strategy = Objects.requireNonNull(strategy, "strategy");
            this.mutationPolicy = Objects.requireNonNull(mutationPolicy, "mutationPolicy");
            this.landValue = landValue;
            this.waterValue = waterValue;
            this.cells = mutationPolicy == MutationPolicy.PRESERVE ? cells.clone() : cells;
        }

        public int rows() { return rows; }
        public int cols() { return cols; }
        public byte landValue() { return landValue; }
        public byte waterValue() { return waterValue; }
        public Strategy strategy() { return strategy; }
        public MutationPolicy mutationPolicy() { return mutationPolicy; }

        /** Snapshot view for callers. */
        public byte[] cells() { return cells.clone(); }

        byte[] rawCells() { return cells; }
    }

    public record GridComponentResult(
            int components,
            int landCells,
            Strategy strategy,
            MutationPolicy mutationPolicy) {
        public GridComponentResult {
            if (components < 0 || landCells < 0 || components > landCells) {
                throw new IllegalArgumentException("component counts");
            }
            Objects.requireNonNull(strategy, "strategy");
            Objects.requireNonNull(mutationPolicy, "mutationPolicy");
        }
    }

    public static CanonicalAlgorithmDonors.Donor<GridInput, GridComponentResult>
            gridConnectedComponents() {
        return donor(
                "connectivity.grid-components.4-neighbor.byte",
                AlgorithmPurpose.ACCUMULATION,
                AlgorithmShape.GRID_CONNECTED_COMPONENTS,
                "O(rows*cols)",
                "O(rows*cols) primitive arrays",
                (input, supplied) -> switch (input.strategy()) {
                    case DEPTH_FIRST -> traverse(input, false, supplied);
                    case BREADTH_FIRST -> traverse(input, true, supplied);
                    case UNION_FIND -> unionFind(input, supplied);
                });
    }

    public static java.util.List<AlgorithmDescriptor> descriptors() {
        return java.util.List.of(gridConnectedComponents().descriptor());
    }

    private static GridComponentResult traverse(
            GridInput input,
            boolean breadthFirst,
            IProgressMonitor supplied) {
        int rows = input.rows();
        int cols = input.cols();
        byte[] cells = input.rawCells();
        byte[] visited = input.mutationPolicy() == MutationPolicy.PRESERVE
                ? new byte[cells.length]
                : null;
        int[] work = new int[cells.length];
        int components = 0;
        int landCells = 0;

        IProgressMonitor monitor = start(
                supplied,
                breadthFirst ? "grid-components-bfs" : "grid-components-dfs",
                IProgressMonitor.UNKNOWN);
        try {
            for (int index = 0; index < cells.length; index++) {
                monitor.checkCanceled();
                if (!isUnvisitedLand(input, cells, visited, index)) {
                    monitor.worked(1);
                    continue;
                }

                components++;
                if (breadthFirst) {
                    int head = 0;
                    int tail = 0;
                    work[tail++] = index;
                    mark(input, cells, visited, index);
                    while (head < tail) {
                        monitor.checkCanceled();
                        int current = work[head++];
                        landCells++;
                        int row = current / cols;
                        int col = current - row * cols;

                        if (row > 0) {
                            int next = current - cols;
                            if (isUnvisitedLand(input, cells, visited, next)) {
                                mark(input, cells, visited, next);
                                work[tail++] = next;
                            }
                        }
                        if (col + 1 < cols) {
                            int next = current + 1;
                            if (isUnvisitedLand(input, cells, visited, next)) {
                                mark(input, cells, visited, next);
                                work[tail++] = next;
                            }
                        }
                        if (row + 1 < rows) {
                            int next = current + cols;
                            if (isUnvisitedLand(input, cells, visited, next)) {
                                mark(input, cells, visited, next);
                                work[tail++] = next;
                            }
                        }
                        if (col > 0) {
                            int next = current - 1;
                            if (isUnvisitedLand(input, cells, visited, next)) {
                                mark(input, cells, visited, next);
                                work[tail++] = next;
                            }
                        }
                        monitor.worked(1);
                    }
                } else {
                    int size = 0;
                    work[size++] = index;
                    mark(input, cells, visited, index);
                    while (size > 0) {
                        monitor.checkCanceled();
                        int current = work[--size];
                        landCells++;
                        int row = current / cols;
                        int col = current - row * cols;

                        if (row > 0) {
                            int next = current - cols;
                            if (isUnvisitedLand(input, cells, visited, next)) {
                                mark(input, cells, visited, next);
                                work[size++] = next;
                            }
                        }
                        if (col + 1 < cols) {
                            int next = current + 1;
                            if (isUnvisitedLand(input, cells, visited, next)) {
                                mark(input, cells, visited, next);
                                work[size++] = next;
                            }
                        }
                        if (row + 1 < rows) {
                            int next = current + cols;
                            if (isUnvisitedLand(input, cells, visited, next)) {
                                mark(input, cells, visited, next);
                                work[size++] = next;
                            }
                        }
                        if (col > 0) {
                            int next = current - 1;
                            if (isUnvisitedLand(input, cells, visited, next)) {
                                mark(input, cells, visited, next);
                                work[size++] = next;
                            }
                        }
                        monitor.worked(1);
                    }
                }
            }
            return new GridComponentResult(
                    components,
                    landCells,
                    input.strategy(),
                    input.mutationPolicy());
        } finally {
            monitor.done();
        }
    }

    private static GridComponentResult unionFind(
            GridInput input,
            IProgressMonitor supplied) {
        int rows = input.rows();
        int cols = input.cols();
        byte[] cells = input.rawCells();
        int[] parent = new int[cells.length];
        byte[] rank = new byte[cells.length];
        java.util.Arrays.fill(parent, -1);

        int components = 0;
        int landCells = 0;
        IProgressMonitor monitor =
                start(supplied, "grid-components-union-find", IProgressMonitor.UNKNOWN);
        try {
            for (int index = 0; index < cells.length; index++) {
                monitor.checkCanceled();
                if (cells[index] != input.landValue()) {
                    monitor.worked(1);
                    continue;
                }

                parent[index] = index;
                components++;
                landCells++;

                int row = index / cols;
                int col = index - row * cols;
                if (row > 0) {
                    int up = index - cols;
                    if (parent[up] >= 0 && union(parent, rank, index, up)) components--;
                }
                if (col > 0) {
                    int left = index - 1;
                    if (parent[left] >= 0 && union(parent, rank, index, left)) components--;
                }
                monitor.worked(1);
            }

            if (input.mutationPolicy() == MutationPolicy.SINK_LAND_TO_WATER) {
                for (int index = 0; index < cells.length; index++) {
                    monitor.checkCanceled();
                    if (cells[index] == input.landValue()) cells[index] = input.waterValue();
                    monitor.worked(1);
                }
            }

            return new GridComponentResult(
                    components,
                    landCells,
                    input.strategy(),
                    input.mutationPolicy());
        } finally {
            monitor.done();
        }
    }

    private static boolean isUnvisitedLand(
            GridInput input,
            byte[] cells,
            byte[] visited,
            int index) {
        return cells[index] == input.landValue()
                && (visited == null || visited[index] == 0);
    }

    private static void mark(
            GridInput input,
            byte[] cells,
            byte[] visited,
            int index) {
        if (visited != null) {
            visited[index] = 1;
        } else {
            cells[index] = input.waterValue();
        }
    }

    private static boolean union(int[] parent, byte[] rank, int left, int right) {
        int a = find(parent, left);
        int b = find(parent, right);
        if (a == b) return false;

        if (rank[a] < rank[b]) {
            parent[a] = b;
        } else if (rank[a] > rank[b]) {
            parent[b] = a;
        } else {
            parent[b] = a;
            if (rank[a] != Byte.MAX_VALUE) rank[a]++;
        }
        return true;
    }

    private static int find(int[] parent, int node) {
        int root = node;
        while (parent[root] != root) root = parent[root];
        while (node != root) {
            int next = parent[node];
            parent[node] = root;
            node = next;
        }
        return root;
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
}
