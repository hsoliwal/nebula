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

import org.eclipse.nebula.widgets.m3.M3FindBar;
import org.eclipse.nebula.widgets.m3.M3Searchable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * An {@link M3FindBar} over a virtual table of 10^6 rows: the bar reads row texts from the model array, never from
 * table items; type to search (sliced, superseding), Enter or Next for the next match.
 * <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3FindBarSnippet {

	public static void main(String[] args) {
		int rows = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
		String[] model = new String[rows];
		for (int i = 0; i < rows; i++) {
			model[i] = i % 997 == 3 ? "order " + i + " overdue" : "order " + i;
		}
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3FindBar: " + rows + " rows");
		shell.setLayout(new GridLayout(1, false));
		M3FindBar bar = new M3FindBar(shell, SWT.NONE);
		bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Table table = new Table(shell, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.addListener(SWT.SetData, e -> {
			TableItem item = (TableItem) e.item;
			item.setText(model[table.indexOf(item)]);
		});
		table.setItemCount(rows);
		bar.setSearchable(M3Searchable.of(table, row -> model[row]));
		bar.setText("overdue");
		shell.setSize(640, 480);
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
