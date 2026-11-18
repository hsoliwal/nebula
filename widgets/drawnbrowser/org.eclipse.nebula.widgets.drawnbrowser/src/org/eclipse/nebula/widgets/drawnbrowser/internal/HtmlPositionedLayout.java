/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import java.util.Arrays;

/**
 * Geometry for the drawn browser's positioned block subset. Normal-flow allocation, containing blocks and visual
 * offsets are separate primitive lanes. A relative offset never changes the next sibling's flow position; absolute
 * and fixed boxes take no flow space. Fixed coordinates are projected only when queried, so scrolling does not
 * measure text again. This is block layout in the existing renderer, not a complete CSS formatting engine.
 * <p>
 * The root's containing-block size is the viewport, even when the document is taller. Physical integer pixel insets
 * and sizes use the CSS left-to-right rules. Margins, padding and borders belong to their owning box; legacy block
 * gaps remain outside a styled leaf's border. Percentages, inline positioning, floats, transforms and
 * overflow clips are outside this subset. Overflow remains visible. Positioned documents resolve their geometry as
 * a whole; ordinary documents retain {@link HtmlLayout}'s lazy prefix layout. TextLayout residency remains bounded.
 * </p>
 */
public final class HtmlPositionedLayout {

	/** Measures with the receiving widget's fonts, text engine, image sizes and existing automatic-table algorithm. */
	public interface Metrics {
		int height(int block, int wrapWidth);
		/** Minimum width in the high 32 bits and preferred width in the low 32 bits. */
		long intrinsicWidths(int block);
		int[] columns(int table, int availableWidth);
	}

	private final HtmlDocument doc;
	private final HtmlPaintOrder order;
	private final int[] x, y, width, height, localY, containing, tableOf;
	private final int[] before, after, leadGap, trailGap, extraIndent, accountedIndent;
	private final boolean[] fixed;
	private final int[] blockX, blockY, blockWidth, blockHeight, blockInset, blockContentTop;
	private final int[] minimum, preferred;
	private final int[][] columns;
	private int viewportWidth, viewportHeight, totalHeight;
	private boolean hasFixed;
	/* Sorted vertical intervals and segment maxima: viewport queries skip entire offscreen subtrees. */
	private long[] normalKeys = new long[0], fixedKeys = new long[0], normalMax = new long[0], fixedMax = new long[0];
	private final int[] visible;
	private int visibleCount;

	public HtmlPositionedLayout(HtmlDocument document) {
		doc = document;
		order = new HtmlPaintOrder(document);
		int n = doc.boxCount, b = doc.blockCount;
		x = new int[n]; y = new int[n]; width = new int[n]; height = new int[n]; localY = new int[n];
		containing = new int[n]; tableOf = new int[n]; fixed = new boolean[n];
		before = new int[n]; after = new int[n]; leadGap = new int[n]; trailGap = new int[n];
		extraIndent = new int[n]; accountedIndent = new int[n];
		minimum = new int[n]; preferred = new int[n];
		blockX = new int[b]; blockY = new int[b]; blockWidth = new int[b]; blockHeight = new int[b]; blockInset = new int[b];
		blockContentTop = new int[b];
		columns = new int[doc.tableCount][];
		visible = new int[order.size()];
	}

	/** Resolves all sizes, then positions. Parent box numbers precede their children, so no recursive stack is used. */
	public void layout(int viewWidth, int viewHeight, Metrics metrics) {
		viewportWidth = Math.max(1, viewWidth);
		viewportHeight = Math.max(0, viewHeight);
		Arrays.fill(columns, null);
		Arrays.fill(tableOf, -1);
		Arrays.fill(minimum, 0);
		Arrays.fill(preferred, 0);
		prepareBoxes();
		boolean shrink = false;
		for (int e = 1; e < doc.boxCount; e++) {
			if (outOfFlow(e) && doc.boxWidth[e] == HtmlStyles.AUTO
					&& (doc.boxLeft[e] == HtmlStyles.AUTO || doc.boxRight[e] == HtmlStyles.AUTO)) shrink = true;
		}
		if (shrink) intrinsic(metrics);
		width[0] = viewportWidth;
		for (int e = 1; e < doc.boxCount; e++) resolveWidth(e, metrics);
		for (int e = doc.boxCount - 1; e >= 0; e--) measureContents(e, metrics);
		x[0] = y[0] = 0;
		fixed[0] = false;
		totalHeight = height[0];
		hasFixed = false;
		for (int e = 1; e < doc.boxCount; e++) resolvePosition(e);
		for (int b = 0; b < doc.blockCount; b++) {
			int e = doc.blockBox[b];
			blockX[b] = add(contentX(e), blockInset[b]);
			blockY[b] = add(contentY(e), blockY[b]);
			if (!fixed[e]) totalHeight = Math.max(totalHeight, add(blockY[b], blockHeight[b]));
		}
		totalHeight = Math.max(0, totalHeight);
		indexIntervals();
	}

