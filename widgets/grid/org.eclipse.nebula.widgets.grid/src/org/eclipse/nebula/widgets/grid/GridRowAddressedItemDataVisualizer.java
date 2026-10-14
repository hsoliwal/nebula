/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * Default Grid visual data owner keyed by stable logical row coordinates.
 *
 * <p>The public DataVisualizer API remains GridItem-based. Internally, however,
 * no GridItem is retained as a map key. This lets the Grid parent own payload
 * independently from an optional SWT.VIRTUAL facade.</p>
 */
final class GridRowAddressedItemDataVisualizer extends GridItemDataVisualizer
		implements GridRowAddressedDataVisualizer {

	private final RowStore<Color> backgrounds = new RowStore<>();
	private final RowStore<Color> foregrounds = new RowStore<>();
	private final RowStore<Boolean> checkables = new RowStore<>();
	private final RowStore<Boolean> checkeds = new RowStore<>();
	private final RowStore<Boolean> grayeds = new RowStore<>();
	private final RowStore<Integer> columnSpans = new RowStore<>();
	private final RowStore<Integer> rowSpans = new RowStore<>();
	private final RowStore<Font> fonts = new RowStore<>();
	private final RowStore<Image> images = new RowStore<>();
	private final RowStore<String> texts = new RowStore<>();
	private final RowStore<String> toolTipTexts = new RowStore<>();

	GridRowAddressedItemDataVisualizer(
			Color defaultBackground, Color defaultForeground, Font defaultFont) {
		super(defaultBackground, defaultForeground, defaultFont);
	}

	@Override
	public Color getBackground(GridItem item, int column) {
		return backgrounds.get(item, column, getDefaultBackground());
	}

	@Override
	public void setBackground(GridItem item, int column, Color color) {
		backgrounds.put(item, column, color);
	}

	@Override
	public boolean getCheckable(GridItem item, int column) {
		return checkables.get(item, column, Boolean.TRUE);
	}

	@Override
	public void setCheckable(GridItem item, int column, boolean checkable) {
		checkables.put(item, column, checkable);
	}

	@Override
	public boolean getChecked(GridItem item, int column) {
		return checkeds.get(item, column, Boolean.FALSE);
	}

	@Override
	public void setChecked(GridItem item, int column, boolean checked) {
		checkeds.put(item, column, checked);
	}

	@Override
	public int getColumnSpan(GridItem item, int column) {
		return columnSpans.get(item, column, 0);
	}

	@Override
	public void setColumnSpan(GridItem item, int column, int span) {
		columnSpans.put(item, column, span);
	}

	@Override
	public int getRowSpan(GridItem item, int column) {
		return rowSpans.get(item, column, 0);
	}

	@Override
	public void setRowSpan(GridItem item, int column, int span) {
		rowSpans.put(item, column, span);
		item.parentGrid().rowSpanChanged(item, column, span);
	}

	@Override
	public Font getFont(GridItem item, int column) {
		return fonts.get(item, column, getDefaultFont());
	}

	@Override
	public void setFont(GridItem item, int column, Font font) {
		fonts.put(item, column, font);
	}

	@Override
	public Color getForeground(GridItem item, int column) {
		return foregrounds.get(item, column, getDefaultForeground());
	}

	@Override
	public void setForeground(GridItem item, int column, Color foreground) {
		foregrounds.put(item, column, foreground);
	}

	@Override
	public boolean getGrayed(GridItem item, int column) {
		return grayeds.get(item, column, Boolean.FALSE);
	}

	@Override
	public void setGrayed(GridItem item, int column, boolean grayed) {
		grayeds.put(item, column, grayed);
	}

	@Override
	public Image getImage(GridItem item, int column) {
		return images.get(item, column, null);
	}

	@Override
	public void setImage(GridItem item, int column, Image image) {
		images.put(item, column, image);
	}

	@Override
	public String getText(GridItem item, int column) {
		return texts.get(item, column, "");
	}

	@Override
	public void setText(GridItem item, int column, String text) {
		texts.put(item, column, text);
	}

	@Override
	public String getToolTipText(GridItem item, int column) {
		return toolTipTexts.get(item, column, null);
	}

	@Override
	public void setToolTipText(GridItem item, int column, String tooltip) {
		toolTipTexts.put(item, column, tooltip);
	}

	@Override
	public void clearRow(GridItem item) {
		item.parentGrid().rowSpanOwnerDisposed(item);
		forEachStore(store -> store.clearRow(item.getParent(), item.getRowIndex()));
	}

	@Override
	public void clearColumn(int column) {
		Grid[] spanParents = rowSpans.grids();
		forEachStore(store -> store.clearColumn(column));
		for (Grid parent : spanParents) parent.rowSpanDataColumnRemoved(column);
	}

	@Override
	public void addColumn(int column) {
		Grid[] spanParents = rowSpans.grids();
		forEachStore(store -> store.addColumn(column));
		for (Grid parent : spanParents) parent.rowSpanDataColumnInserted(column);
	}

	@Override
	public void clearAll() {
		Grid[] spanParents = rowSpans.grids();
		forEachStore(RowStore::clearAll);
		for (Grid parent : spanParents) parent.rowSpanDataCleared();
	}

	@Override
	public void insertRow(Grid grid, int index) {
		forEachStore(store -> store.insertRow(grid, index));
	}

	@Override
	public void removeRow(Grid grid, int index) {
		forEachStore(store -> store.removeRow(grid, index));
	}

	@Override
	public void truncateRows(Grid grid, int count) {
		forEachStore(store -> store.truncateRows(grid, count));
	}

	@Override
	public void clearRows(Grid grid) {
		forEachStore(store -> store.clearRows(grid));
	}

	int storedRowCount(Grid grid) {
		return texts.rowCount(grid);
	}

	private void forEachStore(java.util.function.Consumer<RowStore<?>> action) {
		action.accept(backgrounds);
		action.accept(foregrounds);
		action.accept(checkables);
		action.accept(checkeds);
		action.accept(grayeds);
		action.accept(columnSpans);
		action.accept(rowSpans);
		action.accept(fonts);
		action.accept(images);
		action.accept(texts);
		action.accept(toolTipTexts);
	}

	private static final class RowStore<T> {
		private final IdentityHashMap<Grid, Map<Integer, List<T>>> rows = new IdentityHashMap<>();

		T get(GridItem item, int column, T defaultValue) {
			if (column < 0) return defaultValue;
			Map<Integer, List<T>> gridRows = rows.get(item.getParent());
			if (gridRows == null) return defaultValue;
			List<T> values = gridRows.get(item.getRowIndex());
			if (values == null || column >= values.size()) return defaultValue;
			T value = values.get(column);
			return value == null ? defaultValue : value;
		}

		void put(GridItem item, int column, T value) {
			if (column < 0) throw new IndexOutOfBoundsException("column=" + column);
			Map<Integer, List<T>> gridRows =
					rows.computeIfAbsent(item.getParent(), ignored -> new HashMap<>());
			List<T> values = gridRows.computeIfAbsent(item.getRowIndex(), ignored -> new ArrayList<>());
			while (column > values.size()) values.add(null);
			if (values.size() > column) values.set(column, value);
			else values.add(value);
		}

		void clearRow(Grid grid, int row) {
			Map<Integer, List<T>> gridRows = rows.get(grid);
			if (gridRows == null) return;
			gridRows.remove(row);
			if (gridRows.isEmpty()) rows.remove(grid);
		}

		void insertRow(Grid grid, int index) {
			remap(grid, index, true);
		}

		void removeRow(Grid grid, int index) {
			remap(grid, index, false);
		}

		private void remap(Grid grid, int index, boolean inserting) {
			Map<Integer, List<T>> gridRows = rows.get(grid);
			if (gridRows == null || gridRows.isEmpty()) return;
			Map<Integer, List<T>> shifted = new HashMap<>(Math.max(4, gridRows.size() * 2));
			for (Map.Entry<Integer, List<T>> entry : gridRows.entrySet()) {
				int row = entry.getKey();
				if (!inserting && row == index) continue;
				int next = inserting
						? (row >= index ? row + 1 : row)
						: (row > index ? row - 1 : row);
				shifted.put(next, entry.getValue());
			}
			if (shifted.isEmpty()) rows.remove(grid);
			else rows.put(grid, shifted);
		}

		void truncateRows(Grid grid, int count) {
			Map<Integer, List<T>> gridRows = rows.get(grid);
			if (gridRows == null) return;
			gridRows.entrySet().removeIf(entry -> entry.getKey() >= count);
			if (gridRows.isEmpty()) rows.remove(grid);
		}

		void clearRows(Grid grid) {
			rows.remove(grid);
		}

		void clearColumn(int column) {
			for (Map<Integer, List<T>> gridRows : rows.values()) {
				for (List<T> values : gridRows.values()) {
					if (column >= 0 && column < values.size()) values.remove(column);
				}
			}
		}

		void addColumn(int column) {
			for (Map<Integer, List<T>> gridRows : rows.values()) {
				for (List<T> values : gridRows.values()) {
					if (!values.isEmpty()) {
						while (values.size() < column) values.add(null);
						values.add(column, null);
					}
				}
			}
		}

		void clearAll() {
			rows.clear();
		}

		int rowCount(Grid grid) {
			Map<Integer, List<T>> gridRows = rows.get(grid);
			return gridRows == null ? 0 : gridRows.size();
		}

		Grid[] grids() {
			return rows.keySet().toArray(new Grid[rows.size()]);
		}
	}
}
