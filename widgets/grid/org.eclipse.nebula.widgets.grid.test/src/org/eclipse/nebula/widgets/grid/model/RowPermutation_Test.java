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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Random;

import org.junit.Test;

/**
 * Differential tests of {@link RowPermutation} against JDK references: a stable
 * {@code Arrays.sort(Integer[], Comparator)} of the model order and a {@link BitSet} filter.
 */
public class RowPermutation_Test {

	private static final int[] SIZES = { 0, 1, 2, 3, 15, 16, 17, 63, 64, 65, 127, 128, 129, 1000, 4099 };

	@Test
	public void identityMapsEveryRowToItself() {
		final RowPermutation p = new RowPermutation(70);
		assertTrue(p.isIdentity());
		assertEquals(70, p.getViewCount());
		for (int i = 0; i < 70; i++) {
			assertEquals(i, p.modelRow(i));
			assertEquals(i, p.viewIndex(i));
			assertTrue(p.passes(i));
		}
	}

	@Test
	public void sortByIntKeysEqualsStableReferenceSort() {
		final Random random = new Random(20261013L);
		for (final int n : SIZES) {
			for (int round = 0; round < 6; round++) {
				final int[] keys = keys(random, n, round);
				for (final boolean descending : new boolean[] { false, true }) {
					final RowPermutation p = new RowPermutation(n);
					p.sortByIntKeys(keys, descending);
					assertArrayEquals("n=" + n + " round=" + round + " desc=" + descending,
							referenceOrder(keys, n, descending), view(p));
					assertInverse(p);
				}
			}
		}
	}

	@Test
	public void sortWithComparatorEqualsStableReferenceSort() {
		final Random random = new Random(7L);
		for (final int n : SIZES) {
			final String[] text = new String[n];
			for (int i = 0; i < n; i++) {
				text[i] = Integer.toString(random.nextInt(Math.max(1, n / 3)), 36);
			}
			final RowPermutation p = new RowPermutation(n);
			p.sort((a, b) -> text[a].compareTo(text[b]));
			final Integer[] boxed = boxedIdentity(n);
			Arrays.sort(boxed, Comparator.comparing((Integer i) -> text[i]));
			assertArrayEquals("n=" + n, unbox(boxed), view(p));
		}
	}

	@Test
	public void filterEqualsBitSetReferenceInModelAndSortedOrder() {
		final Random random = new Random(99L);
		for (final int n : SIZES) {
			final int[] keys = keys(random, n, 1);
			final BitSet reference = new BitSet(n);
			for (int i = 0; i < n; i++) {
				if (random.nextInt(3) != 0) {
					reference.set(i);
				}
			}
			final RowPermutation p = new RowPermutation(n);
			p.setFilter(reference::get);
			assertArrayEquals(reference.stream().toArray(), view(p));
			assertInverse(p);

			p.sortByIntKeys(keys, true);
			final int[] expected = Arrays.stream(referenceOrder(keys, n, true)).filter(reference::get).toArray();
			assertArrayEquals("n=" + n, expected, view(p));
			assertInverse(p);

			final RowPermutation q = new RowPermutation(n);
			q.sortByIntKeys(keys, true);
			q.setFilterBits(Arrays.copyOf(reference.toLongArray(), (n + 63) / 64));
			assertArrayEquals(expected, view(q));
		}
	}

	@Test
	public void filterBitsPastTheModelAreIgnored() {
		final RowPermutation p = new RowPermutation(70);
		p.setFilterBits(new long[] { 0L, -1L });
		assertEquals(6, p.getViewCount());
		assertEquals(64, p.modelRow(0));
		assertEquals(69, p.modelRow(5));
		assertFalse(p.passes(0));
		assertEquals(-1, p.viewIndex(0));
		assertEquals(0, p.viewIndex(64));
	}

	@Test
	public void clearSortKeepsFilterAndClearFilterKeepsSort() {
		final int[] keys = { 5, 4, 3, 2, 1, 0 };
		final RowPermutation p = new RowPermutation(6);
		p.sortByIntKeys(keys, false);
		p.setFilter(m -> m % 2 == 0);
		assertArrayEquals(new int[] { 4, 2, 0 }, view(p));
		p.clearSort();
		assertArrayEquals(new int[] { 0, 2, 4 }, view(p));
		p.sortByIntKeys(keys, false);
		p.clearFilter();
		assertArrayEquals(new int[] { 5, 4, 3, 2, 1, 0 }, view(p));
		assertTrue(p.isSorted());
		assertFalse(p.isFiltered());
	}

	@Test
	public void setOrderValidatesThePermutation() {
		final RowPermutation p = new RowPermutation(4);
		p.setOrder(new int[] { 2, 0, 3, 1 });
		assertArrayEquals(new int[] { 2, 0, 3, 1 }, view(p));
		assertEquals(1, p.viewIndex(0));
		for (final int[] bad : new int[][] { { 0, 0, 1, 2 }, { 0, 1, 2 }, { 0, 1, 2, 4 }, { -1, 0, 1, 2 } }) {
			try {
				p.setOrder(bad);
				fail(Arrays.toString(bad));
			} catch (final IllegalArgumentException expected) {
				// expected
			}
		}
		assertArrayEquals("a rejected order leaves the permutation unchanged", new int[] { 2, 0, 3, 1 }, view(p));
	}

