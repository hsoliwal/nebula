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
package org.eclipse.nebula.widgets.grid;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

/**
 * CPU jobs of the Grid model with a batch threshold: small jobs run inline on the UI thread,
 * large ones run on a worker over an immutable snapshot and are applied on the UI thread. Only
 * paint touches the GC; sort, filter, grouping and pinning math never do.
 *
 * @since 1.2
 */
public final class GridModelJobs {

	/** Model rows at or above which a job leaves the UI thread. */
	public static final int BATCH_THRESHOLD = 1 << 17;

	private GridModelJobs() {
	}

	/**
	 * Sorts the Grid's installed {@link RowPermutation} by an {@code int} key column, ties in
	 * model order. Below {@link #BATCH_THRESHOLD} model rows it runs inline. Otherwise the keys
	 * are copied on the calling (UI) thread, the order is computed on {@code executor}
	 * ({@link RowPermutation#sortedOrder(int[], int, boolean)}) and applied with
	 * {@link RowPermutation#setOrder(int[])} and {@link Grid#refreshRowPermutation()} through
	 * {@link Display#asyncExec(Runnable)}. A result is dropped when the permutation changed in
	 * between (any newer sort, filter or size change wins) or the Grid was disposed.
	 *
	 * @param grid       a Grid with an installed permutation
	 * @param keys       one key per model row
	 * @param descending {@code true} for descending keys
	 * @param executor   worker for large jobs
	 * @return completes on the UI thread: {@code true} if applied, {@code false} if dropped
	 * @exception IllegalArgumentException
	 *                                     <ul>
	 *                                     <li>ERROR_INVALID_ARGUMENT - if no permutation
	 *                                     is installed</li>
	 *                                     </ul>
	 */
	public static CompletableFuture<Boolean> sortByIntKeys(final Grid grid, final int[] keys,
			final boolean descending, final Executor executor) {
		final RowPermutation permutation = grid.getRowPermutation();
		if (permutation == null) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		final int rows = permutation.getModelCount();
		if (rows < BATCH_THRESHOLD) {
			permutation.sortByIntKeys(keys, descending);
			grid.refreshRowPermutation();
			return CompletableFuture.completedFuture(Boolean.TRUE);
		}
		if (keys.length < rows) {
			throw new IllegalArgumentException("keys.length=" + keys.length + " < " + rows);
		}
		final int[] snapshot = java.util.Arrays.copyOf(keys, rows);
		final int version = permutation.getVersion();
		final Display display = grid.getDisplay();
		final CompletableFuture<Boolean> done = new CompletableFuture<>();
		executor.execute(() -> {
			final int[] order;
			try {
				order = RowPermutation.sortedOrder(snapshot, rows, descending);
			} catch (final RuntimeException failure) {
				done.completeExceptionally(failure);
				return;
			}
			display.asyncExec(() -> {
				if (grid.isDisposed() || grid.getRowPermutation() != permutation
						|| permutation.getVersion() != version) {
					done.complete(Boolean.FALSE);
					return;
				}
				permutation.setOrder(order);
				grid.refreshRowPermutation();
				done.complete(Boolean.TRUE);
			});
		});
		return done;
	}
}
