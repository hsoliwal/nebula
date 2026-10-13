/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.tablecombo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * EclipseNebula/nebula#705: {@link TableCombo#setClosePopupAfterSelection(boolean)} with {@code false} must keep the
 * drop-down open after a row is selected, also when the selected row has an image (the selected image is shown in the
 * combo by default, which re-lays out the combo).
 */
public class TableComboClosePopupTests {

	private Display display;
	private Shell shell;
	private Image image;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new GridLayout());
		shell.setSize(400, 300);
		image = new Image(display, 12, 12);
	}

	@After
	public void tearDown() {
		shell.dispose();
		image.dispose();
	}

	@Test
	public void popupStaysOpenAfterSelectionWhenClosePopupAfterSelectionIsFalse() throws Exception {
		/*
		 * GIVEN a TableCombo whose rows have images and closePopupAfterSelection = false, with its drop-down open.
		 */
		final TableCombo combo = createTableCombo();
		combo.setClosePopupAfterSelection(false);
		shell.open();
		dropDown(combo, true);
		final Table table = combo.getTable();
		assertTrue("drop-down open", table.getShell().isVisible());

		/*
		 * WHEN the user selects a row (selection, then mouse up).
		 */
		select(table, 1);
		mouseUp(table);

		/*
		 * THEN the drop-down is still open and the selection is applied.
		 */
		assertTrue("drop-down must stay open", combo.getTable().getShell().isVisible());
		assertEquals(1, combo.getSelectionIndex());

		select(combo.getTable(), 3);
		mouseUp(combo.getTable());
		assertTrue("drop-down must stay open for further selections", combo.getTable().getShell().isVisible());
		assertEquals(3, combo.getSelectionIndex());
	}

	@Test
	public void popupClosesAfterSelectionByDefault() throws Exception {
		/*
		 * GIVEN a TableCombo with the default closePopupAfterSelection = true and its drop-down open.
		 */
		final TableCombo combo = createTableCombo();
		shell.open();
		dropDown(combo, true);

		/*
		 * WHEN the user selects a row.
		 */
		select(combo.getTable(), 2);
		mouseUp(combo.getTable());

		/*
		 * THEN the drop-down is closed and the selection is applied.
		 */
		assertFalse("drop-down must close", combo.getTable().getShell().isVisible());
		assertEquals(2, combo.getSelectionIndex());
	}

	private TableCombo createTableCombo() {
		final TableCombo combo = new TableCombo(shell, SWT.BORDER | SWT.READ_ONLY);
		final Table table = combo.getTable();
		new TableColumn(table, SWT.NONE).setWidth(120);
		for (int i = 0; i < 5; i++) {
			final TableItem item = new TableItem(table, SWT.NONE);
			item.setText("row " + i);
			item.setImage(image);
		}
		shell.layout(true, true);
		return combo;
	}

	private void dropDown(final TableCombo combo, final boolean drop) throws Exception {
		final Method method = TableCombo.class.getDeclaredMethod("dropDown", boolean.class);
		method.setAccessible(true);
		method.invoke(combo, Boolean.valueOf(drop));
		pump();
	}

	private void select(final Table table, final int index) {
		table.setSelection(index);
		final Event event = new Event();
		event.item = table.getItem(index);
		table.notifyListeners(SWT.Selection, event);
		pump();
	}

	private void mouseUp(final Table table) {
		final Event event = new Event();
		event.button = 1;
		table.notifyListeners(SWT.MouseUp, event);
		pump();
	}

	private void pump() {
		for (int i = 0; i < 1_000 && display.readAndDispatch(); i++) {
			// drain pending layout and paint work
		}
	}
}
