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
package org.eclipse.nebula.widgets.chips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The close button of a SWT.CLOSE chip: its center follows the last paint and is kept in one Point; a mouse up closes
 * the chip strictly inside the circle of radius 7 around it; before the first paint a mouse up fails as it always did.
 */
public class ChipsCloseButtonTest {

	private static final int RADIUS = 7;

	private Display display;
	private Shell shell;
	private Chips chips;
	private int closes;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		chips = new Chips(shell, SWT.CLOSE);
		chips.setText("Chip");
		chips.addCloseListener(event -> closes++);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void mouseUpBeforeTheFirstPaintFails() throws Exception {
		assertNull(center());
		final List<RuntimeException> failures = failuresOf(() -> mouseUp(0, 0));
		assertEquals("no close center before the first paint", 1, failures.size());
		assertTrue(failures.get(0) instanceof NullPointerException);
		assertEquals(0, closes);
	}

	@Test
	public void centerFollowsThePaintInOnePoint() throws Exception {
		chips.setSize(120, 30);
		paint();
		final Point first = center();
		assertNotNull(first);
		assertEquals((30 - 2 * RADIUS) / 2 + RADIUS, first.y);
		final int x = first.x;
		chips.setSize(120, 41);
		paint();
		assertSame("one Point for the close center", first, center());
		assertEquals((41 - 2 * RADIUS) / 2 + RADIUS, first.y);
		chips.setSize(120, 30);
		paint();
		assertSame(first, center());
		assertEquals("same size, same center", x, first.x);
		assertEquals((30 - 2 * RADIUS) / 2 + RADIUS, first.y);
	}

	@Test
	public void mouseUpClosesStrictlyInsideTheCircle() throws Exception {
		chips.setSize(120, 30);
		paint();
		final Point c = center();
		mouseUp(c.x + RADIUS, c.y);
		mouseUp(c.x, c.y - RADIUS);
		mouseUp(c.x + 5, c.y + 5);
		assertEquals("on or outside the circle", 0, closes);
		mouseUp(c.x, c.y);
		mouseUp(c.x + RADIUS - 1, c.y);
		mouseUp(c.x - 4, c.y + 4);
		assertEquals("inside the circle", 3, closes);
	}

	/** The failures that listeners report to the display while the action runs. */
	private List<RuntimeException> failuresOf(final Runnable action) {
		final List<RuntimeException> failures = new ArrayList<>();
		final Consumer<RuntimeException> previous = display.getRuntimeExceptionHandler();
		display.setRuntimeExceptionHandler(failures::add);
		try {
			action.run();
		} finally {
			display.setRuntimeExceptionHandler(previous);
		}
		return failures;
	}

	private void paint() {
		final Point size = chips.getSize();
		final Image image = new Image(display, size.x, size.y);
		final GC gc = new GC(image);
		try {
			final Event event = new Event();
			event.gc = gc;
			event.width = size.x;
			event.height = size.y;
			chips.notifyListeners(SWT.Paint, event);
		} finally {
			gc.dispose();
			image.dispose();
		}
	}

	private void mouseUp(final int x, final int y) {
		final Event event = new Event();
		event.button = 1;
		event.x = x;
		event.y = y;
		chips.notifyListeners(SWT.MouseUp, event);
	}

	private Point center() throws Exception {
		final Field field = Chips.class.getDeclaredField("closeCenter");
		field.setAccessible(true);
		return (Point) field.get(chips);
	}
}
