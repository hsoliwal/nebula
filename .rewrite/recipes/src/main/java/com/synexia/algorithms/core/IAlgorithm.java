/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.algorithms.core;

import com.synexia.job.IProgressMonitor;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import java.util.function.Predicate;

/** Source-compatible root. New implementations consume the monitor, not just a boundary wrapper. */
@FunctionalInterface
public interface IAlgorithm<I, O> {
    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IAlgorithm.class);
    O execute(I input);

    default O execute(I input, IProgressMonitor suppliedMonitor) {
        IProgressMonitor monitor = ProgressMonitors.nonNull(suppliedMonitor);
        ProgressMonitors.checkCanceled(monitor);
        monitor.beginTask(ProgressMonitors.taskName(this), 1L);
        try {
            ProgressMonitors.checkCanceled(monitor);
            O result = execute(input);
            ProgressMonitors.checkCanceled(monitor);
            monitor.worked(1L);
            ProgressMonitors.checkCanceled(monitor);
            return result;
        } finally { monitor.done(); }
    }

    default <R> IAlgorithm<I, R> andThen(IAlgorithm<O, R> next) {
        Objects.requireNonNull(next, "next");
        return monitored((input, monitor) -> {
            monitor.beginTask("algorithm-chain", 2L);
            try {
                ProgressMonitors.checkCanceled(monitor);
                O intermediate = execute(input, ProgressMonitors.child(monitor, 1L));
                ProgressMonitors.checkCanceled(monitor);
                R result = next.execute(intermediate, ProgressMonitors.child(monitor, 1L));
                ProgressMonitors.checkCanceled(monitor);
                return result;
            } finally { monitor.done(); }
        });
    }

    default <T> IAlgorithm<T, O> compose(IAlgorithm<T, I> before) {
        return Objects.requireNonNull(before, "before").andThen(this);
    }

    default IAlgorithm<I, TimedResult<O>> timed() {
        return monitored((input, monitor) -> {
            monitor.beginTask("timed-" + ProgressMonitors.taskName(this), 1L);
            try {
                long start = System.nanoTime();
                O result = execute(input, ProgressMonitors.child(monitor, 1L));
                ProgressMonitors.checkCanceled(monitor);
                return new TimedResult<>(result, System.nanoTime() - start);
            } finally { monitor.done(); }
        });
    }

    default IAlgorithm<I, O> logged(String name) {
        Objects.requireNonNull(name, "name");
        return monitored((input, monitor) -> {
            monitor.beginTask(name, 1L);
            try {
                log.info("[%s] Starting with input: %s".formatted(name, summarize(input)));
                ProgressMonitors.checkCanceled(monitor);
                long start = System.nanoTime();
                O result = execute(input, ProgressMonitors.child(monitor, 1L));
                ProgressMonitors.checkCanceled(monitor);
                long elapsed = (System.nanoTime() - start) / 1_000_000L;
                log.info("[%s] Completed in %dms with output: %s".formatted(name, elapsed, summarize(result)));
                ProgressMonitors.checkCanceled(monitor);
                return result;
            } finally { monitor.done(); }
        });
    }

    /** Explicit opt-in: the caller is responsible for retry safety/idempotency. */
    default IAlgorithm<I, O> retry(int maxRetries) {
        if (maxRetries < 0) { throw new IllegalArgumentException("maxRetries must be >= 0"); }
        int attempts = Math.addExact(maxRetries, 1);
        return monitored((input, monitor) -> {
            monitor.beginTask("retry-" + ProgressMonitors.taskName(this), attempts);
            Exception lastException = null;
            try {
                for (int attempt = 0; attempt < attempts; attempt++) {
                    ProgressMonitors.checkCanceled(monitor);
                    try {
                        O result = execute(input, ProgressMonitors.child(monitor, 1L));
                        ProgressMonitors.checkCanceled(monitor);
                        return result;
                    } catch (CancellationException canceled) {
                        throw canceled;
                    } catch (Exception failure) {
                        ProgressMonitors.checkCanceled(monitor);
                        lastException = failure;
                    }
                }
                throw new AlgorithmException("Failed after " + attempts + " attempts", lastException);
            } finally { monitor.done(); }
        });
    }

    default IAlgorithm<I, O> when(Predicate<I> condition, O fallback) {
        Objects.requireNonNull(condition, "condition");
        return monitored((input, monitor) -> {
            monitor.beginTask("conditional-" + ProgressMonitors.taskName(this), 1L);
            try {
                ProgressMonitors.checkCanceled(monitor);
                boolean accepted = condition.test(input);
                ProgressMonitors.checkCanceled(monitor);
                if (!accepted) {
                    monitor.worked(1L);
                    ProgressMonitors.checkCanceled(monitor);
                    return fallback;
                }
                O result = execute(input, ProgressMonitors.child(monitor, 1L));
                ProgressMonitors.checkCanceled(monitor);
                return result;
            } finally { monitor.done(); }
        });
    }

    default <R> IAlgorithm<I, R> map(Function<O, R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return monitored((input, monitor) -> {
            monitor.beginTask("map-" + ProgressMonitors.taskName(this), 2L);
            try {
                O value = execute(input, ProgressMonitors.child(monitor, 1L));
                ProgressMonitors.checkCanceled(monitor);
                R mapped = mapper.apply(value);
                ProgressMonitors.checkCanceled(monitor);
                monitor.worked(1L);
                ProgressMonitors.checkCanceled(monitor);
                return mapped;
            } finally { monitor.done(); }
        });
    }

    static <I, O> IAlgorithm<I, O> of(Function<I, O> function) {
        return Objects.requireNonNull(function, "function")::apply;
    }

    /** Function owns its task lifecycle. Boundary checks also guard custom monitored delegates. */
    static <I, O> IAlgorithm<I, O> monitored(MonitoredFunction<I, O> function) {
        Objects.requireNonNull(function, "function");
        return new IAlgorithm<>() {
            @Override public O execute(I input) { return execute(input, IProgressMonitor.noop()); }
            @Override public O execute(I input, IProgressMonitor supplied) {
                IProgressMonitor monitor = ProgressMonitors.nonNull(supplied);
                ProgressMonitors.checkCanceled(monitor);
                O result = function.apply(input, monitor);
                ProgressMonitors.checkCanceled(monitor);
                return result;
            }
        };
    }

    static <T> IAlgorithm<T, T> identity() { return input -> input; }

    private static String summarize(Object obj) {
        if (obj == null) { return "null"; }
        if (obj.getClass().isArray()) {
            return obj.getClass().getComponentType().getSimpleName()
                    + "[" + java.lang.reflect.Array.getLength(obj) + "]";
        }
        String value = obj.toString();
        return value.length() > 50 ? value.substring(0, 47) + "..." : value;
    }

    @FunctionalInterface
    interface MonitoredFunction<I, O> { O apply(I input, IProgressMonitor monitor); }
    record TimedResult<T>(T value, long nanos) {
        public long millis() { return nanos / 1_000_000L; }
        public double seconds() { return nanos / 1_000_000_000.0d; }
    }
}
