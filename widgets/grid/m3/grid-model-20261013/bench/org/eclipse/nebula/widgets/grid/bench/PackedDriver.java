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
import org.eclipse.nebula.widgets.grid.model.RowPermutation;

/** RowPermutation installed on the Grid (new API). */
public final class PackedDriver implements GridModelBench.Driver {
	private Grid grid;
	private RowPermutation permutation;

	@Override
	public void install(final Grid g, final int n) {
		grid = g;
		permutation = new RowPermutation(n);
		grid.setRowPermutation(permutation);
	}

	@Override
	public int modelRow(final int index) {
		return permutation.modelRow(index);
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
		permutation.clearSort();
		permutation.clearFilter();
		grid.refreshRowPermutation();
	}

	@Override
	public int group(final int[] keys, final int groups) {
		return -1;
	}
}
