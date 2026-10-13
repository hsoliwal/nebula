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
import static org.junit.Assert.assertTrue;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

/** {@link RowSelection} against BitSet and map references, mapped through views and filters. */
public class RowSelection_Test {

	@Test
	public void rowAndCellSelectionMatchReferences() {
		final Random random = new Random(17L);
		final int n = 200_000;
		final RowSelection selection = new RowSelection(n);
		final BitSet ref = new BitSet();
		final Map<Long, Boolean> cells = new HashMap<>();
		for (int op = 0; op < 5000; op++) {
			final int a = random.nextInt(n);
			switch (random.nextInt(6)) {
			case 0:
				selection.selectAll();
				ref.set(0, n);
				break;
			case 1: {
				final int b = Math.min(n, a + random.nextInt(50_000));
				final boolean on = random.nextBoolean();
				selection.setSelected(a, b, on);
				ref.set(a, b, on);
				break;
			}
			case 2: {
				final int col = random.nextInt(100);
				final boolean on = random.nextBoolean();
				selection.setCellSelected(a % 500, col, on);
				cells.put(((long) (a % 500) << 32) | col, on);
				break;
			}
			default: {
				final boolean on = random.nextBoolean();
				selection.setSelected(a, on);
				ref.set(a, on);
				break;
			}
			}
		}
		assertEquals(ref.cardinality(), selection.getSelectionCount());
		for (int probe = 0; probe < 20_000; probe++) {
			final int r = random.nextInt(n);
			assertEquals(ref.get(r), selection.isSelected(r));
		}
		int selectedCells = 0;
		for (final Map.Entry<Long, Boolean> e : cells.entrySet()) {
			final int row = (int) (e.getKey() >>> 32);
			final int col = (int) (e.getKey() & 0xffffffffL);
			assertEquals(e.getValue(), selection.isCellSelected(row, col));
			selectedCells += e.getValue() ? 1 : 0;
		}
		assertEquals(selectedCells, selection.getCellCount());
		final int first = ref.nextSetBit(0);
		assertEquals(first, selection.nextSelected(0));
	}

	@Test
	public void viewIndicesAndFilterAwareCountsFollowTheView() {
		final int n = 5000;
		final RowSelection selection = new RowSelection(n);
		final RowPermutation p = new RowPermutation(n);
		final int[] keys = new int[n];
		for (int m = 0; m < n; m++) {
			keys[m] = (m * 31) % 977;
		}
		selection.setSelected(100, 900, true);
		selection.setSelected(4000, true);
		p.sortByIntKeys(keys, true);
		p.setFilter(m -> m % 3 != 0);
		final long[] displayed = p.displayedBits(new long[(n + 63) / 64]);
		int expectedDisplayed = 0;
		final java.util.List<Integer> views = new java.util.ArrayList<>();
		for (int m = 0; m < n; m++) {
			if (selection.isSelected(m) && p.passes(m)) {
				expectedDisplayed++;
				views.add(p.viewIndex(m));
			}
		}
		views.sort(null);
		assertEquals(expectedDisplayed, selection.getSelectionCount(displayed));
		assertArrayEquals(views.stream().mapToInt(Integer::intValue).toArray(), selection.toViewIndices(p));
		selection.setModelCount(500);
		assertEquals(400, selection.getSelectionCount());
		assertTrue(selection.nextSelected(500) < 0);
	}
}
