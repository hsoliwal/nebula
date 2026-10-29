/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * A Grid cell that looks like a cell of the platform's list or tree: native row height, text
 * position, full-row selection and hot-track styles, column lines, tree expander glyphs and check
 * boxes ({@code GridNativeLook}, {@code GridNativeTheme}). Without visual styles the selection uses
 * the system selection colours.
 * <p>
 * The row's one-pixel separator belongs to the row, as in the native controls: the renderer paints
 * {@code getBounds ().height + 1} pixels.
 * </p>
 */
final class GridNativeCellRenderer extends GridCellRenderer {

	/* the colour of the native list's column lines */
	private static final Color COLUMN_LINE = new Color(235, 244, 254);
	/* the native check box size without visual styles */
	private static final int CHECK_SIZE = 13;

	private final GridNativeTheme theme;

	/**
	 * @param theme the Grid's visual styles
	 */
	GridNativeCellRenderer(final GridNativeTheme theme) {
		this.theme = theme;
	}

	@Override
	public void paint(final GC gc, final Object value) {
		final GridItem item = (GridItem) value;
		final Grid grid = item.getParent();
		final GridNativeLook look = GridNativeLook.of(grid);
		final Rectangle cell = getBounds();
		final Rectangle row = new Rectangle(cell.x, cell.y, cell.width, cell.height + 1);
		final boolean tree = GridNativeLook.isTree(grid);
		paintBackground(gc, item, grid, row, tree);
		final Layout layout = layout(gc, item, look);
		if (isTree() && item.hasChildren()) {
			paintGlyph(gc, item, row, layout.glyph);
		}
		if (isCheck()) {
			paintCheck(gc, item, grid, row, layout.check);
		}
		final Image image = item.getImage(getColumn());
		if (image != null) {
			gc.drawImage(image, row.x + layout.image, row.y + (row.height - image.getBounds().height) / 2);
		}
		paintText(gc, item, grid, look, row, layout.text);
		if (grid.getLinesVisible()) {
			// grid lines in the separator row and at the right edge, in the Grid's line colour
			gc.setForeground(grid.getLineColor());
			gc.drawLine(row.x, row.y + row.height - 1, row.x + row.width - 1, row.y + row.height - 1);
			gc.drawLine(row.x + row.width - 1, row.y, row.x + row.width - 1, row.y + row.height - 1);
		}
	}

