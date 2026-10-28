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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.nebula.widgets.m3.M3GradientChooser;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3GradientChooser: stops (add, move clamped between neighbours, recolour, remove, selection), interpolation and
 * lookup tables, each change repaints only the strip between the changed stop's neighbours, pixels after deltas equal
 * a chooser built with the final stops, mouse add and drag, the hidden gate, errors. Runs in both startup modes.
 */
public class M3GradientChooserTest {

	Display display;
	Shell shell;
	M3GradientChooser chooser;
	List<Rectangle> paints;
	int modifies;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 420, 120);
		chooser = build(0);
		chooser.addListener(SWT.Modify, e -> modifies++);
		paints = M3TestSupport.recordPaints(chooser);
		shell.open();
		M3TestSupport.flush(display);
	}

	M3GradientChooser build(int y) {
		M3GradientChooser built = new M3GradientChooser(shell, SWT.NONE);
		built.setBounds(0, y, 400, 50);
		built.setStops(new float[] { 0f, 0.25f, 0.5f, 1f }, new RGB[] { new RGB(0, 0, 160), new RGB(0, 200, 220), new RGB(255, 230, 0), new RGB(200, 0, 0) });
		return built;
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3GradientChooser", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	@Test
	public void stopsInterpolateAndSample() {
		assertEquals(4, chooser.getStopCount());
		assertEquals(0x0000a0, chooser.colorAt(0f));
		assertEquals(0xc80000, chooser.colorAt(1f));
		assertEquals(0x00c8dc, chooser.colorAt(0.25f));
		int mid = chooser.colorAt(0.125f);
		assertEquals(0x00, (mid >> 16) & 0xff);
		assertEquals(100, (mid >> 8) & 0xff);
		int[] table = chooser.toLookupTable(256);
		assertEquals(0x0000a0, table[0]);
		assertEquals(0xc80000, table[255]);
	}

	@Test
	public void moveRepaintsBetweenTheNeighboursOnly() {
		chooser.update();
		paints.clear();
		chooser.moveStop(1, 0.3f);
		chooser.update();
		assertEquals(0.3f, chooser.getStopPosition(1), 0f);
		if (viewport()) {
			Rectangle bar = chooser.getBarBounds();
			int x0 = 0, x1 = chooser.xOf(0.5f) + 11;
			M3TestSupport.assertPaintsInside(paints, new Rectangle(x0, bar.y, x1 - x0, bar.height + 13));
		}
		chooser.moveStop(1, 0.9f);
		assertEquals("clamped at the right neighbour", 0.5f, chooser.getStopPosition(1), 0f);
		paints.clear();
		chooser.setStopColor(3, new RGB(120, 0, 120));
		chooser.update();
		if (viewport()) {
			for (Rectangle paint : paints) {
				assertTrue("left of 0.5 untouched: " + paint, paint.x >= chooser.xOf(0.5f) - 11);
			}
		}
		assertEquals(3, modifies);
	}

	@Test
	public void addRemoveSelectAndPixels() {
		int stop = chooser.addStop(0.75f, new RGB(0, 255, 0));
		assertEquals(3, stop);
		assertEquals(5, chooser.getStopCount());
		chooser.setSelectedStop(stop);
		assertEquals(3, chooser.getSelectedStop());
		chooser.removeStop(1);
		assertEquals("selection follows the removal", 2, chooser.getSelectedStop());
		chooser.removeStop(2);
		assertEquals(-1, chooser.getSelectedStop());
		M3GradientChooser fresh = new M3GradientChooser(shell, SWT.NONE);
		fresh.setBounds(0, 60, 400, 50);
		fresh.setStops(new float[] { 0f, 0.5f, 1f }, new RGB[] { new RGB(0, 0, 160), new RGB(255, 230, 0), new RGB(200, 0, 0) });
		M3TestSupport.flush(display);
		M3TestSupport.assertSamePixels("gradient-after-deltas", M3TestSupport.pixels(fresh), M3TestSupport.pixels(chooser));
	}

	@Test
	public void mouseAddsAndDragsAStop() {
		Rectangle bar = chooser.getBarBounds();
		Event down = new Event();
		down.button = 1;
		down.x = chooser.xOf(0.75f);
		down.y = bar.y + bar.height + 4;
		chooser.notifyListeners(SWT.MouseDown, down);
		assertEquals(5, chooser.getStopCount());
		assertEquals(3, chooser.getSelectedStop());
		Event move = new Event();
		move.x = chooser.xOf(0.8f);
		move.y = down.y;
		move.stateMask = SWT.BUTTON1;
		chooser.notifyListeners(SWT.MouseMove, move);
		chooser.notifyListeners(SWT.MouseUp, new Event());
		assertEquals(0.8f, chooser.getStopPosition(3), 0.005f);
		Event key = new Event();
		key.keyCode = SWT.DEL;
		chooser.notifyListeners(SWT.KeyDown, key);
		assertEquals(4, chooser.getStopCount());
	}

	@Test
	public void hiddenChooserFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		M3TestSupport.assertHiddenGate(chooser, paints, i -> {
			chooser.moveStop(1, 0.05f + (i % 40) / 100f);
			chooser.setStopColor(2, new RGB(i & 0xff, 230, 0));
		});
		M3GradientChooser fresh = build(60);
		fresh.moveStop(1, chooser.getStopPosition(1));
		fresh.setStopColor(2, chooser.getStopColor(2));
		M3TestSupport.flush(display);
		assertArrayEquals(M3TestSupport.pixels(fresh).data, M3TestSupport.pixels(chooser).data);
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> chooser.setStops(new float[] { 0f }, new RGB[] { new RGB(0, 0, 0) }));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> chooser.setStops(new float[] { 0.5f, 0.2f }, new RGB[] { new RGB(0, 0, 0), new RGB(1, 1, 1) }));
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> chooser.setStops(null, null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> chooser.addStop(1.5f, new RGB(0, 0, 0)));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> chooser.removeStop(9));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> chooser.setSelectedStop(9));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> chooser.toLookupTable(1));
		chooser.removeStop(1);
		chooser.removeStop(1);
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> chooser.removeStop(0));
	}
}
