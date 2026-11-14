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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Chips borrows the paint GC: after a paint, and after a paint that fails, the GC has the background and foreground it
 * came with, and the failure reaches the caller. computeSize releases the GC it creates for measuring.
 */
public class ChipsGcTest {

	private Display display;
	private boolean tracking;
	private Shell shell;
	private Chips chips;
	private Image canvas;
	private GC gc;

	@Before
	public void setUp() {
		display = Display.getDefault();
		tracking = display.isTracking();
		display.setTracking(true);
		shell = new Shell(display);
		chips = new Chips(shell, SWT.CLOSE);
		chips.setText("Chip");
		chips.setSize(120, 30);
		canvas = new Image(display, 120, 30);
		gc = new GC(canvas);
		gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
		gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
	}

	@After
	public void tearDown() {
		gc.dispose();
		canvas.dispose();
		shell.dispose();
		display.setTracking(tracking);
	}

	@Test
	public void paintRestoresTheBorrowedGc() {
		assertEquals("no failure", 0, paint().size());
		assertColors();
	}

	@Test
	public void failingPaintRestoresTheBorrowedGcAndRethrows() {
		final Image disposed = new Image(display, 8, 8);
		disposed.dispose();
		chips.setImage(disposed);
		final List<RuntimeException> failures = paint();
		assertEquals("painting a disposed image fails", 1, failures.size());
		assertTrue(failures.get(0) instanceof SWTException);
		assertEquals("nothing suppressed", 0, failures.get(0).getSuppressed().length);
		assertColors();
	}

	@Test
	public void computeSizeReleasesItsGc() {
		final int before = liveGcs();
		final GC probe = new GC(chips);
		assertEquals("the device tracks GCs", before + 1, liveGcs());
		probe.dispose();
		chips.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		assertEquals(before, liveGcs());
	}

	/** Paints on the test GC; returns the failures that the paint listener reports to the display. */
	private List<RuntimeException> paint() {
		final Event event = new Event();
		event.gc = gc;
		event.width = 120;
		event.height = 30;
		final List<RuntimeException> failures = new ArrayList<>();
		final Consumer<RuntimeException> previous = display.getRuntimeExceptionHandler();
		display.setRuntimeExceptionHandler(failures::add);
		try {
			chips.notifyListeners(SWT.Paint, event);
		} finally {
			display.setRuntimeExceptionHandler(previous);
		}
		return failures;
	}

	private void assertColors() {
		final Color red = display.getSystemColor(SWT.COLOR_RED);
		final Color blue = display.getSystemColor(SWT.COLOR_BLUE);
		assertEquals(red.getRGB(), gc.getBackground().getRGB());
		assertEquals(blue.getRGB(), gc.getForeground().getRGB());
	}

	private int liveGcs() {
		int count = 0;
		for (final Object object : display.getDeviceData().objects) {
			if (object instanceof GC) {
				count++;
			}
		}
		return count;
	}
}
