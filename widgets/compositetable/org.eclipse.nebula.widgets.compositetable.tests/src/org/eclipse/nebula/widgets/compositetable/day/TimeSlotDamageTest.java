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
package org.eclipse.nebula.widgets.compositetable.day;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.eclipse.nebula.widgets.compositetable.day.internal.TimeSlot;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget D leaf of the day editor's time slot: a new time that draws the same lines (no hour or half-hour line
 * change) repaints nothing, and a time that changes them repaints to the same pixels as a full repaint.
 */
public class TimeSlotDamageTest extends TestCase {

	private Display display;
	private Shell shell;
	private TimeSlot slot;
	private int paints;

	@Override
	protected void setUp() throws Exception {
		display = Display.getDefault();
		shell = new Shell(display);
		slot = new TimeSlot(shell, SWT.NONE);
		slot.setBounds(10, 10, 200, 24);
		slot.addListener(SWT.Paint, e -> paints++);
		slot.setTime(at(10, 15));
		shell.setSize(260, 100);
		shell.open();
		spin();
	}

	@Override
	protected void tearDown() throws Exception {
		shell.dispose();
	}

	public void testATimeWithTheSameLinesRepaintsNothing() {
		paints = 0;
		slot.setTime(at(10, 45));
		spin();
		assertEquals("10:15 -> 10:45 draws the same lines: no repaint", 0, paints);
		assertFalse(slot.isMinutesAfterHour(15));
		assertTrue(slot.isMinutesAfterHour(45));
	}

	public void testATimeThatChangesTheLinesRepaintsLikeAFullRepaint() {
		for (int minute : new int[] { 0, 30, 20, 0 }) {
			paints = 0;
			slot.setTime(at(11, minute));
			spin();
			assertTrue("11:" + minute + " changes the lines: repaint", paints > 0);
			int[] incremental = pixels();
			slot.redraw();
			slot.update();
			spin();
			assertTrue("11:" + minute + ": the pixels equal a full repaint", Arrays.equals(incremental, pixels()));
		}
	}

	private static Date at(int hour, int minute) {
		Calendar calendar = new GregorianCalendar(2026, Calendar.NOVEMBER, 10, hour, minute);
		return calendar.getTime();
	}

	private int[] pixels() {
		Rectangle area = slot.getClientArea();
		Image shot = new Image(display, area.width, area.height);
		GC gc = new GC(slot);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int[] all = new int[area.width * area.height];
		for (int y = 0; y < area.height; y++) {
			data.getPixels(0, y, area.width, all, y * area.width);
		}
		return all;
	}

	private void spin() {
		long end = System.currentTimeMillis() + 150;
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
