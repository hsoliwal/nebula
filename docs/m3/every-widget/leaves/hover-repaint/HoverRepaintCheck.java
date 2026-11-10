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
package org.eclipse.nebula.widgets.passwordrevealer;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.eclipse.nebula.widgets.chips.Chips;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget Chips and EyeButton D leaf (neither bundle has a test plugin): hovering a plain
 * chip and hovering the eye button in image mode repaint nothing; hovering a toggle chip and the eye button without an
 * image repaint, to the pixels of a full repaint. In the EyeButton package because the class is package-private. Run
 * with the bundle classes and an SWT jar on the class path:
 * java org.eclipse.nebula.widgets.passwordrevealer.HoverRepaintCheck. Prints PASSED or throws.
 */
public class HoverRepaintCheck {

	static int paints;

	public static void main(String[] args) throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(420, 220);
		Chips plain = new Chips(shell, SWT.NONE);
		plain.setText("Plain chip");
		plain.setBounds(10, 10, 120, 30);
		Chips toggle = new Chips(shell, SWT.TOGGLE);
		toggle.setText("Toggle chip");
		toggle.setBounds(150, 10, 120, 30);
		PasswordRevealer withImage = new PasswordRevealer(shell, SWT.NONE);
		Image eye = new Image(display, 16, 16);
		withImage.setImage(eye);
		withImage.setBounds(10, 60, 200, 30);
		PasswordRevealer drawn = new PasswordRevealer(shell, SWT.NONE);
		drawn.setBounds(10, 110, 200, 30);
		shell.open();
		spin(display);

		hover(display, plain, "plain chip", false);
		hover(display, toggle, "toggle chip", true);
		hover(display, eyeButton(withImage), "eye button with an image", false);
		hover(display, eyeButton(drawn), "drawn eye button", true);
		shell.dispose();
		eye.dispose();
		System.out.println("PASSED");
	}

	static void hover(Display display, Control control, String name, boolean repaints) {
		control.addListener(SWT.Paint, e -> paints++);
		for (int type : new int[] { SWT.MouseEnter, SWT.MouseExit }) {
			String step = name + (type == SWT.MouseEnter ? " enter" : " exit");
			paints = 0;
			control.notifyListeners(type, new Event());
			spin(display);
			check(repaints == paints > 0, step + (repaints ? " repaints" : " repaints nothing") + " (" + paints + ")");
			int[] incremental = pixels(control);
			control.redraw();
			control.update();
			spin(display);
			check(Arrays.equals(incremental, pixels(control)), step + ": the pixels equal a full repaint");
		}
	}

	static Canvas eyeButton(PasswordRevealer revealer) throws ReflectiveOperationException {
		Field field = PasswordRevealer.class.getDeclaredField("eyeButton");
		field.setAccessible(true);
		return (Canvas) field.get(revealer);
	}

	static int[] pixels(Control control) {
		Rectangle area = control.getBounds();
		Image shot = new Image(control.getDisplay(), area.width, area.height);
		GC gc = new GC(control);
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
