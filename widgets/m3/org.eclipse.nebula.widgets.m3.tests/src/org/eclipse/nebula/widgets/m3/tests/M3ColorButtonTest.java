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

import org.eclipse.nebula.widgets.m3.M3ColorButton;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3ColorButton: a colour change repaints the swatch only and fires Selection once, a resize sweep repaints only the
 * moving edges and leaves the pixels of a button created at that size, the hidden gate, errors. Runs in both startup
 * modes. (The colour dialog itself is the system's and is not opened here.)
 */
public class M3ColorButtonTest {

	Display display;
	Shell shell;
	M3ColorButton button;
	List<Rectangle> paints;
	int selections;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 320, 120);
		button = new M3ColorButton(shell, SWT.NONE);
		button.setBounds(0, 0, 60, 30);
		button.addListener(SWT.Selection, e -> selections++);
		paints = M3TestSupport.recordPaints(button);
		shell.open();
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3ColorButton", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	M3ColorButton fresh(int width, int height) {
		M3ColorButton other = new M3ColorButton(shell, SWT.NONE);
		other.setBounds(0, 60, width, height);
		other.setColor(button.getColor());
		M3TestSupport.flush(display);
		return other;
	}

	@Test
	public void colorChangeRepaintsTheSwatchOnly() {
		paints.clear();
		button.setColor(new RGB(200, 30, 60));
		button.update();
		assertEquals(1, selections);
		assertEquals(0xc81e3c, button.getPackedColor());
		if (viewport()) {
			M3TestSupport.assertPaintsInside(paints, button.getSwatchBounds());
		}
		button.setColor(new RGB(200, 30, 60));
		assertEquals("no event without a change", 1, selections);
		assertArrayEquals(M3TestSupport.pixels(fresh(60, 30)).data, M3TestSupport.pixels(button).data);
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		button.setColor(new RGB(10, 120, 240));
		M3Counters.reset();
		int height = button.getSize().y;
		for (int width : M3TestSupport.sweepWidths(60)) {
			button.setSize(width, height);
			button.update();
		}
		assertEquals(M3Counters.line(), 0, M3Counters.fullRepaints);
		assertArrayEquals(M3TestSupport.pixels(fresh(60, height)).data, M3TestSupport.pixels(button).data);
	}

	@Test
	public void hiddenButtonFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		M3TestSupport.assertHiddenGate(button, paints, i -> button.setColor(new RGB(i & 0xff, (i >> 8) & 0xff, 77)));
		assertTrue(selections >= 10_000);
		assertArrayEquals(M3TestSupport.pixels(fresh(60, 30)).data, M3TestSupport.pixels(button).data);
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> button.setColor(null));
		assertEquals(new RGB(0, 0, 0), new M3ColorButton(shell, SWT.NONE).getColor());
	}
}
