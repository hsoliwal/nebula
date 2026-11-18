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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;

/**
 * The vertical layout of an {@link HtmlDocument} for one width, computed lazily: blocks are measured in document order
 * only as far down as a caller asks (the viewport plus overscan), so a long page costs what is shown. Block tops are
 * prefix sums in one int lane; {@link #blockAt(int)} is a binary search over the measured part. A block's
 * {@link TextLayout} exists only while it is in a small cache of recently used blocks (the visible ones); others are
 * measured and disposed. Changing the width forgets all measures.
 * <p>
 * A table row is measured as one step: its cells share a top, and the row is as tall as its tallest cell. Column
 * widths follow CSS 2.1 automatic table layout (section 17.5.2.2) without spans: each column has a minimum (its
 * widest word) and a maximum (its widest unwrapped line) over its cells; a table that fits takes its maximums, one
 * that cannot fit even its minimums takes those and overflows, and otherwise each column gets its minimum plus a
 * share of the remaining width in proportion to its maximum less its minimum. A table's widths are computed when its
 * first row is measured, from all its cells (as automatic layout requires), and forgotten with the width.
 * </p>
 */
public final class HtmlLayout {

	/** Space before and after a block, and the indent of one list level, in pixels. */
	public static final int BLOCK_GAP = 8, LIST_INDENT = 24, IMAGE_PLACEHOLDER = 16;

	/** Space between a table cell's edge and its text, in pixels, on each side. */
	public static final int CELL_PADDING = 4;

	/** The height of a rule's line (hr), in pixels, between its gaps. */
	public static final int RULE_HEIGHT = 2;

	/* the platform's monospace family, for code and pre */
	private static final String MONOSPACE = switch (SWT.getPlatform()) {
		case "win32" -> "Consolas";
		case "cocoa" -> "Menlo";
		default -> "Monospace";
	};

	/** CSS default heading sizes relative to body text, h1 to h6. */
	private static final float[] HEADING_SCALE = { 2f, 1.5f, 1.17f, 1f, .83f, .67f };

	private static final int CACHE = 64;

	private final Device device;
	private final HtmlDocument doc;
	private final FontData base;
	private final Color linkColor;
	/* fonts by (size class: 0 body, 1-6 headings) x (bold, italic bits) x (proportional, monospace), made on first use */
	private final Font[] fonts = new Font[7 * 4 * 2];
	/* CSS colours by ARGB, made on first use */
	private final java.util.Map<Integer, Color> colors = new java.util.HashMap<>();

	private int width = -1;
	/* per image: the width and height it is drawn at once loaded (-1: not loaded), or null before any loads */
	private int[] shown;
	/* block tops (top[b] valid for b <= measured) and heights; measured = number of blocks measured so far */
	private int[] top, height;
	private int measured;
	/* per table: column left edges relative to the table, one more than its columns (the last is its width), or null */
	private int[][] columnX;

	/* the TextLayout cache: block index per slot, least recently used first */
	private final int[] cachedBlock = new int[CACHE];
	private final TextLayout[] cached = new TextLayout[CACHE];
	private int cacheSize;
	/** Number of TextLayouts made so far (measuring included), for tests of laziness. */
	public int layoutsMade;

	public HtmlLayout(Device device, HtmlDocument doc, Font bodyFont, Color linkColor) {
		this.device = device;
		this.doc = doc;
		this.base = bodyFont.getFontData()[0];
		this.linkColor = linkColor;
		top = new int[doc.blockCount + 1];
		height = new int[Math.max(1, doc.blockCount)];
		columnX = new int[doc.tableCount][];
	}

	/** Sets the width blocks wrap to; a new width forgets every measure. */
	public void setWidth(int newWidth) {
		if (newWidth == width) return;
		width = Math.max(1, newWidth);
		measured = 0;
		Arrays.fill(columnX, null);
		clearCache();
	}

	public int width() {
		return width;
	}

	/** Measures blocks until {@code y} is covered or the document ends. */
	public void ensure(int y) {
		while (measured < doc.blockCount && top[measured] <= y) measureNext();
	}

