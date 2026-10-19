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
 *
 * Out-of-tree measurement probe for docs/m3/GRID_MEDIATED_VIEWPORT.md (compile against the Grid classes and SWT).
 *******************************************************************************/
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** Measures building a tree Grid of roots x 99 children. Args: roots. */
public class BuildProbe {
	public static void main(String[] args) {
		int roots = Integer.parseInt(args[0]);
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(5, 5, 320, 380);
		GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setTree(true);
		column.setWidth(200);
		long t0 = System.nanoTime();
		for (int r = 0; r < roots; r++) {
			GridItem root = new GridItem(grid, SWT.NONE);
			root.setText("r" + r);
			for (int c = 0; c < 99; c++) {
				new GridItem(root, SWT.NONE).setText("r" + r + "/" + c);
			}
		}
		long t1 = System.nanoTime();
		System.out.println("mode " + System.getProperty("swt.widget.mode.Grid") + ": " + roots * 100 + " items in " + (t1 - t0) / 1_000_000 + " ms");
		shell.dispose();
	}
}
