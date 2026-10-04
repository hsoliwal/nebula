// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import java.util.Arrays;
import java.util.Objects;

/** Request/result records for stateful or multi-input canonical shapes. */
public final class StatefulInputs {
    private StatefulInputs() {}

    public record Reachability(PrimitiveInputs.IntCsrGraph graph, int target) {
        public Reachability {
            Objects.requireNonNull(graph, "graph");
            if (target < 0 || target >= graph.offsets().length - 1) {
                throw new IllegalArgumentException("target");
            }
        }
    }

    public record CacheTrace(byte[] operations, long[] keys, long[] values, int capacity) {
        public CacheTrace {
            operations = Objects.requireNonNull(operations, "operations").clone();
            keys = Objects.requireNonNull(keys, "keys").clone();
            values = Objects.requireNonNull(values, "values").clone();
            if (operations.length != keys.length || keys.length != values.length) {
                throw new IllegalArgumentException("trace lengths");
            }
            if (capacity < 1 || capacity > (1 << 29)) {
                throw new IllegalArgumentException("capacity");
            }
            for (byte operation : operations) if (operation != 0 && operation != 1) {
                throw new IllegalArgumentException("operation must be 0=get or 1=put");
            }
        }
        @Override public byte[] operations() { return operations.clone(); }
        @Override public long[] keys() { return keys.clone(); }
        @Override public long[] values() { return values.clone(); }
    }

    public record CacheTraceResult(long[] values, byte[] hits) {
        public CacheTraceResult {
            values = Objects.requireNonNull(values, "values").clone();
            hits = Objects.requireNonNull(hits, "hits").clone();
            if (values.length != hits.length) throw new IllegalArgumentException("result lengths");
        }
        @Override public long[] values() { return values.clone(); }
        @Override public byte[] hits() { return hits.clone(); }
    }

    public record PriorityJobs(long[] jobIds, long[] priorities) {
        public PriorityJobs {
            jobIds = Objects.requireNonNull(jobIds, "jobIds").clone();
            priorities = Objects.requireNonNull(priorities, "priorities").clone();
            if (jobIds.length != priorities.length) throw new IllegalArgumentException("job lengths");
        }
        @Override public long[] jobIds() { return jobIds.clone(); }
        @Override public long[] priorities() { return priorities.clone(); }
    }

    public record TokenBucketTrace(
            long[] requestNanos,
            long capacity,
            long refillTokens,
            long refillPeriodNanos) {
        public TokenBucketTrace {
            requestNanos = Objects.requireNonNull(requestNanos, "requestNanos").clone();
            if (capacity < 1 || refillTokens < 1 || refillPeriodNanos < 1) {
                throw new IllegalArgumentException("token bucket parameters");
            }
            long previous = -1L;
            for (long value : requestNanos) {
                if (value < 0L || value < previous) {
                    throw new IllegalArgumentException(
                            "request times must be nonnegative and monotonic");
                }
                previous = value;
            }
        }
        @Override public long[] requestNanos() { return requestNanos.clone(); }
    }

    public record LongRuns(long[][] runs) {
        public LongRuns {
            Objects.requireNonNull(runs, "runs");
            runs = Arrays.stream(runs)
                    .map(run -> Objects.requireNonNull(run, "run").clone())
                    .toArray(long[][]::new);
        }
        @Override public long[][] runs() {
            return Arrays.stream(runs).map(long[]::clone).toArray(long[][]::new);
        }
    }
}