	/** The block whose box holds {@code y} (clamped to the document); in a table row, the row's first cell. */
	public int blockAt(int y) {
		ensure(y);
		if (measured == 0) return -1;
		int index = Arrays.binarySearch(top, 0, measured + 1, y);
		int b = Math.max(0, Math.min(index >= 0 ? index : -index - 2, measured - 1));
		while (b > 0 && doc.sameRow(b - 1, b)) b--;
		// a box out of the flow has no extent there: the block of the flow at that y follows it
		while (b + 1 < measured && outOfFlow(b) && top[b + 1] <= y) b++;
		return b;
	}

	/** The block under ({@code x}, {@code y}) in page coordinates: in a table row, the cell whose column holds x. */
	public int blockAt(int x, int y) {
		int b = blockAt(y);
		if (b < 0 || doc.kind[b] != HtmlDocument.CELL) return b;
		// a cell of an earlier row whose rowspan reaches y, under x, comes first
		for (int k = b - 1; k >= 0 && b - k <= SPAN_LOOKBACK && doc.kind[k] == HtmlDocument.CELL && doc.cellTable[k] == doc.cellTable[b]; k--) {
			if (doc.cellRowSpan[k] > 1 && top[k] + height[k] > y && x >= cellX(k) && x < cellX(k) + cellWidth(k)) return k;
		}
		int hit = b;
		for (int k = b; k < doc.blockCount && doc.sameRow(b, k); k++) if (x >= cellX(k)) hit = k;
		return hit;
	}

	public int top(int b) {
		ensureBlock(b);
		return top[b];
	}

	public int height(int b) {
		ensureBlock(b);
		return height[b];
	}

	/** The page height: exact once every block is measured, else an estimate from the average so far. */
	public int totalHeight() {
		if (measured == doc.blockCount) return top[measured];
		if (measured == 0) return doc.blockCount * (base.getHeight() * 2 + 2 * BLOCK_GAP);
		return top[measured] + (int) ((long) top[measured] * (doc.blockCount - measured) / measured);
	}

	/** Number of blocks measured so far. */
	public int measured() {
		return measured;
	}

	/** The x where block {@code b}'s content starts (list indent, left margin, border and padding; a cell's column and padding). */
	public int indent(int b) {
		if (doc.kind[b] == HtmlDocument.CELL) return cellX(b) + CELL_PADDING;
		return boxLeft(b) + doc.blockBorder[b] + doc.blockPadLeft[b];
	}

	/** How far below its top block {@code b}'s content starts: the gap and top margin, border and padding; a cell's padding. */
	public int contentTop(int b) {
		return doc.kind[b] == HtmlDocument.CELL ? CELL_PADDING : gapTop(b) + doc.blockBorder[b] + doc.blockPadTop[b];
	}

	/** Whether block {@code b} has a CSS box of its own (a border, padding, or a top, right or bottom margin). */
	public boolean hasBox(int b) {
		return doc.blockBorder[b] > 0 || doc.blockPadTop[b] > 0 || doc.blockPadRight[b] > 0 || doc.blockPadBottom[b] > 0
				|| doc.blockPadLeft[b] > 0 || doc.blockMarginTop[b] > 0 || doc.blockMarginRight[b] > 0
				|| doc.blockMarginBottom[b] > 0;
	}

	/** The left edge of block {@code b}'s border box: list indent and left margin. */
	public int boxLeft(int b) {
		return (doc.kind[b] == HtmlDocument.LIST_ITEM ? doc.level[b] * LIST_INDENT : 0) + doc.blockIndent[b];
	}

	/** The page y of the top of block {@code b}'s border box. */
	public int boxTop(int b) {
		return top(b) + gapTop(b);
	}

	/** The width of block {@code b}'s border box: the view less the left edge and the right margin. */
	public int boxWidth(int b) {
		return Math.max(0, width - boxLeft(b) - doc.blockMarginRight[b]);
	}

	/** The height of block {@code b}'s border box. */
	public int boxHeight(int b) {
		return height(b) - gapTop(b) - gapBottom(b);
	}

