/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/** One flex line: base sizes, grow, shrink, justify-content and gap. */
public class FlexRowTest {

	private static final int[] NONE = { -1, -1, -1 };

	@Test
	public void freeSpaceGoesToGrowingItemsByWeight() {
		// content 30, 40, 50 in 300: 180 free shared 1:2 between the first two
		assertArrayEquals(new int[] { 0, 90, 250, 90, 160, 50 },
				FlexRow.layout(new int[] { 10, 10, 10 }, new int[] { 30, 40, 50 }, NONE, new int[] { 1, 2, 0 }, 300, 0, FlexRow.START));
		// a share that does not divide: the line still ends exactly at 100
		assertArrayEquals(new int[] { 0, 33, 66, 33, 33, 34 },
				FlexRow.layout(new int[3], new int[3], NONE, new int[] { 1, 1, 1 }, 100, 0, FlexRow.START));
	}

	@Test
	public void justifyContentAndGapPlaceItemsThatDoNotGrow() {
		int[] min = new int[2], max = { 20, 30 }, none = { -1, -1 }, still = { 0, 0 };
		assertArrayEquals(new int[] { 0, 30, 20, 30 }, FlexRow.layout(min, max, none, still, 100, 10, FlexRow.START));
		assertArrayEquals("center: (100 - 60) / 2", new int[] { 20, 50, 20, 30 }, FlexRow.layout(min, max, none, still, 100, 10, FlexRow.CENTER));
		assertArrayEquals(new int[] { 40, 70, 20, 30 }, FlexRow.layout(min, max, none, still, 100, 10, FlexRow.END));
		assertArrayEquals("space-between: the rest between them", new int[] { 0, 70, 20, 30 },
				FlexRow.layout(min, max, none, still, 100, 10, FlexRow.SPACE_BETWEEN));
	}

	@Test
	public void aBasisSetsTheBaseSizeButNotBelowTheMinimum() {
		assertArrayEquals(new int[] { 0, 50, 50, 25 },
				FlexRow.layout(new int[] { 10, 25 }, new int[] { 99, 99 }, new int[] { 50, 5 }, new int[] { 0, 0 }, 500, 0, FlexRow.START));
	}

	@Test
	public void aNarrowLineShrinksItemsByTheirSizeDownToTheirMinimum() {
		// 100 + 300 in 200: 200 too wide, shrunk 1:3
		assertArrayEquals(new int[] { 0, 50, 50, 150 },
				FlexRow.layout(new int[] { 0, 0 }, new int[] { 100, 300 }, new int[] { -1, -1 }, new int[] { 0, 0 }, 200, 0, FlexRow.START));
		// the second cannot go below 250: it is frozen there and the first takes the rest
		assertArrayEquals(new int[] { 0, 0, 0, 250 },
				FlexRow.layout(new int[] { 0, 250 }, new int[] { 100, 300 }, new int[] { -1, -1 }, new int[] { 0, 0 }, 200, 0, FlexRow.START));
		// everything at its minimum: the line overflows
		assertArrayEquals(new int[] { 0, 150, 150, 150 },
				FlexRow.layout(new int[] { 150, 150 }, new int[] { 200, 200 }, new int[] { -1, -1 }, new int[] { 0, 0 }, 200, 0, FlexRow.START));
	}
}
