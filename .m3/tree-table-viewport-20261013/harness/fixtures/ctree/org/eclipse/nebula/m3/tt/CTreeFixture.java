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

import org.eclipse.nebula.widgets.ctree.CTree;
import org.eclipse.nebula.widgets.ctree.CTreeCell;
import org.eclipse.nebula.widgets.ctree.CTreeColumn;
import org.eclipse.nebula.widgets.ctree.CTreeItem;
import org.eclipse.nebula.widgets.ctree.SColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/** CTree with 2 columns, 30 roots x 3 children, root 0 expanded, item 1 selected. */
public final class CTreeFixture implements TtFixture {
	private CTree tree;
	private CTreeItem first;
	private CTreeItem child;

	@Override
	public void build(Composite parent) {
		tree = new CTree(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		CTreeColumn a = new CTreeColumn(tree, SWT.LEFT);
		a.setText("Name");
		a.setWidth(140);
		CTreeColumn b = new CTreeColumn(tree, SWT.LEFT);
		b.setText("Value");
		b.setWidth(80);
		tree.setHeaderVisible(true);
		for (int i = 0; i < 30; i++) {
			CTreeItem root = new CTreeItem(tree, SWT.NONE);
			root.setText(new String[] { "root " + i, Integer.toString(i * 7 % 13) });
			for (int j = 0; j < 3; j++) {
				CTreeItem c = new CTreeItem(root, SWT.NONE);
				c.setText(new String[] { "child " + i + "." + j, Integer.toString(j) });
			}
		}
		first = tree.getItem(0);
		child = first.getItems()[0];
		first.setExpanded(true);
		tree.setSelection(tree.getItem(1));
	}

	@Override
	public Object instanceOf(Class<?> type) {
		if (tree == null || tree.isDisposed()) {
			return null;
		}
		if (type == Object.class) {
			return first;
		}
		Object[] candidates = { tree, child, first, first.getCell(0), columnZero(), tree.getColors() };
		for (Object c : candidates) {
			if (c != null && type.isInstance(c)) {
				return c;
			}
		}
		return null;
	}

	private CTreeColumn columnZero() {
		try {
			java.lang.reflect.Method m = CTree.class.getDeclaredMethod("getColumn", int.class);
			m.setAccessible(true);
			return (CTreeColumn) m.invoke(tree, 0);
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	public String idOf(Object o) {
		if (o == tree) {
			return "tree";
		}
		if (o instanceof CTreeItem) {
			CTreeItem item = (CTreeItem) o;
			StringBuilder path = new StringBuilder();
			while (item != null) {
				CTreeItem parent = item.getParentItem();
				int index = parent == null ? tree.indexOf(item) : indexIn(parent, item);
				path.insert(0, "/" + index);
				item = parent;
			}
			return "item" + path;
		}
		if (o instanceof CTreeCell) {
			for (CTreeItem owner : new CTreeItem[] { first, child }) {
				int index = indexOfCell(owner, (CTreeCell) o);
				if (index >= 0) {
					return "cell:" + idOf(owner) + "#" + index;
				}
			}
			return null;
		}
		if (o instanceof CTreeColumn) {
			return "col:" + ((CTreeColumn) o).getText();
		}
		if (o instanceof SColors) {
			return "colors";
		}
		return null;
	}

	private static int indexIn(CTreeItem parent, CTreeItem item) {
		CTreeItem[] items = parent.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i] == item) {
				return i;
			}
		}
		return -1;
	}

	private static int indexOfCell(CTreeItem owner, CTreeCell cell) {
		if (owner.isDisposed()) {
			return -1;
		}
		CTreeCell[] cells = owner.getCells();
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] == cell) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Object primary() {
		return tree;
	}
}
