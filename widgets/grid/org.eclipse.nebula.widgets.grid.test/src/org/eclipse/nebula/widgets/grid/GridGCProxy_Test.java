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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.junit.Test;

public class GridGCProxy_Test {

	@Test
	public void restoresCompleteMutableGcStateAfterPlanePaint() {
		Display display = Display.getDefault();
		Image image = new Image(display, 64, 64);
		GC gc = new GC(image);
		Pattern initialForegroundPattern = null;
		Pattern initialBackgroundPattern = null;
		Pattern temporaryForegroundPattern = null;
		Pattern temporaryBackgroundPattern = null;
		Font changedFont = null;
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
			initialForegroundPattern = new Pattern(
					display, 0, 0, 16, 16,
					display.getSystemColor(SWT.COLOR_BLUE),
					display.getSystemColor(SWT.COLOR_CYAN));
			initialBackgroundPattern = new Pattern(
					display, 0, 0, 16, 16,
					display.getSystemColor(SWT.COLOR_WHITE),
					display.getSystemColor(SWT.COLOR_GRAY));
			gc.setForegroundPattern(initialForegroundPattern);
			gc.setBackgroundPattern(initialBackgroundPattern);
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
			boolean expectedAdvanced = gc.getAdvanced();
			Color expectedForeground = gc.getForeground();
			Color expectedBackground = gc.getBackground();
			Pattern expectedForegroundPattern = gc.getForegroundPattern();
			Pattern expectedBackgroundPattern = gc.getBackgroundPattern();
			Font expectedFont = gc.getFont();
			float[] expectedTransform = elements(gc);

			try (GridGCProxy ignored = GridGCProxy.wrap(gc)) {
				gc.setAdvanced(false);
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
				temporaryForegroundPattern = new Pattern(
						display, 0, 0, 16, 16,
						display.getSystemColor(SWT.COLOR_RED),
						display.getSystemColor(SWT.COLOR_GREEN));
				temporaryBackgroundPattern = new Pattern(
						display, 0, 0, 16, 16,
						display.getSystemColor(SWT.COLOR_BLACK),
						display.getSystemColor(SWT.COLOR_YELLOW));
				gc.setForegroundPattern(temporaryForegroundPattern);
				gc.setBackgroundPattern(temporaryBackgroundPattern);
				changedFont = new Font(
						display,
						expectedFont.getFontData()[0].getName(),
						Math.max(1, expectedFont.getFontData()[0].getHeight() + 1),
						SWT.BOLD);
				gc.setFont(changedFont);
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
			assertTrue(expectedXor == gc.getXORMode());
			assertTrue(expectedAdvanced == gc.getAdvanced());
			assertEquals(expectedForeground, gc.getForeground());
			assertEquals(expectedBackground, gc.getBackground());
			assertSame(expectedForegroundPattern, gc.getForegroundPattern());
			assertSame(expectedBackgroundPattern, gc.getBackgroundPattern());
			assertEquals(expectedFont, gc.getFont());
			assertArrayEquals(expectedTransform, elements(gc), 0.0001f);
		} finally {
            if (changedTransform != null) {
                changedTransform.dispose();
            }
            if (initialTransform != null) {
                initialTransform.dispose();
            }
			gc.dispose();
            if (changedFont != null) {
                changedFont.dispose();
            }
            if (temporaryForegroundPattern != null) {
                temporaryForegroundPattern.dispose();
            }
            if (temporaryBackgroundPattern != null) {
                temporaryBackgroundPattern.dispose();
            }
            if (initialForegroundPattern != null) {
                initialForegroundPattern.dispose();
            }
            if (initialBackgroundPattern != null) {
                initialBackgroundPattern.dispose();
            }
			image.dispose();
		}
	}

