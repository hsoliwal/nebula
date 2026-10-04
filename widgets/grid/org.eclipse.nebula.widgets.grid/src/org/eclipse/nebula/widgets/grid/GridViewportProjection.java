/*******************************************************************************
 * Copyright (c) 2026 Eclipse Nebula contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Package-private viewport projection atom for {@link Grid}.
 *
 * <p>It owns only coordinate-to-visible-object projection. It does not retain
 * widget state, listeners, native resources or scroll bars.</p>
 */
final class GridViewportProjection {

	private GridViewportProjection() {
	}

	static GridItem[] visibleItems(List<GridItem> items, int topIndex, int bottomIndex) {
		if (topIndex > bottomIndex || items.isEmpty()) {
			return new GridItem[0];
		}
		GridItem[] visible = new GridItem[bottomIndex - topIndex + 1];
		for (int index = topIndex; index <= bottomIndex; index++) {
			visible[index - topIndex] = items.get(index);
		}
		return visible;
	}

	static GridColumn[] visibleColumns(List<GridColumn> columns, int startIndex, int endIndex) {
		if (startIndex > endIndex || columns.isEmpty()) {
			return new GridColumn[0];
		}
		List<GridColumn> visible = new ArrayList<>();
		for (int index = startIndex; index <= endIndex; index++) {
			GridColumn column = columns.get(index);
			if (column.isVisible()) {
				visible.add(column);
			}
		}
		return visible.toArray(new GridColumn[visible.size()]);
	}

	static int endColumnIndex(List<GridColumn> columns, int startIndex, int x, int clientWidth) {
		int endIndex = -1;
		for (int index = startIndex; index < columns.size(); index++) {
			endIndex = index;
			GridColumn column = columns.get(index);
			if (column.isVisible()) {
				x += column.getWidth();
			}
			if (x > clientWidth) {
				break;
			}
		}
		return Math.max(0, endIndex);
	}

	static GridColumn columnAt(
			List<GridColumn> columns,
			int pointX,
			boolean rowHeaderVisible,
			int rowHeaderWidth,
			boolean fixedOverlayActive,
			int horizontalSelectionPixels) {
		int x = 0;
		if (rowHeaderVisible) {
			if (pointX <= rowHeaderWidth) {
				return null;
			}
			x += rowHeaderWidth;
		}

		if (fixedOverlayActive) {
			int fixedX = rowHeaderVisible ? rowHeaderWidth : 0;
			for (GridColumn column : columns) {
				if (!column.isVisible()) {
					continue;
				}
				if (!column.isFixed()) {
					break;
				}
				if (pointX >= fixedX && pointX < fixedX + column.getWidth()) {
					return column;
				}
				fixedX += column.getWidth();
			}
		}

		x -= horizontalSelectionPixels;
		for (GridColumn column : columns) {
			if (!column.isVisible()) {
				continue;
			}
			if (pointX >= x && pointX < x + column.getWidth()) {
				return column;
			}
			x += column.getWidth();
		}
		return null;
	}
}
