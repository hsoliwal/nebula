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
package org.eclipse.nebula.widgets.led;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
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
 * BaseLED keeps the paint GC in its protected field while paintInternal runs. After the outermost paint the field still
 * holds that paint's GC; a paint nested in paintInternal borrows the field and gives the outer GC back, also when the
 * nested paint fails, so the outer paint resumes with its own GC.
 */
public class BaseLEDPaintScopeTest {

	private Display display;
	private Consumer<RuntimeException> runtimeHandler;
	private Consumer<Error> errorHandler;
	private Shell shell;
	private ProbeLED led;
	private final List<Image> images = new ArrayList<>();

	/** A BaseLED whose paintInternal records the GC it sees and can paint itself again. */
	private static final class ProbeLED extends BaseLED {
		final List<GC> seen = new ArrayList<>();
		Runnable nested;

		ProbeLED(final Shell parent) {
			super(parent, SWT.NONE);
		}

		@Override
		protected void paintInternal() {
			seen.add(gc);
			final Runnable inner = nested;
			if (inner != null) {
				nested = null;
				inner.run();
				seen.add(gc);
			}
		}
	}

	@Before
	public void setUp() {
		display = Display.getDefault();
		// nested failures reach the outer paint, as with a plain SWT display
		runtimeHandler = display.getRuntimeExceptionHandler();
		errorHandler = display.getErrorHandler();
		display.setRuntimeExceptionHandler(e -> {
			throw e;
		});
		display.setErrorHandler(e -> {
			throw e;
		});
		shell = new Shell(display);
		led = new ProbeLED(shell);
		led.setSize(40, 60);
	}

	@After
	public void tearDown() {
		shell.dispose();
		images.forEach(Image::dispose);
		display.setRuntimeExceptionHandler(runtimeHandler);
		display.setErrorHandler(errorHandler);
	}

	@Test
	public void outermostPaintLeavesItsGc() throws Exception {
		final GC first = newGc();
		paint(first);
		assertSame(first, led.gc);
		final GC second = newGc();
		paint(second);
		assertSame(second, led.gc);
		assertEquals(0, depth());
	}

	@Test
	public void nestedPaintGivesTheOuterGcBack() throws Exception {
		final GC outer = newGc();
		final GC inner = newGc();
		led.nested = () -> paint(inner);
		paint(outer);
		assertEquals(3, led.seen.size());
		assertSame(outer, led.seen.get(0));
		assertSame(inner, led.seen.get(1));
		assertSame("the outer paint resumes with its GC", outer, led.seen.get(2));
		assertSame(outer, led.gc);
		assertEquals(0, depth());
	}

	@Test
	public void failingNestedPaintGivesTheOuterGcBack() throws Exception {
		failingNestedPaint(new IllegalStateException("nested failure"));
		failingNestedPaint(new AssertionError("nested error"));
	}

	private void failingNestedPaint(final Throwable failure) throws Exception {
		led.seen.clear();
		final GC outer = newGc();
		final GC inner = newGc();
		final Throwable[] caught = new Throwable[1];
		led.nested = () -> {
			final Runnable previous = led.nested;
			led.nested = () -> {
				if (failure instanceof Error) {
					throw (Error) failure;
				}
				throw (RuntimeException) failure;
			};
			try {
				paint(inner);
			} catch (RuntimeException | Error e) {
				caught[0] = e;
			}
			led.nested = previous;
		};
		paint(outer);
		assertSame("the failure reaches the outer paint", failure, caught[0]);
		assertEquals(3, led.seen.size());
		assertSame(inner, led.seen.get(1));
		assertSame("the outer paint resumes with its GC", outer, led.seen.get(2));
		assertSame(outer, led.gc);
		assertEquals(0, depth());
	}

	private void paint(final GC gc) {
		final Event event = new Event();
		event.gc = gc;
		event.width = 40;
		event.height = 60;
		led.notifyListeners(SWT.Paint, event);
	}

	private GC newGc() {
		final Image image = new Image(display, 40, 60);
		images.add(image);
		final GC gc = new GC(image);
		shell.addListener(SWT.Dispose, e -> gc.dispose());
		return gc;
	}

	private int depth() throws Exception {
		final Field field = BaseLED.class.getDeclaredField("paintDepth");
		field.setAccessible(true);
		return field.getInt(led);
	}
}
