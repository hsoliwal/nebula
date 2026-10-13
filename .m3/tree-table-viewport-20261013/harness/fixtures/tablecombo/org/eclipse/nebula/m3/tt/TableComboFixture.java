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
package org.eclipse.nebula.m3.tt;

import org.eclipse.nebula.widgets.checktablecombo.CheckTableCombo;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/** TableCombo with 2 columns x 20 rows, item 2 selected; a CheckTableCombo with 5 rows. */
public final class TableComboFixture implements TtFixture {
	private TableCombo combo;
	private CheckTableCombo check;

	@Override
	public void build(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		combo = new TableCombo(c, SWT.BORDER | SWT.READ_ONLY);
		Table table = combo.getTable();
		new TableColumn(table, SWT.NONE).setWidth(90);
		new TableColumn(table, SWT.NONE).setWidth(60);
		for (int i = 0; i < 20; i++) {
			new TableItem(table, SWT.NONE).setText(new String[] { "row " + i, Integer.toString(i * 3) });
		}
		combo.setDisplayColumnIndex(0);
		combo.select(2);
		check = new CheckTableCombo(c, SWT.BORDER | SWT.READ_ONLY);
		for (int i = 0; i < 5; i++) {
			new TableItem(check.getTable(), SWT.NONE).setText("check " + i);
		}
	}

	@Override
	public Object instanceOf(Class<?> type) {
		if (combo == null || combo.isDisposed()) {
			return null;
		}
		if (type == Object.class) {
			return "row 3";
		}
		Table t = combo.getTable();
		Object[] candidates = { combo, check, t, t.getItemCount() > 0 ? t.getItem(0) : null,
				t.getColumnCount() > 0 ? t.getColumn(0) : null };
		for (Object c : candidates) {
			if (c != null && type.isInstance(c)) {
				return c;
			}
		}
		return null;
	}

	@Override
	public String idOf(Object o) {
		if (o == combo) {
			return "combo";
		}
		if (o == check) {
			return "check";
		}
		if (o instanceof Table) {
			return o == combo.getTable() ? "table" : "checkTable";
		}
		if (o instanceof TableItem) {
			TableItem item = (TableItem) o;
			return "item" + item.getParent().indexOf(item);
		}
		if (o instanceof TableColumn) {
			TableColumn col = (TableColumn) o;
			return "col" + col.getParent().indexOf(col);
		}
		return null;
	}

	@Override
	public Object primary() {
		return combo;
	}
}
