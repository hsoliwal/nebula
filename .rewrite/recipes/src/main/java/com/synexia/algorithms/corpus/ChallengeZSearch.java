// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.util.Objects;

/**
 * Exact first-occurrence search over Java UTF-16 code units using the Z algorithm.
 *
 * <p>The Java implementation does not build {@code pattern + delimiter + text}. Instead it scans a
 * virtual sequence whose separator is the integer value 65536, outside the complete UTF-16 code
 * unit domain. Therefore NUL, '$', isolated surrogates and every other {@code char} value retain
 * exact {@link String#indexOf(String)} semantics. Java remains the oracle; JNI is an optional
 * large-input shadow.</p>
 */
public final class ChallengeZSearch {
    private static final int NATIVE_MIN_TEXT_UNITS = 512;
    private static final int SENTINEL = Character.MAX_VALUE + 1;

    private ChallengeZSearch() {}

    public static int indexOf(String text, String pattern) {
        return indexOf(text, pattern, null);
    }

    public static int indexOf(
            String text, String pattern, IProgressMonitor monitor) {
        String haystack = Objects.requireNonNull(text, "text");
        String needle = Objects.requireNonNull(pattern, "pattern");
        IProgressMonitor checked = ProgressMonitors.nonNull(monitor);
        ProgressMonitors.checkCanceled(checked);
        if (needle.isEmpty()) {
            return 0;
        }
        if (needle.length() > haystack.length()) {
            return -1;
        }

        long virtualLength =
                (long) needle.length() + 1L + haystack.length();
        if (monitor == null
                && ChallengeNativeSearch.nativeEnabled()
                && haystack.length() >= NATIVE_MIN_TEXT_UNITS
                && needle.length() >= 2
                && virtualLength <= Integer.MAX_VALUE) {
            try {
                int result = nativeIndexOf(haystack, needle);
                requireResult(haystack, needle, result);
                return result;
            } catch (UnsatisfiedLinkError olderLibrary) {
                // ABI 1 predates some additive search symbols. Java remains the exact fallback.
            }
        }

        return indexOfJava(haystack, needle, checked);
    }

    public static int indexOfJava(String text, String pattern) {
        return indexOfJava(
                Objects.requireNonNull(text, "text"),
                Objects.requireNonNull(pattern, "pattern"),
                IProgressMonitor.noop());
    }

    static boolean verifyNativeIfPresent() {
        try {
            String[] texts = {
                "",
                "a",
                "abc abc abcd",
                "a$bc$abc",
                "a\0b\0c",
                "\ud800x\udfffy",
                "😀alpha😀beta",
                "x".repeat(1024) + "needle"
            };
            String[] patterns = {
                "",
                "a",
                "$",
                "abc",
                "\0b",
                "\ud800",
                "\udfff",
                "😀beta",
                "needle",
                "missing"
            };
            for (String text : texts) {
                for (String pattern : patterns) {
                    int expected = text.indexOf(pattern);
                    int actual = nativeIndexOf(text, pattern);
                    if (actual != expected) {
                        throw new LinkageError(
                                "native Z-search oracle probe failed: expected="
                                        + expected + " actual=" + actual);
                    }
                }
            }
            return true;
        } catch (UnsatisfiedLinkError olderLibrary) {
            return false;
        }
    }

    private static int indexOfJava(
            String haystack, String needle, IProgressMonitor monitor) {
        int patternLength = needle.length();
        int textLength = haystack.length();
        if (patternLength == 0) {
            return 0;
        }
        if (patternLength > textLength) {
            return -1;
        }

        long totalLong = (long) patternLength + 1L + textLength;
        if (totalLong > Integer.MAX_VALUE) {
            // An int[] Z lane cannot represent this virtual sequence. Preserve exact semantics
            // without allocating or flattening another combined String.
            ProgressMonitors.checkCanceled(monitor);
            int result = haystack.indexOf(needle);
            ProgressMonitors.checkCanceled(monitor);
            return result;
        }

        int total = (int) totalLong;
        int[] z = new int[total];
        int left = 0;
        int right = 0;
        long comparisons = 0L;
        for (int index = 1; index < total; index++) {
            if ((index & 1023) == 0) {
                ProgressMonitors.checkCanceled(monitor);
            }
            if (index <= right) {
                z[index] = Math.min(right - index + 1, z[index - left]);
            }

            while (z[index] < total - index
                    && virtualUnit(needle, haystack, z[index])
                            == virtualUnit(needle, haystack, index + z[index])) {
                z[index]++;
                if ((++comparisons & 1023L) == 0L) {
                    ProgressMonitors.checkCanceled(monitor);
                }
            }

            int candidateRight = index + z[index] - 1;
            if (candidateRight > right) {
                left = index;
                right = candidateRight;
            }
            if (index > patternLength && z[index] >= patternLength) {
                ProgressMonitors.checkCanceled(monitor);
                return index - patternLength - 1;
            }
        }
        ProgressMonitors.checkCanceled(monitor);
        return -1;
    }

    private static int virtualUnit(
            String pattern, String text, int virtualIndex) {
        int patternLength = pattern.length();
        if (virtualIndex < patternLength) {
            return pattern.charAt(virtualIndex);
        }
        if (virtualIndex == patternLength) {
            return SENTINEL;
        }
        return text.charAt(virtualIndex - patternLength - 1);
    }

    private static void requireResult(String text, String pattern, int result) {
        if (result < -1) {
            throw new IllegalStateException("native Z-search index outside contract");
        }
        if (result >= 0) {
            if (pattern.length() > text.length()
                    || result > text.length() - pattern.length()
                    || !text.startsWith(pattern, result)) {
                throw new IllegalStateException("native Z-search false positive");
            }
        }
    }

    private static native int nativeIndexOf(String text, String pattern);
}
