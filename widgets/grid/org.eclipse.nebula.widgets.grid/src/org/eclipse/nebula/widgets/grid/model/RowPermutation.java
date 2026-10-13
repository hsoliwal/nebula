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
package org.eclipse.nebula.widgets.grid.model;

import java.util.Arrays;

/**
 * Packed sort and filter index over {@code modelCount} model rows.
 *
 * <p>A model row is the caller's data index ({@code 0..modelCount-1}). A view position is a
 * displayed row ({@code 0..getViewCount()-1}). The permutation maps one to the other with
 * plain {@code int} arrays and a {@code long[]} filter bitset; it never creates a row object.</p>
 *
 * <ul>
 * <li>Sort: an {@code int[]} order sorted in place (no allocation after the first sort of a
 * given model size). Ties are broken by the model row, so the order is total and equals a
 * stable sort of the model order.</li>
 * <li>Filter: one bit per model row; the visible rows are compacted, in sort order, into a
 * reused {@code int[]}.</li>
 * <li>{@link #viewIndex(int)} builds the inverse lazily, once per change.</li>
 * </ul>
 *
 * <p>Pure Java with no widget dependency, so Grid, Table and Tree can share it and GWT can
 * compile it (no {@code int} overflow reliance; the bitset is {@code long[]}).</p>
 *
 * <p>Not thread safe; use it from the thread that owns the widget it is installed on.</p>
 *
 * @since 1.2
 */
public final class RowPermutation implements RowView {

	private static final int SORTED = 1;
	private static final int FILTERED = 1 << 1;
	private static final int VIEW_VALID = 1 << 2;
	private static final int INVERSE_VALID = 1 << 3;
	private static final int VERSION_MASK = 0x7fffffff;
	private static final int[] NO_INTS = new int[0];
	private static final long[] NO_LONGS = new long[0];

	private int modelCount;
	private int flags;
	private int version;
	private int viewCount;
	private int[] order = NO_INTS;
	private long[] bits = NO_LONGS;
	private int[] visible = NO_INTS;
	private int[] inverse = NO_INTS;
	private static final int RADIX = 1 << 11;
	private int[] radixCount = NO_INTS;

	/**
	 * Creates an identity permutation over no rows.
	 */
	public RowPermutation() {
		this(0);
	}

	/**
	 * Creates an identity permutation (unsorted, unfiltered).
	 *
	 * @param modelCount number of model rows
	 * @throws IllegalArgumentException if {@code modelCount < 0}
	 */
	public RowPermutation(final int modelCount) {
		setModelCount(modelCount);
	}

	/**
	 * @return the number of model rows
	 */
	@Override
	public int getModelCount() {
		return modelCount;
	}

	/**
	 * Sets the number of model rows and resets sort and filter to the identity. Arrays are
	 * kept when they are large enough.
	 *
	 * @param count number of model rows
	 * @throws IllegalArgumentException if {@code count < 0}
	 */
	public void setModelCount(final int count) {
		if (count < 0) {
			throw new IllegalArgumentException("negative model count: " + count);
		}
		modelCount = count;
		flags = 0;
		viewCount = count;
		changed();
	}

	/**
	 * @return the number of displayed rows (model rows passing the filter)
	 */
	@Override
	public int getViewCount() {
		if ((flags & FILTERED) == 0) {
			return modelCount;
		}
		ensureView();
		return viewCount;
	}

	/**
	 * @param viewIndex a displayed row
	 * @return the model row shown at {@code viewIndex}
	 * @throws IndexOutOfBoundsException if {@code viewIndex} is not a displayed row
	 */
	@Override
	public int modelRow(final int viewIndex) {
		if ((flags & FILTERED) != 0) {
			ensureView();
			checkIndex(viewIndex, viewCount);
			return visible[viewIndex];
		}
		checkIndex(viewIndex, modelCount);
		return (flags & SORTED) != 0 ? order[viewIndex] : viewIndex;
	}

	/**
	 * @param modelRow a model row
	 * @return the displayed row of {@code modelRow}, or {@code -1} if it is filtered out
	 * @throws IndexOutOfBoundsException if {@code modelRow} is not a model row
	 */
	@Override
	public int viewIndex(final int modelRow) {
		checkIndex(modelRow, modelCount);
		if ((flags & (SORTED | FILTERED)) == 0) {
			return modelRow;
		}
		ensureInverse();
		return inverse[modelRow];
	}

