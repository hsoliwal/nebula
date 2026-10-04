/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.algorithms.core;

import com.synexia.job.IProgressMonitor;
import com.synexia.job.ProgressSnapshot;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/** Progress slices preserve cancellation, including zero-weight children and parent overrides. */
public final class ProgressMonitors {
    private ProgressMonitors() { }

    public static IProgressMonitor nonNull(IProgressMonitor monitor) {
        return monitor == null ? IProgressMonitor.noop() : monitor;
    }

    public static void checkCanceled(IProgressMonitor monitor) {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Algorithm thread interrupted");
        }
        monitor.checkCanceled();
    }

    public static IProgressMonitor child(IProgressMonitor monitor, long allocation) {
        if (allocation < 0L) { throw new IllegalArgumentException("Negative allocation"); }
        return new ChildProgressMonitor(nonNull(monitor), allocation);
    }

    public static String taskName(Object algorithm) {
        Objects.requireNonNull(algorithm, "algorithm");
        String name = algorithm.getClass().getSimpleName();
        return name.isBlank() ? algorithm.getClass().getName() : name;
    }

    /** Single-task slice. done closes a task; it is not a success receipt. */
    private static final class ChildProgressMonitor implements IProgressMonitor {
        private final IProgressMonitor parent;
        private final long allocation;
        private long worked;
        private long reported;
        private long total = UNKNOWN;
        private boolean started;
        private boolean done;
        private String task = "";
        private String subTask = "";
        private long startedAt = System.nanoTime();

        private ChildProgressMonitor(IProgressMonitor parent, long allocation) {
            this.parent = parent;
            this.allocation = allocation;
        }

        @Override
        public synchronized void beginTask(String name, long totalWork) {
            if (started || done) { throw new IllegalStateException("Slice already used"); }
            if (totalWork < UNKNOWN) { throw new IllegalArgumentException("Invalid total"); }
            started = true;
            task = Objects.requireNonNull(name, "name");
            total = totalWork;
            startedAt = System.nanoTime();
            parent.subTask(name);
        }

        @Override
        public synchronized void done() {
            if (!done) {
                done = true;
                if (!isCanceled()) { report(allocation); }
            }
        }

        @Override
        public synchronized void worked(long work) {
            if (work < 0L) { throw new IllegalArgumentException("Negative work"); }
            if (done) { return; }
            worked = work > Long.MAX_VALUE - worked ? Long.MAX_VALUE : worked + work;
            scale();
        }

        @Override
        public synchronized void setProgress(long value, long totalWork) {
            if (value < 0L || totalWork < UNKNOWN) { throw new IllegalArgumentException("Invalid progress"); }
            if (done) { return; }
            worked = Math.max(worked, value);
            total = totalWork;
            scale();
        }

        private void scale() {
            if (total <= 0L || allocation == 0L) { return; }
            long completed = Math.min(worked, total);
            long scaled = completed <= Long.MAX_VALUE / allocation
                    ? completed * allocation / total
                    : BigInteger.valueOf(completed).multiply(BigInteger.valueOf(allocation))
                            .divide(BigInteger.valueOf(total)).longValueExact();
            report(scaled);
        }

        private void report(long value) {
            long bounded = Math.min(allocation, value);
            if (bounded > reported) {
                long delta = bounded - reported;
                reported = bounded;
                parent.worked(delta);
            }
        }

        @Override
        public synchronized void setTaskName(String name) { task = name; parent.subTask(name); }
        @Override
        public synchronized void subTask(String name) { subTask = name; parent.subTask(name); }
        @Override
        public boolean isCanceled() { return parent.isCanceled() || Thread.currentThread().isInterrupted(); }
        @Override
        public void checkCanceled() { ProgressMonitors.checkCanceled(parent); }
        @Override
        public void setCanceled(boolean canceled) { parent.setCanceled(canceled); }

        @Override
        public synchronized ProgressSnapshot snapshot() {
            long elapsed = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
            double percentage = total < 0 ? -1.0 : total == 0 ? 100.0
                    : Math.min(100.0, 100.0 * worked / total);
            double rate = elapsed == 0 ? 0.0 : 1000.0 * worked / elapsed;
            return new ProgressSnapshot(task, subTask, worked, total, percentage, rate,
                    elapsed, -1L, isCanceled(), done, Instant.now());
        }
    }
}
