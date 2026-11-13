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

import java.util.Arrays;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.nebula.widgets.pgroup.PGroup;
import org.eclipse.nebula.widgets.progresscircle.ProgressCircle;
import org.eclipse.nebula.widgets.roundedswitch.RoundedSwitch;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget D check of the setters guarded by same_value_guard.py in PGroup, RoundedSwitch, ProgressCircle, Grid and
 * GridColumn: each setter with a new value repaints to the pixels of a full repaint, and the same value again paints
 * nothing (before: one repaint per call).
 */
public class SameValueRound2Check {

	static Display display;
	static int paints;
	static int failures;

	public static void main(String[] args) throws Exception {
		display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(700, 520);

		PGroup group = new PGroup(shell, SWT.SMOOTH);
		group.setText("Group");
		group.setBounds(10, 10, 300, 120);
		RoundedSwitch roundedSwitch = new RoundedSwitch(shell, SWT.NONE);
		roundedSwitch.setBounds(330, 10, 80, 40);
		ProgressCircle circle = new ProgressCircle(shell, SWT.NONE);
		circle.setBounds(430, 10, 120, 120);
		Grid grid = new Grid(shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setBounds(10, 150, 540, 300);
		grid.setHeaderVisible(true);
		GridColumn first = new GridColumn(grid, SWT.NONE);
		first.setText("first");
		first.setWidth(200);
		first.setTree(true);
		GridColumn second = new GridColumn(grid, SWT.NONE);
		second.setText("second");
		second.setWidth(200);
		for (int r = 0; r < 6; r++) {
			GridItem item = new GridItem(grid, SWT.NONE);
			item.setText(0, "row " + r);
			item.setText(1, "cell " + r);
			new GridItem(item, SWT.NONE).setText(0, "child " + r);
		}
		for (Control control : new Control[] { group, roundedSwitch, circle, grid }) {
			control.addListener(SWT.Paint, e -> paints++);
		}
		shell.open();
		spin();

		step(group, "PGroup.setBackground", () -> group.setBackground(display.getSystemColor(SWT.COLOR_YELLOW)));
		step(group, "PGroup.setImagePosition", () -> group.setImagePosition(SWT.RIGHT));
		step(group, "PGroup.setTogglePosition", () -> group.setTogglePosition(SWT.LEFT));
		step(group, "PGroup.setLinePosition", () -> group.setLinePosition(SWT.CENTER));
		step(roundedSwitch, "RoundedSwitch.setSelection", () -> roundedSwitch.setSelection(true));
		step(circle, "ProgressCircle.setThickness", () -> circle.setThickness(18));
		step(circle, "ProgressCircle.setShowText", () -> circle.setShowText(true));
		step(circle, "ProgressCircle.setHighlightColor",
				() -> circle.setHighlightColor(display.getSystemColor(SWT.COLOR_RED)));
		step(grid, "Grid.setLinesVisible", () -> grid.setLinesVisible(false));
		step(grid, "Grid.setTreeLinesVisible", () -> grid.setTreeLinesVisible(false));
		step(grid, "GridColumn.setSort", () -> second.setSort(SWT.UP));
		step(grid, "GridColumn.setMoveable", () -> second.setMoveable(true));

		shell.dispose();
		display.dispose();
		if (failures > 0) {
			throw new IllegalStateException(failures + " setter(s) failed");
		}
		System.out.println("PASSED");
	}

	/* a new value repaints to the pixels of a full repaint; the same value again paints nothing */
	static void step(Control control, String name, Runnable setter) throws InterruptedException {
		setter.run();
		spin();
		int[] shown = pixels(control);
		control.redraw();
		control.update();
		spin();
		boolean equal = Arrays.equals(shown, pixels(control));
		paints = 0;
		setter.run();
		spin();
		boolean ok = equal && paints == 0;
		failures += ok ? 0 : 1;
		System.out.println((ok ? "ok   " : "FAIL ") + name + ": pixels equal a full repaint=" + equal
				+ ", paints for the same value=" + paints);
	}

	static int[] pixels(Control control) {
		Rectangle size = control.getBounds();
		Image shot = new Image(display, size.width, size.height);
		GC gc = new GC(control);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int[] all = new int[size.width * size.height];
		for (int y = 0; y < size.height; y++) {
			data.getPixels(0, y, size.width, all, y * size.width);
		}
		return all;
	}

	static void spin() throws InterruptedException {
		long end = System.currentTimeMillis() + 200;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				Thread.sleep(5);
			}
		}
	}
}