	/* Lift only already-existing block indentation; parent ownership prevents charging it to every descendant. */
	private void prepareBoxes() {
		before[0] = after[0] = leadGap[0] = trailGap[0] = extraIndent[0] = accountedIndent[0] = 0;
		for (int e = 1; e < doc.boxCount; e++) {
			boolean styled = hasBoxModel(e);
			int first = doc.boxFirstBlock[e], end = doc.boxEndBlock[e];
			boolean ownFirst = first < end && doc.blockBox[first] == e && doc.kind[first] != HtmlDocument.CELL;
			boolean ownLast = first < end && doc.blockBox[end - 1] == e && doc.kind[end - 1] != HtmlDocument.CELL;
			leadGap[e] = styled && ownFirst ? HtmlLayout.BLOCK_GAP : 0;
			trailGap[e] = styled && ownLast ? HtmlLayout.BLOCK_GAP : 0;
			before[e] = styled ? add(doc.boxMarginTop[e], leadGap[e]) : 0;
			after[e] = styled ? add(doc.boxMarginBottom[e], trailGap[e]) : 0;
			int inherited = accountedIndent[doc.boxParent[e]], wanted = inherited;
			if (doc.boxRole[e] == HtmlDocument.TABLE_BOX) {
				for (int b = first; b < end; b++) {
					if (doc.kind[b] == HtmlDocument.CELL) { wanted = doc.blockIndent[b]; break; }
				}
			} else if (styled && ownFirst) {
				wanted = legacyInset(first);
			}
			extraIndent[e] = Math.max(0, add(wanted, -inherited));
			accountedIndent[e] = add(inherited, extraIndent[e]);
		}
	}

	private boolean hasBoxModel(int e) {
		return doc.boxBorder[e] > 0 || doc.boxPadTop[e] > 0 || doc.boxPadRight[e] > 0
				|| doc.boxPadBottom[e] > 0 || doc.boxPadLeft[e] > 0 || doc.boxMarginTop[e] > 0
				|| doc.boxMarginRight[e] > 0 || doc.boxMarginBottom[e] > 0;
	}

	private int horizontalEdges(int e) {
		return clamp(2L * doc.boxBorder[e] + doc.boxPadLeft[e] + doc.boxPadRight[e]);
	}

	private int verticalEdges(int e) {
		return clamp(2L * doc.boxBorder[e] + doc.boxPadTop[e] + doc.boxPadBottom[e]);
	}

	private int contentX(int e) { return add(x[e], add(doc.boxBorder[e], doc.boxPadLeft[e])); }
	private int contentY(int e) { return add(y[e], add(doc.boxBorder[e], doc.boxPadTop[e])); }
	private int contentWidth(int e) { return Math.max(0, add(width[e], -horizontalEdges(e))); }

	private void indexIntervals() {
		int ordinary = 0, stationary = 0;
		for (int s = 0; s < order.size(); s++) {
			if (stepFixed(s)) stationary++; else ordinary++;
		}
		if (normalKeys.length != ordinary) normalKeys = new long[ordinary];
		if (fixedKeys.length != stationary) fixedKeys = new long[stationary];
		ordinary = stationary = 0;
		for (int s = 0; s < order.size(); s++) {
			long key = (long) stepY(s) << 32 | s & 0xFFFFFFFFL;
			if (stepFixed(s)) fixedKeys[stationary++] = key; else normalKeys[ordinary++] = key;
		}
		Arrays.sort(normalKeys);
		Arrays.sort(fixedKeys);
		normalMax = maxima(normalKeys, normalMax);
		fixedMax = maxima(fixedKeys, fixedMax);
	}