	/** Whether block {@code b} is positioned absolute or fixed: painted at its own place, taking no flow space. */
	public boolean outOfFlow(int b) {
		int position = doc.blockPosition[b];
		return position == HtmlStyles.ABSOLUTE || position == HtmlStyles.FIXED;
	}

	/** Whether block {@code b} is positioned (relative, absolute or fixed): painted as a layer over the flow. */
	public boolean positioned(int b) {
		return doc.blockPosition[b] != HtmlStyles.STATIC;
	}

	/**
	 * How far block {@code b} is drawn right of its place in the flow: a relative box by its left offset; an absolute
	 * or fixed box so that its border box starts at its left offset; 0 when the offset is auto.
	 */
	public int offsetX(int b) {
		int left = doc.blockLeft[b];
		if (left == HtmlStyles.AUTO || !positioned(b)) return 0;
		return doc.blockPosition[b] == HtmlStyles.RELATIVE ? left : left - boxLeft(b);
	}

	/**
	 * How far block {@code b} is drawn below its place in the flow (page coordinates): a relative box by its top
	 * offset; an absolute box so that its top is at its top offset in the page; a fixed box at its top offset in the
	 * view, given the scroll position; 0 when the offset is auto.
	 */
	public int offsetY(int b, int scrollY) {
		int offset = doc.blockTop[b];
		if (offset == HtmlStyles.AUTO || !positioned(b)) return 0;
		return switch (doc.blockPosition[b]) {
			case HtmlStyles.RELATIVE -> offset;
			case HtmlStyles.FIXED -> scrollY + offset - top(b);
			default -> offset - top(b);
		};
	}

	/** The link target in block {@code b} at ({@code x}, {@code y}) in that block's own page coordinates, or null. */
	public String linkIn(int b, int x, int y) {
		if (b < 0 || textless(b)) return null;
		TextLayout layout = textLayout(b);
		int lx = x - indent(b), ly = y - top[b] - contentTop(b);
		if (lx < 0 || ly < 0 || !layout.getBounds().contains(lx, ly)) return null;
		int[] trailing = new int[1];
		int offset = layout.getOffset(lx, ly, trailing) + doc.textStart[b];
		for (int k = 0; k < doc.runCount[b]; k++) {
			int r = doc.firstRun[b] + k;
			if (doc.runLink[r] >= 0 && offset >= doc.runStart[r] && offset < doc.runEnd[r]) return doc.href[doc.runLink[r]];
		}
		return null;
	}

	private int gapTop(int b) {
		return BLOCK_GAP + doc.blockMarginTop[b];
	}

	private int gapBottom(int b) {
		return BLOCK_GAP + doc.blockMarginBottom[b];
	}

	/** The left edge of cell {@code b}'s box. */
	public int cellX(int b) {
		return doc.blockIndent[b] + columns(doc.cellTable[b])[doc.cellColumn[b]];
	}

	/** The width of cell {@code b}'s box (its columns' width). */
	public int cellWidth(int b) {
		int[] x = columns(doc.cellTable[b]);
		return x[doc.cellColumn[b] + doc.cellSpan[b]] - x[doc.cellColumn[b]];
	}

	/** The block's text layout, made and cached on demand; null for an image block. */
	public TextLayout textLayout(int b) {
		ensureBlock(b);
		if (textless(b)) return null;
		for (int s = cacheSize - 1; s >= 0; s--) {
			if (cachedBlock[s] == b) {
				TextLayout hit = cached[s];
				System.arraycopy(cachedBlock, s + 1, cachedBlock, s, cacheSize - s - 1);
				System.arraycopy(cached, s + 1, cached, s, cacheSize - s - 1);
				cachedBlock[cacheSize - 1] = b;
				cached[cacheSize - 1] = hit;
				return hit;
			}
		}
		if (cacheSize == CACHE) {
			cached[0].dispose();
			System.arraycopy(cachedBlock, 1, cachedBlock, 0, CACHE - 1);
			System.arraycopy(cached, 1, cached, 0, CACHE - 1);
			cacheSize--;
		}
		TextLayout made = make(b);
		cachedBlock[cacheSize] = b;
		cached[cacheSize++] = made;
		return made;
	}

