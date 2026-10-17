/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
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

		// the span 499_995..500_015 covers every first visible row after its start up to its end
		assertEquals(499_995,
				GridRowSpanIndex.earliestCovering(500_000, starts, prefix, starts.length));
		assertEquals(499_995,
				GridRowSpanIndex.earliestCovering(500_005, starts, prefix, starts.length));
		assertEquals(499_995,
				GridRowSpanIndex.earliestCovering(500_015, starts, prefix, starts.length));
		// a row is never covered by its own span, nor past the span's end
		assertEquals(499_995,
				GridRowSpanIndex.earliestCovering(499_995, starts, prefix, starts.length));
		assertEquals(500_016,
				GridRowSpanIndex.earliestCovering(500_016, starts, prefix, starts.length));
		assertEquals(700_000,
				GridRowSpanIndex.earliestCovering(700_000, starts, prefix, starts.length));
		for (int target : new int[] {499_995, 500_000, 500_005, 500_015, 500_016, 700_000}) {
			assertEquals("oracle at " + target, linear(target, starts, prefix),
					GridRowSpanIndex.earliestCovering(target, starts, prefix, starts.length));
		}
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

	@Test
	public void nullAllColumnsIsDistinctFromAnEmptyVisibleColumnMask() {
		assertTrue(GridRowSpanIndex.sameVisibility(null, null));
		assertTrue(GridRowSpanIndex.sameVisibility(new boolean[0], new boolean[0]));
		assertFalse(GridRowSpanIndex.sameVisibility(null, new boolean[0]));
		assertFalse(GridRowSpanIndex.sameVisibility(new boolean[0], null));
		assertFalse(GridRowSpanIndex.sameVisibility(null, new boolean[] {true}));
		assertFalse(GridRowSpanIndex.sameVisibility(new boolean[] {false}, null));
		assertFalse(GridRowSpanIndex.sameVisibility(new boolean[] {false}, new boolean[0]));
	}

	@Test
	public void cachedVisibilitySnapshotOwnsItsArrayAndKeepsItsSentinel() {
		assertNull(GridRowSpanIndex.copyVisibility(null));
		assertArrayEquals(new boolean[0], GridRowSpanIndex.copyVisibility(new boolean[0]));
		boolean[] requested = {true, false, true};
		boolean[] snapshot = GridRowSpanIndex.copyVisibility(requested);
		assertNotSame("the caller may reuse its visibility mask", requested, snapshot);
		assertArrayEquals(requested, snapshot);
		assertTrue(GridRowSpanIndex.sameVisibility(requested, snapshot));
		requested[0] = false;
		assertFalse(GridRowSpanIndex.sameVisibility(requested, snapshot));
		assertArrayEquals("cached projection cannot observe later caller mutations",
				new boolean[] {true, false, true}, snapshot);
	}

	private static int linear(int target, int[] starts, long[] ends) {
		if (target <= 0) return Math.max(0, target);
		for (int i = 0; i < starts.length && starts[i] < target; i++) {
			if (ends[i] >= target) return starts[i];
		}
		return target;
	}
}
