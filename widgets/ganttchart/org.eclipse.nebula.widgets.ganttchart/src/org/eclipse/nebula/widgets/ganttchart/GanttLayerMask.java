/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.ganttchart;

import java.util.Arrays;

/** CPU-owned layer visibility bits for the existing GanttComposite (not a widget). */
final class GanttLayerMask {
    private static final long[] EMPTY_WORDS = new long[0];
    private static final int[] EMPTY_IDS = new int[0];
    private long[] words = EMPTY_WORDS;
    private int[] overflow = EMPTY_IDS;
    private int overflowCount;
    private int count;

    boolean contains(int layer) {
        if (layer >= 0 && layer < 4096) {
            int word = layer >>> 6;
            return word < words.length && (words[word] & (1L << (layer & 63))) != 0L;
        }
        return Arrays.binarySearch(overflow, 0, overflowCount, layer) >= 0;
    }

    boolean add(int layer) {
        if (layer >= 0 && layer < 4096) {
            int word = layer >>> 6;
            if (word < words.length && (words[word] & (1L << (layer & 63))) != 0L) return false;
            if (word >= words.length) words = Arrays.copyOf(words, word + 1);
            words[word] |= 1L << (layer & 63);
        } else {
            int index = Arrays.binarySearch(overflow, 0, overflowCount, layer);
            if (index >= 0) return false;
            index = -index - 1;
            if (overflowCount == overflow.length) {
                long capacity = Math.max(8L, (long) overflowCount + (overflowCount >>> 1) + 1L);
                if (capacity > Integer.MAX_VALUE - 8L) throw new OutOfMemoryError("Gantt hidden layers capacity");
                overflow = Arrays.copyOf(overflow, (int) capacity);
            }
            System.arraycopy(overflow, index, overflow, index + 1, overflowCount - index);
            overflow[index] = layer;
            overflowCount++;
        }
        count++;
        return true;
    }

    boolean remove(int layer) {
        if (layer >= 0 && layer < 4096) {
            int word = layer >>> 6;
            if (word >= words.length || (words[word] & (1L << (layer & 63))) == 0L) return false;
            words[word] &= ~(1L << (layer & 63));
        } else {
            int index = Arrays.binarySearch(overflow, 0, overflowCount, layer);
            if (index < 0) return false;
            System.arraycopy(overflow, index + 1, overflow, index, overflowCount - index - 1);
            overflow[--overflowCount] = 0;
            if (overflowCount == 0) overflow = EMPTY_IDS;
        }
        count--;
        return true;
    }

    boolean isEmpty() { return count == 0; }

    void clear() {
        words = EMPTY_WORDS;
        overflow = EMPTY_IDS;
        overflowCount = 0;
        count = 0;
    }
}