	/** The link target under ({@code x}, {@code y}) in page coordinates, or null. */
	public String linkAt(int x, int y) {
		int b = blockAt(x, y);
		if (b < 0 || textless(b)) return null;
		TextLayout layout = textLayout(b);
		int lx = x - indent(b), ly = y - top[b] - contentTop(b);
		if (lx < 0 || ly < 0 || !layout.getBounds().contains(lx, ly)) return null;
		int[] trailing = new int[1];
		int offset = layout.getOffset(lx, ly, trailing) + doc.textStart[b];
		for (int k = 0; k < doc.runCount[b]; k++) {
			int r = doc.firstRun[b] + k;
			if (doc.runLink[r] >= 0 && offset >= doc.runStart[r] && offset < doc.runEnd[r]) return doc.href[doc.runLink[r]];
		}
		return null;
	}

	/**
	 * The offset in the page's text nearest to ({@code x}, {@code y}) in page coordinates, for selecting with the mouse:
	 * above or below a block's text its first or last line, in an image's block the block's place in the text.
	 */
	public int offsetAt(int x, int y) {
		int b = blockAt(x, y);
		if (b < 0) return 0;
		if (textless(b)) return doc.textStart[b];
		TextLayout layout = textLayout(b);
		int[] trailing = new int[1];
		int local = layout.getOffset(x - indent(b), y - top[b] - contentTop(b), trailing) + trailing[0];
		return doc.textStart[b] + Math.min(local, doc.textEnd[b] - doc.textStart[b]);
	}

	/**
	 * Gives image {@code index} its natural size once its pixels are loaded. A width or height the tag declared is
	 * kept; a missing one is taken from the image, scaled to keep its aspect ratio. Blocks from the image's block on
	 * are measured again.
	 *
	 * @return whether the image's box changed
	 */
	public boolean setNaturalSize(int index, int naturalWidth, int naturalHeight) {
		int w = doc.imageWidth[index], h = doc.imageHeight[index];
		int width = w > 0 ? w : h > 0 && naturalHeight > 0 ? Math.round((float) naturalWidth * h / naturalHeight) : naturalWidth;
		int height = h > 0 ? h : w > 0 && naturalWidth > 0 ? Math.round((float) naturalHeight * w / naturalWidth) : naturalHeight;
		if (width == shownWidth(index) && height == shownHeight(index)) return false;
		if (shown == null) {
			shown = new int[2 * doc.imageCount];
			Arrays.fill(shown, -1);
		}
		shown[2 * index] = width;
		shown[2 * index + 1] = height;
		for (int b = 0; b < measured; b++) {
			if (doc.image[b] == index) {
				measured = b;
				break;
			}
		}
		return true;
	}

	/** The width image {@code index} is drawn at: declared, natural once loaded, else the placeholder. */
	public int shownWidth(int index) {
		if (shown != null && shown[2 * index] > 0) return shown[2 * index];
		return doc.imageWidth[index] > 0 ? doc.imageWidth[index] : IMAGE_PLACEHOLDER;
	}

	/** The height image {@code index} is drawn at: declared, natural once loaded, else the placeholder. */
	public int shownHeight(int index) {
		if (shown != null && shown[2 * index + 1] > 0) return shown[2 * index + 1];
		return doc.imageHeight[index] > 0 ? doc.imageHeight[index] : IMAGE_PLACEHOLDER;
	}

	public void dispose() {
		clearCache();
		for (Font font : fonts) if (font != null) font.dispose();
		colors.values().forEach(Color::dispose);
		colors.clear();
		Arrays.fill(fonts, null);
	}

	private void ensureBlock(int b) {
		if (b < 0 || b >= doc.blockCount) throw new IndexOutOfBoundsException(b);
		while (measured <= b) measureNext();
	}

