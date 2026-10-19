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
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/**
 * {@link GridRowExtents} against a brute-force walk over the same extents: random visibility,
 * heights, appends and point changes, every question at every position.
 */
public class GridRowExtents_Test {

	private static int[] randomExtents(final Random random, final int count) {
		final int[] extents = new int[count];
		for (int i = 0; i < count; i++) {
			extents[i] = random.nextInt(3) == 0 ? 0 : 2 + random.nextInt(30);
		}
		return extents;
	}

	private static void assertAnswers(final GridRowExtents index, final int[] extents, final int count) {
		int rank = 0;
		long extent = 0;
		for (int p = 0; p <= count; p++) {
			assertEquals("rank " + p, rank, index.rankOf(p));
			assertEquals("extent " + p, extent, index.extentBefore(p));
			if (p < count && extents[p] > 0) {
				assertEquals("position of rank " + rank, p, index.positionOfRank(rank));
				assertEquals("extent of rank " + rank, extent, index.extentOfRank(rank));
				// every offset inside the row maps to the row
				assertEquals(rank, index.rankAtExtent(extent));
				assertEquals(rank, index.rankAtExtent(extent + extents[p] - 1));
				rank++;
				extent += extents[p];
			}
			if (p < count) {
				assertEquals(next(extents, count, p), index.nextVisible(p));
				assertEquals(previous(extents, p), index.previousVisible(p));
			}
		}
		assertEquals(rank, index.visibleCount());
		assertEquals(extent, index.totalExtent());
		assertEquals(extent, index.extentOfRank(rank));
		assertEquals(rank, index.rankAtExtent(extent + 5));
		assertEquals(-1, index.rankAtExtent(-1));
		assertEquals(-1, index.nextVisible(count));
		assertEquals(-1, index.previousVisible(-1));
	}

	private static int next(final int[] extents, final int count, final int from) {
		for (int p = from; p < count; p++) {
			if (extents[p] > 0) {
				return p;
			}
		}
		return -1;
	}

	private static int previous(final int[] extents, final int from) {
		for (int p = from; p >= 0; p--) {
			if (extents[p] > 0) {
				return p;
			}
		}
		return -1;
	}

	@Test
	public void rebuildAnswersLikeAWalk() {
		final Random random = new Random(11);
		for (int count = 0; count < 70; count++) {
			final int[] extents = randomExtents(random, count);
			final GridRowExtents index = new GridRowExtents();
			index.rebuild(count, p -> extents[p]);
			assertTrue(index.isValid(count));
			assertAnswers(index, extents, count);
		}
	}

	@Test
	public void appendsAndPointChangesAnswerLikeAWalk() {
		final Random random = new Random(12);
		final int[] extents = new int[300];
		final GridRowExtents index = new GridRowExtents();
		index.rebuild(0, p -> 0);
		for (int count = 0; count < extents.length; count++) {
			extents[count] = random.nextInt(4) == 0 ? 0 : 2 + random.nextInt(20);
			index.append(extents[count]);
			if (count % 7 == 0) {
				final int p = random.nextInt(count + 1);
				extents[p] = random.nextInt(3) == 0 ? 0 : 2 + random.nextInt(40);
				index.set(p, extents[p]);
			}
			assertAnswers(index, extents, count + 1);
		}
	}

	@Test
	public void invalidationAndSize() {
		final GridRowExtents index = new GridRowExtents();
		index.rebuild(3, p -> 5);
		assertTrue(index.isValid(3));
		assertTrue(!index.isValid(4));
		index.invalidate();
		assertTrue(!index.isValid(3));
	}

	@Test
	public void millionPositionsAnswerInLogarithmicSteps() {
		final int count = 1_000_000;
		final GridRowExtents index = new GridRowExtents();
		// every 100th position is a visible root of height 20 + 1; the rest are hidden children
		index.rebuild(count, p -> p % 100 == 0 ? 21 : 0);
		assertEquals(10_000, index.visibleCount());
		assertEquals(210_000L, index.totalExtent());
		assertEquals(999_900, index.positionOfRank(9_999));
		assertEquals(9_999, index.rankAtExtent(209_999L));
		assertEquals(999_900, index.previousVisible(count - 1));
		assertEquals(-1, index.nextVisible(999_901));
		index.set(999_950, 31);
		assertEquals(999_950, index.positionOfRank(10_000));
		assertEquals(210_031L, index.totalExtent());
	}
}
