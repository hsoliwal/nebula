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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

/** Allocation-free filter/selection intersection against the original scalar contract. */
public class RowSelectionMaskedCount_Test {

	@Test
	public void arrayBitmapAndRunContainersCountClippedMaskWords() {
		final int chunk = RowSparseBits.CHUNK;
		final int rows = 3 * chunk + 65;
		final RowSelection selected = new RowSelection(rows);
		final RowSparseBits encodings = new RowSparseBits();
		final BitSet oracle = new BitSet(rows);
		selected.setSelected(0, chunk, true);
		encodings.setRange(0, chunk, true);
		oracle.set(0, chunk);
		for (int i = 0; i <= RowSparseBits.ARRAY_MAX; i++) {
			final int row = chunk + i * 7;
			selected.setSelected(row, true);
			encodings.set(row, true);
			oracle.set(row);
		}
		for (final int offset : new int[] {0, 17, 63, 64, 65, 65535}) {
			selected.setSelected(2 * chunk + offset, true);
			encodings.set(2 * chunk + offset, true);
			oracle.set(2 * chunk + offset);
		}
		selected.setSelected(rows - 1, true);
		encodings.set(rows - 1, true);
		oracle.set(rows - 1);
		assertEquals(RowSparseBits.RUN, encodings.kinds[0]);
		assertEquals(RowSparseBits.BITMAP, encodings.kinds[1]);
		assertEquals(RowSparseBits.ARRAY, encodings.kinds[2]);

		final Random random = new Random(0xD00F);
		final int fullWords = (rows + 63) >>> 6;
		for (final int length : new int[] {
				0, 1, 2, 63, 64, 65, 1023, 1024, 1025, 2048,
				fullWords - 1, fullWords, fullWords + 2
		}) {
			for (int variant = 0; variant < 8; variant++) {
				final long[] mask = new long[length];
				if (variant == 0) {
					Arrays.fill(mask, -1L);
				} else if (variant == 1) {
					Arrays.fill(mask, 0L);
				} else {
					for (int word = 0; word < mask.length; word++) {
						mask[word] = random.nextLong();
					}
				}
				for (final int limit : new int[] {
						0, 1, 63, 64, 65, chunk - 1, chunk, chunk + 1,
						2 * chunk, 2 * chunk + 1, rows - 1, rows
				}) {
					final int expected = scalar(oracle, mask, limit);
					assertEquals("length=" + length + " variant=" + variant + " limit=" + limit,
							expected, encodings.countMasked(mask, limit));
					if (limit == rows) {
						assertEquals(expected, selected.getSelectionCount(mask));
					}
				}
			}
		}
	}

	@Test
	public void largeSelectAllUsesRunWordsAndKeepsLogicalBounds() {
		final int rows = 2_000_017;
		final RowSelection selection = new RowSelection(rows);
		selection.selectAll();
		final long[] all = new long[(rows + 63) >>> 6];
		Arrays.fill(all, -1L);
		assertEquals(rows, selection.getSelectionCount(all));
		assertEquals(64, selection.getSelectionCount(new long[] {-1L}));
		assertEquals(0, selection.getSelectionCount(new long[0]));
		selection.setSelected(63, 130, false);
		assertEquals(rows - 67, selection.getSelectionCount(all));
		assertEquals(63, selection.getSelectionCount(new long[] {-1L}));
		selection.setModelCount(65);
		assertEquals(63, selection.getSelectionCount(all));
		assertEquals(63, selection.getSelectionCount(new long[] {-1L}));
	}

	@Test
	public void emptySelectionDoesNotReadMask() {
		assertEquals(0, new RowSelection(0).getSelectionCount(null));
		assertEquals(0, new RowSelection(123).getSelectionCount(null));
	}

	private static int scalar(final BitSet selected, final long[] mask, final int limit) {
		int count = 0;
		for (int row = selected.nextSetBit(0); row >= 0 && row < limit;
				row = selected.nextSetBit(row + 1)) {
			if ((row >>> 6) < mask.length
					&& (mask[row >>> 6] & (1L << (row & 63))) != 0L) {
				count++;
			}
		}
		return count;
	}
}
