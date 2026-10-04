// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Objects;

/** Detailed diagnostic receipt for exactly one file-local OpenRewrite scheduler cycle. */
public record M3OpenRewriteExecution(
        String sourcePath,
        String before,
        String after,
        String recipeClass,
        String displayName,
        List<String> recipeDescriptors,
        String diff) {

    public M3OpenRewriteExecution {
        sourcePath = M3OpenRewriteTranspiler.normalizeSourcePathOrDirectory(sourcePath);
        before = Objects.requireNonNull(before, "before");
        after = Objects.requireNonNull(after, "after");
        recipeClass = text(recipeClass, "recipeClass");
        displayName = text(displayName, "displayName");
        recipeDescriptors = List.copyOf(
                Objects.requireNonNull(recipeDescriptors, "recipeDescriptors"));
        diff = Objects.requireNonNull(diff, "diff");
    }

    public boolean changed() {
        return !before.equals(after);
    }

    private static String text(String value, String field) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return value.strip();
    }
}