	@Test
	public void indicesOutsideTheViewAreRejected() {
		final RowPermutation p = new RowPermutation(3);
		p.setFilter(m -> m != 1);
		for (final int bad : new int[] { -1, 2, 3 }) {
			try {
				p.modelRow(bad);
				fail("view " + bad);
			} catch (final IndexOutOfBoundsException expected) {
				// expected
			}
		}
		try {
			p.viewIndex(3);
			fail("model 3");
		} catch (final IndexOutOfBoundsException expected) {
			// expected
		}
	}

	@Test
	public void copyModelRowsCopiesTheView() {
		final RowPermutation p = new RowPermutation(10);
		p.sortByIntKeys(new int[] { 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 }, false);
		final int[] target = new int[6];
		assertEquals(4, p.copyModelRows(6, target, 2, 10));
		assertArrayEquals(new int[] { 0, 0, 3, 2, 1, 0 }, target);
		assertEquals(0, p.copyModelRows(10, target, 0, 3));
	}

	@Test
	public void versionChangesOnEveryMutationAndSizeResetsToIdentity() {
		final RowPermutation p = new RowPermutation(5);
		final int v0 = p.getVersion();
		p.sortByIntKeys(new int[] { 1, 1, 0, 0, 2 }, false);
		final int v1 = p.getVersion();
		assertNotEquals(v0, v1);
		p.setFilter(m -> true);
		assertNotEquals(v1, p.getVersion());
		p.setModelCount(3);
		assertTrue(p.isIdentity());
		assertArrayEquals(new int[] { 0, 1, 2 }, view(p));
	}

	@Test
	public void repeatedSortsOfOneSizeReuseTheirArrays() throws Exception {
		final RowPermutation p = new RowPermutation(4096);
		final int[] keys = keys(new Random(3L), 4096, 1);
		p.sortByIntKeys(keys, false);
		p.setFilter(m -> (m & 1) == 0);
		p.viewIndex(0);
		final Object order = field(p, "order");
		final Object visible = field(p, "visible");
		final Object inverse = field(p, "inverse");
		final Object bits = field(p, "bits");
		for (int i = 0; i < 5; i++) {
			p.sortByIntKeys(keys, (i & 1) == 0);
			p.setFilter(m -> (m % 3) == 0);
			p.viewIndex(0);
		}
		// the radix sort swaps order and its scatter buffer (inverse): same two arrays, any parity
		final Object o2 = field(p, "order");
		final Object i2 = field(p, "inverse");
		assertTrue((order == o2 && inverse == i2) || (order == i2 && inverse == o2));
		assertTrue(visible == field(p, "visible"));
		assertTrue(bits == field(p, "bits"));
	}

	static int[] view(final RowPermutation p) {
		final int[] out = new int[p.getViewCount()];
		for (int v = 0; v < out.length; v++) {
			out[v] = p.modelRow(v);
		}
		return out;
	}

	private static void assertInverse(final RowPermutation p) {
		final boolean[] shown = new boolean[p.getModelCount()];
		for (int v = 0; v < p.getViewCount(); v++) {
			assertEquals(v, p.viewIndex(p.modelRow(v)));
			shown[p.modelRow(v)] = true;
		}
		for (int m = 0; m < shown.length; m++) {
			if (!shown[m]) {
				assertEquals(-1, p.viewIndex(m));
			}
		}
	}

	/** Random, constant, ascending, descending, organ-pipe and sawtooth key columns. */
	static int[] keys(final Random random, final int n, final int round) {
		final int[] k = new int[n];
		for (int i = 0; i < n; i++) {
			switch (round % 6) {
			case 0:
				k[i] = random.nextInt();
				break;
			case 1:
				k[i] = random.nextInt(Math.max(1, n / 4));
				break;
			case 2:
				k[i] = 7;
				break;
			case 3:
				k[i] = i;
				break;
			case 4:
				k[i] = i < n / 2 ? i : n - i;
				break;
			default:
				k[i] = i % 5 == 0 ? Integer.MIN_VALUE : (i % 7 == 0 ? Integer.MAX_VALUE : i % 13);
				break;
			}
		}
		return k;
	}

	static int[] referenceOrder(final int[] keys, final int n, final boolean descending) {
		final Integer[] boxed = boxedIdentity(n);
		final Comparator<Integer> byKey = (a, b) -> Integer.compare(keys[a], keys[b]);
		Arrays.sort(boxed, descending ? (a, b) -> byKey.compare(b, a) : byKey);
		return unbox(boxed);
	}

	private static Integer[] boxedIdentity(final int n) {
		final Integer[] boxed = new Integer[n];
		for (int i = 0; i < n; i++) {
			boxed[i] = i;
		}
		return boxed;
	}

	private static int[] unbox(final Integer[] boxed) {
		final int[] out = new int[boxed.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = boxed[i];
		}
		return out;
	}

	private static Object field(final Object target, final String name) throws Exception {
		final java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}
}
