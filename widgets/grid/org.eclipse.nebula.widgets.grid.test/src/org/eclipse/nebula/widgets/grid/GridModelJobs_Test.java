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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CompletableFuture;

import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Batch threshold: inline below it, worker + UI-thread apply above it, stale results dropped. */
public class GridModelJobs_Test {

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void smallSortRunsInline() {
		final Grid grid = grid(1000);
		final int[] keys = keys(1000);
		final CompletableFuture<Boolean> done = GridModelJobs.sortByIntKeys(grid, keys, true, r -> {
			throw new AssertionError("no worker below the threshold");
		});
		assertTrue(done.isDone());
		assertTrue((done.join()).booleanValue());
		assertArrayEquals(RowPermutation.sortedOrder(keys, 1000, true), view(grid.getRowPermutation()));
	}

	@Test
	public void largeSortRunsOnTheWorkerAndAppliesOnTheUiThread() {
		final int rows = GridModelJobs.BATCH_THRESHOLD + 7;
		final Grid grid = grid(rows);
		final int[] keys = keys(rows);
		final List<Runnable> worker = new ArrayList<>();
		final CompletableFuture<Boolean> done = GridModelJobs.sortByIntKeys(grid, keys, false, worker::add);
		assertEquals(1, worker.size());
		assertFalse("nothing applied before the worker ran", grid.getRowPermutation().isSorted());
		keys[0] = Integer.MAX_VALUE; // the job sorts its snapshot, not the caller's array
		final int[] expected = RowPermutation.sortedOrder(java.util.Arrays.copyOf(keys(rows), rows), rows, false);
		new Thread(worker.get(0)).start();
		while (!done.isDone()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		assertTrue((done.join()).booleanValue());
		assertArrayEquals(expected, view(grid.getRowPermutation()));
		assertEquals(rows, grid.getItemCount());
	}

	@Test
	public void resultForAChangedPermutationIsDropped() {
		final int rows = GridModelJobs.BATCH_THRESHOLD;
		final Grid grid = grid(rows);
		final List<Runnable> worker = new ArrayList<>();
		final CompletableFuture<Boolean> done = GridModelJobs.sortByIntKeys(grid, keys(rows), false, worker::add);
		grid.getRowPermutation().setFilter(m -> (m & 1) == 0);
		grid.refreshRowPermutation();
		worker.get(0).run();
		while (!done.isDone()) {
			display.readAndDispatch();
		}
		assertFalse((done.join()).booleanValue());
		assertFalse(grid.getRowPermutation().isSorted());
		assertEquals(rows / 2, grid.getItemCount());
	}

	@Test
	public void newerLargeSortWinsEvenWhenOlderWorkerFinishesFirst() {
		final int rows = GridModelJobs.BATCH_THRESHOLD;
		final Grid grid = grid(rows);
		final List<Runnable> workers = new ArrayList<>();
		final int[] keys = keys(rows);
		final CompletableFuture<Boolean> old = GridModelJobs.sortByIntKeys(grid, keys, false, workers::add);
		final CompletableFuture<Boolean> newer = GridModelJobs.sortByIntKeys(grid, keys, true, workers::add);
		assertEquals(2, workers.size());
		workers.get(0).run();
		workers.get(1).run();
		final long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
		while ((!old.isDone() || !newer.isDone()) && System.nanoTime() < deadline) {
			display.readAndDispatch();
		}
		assertTrue("Both queued results must complete", old.isDone() && newer.isDone());
		assertFalse("The superseded result must not commit", old.join());
		assertTrue("The last submitted sort must commit", newer.join());
		assertArrayEquals(RowPermutation.sortedOrder(keys, rows, true), view(grid.getRowPermutation()));
	}

	@Test
	public void cancelledLargeSortNeverMutatesTheGrid() {
		final int rows = GridModelJobs.BATCH_THRESHOLD;
		final Grid grid = grid(rows);
		final List<Runnable> workers = new ArrayList<>();
		final CompletableFuture<Boolean> done = GridModelJobs.sortByIntKeys(grid, keys(rows), false, workers::add);
		assertTrue(done.cancel(false));
		workers.get(0).run();
		display.readAndDispatch();
		assertFalse(grid.getRowPermutation().isSorted());
	}

	@Test
	public void disposingGridCompletesPendingLargeSortWithoutWaitingForWorker() {
		final Grid grid = grid(GridModelJobs.BATCH_THRESHOLD);
		final List<Runnable> workers = new ArrayList<>();
		final CompletableFuture<Boolean> done = GridModelJobs.sortByIntKeys(grid,
				keys(GridModelJobs.BATCH_THRESHOLD), false, workers::add);
		grid.dispose();
		assertTrue(done.isDone());
		assertFalse(done.join());
	}

	@Test
	public void busySortCannotOverrideNewerRequestAdmittedDuringNestedUiPump() throws Exception {
		/* Force the old busy method's worker branch regardless of measurements
		 * left by other tests. Its nested SWT event loop must not let an older
		 * order supersede either kind of newer asynchronous request. */
		final java.lang.reflect.Field cost = GridModelJobs.class.getDeclaredField("sortNanosPerRow"); //$NON-NLS-1$
		cost.setAccessible(true);
		final long originalCost = cost.getLong(null);
		try {
			for (boolean newestCpuLane : new boolean[] { false, true }) {
				cost.setLong(null, GridModelJobs.INLINE_BUDGET_NANOS);
				final int rows = GridModelJobs.BATCH_THRESHOLD;
				final Grid grid = grid(rows);
				final int[] keys = keys(rows);
				final CountDownLatch allowOldWorker = new CountDownLatch(1);
				final List<Runnable> workers = new ArrayList<>();
				final AtomicReference<CompletableFuture<Boolean>> newest = new AtomicReference<>();
				try {
					display.asyncExec(() -> {
						newest.set(newestCpuLane
								? GridModelJobs.sortByIntKeysCpuLatest(grid, keys, true, workers::add)
								: GridModelJobs.sortByIntKeys(grid, keys, true, workers::add));
						allowOldWorker.countDown();
					});
					final boolean olderApplied = GridModelJobs.sortByIntKeysBusy(grid, keys, false, task -> {
						final Thread worker = new Thread(() -> {
							try {
								if (!allowOldWorker.await(5, TimeUnit.SECONDS)) {
									throw new IllegalStateException("SWT busy event loop did not dispatch newer sort"); //$NON-NLS-1$
								}
								task.run();
							} catch (InterruptedException failure) {
								Thread.currentThread().interrupt();
								throw new IllegalStateException(failure);
							}
						}, "nebula-older-busy-sort"); //$NON-NLS-1$
						worker.setDaemon(true);
						worker.start();
					});
					assertFalse("newer request must invalidate pending busy result", olderApplied); //$NON-NLS-1$
					assertFalse("older result must not mutate permutation", grid.getRowPermutation().isSorted()); //$NON-NLS-1$
					assertEquals(1, workers.size());
					final Thread worker = new Thread(workers.get(0), "nebula-newer-sort"); //$NON-NLS-1$
					worker.setDaemon(true);
					worker.start();
					pumpUntil(newest.get());
					worker.join(2_000);
					assertTrue("latest sort must apply", newest.get().join()); //$NON-NLS-1$
					assertArrayEquals(RowPermutation.sortedOrder(keys, rows, true), view(grid.getRowPermutation()));
				} finally {
					allowOldWorker.countDown();
				}
			}
		} finally {
			cost.setLong(null, originalCost);
		}
	}

	@Test
	public void workerFailureCompletesFutureAndRunsCleanupOnUiThread() throws Exception {
		/* CompletableFuture's dependent callbacks run inline with completion.
		 * The failure path must be a Display handoff just like success, even
		 * for an Error thrown by the detached CPU computation. */
		final java.lang.reflect.Method handoff = GridModelJobs.class.getDeclaredMethod(
				"finishWorkerFailure", Display.class, CompletableFuture.class, //$NON-NLS-1$
				Throwable.class, Runnable.class);
		handoff.setAccessible(true);
		for (Throwable failure : new Throwable[] {
				new IllegalStateException("worker failure"), new AssertionError("worker error") }) { //$NON-NLS-1$ //$NON-NLS-2$
			final CompletableFuture<Boolean> done = new CompletableFuture<>();
			final AtomicReference<Thread> completedOn = new AtomicReference<>();
			final AtomicReference<Thread> cleanedOn = new AtomicReference<>();
			final AtomicReference<Throwable> invocationFailure = new AtomicReference<>();
			done.whenComplete((value, error) -> completedOn.set(Thread.currentThread()));
			final Thread worker = new Thread(() -> {
				try {
					handoff.invoke(null, display, done, failure,
							(Runnable) () -> cleanedOn.set(Thread.currentThread()));
				} catch (ReflectiveOperationException error) {
					invocationFailure.set(error);
				}
			}, "nebula-failed-cpu-sort"); //$NON-NLS-1$
			worker.setDaemon(true);
			worker.start();
			worker.join(2_000);
			assertFalse("failure handoff may not finish on a worker", done.isDone()); //$NON-NLS-1$
			org.junit.Assert.assertNull(invocationFailure.get());
			pumpUntil(done);
			org.junit.Assert.assertSame(Thread.currentThread(), completedOn.get());
			org.junit.Assert.assertSame(Thread.currentThread(), cleanedOn.get());
			try {
				done.join();
				org.junit.Assert.fail("CPU failure must retain its original cause"); //$NON-NLS-1$
			} catch (java.util.concurrent.CompletionException expected) {
				org.junit.Assert.assertSame(failure, expected.getCause());
			}
		}
	}


	private void pumpUntil(final CompletableFuture<Boolean> future) {
		final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
		while (!future.isDone() && System.nanoTime() < deadline) {
			if (!display.readAndDispatch()) Thread.yield();
		}
		assertTrue("CPU sort did not publish to the SWT event thread", future.isDone());
	}

	@Test
	public void cpuLatestSortAlwaysOffloadsEvenSmallModel() throws InterruptedException {
		final int rows = 32;
		final Grid grid = grid(rows);
		final int[] input = keys(rows);
		final List<Runnable> submitted = new ArrayList<>();
		final Thread uiThread = Thread.currentThread();
		final AtomicBoolean calculatedOffUi = new AtomicBoolean();
		final CompletableFuture<Boolean> done = GridModelJobs.sortByIntKeysCpuLatest(grid,
				input, false, task -> submitted.add(() -> {
					calculatedOffUi.set(Thread.currentThread() != uiThread);
					task.run();
				}));
		assertEquals("worker queue should get one job", 1, submitted.size());
		assertFalse("small job cannot execute inline", done.isDone());
		assertFalse(grid.getRowPermutation().isSorted());
		input[0] = Integer.MAX_VALUE; // detached CPU input: caller may reuse buffer
		final Thread worker = new Thread(submitted.get(0), "nebula-cpu-sort");
		worker.start();
		pumpUntil(done);
		worker.join(2000);
		assertTrue(calculatedOffUi.get());
		assertTrue(done.join());
		assertArrayEquals(RowPermutation.sortedOrder(keys(rows), rows, false),
				view(grid.getRowPermutation()));
	}

	@Test
	public void cpuPreparedImmutableKeysAvoidUiSideModelCopy() throws InterruptedException {
		final int rows = 800;
		final int[] caller = keys(rows);
		final int[] expected = caller.clone();
		final java.util.concurrent.atomic.AtomicReference<GridModelJobs.IntKeySnapshot> frozen =
				new java.util.concurrent.atomic.AtomicReference<>();
		final Thread preparer = new Thread(() ->
				frozen.set(GridModelJobs.IntKeySnapshot.copyOf(caller, rows)), "key-copy-backend");
		preparer.start();
		preparer.join(2000);
		assertTrue(frozen.get() != null);
		final Grid grid = grid(rows);
		final List<Runnable> pending = new ArrayList<>();
		final CompletableFuture<Boolean> done = GridModelJobs.sortByIntKeysCpuLatest(
				grid, frozen.get(), false, pending::add);
		caller[0] = Integer.MAX_VALUE;
		final Thread worker = new Thread(pending.get(0), "grid-key-cpu");
		worker.start();
		pumpUntil(done);
		worker.join(2000);
		assertTrue(done.join());
		assertArrayEquals(RowPermutation.sortedOrder(expected, rows, false), view(grid.getRowPermutation()));
	}

	@Test
	public void cpuLatestSortCoalescesBurstAndCancelsOlderFutures() throws InterruptedException {
		final int rows = 1200;
		final Grid grid = grid(rows);
		final List<Runnable> submitted = new ArrayList<>();
		final int[] input = keys(rows);
		final CompletableFuture<Boolean> first = GridModelJobs.sortByIntKeysCpuLatest(
				grid, input, false, submitted::add);
		final CompletableFuture<Boolean> second = GridModelJobs.sortByIntKeysCpuLatest(
				grid, input, false, submitted::add);
		final CompletableFuture<Boolean> last = GridModelJobs.sortByIntKeysCpuLatest(
				grid, input, true, submitted::add);
		assertEquals("one worker drain must serve the newest request", 1, submitted.size());
		assertTrue(first.isDone());
		assertFalse(first.join());
		assertTrue(second.isDone());
		assertFalse(second.join());
		final Thread worker = new Thread(submitted.get(0), "nebula-coalesced-cpu");
		worker.start();
		pumpUntil(last);
		worker.join(2000);
		assertTrue(last.join());
		assertArrayEquals(RowPermutation.sortedOrder(input, rows, true), view(grid.getRowPermutation()));
	}

	@Test
	public void cpuLatestSortRefusesExecutorThatRunsOnUiThread() {
		final Grid grid = grid(32);
		try {
			GridModelJobs.sortByIntKeysCpuLatest(grid, keys(32), false, Runnable::run);
			org.junit.Assert.fail("inline executor must not run CPU sorting on UI thread");
		} catch (IllegalArgumentException expected) {
			assertFalse(grid.getRowPermutation().isSorted());
		}
	}

	@Test
	public void cpuLatestSortDropsStaleModelAndDisposedWidget() throws InterruptedException {
		final int rows = 64;
		final Grid grid = grid(rows);
		final List<Runnable> submitted = new ArrayList<>();
		final CompletableFuture<Boolean> stale = GridModelJobs.sortByIntKeysCpuLatest(
				grid, keys(rows), false, submitted::add);
		grid.getRowPermutation().setFilter(row -> (row & 1) == 0);
		grid.refreshRowPermutation();
		final Thread worker = new Thread(submitted.remove(0), "nebula-stale-cpu");
		worker.start();
		pumpUntil(stale);
		worker.join(2000);
		assertFalse(stale.join());

		final List<Runnable> pending = new ArrayList<>();
		final CompletableFuture<Boolean> disposed = GridModelJobs.sortByIntKeysCpuLatest(
				grid, keys(rows), true, pending::add);
		grid.dispose();
		assertTrue(disposed.isDone());
		assertFalse(disposed.join());
		// An accepted executor task can still start after disposal; it must be inert.
		final Thread trailing = new Thread(pending.get(0), "nebula-disposed-cpu");
		trailing.start();
		trailing.join(2000);
	}

	@Test
	public void cancellablePackedRadixStopsInsideCpuPass() {
		final int rows = 40_000;
		final int[] input = keys(rows);
		final int[] original = input.clone();
		final java.util.concurrent.atomic.AtomicInteger polls = new java.util.concurrent.atomic.AtomicInteger();
		try {
			RowPermutation.sortedOrder(input, rows, false, () -> polls.incrementAndGet() >= 4);
			org.junit.Assert.fail("obsolete sort must stop before a full radix pass");
		} catch (CancellationException expected) {
			assertTrue(polls.get() >= 4);
			assertArrayEquals("sort input must remain immutable", original, input);
		}
	}

	private Grid grid(final int rows) {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL);
		new GridColumn(grid, SWT.NONE).setWidth(80);
		grid.setItemCount(rows);
		grid.setRowPermutation(new RowPermutation(rows));
		return grid;
	}

	private static int[] keys(final int rows) {
		final Random random = new Random(rows);
		final int[] keys = new int[rows];
		for (int i = 0; i < rows; i++) {
			keys[i] = random.nextInt(1000);
		}
		return keys;
	}

	private static int[] view(final RowPermutation p) {
		final int[] out = new int[p.getViewCount()];
		p.copyModelRows(0, out, 0, out.length);
		return out;
	}
}
