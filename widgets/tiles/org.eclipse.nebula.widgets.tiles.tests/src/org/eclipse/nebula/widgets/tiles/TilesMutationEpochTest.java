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
package org.eclipse.nebula.widgets.tiles;

import static org.eclipse.nebula.widgets.tiles.TilesHitGridTest.hit;
import static org.eclipse.nebula.widgets.tiles.TilesHitGridTest.scan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The list returned by FrameStatic.getTiles() stays mutable; every edit through it (including same-size set and swap,
 * subList, listIterator, replaceAll, sort, removeIf and iterator removal, and edits through a reference kept across an
 * update) takes the frame off its indexed hit lookup until the next update, so a hit always equals the ordered scan.
 */
public class TilesMutationEpochTest {

	private static final int COLUMNS = 119;
	private static final int COUNT = 10000;
	private static final int STEP = 25;

	private Shell shell;
	private FrameStatic<Integer> frame;

	@Before
	public void setUp() {
		shell = new Shell(Display.getDefault());
		frame = new FrameStatic<>(new Tiles<Integer>(shell, SWT.NONE));
		final List<Integer> items = new ArrayList<>(COUNT);
		for (int i = 0; i < COUNT; i++) {
			items.add(Integer.valueOf(i));
		}
		frame.setWidth(5 + COLUMNS * STEP);
		frame.setHeight(2500);
		frame.setTileWidth(20);
		frame.setTileHeight(20);
		frame.setTileMarginX(5);
		frame.setTileMarginY(5);
		frame.setItems(items);
	}

	@After
	public void tearDown() {
		frame.dispose();
		shell.dispose();
	}

	@Test
	public void editsThroughTheTileListLeaveTheIndex() {
		final Random random = new Random(0x4d3354494c45534cL);
		rebuild();
		assertTrue(frame.getTiles().size() == COUNT);
		randomHits(random, 40000);

		final Tile<Integer> fifth = frame.getTiles().get(5);
		final Tile<Integer> sixth = frame.getTiles().get(6);
		Collections.swap(frame.getTiles(), 5, 6);
		final Tile<Integer> old = scan(frame.getTiles(), fifth.x + 2, fifth.y + 2);
		assertSame("the scan keeps tile identity", fifth, old);
		assertSame(sixth, frame.getTiles().get(5));
		assertSame(fifth, frame.getTiles().get(6));
		assertTrue(fifth.x + 2 < sixth.x);
		assertNotSame("a lookup by slot alone would miss the swapped tile", old, slotOnly(fifth.x + 2, fifth.y + 2));
		assertFalse("a same-size swap leaves the index", frame.hasIndexedHitGrid());
		randomHits(random, 10000);

		rebuild();
		final List<Tile<Integer>> slice = frame.getTiles().subList(100, 102);
		assertFalse("subList leaves the index", frame.hasIndexedHitGrid());
		Collections.swap(slice, 0, 1);
		randomHits(random, 1500);

		rebuild();
		final ListIterator<Tile<Integer>> iter = frame.getTiles().listIterator(10);
		assertFalse("listIterator leaves the index", frame.hasIndexedHitGrid());
		final Tile<Integer> at10 = iter.next();
		iter.set(frame.getTiles().get(11));
		assertHit(at10.x + 3, at10.y + 3);

		rebuild();
		frame.getTiles().removeIf(t -> t.item.intValue() % 777 == 0);
		assertFalse("removeIf leaves the index", frame.hasIndexedHitGrid());
		assertHit(8, 8);

		rebuild();
		frame.getTiles().replaceAll(t -> new Tile<>(Integer.valueOf(t.item.intValue() + 1), t.x, t.y, t.width,
				t.height, t.label, t.lineWidth, t.lineStyle, t.lineColor, t.foregroundColor, t.backgroundColor));
		assertFalse("replaceAll leaves the index", frame.hasIndexedHitGrid());
		assertHit(8, 8);

		rebuild();
		frame.getTiles().replaceAll(t -> {
			if (t.item.intValue() == 7) {
				assertFalse("a replaceAll operator sees the scan", frame.hasIndexedHitGrid());
			}
			return t;
		});
		assertHit(8, 8);

		rebuild();
		frame.getTiles().sort((a, b) -> {
			assertFalse("a sort comparator sees the scan", frame.hasIndexedHitGrid());
			return Integer.compare(b.item.intValue(), a.item.intValue());
		});
		assertFalse("sort leaves the index", frame.hasIndexedHitGrid());
		assertHit(8, 8);

		rebuild();
		frame.getTiles().removeIf(t -> {
			if (t.item.intValue() == 7) {
				assertFalse("a removeIf predicate sees the scan", frame.hasIndexedHitGrid());
			}
			return false;
		});
		assertFalse("a removeIf that removes nothing still leaves the index", frame.hasIndexedHitGrid());

		rebuild();
		final Iterator<Tile<Integer>> iterator = frame.getTiles().iterator();
		iterator.next();
		iterator.remove();
		assertFalse("iterator.remove leaves the index", frame.hasIndexedHitGrid());
		assertHit(8, 8);

		final List<Tile<Integer>> escaped = frame.getTiles();
		rebuild();
		assertSame("update keeps the same list", escaped, frame.getTiles());
		escaped.set(1, escaped.get(2));
		assertFalse("an edit through a kept reference leaves the index", frame.hasIndexedHitGrid());
		assertHit(32, 7);
		assertTrue(frame.getTiles() instanceof ArrayList<?>);
	}

	private void rebuild() {
		frame.update();
		assertTrue("the exact grid is indexed after update", frame.hasIndexedHitGrid());
	}

	private void randomHits(final Random random, final int count) {
		for (int i = 0; i < count; i++) {
			assertHit(random.nextInt(3100) - 50, random.nextInt(2500) - 50);
		}
	}

	private void assertHit(final int x, final int y) {
		assertSame("hit at " + x + "," + y, scan(frame.getTiles(), x, y), hit(frame, x, y));
	}

	/** A lookup that trusts the slot of the grid laid out by the last update (what the index must not do). */
	private Tile<Integer> slotOnly(final int x, final int y) {
		final List<Tile<Integer>> rendered = frame.getTiles();
		final Tile<Integer> first = rendered.get(0);
		if (x < first.x || y < first.y) {
			return null;
		}
		final long col = ((long) x - first.x) / STEP;
		final long row = ((long) y - first.y) / STEP;
		if (col >= COLUMNS) {
			return null;
		}
		final long slot = row * COLUMNS + col;
		if (slot >= rendered.size()) {
			return null;
		}
		final Tile<Integer> t = rendered.get((int) slot);
		return x >= t.x && y >= t.y && x <= t.x + t.width && y <= t.y + t.height ? t : null;
	}
}
