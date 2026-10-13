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
import org.eclipse.swt.custom.BusyIndicator;
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

	/** UI-thread budget of an inline job; a job predicted to take longer goes to a worker. */
	public static final long INLINE_BUDGET_NANOS = 100_000_000L;

	/** Measured sort cost per model row (0 = not measured yet). */
	private static volatile long sortNanosPerRow;

	private GridModelJobs() {
	}

	/**
	 * Sorts the installed {@link RowPermutation} by an {@code int} key column and returns when it
	 * is applied. A sort predicted (from the measured cost per row, or {@link #BATCH_THRESHOLD}
	 * before the first measurement) to exceed {@link #INLINE_BUDGET_NANOS} runs on
	 * {@code executor} over a key snapshot while the UI thread waits in
	 * {@link BusyIndicator#showWhile(java.util.concurrent.Future)}: busy cursor, event loop and
	 * scroll bars live, the body paints the old view until the new order is applied once.
	 *
	 * @param grid       a Grid with an installed permutation
	 * @param keys       one key per model row
	 * @param descending {@code true} for descending keys
	 * @param executor   worker for long sorts
	 * @return {@code true} if applied, {@code false} if the permutation changed meanwhile
	 */
	public static boolean sortByIntKeysBusy(final Grid grid, final int[] keys, final boolean descending,
			final Executor executor) {
		final RowPermutation permutation = grid.getRowPermutation();
		if (permutation == null) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		final int rows = permutation.getModelCount();
		final long perRow = sortNanosPerRow;
		final boolean inline = perRow == 0L ? rows < BATCH_THRESHOLD : rows * perRow < INLINE_BUDGET_NANOS;
		final long t0 = System.nanoTime();
		if (inline) {
			permutation.sortByIntKeys(keys, descending);
			grid.refreshRowPermutation();
			measured(rows, System.nanoTime() - t0);
			return true;
		}
		if (keys.length < rows) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		final int[] snapshot = java.util.Arrays.copyOf(keys, rows);
		final int version = permutation.getVersion();
		final CompletableFuture<int[]> order = CompletableFuture
				.supplyAsync(() -> RowPermutation.sortedOrder(snapshot, rows, descending), executor);
		BusyIndicator.showWhile(order);
		final int[] result = order.join();
		measured(rows, System.nanoTime() - t0);
		if (grid.isDisposed() || grid.getRowPermutation() != permutation || permutation.getVersion() != version) {
			return false;
		}
		permutation.setOrder(result);
		grid.refreshRowPermutation();
		return true;
	}

	private static void measured(final int rows, final long nanos) {
		if (rows >= 4096) {
			sortNanosPerRow = Math.max(1L, nanos / rows);
		}
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
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
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
