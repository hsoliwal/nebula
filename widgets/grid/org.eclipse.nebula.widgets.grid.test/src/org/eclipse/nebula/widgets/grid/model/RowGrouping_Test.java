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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

/**
 * Differential test of {@link RowGrouping} against a {@link LinkedHashMap} reference grouping of
 * the source view (first-appearance group order, stable rows, header + rows of expanded groups).
 */
public class RowGrouping_Test {

	@Test
	public void displayEqualsReferenceUnderSortFilterAndCollapse() {
		final Random random = new Random(20261013L);
		for (final int n : new int[] { 0, 1, 2, 17, 64, 65, 300, 2000 }) {
			for (int round = 0; round < 4; round++) {
				final int[] sortKeys = RowPermutation_Test.keys(random, n, round);
				final int[] groupKeys = new int[n];
				final int[] values = new int[n];
				for (int m = 0; m < n; m++) {
					groupKeys[m] = random.nextInt(round == 3 ? Math.max(1, n) : 7) - 3;
					values[m] = random.nextInt(2001) - 1000;
				}
				final RowPermutation p = new RowPermutation(n);
				final RowGrouping grouping = new RowGrouping(p);
				grouping.groupBy(groupKeys);
				final Set<Integer> collapsedKeys = new TreeSet<>();
				assertSame("n=" + n, p, grouping, groupKeys, collapsedKeys, values);

				p.sortByIntKeys(sortKeys, round % 2 == 0);
				assertSame("sorted n=" + n, p, grouping, groupKeys, collapsedKeys, values);

				if (grouping.getGroupCount() > 0) {
					final int g = random.nextInt(grouping.getGroupCount());
					collapsedKeys.add(grouping.getGroupKey(g));
					grouping.setExpanded(g, false);
					assertSame("collapsed n=" + n, p, grouping, groupKeys, collapsedKeys, values);
				}

				p.setFilter(m -> (m % 5) != 0);
				assertSame("filtered n=" + n, p, grouping, groupKeys, collapsedKeys, values);

				p.sortByIntKeys(sortKeys, round % 2 != 0);
				assertSame("resorted n=" + n, p, grouping, groupKeys, collapsedKeys, values);

				grouping.setExpanded(0, grouping.getGroupCount(), false);
				for (int g = 0; g < grouping.getGroupCount(); g++) {
					collapsedKeys.add(grouping.getGroupKey(g));
				}
				assertEquals(grouping.getGroupCount(), grouping.getCollapsedCount());
				assertSame("all collapsed n=" + n, p, grouping, groupKeys, collapsedKeys, values);

				grouping.setExpanded(0, grouping.getGroupCount(), true);
				collapsedKeys.clear();
				assertEquals(0, grouping.getCollapsedCount());
				assertSame("all expanded n=" + n, p, grouping, groupKeys, collapsedKeys, values);
			}
		}
	}

	@Test
	public void clearShowsTheSourceView() {
		final RowPermutation p = new RowPermutation(5);
		p.sortByIntKeys(new int[] { 4, 3, 2, 1, 0 }, false);
		final RowGrouping grouping = new RowGrouping(p);
		grouping.groupBy(new int[] { 0, 1, 0, 1, 0 });
		assertEquals(7, grouping.getViewCount());
		final int version = grouping.getVersion();
		grouping.clear();
		assertFalse(grouping.isGrouped());
		assertNotEquals(version, grouping.getVersion());
		assertArrayEquals(new int[] { 4, 3, 2, 1, 0 }, RowPermutation_Test.view(p));
		assertEquals(5, grouping.getViewCount());
		for (int v = 0; v < 5; v++) {
			assertEquals(p.modelRow(v), grouping.modelRow(v));
			assertFalse(grouping.isGroupHeader(v));
		}
	}

	private static void assertSame(final String msg, final RowPermutation p, final RowGrouping grouping,
			final int[] groupKeys, final Set<Integer> collapsedKeys, final int[] values) {
		final Map<Integer, List<Integer>> reference = new LinkedHashMap<>();
		for (int v = 0; v < p.getViewCount(); v++) {
			final int m = p.modelRow(v);
			reference.computeIfAbsent(groupKeys[m], k -> new ArrayList<>()).add(m);
		}
		assertEquals(msg + " groups", reference.size(), grouping.getGroupCount());
		final List<Integer> display = new ArrayList<>();
		final List<Integer> displayGroup = new ArrayList<>();
		int g = 0;
		final long[] sum = new long[reference.size()];
		final int[] min = new int[reference.size()];
		final int[] max = new int[reference.size()];
		grouping.aggregate(values, sum, min, max);
		for (final Map.Entry<Integer, List<Integer>> e : reference.entrySet()) {
			assertEquals(msg, e.getKey().intValue(), grouping.getGroupKey(g));
			assertEquals(msg, e.getValue().size(), grouping.getGroupSize(g));
			assertEquals(msg, !collapsedKeys.contains(e.getKey()), grouping.isExpanded(g));
			assertEquals(msg + " header", display.size(), grouping.headerIndex(g));
			display.add(-1);
			displayGroup.add(g);
			long s = 0;
			int lo = Integer.MAX_VALUE;
			int hi = Integer.MIN_VALUE;
			for (final int m : e.getValue()) {
				s += values[m];
				lo = Math.min(lo, values[m]);
				hi = Math.max(hi, values[m]);
				if (!collapsedKeys.contains(e.getKey())) {
					display.add(m);
					displayGroup.add(g);
				}
			}
			assertEquals(msg + " sum", s, sum[g]);
			assertEquals(msg + " min", lo, min[g]);
			assertEquals(msg + " max", hi, max[g]);
			g++;
		}
		assertEquals(msg + " display count", display.size(), grouping.getViewCount());
		for (int d = 0; d < display.size(); d++) {
			assertEquals(msg + " d=" + d, display.get(d).intValue(), grouping.modelRow(d));
			assertEquals(msg + " header d=" + d, display.get(d) < 0, grouping.isGroupHeader(d));
			assertEquals(msg + " group d=" + d, displayGroup.get(d).intValue(), grouping.groupOf(d));
		}
		for (int m = 0; m < p.getModelCount(); m++) {
			final int expected = display.indexOf(m);
			assertEquals(msg + " m=" + m, expected, grouping.viewIndex(m));
		}
		assertTrue(grouping.getVersion() >= 0);
	}
}
