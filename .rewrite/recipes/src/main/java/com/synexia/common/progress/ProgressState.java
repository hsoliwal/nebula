// SPDX-License-Identifier: Apache-2.0
package com.synexia.common.progress;

import java.util.Objects;

/** Dependency-light immutable state shared by all Synexia progress monitor adapters. */
public record ProgressState(
        String taskName,
        String subTaskName,
        long worked,
        long totalWork,
        boolean canceled,
        boolean done,
        long generation,
        long startedAtMs) {
    public ProgressState {
        taskName = Objects.requireNonNullElse(taskName, "");
        subTaskName = Objects.requireNonNullElse(subTaskName, "");
        if (worked < 0L) throw new IllegalArgumentException("worked must be non-negative");
        if (totalWork < ProgressMonitor.UNKNOWN) {
            throw new IllegalArgumentException("invalid totalWork");
        }
        if (generation < 0L || startedAtMs < 0L) {
            throw new IllegalArgumentException("negative generation or start time");
        }
    }
}
