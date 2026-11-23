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
 * Rules come from {@code <style>} elements and declarations from {@code style} attributes. Selectors, comma-separated:
 * compounds of a tag or {@code *}, {@code #id}, {@code .class}es, attribute tests ({@code [a]}, {@code [a=v]},
 * {@code [a~=v]}, {@code [a|=v]}, {@code [a^=v]}, {@code [a$=v]}, {@code [a*=v]}), {@code :first-child} and
 * {@code :link}, joined by descendant, child ({@code >}) and adjacent sibling ({@code +}) combinators; a selector with
 * anything else is ignored. Matching and specificity follow Flying Saucer's newmatch (LGPL, e02852faa3; its licence is
 * recorded, not acted on): axes, conditions, and specificity as ids, then classes, attributes and pseudo-classes,
 * then elements. Properties: {@code color},
 * {@code background-color}, {@code font-weight}, {@code font-style}, {@code text-decoration}, {@code text-align}, the box
 * model in px: {@code margin} and {@code padding} (one to four values, or one side: {@code margin-top} ...),
 * {@code border} (width, {@code solid}/{@code none}, colour), {@code border-width}, {@code border-color},
 * {@code border-style}; {@code display} (none, flex, grid, others kept as the element's default), and for a flex row
 * {@code flex-direction} row, {@code justify-content}, {@code gap}, and on its items {@code flex-grow},
 * {@code flex-shrink} (finite nonnegative numbers), {@code flex-basis} and {@code width} (px). The flex shorthand
 * accepts the existing integer grow/pixel-or-auto basis subset, including its fractional shrink factor; for a grid
 * {@code grid-template-columns} ({@link GridTracks}),
 * {@code justify-content}, {@code gap} (row and column), {@code row-gap}, {@code column-gap}, and on its items
 * {@code grid-column} or {@code grid-column-end} {@code span N} (or {@code 1 / -1}, the whole row); positioning: {@code position} (static, relative, absolute, fixed, sticky), {@code top} and {@code left}
 * (px, negative allowed, or auto) and {@code z-index}. Anything else (other units, other border styles) is ignored,
 * never an error.
 * {@code border-style}. Block boxes additionally accept {@code position} (static, relative, absolute, fixed),
 * {@code left/top/right/bottom}, {@code width/height} and {@code z-index}. Pixel lengths are signed integers;
 * width and height must be nonnegative. Top and left retain their existing unitless integers (up to five decimal
 * digits with an optional minus sign); the other lengths accept unitless zero. Percentages, fractional lengths
 * and other units are ignored.
 * Geometry reserves {@link #AUTO} ({@code Integer.MIN_VALUE + 1}) for {@code auto}; z-index accepts every signed
 * int, including that value. Anything else is ignored, never an error.
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

	private final Runnable checkpoint;

	/** Creates a style sheet with the ordinary, non-cancellable parsing policy. */
	public HtmlStyles() {
		this(HtmlTokenizer.UNCHECKED);
	}

	HtmlStyles(Runnable checkpoint) {
		this.checkpoint = checkpoint;
	}

	/**
	 * Computed style slots: ARGB colour (0 inherits the widget's), background, set style bits, align, indent px (the
	 * left margin); then the box, px: top, right and bottom margins, the four paddings, the border's width and its ARGB
	 * colour (0: the widget's foreground).
	 */
	public static final int COLOR = 0, BACKGROUND = 1, BITS = 2, ALIGN = 3, INDENT = 4, MARGIN_TOP = 5,
			MARGIN_RIGHT = 6, MARGIN_BOTTOM = 7, PADDING_TOP = 8, PADDING_RIGHT = 9, PADDING_BOTTOM = 10,
			PADDING_LEFT = 11, BORDER_WIDTH = 12, BORDER_COLOR = 13, POSITION = 14, POS_TOP = 15, POS_LEFT = 16,
			Z_INDEX = 17, DISPLAY = 18, FLEX_GROW = 19, FLEX_BASIS = 20, JUSTIFY = 21, GAP = 22, ROW_GAP = 23,
			TRACKS = 24, COLUMN_SPAN = 25, BORDER_STYLE = 26;
	/** Positioned-box additions preserve every current table, flex, grid and border slot. */
	public static final int OFFSET_TOP = POS_TOP, OFFSET_LEFT = POS_LEFT, OFFSET_RIGHT = 27, OFFSET_BOTTOM = 28,
			WIDTH = 29, HEIGHT = 30, Z_AUTO = 31;
	/** Two primitive words preserve a finite double shrink factor without reusing the positional presence mask. */
	private static final int SHRINK_HIGH = 32, SHRINK_LOW = 33;
	/** Number of primitive computed-style slots. Existing slots retain their indices. */
	public static final int OVERFLOW = 34, SLOTS = 35;
	/** Parent-owned modes: hidden/auto/scroll are local scrollports; clip paints only. */
	public static final int OVERFLOW_VISIBLE = 0, OVERFLOW_HIDDEN = 1, OVERFLOW_CLIP = 2,
			OVERFLOW_AUTO = 3, OVERFLOW_SCROLL = 4;

	/** Draw2D SchemeBorder-style highlight/shadow schemes, encoded as scalar CSS. */
	public static final int BORDER_SOLID = 0, BORDER_INSET = 1, BORDER_OUTSET = 2,
			BORDER_GROOVE = 3, BORDER_RIDGE = 4, BORDER_DOUBLE = 5;

	/**
	 * Displays ({@link #DISPLAY}): as its element is by default, none (not shown at all), flex (a row flex container),
	 * grid (a grid container: {@link #TRACKS} names its columns through {@link #tracks(int)}, {@link #GAP} is its column
	 * gap and {@link #ROW_GAP} its row gap; an item spans {@link #COLUMN_SPAN} columns, 0 meaning 1).
	 */
	public static final int DISPLAY_DEFAULT = 0, DISPLAY_NONE = 1, DISPLAY_FLEX = 2, DISPLAY_GRID = 3;

	/* grid-column span N */
	private static final java.util.regex.Pattern SPAN = java.util.regex.Pattern.compile("span\\s+(\\d{1,4})");

	/** Positions ({@link #POSITION}). */
	public static final int STATIC = 0, RELATIVE = 1, ABSOLUTE = 2, FIXED = 3, STICKY = 4;

	/** {@link #POS_TOP} or {@link #POS_LEFT} not given (auto): the box keeps its place in the flow on that axis. */
	public static final int AUTO = Integer.MIN_VALUE + 1;

	/* a declaration that does not set a position slot */
	private static final int NOT_SET = Integer.MIN_VALUE;
	/* Only original position slots 14..31 use the 32-bit presence mask. New lanes use explicit sentinels. */
	private static final int PRESENT = SLOTS;
	private static final int SHRINK_PRESENT = SLOTS + 1;
	private static final java.util.regex.Pattern NUMBER = java.util.regex.Pattern.compile(
			"[+-]?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");

	/* the numeric box slots, "not set" (-1) in a declaration delta */
	private static final int[] BOX = { MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, PADDING_TOP, PADDING_RIGHT,
			PADDING_BOTTOM, PADDING_LEFT, BORDER_WIDTH, BORDER_STYLE };

	/** Alignments. */
	public static final int LEFT = 0, CENTER = 1, RIGHT = 2;

	/* the rules of the sheets, one per selector, in sheet order */
	private final List<Rule> rules = new ArrayList<>();

	/**
	 * An element as the parser holds it, with its ancestors and their previous siblings: level {@code up} 0 is the
	 * element's own level, 1 its parent's, and so on; at a level, {@code back} 0 is the element (or the ancestor) and
	 * {@code back} k its k-th previous element sibling.
	 */
	public interface Path {
		/** The number of levels: the element and its ancestors. */
		int depth();

		/** The number of element siblings before the element (or the ancestor) at level {@code up}. */
		int siblings(int up);

		/** The tag, lower case, of the element at ({@code up}, {@code back}). */
		String tag(int up, int back);

		/** An attribute of the element at ({@code up}, {@code back}), or null. */
		String attribute(int up, int back, String name);
	}

	/* attribute operators: [a], [a=v], [a~=v] (a word of a list), [a|=v] (v or v-...), [a^=v], [a$=v], [a*=v] */
	private static final int EXISTS = 0, EQUALS = 1, WORD = 2, DASH = 3, PREFIX = 4, SUFFIX = 5, SUBSTRING = 6;

	/*
	 * one compound selector: a tag (null: any), an id, classes, attribute tests (name, operator, value), first-child,
	 * :link
	 */
	private record Compound(String tag, String id, String[] classes, String[] attributes, int[] operators, String[] values,
			boolean firstChild, boolean link) {
	}

	/*
	 * a selector: compounds from the outermost to the subject; axes[i] joins compound i - 1 and i: ' ' descendant, '>'
	 * child, '+' immediate sibling (Flying Saucer's Selector axes); specificity B (ids), C (classes, attributes,
	 * pseudo-classes), D (elements) as one int; sheet order breaks ties
	 */
	private record Rule(Compound[] compounds, char[] axes, int specificity, int order, int[] declarations) {
	}

	private static final java.util.regex.Pattern COMPOUND = java.util.regex.Pattern.compile(
			"(\\*|[a-z][a-z0-9-]*)?((?:#[a-z_][\\w-]*|\\.[a-z_][\\w-]*|\\[[a-z_][\\w-]*(?:[~|^$*]?=(?:\"[^\"]*\"|'[^']*'|[\\w-]*))?\\]|:[a-z-]+)*)");
	private static final java.util.regex.Pattern PART = java.util.regex.Pattern.compile(
			"#([a-z_][\\w-]*)|\\.([a-z_][\\w-]*)|\\[([a-z_][\\w-]*)(?:([~|^$*]?=)(\"[^\"]*\"|'[^']*'|[\\w-]*))?\\]|:([a-z-]+)");

	/* the grid track lists the declarations name, each once; a TRACKS slot holds its index + 1 */
	private final List<int[]> trackLists = new ArrayList<>();

	/** The tracks ({@link GridTracks}) a computed style's {@link #TRACKS} slot names; empty for none. */
	public int[] tracks(int ref) {
		return ref <= 0 || ref > trackLists.size() ? new int[0] : trackLists.get(ref - 1).clone();
	}

	private int trackRef(int[] tracks) {
		for (int i = 0; i < trackLists.size(); i++) {
			checkpoint.run();
			if (java.util.Arrays.equals(trackLists.get(i), tracks)) return i + 1;
		}
		trackLists.add(tracks);
		return trackLists.size();
	}

	/** Adds the rules of a style sheet. */
	public void addSheet(String css) {
		checkpoint.run();
		String text = css.replaceAll("(?s)/\\*.*?\\*/", "");
		int i = 0;
		while (i < text.length()) {
			checkpoint.run();
			int open = text.indexOf('{', i), close = open < 0 ? -1 : text.indexOf('}', open);
			if (open < 0 || close < 0) break;
			int[] decls = parse(text.substring(open + 1, close), emptyDelta());
			for (String selector : text.substring(i, open).split(",")) {
				checkpoint.run();
				Rule rule = rule(selector.trim().toLowerCase(Locale.ROOT), decls);
				if (rule != null) rules.add(rule);
			}
			i = close + 1;
		}
	}

	/* a selector of compounds joined by descendant, child and sibling axes; null for anything else (never matches) */
	private Rule rule(String selector, int[] decls) {
		String[] tokens = selector.replace(">", " > ").replace("+", " + ").trim().split("\\s+");
		List<Compound> compounds = new ArrayList<>();
		StringBuilder axes = new StringBuilder();
		char axis = ' ';
		boolean pending = false;
		int b = 0, c = 0, d = 0;
		for (String token : tokens) {
			checkpoint.run();
			if (token.isEmpty()) return null;
			if (token.equals(">") || token.equals("+")) {
				if (compounds.isEmpty() || pending) return null;
				axis = token.charAt(0);
				pending = true;
				continue;
			}
			Compound compound = compound(token);
			if (compound == null) return null;
			axes.append(compounds.isEmpty() ? ' ' : axis);
			compounds.add(compound);
			axis = ' ';
			pending = false;
			b += compound.id() != null ? 1 : 0;
			c += compound.classes().length + compound.attributes().length + (compound.firstChild() ? 1 : 0) + (compound.link() ? 1 : 0);
			d += compound.tag() != null ? 1 : 0;
		}
		if (compounds.isEmpty() || pending) return null;
		return new Rule(compounds.toArray(new Compound[0]), axes.toString().toCharArray(), b * 10000 + c * 100 + d, rules.size(), decls);
	}

	/* one compound; null when it has a pseudo-class this subset does not know (as Flying Saucer's unsupported condition) */
	private Compound compound(String token) {
		checkpoint.run();
		java.util.regex.Matcher m = COMPOUND.matcher(token);
		if (!m.matches()) return null;
		String tag = m.group(1) == null || m.group(1).equals("*") ? null : m.group(1), id = null;
		List<String> classes = new ArrayList<>(), attributes = new ArrayList<>(), values = new ArrayList<>();
		List<Integer> operators = new ArrayList<>();
		boolean firstChild = false, link = false;
		java.util.regex.Matcher part = PART.matcher(m.group(2));
		while (part.find()) {
			checkpoint.run();
			if (part.group(1) != null) {
				if (id != null) return null;
				id = part.group(1);
			} else if (part.group(2) != null) {
				classes.add(part.group(2));
			} else if (part.group(3) != null) {
				attributes.add(part.group(3));
				String op = part.group(4), value = part.group(5);
				operators.add(op == null ? EXISTS : switch (op) {
					case "~=" -> WORD;
					case "|=" -> DASH;
					case "^=" -> PREFIX;
					case "$=" -> SUFFIX;
					case "*=" -> SUBSTRING;
					default -> EQUALS;
				});
				values.add(value == null ? null : value.startsWith("\"") || value.startsWith("'") ? value.substring(1, value.length() - 1) : value);
			} else {
				switch (part.group(6)) {
					case "first-child" -> firstChild = true;
					case "link" -> link = true;
					default -> {
						return null;
					}
				}
			}
		}
		int[] ops = new int[operators.size()];
		for (int k = 0; k < ops.length; k++) ops[k] = operators.get(k);
		return new Compound(tag, id, classes.toArray(new String[0]), attributes.toArray(new String[0]), ops,
				values.toArray(new String[0]), firstChild, link);
	}

	/*
	 * whether compounds 0..i of a rule match with compound i at (up, back): right to left, backtracking over the
	 * ancestors for a descendant axis (Flying Saucer's Selector.matches)
	 */
	private boolean matches(Rule rule, int i, Path path, int up, int back) {
		checkpoint.run();
		if (!matches(rule.compounds()[i], path, up, back)) return false;
		if (i == 0) return true;
		return switch (rule.axes()[i]) {
			case '>' -> up + 1 < path.depth() && matches(rule, i - 1, path, up + 1, 0);
			case '+' -> back + 1 <= path.siblings(up) && matches(rule, i - 1, path, up, back + 1);
			default -> {
				for (int a = up + 1; a < path.depth(); a++) if (matches(rule, i - 1, path, a, 0)) yield true;
				yield false;
			}
		};
	}

	private boolean matches(Compound c, Path path, int up, int back) {
		String tag = path.tag(up, back);
		if (c.tag() != null && !c.tag().equals(tag)) return false;
		if (c.firstChild() && path.siblings(up) - back != 0) return false;
		if (c.link() && !("a".equals(tag) && path.attribute(up, back, "href") != null)) return false;
		if (c.id() != null && !c.id().equals(lower(path.attribute(up, back, "id")))) return false;
		if (c.classes().length > 0) {
			String cls = path.attribute(up, back, "class");
			if (cls == null) return false;
			String[] own = cls.trim().toLowerCase(Locale.ROOT).split("\\s+");
			for (String want : c.classes()) if (!contains(own, want)) return false;
		}
		for (int k = 0; k < c.attributes().length; k++) {
			String value = lower(path.attribute(up, back, c.attributes()[k])), want = c.values()[k];
			if (value == null) return false;
			boolean ok = switch (c.operators()[k]) {
				case EQUALS -> value.equals(want);
				case WORD -> contains(value.trim().split("\\s+"), want);
				case DASH -> value.equals(want) || value.startsWith(want + "-");
				case PREFIX -> !want.isEmpty() && value.startsWith(want);
				case SUFFIX -> !want.isEmpty() && value.endsWith(want);
				case SUBSTRING -> !want.isEmpty() && value.contains(want);
				default -> true;
			};
			if (!ok) return false;
		}
		return true;
	}

	private static String lower(String value) {
		return value == null ? null : value.toLowerCase(Locale.ROOT);
	}

	/* an element with no ancestors and no siblings, from its tag and class attribute */
	private static Path single(String tag, String classes) {
		return new Path() {
			@Override
			public int depth() {
				return 1;
			}

			@Override
			public int siblings(int up) {
				return 0;
			}

			@Override
			public String tag(int up, int back) {
				return tag;
			}

			@Override
			public String attribute(int up, int back, String name) {
				return name.equals("class") ? classes : null;
			}
		};
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
		return compute(parent, single(tag, classes), inline);
	}

	/**
	 * The computed style of the element {@code path} starts at: the rules that match it, in specificity order (ids,
	 * then classes, attributes and pseudo-classes, then elements; sheet order among equals), then its {@code style}
	 * attribute.
	 */
	public int[] compute(int[] parent, Path path, String inline) {
		checkpoint.run();
		int[] style = root();
		style[COLOR] = parent[COLOR];
		style[BITS] = parent[BITS];
		style[ALIGN] = parent[ALIGN];
		style[POS_TOP] = AUTO;
		style[POS_LEFT] = AUTO;
		style[FLEX_BASIS] = -1;
		List<Rule> matched = new ArrayList<>();
		for (Rule rule : rules) if (matches(rule, rule.compounds().length - 1, path, 0, 0)) matched.add(rule);
		matched.sort(java.util.Comparator.comparingInt(Rule::specificity).thenComparingInt(Rule::order));
		for (Rule rule : matched) {
			checkpoint.run();
			apply(style, rule.declarations());
		}
		if (inline != null) apply(style, parse(inline, emptyDelta()));
		return style;
	}

	/** The style of the document root: the widget's colours, nothing set. */
	public static int[] root() {
		int[] root = new int[SLOTS];
		root[POS_TOP] = root[POS_LEFT] = root[OFFSET_RIGHT] = root[OFFSET_BOTTOM] = root[WIDTH] = root[HEIGHT] = AUTO;
		root[Z_AUTO] = 1;
		root[OVERFLOW] = OVERFLOW_VISIBLE;
		root[FLEX_BASIS] = -1;
		putShrink(root, 1);
		return root;
	}

	/** The non-inherited flex-shrink factor, initially 1; the legacy 32-slot style also means 1. */
	public static double shrink(int[] style) {
		return style.length <= SHRINK_LOW ? 1 : Double.longBitsToDouble(
				((long) style[SHRINK_HIGH] << 32) | (style[SHRINK_LOW] & 0xFFFFFFFFL));
	}

	private static void putShrink(int[] target, double factor) {
		long bits = Double.doubleToLongBits(factor == 0 ? 0 : factor);
		target[SHRINK_HIGH] = (int) (bits >>> 32);
		target[SHRINK_LOW] = (int) bits;
	}

	/* CSS numbers exclude Java's NaN, infinity, suffixes and hexadecimal floating-point syntax. */
	private static double shrinkNumber(String value) {
		if (!NUMBER.matcher(value).matches()) return -1;
		try {
			double factor = Double.parseDouble(value);
			return Double.isFinite(factor) && factor >= 0 ? factor : -1;
		} catch (NumberFormatException invalid) {
			return -1;
		}
	}

	private static void shrinkDeclaration(int[] delta, double factor) {
		putShrink(delta, factor);
		delta[SHRINK_PRESENT] = 1;
	}

	/* The admitted grow/basis grammar stays integer/px; the second factor accepts finite CSS numbers. */
	private static void flex(int[] delta, String value) {
		int grow, basis;
		double shrink = 1;
		if (value.equals("none") || value.equals("auto") || value.equals("initial")) {
			grow = value.equals("auto") ? 1 : 0;
			shrink = value.equals("none") ? 0 : 1;
			basis = -1;
		} else {
			String[] parts = value.trim().split("\\s+");
			if (parts.length > 3) return;
			if (!parts[0].matches("\\d{1,4}")) {
				if (parts.length != 1 || (basis = px(parts[0])) < 0) return;
				grow = 1;
			} else {
				grow = Integer.parseInt(parts[0]);
				basis = 0;
				if (parts.length > 1) {
					double factor = shrinkNumber(parts[1]);
					if (factor >= 0) {
						shrink = factor;
						if (parts.length == 3) {
							basis = parts[2].equals("auto") ? -1 : px(parts[2]);
							if (basis < 0 && !parts[2].equals("auto")) return;
						}
					} else {
						if (parts.length != 2) return;
						basis = parts[1].equals("auto") ? -1 : px(parts[1]);
						if (basis < 0 && !parts[1].equals("auto")) return;
					}
				}
			}
		}
		delta[FLEX_GROW] = grow;
		delta[FLEX_BASIS] = basis;
		shrinkDeclaration(delta, shrink);
	}

	/* a declaration delta with nothing set */
	private static int[] emptyDelta() {
		int[] delta = new int[SLOTS + 2];
		delta[ALIGN] = delta[INDENT] = -1;
		for (int slot : BOX) delta[slot] = -1;
		delta[DISPLAY] = -1;
		delta[FLEX_GROW] = -1;
		delta[FLEX_BASIS] = NOT_SET;
		delta[JUSTIFY] = -1;
		delta[GAP] = -1;
		delta[ROW_GAP] = -1;
		delta[TRACKS] = -1;
		delta[COLUMN_SPAN] = -1;
		delta[OVERFLOW] = -1;
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

	/* Supported Draw2D-style side-shading rules, encoded as primitive CSS constants. */
	private static int borderStyle(String part) {
		return switch (part) {
			case "solid" -> BORDER_SOLID;
			case "inset" -> BORDER_INSET;
			case "outset" -> BORDER_OUTSET;
			case "groove" -> BORDER_GROOVE;
			case "ridge" -> BORDER_RIDGE;
			case "double" -> BORDER_DOUBLE;
			default -> -1;
		};
	}

	/* A width, style and colour, in any order. */
	private static void border(int[] delta, String value) {
		for (String part : value.trim().split("\\s+")) {
			if (px(part) >= 0) delta[BORDER_WIDTH] = px(part);
			else if (part.equals("none") || part.equals("hidden")) delta[BORDER_WIDTH] = 0;
			else if (borderStyle(part) >= 0) delta[BORDER_STYLE] = borderStyle(part);
			else if (color(part) != 0) delta[BORDER_COLOR] = color(part);
		}
	}

	private static boolean contains(String[] values, String value) {
		for (String v : values) if (v.equals(value)) return true;
		return false;
	}

	/* declarations as a delta: 0 colour/background = not set, BITS = bits set | (bits cleared << 8), -1 = not set */
	private int[] parse(String block, int[] delta) {
		for (String declaration : block.split(";")) {
			checkpoint.run();
			int colon = declaration.indexOf(':');
			if (colon < 0) continue;
			String name = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
			String value = declaration.substring(colon + 1).trim().toLowerCase(Locale.ROOT).replace("!important", "").trim();
			switch (name) {
				case "color" -> {
					int parsed = color(value);
					if (parsed != 0) delta[COLOR] = parsed;
				}
				case "background-color", "background" -> {
					int parsed = color(value);
					if (parsed != 0) delta[BACKGROUND] = parsed;
				}
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
				case "border-color" -> {
					int parsed = color(value);
					if (parsed != 0) delta[BORDER_COLOR] = parsed;
				}
				case "border-style" -> {
					if (value.equals("none") || value.equals("hidden")) delta[BORDER_WIDTH] = 0;
					else if (borderStyle(value) >= 0) delta[BORDER_STYLE] = borderStyle(value);
				}
				case "position" -> {
					int position = switch (value) {
						case "static" -> STATIC;
						case "relative" -> RELATIVE;
						case "absolute" -> ABSOLUTE;
						case "fixed" -> FIXED;
						case "sticky", "-webkit-sticky" -> STICKY;
						default -> -1;
					};
					if (position >= 0) set(delta, POSITION, position);
				}
				case "left" -> length(delta, OFFSET_LEFT, value, false);
				case "top" -> length(delta, OFFSET_TOP, value, false);
				case "right" -> length(delta, OFFSET_RIGHT, value, false);
				case "bottom" -> length(delta, OFFSET_BOTTOM, value, false);
				case "width" -> {
					length(delta, WIDTH, value, true);
					if (px(value) >= 0) delta[FLEX_BASIS] = px(value);
					else if (value.equals("auto")) delta[FLEX_BASIS] = -1;
				}
				case "height" -> length(delta, HEIGHT, value, true);
				case "overflow" -> {
					if (value.equals("visible")) delta[OVERFLOW] = OVERFLOW_VISIBLE;
					else if (value.equals("hidden")) delta[OVERFLOW] = OVERFLOW_HIDDEN;
					else if (value.equals("clip")) delta[OVERFLOW] = OVERFLOW_CLIP;
					else if (value.equals("auto") || value.equals("overlay")) delta[OVERFLOW] = OVERFLOW_AUTO;
					else if (value.equals("scroll")) delta[OVERFLOW] = OVERFLOW_SCROLL;
				}
				case "display" -> delta[DISPLAY] = switch (value) {
					case "none" -> DISPLAY_NONE;
					case "flex", "inline-flex" -> DISPLAY_FLEX;
					case "grid", "inline-grid" -> DISPLAY_GRID;
					default -> DISPLAY_DEFAULT;
				};
				case "flex-grow" -> {
					if (value.matches("\\d{1,4}")) delta[FLEX_GROW] = Integer.parseInt(value);
				}
				case "flex-shrink" -> {
					double factor = shrinkNumber(value);
					if (factor >= 0) shrinkDeclaration(delta, factor);
				}
				case "flex-basis" -> {
					if (px(value) >= 0) delta[FLEX_BASIS] = px(value);
					else if (value.equals("auto")) delta[FLEX_BASIS] = -1;
				}
				case "flex" -> flex(delta, value);
				case "justify-content" -> delta[JUSTIFY] = switch (value) {
					case "center" -> FlexRow.CENTER;
					case "flex-end", "end", "right" -> FlexRow.END;
					case "space-between" -> FlexRow.SPACE_BETWEEN;
					default -> FlexRow.START;
				};
				case "gap" -> {
					String[] parts = value.split("\\s+");
					int row = px(parts[0]), column = parts.length > 1 ? px(parts[1]) : row;
					if (parts.length <= 2 && row >= 0 && column >= 0) {
						delta[ROW_GAP] = row;
						delta[GAP] = column;
					}
				}
				case "column-gap" -> {
					if (px(value) >= 0) delta[GAP] = px(value);
				}
				case "row-gap" -> {
					if (px(value) >= 0) delta[ROW_GAP] = px(value);
				}
				case "grid-template-columns" -> {
					int[] tracks = GridTracks.parse(value);
					if (tracks != null) delta[TRACKS] = trackRef(tracks);
					else if (value.equals("none")) delta[TRACKS] = 0;
				}
				case "grid-column", "grid-column-end" -> {
					java.util.regex.Matcher span = SPAN.matcher(value);
					if (span.find()) delta[COLUMN_SPAN] = Math.max(1, Integer.parseInt(span.group(1)));
					else if (value.replace(" ", "").equals("1/-1")) delta[COLUMN_SPAN] = GridTracks.MAX_TRACKS;
				}
				case "z-index" -> {
					if (value.equals("auto")) {
						set(delta, Z_INDEX, 0);
						set(delta, Z_AUTO, 1);
					} else if (value.matches("[+-]?[0-9]+")) {
						try {
							set(delta, Z_INDEX, Integer.parseInt(value));
							set(delta, Z_AUTO, 0);
						} catch (NumberFormatException ignored) {
							// Invalid declarations leave an earlier valid declaration in force.
						}
					}
				}
				default -> {
				}
			}
		}
		return delta;
	}

	private static void set(int[] delta, int slot, int value) {
		delta[slot] = value;
		delta[PRESENT] |= 1 << slot;
	}

	private static void length(int[] delta, int slot, String value, boolean nonnegative) {
		if (value.equals("auto")) {
			set(delta, slot, AUTO);
			return;
		}
		boolean pixels = value.endsWith("px");
		String number = pixels ? value.substring(0, value.length() - 2) : value;
		if (!number.matches("[+-]?[0-9]+")) return;
		try {
			int length = Integer.parseInt(number);
			boolean unitless = slot == POS_TOP || slot == POS_LEFT ? number.matches("-?[0-9]{1,5}") : length == 0;
			if (length != AUTO && (pixels || unitless) && (!nonnegative || length >= 0)) set(delta, slot, length);
		} catch (NumberFormatException ignored) {
			// Overflow is an unsupported length, not auto and not a replacement for an earlier value.
		}
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
		for (int slot = POSITION; slot <= Z_AUTO; slot++) {
			if ((delta[PRESENT] & (1 << slot)) != 0) style[slot] = delta[slot];
		}
		if (delta[SHRINK_PRESENT] != 0) putShrink(style, shrink(delta));
		if (delta[OVERFLOW] >= 0) style[OVERFLOW] = delta[OVERFLOW];
		if (delta[DISPLAY] >= 0) style[DISPLAY] = delta[DISPLAY];
		if (delta[FLEX_GROW] >= 0) style[FLEX_GROW] = delta[FLEX_GROW];
		if (delta[FLEX_BASIS] != NOT_SET) style[FLEX_BASIS] = delta[FLEX_BASIS];
		if (delta[JUSTIFY] >= 0) style[JUSTIFY] = delta[JUSTIFY];
		if (delta[GAP] >= 0) style[GAP] = delta[GAP];
		if (delta[ROW_GAP] >= 0) style[ROW_GAP] = delta[ROW_GAP];
		if (delta[TRACKS] >= 0) style[TRACKS] = delta[TRACKS];
		if (delta[COLUMN_SPAN] >= 0) style[COLUMN_SPAN] = delta[COLUMN_SPAN];
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
