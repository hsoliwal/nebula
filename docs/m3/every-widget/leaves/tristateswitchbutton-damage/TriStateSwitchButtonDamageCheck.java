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

import org.eclipse.nebula.widgets.opal.switchbutton.TriState;
import org.eclipse.nebula.widgets.opal.switchbutton.TriStateSwitchButton;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget opal TriStateSwitchButton D leaf (opal has no test plugins): state and hover changes
 * repaint the switch box, not the text beside it, to the pixels of a full repaint; setState with the current state
 * and a repeated hover repaint nothing. Run with the bundle classes and an SWT jar on the class path:
 * java TriStateSwitchButtonDamageCheck. Prints PASSED or throws.
 */
public class TriStateSwitchButtonDamageCheck {

	static int paints;
	static boolean textRepainted;

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(400, 120);
		TriStateSwitchButton button = new TriStateSwitchButton(shell, SWT.NONE);
		button.setText("Synchronize the archive in the background");
		button.setFocusColor(display.getSystemColor(SWT.COLOR_BLUE));
		button.setBounds(10, 10, 360, 40);
		button.addListener(SWT.Paint, e -> {
			paints++;
			// the damaged rectangle of this paint (the widget widens its GC clip while drawing, the update region
			// still bounds what reaches the screen): does it reach the far end of the text beside the switch?
			textRepainted |= new Rectangle(e.x, e.y, e.width, e.height).contains(button.getClientArea().width - 6,
					button.getClientArea().height / 2);
		});
		shell.open();
		spin(display);

		step(display, button, "state FIRST", () -> button.setState(TriState.FIRST), true);
		step(display, button, "state FIRST again", () -> button.setState(TriState.FIRST), false);
		step(display, button, "mouse enter", () -> button.notifyListeners(SWT.MouseEnter, new Event()), true);
		step(display, button, "mouse hover", () -> button.notifyListeners(SWT.MouseHover, new Event()), false);
		step(display, button, "click", () -> button.notifyListeners(SWT.MouseUp, new Event()), true);
		step(display, button, "mouse exit", () -> button.notifyListeners(SWT.MouseExit, new Event()), true);
		check(button.getState() == TriState.OFF, "the click switched the state off");
		shell.dispose();
		System.out.println("PASSED");
	}

	static void step(Display display, TriStateSwitchButton button, String name, Runnable change, boolean repaints) {
		paints = 0;
		textRepainted = false;
		change.run();
		spin(display);
		if (!repaints) {
			check(paints == 0, name + ": nothing changes, nothing repaints (" + paints + ")");
			return;
		}
		check(paints > 0, name + ": repaints");
		check(!textRepainted, name + ": repaints the switch, not the text beside it");
		int[] incremental = pixels(button);
		button.redraw();
		button.update();
		spin(display);
		check(Arrays.equals(incremental, pixels(button)), name + ": the pixels equal a full repaint");
	}

	static int[] pixels(TriStateSwitchButton button) {
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
