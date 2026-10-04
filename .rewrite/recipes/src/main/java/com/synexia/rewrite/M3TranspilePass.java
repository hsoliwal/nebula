// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Objects;
import org.openrewrite.Recipe;

/** One deterministic OpenRewrite pass in an M3 file-local transpilation plan. */
public record M3TranspilePass(String id, Recipe recipe) {
    public M3TranspilePass {
        if (id == null || id.isBlank() || id.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("id required");
        }
        id = id.strip();
        recipe = Objects.requireNonNull(recipe, "recipe");
    }
}
