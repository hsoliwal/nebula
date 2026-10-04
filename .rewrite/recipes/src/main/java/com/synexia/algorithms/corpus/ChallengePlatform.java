// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.Locale;
import java.util.Objects;

/** Supported challenge-corpus platforms. */
public enum ChallengePlatform {
    LEETCODE,
    HACKERRANK,
    GEEKSFORGEEKS,
    OTHER;

    public static ChallengePlatform from(String value) {
        String normalized = Objects.requireNonNull(value, "value")
                .trim()
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LEETCODE" -> LEETCODE;
            case "HACKERRANK" -> HACKERRANK;
            case "GEEKSFORGEEKS", "GEEKSFORGEEKS.COM", "GEEKS FOR GEEKS", "GFG"
                    -> GEEKSFORGEEKS;
            default -> OTHER;
        };
    }
}
