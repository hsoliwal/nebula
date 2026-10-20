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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/** TREE_TABLE on RWT: a virtual Tree with three columns, 100,000 roots of 10 children each. */
public final class TreeTableExample extends RwtExample {

	static final int ROOTS = 100_000;

	public TreeTableExample () {
		super ("TREE_TABLE", "org.eclipse.swt.widgets.Tree");
	}

	@Override
	public Control create (Composite parent) {
		Tree tree = new Tree (parent, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
		tree.setHeaderVisible (true);
		tree.setLinesVisible (true);
		for (String title : new String[] {"Name", "Size", "Owner"}) {
			TreeColumn column = new TreeColumn (tree, SWT.NONE);
			column.setText (title);
			column.setWidth (160);
		}
		tree.addListener (SWT.SetData, event -> {
			TreeItem item = (TreeItem) event.item;
			TreeItem parentItem = item.getParentItem ();
			int row = parentItem == null ? tree.indexOf (item) : parentItem.indexOf (item);
			item.setText (new String[] {RwtSampleData.cell (row, 0), RwtSampleData.cell (row, 1), RwtSampleData.cell (row, 2)});
			if (parentItem == null) item.setItemCount (10);
		});
		tree.setItemCount (ROOTS);
		return tree;
	}

	@Override
	public void change (Control control) {
		Tree tree = (Tree) control;
		tree.setItemCount (tree.getItemCount () + 1);
	}
}
