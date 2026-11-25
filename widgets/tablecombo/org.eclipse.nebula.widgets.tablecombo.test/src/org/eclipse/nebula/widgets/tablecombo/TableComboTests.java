/*******************************************************************************
 * Copyright (c) 2017 EclipseSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
******************************************************************************/
package org.eclipse.nebula.widgets.tablecombo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TableComboTests {

	private Shell shell;

	@Before
	public final void setupTableComboTests() {
		shell = new Shell(Display.getDefault());
		shell.setLayout(new GridLayout());
	}

	@After
	public final void disposeTableComboTestsShell() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	/**
	 * Ensures that the items in the {@link TableCombo} are correctly displayed in
	 * the table widget.
	 */
	@Test
	public void testDisplayOfItems() {

		/*
		 * GIVEN a TableCombo with one column and a number of TableItems assigned to the
		 * TableCombo.
		 */
		createTableCombo(shell, 1, 5);

		/*
		 * WHEN the TableCombo gets displayed
		 */
		shell.open();

		/*
		 * THEN the number of rows displayed in the table widget of the TableCombo must
		 * match the number of TableItems.
		 */
		final SWTBot shellBot = new SWTBot(shell);
		final TableCombo tableComboWidget = shellBot.widget(WidgetMatcherFactory.widgetOfType(TableCombo.class));
		final SWTBotTable tableBot = new SWTBotTable(tableComboWidget.getTable());

		assertEquals(
				"Expected the number of items in the table widget of the TableCombo to be the same as the number of TableItems added to the TableCombo",
				5, tableBot.rowCount());

	}

	/**
	 * Ensures that the columns defined for a {@link TableCombo} are correctly
	 * displayed in the table widget.
	 */
	@Test
	public void testDisplayOfColumns() {

		/*
		 * GIVEN a TableCombo with a number of columns and a number of TableItems
		 * assigned to the TableCombo.
		 */
		createTableCombo(shell, 3, 5);

		/*
		 * WHEN the TableCombo gets displayed
		 */
		shell.open();

		/*
		 * THEN the number of columns displayed in the table widget of the TableCombo
		 * must match the configured number
		 */
		final SWTBot shellBot = new SWTBot(shell);
		final TableCombo tableComboWidget = shellBot.widget(WidgetMatcherFactory.widgetOfType(TableCombo.class));
		final SWTBotTable tableBot = new SWTBotTable(tableComboWidget.getTable());

		assertEquals(
				"Expected the number of columns in the table widget of the TableCombo to be the same as the number of columns configured in the TableCombo",
				3, tableBot.columnCount());

	}

	/**
	 * Ensures that the text displayed in tableCombo text widget corresponds to the
	 * item selected in the tableCombo.
	 */
	@Test
	public void testSelectionOfSingleItem() {

		/*
		 * GIVEN a TableCombo with one column and a number of TableItems assigned to the
		 * TableCombo.
		 */
		createTableCombo(shell, 1, 5);

		/*
		 * WHEN the TableCombo gets displayed and a single item is selected in the table
		 * combo
		 */
		shell.open();

		final SWTBot shellBot = new SWTBot(shell);
		final TableCombo tableComboWidget = shellBot.widget(WidgetMatcherFactory.widgetOfType(TableCombo.class));
		tableComboWidget.select(0);

		/*
		 * THEN the text displayed in tableCombo text widget must correspond to the item
		 * selected in the tableCombo.
		 */
		assertEquals(
				"Expected the text displayed in the tableCombo text widget to correspond to the item selected in the tableCombo",
				"Item 0", tableComboWidget.getText());

	}

	@Test
	public void testClosePopupAfterSelectionFalseKeepsPopupOpen() {
		createTableCombo(shell, 1, 5);
		shell.open();

		final SWTBot shellBot = new SWTBot(shell);
		final TableCombo tableComboWidget = shellBot.widget(WidgetMatcherFactory.widgetOfType(TableCombo.class));
		final SWTBotTable tableBot = new SWTBotTable(tableComboWidget.getTable());
		tableComboWidget.setClosePopupAfterSelection(false);
		waitUntilGeometrySettled(tableComboWidget);

		tableComboWidget.getArrowControl().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
		waitUntilPopupVisible(tableComboWidget);

		tableBot.click(0, 0);
		consumeEvents();

		assertTrue(
				"Expected the popup to remain open after selecting an item when closePopupAfterSelection is false",
				tableComboWidget.getTable().getShell().isVisible());
	}

	@Test
	public void testClosePopupAfterSelectionTrueClosesPopup() {
		createTableCombo(shell, 1, 5);
		shell.open();

		final SWTBot shellBot = new SWTBot(shell);
		final TableCombo tableComboWidget = shellBot.widget(WidgetMatcherFactory.widgetOfType(TableCombo.class));
		final SWTBotTable tableBot = new SWTBotTable(tableComboWidget.getTable());
		waitUntilGeometrySettled(tableComboWidget);

		tableComboWidget.getArrowControl().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
		waitUntilPopupVisible(tableComboWidget);

		tableBot.click(0, 0);
		consumeEvents();

		assertFalse("Expected the popup to close after selecting an item when closePopupAfterSelection is true",
				tableComboWidget.getTable().getShell().isVisible());
	}
	
	/*
	 * A window manager may move or resize a newly mapped shell after shell.open() returned (openbox does on the Linux
	 * CI: the shell's size allocation re-lays out the combo a few milliseconds later). TableCombo, like CCombo, closes its
	 * drop-down when it is resized, so a drop-down opened before the shell's final geometry arrived was closed by that
	 * resize, not by the selection under test. Open it only after the shell and the combo stopped moving and resizing
	 * for a quiet interval.
	 */
	private void waitUntilGeometrySettled(final TableCombo combo) {
		final long[] changed = { System.nanoTime() };
		final Listener listener = e -> changed[0] = System.nanoTime();
		shell.addListener(SWT.Move, listener);
		shell.addListener(SWT.Resize, listener);
		combo.addListener(SWT.Resize, listener);
		try {
			final long deadline = System.nanoTime() + 5_000_000_000L;
			while (System.nanoTime() - changed[0] < 300_000_000L) {
				assertTrue("the shell geometry did not settle", System.nanoTime() < deadline);
				if (!Display.getCurrent().readAndDispatch()) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		} finally {
			if (!shell.isDisposed()) {
				shell.removeListener(SWT.Move, listener);
				shell.removeListener(SWT.Resize, listener);
			}
			if (!combo.isDisposed()) {
				combo.removeListener(SWT.Resize, listener);
			}
		}
	}

	private void waitUntilPopupVisible(final TableCombo tableComboWidget) {
		for (int i = 0; i < 50; i++) {
			consumeEvents();
			if (!tableComboWidget.isDisposed()
					&& !tableComboWidget.getTable().isDisposed()
					&& tableComboWidget.getTable().getShell().isVisible()) {
				return;
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		assertTrue("Expected the popup to be visible after opening it",
				tableComboWidget.getTable().getShell().isVisible());
	}
	
	private TableCombo createTableCombo(final Composite parent, final int noOfColumns, final int noOfItems) {
		final TableCombo result = new TableCombo(parent, SWT.NONE);
		result.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));

		// create columns
		final String[] columns = new String[noOfColumns];
		for (int i = 0; i < noOfColumns; i++) {
			columns[i] = "Column " + i;
		}
		result.defineColumns(columns);

		// create table items
		for (int i = 0; i < noOfItems; i++) {
			createTableItem(result, "Item " + i);
		}
		return result;
	}

	private void createTableItem(final TableCombo tableCombo, final String text) {
		final TableItem tableItem = new TableItem(tableCombo.getTable(), SWT.NONE);
		tableItem.setText(text);
	}

	private void consumeEvents() {
		while (Display.getCurrent().readAndDispatch()) {
			// drain UI queue
		}
	}

}
