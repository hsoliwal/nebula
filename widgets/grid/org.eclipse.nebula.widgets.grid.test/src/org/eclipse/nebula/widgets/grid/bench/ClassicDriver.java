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
package org.eclipse.nebula.widgets.grid.bench;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.nebula.widgets.grid.Grid;

/** Application-side index + setItemCount + clearAll: what a caller does with the unmodified Grid. */
public final class ClassicDriver implements GridModelBench.Driver {
	private Grid grid;
	private int[] order;
	private int[] view;
	private int rows;

	@Override
	public void install(final Grid g, final int n) {
		grid = g;
		rows = n;
		order = identity(n);
		view = order;
	}

	@Override
	public int modelRow(final int index) {
		return view[index];
	}

	@Override
	public void sort(final int[] keys, final boolean descending) {
		final Integer[] boxed = new Integer[rows];
		for (int i = 0; i < rows; i++) {
			boxed[i] = i;
		}
		Arrays.sort(boxed, descending ? (a, b) -> Integer.compare(keys[b], keys[a]) : (a, b) -> Integer.compare(keys[a], keys[b]));
		order = new int[rows];
		for (int i = 0; i < rows; i++) {
			order[i] = boxed[i];
		}
		apply(order);
	}

	@Override
	public void filter(final int[] keys, final int modulus) {
		apply(Arrays.stream(order).filter(m -> keys[m] % modulus != 0).toArray());
	}

	@Override
	public void clear() {
		order = identity(rows);
		apply(order);
	}

	@Override
	public int group(final int[] keys, final int groups) {
		final Map<Integer, int[]> buckets = new HashMap<>();
		final int[] firstSeen = new int[groups];
		Arrays.fill(firstSeen, -1);
		int seen = 0;
		final int[][] lists = new int[groups][];
		final int[] sizes = new int[groups];
		for (final int m : view) {
			final int g = keys[m] % groups;
			if (firstSeen[g] < 0) {
				firstSeen[g] = seen++;
				lists[g] = new int[16];
				buckets.put(g, lists[g]);
			}
			if (sizes[g] == lists[g].length) {
				lists[g] = Arrays.copyOf(lists[g], sizes[g] * 2);
			}
			lists[g][sizes[g]++] = m;
		}
		final Integer[] groupOrder = new Integer[seen];
		for (int g = 0; g < groups; g++) {
			if (firstSeen[g] >= 0) {
				groupOrder[firstSeen[g]] = g;
			}
		}
		// one header row (code ~g) per group, then its rows
		final int[] grouped = new int[view.length + seen];
		int k = 0;
		for (final Integer g : groupOrder) {
			grouped[k++] = ~g;
			System.arraycopy(lists[g], 0, grouped, k, sizes[g]);
			k += sizes[g];
		}
		apply(grouped);
		return seen;
	}

	@Override
	public void checkAll(final Grid g) {
		// the unmodified Grid API: one item per row
		for (int i = 0; i < g.getItemCount(); i++) {
			g.getItem(i).setChecked(true);
		}
	}

	private void apply(final int[] newView) {
		view = newView;
		grid.setItemCount(newView.length);
		grid.clearAll(false);
	}

	static int[] identity(final int n) {
		final int[] a = new int[n];
		for (int i = 0; i < n; i++) {
			a[i] = i;
		}
		return a;
	}
}
