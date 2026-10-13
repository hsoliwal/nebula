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

import org.eclipse.nebula.widgets.ctreecombo.CTreeCombo;
import org.eclipse.nebula.widgets.ctreecombo.CTreeComboColumn;
import org.eclipse.nebula.widgets.ctreecombo.CTreeComboItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/** CTreeCombo with 10 roots x 3 children (no columns), and a second combo with one column. */
public final class CTreeComboFixture implements TtFixture {
	private CTreeCombo combo;
	private CTreeCombo columned;
	private CTreeComboItem first;
	private CTreeComboItem child;
	private CTreeComboColumn column;

	@Override
	public void build(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		combo = new CTreeCombo(c, SWT.BORDER | SWT.READ_ONLY);
		for (int i = 0; i < 10; i++) {
			CTreeComboItem root = new CTreeComboItem(combo, SWT.NONE);
			root.setText("root " + i);
			for (int j = 0; j < 3; j++) {
				new CTreeComboItem(root, SWT.NONE).setText("child " + i + "." + j);
			}
		}
		first = combo.getItems()[0];
		child = first.getItems()[0];
		columned = new CTreeCombo(c, SWT.BORDER);
		column = new CTreeComboColumn(columned, SWT.NONE);
		column.setText("Name");
		column.setWidth(80);
		new CTreeComboItem(columned, SWT.NONE).setText("only");
	}

	@Override
	public Object instanceOf(Class<?> type) {
		if (combo == null || combo.isDisposed()) {
			return null;
		}
		if (type == Object.class) {
			return "root 1";
		}
		Object[] candidates = { combo, child, first, column };
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
		if (o == columned) {
			return "columned";
		}
		if (o == column) {
			return "column";
		}
		if (o instanceof CTreeComboItem) {
			CTreeComboItem item = (CTreeComboItem) o;
			StringBuilder path = new StringBuilder();
			while (item != null) {
				CTreeComboItem p = item.getParentItem();
				int index = p == null ? indexIn(item) : p.indexOf(item);
				path.insert(0, "/" + index);
				item = p;
			}
			return "item" + path;
		}
		return null;
	}

	private int indexIn(CTreeComboItem item) {
		CTreeComboItem[] roots = combo.getItems();
		for (int i = 0; i < roots.length; i++) {
			if (roots[i] == item) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Object primary() {
		return combo;
	}
}
