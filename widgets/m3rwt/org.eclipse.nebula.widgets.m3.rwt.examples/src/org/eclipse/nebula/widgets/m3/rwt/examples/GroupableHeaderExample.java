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
import org.eclipse.nebula.widgets.grid.GridColumnGroup;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** GROUPABLE_HEADER on RWT: RAP Grid with two column groups over a virtual 100,000-row model. */
public final class GroupableHeaderExample extends RwtExample {

	public GroupableHeaderExample () {
		super ("GROUPABLE_HEADER", "org.eclipse.nebula.widgets.grid.GridColumnGroup (org.eclipse.rap.nebula.widgets.grid 4.8.0)");
	}

	@Override
	public Control create (Composite parent) {
		Grid grid = new Grid (parent, SWT.VIRTUAL | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setHeaderVisible (true);
		for (String name : new String[] {"Address", "Account"}) {
			GridColumnGroup group = new GridColumnGroup (grid, SWT.TOGGLE);
			group.setText (name);
			for (int c = 0; c < 3; c++) {
				GridColumn column = new GridColumn (group, SWT.NONE);
				column.setText (name + " " + c);
				column.setWidth (110);
			}
		}
		grid.addListener (SWT.SetData, event -> {
			GridItem item = (GridItem) event.item;
			int row = grid.indexOf (item);
			for (int c = 0; c < 6; c++) item.setText (c, RwtSampleData.cell (row, c));
		});
		grid.setItemCount (100_000);
		return grid;
	}

	@Override
	public void change (Control control) {
		Grid grid = (Grid) control;
		grid.setItemCount (grid.getItemCount () + 1);
	}
}
