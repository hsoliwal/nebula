// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.ArrayList;
import java.util.List;

/** Canonical OpenRewrite pass order for one admitted IOP pattern source atom. */
public final class M3IopPatternMechanicalPasses {
    private M3IopPatternMechanicalPasses() {}

    public static List<M3TranspilePass> forFile(String sourcePath) {
        return forPattern(M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath));
    }

    public static List<M3TranspilePass> forPattern(String sourceFilePattern) {
        return forPattern(sourceFilePattern, List.of());
    }

    /**
     * Compose the canonical IOP pass order with bounded task-specific operator leaves.
     *
     * <p>Mechanical normalization always runs first. Operator leaves are then applied in the
     * deterministic order supplied by the operator writer. Static-analysis checks remain last.
     * Every additional pass must already be wrapped by the IOP pattern/class-hook fence and must
     * use the exact same source pattern.</p>
     */
    public static List<M3TranspilePass> forPattern(
            String sourceFilePattern,
            List<M3TranspilePass> operatorPasses) {
        if (sourceFilePattern == null
                || sourceFilePattern.isBlank()
                || sourceFilePattern.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        String pattern = sourceFilePattern.strip();

        List<M3TranspilePass> passes = new ArrayList<>(
                M3MechanicalPasses.forPattern(pattern).stream()
                        .map(pass -> new M3TranspilePass(
                                pass.id(),
                                new M3IopGuardedRecipe(pass.id(), pattern)))
                        .toList());

        java.util.HashSet<String> ids = new java.util.HashSet<>();
        passes.forEach(pass -> ids.add(pass.id()));

        for (M3TranspilePass operator : List.copyOf(
                java.util.Objects.requireNonNull(operatorPasses, "operatorPasses"))) {
            if (operator == null) throw new IllegalArgumentException("operator pass cannot be null");
            if (!(operator.recipe() instanceof M3IopPatternScopedRecipe scoped)) {
                throw new IllegalArgumentException("operator pass must be IOP pattern scoped");
            }
            if (!scoped.sourceFilePattern().equals(pattern)) {
                throw new IllegalArgumentException("operator pass source pattern mismatch");
            }
            if (!ids.add(operator.id())) {
                throw new IllegalArgumentException("duplicate IOP pass id: " + operator.id());
            }
            passes.add(operator);
        }

        List<M3TranspilePass> analysis = M3StaticAnalysisRules.passesForIopPattern(pattern);
        for (M3TranspilePass check : analysis) {
            if (!ids.add(check.id())) {
                throw new IllegalArgumentException("duplicate IOP pass id: " + check.id());
            }
            passes.add(check);
        }
        return List.copyOf(passes);
    }
}
