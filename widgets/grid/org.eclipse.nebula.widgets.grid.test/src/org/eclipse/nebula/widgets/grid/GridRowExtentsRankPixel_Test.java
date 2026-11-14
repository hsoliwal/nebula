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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

/**
 * The rank and pixel queries of {@link GridRowExtents} (extent before a row, rank of a row, position and extent of
 * a rank, rank at an extent) against a scalar model: every hidden/visible mask of 9 rows with single-row toggles and
 * an invalidated rebuild, 1025 appended rows, and a rebuild whose extent callback fails.
 */
public class GridRowExtentsRankPixel_Test {

	private static void verify(GridRowExtents g, int[] e) {
		long sum = 0;
		int rank = 0;
		assertEquals(e.length, g.size());
		assertEquals(0, g.extentOfRank(0));
		for (int i = 0; i < e.length; i++) {
			assertEquals(sum, g.extentBefore(i));
			assertEquals(rank, g.rankOf(i));
			assertEquals(e[i], g.extentAt(i));
			if (e[i] > 0) {
				assertEquals(i, g.positionOfRank(rank));
				assertEquals(sum, g.extentOfRank(rank));
				assertEquals(rank, g.rankAtExtent(sum));
				sum += e[i];
				rank++;
			}
		}
		assertEquals(rank, g.visibleCount());
		assertEquals(sum, g.totalExtent());
		assertEquals(sum, g.extentOfRank(rank));
		assertEquals(rank, g.rankAtExtent(sum));
		assertEquals(-1, g.rankAtExtent(-1));
		for (final int r : new int[] { -1, -2, Integer.MIN_VALUE, Integer.MAX_VALUE, rank + 1 }) {
			final long expected = r == 0 ? 0
					: r == g.visibleCount() ? g.totalExtent() : g.extentBefore(g.positionOfRank(r));
			assertEquals(expected, g.extentOfRank(r));
		}
	}

	@Test
	public void everyMaskOfNineRows() {
		for (int mask = 0; mask < 512; mask++) {
			final int[] e = new int[9];
			for (int i = 0; i < 9; i++) {
				e[i] = (mask & (1 << i)) == 0 ? 0 : (i % 3 == 0 ? Integer.MAX_VALUE : i + 1);
			}
			final GridRowExtents g = new GridRowExtents();
			g.rebuild(e.length, i -> e[i]);
			verify(g, e);
			for (int i = 0; i < e.length; i++) {
				e[i] = e[i] == 0 ? i + 1 : 0;
				g.set(i, e[i]);
				verify(g, e);
			}
			g.invalidate();
			assertFalse(g.isValid(e.length));
			g.rebuild(e.length, i -> e[i]);
			verify(g, e);
		}
	}

	@Test
	public void appendedRowsAndAFailedRebuild() {
		final GridRowExtents g = new GridRowExtents();
		int[] model = new int[0];
		for (int n = 0; n < 1025; n++) {
			final int v = n % 5 == 0 ? 0 : n % 17 == 0 ? Integer.MAX_VALUE : n % 137 + 1;
			g.append(v);
			model = Arrays.copyOf(model, n + 1);
			model[n] = v;
			if (n < 64 || (n & 63) == 0 || n == 1024) {
				verify(g, model);
			}
		}
		final RuntimeException marker = new RuntimeException("extent callback");
		try {
			g.rebuild(4, i -> {
				if (i == 2) {
					throw marker;
				}
				return 7;
			});
			fail("lost failure");
		} catch (final RuntimeException ex) {
			assertSame(marker, ex);
		}
		assertFalse(g.isValid(1025));
		g.rebuild(3, i -> i == 1 ? 0 : 17);
		verify(g, new int[] { 17, 0, 17 });
	}
}
