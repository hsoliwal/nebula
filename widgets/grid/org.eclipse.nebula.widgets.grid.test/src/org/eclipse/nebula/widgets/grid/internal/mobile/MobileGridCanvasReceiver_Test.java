/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid.internal.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link MobileGridCanvasReceiver} on an SWT Canvas: the viewport, hit testing, click selection, fractional and
 * drag scrolling, a long index jump, the zero-row reset and disposal with the Canvas. Same checks as the former
 * contract harness of the mobile preview, on desktop SWT; that harness also checked the native owners of the SWT
 * mobile backend, which desktop SWT does not have.
 */
public class MobileGridCanvasReceiver_Test {

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(0, 0, 320, 120);
	}

	@After
	public void tearDown() {
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

	private void pump() {
		for (int i = 0; i < 100 && display.readAndDispatch(); i++) {
			// drain pending events
		}
		display.update();
	}

	private static void pointer(Canvas canvas, int eventType, int y) {
		final Event event = new Event();
		event.button = 1;
		event.x = 10;
		event.y = y;
		canvas.notifyListeners(eventType, event);
	}

	@Test
	public void canvasEventsDriveTheViewport() {
		final Canvas canvas = new Canvas(shell, SWT.NONE);
		canvas.setBounds(0, 0, 320, 120);
		final List<Long> selections = new ArrayList<>();
		final MobileGridCanvasReceiver receiver = new MobileGridCanvasReceiver(canvas, 1_000_000_000_000L, 2, 24, 2,
				(row, column) -> "row " + row + " col " + column, row -> selections.add(Long.valueOf(row)));
		shell.open();
		pump();
		assertEquals("initial viewport", 0, receiver.window().visibleStart());
		assertTrue("bounded native row slots", receiver.window().nativeSlots() <= 9);
		assertEquals("initial row hit test", 1, receiver.rowAt(25));
		pointer(canvas, SWT.MouseDown, 25);
		pointer(canvas, SWT.MouseUp, 25);
		assertEquals("selection", List.of(Long.valueOf(1)), selections);
		assertEquals("selection", 1, receiver.selectedRow());
		receiver.scrollTo(12);
		pump();
		assertEquals("fractional scroll hit test", 1, receiver.rowAt(12));
		pointer(canvas, SWT.MouseDown, 100);
		pointer(canvas, SWT.MouseMove, 80);
		pointer(canvas, SWT.MouseUp, 80);
		assertEquals("drag scroll without click", 32, receiver.scrollY());
		assertEquals("drag scroll without click", 1, selections.size());
		receiver.scrollTo(Long.MAX_VALUE);
		pump();
		assertEquals("long-index jump", 999_999_999_999L, receiver.window().visibleStart());
		assertTrue("long-index bounded cache", receiver.window().nativeSlots() <= 9);
		receiver.setItemCount(0);
		pump();
		assertEquals("zero-row reset", -1, receiver.rowAt(20));
		assertEquals("zero-row reset", 0, receiver.window().visibleEnd());
		canvas.dispose();
		assertTrue("canvas owns receiver disposal", receiver.isClosed());
		receiver.close(); // idempotent
	}
}
