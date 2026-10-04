// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.ArrayList;
import java.util.List;

/** Canonical concern-separated mechanical pass order for one Java source atom. */
public final class M3MechanicalPasses {
    private M3MechanicalPasses() {}

    public static List<M3TranspilePass> forFile(String sourcePath) {
        return forPattern(M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath));
    }

    /**
     * Opt-in broad candidate plan: the canonical Synexia passes followed by the admitted external
     * static-analysis leaves.  The external pack is resolved fail-closed by
     * {@link M3StaticAnalysisRules}; the conservative {@link #forFile(String)} plan never needs
     * that separately licensed runtime.
     */
    public static List<M3TranspilePass> forFileWithStaticAnalysis(String sourcePath) {
        String pattern = M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath);
        List<M3TranspilePass> passes = new ArrayList<>(forPattern(pattern));
        passes.addAll(M3StaticAnalysisRules.passesForFile(pattern));
        return List.copyOf(passes);
    }

    public static List<M3TranspilePass> forPattern(String sourceFilePattern) {
        return M3MechanicalRecipeCatalog.entriesForPattern(sourceFilePattern).stream()
                .map(M3MechanicalRecipeCatalog.Entry::transpilePass)
                .toList();
    }
}
