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
package org.eclipse.nebula.widgets.cdatetime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.cwt.v.VNative;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget H leaf of CDatePanel: resizes while the panel is hidden, or collapsed to no room for a month, keep the
 * months (before, a collapse disposed them all and the restore created them again); the reveal applies the size, to
 * the month count of a panel laid out at that size.
 */
public class CDatePanelHiddenResizeTest {

	private Display display;
	private Shell shell;
	private Composite holder;
	private CDatePanel panel;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		panel = new CDatePanel(holder, SWT.NONE);
		shell.setSize(320, 700);
		shell.open();
		spin();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void hiddenResizesKeepTheMonths() throws Exception {
		List<CDateTime> before = months(panel);
		assertTrue("several months fit", before.size() > 1);

		holder.setVisible(false);
		shell.setSize(320, 200);
		spin();
		shell.setSize(320, 700);
		spin();
		for (CDateTime month : before) {
			assertFalse("a hidden shrink and grow disposes no month", month.isDisposed());
		}
		holder.setVisible(true);
		spin();
		assertEquals(before, months(panel));
	}

	@Test
	public void aCollapseKeepsTheMonths() throws Exception {
		List<CDateTime> before = months(panel);
		shell.setSize(320, 40);
		spin();
		shell.setSize(320, 700);
		spin();
		for (CDateTime month : before) {
			assertFalse("a collapse and restore disposes no month", month.isDisposed());
		}
		assertEquals(before, months(panel));
	}

	@Test
	public void theRevealAppliesTheHiddenSize() throws Exception {
		int tall = months(panel).size();
		holder.setVisible(false);
		shell.setSize(320, 380);
		spin();
		holder.setVisible(true);
		spin();
		Shell other = new Shell(display);
		other.setLayout(new FillLayout());
		CDatePanel reference = new CDatePanel(other, SWT.NONE);
		other.setSize(320, 380);
		other.open();
		spin();
		int expected = months(reference).size();
		other.dispose();
		assertTrue(expected < tall);
		assertEquals("the reveal sizes the month count", expected, months(panel).size());
	}

	@SuppressWarnings("unchecked")
	private static List<CDateTime> months(CDatePanel panel) throws Exception {
		Field field = CDatePanel.class.getDeclaredField("pickers");
		field.setAccessible(true);
		List<CDateTime> controls = new ArrayList<>();
		for (VNative<CDateTime> picker : (List<VNative<CDateTime>>) field.get(panel)) {
			controls.add(picker.getControl());
		}
		return controls;
	}

	private void spin() {
		long end = System.currentTimeMillis() + 300;
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
