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

import org.eclipse.nebula.widgets.opal.heapmanager.HeapManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget HeapManager H leaf (opal has no test plugins): the bar is redrawn while shown,
 * not at all while an ancestor is hidden or the shell is minimized, and again after the show. Run with the bundle
 * classes and an SWT jar on the class path: java HeapManagerHiddenCheck. Prints PASSED or throws.
 */
public class HeapManagerHiddenCheck {

	static int paints;

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Composite holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		HeapManager heap = new HeapManager(holder, SWT.NONE);
		Canvas bar = (Canvas) heap.getChildren()[0];
		bar.addListener(SWT.Paint, e -> paints++);
		shell.setSize(400, 80);
		shell.open();
		spin(display, 1200);
		check(paints >= 2, "the bar is redrawn while shown, paints " + paints);
		check(timers(display) == 1, "one sample timer while shown, timers " + timers(display));
		holder.setVisible(false);
		spin(display, 300);
		paints = 0;
		spin(display, 1600);
		check(timers(display) == 0, "no sample timer while an ancestor is hidden, timers " + timers(display));
		holder.setVisible(true);
		spin(display, 1200);
		check(paints >= 2, "the bar is redrawn again after the show, paints " + paints);
		shell.setMinimized(true);
		spin(display, 600);
		paints = 0;
		spin(display, 1600);
		if (shell.getMinimized()) {
			check(timers(display) == 0, "no sample timer while the shell is minimized, timers " + timers(display));
			shell.setMinimized(false);
			spin(display, 1200);
			check(paints >= 2, "the bar is redrawn again after the restore, paints " + paints);
			System.out.println("minimize: checked");
		}
		shell.dispose();
		System.out.println("PASSED");
	}

	/* pending HeapManager timers in the display's timer list (win32 Display.timerList) */
	static int timers(Display display) {
		try {
			java.lang.reflect.Field field = Display.class.getDeclaredField("timerList");
			field.setAccessible(true);
			Runnable[] list = (Runnable[]) field.get(display);
			int count = 0;
			if (list != null) {
				for (Runnable timer : list) {
					if (timer != null && timer.getClass().getName().startsWith(HeapManager.class.getName())) {
						count++;
					}
				}
			}
			return count;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
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
