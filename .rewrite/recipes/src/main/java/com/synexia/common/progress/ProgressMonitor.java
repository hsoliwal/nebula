// SPDX-License-Identifier: Apache-2.0
package com.synexia.common.progress;

import java.util.concurrent.CancellationException;

/** Dependency-light progress/cancellation SPI shared below job and collection layers. */
public interface ProgressMonitor {
    long UNKNOWN = -1L;

    void beginTask(String taskName, long totalWork);
    void done();
    void worked(long work);
    void setProgress(long worked, long totalWork);
    void setTaskName(String taskName);
    void subTask(String subTaskName);
    boolean isCanceled();
    void setCanceled(boolean canceled);
    ProgressState state();

    default void checkCanceled() {
        if (isCanceled()) {
            throw new CancellationException("Task canceled: " + state().taskName());
        }
    }

    default ProgressMonitor split(long parentWork) {
        return new ChildProgressMonitor(this, parentWork, state().generation());
    }
}
