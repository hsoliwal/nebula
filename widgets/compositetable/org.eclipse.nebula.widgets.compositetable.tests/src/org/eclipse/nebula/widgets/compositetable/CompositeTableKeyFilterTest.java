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

import java.lang.reflect.Field;

import junit.framework.TestCase;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Every-widget leaf of the CompositeTable key filter: the display-wide KeyDown filter that keeps the focused row in
 * view acts only on this table's rows, and leaves the display when a parent disposes the table.
 */
public class CompositeTableKeyFilterTest extends TestCase {

	private static final String[] WORDS = { "Basic", "Bash", "C", "C++", "Clipper", "Cobol", "Java", "ML", "Pascal",
			"Perl", "Python", "Ruby", "Scheme", "Smalltalk", };

	private static final class Row extends Composite {
		private final Text text;

		public Row(Composite parent, int style) {
			super(parent, style);
			setLayout(new ResizableGridRowLayout());
			text = new Text(this, SWT.BORDER | SWT.SINGLE);
		}
	}

	private Display display;
	private Shell shell;
	private Composite holder;
	private CompositeTable first;
	private CompositeTable second;

	@Override
	protected void setUp() throws Exception {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(400, 200);
		holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		first = table(holder);
		second = table(shell);
		shell.open();
		spin();
		// a key press reaches a row that has focus: give each table a current row, as typing would
		first.setSelection(0, 0);
		second.setSelection(0, 0);
		spin();
	}

	@Override
	protected void tearDown() throws Exception {
		shell.dispose();
	}

	private CompositeTable table(Composite parent) {
		CompositeTable table = new CompositeTable(parent, SWT.NONE);
		new Row(table, SWT.NONE);
		table.addRowContentProvider((sender, offset, rowControl) -> ((Row) rowControl).text.setText(WORDS[offset]));
		table.setNumRowsInCollection(WORDS.length);
		table.setRunTime(true);
		return table;
	}

	public void testAKeyInAnotherTablesRowLeavesThisTableAlone() {
		first.setTopRow(5); // its current row is now above the shown rows
		spin();
		int top = first.getTopRow();
		keyDownInRowOf(second);
		spin();
		assertEquals("a key in the other table's row does not scroll this table", top, first.getTopRow());
	}

	public void testTheFilterLeavesTheDisplayWithTheTable() throws Exception {
		Listener filter = keyDownFilter(first);
		assertTrue("a table with its slider shown filters KeyDown", filtered(filter));
		holder.dispose(); // the parent disposes the table: InternalCompositeTable.dispose() is not called
		assertFalse("a disposed table keeps no display filter", filtered(filter));
		keyDownInRowOf(second); // must not reach the disposed table
	}

	private void keyDownInRowOf(CompositeTable table) {
		Control row = table.getRowControls()[0];
		Event event = new Event();
		event.keyCode = 'a';
		event.character = 'a';
		row.notifyListeners(SWT.KeyDown, event);
	}

	private static Listener keyDownFilter(CompositeTable table) throws ReflectiveOperationException {
		Field pane = CompositeTable.class.getDeclaredField("contentPane");
		pane.setAccessible(true);
		Field filter = InternalCompositeTable.class.getDeclaredField("displayKeyDownFilter");
		filter.setAccessible(true);
		return (Listener) filter.get(pane.get(table));
	}

	/* whether the display's filter table holds the listener (Display.filterTable, EventTable.listeners) */
	private boolean filtered(Listener listener) throws ReflectiveOperationException {
		Field table = Display.class.getDeclaredField("filterTable");
		table.setAccessible(true);
		Object events = table.get(display);
		if (events == null) {
			return false;
		}
		Field listeners = events.getClass().getDeclaredField("listeners");
		listeners.setAccessible(true);
		Listener[] all = (Listener[]) listeners.get(events);
		if (all != null) {
			for (Listener each : all) {
				if (each == listener) {
					return true;
				}
			}
		}
		return false;
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
