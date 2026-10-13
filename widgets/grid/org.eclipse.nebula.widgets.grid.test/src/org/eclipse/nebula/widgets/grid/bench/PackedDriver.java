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

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridModelJobs;
import org.eclipse.nebula.widgets.grid.model.RowGrouping;
import org.eclipse.nebula.widgets.grid.model.RowPermutation;

/** RowPermutation installed on the Grid (new API). */
public final class PackedDriver implements GridModelBench.Driver {
	private Grid grid;
	private RowPermutation permutation;
	private RowGrouping grouping;

	@Override
	public void install(final Grid g, final int n) {
		grid = g;
		permutation = new RowPermutation(n);
		grid.setRowPermutation(permutation);
	}

	@Override
	public int modelRow(final int index) {
		return grouping != null ? grouping.modelRow(index) : permutation.modelRow(index);
	}

	@Override
	public void sort(final int[] keys, final boolean descending) {
		permutation.sortByIntKeys(keys, descending);
		grid.refreshRowPermutation();
	}

	@Override
	public void filter(final int[] keys, final int modulus) {
		permutation.setFilter(m -> keys[m] % modulus != 0);
		grid.refreshRowPermutation();
	}

	@Override
	public void clear() {
		if (grouping != null) {
			grouping = null;
			grid.setRowGrouping(null);
		}
		permutation.clearSort();
		permutation.clearFilter();
		grid.refreshRowPermutation();
	}

	@Override
	public boolean busySort(final Grid g, final int[] keys, final boolean descending) {
		return GridModelJobs.sortByIntKeysBusy(g, keys, descending, java.util.concurrent.ForkJoinPool.commonPool());
	}

	@Override
	public int group(final int[] keys, final int groups) {
		final int[] groupKeys = new int[keys.length];
		for (int m = 0; m < keys.length; m++) {
			groupKeys[m] = keys[m] % groups;
		}
		grouping = new RowGrouping(permutation);
		grouping.groupBy(groupKeys);
		grid.setRowGrouping(grouping);
		return grouping.getGroupCount();
	}
}
