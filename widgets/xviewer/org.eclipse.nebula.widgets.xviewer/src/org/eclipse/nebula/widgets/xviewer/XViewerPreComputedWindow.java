/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.xviewer;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * The resident values of one {@link IXViewerPreComputedColumn} while its {@link XViewer} bounds precomputed values (see
 * {@link XViewer#setBoundedPreCompute(boolean)}). The viewer keeps it beside the column, whose own
 * {@code preComputedValueMap} it leaves alone, and {@link IXViewerPreComputedColumn#populateCachedValues} fills it for
 * the rows asked for. It holds at most {@link #SLOTS} values for non-null keys by default, plus one for the
 * {@code null} key: the rows the native tree shows, their overscan and recently used rows. When a pass keeps more than
 * half that many rows (a tall tree), {@link #ensureCapacity(int)} grows it to twice those rows; its size follows the
 * viewport, never the data. A value that leaves is computed again by the column if its row is asked for again.
 * <p>
 * The values live in parent-owned lanes (a {@code long[]} of keys, a {@code String[]} of values and an {@code int[]}
 * of use ticks), not in one entry object per row. A key is looked up in a window of {@link #PROBE} slots after its
 * hash slot; a new key takes the first free slot of that window, else the least recently used slot of that window,
 * so a value can leave before all {@link #SLOTS} slots are in use. A {@code null} value is a resident value, as in
 * {@link java.util.HashMap}; so is the {@code null} key. {@link #entrySet()} hands out fresh entries that write
 * through to the slot.
 * </p>
 */
final class XViewerPreComputedWindow extends AbstractMap<Long, String> {
   /** Values kept per precomputed column; a power of two. */
   static final int SLOTS = 1024;
   private static final int SHIFT = Long.SIZE - Integer.numberOfTrailingZeros(SLOTS);
   private static final int MASK = SLOTS - 1;
   private static final int PROBE = 8;

   private long[] keys = new long[SLOTS];
   private String[] values = new String[SLOTS];
   /** Last use of each slot; 0 marks a free slot. */
   private int[] ticks = new int[SLOTS];
   private int mask = MASK;
   private int shift = SHIFT;
   private int tick;
   private int size;
   private boolean hasNullKey;
   private String nullKeyValue;
   private int modCount;

   private int home(long key) {
      return (int) ((key * 0x9E3779B97F4A7C15L) >>> shift);
   }

   /**
    * Grows the lanes to a power of two of at least twice {@code count} slots, keeping the resident values and their
    * order of use; never shrinks.
    */
   void ensureCapacity(int count) {
      int target = Math.max(SLOTS, 2 * count);
      int slots = Integer.highestOneBit(target - 1) << 1;
      if (slots <= ticks.length) {
         return;
      }
      long[] oldKeys = keys;
      String[] oldValues = values;
      int[] oldTicks = ticks;
      Integer[] order = new Integer[oldTicks.length];
      int resident = 0;
      for (int slot = 0; slot < oldTicks.length; slot++) {
         if (oldTicks[slot] != 0) {
            order[resident++] = Integer.valueOf(slot);
         }
      }
      Arrays.sort(order, 0, resident, (a, b) -> Integer.compare(oldTicks[a.intValue()], oldTicks[b.intValue()]));
      keys = new long[slots];
      values = new String[slots];
      ticks = new int[slots];
      mask = slots - 1;
      shift = Long.SIZE - Integer.numberOfTrailingZeros(slots);
      size = 0;
      for (int i = 0; i < resident; i++) {
         int slot = order[i].intValue();
         put(Long.valueOf(oldKeys[slot]), oldValues[slot]);
      }
      modCount++;
   }

   private int slotOf(long key) {
      int home = home(key);
      for (int i = 0; i < PROBE; i++) {
         int slot = home + i & mask;
         if (ticks[slot] != 0 && keys[slot] == key) {
            return slot;
         }
      }
      return -1;
   }

   private int nextTick() {
      if (tick == Integer.MAX_VALUE) {
         // restart the clock after 2^31 uses: every resident value becomes equally old
         for (int slot = 0; slot < ticks.length; slot++) {
            if (ticks[slot] != 0) {
               ticks[slot] = 1;
            }
         }
         tick = 1;
      }
      return ++tick;
   }

   @Override
   public int size() {
      return hasNullKey ? size + 1 : size;
   }

   @Override
   public boolean isEmpty() {
      return size() == 0;
   }

   @Override
   public boolean containsKey(Object key) {
      if (key == null) {
         return hasNullKey;
      }
      return key instanceof Long && slotOf(((Long) key).longValue()) >= 0;
   }

   @Override
   public String get(Object key) {
      if (key == null) {
         return nullKeyValue;
      }
      if (!(key instanceof Long)) {
         return null;
      }
      int slot = slotOf(((Long) key).longValue());
      if (slot < 0) {
         return null;
      }
      ticks[slot] = nextTick();
      return values[slot];
   }

   @Override
   public String put(Long key, String value) {
      if (key == null) {
         String old = nullKeyValue;
         if (!hasNullKey) {
            hasNullKey = true;
            modCount++;
         }
         nullKeyValue = value;
         return old;
      }
      long k = key.longValue();
      int home = home(k);
      int free = -1;
      int victim = -1;
      for (int i = 0; i < PROBE; i++) {
         int slot = home + i & mask;
         if (ticks[slot] == 0) {
            if (free < 0) {
               free = slot;
            }
         } else if (keys[slot] == k) {
            String old = values[slot];
            values[slot] = value;
            ticks[slot] = nextTick();
            return old;
         } else if (victim < 0 || ticks[slot] < ticks[victim]) {
            victim = slot;
         }
      }
      int slot = free;
      if (slot < 0) {
         // the window is full: the least recently used value leaves and is recomputed if asked for again
         slot = victim;
         size--;
      }
      keys[slot] = k;
      values[slot] = value;
      ticks[slot] = nextTick();
      size++;
      modCount++;
      return null;
   }

   @Override
   public String remove(Object key) {
      if (key == null) {
         String old = nullKeyValue;
         if (hasNullKey) {
            hasNullKey = false;
            nullKeyValue = null;
            modCount++;
         }
         return old;
      }
      if (!(key instanceof Long)) {
         return null;
      }
      int slot = slotOf(((Long) key).longValue());
      return slot < 0 ? null : free(slot);
   }

   private String free(int slot) {
      String old = values[slot];
      values[slot] = null;
      ticks[slot] = 0;
      size--;
      modCount++;
      return old;
   }

   @Override
   public void clear() {
      Arrays.fill(values, null);
      Arrays.fill(ticks, 0);
      size = 0;
      tick = 0;
      hasNullKey = false;
      nullKeyValue = null;
      modCount++;
   }

   @Override
   public Set<Map.Entry<Long, String>> entrySet() {
      return new AbstractSet<Map.Entry<Long, String>>() {
         @Override
         public int size() {
            return XViewerPreComputedWindow.this.size();
         }

         @Override
         public void clear() {
            XViewerPreComputedWindow.this.clear();
         }

         @Override
         public Iterator<Map.Entry<Long, String>> iterator() {
            return new Slots();
         }
      };
   }

   /** Walks the null key, then the occupied slots; each entry is a fresh caller-owned view of its slot. */
   private final class Slots implements Iterator<Map.Entry<Long, String>> {
      private int expected = modCount;
      /** -1 is the null key; 0..SLOTS-1 are slots. */
      private int next = hasNullKey ? -1 : advance(0);
      private int last = Integer.MIN_VALUE;

      private int advance(int from) {
         int slot = from;
         while (slot < ticks.length && ticks[slot] == 0) {
            slot++;
         }
         return slot;
      }

      @Override
      public boolean hasNext() {
         return next < ticks.length;
      }

      @Override
      public Map.Entry<Long, String> next() {
         if (modCount != expected) {
            throw new java.util.ConcurrentModificationException();
         }
         if (!hasNext()) {
            throw new NoSuchElementException();
         }
         last = next;
         next = advance(next + 1);
         if (last < 0) {
            return new AbstractMap.SimpleEntry<Long, String>(null, nullKeyValue) {
               private static final long serialVersionUID = 1L;

               @Override
               public String setValue(String value) {
                  if (hasNullKey) {
                     nullKeyValue = value;
                  }
                  return super.setValue(value);
               }
            };
         }
         final int slot = last;
         final long key = keys[slot];
         return new AbstractMap.SimpleEntry<Long, String>(Long.valueOf(key), values[slot]) {
            private static final long serialVersionUID = 1L;

            @Override
            public String setValue(String value) {
               if (ticks[slot] != 0 && keys[slot] == key) {
                  values[slot] = value;
               }
               return super.setValue(value);
            }
         };
      }

      @Override
      public void remove() {
         if (last == Integer.MIN_VALUE) {
            throw new IllegalStateException();
         }
         if (modCount != expected) {
            throw new java.util.ConcurrentModificationException();
         }
         if (last < 0) {
            XViewerPreComputedWindow.this.remove(null);
         } else {
            free(last);
         }
         last = Integer.MIN_VALUE;
         expected = modCount;
      }
   }
}
