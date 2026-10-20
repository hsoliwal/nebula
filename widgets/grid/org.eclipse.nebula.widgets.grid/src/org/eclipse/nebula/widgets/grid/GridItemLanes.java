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

import java.util.Arrays;

import org.eclipse.nebula.widgets.grid.model.HeapGrowth;

/**
 * The row state of a Grid's items in lanes (DOOP, brief rule 18; one model, no second store): per
 * item a height, a tree level and a byte of flags (visible, expanded, has children, data
 * answered). A {@link GridItem} holds only its lane id; its getters and setters read and write
 * here. Ids are recycled through a free list when an item is disposed or released. Both modes
 * keep their item state here; the row tree's extents are an index derived from it.
 */
final class GridItemLanes {
	static final int VISIBLE = 1;
	static final int EXPANDED = 2;
	static final int HAS_CHILDREN = 4;
	static final int HAS_SET_DATA = 8;

	/* bytes per lane id: height, level (int), flags (byte), the free list's int */
	private static final int LANE_BYTES = 13;

	private int[] heights = new int[0];
	private int[] levels = new int[0];
	private byte[] flags = new byte[0];
	private int[] free = new int[0];
	private int freeCount;
	private int next;
	private int live;

	/**
	 * @param height the item's height
	 * @param level  its tree level
	 * @param bits   its flags
	 * @return a lane id for a new item
	 */
	int allocate(final int height, final int level, final int bits) {
		final int id;
		if (freeCount > 0) {
			id = free[--freeCount];
		} else {
			if (next == heights.length) {
				final int capacity = HeapGrowth.capacity(heights.length, next + 1, LANE_BYTES);
				final int[] nextHeights = Arrays.copyOf(heights, capacity);
				final int[] nextLevels = Arrays.copyOf(levels, capacity);
				final byte[] nextFlags = Arrays.copyOf(flags, capacity);
				heights = nextHeights;
				levels = nextLevels;
				flags = nextFlags;
			}
			id = next++;
		}
		heights[id] = height;
		levels[id] = level;
		flags[id] = (byte) bits;
		live++;
		return id;
	}

	/** Gives a lane id back (the item is disposed or released). */
	void release(final int id) {
		if (freeCount == free.length) {
			free = Arrays.copyOf(free, Math.max(8, free.length * 2));
		}
		free[freeCount++] = id;
		live--;
	}

	/** @return the number of ids in use */
	int live() {
		return live;
	}

	int height(final int id) {
		return heights[id];
	}

	void setHeight(final int id, final int height) {
		heights[id] = height;
	}

	int level(final int id) {
		return levels[id];
	}

	void setLevel(final int id, final int level) {
		levels[id] = level;
	}

	boolean flag(final int id, final int bit) {
		return (flags[id] & bit) != 0;
	}

	/** Gives every id the height {@code height} (the Grid's item height for all rows), O(ids). */
	void fillHeights(final int height) {
		Arrays.fill(heights, 0, next, height);
	}

	void setFlag(final int id, final int bit, final boolean on) {
		flags[id] = (byte) (on ? flags[id] | bit : flags[id] & ~bit);
	}
}