	@Test
	public void restoresExactNonRectangularClippingRegion() {
		Display display = Display.getDefault();
		Image image = new Image(display, 64, 64);
		GC gc = new GC(image);
		Region original = new Region(display);
		Region restored = new Region(display);
		try {
			original.add(new Rectangle(2, 2, 8, 8));
			original.add(new Rectangle(30, 30, 8, 8));
			gc.setClipping(original);

			try (GridGCProxy ignored = GridGCProxy.wrap(gc)) {
				gc.setClipping(new Rectangle(0, 0, 64, 64));
			}

			gc.getClipping(restored);
			assertTrue(restored.contains(4, 4));
			assertTrue(restored.contains(32, 32));
			assertFalse(restored.contains(20, 20));
		} finally {
			restored.dispose();
			original.dispose();
			gc.dispose();
			image.dispose();
		}
	}


	@Test
	public void rejectsDisposedGcAndUseAfterClose() {
		Display display = Display.getDefault();
		Image image = new Image(display, 16, 16);
		GC disposed = new GC(image);
		disposed.dispose();
		try {
			try {
				GridGCProxy.wrap(disposed);
				fail("disposed GC must be rejected");
			} catch (IllegalArgumentException expected) {
				// expected
			}

			GC gc = new GC(image);
			try {
				GridGCProxy proxy = GridGCProxy.wrap(gc);
				proxy.close();
				proxy.close();
				try {
					proxy.gc();
					fail("closed scope must reject access");
				} catch (IllegalStateException expected) {
					// expected
				}
				try {
					proxy.alpha(17);
					fail("closed scope must reject mutation");
				} catch (IllegalStateException expected) {
					// expected
				}
			} finally {
				gc.dispose();
			}
		} finally {
			image.dispose();
		}
	}

	@Test
	public void collapsesEquivalentStrokeAndAlphaTransitionsAndFlushesOneAffineNode() {
		Display display = Display.getDefault();
		Image image = new Image(display, 32, 32);
		GC gc = new GC(image);
		try {
			Transform identity = new Transform(display);
			try {
				gc.setTransform(identity);
			} finally {
				identity.dispose();
			}
			LineAttributes stroke = new LineAttributes(
					2f, SWT.CAP_ROUND, SWT.JOIN_BEVEL, SWT.LINE_DASH,
					new float[] {3f, 5f}, 1f, 8f);
			try (GridGCProxy proxy = GridGCProxy.wrap(gc)) {
				assertEquals(0, proxy.nativeStateTransitionCount());

				proxy.lineAttributes(stroke);
				proxy.lineAttributes(new LineAttributes(
						2f, SWT.CAP_ROUND, SWT.JOIN_BEVEL, SWT.LINE_DASH,
						new float[] {3f, 5f}, 1f, 8f));
				assertEquals("equivalent stroke nodes must collapse",
						1, proxy.nativeStateTransitionCount());

				proxy.alpha(123);
				proxy.alpha(123);
				assertEquals("equivalent alpha nodes must collapse",
						2, proxy.nativeStateTransitionCount());

				float[] before = elements(gc);
				proxy.translate(4, 0);
				proxy.translate(4, 0);
				assertEquals("ordered affine deltas stay retained until a renderer requests the GC",
						2, proxy.nativeStateTransitionCount());
				assertArrayEquals("retained affine state must not mutate the native GC early",
						before, elements(gc), 0.0001f);

				assertSame(gc, proxy.gc());
				assertEquals("two ordered translations must flush as one native transform transition",
						3, proxy.nativeStateTransitionCount());
				assertArrayEquals(new float[] {1, 0, 0, 1, 8, 0}, elements(gc), 0.0001f);

				proxy.translate(5, 0);
				proxy.translate(-5, 0);
				assertSame(gc, proxy.gc());
				assertEquals("an affine batch that cancels to identity must not hit the native GC",
						3, proxy.nativeStateTransitionCount());
				assertArrayEquals(new float[] {1, 0, 0, 1, 8, 0}, elements(gc), 0.0001f);
			}
		} finally {
			gc.dispose();
			image.dispose();
		}
	}


	@Test
	public void closeAfterCallerDisposesGcOnlyReleasesScopeSnapshots() {
		Display display = Display.getDefault();
		Image image = new Image(display, 16, 16);
		GC gc = new GC(image);
		GridGCProxy proxy = GridGCProxy.wrap(gc);
		gc.dispose();
		try {
			proxy.close();
			proxy.close();
		} finally {
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
