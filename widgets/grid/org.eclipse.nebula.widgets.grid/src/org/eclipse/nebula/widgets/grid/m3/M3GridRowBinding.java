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

import java.util.Arrays;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.m3.M3RowChange;
import org.eclipse.swt.m3.M3RowView;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Drives a virtual {@link Grid} from an {@link M3RowView} (enhanced M3 API for the Grid).
 * <p>
 * The grid's item count follows the view and each <code>SWT.SetData</code> request fills the item
 * from the source row the view maps it to. A change of the view reaches the grid as a patch: the
 * item count carries the extent (scroll bar) and only the damaged rows are cleared, so only
 * resident, damaged rows are asked again. While the grid is hidden (it or an ancestor), changes
 * only mark it dirty, and it is redone once when it shows. The selection is kept by source row: the rows the user
 * selected stay selected wherever the view moves them, as long as the view shows them. The
 * binding keeps no copy of the rows (one model: the view's lanes).
 * </p>
 *
 * @since 1.2
 */
public final class M3GridRowBinding {

	/** Fills a grid item from a source row. */
	@FunctionalInterface
	public interface RowFill {
		/**
		 * Fills the item.
		 *
		 * @param item the item to fill
		 * @param sourceRow the source row it shows
		 */
		void fill(GridItem item, int sourceRow);
	}

	private static final int[] NONE = {};

	private final Grid grid;
	private final Display display;
	private final M3RowView view;
	private final Listener setData, selection, visibility = this::visibility, paint = this::paint;
	private final M3RowView.Listener changed = this::viewChanged;
	private int[] selected = NONE;
	/* ancestry snapshot for detecting reparenting only; visibility always walks the current parents */
	private Control[] path = {};
	/* shown: the viewport is visible; dirty: the view changed while it was hidden */
	private boolean shown, dirty;
	/* grid refresh passes (read by the hidden-viewport test) */
	int refreshes;

	private M3GridRowBinding(final Grid grid, final M3RowView view, final RowFill fill) {
		this.grid = grid;
		display = grid.getDisplay();
		this.view = view;
		setData = event -> {
			if (event.index < view.size()) {
				fill.fill((GridItem) event.item, view.map(event.index));
			}
		};
		selection = event -> capture();
		grid.addListener(SWT.SetData, setData);
		grid.addListener(SWT.Selection, selection);
		grid.addListener(SWT.DefaultSelection, selection);
		grid.addListener(SWT.Dispose, event -> unhook());
		view.addListener(changed);
		updatePath();
		shown = grid.isVisible();
		display.addFilter(SWT.Show, visibility);
		display.addFilter(SWT.Hide, visibility);
		display.addFilter(SWT.Paint, paint);
	}

	private void unhook() {
		view.removeListener(changed);
		if (!display.isDisposed()) {
			display.removeFilter(SWT.Show, visibility);
			display.removeFilter(SWT.Hide, visibility);
			display.removeFilter(SWT.Paint, paint);
		}
		path = new Control[0];
	}

	/* No array is allocated unless setParent changed the grid's ancestry. Stop at its own shell. */
	private boolean updatePath() {
		int length = 0;
		boolean same = true;
		for (Control c = grid; c != null; c = c instanceof Shell ? null : c.getParent()) {
			if (length >= path.length || path[length] != c) {
				same = false;
			}
			length++;
		}
		if (same && length == path.length) {
			return false;
		}
		path = new Control[length];
		int index = 0;
		for (Control c = grid; c != null; c = c instanceof Shell ? null : c.getParent()) {
			path[index++] = c;
		}
		return true;
	}

	/*
	 * A change of the view: a patch of the grid while it shows; while it is hidden the view's lanes are already up to
	 * date, so the grid is only marked dirty and redone once when it shows (brief rule 24).
	 */
	private void viewChanged(final M3RowChange change) {
		if (grid.isDisposed()) {
			return;
		}
		/* Keep Show/Hide's requested state during callbacks unless the ancestry actually changed. */
		if (updatePath()) {
			shown = grid.isVisible();
		}
		if (shown) {
			if (dirty) {
				renumber(change);
				refreshDirty();
			} else {
				refreshes++;
				refresh(change);
			}
			return;
		}
		dirty = true;
		renumber(change);
	}

	/* Show and Hide are sent before visibility changes; only the current ancestry is relevant. */
	private void visibility(final Event event) {
		if (grid.isDisposed()) {
			return;
		}
		boolean related = false, visible = true;
		for (Control c = grid; c != null; c = c instanceof Shell ? null : c.getParent()) {
			if (c == event.widget) {
				related = true;
				if (event.type == SWT.Hide) {
					visible = false;
				}
			} else if (!c.getVisible()) {
				visible = false;
			}
		}
		if (!related) {
			return;
		}
		updatePath();
		shown = visible;
		refreshDirty();
	}

	/* setParent can reveal dirty rows without Show. Filters run before Grid's existing paint listener. */
	private void paint(final Event event) {
		if (event.widget == grid && !grid.isDisposed() && updatePath()) {
			shown = grid.isVisible();
			refreshDirty();
		}
	}

	private void refreshDirty() {
		if (shown && dirty && !grid.isDisposed()) {
			dirty = false;
			refreshes++;
			grid.setItemCount(view.size());
			grid.clearAll(false);
			restore();
		}
	}

	/**
	 * Drives a virtual grid from a view.
	 *
	 * @param grid a grid with <code>SWT.VIRTUAL</code>
	 * @param view the view
	 * @param fill fills an item from a source row
	 * @return the binding
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if an argument is null</li>
	 *    <li>ERROR_INVALID_ARGUMENT - if the grid is not virtual</li>
	 * </ul>
	 * @exception org.eclipse.swt.SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the grid has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the grid</li>
	 * </ul>
	 */
	public static M3GridRowBinding bind(final Grid grid, final M3RowView view, final RowFill fill) {
		if (grid == null || view == null || fill == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if ((grid.getStyle() & SWT.VIRTUAL) == 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		final M3GridRowBinding binding = new M3GridRowBinding(grid, view, fill);
		grid.setItemCount(view.size());
		grid.clearAll(false);
		return binding;
	}

	/* the grid's selection as source rows (O(selected)) */
	private void capture() {
		final int[] indices = grid.getSelectionIndices();
		selected = new int[indices.length];
		for (int i = 0; i < indices.length; i++) {
			selected[i] = view.map(indices[i]);
		}
	}

	private void refresh(final M3RowChange change) {
		if (grid.isDisposed()) {
			return;
		}
		final int size = view.size();
		if (grid.getItemCount() != size) {
			grid.setItemCount(size);
		}
		if (change.firstDamaged() >= 0 && change.firstDamaged() < size) {
			grid.clear(change.firstDamaged(), Math.min(change.lastDamaged(), size - 1), false);
		}
		renumber(change);
		restore();
	}

	/* source rows move with insertions and removals of the source */
	private void renumber(final M3RowChange change) {
		final int kind = change.kind(), start = change.start(), count = change.count();
		if (kind != M3RowChange.INSERTED && kind != M3RowChange.REMOVED) {
			return;
		}
		int kept = 0;
		for (final int row : selected) {
			if (row < start) {
				selected[kept++] = row;
			} else if (kind == M3RowChange.INSERTED) {
				selected[kept++] = row + count;
			} else if (row >= start + count) {
				selected[kept++] = row - count;
			}
		}
		selected = Arrays.copyOf(selected, kept);
	}

	/* selects the view rows of the selected source rows */
	private void restore() {
		final int[] indices = new int[selected.length];
		int count = 0;
		for (final int row : selected) {
			final int index = row < view.sourceSize() ? view.unmap(row) : -1;
			if (index >= 0) {
				indices[count++] = index;
			}
		}
		if (count == 0) {
			grid.deselectAll();
		} else {
			grid.setSelection(Arrays.copyOf(indices, count));
		}
	}

	/**
	 * Returns the selected source rows, as last selected by the user or set with
	 * {@link #setSelectedRows(int...)}.
	 *
	 * @return a copy of the selected source rows
	 */
	public int[] getSelectedRows() {
		return selected.clone();
	}

	/**
	 * Selects source rows; the rows the view does not show stay selected and appear selected
	 * when the view shows them again.
	 *
	 * @param sourceRows the source rows
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the rows are null</li>
	 * </ul>
	 */
	public void setSelectedRows(final int... sourceRows) {
		if (sourceRows == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		selected = sourceRows.clone();
		restore();
	}

	/**
	 * Returns the view.
	 *
	 * @return the view
	 */
	public M3RowView getView() {
		return view;
	}

	/** Stops driving the grid; the grid keeps its items. */
	public void dispose() {
		unhook();
		if (grid.isDisposed()) {
			return;
		}
		grid.removeListener(SWT.SetData, setData);
		grid.removeListener(SWT.Selection, selection);
		grid.removeListener(SWT.DefaultSelection, selection);
	}
}
