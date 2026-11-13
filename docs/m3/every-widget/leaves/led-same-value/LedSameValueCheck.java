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

import org.eclipse.nebula.widgets.led.LED;
import org.eclipse.nebula.widgets.led.LEDCharacter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget D check of the LED widgets: the idle colour, selected colour and dot flag in use repaint nothing
 * (before: one repaint per call); a new value repaints to the pixels of a full repaint.
 */
public class LedSameValueCheck {

	static int paints;

	public static void main(String[] args) throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(200, 220);
		LED bar = new LED(shell, SWT.NONE);
		bar.setCharacter(LEDCharacter.EIGHT);
		bar.setBounds(20, 20, 80, 130);
		Color red = display.getSystemColor(SWT.COLOR_RED);
		Color grey = display.getSystemColor(SWT.COLOR_DARK_GRAY);
		bar.addListener(SWT.Paint, e -> paints++);
		shell.open();
		spin(display);

		bar.setSelectedColor(red);
		spin(display);
		check(sameAsFullRepaint(display, bar), "setSelectedColor: the pixels equal a full repaint");
		bar.setIdleColor(grey);
		spin(display);
		check(sameAsFullRepaint(display, bar), "setIdleColor: the pixels equal a full repaint");
		bar.setShowDot(true);
		spin(display);
		check(sameAsFullRepaint(display, bar), "setShowDot: the pixels equal a full repaint");

		paints = 0;
		bar.setSelectedColor(red);
		bar.setIdleColor(grey);
		bar.setShowDot(true);
		spin(display);
		System.out.println("paints for the values in use: " + paints);
		check(paints == 0, "the values in use paint nothing, got " + paints);

		shell.dispose();
		display.dispose();
		System.out.println("PASSED");
	}

	static boolean sameAsFullRepaint(Display display, LED bar) throws InterruptedException {
		int[] incremental = pixels(display, bar);
		bar.redraw();
		bar.update();
		spin(display);
		return Arrays.equals(incremental, pixels(display, bar));
	}

	static int[] pixels(Display display, LED bar) {
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
