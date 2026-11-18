/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.nebula.widgets.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.nebula.widgets.grid.model.RowPermutation;
import org.eclipse.swt.widgets.Display;

/**
 * Standalone CI proof against actual GridModelJobs and RowPermutation sources.
 * Only the native widget/device classes are replaced by strict thread stubs.
 */
public final class HeadlessGridCpuUiContract {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static int[] keys(int size) {
        int[] a = new int[size];
        Random rng = new Random(20261118L);
        for (int i = 0; i < size; i++) a[i] = rng.nextInt(400) - 200;
        return a;
    }

    private static int[] order(RowPermutation p) {
        int[] out = new int[p.getViewCount()];
        p.copyModelRows(0, out, 0, out.length);
        return out;
    }

    private static void complete(Display display, CompletableFuture<Boolean> done) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!done.isDone() && System.nanoTime() < deadline) {
            if (!display.readAndDispatch()) Thread.yield();
        }
        require(done.isDone(), "UI publication timed out");
    }

    private static void worker(Runnable job) throws InterruptedException {
        Thread thread = new Thread(job, "headless-viewport-cpu");
        thread.start();
        thread.join(5000);
        require(!thread.isAlive(), "CPU worker did not finish");
    }

    public static void main(String[] args) throws Exception {
        Display display = new Display();
        Thread uiThread = Thread.currentThread();
        Grid grid = new Grid(display, 1200);
        int[] input = keys(1200);
        int[] immutableInput = input.clone();
        List<Runnable> scheduled = new ArrayList<>();
        Executor deferred = scheduled::add;

        CompletableFuture<Boolean> old = GridModelJobs.sortByIntKeysCpuLatest(grid, input, false, deferred);
        CompletableFuture<Boolean> middle = GridModelJobs.sortByIntKeysCpuLatest(grid, input, false, deferred);
        CompletableFuture<Boolean> newest = GridModelJobs.sortByIntKeysCpuLatest(grid, input, true, deferred);
        require(scheduled.size() == 1, "burst queued more than one CPU worker");
        require(old.isDone() && !old.join(), "old request was not superseded");
        require(middle.isDone() && !middle.join(), "middle request was not superseded");
        require(!newest.isDone(), "sort ran on SWT thread");
        require(grid.refreshCount() == 0, "view mutated before worker/UI commit");
        input[0] = Integer.MAX_VALUE;

        AtomicReference<Thread> completionThread = new AtomicReference<>();
        newest.thenAccept(ok -> completionThread.set(Thread.currentThread()));
        worker(scheduled.remove(0));
        require(!newest.isDone(), "worker applied UI result directly");
        complete(display, newest);
        require(newest.join(), "newest request was not applied");
        require(completionThread.get() == uiThread, "result was not published on SWT thread");
        require(grid.refreshCount() == 1, "expected exactly one Grid refresh");
        require(Arrays.equals(RowPermutation.sortedOrder(immutableInput, 1200, true),
                order(grid.getRowPermutation())), "wrong stable sorted permutation");

        // A finished CPU result still in the SWT queue must not supersede a newer request.
        Grid queuedResultGrid = new Grid(display, 320);
        int[] keys320 = keys(320);
        List<Runnable> firstWorker = new ArrayList<>();
        CompletableFuture<Boolean> firstReady = GridModelJobs.sortByIntKeysCpuLatest(
                queuedResultGrid, keys320, false, firstWorker::add);
        worker(firstWorker.remove(0)); // computed but SWT has not applied its result
        require(!firstReady.isDone(), "CPU worker completed old future before GUI publication");
        List<Runnable> secondWorker = new ArrayList<>();
        CompletableFuture<Boolean> secondReady = GridModelJobs.sortByIntKeysCpuLatest(
                queuedResultGrid, keys320, true, secondWorker::add);
        require(firstReady.isDone() && !firstReady.join(), "queued old UI result was not superseded");
        worker(secondWorker.remove(0));
        complete(display, secondReady);
        require(secondReady.join(), "new UI result was not published");
        require(queuedResultGrid.refreshCount() == 1, "old frame mutated UI after new request");

        // A rejected executor must preserve the previously accepted queued UI result.
        Grid rejectionGrid = new Grid(display, 81);
        List<Runnable> acceptedWorker = new ArrayList<>();
        CompletableFuture<Boolean> accepted = GridModelJobs.sortByIntKeysCpuLatest(
                rejectionGrid, keys(81), false, acceptedWorker::add);
        worker(acceptedWorker.remove(0));
        try {
            GridModelJobs.sortByIntKeysCpuLatest(rejectionGrid, keys(81), true,
                    runnable -> { throw new java.util.concurrent.RejectedExecutionException("pool closed"); });
            throw new AssertionError("rejected executor unexpectedly accepted CPU work");
        } catch (java.util.concurrent.RejectedExecutionException expected) {
            // The earlier generation is still current and must remain eligible.
        }
        complete(display, accepted);
        require(accepted.join(), "rejected second request invalidated accepted sort");

        // The large primitive copy is prepared on a CPU worker, not in a SWT callback.
        Grid snapshotGrid = new Grid(display, 420);
        int[] mutableInput = keys(420);
        int[] frozenExpectation = mutableInput.clone();
        AtomicReference<GridModelJobs.IntKeySnapshot> frozen = new AtomicReference<>();
        worker(() -> frozen.set(GridModelJobs.IntKeySnapshot.copyOf(mutableInput, 420)));
        require(frozen.get() != null && frozen.get().size() == 420,
                "background key snapshot missing");
        mutableInput[0] = Integer.MAX_VALUE;
        List<Runnable> snapshotWorkers = new ArrayList<>();
        CompletableFuture<Boolean> snapshotResult = GridModelJobs.sortByIntKeysCpuLatest(
                snapshotGrid, frozen.get(), false, snapshotWorkers::add);
        worker(snapshotWorkers.remove(0));
        complete(display, snapshotResult);
        require(snapshotResult.join(), "CPU-prepared key snapshot was rejected");
        require(Arrays.equals(RowPermutation.sortedOrder(frozenExpectation, 420, false),
                order(snapshotGrid.getRowPermutation())), "snapshot aliased mutated caller array");

        Grid staleGrid = new Grid(display, 512);
        List<Runnable> staleJobs = new ArrayList<>();
        CompletableFuture<Boolean> stale = GridModelJobs.sortByIntKeysCpuLatest(staleGrid,
                keys(512), false, staleJobs::add);
        staleGrid.getRowPermutation().setFilter(row -> (row & 1) == 0);
        staleGrid.refreshRowPermutation();
        worker(staleJobs.remove(0));
        complete(display, stale);
        require(!stale.join(), "stale model-version job committed");

        Grid disposedGrid = new Grid(display, 300);
        List<Runnable> disposeJobs = new ArrayList<>();
        CompletableFuture<Boolean> pending = GridModelJobs.sortByIntKeysCpuLatest(disposedGrid,
                keys(300), false, disposeJobs::add);
        disposedGrid.dispose();
        require(pending.isDone() && !pending.join(), "disposal did not retire queued CPU work");
        worker(disposeJobs.remove(0));

        Grid invalidExecutor = new Grid(display, 17);
        try {
            GridModelJobs.sortByIntKeysCpuLatest(invalidExecutor, keys(17), false, Runnable::run);
            throw new AssertionError("inlining executor executed a CPU sort on the UI thread");
        } catch (IllegalArgumentException expected) {
            require(invalidExecutor.refreshCount() == 0, "inline executor mutated Grid");
        }
        List<Runnable> recoveredJobs = new ArrayList<>();
        CompletableFuture<Boolean> recovered = GridModelJobs.sortByIntKeysCpuLatest(
                invalidExecutor, keys(17), false, recoveredJobs::add);
        worker(recoveredJobs.remove(0));
        complete(display, recovered);
        require(recovered.join(), "rejected executor permanently poisoned the lane");
        require(invalidExecutor.refreshCount() == 1, "recovered request did not refresh Grid");
        display.dispose();
        System.out.println("PASS actual GridModelJobs: bounded CPU work, detached keys, UI commit, stale/closed refusal");
    }
}
