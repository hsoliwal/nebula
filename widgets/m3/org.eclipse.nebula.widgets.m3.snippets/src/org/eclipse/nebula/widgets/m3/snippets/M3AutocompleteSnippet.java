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

import org.eclipse.nebula.widgets.m3.M3Autocomplete;
import org.eclipse.nebula.widgets.m3.M3Searchable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * {@link M3Autocomplete} over 10^6 generated class names: prefix proposals first, then names containing the text;
 * Up/Down, Enter, Escape. <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3AutocompleteSnippet {

	public static void main(String[] args) {
		String[] parts = { "Table", "Tree", "Viewer", "Layout", "Composite", "Label", "Button", "Model", "Delta", "Lane" };
		String[] names = new String[1_000_000];
		for (int i = 0; i < names.length; i++) {
			names[i] = parts[i % 10] + parts[(i / 10) % 10] + parts[(i / 100) % 10] + i;
		}
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3Autocomplete: 10^6 names");
		shell.setLayout(new GridLayout(1, false));
		new Label(shell, SWT.NONE).setText("Type a class name (try 'treeview'):");
		Text field = new Text(shell, SWT.BORDER);
		field.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Label chosen = new Label(shell, SWT.NONE);
		chosen.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		M3Autocomplete complete = new M3Autocomplete(field, M3Searchable.of(() -> names.length, row -> names[row], row -> {
		}));
		complete.setLimit(500);
		field.addListener(SWT.Selection, e -> chosen.setText("accepted row " + e.index));
		shell.setSize(420, 300);
		shell.open();
		field.setText("treeview");
		Screenshot.runOrSave(shell, display);
	}
}
