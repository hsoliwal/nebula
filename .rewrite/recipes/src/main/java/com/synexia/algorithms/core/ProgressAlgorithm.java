// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.core;

import com.synexia.job.IProgressMonitor;
import java.util.Objects;

/**
 * Strict algorithm contract whose execution always receives a progress monitor.
 *
 * <p>This is the preferred contract for new algorithm donors. The older {@link IAlgorithm}
 * remains available as a compatibility surface.
 */
@FunctionalInterface
public interface ProgressAlgorithm<I, O> {

    O execute(I input, IProgressMonitor monitor);

    default IAlgorithm<I, O> asAlgorithm() {
        return IAlgorithm.monitored((input, monitor) ->
                execute(input, ProgressMonitors.nonNull(monitor)));
    }

    default <R> ProgressAlgorithm<I, R> andThen(ProgressAlgorithm<O, R> next) {
        Objects.requireNonNull(next, "next");
        return (input, monitor) -> {
            IProgressMonitor root = ProgressMonitors.nonNull(monitor);
            root.beginTask("progress-algorithm-chain", 2L);
            try {
                O intermediate = execute(input, ProgressMonitors.child(root, 1L));
                root.checkCanceled();
                return next.execute(intermediate, ProgressMonitors.child(root, 1L));
            } finally {
                root.done();
            }
        };
    }
}
