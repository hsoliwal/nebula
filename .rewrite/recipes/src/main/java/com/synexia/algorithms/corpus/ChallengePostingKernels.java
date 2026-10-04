// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/** Signed-int posting leaves; callers own validated immutable input arrays. */
final class ChallengePostingKernels {
    private static final int SKEW_RATIO = 32;
    private static final int POLL_MASK = 1023;

    private ChallengePostingKernels() { }

    static boolean skewed(int leftLength, int rightLength) {
        int small = Math.min(leftLength, rightLength);
        return small != 0 && (long) Math.max(leftLength, rightLength) >= (long) small * SKEW_RATIO;
    }

    /** Bounded primitive bulk path; no stream-builder or boxed output allocation. */
    static int[] intersect(int[] left, int[] right, int limit, IProgressMonitor supplied) {
        IProgressMonitor monitor = ProgressMonitors.nonNull(supplied);
        ProgressMonitors.checkCanceled(monitor);
        int[] out = new int[Math.min(limit, Math.min(left.length, right.length))];
        boolean gallop = skewed(left.length, right.length);
        int li = 0;
        int ri = 0;
        int count = 0;
        int polls = 0;
        while (li < left.length && ri < right.length && count < out.length) {
            if ((polls++ & POLL_MASK) == 0) ProgressMonitors.checkCanceled(monitor);
            int lv = left[li];
            int rv = right[ri];
            if (lv == rv) {
                out[count++] = lv;
                li++;
                ri++;
                if (supplied != null) {
                    monitor.worked(1);
                    ProgressMonitors.checkCanceled(monitor);
                }
            } else if (lv < rv) {
                li = gallop ? advance(left, li, rv) : li + 1;
            } else {
                ri = gallop ? advance(right, ri, lv) : ri + 1;
            }
        }
        ProgressMonitors.checkCanceled(monitor);
        return count == out.length ? out : Arrays.copyOf(out, count);
    }

    /** Overflow-safe exponential seek, followed by a lower-bound binary search. */
    static int advance(int[] values, int from, int target) {
        if (from == values.length || values[from] >= target) return from;
        int low = from + 1;
        long span = 1;
        while ((long) from + span < values.length && values[(int) (from + span)] < target) {
            low = (int) (from + span) + 1;
            span <<= 1;
        }
        int high = (int) Math.min((long) values.length, (long) from + span + 1);
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (values[middle] < target) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    static IntStream stream(int[] left, int[] right, IProgressMonitor supplied) {
        IProgressMonitor monitor = ProgressMonitors.nonNull(supplied);
        ProgressMonitors.checkCanceled(monitor);
        return StreamSupport.intStream(new Cursor(left, right, monitor), false);
    }

    /** Sequential cursor; it owns no output array and never exposes its input arrays. */
    private static final class Cursor extends Spliterators.AbstractIntSpliterator {
        private final int[] left;
        private final int[] right;
        private final IProgressMonitor monitor;
        private final boolean gallop;
        private int li;
        private int ri;
        private int polls;

        private Cursor(int[] left, int[] right, IProgressMonitor monitor) {
            super(Math.min(left.length, right.length),
                    Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT
                            | Spliterator.NONNULL | Spliterator.IMMUTABLE);
            this.left = left;
            this.right = right;
            this.monitor = monitor;
            this.gallop = skewed(left.length, right.length);
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            ProgressMonitors.checkCanceled(monitor);
            while (li < left.length && ri < right.length) {
                if ((polls++ & POLL_MASK) == 0) ProgressMonitors.checkCanceled(monitor);
                int lv = left[li];
                int rv = right[ri];
                if (lv == rv) {
                    li++;
                    ri++;
                    monitor.worked(1);
                    ProgressMonitors.checkCanceled(monitor);
                    action.accept(lv);
                    return true;
                }
                if (lv < rv) li = gallop ? advance(left, li, rv) : li + 1;
                else ri = gallop ? advance(right, ri, lv) : ri + 1;
            }
            ProgressMonitors.checkCanceled(monitor);
            return false;
        }

        @Override
        public Spliterator.OfInt trySplit() {
            return null;
        }

        @Override
        public Comparator<? super Integer> getComparator() {
            return null;
        }
    }
}
