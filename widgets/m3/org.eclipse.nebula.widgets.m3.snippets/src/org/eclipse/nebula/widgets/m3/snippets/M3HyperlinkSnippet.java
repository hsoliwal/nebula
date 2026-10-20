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

import org.eclipse.nebula.widgets.m3.M3Hyperlink;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Three {@link M3Hyperlink}s as stand-alone actions; a click marks a link visited and updates the status label.
 * <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3HyperlinkSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3Hyperlink");
		shell.setLayout(new GridLayout(1, false));
		Label status = new Label(shell, SWT.NONE);
		String[] actions = { "Open the Problems view", "Show 10^6 markers", "Configure filters" };
		for (String action : actions) {
			M3Hyperlink link = new M3Hyperlink(shell, SWT.NONE);
			link.setText(action);
			link.addListener(SWT.Selection, e -> status.setText("activated: " + action));
		}
		status.setText("click a link");
		shell.setSize(320, 200);
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
