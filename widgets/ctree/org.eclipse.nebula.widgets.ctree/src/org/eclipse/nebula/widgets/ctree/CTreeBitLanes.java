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
package org.eclipse.nebula.widgets.ctree;

/**
 * Per-item and per-cell boolean state of one {@link CTree}, kept in the tree as packed
 * bit lanes indexed by a slot that the tree hands out when an item or cell is created.
 * Items and cells keep only their slot; lanes grow by doubling and are never allocated
 * per frame. Int-only and GWT-safe.
 */
final class CTreeBitLanes {
	//FLLDCIM: HPCPOP — booleans as bit lanes in the parent, not as per-object fields
	static final int ITEM_PAINTED = 0;
	static final int ITEM_AUTO_HEIGHT = 1;
	static final int CELL_PAINTED = 2;
	static final int CELL_HAS_CHILD = 3;
	static final int CELL_INITED = 4;
	static final int CELL_HOLD_CHILD = 5;
	static final int CELL_CHECKABLE = 6;
	static final int CELL_CHECKED = 7;
	private static final int LANES = 8;

	private final int[][] lanes = new int[LANES][];
	private int slots;

	CTreeBitLanes() {
		for (int i = 0; i < LANES; i++) {
			lanes[i] = new int[2];
		}
	}

	/** @return a fresh slot whose bits are all clear */
	int allocate() {
		int slot = slots++;
		int word = slot >>> 5;
		if (word >= lanes[0].length) {
			for (int i = 0; i < LANES; i++) {
				int[] grown = new int[lanes[i].length * 2];
				System.arraycopy(lanes[i], 0, grown, 0, lanes[i].length);
				lanes[i] = grown;
			}
		}
		return slot;
	}

	boolean get(int lane, int slot) {
		return (lanes[lane][slot >>> 5] & 1 << (slot & 31)) != 0;
	}

	void set(int lane, int slot, boolean value) {
		if (value) {
			lanes[lane][slot >>> 5] |= 1 << (slot & 31);
		} else {
			lanes[lane][slot >>> 5] &= ~(1 << (slot & 31));
		}
	}

	/** @return number of set bits of a lane, word-wise */
	int count(int lane) {
		int n = 0;
		int[] bits = lanes[lane];
		for (int i = 0, words = (slots + 31) >>> 5; i < words; i++) {
			n += Integer.bitCount(bits[i]);
		}
		return n;
	}
}
