/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

/**
 * Contracts of the containers mirrored from SWT #122 (adapted from its
 * {@code Test_ItemResidencyLanes}): {@link RowSparseBits} against {@link BitSet}, and
 * {@link RowStateLanes} patterns with exception precedence against a boolean reference.
 */
public class RowStateLanes_Test {

	@Test
	public void sparseBitsMatchBitSetUnderRandomOperations() {
		final RowSparseBits set = new RowSparseBits();
		final BitSet ref = new BitSet();
		final Random random = new Random(3);
		final int limit = 600_000;
		for (int op = 0; op < 20_000; op++) {
			final int a = random.nextInt(limit);
			final int kind = random.nextInt(10);
			final boolean on = random.nextBoolean();
			if (kind < 5) {
				set.set(a, on);
				ref.set(a, on);
			} else {
				final int b = Math.min(limit, a + random.nextInt(kind < 8 ? 300 : 150_000));
				set.setRange(a, b, on);
				ref.set(a, b, on);
			}
			if (op % 2000 == 0) {
				assertSame(set, ref, limit);
			}
		}
		assertSame(set, ref, limit);
		set.shift(1000, 1, limit + 1, new int[][] { new int[4] });
		final BitSet shifted = new BitSet();
		for (int i = ref.nextSetBit(0); i >= 0; i = ref.nextSetBit(i + 1)) {
			shifted.set(i < 1000 ? i : i + 1);
		}
		assertSame(set, shifted, limit + 1);
	}

	@Test
	public void everyThirdRowOfOneBillionIsOnePatternWithExactCounts() {
		final RowStateLanes lanes = new RowStateLanes(2);
		final int n = 1_000_000_000;
		lanes.setCount(n);
		final int before = lanes.bytes();
		lanes.setPattern(0, 0, n, 3, 0b001);
		assertTrue("O(1) memory", lanes.bytes() - before < 512);
		assertEquals(333_333_334, lanes.countRange(0, 0, n));
		assertTrue(lanes.get(0, 999_999_999));
		assertFalse(lanes.get(0, 999_999_998));
		lanes.set(0, 3, false);
		lanes.set(0, 4, true);
		assertFalse("exception wins over pattern", lanes.get(0, 3));
		assertTrue(lanes.get(0, 4));
		assertEquals(333_333_334, lanes.countRange(0, 0, n));
		assertEquals(0, lanes.nextSet(0, 0));
		assertEquals(4, lanes.nextSet(0, 1));
		assertEquals(6, lanes.nextSet(0, 5));
		lanes.setRange(0, 10, 20, true);
		assertEquals(10, lanes.countRange(0, 10, 20));
		assertEquals("the pattern resumes with its phase after the cut", 1, lanes.countRange(0, 20, 24));
		lanes.setPattern(1, 500, 600, 4, 0b0101);
		assertEquals(50, lanes.countRange(1, 0, n));
		lanes.insert(501);
		assertTrue(lanes.get(1, 500));
		assertFalse("an inserted row is default", lanes.get(1, 501));
		assertTrue("the pattern phase follows the shifted rows", lanes.get(1, 503));
		assertEquals(50, lanes.countRange(1, 0, n));
		lanes.remove(500);
		assertFalse(lanes.get(1, 500));
		assertTrue(lanes.get(1, 502));
		assertEquals(49, lanes.countRange(1, 0, n));
	}

	@Test
	public void patternLanesMatchABooleanReference() {
		final int n = 50_000;
		final RowStateLanes lanes = new RowStateLanes(1);
		lanes.setCount(n);
		final boolean[] ref = new boolean[n];
		final Random random = new Random(8);
		for (int op = 0; op < 600; op++) {
			final int a = random.nextInt(n);
			final int b = Math.min(n, a + random.nextInt(5000));
			final int kind = random.nextInt(4);
			if (kind == 0) {
				final int stride = 1 + random.nextInt(32);
				final int mask = random.nextInt();
				lanes.setPattern(0, a, b, stride, mask);
				for (int i = a; i < b; i++) {
					ref[i] = (mask >>> ((i - a) % stride) & 1) != 0;
				}
			} else if (kind == 1) {
				final boolean on = random.nextBoolean();
				lanes.setRange(0, a, b, on);
				Arrays.fill(ref, a, b, on);
			} else {
				final boolean on = random.nextBoolean();
				lanes.set(0, a, on);
				ref[a] = on;
			}
		}
		int total = 0;
		int first = -1;
		for (int i = 0; i < n; i++) {
			assertEquals("row " + i, ref[i], lanes.get(0, i));
			if (ref[i]) {
				total++;
				if (first < 0) {
					first = i;
				}
			}
		}
		assertEquals(total, lanes.countRange(0, 0, n));
		assertEquals(first, lanes.nextSet(0, 0));
	}

	private static void assertSame(final RowSparseBits set, final BitSet ref, final int limit) {
		assertEquals(ref.cardinality(), set.count(0, limit));
		for (int i = ref.nextSetBit(0), j = set.next(0, limit); i >= 0 || j >= 0; i = ref.nextSetBit(i + 1), j = set
				.next(j + 1, limit)) {
			assertEquals("next", i, j);
		}
		final Random random = new Random(limit);
		for (int probe = 0; probe < 2000; probe++) {
			final int a = random.nextInt(limit);
			final int b = Math.min(limit, a + random.nextInt(70_000));
			assertEquals(ref.get(a), set.get(a));
			assertEquals(ref.get(a, b).cardinality(), set.count(a, b));
		}
	}
}
