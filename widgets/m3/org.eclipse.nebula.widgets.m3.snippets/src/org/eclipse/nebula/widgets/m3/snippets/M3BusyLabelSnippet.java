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

import org.eclipse.nebula.widgets.m3.M3BusyLabel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Three {@link M3BusyLabel}s (16, 24 and 32 pixel rings with 8, 12 and 16 points); the button starts and stops them.
 * <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3BusyLabelSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3BusyLabel");
		shell.setLayout(new GridLayout(1, false));
		M3BusyLabel[] labels = new M3BusyLabel[3];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = new M3BusyLabel(shell, SWT.NONE);
			labels[i].setIconSize(16 + 8 * i);
			labels[i].setPointCount(8 + 4 * i);
			labels[i].setTrailLength(3 + 2 * i);
			labels[i].setText("Loading page " + (i + 1) + " of 10^6 rows");
			labels[i].setBusy(true);
			labels[i].setFrame(2 * i + 1);
		}
		Button toggle = new Button(shell, SWT.PUSH);
		toggle.setText("Stop");
		toggle.addListener(SWT.Selection, e -> {
			boolean busy = !labels[0].isBusy();
			for (M3BusyLabel label : labels) {
				label.setBusy(busy);
			}
			toggle.setText(busy ? "Stop" : "Start");
		});
		shell.pack();
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