	/**
	 * Copies displayed rows to {@code target} as model rows.
	 *
	 * @param fromView first displayed row
	 * @param target   destination
	 * @param offset   first destination slot
	 * @param length   maximum number of rows to copy
	 * @return the number of rows copied
	 */
	public int copyModelRows(final int fromView, final int[] target, final int offset, final int length) {
		final int count = getViewCount();
		if (fromView < 0 || offset < 0 || length < 0 || offset > target.length) {
			throw new IndexOutOfBoundsException("fromView=" + fromView + ", offset=" + offset + ", length=" + length);
		}
		final int n = Math.max(0, Math.min(length, Math.min(count - fromView, target.length - offset)));
		if ((flags & FILTERED) != 0) {
			System.arraycopy(visible, fromView, target, offset, n);
		} else if ((flags & SORTED) != 0) {
			System.arraycopy(order, fromView, target, offset, n);
		} else {
			for (int i = 0; i < n; i++) {
				target[offset + i] = fromView + i;
			}
		}
		return n;
	}

	/**
	 * @return {@code true} if a sort order is set
	 */
	public boolean isSorted() {
		return (flags & SORTED) != 0;
	}

	/**
	 * @return {@code true} if a filter is set
	 */
	public boolean isFiltered() {
		return (flags & FILTERED) != 0;
	}

	/**
	 * @return {@code true} if every model row is displayed at its own index
	 */
	public boolean isIdentity() {
		return (flags & (SORTED | FILTERED)) == 0;
	}

	/**
	 * Sorts the model rows by {@code comparator}, ties broken by model row.
	 *
	 * @param comparator row comparator
	 * @throws NullPointerException if {@code comparator} is null
	 */
	public void sort(final RowComparator comparator) {
		if (comparator == null) {
			throw new NullPointerException("comparator");
		}
		final int[] o = orderArray();
		for (int i = 0; i < modelCount; i++) {
			o[i] = i;
		}
		IntPermutationSort.sort(o, 0, modelCount, comparator);
		flags |= SORTED;
		changed();
	}

	/**
	 * Sorts the model rows by a packed key column, ties broken by model row.
	 *
	 * @param keys       one key per model row ({@code keys.length >= getModelCount()})
	 * @param descending {@code true} for descending keys (ties stay in model order)
	 * @throws IllegalArgumentException if {@code keys} is shorter than the model
	 */
	public void sortByIntKeys(final int[] keys, final boolean descending) {
		if (keys.length < modelCount) {
			throw new IllegalArgumentException("keys.length=" + keys.length + " < modelCount=" + modelCount);
		}
		// LSD radix sort (11/11/10-bit digits) of the identity order: stable, so ties keep model
		// order, which equals the model-row tie-break of sort(RowComparator). The inverse array
		// is the scatter buffer (a sort invalidates it anyway): no allocation after the first sort.
		int[] src = orderArray();
		int[] dst = inverseArray();
		for (int i = 0; i < modelCount; i++) {
			src[i] = i;
		}
		if (radixCount.length == 0) {
			radixCount = new int[RADIX + 1];
		}
		final int flip = descending ? -1 : 0;
		for (int shift = 0; shift < 32; shift += 11) {
			if (radixPass(keys, src, dst, shift, flip)) {
				final int[] t = src;
				src = dst;
				dst = t;
			}
		}
		order = src;
		inverse = dst;
		flags |= SORTED;
		changed();
	}

	/** One stable counting pass; returns false (no scatter) when every row has the same digit. */
	private boolean radixPass(final int[] keys, final int[] src, final int[] dst, final int shift, final int flip) {
		final int[] count = radixCount;
		final int mask = shift == 22 ? 0x3ff : 0x7ff;
		Arrays.fill(count, 0);
		for (int i = 0; i < modelCount; i++) {
			count[((((keys[src[i]] ^ Integer.MIN_VALUE) ^ flip) >>> shift) & mask) + 1]++;
		}
		for (int d = 1; d <= RADIX; d++) {
			if (count[d] == modelCount) {
				return false;
			}
		}
		for (int d = 1; d <= RADIX; d++) {
			count[d] += count[d - 1];
		}
		for (int i = 0; i < modelCount; i++) {
			final int m = src[i];
			dst[count[(((keys[m] ^ Integer.MIN_VALUE) ^ flip) >>> shift) & mask]++] = m;
		}
		return true;
	}

	/**
	 * Sets the sort order directly (for example a server-side sort).
	 *
	 * @param newOrder view position to model row; a permutation of {@code 0..getModelCount()-1}
	 * @throws IllegalArgumentException if {@code newOrder} is not such a permutation
	 */
	public void setOrder(final int[] newOrder) {
		if (newOrder.length != modelCount) {
			throw new IllegalArgumentException("order.length=" + newOrder.length + " != modelCount=" + modelCount);
		}
		final int[] seen = inverseArray();
		Arrays.fill(seen, 0, modelCount, -1);
		flags &= ~INVERSE_VALID;
		for (int v = 0; v < modelCount; v++) {
			final int m = newOrder[v];
			if (m < 0 || m >= modelCount || seen[m] >= 0) {
				throw new IllegalArgumentException("not a permutation at view " + v + ": " + m);
			}
			seen[m] = v;
		}
		System.arraycopy(newOrder, 0, orderArray(), 0, modelCount);
		flags |= SORTED;
		changed();
	}

