// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import com.synexia.algorithms.core.IAlgorithm;
import com.synexia.job.IProgressMonitor;

/**
 * Algorithm plus stable descriptor used by ROP, DAG planners and accelerator selection.
 *
 * <p>The monitor-aware execution method is deliberately re-declared as abstract here. Canonical
 * shapes are therefore monitor-first by type contract rather than merely inheriting the
 * compatibility default from {@link IAlgorithm}. The historical one-argument entry point remains
 * available and delegates through a no-op monitor.
 */
public interface CanonicalShape<I, O> extends IAlgorithm<I, O> {

    AlgorithmDescriptor descriptor();

    /** Execute the canonical algorithm with explicit progress and cancellation. */
    @Override
    O execute(I input, IProgressMonitor monitor);

    /** Compatibility surface; all canonical execution still flows through the monitor-first path. */
    @Override
    default O execute(I input) {
        return execute(input, IProgressMonitor.noop());
    }
}
