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

import org.eclipse.nebula.widgets.opal.titledseparator.TitledSeparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget opal TitledSeparator H leaf (opal has no test plugins): resizes, also during a
 * hidden parent's layout, keep the separator's labels instead of disposing and recreating them, and a font or colour
 * change still reaches the labels on the next resize. Run with the bundle classes and an SWT jar on the class path:
 * java TitledSeparatorHiddenCheck. Prints PASSED or throws.
 */
public class TitledSeparatorHiddenCheck {

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Composite holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		TitledSeparator separator = new TitledSeparator(holder, SWT.NONE);
		separator.setText("Section");
		shell.setSize(300, 80);
		shell.open();
		spin(display, 200);
		Control[] built = separator.getChildren();
		check(built.length == 2, "a left-aligned text separator has a title and a line (" + built.length + ")");

		holder.setVisible(false);
		for (int width = 310; width <= 400; width += 10) {
			shell.setSize(width, 80);
			spin(display, 20);
		}
		check(same(built, separator.getChildren()), "hidden resizes keep the labels");

		holder.setVisible(true);
		shell.setSize(320, 90);
		spin(display, 200);
		check(same(built, separator.getChildren()), "a shown resize keeps the labels");
		check("Section".equals(((Label) separator.getChildren()[0]).getText()), "the title keeps its text");

		separator.setForeground(display.getSystemColor(SWT.COLOR_RED));
		shell.setSize(330, 90);
		spin(display, 200);
		Label title = (Label) separator.getChildren()[0];
		check(display.getSystemColor(SWT.COLOR_RED).equals(title.getForeground()),
				"a colour change reaches the labels on the next resize");
		shell.dispose();
		System.out.println("PASSED");
	}

	static boolean same(Control[] a, Control[] b) {
		if (a.length != b.length) {
			return false;
		}
		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i] || a[i].isDisposed()) {
				return false;
			}
		}
		return true;
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
