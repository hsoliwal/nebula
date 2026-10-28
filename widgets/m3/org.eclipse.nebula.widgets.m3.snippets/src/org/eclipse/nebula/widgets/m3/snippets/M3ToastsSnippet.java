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
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.snippets;

import org.eclipse.nebula.widgets.m3.M3Toasts;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * {@link M3Toasts}: buttons add info, warning and error toasts to one overlay in the bottom-right corner; click a toast
 * to dismiss it, rest the pointer on the stack to pause expiry. <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code>
 * saves the shell's pixels and exits.
 */
public class M3ToastsSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3Toasts");
		shell.setLayout(new GridLayout(3, true));
		M3Toasts toasts = new M3Toasts(shell);
		String[] labels = { "Info", "Warning", "Error" };
		for (int level = 0; level < labels.length; level++) {
			Button button = new Button(shell, SWT.PUSH);
			button.setText(labels[level]);
			int chosen = level;
			int[] counter = { 0 };
			button.addListener(SWT.Selection, e -> toasts.show(labels[chosen] + " " + ++counter[0], "Indexed 10^6 rows in 1.2 s", chosen, 5000));
		}
		shell.setSize(560, 400);
		shell.open();
		toasts.show("Index ready", "10^6 rows indexed", M3Toasts.INFO, 60_000);
		toasts.show("Slow filter", "regex over 10^6 rows took 900 ms", M3Toasts.WARNING, 60_000);
		Screenshot.runOrSave(shell, display);
	}
}
