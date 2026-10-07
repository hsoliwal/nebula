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
    private static final long HIGH_WORD = 0xffff_ffff_0000_0000L;
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
        Object[] table = keys;
        if (table == null) {
            grow();
            table = keys;
        }
        int slot = slot(value, table);
        if (table[slot] != null) {
            long count = counts[slot];
            counts[slot] = (count & HIGH_WORD) | ((count + 1L) & LOW_WORD);
            return;
        }
        if (distinct >= table.length - (table.length >>> 2)) {
            grow();
            table = keys;
            slot = slot(value, table);
        }
        table[slot] = value;
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
        Object[] table = keys;
        if (table == null) {
            return false;
        }
        int slot = slot(value, table);
        if (table[slot] == null) {
            return false;
        }
        long count = counts[slot];
        if ((count & LOW_WORD) == 0L) {
            return false;
        }
        // Lower word is nonzero: subtract one there and add one to the high word.
        counts[slot] = count + LOW_WORD;
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
        Object[] table = keys;
        if (table == null) {
            return false;
        }
        int slot = slot(value, table);
        if (table[slot] == null) {
            return false;
        }
        long count = counts[slot];
        if ((count >>> 32) == 0L) {
            return false;
        }
        counts[slot] = count - (1L << 32);
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
        Object[] oldKeys = keys;
        if (oldKeys != null && oldKeys.length >= (1 << 30)) {
            throw new OutOfMemoryError("Visible-range identity index capacity exceeded");
        }
        int capacity = oldKeys == null ? 16 : oldKeys.length << 1;
        Object[] newKeys = new Object[capacity];
        long[] newCounts = new long[capacity];
        if (oldKeys != null) {
            long[] oldCounts = counts;
            for (int i = 0; i < oldKeys.length; i++) {
                Object key = oldKeys[i];
                if (key != null) {
                    int destination = slot(key, newKeys);
                    newKeys[destination] = key;
                    newCounts[destination] = oldCounts[i];
                }
            }
        }
        keys = newKeys;
        counts = newCounts;
    }
}
