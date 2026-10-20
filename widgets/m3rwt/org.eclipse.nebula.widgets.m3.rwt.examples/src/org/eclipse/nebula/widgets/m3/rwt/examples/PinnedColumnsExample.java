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

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/** PINNED_COLUMNS on RWT: a virtual Table with 12 columns whose first column stays while scrolling (RWT.FIXED_COLUMNS). */
public final class PinnedColumnsExample extends RwtExample {

	public PinnedColumnsExample () {
		super ("PINNED_COLUMNS", "org.eclipse.swt.widgets.Table (RWT.FIXED_COLUMNS)");
	}

	@Override
	public Control create (Composite parent) {
		Table table = new Table (parent, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setData (RWT.FIXED_COLUMNS, Integer.valueOf (1));
		table.setHeaderVisible (true);
		for (int c = 0; c < 12; c++) {
			TableColumn column = new TableColumn (table, SWT.NONE);
			column.setText (c == 0 ? "Pinned" : "Column " + c);
			column.setWidth (120);
		}
		table.addListener (SWT.SetData, event -> {
			TableItem item = (TableItem) event.item;
			int row = table.indexOf (item);
			for (int c = 0; c < 12; c++) item.setText (c, RwtSampleData.cell (row, c));
		});
		table.setItemCount (100_000);
		return table;
	}

	@Override
	public void change (Control control) {
		Table table = (Table) control;
		table.setItemCount (table.getItemCount () + 1);
	}
}