	private long[] maxima(long[] keys, long[] tree) {
		int leaves = 1;
		while (leaves < keys.length) leaves *= 2;
		if (tree.length != 2 * leaves) tree = new long[2 * leaves];
		Arrays.fill(tree, Long.MIN_VALUE);
		for (int k = 0; k < keys.length; k++) {
			int step = (int) keys[k];
			tree[leaves + k] = (long) stepY(step) + stepHeight(step);
		}
		for (int k = leaves - 1; k > 0; k--) tree[k] = Math.max(tree[2 * k], tree[2 * k + 1]);
		return tree;
	}

	/**
	 * Reuses one scratch lane for paint steps intersecting a page-space vertical interval, in paint order. Fixed
	 * intervals are queried in viewport coordinates. Its contents last until the next query on this scene.
	 */
	public int collectVisible(int pageTop, int extent, int scroll) {
		visibleCount = 0;
		if (extent <= 0) return 0;
		collect(normalKeys, normalMax, 1, 0, normalMax.length / 2, pageTop, (long) pageTop + extent);
		if (fixedKeys.length != 0 && ((fixedKeys[0] >> 32) + scroll < Integer.MIN_VALUE + 1L
				|| (fixedKeys[fixedKeys.length - 1] >> 32) + scroll > Integer.MAX_VALUE)) {
			// At int endpoints projection saturates. Match the public geometry instead of translating a wrapped query.
			for (long key : fixedKeys) {
				int step = (int) key, top = add(stepY(step), scroll), height = stepHeight(step);
				if (height > 0 && top < (long) pageTop + extent && (long) top + height > pageTop) visible[visibleCount++] = step;
			}
		} else {
			collect(fixedKeys, fixedMax, 1, 0, fixedMax.length / 2, (long) pageTop - scroll, (long) pageTop - scroll + extent);
		}
		Arrays.sort(visible, 0, visibleCount);
		return visibleCount;
	}

	/* Tree depth is logarithmic in the number of fragments, independent of HTML nesting depth. */
	private void collect(long[] keys, long[] tree, int node, int lo, int hi, long top, long bottom) {
		if (lo >= keys.length || tree[node] <= top || (keys[lo] >> 32) >= bottom) return;
		if (hi - lo == 1) {
			int step = (int) keys[lo];
			if (stepHeight(step) > 0) visible[visibleCount++] = step;
			return;
		}
		int mid = lo + (hi - lo) / 2;
		collect(keys, tree, node * 2, lo, mid, top, bottom);
		collect(keys, tree, node * 2 + 1, mid, hi, top, bottom);
	}

	public int visibleStep(int index) { return visible[index]; }

	private boolean stepFixed(int step) {
		int i = order.index(step);
		return fixed[order.isBackground(step) ? i : doc.blockBox[i]];
	}

	private int stepY(int step) {
		int i = order.index(step);
		return order.isBackground(step) ? y[i] : blockY[i];
	}

	private int stepHeight(int step) {
		int i = order.index(step);
		return order.isBackground(step) ? height[i] : blockHeight[i];
	}

	private void intrinsic(Metrics metrics) {
		for (int b = 0; b < doc.blockCount; b++) {
			long pair = metrics.intrinsicWidths(b);
			int e = doc.blockBox[b], inset = doc.kind[b] == HtmlDocument.CELL ? 0 : inset(b);
			minimum[e] = Math.max(minimum[e], add((int) (pair >>> 32), inset));
			preferred[e] = Math.max(preferred[e], add((int) pair, inset));
		}
		for (int e = doc.boxCount - 1; e > 0; e--) {
			if (doc.boxRole[e] == HtmlDocument.TABLE_BOX) {
				int table = tableIndex(e);
				if (table >= 0) {
					int[] min = metrics.columns(table, 0), max = metrics.columns(table, Integer.MAX_VALUE);
					minimum[e] = min[min.length - 1];
					preferred[e] = max[max.length - 1];
				}
			}
			if (doc.boxWidth[e] != HtmlStyles.AUTO) minimum[e] = preferred[e] = doc.boxWidth[e];
			minimum[e] = add(minimum[e], horizontalEdges(e));
			preferred[e] = add(preferred[e], horizontalEdges(e));
			int p = doc.boxParent[e];
			if (!outOfFlow(e)) {
				int margins = clamp((long) doc.boxIndent[e] + doc.boxMarginRight[e] + extraIndent[e]);
				minimum[p] = Math.max(minimum[p], add(minimum[e], margins));
				preferred[p] = Math.max(preferred[p], add(preferred[e], margins));
			}
		}
	}

