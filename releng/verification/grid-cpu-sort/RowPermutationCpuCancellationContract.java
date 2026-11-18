/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.grid.model;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/** Headless Java 21 proof against actual product RowPermutation.java, no SWT stubs. */
public final class RowPermutationCpuCancellationContract {
    public static void main(String[] args) {
        int[] sizes = {0, 1, 2, 1023, 4096, 50000};
        for (int size : sizes) {
            int[] keys = new int[size];
            Random random = new Random(0x6abdd3L + size);
            for (int i = 0; i < size; i++) {
                keys[i] = switch (i % 7) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    case 2 -> -1;
                    default -> random.nextInt(77) - 38;
                };
            }
            int[] before = keys.clone();
            for (boolean descending : new boolean[] {false, true}) {
                int[] expected = RowPermutation.sortedOrder(keys, size, descending);
                int[] actual = RowPermutation.sortedOrder(keys, size, descending, () -> false);
                if (!Arrays.equals(expected, actual)) {
                    throw new AssertionError("sort changed for n=" + size + ", descending=" + descending);
                }
                boolean[] seen = new boolean[size];
                for (int row : actual) {
                    if (row < 0 || row >= size || seen[row]) {
                        throw new AssertionError("invalid permutation n=" + size);
                    }
                    seen[row] = true;
                }
            }
            if (!Arrays.equals(before, keys)) {
                throw new AssertionError("source key lane mutated at n=" + size);
            }
        }

        final int n = 100_000;
        int[] large = new int[n];
        for (int i = 0; i < n; i++) large[i] = n - i;
        int[] original = large.clone();
        AtomicInteger polls = new AtomicInteger();
        try {
            RowPermutation.sortedOrder(large, n, false, () -> polls.incrementAndGet() >= 5);
            throw new AssertionError("in-flight cancellation not honored");
        } catch (CancellationException expected) {
            if (polls.get() < 5) throw new AssertionError("too few cancellation polls");
        }
        if (!Arrays.equals(original, large)) throw new AssertionError("cancelled sort mutated input");

        try {
            RowPermutation.sortedOrder(large, n, true, () -> true);
            throw new AssertionError("pre-cancelled sort not rejected");
        } catch (CancellationException expected) {
            // The algorithm must stop without touching any SWT or RowPermutation state.
        }
        RowPermutation live = new RowPermutation(n);
        int version = live.getVersion();
        RowPermutation.sortedOrder(large, n, false, () -> false);
        if (live.getVersion() != version) throw new AssertionError("CPU sort mutated installed model");
        System.out.println("PASS CPU radix: all sizes, ascending/descending, stable validity, cancellation, immutable inputs");
    }
}
