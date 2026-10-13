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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Pure capacity decisions of {@link HeapGrowth} with explicit heap measurements. */
public class HeapGrowth_Test {

	private static final long MIB = 1024L * 1024L;

	@Test
	public void doublesWhenTheHeapHasRoom() {
		assertEquals(2000, HeapGrowth.choose(1000, 1001, 4, 2048 * MIB, 100 * MIB));
		assertEquals(16, HeapGrowth.choose(0, 1, 8, 2048 * MIB, 0));
		assertEquals(5000, HeapGrowth.choose(1000, 5000, 4, 2048 * MIB, 100 * MIB));
	}

	@Test
	public void returnsExactlyTheMinimumUnderPressure() {
		// 70 % warm threshold crossed
		assertEquals(1_000_001, HeapGrowth.choose(1_000_000, 1_000_001, 4, 2048 * MIB, 1500 * MIB));
		// reserve exhausted
		assertEquals(10, HeapGrowth.choose(8, 10, 8, 64 * MIB, 63 * MIB));
	}

	@Test
	public void noMeasurementMeansPlainDoubling() {
		assertEquals(200, HeapGrowth.choose(100, 101, 4, 0, 0));
		assertEquals(200, HeapGrowth.choose(100, 101, 4, Long.MAX_VALUE, 5));
	}

	@Test
	public void neverShrinksAndRejectsBadWidths() {
		assertEquals(100, HeapGrowth.choose(100, 50, 4, 2048 * MIB, 0));
		try {
			HeapGrowth.choose(1, 2, 3, 0, 0);
			throw new AssertionError("width 3 accepted");
		} catch (final IllegalArgumentException expected) {
			// expected
		}
	}
}
