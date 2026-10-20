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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/** A virtual Table over {@link RwtSampleData}: the shared body of the table-based examples. */
final class VirtualTables {

	private VirtualTables () {
	}

	static Table create (Composite parent, int style, int rows, int columns) {
		Table table = new Table (parent, style | SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible (columns > 1);
		table.setLinesVisible (true);
		for (int c = 0; c < columns; c++) {
			TableColumn column = new TableColumn (table, SWT.NONE);
			column.setText ("Column " + c);
			column.setWidth (columns > 1 ? 110 : 300);
		}
		table.addListener (SWT.SetData, event -> {
			TableItem item = (TableItem) event.item;
			int row = table.indexOf (item);
			for (int c = 0; c < columns; c++) item.setText (c, RwtSampleData.cell (row, c));
			if ((style & SWT.CHECK) != 0) item.setChecked (row % 3 == 0);
		});
		table.setItemCount (rows);
		return table;
	}
}
