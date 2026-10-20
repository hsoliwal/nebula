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

import org.eclipse.nebula.widgets.m3.M3GradientChooser;
import org.eclipse.nebula.widgets.m3.M3HeatMap;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * An {@link M3GradientChooser} driving the palette of an {@link M3HeatMap}: click under the bar to add a stop, drag
 * a marker, double-click it to recolour, Delete removes the selected stop; the heat map repaints with the sampled
 * palette. <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3GradientChooserSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3GradientChooser");
		shell.setLayout(new GridLayout(1, false));
		M3GradientChooser chooser = new M3GradientChooser(shell, SWT.NONE);
		chooser.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		chooser.setStops(new float[] { 0f, 0.3f, 0.6f, 1f }, new RGB[] { new RGB(20, 20, 90), new RGB(0, 170, 200), new RGB(250, 220, 40), new RGB(190, 20, 20) });
		M3HeatMap map = new M3HeatMap(shell, SWT.BORDER);
		map.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		map.setCellSize(6, 6);
		map.setGridSize(60, 100);
		float[] values = new float[60 * 100];
		for (int row = 0; row < 60; row++) {
			for (int column = 0; column < 100; column++) {
				values[row * 100 + column] = (float) (Math.sin(column / 9.0) * Math.cos(row / 7.0));
			}
		}
		map.setValues(values);
		Runnable apply = () -> {
			int[] table = chooser.toLookupTable(256);
			RGB[] palette = new RGB[table.length];
			for (int i = 0; i < table.length; i++) {
				palette[i] = new RGB((table[i] >> 16) & 0xff, (table[i] >> 8) & 0xff, table[i] & 0xff);
			}
			map.setPalette(palette);
		};
		apply.run();
		chooser.addListener(SWT.Modify, e -> apply.run());
		shell.setSize(640, 480);
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
