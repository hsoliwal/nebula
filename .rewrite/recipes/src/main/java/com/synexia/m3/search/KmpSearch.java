// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.search;

import com.synexia.job.IProgressMonitor;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.IntConsumer;

/** Primitive Knuth-Morris-Pratt exact byte search with one immutable prefix table. */
final class KmpSearch implements CompiledSearch {
    private final byte[] needle;
    private final int[] prefix;

    KmpSearch(byte[] needle) {
        this(needle, false);
    }

    /** Takes the adaptive compiler's private snapshot without allocating a second array. */
    static KmpSearch fromOwnedSnapshot(byte[] snapshot) {
        return new KmpSearch(snapshot, true);
    }

    private KmpSearch(byte[] needle, boolean owned) {
        if (needle.length == 0) throw new IllegalArgumentException("needle must not be empty");
        this.needle = owned ? needle : Arrays.copyOf(needle, needle.length);
        this.prefix = prefix(this.needle);
    }

    @Override
    public int indexOf(byte[] haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int start = Math.max(0, fromIndex);
        if (needle.length > haystack.length - Math.min(start, haystack.length)) return -1;
        int matched = 0;
        for (int index = start; index < haystack.length; index++) {
            while (matched > 0 && haystack[index] != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (haystack[index] == needle[matched]) matched++;
            if (matched == needle.length) return index - needle.length + 1;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(byte[] haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int maximum = haystack.length - needle.length;
        if (fromIndex < 0 || maximum < 0) return -1;
        int limit = Math.min(fromIndex, maximum);
        int scanEnd = limit + needle.length;
        int matched = 0;
        int last = -1;
        for (int index = 0; index < scanEnd; index++) {
            while (matched > 0 && haystack[index] != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (haystack[index] == needle[matched]) matched++;
            if (matched == needle.length) {
                last = index - needle.length + 1;
                matched = prefix[matched - 1];
            }
        }
        return last;
    }

    @Override
    public int indexOf(ByteBuffer haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        ByteBuffer view = haystack.duplicate();
        int base = view.position();
        int length = view.remaining();
        int start = Math.max(0, fromIndex);
        if (needle.length > length - Math.min(start, length)) return -1;
        int matched = 0;
        for (int index = start; index < length; index++) {
            byte value = view.get(base + index);
            while (matched > 0 && value != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (value == needle[matched]) matched++;
            if (matched == needle.length) return index - needle.length + 1;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(ByteBuffer haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        ByteBuffer view = haystack.duplicate();
        int base = view.position();
        int length = view.remaining();
        int maximum = length - needle.length;
        if (fromIndex < 0 || maximum < 0) return -1;
        int limit = Math.min(fromIndex, maximum);
        int scanEnd = limit + needle.length;
        int matched = 0;
        int last = -1;
        for (int index = 0; index < scanEnd; index++) {
            byte value = view.get(base + index);
            while (matched > 0 && value != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (value == needle[matched]) matched++;
            if (matched == needle.length) {
                last = index - needle.length + 1;
                matched = prefix[matched - 1];
            }
        }
        return last;
    }

    @Override
    public int needleLength() {
        return needle.length;
    }

    @Override
    public String strategy() {
        return "KMP";
    }

    /** Continue the compiled prefix automaton after a hit, retaining overlapping matches. */
    int visitMatches(byte[] haystack, int start, int maxMatches, IntConsumer receiver,
            IProgressMonitor monitor) {
        int count = 0;
        int matched = 0;
        for (int index = start; index < haystack.length; index++) {
            if (((index - start) & 4095) == 0) monitor.checkCanceled();
            while (matched > 0 && haystack[index] != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (haystack[index] == needle[matched]) matched++;
            if (matched == needle.length) {
                receiver.accept(index - needle.length + 1);
                count++;
                monitor.checkCanceled();
                if (count == maxMatches) return count;
                matched = prefix[matched - 1];
            }
        }
        monitor.checkCanceled();
        return count;
    }

    private static int[] prefix(byte[] pattern) {
        int[] table = new int[pattern.length];
        for (int index = 1, matched = 0; index < pattern.length; index++) {
            while (matched > 0 && pattern[index] != pattern[matched]) {
                matched = table[matched - 1];
            }
            if (pattern[index] == pattern[matched]) matched++;
            table[index] = matched;
        }
        return table;
    }
}
