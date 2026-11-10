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

import org.eclipse.nebula.widgets.opal.calculator.CalculatorCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget CalculatorCombo H leaf (opal has no test plugins): the popup closes when the
 * combo hides, when its shell is minimized, and (on SWT with part visibility) when an ancestor hides. Run with the
 * bundle classes and an SWT jar on the class path: java CalculatorComboHiddenCheck. Prints PASSED or throws.
 */
public class CalculatorComboHiddenCheck {

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Composite holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		CalculatorCombo combo = new CalculatorCombo(holder, SWT.NONE);
		shell.setSize(300, 80);
		shell.open();
		spin(display, 200);

		open(combo, display);
		combo.setVisible(false);
		spin(display, 100);
		check(!popupVisible(shell), "the popup closes when the combo hides");
		combo.setVisible(true);
		spin(display, 100);

		open(combo, display);
		shell.setMinimized(true);
		spin(display, 300);
		boolean minimized = shell.getMinimized();
		shell.setMinimized(false);
		spin(display, 300);
		if (minimized) {
			check(!popupVisible(shell), "the popup closes when the shell is minimized");
			System.out.println("minimize: checked");
		}

		boolean partVisibility;
		try {
			SWT.class.getField("PartHidden");
			partVisibility = true;
		} catch (NoSuchFieldException e) {
			partVisibility = false;
		}
		if (partVisibility) {
			open(combo, display);
			holder.setVisible(false);
			spin(display, 100);
			check(!popupVisible(shell), "the popup closes when an ancestor hides (PartHidden)");
			holder.setVisible(true);
			System.out.println("ancestor: checked");
		}
		shell.dispose();
		System.out.println("PASSED");
	}

	static void open(CalculatorCombo combo, Display display) {
		for (Control child : combo.getChildren()) {
			if (child instanceof Button) {
				child.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
			}
		}
		spin(display, 100);
		check(popupVisible(combo.getShell()), "the popup opens from the arrow");
	}

	static boolean popupVisible(Shell shell) {
		for (Shell child : shell.getShells()) {
			if (child.isVisible()) {
				return true;
			}
		}
		return false;
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
