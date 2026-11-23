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
import static org.junit.Assert.assertThrows;

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

	@Test
	public void aZeroShrinkFactorKeepsTheSidebarAtItsBasis() {
		assertArrayEquals(new int[] { 0, 120, 120, 30 }, row(new int[2], new int[] { 120, 80 }, new double[] { 0, 1 }, 150));
		assertArrayEquals(new int[] { 0, 120, 120, 80 }, row(new int[2], new int[] { 120, 80 }, new double[] { 0, 0 }, 150));
	}

	@Test
	public void fractionalFactorsWeightTheOriginalBases() {
		assertArrayEquals(new int[] { 0, 110, 110, 60 }, row(new int[2], new int[] { 120, 80 }, new double[] { 1, 3 }, 170));
		assertArrayEquals(new int[] { 0, 170, 170, 70 }, row(new int[2], new int[] { 200, 100 }, new double[] { 1, 2 }, 240));
		assertArrayEquals(new int[] { 0, 75, 75, 25 }, row(new int[2], new int[] { 100, 100 }, new double[] { .5, 1.5 }, 100));
	}

	@Test
	public void factorsSummingBelowOneIntentionallyLeaveOverflow() {
		assertArrayEquals(new int[] { 0, 75, 75, 75 }, row(new int[2], new int[] { 100, 100 }, new double[] { .25, .25 }, 100));
		assertArrayEquals(new int[] { 0, 100, 100, 50 }, row(new int[2], new int[] { 100, 100 }, new double[] { 0, .5 }, 100));
		assertArrayEquals(new int[] { 0, 90, 90, 75 }, row(new int[] { 90, 0 }, new int[] { 100, 100 }, new double[] { .25, .25 }, 100));
	}

	@Test
	public void minimumFreezingRestartsFromOriginalBasesAndFactors() {
		assertArrayEquals(new int[] { 0, 0, 0, 80 }, row(new int[2], new int[] { 50, 150 }, new double[] { 10, 1 }, 80));
		assertArrayEquals(new int[] { 0, 0, 0, 90 }, row(new int[2], new int[] { 50, 150 }, new double[] { 10, .5 }, 80));
		assertArrayEquals(new int[] { 0, 0, 40, 0, 40, 70 }, row(new int[3], new int[] { 20, 100, 100 }, new double[] { 10, 2, 1 }, 110));
		assertArrayEquals(new int[] { 0, 80, 80, 20 }, row(new int[] { 80, 0 }, new int[] { 100, 100 }, new double[] { 1, 1 }, 100));
	}

	@Test
	public void paddingAndBordersDoNotIncreaseTheShrinkWeight() {
		assertArrayEquals(new int[] { 0, 70, 70, 50 }, FlexRow.layout(new int[] { 20, 0 }, new int[] { 120, 100 },
				new int[] { 120, 100 }, new int[2], new double[] { 1, 1 }, new int[] { 20, 0 }, 120, 0, FlexRow.START));
		assertArrayEquals(new int[] { 0, 40, 40, 40 }, FlexRow.layout(new int[] { 40, 0 }, new int[] { 50, 100 },
				new int[] { 50, 100 }, new int[2], new double[] { 10, 1 }, new int[] { 40, 0 }, 80, 0, FlexRow.START));
	}

	@Test
	public void finiteHugeFactorsAndBasesDoNotOverflow() {
		assertArrayEquals(new int[] { 0, 60, 60, 60 }, row(new int[2], new int[] { 120, 80 }, new double[] { 1e308, 5e307 }, 120));
		assertArrayEquals(new int[] { 0, 50, 50, 50 }, row(new int[2], new int[] { 100, 100 },
				new double[] { Double.MAX_VALUE, Double.MAX_VALUE }, 100));
		assertArrayEquals(new int[] { 0, 700000000, 1400000000, 700000000, 700000000, 700000000 },
				row(new int[3], new int[] { 2000000000, 2000000000, 2000000000 }, new double[] { 1, 1, 1 }, 2100000000));
	}

	@Test
	public void aHugeFactorOnZeroContentCannotEraseAnotherItemsPositiveWeight() {
		assertArrayEquals(new int[] { 0, 0, 0, 0 },
				row(new int[2], new int[] { 0, 100 }, new double[] { 1e308, Double.MIN_VALUE }, 0));
		assertArrayEquals(new int[] { 0, 20, 20, 0 }, FlexRow.layout(new int[] { 20, 0 }, new int[] { 20, 100 },
				new int[] { 20, 100 }, new int[2], new double[] { 1e308, Double.MIN_VALUE }, new int[] { 20, 0 },
				20, 0, FlexRow.START));
	}

	@Test
	public void weightedPathSaturatesUnrepresentableCoordinatesAndExactGrowProducts() {
		int largest = Integer.MAX_VALUE;
		assertArrayEquals(new int[] { 0, largest, largest, largest, largest, largest },
				row(new int[3], new int[] { largest, largest, largest }, new double[3], 0));
		assertArrayEquals(new int[] { 0, 715827882, 1431655764, 715827882, 715827882, 715827883 },
				FlexRow.layout(new int[3], new int[3], NONE, new int[] { largest, largest, largest }, new double[] { 1, 1, 1 },
						largest, 0, FlexRow.START));
	}

	@Test
	public void invalidFactorsAreRejectedBeforeLayoutAndInputsStayUnchanged() {
		int[] min = { 20, 0 }, base = { 120, 80 }, grow = { 0, 0 }, inset = { 20, 0 };
		double[] factors = { .5, 1.5 };
		FlexRow.layout(min, base, base, grow, factors, inset, 100, 0, FlexRow.START);
		assertArrayEquals(new int[] { 20, 0 }, min);
		assertArrayEquals(new int[] { 120, 80 }, base);
		assertArrayEquals(new int[] { 0, 0 }, grow);
		assertArrayEquals(new int[] { 20, 0 }, inset);
		assertArrayEquals(new double[] { .5, 1.5 }, factors, 0);
		for (double invalid : new double[] { -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
			assertThrows(IllegalArgumentException.class, () -> row(min, base, new double[] { invalid, 1 }, 100));
		}
		assertThrows(IllegalArgumentException.class, () -> row(min, base, new double[] { 1 }, 100));
	}

	private static int[] row(int[] min, int[] bases, double[] factors, int available) {
		return FlexRow.layout(min, bases, bases, new int[min.length], factors, available, 0, FlexRow.START);
	}
}
