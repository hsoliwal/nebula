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

import org.eclipse.nebula.widgets.m3.M3CollapsiblePane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Two {@link M3CollapsiblePane}s, a vertical search options pane and a horizontal side panel, each with a toggle
 * button; the collapse is animated and slides the content without laying it out again.
 * <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3CollapsiblePaneSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3CollapsiblePane");
		shell.setLayout(new GridLayout(2, false));
		Button toggleOptions = new Button(shell, SWT.PUSH);
		toggleOptions.setText("Search options");
		Button toggleSide = new Button(shell, SWT.PUSH);
		toggleSide.setText("Side panel");
		M3CollapsiblePane options = new M3CollapsiblePane(shell, SWT.VERTICAL);
		options.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		options.getContent().setLayout(new GridLayout(2, false));
		String[] names = { "Match case", "Whole word", "Regular expression", "Wrap around" };
		for (String name : names) {
			Button check = new Button(options.getContent(), SWT.CHECK);
			check.setText(name);
		}
		M3CollapsiblePane side = new M3CollapsiblePane(shell, SWT.HORIZONTAL);
		side.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, true));
		side.getContent().setLayout(new GridLayout(1, false));
		for (int i = 1; i <= 6; i++) {
			new Label(side.getContent(), SWT.NONE).setText("Outline entry " + i);
		}
		Text text = new Text(shell, SWT.MULTI | SWT.BORDER | SWT.WRAP);
		text.setText("Main content. The panes collapse with an animation; the content slides and is measured once.");
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toggleOptions.addListener(SWT.Selection, e -> options.setCollapsed(!options.isCollapsed()));
		toggleSide.addListener(SWT.Selection, e -> side.setCollapsed(!side.isCollapsed()));
		shell.setSize(560, 360);
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
