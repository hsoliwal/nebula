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
package org.eclipse.nebula.widgets.grid.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * InfoPopup and PopupPlacement: placement with flip and shift, anchor packing, a click popover on a table cell
 * (content built only on open, placed next to the cell), Escape and a click elsewhere close it, hover after the show
 * delay, keyboard open for the selected row, content that declines. No pointer is moved: events are notified.
 */
public class InfoPopup_Test {

	Display display;
	Shell shell;
	Table table;
	InfoPopup popup;
	int fills;
	long lastFilled = InfoPopup.NONE;
	int lastTrigger;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(300, 200, 400, 300);
		table = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION);
		table.setBounds(0, 0, 380, 280);
		table.setHeaderVisible(true);
		for (int c = 0; c < 3; c++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText("column " + c);
			column.setWidth(110);
		}
		for (int r = 0; r < 20; r++) {
			new TableItem(table, SWT.NONE).setText(new String[] { "name " + r, Integer.toString(r * 1024), "2026-10-" + (r + 1) });
		}
		popup = InfoPopup.forTable(table, (parent, anchor, trigger) -> {
			fills++;
			lastFilled = anchor;
			lastTrigger = trigger;
			if (PopupPlacement.columnOf(anchor) == 2 && PopupPlacement.rowOf(anchor) == 0) {
				return false;
			}
			new Label(parent, SWT.NONE).setText("row " + PopupPlacement.rowOf(anchor) + " column " + PopupPlacement.columnOf(anchor));
			return true;
		});
		shell.open();
		TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	Point cellCentre(int row, int column) {
		Rectangle cell = table.getItem(row).getBounds(column);
		return new Point(cell.x + cell.width / 2, cell.y + cell.height / 2);
	}

	@Test
	public void placementFlipsAndShifts() {
		int[] out = new int[3];
		PopupPlacement.place(100, 100, 50, 20, 80, 40, 0, 0, 1000, 800, PopupPlacement.BOTTOM, 4, out);
		assertEquals(100, out[0]);
		assertEquals(124, out[1]);
		assertEquals(PopupPlacement.BOTTOM, out[2]);
		PopupPlacement.place(100, 770, 50, 20, 80, 40, 0, 0, 1000, 800, PopupPlacement.BOTTOM, 4, out);
		assertEquals("flipped above", PopupPlacement.TOP, out[2]);
		assertEquals(770 - 4 - 40, out[1]);
		PopupPlacement.place(960, 100, 30, 20, 80, 40, 0, 0, 1000, 800, PopupPlacement.BOTTOM, 4, out);
		assertEquals("shifted inside", 920, out[0]);
		PopupPlacement.place(950, 100, 40, 20, 80, 40, 0, 0, 1000, 800, PopupPlacement.RIGHT, 4, out);
		assertEquals(PopupPlacement.LEFT, out[2]);
		assertEquals(950 - 4 - 80, out[0]);
		long anchor = PopupPlacement.anchor(InfoPopup.BODY, 1_000_000, 7);
		assertEquals(InfoPopup.BODY, PopupPlacement.kindOf(anchor));
		assertEquals(1_000_000, PopupPlacement.rowOf(anchor));
		assertEquals(7, PopupPlacement.columnOf(anchor));
		assertEquals(-1, PopupPlacement.rowOf(PopupPlacement.anchor(InfoPopup.HEADER, -1, 2)));
	}

	@Test
	public void clickOpensAPopoverNextToTheCell() {
		assertEquals("nothing is built before a popup opens", 0, fills);
		Point at = cellCentre(3, 1);
		Event down = new Event();
		down.button = 1;
		down.x = at.x;
		down.y = at.y;
		table.notifyListeners(SWT.MouseDown, down);
		TestSupport.flush(display);
		assertTrue(popup.isVisible());
		assertEquals(1, fills);
		assertEquals(PopupPlacement.anchor(InfoPopup.BODY, 3, 1), popup.getAnchor());
		assertEquals(InfoPopup.CLICK, lastTrigger);
		Rectangle cell = table.getItem(3).getBounds(1);
		Point cellBottom = table.toDisplay(cell.x, cell.y + cell.height);
		Rectangle card = popup.getShell().getBounds();
		assertTrue(card + " below " + cellBottom, card.y >= cellBottom.y || card.y + card.height <= cellBottom.y - cell.height);
		table.notifyListeners(SWT.MouseDown, down);
		assertEquals("the same anchor does not rebuild", 1, fills);
		Event key = new Event();
		key.keyCode = SWT.ESC;
		table.notifyListeners(SWT.KeyDown, key);
		assertFalse(popup.isVisible());
		table.notifyListeners(SWT.MouseDown, down);
		assertTrue(popup.isVisible());
		Event outside = new Event();
		outside.button = 1;
		// the header right of the last column (3 x 110 px): no anchor at any row height; a body point is a cell
		// whenever the 20 rows overflow the table, as they do at 96 dpi with the default font
		outside.x = 340;
		outside.y = 2;
		table.notifyListeners(SWT.MouseDown, outside);
		assertFalse("a click on no anchor closes it", popup.isVisible());
	}

	@Test
	public void hiddenOwnerClosesThePopupAndDropsThePendingHover() {
		if (WidgetMode.of("InfoPopup", WidgetMode.VIEWPORT) != WidgetMode.VIEWPORT) { //$NON-NLS-1$
			return;
		}
		popup.setDelays(40, 40);
		Point at = cellCentre(5, 0);
		Event move = new Event();
		move.x = at.x;
		move.y = at.y;
		table.notifyListeners(SWT.MouseMove, move);
		table.setVisible(false);
		TestSupport.spin(display, 150);
		assertFalse("the pending hover is dropped with the owner", popup.isVisible());
		assertEquals("nothing built for a hidden owner", 0, fills);
		table.setVisible(true);
		TestSupport.flush(display);
		Point cell = cellCentre(3, 1);
		Event down = new Event();
		down.button = 1;
		down.x = cell.x;
		down.y = cell.y;
		table.notifyListeners(SWT.MouseDown, down);
		TestSupport.flush(display);
		assertTrue(popup.isVisible());
		table.setVisible(false);
		TestSupport.flush(display);
		assertFalse("the open popup closes with its owner", popup.isVisible());
		table.notifyListeners(SWT.MouseDown, down);
		TestSupport.flush(display);
		assertFalse("a click on a hidden owner opens nothing", popup.isVisible());
	}

	@Test
	public void hoverOpensAfterTheShowDelay() {
		popup.setDelays(40, 40);
		Point at = cellCentre(5, 0);
		Event move = new Event();
		move.x = at.x;
		move.y = at.y;
		table.notifyListeners(SWT.MouseMove, move);
		assertFalse("not before the delay", popup.isVisible());
		TestSupport.spin(display, 150);
		assertTrue(popup.isVisible());
		assertEquals(PopupPlacement.anchor(InfoPopup.BODY, 5, 0), popup.getAnchor());
		assertEquals(InfoPopup.HOVER, lastTrigger);
		Point next = cellCentre(6, 2);
		move.x = next.x;
		move.y = next.y;
		table.notifyListeners(SWT.MouseMove, move);
		TestSupport.spin(display, 150);
		assertEquals(PopupPlacement.anchor(InfoPopup.BODY, 6, 2), lastFilled);
	}

	@Test
	public void keyboardOpensTheSelectedRowAndContentMayDecline() {
		table.setSelection(4);
		Event key = new Event();
		key.keyCode = 'i';
		key.stateMask = SWT.MOD1;
		table.notifyListeners(SWT.KeyDown, key);
		assertTrue(popup.isVisible());
		assertEquals(PopupPlacement.anchor(InfoPopup.BODY, 4, 0), popup.getAnchor());
		popup.hide();
		assertFalse(popup.show(PopupPlacement.anchor(InfoPopup.BODY, 0, 2)));
		assertFalse(popup.isVisible());
		assertFalse("a row that is not there", popup.show(PopupPlacement.anchor(InfoPopup.BODY, 99, 0)));
		TestSupport.expect(SWT.ERROR_NULL_ARGUMENT, () -> new InfoPopup(table, null, (p, a, t) -> true));
		TestSupport.expect(SWT.ERROR_INVALID_ARGUMENT, () -> popup.setDelays(-1, 0));
		TestSupport.expect(SWT.ERROR_INVALID_ARGUMENT, () -> popup.setPlacement(9, 0));
	}
}
