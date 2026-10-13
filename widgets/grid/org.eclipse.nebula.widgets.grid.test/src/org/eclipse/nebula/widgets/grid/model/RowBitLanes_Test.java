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
 * Differential test of {@link RowBitLanes} against {@link BitSet} under random operation
 * sequences (set, range set/clear, masked set/clear/retain, insert/remove rows, resize).
 */
public class RowBitLanes_Test {

	@Test
	public void randomOperationsMatchBitSetReference() {
		final Random random = new Random(20261013L);
		for (int round = 0; round < 300; round++) {
			int rows = random.nextInt(300);
			final RowBitLanes lanes = new RowBitLanes(3);
			lanes.setRowCount(rows);
			final BitSet[] ref = { new BitSet(), new BitSet(), new BitSet() };
			for (int op = 0; op < 60; op++) {
				final int lane = random.nextInt(3);
				final int kind = random.nextInt(9);
				if (kind == 0 && rows > 0) {
					final int r = random.nextInt(rows);
					final boolean v = random.nextBoolean();
					lanes.set(lane, r, v);
					ref[lane].set(r, v);
				} else if (kind == 1) {
					final int a = random.nextInt(rows + 1);
					final int b = a + random.nextInt(rows - a + 1);
					final boolean v = random.nextBoolean();
					lanes.setRange(lane, a, b, v);
					ref[lane].set(a, b, v);
				} else if (kind == 2) {
					final long[] mask = mask(random, rows);
					final boolean v = random.nextBoolean();
					lanes.setWhere(lane, mask, v);
					final BitSet m = BitSet.valueOf(mask);
					m.clear(rows, Math.max(rows, m.length()));
					if (v) {
						ref[lane].or(m);
					} else {
						ref[lane].andNot(m);
					}
				} else if (kind == 3) {
					final long[] mask = mask(random, rows);
					lanes.retainWhere(lane, mask);
					ref[lane].and(BitSet.valueOf(mask));
				} else if (kind == 4) {
					final int at = random.nextInt(rows + 1);
					final int count = random.nextInt(70);
					lanes.insertRows(at, count);
					for (int l = 0; l < 3; l++) {
						ref[l] = insert(ref[l], at, count, rows);
					}
					rows += count;
				} else if (kind == 5 && rows > 0) {
					final int at = random.nextInt(rows);
					final int count = random.nextInt(rows - at + 1);
					lanes.removeRows(at, count);
					for (int l = 0; l < 3; l++) {
						ref[l] = remove(ref[l], at, count, rows);
					}
					rows -= count;
				} else if (kind == 6) {
					final int newRows = random.nextInt(320);
					lanes.setRowCount(newRows);
					for (int l = 0; l < 3; l++) {
						ref[l].clear(Math.min(rows, newRows), Math.max(rows, newRows));
					}
					rows = newRows;
				} else if (kind == 7) {
					final boolean v = random.nextBoolean();
					lanes.fill(lane, v);
					ref[lane].set(0, rows, v);
				}
				for (int l = 0; l < 3; l++) {
					assertSame(round + "/" + op, ref[l], lanes, l, rows, random);
				}
			}
		}
	}

	@Test
	public void filterAwareCheckAllAndCountsAreWordOperations() {
		final int rows = 1000;
		final RowBitLanes lanes = new RowBitLanes(1);
		lanes.setRowCount(rows);
		final RowPermutation filter = new RowPermutation(rows);
		filter.setFilter(m -> m % 3 == 0);
		final long[] filterBits = new long[(rows + 63) / 64];
		for (int m = 0; m < rows; m++) {
			if (filter.passes(m)) {
				filterBits[m >>> 6] |= 1L << (m & 63);
			}
		}
		lanes.setWhere(0, filterBits, true);
		assertEquals(filter.getViewCount(), lanes.cardinality(0));
		assertEquals(filter.getViewCount(), lanes.cardinality(0, filterBits));
		lanes.set(0, 1, true);
		assertEquals(filter.getViewCount() + 1, lanes.cardinality(0));
		assertEquals(filter.getViewCount(), lanes.cardinality(0, filterBits));
		lanes.setWhere(0, filterBits, false);
		assertEquals(1, lanes.cardinality(0));
		assertTrue(lanes.get(0, 1));
		assertFalse(lanes.get(0, 3));
	}

	private static void assertSame(final String msg, final BitSet ref, final RowBitLanes lanes, final int lane,
			final int rows, final Random random) {
		assertEquals(msg, rows, lanes.getRowCount());
		assertEquals(msg + " cardinality", ref.cardinality(), lanes.cardinality(lane));
		for (int r = 0; r < rows; r++) {
			assertEquals(msg + " row " + r, ref.get(r), lanes.get(lane, r));
		}
		final int from = random.nextInt(rows + 2);
		final int expected = ref.nextSetBit(from);
		assertEquals(msg + " next from " + from, expected < 0 || expected >= rows ? -1 : expected,
				lanes.nextSetRow(lane, from));
		final long[] words = lanes.copyWords(lane, new long[(rows + 63) / 64]);
		assertEquals(msg, ref, BitSet.valueOf(words));
		final long[] mask = mask(random, rows);
		final BitSet and = (BitSet) ref.clone();
		and.and(BitSet.valueOf(mask));
		assertEquals(msg + " masked count", and.cardinality(), lanes.cardinality(lane, mask));
	}

	private static long[] mask(final Random random, final int rows) {
		final long[] m = new long[random.nextInt((rows + 63) / 64 + 2)];
		for (int i = 0; i < m.length; i++) {
			m[i] = random.nextLong() & random.nextLong();
		}
		return m;
	}

	private static BitSet insert(final BitSet src, final int at, final int count, final int rows) {
		final BitSet out = new BitSet();
		for (int r = src.nextSetBit(0); r >= 0 && r < rows; r = src.nextSetBit(r + 1)) {
			out.set(r < at ? r : r + count);
		}
		return out;
	}

	private static BitSet remove(final BitSet src, final int at, final int count, final int rows) {
		final BitSet out = new BitSet();
		for (int r = src.nextSetBit(0); r >= 0 && r < rows; r = src.nextSetBit(r + 1)) {
			if (r < at) {
				out.set(r);
			} else if (r >= at + count) {
				out.set(r - count);
			}
		}
		return out;
	}

	static long[] words(final BitSet set, final int rows) {
		return Arrays.copyOf(set.toLongArray(), (rows + 63) / 64);
	}
}
