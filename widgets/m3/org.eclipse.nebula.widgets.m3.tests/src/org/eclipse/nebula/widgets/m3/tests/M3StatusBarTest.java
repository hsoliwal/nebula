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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.nebula.widgets.m3.M3StatusBar;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3StatusBar: prefix-sum layout (fixed, preferred, weighted slots tile the bar), delta repaints (a text change
 * repaints its slot; a preferred slot that grows repaints from itself on), controls in slots, grip drag, pixels after
 * deltas equal a bar built with the final texts, errors. Runs in both startup modes.
 */
public class M3StatusBarTest {

	Display display;
	Shell shell;
	M3StatusBar bar;
	List<Rectangle> paints;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 420, 80);
		bar = build(shell, 0);
		paints = M3TestSupport.recordPaints(bar);
		shell.open();
		M3TestSupport.flush(display);
	}

	static M3StatusBar build(Shell shell, int y) {
		M3StatusBar bar = new M3StatusBar(shell, SWT.NONE);
		bar.setBounds(0, y, 400, 24);
		bar.addSlot(M3StatusBar.FIXED, 80);
		bar.addSlot(M3StatusBar.PREFERRED, 20);
		bar.addSlot(M3StatusBar.FILL, 1);
		bar.addSlot(M3StatusBar.FILL, 3);
		bar.setText(0, "Ready");
		bar.setText(1, "row 1");
		bar.setText(2, "filter: none");
		bar.setText(3, "10^6 rows");
		return bar;
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3StatusBar", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	@Test
	public void slotsTileTheBar() {
		Rectangle client = bar.getClientArea();
		Rectangle s0 = bar.getSlotBounds(0), s1 = bar.getSlotBounds(1), s2 = bar.getSlotBounds(2), s3 = bar.getSlotBounds(3);
		assertEquals(80, s0.width);
		assertTrue(s1.width >= 20);
		assertEquals(s0.x + s0.width + 5, s1.x);
		assertEquals(s1.x + s1.width + 5, s2.x);
		assertEquals(s2.x + s2.width + 5, s3.x);
		int rest = s2.width + s3.width;
		assertEquals(client.width - 8 - 14 - 15 - 80 - s1.width, rest);
		assertEquals(rest / 4, s2.width);
		assertEquals(bar.getGripBounds().x, client.x + client.width - 14);
		bar.setGrip(false);
		assertEquals(0, bar.getGripBounds().width);
		assertEquals(client.width - 8 - 15 - 80 - s1.width, bar.getSlotBounds(2).width + bar.getSlotBounds(3).width);
	}

	@Test
	public void textChangeRepaintsItsSlotOnly() {
		paints.clear();
		bar.setText(0, "Busy");
		bar.setText(3, "999,999 rows");
		bar.update();
		assertFalse(paints.isEmpty());
		if (viewport()) {
			for (Rectangle paint : paints) {
				assertFalse("slot 1 untouched: " + paint, paint.intersects(bar.getSlotBounds(1)));
				assertFalse("slot 2 untouched: " + paint, paint.intersects(bar.getSlotBounds(2)));
			}
		}
		paints.clear();
		bar.setText(0, "Busy");
		bar.update();
		assertTrue(paints.isEmpty());
	}

	@Test
	public void preferredSlotGrowthRepaintsFromItself() {
		Rectangle before = bar.getSlotBounds(1);
		paints.clear();
		bar.setText(1, "row 123,456 of 1,000,000");
		bar.update();
		Rectangle after = bar.getSlotBounds(1);
		assertTrue(after.width > before.width);
		assertEquals(before.x, after.x);
		if (viewport()) {
			Rectangle client = bar.getClientArea();
			M3TestSupport.assertPaintsInside(paints, new Rectangle(after.x, client.y, client.x + client.width - after.x, client.height));
		}
	}

	@Test
	public void controlsFollowTheirSlot() {
		Label label = new Label(bar, SWT.NONE);
		label.setText("*");
		bar.setControl(3, label);
		assertSame(label, bar.getControl(3));
		assertEquals(bar.getSlotBounds(3), label.getBounds());
		bar.setText(1, "a much longer preferred text");
		assertEquals(bar.getSlotBounds(3), label.getBounds());
		bar.setControl(3, null);
		assertNull(bar.getControl(3));
		bar.removeSlot(0);
		assertEquals(3, bar.getSlotCount());
		assertEquals(M3StatusBar.PREFERRED, bar.getSlotMode(0));
		assertEquals("a much longer preferred text", bar.getText(0));
	}

	@Test
	public void gripDragResizesTheShell() {
		Rectangle grip = bar.getGripBounds();
		int width = shell.getSize().x, height = shell.getSize().y;
		Event down = new Event();
		down.x = grip.x + 4;
		down.y = grip.y + 4;
		down.button = 1;
		bar.notifyListeners(SWT.MouseDown, down);
		Event move = new Event();
		move.x = down.x + 20;
		move.y = down.y + 10;
		move.stateMask = SWT.BUTTON1;
		bar.notifyListeners(SWT.MouseMove, move);
		bar.notifyListeners(SWT.MouseUp, new Event());
		assertEquals(width + 20, shell.getSize().x);
		assertEquals(height + 10, shell.getSize().y);
	}

	@Test
	public void deltaPixelsEqualAFreshBar() {
		bar.setText(0, "Busy");
		bar.setText(1, "row 77");
		bar.setText(3, "12 rows");
		bar.update();
		M3StatusBar fresh = build(shell, 40);
		fresh.setText(0, "Busy");
		fresh.setText(1, "row 77");
		fresh.setText(3, "12 rows");
		M3TestSupport.flush(display);
		assertArrayEquals(M3TestSupport.pixels(fresh).data, M3TestSupport.pixels(bar).data);
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		Label pinned = new Label(bar, SWT.NONE);
		pinned.setText("*");
		bar.setControl(0, pinned);
		int[] moves = new int[1];
		pinned.addListener(SWT.Move, e -> moves[0]++);
		pinned.addListener(SWT.Resize, e -> moves[0]++);
		M3StatusBar fixedOnly = new M3StatusBar(shell, SWT.NONE);
		fixedOnly.setBounds(0, 40, 400, 24);
		fixedOnly.addSlot(M3StatusBar.FIXED, 60);
		fixedOnly.addSlot(M3StatusBar.PREFERRED, 0);
		fixedOnly.setText(1, "no weighted slot");
		List<Rectangle> fixedPaints = M3TestSupport.recordPaints(fixedOnly);
		M3TestSupport.flush(display);
		int firstFill = bar.getSlotBounds(2).x, height = bar.getSize().y, old = bar.getSize().x;
		M3Counters.reset();
		for (int width : M3TestSupport.sweepWidths(400)) {
			paints.clear();
			fixedPaints.clear();
			bar.setSize(width, height);
			fixedOnly.setSize(width, height);
			bar.update();
			fixedOnly.update();
			if (viewport()) {
				for (Rectangle paint : paints) {
					assertTrue(old + " -> " + width + ": " + paint, paint.x >= firstFill);
				}
				for (Rectangle paint : fixedPaints) {
					assertTrue("only the grip moves: " + old + " -> " + width + ": " + paint, paint.x >= Math.min(old, width) - 14 - 4);
				}
			}
			assertEquals(bar.getSlotBounds(0), pinned.getBounds());
			old = width;
		}
		assertEquals("fixed slot control never moves", 0, moves[0]);
		assertEquals(M3Counters.line(), viewport() ? 0 : 100, M3Counters.fullRepaints);
		bar.setSize(old, height + 6);
		assertEquals("a height change moves every baseline", viewport() ? 1 : 101, M3Counters.fullRepaints);
	}

	@Test
	public void hiddenBarFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		M3TestSupport.assertHiddenGate(bar, paints, i -> {
			bar.setText(1, "row " + i);
			bar.setText(0, i % 2 == 0 ? "Busy" : "Ready");
			bar.setText(3, i + " rows");
		});
		M3StatusBar fresh = build(shell, 40);
		fresh.setText(1, "row 9999");
		fresh.setText(0, "Ready");
		fresh.setText(3, "9999 rows");
		M3TestSupport.flush(display);
		assertEquals(fresh.getSlotBounds(1), bar.getSlotBounds(1));
		assertArrayEquals(M3TestSupport.pixels(fresh).data, M3TestSupport.pixels(bar).data);
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> bar.addSlot(3, 0));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> bar.addSlot(M3StatusBar.FIXED, -1));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> bar.setText(4, "x"));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> bar.getText(-1));
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> bar.setText(0, null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_PARENT, () -> bar.setControl(0, new Label(shell, SWT.NONE)));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> bar.removeSlot(9));
		for (int i = 0; i < 9; i++) {
			bar.addSlot(M3StatusBar.FIXED, 4);
		}
		assertEquals(13, bar.getSlotCount());
		assertTrue(bar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x > 80);
	}
}
