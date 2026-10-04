// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Read-only Java semantic-atom inventory for recipe-first M3 transformations. */
public final class M3RecipeFirstJavaAtomInventoryTable
        extends DataTable<M3RecipeFirstJavaAtomInventoryTable.Row> {
    public M3RecipeFirstJavaAtomInventoryTable(Recipe recipe) {
        super(
                recipe,
                "M3 recipe-first Java atom inventory",
                "Java semantic atoms with observable-contract classification. "
                        + "Inventory rows never grant substitution authority.");
    }

    public record Row(
            @Column(displayName = "Source path", description = "Repository-relative source path.")
                    String sourcePath,
            @Column(displayName = "Owner type", description = "Fully qualified owning Java type.")
                    String ownerType,
            @Column(displayName = "Atom kind", description = "CLASS, METHOD, FIELD, INITIALIZER, LAMBDA, ANONYMOUS_CLASS or ENUM_VALUE.")
                    String atomKind,
            @Column(displayName = "Atom name", description = "Stable simple or synthetic atom name.")
                    String atomName,
            @Column(displayName = "Contract surface", description = "Observable contract classification.")
                    String contractSurface,
            @Column(displayName = "Native", description = "Whether the atom is a Java native method.")
                    boolean nativeAtom,
            @Column(displayName = "Recipe action", description = "Required recipe-first next action.")
                    String recipeAction,
            @Column(displayName = "Authority", description = "Always READ_ONLY_EVIDENCE.")
                    String authority) {}
}
