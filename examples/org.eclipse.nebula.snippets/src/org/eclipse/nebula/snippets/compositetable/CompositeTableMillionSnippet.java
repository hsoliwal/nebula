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
package org.eclipse.nebula.snippets.compositetable;

import org.eclipse.nebula.widgets.compositetable.AbstractNativeHeader;
import org.eclipse.nebula.widgets.compositetable.CompositeTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A CompositeTable over 1,000,000 logical rows with three Text editors per row: only the rows that fit exist as
 * controls, and each is filled by the row content provider when it scrolls in. Arguments: the row count (default
 * 1000000); <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class CompositeTableMillionSnippet {

	/** The header row. */
	public static class Header extends AbstractNativeHeader {
		public Header(Composite parent, int style) {
			super(parent, style);
			setWeights(new int[] { 100, 200, 140 });
			setColumnText(new String[] { "Id", "Name", "Group" });
		}
	}

	/** A row of three text editors. */
	public static class Row extends Composite {
		final Text id, name, group;

		public Row(Composite parent, int style) {
			super(parent, style);
			setLayout(new FillLayout(SWT.HORIZONTAL));
			id = new Text(this, SWT.BORDER);
			name = new Text(this, SWT.BORDER);
			group = new Text(this, SWT.BORDER);
		}
	}

	public static void main(String[] args) {
		int count = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		CompositeTable table = new CompositeTable(shell, SWT.NONE);
		new Header(table, SWT.NONE);
		new Row(table, SWT.NONE);
		table.setRunTime(true);
		table.addRowContentProvider((sender, currentObjectOffset, rowControl) -> {
			Row row = (Row) rowControl;
			row.id.setText(Integer.toString(currentObjectOffset));
			row.name.setText("Row " + currentObjectOffset);
			row.group.setText("Group " + currentObjectOffset / 100);
		});
		long start = System.nanoTime();
		table.setNumRowsInCollection(count);
		table.setTopRow(count / 2);
		shell.setSize(520, 400);
		shell.open();
		table.update();
		shell.setText("CompositeTable: " + count + " rows, " + table.getRowControls().length + " row controls, "
				+ (System.nanoTime() - start) / 1_000_000 + " ms");
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
