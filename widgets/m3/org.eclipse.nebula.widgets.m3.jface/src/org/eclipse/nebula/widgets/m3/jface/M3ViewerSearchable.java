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
package org.eclipse.nebula.widgets.m3.jface;

import java.util.Arrays;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.nebula.widgets.m3.M3FindBar;
import org.eclipse.nebula.widgets.m3.M3Searchable;
import org.eclipse.swt.SWT;

/**
 * JFace binding of {@link M3FindBar}: an {@link M3Searchable} over a structured viewer's model, in the order the viewer
 * shows it (content provider elements, the viewer's filters, then its comparator), with row texts from the viewer's
 * label provider and the found element selected and revealed through the viewer. Rows are read from the model, never
 * from widget items, so a virtual table viewer is searched without materializing items.
 * <p>
 * The shown order is computed once, on the first read after {@link #modelChanged()} (call it after a refresh, a delta
 * or a filter or sort change, then {@link M3FindBar#modelChanged()}); texts are read per row on demand.
 * </p>
 */
public final class M3ViewerSearchable implements M3Searchable {

	private final StructuredViewer viewer;
	private final int column;
	private Object[] rows;

	private M3ViewerSearchable(StructuredViewer viewer, int column) {
		this.viewer = viewer;
		this.column = column;
	}

	/**
	 * @param viewer a structured viewer with an IStructuredContentProvider
	 * @param column the column whose text is searched (ITableLabelProvider), or -1 for ILabelProvider text
	 * @return the searchable
	 */
	public static M3ViewerSearchable of(StructuredViewer viewer, int column) {
		if (viewer == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		return new M3ViewerSearchable(viewer, column);
	}

	/** Forgets the shown order; the next read computes it from the model again. */
	public void modelChanged() {
		rows = null;
	}

	private Object[] rows() {
		if (rows == null) {
			Object input = viewer.getInput();
			Object[] elements = viewer.getContentProvider() instanceof IStructuredContentProvider
					? ((IStructuredContentProvider) viewer.getContentProvider()).getElements(input)
					: new Object[0];
			ViewerFilter[] filters = viewer.getFilters();
			int kept = 0;
			Object[] shown = new Object[elements.length];
			for (Object element : elements) {
				boolean select = true;
				for (ViewerFilter filter : filters) {
					if (!filter.select(viewer, input, element)) {
						select = false;
						break;
					}
				}
				if (select) {
					shown[kept++] = element;
				}
			}
			shown = Arrays.copyOf(shown, kept);
			ViewerComparator comparator = viewer.getComparator();
			if (comparator != null) {
				comparator.sort(viewer, shown);
			}
			rows = shown;
		}
		return rows;
	}

	@Override
	public int getRowCount() {
		return rows().length;
	}

	@Override
	public String getText(int row) {
		Object element = rows()[row];
		IBaseLabelProvider provider = viewer.getLabelProvider();
		String text = null;
		if (column >= 0 && provider instanceof ITableLabelProvider) {
			text = ((ITableLabelProvider) provider).getColumnText(element, column);
		} else if (provider instanceof ILabelProvider) {
			text = ((ILabelProvider) provider).getText(element);
		}
		return text == null ? "" : text; //$NON-NLS-1$
	}

	@Override
	public void reveal(int row) {
		viewer.setSelection(new StructuredSelection(rows()[row]), true);
	}

	/** @return the element shown at a row (in the viewer's order) */
	public Object getElement(int row) {
		return rows()[row];
	}
}
