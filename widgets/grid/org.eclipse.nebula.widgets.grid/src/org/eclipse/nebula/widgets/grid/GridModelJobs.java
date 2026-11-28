/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.SWTException;

/**
 * CPU jobs of the Grid model with a batch threshold: small jobs run inline on the UI thread,
 * large ones run on a worker over an immutable snapshot and are applied on the UI thread. Only
 * paint touches the GC; sort, filter, grouping and pinning math never do.
 *
 * @since 1.3
 */
public final class GridModelJobs {

	/** Model rows at or above which a job leaves the UI thread. */
	public static final int BATCH_THRESHOLD = 1 << 17;

	/** UI-thread budget of an inline job; a job predicted to take longer goes to a worker. */
	public static final long INLINE_BUDGET_NANOS = 100_000_000L;

	/** Measured sort cost per model row (0 = not measured yet). */
	private static volatile long sortNanosPerRow;

	/* UI-thread only: an outstanding worker result may not supersede a later sort
	 * request, even when both captured the same RowPermutation version. Weak keys
	 * avoid retaining Grids once their applications release them. */
	private static final Map<Grid, Object> latestSort = java.util.Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * Immutable, detached primitive key snapshot. Call {@link #copyOf(int[], int)}
	 * on a CPU thread before posting the viewport command to SWT to avoid a
	 * model-sized copy in the GUI event callback. The internal array is never
	 * returned to clients or mutated by the sort worker.
	 */
	public static final class IntKeySnapshot {
		private final int[] keys;

		private IntKeySnapshot(int[] ownedCopy) {
			keys = ownedCopy;
		}

		/**
		 * Copies the first {@code rows} keys into a detached immutable snapshot.
		 *
		 * @param source the keys to copy
		 * @param rows the number of keys to include
		 * @return a snapshot that owns its copy of the keys
		 * @throws NullPointerException if {@code source} is {@code null}
		 * @throws IndexOutOfBoundsException if {@code rows} is negative or exceeds the source length
		 */
		public static IntKeySnapshot copyOf(int[] source, int rows) {
			Objects.requireNonNull(source, "source");
			if (rows < 0 || rows > source.length) throw new IndexOutOfBoundsException("rows");
			return new IntKeySnapshot(java.util.Arrays.copyOf(source, rows));
		}

		/**
		 * Returns the number of keys in this snapshot.
		 *
		 * @return the number of copied keys
		 */
		public int size() {
			return keys.length;
		}
	}

	/*
	 * Opt-in latest-only CPU sort lane. A weak Grid key and weak Grid reference
	 * prevent the scheduler map from making otherwise abandoned widgets immortal.
	 * Grid and RowPermutation are inspected or mutated only by the SWT UI thread.
	 */
	private static final Map<Grid, CpuSortLane> cpuSortLanes = java.util.Collections.synchronizedMap(new WeakHashMap<>());

	private static final class CpuSortJob {
		final RowPermutation permutation;
		final int version;
		final int[] keys;
		final int rows;
		final boolean descending;
		final Object token = new Object();
		final CompletableFuture<Boolean> future = new CompletableFuture<>();

		CpuSortJob(RowPermutation permutation, int[] keys, boolean descending) {
			this.permutation = permutation;
			this.version = permutation.getVersion();
			this.rows = permutation.getModelCount();
			this.keys = keys;
			this.descending = descending;
		}
	}

	private static final class CpuSortResult {
		final CpuSortJob job;
		final int[] order;
		final Throwable failure;
		final boolean cancelled;

		CpuSortResult(CpuSortJob job, int[] order, Throwable failure, boolean cancelled) {
			this.job = job;
			this.order = order;
			this.failure = failure;
			this.cancelled = cancelled;
		}
	}

	private static final class CpuSortLane {
		private final WeakReference<Grid> owner;
		private final Display display;
		private final Thread uiThread;
		private final Listener onDispose = this::disposed;
		private CpuSortJob queued, active;
		private CpuSortResult ready;
		private boolean working, uiQueued;
		private volatile boolean closed;

		CpuSortLane(Grid grid) {
			owner = new WeakReference<>(grid);
			display = grid.getDisplay();
			uiThread = display.getThread();
			grid.addListener(SWT.Dispose, onDispose);
		}

		/**
		 * Called on the SWT thread; rejection does not invalidate previously
		 * accepted work. A worker already active drains only the newest queued job.
		 */
		void submit(CpuSortJob next, Executor executor, Grid grid) {
			CpuSortJob oldQueued, oldActive, oldReady;
			synchronized (this) {
				if (closed) throw new IllegalStateException("disposed Grid CPU sort lane");
				oldQueued = queued;
				oldActive = active;
				oldReady = ready == null ? null : ready.job;
				queued = next;
				if (!working) {
					working = true;
					try {
						executor.execute(this::drain);
					} catch (RuntimeException error) {
						queued = oldQueued;
						working = false;
						throw error;
					}
				}
			}
			// Only after successful admission may earlier accepted jobs be retired.
			latestSort.put(grid, next.token);
			if (oldQueued != null) oldQueued.future.complete(Boolean.FALSE);
			if (oldActive != null) oldActive.future.complete(Boolean.FALSE);
			if (oldReady != null) oldReady.future.complete(Boolean.FALSE);
		}

		private void drain() {
			if (Thread.currentThread() == uiThread) {
				throw new IllegalArgumentException("CPU sort executor ran on SWT event thread");
			}
			for (;;) {
				final CpuSortJob job;
				synchronized (this) {
					if (closed || queued == null) {
						working = false;
						active = null;
						return;
					}
					job = queued;
					queued = null;
					active = job;
				}
				if (job.future.isDone()) continue;
				try {
					int[] order = RowPermutation.sortedOrder(job.keys, job.rows,
							job.descending, () -> closed || job.future.isDone()
								|| Thread.currentThread().isInterrupted());
					if (!closed && !job.future.isDone())
						publish(new CpuSortResult(job, order, null, false));
				} catch (CancellationException cancelled) {
					if (!closed && !job.future.isDone())
						publish(new CpuSortResult(job, null, null, true));
				} catch (RuntimeException | Error failure) {
					if (!closed && !job.future.isDone())
						publish(new CpuSortResult(job, null, failure, false));
				}
			}
		}

		private void publish(CpuSortResult completed) {
			boolean post;
			synchronized (this) {
				if (closed) return;
				ready = completed;
				post = !uiQueued;
				if (post) uiQueued = true;
			}
			if (post) postUi();
		}

		private void postUi() {
			try {
				display.asyncExec(this::apply);
			} catch (SWTException disposed) {
				CpuSortResult lost;
				synchronized (this) {
					lost = ready;
					ready = null;
					uiQueued = false;
				}
				if (lost != null) lost.job.future.complete(Boolean.FALSE);
			}
		}

		private void apply() {
			CpuSortResult completed;
			synchronized (this) {
				completed = ready;
				ready = null;
			}
			try {
				if (completed == null || completed.job.future.isDone()) return;
				CpuSortJob job = completed.job;
				Grid grid = owner.get();
				if (closed || grid == null || grid.isDisposed()
						|| latestSort.get(grid) != job.token
						|| grid.getRowPermutation() != job.permutation
						|| job.permutation.getVersion() != job.version
						|| completed.cancelled) {
					job.future.complete(Boolean.FALSE);
					return;
				}
				if (completed.failure != null) {
					job.future.completeExceptionally(completed.failure);
					return;
				}
				try {
					job.permutation.setOrder(completed.order);
					grid.refreshRowPermutation();
					job.future.complete(Boolean.TRUE);
				} catch (RuntimeException | Error error) {
					job.future.completeExceptionally(error);
				}
			} finally {
				Grid grid = owner.get();
				if (grid != null && !grid.isDisposed() && completed != null
						&& latestSort.get(grid) == completed.job.token) {
					latestSort.remove(grid);
				}
				boolean repost;
				synchronized (this) {
					uiQueued = false;
					repost = ready != null && !closed;
					if (repost) uiQueued = true;
				}
				if (repost) postUi();
			}
		}

		private void disposed(Event event) {
			closed = true;
			Grid grid = (Grid) event.widget;
			cpuSortLanes.remove(grid);
			latestSort.remove(grid);
			CpuSortJob waiting, computing;
			CpuSortResult published;
			synchronized (this) {
				waiting = queued;
				computing = active;
				published = ready;
				queued = active = null;
				ready = null;
			}
			if (waiting != null) waiting.future.complete(Boolean.FALSE);
			if (computing != null) computing.future.complete(Boolean.FALSE);
			if (published != null) published.job.future.complete(Boolean.FALSE);
		}
	}

	/**
	 * Asynchronous CPU-only Grid sort at every model size. Unlike the legacy
	 * thresholded method, this method never sorts on the SWT event thread.
	 * One worker drain per Grid coalesces pending requests; older requests
	 * complete false, and only the latest matching model/version is applied.
	 * A rejected executor leaves the previous accepted request untouched.
	 *
	 * <p>Provide a genuine background `Executor`, not `Runnable::run`.
	 * The executor and RowPermutation sorted-order computation must not access
	 * any SWT widgets. Results are installed solely through Display.asyncExec.
	 * This method is additive; original sortByIntKeys behavior is unchanged.</p>
	 *
	 * @since 1.3
	 */
	public static CompletableFuture<Boolean> sortByIntKeysCpuLatest(
			final Grid grid, final int[] keys, final boolean descending,
			final Executor executor) {
		Objects.requireNonNull(grid, "grid");
		Objects.requireNonNull(keys, "keys");
		final RowPermutation permutation = grid.getRowPermutation();
		if (permutation == null) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		final int rows = permutation.getModelCount();
		if (keys.length < rows) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		return sortByIntKeysCpuLatest(grid, IntKeySnapshot.copyOf(keys, rows), descending, executor);
	}

	/**
	 * Zero GUI-side key-copy path. Prepare the {@link IntKeySnapshot} on a CPU
	 * worker, then submit on SWT's UI thread. No SWT object may be captured by
	 * the worker preparing the key snapshot; this overload only validates model
	 * identity and enqueues the existing latest-sort CPU lane.
	 */
	public static CompletableFuture<Boolean> sortByIntKeysCpuLatest(
			final Grid grid, final IntKeySnapshot keys, final boolean descending,
			final Executor executor) {
		Objects.requireNonNull(grid, "grid");
		Objects.requireNonNull(keys, "keys");
		Objects.requireNonNull(executor, "executor");
		final RowPermutation permutation = grid.getRowPermutation();
		if (permutation == null) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		final int rows = permutation.getModelCount();
		if (keys.size() < rows) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		final CpuSortJob job = new CpuSortJob(permutation, keys.keys, descending);
		CpuSortLane lane = cpuSortLanes.get(grid);
		if (lane == null) {
			lane = new CpuSortLane(grid);
			cpuSortLanes.put(grid, lane);
		}
		lane.submit(job, executor, grid);
		return job.future;
	}

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
		final boolean inline = perRow == 0L ? rows < BATCH_THRESHOLD
				: rows == 0 || (perRow > 0 && perRow < INLINE_BUDGET_NANOS / rows);
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
		/* BusyIndicator runs a nested SWT event loop while the CPU job completes.
		 * Publish this request to the same latest-sort owner as the asynchronous
		 * entry points: a newer request may be admitted before this sort returns,
		 * without having changed the permutation version yet. */
		final Object request = new Object();
		latestSort.put(grid, request);
		BusyIndicator.showWhile(order);
		final int[] result = order.join();
		measured(rows, System.nanoTime() - t0);
		if (grid.isDisposed() || grid.getRowPermutation() != permutation
				|| permutation.getVersion() != version || latestSort.get(grid) != request) {
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
		final Object request = new Object();
		final Object previous = latestSort.put(grid, request);
		final Listener onDispose = e -> done.complete(Boolean.FALSE);
		grid.addListener(SWT.Dispose, onDispose);
		try {
			executor.execute(() -> {
				final int[] order;
				try {
					order = RowPermutation.sortedOrder(snapshot, rows, descending);
				} catch (final RuntimeException | Error failure) {
					finishWorkerFailure(display, done, failure, () -> {
						if (!grid.isDisposed()) grid.removeListener(SWT.Dispose, onDispose);
					});
					return;
				}
				try {
					display.asyncExec(() -> {
						try {
							if (done.isDone() || grid.isDisposed()
									|| grid.getRowPermutation() != permutation
									|| permutation.getVersion() != version
									|| latestSort.get(grid) != request) {
								done.complete(Boolean.FALSE);
								return;
							}
							permutation.setOrder(order);
							grid.refreshRowPermutation();
							done.complete(Boolean.TRUE);
						} catch (final RuntimeException | Error failure) {
							done.completeExceptionally(failure);
							throw failure;
						} finally {
							if (!grid.isDisposed()) grid.removeListener(SWT.Dispose, onDispose);
						}
					});
				} catch (final org.eclipse.swt.SWTException disposed) {
					done.complete(Boolean.FALSE);
				}
			});
		} catch (final RuntimeException rejected) {
			grid.removeListener(SWT.Dispose, onDispose);
			if (latestSort.get(grid) == request) {
				if (previous == null) latestSort.remove(grid);
				else latestSort.put(grid, previous);
			}
			throw rejected;
		}
		return done;
	}

	/*
	 * Future callbacks may update SWT widgets. CompletableFuture invokes
	 * non-async continuations on its completing thread, so even a failed CPU
	 * sort must complete on the Display thread while that thread is live.
	 */
	private static void finishWorkerFailure(final Display display,
			final CompletableFuture<Boolean> done, final Throwable failure,
			final Runnable onUiCleanup) {
		try {
			display.asyncExec(() -> {
				try {
					done.completeExceptionally(failure);
				} finally {
					onUiCleanup.run();
				}
			});
		} catch (final SWTException disposed) {
			// The Display cannot deliver another UI callback after disposal.
			done.complete(Boolean.FALSE);
		}
	}
}
