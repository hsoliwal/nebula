// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/**
 * Ordered M3 normalization scope.
 *
 * <p>Promotion is monotonic. A transformation must be proven at FILE before it may be promoted to
 * PACKAGE, then MODULE, PROJECT, and finally REPOSITORY. No caller may skip a scope.</p>
 */
public enum M3AtomPromotionScope {
    FILE,
    PACKAGE,
    MODULE,
    PROJECT,
    REPOSITORY;

    public M3AtomPromotionScope next() {
        return switch (this) {
            case FILE -> PACKAGE;
            case PACKAGE -> MODULE;
            case MODULE -> PROJECT;
            case PROJECT -> REPOSITORY;
            case REPOSITORY -> REPOSITORY;
        };
    }

    public boolean mayPromoteTo(final M3AtomPromotionScope candidate) {
        return candidate != null
                && candidate.ordinal() >= ordinal()
                && candidate.ordinal() <= ordinal() + 1;
    }

    public boolean terminal() {
        return this == REPOSITORY;
    }

    public M3AtomPromotionScope previous() {
        return switch (this) {
            case FILE -> FILE;
            case PACKAGE -> FILE;
            case MODULE -> PACKAGE;
            case PROJECT -> MODULE;
            case REPOSITORY -> PROJECT;
        };
    }
}
