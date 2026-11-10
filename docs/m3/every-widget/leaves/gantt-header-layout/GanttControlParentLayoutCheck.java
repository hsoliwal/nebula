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
 *******************************************************************************/

import org.eclipse.nebula.widgets.ganttchart.GanttControlParent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Acceptance check of the every-widget gantt header layout leaf (ganttchart has no test plugin): laying out a
 * GanttControlParent, also while it is hidden, no longer measures its last child (the tree, whose default size walks
 * every row), and the tree still fills the parent. Run with the bundle classes and an SWT jar on the class path:
 * java GanttControlParentLayoutCheck. Prints PASSED or throws.
 */
public class GanttControlParentLayoutCheck {

	static int measured;

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Composite holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		GanttControlParent parent = new GanttControlParent(holder, SWT.NONE);
		Tree tree = new Tree(parent, SWT.NONE) {
			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {
				measured++;
				return super.computeSize(wHint, hHint, changed);
			}

			@Override
			protected void checkSubclass() {
				// counting subclass for the check
			}
		};
		for (int i = 0; i < 2000; i++) {
			new TreeItem(tree, SWT.NONE).setText("row " + i);
		}
		shell.setSize(300, 300);
		shell.open();
		holder.setVisible(false);
		measured = 0;
		for (int width = 310; width <= 400; width += 10) {
			shell.setSize(width, 300);
		}
		System.out.println("tree measurements in 10 hidden layouts: " + measured);
		check(measured == 0, "layouts do not measure the last child (" + measured + " measurements)");

		holder.setVisible(true);
		parent.layout(true);
		Rectangle area = parent.getClientArea();
		Rectangle bounds = tree.getBounds();
		check(bounds.x == 0 && bounds.width == area.width && bounds.y + bounds.height == area.height,
				"the tree fills the parent below the header (" + bounds + " in " + area + ")");
		shell.dispose();
		System.out.println("PASSED");
	}

	static void check(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
