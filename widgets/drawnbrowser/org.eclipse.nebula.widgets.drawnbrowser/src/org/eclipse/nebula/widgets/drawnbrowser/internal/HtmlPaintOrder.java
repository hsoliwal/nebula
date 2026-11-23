/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import java.util.Objects;

/**
 * An immutable paint sequence for the drawn browser's block and inline-content
 * subset of CSS stacking. Each box contributes a background fragment; each text,
 * image, rule or cell block contributes a content fragment. Painting walks forward
 * and picking walks backward through the same sequence.
 * <p>
 * The root, positioned boxes with an integer z-index, and flex/grid items with an
 * integer z-index form atomic stacking contexts; fixed and sticky boxes do so even
 * with an automatic z-index. A relatively or
 * absolutely positioned box with an automatic z-index is only a paint group: its
 * positioned descendants still belong to the nearest actual stacking context.
 * Other static boxes ignore z-index. Negative contexts paint after their context's own
 * background, before its normal-flow content. Equal stack levels retain document
 * tree order. Within an ordinary table's flow backgrounds, every row precedes
 * every cell, including cells spanning later rows. These are the block/content
 * phases of CSS 2.2 section 9.9, Appendix E and section 17.5.1, with the item
 * z-index rules of Flexbox section 4.3 and Grid section 6.5. Floats, arbitrary
 * inline-blocks, opacity and transforms are outside this renderer's current subset.
 * </p>
 * <p>
 * The plan snapshots the document's style lanes. Rebuild it after changing those
 * lanes, not on each repaint or scroll. Construction uses primitive arrays,
 * O((boxes + blocks) log(boxes + blocks)) work and linear temporary storage, with
 * no recursion or per-entry objects. A page without paint groups takes a
 * linear path. A stacking context does not imply a clip or a coordinate system.
 * </p>
 */
public final class HtmlPaintOrder {

	private static final byte OWN_BACKGROUND = 0, NEGATIVE = 1, FLOW_BACKGROUND = 2,
			CONTENT = 3, ZERO = 4, POSITIVE = 5;

	/* A negative value is the bitwise complement of a box index; otherwise a block. */
	private final int[] steps;

	/** Captures the paint order of the document's preorder box and block lanes. */
	public HtmlPaintOrder(HtmlDocument document) {
		Objects.requireNonNull(document, "document");
		steps = new int[Math.addExact(document.boxCount, document.blockCount)];
		int groups = 0;
		for (int box = 1; box < document.boxCount; box++) {
			if (isPaintGroup(document, box)) groups++;
		}
		int[] backgroundRank = backgroundRanks(document);
		if (groups == 0) {
			for (int box = 0; box < document.boxCount; box++) steps[backgroundRank == null ? box : backgroundRank[box]] = ~box;
			for (int block = 0; block < document.blockCount; block++) steps[document.boxCount + block] = block;
			return;
		}
		build(document, groups, backgroundRank);
	}

	/** Number of background and content fragments in this snapshot. */
	public int size() {
		return steps.length;
	}

	/** Whether step {@code step} paints a box background rather than block content. */
	public boolean isBackground(int step) {
		return steps[step] < 0;
	}

	/** The box index for a background step, or block index for a content step. */
	public int index(int step) {
		int entry = steps[step];
		return entry < 0 ? ~entry : entry;
	}

	/* A layout item with integer z-index changes painting, never its positioning or containing block. */
	private static boolean isPaintGroup(HtmlDocument document, int box) {
		if (document.boxPosition[box] != HtmlStyles.STATIC) return true;
		if (document.boxZAuto[box] || document.boxRole[box] != HtmlDocument.CELL_BOX) return false;
		int parent = document.boxParent[box];
		return parent >= 0 && (document.boxRole[parent] == HtmlDocument.FLEX_BOX || document.boxRole[parent] == HtmlDocument.GRID_BOX);
	}

	/*
	 * Stable partition of each table's row/cell background slots. Other slots, including captions and unrelated
	 * siblings, keep their original ranks; a global cell phase would wrongly lift cells over later subtrees.
	 * The parser's ordinary table boxes have direct rows with direct cells. Every child edge is visited a bounded
	 * number of times, and one primitive workspace is reused across tables. Positioned groups retain their phases.
	 */
	private static int[] backgroundRanks(HtmlDocument document) {
		int[] rank = null, slots = null;
		for (int table = 1; table < document.boxCount; table++) {
			if (document.boxRole[table] != HtmlDocument.TABLE_BOX) continue;
			if (rank == null) {
				rank = new int[document.boxCount];
				slots = new int[document.boxCount];
				for (int box = 0; box < rank.length; box++) rank[box] = box;
			}
			int count = 0;
			for (int row = document.boxFirstChild[table]; row >= 0; row = document.boxNextSibling[row]) {
				if (document.boxRole[row] != HtmlDocument.ROW_BOX) continue;
				slots[count++] = row;
				for (int cell = document.boxFirstChild[row]; cell >= 0; cell = document.boxNextSibling[cell]) {
					if (document.boxRole[cell] == HtmlDocument.CELL_BOX) slots[count++] = cell;
				}
			}
			int next = 0;
			for (int row = document.boxFirstChild[table]; row >= 0; row = document.boxNextSibling[row]) {
				if (document.boxRole[row] == HtmlDocument.ROW_BOX) rank[row] = slots[next++];
			}
			for (int row = document.boxFirstChild[table]; row >= 0; row = document.boxNextSibling[row]) {
				if (document.boxRole[row] != HtmlDocument.ROW_BOX) continue;
				for (int cell = document.boxFirstChild[row]; cell >= 0; cell = document.boxNextSibling[cell]) {
					if (document.boxRole[cell] == HtmlDocument.CELL_BOX) rank[cell] = slots[next++];
				}
			}
		}
		return rank;
	}

