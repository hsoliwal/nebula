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
package org.eclipse.nebula.widgets.datechooser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link AbstractComboViewport} on a display (docs/m3/every-widget, aspect H): the popup of a {@link DateChooserCombo}
 * closes when the combo or an ancestor hides, a combo that is not shown drops nothing, and redraw works without a popup.
 */
public class DateChooserComboViewportTest {

	private Display display;
	private Shell shell;
	private Composite holder;

	@Before
	public void setUp() {
		assumeTrue("viewport mode", AbstractComboViewport.ENABLED);
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setBounds(10, 10, 320, 120);
		holder = new Composite(shell, SWT.NONE);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	private DateChooserCombo combo() {
		DateChooserCombo combo = new DateChooserCombo(holder, SWT.BORDER);
		combo.setBounds(10, 10, 200, 24);
		shell.open();
		flush();
		return combo;
	}

	private void flush() {
		for (int i = 0; i < 20; i++) {
			while (display.readAndDispatch()) {
				// drain
			}
		}
	}

	@Test
	public void hidingTheComboClosesItsPopup() {
		DateChooserCombo combo = combo();
		combo.dropDown(true);
		flush();
		assertTrue("dropped", combo.isDropped());
		combo.setVisible(false);
		flush();
		assertFalse("closed by the hide", combo.isDropped());
		assertEquals(1, combo.viewport.closes);
	}

	@Test
	public void hidingAnAncestorClosesItsPopup() {
		DateChooserCombo combo = combo();
		combo.dropDown(true);
		flush();
		assertTrue("dropped", combo.isDropped());
		holder.setVisible(false);
		flush();
		assertFalse("closed by the ancestor's hide (stock keeps it on screen)", combo.isDropped());
		assertEquals(1, combo.viewport.closes);
		assertFalse("no ancestor watch while closed", combo.viewport.isWatching());
	}

	@Test
	public void aComboThatIsNotShownDropsNothing() {
		DateChooserCombo combo = combo();
		combo.setSize(0, 0); // visible, but nothing of it on screen
		combo.dropDown(true);
		flush();
		assertFalse("not dropped", combo.isDropped());
		assertEquals(1, combo.viewport.refused);
		combo.setBounds(10, 10, 200, 24);
		combo.dropDown(true);
		flush();
		assertTrue("drops once shown", combo.isDropped());
		combo.dropDown(false);
	}

	@Test
	public void redrawWithoutAPopup() {
		DateChooserCombo combo = combo();
		combo.redraw(); // the stock redraw reads the popup without a null check
		combo.dropDown(true);
		flush();
		combo.dropDown(false); // a popup built per drop is disposed by the close
		flush();
		combo.redraw();
	}
}
