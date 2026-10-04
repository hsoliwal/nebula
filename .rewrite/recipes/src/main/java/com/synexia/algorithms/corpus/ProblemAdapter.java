// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressAlgorithm;
import com.synexia.algorithms.enterprise.AlgorithmUseCaseTemplate;
import com.synexia.algorithms.shapes.CanonicalDonorRegistry;
import com.synexia.job.IProgressMonitor;
import java.util.Objects;
import java.util.Optional;

/**
 * Production adapter for one pinned Java problem implementation.
 *
 * <p>The external source is provenance only. Execution uses Synexia's canonical donor for the
 * classified computational shape. Business callers may bind their own DTO encoder/decoder through
 * the shared Template Method instead of copying contest-specific method signatures.
 */
public record ProblemAdapter(
        CorpusSourceEntry source,
        ProblemShape classification,
        TemplateStyle templateStyle) {

    @FunctionalInterface
    public interface Encoder<I, K> {
        K encode(I input, IProgressMonitor monitor);
    }

    @FunctionalInterface
    public interface Decoder<I, V, O> {
        O decode(I input, V value, IProgressMonitor monitor);
    }

    public ProblemAdapter {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(classification, "classification");
        Objects.requireNonNull(templateStyle, "templateStyle");
    }

    public String id() {
        return source.platform() + ":" + source.repository() + ":" + source.path();
    }

    public String problemName() {
        return source.problemName();
    }

    public boolean executable() {
        return classification.classified()
                && CanonicalDonorRegistry.supports(classification.shape());
    }

    public Optional<CanonicalDonorRegistry.Entry> donor() {
        return executable()
                ? Optional.of(CanonicalDonorRegistry.require(classification.shape()))
                : Optional.empty();
    }

    /**
     * Execute using the canonical donor's native input/output representation.
     *
     * <p>This is mainly for infrastructure, corpus verification and generated adapters.
     */
    public Object executeCanonical(Object donorInput, IProgressMonitor monitor) {
        CanonicalDonorRegistry.Entry entry = donor().orElseThrow(
                () -> new IllegalStateException("problem is not yet classified: " + id()));
        return entry.execute(donorInput, monitor);
    }

    /**
     * Bind ordinary domain DTOs to this problem's canonical donor through Template Method.
     *
     * <p>The encoder/decoder are the only problem/application-specific pieces. The algorithm itself
     * remains the shared canonical donor.
     */
    public <I, K, V, O> ProgressAlgorithm<I, O> bind(
            Encoder<I, K> encoder,
            Decoder<I, V, O> decoder) {
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(decoder, "decoder");
        CanonicalDonorRegistry.Entry entry = donor().orElseThrow(
                () -> new IllegalStateException("problem is not yet classified: " + id()));

        return new AlgorithmUseCaseTemplate<I, K, V, O>() {
            @Override
            protected String operationName() {
                return templateStyle.kind().name().toLowerCase()
                        + ":" + ProblemAdapter.this.problemName();
            }

            @Override
            protected K encode(I input, IProgressMonitor monitor) {
                return encoder.encode(input, monitor);
            }

            @Override
            @SuppressWarnings("unchecked")
            protected ProgressAlgorithm<K, V> kernel() {
                return (input, monitor) -> (V) entry.execute(input, monitor);
            }

            @Override
            protected O decode(I input, V value, IProgressMonitor monitor) {
                return decoder.decode(input, value, monitor);
            }
        };
    }
}
