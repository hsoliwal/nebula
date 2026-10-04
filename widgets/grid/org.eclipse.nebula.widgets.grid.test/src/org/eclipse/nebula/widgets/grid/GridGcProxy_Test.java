/*******************************************************************************
 * Copyright (c) 2026 Eclipse Nebula contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.junit.Test;

public class GridGcProxy_Test {

	@Test
	public void restoresCompleteMutableGcStateAfterPlanePaint() {
		Display display = Display.getDefault();
		Image image = new Image(display, 64, 64);
		GC gc = new GC(image);
		Pattern temporaryPattern = null;
		Transform initialTransform = null;
		Transform changedTransform = null;
		try {
			Rectangle initialClip = new Rectangle(2, 3, 40, 41);
			gc.setClipping(initialClip);
			gc.setLineAttributes(new LineAttributes(
					3f, SWT.CAP_ROUND, SWT.JOIN_BEVEL, SWT.LINE_DASH,
					new float[] {2f, 4f}, 1f, 5f));
			gc.setAlpha(201);
			gc.setAntialias(SWT.ON);
			gc.setTextAntialias(SWT.OFF);
			gc.setInterpolation(SWT.HIGH);
			gc.setFillRule(SWT.FILL_WINDING);
			gc.setXORMode(false);
			Color foreground = display.getSystemColor(SWT.COLOR_BLUE);
			Color background = display.getSystemColor(SWT.COLOR_WHITE);
			gc.setForeground(foreground);
			gc.setBackground(background);
			initialTransform = new Transform(display, 1, 0, 0, 1, 3, 4);
			gc.setTransform(initialTransform);

			Rectangle expectedClip = gc.getClipping();
			LineAttributes expectedLine = gc.getLineAttributes();
			int expectedAlpha = gc.getAlpha();
			int expectedAntialias = gc.getAntialias();
			int expectedTextAntialias = gc.getTextAntialias();
			int expectedInterpolation = gc.getInterpolation();
			int expectedFillRule = gc.getFillRule();
			boolean expectedXor = gc.getXORMode();
			Color expectedForeground = gc.getForeground();
			Color expectedBackground = gc.getBackground();
			float[] expectedTransform = elements(gc);

			try (GridGcProxy ignored = GridGcProxy.wrap(gc)) {
				gc.setClipping(new Rectangle(20, 20, 5, 5));
				gc.setLineAttributes(new LineAttributes(
						11f, SWT.CAP_SQUARE, SWT.JOIN_MITER, SWT.LINE_DOT,
						null, 0f, 10f));
				gc.setAlpha(17);
				gc.setAntialias(SWT.OFF);
				gc.setTextAntialias(SWT.ON);
				gc.setInterpolation(SWT.NONE);
				gc.setFillRule(SWT.FILL_EVEN_ODD);
				gc.setXORMode(true);
				gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
				gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
				temporaryPattern = new Pattern(
						display, 0, 0, 16, 16,
						display.getSystemColor(SWT.COLOR_RED),
						display.getSystemColor(SWT.COLOR_GREEN));
				gc.setForegroundPattern(temporaryPattern);
				changedTransform = new Transform(display, 2, 0, 0, 2, 30, 40);
				gc.setTransform(changedTransform);
			}

			assertEquals(expectedClip, gc.getClipping());
			assertLineAttributes(expectedLine, gc.getLineAttributes());
			assertEquals(expectedAlpha, gc.getAlpha());
			assertEquals(expectedAntialias, gc.getAntialias());
			assertEquals(expectedTextAntialias, gc.getTextAntialias());
			assertEquals(expectedInterpolation, gc.getInterpolation());
			assertEquals(expectedFillRule, gc.getFillRule());
			assertEquals(expectedXor, gc.getXORMode());
			assertEquals(expectedForeground, gc.getForeground());
			assertEquals(expectedBackground, gc.getBackground());
			assertNull(gc.getForegroundPattern());
			assertArrayEquals(expectedTransform, elements(gc), 0.0001f);
		} finally {
			if (changedTransform != null) changedTransform.dispose();
			if (initialTransform != null) initialTransform.dispose();
			if (temporaryPattern != null) temporaryPattern.dispose();
			gc.dispose();
			image.dispose();
		}
	}

	private static float[] elements(GC gc) {
		Transform transform = new Transform(gc.getDevice());
		try {
			gc.getTransform(transform);
			float[] values = new float[6];
			transform.getElements(values);
			return values;
		} finally {
			transform.dispose();
		}
	}

	private static void assertLineAttributes(LineAttributes expected, LineAttributes actual) {
		assertEquals(expected.width, actual.width, 0.0001f);
		assertEquals(expected.cap, actual.cap);
		assertEquals(expected.join, actual.join);
		assertEquals(expected.style, actual.style);
		assertEquals(expected.dashOffset, actual.dashOffset, 0.0001f);
		assertEquals(expected.miterLimit, actual.miterLimit, 0.0001f);
		if (expected.dash == null) {
			assertNull(actual.dash);
		} else {
			assertArrayEquals(expected.dash, actual.dash, 0.0001f);
		}
	}
}
