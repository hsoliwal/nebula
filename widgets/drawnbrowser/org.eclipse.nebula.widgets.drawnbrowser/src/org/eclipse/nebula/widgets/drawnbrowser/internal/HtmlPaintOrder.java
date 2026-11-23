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
 * The root and positioned boxes with an integer z-index form atomic stacking
 * contexts; fixed boxes do so even with an automatic z-index. A relatively or
 * absolutely positioned box with an automatic z-index is only a paint group: its
 * positioned descendants still belong to the nearest actual stacking context.
 * Static boxes ignore z-index. Negative contexts paint after their context's own
 * background, before its normal-flow content. Equal stack levels retain document
 * tree order. These are the block/content phases of CSS 2.2 section 9.9 and
 * Appendix E; floats, inline-blocks, opacity and transforms are outside this
 * renderer's current layout subset.
 * </p>
 * <p>
 * The plan snapshots the document's style lanes. Rebuild it after changing those
 * lanes, not on each repaint or scroll. Construction uses primitive arrays,
 * O((boxes + blocks) log(boxes + blocks)) work and linear temporary storage, with
 * no recursion or per-entry objects. A page without positioned boxes takes a
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
		int positioned = 0;
		for (int box = 1; box < document.boxCount; box++) {
			if (document.boxPosition[box] != HtmlStyles.STATIC) positioned++;
		}
		if (positioned == 0) {
			for (int box = 0; box < document.boxCount; box++) steps[box] = ~box;
			for (int block = 0; block < document.blockCount; block++) steps[document.boxCount + block] = block;
			return;
		}
		build(document, positioned);
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

	private void build(HtmlDocument document, int positioned) {
		int boxes = document.boxCount;
		int entries = Math.addExact(steps.length, positioned);
		int[] owner = new int[entries], index = new int[entries];
		byte[] phase = new byte[entries];
		int[] context = new int[boxes], group = new int[boxes];
		int entry = 0;
		for (int box = 0; box < boxes; box++) {
			if (box != 0) {
				int parent = document.boxParent[box];
				boolean isPositioned = document.boxPosition[box] != HtmlStyles.STATIC;
				boolean isContext = isPositioned && (!document.boxZAuto[box]
						|| document.boxPosition[box] == HtmlStyles.FIXED || document.boxPosition[box] == HtmlStyles.STICKY);
				context[box] = isContext ? box : context[parent];
				group[box] = isPositioned ? box : group[parent];
				if (isPositioned) {
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
		sort(order, owner, phase, index, document);
		emit(order, start, phase, index, boxes);
	}

	/* Sort by owning group, CSS phase, stack level, then the original tree order. */
	private static void sort(int[] order, int[] owner, byte[] phase, int[] index, HtmlDocument document) {
		int[] work = new int[order.length];
		for (int width = 1; width < order.length;) {
			int begin = 0;
			while (begin < order.length) {
				int middle = begin + Math.min(width, order.length - begin);
				int end = middle + Math.min(width, order.length - middle);
				int left = begin, right = middle;
				for (int target = begin; target < end; target++) {
					if (right == end || (left < middle && before(order[left], order[right], owner, phase, index, document))) {
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

	private static boolean before(int a, int b, int[] owner, byte[] phase, int[] index, HtmlDocument document) {
		if (owner[a] != owner[b]) return owner[a] < owner[b];
		if (phase[a] != phase[b]) return phase[a] < phase[b];
		if (phase[a] == NEGATIVE || phase[a] == POSITIVE) {
			int az = document.boxZIndex[index[a]], bz = document.boxZIndex[index[b]];
			if (az != bz) return az < bz;
		}
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
