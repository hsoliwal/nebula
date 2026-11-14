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
package org.eclipse.nebula.widgets.opal.colorbutton;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.nebula.widgets.opal.colorbutton.internal.RepaintCounters;
import org.eclipse.nebula.widgets.opal.colorbutton.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * ColorButton: a colour change repaints the swatch only and fires Selection once, a resize sweep repaints only the
 * moving edges and leaves the pixels of a button created at that size, the hidden gate, errors. Runs in both startup
 * modes. (The colour dialog itself is the system's and is not opened here.)
 */
public class ColorButtonTest {

	Display display;
	Shell shell;
	ColorButton button;
	List<Rectangle> paints;
	int selections;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 320, 120);
		button = new ColorButton(shell, SWT.NONE);
		button.setBounds(0, 0, 60, 30);
		button.addListener(SWT.Selection, e -> selections++);
		paints = TestSupport.recordPaints(button);
		// the focus goes to a field away from the compared buttons: a focused button draws its focus ring, and when
		// the shell happened to be active the first button had it and the fresh one compared with it did not
		Text focusSink = new Text(shell, SWT.NONE);
		focusSink.setBounds(200, 90, 100, 20);
		shell.open();
		focusSink.setFocus();
		TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("ColorButton", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	ColorButton fresh(int width, int height) {
		ColorButton other = new ColorButton(shell, SWT.NONE);
		other.setBounds(0, 60, width, height);
		other.setColor(button.getColor());
		TestSupport.flush(display);
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
			TestSupport.assertPaintsInside(paints, button.getSwatchBounds());
		}
		button.setColor(new RGB(200, 30, 60));
		assertEquals("no event without a change", 1, selections);
		assertArrayEquals(TestSupport.pixels(fresh(60, 30)).data, TestSupport.pixels(button).data);
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		button.setColor(new RGB(10, 120, 240));
		RepaintCounters.reset();
		int height = button.getSize().y;
		for (int width : TestSupport.sweepWidths(60)) {
			button.setSize(width, height);
			button.update();
		}
		assertEquals(RepaintCounters.line(), 0, RepaintCounters.fullRepaints);
		assertArrayEquals(TestSupport.pixels(fresh(60, height)).data, TestSupport.pixels(button).data);
	}

	@Test
	public void hiddenButtonFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		TestSupport.assertHiddenGate(button, paints, i -> button.setColor(new RGB(i & 0xff, (i >> 8) & 0xff, 77)));
		assertTrue(selections >= 10_000);
		assertArrayEquals(TestSupport.pixels(fresh(60, 30)).data, TestSupport.pixels(button).data);
	}

	@Test
	public void errors() {
		TestSupport.expect(SWT.ERROR_NULL_ARGUMENT, () -> button.setColor(null));
		assertEquals(new RGB(0, 0, 0), new ColorButton(shell, SWT.NONE).getColor());
	}
}
