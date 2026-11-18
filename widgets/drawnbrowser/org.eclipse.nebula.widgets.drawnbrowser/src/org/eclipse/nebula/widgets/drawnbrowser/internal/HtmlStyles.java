/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The CSS subset of the drawn browser.
 * <p>
 * Rules come from {@code <style>} elements and declarations from {@code style} attributes. Selectors are a tag, a
 * {@code .class}, a {@code tag.class}, or comma-separated lists of these. Properties: {@code color},
 * {@code background-color}, {@code font-weight}, {@code font-style}, {@code text-decoration}, {@code text-align}, the box
 * model in px: {@code margin} and {@code padding} (one to four values, or one side: {@code margin-top} ...),
 * {@code border} (width, {@code solid}/{@code none}, colour), {@code border-width}, {@code border-color},
 * {@code border-style}. Anything else (other units, other border styles) is ignored, never an error.
 * </p>
 * <p>
 * The cascade applies the rules that match an element in specificity order (tag, class, tag.class; a later rule wins
 * a tie), then the element's own {@code style} attribute. Text properties (color, weight, style, decoration) inherit;
 * so does text-align; background, indent and the box (margins, padding, border) belong to the element that sets them. A computed style is an
 * int array indexed by the constants below, no object per element.
 * </p>
 * <p>
 * Pattern: Flying Saucer (XHTML and CSS 2.1, LGPL; its licence is recorded, not copied) for the cascade order; this
 * class keeps only the subset above.
 * </p>
 */
public final class HtmlStyles {

	/**
	 * Computed style slots: ARGB colour (0 inherits the widget's), background, set style bits, align, indent px (the
	 * left margin); then the box, px: top, right and bottom margins, the four paddings, the border's width and its ARGB
	 * colour (0: the widget's foreground).
	 */
	public static final int COLOR = 0, BACKGROUND = 1, BITS = 2, ALIGN = 3, INDENT = 4, MARGIN_TOP = 5,
			MARGIN_RIGHT = 6, MARGIN_BOTTOM = 7, PADDING_TOP = 8, PADDING_RIGHT = 9, PADDING_BOTTOM = 10,
			PADDING_LEFT = 11, BORDER_WIDTH = 12, BORDER_COLOR = 13, SLOTS = 14;

	/* the numeric box slots, "not set" (-1) in a declaration delta */
	private static final int[] BOX = { MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, PADDING_TOP, PADDING_RIGHT,
			PADDING_BOTTOM, PADDING_LEFT, BORDER_WIDTH };

	/** Alignments. */
	public static final int LEFT = 0, CENTER = 1, RIGHT = 2;

	/* one rule per selector: tag (or null), class (or null), specificity, declarations, order */
	private final List<String[]> selectors = new ArrayList<>();
	private final List<int[]> declarations = new ArrayList<>();

	/** Adds the rules of a style sheet. */
	public void addSheet(String css) {
		String text = css.replaceAll("(?s)/\\*.*?\\*/", "");
		int i = 0;
		while (i < text.length()) {
			int open = text.indexOf('{', i), close = open < 0 ? -1 : text.indexOf('}', open);
			if (open < 0 || close < 0) break;
			int[] decls = parse(text.substring(open + 1, close), emptyDelta());
			for (String selector : text.substring(i, open).split(",")) {
				String s = selector.trim().toLowerCase(Locale.ROOT);
				if (!s.matches("[a-z][a-z0-9]*|\\.[a-z_][a-z0-9_-]*|[a-z][a-z0-9]*\\.[a-z_][a-z0-9_-]*")) continue;
				int dot = s.indexOf('.');
				String tag = dot == 0 ? null : dot < 0 ? s : s.substring(0, dot);
				String cls = dot < 0 ? null : s.substring(dot + 1);
				selectors.add(new String[] { tag, cls, String.valueOf((tag != null ? 1 : 0) + (cls != null ? 10 : 0)) });
				declarations.add(decls);
			}
			i = close + 1;
		}
	}

