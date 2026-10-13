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
package org.eclipse.nebula.widgets.compositetable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * CompositeTable viewport: a 1,000,000-row collection keeps only the rows in view; rows are not built while hidden;
 * row editor values, focus and caret are reproduced after the row scrolls out, is recycled, and scrolls back.
 */
public class CompositeTableViewport_Test {
	private static final int ROWS = 1_000_000;

	/** Row prototype with one editor. */
	public static final class Row extends Composite {
		final Text text;

		public Row(Composite parent, int style) {
			super(parent, style);
			setLayout(new ResizableGridRowLayout());
			text = new Text(this, SWT.BORDER | SWT.SINGLE);
		}
	}

	private Display display;
	private Shell shell;
	private CompositeTable table;
	private final String[] edited = new String[16];

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setBounds(10, 10, 400, 300);
		table = new CompositeTable(shell, SWT.NONE);
		new Row(table, SWT.NONE);
		table.addRowContentProvider((sender, offset, row) -> {
			String value = offset < edited.length && edited[offset] != null ? edited[offset] : "row " + offset;
			((Row) row).text.setText(value);
		});
		table.setNumRowsInCollection(ROWS);
		table.setRunTime(true);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void rowsAreBuiltOnlyWhenShownAndOnlyForTheView() {
		shell.layout(true, true);
		int hidden = table.getRowControls().length;
		shell.open();
		pump();
		Control[] rows = table.getRowControls();
		assertEquals("a public row read catches up once", rows.length, hidden);
		assertTrue("rows " + rows.length, rows.length > 0 && rows.length < 64);
		table.setTopRow(ROWS - 1);
		pump();
		assertTrue(table.getRowControls().length < 64);
	}

	@Test
	public void editorStateSurvivesScrollingAwayAndBack() {
		shell.open();
		pump();
		Row second = (Row) table.getRowControls()[2];
		second.text.setText("edited");
		edited[2] = "edited";
		second.text.setFocus();
		second.text.setSelection(3);
		pump();
		String before = state();
		String pixels = pixels(table);

		table.setTopRow(500_000);
		pump();
		table.setTopRow(0);
		pump();
		assertEquals(before, state());
		assertEquals(pixels, pixels(table));
	}

	private String state() {
		StringBuilder s = new StringBuilder();
		s.append(table.getTopRow()).append('|').append(table.getSelection());
		Control focus = display.getFocusControl();
		Control[] rows = table.getRowControls();
		for (int i = 0; i < rows.length; i++) {
			Text text = ((Row) rows[i]).text;
			s.append('|').append(text.getText());
			if (focus == text) {
				s.append("*caret=").append(text.getCaretPosition());
			}
		}
		return s.toString();
	}

	private static String pixels(Control control) {
		Point size = control.getSize();
		Image image = new Image(control.getDisplay(), size.x, size.y);
		GC gc = new GC(image);
		try {
			control.print(gc);
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
			md.update(image.getImageData().data);
			StringBuilder hex = new StringBuilder();
			for (byte b : md.digest()) {
				hex.append(String.format("%02x", b & 0xff));
			}
			return hex.toString();
		} catch (java.security.NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		} finally {
			gc.dispose();
			image.dispose();
		}
	}

	private void pump() {
		for (int i = 0; i < 10_000 && display.readAndDispatch(); i++) {
			// drain paint and async row work
		}
	}
}
