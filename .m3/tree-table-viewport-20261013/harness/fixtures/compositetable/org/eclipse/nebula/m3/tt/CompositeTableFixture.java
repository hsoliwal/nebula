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

import org.eclipse.nebula.widgets.compositetable.CompositeTable;
import org.eclipse.nebula.widgets.compositetable.ResizableGridRowLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/** CompositeTable with a one-Text row prototype over 40 rows (the CompositeTableTest shape). */
public final class CompositeTableFixture implements TtFixture {
	private CompositeTable table;

	/** Row prototype. */
	public static final class Row extends Composite {
		final Text text;

		public Row(Composite parent, int style) {
			super(parent, style);
			setLayout(new ResizableGridRowLayout());
			text = new Text(this, SWT.BORDER | SWT.SINGLE);
		}

		@Override
		public String toString() {
			return text.isDisposed() ? "disposed" : text.getText();
		}
	}

	@Override
	public void build(Composite parent) {
		table = new CompositeTable(parent, SWT.NONE);
		new Row(table, SWT.NONE);
		table.addRowContentProvider((sender, offset, row) -> ((Row) row).text.setText("word " + offset));
		table.setNumRowsInCollection(40);
		table.setRunTime(true);
	}

	@Override
	public Object instanceOf(Class<?> type) {
		if (table == null || table.isDisposed()) {
			return null;
		}
		if (type == Object.class) {
			return "probe";
		}
		Control[] rows = table.getRowControls();
		Object[] candidates = { table, rows.length > 0 ? rows[0] : null };
		for (Object c : candidates) {
			if (c != null && type.isInstance(c)) {
				return c;
			}
		}
		return null;
	}

	@Override
	public String idOf(Object o) {
		if (o == table) {
			return "table";
		}
		if (o instanceof Row) {
			return "row:" + o;
		}
		return null;
	}

	@Override
	public Object primary() {
		return table;
	}
}
