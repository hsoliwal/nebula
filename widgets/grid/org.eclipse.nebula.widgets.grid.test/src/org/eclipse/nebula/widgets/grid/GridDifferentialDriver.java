/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.nebula.widgets.grid.internal.DefaultEmptyColumnHeaderRenderer;
import org.eclipse.nebula.widgets.grid.internal.DefaultColumnHeaderRenderer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * One side of the recorded Grid differential ({@link GridViewportDifferential_Test}): builds a Grid
 * and applies scripted operations to it through the public Grid, GridItem and GridColumn API only,
 * so the same driver runs against any Grid build (stock, viewport, upstream).
 * <p>
 * The test loads this class once per Grid build through an isolating class loader and talks to it
 * through {@link Function}: an operation is an {@code int[]} of a kind and three random numbers,
 * the answer a {@link String}, or the printed pixels ({@link ImageData}) for {@link #PIXELS}.
 * </p>
 */
public class GridDifferentialDriver implements Function<int[], Object> {

	/** Operation kinds. */
	static final int STATE = -1, PIXELS = -2, MODE = -3, KINDS = 26;

	static final int ROOTS = 120, CHILDREN = 12, GRANDCHILDREN = 3, COLUMNS = 4;

	/**
	 * Scripted steps, outside the random kinds: header and footer content and footer visibility
	 * changed after the columns exist ({@code a == 0} undoes the change; {@code b} picks the column).
	 */
	static final int HEADER_FONT = 100, HEADER_IMAGE = 101, FOOTER_VISIBLE = 102, FOOTER_TEXT = 103, FOOTER_FONT = 104;

	/**
	 * Scripted steps: install header renderers that paint {@link #OVERFLOW_ROWS} rows below their
	 * bounds (the empty column header and the picked column), and remove every item.
	 */
	static final int OVERFLOW_RENDERERS = 105, REMOVE_ALL = 106;

	/** Pixel rows the overflowing renderers paint below their bounds. */
	static final int OVERFLOW_ROWS = 3;

	/* fills the rows below the bounds a header renderer paints past them */
	static void paintBelow(final GC gc, final Rectangle bounds) {
		final Color old = gc.getBackground();
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_YELLOW));
		gc.fillRectangle(bounds.x, bounds.y + bounds.height, bounds.width, OVERFLOW_ROWS);
		gc.setBackground(old);
	}

	/** A column header renderer that paints below its bounds and declares it. */
	public static final class OverflowingHeader extends DefaultColumnHeaderRenderer {
		@Override
		public void paint(final GC gc, final Object value) {
			super.paint(gc, value);
			paintBelow(gc, getBounds());
		}

		@Override
		public int getOverflowBelow() {
			return OVERFLOW_ROWS;
		}
	}

	/** An empty column header renderer that paints below its bounds and declares it. */
	public static final class OverflowingEmptyHeader extends DefaultEmptyColumnHeaderRenderer {
		@Override
		public void paint(final GC gc, final Object value) {
			super.paint(gc, value);
			paintBelow(gc, getBounds());
		}

		@Override
		public int getOverflowBelow() {
			return OVERFLOW_ROWS;
		}
	}

	/**
	 * Configuration bits; {@link #LARGE} builds 1,000 roots with 99 children each instead;
	 * {@link #VIRTUAL} makes an {@code SWT.VIRTUAL} Grid whose items get their texts from a SetData
	 * listener, which counts the requests per item.
	 */
	static final int CELL_SELECTION = 1, DIFFERING_HEIGHTS = 2, LARGE = 4, VIRTUAL = 8;

	private final Grid grid;
	private final StringBuilder events = new StringBuilder();
	private int added;
	/* SetData requests per item label (VIRTUAL configurations) */
	private final java.util.Map<String, Integer> setData = new java.util.TreeMap<>();
	private final boolean virtual;

	/**
	 * @param parent where the Grid is created
	 * @param x      left edge of the Grid
	 * @param config {@link #CELL_SELECTION} and {@link #DIFFERING_HEIGHTS} bits
	 */
	public GridDifferentialDriver(final Composite parent, final Integer x, final Integer config) {
		final int style = SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.CHECK;
		// a large tree keeps its data in the items (the row-addressed default re-maps every row per insert)
		grid = (config.intValue() & LARGE) != 0 ? new Grid(visualizer(parent), parent, style) : new Grid(parent, style);
		grid.setBounds(x.intValue(), 5, 300, 330);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		grid.setRowHeaderVisible((config.intValue() & DIFFERING_HEIGHTS) != 0);
		grid.setCellSelectionEnabled((config.intValue() & CELL_SELECTION) != 0);
		for (int c = 0; c < COLUMNS; c++) {
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("c" + c);
			column.setWidth(60 + 15 * c);
			column.setTree(c == 0);
		}
		final boolean large = (config.intValue() & LARGE) != 0;
		for (int r = 0; r < (large ? 1000 : ROOTS); r++) {
			final GridItem root = item(new GridItem(grid, SWT.NONE), "r" + r);
			if (large || r % 5 == 0) {
				for (int c = 0; c < (large ? 99 : CHILDREN); c++) {
					final GridItem child = item(new GridItem(root, SWT.NONE), "r" + r + "/" + c);
					for (int g = 0; !large && c % 4 == 1 && g < GRANDCHILDREN; g++) {
						item(new GridItem(child, SWT.NONE), "r" + r + "/" + c + "/" + g);
					}
				}
			}
			if ((config.intValue() & DIFFERING_HEIGHTS) != 0 && r % 7 == 3) {
				root.setHeight(root.getHeight() + 9);
			}
		}
		grid.addListener(SWT.Selection, e -> record("Selection", e));
		grid.addListener(SWT.DefaultSelection, e -> record("DefaultSelection", e));
		grid.addListener(SWT.Expand, e -> record("Expand", e));
		grid.addListener(SWT.Collapse, e -> record("Collapse", e));
	}

	private static DataVisualizer visualizer(final Composite parent) {
		return new GridItemDataVisualizer(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE),
				parent.getDisplay().getSystemColor(SWT.COLOR_BLACK), null);
	}

	private static GridItem item(final GridItem item, final String text) {
		for (int c = 0; c < COLUMNS; c++) {
			item.setText(c, c == 0 ? text : text + ":" + c);
		}
		return item;
	}

	private void record(final String type, final Event e) {
		events.append(type).append('(').append(e.item instanceof GridItem ? ((GridItem) e.item).getText(0) : "-")
				.append(' ').append(e.detail).append(") ");
	}

	@Override
	public Object apply(final int[] op) {
		try {
			switch (op[0]) {
			case STATE:
				return state();
			case PIXELS:
				return pixels();
			case MODE:
				return mode();
			default:
				return step(op[0], op[1], op[2], op[3]);
			}
		} catch (final RuntimeException e) {
			return e.getClass().getSimpleName() + ": " + e.getMessage();
		}
	}

	private String step(final int kind, final int a, final int b, final int c) {
		if (kind >= HEADER_FONT) {
			return scripted(kind, a != 0, grid.getColumns()[b % grid.getColumns().length]);
		}
		final GridItem[] items = grid.getItems();
		final GridColumn[] columns = grid.getColumns();
		final GridItem item = items.length == 0 ? null : items[a % items.length];
		final int index = items.length == 0 ? 0 : a % items.length;
		final GridColumn column = columns[b % columns.length];
		switch (kind) {
		case 0:
			item.setExpanded(true);
			return "expanded " + item.getText(0);
		case 1:
			item.setExpanded(false);
			return "collapsed " + item.getText(0);
		case 2:
			grid.setTopIndex(index);
			return "top " + grid.getTopIndex();
		case 3:
			grid.showItem(item);
			return "top " + grid.getTopIndex();
		case 4:
			grid.setSelection(index);
			return "selected";
		case 5:
			grid.select(index);
			return "selected";
		case 6:
			grid.deselect(index);
			return "deselected";
		case 7:
			grid.deselectAll();
			return "deselected all";
		case 8:
			if (grid.getCellSelectionEnabled()) {
				grid.selectCell(new Point(b % columns.length, index));
			} else {
				grid.select(index, Math.min(items.length - 1, index + c % 6));
			}
			return "selected range";
		case 9:
			grid.setFocusItem(item);
			return "focus " + text(grid.getFocusItem());
		case 10:
			grid.setFocusColumn(column);
			return "focus cell " + grid.getFocusCell();
		case 11:
			grid.showColumn(column);
			return "hbar " + bar(grid.getHorizontalBar());
		case 12:
			grid.setColumnOrder(rotation(columns.length, b));
			return "order " + Arrays.toString(grid.getColumnOrder());
		case 13:
			column.setWidth(30 + c % 140);
			return "width " + column.getWidth();
		case 14:
			column.setVisible(c % 3 != 0 || b % columns.length == 0);
			return "visible " + column.isVisible();
		case 15:
			item.setHeight(8 + c % 30);
			return "height " + item.getHeight();
		case 16:
			item.setChecked(c % 2 == 0);
			return "checked " + item.getChecked();
		case 17:
			return probe(b, c);
		case 18:
			return "bounds " + item.getBounds(b % columns.length);
		case 19:
			return "next " + text(grid.getNextVisibleItem(item)) + " previous " + text(grid.getPreviousVisibleItem(item));
		case 20:
			return scroll(a, b);
		case 21:
			return key(b, c);
		case 22:
			return click(b, c);
		case 23:
			return insert(item, b);
		case 24:
			if (items.length > 20) {
				final String text = item.getText(0);
				item.dispose();
				return "disposed " + text;
			}
			return "kept";
		default:
			grid.setItemHeight(14 + c % 12);
			return "item height " + grid.getItemHeight();
		}
	}

	private Font largeFont;
	private Image tallImage;

	private String scripted(final int kind, final boolean set, final GridColumn column) {
		if (largeFont == null) {
			largeFont = new Font(grid.getDisplay(), "Arial", 20, SWT.NORMAL);
			tallImage = new Image(grid.getDisplay(), 16, 40);
			grid.addDisposeListener(e -> {
				largeFont.dispose();
				tallImage.dispose();
			});
		}
		switch (kind) {
		case HEADER_FONT:
			column.setHeaderFont(set ? largeFont : null);
			break;
		case HEADER_IMAGE:
			column.setImage(set ? tallImage : null);
			break;
		case FOOTER_VISIBLE:
			grid.setFooterVisible(set);
			break;
		case FOOTER_TEXT:
			column.setFooterText(set ? "f" + column.getText() : "");
			break;
		case OVERFLOW_RENDERERS:
			grid.setEmptyColumnHeaderRenderer(new OverflowingEmptyHeader());
			column.setHeaderRenderer(new OverflowingHeader());
			break;
		case REMOVE_ALL:
			grid.removeAll();
			break;
		default:
			column.setFooterFont(set ? largeFont : null);
		}
		return "scripted " + kind + " " + set;
	}

	private String probe(final int b, final int c) {
		final Rectangle area = grid.getClientArea();
		final Point point = new Point(b % Math.max(1, area.width), c % Math.max(1, area.height + 20) - 10);
		return "at " + point + " item " + text(grid.getItem(point)) + " cell " + grid.getCell(point);
	}

	private String scroll(final int a, final int b) {
		final ScrollBar bar = grid.getVerticalBar();
		if (!bar.isVisible()) {
			return "no bar";
		}
		final int selection = b % 2 == 0 ? a % (bar.getMaximum() + 1) : bar.getMaximum();
		bar.setSelection(selection);
		bar.notifyListeners(SWT.Selection, new Event());
		return "scrolled " + bar.getSelection() + " top " + grid.getTopIndex();
	}

	private static final int[] KEYS = { SWT.ARROW_DOWN, SWT.ARROW_UP, SWT.PAGE_DOWN, SWT.PAGE_UP, SWT.HOME, SWT.END,
			SWT.ARROW_RIGHT, SWT.ARROW_LEFT };

	private String key(final int b, final int c) {
		final Event event = new Event();
		event.keyCode = KEYS[b % KEYS.length];
		event.stateMask = c % 3 == 0 ? SWT.SHIFT : 0;
		event.doit = true;
		grid.notifyListeners(SWT.KeyDown, event);
		return "key " + event.keyCode + " top " + grid.getTopIndex();
	}

	private String click(final int b, final int c) {
		final Rectangle area = grid.getClientArea();
		final Event event = new Event();
		event.button = 1;
		event.x = b % Math.max(1, area.width);
		event.y = c % Math.max(1, area.height);
		// With cell selection, a header click selects the clicked column's cells in the default
		// mode (UPSTREAM_DEFECTS.tsv D27, archived bugs 218713/238601) and nothing in stock: a
		// listed divergence, asserted per mode by GridArchivedBugs_Test. The seeded scripts stay a
		// strict parity oracle, so they park such clicks below the header, deterministically and
		// identically on both sides; configs without cell selection keep clicking headers.
		if (grid.getCellSelectionEnabled() && grid.getHeaderVisible() && event.y <= grid.getHeaderHeight()) {
			event.y = Math.min(area.height - 1, grid.getHeaderHeight() + 1 + event.y);
		}
		event.stateMask = c % 4 == 0 ? SWT.MOD1 : 0;
		grid.notifyListeners(SWT.MouseDown, event);
		final Event up = new Event();
		up.button = 1;
		up.x = event.x;
		up.y = event.y;
		up.stateMask = event.stateMask;
		grid.notifyListeners(SWT.MouseUp, up);
		return "click " + event.x + "," + event.y + " top " + grid.getTopIndex();
	}

	private String insert(final GridItem item, final int b) {
		final String text = "n" + added++;
		if (b % 3 == 0 || item.getParentItem() == null && b % 3 == 1) {
			item(new GridItem(grid, SWT.NONE, b % (grid.getRootItemCount() + 1)), text);
		} else {
			final GridItem parent = item.getParentItem() != null ? item.getParentItem() : item;
			item(new GridItem(parent, SWT.NONE, b % (parent.getItemCount() + 1)), text);
		}
		return "inserted " + text;
	}

	private static int[] rotation(final int count, final int shift) {
		final int[] order = new int[count];
		for (int i = 0; i < count; i++) {
			order[i] = (i + shift) % count;
		}
		return order;
	}

	private static String text(final GridItem item) {
		return item == null ? "null" : item.getText(0);
	}

	private static String bar(final ScrollBar bar) {
		return bar == null ? "-" : bar.isVisible() + " " + bar.getSelection() + "/" + bar.getMaximum() + " thumb " + bar.getThumb();
	}

	private String state() {
		final List<String> selection = new ArrayList<>();
		for (final GridItem item : grid.getSelection()) {
			selection.add(item.getText(0));
		}
		final List<String> cells = new ArrayList<>();
		for (final Point cell : grid.getCellSelection()) {
			cells.add(cell.x + ":" + cell.y);
		}
		cells.sort(null);
		final String answer = "vbar " + bar(grid.getVerticalBar()) + " | hbar " + bar(grid.getHorizontalBar())
				+ " | top " + grid.getTopIndex() + " count " + grid.getItemCount()
				+ " | sel " + Arrays.toString(grid.getSelectionIndices()) + selection
				+ " | cells " + cells
				+ " | focus " + text(grid.getFocusItem()) + " " + grid.getFocusCell()
				+ " | header " + grid.getHeaderHeight() + " footer " + grid.getFooterHeight()
				+ " | events " + events;
		events.setLength(0);
		return answer;
	}

	private ImageData pixels() {
		// the client area: what the Grid paints (scroll bars and borders are compared by value)
		final Rectangle area = grid.getClientArea();
		final Point size = new Point(Math.max(1, area.width), Math.max(1, area.height));
		final Image image = new Image(grid.getDisplay(), size.x, size.y);
		try {
			// the Grid's own paint: Control.print can return the window's last presented surface
			final GC gc = new GC(image);
			try {
				// what the window's background erase does before the paint event
				gc.setBackground(grid.getBackground());
				gc.fillRectangle(0, 0, size.x, size.y);
				final Event event = new Event();
				event.gc = gc;
				event.width = size.x;
				event.height = size.y;
				grid.notifyListeners(SWT.Paint, event);
			} finally {
				gc.dispose();
			}
			return image.getImageData();
		} finally {
			image.dispose();
		}
	}

	/* "viewport", "original", or "stock" for a Grid build without the mode switch */
	private String mode() {
		try {
			final java.lang.reflect.Field field = Grid.class.getDeclaredField("viewport");
			field.setAccessible(true);
			return field.get(grid) != null ? "viewport" : "original";
		} catch (final ReflectiveOperationException e) {
			return "stock";
		}
	}
}
