// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Objects;

/**
 * Canonical, syntax-only naming policy for the staged MIndex to M3 migration.
 *
 * <p>The policy deliberately performs only the mechanically provable prefix contraction
 * {@code MIndex* -> M3*}. It does not guess acronym casing, package moves, semantic equivalence,
 * compatibility, or implementation ownership.</p>
 */
public final class M3MIndexNamePolicy {
    private static final String OLD_PREFIX = "MIndex";
    private static final String NEW_PREFIX = "M3";
    private static final String NEW_INDEX_PREFIX = "M3Index";

    /**
     * Canonical ownership family.
     *
     * <p>{@link #M3} is the existing default and preserves all current migrations.
     * {@link #M3_INDEX} is explicit-only: callers must have separately proved that indexing is
     * part of the semantic identity rather than merely historical MIndex naming.</p>
     */
    public enum CanonicalFamily {
        M3,
        M3_INDEX
    }

    private M3MIndexNamePolicy() {}

    public static boolean isMIndexSimpleName(String simpleName) {
        return simpleName != null
                && simpleName.startsWith(OLD_PREFIX)
                && simpleName.length() > OLD_PREFIX.length();
    }

    public static String canonicalSimpleName(String simpleName) {
        return canonicalSimpleName(simpleName, CanonicalFamily.M3);
    }

    public static String canonicalSimpleName(
            String simpleName, CanonicalFamily family) {
        String checked = text(simpleName, "simpleName");
        if (!isMIndexSimpleName(checked)) {
            throw new IllegalArgumentException("not an MIndex type name: " + checked);
        }
        Objects.requireNonNull(family, "family");
        String prefix =
                family == CanonicalFamily.M3_INDEX ? NEW_INDEX_PREFIX : NEW_PREFIX;
        return prefix + checked.substring(OLD_PREFIX.length());
    }

    /**
     * Convert every class-name segment in a fully qualified Java type while preserving the package.
     *
     * <p>Nested classes use the JVM/OpenRewrite {@code $} separator, so both
     * {@code a.MIndexOuter$Inner} and {@code a.Outer$MIndexInner} are handled deterministically.</p>
     */
    public static boolean containsMIndexTypeSegment(String fullyQualifiedName) {
        if (fullyQualifiedName == null || fullyQualifiedName.isBlank()) {
            return false;
        }
        int packageSeparator = fullyQualifiedName.lastIndexOf('.');
        String classPart =
                packageSeparator < 0
                        ? fullyQualifiedName
                        : fullyQualifiedName.substring(packageSeparator + 1);
        for (String segment : classPart.split("\\$")) {
            if (isMIndexSimpleName(segment)) {
                return true;
            }
        }
        return false;
    }

    public static String canonicalFullyQualifiedName(String fullyQualifiedName) {
        return canonicalFullyQualifiedName(
                fullyQualifiedName, CanonicalFamily.M3);
    }

    public static String canonicalFullyQualifiedName(
            String fullyQualifiedName, CanonicalFamily family) {
        String checked = text(fullyQualifiedName, "fullyQualifiedName");
        Objects.requireNonNull(family, "family");
        int packageSeparator = checked.lastIndexOf('.');
        String packagePrefix =
                packageSeparator < 0 ? "" : checked.substring(0, packageSeparator + 1);
        String classPart =
                packageSeparator < 0 ? checked : checked.substring(packageSeparator + 1);

        String[] segments = classPart.split("\\$", -1);
        StringBuilder renamed = new StringBuilder(packagePrefix);
        boolean changed = false;
        for (int index = 0; index < segments.length; index++) {
            if (index > 0) {
                renamed.append('$');
            }
            String segment = segments[index];
            if (isMIndexSimpleName(segment)) {
                renamed.append(canonicalSimpleName(segment, family));
                changed = true;
            } else {
                renamed.append(segment);
            }
        }
        if (!changed) {
            throw new IllegalArgumentException(
                    "fully qualified name has no MIndex type segment: " + checked);
        }
        return renamed.toString();
    }

    static String text(String value, String field) {
        Objects.requireNonNull(value, field);
        String checked = value.strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return checked;
    }
}
