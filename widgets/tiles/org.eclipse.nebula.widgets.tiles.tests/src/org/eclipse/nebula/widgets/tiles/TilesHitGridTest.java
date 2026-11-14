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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The indexed hit lookup of a static frame returns exactly the tile that the ordered, inclusive-edge scan of the
 * rendered tiles returns, for positive, zero, negative, edge and signed-overflow frame geometry; frames that are not an
 * exact grid stay on the scan.
 */
public class TilesHitGridTest {

	private static final long SEED = 0x4d3354494c45534cL;

	private Shell shell;
	private FrameStatic<Integer> frame;

	@Before
	public void setUp() {
		shell = new Shell(Display.getDefault());
		frame = new FrameStatic<>(new Tiles<Integer>(shell, SWT.NONE));
	}

	@After
	public void tearDown() {
		frame.dispose();
		shell.dispose();
	}

	@Test
	public void indexedHitMatchesOrderedScan() {
		final Random r = new Random(SEED);
		for (final int width : new int[] { 0, 1, 16, 24, 64, 128, 256, 480, 1920, Integer.MAX_VALUE,
				Integer.MIN_VALUE }) {
			for (final int height : new int[] { 0, 1, 32, 128, 768, 1920, Integer.MAX_VALUE, Integer.MIN_VALUE }) {
				for (final int tileWidth : new int[] { -1, 0, 1, 12, 32, 96, Integer.MAX_VALUE }) {
					caseRun(width, height, tileWidth, 20, 5, 4, 70, r);
				}
			}
		}
		for (int i = 0; i < 4000; i++) {
			final int w = i % 5 == 0 ? r.nextInt() : r.nextInt(1800);
			final int h = i % 7 == 0 ? r.nextInt() : r.nextInt(1200);
			final int tw = i % 9 == 0 ? r.nextInt() : r.nextInt(80) - 10;
			final int th = i % 11 == 0 ? r.nextInt() : r.nextInt(80) - 10;
			final int mx = i % 13 == 0 ? r.nextInt() : r.nextInt(30) - 5;
			final int my = i % 17 == 0 ? r.nextInt() : r.nextInt(30) - 5;
			caseRun(w, h, tw, th, mx, my, r.nextInt(1000) + 1, r);
		}
		layout(3000, 3000, 20, 20, 5, 5, 10000);
		assertTrue("large exact grid is indexed", frame.hasIndexedHitGrid());
		assertTrue(frame.getTiles().size() >= 5000);
		for (int i = 0; i < 60000; i++) {
			assertHit(r.nextInt(3200) - 100, r.nextInt(3200) - 100);
		}
		final Tile<Integer> t = frame.getTiles().get(99);
		frame.getTiles().set(99, new Tile<>(t.item, t.x + 1, t.y, t.width, t.height, t.label, t.lineWidth,
				t.lineStyle, t.lineColor, t.foregroundColor, t.backgroundColor));
		assertFalse("a moved tile leaves the indexed lookup", frame.hasIndexedHitGrid());
		assertHit(t.x, t.y);
		assertHit(t.x + 1, t.y);
		assertHit(t.x + t.width + 1, t.y + t.height);
	}

	private void caseRun(final int width, final int height, final int tileWidth, final int tileHeight,
			final int marginX, final int marginY, final int requested, final Random r) {
		layout(width, height, tileWidth, tileHeight, marginX, marginY, requested);
		final List<Tile<Integer>> tiles = frame.getTiles();
		for (int i = 0; i < 24; i++) {
			int x;
			int y;
			if (!tiles.isEmpty() && i < 12) {
				final Tile<Integer> t = tiles.get(r.nextInt(tiles.size()));
				x = probe(i, t.x, t.width, r);
				y = probe(i, t.y, t.height, r);
			} else {
				x = r.nextBoolean() ? r.nextInt() : r.nextInt(2048) - 512;
				y = r.nextBoolean() ? r.nextInt() : r.nextInt(2048) - 512;
			}
			assertHit(x, y);
		}
	}

	/** The tile's start edge, end edge, one outside each, then a point near the start. */
	private static int probe(final int i, final int start, final int extent, final Random r) {
		switch (i % 6) {
		case 0:
			return start;
		case 1:
			return start + extent;
		case 2:
			return start - 1;
		case 3:
			return start + extent + 1;
		default:
			return start + r.nextInt(20) - 10;
		}
	}

	private void layout(final int width, final int height, final int tileWidth, final int tileHeight,
			final int marginX, final int marginY, final int count) {
		final List<Integer> items = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			items.add(Integer.valueOf(i));
		}
		frame.setWidth(width);
		frame.setHeight(height);
		frame.setTileWidth(tileWidth);
		frame.setTileHeight(tileHeight);
		frame.setTileMarginX(marginX);
		frame.setTileMarginY(marginY);
		frame.setItems(items);
		frame.update();
	}

	private void assertHit(final int x, final int y) {
		assertSame("hit at " + x + "," + y, scan(frame.getTiles(), x, y), hit(frame, x, y));
	}

	/** The lookup of Tiles.getTileAt for a static frame. */
	static <T> Tile<T> hit(final FrameStatic<T> frame, final int x, final int y) {
		return frame.hasIndexedHitGrid() ? frame.indexedTileAt(x, y) : scan(frame.getTiles(), x, y);
	}

	/** The ordered first-hit scan with inclusive edges (int arithmetic, as in Tiles.getTileAt). */
	static <T> Tile<T> scan(final List<Tile<T>> tiles, final int x, final int y) {
		for (final Tile<T> tile : tiles) {
			if (x >= tile.x && y >= tile.y && x <= tile.x + tile.width && y <= tile.y + tile.height) {
				return tile;
			}
		}
		return null;
	}
}
