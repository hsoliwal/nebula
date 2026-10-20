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

import org.eclipse.nebula.widgets.m3.M3InfoPopup;
import org.eclipse.nebula.widgets.m3.M3Placement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * An {@link M3InfoPopup} on a virtual table of 10^5 rows: rest the pointer on a cell for a hover card with the full
 * value; click a cell for a popover with details and a Copy action; Ctrl+I opens the popover of the selected row.
 * Content is built from the model arrays only when a popup opens. <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code>
 * saves the shell's pixels and exits.
 */
public class M3InfoPopupSnippet {

	public static void main(String[] args) {
		int rows = 100_000;
		String[] names = new String[rows];
		long[] sizes = new long[rows];
		for (int i = 0; i < rows; i++) {
			names[i] = "src/org/example/module" + (i % 97) + "/File" + i + ".java";
			sizes[i] = (i * 7919L) % 1_000_000;
		}
		String[] meanings = { "Path of the file, relative to the workspace", "Size in bytes on disk", "Index in the model" };
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3InfoPopup: 10^5 rows");
		shell.setLayout(new FillLayout());
		Table table = new Table(shell, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		String[] titles = { "Name", "Size", "Row" };
		int[] widths = { 220, 90, 70 };
		for (int c = 0; c < titles.length; c++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[c]);
			column.setWidth(widths[c]);
		}
		table.addListener(SWT.SetData, e -> {
			TableItem item = (TableItem) e.item;
			int i = table.indexOf(item);
			item.setText(new String[] { names[i], Long.toString(sizes[i]), Integer.toString(i) });
		});
		table.setItemCount(rows);
		M3InfoPopup popup = M3InfoPopup.forTable(table, (parent, anchor, trigger) -> {
			int row = M3Placement.rowOf(anchor), column = M3Placement.columnOf(anchor);
			parent.setLayout(new GridLayout(1, false));
			if (M3Placement.kindOf(anchor) == M3InfoPopup.HEADER) {
				new Label(parent, SWT.NONE).setText(titles[column] + ": " + meanings[column]);
				return true;
			}
			String value = column == 0 ? names[row] : column == 1 ? sizes[row] + " bytes" : Integer.toString(row);
			new Label(parent, SWT.NONE).setText(value);
			if (trigger == M3InfoPopup.CLICK) {
				new Label(parent, SWT.NONE).setText(titles[column] + " of row " + row + " (" + meanings[column] + ")");
				Button copy = new Button(parent, SWT.PUSH);
				copy.setText("Copy value");
				copy.addListener(SWT.Selection, e -> {
					Clipboard clipboard = new Clipboard(display);
					clipboard.setContents(new Object[] { value }, new Transfer[] { TextTransfer.getInstance() });
					clipboard.dispose();
				});
			}
			return true;
		});
		popup.setDelays(500, 300);
		shell.setSize(460, 420);
		shell.open();
		table.setSelection(2);
		popup.show(M3Placement.anchor(M3InfoPopup.BODY, 2, 0));
		Screenshot.runOrSave(shell, display);
	}
}
