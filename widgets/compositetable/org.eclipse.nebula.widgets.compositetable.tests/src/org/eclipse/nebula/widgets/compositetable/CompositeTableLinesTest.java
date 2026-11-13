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
 *******************************************************************************/
package org.eclipse.nebula.widgets.compositetable;

import java.util.Arrays;

import junit.framework.TestCase;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget D leaf of CompositeTable: hiding or showing the grid lines shows at once on the header and the rows, to
 * the pixels of a full repaint (before, the old lines stayed until the next unrelated repaint).
 */
public class CompositeTableLinesTest extends TestCase {

	public static final class Header extends AbstractNativeHeader {
		public Header(Composite parent, int style) {
			super(parent, style);
			setColumnText(new String[] { "Name", "Kind" });
			setWeights(new int[] { 60, 40 });
		}
	}

	public static final class Row extends Composite {
		public Row(Composite parent, int style) {
			super(parent, style);
			setLayout(new ResizableGridRowLayout());
			new Label(this, SWT.NONE).setText("name");
			new Label(this, SWT.NONE).setText("kind");
		}
	}

	private Display display;
	private Shell shell;
	private CompositeTable table;

	@Override
	protected void setUp() throws Exception {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(400, 300);
		table = new CompositeTable(shell, SWT.NONE);
		new Header(table, SWT.NONE);
		new Row(table, SWT.NONE);
		table.setNumRowsInCollection(20);
		table.setRunTime(true);
		shell.open();
		spin();
	}

	@Override
	protected void tearDown() throws Exception {
		shell.dispose();
	}

	public void testTheLineFlagShowsAtOnce() {
		assertTrue(table.getLinesVisible());
		int[] withLines = pixels();
		table.setLinesVisible(false);
		spin();
		int[] shown = pixels();
		assertFalse("the lines were painted", Arrays.equals(withLines, fullRepaint()));
		assertTrue("hiding the lines shows without another repaint", Arrays.equals(fullRepaint(), shown));

		table.setLinesVisible(true);
		spin();
		shown = pixels();
		assertTrue("showing the lines shows without another repaint", Arrays.equals(fullRepaint(), shown));
	}

	private int[] fullRepaint() {
		Point size = table.getSize();
		table.redraw(0, 0, size.x, size.y, true);
		table.update();
		spin();
		return pixels();
	}

	private int[] pixels() {
		Point size = shell.getSize();
		Image shot = new Image(display, size.x, size.y);
		GC gc = new GC(shell);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int[] all = new int[size.x * size.y];
		for (int y = 0; y < size.y; y++) {
			data.getPixels(0, y, size.x, all, y * size.x);
		}
		return all;
	}

	private void spin() {
		long end = System.currentTimeMillis() + 250;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
}
