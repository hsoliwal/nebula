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
 */
public final class HtmlLayout {

	/** Space before and after a block, and the indent of one list level, in pixels. */
	public static final int BLOCK_GAP = 8, LIST_INDENT = 24, IMAGE_PLACEHOLDER = 16;

	/** CSS default heading sizes relative to body text, h1 to h6. */
	private static final float[] HEADING_SCALE = { 2f, 1.5f, 1.17f, 1f, .83f, .67f };

	private static final int CACHE = 64;

	private final Device device;
	private final HtmlDocument doc;
	private final FontData base;
	private final Color linkColor;
	/* fonts by (size class: 0 body, 1-6 headings) x (bold, italic bits), made on first use */
	private final Font[] fonts = new Font[7 * 4];

	private int width = -1;
	/* block tops (top[b] valid for b <= measured) and heights; measured = number of blocks measured so far */
	private int[] top, height;
	private int measured;

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
	}

	/** Sets the width blocks wrap to; a new width forgets every measure. */
	public void setWidth(int newWidth) {
		if (newWidth == width) return;
		width = Math.max(1, newWidth);
		measured = 0;
		clearCache();
	}

	public int width() {
		return width;
	}

	/** Measures blocks until {@code y} is covered or the document ends. */
	public void ensure(int y) {
		while (measured < doc.blockCount && top[measured] <= y) measureNext();
	}

	/** The block whose box holds {@code y} (clamped to the document). */
	public int blockAt(int y) {
		ensure(y);
		if (measured == 0) return -1;
		int index = Arrays.binarySearch(top, 0, measured + 1, y);
		int b = index >= 0 ? index : -index - 2;
		return Math.max(0, Math.min(b, measured - 1));
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

	/** The x where block {@code b}'s content starts (list indent). */
	public int indent(int b) {
		return doc.kind[b] == HtmlDocument.LIST_ITEM ? doc.level[b] * LIST_INDENT : 0;
	}

	/** The block's text layout, made and cached on demand; null for an image block. */
	public TextLayout textLayout(int b) {
		ensureBlock(b);
		if (doc.kind[b] == HtmlDocument.IMAGE) return null;
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
		int b = blockAt(y);
		if (b < 0 || doc.kind[b] == HtmlDocument.IMAGE) return null;
		TextLayout layout = textLayout(b);
		int lx = x - indent(b), ly = y - top[b] - BLOCK_GAP;
		if (lx < 0 || ly < 0 || !layout.getBounds().contains(lx, ly)) return null;
		int[] trailing = new int[1];
		int offset = layout.getOffset(lx, ly, trailing) + doc.textStart[b];
		for (int k = 0; k < doc.runCount[b]; k++) {
			int r = doc.firstRun[b] + k;
			if (doc.runLink[r] >= 0 && offset >= doc.runStart[r] && offset < doc.runEnd[r]) return doc.href[doc.runLink[r]];
		}
		return null;
	}

	public void dispose() {
		clearCache();
		for (Font font : fonts) if (font != null) font.dispose();
		Arrays.fill(fonts, null);
	}

	private void ensureBlock(int b) {
		if (b < 0 || b >= doc.blockCount) throw new IndexOutOfBoundsException(b);
		while (measured <= b) measureNext();
	}

	private void measureNext() {
		if (width < 0) throw new IllegalStateException("setWidth first");
		int b = measured;
		int h;
		if (doc.kind[b] == HtmlDocument.IMAGE) {
			int i = doc.image[b];
			h = doc.imageHeight[i] > 0 ? doc.imageHeight[i] : IMAGE_PLACEHOLDER;
		} else {
			boolean keep = false;
			TextLayout layout = null;
			for (int s = 0; s < cacheSize; s++) if (cachedBlock[s] == b) { layout = cached[s]; keep = true; }
			if (layout == null) layout = make(b);
			h = layout.getBounds().height;
			if (!keep) layout.dispose();
		}
		height[b] = h + 2 * BLOCK_GAP;
		top[b + 1] = top[b] + height[b];
		measured++;
	}

	private TextLayout make(int b) {
		layoutsMade++;
		TextLayout layout = new TextLayout(device);
		layout.setWidth(Math.max(1, width - indent(b)));
		int sizeClass = doc.kind[b] == HtmlDocument.HEADING ? doc.level[b] : 0;
		int blockBold = sizeClass > 0 ? HtmlDocument.BOLD : 0;
		layout.setFont(font(sizeClass, blockBold));
		int start = doc.textStart[b];
		layout.setText(doc.text.substring(start, doc.textEnd[b]));
		for (int k = 0; k < doc.runCount[b]; k++) {
			int r = doc.firstRun[b] + k, s = doc.runStyle[r];
			if (s == 0) continue;
			TextStyle style = new TextStyle(font(sizeClass, (s | blockBold) & (HtmlDocument.BOLD | HtmlDocument.ITALIC)), null, null);
			style.underline = (s & (HtmlDocument.UNDERLINE | HtmlDocument.LINK)) != 0;
			style.strikeout = (s & HtmlDocument.STRIKE) != 0;
			if ((s & HtmlDocument.LINK) != 0) style.foreground = linkColor;
			layout.setStyle(style, doc.runStart[r] - start, doc.runEnd[r] - start - 1);
		}
		return layout;
	}

	private Font font(int sizeClass, int boldItalic) {
		int slot = sizeClass * 4 + (boldItalic & 3);
		if (fonts[slot] == null) {
			float scale = sizeClass == 0 ? 1f : HEADING_SCALE[sizeClass - 1];
			int style = ((boldItalic & HtmlDocument.BOLD) != 0 ? SWT.BOLD : 0) | ((boldItalic & HtmlDocument.ITALIC) != 0 ? SWT.ITALIC : 0);
			fonts[slot] = new Font(device, new FontData(base.getName(), Math.max(1, Math.round(base.getHeight() * scale)), style));
		}
		return fonts[slot];
	}

	private void clearCache() {
		for (int s = 0; s < cacheSize; s++) cached[s].dispose();
		Arrays.fill(cached, null);
		cacheSize = 0;
	}
}
