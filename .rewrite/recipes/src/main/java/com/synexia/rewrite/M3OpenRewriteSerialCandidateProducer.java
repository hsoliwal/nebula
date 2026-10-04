// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.m3.contract.SerialAtomReview;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * OpenRewrite candidate producer for the serial atom engine.
 *
 * <p>Each request reparses through the canonical OpenRewrite runner and returns only the first pass
 * that changes the current source. SerialAtomReview then decides whether that complete candidate
 * changes exactly one atom and preserves the locked contract.</p>
 */
public final class M3OpenRewriteSerialCandidateProducer implements SerialAtomReview.CandidateProducer {
    private final String sourcePath;
    private final List<Path> classpath;
    private final List<M3TranspilePass> passes;

    public M3OpenRewriteSerialCandidateProducer(
            String sourcePath, List<Path> classpath, List<M3TranspilePass> passes) {
        this.sourcePath = M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath);
        this.classpath = List.copyOf(Objects.requireNonNull(classpath, "classpath"));
        this.passes = List.copyOf(Objects.requireNonNull(passes, "passes"));
        if (this.passes.isEmpty()) throw new IllegalArgumentException("passes must not be empty");
    }

    @Override
    public Optional<String> propose(SerialAtomReview.Request request) {
        String requestPath = request.sourcePath().toString().replace('\\', '/');
        if (!requestPath.equals(sourcePath)) {
            throw new IllegalArgumentException("serial request path does not match recipe source path");
        }
        for (M3TranspilePass pass : passes) {
            M3OpenRewriteExecution execution =
                    M3OpenRewriteTranspiler.executeSingle(sourcePath, request.source(), classpath, pass);
            if (!execution.after().equals(request.source())) return Optional.of(execution.after());
        }
        return Optional.empty();
    }

    public List<M3TranspilePass> passes() { return passes; }
}
