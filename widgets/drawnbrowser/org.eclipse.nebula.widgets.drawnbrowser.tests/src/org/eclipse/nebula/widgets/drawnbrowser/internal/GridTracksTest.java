/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.eclipse.nebula.widgets.drawnbrowser.internal.GridTracks.AUTO;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.GridTracks.FR;
import static org.eclipse.nebula.widgets.drawnbrowser.internal.GridTracks.PERCENT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** grid-template-columns and the grid track sizing steps: fixed, percent, auto and fr tracks, justify-content, gap. */
public class GridTracksTest {

	@Test
	public void trackListsAreParsed() {
		assertArrayEquals(new int[] { 100, FR | 100, FR | 200 }, GridTracks.parse("100px 1fr 2fr"));
		assertArrayEquals(new int[] { FR | 100, FR | 100, FR | 100 }, GridTracks.parse("repeat(3, 1fr)"));
		assertArrayEquals(new int[] { AUTO, PERCENT | 2500, FR | 150, 0 }, GridTracks.parse("auto 25% minmax(50px, 1.5fr) 0"));
		assertArrayEquals(new int[] { 40, AUTO, 40, AUTO, 13 }, GridTracks.parse(" repeat(2, 40px max-content)  12.6PX "));
		assertArrayEquals(new int[] { AUTO }, GridTracks.parse("fit-content(200px)"));
		for (String wrong : new String[] { "none", "", "repeat(auto-fill, 100px)", "1em", "10px foo", "repeat(2 1fr)",
				"repeat(1001, 1px)", "minmax(1em, 1fr)", "repeat(2, (1fr)", "-5px" }) {
			assertNull(wrong, GridTracks.parse(wrong));
		}
	}

	@Test
	public void fixedTracksKeepTheirWidthAndFrTracksShareTheRest() {
		// 400 less two gaps of 10: 380; 100 fixed, 280 shared 1:2 and the last column ends exactly at 400
		assertArrayEquals(new int[] { 0, 110, 213, 100, 93, 187 },
				GridTracks.layout(new int[] { 100, FR | 100, FR | 200 }, new int[3], new int[3], 400, 10, FlexRow.START));
		// a percentage is of the whole width
		assertArrayEquals(new int[] { 0, 100, 100, 300 },
				GridTracks.layout(new int[] { PERCENT | 2500, FR | 100 }, new int[2], new int[2], 400, 0, FlexRow.START));
		// not of the width less the gaps: two halves and a gap overflow
		assertArrayEquals(new int[] { 0, 220, 200, 200 },
				GridTracks.layout(new int[] { PERCENT | 5000, PERCENT | 5000 }, new int[2], new int[2], 400, 20, FlexRow.START));
	}

	@Test
	public void anAutoTrackTakesItsContentWidthBeforeTheFrTracks() {
		assertArrayEquals(new int[] { 0, 60, 60, 240 },
				GridTracks.layout(new int[] { AUTO, FR | 100 }, new int[] { 20, 10 }, new int[] { 60, 30 }, 300, 0, FlexRow.START));
	}

	@Test
	public void autoTracksGrowToTheirContentThenStretchWhenNothingIsFlexible() {
		// maximise: 30 and 50 (their content); stretch: the remaining 120 shared equally
		assertArrayEquals(new int[] { 0, 90, 90, 110 },
				GridTracks.layout(new int[] { AUTO, AUTO }, new int[] { 10, 10 }, new int[] { 30, 50 }, 200, 0, FlexRow.START));
		// half an fr takes half of what is left; the auto track stretches into the other half
		assertArrayEquals(new int[] { 0, 90, 90, 110 },
				GridTracks.layout(new int[] { FR | 50, AUTO }, new int[2], new int[] { 0, 20 }, 200, 0, FlexRow.START));
	}

