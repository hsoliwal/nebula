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

import org.eclipse.nebula.widgets.m3.M3MultiSplitPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * An IDE-like {@link M3MultiSplitPane}: a navigator on the left, an editor over a console in the middle column, an
 * outline on the right; drag any divider. <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's
 * pixels and exits.
 */
public class M3MultiSplitPaneSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3MultiSplitPane");
		shell.setLayout(new FillLayout());
		M3MultiSplitPane pane = new M3MultiSplitPane(shell, SWT.HORIZONTAL);
		List navigator = new List(pane, SWT.BORDER | SWT.V_SCROLL);
		for (int i = 0; i < 40; i++) {
			navigator.add("File" + i + ".java");
		}
		pane.addLeaf(0, 1, navigator);
		int middle = pane.addSplit(0, M3MultiSplitPane.COLUMN, 3);
		Text editor = new Text(pane, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		editor.setText("class Editor {\n    // drag the dividers\n}\n");
		pane.addLeaf(middle, 3, editor);
		Text console = new Text(pane, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
		console.setText("console: 0 errors");
		pane.addLeaf(middle, 1, console);
		List outline = new List(pane, SWT.BORDER);
		outline.setItems("Editor", "  field", "  method()");
		pane.addLeaf(0, 1, outline);
		shell.setSize(720, 460);
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
