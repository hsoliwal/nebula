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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * The native look of a Grid: the metrics of the platform's own list (SWT {@code Table}) and tree
 * ({@code Tree}) for the Grid's font, measured once per display and font from hidden native
 * controls through their public API, plus the platform colours and visual styles
 * ({@link GridNativeTheme}).
 * <p>
 * A Grid in the viewport mode uses this look by default; start with
 * {@code -Dorg.eclipse.nebula.widgets.grid.look=classic} for Grid's classic renderers (they also
 * stay selectable per column with {@link GridColumn#setCellRenderer(GridCellRenderer)} and
 * {@link GridColumn#setHeaderRenderer(GridHeaderRenderer)}).
 * </p>
 */
final class GridNativeLook {

	/** Whether viewport Grids use the native look (read once). */
	static final boolean ENABLED = !"classic".equals(System.getProperty("org.eclipse.nebula.widgets.grid.look"));

	private static final String CACHE = GridNativeLook.class.getName();

	/** Row heights of the native list, the native list with check boxes and the native tree. */
	final int listItemHeight, checkItemHeight, treeItemHeight;
	/** Height of the native header. */
	final int headerHeight;
	/** Text origin from a list column's left edge, and with a check box in the first column. */
	final int listTextInset, checkTextInset;
	/** Text origin from a tree's non-tree column's left edge. */
	final int treeTextInset;
	/** Tree indent per level, the width of the expander area and the text origin after it. */
	final int treeIndent, expanderWidth, treeTextGap;
	/** Font height. */
	final int fontHeight;

	private GridNativeLook(final Display display, final Font font) {
		final Shell shell = new Shell(display);
		try {
			shell.setFont(font);
			final Table table = table(shell, 0);
			final TableItem item = table.getItem(0);
			listItemHeight = table.getItemHeight();
			headerHeight = table.getHeaderHeight();
			listTextInset = item.getTextBounds(1).x - item.getBounds(1).x;
			final Table checked = table(shell, SWT.CHECK);
			checkItemHeight = checked.getItemHeight();
			checkTextInset = checked.getItem(0).getTextBounds(0).x;
			final Tree tree = new Tree(shell, SWT.FULL_SELECTION);
			tree.setFont(font);
			tree.setHeaderVisible(true);
			for (int c = 0; c < 2; c++) {
				new TreeColumn(tree, SWT.NONE).setWidth(100);
			}
			final TreeItem root = new TreeItem(tree, SWT.NONE);
			root.setText(new String[] { "Root", "Root" });
			final TreeItem child = new TreeItem(root, SWT.NONE);
			child.setText("Child");
			root.setExpanded(true);
			treeItemHeight = tree.getItemHeight();
			expanderWidth = root.getBounds(0).x;
			treeTextGap = root.getTextBounds(0).x - expanderWidth;
			treeIndent = child.getBounds(0).x - expanderWidth;
			// the tree reports its sub-item text bounds two pixels left of where it draws the text
			treeTextInset = root.getTextBounds(1).x - root.getBounds(1).x + 2;
			final GC gc = new GC(shell);
			try {
				gc.setFont(font);
				fontHeight = gc.getFontMetrics().getHeight();
			} finally {
				gc.dispose();
			}
		} finally {
			shell.dispose();
		}
	}

	private static Table table(final Shell shell, final int style) {
		final Table table = new Table(shell, SWT.FULL_SELECTION | style);
		table.setFont(shell.getFont());
		table.setHeaderVisible(true);
		for (int c = 0; c < 2; c++) {
			new TableColumn(table, SWT.NONE).setWidth(100);
		}
		new TableItem(table, SWT.NONE).setText(new String[] { "Item", "Item" });
		return table;
	}

	/**
	 * @param grid a Grid
	 * @return the native look for the Grid's display and font
	 */
	@SuppressWarnings("unchecked")
	static GridNativeLook of(final Grid grid) {
		final Display display = grid.getDisplay();
		Map<Font, GridNativeLook> looks = (Map<Font, GridNativeLook>) display.getData(CACHE);
		if (looks == null) {
			looks = new HashMap<>();
			display.setData(CACHE, looks);
		}
		return looks.computeIfAbsent(grid.getFont(), font -> new GridNativeLook(display, font));
	}

	/**
	 * @param grid a Grid
	 * @return whether the Grid shows tree rows (a tree column), so it looks like the native tree
	 */
	static boolean isTree(final Grid grid) {
		for (final GridColumn column : grid.getColumns()) {
			if (column.isTree()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param grid a Grid
	 * @return the native row height (the Grid row plus its one-pixel separator)
	 */
	int rowHeight(final Grid grid) {
		if (isTree(grid)) {
			return treeItemHeight;
		}
		for (final GridColumn column : grid.getColumns()) {
			if (column.isCheck()) {
				return checkItemHeight;
			}
		}
		return listItemHeight;
	}

	/**
	 * @param rowHeight a row or header height
	 * @return where the native controls put a line of text in it, from its top
	 */
	int textTop(final int rowHeight) {
		return (rowHeight - fontHeight) / 2;
	}

	/**
	 * @return the text origin from a native header item's left edge (one pixel right of the list's)
	 */
	int headerTextInset() {
		return listTextInset + 1;
	}

	/**
	 * Installs the native look's Grid-level renderers.
	 *
	 * @param grid the Grid
	 */
	static void install(final Grid grid, final GridNativeTheme theme) {
		grid.setEmptyColumnHeaderRenderer(new EmptyHeader());
		grid.setEmptyCellRenderer(new EmptyCell(theme));
		grid.setFocusRenderer(new Focus(theme));
	}

	/**
	 * Installs the native look's column renderers.
	 *
	 * @param column the new column
	 */
	static void install(final GridColumn column, final GridNativeTheme theme) {
		column.setHeaderRenderer(new GridNativeHeaderRenderer(theme));
		column.setCellRenderer(new GridNativeCellRenderer(theme));
	}

	/**
	 * @param grid    the Grid
	 * @param cell    a cell of a row
	 * @param column  the cell's column index
	 * @return the native full-row rectangle of the cell's row: from the left edge of the first
	 *         visible column to the right edge of the last one, in display order
	 */
	static Rectangle row(final Grid grid, final Rectangle cell, final int column) {
		final Rectangle row = columnsRow(grid, cell, column);
		if (isTree(grid)) {
			// the native tree selects its rows across the whole client width
			row.width = Math.max(row.width, grid.getClientArea().width - row.x);
		}
		return row;
	}

	private static Rectangle columnsRow(final Grid grid, final Rectangle cell, final int column) {
		int before = 0, total = 0;
		boolean found = false;
		for (final int index : grid.getColumnOrder()) {
			final GridColumn c = grid.getColumn(index);
			if (index == column) {
				found = true;
			}
			if (c.isVisible()) {
				if (!found) {
					before += c.getWidth();
				}
				total += c.getWidth();
			}
		}
		return new Rectangle(cell.x - before, cell.y, total, cell.height);
	}

	/** The header area right of the last column: plain list background. */
	static final class EmptyHeader extends AbstractRenderer {
		@Override
		public int getOverflowBelow() {
			return 1;
		}

		@Override
		public void paint(final GC gc, final Object value) {
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			final Rectangle b = getBounds();
			gc.fillRectangle(b.x, b.y, b.width + 1, b.height + 1);
		}

		@Override
		public Point computeSize(final GC gc, final int wHint, final int hHint, final Object value) {
			return new Point(wHint, hHint);
		}
	}

	/** Cells without an item or right of the last column: background and the list's column lines. */
	static final class EmptyCell extends GridCellRenderer {
		private final GridNativeTheme theme;

		EmptyCell(final GridNativeTheme theme) {
			this.theme = theme;
		}

		@Override
		public void paint(final GC gc, final Object value) {
			final Grid grid = value instanceof GridItem ? ((GridItem) value).getParent() : (Grid) value;
			final Rectangle b = getBounds();
			gc.setBackground(grid.getDisplay().getSystemColor(grid.isEnabled() ? SWT.COLOR_LIST_BACKGROUND : SWT.COLOR_WIDGET_BACKGROUND));
			gc.fillRectangle(b.x, b.y, b.width + 1, b.height + 1);
			if (isSelected() && value instanceof GridItem && GridNativeLook.isTree(grid)) {
				// the native tree selects its rows across the whole client width
				int total = 0;
				for (final GridColumn column : grid.getColumns()) {
					total += column.isVisible() ? column.getWidth() : 0;
				}
				final int state = !grid.isEnabled() || !grid.isFocusOnGrid() ? GridNativeTheme.ITEM_SELECTED_NOT_FOCUS
						: GridNativeTheme.ITEM_SELECTED;
				theme.draw(gc, GridNativeTheme.TREE, GridNativeTheme.ITEM, state, new Rectangle(b.x - total, b.y,
						Math.max(total, grid.getClientArea().width - (b.x - total)), b.height + 1),
						new Rectangle(b.x, b.y, b.width + 1, b.height + 1));
			}
			if (getColumn() >= 0 && getColumn() < grid.getColumnCount() && !GridNativeLook.isTree(grid)) {
				GridNativeCellRenderer.columnLine(gc, theme, b.x + b.width - 1, b.y, b.height + 1);
			}
		}

		@Override
		public Point computeSize(final GC gc, final int wHint, final int hHint, final Object value) {
			return new Point(wHint, hHint);
		}

		@Override
		public boolean notify(final int event, final Point point, final Object value) {
			return false;
		}
	}

	/**
	 * The focus row of a focused Grid: the native dotted focus rectangle, one pixel inside the
	 * full-row rectangle. The Grid gives this renderer the row from its left edge to the client
	 * area's right edge, one pixel above the row.
	 */
	static final class Focus extends AbstractRenderer {
		private final GridNativeTheme theme;

		Focus(final GridNativeTheme theme) {
			this.theme = theme;
		}

		@Override
		public void paint(final GC gc, final Object value) {
			final Grid grid = ((GridItem) value).getParent();
			if (isTree(grid) && theme.hidesFocusCues()) {
				// the native tree shows its focus rectangle only with keyboard cues; the list always
				return;
			}
			final Rectangle b = getBounds();
			int total = 0;
			for (final GridColumn column : grid.getColumns()) {
				total += column.isVisible() ? column.getWidth() : 0;
			}
			final org.eclipse.swt.widgets.ScrollBar bar = grid.getHorizontalBar();
			final int left = b.x - (bar != null && bar.isVisible() && !grid.getColumnScrolling() ? bar.getSelection() : 0);
			if (isTree(grid)) {
				total = Math.max(total, grid.getClientArea().width - left);
			}
			gc.setForeground(grid.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
			gc.setBackground(grid.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			gc.drawFocus(left + 1, b.y + 2, total - 2, b.height - 2);
		}

		@Override
		public Point computeSize(final GC gc, final int wHint, final int hHint, final Object value) {
			return new Point(wHint, hHint);
		}
	}
}
