/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.nebula.widgets.grid;

/**
 * Packed identity index: two parallel arrays, never one counter object per key.
 * Hashes only nominate a slot; reference equality is the exact match test.
 * Occupied keys are not removed while counters are consumed, preserving probes.
 * Capacity follows distinct non-null identities, not duplicate occurrence count.
 * No backing arrays are allocated until a non-null previous identity is added.
 * This table is private to one invocation and is not thread safe.
 */
final class GridIdentityOccurrenceTable<T> extends AbstractGridIdentityOccurrenceCounter<T> {
    private Object[] keys;
    private static final long LOW_WORD = 0xffff_ffffL;
    // Low word: unmatched previous occurrences. High word: matched occurrences to skip.
    private long[] counts;
    private int distinct;
    private int nullRemaining;
    private int nullMatched;

    @Override
    public void addPrevious(T value) {
        if (value == null) {
            nullRemaining++;
            return;
        }
        if (keys == null) {
            grow();
        }
        int slot = slot(value, keys);
        if (keys[slot] != null) {
            counts[slot] = (counts[slot] & ~LOW_WORD) | ((counts[slot] + 1L) & LOW_WORD);
            return;
        }
        if (distinct >= keys.length - (keys.length >>> 2)) {
            grow();
            slot = slot(value, keys);
        }
        keys[slot] = value;
        counts[slot] = 1L;
        distinct++;
    }

    @Override
    public boolean matchCurrent(T value) {
        if (value == null) {
            if (nullRemaining == 0) {
                return false;
            }
            nullRemaining--;
            nullMatched++;
            return true;
        }
        if (keys == null) {
            return false;
        }
        int slot = slot(value, keys);
        if (keys[slot] == null || (counts[slot] & LOW_WORD) == 0L) {
            return false;
        }
        // Lower word is nonzero: subtract one there and add one to the high word.
        counts[slot] += LOW_WORD;
        return true;
    }

    @Override
    public boolean skipMatchedPrevious(T value) {
        if (value == null) {
            if (nullMatched == 0) {
                return false;
            }
            nullMatched--;
            return true;
        }
        if (keys == null) {
            return false;
        }
        int slot = slot(value, keys);
        if (keys[slot] == null || (counts[slot] >>> 32) == 0L) {
            return false;
        }
        counts[slot] -= 1L << 32;
        return true;
    }

    private static int slot(Object key, Object[] table) {
        int hash = System.identityHashCode(key);
        int mask = table.length - 1;
        int index = (hash ^ (hash >>> 16)) & mask;
        while (table[index] != null && table[index] != key) {
            index = (index + 1) & mask;
        }
        return index;
    }

    private void grow() {
        if (keys != null && keys.length >= (1 << 30)) {
            throw new OutOfMemoryError("Visible-range identity index capacity exceeded");
        }
        int capacity = keys == null ? 16 : keys.length << 1;
        Object[] newKeys = new Object[capacity];
        long[] newCounts = new long[capacity];
        for (int i = 0; keys != null && i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                int destination = slot(key, newKeys);
                newKeys[destination] = key;
                newCounts[destination] = counts[i];
            }
        }
        keys = newKeys;
        counts = newCounts;
    }
}
