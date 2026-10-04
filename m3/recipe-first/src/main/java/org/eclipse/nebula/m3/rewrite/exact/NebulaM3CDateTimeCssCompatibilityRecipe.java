// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * FILE-local compatibility migration for CDateTimePropertyHandler against the
 * current Eclipse CSS value model.
 *
 * <p>OpenRewrite's stock {@code ChangeType} recipe is the canonical donor for the
 * mechanical Measure -> CSSPrimitiveValue type migration. The reviewed postimage
 * additionally preserves Nebula's historical font-shorthand classification because
 * the newer Eclipse CSS2FontHelper changed that observable behavior.</p>
 */
public final class NebulaM3CDateTimeCssCompatibilityRecipe
        extends NebulaM3ExactJavaSnapshotRecipe {
    public static final String REPOSITORY_PATH =
            "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/src/"
                    + "org/eclipse/nebula/widgets/cdatetime/css/CDateTimePropertyHandler.java";
    public static final String MODULE_PATH =
            "src/org/eclipse/nebula/widgets/cdatetime/css/CDateTimePropertyHandler.java";
    public static final String BEFORE =
            "362526ff2fe0f9d877b258bcce631f5993f8a55fd8a4bc41e7edd6b87facdaf5";
    public static final String AFTER =
            "e9be2a0a729e0e198c86e33ffa7fbab4879bbd9c5ca627e34120e7ce565c1e0e";
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/cdatetime-css/"
                    + "CDateTimePropertyHandler.after.java.txt";

    @Override
    public String getDisplayName() {
        return "M3 migrate Nebula CDateTime CSS handler to current Eclipse CSS values";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed FILE-local compatibility postimage: remove the deleted "
                + "Measure dependency, preserve W3C CSSPrimitiveValue access, and retain the "
                + "historical Nebula font-shorthand classification contract.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "cdatetime",
                "css",
                "openrewrite",
                "change-type-donor",
                "compatibility",
                "file-local",
                "behavior-contract-preserving",
                "exact-source");
    }

    @Override
    protected String repositoryPath() {
        return REPOSITORY_PATH;
    }

    @Override
    protected String moduleRelativePath() {
        return MODULE_PATH;
    }

    @Override
    protected String beforeSha256() {
        return BEFORE;
    }

    @Override
    protected String afterSha256() {
        return AFTER;
    }

    @Override
    protected String afterResource() {
        return RESOURCE;
    }
}
