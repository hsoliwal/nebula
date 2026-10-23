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
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

/**
 * The row tree's batched extent changes (optimization pass, leaf 1) answer as the same changes made
 * one by one: two trees of rows without items get the same seeded inserts, removals and reweighs,
 * one of them with the reweighs in batches (some of them read in the middle of the batch, which
 * settles it), the other one change at a time, and a uniform extent for every visible row. After
 * every step every row question has the same answer. Headless: rows without items need no Display.
 */
public class GridRowTreeBatch_Test {

	@Test
	public void batchedReweighsAnswerAsSingleOnes() {
		final Random random = new Random(0xBA7C4L);
		final GridRowTree batched = new GridRowTree();
		final GridRowTree single = new GridRowTree();
		int[] batchedSlots = batched.insertUnread(0, null, 200, 17);
		int[] singleSlots = single.insertUnread(0, null, 200, 17);
		for (int step = 0; step < 3_000; step++) {
			switch (random.nextInt(6)) {
			case 0: {
				final int at = random.nextInt(batched.size() + 1);
				final int count = 1 + random.nextInt(40);
				final int weight = random.nextInt(3) == 0 ? 0 : 1 + random.nextInt(30);
				batched.insertUnread(at, null, count, weight);
				single.insertUnread(at, null, count, weight);
				break;
			}
			case 1:
				if (batched.size() > 10) {
					final int row = random.nextInt(batched.size());
					batched.removeUnread(slotAtRow(batched, row));
					single.removeUnread(slotAtRow(single, row));
				}
				break;
			case 2:
			case 3: {
				// an expand or collapse: a run of rows changes extent in one batch
				final int from = random.nextInt(batched.size());
				final int to = Math.min(batched.size(), from + 1 + random.nextInt(300));
				final boolean show = random.nextBoolean();
				final int weight = show ? 1 + random.nextInt(25) : 0;
				final boolean readInside = random.nextInt(4) == 0;
				batched.beginBatch();
				for (int row = from; row < to; row++) {
					batched.reweighSlot(slotAtRow(batched, row), weight);
					single.reweighSlot(slotAtRow(single, row), weight);
					if (readInside && row == (from + to) / 2) {
						assertEquals(single.totalExtent(), batched.totalExtent());
					}
				}
				batched.endBatch();
				break;
			}
			case 4: {
				final int extent = 5 + random.nextInt(30);
				batched.setVisibleExtent(extent);
				for (int row = 0; row < single.size(); row++) {
					final int slot = slotAtRow(single, row);
					if (single.weightOf(slot) > 0) {
						single.reweighSlot(slot, extent);
					}
				}
				break;
			}
			default: {
				// nested batches settle at the outer end
				batched.beginBatch();
				batched.beginBatch();
				final int row = random.nextInt(batched.size());
				final int weight = random.nextInt(20);
				batched.reweighSlot(slotAtRow(batched, row), weight);
				single.reweighSlot(slotAtRow(single, row), weight);
				batched.endBatch();
				batched.endBatch();
			}
			}
			assertSameAnswers(step, single, batched);
		}
	}

	/* the slot of a row: rows without items are found through their extents and order */
	private static int slotAtRow(final GridRowTree tree, final int row) {
		return tree.slotAtRow(row);
	}

	private static void assertSameAnswers(final int step, final GridRowTree expected, final GridRowTree actual) {
		final String where = "step " + step;
		assertEquals(where, expected.size(), actual.size());
		assertEquals(where, expected.visibleCount(), actual.visibleCount());
		assertEquals(where, expected.totalExtent(), actual.totalExtent());
		for (int row = 0; row <= expected.size(); row += 1 + row / 50) {
			assertEquals(where + " rank " + row, expected.rankOf(row), actual.rankOf(row));
			assertEquals(where + " extent " + row, expected.extentBefore(row), actual.extentBefore(row));
		}
		for (int rank = 0; rank < expected.visibleCount(); rank += 1 + rank / 40) {
			assertEquals(where + " position of rank " + rank, expected.positionOfRank(rank), actual.positionOfRank(rank));
		}
		for (long offset = -1; offset < expected.totalExtent(); offset += 1 + offset / 30 + 7) {
			assertEquals(where + " rank at " + offset, expected.rankAtExtent(offset), actual.rankAtExtent(offset));
		}
	}
}
