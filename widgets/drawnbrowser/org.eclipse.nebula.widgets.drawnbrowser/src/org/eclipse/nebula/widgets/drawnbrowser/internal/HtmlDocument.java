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
 * <p>
 * A table is its cells: one {@link #CELL} block per td or th, with its table, row and column in three lanes; the cells
 * of a row are consecutive blocks. Inside a cell, block elements become line breaks (the cell stays one block), images
 * are not shown, and a nested table's cells and rows become spaces and line breaks. A caption is a centred paragraph;
 * text in a table outside any cell gets a cell of its own. Empty cells are kept: they hold their column.
 * </p>
 */
public final class HtmlDocument {

	/**
	 * Block kinds: pre keeps its white space and does not wrap; a rule (hr) has no text; a cell is a td (level 0) or a
	 * th (level 1) of a table; a control is a form control (input, textarea, select, button), drawn as a native widget.
	 */
	public static final int PARAGRAPH = 0, HEADING = 1, LIST_ITEM = 2, IMAGE = 3, PRE = 4, RULE = 5, CELL = 6, CONTROL = 7;

	/** Form control types ({@link #controlType}). A hidden input has no block: it is only submitted. */
	public static final int TEXT = 0, PASSWORD = 1, CHECKBOX = 2, RADIO = 3, SUBMIT = 4, BUTTON = 5, TEXTAREA = 6,
			SELECT = 7, HIDDEN = 8;

	/** Style bits of a run; code is monospace (code, tt, kbd, samp, and all of pre). */
	public static final int BOLD = 1, ITALIC = 2, UNDERLINE = 4, STRIKE = 8, LINK = 16, CODE = 32;

	/** The indent, px, of each blockquote a block is in. */
	public static final int QUOTE_INDENT = 32;

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
	/**
	 * Per block, the CSS box of its own element (0 when not set; table cells have none): top, right and bottom
	 * margins, the four paddings, the border width (px) and the border's ARGB colour (0: the widget's foreground).
	 * The left margin is {@link #blockIndent}.
	 */
	public int[] blockMarginTop = new int[16], blockMarginRight = new int[16], blockMarginBottom = new int[16],
			blockPadTop = new int[16], blockPadRight = new int[16], blockPadBottom = new int[16], blockPadLeft = new int[16],
			blockBorder = new int[16], blockBorderColor = new int[16];
	/**
	 * Per block, the position of its own element: {@link HtmlStyles#STATIC} (also for table cells), relative,
	 * absolute or fixed; its top and left offsets in px ({@link HtmlStyles#AUTO} when not given) and its z-index.
	 */
	public int[] blockPosition = new int[16], blockTop = new int[16], blockLeft = new int[16], blockZ = new int[16];
	/** Per block: its table, row (numbered across the page) and column if it is a {@link #CELL}, else -1. */
	public int[] cellTable = new int[16], cellRow = new int[16], cellColumn = new int[16];
	/** Per block: the number of columns a cell spans (its colspan, at least 1); 1 for any other block. */
	public int[] cellSpan = new int[16];

	public int tableCount;
	/** Per table: number of columns (its widest row), and the border attribute in px (0: none). */
	public int[] tableColumns = new int[4], tableBorder = new int[4];

	public int runTotal;
	/** Per run: start and end in {@link #text}, style bits, link index or -1. */
	public int[] runStart = new int[32], runEnd = new int[32], runStyle = new int[32], runLink = new int[32];
	/** Per run, from CSS: text colour (ARGB, 0 the widget's). */
	public int[] runColor = new int[32];

	public int linkCount;
	public String[] href = new String[8];

	/** Per block: its form control if it is a {@link #CONTROL}, else -1. */
	public int[] blockControl = new int[16];

	public int controlCount;
	/**
	 * Per form control: type, name (null when unnamed), value (the text of a text field or text area, the value an
	 * checked box, radio or button submits, a button's label), checked (check box and radio: 1 when checked; select:
	 * the selected option), options (select), rows and columns (text area; a text field's size in columns; 0 when
	 * not given), its form (-1 outside any) and, for a button, its label.
	 */
	public int[] controlType = new int[4], controlChecked = new int[4], controlRows = new int[4], controlCols = new int[4],
			controlForm = new int[4];
	public String[] controlName = new String[4], controlValue = new String[4], controlLabel = new String[4];
	public String[][] controlOptions = new String[4][];

	public int formCount;
	/** Per form: its action (null: the page's own location). */
	public String[] formAction = new String[2];

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
		builder.closeCell();
		builder.doc.text = builder.chars.toString();
		return builder.doc;
	}

	/** The text of block {@code b}. */
	public String blockText(int b) {
		return text.substring(textStart[b], textEnd[b]);
	}

	/** Whether blocks {@code a} and {@code b} are cells of the same table row. */
	public boolean sameRow(int a, int b) {
		return kind[a] == CELL && kind[b] == CELL && cellRow[a] == cellRow[b];
	}

	private static final class Builder implements HtmlTokenizer.Handler {
		final HtmlDocument doc = new HtmlDocument();
		final StringBuilder chars = new StringBuilder();
		int open = -1;                 // the block being filled, or -1
		int style, link = -1;          // current inline style bits and link
		int bold, italic, underline, strike, code;
		int skip;                      // inside script
		int pre;                       // inside pre: white space is kept
		boolean preStart;              // a newline right after <pre> is not content
		int quote;                     // blockquote depth
		int tables;                    // table depth: only the outermost table's td, th and tr make cells and rows
		int table = -1, row = -1, rows, column;
		boolean rowOpen;               // a row of the outermost table has started and not ended
		boolean cellOpen;              // the open block is a cell: block elements inside it are line breaks
		int form = -1;                 // the open form, or -1
		int collecting = -1;           // the control whose text is being read (text area, option, button), or -1
		final StringBuilder collected = new StringBuilder();
		int selectFor = -1, selected;  // the open select, and its selected option
		final java.util.List<String> options = new java.util.ArrayList<>();
		/* CSS: the sheet, the text of a style element being read, and one computed style per open element */
		final HtmlStyles styles = new HtmlStyles();
		final StringBuilder css = new StringBuilder();
		int cssDepth;
		int[][] frames = { HtmlStyles.root() };
		String[] frameTags = { "" };
		/* per open element: its attributes, and its place among its parent's element children */
		String[][] frameAttributes = { new String[0] };
		int[] frameIndex = { 0 };
		/* per level: the tags and attributes of the element children so far of the element open at that level */
		java.util.List<java.util.List<String>> kidTags = new java.util.ArrayList<>();
		java.util.List<java.util.List<String[]>> kidAttributes = new java.util.ArrayList<>();
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
			if (!selfClosing && !name.equals("br") && !name.equals("img") && !name.equals("hr") && !name.equals("script")
					&& !name.equals("input")) {
				push(name, attributes, styles.compute(frames[depth], path(name, attributes), HtmlTokenizer.attribute(attributes, "style")));
			} else {
				sibling(name, attributes); // a void element is a sibling of the elements after it
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
				case "table" -> {
					if (tables++ == 0) startTable(attributes);
					else lineBreak('\n');
				}
				case "tr" -> {
					if (tables == 1) startRow();
					else if (tables > 1) lineBreak('\n');
				}
				case "td", "th" -> {
					if (tables == 1) openCell(name.equals("th") ? 1 : 0, Math.max(1, Math.min(1000, number(HtmlTokenizer.attribute(attributes, "colspan")))));
					else if (tables > 1) lineBreak(' ');
				}
				case "caption" -> {
					if (tables == 1) {
						closeCell();
						openBlock(PARAGRAPH, 0, 0);
						doc.blockAlign[open] = HtmlStyles.CENTER;
					}
				}
				case "br" -> {
					ensureOpen();
					trimTrailingSpace();
					chars.append('\n');
					space = false;
				}
				case "img" -> image(attributes);
				case "form" -> form = addForm(HtmlTokenizer.attribute(attributes, "action"));
				case "input" -> input(attributes);
				case "textarea" -> {
					int c = controlBlock(TEXTAREA, attributes);
					if (c >= 0) {
						doc.controlRows[c] = number(HtmlTokenizer.attribute(attributes, "rows"));
						doc.controlCols[c] = number(HtmlTokenizer.attribute(attributes, "cols"));
						collect(c);
					}
				}
				case "select" -> {
					selectFor = controlBlock(SELECT, attributes);
					options.clear();
					selected = 0;
				}
				case "option" -> {
					if (selectFor >= 0) {
						endOption();
						if (HtmlTokenizer.attribute(attributes, "selected") != null) selected = options.size();
						collect(selectFor);
					}
				}
				case "button" -> {
					String type = HtmlTokenizer.attribute(attributes, "type");
					int c = controlBlock("button".equalsIgnoreCase(type) ? BUTTON : SUBMIT, attributes);
					if (c >= 0) collect(c);
				}
				case "pre" -> {
					openBlock(PRE, 0, 0);
					pre++;
					preStart = true;
				}
				case "blockquote" -> {
					closeBlock();
					quote++;
				}
				case "hr" -> {
					openBlock(RULE, 0, 0);
					closeBlock();
				}
				case "code", "tt", "kbd", "samp" -> style(CODE, ++code);
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
				case "form" -> form = -1;
				case "textarea" -> {
					if (collecting >= 0 && doc.controlType[collecting] == TEXTAREA) {
						String value = collected.toString().replace("\r\n", "\n").replace('\r', '\n');
						doc.controlValue[collecting] = value.startsWith("\n") ? value.substring(1) : value;
						collecting = -1;
					}
				}
				case "option" -> endOption();
				case "select" -> {
					endOption();
					if (selectFor >= 0) {
						doc.controlOptions[selectFor] = options.toArray(new String[0]);
						doc.controlChecked[selectFor] = Math.min(selected, Math.max(0, options.size() - 1));
					}
					selectFor = -1;
				}
				case "button" -> {
					if (collecting >= 0 && doc.controlType[collecting] != TEXTAREA && doc.controlType[collecting] != SELECT) {
						doc.controlLabel[collecting] = collected.toString().trim().replaceAll("\\s+", " ");
						collecting = -1;
					}
				}
				case "td", "th" -> {
					if (tables == 1) closeCell();
				}
				case "tr" -> {
					if (tables == 1) {
						closeCell();
						rowOpen = false;
					}
				}
				case "table" -> {
					if (tables == 1) {
						closeCell();
						rowOpen = false;
					}
					tables = Math.max(0, tables - 1);
				}
				case "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "caption" -> closeBlock();
				case "ul", "ol" -> {
					closeBlock();
					listDepth = Math.max(0, listDepth - 1);
				}
				case "pre" -> {
					closeBlock();
					pre = Math.max(0, pre - 1);
				}
				case "blockquote" -> {
					closeBlock();
					quote = Math.max(0, quote - 1);
				}
				case "code", "tt", "kbd", "samp" -> style(CODE, code = Math.max(0, code - 1));
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
			if (collecting >= 0) {
				collected.append(text); // a text area's value, an option's or a button's label
				return;
			}
			if (pre > 0) {
				preformatted(text);
				return;
			}
			for (int i = 0, n = text.length(); i < n; i++) {
				char c = text.charAt(i);
				if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
					space = true;
					continue;
				}
				ensureOpen();
				if (space && chars.length() > doc.textStart[open] && chars.charAt(chars.length() - 1) != '\n') appendRun(' ');
				space = false;
				appendRun(c);
			}
		}

		/* text inside pre: every character kept, line ends as \n, except a newline right after <pre> */
		void preformatted(String text) {
			for (int i = 0, n = text.length(); i < n; i++) {
				char c = text.charAt(i);
				if (c == '\r') {
					if (i + 1 < n && text.charAt(i + 1) == '\n') continue;
					c = '\n';
				}
				if (preStart) {
					preStart = false;
					if (c == '\n') continue;
				}
				if (open < 0) openBlock(PRE, 0, 0);
				appendRun(c);
			}
		}

		/* text needs a block: in a table outside any cell it gets a cell of its own, else a paragraph */
		void ensureOpen() {
			if (open >= 0) return;
			if (tables > 0) openCell(0, 1);
			else openBlock(PARAGRAPH, 0, 0);
		}

		void startTable(String[] attributes) {
			closeBlock();
			HtmlDocument d = doc;
			if (d.tableCount == d.tableColumns.length) {
				d.tableColumns = Arrays.copyOf(d.tableColumns, 2 * d.tableCount);
				d.tableBorder = Arrays.copyOf(d.tableBorder, 2 * d.tableCount);
			}
			table = d.tableCount++;
			String border = HtmlTokenizer.attribute(attributes, "border");
			d.tableBorder[table] = border == null ? 0 : border.isBlank() ? 1 : Math.max(0, size(border));
			rowOpen = false;
		}

		void startRow() {
			closeCell();
			row = rows++;
			column = 0;
			rowOpen = true;
		}

		/* a td or th of the outermost table: one block of span columns, placed in the current row (a row starts if none has) */
		void openCell(int header, int span) {
			if (!rowOpen) startRow();
			else closeCell();
			openBlock(CELL, header, 0);
			HtmlDocument d = doc;
			d.cellTable[open] = table;
			d.cellRow[open] = row;
			d.cellColumn[open] = column;
			d.cellSpan[open] = span;
			column += span;
			d.tableColumns[table] = Math.max(d.tableColumns[table], column);
			d.blockIndent[open] = quote * QUOTE_INDENT;
			if (header == 1 && d.blockAlign[open] == HtmlStyles.LEFT) d.blockAlign[open] = HtmlStyles.CENTER;
			cellOpen = true;
		}

		/* ends the open cell, or the open block when it is not a cell */
		void closeCell() {
			cellOpen = false;
			closeBlock();
		}

		/* inside a cell a block element, or a nested table's row or cell, separates its text and opens no block */
		void lineBreak(char separator) {
			if (!cellOpen) return;
			trimTrailingSpace();
			int end = chars.length();
			if (end > doc.textStart[open] && chars.charAt(end - 1) != '\n') chars.append(separator);
			space = false;
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
			int effective = (link >= 0 ? style | LINK : style) | frame[HtmlStyles.BITS] | (pre > 0 ? CODE : 0), color = frame[HtmlStyles.COLOR];
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
			if (cellOpen) {
				lineBreak('\n');
				return;
			}
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
				d.cellTable = Arrays.copyOf(d.cellTable, capacity);
				d.cellRow = Arrays.copyOf(d.cellRow, capacity);
				d.cellColumn = Arrays.copyOf(d.cellColumn, capacity);
				d.blockControl = Arrays.copyOf(d.blockControl, capacity);
				d.cellSpan = Arrays.copyOf(d.cellSpan, capacity);
				d.blockMarginTop = Arrays.copyOf(d.blockMarginTop, capacity);
				d.blockMarginRight = Arrays.copyOf(d.blockMarginRight, capacity);
				d.blockMarginBottom = Arrays.copyOf(d.blockMarginBottom, capacity);
				d.blockPadTop = Arrays.copyOf(d.blockPadTop, capacity);
				d.blockPadRight = Arrays.copyOf(d.blockPadRight, capacity);
				d.blockPadBottom = Arrays.copyOf(d.blockPadBottom, capacity);
				d.blockPadLeft = Arrays.copyOf(d.blockPadLeft, capacity);
				d.blockBorder = Arrays.copyOf(d.blockBorder, capacity);
				d.blockBorderColor = Arrays.copyOf(d.blockBorderColor, capacity);
				d.blockPosition = Arrays.copyOf(d.blockPosition, capacity);
				d.blockTop = Arrays.copyOf(d.blockTop, capacity);
				d.blockLeft = Arrays.copyOf(d.blockLeft, capacity);
				d.blockZ = Arrays.copyOf(d.blockZ, capacity);
			}
			open = d.blockCount++;
			d.cellTable[open] = d.cellRow[open] = d.cellColumn[open] = -1;
			d.blockControl[open] = -1;
			d.cellSpan[open] = 1;
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
			d.blockIndent[open] = (own ? frame[HtmlStyles.INDENT] : 0) + quote * QUOTE_INDENT;
			d.blockAlign[open] = frame[HtmlStyles.ALIGN];
			boolean box = own && kind != CELL;
			d.blockMarginTop[open] = box ? frame[HtmlStyles.MARGIN_TOP] : 0;
			d.blockMarginRight[open] = box ? frame[HtmlStyles.MARGIN_RIGHT] : 0;
			d.blockMarginBottom[open] = box ? frame[HtmlStyles.MARGIN_BOTTOM] : 0;
			d.blockPadTop[open] = box ? frame[HtmlStyles.PADDING_TOP] : 0;
			d.blockPadRight[open] = box ? frame[HtmlStyles.PADDING_RIGHT] : 0;
			d.blockPadBottom[open] = box ? frame[HtmlStyles.PADDING_BOTTOM] : 0;
			d.blockPadLeft[open] = box ? frame[HtmlStyles.PADDING_LEFT] : 0;
			d.blockBorder[open] = box ? frame[HtmlStyles.BORDER_WIDTH] : 0;
			d.blockBorderColor[open] = box ? frame[HtmlStyles.BORDER_COLOR] : 0;
			d.blockPosition[open] = box ? frame[HtmlStyles.POSITION] : HtmlStyles.STATIC;
			d.blockTop[open] = box ? frame[HtmlStyles.POS_TOP] : HtmlStyles.AUTO;
			d.blockLeft[open] = box ? frame[HtmlStyles.POS_LEFT] : HtmlStyles.AUTO;
			d.blockZ[open] = box ? frame[HtmlStyles.Z_INDEX] : 0;
			space = false;
		}

		static boolean blockTag(String tag) {
			return switch (tag) {
				case "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "pre", "td", "th", "caption" -> true;
				default -> false;
			};
		}

		void push(String tag, String[] attributes, int[] frame) {
			int index = kids(depth).size();
			sibling(tag, attributes);
			if (++depth == frames.length) {
				frames = Arrays.copyOf(frames, 2 * frames.length);
				frameTags = Arrays.copyOf(frameTags, 2 * frameTags.length);
				frameAttributes = Arrays.copyOf(frameAttributes, 2 * frameAttributes.length);
				frameIndex = Arrays.copyOf(frameIndex, 2 * frameIndex.length);
			}
			frames[depth] = frame;
			frameTags[depth] = tag;
			frameAttributes[depth] = attributes;
			frameIndex[depth] = index;
			kids(depth).clear(); // the new element has no children yet
			kidAttributes.get(depth).clear();
		}

		/* the element children so far of the element open at a level */
		java.util.List<String> kids(int level) {
			while (kidTags.size() <= level) {
				kidTags.add(new java.util.ArrayList<>());
				kidAttributes.add(new java.util.ArrayList<>());
			}
			return kidTags.get(level);
		}

		void sibling(String tag, String[] attributes) {
			kids(depth).add(tag);
			kidAttributes.get(depth).add(attributes);
		}

		/* the element being opened (level 0), its ancestors and their previous element siblings, for the cascade */
		HtmlStyles.Path path(String name, String[] attributes) {
			int open = depth, before = kids(depth).size();
			return new HtmlStyles.Path() {
				@Override
				public int depth() {
					return open + 1;
				}

				@Override
				public int siblings(int up) {
					return up == 0 ? before : frameIndex[open - up + 1];
				}

				@Override
				public String tag(int up, int back) {
					if (back == 0) return up == 0 ? name : frameTags[open - up + 1];
					return kidTags.get(open - up).get(siblings(up) - back);
				}

				@Override
				public String attribute(int up, int back, String key) {
					String[] attrs = back > 0 ? kidAttributes.get(open - up).get(siblings(up) - back) : up == 0 ? attributes : frameAttributes[open - up + 1];
					return HtmlTokenizer.attribute(attrs, key);
				}
			};
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
			if (open < 0 || cellOpen) return;
			if (doc.kind[open] == PRE) trimLastNewline();
			else trimTrailingSpace();
			HtmlDocument d = doc;
			d.textEnd[open] = chars.length();
			int k = d.kind[open];
			boolean empty = d.textEnd[open] == d.textStart[open] && d.image[open] < 0 && k != LIST_ITEM && k != RULE && k != CELL && k != CONTROL;
			if (empty) d.blockCount--;
			open = -1;
			space = false;
		}

		void trimTrailingSpace() {
			trimLast(' ');
		}

		/* the line end before </pre> ends the last line; it does not start an empty one */
		void trimLastNewline() {
			trimLast('\n');
		}

		void trimLast(char c) {
			int end = chars.length();
			if (open < 0 || end == doc.textStart[open] || chars.charAt(end - 1) != c) return;
			chars.setLength(end - 1);
			int last = doc.runTotal - 1;
			if (doc.runEnd[last] == end && --doc.runEnd[last] == doc.runStart[last]) {
				doc.runTotal--;
				doc.runCount[open]--;
			}
		}

		int addForm(String action) {
			HtmlDocument d = doc;
			if (d.formCount == d.formAction.length) d.formAction = Arrays.copyOf(d.formAction, 2 * d.formCount);
			d.formAction[d.formCount] = action;
			return d.formCount++;
		}

		/* a form control; it has a block of its own unless it is hidden (or in a table cell, where it is not shown yet) */
		int addControl(int type, String[] attributes) {
			HtmlDocument d = doc;
			if (d.controlCount == d.controlType.length) {
				int capacity = 2 * d.controlCount;
				d.controlType = Arrays.copyOf(d.controlType, capacity);
				d.controlChecked = Arrays.copyOf(d.controlChecked, capacity);
				d.controlRows = Arrays.copyOf(d.controlRows, capacity);
				d.controlCols = Arrays.copyOf(d.controlCols, capacity);
				d.controlForm = Arrays.copyOf(d.controlForm, capacity);
				d.controlName = Arrays.copyOf(d.controlName, capacity);
				d.controlValue = Arrays.copyOf(d.controlValue, capacity);
				d.controlLabel = Arrays.copyOf(d.controlLabel, capacity);
				d.controlOptions = Arrays.copyOf(d.controlOptions, capacity);
			}
			int c = d.controlCount++;
			d.controlType[c] = type;
			d.controlName[c] = HtmlTokenizer.attribute(attributes, "name");
			String value = HtmlTokenizer.attribute(attributes, "value");
			d.controlValue[c] = value != null ? value : type == CHECKBOX || type == RADIO ? "on" : "";
			d.controlLabel[c] = value != null ? value : type == SUBMIT ? "Submit" : "";
			d.controlChecked[c] = HtmlTokenizer.attribute(attributes, "checked") != null ? 1 : 0;
			d.controlForm[c] = form;
			d.controlOptions[c] = new String[0];
			return c;
		}

		/* a control with a block: -1 inside a table cell (not shown there yet) */
		int controlBlock(int type, String[] attributes) {
			if (cellOpen) return -1;
			int c = addControl(type, attributes);
			openBlock(CONTROL, 0, 0);
			doc.blockControl[open] = c;
			closeBlock();
			return c;
		}

		void input(String[] attributes) {
			String type = HtmlTokenizer.attribute(attributes, "type");
			int kind = switch (type == null ? "text" : type.toLowerCase(java.util.Locale.ROOT)) {
				case "password" -> PASSWORD;
				case "checkbox" -> CHECKBOX;
				case "radio" -> RADIO;
				case "submit" -> SUBMIT;
				case "button", "reset" -> BUTTON;
				case "hidden" -> HIDDEN;
				default -> TEXT;
			};
			if (kind == HIDDEN) {
				addControl(HIDDEN, attributes);
				return;
			}
			int c = controlBlock(kind, attributes);
			if (c >= 0) doc.controlCols[c] = number(HtmlTokenizer.attribute(attributes, "size"));
		}

		void collect(int control) {
			collecting = control;
			collected.setLength(0);
		}

		/* an option of the open select ends: its text, collapsed */
		void endOption() {
			if (selectFor < 0 || collecting != selectFor) return;
			options.add(collected.toString().trim().replaceAll("\\s+", " "));
			collecting = -1;
		}

		int number(String value) {
			if (value == null) return 0;
			try {
				return Math.max(0, Integer.parseInt(value.trim()));
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		void image(String[] attributes) {
			String src = HtmlTokenizer.attribute(attributes, "src");
			if (src == null || cellOpen) return; // a cell is one text block: images in cells are not shown yet
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
