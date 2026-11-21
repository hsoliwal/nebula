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
 * gaps remain outside a styled leaf's border. The {@code overflow: hidden/clip} panel subset clips descendants to
 * the padding edge without creating nested scrollbars. Percentages, inline positioning, floats and transforms remain
 * outside this subset. Positioned documents resolve their geometry as
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
	/* Nearest clipping ancestor, excluding the box itself; preorder makes this a linear one-time construction. */
	private final int[] clipAncestor;
	private final int[] before, after, leadGap, trailGap, extraIndent, accountedIndent;
	private final boolean[] fixed;
	private final int[] blockX, blockY, blockWidth, blockHeight, blockInset, blockContentTop;
	private final int[] minimum, preferred;
	private final int[][] columns;
	private int viewportWidth, viewportHeight, totalHeight;
	private boolean hasFixed;
	/* Sticky motion is projected once per scroll; ordinary/fixed interval queries retain their existing indexes. */
	private final int[] motionOwner, motionParent, stickyBoxes, stickyShift;
	private int stickyCount, projectedScroll;
	private boolean projectionValid;
	private int[] movingSteps = new int[0];
	/* Sorted vertical intervals and segment maxima: viewport queries skip entire offscreen subtrees. */
	private long[] normalKeys = new long[0], fixedKeys = new long[0], normalMax = new long[0], fixedMax = new long[0];
	private final int[] visible;
	private int visibleCount;

	public HtmlPositionedLayout(HtmlDocument document) {
		doc = document;
		order = new HtmlPaintOrder(document);
		int n = doc.boxCount, b = doc.blockCount;
		x = new int[n]; y = new int[n]; width = new int[n]; height = new int[n]; localY = new int[n];
		clipAncestor = new int[n];
		clipAncestor[0] = -1;
		for (int e = 1; e < n; e++) {
			int parent = doc.boxParent[e];
			clipAncestor[e] = doc.boxOverflow[parent] == HtmlStyles.OVERFLOW_HIDDEN ? parent : clipAncestor[parent];
		}
		containing = new int[n]; tableOf = new int[n]; fixed = new boolean[n];
		motionOwner = new int[n]; motionParent = new int[n]; stickyBoxes = new int[n]; stickyShift = new int[n];
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
		stickyCount = 0; projectionValid = false;
		Arrays.fill(motionOwner, -1);
		Arrays.fill(motionParent, -1);
		for (int e = 1; e < doc.boxCount; e++) resolvePosition(e);
		for (int b = 0; b < doc.blockCount; b++) {
			int e = doc.blockBox[b];
			blockX[b] = add(contentX(e), blockInset[b]);
			blockY[b] = add(doc.kind[b] == HtmlDocument.CELL ? y[e] : contentY(e), blockY[b]);
			if (!fixed[e]) totalHeight = Math.max(totalHeight, visibleBottom(e, true, add(blockY[b], blockHeight[b])));
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
			if (doc.boxRole[e] == HtmlDocument.TABLE_BOX || doc.boxRole[e] == HtmlDocument.FLEX_BOX || doc.boxRole[e] == HtmlDocument.GRID_BOX) {
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
		int ordinary = 0, stationary = 0, moving = 0;
		for (int s = 0; s < order.size(); s++) {
			if (stepMoving(s)) moving++; else if (stepFixed(s)) stationary++; else ordinary++;
		}
		if (normalKeys.length != ordinary) normalKeys = new long[ordinary];
		if (fixedKeys.length != stationary) fixedKeys = new long[stationary];
		if (movingSteps.length != moving) movingSteps = new int[moving];
		ordinary = stationary = moving = 0;
		for (int s = 0; s < order.size(); s++) {
			long key = (long) stepY(s) << 32 | s & 0xFFFFFFFFL;
			if (stepMoving(s)) movingSteps[moving++] = s;
			else if (stepFixed(s)) fixedKeys[stationary++] = key; else normalKeys[ordinary++] = key;
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
		projectSticky(scroll);
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
		for (int step : movingSteps) {
			int i = order.index(step);
			int top = order.isBackground(step) ? boxY(i, scroll) : blockY(i, scroll);
			int h = stepHeight(step);
			if (h > 0 && top < (long) pageTop + extent && (long) top + h > pageTop) visible[visibleCount++] = step;
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

	private boolean stepMoving(int step) {
		int i = order.index(step);
		return motionOwner[order.isBackground(step) ? i : doc.blockBox[i]] >= 0;
	}

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
			if (doc.boxRole[e] == HtmlDocument.TABLE_BOX || doc.boxRole[e] == HtmlDocument.FLEX_BOX || doc.boxRole[e] == HtmlDocument.GRID_BOX) {
				int table = tableIndex(e);
				if (table >= 0) {
					int[] min = metrics.columns(table, 0), max = metrics.columns(table, Integer.MAX_VALUE);
					minimum[e] = tableExtent(table, min);
					preferred[e] = tableExtent(table, max);
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
		if (doc.boxRole[e] == HtmlDocument.TABLE_BOX || doc.boxRole[e] == HtmlDocument.FLEX_BOX || doc.boxRole[e] == HtmlDocument.GRID_BOX) {
			int t = tableIndex(e);
			if (t >= 0) {
				tableOf[e] = t;
				columns[t] = metrics.columns(t, contentWidth(e));
				if (doc.tableDisplay[t] == HtmlStyles.DISPLAY_DEFAULT) width[e] = add(tableExtent(t, columns[t]), edges);
			}
		} else if (doc.boxRole[e] == HtmlDocument.CELL_BOX && doc.boxFirstBlock[e] < doc.boxEndBlock[e]) {
			int b = doc.boxFirstBlock[e], t = doc.cellTable[b];
			if (t >= 0 && columns[t] != null) {
				int c = doc.cellColumn[b];
				int last = c + doc.cellSpan[b] - 1;
				width[e] = doc.tableDisplay[t] != HtmlStyles.DISPLAY_DEFAULT
						? columns[t][last] + columns[t][doc.tableColumns[t] + last] - columns[t][c]
						: columns[t][c + doc.cellSpan[b]] - columns[t][c];
			}
		}
	}

	private int tableExtent(int table, int[] values) {
		int n = doc.tableColumns[table];
		return doc.tableDisplay[table] != HtmlStyles.DISPLAY_DEFAULT ? n == 0 ? 0 : add(values[n - 1], values[2 * n - 1]) : values[values.length - 1];
	}

	private boolean boxedItem(int block) {
		return doc.kind[block] == HtmlDocument.CELL && doc.tableDisplay[doc.cellTable[block]] != HtmlStyles.DISPLAY_DEFAULT;
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
		if (doc.boxRole[e] == HtmlDocument.GRID_BOX) { measureGridRows(e); return; }
		int cursor = 0, child = doc.boxFirstChild[e], b = doc.boxFirstBlock[e], end = doc.boxEndBlock[e];
		boolean row = doc.boxRole[e] == HtmlDocument.ROW_BOX || doc.boxRole[e] == HtmlDocument.FLEX_BOX;
		if (doc.boxRole[e] == HtmlDocument.TABLE_BOX) resolveRowSpans(e);
		while (child >= 0 || b < end) {
			if (child >= 0 && doc.boxFirstBlock[child] <= b) {
				localY[child] = add(row ? 0 : cursor, before[child]);
				int allocation = clamp((long) before[child] + height[child] + after[child]);
				if (!outOfFlow(child) && (!row || cellSpanRows(child) == 1)) cursor = row ? Math.max(cursor, allocation) : add(cursor, allocation);
				b = Math.max(b, doc.boxEndBlock[child]);
				child = doc.boxNextSibling[child];
			} else if (b < end) {
				if (doc.blockBox[b] == e) {
					blockInset[b] = inset(b);
					blockWidth[b] = Math.max(0, add(contentWidth(e), -blockInset[b]));
					if (doc.kind[b] == HtmlDocument.CELL) blockWidth[b] = Math.max(0, boxedItem(b) ? contentWidth(e) : width[e] - 2 * HtmlLayout.CELL_PADDING);
					if (doc.kind[b] == HtmlDocument.PRE || doc.kind[b] == HtmlDocument.IMAGE) {
						blockWidth[b] = Math.max(blockWidth[b], (int) metrics.intrinsicWidths(b));
					}
					int leading = b == doc.boxFirstBlock[e] ? leadGap[e] : 0;
					int trailing = b + 1 == doc.boxEndBlock[e] ? trailGap[e] : 0;
					int raw = metrics.height(b, Math.max(1, blockWidth[b]));
					int inside = Math.max(0, subtract(raw, leading, trailing));
					blockY[b] = cursor;
					blockHeight[b] = inside;
					blockContentTop[b] = doc.kind[b] == HtmlDocument.CELL
							? boxedItem(b) ? add(doc.boxBorder[e], doc.boxPadTop[e]) : HtmlLayout.CELL_PADDING : HtmlLayout.BLOCK_GAP - leading;
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
				if (doc.boxRole[c] != HtmlDocument.CELL_BOX || outOfFlow(c) || cellSpanRows(c) != 1) continue;
				height[c] = Math.max(height[c], Math.max(0, clamp((long) height[e] - verticalEdges(e) - before[c] - after[c])));
				int cell = doc.boxFirstBlock[c];
				if (cell < doc.boxEndBlock[c]) blockHeight[cell] = height[c];
			}
		}
	}

	/* The existing GridTracks owner sizes columns; cells in the same implicit row share its height. */
	private void measureGridRows(int e) {
		int table = tableIndex(e), cursor = 0, rowHeight = 0, previous = -1, first = -1;
		int gap = table >= 0 ? doc.tableRowGap[table] : 0;
		for (int child = doc.boxFirstChild[e]; child >= 0; child = doc.boxNextSibling[child]) {
			int b = doc.boxFirstBlock[child], row = b < doc.boxEndBlock[child] ? doc.cellRow[b] : previous;
			if (first >= 0 && row != previous) {
				stretchGridRow(first, child, rowHeight);
				cursor = clamp((long) cursor + rowHeight + gap);
				rowHeight = 0;
				first = -1;
			}
			if (first < 0) first = child;
			previous = row;
			localY[child] = add(cursor, before[child]);
			if (!outOfFlow(child)) rowHeight = Math.max(rowHeight, clamp((long) before[child] + height[child] + after[child]));
		}
		if (first >= 0) stretchGridRow(first, -1, rowHeight);
		cursor = add(cursor, rowHeight);
		height[e] = add(doc.boxHeight[e] != HtmlStyles.AUTO ? doc.boxHeight[e] : cursor, verticalEdges(e));
	}

	private void stretchGridRow(int first, int end, int rowHeight) {
		for (int child = first; child != end && child >= 0; child = doc.boxNextSibling[child]) {
			if (outOfFlow(child)) continue;
			height[child] = Math.max(height[child], Math.max(0, clamp((long) rowHeight - before[child] - after[child])));
			int b = doc.boxFirstBlock[child];
			if (b < doc.boxEndBlock[child]) blockHeight[b] = height[child];
		}
	}

	private int cellSpanRows(int box) {
		int b = doc.boxFirstBlock[box];
		return b < doc.boxEndBlock[box] && doc.kind[b] == HtmlDocument.CELL ? Math.max(1, doc.cellRowSpan[b]) : 1;
	}

	/* Keep the original table owner's row-span rule: content deficits extend the last covered row. */
	private void resolveRowSpans(int table) {
		int count = 0;
		for (int row = doc.boxFirstChild[table]; row >= 0; row = doc.boxNextSibling[row]) if (doc.boxRole[row] == HtmlDocument.ROW_BOX) count++;
		int[] rows = new int[count];
		int at = 0;
		for (int row = doc.boxFirstChild[table]; row >= 0; row = doc.boxNextSibling[row]) if (doc.boxRole[row] == HtmlDocument.ROW_BOX) rows[at++] = row;
		for (int r = 0; r < count; r++) {
			for (int cell = doc.boxFirstChild[rows[r]]; cell >= 0; cell = doc.boxNextSibling[cell]) {
				int span = cellSpanRows(cell), last = Math.min(count, r + span) - 1;
				if (span <= 1 || outOfFlow(cell)) continue;
				long have = 0;
				for (int k = r; k <= last; k++) have += height[rows[k]];
				if (height[cell] > have) height[rows[last]] = add(height[rows[last]], clamp(height[cell] - have));
			}
		}
		for (int r = 0; r < count; r++) {
			for (int cell = doc.boxFirstChild[rows[r]]; cell >= 0; cell = doc.boxNextSibling[cell]) {
				if (outOfFlow(cell)) continue;
				long have = 0;
				for (int k = r, end = Math.min(count, r + cellSpanRows(cell)); k < end; k++) have += height[rows[k]];
				height[cell] = clamp(have);
				int b = doc.boxFirstBlock[cell];
				if (b < doc.boxEndBlock[cell]) blockHeight[b] = height[cell];
			}
		}
	}

	private int inset(int b) {
		if (doc.kind[b] == HtmlDocument.CELL) return boxedItem(b) ? 0 : HtmlLayout.CELL_PADDING;
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
		if (!fixed[e]) {
			motionOwner[e] = motionOwner[outOfFlow(e) ? containing[e] : p];
			if (position == HtmlStyles.STICKY) {
				motionParent[e] = motionOwner[e];
				motionOwner[e] = e;
				stickyBoxes[stickyCount++] = e;
			}
		}
		hasFixed |= fixed[e];
		if (!fixed[e]) totalHeight = Math.max(totalHeight, visibleBottom(e, false, clamp((long) y[e] + height[e] + after[e])));
	}

	/* Bound sticky projection by the original containing box (table cells use the entire table). */
	private void projectSticky(int scroll) {
		if (projectionValid && projectedScroll == scroll) return;
		projectedScroll = scroll;
		projectionValid = true;
		for (int k = 0; k < stickyCount; k++) {
			int e = stickyBoxes[k], inherited = motionParent[e] >= 0 ? stickyShift[motionParent[e]] : 0;
			stickyShift[e] = inherited;
			if (doc.boxTop[e] == HtmlStyles.AUTO) continue;
			int parent = doc.boxParent[e];
			if (doc.boxRole[e] == HtmlDocument.CELL_BOX) {
				while (parent > 0 && doc.boxRole[parent] != HtmlDocument.TABLE_BOX
						&& doc.boxRole[parent] != HtmlDocument.FLEX_BOX && doc.boxRole[parent] != HtmlDocument.GRID_BOX) parent = doc.boxParent[parent];
			}
			int parentMotion = motionOwner[parent] >= 0 ? stickyShift[motionOwner[parent]] : fixed[parent] ? scroll : 0;
			long end = (long) y[parent] + height[parent] + parentMotion;
			int first = doc.boxFirstBlock[e];
			int gap = first < doc.boxEndBlock[e] && doc.blockBox[first] == e && doc.kind[first] != HtmlDocument.CELL
					&& leadGap[e] == 0 ? HtmlLayout.BLOCK_GAP : 0;
			long requested = (long) scroll + doc.boxTop[e] - y[e] - gap - inherited;
			long limit = end - y[e] - height[e] - after[e] - inherited;
			stickyShift[e] = clamp(inherited + Math.max(0L, Math.min(requested, limit)));
		}
	}

	private int projection(int box, int scroll) {
		if (fixed[box]) return scroll;
		int owner = motionOwner[box];
		if (owner < 0) return 0;
		projectSticky(scroll);
		return stickyShift[owner];
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
	public boolean hasSticky() { return stickyCount != 0; }
	public int blockX(int block) { return blockX[block]; }
	public int blockY(int block, int scroll) { return add(blockY[block], projection(doc.blockBox[block], scroll)); }
	public int blockWidth(int block) { return blockWidth[block]; }
	public int blockHeight(int block) { return blockHeight[block]; }
	public int blockContentTop(int block) { return blockContentTop[block]; }
	public int boxX(int box) { return x[box]; }
	public int boxY(int box, int scroll) { return add(y[box], projection(box, scroll)); }
	public int boxWidth(int box) { return width[box]; }
	public int boxHeight(int box) { return height[box]; }

	private int firstClip(int box, boolean content) {
		return content && doc.boxOverflow[box] == HtmlStyles.OVERFLOW_HIDDEN ? box : clipAncestor[box];
	}

	/*
	 * Offscreen descendants still participate in local layout, but cannot extend the root scrollbar
	 * past a clipping ancestor. Reuse the exact parent-owned clip chain used by painting and picking.
	 * This method runs at layout publication, never per paint fragment.
	 */
	private int visibleBottom(int box, boolean content, int bottom) {
		for (int clip = firstClip(box, content); clip >= 0; clip = clipAncestor[clip]) {
			bottom = Math.min(bottom, clamp((long) y[clip] + height[clip] - doc.boxBorder[clip]));
		}
		return bottom;
	}

	/** Whether a paint fragment inherits an overflow clip; a box's own border/background is not self-clipped. */
	public boolean hasClip(int box, boolean content) { return firstClip(box, content) >= 0; }

	/**
	 * Intersects a viewport-coordinate rectangle [left, top, right, bottom) with all clipping ancestors. The caller
	 * owns the four-integer scratch rectangle. No SWT clipping state, Region, or per-fragment object is retained here.
	 */
	public boolean clipBounds(int box, boolean content, int scroll, int[] bounds) {
		for (int clip = firstClip(box, content); clip >= 0; clip = clipAncestor[clip]) {
			int border = doc.boxBorder[clip];
			int left = add(x[clip], border), top = add(add(boxY(clip, scroll), -scroll), border);
			int right = add(x[clip], clamp((long) width[clip] - border));
			int bottom = add(add(boxY(clip, scroll), -scroll), clamp((long) height[clip] - border));
			bounds[0] = Math.max(bounds[0], left);
			bounds[1] = Math.max(bounds[1], top);
			bounds[2] = Math.min(bounds[2], right);
			bounds[3] = Math.min(bounds[3], bottom);
			if (bounds[0] >= bounds[2] || bounds[1] >= bounds[3]) return false;
		}
		return bounds[0] < bounds[2] && bounds[1] < bounds[3];
	}

	private boolean withinClip(int box, boolean content, int px, int py, int scroll) {
		for (int clip = firstClip(box, content); clip >= 0; clip = clipAncestor[clip]) {
			int border = doc.boxBorder[clip];
			if (!contains(px, py, add(x[clip], border), add(boxY(clip, scroll), border),
					Math.max(0, clamp((long) width[clip] - 2L * border)),
					Math.max(0, clamp((long) height[clip] - 2L * border)))) return false;
		}
		return true;
	}

	/** Topmost eligible block or an empty box (-1), using the exact reverse of the paint sequence. */
	public int blockAt(int px, int py, int scroll) {
		int count = collectVisible(py, 1, scroll);
		for (int v = count - 1; v >= 0; v--) {
			int s = visible[v];
			int i = order.index(s);
			if (order.isBackground(s)) {
				// A structural row must not mask a cell that spans into it from an earlier row.
				if (doc.boxRole[i] == HtmlDocument.ROW_BOX && doc.boxBackground[i] == 0 && doc.boxBorder[i] == 0) continue;
				if (i != 0 && contains(px, py, x[i], boxY(i, scroll), width[i], height[i])
						&& withinClip(i, false, px, py, scroll)) {
					int b = doc.boxFirstBlock[i];
					return b < doc.boxEndBlock[i] && doc.blockBox[b] == i ? b : -1;
				}
			} else {
				if (contains(px, py, blockX[i], blockY(i, scroll), blockWidth[i], blockHeight[i])
						&& withinClip(doc.blockBox[i], true, px, py, scroll)) return i;
			}
		}
		return -1;
	}

	/** The nearest selectable fragment when a drag leaves every box; ties follow the later painted fragment. */
	public int nearestBlock(int px, int py, int scroll) {
		long vertical = Long.MAX_VALUE, horizontal = Long.MAX_VALUE;
		int found = -1;
		int[] bounds = new int[4];
		for (int s = order.size() - 1; s >= 0; s--) {
			if (order.isBackground(s)) continue;
			int b = order.index(s), top = blockY(b, scroll);
			long dx, dy;
			if (hasClip(doc.blockBox[b], true)) {
				int viewTop = add(top, -scroll);
				bounds[0] = blockX[b]; bounds[1] = viewTop;
				bounds[2] = add(blockX[b], blockWidth[b]); bounds[3] = add(viewTop, blockHeight[b]);
				if (!clipBounds(doc.blockBox[b], true, scroll, bounds)) continue;
				int viewY = add(py, -scroll);
				dx = Math.max(0L, Math.max((long) bounds[0] - px, (long) px - bounds[2]));
				dy = Math.max(0L, Math.max((long) bounds[1] - viewY, (long) viewY - bounds[3]));
			} else {
				dx = Math.max(0L, Math.max((long) blockX[b] - px, (long) px - blockX[b] - blockWidth[b]));
				dy = Math.max(0L, Math.max((long) top - py, (long) py - top - blockHeight[b]));
			}
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
