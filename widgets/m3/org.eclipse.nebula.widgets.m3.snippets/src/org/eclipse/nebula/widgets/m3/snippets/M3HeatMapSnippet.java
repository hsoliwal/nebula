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
package org.eclipse.nebula.widgets.m3.snippets;

import org.eclipse.nebula.widgets.m3.M3HeatMap;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * An {@link M3HeatMap} of 1000 x 1000 cells (10^6 values in one float lane, 4 MB): scroll with the bars, click a cell
 * to select it, arrow keys move the selection. Arguments: the grid side (default 1000);
 * <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3HeatMapSnippet {

	public static void main(String[] args) {
		int side = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(1, false));
		M3HeatMap map = new M3HeatMap(shell, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		map.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		map.setCellSize(4, 4);
		long start = System.nanoTime();
		map.setSize(side, side);
		float[] values = new float[side * side];
		for (int row = 0; row < side; row++) {
			for (int column = 0; column < side; column++) {
				double x = column / (double) side, y = row / (double) side;
				values[row * side + column] = (float) (Math.sin(x * 23) * Math.cos(y * 17) + 0.3 * Math.sin((x + y) * 61));
			}
		}
		map.setValues(values);
		map.setSelection(side / 2, side / 2);
		map.showCell(side / 2, side / 2);
		Label status = new Label(shell, SWT.NONE);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		status.setText(side + " x " + side + " cells, " + (System.nanoTime() - start) / 1_000_000 + " ms to fill; range "
				+ map.getMinimum() + " .. " + map.getMaximum());
		map.addListener(SWT.Selection, e -> status.setText("cell " + e.y + "," + e.x + " = " + map.getValue(e.y, e.x)));
		shell.setText("M3HeatMap: " + (long) side * side + " cells");
		shell.setSize(640, 480);
		shell.open();
		Screenshot.runOrSave(shell, display);
		display.dispose();
	}
}