	private void resolveWidth(int e, Metrics metrics) {
		int p = doc.boxParent[e];
		containing[e] = doc.boxPosition[e] == HtmlStyles.FIXED ? 0
				: doc.boxPosition[p] != HtmlStyles.STATIC ? p : containing[p];
		tableOf[e] = tableOf[p];
		int cb = containing[e], edges = horizontalEdges(e);
		int reference = outOfFlow(e) ? cb == 0 ? viewportWidth : clamp((long) width[cb] - 2L * doc.boxBorder[cb]) : contentWidth(p);
		int available = Math.max(0, clamp((long) reference - doc.boxIndent[e] - doc.boxMarginRight[e] - extraIndent[e]));
		int left = doc.boxLeft[e], right = doc.boxRight[e];
		if (doc.boxWidth[e] != HtmlStyles.AUTO) {
			width[e] = add(doc.boxWidth[e], edges);
		} else if (imageBox(e)) {
			width[e] = add((int) metrics.intrinsicWidths(doc.boxFirstBlock[e]), edges);
		} else if (outOfFlow(e)) {
			int fit = Math.max(0, subtract(available, left == HtmlStyles.AUTO ? 0 : left, right == HtmlStyles.AUTO ? 0 : right));
			width[e] = left != HtmlStyles.AUTO && right != HtmlStyles.AUTO ? Math.max(edges, fit)
					: Math.min(Math.max(minimum[e], fit), Math.max(minimum[e], preferred[e]));
		} else {
			width[e] = Math.max(edges, available);
		}
		if (doc.boxRole[e] == HtmlDocument.TABLE_BOX) {
			int t = tableIndex(e);
			if (t >= 0) {
				tableOf[e] = t;
				columns[t] = metrics.columns(t, contentWidth(e));
				width[e] = add(columns[t][columns[t].length - 1], edges);
			}
		} else if (doc.boxRole[e] == HtmlDocument.CELL_BOX && doc.boxFirstBlock[e] < doc.boxEndBlock[e]) {
			int b = doc.boxFirstBlock[e], t = doc.cellTable[b];
			if (t >= 0 && columns[t] != null) {
				int c = doc.cellColumn[b];
				width[e] = columns[t][c + 1] - columns[t][c];
			}
		}
	}

	private int tableIndex(int box) {
		for (int b = doc.boxFirstBlock[box]; b < doc.boxEndBlock[box]; b++) {
			if (doc.kind[b] == HtmlDocument.CELL) return doc.cellTable[b];
		}
		return -1;
	}

	private boolean imageBox(int box) {
		int b = doc.boxFirstBlock[box];
		return b < doc.boxEndBlock[box] && doc.blockBox[b] == box && doc.kind[b] == HtmlDocument.IMAGE;
	}

