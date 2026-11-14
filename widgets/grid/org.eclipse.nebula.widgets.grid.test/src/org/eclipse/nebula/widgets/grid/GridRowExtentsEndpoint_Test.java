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

import org.junit.Test;

/**
 * {@link GridRowExtents#rankAtExtent(long)} at the endpoints of valid and invalid offsets, checked against a scalar
 * row oracle, after a rebuild, after single-row changes and for an appended index.
 */
public class GridRowExtentsEndpoint_Test {

	private static void verify(GridRowExtents index, int[] extents) {
		long total = 0;
		int visible = 0;
		for (final int extent : extents) {
			total += extent;
			if (extent != 0) {
				visible++;
			}
		}
		assertEquals(total, index.totalExtent());
		assertEquals(visible, index.visibleCount());
		final long[] offsets = { Long.MIN_VALUE, -1, 0, 1, total / 2, total == 0 ? 0 : total - 1, total, total + 1,
				Long.MAX_VALUE };
		for (final long offset : offsets) {
			int expected = offset < 0 ? -1 : 0;
			long prefix = 0;
			if (offset >= 0) {
				for (final int extent : extents) {
					if (extent == 0) {
						continue;
					}
					if (prefix + extent > offset) {
						break;
					}
					prefix += extent;
					expected++;
				}
			}
			assertEquals("rank at extent " + offset, expected, index.rankAtExtent(offset));
		}
	}

	@Test
	public void rankAtExtentMatchesTheScalarOracle() {
		for (final int size : new int[] { 0, 1, 2, 31, 32, 33, 127, 128, 129, 1025 }) {
			for (int mask = 0; mask < 32; mask++) {
				final int[] extents = new int[size];
				for (int i = 0; i < size; i++) {
					extents[i] = (mask & (1 << (i % 5))) == 0 ? 0 : (i % 7 == 0 ? Integer.MAX_VALUE : i % 59 + 1);
				}
				final GridRowExtents index = new GridRowExtents();
				index.rebuild(size, i -> extents[i]);
				verify(index, extents);
				for (int i = 0; i < size; i += Math.max(1, size / 17)) {
					extents[i] = extents[i] == 0 ? Integer.MAX_VALUE : 0;
					index.set(i, extents[i]);
					verify(index, extents);
				}
				final GridRowExtents appended = new GridRowExtents();
				appended.rebuild(0, i -> 0);
				for (final int extent : extents) {
					appended.append(extent);
				}
				verify(appended, extents);
			}
		}
	}
}
