// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.Locale;
import java.util.Objects;

/** Small fail-closed atom gate for replacing one reviewed implementation file. */
public final class ProblemReplacementGate {
    public enum Decision {
        ADMIT_CANDIDATE,
        SOURCE_CHANGED,
        INTERFACE_CHANGED,
        PROOF_MISSING
    }

    public record Request(
            String sourceId,
            String expectedSourceSha256,
            String observedSourceSha256,
            String expectedInterfaceSha256,
            String candidateInterfaceSha256,
            String differentialProofSha256) {
        public Request {
            sourceId = required(sourceId, "sourceId");
            expectedSourceSha256 = hash(expectedSourceSha256, "expectedSourceSha256");
            observedSourceSha256 = hash(observedSourceSha256, "observedSourceSha256");
            expectedInterfaceSha256 = hash(expectedInterfaceSha256, "expectedInterfaceSha256");
            candidateInterfaceSha256 = hash(candidateInterfaceSha256, "candidateInterfaceSha256");
            if (differentialProofSha256 != null && !differentialProofSha256.isBlank()) {
                differentialProofSha256 = hash(differentialProofSha256, "differentialProofSha256");
            } else {
                differentialProofSha256 = "";
            }
        }
    }

    private ProblemReplacementGate() {}

    public static Decision evaluate(Request request) {
        Objects.requireNonNull(request, "request");
        if (!request.expectedSourceSha256().equals(request.observedSourceSha256())) {
            return Decision.SOURCE_CHANGED;
        }
        if (!request.expectedInterfaceSha256().equals(request.candidateInterfaceSha256())) {
            return Decision.INTERFACE_CHANGED;
        }
        if (request.differentialProofSha256().isEmpty()) {
            return Decision.PROOF_MISSING;
        }
        return Decision.ADMIT_CANDIDATE;
    }

    public static boolean admitted(Request request) {
        return evaluate(request) == Decision.ADMIT_CANDIDATE;
    }

    private static String hash(String value, String label) {
        String checked = required(value, label).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(label);
        return checked;
    }

    private static String required(String value, String label) {
        String checked = Objects.requireNonNull(value, label).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(label);
        return checked;
    }
}