	private void measureNext() {
		if (width < 0) throw new IllegalStateException("setWidth first");
		int b = measured;
		if (doc.kind[b] == HtmlDocument.CELL) {
			measureRow(b);
			return;
		}
		int h;
		if (doc.kind[b] == HtmlDocument.IMAGE) {
			h = shownHeight(doc.image[b]);
		} else if (doc.kind[b] == HtmlDocument.RULE) {
			h = RULE_HEIGHT;
		} else if (doc.kind[b] == HtmlDocument.CONTROL) {
			h = controlHeight(b);
		} else {
			h = textHeight(b);
		}
		// the block gap and the CSS margins outside the border box; the border and the padding inside it
		height[b] = h + gapTop(b) + gapBottom(b) + 2 * doc.blockBorder[b] + doc.blockPadTop[b] + doc.blockPadBottom[b];
		// an absolute or fixed box keeps its height but takes no space in the flow
		top[b + 1] = top[b] + (outOfFlow(b) ? 0 : height[b]);
		measured++;
	}

	/* the cells of the row starting at block b share its top; the row is as tall as its tallest cell */
	/*
	 * the row starting at block b, and the rows its rowspans reach, measured as one group: a row is as tall as its cells
	 * of one row; a spanning cell taller than its rows grows the last of them, and is as tall as the rows it spans
	 */
	private void measureRow(int b) {
		int[] rowStart = new int[4], rowHeight = new int[4];
		int rowsInGroup = 0, reach = 0, k = b;
		while (k < doc.blockCount && doc.kind[k] == HtmlDocument.CELL && doc.cellTable[k] == doc.cellTable[b]
				&& (rowsInGroup == 0 || rowsInGroup <= reach)) {
			if (rowsInGroup == rowStart.length) {
				rowStart = Arrays.copyOf(rowStart, 2 * rowsInGroup);
				rowHeight = Arrays.copyOf(rowHeight, 2 * rowsInGroup);
			}
			rowStart[rowsInGroup] = k;
			int h = 0, first = k;
			for (; k < doc.blockCount && doc.sameRow(first, k); k++) {
				if (doc.cellRowSpan[k] == 1) h = Math.max(h, textHeight(k));
				reach = Math.max(reach, rowsInGroup + doc.cellRowSpan[k] - 1);
			}
			rowHeight[rowsInGroup++] = h + 2 * CELL_PADDING;
		}
		for (int r = 0; r < rowsInGroup; r++) {
			for (int c = rowStart[r]; c < (r + 1 < rowsInGroup ? rowStart[r + 1] : k); c++) {
				if (doc.cellRowSpan[c] == 1) continue;
				int last = Math.min(rowsInGroup, r + doc.cellRowSpan[c]) - 1, have = 0;
				for (int s = r; s <= last; s++) have += rowHeight[s];
				int need = textHeight(c) + 2 * CELL_PADDING;
				if (need > have) rowHeight[last] += need - have;
			}
		}
		int y = top[b];
		for (int r = 0; r < rowsInGroup; r++) {
			int end = r + 1 < rowsInGroup ? rowStart[r + 1] : k;
			for (int c = rowStart[r]; c < end; c++) {
				top[c] = y;
				int last = Math.min(rowsInGroup, r + doc.cellRowSpan[c]) - 1, h = 0;
				for (int s = r; s <= last; s++) h += rowHeight[s];
				height[c] = h;
			}
			y += rowHeight[r];
		}
		top[k] = y;
		measured = k;
	}

	/**
	 * The first block to paint for a band starting at page y {@code y}: {@link #blockAt(int)}, or an earlier cell of the
	 * same table whose rowspan reaches down to {@code y}.
	 */
	public int paintStart(int y) {
		int b = blockAt(y);
		if (b < 0 || doc.kind[b] != HtmlDocument.CELL) return b;
		int start = b;
		for (int k = b - 1; k >= 0 && b - k <= SPAN_LOOKBACK && doc.kind[k] == HtmlDocument.CELL && doc.cellTable[k] == doc.cellTable[b]; k--) {
			if (top[k] + height[k] > y) start = k;
		}
		return start;
	}

	/* how many cells before a row the hit test and the painting look back for a rowspan reaching into it */
	private static final int SPAN_LOOKBACK = 256;

