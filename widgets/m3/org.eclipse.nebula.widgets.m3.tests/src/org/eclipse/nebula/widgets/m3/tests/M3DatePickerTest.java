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
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.eclipse.nebula.widgets.m3.M3DatePicker;
import org.eclipse.nebula.widgets.m3.M3MonthView;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3DatePicker: the field shows the date, typed text is parsed on Enter (an unparsable text is restored), the drop-down
 * month view is created once and reused, picking a day sets the date and closes nothing it should not, a closed
 * drop-down takes flagged days without painting and paints once when opened, errors.
 */
public class M3DatePickerTest {

	static final long OCT_20 = LocalDate.of(2026, 10, 20).toEpochDay();

	Display display;
	Shell shell;
	M3DatePicker picker;
	int selections;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 300, 80);
		picker = new M3DatePicker(shell, SWT.NONE);
		picker.setBounds(0, 0, 200, 30);
		picker.addListener(SWT.Selection, e -> selections++);
		shell.open();
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void fieldShowsAndParsesTheDate() {
		picker.setDate(OCT_20);
		assertEquals("2026-10-20", picker.getTextField().getText());
		assertEquals(1, selections);
		picker.getTextField().setText("2026-11-03");
		picker.getTextField().notifyListeners(SWT.DefaultSelection, new Event());
		assertEquals(LocalDate.of(2026, 11, 3).toEpochDay(), picker.getDate());
		picker.getTextField().setText("not a date");
		picker.getTextField().notifyListeners(SWT.DefaultSelection, new Event());
		assertEquals("restored", "2026-11-03", picker.getTextField().getText());
		picker.getTextField().setText("");
		picker.getTextField().notifyListeners(SWT.DefaultSelection, new Event());
		assertEquals(M3DatePicker.NO_DATE, picker.getDate());
	}

	@Test
	public void dropDownIsReusedAndPicksADay() {
		picker.setDate(OCT_20);
		picker.setPopupVisible(true);
		M3TestSupport.flush(display);
		assertTrue(picker.isPopupVisible());
		M3MonthView view = picker.getMonthView();
		assertEquals(OCT_20, view.getSelectionStart());
		Rectangle cell = view.getDayBounds(OCT_20 + 2);
		Event down = new Event();
		down.button = 1;
		down.x = cell.x + 2;
		down.y = cell.y + 2;
		view.notifyListeners(SWT.MouseDown, down);
		assertEquals(OCT_20 + 2, picker.getDate());
		view.notifyListeners(SWT.MouseUp, down);
		assertFalse(picker.isPopupVisible());
		picker.setPopupVisible(true);
		assertSame("reused", view, picker.getMonthView());
		picker.setPopupVisible(false);
	}

	@Test
	public void closedDropDownFoldsDeltasAndPaintsOnceWhenOpened() {
		if (WidgetMode.of("M3MonthView", WidgetMode.VIEWPORT) != WidgetMode.VIEWPORT) { //$NON-NLS-1$
			return;
		}
		picker.setDate(OCT_20);
		M3MonthView view = picker.getMonthView();
		List<Rectangle> paints = M3TestSupport.recordPaints(view);
		for (int i = 0; i < 10_000; i++) {
			view.setFlagged(OCT_20 + i % 40, i % 3 != 0);
		}
		M3TestSupport.flush(display);
		assertEquals("no paint while the drop-down is closed", 0, paints.size());
		picker.setPopupVisible(true);
		M3TestSupport.flush(display);
		view.update();
		assertEquals("one paint pass on open: " + paints, 1, paints.size());
		picker.setPopupVisible(false);
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> picker.setFormat(null));
		picker.setFormat(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		picker.setDate(OCT_20);
		assertEquals("20.10.2026", picker.getTextField().getText());
	}
}
