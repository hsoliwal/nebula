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
package org.eclipse.nebula.widgets.pgroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * PGroup borrows the paint GC: when the toggle renderer fails, the GC gets back the background and foreground it came
 * with and the failure reaches the display unchanged; a successful paint paints the toggle after restoring them.
 */
public class PGroupPaintGcTest {

	private Display display;
	private Shell shell;
	private PGroup group;
	private Image canvas;
	private GC gc;

	/** A toggle renderer that changes the GC colours and can fail. */
	private static final class ProbeToggle extends AbstractRenderer {
		RuntimeException failure;
		int paints;

		@Override
		public void paint(final GC target, final Object value) {
			paints++;
			target.setForeground(target.getDevice().getSystemColor(SWT.COLOR_GREEN));
			if (failure != null) {
				throw failure;
			}
		}
	}

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		group = new PGroup(shell, SWT.NONE);
		group.setText("Group");
		group.setSize(200, 80);
		canvas = new Image(display, 200, 80);
		gc = new GC(canvas);
		gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
		gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
	}

	@After
	public void tearDown() {
		gc.dispose();
		canvas.dispose();
		shell.dispose();
	}

	@Test
	public void failingToggleRestoresTheBorrowedGc() {
		final ProbeToggle toggle = new ProbeToggle();
		final IllegalStateException failure = new IllegalStateException("toggle");
		toggle.failure = failure;
		group.setToggleRenderer(toggle);
		final List<RuntimeException> failures = paint();
		assertEquals(1, toggle.paints);
		assertEquals(1, failures.size());
		assertSame("the failure reaches the display", failure, failures.get(0));
		assertEquals("nothing suppressed", 0, failure.getSuppressed().length);
		assertEquals(display.getSystemColor(SWT.COLOR_RED).getRGB(), gc.getBackground().getRGB());
		assertEquals(display.getSystemColor(SWT.COLOR_BLUE).getRGB(), gc.getForeground().getRGB());
	}

	@Test
	public void successfulPaintKeepsTheToggleColours() {
		final ProbeToggle toggle = new ProbeToggle();
		group.setToggleRenderer(toggle);
		assertEquals(0, paint().size());
		assertEquals(1, toggle.paints);
		assertEquals("the toggle paints last", display.getSystemColor(SWT.COLOR_GREEN).getRGB(),
				gc.getForeground().getRGB());
		assertEquals(display.getSystemColor(SWT.COLOR_RED).getRGB(), gc.getBackground().getRGB());
	}

	/** Paints on the test GC; returns the failures that the paint listener reports to the display. */
	private List<RuntimeException> paint() {
		final Event event = new Event();
		event.gc = gc;
		event.width = 200;
		event.height = 80;
		final List<RuntimeException> failures = new ArrayList<>();
		final Consumer<RuntimeException> previous = display.getRuntimeExceptionHandler();
		display.setRuntimeExceptionHandler(failures::add);
		try {
			group.notifyListeners(SWT.Paint, event);
		} finally {
			display.setRuntimeExceptionHandler(previous);
		}
		return failures;
	}
}