	/* the height of block b's text, from its cached layout or one made and disposed */
	private int textHeight(int b) {
		for (int s = 0; s < cacheSize; s++) if (cachedBlock[s] == b) return cached[s].getBounds().height;
		TextLayout layout = make(b);
		int h = layout.getBounds().height;
		layout.dispose();
		return h;
	}

	/*
	 * the column edges of table t, computed on first use (CSS 2.1 automatic table layout without spans): a column's
	 * minimum is its widest word, its maximum its widest unwrapped line, each plus the cell padding
	 */
	private int[] columns(int t) {
		if (columnX[t] != null) return columnX[t];
		int n = doc.tableColumns[t], first = -1;
		int[] min = new int[n], max = new int[n];
		// cells of one column first; then a spanning cell spreads what its columns lack over them (CSS 2.1 17.5.2.2)
		for (int pass = 0; pass < 2; pass++) {
			for (int k = 0; k < doc.blockCount; k++) {
				if (doc.kind[k] != HtmlDocument.CELL || doc.cellTable[k] != t || (doc.cellSpan[k] > 1) != (pass == 1)) continue;
				if (first < 0 || k < first) first = k;
				TextLayout layout = make(k, -1);
				int c = doc.cellColumn[k], span = doc.cellSpan[k];
				int cellMax = layout.getBounds().width + 2 * CELL_PADDING, cellMin = widestWord(layout, doc.blockText(k)) + 2 * CELL_PADDING;
				layout.dispose();
				TableColumns.spread(min, c, span, cellMin);
				TableColumns.spread(max, c, span, cellMax);
			}
		}
		for (int c = 0; c < n; c++) max[c] = Math.max(max[c], min[c]);
		return columnX[t] = TableColumns.edges(min, max, width - doc.blockIndent[first]);
	}

	/* the width of the widest run of text between spaces and line ends, laid out unwrapped */
	private static int widestWord(TextLayout layout, String text) {
		int widest = 0;
		for (int start = 0, n = text.length(); start < n;) {
			int end = start;
			while (end < n && text.charAt(end) != ' ' && text.charAt(end) != '\n') end++;
			if (end > start) widest = Math.max(widest, layout.getLocation(end - 1, true).x - layout.getLocation(start, false).x);
			start = end + 1;
		}
		return widest;
	}

	/* pre does not wrap; a cell wraps to its column, other blocks to the view less their indent */
	private TextLayout make(int b) {
		boolean pre = doc.kind[b] == HtmlDocument.PRE, cell = doc.kind[b] == HtmlDocument.CELL;
		return make(b, pre ? -1 : Math.max(1, cell ? cellWidth(b) - 2 * CELL_PADDING
				: width - indent(b) - doc.blockBorder[b] - doc.blockPadRight[b] - doc.blockMarginRight[b]));
	}

	/* block b's text and styles laid out to the wrap width (-1: no wrapping) */
	private TextLayout make(int b, int wrap) {
		layoutsMade++;
		TextLayout layout = new TextLayout(device);
		boolean pre = doc.kind[b] == HtmlDocument.PRE, cell = doc.kind[b] == HtmlDocument.CELL;
		layout.setWidth(wrap);
		int align = doc.blockAlign[b];
		layout.setAlignment(align == HtmlStyles.CENTER ? SWT.CENTER : align == HtmlStyles.RIGHT ? SWT.RIGHT : SWT.LEFT);
		int sizeClass = doc.kind[b] == HtmlDocument.HEADING ? doc.level[b] : 0;
		int blockBold = sizeClass > 0 || cell && doc.level[b] == 1 ? HtmlDocument.BOLD : 0;
		layout.setFont(font(sizeClass, blockBold | (pre ? HtmlDocument.CODE : 0)));
		int start = doc.textStart[b];
		layout.setText(doc.text.substring(start, doc.textEnd[b]));
		for (int k = 0; k < doc.runCount[b]; k++) {
			int r = doc.firstRun[b] + k, s = doc.runStyle[r], argb = doc.runColor[r];
			if (s == 0 && argb == 0) continue;
			TextStyle style = new TextStyle(font(sizeClass, (s | blockBold) & (HtmlDocument.BOLD | HtmlDocument.ITALIC | HtmlDocument.CODE)), null, null);
			style.underline = (s & (HtmlDocument.UNDERLINE | HtmlDocument.LINK)) != 0;
			style.strikeout = (s & HtmlDocument.STRIKE) != 0;
			if ((s & HtmlDocument.LINK) != 0) style.foreground = linkColor;
			if (argb != 0) style.foreground = color(argb);
			layout.setStyle(style, doc.runStart[r] - start, doc.runEnd[r] - start - 1);
		}
		return layout;
	}

