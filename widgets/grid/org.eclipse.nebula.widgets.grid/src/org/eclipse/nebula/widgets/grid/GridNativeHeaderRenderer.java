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
import org.eclipse.swt.graphics.TextLayout;

/**
 * A Grid column header that looks like the platform's list header: native height, the header
 * item's visual style (normal, hot, pressed), text position and the separator at its right edge.
 * Without visual styles it paints the system's flat header colours.
 */
final class GridNativeHeaderRenderer extends GridHeaderRenderer {

	/* the colour of the native header separator when no visual style provides it */
	private static final Color SEPARATOR = new Color(229, 229, 229);

	private final GridNativeTheme theme;

	/**
	 * @param theme the Grid's visual styles
	 */
	GridNativeHeaderRenderer(final GridNativeTheme theme) {
		this.theme = theme;
	}

	@Override
	public void paint(final GC gc, final Object value) {
		final GridColumn column = (GridColumn) value;
		final GridNativeLook look = GridNativeLook.of(column.getParent());
		final Rectangle b = getBounds();
		final int state = isMouseDown() && isHover() ? GridNativeTheme.HEADER_PRESSED
				: isHover() ? GridNativeTheme.HEADER_HOT : GridNativeTheme.HEADER_NORMAL;
		if (!theme.draw(gc, GridNativeTheme.HEADER, GridNativeTheme.HEADER_ITEM, state, b, null)) {
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			gc.fillRectangle(b);
			gc.setForeground(SEPARATOR);
			gc.drawLine(b.x + b.width - 1, b.y, b.x + b.width - 1, b.y + b.height - 1);
		}
		gc.setFont(column.getHeaderFont());
		int x = b.x + look.headerTextInset();
		final Image image = column.getImage();
		if (image != null) {
			gc.drawImage(image, x, b.y + (b.height - image.getBounds().height) / 2);
			x += image.getBounds().width + look.listTextInset;
		}
		final int width = b.x + b.width - x - look.headerTextInset();
		if (width > 0) {
			gc.setForeground(getDisplay().getSystemColor(
					column.getParent().isEnabled() ? SWT.COLOR_LIST_FOREGROUND : SWT.COLOR_WIDGET_DISABLED_FOREGROUND));
			if (isWordWrap()) {
				// the native header has one line; a Grid header that wraps draws its lines itself
				final TextLayout layout = wrapped(gc, column, width);
				try {
					layout.draw(gc, x, b.y + Math.max(0, (b.height - layout.getBounds().height) / 2));
				} finally {
					layout.dispose();
				}
				return;
			}
			final String text = GridNativeCellRenderer.shorten(gc, column.getText(), width);
			final int extent = gc.textExtent(text).x;
			final int alignment = column.getAlignment();
			final int left = alignment == SWT.RIGHT ? x + width - extent : alignment == SWT.CENTER ? x + (width - extent) / 2 : x;
			// the themed text lands one pixel right of its rectangle
			if (alignment != SWT.LEFT || !theme.drawText(gc, GridNativeTheme.HEADER,
					GridNativeTheme.HEADER_ITEM, state, text, new Rectangle(left - 1, b.y, width + 1, b.height))) {
				gc.drawString(text, left, b.y + look.textTop(b.height), true);
			}
		}
	}

	@Override
	public Point computeSize(final GC gc, final int wHint, final int hHint, final Object value) {
		final GridColumn column = (GridColumn) value;
		final GridNativeLook look = GridNativeLook.of(column.getParent());
		gc.setFont(column.getHeaderFont());
		final Image image = column.getImage();
		final int width = 2 * look.listTextInset + gc.textExtent(column.getText()).x
				+ (image == null ? 0 : image.getBounds().width + look.listTextInset);
		// a taller header font or image makes the header taller, as the native header grows
		int text = gc.getFontMetrics().getHeight();
		if (isWordWrap()) {
			final int available = (wHint == SWT.DEFAULT ? getBounds().width : wHint) - 2 * look.headerTextInset()
					- (image == null ? 0 : image.getBounds().width + look.listTextInset);
			final TextLayout layout = wrapped(gc, column, available);
			try {
				text = layout.getBounds().height;
			} finally {
				layout.dispose();
			}
		}
		final int content = Math.max(text, image == null ? 0 : image.getBounds().height);
		return new Point(width, Math.max(look.headerHeight, content + look.headerHeight - look.fontHeight));
	}

	/* the column text wrapped at the width, in the header font (the caller disposes it) */
	private static TextLayout wrapped(final GC gc, final GridColumn column, final int width) {
		final TextLayout layout = new TextLayout(gc.getDevice());
		layout.setFont(column.getHeaderFont());
		layout.setText(column.getText());
		layout.setWidth(Math.max(1, width));
		layout.setAlignment(column.getAlignment());
		return layout;
	}

	@Override
	public boolean notify(final int event, final Point point, final Object value) {
		return false;
	}

	@Override
	public Rectangle getTextBounds(final Object value, final boolean preferred) {
		final GridColumn column = (GridColumn) value;
		final GridNativeLook look = GridNativeLook.of(column.getParent());
		final Rectangle b = getBounds();
		return new Rectangle(look.listTextInset, look.textTop(b.height), b.width - 2 * look.listTextInset, look.fontHeight);
	}
}
