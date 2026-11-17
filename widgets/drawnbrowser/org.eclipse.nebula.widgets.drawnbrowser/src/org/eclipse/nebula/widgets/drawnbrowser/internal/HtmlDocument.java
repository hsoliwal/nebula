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
 * A parsed HTML document as parent-owned primitive lanes: no object per element. The drawn browser lays out and
 * paints blocks; a block is a paragraph, a heading, a list item or an image. Its text is a slice of one shared
 * string; its style runs (bold, italic, underline, strike-through, link) are ranges of that slice.
 * <p>
 * Semantics follow prasser/swtbrowser's tag handlers (Apache-2.0, 92120bef61): p, h1-h6, br, strong/b, em/i, u,
 * del/strike/s, a, ul, ol, li, img; script content is not text; style content and style attributes are the CSS subset
 * of {@link HtmlStyles}. Whitespace collapses as in HTML's normal
 * flow: runs of white space become one space, and a block's text has no leading or trailing space; br is a hard
 * line break.
 * </p>
 */
public final class HtmlDocument {

	/** Block kinds. */
	public static final int PARAGRAPH = 0, HEADING = 1, LIST_ITEM = 2, IMAGE = 3;

	/** Style bits of a run. */
	public static final int BOLD = 1, ITALIC = 2, UNDERLINE = 4, STRIKE = 8, LINK = 16;

	/** No ordinal: an unordered list item's marker. */
	public static final int BULLET = -1;

	/** The text of every block, one after another. */
	public String text = "";

	public int blockCount;
	/**
	 * Per block: kind; level (heading 1-6, list depth from 1, else 0); marker (list ordinal, {@link #BULLET}, or 0);
	 * text start and end in {@link #text}; first run and run count; image index or -1.
	 */
	public int[] kind = new int[16], level = new int[16], marker = new int[16], textStart = new int[16],
			textEnd = new int[16], firstRun = new int[16], runCount = new int[16], image = new int[16];
	/** Per block, from CSS: background (ARGB, 0 none), alignment ({@link HtmlStyles#LEFT} ...), extra indent px. */
	public int[] blockBackground = new int[16], blockAlign = new int[16], blockIndent = new int[16];

	public int runTotal;
	/** Per run: start and end in {@link #text}, style bits, link index or -1. */
	public int[] runStart = new int[32], runEnd = new int[32], runStyle = new int[32], runLink = new int[32];
	/** Per run, from CSS: text colour (ARGB, 0 the widget's). */
	public int[] runColor = new int[32];

	public int linkCount;
	public String[] href = new String[8];

	public int imageCount;
	/** Per image: source, and width and height from the tag (or -1). */
	public String[] source = new String[4];
	public int[] imageWidth = new int[4], imageHeight = new int[4];

	private HtmlDocument() {
	}

	/** Parses {@code html}; never fails. */
	public static HtmlDocument parse(String html) {
		Builder builder = new Builder();
		HtmlTokenizer.tokenize(html, builder);
		builder.closeBlock();
		builder.doc.text = builder.chars.toString();
		return builder.doc;
	}

	/** The text of block {@code b}. */
	public String blockText(int b) {
		return text.substring(textStart[b], textEnd[b]);
	}

	private static final class Builder implements HtmlTokenizer.Handler {
		final HtmlDocument doc = new HtmlDocument();
		final StringBuilder chars = new StringBuilder();
		int open = -1;                 // the block being filled, or -1
		int style, link = -1;          // current inline style bits and link
		int bold, italic, underline, strike;
		int skip;                      // inside script
		/* CSS: the sheet, the text of a style element being read, and one computed style per open element */
		final HtmlStyles styles = new HtmlStyles();
		final StringBuilder css = new StringBuilder();
		int cssDepth;
		int[][] frames = { HtmlStyles.root() };
		String[] frameTags = { "" };
		int depth;
		int listDepth;
		int[] ordinals = new int[8];   // per list depth: next ordinal, or BULLET for ul
		boolean space;                 // a collapsed space is pending

