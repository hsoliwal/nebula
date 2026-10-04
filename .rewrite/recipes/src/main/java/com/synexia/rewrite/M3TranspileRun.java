// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Objects;

/** Final text plus a deterministic chained receipt for a serial file-local transpilation run. */
public record M3TranspileRun(
        String sourcePath,
        String inputSha256,
        String outputSha256,
        String output,
        List<M3TranspilePassResult> passes) {

    public M3TranspileRun {
        sourcePath = text(sourcePath, "sourcePath");
        inputSha256 = hash(inputSha256, "inputSha256");
        outputSha256 = hash(outputSha256, "outputSha256");
        output = Objects.requireNonNull(output, "output");
        passes = List.copyOf(Objects.requireNonNull(passes, "passes"));
        validateChain(sourcePath, inputSha256, outputSha256, passes);
    }

    public boolean changed() {
        return !inputSha256.equals(outputSha256);
    }

    public String toTsv() {
        StringBuilder out = new StringBuilder(
                "ordinal\tpass_id\trecipe\tsource_path\tbefore_sha256\tafter_sha256\tchanged\n");
        for (M3TranspilePassResult pass : passes) {
            out.append(pass.ordinal()).append('\t')
                    .append(safe(pass.passId())).append('\t')
                    .append(safe(pass.recipeName())).append('\t')
                    .append(safe(pass.sourcePath())).append('\t')
                    .append(pass.beforeSha256()).append('\t')
                    .append(pass.afterSha256()).append('\t')
                    .append(pass.changed()).append('\n');
        }
        return out.toString();
    }

    private static void validateChain(
            String sourcePath,
            String inputSha256,
            String outputSha256,
            List<M3TranspilePassResult> passes) {
        String expectedBefore = inputSha256;
        for (int index = 0; index < passes.size(); index++) {
            M3TranspilePassResult pass = passes.get(index);
            if (pass.ordinal() != index + 1) {
                throw new IllegalArgumentException("pass ordinals must be contiguous");
            }
            if (!pass.sourcePath().equals(sourcePath)) {
                throw new IllegalArgumentException("pass source path changed");
            }
            if (!pass.beforeSha256().equals(expectedBefore)) {
                throw new IllegalArgumentException("pass hash chain is broken");
            }
            expectedBefore = pass.afterSha256();
        }
        if (!expectedBefore.equals(outputSha256)) {
            throw new IllegalArgumentException("output hash does not match final pass");
        }
    }

    private static String safe(String value) {
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static String text(String value, String field) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return value.strip();
    }

    private static String hash(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }
}
