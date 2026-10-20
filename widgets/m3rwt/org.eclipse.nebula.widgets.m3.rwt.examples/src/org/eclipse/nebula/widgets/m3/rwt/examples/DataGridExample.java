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
package org.eclipse.nebula.widgets.m3.rwt.examples;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** DATA_GRID on RWT: RAP's Nebula Grid port, virtual, 100,000 rows by 6 columns. */
public final class DataGridExample extends RwtExample {

	static final int ROWS = 100_000;

	public DataGridExample () {
		super ("DATA_GRID", "org.eclipse.nebula.widgets.grid.Grid (org.eclipse.rap.nebula.widgets.grid 4.8.0)");
	}

	@Override
	public Control create (Composite parent) {
		Grid grid = new Grid (parent, SWT.VIRTUAL | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		grid.setHeaderVisible (true);
		grid.setLinesVisible (true);
		for (int c = 0; c < 6; c++) {
			GridColumn column = new GridColumn (grid, SWT.NONE);
			column.setText ("Column " + c);
			column.setWidth (120);
		}
		grid.addListener (SWT.SetData, event -> {
			GridItem item = (GridItem) event.item;
			int row = grid.indexOf (item);
			for (int c = 0; c < 6; c++) item.setText (c, RwtSampleData.cell (row, c));
		});
		grid.setItemCount (ROWS);
		return grid;
	}

	@Override
	public void change (Control control) {
		Grid grid = (Grid) control;
		grid.setItemCount (grid.getItemCount () + 1);
	}
}