	private void paintBackground(final GC gc, final GridItem item, final Grid grid, final Rectangle row,
			final boolean tree) {
		final boolean selected = isSelected() || isCellSelected();
		if (!grid.isEnabled()) {
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			gc.fillRectangle(row);
		} else {
			final Color background = item.getBackground(getColumn());
			if (background != null) {
				gc.setBackground(background);
				gc.fillRectangle(row);
			}
		}
		if (!tree) {
			// the native list draws its column lines below the selection
			columnLine(gc, theme, row.x + row.width - 1, row.y, row.height);
		}
		if (!selected) {
			return;
		}
		final boolean focus = grid.isFocusOnGrid();
		final int state = !grid.isEnabled() ? GridNativeTheme.ITEM_SELECTED_NOT_FOCUS
				: focus ? GridNativeTheme.ITEM_SELECTED : GridNativeTheme.ITEM_SELECTED_NOT_FOCUS;
		final Rectangle full = isCellSelected() ? row : GridNativeLook.row(grid, row, getColumn());
		if (!theme.draw(gc, GridNativeLook.isTree(grid) ? GridNativeTheme.TREE : GridNativeTheme.LIST,
				GridNativeTheme.ITEM, state, full, row)) {
			gc.setBackground(getDisplay().getSystemColor(focus ? SWT.COLOR_LIST_SELECTION : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			gc.fillRectangle(row);
		}
	}

	/**
	 * Draws the native list's one-pixel column line.
	 *
	 * @param gc     the GC
	 * @param theme  the visual styles
	 * @param x      the line's x
	 * @param y      its top
	 * @param height its height
	 */
	static void columnLine(final GC gc, final GridNativeTheme theme, final int x, final int y, final int height) {
		if (theme.draw(gc, GridNativeTheme.LIST, GridNativeTheme.COLUMN_DETAIL, 0, new Rectangle(x - 1, y, 2, height), null)) {
			return;
		}
		final Color old = gc.getForeground();
		gc.setForeground(COLUMN_LINE);
		gc.drawLine(x, y, x, y + height - 1);
		gc.setForeground(old);
	}

	private void paintGlyph(final GC gc, final GridItem item, final Rectangle row, final int x) {
		final Point size = theme.size(gc, GridNativeTheme.TREE, GridNativeTheme.GLYPH, GridNativeTheme.GLYPH_CLOSED);
		final int state = item.isExpanded() ? GridNativeTheme.GLYPH_OPENED : GridNativeTheme.GLYPH_CLOSED;
		final int part = getHoverDetail().equals("toggle") ? GridNativeTheme.HOT_GLYPH : GridNativeTheme.GLYPH;
		if (size != null) {
			// the native tree puts its glyph right of the centre of the expander area
			final GridNativeLook look = GridNativeLook.of(item.getParent());
			final Rectangle bounds = new Rectangle(row.x + x + (look.expanderWidth - size.x + 3) / 2,
					row.y + (row.height - size.y + 2) / 2, size.x, size.y);
			if (theme.draw(gc, GridNativeTheme.TREE, part, state, bounds, null)) {
				return;
			}
		}
		// without visual styles: a plus or minus box
		final int s = 9, left = row.x + x + 3, top = row.y + (row.height - s) / 2;
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		gc.drawRectangle(left, top, s - 1, s - 1);
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
		gc.drawLine(left + 2, top + s / 2, left + s - 3, top + s / 2);
		if (!item.isExpanded()) {
			gc.drawLine(left + s / 2, top + 2, left + s / 2, top + s - 3);
		}
	}

	private void paintCheck(final GC gc, final GridItem item, final Grid grid, final Rectangle row, final int x) {
		final boolean enabled = grid.isEnabled() && item.getCheckable(getColumn());
		final boolean grayed = item.getGrayed(getColumn());
		final boolean checked = item.getChecked(getColumn());
		final int state = grayed ? enabled ? GridNativeTheme.MIXED : GridNativeTheme.MIXED_DISABLED
				: checked ? enabled ? GridNativeTheme.CHECKED : GridNativeTheme.CHECKED_DISABLED
						: enabled ? GridNativeTheme.UNCHECKED : GridNativeTheme.UNCHECKED_DISABLED;
		final Point size = checkSize(gc);
		final Rectangle bounds = new Rectangle(row.x + x, row.y + (row.height - size.y) / 2, size.x, size.y);
		// the native list draws its check boxes from images painted on the list background
		if (theme.drawOpaque(gc, GridNativeTheme.BUTTON, GridNativeTheme.CHECK_BOX, state, bounds, grid.getBackground())) {
			return;
		}
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		gc.drawRectangle(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
		if (checked || grayed) {
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
			gc.drawLine(bounds.x + 3, bounds.y + 6, bounds.x + 5, bounds.y + 8);
			gc.drawLine(bounds.x + 5, bounds.y + 8, bounds.x + 9, bounds.y + 4);
		}
	}

	private Point checkSize(final GC gc) {
		final Point size = theme.size(gc, GridNativeTheme.BUTTON, GridNativeTheme.CHECK_BOX, GridNativeTheme.UNCHECKED);
		return size != null ? size : new Point(CHECK_SIZE, CHECK_SIZE);
	}

	private void paintText(final GC gc, final GridItem item, final Grid grid, final GridNativeLook look,
			final Rectangle row, final int x) {
		gc.setFont(item.getFont(getColumn()));
		final Color foreground = item.getForeground(getColumn());
		gc.setForeground(!grid.isEnabled() ? getDisplay().getSystemColor(SWT.COLOR_WIDGET_DISABLED_FOREGROUND)
				: foreground != null && !isSelected() ? foreground : getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
		final int right = look.listTextInset;
		final int width = row.width - x - right;
		String text = item.getText(getColumn());
		if (width <= 0 || text.isEmpty()) {
			return;
		}
		text = shorten(gc, text, width - ELLIPSIS_SLACK);
		int left = row.x + x;
		if (getAlignment() == SWT.RIGHT || getAlignment() == SWT.CENTER) {
			final int extent = gc.textExtent(text).x;
			left += getAlignment() == SWT.RIGHT ? width - extent : (width - extent) / 2;
		}
		gc.drawString(text, left, row.y + look.textTop(row.height), true);
	}

	/* the native list ends a cell text with an ellipsis two pixels before the end of its text area */
	private static final int ELLIPSIS_SLACK = 2;

	/**
	 * Shortens text the way the native controls do (an end ellipsis): the longest prefix that fits
	 * with "..." appended, when the whole text does not fit.
	 *
	 * @param gc    the GC with the font
	 * @param text  the text
	 * @param width the available width
	 * @return the text to draw
	 */
	static String shorten(final GC gc, final String text, final int width) {
		if (gc.textExtent(text).x <= width) {
			return text;
		}
		int low = 0, high = text.length();
		while (low < high) {
			final int mid = (low + high + 1) >>> 1;
			if (gc.textExtent(text.substring(0, mid) + "...").x <= width) {
				low = mid;
			} else {
				high = mid - 1;
			}
		}
		return low == 0 && gc.textExtent("...").x > width ? "" : text.substring(0, low) + "...";
	}

	/** Positions inside a cell, from its left edge. */
	private static final class Layout {
		int glyph, check, image, text;
	}

	private Layout layout(final GC gc, final GridItem item, final GridNativeLook look) {
		final Layout layout = new Layout();
		final boolean treeGrid = GridNativeLook.isTree(item.getParent());
		int x = 0;
		if (isTree()) {
			x = item.getLevel() * look.treeIndent;
			layout.glyph = x;
			x += look.expanderWidth;
		}
		if (isCheck()) {
			final Point size = checkSize(gc);
			// the native list puts its check box two pixels before the space of the first column's text
			final int space = look.checkTextInset - look.listTextInset;
			layout.check = x + space - size.x - 2;
			x += space;
		}
		layout.image = x + (isTree() || isCheck() ? 0 : 4);
		final Image image = item.getImage(getColumn());
		if (image != null) {
			x += image.getBounds().width + 4;
		}
		layout.text = isTree() ? x + look.treeTextGap : x + (treeGrid ? look.treeTextInset : look.listTextInset);
		return layout;
	}

	@Override
	public Point computeSize(final GC gc, final int wHint, final int hHint, final Object value) {
		final GridItem item = (GridItem) value;
		final GridNativeLook look = GridNativeLook.of(item.getParent());
		gc.setFont(item.getFont(getColumn()));
		final Layout layout = layout(gc, item, look);
		final int width = layout.text + gc.textExtent(item.getText(getColumn())).x + look.listTextInset;
		// the Grid adds a one-pixel separator below each row; together they are the native row,
		// which grows with a taller font or image as the native rows do
		final int row = look.rowHeight(item.getParent());
		final Image image = item.getImage(getColumn());
		final int text = item.getFont(getColumn()) == null ? look.fontHeight : gc.getFontMetrics().getHeight();
		final int content = Math.max(text, image == null ? 0 : image.getBounds().height);
		return new Point(width, Math.max(row, content + row - look.fontHeight) - 1);
	}

	@Override
	public boolean notify(final int event, final Point point, final Object value) {
		final GridItem item = (GridItem) value;
		final Rectangle cell = getBounds();
		final GridNativeLook look = GridNativeLook.of(item.getParent());
		final int x = point.x - cell.x;
		final int y = point.y - cell.y;
		final GC gc = new GC(item.getParent());
		final Layout layout;
		final Point checkSize;
		try {
			layout = layout(gc, item, look);
			checkSize = checkSize(gc);
		} finally {
			gc.dispose();
		}
		if (isCheck() && x >= layout.check && x < layout.check + checkSize.x) {
			if (event == IInternalWidget.MouseMove) {
				setHoverDetail("check");
				return true;
			}
			if (event == IInternalWidget.LeftMouseButtonDown && item.getCheckable(getColumn())) {
				item.setChecked(getColumn(), !item.getChecked(getColumn()));
				item.getParent().redraw();
				item.fireCheckEvent(getColumn());
				return true;
			}
		}
		if (isTree() && item.hasChildren() && x >= layout.glyph && x < layout.glyph + look.expanderWidth
				&& y >= 0 && y <= cell.height) {
			if (event == IInternalWidget.MouseMove) {
				setHoverDetail("toggle");
				return true;
			}
			if (event == IInternalWidget.LeftMouseButtonDown) {
				item.setExpanded(!item.isExpanded());
				item.getParent().redraw();
				item.fireEvent(item.isExpanded() ? SWT.Expand : SWT.Collapse);
				return true;
			}
		}
		return false;
	}

	@Override
	public Rectangle getTextBounds(final GridItem item, final boolean preferred) {
		final GridNativeLook look = GridNativeLook.of(item.getParent());
		final GC gc = new GC(item.getParent());
		try {
			gc.setFont(item.getFont(getColumn()));
			final Layout layout = layout(gc, item, look);
			final Point size = gc.stringExtent(item.getText(getColumn()));
			final int rowHeight = getBounds().height + 1;
			return new Rectangle(layout.text, look.textTop(rowHeight), preferred ? size.x - 1
					: getBounds().width - layout.text - look.listTextInset, size.y);
		} finally {
			gc.dispose();
		}
	}
}