	@Test
	public void anFrTrackUnderItsMinimumKeepsItAndTheOthersShareAgain() {
		assertArrayEquals(new int[] { 0, 150, 150, 50 },
				GridTracks.layout(new int[] { FR | 100, FR | 100 }, new int[] { 150, 0 }, new int[2], 200, 0, FlexRow.START));
		// too narrow: the fr track stays at its minimum and the grid overflows
		assertArrayEquals(new int[] { 0, 100, 100, 80 },
				GridTracks.layout(new int[] { 100, FR | 100 }, new int[] { 0, 80 }, new int[2], 150, 0, FlexRow.START));
	}

	@Test
	public void justifyContentPlacesTracksThatDoNotFill() {
		int[] tracks = { PERCENT | 2500, 50 }, none = new int[2];
		assertArrayEquals(new int[] { 125, 225, 100, 50 }, GridTracks.layout(tracks, none, none, 400, 0, FlexRow.CENTER));
		assertArrayEquals(new int[] { 250, 350, 100, 50 }, GridTracks.layout(tracks, none, none, 400, 0, FlexRow.END));
		assertArrayEquals(new int[] { 0, 350, 100, 50 }, GridTracks.layout(tracks, none, none, 400, 0, FlexRow.SPACE_BETWEEN));
	}

	@Test
	public void hugeFixedTracksSaturateNativeCoordinatesInsteadOfWrapping() {
		int[] tracks = new int[32];
		java.util.Arrays.fill(tracks, GridTracks.MAX_PX);
		int[] result = GridTracks.layout(tracks, new int[32], new int[32],
				Integer.MAX_VALUE, 7, FlexRow.START);
		assertArrayEquals(new int[] { 0, GridTracks.MAX_PX + 7, 2 * GridTracks.MAX_PX + 14 },
				new int[] { result[0], result[1], result[2] });
		for (int i = 0; i < tracks.length; i++) {
			org.junit.Assert.assertTrue("x must not wrap at " + i, result[i] >= 0);
			org.junit.Assert.assertEquals(GridTracks.MAX_PX, result[tracks.length + i]);
			if (i > 0) org.junit.Assert.assertTrue("monotonic x", result[i] >= result[i - 1]);
		}
		org.junit.Assert.assertEquals(Integer.MAX_VALUE, result[31]);
	}

	@Test
	public void thousandLargeFrFactorsRetainExactCumulativeEdges() {
		int n = 1000;
		int[] tracks = new int[n];
		java.util.Arrays.fill(tracks, FR | 100_000_000);
		int[] result = GridTracks.layout(tracks, new int[n], new int[n],
				Integer.MAX_VALUE, 0, FlexRow.START);
		long total = 0;
		for (int i = 0; i < n; i++) {
			long expectedStart = (long) Integer.MAX_VALUE * i / n;
			long expectedEnd = (long) Integer.MAX_VALUE * (i + 1) / n;
			org.junit.Assert.assertEquals("x at " + i, expectedStart, result[i]);
			org.junit.Assert.assertEquals("width at " + i, expectedEnd - expectedStart, result[n + i]);
			total += result[n + i];
		}
		org.junit.Assert.assertEquals(Integer.MAX_VALUE, total);
	}

	@Test
	public void oversizedFrFactorComparisonFreezesLargeMinimum() {
		int n = 1000;
		int[] tracks = new int[n], min = new int[n];
		java.util.Arrays.fill(tracks, FR | 100_000_000);
		min[0] = 2_000_000_000;
		int[] result = GridTracks.layout(tracks, min, new int[n],
				Integer.MAX_VALUE, 0, FlexRow.START);
		org.junit.Assert.assertEquals(2_000_000_000, result[n]);
		org.junit.Assert.assertEquals(0, result[0]);
		org.junit.Assert.assertEquals(2_000_000_000, result[1]);
		long width = 0;
		for (int i = 0; i < n; i++) {
			org.junit.Assert.assertTrue("nonnegative width", result[n + i] >= 0);
			width += result[n + i];
		}
		org.junit.Assert.assertEquals(Integer.MAX_VALUE, width);
	}

}
