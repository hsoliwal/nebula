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
	/** Box roles retain the existing automatic table/row/cell layout within positioned containers. */
	public static final int NORMAL_BOX = 0, TABLE_BOX = 1, ROW_BOX = 2, CELL_BOX = 3, FLEX_BOX = 4, GRID_BOX = 5;

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
			blockBorder = new int[16], blockBorderColor = new int[16], blockBorderStyle = new int[16];
	/**
	 * Per block, the position of its own element: {@link HtmlStyles#STATIC} (also for table cells, unless sticky),
	 * relative, absolute, fixed or sticky; its top and left offsets in px ({@link HtmlStyles#AUTO} when not given) and
	 * its z-index.
	 */
	public int[] blockPosition = new int[16], blockTop = new int[16], blockLeft = new int[16], blockZ = new int[16];
	/**
	 * Per block: for a sticky box, the last block of the element that contains it (a table cell's: its table), whose
	 * end the box is not carried past; -1 for any other block.
	 */
	public int[] blockStickyEnd = new int[16];
	/** Per block: its table, row (numbered across the page) and column if it is a {@link #CELL}, else -1. */
	public int[] cellTable = new int[16], cellRow = new int[16], cellColumn = new int[16];
	/** Per block: the number of columns a cell spans (its colspan, at least 1); 1 for any other block. */
	public int[] cellSpan = new int[16];
	/** Per block: the number of rows a cell spans (its rowspan, at least 1); 1 for any other block. */
	public int[] cellRowSpan = new int[16];
	/** The nearest owning block box of each flat block. */
	public int[] blockBox = new int[16];

	/**
	 * Block-box tree in document preorder; box 0 is the document root. Children and siblings are linked by indices,
	 * with -1 for none. A box's [firstBlock,endBlock) includes descendant fragments; an empty box remains in the tree.
	 * Inline elements and elements flattened inside a table cell do not introduce boxes.
	 */
	public int boxCount = 1;
	public int[] boxParent = new int[16], boxFirstChild = new int[16], boxNextSibling = new int[16],
			boxFirstBlock = new int[16], boxEndBlock = new int[16], boxRole = new int[16], boxPosition = new int[16],
			boxLeft = new int[16], boxTop = new int[16], boxRight = new int[16], boxBottom = new int[16],
			boxWidth = new int[16], boxHeight = new int[16], boxZIndex = new int[16], boxBackground = new int[16],
			boxOverflow = new int[16], boxIndent = new int[16];
	/** Z auto is distinct from explicit zero and from every signed integer z-index. */
	public boolean[] boxZAuto = new boolean[16];
	/** Per element box, the same non-inherited box model as the flat block lanes; table cells keep their table padding. */
	public int[] boxMarginTop = new int[16], boxMarginRight = new int[16], boxMarginBottom = new int[16],
			boxPadTop = new int[16], boxPadRight = new int[16], boxPadBottom = new int[16], boxPadLeft = new int[16],
			boxBorder = new int[16], boxBorderColor = new int[16], boxBorderStyle = new int[16];
	/** Whether a supported box requests non-static positioning, explicit sizing or a clipping boundary. */
	public boolean positioned;
	/**
	 * Whether sizes, right/bottom offsets, empty boxes or ancestry require the resolved box scene. Flat nonempty
	 * positioned blocks using only top/left/z-index retain the existing flat layout and painting path.
	 */
	public boolean boxLayout;

	public int tableCount;
	/** Per table: number of columns (its widest row), and the border attribute in px (0: none). */
	public int[] tableColumns = new int[4], tableBorder = new int[4];
	/**
	 * Per table: what made it ({@link HtmlStyles#DISPLAY_DEFAULT} a table element; {@link HtmlStyles#DISPLAY_FLEX} a row
	 * flex container, its items the cells of its one row; {@link HtmlStyles#DISPLAY_GRID} a grid container, its items
	 * placed row by row in its columns; flex and grid items keep their CSS box), its justify-content ({@link FlexRow}),
	 * its column gap and its row gap px.
	 */
	public int[] tableDisplay = new int[4], tableJustify = new int[4], tableGap = new int[4], tableRowGap = new int[4];
	/** Per table: a grid's columns, {@code tableTrackCount} tracks ({@link GridTracks}) from {@code tableTrackStart} in {@link #tracks}. */
	public int[] tableTrackStart = new int[4], tableTrackCount = new int[4];
	public int[] tracks = new int[8];
	public int trackTotal;
	/** Per block: a flex item's flex-grow, and its flex basis (or width) in px, -1 for none. */
	public int[] cellGrow = new int[16], cellBasis = new int[16];
	/** Per flex item, its finite nonnegative flex-shrink factor; anonymous items use the initial value 1. */
	public double[] cellShrink = new double[16];

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
		boxParent[0] = boxFirstChild[0] = boxNextSibling[0] = -1;
		boxLeft[0] = boxTop[0] = boxRight[0] = boxBottom[0] = boxWidth[0] = boxHeight[0] = HtmlStyles.AUTO;
		boxZAuto[0] = true;
	}

	/** Parses {@code html}; never fails. */
	public static HtmlDocument parse(String html) {
		return parseChecked(html, HtmlTokenizer.UNCHECKED);
	}

	/**
	 * Parses detached asynchronous input, or returns null when cancellation is observed. The supplier must
	 * inspect detached state only; it must not call SWT widgets. Cancellation is cooperative at parser
	 * checkpoints, not a deadline for an individual JDK operation. The ordinary {@link #parse(String)}
	 * entry point never observes this supplier or the thread's interrupt status.
	 */
	public static HtmlDocument parse(String html, java.util.function.BooleanSupplier cancelled) {
		java.util.Objects.requireNonNull(cancelled);
		try {
			return parseChecked(html, () -> {
				if (cancelled.getAsBoolean()) throw new ParseCancelled();
			});
		} catch (ParseCancelled cancelledParse) {
			return null;
		}
	}

	private static final class ParseCancelled extends RuntimeException {
		private static final long serialVersionUID = 1L;

		ParseCancelled() {
			super(null, null, false, false);
		}
	}

	private static HtmlDocument parseChecked(String html, Runnable checkpoint) {
		checkpoint.run();
		Builder builder = new Builder(checkpoint);
		HtmlTokenizer.tokenize(html, builder, checkpoint);
		checkpoint.run();
		builder.finish();
		builder.unstick(-1); // close the remaining sticky containing elements
		checkpoint.run();
		builder.doc.text = builder.chars.toString();
		checkpoint.run();
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
		final Runnable checkpoint;
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
		int containerFrame;            // the frame of the open flex or grid container, or 0
		int tableDepth;                // the level of the open table element (or flex or grid container)
		/* sticky blocks whose containing element (open at stickyDepth) has not ended yet, innermost last */
		int[] stickyOpen = new int[4], stickyDepth = new int[4];
		int stickyCount;
		boolean[] frameHidden = new boolean[1]; // per open element: display none (it counts in skip)
		int[] spanRows = new int[8];   // per column: rows below the current one still covered by a rowspan from above
		boolean[] taken = new boolean[8]; // per column: covered in the current row by a rowspan from above
		int table = -1, row = -1, rows, column;
		boolean rowOpen;               // a row of the outermost table has started and not ended
		boolean cellOpen;              // the open block is a cell: block elements inside it are line breaks
		int form = -1;                 // the open form, or -1
		int collecting = -1;           // the control whose text is being read (text area, option, button), or -1
		final StringBuilder collected = new StringBuilder();
		int selectFor = -1, selected;  // the open select, and its selected option
		final java.util.List<String> options = new java.util.ArrayList<>();
		int anonymousContainerBox = -1;
		int implicitCellDepth = -1;    // an anonymous text cell closes its synthetic frame together with its block
		/* CSS: the sheet, the text of a style element being read, and one computed style per open element */
		final HtmlStyles styles;
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
		int[] frameBox = { 0 }, frameOwnBox = { -1 };
		int[] lastChild = new int[16];
		int depth;
		int listDepth;
		int[] ordinals = new int[8];   // per list depth: next ordinal, or BULLET for ul
		boolean space;                 // a collapsed space is pending

		Builder(Runnable checkpoint) {
			this.checkpoint = checkpoint;
			styles = new HtmlStyles(checkpoint);
			Arrays.fill(lastChild, -1);
		}

		@Override
		public void startTag(String name, String[] attributes, boolean selfClosing) {
			checkpoint.run();
			flushSpace();
			if (name.equals("style")) {
				cssDepth += selfClosing ? 0 : 1;
				return;
			}
			boolean flexItem = containerFrame > 0 && depth == containerFrame && !selfClosing;
			boolean ownBox = skip == 0 && (flexItem || boxTag(name) && boxSupported(name));
			if (ownBox) beforeBox(name);
			if (ownBox && (name.equals("td") || name.equals("th"))) ensureRowBox();
			boolean temporary = ownBox && (selfClosing || name.equals("img") || name.equals("hr"));
			if (temporary || !selfClosing && !name.equals("br") && !name.equals("img") && !name.equals("hr")
					&& !name.equals("script") && !name.equals("input")) {
				int[] computed = styles.compute(frames[depth], path(name, attributes), HtmlTokenizer.attribute(attributes, "style"));
				boolean hidden = computed[HtmlStyles.DISPLAY] == HtmlStyles.DISPLAY_NONE;
				int box = ownBox && !hidden ? addBox(frameBox[depth], name, computed) : -1;
				push(name, attributes, computed, box);
				if (hidden) { frameHidden[depth] = true; skip++; }
			} else {
				sibling(name, attributes);
			}
			if (name.equals("script")) {
				skip += selfClosing ? 0 : 1;
				return;
			}
			if (skip > 0) { if (temporary) closeFrames(depth); return; }
			if (containerFrame > 0 && depth == containerFrame + 1 && depth > 0 && frameTags[depth].equals(name)) {
				openItem(true);
			} else if (!selfClosing && tables == 0 && depth > 0 && frameTags[depth].equals(name)
					&& (frames[depth][HtmlStyles.DISPLAY] == HtmlStyles.DISPLAY_FLEX || frames[depth][HtmlStyles.DISPLAY] == HtmlStyles.DISPLAY_GRID)) {
				startContainer();
				return;
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
					if (tables == 1) openCell(name.equals("th") ? 1 : 0, Math.max(1, Math.min(1000, number(HtmlTokenizer.attribute(attributes, "colspan")))),
							Math.max(1, Math.min(65534, number(HtmlTokenizer.attribute(attributes, "rowspan")))));
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
			if (temporary) {
				closeBlock();
				closeFrames(depth);
			}
		}

		@Override
		public void endTag(String name) {
			checkpoint.run();
			endElement(name);
			unstick(depth); // the sticky boxes of the elements that ended end with their last block
		}

		private void endElement(String name) {
			flushSpace();
			if (name.equals("style")) {
				if (cssDepth > 0 && --cssDepth == 0) {
					styles.addSheet(css.toString());
					css.setLength(0);
				}
				return;
			}
			int before = depth;
			pop(name);
			if (containerFrame > 0) {
				if (depth < containerFrame) {
					endContainer(); // the container ended
					return;
				}
				if (before >= containerFrame + 1 && depth == containerFrame) closeCell(); // an item ended
			}
			if (name.equals("script")) {
				skip = Math.max(0, skip - 1);
				return;
			}
			if (skip > 0) return;
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
			checkpoint.run();
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
				if ((i & 1023) == 0) checkpoint.run();
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
				if ((i & 1023) == 0) checkpoint.run();
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
			if (containerFrame > 0) openItem(false); // loose text in a flex or grid container is an anonymous item
			else if (tables > 0) openCell(0, 1);
			else openBlock(PARAGRAPH, 0, 0);
		}

		void startTable(String[] attributes) {
			closeBlock();
			HtmlDocument d = doc;
			if (d.tableCount == d.tableColumns.length) {
				d.tableColumns = Arrays.copyOf(d.tableColumns, 2 * d.tableCount);
				d.tableBorder = Arrays.copyOf(d.tableBorder, 2 * d.tableCount);
				d.tableDisplay = Arrays.copyOf(d.tableDisplay, 2 * d.tableCount);
				d.tableRowGap = Arrays.copyOf(d.tableRowGap, 2 * d.tableCount);
				d.tableTrackStart = Arrays.copyOf(d.tableTrackStart, 2 * d.tableCount);
				d.tableTrackCount = Arrays.copyOf(d.tableTrackCount, 2 * d.tableCount);
				d.tableJustify = Arrays.copyOf(d.tableJustify, 2 * d.tableCount);
				d.tableGap = Arrays.copyOf(d.tableGap, 2 * d.tableCount);
			}
			table = d.tableCount++;
			tableDepth = depth;
			String border = HtmlTokenizer.attribute(attributes, "border");
			d.tableBorder[table] = border == null ? 0 : border.isBlank() ? 1 : Math.max(0, size(border));
			d.tableDisplay[table] = HtmlStyles.DISPLAY_DEFAULT;
			d.tableRowGap[table] = 0;
			d.tableTrackCount[table] = 0;
			rowOpen = false;
			Arrays.fill(spanRows, 0);
		}

		/* a row starts: the columns rowspans from above still cover are taken in it (HTML's table model) */
		void startRow() {
			closeCell();
			row = rows++;
			column = 0;
			rowOpen = true;
			for (int c = 0; c < spanRows.length; c++) {
				checkpoint.run();
				taken[c] = spanRows[c] > 0;
				if (taken[c]) spanRows[c]--;
			}
		}

		/* grows the per-column lanes to hold column c */
		void columns(int c) {
			if (c < spanRows.length) return;
			int capacity = Math.max(2 * spanRows.length, c + 1);
			spanRows = Arrays.copyOf(spanRows, capacity);
			taken = Arrays.copyOf(taken, capacity);
		}

		/* a td or th of the outermost table: one block of span columns, placed in the current row (a row starts if none has) */
		void openCell(int header, int span) {
			openCell(header, span, 1);
		}

		void openCell(int header, int span, int rowSpan) {
			if (!rowOpen) startRow();
			else closeCell();
			if (tables == 1 && containerFrame == 0 && doc.boxRole[frameBox[depth]] != CELL_BOX) {
				ensureRowBox();
				int[] inherited = styles.compute(frames[depth], "", null, null);
				int box = addBox(frameBox[depth], "td", inherited);
				push("td", inherited, box);
				implicitCellDepth = depth;
			}
			if (containerFrame > 0 && depth == containerFrame) {
				int[] inherited = styles.compute(frames[depth], "", null, null);
				anonymousContainerBox = addBox(frameBox[depth], "td", inherited);
			}
			openBlock(CELL, header, 0);
			if (anonymousContainerBox >= 0) doc.blockBox[open] = anonymousContainerBox;
			HtmlDocument d = doc;
			columns(column);
			while (taken[column]) columns(++column); // past the columns rowspans from above cover
			d.cellTable[open] = table;
			d.cellRow[open] = row;
			d.cellColumn[open] = column;
			d.cellSpan[open] = span;
			d.cellRowSpan[open] = rowSpan;
			columns(column + span);
			for (int c = column; c < column + span; c++) spanRows[c] = Math.max(spanRows[c], rowSpan - 1);
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
			if (anonymousContainerBox >= 0) {
				doc.boxEndBlock[anonymousContainerBox] = doc.blockCount;
				anonymousContainerBox = -1;
			}
			if (implicitCellDepth >= 0) {
				closeFrames(implicitCellDepth);
				implicitCellDepth = -1;
			}
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
				d.cellRowSpan = Arrays.copyOf(d.cellRowSpan, capacity);
				d.cellGrow = Arrays.copyOf(d.cellGrow, capacity);
				d.cellBasis = Arrays.copyOf(d.cellBasis, capacity);
				d.cellShrink = Arrays.copyOf(d.cellShrink, capacity);
				d.blockBox = Arrays.copyOf(d.blockBox, capacity);
				d.blockMarginTop = Arrays.copyOf(d.blockMarginTop, capacity);
				d.blockMarginRight = Arrays.copyOf(d.blockMarginRight, capacity);
				d.blockMarginBottom = Arrays.copyOf(d.blockMarginBottom, capacity);
				d.blockPadTop = Arrays.copyOf(d.blockPadTop, capacity);
				d.blockPadRight = Arrays.copyOf(d.blockPadRight, capacity);
				d.blockPadBottom = Arrays.copyOf(d.blockPadBottom, capacity);
				d.blockPadLeft = Arrays.copyOf(d.blockPadLeft, capacity);
				d.blockBorder = Arrays.copyOf(d.blockBorder, capacity);
				d.blockBorderColor = Arrays.copyOf(d.blockBorderColor, capacity);
				d.blockBorderStyle = Arrays.copyOf(d.blockBorderStyle, capacity);
				d.blockPosition = Arrays.copyOf(d.blockPosition, capacity);
				d.blockTop = Arrays.copyOf(d.blockTop, capacity);
				d.blockLeft = Arrays.copyOf(d.blockLeft, capacity);
				d.blockZ = Arrays.copyOf(d.blockZ, capacity);
				d.blockStickyEnd = Arrays.copyOf(d.blockStickyEnd, capacity);
			}
			open = d.blockCount++;
			d.cellTable[open] = d.cellRow[open] = d.cellColumn[open] = -1;
			d.blockControl[open] = -1;
			d.cellSpan[open] = 1;
			d.cellRowSpan[open] = 1;
			d.cellGrow[open] = 0;
			d.cellBasis[open] = -1;
			d.kind[open] = kind;
			d.level[open] = level;
			d.marker[open] = mark;
			d.textStart[open] = d.textEnd[open] = chars.length();
			d.firstRun[open] = d.runTotal;
			d.runCount[open] = 0;
			d.image[open] = -1;
			d.blockBox[open] = frameBox[depth];
			// an explicit block's own element is the top frame; an implicit paragraph only inherits the alignment
			int[] frame = frames[depth];
			boolean own = kind != IMAGE && depth > 0 && blockTag(frameTags[depth]);
			boolean ownLeaf = depth > 0 && (kind == IMAGE && frameTags[depth].equals("img") || kind == RULE && frameTags[depth].equals("hr"));
			d.blockBackground[open] = own ? frame[HtmlStyles.BACKGROUND] : 0;
			d.blockIndent[open] = (own || ownLeaf ? frame[HtmlStyles.INDENT] : 0) + quote * QUOTE_INDENT;
			d.blockAlign[open] = frame[HtmlStyles.ALIGN];
			boolean box = own && (kind != CELL || d.tableDisplay[table] != HtmlStyles.DISPLAY_DEFAULT); // a flex or grid item keeps its CSS box
			d.blockMarginTop[open] = box ? frame[HtmlStyles.MARGIN_TOP] : 0;
			d.blockMarginRight[open] = box ? frame[HtmlStyles.MARGIN_RIGHT] : 0;
			d.blockMarginBottom[open] = box ? frame[HtmlStyles.MARGIN_BOTTOM] : 0;
			d.blockPadTop[open] = box ? frame[HtmlStyles.PADDING_TOP] : 0;
			d.blockPadRight[open] = box ? frame[HtmlStyles.PADDING_RIGHT] : 0;
			d.blockPadBottom[open] = box ? frame[HtmlStyles.PADDING_BOTTOM] : 0;
			d.blockPadLeft[open] = box ? frame[HtmlStyles.PADDING_LEFT] : 0;
			d.blockBorder[open] = box ? frame[HtmlStyles.BORDER_WIDTH] : 0;
			d.blockBorderColor[open] = box ? frame[HtmlStyles.BORDER_COLOR] : 0;
			d.blockBorderStyle[open] = box ? frame[HtmlStyles.BORDER_STYLE] : HtmlStyles.BORDER_SOLID;
			d.blockPosition[open] = box ? frame[HtmlStyles.POSITION] : HtmlStyles.STATIC;
			d.blockTop[open] = box ? frame[HtmlStyles.POS_TOP] : HtmlStyles.AUTO;
			d.blockLeft[open] = box ? frame[HtmlStyles.POS_LEFT] : HtmlStyles.AUTO;
			d.blockZ[open] = box ? frame[HtmlStyles.Z_INDEX] : 0;
			// a table cell may be sticky too: a header row that stays while its table scrolls under it
			boolean stickyCell = kind == CELL && !box && own && frame[HtmlStyles.POSITION] == HtmlStyles.STICKY;
			if (stickyCell) {
				d.blockPosition[open] = HtmlStyles.STICKY;
				d.blockTop[open] = frame[HtmlStyles.POS_TOP];
				d.blockZ[open] = frame[HtmlStyles.Z_INDEX];
			}
			d.blockStickyEnd[open] = -1;
			if (d.blockPosition[open] == HtmlStyles.STICKY) stick(open, stickyCell ? tableDepth : depth - 1);
			space = false;
		}

		/* a sticky block, bounded by its containing element open at level {@code within} */
		void stick(int block, int within) {
			if (stickyCount == stickyOpen.length) {
				stickyOpen = Arrays.copyOf(stickyOpen, 2 * stickyCount);
				stickyDepth = Arrays.copyOf(stickyDepth, 2 * stickyCount);
			}
			stickyOpen[stickyCount] = block;
			stickyDepth[stickyCount++] = within;
		}

		/* the elements deeper than {@code level} have ended: the sticky blocks they contain end at the last block so far */
		void unstick(int level) {
			while (stickyCount > 0 && stickyDepth[stickyCount - 1] > level) {
				checkpoint.run();
				doc.blockStickyEnd[stickyOpen[--stickyCount]] = doc.blockCount - 1;
			}
		}

		static boolean blockTag(String tag) {
			return switch (tag) {
				case "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "pre", "td", "th", "caption" -> true;
				default -> false;
			};
		}

		void push(String tag, String[] attributes, int[] frame, int ownBox) {
			int index = kids(depth).size();
			sibling(tag, attributes);
			if (++depth == frames.length) {
				frames = Arrays.copyOf(frames, 2 * frames.length);
				frameTags = Arrays.copyOf(frameTags, 2 * frameTags.length);
				frameAttributes = Arrays.copyOf(frameAttributes, 2 * frameAttributes.length);
				frameIndex = Arrays.copyOf(frameIndex, 2 * frameIndex.length);
				frameHidden = Arrays.copyOf(frameHidden, 2 * frameHidden.length);
				frameBox = Arrays.copyOf(frameBox, 2 * frameBox.length);
				frameOwnBox = Arrays.copyOf(frameOwnBox, 2 * frameOwnBox.length);
			}
			frames[depth] = frame;
			frameTags[depth] = tag;
			frameOwnBox[depth] = ownBox;
			frameBox[depth] = ownBox >= 0 ? ownBox : frameBox[depth - 1];
			frameAttributes[depth] = attributes;
			frameIndex[depth] = index;
			kids(depth).clear(); // the new element has no children yet
			kidAttributes.get(depth).clear();
		}

		/* the element children so far of the element open at a level */
		java.util.List<String> kids(int level) {
			while (kidTags.size() <= level) {
				checkpoint.run();
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

		static boolean boxTag(String tag) {
			return switch (tag) {
				case "body", "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "pre", "blockquote", "ul", "ol",
						"table", "tr", "td", "th", "caption", "img", "hr" -> true;
				default -> false;
			};
		}

		boolean boxSupported(String tag) {
			return switch (tag) {
				case "table" -> tables == 0;
				case "tr", "td", "th", "caption" -> tables == 1;
				default -> !cellOpen;
			};
		}

		/* End the preceding fragment before recording a box range, including omitted cell/row end tags. */
		void beforeBox(String tag) {
			if (containerFrame > 0 && depth == containerFrame) {
				closeCell();
			} else if (tables == 1 && (tag.equals("tr") || tag.equals("td") || tag.equals("th") || tag.equals("caption"))) {
				closeCell();
				int from = -1;
				for (int k = depth; k > 0; k--) {
					checkpoint.run();
					String openTag = frameTags[k];
					if (openTag.equals("table")) break;
					if (openTag.equals("tr")) {
						if (tag.equals("tr") || tag.equals("caption")) from = k;
						break;
					}
					if (openTag.equals("td") || openTag.equals("th") || openTag.equals("caption")) from = k;
				}
				if (from >= 0) closeFrames(from);
			} else {
				closeBlock();
			}
		}

		/* The flat table parser already implies a row for td/th without tr; preserve that row in the box tree too. */
		void ensureRowBox() {
			for (int k = depth; k > 0; k--) {
				checkpoint.run();
				if (frameTags[k].equals("tr")) return;
				if (frameTags[k].equals("table")) break;
			}
			// Anonymous boxes inherit text, but must not acquire declarations from a real tr selector.
			int[] inherited = styles.compute(frames[depth], "", null, null);
			int box = addBox(frameBox[depth], "tr", inherited);
			push("tr", new String[0], inherited, box);
		}

		int addBox(int parent, String tag, int[] frame) {
			HtmlDocument d = doc;
			if (d.boxCount == d.boxParent.length) growBoxes();
			int box = d.boxCount++;
			d.boxParent[box] = parent;
			d.boxFirstChild[box] = d.boxNextSibling[box] = -1;
			if (lastChild[parent] < 0) d.boxFirstChild[parent] = box;
			else d.boxNextSibling[lastChild[parent]] = box;
			lastChild[parent] = box;
			d.boxFirstBlock[box] = d.boxEndBlock[box] = d.blockCount;
			d.boxRole[box] = switch (tag) {
				case "table" -> TABLE_BOX;
				case "tr" -> ROW_BOX;
				case "td", "th" -> CELL_BOX;
				default -> NORMAL_BOX;
			};
			d.boxPosition[box] = frame[HtmlStyles.POSITION];
			d.boxLeft[box] = frame[HtmlStyles.OFFSET_LEFT];
			d.boxTop[box] = frame[HtmlStyles.OFFSET_TOP];
			d.boxRight[box] = frame[HtmlStyles.OFFSET_RIGHT];
			d.boxBottom[box] = frame[HtmlStyles.OFFSET_BOTTOM];
			d.boxWidth[box] = frame[HtmlStyles.WIDTH];
			d.boxHeight[box] = frame[HtmlStyles.HEIGHT];
			d.boxZIndex[box] = frame[HtmlStyles.Z_INDEX];
			d.boxZAuto[box] = frame[HtmlStyles.Z_AUTO] != 0;
			d.boxBackground[box] = frame[HtmlStyles.BACKGROUND];
			d.boxOverflow[box] = frame[HtmlStyles.OVERFLOW];
			boolean boxModel = d.boxRole[box] != CELL_BOX;
			d.boxIndent[box] = boxModel ? frame[HtmlStyles.INDENT] : 0;
			d.boxMarginTop[box] = boxModel ? frame[HtmlStyles.MARGIN_TOP] : 0;
			d.boxMarginRight[box] = boxModel ? frame[HtmlStyles.MARGIN_RIGHT] : 0;
			d.boxMarginBottom[box] = boxModel ? frame[HtmlStyles.MARGIN_BOTTOM] : 0;
			d.boxPadTop[box] = boxModel ? frame[HtmlStyles.PADDING_TOP] : 0;
			d.boxPadRight[box] = boxModel ? frame[HtmlStyles.PADDING_RIGHT] : 0;
			d.boxPadBottom[box] = boxModel ? frame[HtmlStyles.PADDING_BOTTOM] : 0;
			d.boxPadLeft[box] = boxModel ? frame[HtmlStyles.PADDING_LEFT] : 0;
			d.boxBorder[box] = boxModel ? frame[HtmlStyles.BORDER_WIDTH] : 0;
			d.boxBorderColor[box] = boxModel ? frame[HtmlStyles.BORDER_COLOR] : 0;
			d.boxBorderStyle[box] = boxModel ? frame[HtmlStyles.BORDER_STYLE] : HtmlStyles.BORDER_SOLID;
			d.positioned |= d.boxPosition[box] != HtmlStyles.STATIC || d.boxWidth[box] != HtmlStyles.AUTO
					|| d.boxHeight[box] != HtmlStyles.AUTO || d.boxOverflow[box] != HtmlStyles.OVERFLOW_VISIBLE;
			return box;
		}

		void growBoxes() {
			HtmlDocument d = doc;
			int old = d.boxParent.length, capacity = 2 * old;
			d.boxParent = Arrays.copyOf(d.boxParent, capacity);
			d.boxFirstChild = Arrays.copyOf(d.boxFirstChild, capacity);
			d.boxNextSibling = Arrays.copyOf(d.boxNextSibling, capacity);
			d.boxFirstBlock = Arrays.copyOf(d.boxFirstBlock, capacity);
			d.boxEndBlock = Arrays.copyOf(d.boxEndBlock, capacity);
			d.boxRole = Arrays.copyOf(d.boxRole, capacity);
			d.boxPosition = Arrays.copyOf(d.boxPosition, capacity);
			d.boxLeft = Arrays.copyOf(d.boxLeft, capacity);
			d.boxTop = Arrays.copyOf(d.boxTop, capacity);
			d.boxRight = Arrays.copyOf(d.boxRight, capacity);
			d.boxBottom = Arrays.copyOf(d.boxBottom, capacity);
			d.boxWidth = Arrays.copyOf(d.boxWidth, capacity);
			d.boxHeight = Arrays.copyOf(d.boxHeight, capacity);
			d.boxZIndex = Arrays.copyOf(d.boxZIndex, capacity);
			d.boxZAuto = Arrays.copyOf(d.boxZAuto, capacity);
			d.boxBackground = Arrays.copyOf(d.boxBackground, capacity);
			d.boxOverflow = Arrays.copyOf(d.boxOverflow, capacity);
			d.boxIndent = Arrays.copyOf(d.boxIndent, capacity);
			d.boxMarginTop = Arrays.copyOf(d.boxMarginTop, capacity);
			d.boxMarginRight = Arrays.copyOf(d.boxMarginRight, capacity);
			d.boxMarginBottom = Arrays.copyOf(d.boxMarginBottom, capacity);
			d.boxPadTop = Arrays.copyOf(d.boxPadTop, capacity);
			d.boxPadRight = Arrays.copyOf(d.boxPadRight, capacity);
			d.boxPadBottom = Arrays.copyOf(d.boxPadBottom, capacity);
			d.boxPadLeft = Arrays.copyOf(d.boxPadLeft, capacity);
			d.boxBorder = Arrays.copyOf(d.boxBorder, capacity);
			d.boxBorderColor = Arrays.copyOf(d.boxBorderColor, capacity);
			d.boxBorderStyle = Arrays.copyOf(d.boxBorderStyle, capacity);
			lastChild = Arrays.copyOf(lastChild, capacity);
			Arrays.fill(lastChild, old, capacity, -1);
		}

		void push(String tag, int[] frame, int ownBox) {
			push(tag, new String[0], frame, ownBox);
		}

		/* closes the innermost open element of that tag and every element opened inside it (misnested tags) */
		void pop(String tag) {
			for (int k = depth; k > 0; k--) {
				checkpoint.run();
				if (frameTags[k].equals(tag)) {
					for (int f = depth; f >= k; f--) {
						checkpoint.run();
						if (frameOwnBox[f] < 0) continue;
						if (doc.boxRole[frameOwnBox[f]] == CELL_BOX) closeCell();
						else closeBlock();
					}
					closeFrames(k);
					return;
				}
			}
		}

		/*
		 * a flex or grid container opens: a table of its items (one row for flex, rows of its columns for grid); inside a
		 * table it is a plain block
		 */
		void startContainer() {
			int[] frame = frames[depth];
			tables++;
			startTable(new String[0]);
			HtmlDocument d = doc;
			int display = frame[HtmlStyles.DISPLAY];
			d.tableDisplay[table] = display;
			d.boxRole[frameBox[depth]] = display == HtmlStyles.DISPLAY_FLEX ? FLEX_BOX : GRID_BOX;
			d.tableJustify[table] = frame[HtmlStyles.JUSTIFY];
			d.tableGap[table] = frame[HtmlStyles.GAP];
			if (display == HtmlStyles.DISPLAY_GRID) {
				int[] list = styles.tracks(frame[HtmlStyles.TRACKS]);
				if (list.length == 0) list = new int[] { GridTracks.AUTO }; // no template: one column
				if (d.trackTotal + list.length > d.tracks.length) {
					d.tracks = Arrays.copyOf(d.tracks, Math.max(2 * d.tracks.length, d.trackTotal + list.length));
				}
				System.arraycopy(list, 0, d.tracks, d.trackTotal, list.length);
				d.tableTrackStart[table] = d.trackTotal;
				d.tableTrackCount[table] = list.length;
				d.trackTotal += list.length;
				d.tableColumns[table] = list.length;
				d.tableRowGap[table] = frame[HtmlStyles.ROW_GAP];
			}
			containerFrame = depth;
			startRow();
		}

		/*
		 * an item of the open container: a cell; an element item carries its flex-grow, basis and (in a grid) its column
		 * span; a grid places it in the current row, or at the start of the next when it does not fit (auto-placement)
		 */
		void openItem(boolean element) {
			if (element) doc.boxRole[frameBox[depth]] = CELL_BOX;
			int[] frame = frames[depth];
			int span = 1;
			if (doc.tableDisplay[table] == HtmlStyles.DISPLAY_GRID) {
				int n = doc.tableTrackCount[table];
				span = element ? Math.max(1, Math.min(n, frame[HtmlStyles.COLUMN_SPAN])) : 1;
				if (rowOpen && column > 0 && column + span > n) startRow();
			}
			openCell(0, span);
			doc.cellGrow[open] = element ? frame[HtmlStyles.FLEX_GROW] : 0;
			doc.cellBasis[open] = element ? frame[HtmlStyles.FLEX_BASIS] : -1;
			// An auto flex basis uses the independently computed width; shorthand declaration order cannot erase it.
			if (element && doc.cellBasis[open] < 0 && frame[HtmlStyles.WIDTH] != HtmlStyles.AUTO) {
				doc.cellBasis[open] = frame[HtmlStyles.WIDTH];
			}
			doc.cellShrink[open] = element ? HtmlStyles.shrink(frame) : 1;
		}

		void endContainer() {
			closeCell();
			rowOpen = false;
			tables = Math.max(0, tables - 1);
			containerFrame = 0;
		}

		void closeFrames(int from) {
			for (int k = depth; k >= from; k--) {
				checkpoint.run();
				int box = frameOwnBox[k];
				if (box >= 0) doc.boxEndBlock[box] = doc.blockCount;
				if (frameHidden[k]) {
					frameHidden[k] = false;
					skip = Math.max(0, skip - 1);
				}
			}
			depth = from - 1;
		}

		void finish() {
			closeCell();
			closeFrames(1);
			doc.boxEndBlock[0] = doc.blockCount;
			doc.boxLayout = needsBoxLayout();
		}

		/* Route only extensions to the resolved scene; a flat fragment remains the authority for its existing path. */
		boolean needsBoxLayout() {
			HtmlDocument d = doc;
			for (int box = 1; box < d.boxCount; box++) {
				if (d.boxOverflow[box] != HtmlStyles.OVERFLOW_VISIBLE) return true;
				checkpoint.run();
				if (d.boxWidth[box] != HtmlStyles.AUTO || d.boxHeight[box] != HtmlStyles.AUTO
						|| d.boxRight[box] != HtmlStyles.AUTO || d.boxBottom[box] != HtmlStyles.AUTO) return true;
				if (d.boxPosition[box] == HtmlStyles.STATIC) continue;
				int first = d.boxFirstBlock[box];
				// Keep the current simple sticky owner; nested sticky containers need their descendants projected together.
				if (d.boxPosition[box] == HtmlStyles.STICKY && d.boxFirstChild[box] < 0 && first < d.boxEndBlock[box]
						&& d.blockBox[first] == box && d.blockPosition[first] == HtmlStyles.STICKY) continue;
				if (d.boxParent[box] != 0 || d.boxFirstChild[box] >= 0 || first == d.boxEndBlock[box]
						|| d.blockBox[first] != box || d.blockPosition[first] != d.boxPosition[box]) return true;
			}
			return false;
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
