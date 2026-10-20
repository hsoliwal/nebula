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
import org.eclipse.nebula.widgets.m3.M3TaskPane;
import org.eclipse.nebula.widgets.m3.M3TaskPaneContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

/**
 * An {@link M3TaskPaneContainer} with three {@link M3TaskPane}s (one special, one with a busy label) in a scrolled
 * composite; click a title bar to collapse it (only the panes below move).
 * <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3TaskPaneSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3TaskPane");
		shell.setLayout(new FillLayout());
		ScrolledComposite scrolled = new ScrolledComposite(shell, SWT.V_SCROLL);
		M3TaskPaneContainer container = new M3TaskPaneContainer(scrolled, SWT.NONE);
		String[][] groups = { { "System tasks", "View system information", "Add or remove programs", "Change a setting" },
				{ "Other places", "My network places", "My documents", "Control panel" },
				{ "Details", "10^6 rows indexed" } };
		for (int g = 0; g < groups.length; g++) {
			M3TaskPane pane = new M3TaskPane(container, SWT.NONE);
			pane.setTitle(groups[g][0]);
			pane.setSpecial(g == 0);
			pane.getContent().setLayout(new GridLayout(1, false));
			for (int i = 1; i < groups[g].length; i++) {
				Link link = new Link(pane.getContent(), SWT.NONE);
				link.setText("<a>" + groups[g][i] + "</a>");
			}
			if (g == 2) {
				M3BusyLabel busy = new M3BusyLabel(pane.getContent(), SWT.NONE);
				busy.setText("Indexing");
				busy.setBusy(true);
			}
			pane.addListener(SWT.Expand, e -> scrolled.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
			pane.addListener(SWT.Collapse, e -> scrolled.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
		}
		scrolled.setContent(container);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		scrolled.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		shell.setSize(300, 520);
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
