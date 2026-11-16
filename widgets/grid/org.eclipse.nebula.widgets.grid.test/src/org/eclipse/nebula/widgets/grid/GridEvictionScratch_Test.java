/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

/**
 * Exactly matches the former two-array (stamp -> victims -> slot) algorithm.
 * Works solely on the existing Grid owner's long[] lane, with no new UI node.
 */
public final class GridEvictionScratch_Test {
    @Test
    public void inPlaceOldestVictimSlotsMatchOriginalTwoArrayOracle() {
        Random random = new Random(0x3D0010A11L);
        for (int trial = 0; trial < 12_000; trial++) {
            int candidates = random.nextInt(258);
            int victims = random.nextInt(candidates + 1);
            long[] lane = new long[candidates + 7];
            for (int slot = 0; slot < candidates; slot++) {
                int stamp = random.nextInt(31);
                lane[slot] = (((long) stamp) << 32) | slot;
            }
            for (int i = candidates - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                long swap = lane[i];
                lane[i] = lane[j];
                lane[j] = swap;
            }
            Arrays.fill(lane, candidates, lane.length, 0x789ABCDE01234567L);

            // Reference: the exact pre-optimization ordering semantics.
            long[] original = Arrays.copyOf(lane, candidates);
            Arrays.sort(original);
            long[] expected = new long[victims];
            for (int i = 0; i < victims; i++) {
                expected[i] = original[i] & 0xffffffffL;
            }
            Arrays.sort(expected);
            long[] retainedIdentity = lane;

            Grid.selectEvictionVictimsInPlace(lane, candidates, victims);

            assertSame("the original parent-owned scratch must be reused", retainedIdentity, lane);
            assertArrayEquals("oldest victims and descending release order " + trial,
                    expected, Arrays.copyOf(lane, victims));
            assertEquals("never overwrite outside populated prefix",
                    0x789ABCDE01234567L, lane[lane.length - 1]);
        }
    }

    @Test
    public void emptyAndSingleVictimSelectionsRemainAllocationFree() {
        long[] empty = {};
        Grid.selectEvictionVictimsInPlace(empty, 0, 0);
        assertEquals(0, empty.length);
        long[] one = { (17L << 32) | 21L, 123_456L };
        Grid.selectEvictionVictimsInPlace(one, 1, 1);
        assertEquals(21L, one[0]);
        assertEquals(123_456L, one[1]);
    }
}
