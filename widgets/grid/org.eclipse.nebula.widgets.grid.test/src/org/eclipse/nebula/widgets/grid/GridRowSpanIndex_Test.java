/*******************************************************************************
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0, available at
 * https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

/** Independent primitive oracle for the sparse row-span prefix frontier. */
public class GridRowSpanIndex_Test {

	@Test
	public void sparseFrontierFindsEarliestCoveringSpanAtDistantViewport() {
		int[] starts = {7, 120, 499_995, 900_000};
		long[] prefix = {9, 125, 500_015, 900_002};

		assertEquals(500_000,
				GridRowSpanIndex.earliestCovering(500_000, starts, prefix, starts.length));
		assertEquals(499_995,
				GridRowSpanIndex.earliestCovering(500_005, starts, prefix, starts.length));
		assertEquals(700_000,
				GridRowSpanIndex.earliestCovering(700_000, starts, prefix, starts.length));
	}

	@Test
	public void longIntervalEndsDoNotOverflowAtIntegerLimit() {
		int[] starts = {Integer.MAX_VALUE - 8};
		long[] prefix = {(long) Integer.MAX_VALUE + 100L};
		assertEquals(Integer.MAX_VALUE - 8,
				GridRowSpanIndex.earliestCovering(Integer.MAX_VALUE, starts, prefix, 1));
	}

	@Test
	public void seededSparseCorpusMatchesIndependentLinearOracle() {
		Random random = new Random(0x5350414E494E4445L);
		for (int iteration = 0; iteration < 16_384; iteration++) {
			int count = random.nextInt(48);
			int[] starts = new int[count];
			long[] prefix = new long[count];
			long max = Long.MIN_VALUE;
			int row = random.nextInt(8);
			long[] ends = new long[count];
			for (int i = 0; i < count; i++) {
				row += 1 + random.nextInt(2_000);
				starts[i] = row;
				ends[i] = (long) row + random.nextInt(20_000);
				max = Math.max(max, ends[i]);
				prefix[i] = max;
			}
			int target = random.nextInt(100_000);
			assertEquals("iteration " + iteration,
					linear(target, starts, ends),
					GridRowSpanIndex.earliestCovering(target, starts, prefix, count));
		}
	}

	private static int linear(int target, int[] starts, long[] ends) {
		if (target <= 0) return Math.max(0, target);
		for (int i = 0; i < starts.length && starts[i] < target; i++) {
			if (ends[i] >= target) return starts[i];
		}
		return target;
	}
}
