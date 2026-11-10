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

import org.eclipse.nebula.widgets.roundedswitch.RoundedSwitch;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget D check of RoundedSwitch: a per-state colour or a new border width shows at once, to the pixels of a
 * full repaint (before, at the next unrelated repaint); the border width in use paints nothing.
 */
public class RoundedSwitchSetterDamageCheck {

	static int paints;

	public static void main(String[] args) throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(200, 120);
		RoundedSwitch bar = new RoundedSwitch(shell, SWT.NONE);
		bar.setBounds(10, 10, 80, 40);
		bar.setSelection(false);
		Color red = display.getSystemColor(SWT.COLOR_RED);
		Color blue = display.getSystemColor(SWT.COLOR_BLUE);
		bar.addListener(SWT.Paint, e -> paints++);
		shell.open();
		spin(display);

		bar.setBorderColorUncheckedEnabled(red);
		spin(display);
		check(sameAsFullRepaint(display, bar), "setBorderColorUncheckedEnabled: the pixels equal a full repaint");
		bar.setBackgroundColorUncheckedEnabled(blue);
		spin(display);
		check(sameAsFullRepaint(display, bar), "setBackgroundColorUncheckedEnabled: the pixels equal a full repaint");
		bar.setBorderWidth(4);
		spin(display);
		check(sameAsFullRepaint(display, bar), "setBorderWidth: the pixels equal a full repaint");

		paints = 0;
		bar.setBorderWidth(4);
		spin(display);
		check(paints == 0, "the border width in use paints nothing, got " + paints);

		shell.dispose();
		display.dispose();
		System.out.println("PASSED");
	}

	static boolean sameAsFullRepaint(Display display, RoundedSwitch bar) throws InterruptedException {
		int[] incremental = pixels(display, bar);
		bar.redraw();
		bar.update();
		spin(display);
		return Arrays.equals(incremental, pixels(display, bar));
	}

	static int[] pixels(Display display, RoundedSwitch bar) {
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
