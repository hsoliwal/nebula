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

import java.lang.reflect.Field;
import java.util.Arrays;

import org.eclipse.nebula.widgets.opal.nebulaslider.NebulaSlider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget NebulaSlider D leaf (nebulaslider has no test plugin): while dragging the
 * selector, a vertical move and the mouse-up repaint nothing, a horizontal move repaints, and the result equals a full
 * repaint. Run with the bundle classes and an SWT jar on the class path: java NebulaSliderRepaintCheck. Prints PASSED
 * or throws.
 */
public class NebulaSliderRepaintCheck {

	static int paints;

	public static void main(String[] args) throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(460, 120);
		NebulaSlider slider = new NebulaSlider(shell, SWT.NONE);
		slider.setMinimum(0);
		slider.setMaximum(100);
		slider.setValue(0);
		slider.setBounds(10, 10, 420, 60);
		slider.addListener(SWT.Paint, e -> paints++);
		shell.open();
		spin(display);

		// inside the selector: its left edge is the horizontal margin past xPosition
		int x = field(slider, "xPosition") + slider.getRenderer().getHorizontalMargin()
				+ slider.getRenderer().getSelectorWidth() / 2;
		int y = slider.getClientArea().height / 2;
		mouse(slider, SWT.MouseDown, x, y);
		step(display, slider, "vertical move", () -> mouse(slider, SWT.MouseMove, x, y + 6), false);
		step(display, slider, "horizontal move", () -> mouse(slider, SWT.MouseMove, x + 80, y + 6), true);
		step(display, slider, "mouse up", () -> mouse(slider, SWT.MouseUp, x + 80, y + 6), false);
		check(slider.getValue() > 0, "the drag moved the value (" + slider.getValue() + ")");
		shell.dispose();
		System.out.println("PASSED");
	}

	static void step(Display display, NebulaSlider slider, String name, Runnable change, boolean repaints) {
		paints = 0;
		change.run();
		spin(display);
		check(repaints == paints > 0, name + (repaints ? " repaints" : " repaints nothing") + " (" + paints + ")");
		int[] incremental = pixels(slider);
		slider.redraw();
		slider.update();
		spin(display);
		check(Arrays.equals(incremental, pixels(slider)), name + ": the pixels equal a full repaint");
	}

	static void mouse(NebulaSlider slider, int type, int x, int y) {
		Event event = new Event();
		event.x = x;
		event.y = y;
		event.button = 1;
		slider.notifyListeners(type, event);
	}

	static int field(Object owner, String name) throws ReflectiveOperationException {
		Field field = NebulaSlider.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(owner);
	}

	static int[] pixels(NebulaSlider slider) {
		Rectangle area = slider.getClientArea();
		Image shot = new Image(slider.getDisplay(), area.width, area.height);
		GC gc = new GC(slider);
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
