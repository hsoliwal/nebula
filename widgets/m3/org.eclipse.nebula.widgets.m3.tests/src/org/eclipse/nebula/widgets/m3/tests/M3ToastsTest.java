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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.widgets.m3.M3Toasts;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3Toasts: one overlay with one canvas for any number of toasts, placement in the owner's bottom-right corner,
 * dismiss by id and by click, expiry by one timer, hover pauses expiry, the 64-toast cap, errors.
 */
public class M3ToastsTest {

	Display display;
	Shell shell;
	M3Toasts toasts;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(100, 100, 700, 500);
		shell.open();
		toasts = new M3Toasts(shell);
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void stackInOneOverlay() {
		for (int i = 0; i < 10; i++) {
			toasts.show("Build " + i, "10^6 rows indexed", i % 3, 60_000);
		}
		M3TestSupport.flush(display);
		assertEquals(10, toasts.getCount());
		Shell overlay = toasts.getOverlay();
		assertTrue(overlay.isVisible());
		assertEquals("one canvas, no control per toast", 1, overlay.getChildren().length);
		Rectangle client = shell.getClientArea();
		Point corner = shell.toDisplay(client.x + client.width, client.y + client.height);
		Rectangle bounds = overlay.getBounds();
		assertTrue(bounds + " inside the owner corner " + corner, bounds.x + bounds.width <= corner.x && bounds.y + bounds.height <= corner.y);
	}

	@Test
	public void dismissByIdAndByClick() {
		int first = toasts.show("one", "first", M3Toasts.INFO, 60_000);
		int second = toasts.show("two", "second", M3Toasts.WARNING, 60_000);
		int third = toasts.show("three", "third", M3Toasts.ERROR, 60_000);
		assertTrue(toasts.dismiss(second));
		assertFalse(toasts.dismiss(second));
		assertEquals(2, toasts.getCount());
		assertEquals("the newest is on top", third, toasts.toastAt(10, 8));
		Canvas canvas = (Canvas) toasts.getOverlay().getChildren()[0];
		Event up = new Event();
		up.x = 10;
		up.y = 8;
		canvas.notifyListeners(SWT.MouseUp, up);
		assertEquals(1, toasts.getCount());
		assertEquals(first, toasts.toastAt(10, 8));
		toasts.dismiss(first);
		assertFalse("an empty stack hides the overlay", toasts.getOverlay().isVisible());
	}

	@Test
	public void expiryAndHoverPause() {
		toasts.show("short", "goes away", M3Toasts.INFO, 40);
		toasts.show("long", "stays", M3Toasts.INFO, 60_000);
		M3TestSupport.spin(display, 250);
		assertEquals(1, toasts.getCount());
		toasts.show("held", "paused by hover", M3Toasts.INFO, 40);
		Canvas canvas = (Canvas) toasts.getOverlay().getChildren()[0];
		canvas.notifyListeners(SWT.MouseEnter, new Event());
		M3TestSupport.spin(display, 200);
		assertEquals("hover pauses expiry", 2, toasts.getCount());
		canvas.notifyListeners(SWT.MouseExit, new Event());
		M3TestSupport.spin(display, 250);
		assertEquals(1, toasts.getCount());
	}

	@Test
	public void capAndErrors() {
		for (int i = 0; i < 70; i++) {
			toasts.show("t" + i, "m", M3Toasts.INFO, 60_000);
		}
		assertEquals(64, toasts.getCount());
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> toasts.show(null, "m", M3Toasts.INFO, 1));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> toasts.show("t", "m", 7, 1));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> toasts.show("t", "m", M3Toasts.INFO, 0));
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> new M3Toasts(null));
	}
}
