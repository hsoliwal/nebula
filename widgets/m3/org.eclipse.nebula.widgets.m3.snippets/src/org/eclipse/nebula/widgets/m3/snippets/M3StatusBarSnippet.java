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

import org.eclipse.nebula.widgets.m3.M3StatusBar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/**
 * An {@link M3StatusBar} with a fixed slot, a preferred-width row counter that a timer updates (each update repaints
 * that slot, or the bar from it when its width changes), a weighted message slot, a progress bar in a fixed slot and
 * the resize grip. <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3StatusBarSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3StatusBar");
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = layout.marginHeight = 0;
		shell.setLayout(layout);
		Label content = new Label(shell, SWT.CENTER);
		content.setText("Resize the window with the grip; the row counter updates every 100 ms.");
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		M3StatusBar bar = new M3StatusBar(shell, SWT.NONE);
		bar.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));
		int ready = bar.addSlot(M3StatusBar.FIXED, 70);
		int rows = bar.addSlot(M3StatusBar.PREFERRED, 60);
		int message = bar.addSlot(M3StatusBar.FILL, 1);
		int progress = bar.addSlot(M3StatusBar.FIXED, 120);
		bar.setText(ready, "Ready");
		bar.setText(message, "Filter: none; sort: name ascending");
		ProgressBar bar0 = new ProgressBar(bar, SWT.SMOOTH);
		bar0.setMaximum(1000);
		bar.setControl(progress, bar0);
		int[] row = { 0 };
		Runnable tick = new Runnable() {
			@Override
			public void run() {
				if (bar.isDisposed()) {
					return;
				}
				row[0] = (row[0] * 7 + 13) % 1_000_000;
				bar.setText(rows, "row " + row[0] + " of 1,000,000");
				bar0.setSelection(row[0] / 1000);
				display.timerExec(100, this);
			}
		};
		tick.run();
		shell.setSize(640, 200);
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
