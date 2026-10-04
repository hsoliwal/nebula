// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;

/**
 * Compatibility facade for the canonical pattern-only IOP pass registry.
 *
 * <p>All M3 IOP mutation authority is pattern-only; callers that need the explicit name should use
 * {@link M3IopPatternMechanicalPasses}.</p>
 */
public final class M3IopMechanicalPasses {
    private M3IopMechanicalPasses() {}

    public static List<M3TranspilePass> forFile(String sourcePath) {
        return M3IopPatternMechanicalPasses.forFile(sourcePath);
    }

    public static List<M3TranspilePass> forPattern(String sourceFilePattern) {
        return M3IopPatternMechanicalPasses.forPattern(sourceFilePattern);
    }
}
