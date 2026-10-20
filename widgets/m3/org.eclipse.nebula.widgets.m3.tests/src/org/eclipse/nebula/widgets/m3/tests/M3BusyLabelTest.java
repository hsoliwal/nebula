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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.nebula.widgets.m3.M3BusyLabel;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3BusyLabel: frames, timer start and stop, delta repaints (icon only per frame, text strip only per text change),
 * pixels after frame deltas equal a label painted at that frame directly, errors. Runs in both startup modes.
 */
public class M3BusyLabelTest {

	Display display;
	Shell shell;
	M3BusyLabel label;
	List<Rectangle> paints;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 240, 80);
		label = new M3BusyLabel(shell, SWT.NONE);
		label.setBounds(0, 0, 200, 30);
		label.setText("Loading");
		paints = M3TestSupport.recordPaints(label);
		shell.open();
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3BusyLabel", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	@Test
	public void frameRepaintsTheIconOnly() {
		label.setBusy(true);
		label.setBusy(false);
		M3TestSupport.flush(display);
		label.setBusy(true);
		label.setDelay(100_000);
		M3TestSupport.flush(display);
		paints.clear();
		label.setFrame(3);
		label.update();
		assertFalse(paints.isEmpty());
		Rectangle icon = label.getIconBounds();
		if (viewport()) {
			M3TestSupport.assertPaintsInside(paints, icon);
		} else {
			M3TestSupport.assertSomePaintCovers(paints, label.getClientArea());
		}
		paints.clear();
		label.setFrame(3);
		label.update();
		assertTrue("same frame repaints nothing", paints.isEmpty());
	}

	@Test
	public void textChangeRepaintsTheTextStripOnly() {
		paints.clear();
		label.setText("Loading 10^6 rows");
		label.update();
		assertFalse(paints.isEmpty());
		M3TestSupport.assertPaintsInside(paints, label.getTextBounds());
		paints.clear();
		label.setText("Loading 10^6 rows");
		label.update();
		assertTrue(paints.isEmpty());
	}

	@Test
	public void timerTurnsTheTrailWhileBusy() {
		label.setDelay(10);
		paints.clear();
		label.setBusy(true);
		assertTrue(label.isBusy());
		M3TestSupport.spin(display, 300);
		assertTrue("frames painted: " + paints.size(), paints.size() >= 3);
		label.setBusy(false);
		M3TestSupport.flush(display);
		int frame = label.getFrame();
		M3TestSupport.spin(display, 150);
		assertEquals("stopped", frame, label.getFrame());
	}

	@Test
	public void framePixelsEqualADirectPaint() {
		label.setBusy(true);
		label.setDelay(100_000);
		for (int f = 1; f <= 5; f++) {
			label.setFrame(f);
			label.update();
		}
		M3BusyLabel direct = new M3BusyLabel(shell, SWT.NONE);
		direct.setBounds(0, 40, 200, 30);
		direct.setText("Loading");
		direct.setBusy(true);
		direct.setDelay(100_000);
		direct.setFrame(5);
		M3TestSupport.flush(display);
		assertArrayEquals(M3TestSupport.pixels(direct).data, M3TestSupport.pixels(label).data);
	}

	@Test
	public void propertiesAndSize() {
		label.setPointCount(12);
		assertEquals(12, label.getPointCount());
		assertEquals(0, label.getFrame());
		label.setTrailLength(6);
		assertEquals(6, label.getTrailLength());
		label.setIconSize(24);
		assertEquals(24, label.getIconSize());
		assertEquals(24, label.getIconBounds().width);
		int withText = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		label.setText("");
		assertEquals(24, label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
		assertTrue(withText > 24);
		label.setPointCount(4);
		assertEquals("trail clamped to the point count", 4, label.getTrailLength());
		label.setBusy(true);
		label.dispose();
		M3TestSupport.spin(display, 50);
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		int height = label.getSize().y, old = label.getSize().x;
		M3Counters.reset();
		for (int width : M3TestSupport.sweepWidths(200)) {
			paints.clear();
			label.setSize(width, height);
			label.update();
			if (viewport()) {
				for (Rectangle paint : paints) {
					assertTrue(old + " -> " + width + ": " + paint, paint.x >= Math.min(old, width));
				}
			}
			old = width;
		}
		assertEquals(M3Counters.line(), viewport() ? 0 : 50, M3Counters.fullRepaints);
		label.setSize(old, height + 10);
		assertEquals("a height change re-centres the content", viewport() ? 1 : 51, M3Counters.fullRepaints);
	}

	@Test
	public void hiddenLabelFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		label.setDelay(100_000);
		label.setBusy(true);
		M3TestSupport.assertHiddenGate(label, paints, i -> {
			label.setFrame(i % 8);
			label.setText("t" + i);
		});
		M3BusyLabel fresh = new M3BusyLabel(shell, SWT.NONE);
		fresh.setBounds(0, 40, 200, 30);
		fresh.setText("t9999");
		fresh.setDelay(100_000);
		fresh.setBusy(true);
		fresh.setFrame(7);
		M3TestSupport.flush(display);
		assertArrayEquals(M3TestSupport.pixels(fresh).data, M3TestSupport.pixels(label).data);
	}

	@Test
	public void hiddenLabelParksItsTimer() {
		if (!viewport()) {
			return;
		}
		label.setDelay(10);
		label.setBusy(true);
		label.setVisible(false);
		M3TestSupport.spin(display, 100);
		int frame = label.getFrame();
		M3TestSupport.spin(display, 100);
		assertEquals("parked while hidden", frame, label.getFrame());
		label.setVisible(true);
		M3TestSupport.spin(display, 150);
		assertTrue("resumed", label.getFrame() != frame || paints.size() > 2);
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> label.setPointCount(2));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> label.setFrame(-1));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> label.setFrame(8));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> label.setTrailLength(0));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> label.setTrailLength(9));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> label.setDelay(0));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> label.setIconSize(7));
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> label.setText(null));
	}
}
