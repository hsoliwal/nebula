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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.graphics.Point;
import org.junit.Test;

/**
 * {@link GridCellSelectionList} answers as the stock {@code ArrayList<Point>}: random adds (with
 * duplicates), removals by value and index, bulk removals, removeIf, in-place shifts followed by
 * {@code reindex}, clears and contains, compared after every step.
 */
public class GridCellSelectionList_Test {

	@Test
	public void randomOperationsAnswerAsAnArrayList() {
		final GridCellSelectionList list = new GridCellSelectionList();
		final List<Point> reference = new ArrayList<>();
		final Random random = new Random(0xCE11L);
		for (int step = 0; step < 30_000; step++) {
			final Point p = new Point(random.nextInt(6), random.nextInt(40));
			switch (random.nextInt(10)) {
			case 0:
			case 1:
			case 2:
				list.add(p);
				reference.add(new Point(p.x, p.y));
				break;
			case 3:
			case 4:
				assertEquals(reference.remove(p), list.remove(p));
				break;
			case 5:
				if (!reference.isEmpty()) {
					final int index = random.nextInt(reference.size());
					assertEquals(reference.remove(index), list.remove(index));
				}
				break;
			case 6:
				final List<Point> some = Arrays.asList(p, new Point(p.x, (p.y + 1) % 40));
				assertEquals(reference.removeAll(some), list.removeAll(some));
				break;
			case 7:
				final int column = random.nextInt(6);
				assertEquals(reference.removeIf(c -> c.x == column), list.removeIf(c -> c.x == column));
				break;
			case 8:
				// the Grid's shift after a column removal: in place, then reindex
				final int from = random.nextInt(6);
				for (final Point c : reference) {
					if (c.x >= from) {
						c.x++;
					}
				}
				for (final Point c : list) {
					if (c.x >= from) {
						c.x++;
					}
				}
				list.reindex();
				break;
			default:
				if (random.nextInt(80) == 0) {
					list.clear();
					reference.clear();
				}
			}
			assertEquals(reference.size(), list.size());
			assertEquals(reference.contains(p), list.contains(p));
			if (step % 101 == 0) {
				assertArrayEquals(reference.toArray(new Point[0]), list.toArray(new Point[0]));
				assertEquals(reference.containsAll(Arrays.asList(p)), list.containsAll(Arrays.asList(p)));
			}
		}
	}

	@Test
	public void handsOutCopies() {
		final GridCellSelectionList list = new GridCellSelectionList();
		final Point p = new Point(1, 2);
		list.add(p);
		p.x = 5;
		list.toArray(new Point[0])[0].y = 9;
		assertEquals(new Point(1, 2), list.get(0));
		assertEquals(true, list.contains(new Point(1, 2)));
	}
}
