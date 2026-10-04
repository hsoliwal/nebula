// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Objects;
import org.openrewrite.Recipe;

/** Shared ordered Java recipe leaves for both project-wide composition and M3 receipts. */
public final class M3MechanicalRecipeCatalog {
    /** One stable recipe identity and its immutable OpenRewrite implementation. */
    public record Entry(String id, Recipe recipe) {
        public Entry {
            if (id == null || id.isBlank() || id.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("id required");
            }
            id = id.strip();
            recipe = Objects.requireNonNull(recipe, "recipe");
        }

        public M3TranspilePass transpilePass() {
            return new M3TranspilePass(id, recipe);
        }
    }

    private M3MechanicalRecipeCatalog() {}

    /** Canonical six-leaf order reused by reactor recipes and file-local receipt planning. */
    public static List<Entry> entriesForPattern(String sourceFilePattern) {
        String pattern = checkedPattern(sourceFilePattern);
        return List.of(
                new Entry(
                        "01-upper-case-literal-suffixes",
                        new M3UpperCaseLiteralSuffixesRecipe(pattern)),
                new Entry("02-modifier-order", new M3ModifierOrderRecipe(pattern)),
                new Entry("03-negated-equality", new M3NegatedEqualityRecipe(pattern)),
                new Entry(
                        "04-remove-unused-imports",
                        new M3MechanicalJavaRecipe(pattern, true, false, false)),
                new Entry(
                        "05-order-imports",
                        new M3MechanicalJavaRecipe(pattern, false, true, false)),
                new Entry(
                        "06-auto-format",
                        new M3MechanicalJavaRecipe(pattern, false, false, true)));
    }

    private static String checkedPattern(String sourceFilePattern) {
        if (sourceFilePattern == null
                || sourceFilePattern.isBlank()
                || sourceFilePattern.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        return sourceFilePattern.strip();
    }
}
