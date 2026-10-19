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

import org.eclipse.nebula.widgets.m3.M3StackLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * A stack of 1,000,000 pages on an {@link M3StackLayout}: a spinner picks the page, which is created on demand; at
 * most 8 pages exist at any time. The window title shows the resident pages. Arguments: the page count (default
 * 1000000); <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3StackLayoutSnippet {

	public static void main(String[] args) {
		int count = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3StackLayout: " + count + " pages");
		shell.setLayout(new GridLayout(2, false));
		new Label(shell, SWT.NONE).setText("Page");
		Spinner spinner = new Spinner(shell, SWT.BORDER);
		spinner.setMinimum(0);
		spinner.setMaximum(count - 1);
		spinner.setPageIncrement(count / 100);
		spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Composite stack = new Composite(shell, SWT.BORDER);
		stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		M3StackLayout layout = new M3StackLayout();
		stack.setLayout(layout);
		layout.setPages(stack, count, M3StackLayoutSnippet::createPage);
		Label status = new Label(shell, SWT.NONE);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		Runnable show = () -> {
			layout.showPage(spinner.getSelection());
			status.setText("resident pages (budget " + layout.getResidencyBudget() + "): " + java.util.Arrays.toString(layout.getResidentPages()));
		};
		spinner.addListener(SWT.Selection, e -> show.run());
		spinner.setSelection(count / 2);
		show.run();
		shell.setSize(480, 320);
		shell.open();
		Screenshot.runOrSave(shell, display);
		display.dispose();
	}

	static Control createPage(Composite parent, int index) {
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new FillLayout(SWT.VERTICAL));
		Label label = new Label(page, SWT.CENTER);
		label.setText("Page " + index + " of " + ((M3StackLayout) parent.getLayout()).getPageCount());
		Text text = new Text(page, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		text.setText("Created on first show; released when more than the budget are resident.");
		return page;
	}
}
