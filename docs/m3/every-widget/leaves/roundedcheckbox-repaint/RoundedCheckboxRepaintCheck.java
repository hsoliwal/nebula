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

import org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckbox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget RoundedCheckbox D leaf (roundedcheckbox has no test plugin): changes that draw
 * nothing new repaint nothing (same selection, grayed while unselected, same grayed, hover on a disabled box), changes
 * that do repaint to the pixels of a full repaint. Run with the bundle classes and an SWT jar on the class path:
 * java RoundedCheckboxRepaintCheck. Prints PASSED or throws.
 */
public class RoundedCheckboxRepaintCheck {

	static int paints;

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(200, 120);
		RoundedCheckbox box = new RoundedCheckbox(shell, SWT.NONE);
		box.setBounds(10, 10, 40, 40);
		box.addListener(SWT.Paint, e -> paints++);
		shell.open();
		spin(display);

		step(display, box, "same selection", () -> box.setSelection(false), false);
		step(display, box, "grayed while unselected", () -> box.setGrayed(true), false);
		step(display, box, "select (grayed)", () -> box.setSelection(true), true);
		step(display, box, "same grayed", () -> box.setGrayed(true), false);
		step(display, box, "ungray while selected", () -> box.setGrayed(false), true);
		step(display, box, "hover enter", () -> box.notifyListeners(SWT.MouseEnter, new Event()), true);
		step(display, box, "hover exit", () -> box.notifyListeners(SWT.MouseExit, new Event()), true);
		box.setEnabled(false);
		box.redraw();
		spin(display);
		step(display, box, "hover on a disabled box", () -> box.notifyListeners(SWT.MouseEnter, new Event()), false);
		shell.dispose();
		System.out.println("PASSED");
	}

	static void step(Display display, RoundedCheckbox box, String name, Runnable change, boolean repaints) {
		paints = 0;
		change.run();
		spin(display);
		check(repaints == paints > 0, name + (repaints ? " repaints" : " repaints nothing") + " (" + paints + ")");
		int[] incremental = pixels(box);
		box.redraw();
		box.update();
		spin(display);
		check(Arrays.equals(incremental, pixels(box)), name + ": the pixels equal a full repaint");
	}

	static int[] pixels(RoundedCheckbox box) {
		Rectangle area = box.getClientArea();
		Image shot = new Image(box.getDisplay(), area.width, area.height);
		GC gc = new GC(box);
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
		long end = System.currentTimeMillis() + 120;
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