	private void build(HtmlDocument document, int groups, int[] backgroundRank) {
		int boxes = document.boxCount;
		int entries = Math.addExact(steps.length, groups);
		int[] owner = new int[entries], index = new int[entries];
		byte[] phase = new byte[entries];
		int[] context = new int[boxes], group = new int[boxes];
		int entry = 0;
		for (int box = 0; box < boxes; box++) {
			if (box != 0) {
				int parent = document.boxParent[box];
				boolean hasGroup = isPaintGroup(document, box);
				boolean isContext = hasGroup && (!document.boxZAuto[box]
						|| document.boxPosition[box] == HtmlStyles.FIXED || document.boxPosition[box] == HtmlStyles.STICKY);
				context[box] = isContext ? box : context[parent];
				group[box] = hasGroup ? box : group[parent];
				if (hasGroup) {
					int z = document.boxZAuto[box] ? 0 : document.boxZIndex[box];
					owner[entry] = context[parent];
					index[entry] = box;
					phase[entry++] = z < 0 ? NEGATIVE : z > 0 ? POSITIVE : ZERO;
				}
			}
			owner[entry] = group[box];
			index[entry] = box;
			phase[entry++] = group[box] == box ? OWN_BACKGROUND : FLOW_BACKGROUND;
		}
		for (int block = 0; block < document.blockCount; block++) {
			owner[entry] = group[document.blockBox[block]];
			index[entry] = block;
			phase[entry++] = CONTENT;
		}

		int[] order = new int[entries];
		int[] start = new int[boxes + 1];
		for (int e = 0; e < entries; e++) {
			order[e] = e;
			start[owner[e] + 1]++;
		}
		for (int box = 1; box <= boxes; box++) start[box] += start[box - 1];
		sort(order, owner, phase, index, document, backgroundRank);
		emit(order, start, phase, index, boxes);
	}

	/* Sort by owning group, CSS phase, stack level, then the original tree order. */
	private static void sort(int[] order, int[] owner, byte[] phase, int[] index, HtmlDocument document, int[] backgroundRank) {
		int[] work = new int[order.length];
		for (int width = 1; width < order.length;) {
			int begin = 0;
			while (begin < order.length) {
				int middle = begin + Math.min(width, order.length - begin);
				int end = middle + Math.min(width, order.length - middle);
				int left = begin, right = middle;
				for (int target = begin; target < end; target++) {
					if (right == end || (left < middle && before(order[left], order[right], owner, phase, index, document, backgroundRank))) {
						work[target] = order[left++];
					} else {
						work[target] = order[right++];
					}
				}
				begin = end;
			}
			System.arraycopy(work, 0, order, 0, order.length);
			if (width > order.length / 2) break;
			width *= 2;
		}
	}

	private static boolean before(int a, int b, int[] owner, byte[] phase, int[] index, HtmlDocument document, int[] backgroundRank) {
		if (owner[a] != owner[b]) return owner[a] < owner[b];
		if (phase[a] != phase[b]) return phase[a] < phase[b];
		if (phase[a] == NEGATIVE || phase[a] == POSITIVE) {
			int az = document.boxZIndex[index[a]], bz = document.boxZIndex[index[b]];
			if (az != bz) return az < bz;
		}
		if (phase[a] == FLOW_BACKGROUND && backgroundRank != null) return backgroundRank[index[a]] <= backgroundRank[index[b]];
		return index[a] <= index[b];
	}

	/* A primitive stack expands group calls without using the Java call stack. */
	private void emit(int[] order, int[] start, byte[] phase, int[] index, int boxes) {
		int[] cursor = new int[boxes], end = new int[boxes];
		int depth = 0, target = 0;
		cursor[0] = start[0];
		end[0] = start[1];
		while (depth >= 0) {
			if (cursor[depth] == end[depth]) {
				depth--;
				continue;
			}
			int entry = order[cursor[depth]++];
			byte current = phase[entry];
			int value = index[entry];
			if (current == NEGATIVE || current == ZERO || current == POSITIVE) {
				depth++;
				cursor[depth] = start[value];
				end[depth] = start[value + 1];
			} else {
				steps[target++] = current == CONTENT ? value : ~value;
			}
		}
	}
}
