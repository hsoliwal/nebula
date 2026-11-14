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
package org.eclipse.nebula.widgets.badgedlabel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The text and badge measurements of BadgedLabel use a temporary GC: it is released after a measurement and after a
 * failing one (the failure reaches the caller unchanged), and a measurement is cached until the text, badge or font
 * changes.
 */
public class BadgedLabelGcTest {

	private Display display;
	private boolean tracking;
	private Shell shell;
	private BadgedLabel label;

	@Before
	public void setUp() {
		display = Display.getDefault();
		tracking = display.isTracking();
		display.setTracking(true);
		shell = new Shell(display);
		label = new BadgedLabel(shell, SWT.NONE);
		final int base = liveGcs();
		final GC probe = new GC(label);
		assertEquals("the device tracks GCs", base + 1, liveGcs());
		probe.dispose();
		assertEquals(base, liveGcs());
	}

	@After
	public void tearDown() {
		shell.dispose();
		display.setTracking(tracking);
	}

	@Test
	public void textMeasurementReleasesItsGcAndIsCached() throws Exception {
		label.setText("#######");
		final int before = liveGcs();
		final Point size = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		assertEquals("GC released after the measurement", before, liveGcs());
		assertEquals(size, label.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		final Point extent = (Point) call("getTextSizeInPixels");
		assertSame("measured once", extent, call("getTextSizeInPixels"));
		assertTrue(extent.x > 0 && extent.y > 0);
		assertEquals(before, liveGcs());
	}

	@Test
	public void textFontFailureReleasesItsGc() throws Exception {
		label.setText("#######");
		((Font) field("boldFont").get(label)).dispose();
		final int before = liveGcs();
		try {
			label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			fail("a disposed font is an invalid argument");
		} catch (final IllegalArgumentException expected) {
			assertEquals("nothing suppressed", 0, expected.getSuppressed().length);
		}
		assertEquals("GC released after the failure", before, liveGcs());
	}

	@Test
	public void badgeMeasurementReleasesItsGcAndIsCached() throws Exception {
		label.setBadgeValue("12");
		final int before = liveGcs();
		final Point extent = (Point) call("getBadgeTextSizeInPixels");
		assertEquals("GC released after the measurement", before, liveGcs());
		assertSame("measured once", extent, call("getBadgeTextSizeInPixels"));
		label.setBadgeValue("123");
		final Point wider = (Point) call("getBadgeTextSizeInPixels");
		assertTrue(wider.x > extent.x);
		assertEquals(before, liveGcs());
	}

	@Test
	public void badgeFontFailureReleasesItsGc() throws Exception {
		label.setBadgeValue("3");
		((Font) field("badgeFont").get(label)).dispose();
		final int before = liveGcs();
		try {
			call("getBadgeTextSizeInPixels");
			fail("a disposed font is an invalid argument");
		} catch (final InvocationTargetException failure) {
			assertTrue(failure.getCause() instanceof IllegalArgumentException);
			assertEquals("nothing suppressed", 0, failure.getCause().getSuppressed().length);
		}
		assertEquals("GC released after the failure", before, liveGcs());
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

	private Object call(final String name) throws Exception {
		final Method method = BadgedLabel.class.getDeclaredMethod(name);
		method.setAccessible(true);
		return method.invoke(label);
	}

	private static Field field(final String name) throws Exception {
		final Field field = BadgedLabel.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}
}
