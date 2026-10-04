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
 * Packed identity index: three parallel arrays, never one counter object per key.
 * Hashes only nominate a slot; reference equality is the exact match test.
 * Occupied keys are not removed while counters are consumed, preserving probes.
 * Capacity follows distinct non-null identities, not duplicate occurrence count.
 * This table is private to one invocation and is not thread safe.
 */
final class GridIdentityOccurrenceTable<T> extends AbstractGridIdentityOccurrenceCounter<T> {
    private Object[] keys = new Object[16];
    private int[] remaining = new int[16];
    private int[] matched = new int[16];
    private int distinct;
    private int nullRemaining;
    private int nullMatched;

    @Override
    public void addPrevious(T value) {
        if (value == null) {
            nullRemaining++;
            return;
        }
        int slot = slot(value, keys);
        if (keys[slot] != null) {
            remaining[slot]++;
            return;
        }
        if (distinct >= keys.length - (keys.length >>> 2)) {
            grow();
            slot = slot(value, keys);
        }
        keys[slot] = value;
        remaining[slot] = 1;
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
        int slot = slot(value, keys);
        if (keys[slot] == null || remaining[slot] == 0) {
            return false;
        }
        remaining[slot]--;
        matched[slot]++;
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
        int slot = slot(value, keys);
        if (keys[slot] == null || matched[slot] == 0) {
            return false;
        }
        matched[slot]--;
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
        if (keys.length >= (1 << 30)) {
            throw new OutOfMemoryError("Visible-range identity index capacity exceeded");
        }
        int capacity = keys.length << 1;
        Object[] newKeys = new Object[capacity];
        int[] newRemaining = new int[capacity];
        int[] newMatched = new int[capacity];
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                int destination = slot(key, newKeys);
                newKeys[destination] = key;
                newRemaining[destination] = remaining[i];
                newMatched[destination] = matched[i];
            }
        }
        keys = newKeys;
        remaining = newRemaining;
        matched = newMatched;
    }
}
