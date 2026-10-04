// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Signed-int posting intersection with immutable preparation and explicit optional JNI.
 * Native loading executes trusted machine code; oracle probes are not a sandbox or a proof
 * of arbitrary native libraries. Java remains available without native loading.
 */
public final class ChallengePostingIntersection {
    private static final int NATIVE_MIN_TOTAL_LENGTH = 256;
    private static volatile boolean nativeEnabled;

    private ChallengePostingIntersection() { }

    /** Validated, privately owned postings; safe for concurrent read-only reuse. */
    public static final class Prepared {
        private final int[] values;

        private Prepared(int[] values) {
            this.values = values;
        }

        public int size() {
            return values.length;
        }

        public int[] toArray() {
            return values.clone();
        }

        /**
         * Read one immutable posting without copying the array.
         *
         * @param ordinal zero-based posting position
         * @return the signed integer at that position
         * @throws IndexOutOfBoundsException if ordinal is outside this posting list
         */
        public int valueAt(int ordinal) {
            return values[ordinal];
        }

        /** Package-local seek on owned values; callers maintain 0 <= from <= size(). */
        int advanceFrom(int from, int target) {
            return ChallengePostingKernels.advance(values, from, target);
        }
    }

    /**
     * Load an explicitly selected library and probe the preserved JNI export before enabling it.
     * A failed load/probe leaves acceleration disabled. A JVM cannot unload a failed binary here.
     */
    public static synchronized void enableNative(Path library) {
        Path normalized = Objects.requireNonNull(library, "library").toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IllegalArgumentException("native library does not exist: " + normalized);
        }
        if (!nativeEnabled) {
            System.load(normalized.toString());
            verifyNative();
            nativeEnabled = true;
        }
    }

    public static boolean nativeEnabled() {
        return nativeEnabled;
    }

    /**
     * Package-local immutable snapshot for the pre-existing non-cancellable catalogue compiler.
     * Do not change that compiler's interruption contract by calling the public cancellable API.
     */
    static Prepared snapshotForIndex(int[] values) {
        int[] owned = Objects.requireNonNull(values, "values").clone();
        for (int index = 1; index < owned.length; index++) {
            if (owned[index] <= owned[index - 1]) {
                throw new IllegalArgumentException("values must be strictly ascending");
            }
        }
        return new Prepared(owned);
    }

    /** Copy and validate once. Racing caller writes do not define a coherent atomic snapshot. */
    public static Prepared prepare(int[] values, IProgressMonitor monitor) {
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        int[] owned = Objects.requireNonNull(values, "values").clone();
        validate(owned, "values", checked);
        return new Prepared(owned);
    }

    public static Prepared prepare(int[] values) {
        return prepare(values, null);
    }

    public static int[] intersect(int[] left, int[] right) {
        return intersect(left, right, null);
    }

    /** Existing raw-array boundary. Callers must not mutate inputs during this call. */
    public static int[] intersect(int[] left, int[] right, IProgressMonitor monitor) {
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        validate(left, "left", checked);
        validate(right, "right", checked);
        return intersectValidated(left, right, Integer.MAX_VALUE, monitor);
    }

    public static int[] intersectJava(int[] left, int[] right) {
        return intersectJava(left, right, null);
    }

    public static int[] intersectJava(int[] left, int[] right, IProgressMonitor monitor) {
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        validate(left, "left", checked);
        validate(right, "right", checked);
        return ChallengePostingKernels.intersect(left, right, Integer.MAX_VALUE, monitor);
    }

    /**
     * Return at most limit smallest matches. Explicit monitors and bounded queries use Java
     * for cooperative cancellation and early termination. Native execution is non-interruptible
     * until it returns; start/end cancellation checks still apply.
     */
    public static int[] intersect(Prepared left, Prepared right, int limit, IProgressMonitor monitor) {
        return intersectValidated(Objects.requireNonNull(left, "left").values,
                Objects.requireNonNull(right, "right").values, limit, monitor);
    }

    private static int[] intersectValidated(int[] a, int[] b, int limit, IProgressMonitor monitor) {
        if (limit < 0) throw new IllegalArgumentException("negative limit");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        if (limit == 0 || a.length == 0 || b.length == 0) return new int[0];
        if (monitor == null && nativeEnabled && limit >= Math.min(a.length, b.length)
                && (long) a.length + b.length >= NATIVE_MIN_TOTAL_LENGTH
                && !ChallengePostingKernels.skewed(a.length, b.length)) {
            int[] result = nativeIntersect(a, b);
            validate(result, "native result", checked);
            if (result.length > Math.min(a.length, b.length)) {
                throw new IllegalStateException("native result exceeds intersection capacity");
            }
            ProgressMonitors.checkCanceled(checked);
            return result;
        }
        return ChallengePostingKernels.intersect(a, b, limit, monitor);
    }

    /** Lazy sorted/distinct stream; no output materialization, with per-cursor cancellation. */
    public static IntStream stream(Prepared left, Prepared right, IProgressMonitor monitor) {
        return ChallengePostingKernels.stream(Objects.requireNonNull(left, "left").values,
                Objects.requireNonNull(right, "right").values, monitor);
    }

    // Kept for existing package consumers. This validates but does not transfer ownership.
    static int[] requireSortedUnique(int[] values, String label) {
        validate(values, label, IProgressMonitor.noop());
        return values;
    }

    private static void validate(int[] values, String label, IProgressMonitor monitor) {
        Objects.requireNonNull(values, label);
        ProgressMonitors.checkCanceled(monitor);
        for (int index = 1; index < values.length; index++) {
            if ((index & 1023) == 0) ProgressMonitors.checkCanceled(monitor);
            if (values[index] <= values[index - 1]) {
                throw new IllegalArgumentException(label + " must be strictly ascending");
            }
        }
        ProgressMonitors.checkCanceled(monitor);
    }

    private static void verifyNative() {
        int[][] cases = {
            {}, {0}, {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE},
            {-7, -3, 1, 5, 11}, {-8, -4, 0, 4, 12},
            IntStream.range(0, 513).map(value -> value * 3).toArray()
        };
        for (int[] left : cases) {
            for (int[] right : cases) {
                int[] expected = intersectJava(left, right);
                int[] a = left.clone();
                int[] b = right.clone();
                int[] actual = nativeIntersect(a, b);
                if (!Arrays.equals(expected, actual) || !Arrays.equals(a, left) || !Arrays.equals(b, right)) {
                    throw new LinkageError("native posting intersection failed oracle probe");
                }
            }
        }
    }

    private static native int[] nativeIntersect(int[] left, int[] right);
}
