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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/** Logs cell paints of a Grid with three pinned top rows during one Control.print. */
public class PinnedProbe {
	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(1000, 500);
		new Text(shell, SWT.SINGLE).setBounds(0, 0, 40, 20);
		shell.open();
		Grid grid = new Grid(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(0, 30, 460, 400);
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);
		for (int c = 0; c < 2; c++) {
			GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText("C" + c);
			column.setWidth(90);
			column.setCellRenderer(new DefaultCellRenderer() {
				@Override
				public void paint(GC gc, Object value) {
					System.out.println("paint row " + ((GridItem) value).getText(0) + " col " + getColumn() + " bounds " + getBounds() + " clip " + gc.getClipping() + " advanced " + gc.getAdvanced() + " textAA " + gc.getTextAntialias() + " alpha " + gc.getAlpha());
					super.paint(gc, value);
				}
			});
		}
		grid.getColumn(0).setFixed(true);
		for (int r = 0; r < 5; r++) {
			GridItem item = new GridItem(grid, SWT.NONE);
			for (int c = 0; c < 2; c++) item.setText(c, "r" + r + "c" + c);
		}
		while (display.readAndDispatch()) { }
		grid.setPinnedRowCount(3, 0);
		while (display.readAndDispatch()) { }
		System.out.println("--- print");
		Image image = new Image(display, 460, 400);
		GC gc = new GC(image);
		grid.print(gc);
		gc.dispose();
		ImageData d = image.getImageData();
		for (int y : new int[] { 25, 30, 44, 82, 100 }) System.out.println("y " + y + " x100 " + d.palette.getRGB(d.getPixel(100, y)) + " x150 " + d.palette.getRGB(d.getPixel(150, y)));
		image.dispose();
		display.dispose();
	}
}
