/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others.
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
 * Headful acceptance test for the EyeButton image-mode hover damage contract.
 *
 * <p>When an image supplies the eye glyph, mouse enter and exit draw the same
 * image at the same coordinates. Neither transition may enqueue a paint.
 * This is deliberately package-local so it exercises the real EyeButton
 * listener and paint code without reflection or a public API change.
 *
 * <p>Run with the passwordrevealer bundle classes and the selected SWT jar:
 * {@code java org.eclipse.nebula.widgets.passwordrevealer.EyeButtonViewportTest}.
 */
public final class EyeButtonViewportTest {
	private static int paints;

	private EyeButtonViewportTest() {
	}

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = null;
		Image eye = null;
		try {
			shell = new Shell(display);
			shell.setSize(160, 100);
			EyeButton button = new EyeButton(shell, SWT.NONE);
			eye = new Image(display, 16, 16);
			button.setImage(eye);
			button.setBounds(12, 12, 36, 36);
			shell.open();
			pump(display, 120);
			button.addListener(SWT.Paint, event -> paints++);

			assertNoHoverDamage(display, button, SWT.MouseEnter, "enter");
			assertNoHoverDamage(display, button, SWT.MouseExit, "exit");
			System.out.println("EYE_BUTTON_IMAGE_HOVER_NO_REDRAW=PASS");
		} finally {
			if (shell != null && !shell.isDisposed()) {
				shell.dispose();
			}
			if (eye != null && !eye.isDisposed()) {
				eye.dispose();
			}
			if (!display.isDisposed()) {
				display.dispose();
			}
		}
	}

	private static void assertNoHoverDamage(Display display, EyeButton button, int eventType, String transition) {
		button.redraw();
		button.update();
		pump(display, 40);
		int[] before = pixels(button);
		paints = 0;

		Event event = new Event();
		event.type = eventType;
		event.widget = button;
		button.notifyListeners(eventType, event);
		pump(display, 120);

		check(paints == 0, transition + " scheduled " + paints + " paint event(s)");
		int[] incremental = pixels(button);
		check(Arrays.equals(before, incremental), transition + " changed pixels without a paint event");

		button.redraw();
		button.update();
		pump(display, 40);
		check(Arrays.equals(incremental, pixels(button)),
				transition + " pixels differ after a forced full repaint");
	}

	private static int[] pixels(EyeButton button) {
		Rectangle area = button.getClientArea();
		Image capture = new Image(button.getDisplay(), area.width, area.height);
		GC gc = new GC(button);
		try {
			gc.copyArea(capture, 0, 0);
		} finally {
			gc.dispose();
		}
		try {
			ImageData data = capture.getImageData();
			int[] result = new int[area.width * area.height];
			for (int y = 0; y < area.height; y++) {
				data.getPixels(0, y, area.width, result, y * area.width);
			}
			return result;
		} finally {
			capture.dispose();
		}
	}

	private static void pump(Display display, long millis) {
		long end = System.nanoTime() + millis * 1_000_000L;
		while (System.nanoTime() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(2L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("interrupted while pumping SWT events", e);
				}
			}
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
