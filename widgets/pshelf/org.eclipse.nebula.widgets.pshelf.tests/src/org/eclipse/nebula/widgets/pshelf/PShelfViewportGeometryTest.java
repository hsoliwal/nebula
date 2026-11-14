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
package org.eclipse.nebula.widgets.pshelf;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

/**
 * PShelfViewportGeometry gives the same header positions as the original PShelf layout loops, for the steady layout and
 * every animation frame, including empty, negative and overflowing sizes.
 */
public class PShelfViewportGeometryTest {

	@Test
	public void steadyAndAnimatedMatchTheOriginalLayout() {
		final double[] progress = { 0d, .000001d, .02d, .25d, .5d, .75d, .98d, 1d };
		for (int count = 0; count <= 20; count++) {
			for (int open = -1; open < count; open++) {
				for (int client = -80; client <= 100; client += 30) {
					for (int header = -5; header <= 40; header += 9) {
						assertArrayEquals("steady", originalSteady(count, open, -20, client, header),
								PShelfViewportGeometry.steady(count, open, -20, client, header));
						for (int previous = -1; previous < count; previous++) {
							for (final double tick : progress) {
								for (final boolean fromBottom : new boolean[] { false, true }) {
									assertArrayEquals("frame",
											originalAnimated(count, previous, open, -20, client, header, tick,
													fromBottom),
											PShelfViewportGeometry.animated(count, previous, open, -20, client,
													header, tick, fromBottom));
								}
							}
						}
					}
				}
			}
		}
	}

	@Test
	public void randomSizesMatchTheOriginalLayout() {
		final Random random = new Random(0x4d335348454c46L);
		for (int trial = 0; trial < 2000; trial++) {
			final int count = random.nextInt(1025);
			final int previous = random.nextInt(count + 1) - 1;
			final int next = random.nextInt(count + 1) - 1;
			final int y = random.nextInt();
			final int height = random.nextInt();
			final int itemHeight = random.nextInt();
			final double p = random.nextDouble();
			assertArrayEquals("random steady", originalSteady(count, next, y, height, itemHeight),
					PShelfViewportGeometry.steady(count, next, y, height, itemHeight));
			assertArrayEquals("random frame", originalAnimated(count, previous, next, y, height, itemHeight, p, true),
					PShelfViewportGeometry.animated(count, previous, next, y, height, itemHeight, p, true));
		}
	}

	/** The steady layout loop of the original PShelf. */
	private static int[] originalSteady(final int count, final int open, final int clientY, final int clientH,
			final int headerH) {
		final List<Integer> boxed = new ArrayList<>();
		int y = clientY;
		for (int i = 0; i < count; i++) {
			boxed.add(Integer.valueOf(y));
			y += headerH;
			if (i == open) {
				y = clientY + clientH - headerH * (count - i - 1);
			}
		}
		return unbox(boxed);
	}

	/** The animation frame loop of the original PShelf. */
	private static int[] originalAnimated(final int count, final int previous, final int next, final int y,
			final int h, final int header, final double progress, final boolean fromBottom) {
		final List<Integer> boxed = new ArrayList<>();
		int yTop = y;
		int yBottom = y + h - header * (count - (next + 1));
		final int area = h - header * count;
		final int gap = (int) (area * (fromBottom ? 1 - progress : progress));
		boolean assigned = false;
		for (int i = 0; i < count; i++) {
			if (i <= previous) {
				boxed.add(Integer.valueOf(yTop));
				yTop += header;
			} else if (i > next) {
				boxed.add(Integer.valueOf(yBottom));
				yBottom += header;
			} else {
				if (!assigned) {
					yTop += gap;
					assigned = true;
				}
				boxed.add(Integer.valueOf(yTop));
				yTop += header;
			}
		}
		return unbox(boxed);
	}

	private static int[] unbox(final List<Integer> boxed) {
		final int[] result = new int[boxed.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = boxed.get(i).intValue();
		}
		return result;
	}
}
