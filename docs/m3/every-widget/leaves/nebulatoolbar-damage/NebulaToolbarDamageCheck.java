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

import org.eclipse.nebula.widgets.nebulatoolbar.NebulaToolbar;
import org.eclipse.nebula.widgets.nebulatoolbar.ToolbarItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget NebulaToolbar D leaf (nebulatoolbar has no test plugin): after each hover,
 * press, release, exit, focus and key step the toolbar's pixels equal a full repaint, in both modes, and in Seven mode
 * moving the hover from one item to the next repaints less than the whole toolbar (Vista mode repaints whole: its
 * gradient background renders a pixel or two differently under a partial clip). Run with the bundle classes and an SWT jar on
 * the class path: java NebulaToolbarDamageCheck. Prints PASSED or throws.
 */
public class NebulaToolbarDamageCheck {

	static long damaged;

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(700, 200);
		for (int mode : new int[] { NebulaToolbar.MODE_SEVEN, NebulaToolbar.MODE_VISTA }) {
			NebulaToolbar toolbar = new NebulaToolbar(shell, SWT.NONE);
			toolbar.setMode(mode);
			ToolbarItem[] items = new ToolbarItem[5];
			for (int i = 0; i < items.length; i++) {
				items[i] = new ToolbarItem(toolbar, SWT.NONE);
				items[i].setText("Item " + i);
			}
			toolbar.setBounds(10, 10, 640, 40);
			toolbar.addListener(SWT.Paint, e -> damaged += (long) e.width * e.height);
			shell.open();
			spin(display, 300);
			Rectangle area = toolbar.getClientArea();
			long full = (long) area.width * area.height;
			String name = mode == NebulaToolbar.MODE_SEVEN ? "seven" : "vista";

			mouse(toolbar, SWT.MouseMove, center(items, 1), 10);
			step(display, toolbar, name + " hover item 1");
			damaged = 0;
			mouse(toolbar, SWT.MouseMove, center(items, 2), 10);
			spin(display, 60);
			long hoverMove = damaged;
			step(display, toolbar, name + " hover item 2");
			mouse(toolbar, SWT.MouseDown, center(items, 2), 10);
			step(display, toolbar, name + " press item 2");
			mouse(toolbar, SWT.MouseUp, center(items, 2), 10);
			step(display, toolbar, name + " release item 2");
			mouse(toolbar, SWT.MouseExit, -5, -5);
			step(display, toolbar, name + " exit");
			toolbar.setFocus();
			step(display, toolbar, name + " focus");
			Event key = new Event();
			key.keyCode = SWT.ARROW_RIGHT;
			toolbar.notifyListeners(SWT.KeyDown, key);
			step(display, toolbar, name + " arrow right");
			System.out.println(name + ": hover move damage " + hoverMove + " of " + full + " pixels");
			check(mode == NebulaToolbar.MODE_VISTA || hoverMove < full, name + ": moving the hover repaints less than the whole toolbar (" + hoverMove
					+ " of " + full + ")");
			toolbar.dispose();
		}
		shell.dispose();
		System.out.println("PASSED");
	}

	/* the x of an item's centre, as the toolbar places its items (indent 3, then each item's width) */
	static int center(ToolbarItem[] items, int index) {
		int x = 3;
		for (int i = 0; i < index; i++) {
			x += items[i].getWidth();
		}
		return x + items[index].getWidth() / 2;
	}

	static void mouse(NebulaToolbar toolbar, int type, int x, int y) {
		Event event = new Event();
		event.x = x;
		event.y = y;
		event.button = type == SWT.MouseMove || type == SWT.MouseExit ? 0 : 1;
		toolbar.notifyListeners(type, event);
	}

	/* the toolbar's pixels after the step equal its pixels after a full repaint */
	static void step(Display display, NebulaToolbar toolbar, String step) {
		spin(display, 60);
		int[] incremental = pixels(toolbar);
		toolbar.redraw();
		toolbar.update();
		spin(display, 30);
		check(Arrays.equals(incremental, pixels(toolbar)), step + ": the pixels equal a full repaint");
	}

	static int[] pixels(NebulaToolbar toolbar) {
		Rectangle area = toolbar.getClientArea();
		Image shot = new Image(toolbar.getDisplay(), area.width, area.height);
		GC gc = new GC(toolbar);
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

	static void spin(Display display, int milliseconds) {
		long end = System.currentTimeMillis() + milliseconds;
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
