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
package org.eclipse.nebula.widgets.roundedcheckbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * RoundedCheckbox keeps the paint GC in a private field while it draws. After the outermost paint the field holds that
 * paint's GC; a paint nested in the drawing (here from getClientArea) borrows the field and gives the caller's GC back,
 * at any depth and also when the nested paint fails.
 */
public class RoundedCheckboxPaintScopeTest {

	private Display display;
	private Consumer<RuntimeException> runtimeHandler;
	private Shell shell;
	private Reentrant checkbox;
	private final List<Image> images = new ArrayList<>();

	/** A RoundedCheckbox that runs queued nested paints from getClientArea and records the GC field after each. */
	private static final class Reentrant extends RoundedCheckbox {
		final Deque<GC> nested = new ArrayDeque<>();
		final List<Object> after = new ArrayList<>();
		final List<RuntimeException> failures = new ArrayList<>();

		Reentrant(final Composite parent) {
			super(parent, SWT.NONE);
		}

		@Override
		public Rectangle getClientArea() {
			final GC inner = nested.poll();
			if (inner != null) {
				final Event callback = new Event();
				callback.gc = inner;
				try {
					notifyListeners(SWT.Paint, callback);
				} catch (final RuntimeException failure) {
					failures.add(failure);
				}
				after.add(field(this, "gc"));
			}
			return super.getClientArea();
		}
	}

	@Before
	public void setUp() {
		display = Display.getDefault();
		// nested failures reach the outer paint, as with a plain SWT display
		runtimeHandler = display.getRuntimeExceptionHandler();
		display.setRuntimeExceptionHandler(e -> {
			throw e;
		});
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		checkbox = new Reentrant(shell);
		shell.setSize(160, 130);
	}

	@After
	public void tearDown() {
		shell.dispose();
		images.forEach(Image::dispose);
		display.setRuntimeExceptionHandler(runtimeHandler);
	}

	@Test
	public void outermostPaintKeepsItsGc() {
		final GC outer = newGc();
		paint(outer);
		assertSame(outer, field(checkbox, "gc"));
		assertEquals(0, field(checkbox, "paintDepth"));
	}

	@Test
	public void nestedPaintGivesTheOuterGcBack() {
		final GC outer = newGc();
		checkbox.nested.add(newGc());
		paint(outer);
		assertEquals(1, checkbox.after.size());
		assertSame("restored for the rest of the outer paint", outer, checkbox.after.get(0));
		assertSame(outer, field(checkbox, "gc"));
		assertEquals(0, field(checkbox, "paintDepth"));
	}

	@Test
	public void threeLevelsRestoreEachCaller() {
		final GC one = newGc();
		final GC two = newGc();
		final GC three = newGc();
		checkbox.nested.add(two);
		checkbox.nested.add(three);
		paint(one);
		assertEquals(2, checkbox.after.size());
		assertSame("third level gives the second its GC back", two, checkbox.after.get(0));
		assertSame("second level gives the first its GC back", one, checkbox.after.get(1));
		assertSame(one, field(checkbox, "gc"));
		assertEquals(0, field(checkbox, "paintDepth"));
	}

	@Test
	public void failingNestedPaintGivesTheOuterGcBack() {
		final GC outer = newGc();
		final GC broken = newGc();
		broken.dispose();
		checkbox.nested.add(broken);
		paint(outer);
		assertEquals(1, checkbox.failures.size());
		assertTrue(checkbox.failures.get(0) instanceof SWTException);
		assertSame(outer, checkbox.after.get(0));
		assertSame(outer, field(checkbox, "gc"));
		assertEquals(0, field(checkbox, "paintDepth"));
	}

	@Test
	public void failingOutermostPaintLeavesTheDepthBalanced() {
		final GC broken = newGc();
		broken.dispose();
		try {
			paint(broken);
		} catch (final SWTException expected) {
			// the failure reaches the caller unchanged
		}
		assertEquals(0, field(checkbox, "paintDepth"));
		final GC next = newGc();
		paint(next);
		assertSame(next, field(checkbox, "gc"));
	}

	private void paint(final GC gc) {
		final Event event = new Event();
		event.gc = gc;
		checkbox.notifyListeners(SWT.Paint, event);
	}

	private GC newGc() {
		final Image image = new Image(display, 160, 130);
		images.add(image);
		final GC gc = new GC(image);
		shell.addListener(SWT.Dispose, e -> {
			if (!gc.isDisposed()) {
				gc.dispose();
			}
		});
		return gc;
	}

	private static Object field(final RoundedCheckbox box, final String name) {
		try {
			final Field field = RoundedCheckbox.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(box);
		} catch (final ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