	/* the font of a size class and the BOLD, ITALIC and CODE bits */
	private Font font(int sizeClass, int bits) {
		boolean mono = (bits & HtmlDocument.CODE) != 0;
		int slot = (sizeClass * 4 + (bits & 3)) * 2 + (mono ? 1 : 0);
		if (fonts[slot] == null) {
			float scale = sizeClass == 0 ? 1f : HEADING_SCALE[sizeClass - 1];
			int style = ((bits & HtmlDocument.BOLD) != 0 ? SWT.BOLD : 0) | ((bits & HtmlDocument.ITALIC) != 0 ? SWT.ITALIC : 0);
			fonts[slot] = new Font(device, new FontData(mono ? MONOSPACE : base.getName(), Math.max(1, Math.round(base.getHeight() * scale)), style));
		}
		return fonts[slot];
	}

	/** Whether block {@code b} has no text to lay out (an image or a rule). */
	private boolean textless(int b) {
		return doc.kind[b] == HtmlDocument.IMAGE || doc.kind[b] == HtmlDocument.RULE || doc.kind[b] == HtmlDocument.CONTROL;
	}

	/** The colour of an opaque ARGB value (CSS), made on first use and disposed with the layout. */
	public Color color(int argb) {
		return colors.computeIfAbsent(argb, v -> new Color(device, (v >>> 16) & 0xFF, (v >>> 8) & 0xFF, v & 0xFF));
	}

	private void clearCache() {
		for (int s = 0; s < cacheSize; s++) cached[s].dispose();
		Arrays.fill(cached, null);
		cacheSize = 0;
	}

	/* the height of a line of body text, px, measured once */
	private int lineHeight;

	private int lineHeight() {
		if (lineHeight == 0) {
			TextLayout probe = new TextLayout(device);
			probe.setFont(font(0, 0));
			probe.setText("Mg");
			lineHeight = Math.max(1, probe.getBounds().height);
			probe.dispose();
		}
		return lineHeight;
	}

	/**
	 * The height of control block {@code b}'s widget: a line and a margin for a field, a button or a list; the rows of a
	 * text area (2 when not given); a line for a check box or a radio.
	 */
	public int controlHeight(int b) {
		int c = doc.blockControl[b], line = lineHeight();
		return switch (doc.controlType[c]) {
			case HtmlDocument.TEXTAREA -> (doc.controlRows[c] > 0 ? doc.controlRows[c] : 2) * line + 8;
			case HtmlDocument.CHECKBOX, HtmlDocument.RADIO -> line + 2;
			default -> line + 10;
		};
	}

	/**
	 * The width of control block {@code b}'s widget: a field or a text area by its columns (20 when not given), a list
	 * by its longest option, a button by its label, a check box or a radio by a line.
	 */
	public int controlWidth(int b) {
		int c = doc.blockControl[b], line = lineHeight(), character = Math.max(1, line / 2);
		return switch (doc.controlType[c]) {
			case HtmlDocument.TEXT, HtmlDocument.PASSWORD, HtmlDocument.TEXTAREA ->
				(doc.controlCols[c] > 0 ? doc.controlCols[c] : 20) * character + 8;
			case HtmlDocument.SELECT -> {
				int longest = 4;
				for (String option : doc.controlOptions[c]) longest = Math.max(longest, option.length());
				yield longest * character + 32;
			}
			case HtmlDocument.CHECKBOX, HtmlDocument.RADIO -> line + 4;
			default -> Math.max(60, doc.controlLabel[c].length() * character + 24);
		};
	}
}
