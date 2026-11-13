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

import org.eclipse.nebula.widgets.badgedlabel.BadgedLabel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget D check of BadgedLabel: a new badge value, badge colour, text or image shows at once, to the pixels of
 * a full repaint (before, at the next unrelated repaint); the badge value and text in use paint nothing.
 */
public class BadgedLabelSetterDamageCheck {

	static int paints;

	public static void main(String[] args) throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(320, 140);
		BadgedLabel bar = new BadgedLabel(shell, SWT.NONE);
		bar.setText("Inbox");
		bar.setBadgeValue("3");
		bar.setBounds(20, 20, 200, 60);
		Color red = display.getSystemColor(SWT.COLOR_RED);
		bar.addListener(SWT.Paint, e -> paints++);
		shell.open();
		spin(display);

		bar.setBadgeValue("42");
		spin(display);
		check(sameAsFullRepaint(display, bar), "setBadgeValue: the pixels equal a full repaint");
		bar.setBadgeBackground(red);
		spin(display);
		check(sameAsFullRepaint(display, bar), "setBadgeBackground: the pixels equal a full repaint");
		bar.setText("Outbox");
		spin(display);
		check(sameAsFullRepaint(display, bar), "setText: the pixels equal a full repaint");
		bar.setPredefinedColor(SWT.COLOR_GREEN);
		spin(display);
		check(sameAsFullRepaint(display, bar), "setPredefinedColor: the pixels equal a full repaint");

		paints = 0;
		bar.setBadgeValue("42");
		bar.setText("Outbox");
		spin(display);
		check(paints == 0, "the badge value and text in use paint nothing, got " + paints);

		shell.dispose();
		display.dispose();
		System.out.println("PASSED");
	}

	static boolean sameAsFullRepaint(Display display, BadgedLabel bar) throws InterruptedException {
		int[] incremental = pixels(display, bar);
		bar.redraw();
		bar.update();
		spin(display);
		return Arrays.equals(incremental, pixels(display, bar));
	}

	static int[] pixels(Display display, BadgedLabel bar) {
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
