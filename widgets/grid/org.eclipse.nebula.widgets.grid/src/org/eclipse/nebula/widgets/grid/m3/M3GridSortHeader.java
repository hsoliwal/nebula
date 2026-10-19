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
package org.eclipse.nebula.widgets.grid.m3;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.m3.M3ColumnSort;
import org.eclipse.swt.m3.M3RowChange;
import org.eclipse.swt.m3.M3RowView;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Connects the column headers of a {@link Grid} to an {@link M3ColumnSort} (enhanced M3 API for
 * the Grid).
 * <p>
 * A header click sorts by the column (ascending, descending, unsorted); a Shift+click adds the
 * column as the next sort key. Every sorted column shows its direction through
 * {@link GridColumn#setSort(int)}; the grid's own header renderer draws the arrow. The columns of
 * the grid are the columns of the sort, in creation order; installing again picks up columns
 * created since.
 * </p>
 *
 * @since 1.2
 */
public final class M3GridSortHeader implements Listener, M3RowView.Listener {

	private static final String KEY = "org.eclipse.nebula.widgets.grid.m3.M3GridSortHeader"; //$NON-NLS-1$

	private final M3ColumnSort sort;
	private final Grid grid;

	private M3GridSortHeader(final M3ColumnSort sort, final Grid grid) {
		this.sort = sort;
		this.grid = grid;
	}

	/**
	 * Connects the column headers of a grid to a sort.
	 *
	 * @param grid the grid
	 * @param sort the sort of the grid's rows
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if an argument is null</li>
	 * </ul>
	 * @exception org.eclipse.swt.SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the grid has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the grid</li>
	 * </ul>
	 */
	public static void install(final Grid grid, final M3ColumnSort sort) {
		if (grid == null || sort == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		final Object old = grid.getData(KEY);
		M3GridSortHeader header = old instanceof M3GridSortHeader && ((M3GridSortHeader) old).sort == sort ? (M3GridSortHeader) old : null;
		if (header == null) {
			if (old instanceof M3GridSortHeader) {
				((M3GridSortHeader) old).uninstall();
			}
			header = new M3GridSortHeader(sort, grid);
			grid.setData(KEY, header);
			sort.addListener(header);
			grid.addListener(SWT.Dispose, header);
		}
		for (int i = 0, n = grid.getColumnCount(); i < n; i++) {
			final GridColumn column = grid.getColumn(i);
			if (column.getData(KEY) != header) {
				column.setData(KEY, header);
				column.addListener(SWT.Selection, header);
			}
		}
		header.indicate();
	}

	private void uninstall() {
		sort.removeListener(this);
		grid.removeListener(SWT.Dispose, this);
		for (int i = 0, n = grid.getColumnCount(); i < n; i++) {
			final GridColumn column = grid.getColumn(i);
			column.removeListener(SWT.Selection, this);
			column.setData(KEY, null);
		}
	}

	@Override
	public void handleEvent(final Event event) {
		if (event.type == SWT.Dispose) {
			sort.removeListener(this);
			return;
		}
		final int column = grid.indexOf((GridColumn) event.widget);
		if (column >= 0 && column < sort.getColumnCount()) {
			sort.toggle(column, (event.stateMask & SWT.SHIFT) != 0);
		}
	}

	@Override
	public void viewChanged(final M3RowChange change) {
		if (change.kind() == M3RowChange.ORDER && !grid.isDisposed()) {
			indicate();
		}
	}

	/* every sorted column shows its direction; the others show none */
	private void indicate() {
		for (int i = 0, n = Math.min(grid.getColumnCount(), sort.getColumnCount()); i < n; i++) {
			final GridColumn column = grid.getColumn(i);
			final int direction = sort.getDirection(i);
			if (column.getSort() != direction) {
				column.setSort(direction);
			}
		}
	}
}