		@Override
		public void startTag(String name, String[] attributes, boolean selfClosing) {
			flushSpace();
			if (name.equals("style")) {
				cssDepth += selfClosing ? 0 : 1;
				return;
			}
			if (!selfClosing && !name.equals("br") && !name.equals("img") && !name.equals("script")) {
				push(name, styles.compute(frames[depth], name, HtmlTokenizer.attribute(attributes, "class"),
						HtmlTokenizer.attribute(attributes, "style")));
			}
			switch (name) {
				case "script" -> skip += selfClosing ? 0 : 1;
				case "p", "div" -> openBlock(PARAGRAPH, 0, 0);
				case "h1", "h2", "h3", "h4", "h5", "h6" -> openBlock(HEADING, name.charAt(1) - '0', 0);
				case "ul", "ol" -> {
					closeBlock();
					listDepth++;
					if (listDepth >= ordinals.length) ordinals = Arrays.copyOf(ordinals, 2 * ordinals.length);
					ordinals[listDepth] = name.equals("ol") ? start(attributes) : BULLET;
				}
				case "li" -> {
					int depth = Math.max(1, listDepth);
					int mark = listDepth == 0 || ordinals[depth] == BULLET ? BULLET : ordinals[depth]++;
					openBlock(LIST_ITEM, depth, mark);
				}
				case "br" -> {
					if (open < 0) openBlock(PARAGRAPH, 0, 0);
					trimTrailingSpace();
					chars.append('\n');
					space = false;
				}
				case "img" -> image(attributes);
				case "strong", "b" -> style(BOLD, ++bold);
				case "em", "i" -> style(ITALIC, ++italic);
				case "u" -> style(UNDERLINE, ++underline);
				case "del", "strike", "s" -> style(STRIKE, ++strike);
				case "a" -> {
					String target = HtmlTokenizer.attribute(attributes, "href");
					if (target != null) {
						flushSpace();
						link = addLink(target);
					}
				}
				default -> {
				}
			}
		}

		@Override
		public void endTag(String name) {
			flushSpace();
			if (name.equals("style")) {
				if (cssDepth > 0 && --cssDepth == 0) {
					styles.addSheet(css.toString());
					css.setLength(0);
				}
				return;
			}
			pop(name);
			switch (name) {
				case "script" -> skip = Math.max(0, skip - 1);
				case "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li" -> closeBlock();
				case "ul", "ol" -> {
					closeBlock();
					listDepth = Math.max(0, listDepth - 1);
				}
				case "strong", "b" -> style(BOLD, bold = Math.max(0, bold - 1));
				case "em", "i" -> style(ITALIC, italic = Math.max(0, italic - 1));
				case "u" -> style(UNDERLINE, underline = Math.max(0, underline - 1));
				case "del", "strike", "s" -> style(STRIKE, strike = Math.max(0, strike - 1));
				case "a" -> link = -1;
				default -> {
				}
			}
		}

		@Override
		public void text(String text) {
			if (cssDepth > 0) {
				css.append(text);
				return;
			}
			if (skip > 0) return;
			for (int i = 0, n = text.length(); i < n; i++) {
				char c = text.charAt(i);
				if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
					space = true;
					continue;
				}
				if (open < 0) openBlock(PARAGRAPH, 0, 0);
				if (space && chars.length() > doc.textStart[open] && chars.charAt(chars.length() - 1) != '\n') appendRun(' ');
				space = false;
				appendRun(c);
			}
		}

		/* sets or clears one style bit; the counter keeps nested tags of the same kind balanced */
		void style(int bit, int depth) {
			flushSpace();
			style = depth > 0 ? style | bit : style & ~bit;
		}

		/* a collapsed space written before a style or link change belongs to the text before it */
		void flushSpace() {
			if (!space || open < 0 || chars.length() == doc.textStart[open] || chars.charAt(chars.length() - 1) == '\n') return;
			appendRun(' ');
			space = false;
		}

		int start(String[] attributes) {
			String value = HtmlTokenizer.attribute(attributes, "start");
			try {
				return value == null ? 1 : Integer.parseInt(value.trim());
			} catch (NumberFormatException e) {
				return 1;
			}
		}

		/* appends one character to the open block, extending the last run when its style and link match */
		void appendRun(char c) {
			int at = chars.length();
			chars.append(c);
			int[] frame = frames[depth];
			int effective = (link >= 0 ? style | LINK : style) | frame[HtmlStyles.BITS], color = frame[HtmlStyles.COLOR];
			HtmlDocument d = doc;
			int last = d.runTotal - 1;
			if (d.runCount[open] > 0 && d.runEnd[last] == at && d.runStyle[last] == effective && d.runLink[last] == link
					&& d.runColor[last] == color) {
				d.runEnd[last] = at + 1;
				return;
			}
			if (d.runTotal == d.runStart.length) {
				int capacity = 2 * d.runStart.length;
				d.runStart = Arrays.copyOf(d.runStart, capacity);
				d.runEnd = Arrays.copyOf(d.runEnd, capacity);
				d.runStyle = Arrays.copyOf(d.runStyle, capacity);
				d.runLink = Arrays.copyOf(d.runLink, capacity);
				d.runColor = Arrays.copyOf(d.runColor, capacity);
			}
			d.runStart[d.runTotal] = at;
			d.runEnd[d.runTotal] = at + 1;
			d.runStyle[d.runTotal] = effective;
			d.runLink[d.runTotal] = link;
			d.runColor[d.runTotal] = color;
			if (d.runCount[open]++ == 0) d.firstRun[open] = d.runTotal;
			d.runTotal++;
		}

