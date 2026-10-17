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
 * Out-of-tree probe used in the regression triage (docs/m3/grid-viewport/REGRESSION_TRIAGE.tsv).
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.lang.reflect.Method;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

/** GridScrollbarSettle_Test's sweep, printing the state around the first mismatch. */
public class SettleProbe {
	public static void main(String[] args) throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(800, 700);
		shell.open();
		Grid settled = grid(shell, true), measured = grid(shell, false);
		Random random = new Random(5L);
		Method table = Grid.class.getDeclaredMethod("getTableSize");
		table.setAccessible(true);
		String last = "";
		for (int i = 0; i < 300; i++) {
			int w = 380 + random.nextInt(200), h = 400 + random.nextInt(300);
			String before = state(settled) + " / " + state(measured);
			settled.setSize(w, h);
			measured.setSize(w, h);
			settle(display, settled);
			settle(display, measured);
			if (settled.getVerticalBar().getVisible() != measured.getVerticalBar().getVisible()
					|| settled.getHorizontalBar().getVisible() != measured.getHorizontalBar().getVisible()) {
				System.out.println("i=" + i + " size " + w + "x" + h + " preferred " + table.invoke(settled) + " / " + table.invoke(measured));
				System.out.println("  before (settled / measured): " + before);
				System.out.println("  after  (settled / measured): " + state(settled) + " / " + state(measured));
				System.out.println("  previous step " + last);
				break;
			}
			last = w + "x" + h + " -> " + state(settled);
		}
		display.dispose();
	}

	static String state(Grid g) {
		return "v=" + g.getVerticalBar().getVisible() + " h=" + g.getHorizontalBar().getVisible() + " client=" + g.getClientArea()
				+ " vbar=" + g.getVerticalBar().getSize() + " hbar=" + g.getHorizontalBar().getSize();
	}

	static Grid grid(Shell shell, boolean settle) {
		Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		grid.settleScrollbars = settle;
		grid.setHeaderVisible(true);
		for (int c = 0; c < 6; c++) new GridColumn(grid, SWT.NONE).setWidth(80);
		for (int r = 0; r < 30; r++) new GridItem(grid, SWT.NONE).setText("row " + r);
		return grid;
	}

	static void settle(Display display, Grid grid) {
		grid.redraw();
		grid.update();
		while (display.readAndDispatch()) { }
	}
}
