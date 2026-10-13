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
package org.eclipse.nebula.widgets.calendarcombo;

import static org.junit.Assert.assertEquals;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.junit.Test;

/**
 * EclipseNebula/nebula#612: the calendar pop-up stays on the monitor that shows the combo. A secondary monitor left of
 * the primary one has negative x; the pop-up used to be clamped to x = 0, i.e. onto the primary monitor.
 */
public class CalendarComboPopupLocationTest {
	private static final Point POPUP = new Point(200, 180);
	/** primary monitor 1920 wide at x = 0 */
	private static final Rectangle PRIMARY = new Rectangle(0, 0, 1920, 1040);
	/** secondary monitor 1920 wide left of the primary one */
	private static final Rectangle LEFT_SECONDARY = new Rectangle(-1920, 0, 1920, 1040);

	@Test
	public void popupOnLeftSecondaryMonitorStaysThere() {
		assertEquals(new Point(-1500, 300), CalendarCombo.keepOnMonitor(new Point(-1500, 300), POPUP, LEFT_SECONDARY));
	}

	@Test
	public void popupIsKeptInsideTheLeftEdgeOfItsOwnMonitor() {
		assertEquals(new Point(-1920, 300), CalendarCombo.keepOnMonitor(new Point(-1950, 300), POPUP, LEFT_SECONDARY));
	}

	@Test
	public void popupIsKeptInsideTheRightEdgeOfItsOwnMonitor() {
		// right edge of the left monitor is x = 0: the pop-up must not spill onto the primary monitor
		assertEquals(new Point(-200, 300), CalendarCombo.keepOnMonitor(new Point(-100, 300), POPUP, LEFT_SECONDARY));
		assertEquals(new Point(1720, 300), CalendarCombo.keepOnMonitor(new Point(1800, 300), POPUP, PRIMARY));
	}

	@Test
	public void primaryMonitorPlacementIsUnchanged() {
		assertEquals(new Point(0, 40), CalendarCombo.keepOnMonitor(new Point(-30, 40), POPUP, PRIMARY));
		assertEquals(new Point(500, 40), CalendarCombo.keepOnMonitor(new Point(500, 40), POPUP, PRIMARY));
	}
}
