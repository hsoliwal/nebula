// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.search;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** Primitive Boyer-Moore-Horspool exact byte search with one 256-entry shift table. */
final class BoyerMooreHorspoolSearch implements CompiledSearch {
    private final byte[] needle;
    private final int[] shifts = new int[256];
    private final int[] reverseShifts = new int[256];

    BoyerMooreHorspoolSearch(byte[] needle) {
        this(needle, false);
    }

    /** Takes the adaptive compiler's private snapshot without allocating a second array. */
    static BoyerMooreHorspoolSearch fromOwnedSnapshot(byte[] snapshot) {
        return new BoyerMooreHorspoolSearch(snapshot, true);
    }

    private BoyerMooreHorspoolSearch(byte[] needle, boolean owned) {
        if (needle.length == 0) throw new IllegalArgumentException("needle must not be empty");
        this.needle = owned ? needle : Arrays.copyOf(needle, needle.length);
        Arrays.fill(shifts, this.needle.length);
        Arrays.fill(reverseShifts, this.needle.length);
        for (int index = 0; index < this.needle.length - 1; index++) {
            shifts[Byte.toUnsignedInt(this.needle[index])] = this.needle.length - 1 - index;
        }
        for (int index = this.needle.length - 1; index > 0; index--) {
            reverseShifts[Byte.toUnsignedInt(this.needle[index])] = index;
        }
    }

    @Override
    public int indexOf(byte[] haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int start = Math.max(0, fromIndex);
        if (start > haystack.length - needle.length) return -1;

        int cursor = start + needle.length - 1;
        while (cursor < haystack.length) {
            int pattern = needle.length - 1;
            int candidate = cursor;
            while (pattern >= 0 && haystack[candidate] == needle[pattern]) {
                candidate--;
                pattern--;
            }
            if (pattern < 0) return candidate + 1;
            cursor += shifts[Byte.toUnsignedInt(haystack[cursor])];
        }
        return -1;
    }

    @Override
    public int lastIndexOf(byte[] haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int maximum = haystack.length - needle.length;
        if (fromIndex < 0 || maximum < 0) return -1;
        int candidate = Math.min(fromIndex, maximum);
        while (candidate >= 0) {
            int pattern = 0;
            int text = candidate;
            while (pattern < needle.length && haystack[text] == needle[pattern]) {
                text++;
                pattern++;
            }
            if (pattern == needle.length) return candidate;
            candidate -= reverseShifts[Byte.toUnsignedInt(haystack[candidate])];
        }
        return -1;
    }

    @Override
    public int indexOf(ByteBuffer haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        ByteBuffer view = haystack.duplicate();
        int base = view.position();
        int length = view.remaining();
        int start = Math.max(0, fromIndex);
        if (start > length - needle.length) return -1;
        int cursor = start + needle.length - 1;
        while (cursor < length) {
            int pattern = needle.length - 1;
            int candidate = cursor;
            while (pattern >= 0 && view.get(base + candidate) == needle[pattern]) {
                candidate--;
                pattern--;
            }
            if (pattern < 0) return candidate + 1;
            cursor += shifts[Byte.toUnsignedInt(view.get(base + cursor))];
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
        int candidate = Math.min(fromIndex, maximum);
        while (candidate >= 0) {
            int pattern = 0;
            int text = candidate;
            while (pattern < needle.length
                    && view.get(base + text) == needle[pattern]) {
                text++;
                pattern++;
            }
            if (pattern == needle.length) return candidate;
            candidate -= reverseShifts[Byte.toUnsignedInt(view.get(base + candidate))];
        }
        return -1;
    }

    @Override
    public int needleLength() {
        return needle.length;
    }

    @Override
    public String strategy() {
        return "BOYER_MOORE_HORSPOOL";
    }
}
