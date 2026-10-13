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

import java.util.ArrayList;
import java.util.List;

/**
 * Seeded workload over the Grid model package, run in the JVM and as GWT JavaScript: sorts
 * (radix and comparator), filters, pinned rows, grouping with aggregates, dense and sparse lanes,
 * patterns and packed selection. Each seed prints one digest line; the gate requires identical
 * lines in both engines. Digests fold through masked {@code long} arithmetic (exact in both).
 */
final class GateWorkload {

	private static final long MASK = 0xffffffffffffL;

	private GateWorkload() {
	}

	static List<String> run() {
		final List<String> lines = new ArrayList<>();
		for (int seed = 1; seed <= 8; seed++) {
			lines.add("seed " + seed + " " + Long.toHexString(workload(seed)));
		}
		return lines;
	}

	private static long workload(final int seed) {
		final long[] rng = { seed * 0x5DEECE66DL & MASK };
		final int n = 1000 + next(rng, 4000);
		final int[] keys = new int[n];
		final int[] values = new int[n];
		final int[] groups = new int[n];
		for (int m = 0; m < n; m++) {
			keys[m] = next(rng, 1 << 30) - (1 << 29);
			values[m] = next(rng, 2001) - 1000;
			groups[m] = next(rng, 17);
		}
		long d = 17;
		final RowPermutation p = new RowPermutation(n);
		p.sortByIntKeys(keys, (seed & 1) == 0);
		d = fold(d, p);
		final int[] sorted = RowPermutation.sortedOrder(keys, n, true);
		for (final int m : sorted) {
			d = fold(d, m);
		}
		p.sort((a, b) -> Integer.compare(values[a], values[b]));
		d = fold(d, p);
		p.setFilter(m -> (keys[m] & 3) != 0);
		d = fold(d, p);
		p.setPinnedRows(new int[] { n - 1, 0 }, new int[] { n / 2 });
		d = fold(d, p);
		for (int i = 0; i < 50; i++) {
			d = fold(d, p.viewIndex(next(rng, n)));
		}
		final RowGrouping g = new RowGrouping(p);
		g.groupBy(groups);
		g.setExpanded(1, false);
		g.setExpanded(3, 6, false);
		d = fold(d, g.getViewCount());
		for (int v = 0; v < g.getViewCount(); v++) {
			d = fold(d, g.modelRow(v));
		}
		final long[] sum = new long[g.getGroupCount()];
		final int[] min = new int[g.getGroupCount()];
		final int[] max = new int[g.getGroupCount()];
		g.aggregate(values, sum, min, max);
		for (int i = 0; i < sum.length; i++) {
			d = fold(fold(fold(d, (int) (sum[i] & 0x7fffffff)), min[i]), max[i]);
		}
		final RowBitLanes lanes = new RowBitLanes(2);
		lanes.setRowCount(n);
		lanes.setRange(0, n / 3, n / 2, true);
		lanes.setWhere(1, p.displayedBits(new long[(n + 63) >>> 6]), true);
		lanes.insertRows(next(rng, n), 70);
		lanes.removeRows(next(rng, n - 100), 30);
		d = fold(fold(fold(d, lanes.cardinality(0)), lanes.cardinality(1)), lanes.nextSetRow(1, n / 4));
		final RowStateLanes state = new RowStateLanes(1);
		state.setCount(1000000000);
		state.setPattern(0, 0, 1000000000, 3, 1);
		state.set(0, 6, false);
		state.setRange(0, 10, 20, true);
		d = fold(fold(d, state.countRange(0, 0, 1000000000)), state.nextSet(0, 5));
		final RowSelection selection = new RowSelection(n);
		selection.setSelected(10, n / 2, true);
		selection.setSelected(next(rng, n), false);
		selection.setCellSelected(7, 40, true);
		d = fold(fold(d, selection.getSelectionCount(p.displayedBits(new long[(n + 63) >>> 6]))),
				selection.getCellCount());
		for (final int v : selection.toViewIndices(p)) {
			d = fold(d, v);
		}
		return d;
	}

	private static long fold(long d, final RowView view) {
		d = fold(d, view.getViewCount());
		for (int v = 0; v < view.getViewCount(); v++) {
			d = fold(d, view.modelRow(v));
		}
		return d;
	}

	private static long fold(final long d, final int x) {
		return (d * 31 + (x & 0xffffffffL)) & MASK;
	}

	/** 48-bit LCG (the java.util.Random constants) in masked long arithmetic. */
	private static int next(final long[] state, final int bound) {
		state[0] = (state[0] * 0x5DEECE66DL + 0xBL) & MASK;
		return (int) ((state[0] >>> 17) % bound);
	}
}
