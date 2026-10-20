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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import org.eclipse.nebula.widgets.m3.M3MonthView;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
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
 * M3MonthView: geometry and hit testing, selection deltas (old and new cell only), intervals in MULTI, flagged and
 * unselectable days, keyboard paging, a day outside the shown months repaints nothing, a resize sweep, the hidden
 * gate, errors. Runs in both startup modes.
 */
public class M3MonthViewTest {

	static final long JAN_1 = LocalDate.of(2026, 1, 1).toEpochDay();

	Display display;
	Shell shell;
	M3MonthView view;
	List<Rectangle> paints;
	int selections;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 700, 500);
		view = build(shell, SWT.MULTI);
		view.setLocation(0, 0);
		view.addListener(SWT.Selection, e -> selections++);
		paints = M3TestSupport.recordPaints(view);
		shell.open();
		M3TestSupport.flush(display);
	}

	static M3MonthView build(Shell shell, int style) {
		M3MonthView view = new M3MonthView(shell, style);
		view.setFirstDayOfWeek(1);
		view.setMonthsShown(2, 1);
		view.setFirstDisplayedDay(JAN_1);
		view.setToday(JAN_1 + 9);
		view.setSize(view.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return view;
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3MonthView", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	@Test
	public void geometryAndHitTesting() {
		assertEquals(JAN_1, view.getFirstDisplayedDay());
		assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), view.getLastDisplayedDay());
		for (long day = JAN_1; day <= view.getLastDisplayedDay(); day++) {
			Rectangle cell = view.getDayBounds(day);
			assertEquals(day, view.getDayAt(cell.x + cell.width / 2, cell.y + cell.height / 2));
		}
		assertNull(view.getDayBounds(JAN_1 - 1));
		Rectangle jan1 = view.getDayBounds(JAN_1), jan5 = view.getDayBounds(JAN_1 + 4);
		assertEquals("2026-01-01 is a Thursday: column 3 of a Monday-first grid", jan5.x + 3 * jan1.width, jan1.x);
	}

	@Test
	public void selectionRepaintsOldAndNewCellsOnly() {
		view.setSelection(JAN_1 + 3);
		view.update();
		paints.clear();
		view.setSelection(JAN_1 + 20);
		view.update();
		assertEquals(2, selections);
		assertTrue(view.isSelected(JAN_1 + 20));
		assertFalse(view.isSelected(JAN_1 + 3));
		if (viewport()) {
			M3TestSupport.assertPaintsInside(paints, view.getDayBounds(JAN_1 + 3).union(view.getDayBounds(JAN_1 + 20)));
		}
		paints.clear();
		view.setFlagged(JAN_1 + 400, true);
		view.update();
		if (viewport()) {
			assertTrue("a day outside the shown months repaints nothing", paints.isEmpty());
		}
		assertTrue(view.isFlagged(JAN_1 + 400));
	}

	@Test
	public void intervalsFlagsAndUnselectableDays() {
		view.setSelectionInterval(JAN_1 + 2, JAN_1 + 12);
		assertEquals(JAN_1 + 2, view.getSelectionStart());
		assertEquals(JAN_1 + 12, view.getSelectionEnd());
		view.setUnselectable(JAN_1 + 30, true);
		assertFalse(view.setSelection(JAN_1 + 30));
		assertTrue(view.isUnselectable(JAN_1 + 30));
		view.setUnselectable(JAN_1 + 30, false);
		assertTrue(view.setSelection(JAN_1 + 30));
		for (int i = 0; i < 50; i++) {
			view.setFlagged(JAN_1 + i * 3, true);
		}
		for (int i = 0; i < 50; i += 2) {
			view.setFlagged(JAN_1 + i * 3, false);
		}
		for (int i = 0; i < 50; i++) {
			assertEquals(i % 2 == 1, view.isFlagged(JAN_1 + i * 3));
		}
		view.clearSelection();
		assertEquals(Long.MIN_VALUE, view.getSelectionStart());
		Rectangle cell = view.getDayBounds(JAN_1 + 5);
		Event down = new Event();
		down.button = 1;
		down.x = cell.x + 2;
		down.y = cell.y + 2;
		view.notifyListeners(SWT.MouseDown, down);
		Rectangle other = view.getDayBounds(JAN_1 + 9);
		down.x = other.x + 2;
		down.y = other.y + 2;
		down.stateMask = SWT.SHIFT;
		view.notifyListeners(SWT.MouseDown, down);
		assertEquals(JAN_1 + 5, view.getSelectionStart());
		assertEquals(JAN_1 + 9, view.getSelectionEnd());
	}

	@Test
	public void keyboardMovesAndPages() {
		view.setSelection(JAN_1 + 58);
		Event key = new Event();
		key.keyCode = SWT.ARROW_DOWN;
		view.notifyListeners(SWT.KeyDown, key);
		assertEquals(JAN_1 + 65, view.getSelectionStart());
		assertEquals("pages so the selection is shown", LocalDate.of(2026, 2, 1).toEpochDay(), view.getFirstDisplayedDay());
		key.keyCode = SWT.PAGE_UP;
		view.notifyListeners(SWT.KeyDown, key);
		assertEquals(LocalDate.ofEpochDay(JAN_1 + 65).minusMonths(1).toEpochDay(), view.getSelectionStart());
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		int height = view.getSize().y, old = view.getSize().x;
		M3Counters.reset();
		for (int width : M3TestSupport.sweepWidths(old)) {
			paints.clear();
			view.setSize(width, height);
			view.update();
			if (viewport()) {
				for (Rectangle paint : paints) {
					assertTrue(old + " -> " + width + ": " + paint, paint.x >= Math.min(old, width));
				}
			}
			old = width;
		}
		assertEquals(M3Counters.line(), 0, M3Counters.fullRepaints + M3Counters.measures);
	}

	@Test
	public void hiddenViewFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		Random random = new Random(3);
		long[] last = new long[1];
		M3TestSupport.assertHiddenGate(view, paints, i -> {
			long day = JAN_1 + random.nextInt(59);
			view.setFlagged(day, !view.isFlagged(day));
			last[0] = JAN_1 + random.nextInt(59);
			view.setSelection(last[0]);
		});
		M3MonthView fresh = build(shell, SWT.MULTI);
		fresh.setLocation(0, view.getSize().y + 5);
		for (long day = JAN_1; day < JAN_1 + 59; day++) {
			fresh.setFlagged(day, view.isFlagged(day));
		}
		fresh.setSelection(view.getSelectionStart());
		M3TestSupport.flush(display);
		assertArrayEquals(M3TestSupport.pixels(fresh).data, M3TestSupport.pixels(view).data);
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> view.setMonthsShown(0, 1));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> view.setFirstDayOfWeek(8));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> view.setSelectionInterval(JAN_1 + 1, JAN_1));
		M3MonthView single = new M3MonthView(shell, SWT.SINGLE);
		single.setSelectionInterval(JAN_1, JAN_1 + 5);
		assertEquals("SINGLE selects the start only", single.getSelectionStart(), single.getSelectionEnd());
	}
}
