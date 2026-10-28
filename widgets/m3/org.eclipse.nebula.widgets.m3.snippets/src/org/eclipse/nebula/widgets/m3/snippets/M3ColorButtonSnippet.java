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

import org.eclipse.nebula.widgets.m3.M3ColorButton;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Three {@link M3ColorButton}s (foreground, background, highlight); click one to open the colour dialog.
 * <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3ColorButtonSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3ColorButton");
		shell.setLayout(new GridLayout(2, false));
		String[] names = { "Foreground", "Background", "Highlight" };
		RGB[] colors = { new RGB(20, 20, 20), new RGB(250, 250, 245), new RGB(255, 200, 0) };
		Label status = new Label(shell, SWT.NONE);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		for (int i = 0; i < names.length; i++) {
			new Label(shell, SWT.NONE).setText(names[i] + ":");
			M3ColorButton button = new M3ColorButton(shell, SWT.NONE);
			button.setColor(colors[i]);
			String name = names[i];
			button.addListener(SWT.Selection, e -> status.setText(name + " = #" + String.format("%06X", Integer.valueOf(e.index))));
		}
		status.setText("click a swatch");
		shell.pack();
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
