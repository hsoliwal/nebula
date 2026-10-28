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
package org.eclipse.nebula.snippets.tablecombo;

import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

/**
 * A TableCombo whose drop-down is a virtual table of 1,000,000 rows and 3 columns: rows are filled on SWT.SetData
 * when the drop-down shows them. Arguments: the row count (default 1000000); <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code>
 * saves the shell's pixels with the drop-down open and exits.
 */
public class TableComboMillionSnippet {

	public static void main(String[] args) {
		int count = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(1, false));
		shell.setText("TableCombo: " + count + " virtual rows");
		Label label = new Label(shell, SWT.NONE);
		label.setText("Pick one of " + count + " rows (SWT.VIRTUAL drop-down, rows bound on SetData)");
		TableCombo combo = new TableCombo(shell, SWT.BORDER | SWT.READ_ONLY | SWT.VIRTUAL);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		combo.defineColumns(new String[] { "Id", "Name", "Group" }, new int[] { 80, 160, 100 });
		combo.setShowTableHeader(true);
		combo.setVisibleItemCount(12);
		combo.getTable().addListener(SWT.SetData, e -> {
			TableItem item = (TableItem) e.item;
			int row = e.index;
			item.setText(new String[] { Integer.toString(row), "Row " + row, "Group " + row / 100 });
		});
		long start = System.nanoTime();
		combo.getTable().setItemCount(count);
		combo.select(count / 2);
		Label status = new Label(shell, SWT.NONE);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		status.setText("setItemCount(" + count + ") + select: " + (System.nanoTime() - start) / 1_000_000 + " ms; text " + combo.getText());
		shell.setSize(420, 400);
		shell.open();
		combo.setTableVisible(true);
		runOrSave(shell, display);
		display.dispose();
	}

	/* the event loop, or with -Dm3.snippet.screenshot=<file.png> one settled paint, a PNG of the shell and an exit */
	static void runOrSave(Shell shell, Display display) {
		String file = System.getProperty("m3.snippet.screenshot");
		if (file == null) {
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			return;
		}
		long end = System.currentTimeMillis() + 300;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		Point size = shell.getSize();
		Image image = new Image(display, Math.max(1, size.x), Math.max(1, size.y));
		GC gc = new GC(shell);
		gc.copyArea(image, 0, 0);
		gc.dispose();
		ImageLoader loader = new ImageLoader();
		loader.data = new org.eclipse.swt.graphics.ImageData[] { image.getImageData() };
		loader.save(file, SWT.IMAGE_PNG);
		image.dispose();
		shell.dispose();
	}
}
