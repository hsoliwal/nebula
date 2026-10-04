// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.shapes;

import java.util.Objects;
import java.util.Set;

/** Machine-readable invariant for one canonical algorithm implementation. */
public record AlgorithmDescriptor(
        String id,
        AlgorithmPurpose purpose,
        AlgorithmShape shape,
        Set<PatternView> views,
        String timeComplexity,
        String spaceComplexity,
        boolean deterministic,
        boolean parallelizable,
        boolean nativeFriendly,
        boolean tornadoFriendly) {

    public AlgorithmDescriptor {
        id = require(id, "id");
        purpose = Objects.requireNonNull(purpose, "purpose");
        shape = Objects.requireNonNull(shape, "shape");
        views = Set.copyOf(Objects.requireNonNull(views, "views"));
        timeComplexity = require(timeComplexity, "timeComplexity");
        spaceComplexity = require(spaceComplexity, "spaceComplexity");
    }

    private static String require(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