		void openBlock(int kind, int level, int mark) {
			closeBlock();
			HtmlDocument d = doc;
			if (d.blockCount == d.kind.length) {
				int capacity = 2 * d.kind.length;
				d.kind = Arrays.copyOf(d.kind, capacity);
				d.level = Arrays.copyOf(d.level, capacity);
				d.marker = Arrays.copyOf(d.marker, capacity);
				d.textStart = Arrays.copyOf(d.textStart, capacity);
				d.textEnd = Arrays.copyOf(d.textEnd, capacity);
				d.firstRun = Arrays.copyOf(d.firstRun, capacity);
				d.runCount = Arrays.copyOf(d.runCount, capacity);
				d.image = Arrays.copyOf(d.image, capacity);
				d.blockBackground = Arrays.copyOf(d.blockBackground, capacity);
				d.blockAlign = Arrays.copyOf(d.blockAlign, capacity);
				d.blockIndent = Arrays.copyOf(d.blockIndent, capacity);
			}
			open = d.blockCount++;
			d.kind[open] = kind;
			d.level[open] = level;
			d.marker[open] = mark;
			d.textStart[open] = d.textEnd[open] = chars.length();
			d.firstRun[open] = d.runTotal;
			d.runCount[open] = 0;
			d.image[open] = -1;
			// an explicit block's own element is the top frame; an implicit paragraph only inherits the alignment
			int[] frame = frames[depth];
			boolean own = kind != IMAGE && depth > 0 && blockTag(frameTags[depth]);
			d.blockBackground[open] = own ? frame[HtmlStyles.BACKGROUND] : 0;
			d.blockIndent[open] = own ? frame[HtmlStyles.INDENT] : 0;
			d.blockAlign[open] = frame[HtmlStyles.ALIGN];
			space = false;
		}

		static boolean blockTag(String tag) {
			return switch (tag) {
				case "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li" -> true;
				default -> false;
			};
		}

		void push(String tag, int[] frame) {
			if (++depth == frames.length) {
				frames = Arrays.copyOf(frames, 2 * frames.length);
				frameTags = Arrays.copyOf(frameTags, 2 * frameTags.length);
			}
			frames[depth] = frame;
			frameTags[depth] = tag;
		}

		/* closes the innermost open element of that tag and every element opened inside it (misnested tags) */
		void pop(String tag) {
			for (int k = depth; k > 0; k--) {
				if (frameTags[k].equals(tag)) {
					depth = k - 1;
					return;
				}
			}
		}

		void closeBlock() {
			if (open < 0) return;
			trimTrailingSpace();
			HtmlDocument d = doc;
			d.textEnd[open] = chars.length();
			if (d.textEnd[open] == d.textStart[open] && d.image[open] < 0 && d.kind[open] != LIST_ITEM) d.blockCount--;  // empty
			open = -1;
			space = false;
		}

		void trimTrailingSpace() {
			int end = chars.length();
			if (open < 0 || end == doc.textStart[open] || chars.charAt(end - 1) != ' ') return;
			chars.setLength(end - 1);
			int last = doc.runTotal - 1;
			if (doc.runEnd[last] == end && --doc.runEnd[last] == doc.runStart[last]) {
				doc.runTotal--;
				doc.runCount[open]--;
			}
		}

		void image(String[] attributes) {
			String src = HtmlTokenizer.attribute(attributes, "src");
			if (src == null) return;
			openBlock(IMAGE, 0, 0);
			HtmlDocument d = doc;
			if (d.imageCount == d.source.length) {
				int capacity = 2 * d.source.length;
				d.source = Arrays.copyOf(d.source, capacity);
				d.imageWidth = Arrays.copyOf(d.imageWidth, capacity);
				d.imageHeight = Arrays.copyOf(d.imageHeight, capacity);
			}
			d.source[d.imageCount] = src;
			d.imageWidth[d.imageCount] = size(HtmlTokenizer.attribute(attributes, "width"));
			d.imageHeight[d.imageCount] = size(HtmlTokenizer.attribute(attributes, "height"));
			d.image[open] = d.imageCount++;
			closeBlock();
		}

		int size(String value) {
			if (value == null) return -1;
			try {
				return Integer.parseInt(value.trim().replace("px", ""));
			} catch (NumberFormatException e) {
				return -1;
			}
		}

		int addLink(String target) {
			HtmlDocument d = doc;
			if (d.linkCount == d.href.length) d.href = Arrays.copyOf(d.href, 2 * d.href.length);
			d.href[d.linkCount] = target;
			return d.linkCount++;
		}
	}
}
