// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.search;

import java.util.Arrays;

/**
 * Deterministic Java byte-search provider.
 *
 * <p>Short or highly repetitive needles use KMP. Longer diverse needles use a precomputed
 * Boyer-Moore-Horspool shift table. Caller arrays are never retained.</p>
 */
public final class JavaAdaptiveSearchEngine implements SearchEngine {
    @Override
    public String name() {
        return "JAVA_ADAPTIVE";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public CompiledSearch compile(byte[] needle) {
        if (needle == null) throw new NullPointerException("needle");
        byte[] snapshot = Arrays.copyOf(needle, needle.length);
        if (snapshot.length == 0) throw new IllegalArgumentException("needle must not be empty");
        return preferKmp(snapshot)
                ? KmpSearch.fromOwnedSnapshot(snapshot)
                : BoyerMooreHorspoolSearch.fromOwnedSnapshot(snapshot);
    }

    private static boolean preferKmp(byte[] needle) {
        if (needle.length < 4) return true;
        int[] frequency = new int[256];
        int max = 0;
        for (byte value : needle) {
            max = Math.max(max, ++frequency[Byte.toUnsignedInt(value)]);
        }
        return max * 2 >= needle.length;
    }
}
