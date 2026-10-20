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

import org.eclipse.nebula.widgets.m3.M3ErrorPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * An embedded {@link M3ErrorPane} showing a caught exception (details expanded), and a button that opens the same
 * error in the modal dialog form. <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and
 * exits.
 */
public class M3ErrorPaneSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3ErrorPane");
		shell.setLayout(new GridLayout(1, false));
		Throwable error;
		try {
			int[] rows = new int[10];
			rows[1_000_000] = 1;
			error = null;
		} catch (ArrayIndexOutOfBoundsException e) {
			error = new IllegalStateException("Sorting 10^6 rows failed", e);
		}
		M3ErrorPane pane = new M3ErrorPane(shell, SWT.BORDER);
		pane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		pane.setMessage("The table could not be sorted. The details show the stack trace; Copy puts message and details on the clipboard.");
		pane.setError(error);
		pane.setDetailsVisible(true);
		Button dialog = new Button(shell, SWT.PUSH);
		dialog.setText("Open as dialog");
		Throwable shown = error;
		dialog.addListener(SWT.Selection, e -> M3ErrorPane.open(shell, "Sort failed", "The table could not be sorted.", shown));
		shell.setSize(620, 460);
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
