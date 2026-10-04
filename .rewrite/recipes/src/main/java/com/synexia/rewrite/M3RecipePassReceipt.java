// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Objects;

/** Immutable proof record for one recipe-first M3 pass. */
public record M3RecipePassReceipt(
        String recipeClassName,
        String recipeArtifactSha256,
        String catalogueRootSha256,
        String catalogueSnapshotSha256,
        String preimageRootSha256,
        String postimageRootSha256,
        List<AtomResult> atoms,
        Verification verification) {

    public M3RecipePassReceipt {
        recipeClassName = M3CodeAtom.required(recipeClassName, "recipeClassName");
        recipeArtifactSha256 = M3CodeAtom.sha256(recipeArtifactSha256, "recipeArtifactSha256");
        catalogueRootSha256 = M3CodeAtom.sha256(catalogueRootSha256, "catalogueRootSha256");
        catalogueSnapshotSha256 =
                M3CodeAtom.sha256(catalogueSnapshotSha256, "catalogueSnapshotSha256");
        preimageRootSha256 = M3CodeAtom.sha256(preimageRootSha256, "preimageRootSha256");
        postimageRootSha256 = M3CodeAtom.sha256(postimageRootSha256, "postimageRootSha256");
        atoms = List.copyOf(Objects.requireNonNull(atoms, "atoms"));
        verification = Objects.requireNonNull(verification, "verification");
    }

    public boolean fixedPoint() {
        // Unbound hashes/counts cannot certify a source fixed point.
        return false;
    }

    public boolean promotable() {
        // Legacy callers must migrate to source-bound admission; no vacuous promotion.
        return false;
    }

    /** Source identity proves a no-op only, never semantic correctness. */
    public boolean fixedPoint(M3SourceCoverageGate.Result coverage) {
        return coverage != null && coverage.status() == M3SourceCoverageGate.Status.NO_OP
                && coverage.matches(this);
    }

    /** Necessary proof gate; the existing serial canonical owner still decides promotion. */
    public boolean promotable(M3SourceCoverageGate.Result coverage) {
        return coverage != null && coverage.status() == M3SourceCoverageGate.Status.VERIFIED_CHANGE
                && coverage.matches(this) && !atoms.isEmpty() && verification.allPassed()
                && atoms.stream().allMatch(AtomResult::contractPreserved)
                && atoms.stream().allMatch(AtomResult::changed)
                && atoms.stream().noneMatch(result -> result.state().blocksMutation());
    }

    public record AtomResult(M3CodeAtom state, boolean changed, String postContractSha256) {
        public AtomResult {
            state = Objects.requireNonNull(state, "state");
            postContractSha256 = M3CodeAtom.sha256(postContractSha256, "postContractSha256");
        }

        public boolean contractPreserved() {
            return state.contractSha256().equals(postContractSha256);
        }
    }

    public record Verification(
            boolean dryRunReviewed,
            boolean compilePassed,
            boolean staticAnalysisPassed,
            boolean testsPassed,
            boolean javaOraclePassed,
            boolean nativeParityRequired,
            boolean nativeParityPassed,
            boolean benchmarkRequired,
            boolean benchmarkPassed) {

        public boolean allPassed() {
            return dryRunReviewed
                    && compilePassed
                    && staticAnalysisPassed
                    && testsPassed
                    && javaOraclePassed
                    && (!nativeParityRequired || nativeParityPassed)
                    && (!benchmarkRequired || benchmarkPassed);
        }
    }
}
