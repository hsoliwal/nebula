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
package org.eclipse.nebula.widgets.cdatetime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link CDateTimeViewport} on a display (docs/m3/every-widget, aspect H): the drop-down closes when the combo or an
 * ancestor hides, a hidden combo opens nothing, and a SIMPLE combo's picker refresh held while hidden runs once on show.
 */
public class CDateTimeViewportTest {

	private Display display;
	private Shell shell;
	private Composite holder;

	@Before
	public void setUp() {
		assumeTrue("viewport mode", CDateTimeViewport.ENABLED);
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setBounds(10, 10, 320, 320);
		holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	private CDateTime combo(int style) {
		CDateTime cdt = new CDateTime(holder, CDT.BORDER | style);
		shell.open();
		shell.layout(true, true);
		flush();
		return cdt;
	}

	private void flush() {
		for (int i = 0; i < 20; i++) {
			while (display.readAndDispatch()) {
				// drain
			}
		}
	}

	private static int[] pixels(CDateTime cdt) {
		Point size = cdt.getSize();
		Image image = new Image(cdt.getDisplay(), size.x, size.y);
		GC gc = new GC(cdt);
		gc.copyArea(image, 0, 0);
		gc.dispose();
		ImageData data = image.getImageData();
		image.dispose();
		int[] out = new int[size.x * size.y];
		for (int y = 0; y < size.y; y++) {
			data.getPixels(0, y, size.x, out, y * size.x);
		}
		return out;
	}

	@Test
	public void hidingTheComboClosesItsDropDown() {
		CDateTime cdt = combo(CDT.DROP_DOWN);
		cdt.setOpen(true);
		flush();
		assertTrue("open", cdt.isOpen());
		cdt.setVisible(false);
		flush();
		assertFalse("closed by the hide", cdt.isOpen());
		assertEquals(1, cdt.viewport.closes);
	}

	@Test
	public void hidingAnAncestorClosesItsDropDown() {
		CDateTime cdt = combo(CDT.DROP_DOWN);
		cdt.setOpen(true);
		flush();
		assertTrue("open", cdt.isOpen());
		holder.setVisible(false);
		flush();
		assertFalse("closed by the ancestor's hide", cdt.isOpen());
		assertEquals(1, cdt.viewport.closes);
		assertFalse("no ancestor watch while closed", cdt.viewport.isWatching());
	}

	@Test
	public void aHiddenComboOpensNothing() {
		CDateTime cdt = combo(CDT.DROP_DOWN);
		holder.setVisible(false);
		cdt.setOpen(true);
		flush();
		assertFalse("not open", cdt.isOpen());
		assertEquals(1, cdt.viewport.refused);
		holder.setVisible(true);
		flush();
		cdt.setOpen(true);
		flush();
		assertTrue("opens once shown", cdt.isOpen());
		cdt.setOpen(false);
	}

	@Test
	public void simplePickerRefreshHeldWhileHiddenRunsOnceOnShow() {
		CDateTime cdt = combo(CDT.SIMPLE);
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -14);
		Date then = calendar.getTime();
		holder.setVisible(false);
		cdt.setSelection(then);
		flush();
		assertTrue("refresh held while hidden: " + cdt.viewport.held, cdt.viewport.held > 0);
		assertEquals("nothing caught up while hidden", 0, cdt.viewport.caughtUp);
		holder.setVisible(true);
		flush();
		assertEquals("caught up once", 1, cdt.viewport.caughtUp);
		int[] shown = pixels(cdt);
		cdt.m3RefreshPicker(); // the stock refresh of the same state
		cdt.redraw(0, 0, cdt.getSize().x, cdt.getSize().y, true);
		flush();
		cdt.update();
		assertArrayEquals("pixels equal a stock refresh", pixels(cdt), shown);
	}
}
