/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.synexia.algorithms.string;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Strings: Rabin-Karp Pattern Matching.
 *
 * @since 1.0.0
 */
public final class StringsRabinKarp {

    private StringsRabinKarp() {}

    // --- RABIN-KARP ---

    /** Rabin-Karp pattern matching using rolling hash - O(n + m) average. */
    public static int rabinKarp(String text, String pattern) {
        if (pattern.isEmpty()) return 0;
        int n = text.length(), m = pattern.length();
        if (m > n) return -1;

        long prime = 101;
        long mod = 1_000_000_007;

        // h = pow(prime, m-1) % mod
        long h = IntStream.range(0, m - 1).mapToLong(i -> prime).reduce(1L, (acc, p) -> (acc * p) % mod);

        // Initial hash values
        long patternHash = IntStream.range(0, m)
                .mapToLong(i -> pattern.charAt(i))
                .reduce(0L, (hash, c) -> (hash * prime + c) % mod);
        long[] textHash = {
            IntStream.range(0, m).mapToLong(i -> text.charAt(i)).reduce(0L, (hash, c) -> (hash * prime + c) % mod)
        };

        // Slide pattern over text
        int[] result = {-1};
        IntStream.rangeClosed(0, n - m).filter(i -> result[0] == -1).forEach(i -> {
            if (patternHash == textHash[0] && text.substring(i, i + m).equals(pattern)) {
                result[0] = i;
            } else if (i < n - m) {
                textHash[0] = (prime * (textHash[0] - text.charAt(i) * h) + text.charAt(i + m)) % mod;
                if (textHash[0] < 0) textHash[0] += mod;
            }
        });
        return result[0];
    }

    /** Rabin-Karp all occurrences. */
    public static List<Integer> rabinKarpAll(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        if (pattern.isEmpty()) return result;
        int n = text.length(), m = pattern.length();
        if (m > n) return result;

        long prime = 101;
        long mod = 1_000_000_007;

        long h = IntStream.range(0, m - 1).mapToLong(i -> prime).reduce(1L, (acc, p) -> (acc * p) % mod);

        long patternHash = IntStream.range(0, m)
                .mapToLong(i -> pattern.charAt(i))
                .reduce(0L, (hash, c) -> (hash * prime + c) % mod);
        long[] textHash = {
            IntStream.range(0, m).mapToLong(i -> text.charAt(i)).reduce(0L, (hash, c) -> (hash * prime + c) % mod)
        };

        IntStream.rangeClosed(0, n - m).forEach(i -> {
            if (patternHash == textHash[0] && text.substring(i, i + m).equals(pattern)) {
                result.add(i);
            }
            if (i < n - m) {
                textHash[0] = (prime * (textHash[0] - text.charAt(i) * h) + text.charAt(i + m)) % mod;
                if (textHash[0] < 0) textHash[0] += mod;
            }
        });
        return result;
    }

    /** Progress-aware compatibility entrypoint. */
    public static int rabinKarp(String text, String pattern, IProgressMonitor monitor) {
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        checked.beginTask("rabinKarp", 1L);
        try {
            var result = rabinKarp(text, pattern);
            ProgressMonitors.checkCanceled(checked);
            checked.worked(1L);
            return result;
        } finally {
            checked.done();
        }
    }

    /** Progress-aware compatibility entrypoint. */
    public static List<Integer> rabinKarpAll(String text, String pattern, IProgressMonitor monitor) {
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        checked.beginTask("rabinKarpAll", 1L);
        try {
            var result = rabinKarpAll(text, pattern);
            ProgressMonitors.checkCanceled(checked);
            checked.worked(1L);
            return result;
        } finally {
            checked.done();
        }
    }

}
