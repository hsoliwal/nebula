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

import org.eclipse.nebula.widgets.progresscircle.ProgressCircle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget ProgressCircle H leaf (progresscircle has no test plugin): setSelection on a
 * circle under a hidden parent returns at once with the new value instead of blocking the UI thread in its animation
 * loop; a visible circle still animates. Run with the bundle classes and an SWT jar on the class path:
 * java ProgressCircleHiddenCheck. Prints PASSED or throws.
 */
public class ProgressCircleHiddenCheck {

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Composite holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		ProgressCircle circle = new ProgressCircle(holder, SWT.NONE);
		circle.setMinimum(0);
		circle.setMaximum(100);
		circle.setAnimationDelay(20); // ten frames of 20 ms when animated
		shell.setSize(200, 200);
		shell.open();
		spin(display);

		long shown = time(() -> circle.setSelection(40));
		System.out.println("visible setSelection: " + shown + " ms");
		check(shown >= 150, "a visible circle animates (" + shown + " ms)");

		holder.setVisible(false);
		spin(display);
		long hidden = time(() -> circle.setSelection(90));
		System.out.println("hidden setSelection: " + hidden + " ms");
		check(circle.getSelection() == 90, "the hidden circle has the new value");
		check(hidden < 50, "setSelection on a hidden circle returns at once (" + hidden + " ms)");
		shell.dispose();
		System.out.println("PASSED");
	}

	static long time(Runnable action) {
		long start = System.currentTimeMillis();
		action.run();
		return System.currentTimeMillis() - start;
	}

	static void spin(Display display) {
		long end = System.currentTimeMillis() + 200;
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
