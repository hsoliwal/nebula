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
package org.eclipse.nebula.widgets.calendarcombo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The calendarcombo hidden gates (every-widget wave 1): the CalendarCombo and CustomCombo drop-downs close when an
 * ancestor hides and a hidden combo opens nothing; the CalendarComposite arrow repeat and the MonthPick scroll repeat
 * run on Display timers (no thread), park while hidden, catch up on the reveal and stop on release or disposal. Runs
 * on the official SWT (Hide/Show, isVisible) and on the M3 SWT fork (SWT.PartHidden/PartVisible, isPartVisible).
 */
//FLLDCIM: MARK — every-widget wave 1 calendarcombo: acceptance test of the hidden gates
public class CalendarComboViewportTest {

	private Display display;
	private Shell shell;
	private Composite parent;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setBounds(20, 20, 320, 260);
		parent = new Composite(shell, SWT.NONE);
		parent.setLayout(new FillLayout());
	}

	@After
	public void tearDown() {
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

	@Test
	public void calendarDropDownClosesWhenAnAncestorHidesAndAHiddenComboOpensNothing() throws Exception {
		assumeTrue(CalendarComboViewport.ENABLED);
		CalendarCombo combo = new CalendarCombo(parent, SWT.READ_ONLY);
		open();
		combo.openCalendar();
		pump();
		assertTrue("drop-down open", combo.m3CalendarOpen());
		CalendarComboViewport viewport = (CalendarComboViewport) field(combo, "viewport");
		parent.setVisible(false);
		pump();
		assertFalse("drop-down closed with the hidden ancestor", combo.m3CalendarOpen());
		if (org.eclipse.nebula.widgets.calendarcombo.internal.HideWatch.PART_HIDDEN < 0) {
			// stock SWT sends the ancestor's Hide before the window hides: the viewport closes it; on the M3 fork the
			// stock focus-out kill can run before SWT.PartHidden, and the viewport then finds it closed
			assertEquals(1, viewport.closes);
		}
		combo.openCalendar();
		pump();
		assertFalse("a hidden combo opens nothing", combo.m3CalendarOpen());
		assertEquals(1, viewport.refused);
		parent.setVisible(true);
		pump();
		combo.openCalendar();
		pump();
		assertTrue("shown again: opens", combo.m3CalendarOpen());
		combo.closeCalendar();
	}

	@Test
	public void customComboDropDownHidesWhenAnAncestorHides() throws Exception {
		assumeTrue(CustomComboViewport.ENABLED);
		CustomCombo combo = new CustomCombo(parent, SWT.NONE);
		combo.setItems(new String[] { "one", "two", "three" });
		open();
		combo.dropDown(true);
		pump();
		assertTrue("dropped", combo.isDropped());
		CustomComboViewport viewport = (CustomComboViewport) field(combo, "viewport");
		parent.setVisible(false);
		pump();
		assertFalse("drop-down hidden with the hidden ancestor", combo.popup.getVisible());
		assertEquals(1, viewport.closes);
	}

	@Test
	public void arrowRepeatRunsOnATimerParksWhileHiddenAndStopsOnRelease() throws Exception {
		assumeTrue(CalendarCompositeViewport.ENABLED);
		CalendarComposite calendar = new CalendarComposite(parent, start(), null, null, new DefaultColorManager(),
				new DefaultSettings(), false, null, null);
		open();
		CalendarCompositeViewport viewport = (CalendarCompositeViewport) field(calendar, "viewport");
		Rectangle left = (Rectangle) field(calendar, "mLeftArrowBounds");
		calendar.mouseDown(mouse(calendar, left.x + 1, left.y + 1));
		assertFalse("no thread is started", ((Thread) field(calendar, "mArrowThread")).isAlive());
		int afterPress = month(calendar);
		waitFor(() -> month(calendar) != afterPress, 2000);
		assertTrue("the timer stepped while shown: " + viewport.steps, viewport.steps >= 1);
		parent.setVisible(false);
		waitFor(() -> viewport.isParked(), 2000);
		int parkedMonth = month(calendar);
		spin(700);
		assertEquals("no month flips while hidden", parkedMonth, month(calendar));
		parent.setVisible(true);
		pump();
		assertTrue("the reveal caught up: " + viewport.caughtUp, viewport.caughtUp >= 2);
		assertNotEquals("months of the hidden time taken at once", parkedMonth, month(calendar));
		calendar.mouseUp(mouse(calendar, left.x + 1, left.y + 1));
		assertFalse("released: no step due", viewport.isArmed());
	}

	@Test
	public void monthPickScrollRunsOnATimerParksWhileHiddenAndStopsOnDispose() throws Exception {
		assumeTrue(MonthPickViewport.ENABLED);
		CalendarComposite calendar = new CalendarComposite(parent, start(), null, null, new DefaultColorManager(),
				new DefaultSettings(), false, null, null);
		open();
		Calendar first = start();
		MonthPick pick = new MonthPick(calendar, SWT.NONE, first, calendar, new DefaultSettings(), Locale.US);
		pick.setVisible(true);
		pump();
		MonthPickViewport viewport = (MonthPickViewport) field(pick, "viewport");
		int startMonth = first.get(Calendar.MONTH);
		pick.mouseMove(5, pick.getLocation().y - 5); // above the list: scroll up
		assertTrue("scroll repeat due", viewport.scrolling(true));
		waitFor(() -> first.get(Calendar.MONTH) != startMonth, 2000);
		pick.setVisible(false);
		waitFor(() -> viewport.parks > 0, 2000);
		int parkedMonth = first.get(Calendar.MONTH);
		spin(600);
		assertEquals("no scroll while hidden", parkedMonth, first.get(Calendar.MONTH));
		pick.setVisible(true);
		pump();
		assertTrue("the reveal caught up: " + viewport.caughtUp, viewport.caughtUp >= 2);
		pick.dispose();
		assertFalse("disposed: no repeat left", viewport.scrolling(true));
	}

	private void open() {
		shell.open();
		pump();
	}

	private static Calendar start() {
		Calendar c = Calendar.getInstance(Locale.US);
		c.set(2026, Calendar.JUNE, 15);
		return c;
	}

	private static int month(CalendarComposite calendar) {
		try {
			Calendar c = (Calendar) field(calendar, "mCalendar");
			return c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static MouseEvent mouse(CalendarComposite calendar, int x, int y) {
		Event e = new Event();
		e.widget = calendar;
		e.display = calendar.getDisplay();
		e.x = x;
		e.y = y;
		e.button = 1;
		return new MouseEvent(e);
	}

	private static Object field(Object owner, String name) throws ReflectiveOperationException {
		for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f.get(owner);
			} catch (NoSuchFieldException e) {
				// up the hierarchy
			}
		}
		throw new NoSuchFieldException(name);
	}

	private interface Condition {
		boolean met();
	}

	private void waitFor(Condition condition, long budgetMillis) {
		long end = System.currentTimeMillis() + budgetMillis;
		while (!condition.met() && System.currentTimeMillis() < end) {
			spin(10); // never display.sleep: a condition that no event meets must still time out
		}
		assertTrue("condition not met within " + budgetMillis + " ms", condition.met());
	}

	private void spin(long millis) {
		long end = System.currentTimeMillis() + millis;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	private void pump() {
		for (int i = 0; i < 10_000 && display.readAndDispatch(); i++) {
			// drain
		}
	}
}
