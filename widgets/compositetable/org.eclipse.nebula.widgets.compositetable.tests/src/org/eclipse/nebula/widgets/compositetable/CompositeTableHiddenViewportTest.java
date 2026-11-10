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
package org.eclipse.nebula.widgets.compositetable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.eclipse.nebula.widgets.compositetable.CompositeTableViewport_Test.Row;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The CompositeTable hidden gate (CompositeTableViewport, docs/m3/every-widget wave 1): while the table is hidden its
 * content pane calls no content provider, places no row, keeps the scroller and header bounds pending and keeps its
 * key filter off the Display; the reveal catches up once to the rows a table that was never hidden shows. Runs on the
 * official SWT (Hide/Show, isVisible) and on the M3 SWT fork (SWT.PartHidden/PartVisible, isPartVisible).
 */
//FLLDCIM: MARK — every-widget wave 1 compositetable: acceptance test of the hidden gate
public class CompositeTableHiddenViewportTest {

	private static final int ROWS = 1000;

	private Display display;
	private Shell first;
	private Shell second;
	private final String[] edited = new String[64];

	@Before
	public void setUp() {
		assumeTrue("viewport mode", InternalCompositeTable.VIEWPORT);
		display = Display.getDefault();
	}

	@After
	public void tearDown() {
		for (Shell shell : new Shell[] { first, second }) {
			if (shell != null && !shell.isDisposed()) {
				shell.dispose();
			}
		}
	}

	@Test
	public void hiddenTableCallsNoProviderAndRevealsTheRowsOfATableNeverHidden() {
		int[] shownCalls = { 0 };
		int[] hiddenCalls = { 0 };
		CompositeTable shown = open(10, shownCalls);
		first = shown.getShell();
		CompositeTable hidden = open(430, hiddenCalls);
		second = hidden.getShell();
		hidden.setVisible(false);
		pump();
		int before = hiddenCalls[0];
		for (CompositeTable table : new CompositeTable[] { shown, hidden }) {
			table.setNumRowsInCollection(50);
			table.getShell().setSize(400, 360);
			table.setTopRow(10);
			table.refreshAllRows();
			edited[12] = "edited";
			table.refreshRow(2);
		}
		pump();
		assertEquals("content provider calls while hidden", before, hiddenCalls[0]);
		CompositeTableViewport viewport = pane(hidden).viewport;
		assertTrue("work folded while hidden: " + viewport.deferred, viewport.deferred > 0);
		String expected = state(shown);
		hidden.setVisible(true);
		pump();
		assertEquals(expected, state(hidden));
		assertTrue("one reveal caught up: " + viewport.reveals, viewport.reveals >= 1);
	}

	@Test
	public void rowRefreshedWhileHiddenIsRefreshedOnTheReveal() {
		int[] calls = { 0 };
		CompositeTable table = open(10, calls);
		first = table.getShell();
		table.setVisible(false);
		pump();
		int before = calls[0];
		edited[3] = "changed while hidden";
		table.refreshRow(3);
		pump();
		assertEquals("no provider call while hidden", before, calls[0]);
		table.setVisible(true);
		pump();
		assertEquals("one provider call: the recorded row", before + 1, calls[0]);
		assertEquals("changed while hidden", ((Row) table.getRowControls()[3]).text.getText());
	}

	@Test
	public void rowCountWhileHiddenIsTheCountOfTheCatchUpWithoutBuildingRows() {
		int[] calls = { 0 };
		CompositeTable table = open(10, calls);
		first = table.getShell();
		table.setVisible(false);
		pump();
		table.getShell().setSize(400, 460);
		table.setNumRowsInCollection(500);
		int before = calls[0];
		int planned = table.getNumRowsVisible();
		assertEquals("no rows built for the count", before, calls[0]);
		int built = table.getRowControls().length; // a public row read catches up while hidden
		assertTrue("the read built the rows", calls[0] > before);
		assertEquals(built, planned);
	}

	@Test
	public void keyFilterIsOnTheDisplayOnlyWhileTheTableIsShown() {
		CompositeTable table = open(10, new int[1]);
		first = table.getShell();
		CompositeTableViewport viewport = pane(table).viewport;
		assertTrue("vertical slider shown: filter installed", viewport.isKeyFilterInstalled());
		table.setVisible(false);
		pump();
		assertFalse("hidden: filter parked", viewport.isKeyFilterInstalled());
		table.setVisible(true);
		pump();
		assertTrue("shown again: filter back", viewport.isKeyFilterInstalled());
		first.dispose();
		assertFalse("disposed: filter removed", viewport.isKeyFilterInstalled());
	}

	private CompositeTable open(int x, int[] calls) {
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setBounds(x, 10, 400, 300);
		CompositeTable table = new CompositeTable(shell, SWT.NONE);
		new Row(table, SWT.NONE);
		table.addRowContentProvider((sender, offset, row) -> {
			calls[0]++;
			((Row) row).text.setText(offset < edited.length && edited[offset] != null ? edited[offset] : "row " + offset);
		});
		table.setNumRowsInCollection(ROWS);
		table.setRunTime(true);
		shell.open();
		pump();
		return table;
	}

	private static InternalCompositeTable pane(CompositeTable table) {
		for (Control child : table.getChildren()) {
			if (child instanceof InternalCompositeTable) {
				return (InternalCompositeTable) child;
			}
		}
		throw new IllegalStateException("no content pane");
	}

	private static String state(CompositeTable table) {
		StringBuilder s = new StringBuilder();
		s.append(table.getTopRow()).append('|').append(table.getNumRowsVisible());
		for (Control row : table.getRowControls()) {
			s.append('|').append(((Row) row).text.getText()).append('@').append(row.getBounds());
		}
		return s.toString();
	}

	private void pump() {
		for (int i = 0; i < 10_000 && display.readAndDispatch(); i++) {
			// drain paint and async work
		}
	}
}
