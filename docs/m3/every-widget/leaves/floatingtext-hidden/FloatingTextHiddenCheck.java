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

import org.eclipse.nebula.widgets.floatingtext.FloatingText;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget FloatingText H leaf (floatingtext has no test plugin): while the floating label
 * is 0 high, a focus gain posts no event-loop poll, and the label shows its fitted message once it gets a height. Run
 * with the bundle classes and an SWT jar on the class path: java FloatingTextHiddenCheck. Prints PASSED or throws.
 */
public class FloatingTextHiddenCheck {

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout());
		FloatingText floating = new FloatingText(shell, SWT.BORDER);
		floating.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		floating.getText().setMessage("Name");
		floating.setRatio(0); // the floating label gets no height
		shell.setSize(300, 120);
		shell.open();
		spin(display, 200);
		check(floating.getLabel().getSize().y == 0, "the label is 0 high with ratio 0");

		floating.getText().notifyListeners(SWT.FocusIn, new Event());
		int busy = 0;
		long end = System.currentTimeMillis() + 300;
		while (System.currentTimeMillis() < end) {
			if (display.readAndDispatch()) {
				busy++;
			} else {
				sleep(5);
			}
		}
		System.out.println("event-loop runs in 300 ms with a 0-high label: " + busy);
		check(busy < 100, "a 0-high label posts no event-loop poll (" + busy + " runs in 300 ms)");

		floating.setRatio(90);
		shell.layout(true, true);
		spin(display, 200);
		check(floating.getLabel().getSize().y > 0, "the label gets a height with ratio 90");
		check("Name".equals(floating.getLabel().getText()), "the label shows the message once it has a height, got '"
				+ floating.getLabel().getText() + "'");
		shell.dispose();
		System.out.println("PASSED");
	}

	static void spin(Display display, int milliseconds) {
		long end = System.currentTimeMillis() + milliseconds;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				sleep(5);
			}
		}
	}

	static void sleep(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	static void check(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