	/* Merge the direct block fragments with child boxes in source order, including empty boxes at the same offset. */
	private void measureContents(int e, Metrics metrics) {
		int cursor = 0, child = doc.boxFirstChild[e], b = doc.boxFirstBlock[e], end = doc.boxEndBlock[e];
		boolean row = doc.boxRole[e] == HtmlDocument.ROW_BOX;
		while (child >= 0 || b < end) {
			if (child >= 0 && doc.boxFirstBlock[child] <= b) {
				localY[child] = add(row ? 0 : cursor, before[child]);
				int allocation = clamp((long) before[child] + height[child] + after[child]);
				if (!outOfFlow(child)) cursor = row ? Math.max(cursor, allocation) : add(cursor, allocation);
				b = Math.max(b, doc.boxEndBlock[child]);
				child = doc.boxNextSibling[child];
			} else if (b < end) {
				if (doc.blockBox[b] == e) {
					blockInset[b] = inset(b);
					blockWidth[b] = Math.max(0, add(contentWidth(e), -blockInset[b]));
					if (doc.kind[b] == HtmlDocument.CELL) blockWidth[b] = Math.max(0, width[e] - 2 * HtmlLayout.CELL_PADDING);
					if (doc.kind[b] == HtmlDocument.PRE || doc.kind[b] == HtmlDocument.IMAGE) {
						blockWidth[b] = Math.max(blockWidth[b], (int) metrics.intrinsicWidths(b));
					}
					int leading = b == doc.boxFirstBlock[e] ? leadGap[e] : 0;
					int trailing = b + 1 == doc.boxEndBlock[e] ? trailGap[e] : 0;
					int raw = metrics.height(b, Math.max(1, blockWidth[b]));
					int inside = Math.max(0, subtract(raw, leading, trailing));
					blockY[b] = cursor;
					blockHeight[b] = inside;
					blockContentTop[b] = doc.kind[b] == HtmlDocument.CELL ? HtmlLayout.CELL_PADDING : HtmlLayout.BLOCK_GAP - leading;
					if (leading != 0) {
						int firstEdges = clamp((long) before[e] + doc.boxBorder[e] + doc.boxPadTop[e]);
						blockY[b] = add(blockY[b], -firstEdges);
						blockHeight[b] = add(blockHeight[b], firstEdges);
						blockContentTop[b] = add(blockContentTop[b], firstEdges);
					}
					if (trailing != 0) blockHeight[b] = clamp((long) blockHeight[b] + after[e] + doc.boxBorder[e] + doc.boxPadBottom[e]);
					cursor = add(cursor, inside);
				}
				b++;
			} else {
				// Empty child after the last text fragment.
				b = doc.boxFirstBlock[child];
			}
		}
		// Images keep the renderer's existing block gaps around their CSS/HTML-sized pixels.
		height[e] = add(e != 0 && doc.boxHeight[e] != HtmlStyles.AUTO && !imageBox(e) ? doc.boxHeight[e] : cursor, verticalEdges(e));
		if (row) {
			for (int c = doc.boxFirstChild[e]; c >= 0; c = doc.boxNextSibling[c]) {
				if (doc.boxRole[c] != HtmlDocument.CELL_BOX || outOfFlow(c)) continue;
				height[c] = Math.max(height[c], Math.max(0, clamp((long) height[e] - verticalEdges(e) - before[c] - after[c])));
				int cell = doc.boxFirstBlock[c];
				if (cell < doc.boxEndBlock[c]) blockHeight[cell] = height[c];
			}
		}
	}

	private int inset(int b) {
		if (doc.kind[b] == HtmlDocument.CELL) return HtmlLayout.CELL_PADDING;
		return Math.max(0, add(legacyInset(b), -accountedIndent[doc.blockBox[b]]));
	}

	private int legacyInset(int b) {
		return add(doc.kind[b] == HtmlDocument.LIST_ITEM ? doc.level[b] * HtmlLayout.LIST_INDENT : 0,
				doc.blockIndent[b] - doc.boxIndent[doc.blockBox[b]]);
	}

	private void resolvePosition(int e) {
		int p = doc.boxParent[e], position = doc.boxPosition[e];
		int staticX = clamp((long) contentX(p) + doc.boxIndent[e] + extraIndent[e]), staticY = add(contentY(p), localY[e]);
		if (doc.boxRole[e] == HtmlDocument.CELL_BOX && doc.boxFirstBlock[e] < doc.boxEndBlock[e]) {
			int b = doc.boxFirstBlock[e], t = doc.cellTable[b];
			if (t >= 0 && columns[t] != null) staticX = add(contentX(p), columns[t][doc.cellColumn[b]]);
		}
		if (outOfFlow(e)) {
			int c = containing[e];
			int cx = c == 0 ? 0 : add(x[c], doc.boxBorder[c]), cy = c == 0 ? 0 : add(y[c], doc.boxBorder[c]);
			int cw = c == 0 ? viewportWidth : clamp((long) width[c] - 2L * doc.boxBorder[c]);
			int ch = c == 0 ? viewportHeight : clamp((long) height[c] - 2L * doc.boxBorder[c]);
			int left = doc.boxLeft[e], right = doc.boxRight[e], top = doc.boxTop[e], bottom = doc.boxBottom[e];
			if (!imageBox(e) && doc.boxHeight[e] == HtmlStyles.AUTO && top != HtmlStyles.AUTO && bottom != HtmlStyles.AUTO) {
				height[e] = Math.max(verticalEdges(e), clamp((long) ch - top - bottom - before[e] - after[e]));
			}
			x[e] = left != HtmlStyles.AUTO ? clamp((long) cx + left + doc.boxIndent[e] + extraIndent[e])
					: right != HtmlStyles.AUTO ? clamp((long) cx + cw - right - doc.boxMarginRight[e] - width[e]) : staticX;
			y[e] = top != HtmlStyles.AUTO ? clamp((long) cy + top + before[e])
					: bottom != HtmlStyles.AUTO ? clamp((long) cy + ch - bottom - after[e] - height[e]) : staticY;
			fixed[e] = position == HtmlStyles.FIXED || fixed[c];
		} else {
			x[e] = staticX;
			y[e] = staticY;
			fixed[e] = fixed[p];
			if (position == HtmlStyles.RELATIVE) {
				x[e] = add(x[e], offset(doc.boxLeft[e], doc.boxRight[e]));
				y[e] = add(y[e], offset(doc.boxTop[e], doc.boxBottom[e]));
			}
		}
		hasFixed |= fixed[e];
		if (!fixed[e]) totalHeight = Math.max(totalHeight, clamp((long) y[e] + height[e] + after[e]));
	}