	/**
	 * The computed style of an element.
	 *
	 * @param parent  the parent's computed style
	 * @param tag     the tag, lower case
	 * @param classes the class attribute, or null
	 * @param inline  the style attribute, or null
	 */
	public int[] compute(int[] parent, String tag, String classes, String inline) {
		int[] style = new int[SLOTS];
		style[COLOR] = parent[COLOR];
		style[BITS] = parent[BITS];
		style[ALIGN] = parent[ALIGN];
		String[] own = classes == null ? new String[0] : classes.trim().toLowerCase(Locale.ROOT).split("\\s+");
		for (int specificity : new int[] { 1, 10, 11 }) {
			for (int r = 0; r < selectors.size(); r++) {
				String[] s = selectors.get(r);
				if (Integer.parseInt(s[2]) != specificity) continue;
				if (s[0] != null && !s[0].equals(tag)) continue;
				if (s[1] != null && !contains(own, s[1])) continue;
				apply(style, declarations.get(r));
			}
		}
		if (inline != null) apply(style, parse(inline, emptyDelta()));
		return style;
	}

	/** The style of the document root: the widget's colours, nothing set. */
	public static int[] root() {
		return new int[SLOTS];
	}

	/* a declaration delta with nothing set */
	private static int[] emptyDelta() {
		int[] delta = new int[SLOTS];
		delta[ALIGN] = -1;
		delta[INDENT] = -1;
		for (int slot : BOX) delta[slot] = -1;
		return delta;
	}

	/* a length in px ("12px", "12", "0"), or -1 when it is not one */
	private static int px(String value) {
		return value.matches("\\d{1,4}(px)?") ? Integer.parseInt(value.replace("px", "")) : -1;
	}

	/* one to four lengths: top, right, bottom, left as CSS repeats them */
	private static void sides(int[] delta, String value, int top, int right, int bottom, int left) {
		String[] v = value.trim().split("\\s+");
		if (v.length > 4) return;
		int[] n = new int[v.length];
		for (int i = 0; i < v.length; i++) if ((n[i] = px(v[i])) < 0) return;
		int t = n[0], r = v.length > 1 ? n[1] : t, b = v.length > 2 ? n[2] : t, l = v.length > 3 ? n[3] : r;
		delta[top] = t;
		delta[right] = r;
		delta[bottom] = b;
		delta[left] = l;
	}

	/* border shorthand: a width, a style (solid, or none / hidden for no border) and a colour, in any order */
	private static void border(int[] delta, String value) {
		for (String part : value.trim().split("\\s+")) {
			if (px(part) >= 0) delta[BORDER_WIDTH] = px(part);
			else if (part.equals("none") || part.equals("hidden")) delta[BORDER_WIDTH] = 0;
			else if (color(part) != 0) delta[BORDER_COLOR] = color(part);
		}
	}

	private static boolean contains(String[] values, String value) {
		for (String v : values) if (v.equals(value)) return true;
		return false;
	}

	/* declarations as a delta: 0 colour/background = not set, BITS = bits set | (bits cleared << 8), -1 = not set */
	private static int[] parse(String block, int[] delta) {
		for (String declaration : block.split(";")) {
			int colon = declaration.indexOf(':');
			if (colon < 0) continue;
			String name = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
			String value = declaration.substring(colon + 1).trim().toLowerCase(Locale.ROOT).replace("!important", "").trim();
			switch (name) {
				case "color" -> delta[COLOR] = color(value);
				case "background-color", "background" -> delta[BACKGROUND] = color(value);
				case "font-weight" -> delta[BITS] = bit(delta[BITS], HtmlDocument.BOLD,
						value.equals("bold") || value.equals("bolder") || value.matches("[6-9]00") ? 1 : value.equals("normal") || value.matches("[1-5]00") ? -1 : 0);
				case "font-style" -> delta[BITS] = bit(delta[BITS], HtmlDocument.ITALIC,
						value.equals("italic") || value.equals("oblique") ? 1 : value.equals("normal") ? -1 : 0);
				case "text-decoration", "text-decoration-line" -> {
					if (value.contains("none")) {
						delta[BITS] = bit(bit(delta[BITS], HtmlDocument.UNDERLINE, -1), HtmlDocument.STRIKE, -1);
					} else {
						if (value.contains("underline")) delta[BITS] = bit(delta[BITS], HtmlDocument.UNDERLINE, 1);
						if (value.contains("line-through")) delta[BITS] = bit(delta[BITS], HtmlDocument.STRIKE, 1);
					}
				}
				case "text-align" -> delta[ALIGN] = value.equals("center") ? CENTER : value.equals("right") ? RIGHT
						: value.equals("left") ? LEFT : delta[ALIGN];
				case "margin-left" -> {
					if (value.matches("\\d{1,4}(px)?")) delta[INDENT] = Integer.parseInt(value.replace("px", ""));
				}
				case "margin" -> sides(delta, value, MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, INDENT);
				case "margin-top", "margin-right", "margin-bottom" -> {
					int slot = name.equals("margin-top") ? MARGIN_TOP : name.equals("margin-right") ? MARGIN_RIGHT : MARGIN_BOTTOM;
					if (px(value) >= 0) delta[slot] = px(value);
				}
				case "padding" -> sides(delta, value, PADDING_TOP, PADDING_RIGHT, PADDING_BOTTOM, PADDING_LEFT);
				case "padding-top", "padding-right", "padding-bottom", "padding-left" -> {
					int slot = switch (name) {
						case "padding-top" -> PADDING_TOP;
						case "padding-right" -> PADDING_RIGHT;
						case "padding-bottom" -> PADDING_BOTTOM;
						default -> PADDING_LEFT;
					};
					if (px(value) >= 0) delta[slot] = px(value);
				}
				case "border" -> border(delta, value);
				case "border-width" -> {
					if (px(value) >= 0) delta[BORDER_WIDTH] = px(value);
				}
				case "border-color" -> delta[BORDER_COLOR] = color(value);
				case "border-style" -> {
					if (value.equals("none") || value.equals("hidden")) delta[BORDER_WIDTH] = 0;
				}
				default -> {
				}
			}
		}
		return delta;
	}

