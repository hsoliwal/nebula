// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.enterprise;

import com.synexia.algorithms.core.ProgressAlgorithm;
import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.Objects;

/**
 * Template Method bridge from enterprise/domain objects to a canonical algorithm donor.
 *
 * <p>Business code implements only encoding and decoding. The computational kernel remains a
 * reusable monitor-first donor. This keeps contest-style primitive encodings out of services,
 * controllers and repositories while preserving efficient algorithm implementations.
 */
public abstract class AlgorithmUseCaseTemplate<I, K, V, O> implements ProgressAlgorithm<I, O> {

    /** Human-readable operation name exposed to progress monitoring. */
    protected abstract String operationName();

    /** Convert ordinary business input into the donor's compact input. */
    protected abstract K encode(I input, IProgressMonitor monitor);

    /** The canonical reusable algorithm. */
    protected abstract ProgressAlgorithm<K, V> kernel();

    /** Convert the donor result back into a business-facing result. */
    protected abstract O decode(I input, V value, IProgressMonitor monitor);

    @Override
    public final O execute(I input, IProgressMonitor suppliedMonitor) {
        IProgressMonitor root = ProgressMonitors.nonNull(suppliedMonitor);
        root.beginTask(operationName(), 3L);
        try {
            root.checkCanceled();

            IProgressMonitor encode = ProgressMonitors.child(root, 1L);
            encode.beginTask("encode", 1L);
            K kernelInput;
            try {
                kernelInput = encode(input, encode);
                encode.checkCanceled();
                encode.worked(1L);
            } finally {
                encode.done();
            }

            root.checkCanceled();
            V kernelValue = Objects.requireNonNull(kernel(), "kernel")
                    .execute(kernelInput, ProgressMonitors.child(root, 1L));

            root.checkCanceled();
            IProgressMonitor decode = ProgressMonitors.child(root, 1L);
            decode.beginTask("decode", 1L);
            try {
                O output = decode(input, kernelValue, decode);
                decode.checkCanceled();
                decode.worked(1L);
                return output;
            } finally {
                decode.done();
            }
        } finally {
            root.done();
        }
    }
}
