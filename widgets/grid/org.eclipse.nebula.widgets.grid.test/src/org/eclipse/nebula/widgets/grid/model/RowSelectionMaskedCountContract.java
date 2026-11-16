/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid.model;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

/** Pure-JDK, native-independent differential gate for the packed selection count. */
public final class RowSelectionMaskedCountContract {
	private static int checks;

	private RowSelectionMaskedCountContract() {
	}

	public static void main(String[] args) {
		final int chunk = RowSparseBits.CHUNK;
		final int rows = 3 * chunk + 65;
		final RowSparseBits bits = new RowSparseBits();
		final RowSelection selection = new RowSelection(rows);
		final BitSet reference = new BitSet(rows);
		bits.setRange(0, chunk, true);
		selection.setSelected(0, chunk, true);
		reference.set(0, chunk);
		for (int i = 0; i <= RowSparseBits.ARRAY_MAX; i++) {
			final int row = chunk + 7 * i;
			bits.set(row, true);
			selection.setSelected(row, true);
			reference.set(row);
		}
		for (int offset : new int[] {0, 31, 32, 63, 64, 65, 65535}) {
			final int row = 2 * chunk + offset;
			bits.set(row, true);
			selection.setSelected(row, true);
			reference.set(row);
		}
		bits.set(rows - 1, true);
		selection.setSelected(rows - 1, true);
		reference.set(rows - 1);
		check(RowSparseBits.RUN, bits.kinds[0], "run");
		check(RowSparseBits.BITMAP, bits.kinds[1], "bitmap");
		check(RowSparseBits.ARRAY, bits.kinds[2], "array");

		final Random random = new Random(20261116L);
		final int words = (rows + 63) >>> 6;
		for (int maskLength : new int[] {0, 1, 2, 1023, 1024, 1025,
				2047, 2048, 2049, words - 1, words, words + 5}) {
			for (int trial = 0; trial < 40; trial++) {
				final long[] mask = new long[maskLength];
				if (trial == 0) {
					Arrays.fill(mask, -1L);
				} else if (trial > 1) {
					for (int i = 0; i < mask.length; i++) mask[i] = random.nextLong();
				}
				for (int limit : new int[] {0, 1, 63, 64, 65, chunk - 1,
						chunk, chunk + 1, 2 * chunk, 2 * chunk + 1, rows - 1, rows}) {
					final int expected = scalar(reference, mask, limit);
					check(expected, bits.countMasked(mask, limit), "mask length=" + maskLength + " limit=" + limit);
					if (limit == rows) {
						check(expected, selection.getSelectionCount(mask), "selection facade");
					}
				}
			}
		}

		// Select-all must traverse the compact visible view, including after a
		// deselection. Result order is the view's order, not model-row order.
		final int viewRows = 200_000;
		final RowSelection dense = new RowSelection(viewRows);
		dense.selectAll();
		final RowPermutation view = new RowPermutation(viewRows);
		view.setFilter(row -> row % 8192 == 0);
		final int shown = view.getViewCount();
		final int[] allIndices = dense.toViewIndices(view);
		check(shown, allIndices.length, "filtered view selection length");
		for (int i = 0; i < shown; i++) {
			check(i, allIndices[i], "filtered view selection position");
		}
		dense.setSelected(0, false);
		final int[] withoutFirst = dense.toViewIndices(view);
		check(shown - 1, withoutFirst.length, "filtered view deselection");
		for (int i = 1; i < shown; i++) {
			check(i, withoutFirst[i - 1], "filtered view deselected index");
		}

		final int million = 2_000_017;
		final RowSelection all = new RowSelection(million);
		all.selectAll();
		final long[] visible = new long[(million + 63) >>> 6];
		Arrays.fill(visible, -1L);
		check(million, all.getSelectionCount(visible), "large select all");
		check(64, all.getSelectionCount(new long[] {-1L}), "short mask");
		all.setSelected(63, 130, false);
		check(million - 67, all.getSelectionCount(visible), "clear across mask boundary");
		all.setModelCount(65);
		check(63, all.getSelectionCount(visible), "shrink logical bound");
		check(0, new RowSelection(100).getSelectionCount(null), "empty/null legacy");
		System.out.println("GRID_MODEL_MASK_INTERSECTION_PASS checks=" + checks);
	}

	private static int scalar(BitSet reference, long[] mask, int limit) {
		int count = 0;
		for (int row = reference.nextSetBit(0); row >= 0 && row < limit;
				row = reference.nextSetBit(row + 1)) {
			if ((row >>> 6) < mask.length && (mask[row >>> 6] & (1L << (row & 63))) != 0L) {
				count++;
			}
		}
		return count;
	}

	private static void check(int expected, int actual, String label) {
		checks++;
		if (expected != actual) {
			throw new AssertionError(label + ": expected " + expected + ", got " + actual);
		}
	}
}
