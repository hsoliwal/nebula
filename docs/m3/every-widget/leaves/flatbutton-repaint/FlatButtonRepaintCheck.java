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
package org.eclipse.nebula.widgets.opal.preferencewindow;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget opal FlatButton D leaf (opal has no test plugins): a tab switch of the
 * preference window (setSelection on every tab button, two of them change) repaints only the two buttons that change,
 * hovering a selected button repaints nothing, and the repainted buttons equal a full repaint. In the FlatButton package
 * because the class is package-private. Run with the bundle classes and an SWT jar on the class path:
 * java org.eclipse.nebula.widgets.opal.preferencewindow.FlatButtonRepaintCheck. Prints PASSED or throws.
 */
public class FlatButtonRepaintCheck {

	static final int[] paints = new int[5];

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(520, 120);
		FlatButton[] buttons = new FlatButton[paints.length];
		for (int i = 0; i < buttons.length; i++) {
			final int index = i;
			buttons[i] = new FlatButton(shell, SWT.NONE);
			buttons[i].setText("Tab " + i);
			buttons[i].setBounds(10 + i * 95, 10, 90, 40);
			buttons[i].addListener(SWT.Paint, e -> paints[index]++);
		}
		select(buttons, 0);
		shell.open();
		spin(display);

		Arrays.fill(paints, 0);
		select(buttons, 3);
		spin(display);
		System.out.println("paints after switching from tab 0 to tab 3: " + Arrays.toString(paints));
		for (int i = 0; i < paints.length; i++) {
			boolean changed = i == 0 || i == 3;
			check(changed == paints[i] > 0, "tab " + i + (changed ? " repaints" : " does not repaint") + " ("
					+ paints[i] + ")");
		}
		for (FlatButton button : buttons) {
			same(display, button, "after the switch");
		}

		Arrays.fill(paints, 0);
		buttons[3].notifyListeners(SWT.MouseEnter, new Event());
		buttons[3].notifyListeners(SWT.MouseExit, new Event());
		spin(display);
		check(paints[3] == 0, "hovering the selected tab repaints nothing (" + paints[3] + ")");
		buttons[1].notifyListeners(SWT.MouseEnter, new Event());
		spin(display);
		check(paints[1] > 0, "hovering an unselected tab repaints it");
		same(display, buttons[1], "hover");
		shell.dispose();
		System.out.println("PASSED");
	}

	/* what PWTabContainer.select does for its buttons */
	static void select(FlatButton[] buttons, int index) {
		for (int i = 0; i < buttons.length; i++) {
			buttons[i].setSelection(i == index);
		}
	}

	static void same(Display display, FlatButton button, String step) {
		int[] incremental = pixels(button);
		button.redraw();
		button.update();
		spin(display);
		check(Arrays.equals(incremental, pixels(button)), step + ": the pixels equal a full repaint");
	}

	static int[] pixels(FlatButton button) {
		Rectangle area = button.getClientArea();
		Image shot = new Image(button.getDisplay(), area.width, area.height);
		GC gc = new GC(button);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int[] all = new int[area.width * area.height];
		for (int y = 0; y < area.height; y++) {
			data.getPixels(0, y, area.width, all, y * area.width);
		}
		return all;
	}

	static void spin(Display display) {
		long end = System.currentTimeMillis() + 150;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	static void check(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
