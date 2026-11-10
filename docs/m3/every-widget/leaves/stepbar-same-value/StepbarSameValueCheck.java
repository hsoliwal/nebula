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

import org.eclipse.nebula.widgets.stepbar.Stepbar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget D check of the Stepbar: the step, error state and colours in use repaint nothing; a new value repaints
 * to the pixels of a full repaint; once the steps list escaped (getSteps), the setters still repaint, so a caller that
 * changed the list can refresh the bar.
 */
public class StepbarSameValueCheck {

	static int paints;

	public static void main(String[] args) throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(600, 160);
		Stepbar bar = new Stepbar(shell, SWT.NONE);
		bar.setBounds(10, 10, 560, 100);
		for (String step : new String[] { "Order", "Pay", "Ship", "Done" }) {
			bar.addStep(step);
		}
		Color blue = display.getSystemColor(SWT.COLOR_BLUE);
		Color grey = display.getSystemColor(SWT.COLOR_DARK_GRAY);
		bar.addListener(SWT.Paint, e -> paints++);
		shell.open();
		spin(display);

		paints = 0;
		bar.setCurrentStep(1);
		bar.setTextColor(blue);
		bar.setUnselectedColor(grey);
		spin(display);
		check(paints > 0, "new values repaint");
		check(sameAsFullRepaint(display, bar), "new values: the pixels equal a full repaint");

		paints = 0;
		bar.setCurrentStep(1);
		bar.setErrorState(false);
		bar.setTextColor(blue);
		bar.setUnselectedColor(grey);
		spin(display);
		System.out.println("paints for the values in use: " + paints);
		check(paints == 0, "the values in use repaint nothing, got " + paints);

		paints = 0;
		bar.setErrorState(true);
		bar.setCurrentStep(2);
		spin(display);
		check(paints > 0, "a new step and error state repaint");
		check(sameAsFullRepaint(display, bar), "new step: the pixels equal a full repaint");

		bar.getSteps();
		paints = 0;
		bar.setCurrentStep(2);
		spin(display);
		check(paints > 0, "with the steps list escaped, the same step still repaints");

		shell.dispose();
		display.dispose();
		System.out.println("PASSED");
	}

	static boolean sameAsFullRepaint(Display display, Stepbar bar) throws InterruptedException {
		int[] incremental = pixels(display, bar);
		bar.redraw();
		bar.update();
		spin(display);
		return Arrays.equals(incremental, pixels(display, bar));
	}

	static int[] pixels(Display display, Stepbar bar) {
		Rectangle size = bar.getBounds();
		Image shot = new Image(display, size.width, size.height);
		GC gc = new GC(bar);
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

	static void spin(Display display) throws InterruptedException {
		long end = System.currentTimeMillis() + 200;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				Thread.sleep(5);
			}
		}
	}

	static void check(boolean condition, String message) {
		System.out.println((condition ? "ok   " : "FAIL ") + message);
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
