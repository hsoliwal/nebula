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

import org.eclipse.nebula.widgets.m3.M3Hyperlink;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
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
 * M3Hyperlink: click, Enter and Space activate and mark visited, hover and visited repaint the text rectangle only, a
 * text change repaints the union of old and new text, a resize sweep repaints only the exposed strip, the hidden gate,
 * errors. Runs in both startup modes.
 */
public class M3HyperlinkTest {

	Display display;
	Shell shell;
	M3Hyperlink link;
	List<Rectangle> paints;
	int selections;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 320, 80);
		link = new M3Hyperlink(shell, SWT.NONE);
		link.setBounds(0, 0, 300, 30);
		link.setText("Open the Problems view");
		link.addListener(SWT.Selection, e -> selections++);
		paints = M3TestSupport.recordPaints(link);
		shell.open();
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3Hyperlink", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	@Test
	public void clickActivatesAndMarksVisited() {
		Rectangle text = link.getTextBounds();
		Event down = new Event();
		down.button = 1;
		down.x = text.x + 3;
		down.y = text.y + 3;
		link.notifyListeners(SWT.MouseDown, down);
		Event up = new Event();
		up.button = 1;
		up.x = down.x;
		up.y = down.y;
		paints.clear();
		link.notifyListeners(SWT.MouseUp, up);
		link.update();
		assertEquals(1, selections);
		assertTrue(link.isVisited());
		if (viewport()) {
			M3TestSupport.assertPaintsInside(paints, text);
		}
		Event key = new Event();
		key.character = '\r';
		link.notifyListeners(SWT.KeyDown, key);
		key.character = ' ';
		link.notifyListeners(SWT.KeyDown, key);
		assertEquals(3, selections);
		Event outside = new Event();
		outside.button = 1;
		outside.x = 299;
		outside.y = 2;
		link.notifyListeners(SWT.MouseDown, outside);
		link.notifyListeners(SWT.MouseUp, outside);
		assertEquals("a click beside the text does nothing", 3, selections);
	}

	@Test
	public void hoverAndTextChangesAreDeltas() {
		paints.clear();
		link.notifyListeners(SWT.MouseEnter, new Event());
		link.update();
		assertFalse(paints.isEmpty());
		if (viewport()) {
			M3TestSupport.assertPaintsInside(paints, link.getTextBounds());
		}
		Rectangle before = link.getTextBounds();
		paints.clear();
		link.setText("Open");
		link.update();
		if (viewport()) {
			M3TestSupport.assertPaintsInside(paints, before.union(link.getTextBounds()));
		}
		link.setLinkColors(new RGB(0, 0, 200), new RGB(120, 0, 120));
		link.setVisited(true);
		assertTrue(link.computeSize(SWT.DEFAULT, SWT.DEFAULT).x < 100);
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		int height = link.getSize().y, old = link.getSize().x;
		M3Counters.reset();
		for (int width : M3TestSupport.sweepWidths(300)) {
			paints.clear();
			link.setSize(width, height);
			link.update();
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
	public void hiddenLinkFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		M3TestSupport.assertHiddenGate(link, paints, i -> {
			link.setText("link " + i);
			link.setVisited(i % 2 == 0);
		});
		M3Hyperlink fresh = new M3Hyperlink(shell, SWT.NONE);
		fresh.setBounds(0, 40, 300, 30);
		fresh.setText("link 9999");
		M3TestSupport.flush(display);
		assertArrayEquals(M3TestSupport.pixels(fresh).data, M3TestSupport.pixels(link).data);
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> link.setText(null));
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> link.setLinkColors(null, new RGB(0, 0, 0)));
	}
}