	private boolean outOfFlow(int box) {
		return doc.boxPosition[box] == HtmlStyles.ABSOLUTE || doc.boxPosition[box] == HtmlStyles.FIXED;
	}

	private static int offset(int start, int end) {
		return start != HtmlStyles.AUTO ? start : end != HtmlStyles.AUTO ? clamp(-(long) end) : 0;
	}

	private static int add(int a, int b) {
		return clamp((long) a + b);
	}

	private static int subtract(int a, int b, int c) {
		return clamp((long) a - b - c);
	}

	private static int clamp(long value) {
		return (int) Math.max(Integer.MIN_VALUE + 1L, Math.min(Integer.MAX_VALUE, value));
	}

	public HtmlPaintOrder order() { return order; }
	public int totalHeight() { return totalHeight; }
	public boolean hasFixed() { return hasFixed; }
	public int blockX(int block) { return blockX[block]; }
	public int blockY(int block, int scroll) { return add(blockY[block], fixed[doc.blockBox[block]] ? scroll : 0); }
	public int blockWidth(int block) { return blockWidth[block]; }
	public int blockHeight(int block) { return blockHeight[block]; }
	public int blockContentTop(int block) { return blockContentTop[block]; }
	public int boxX(int box) { return x[box]; }
	public int boxY(int box, int scroll) { return add(y[box], fixed[box] ? scroll : 0); }
	public int boxWidth(int box) { return width[box]; }
	public int boxHeight(int box) { return height[box]; }

	/** Topmost eligible block or an empty box (-1), using the exact reverse of the paint sequence. */
	public int blockAt(int px, int py, int scroll) {
		int count = collectVisible(py, 1, scroll);
		for (int v = count - 1; v >= 0; v--) {
			int s = visible[v];
			int i = order.index(s);
			if (order.isBackground(s)) {
				if (i != 0 && contains(px, py, x[i], boxY(i, scroll), width[i], height[i])) {
					int b = doc.boxFirstBlock[i];
					return b < doc.boxEndBlock[i] && doc.blockBox[b] == i ? b : -1;
				}
			} else {
				if (contains(px, py, blockX[i], blockY(i, scroll), blockWidth[i], blockHeight[i])) return i;
			}
		}
		return -1;
	}

	/** The nearest selectable fragment when a drag leaves every box; ties follow the later painted fragment. */
	public int nearestBlock(int px, int py, int scroll) {
		long vertical = Long.MAX_VALUE, horizontal = Long.MAX_VALUE;
		int found = -1;
		for (int s = order.size() - 1; s >= 0; s--) {
			if (order.isBackground(s)) continue;
			int b = order.index(s), top = blockY(b, scroll);
			long dx = Math.max(0L, Math.max((long) blockX[b] - px, (long) px - blockX[b] - blockWidth[b]));
			long dy = Math.max(0L, Math.max((long) top - py, (long) py - top - blockHeight[b]));
			if (dy < vertical || dy == vertical && dx < horizontal) {
				vertical = dy; horizontal = dx; found = b;
			}
		}
		return found;
	}

	private static boolean contains(int px, int py, int x, int y, int width, int height) {
		return px >= x && py >= y && (long) px < (long) x + width && (long) py < (long) y + height;
	}
}