	/**
	 * Returns to model order. The filter is kept.
	 */
	public void clearSort() {
		flags &= ~SORTED;
		changed();
	}

	/**
	 * Sets the filter by testing every model row once.
	 *
	 * @param predicate row test
	 * @throws NullPointerException if {@code predicate} is null
	 */
	public void setFilter(final RowPredicate predicate) {
		if (predicate == null) {
			throw new NullPointerException("predicate");
		}
		final long[] b = bitsArray();
		final int words = words(modelCount);
		Arrays.fill(b, 0, words, 0L);
		for (int m = 0; m < modelCount; m++) {
			if (predicate.test(m)) {
				b[m >>> 6] |= 1L << (m & 63);
			}
		}
		flags |= FILTERED;
		changed();
	}

	/**
	 * Sets the filter from a bitset: bit {@code m} of {@code filterBits[m >>> 6]} set means model
	 * row {@code m} is displayed. The bits are copied; bits past the model are ignored.
	 *
	 * @param filterBits one bit per model row
	 * @throws IllegalArgumentException if {@code filterBits} has fewer than
	 *                                  {@code (getModelCount() + 63) / 64} words
	 */
	public void setFilterBits(final long[] filterBits) {
		final int words = words(modelCount);
		if (filterBits.length < words) {
			throw new IllegalArgumentException("filterBits.length=" + filterBits.length + " < " + words);
		}
		final long[] b = bitsArray();
		System.arraycopy(filterBits, 0, b, 0, words);
		final int tail = modelCount & 63;
		if (tail != 0) {
			b[words - 1] &= (1L << tail) - 1L;
		}
		flags |= FILTERED;
		changed();
	}

	/**
	 * Removes the filter. The sort order is kept.
	 */
	public void clearFilter() {
		flags &= ~FILTERED;
		changed();
	}

	/**
	 * @param modelRow a model row
	 * @return {@code true} if {@code modelRow} passes the filter (always, when unfiltered)
	 */
	public boolean passes(final int modelRow) {
		checkIndex(modelRow, modelCount);
		return (flags & FILTERED) == 0 || (bits[modelRow >>> 6] & (1L << (modelRow & 63))) != 0L;
	}

	/**
	 * @return a counter that changes on every sort, filter or size change (31-bit, wraps to 0)
	 */
	@Override
	public int getVersion() {
		return version;
	}

	private void changed() {
		flags &= ~(VIEW_VALID | INVERSE_VALID);
		version = (version + 1) & VERSION_MASK;
	}

	private void ensureView() {
		if ((flags & VIEW_VALID) != 0) {
			return;
		}
		if (visible.length < modelCount) {
			visible = new int[modelCount];
		}
		int k = 0;
		if ((flags & SORTED) != 0) {
			for (int v = 0; v < modelCount; v++) {
				final int m = order[v];
				if ((bits[m >>> 6] & (1L << (m & 63))) != 0L) {
					visible[k++] = m;
				}
			}
		} else {
			final int words = words(modelCount);
			for (int w = 0; w < words; w++) {
				long word = bits[w];
				final int base = w << 6;
				while (word != 0L) {
					visible[k++] = base + Long.numberOfTrailingZeros(word);
					word &= word - 1L;
				}
			}
		}
		viewCount = k;
		flags |= VIEW_VALID;
	}

	private void ensureInverse() {
		if ((flags & INVERSE_VALID) != 0) {
			return;
		}
		final int[] inv = inverseArray();
		final int count = getViewCount();
		if ((flags & FILTERED) != 0) {
			Arrays.fill(inv, 0, modelCount, -1);
			for (int v = 0; v < count; v++) {
				inv[visible[v]] = v;
			}
		} else {
			for (int v = 0; v < count; v++) {
				inv[order[v]] = v;
			}
		}
		flags |= INVERSE_VALID;
	}

	private int[] orderArray() {
		if (order.length < modelCount) {
			order = new int[modelCount];
		}
		return order;
	}

	private int[] inverseArray() {
		if (inverse.length < modelCount) {
			inverse = new int[modelCount];
		}
		return inverse;
	}

	private long[] bitsArray() {
		final int words = words(modelCount);
		if (bits.length < words) {
			bits = new long[words];
		}
		return bits;
	}

	private static int words(final int rows) {
		return (rows + 63) >>> 6;
	}

	private static void checkIndex(final int index, final int size) {
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
		}
	}
}
