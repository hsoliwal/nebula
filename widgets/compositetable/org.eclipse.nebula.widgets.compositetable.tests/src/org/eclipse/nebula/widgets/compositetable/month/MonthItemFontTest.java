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
package org.eclipse.nebula.widgets.compositetable.month;

import junit.framework.TestCase;

import org.eclipse.nebula.widgets.compositetable.month.internal.MonthCalendarableItemControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget D leaf of the month calendar item: setting the font the item already uses repaints nothing; a new font
 * repaints it.
 */
public class MonthItemFontTest extends TestCase {

	private Display display;
	private Shell shell;
	private MonthCalendarableItemControl item;
	private int paints;

	@Override
	protected void setUp() throws Exception {
		display = Display.getDefault();
		shell = new Shell(display);
		item = new MonthCalendarableItemControl(shell, SWT.NONE);
		item.setText("Meeting");
		item.setBounds(10, 10, 150, 20);
		item.addListener(SWT.Paint, e -> paints++);
		shell.setSize(220, 80);
		shell.open();
		spin();
		item.update();
		spin();
	}

	@Override
	protected void tearDown() throws Exception {
		shell.dispose();
	}

	public void testTheSameFontRepaintsNothing() {
		paints = 0;
		spin();
		int settled = paints; // paints that arrive without any change (none once the item is drawn)
		paints = 0;
		item.setFont(item.getFont());
		spin();
		assertEquals("the same font: no repaint (" + settled + " paint(s) while settling)", 0, paints);
	}

	public void testANewFontRepaints() {
		FontData data = item.getFont().getFontData()[0];
		Font bold = new Font(display, data.getName(), data.getHeight(), SWT.BOLD);
		try {
			paints = 0;
			item.setFont(bold);
			spin();
			assertTrue("a new font repaints the item", paints > 0);
			assertEquals(bold, item.getFont());
		} finally {
			item.setFont(null);
			bold.dispose();
		}
	}

	private void spin() {
		long end = System.currentTimeMillis() + 150;
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
