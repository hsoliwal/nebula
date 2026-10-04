// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.algorithms.string.StringsRabinKarp;
import com.synexia.job.IProgressMonitor;
import java.util.Objects;

/**
 * Reusable UTF-16 Rabin-Karp first-occurrence search.
 *
 * <p>{@link StringsRabinKarp} remains the Java oracle. The optional JNI path operates on Java
 * UTF-16 code units and verifies every hash hit exactly, preserving {@link String#indexOf(String)}
 * style code-unit offsets and collision safety.</p>
 */
public final class ChallengeRabinKarpSearch {
    private static final int NATIVE_MIN_TEXT_UNITS = 512;

    private ChallengeRabinKarpSearch() {}

    public static int indexOf(String text, String pattern) {
        return indexOf(text, pattern, null);
    }

    public static int indexOf(
            String text, String pattern, IProgressMonitor monitor) {
        String haystack = Objects.requireNonNull(text, "text");
        String needle = Objects.requireNonNull(pattern, "pattern");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);

        if (monitor == null
                && ChallengeNativeSearch.nativeEnabled()
                && haystack.length() >= NATIVE_MIN_TEXT_UNITS
                && needle.length() >= 2) {
            int result = nativeIndexOf(haystack, needle);
            ProgressMonitors.checkCanceled(checked);
            requireResult(haystack, needle, result);
            return result;
        }
        return StringsRabinKarp.rabinKarp(haystack, needle, checked);
    }

    public static int indexOfJava(String text, String pattern) {
        return StringsRabinKarp.rabinKarp(
                Objects.requireNonNull(text, "text"),
                Objects.requireNonNull(pattern, "pattern"));
    }

    static void verifyNative() {
        String[] texts = {
            "",
            "a",
            "abc abc abcd",
            "Hello 世界 World",
            "x".repeat(1024) + "needle",
            "😀alpha😀beta"
        };
        String[] patterns = {
            "",
            "a",
            "abc",
            "abcd",
            "世界",
            "needle",
            "😀beta",
            "missing"
        };
        for (String text : texts) {
            for (String pattern : patterns) {
                int expected = indexOfJava(text, pattern);
                int actual = nativeIndexOf(text, pattern);
                if (actual != expected) {
                    throw new LinkageError(
                            "native Rabin-Karp oracle probe failed: expected="
                                    + expected + " actual=" + actual);
                }
            }
        }
    }

    private static void requireResult(String text, String pattern, int result) {
        if (result < -1) {
            throw new IllegalStateException("native Rabin-Karp index outside contract");
        }
        if (result >= 0) {
            if (pattern.length() > text.length()
                    || result > text.length() - pattern.length()) {
                throw new IllegalStateException("native Rabin-Karp index outside contract");
            }
            for (int index = 0; index < pattern.length(); index++) {
                if (text.charAt(result + index) != pattern.charAt(index)) {
                    throw new IllegalStateException("native Rabin-Karp false positive");
                }
            }
        }
    }

    private static native int nativeIndexOf(String text, String pattern);
}
