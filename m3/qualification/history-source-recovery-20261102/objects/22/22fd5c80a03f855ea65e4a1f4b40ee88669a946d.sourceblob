/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * EclipseNebula/nebula#700: disposing the item a {@link GridEditor} edits, when the removal hides
 * the vertical scroll bar, laid the editor out on the half-removed item and failed in
 * {@code GridItem.getCellSize} with "Index out of bounds".
 */
public class GridIssue700_Test {

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setSize(400, 400);
		shell.open();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void disposingTheEditedLastItemThatHidesTheScrollBar() {
		final Grid grid = new Grid(shell, SWT.V_SCROLL | SWT.H_SCROLL);
		new GridColumn(grid, SWT.NONE).setWidth(120);
		final int rows = 6;
		for (int i = 0; i < rows; i++) {
			new GridItem(grid, SWT.NONE).setText("row " + i);
		}
		// the tallest Grid that still needs the vertical scroll bar for all rows: one row less fits
		int height = 400;
		grid.setBounds(10, 10, 200, height);
		settle(grid);
		while (!grid.getVerticalBar().getVisible() && height > 20) {
			height--;
			grid.setBounds(10, 10, 200, height);
			settle(grid);
		}
		assertTrue("precondition: vertical scroll bar shown", grid.getVerticalBar().getVisible());

		final GridItem last = grid.getItem(rows - 1);
		final GridEditor editor = new GridEditor(grid);
		editor.grabHorizontal = true;
		final Text text = new Text(grid, SWT.NONE);
		editor.setEditor(text, last, 0);
		grid.showItem(last);
		flush();

		last.dispose(); // threw IllegalArgumentException("Index out of bounds") before the fix
		flush();

		assertEquals(rows - 1, grid.getItemCount());
		assertFalse("the scroll bar was hidden by the removal", grid.getVerticalBar().getVisible());
		editor.layout();
		assertEquals("editor of the removed item is parked off screen", new Point(0, 0), text.getSize());
	}

	private void settle(final Grid grid) {
		flush();
		grid.redraw();
		grid.update();
		flush();
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain
		}
	}
}