	private static int bit(int bits, int bit, int sign) {
		if (sign > 0) return (bits | bit) & ~(bit << 8);
		if (sign < 0) return (bits & ~bit) | (bit << 8);
		return bits;
	}

	private static void apply(int[] style, int[] delta) {
		if (delta[COLOR] != 0) style[COLOR] = delta[COLOR];
		if (delta[BACKGROUND] != 0) style[BACKGROUND] = delta[BACKGROUND];
		int set = delta[BITS] & 0xFF, cleared = (delta[BITS] >>> 8) & 0xFF;
		style[BITS] = (style[BITS] | set) & ~cleared;
		if (delta[ALIGN] >= 0) style[ALIGN] = delta[ALIGN];
		if (delta[INDENT] >= 0) style[INDENT] = delta[INDENT];
		for (int slot : BOX) if (delta[slot] >= 0) style[slot] = delta[slot];
		if (delta[BORDER_COLOR] != 0) style[BORDER_COLOR] = delta[BORDER_COLOR];
	}

	/** An opaque ARGB colour from #rgb, #rrggbb, rgb(r, g, b) or a basic colour name; 0 when not one of these. */
	public static int color(String value) {
		String v = value.trim().toLowerCase(Locale.ROOT);
		try {
			if (v.matches("#[0-9a-f]{3}")) {
				int r = Integer.parseInt(v.substring(1, 2), 16), g = Integer.parseInt(v.substring(2, 3), 16), b = Integer.parseInt(v.substring(3, 4), 16);
				return 0xFF000000 | (r * 17) << 16 | (g * 17) << 8 | b * 17;
			}
			if (v.matches("#[0-9a-f]{6}")) return 0xFF000000 | Integer.parseInt(v.substring(1), 16);
			if (v.matches("rgb\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*\\)")) {
				String[] c = v.substring(4, v.length() - 1).split(",");
				int r = Math.min(255, Integer.parseInt(c[0].trim())), g = Math.min(255, Integer.parseInt(c[1].trim())), b = Math.min(255, Integer.parseInt(c[2].trim()));
				return 0xFF000000 | r << 16 | g << 8 | b;
			}
		} catch (NumberFormatException e) {
			return 0;
		}
		return switch (v) {
			case "black" -> 0xFF000000;
			case "white" -> 0xFFFFFFFF;
			case "red" -> 0xFFFF0000;
			case "lime" -> 0xFF00FF00;
			case "green" -> 0xFF008000;
			case "blue" -> 0xFF0000FF;
			case "gray", "grey" -> 0xFF808080;
			case "silver" -> 0xFFC0C0C0;
			case "maroon" -> 0xFF800000;
			case "navy" -> 0xFF000080;
			case "olive" -> 0xFF808000;
			case "purple" -> 0xFF800080;
			case "teal" -> 0xFF008080;
			case "orange" -> 0xFFFFA500;
			case "yellow" -> 0xFFFFFF00;
			case "fuchsia", "magenta" -> 0xFFFF00FF;
			case "aqua", "cyan" -> 0xFF00FFFF;
			default -> 0;
		};
	}
}
