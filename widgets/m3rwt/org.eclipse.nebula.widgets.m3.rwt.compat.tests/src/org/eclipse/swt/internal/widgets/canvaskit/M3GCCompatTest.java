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
package org.eclipse.swt.internal.widgets.canvaskit;

import static org.eclipse.swt.internal.widgets.canvaskit.GCOperationWriter.getGcId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rap.json.JsonArray;
import org.eclipse.rap.json.JsonValue;
import org.eclipse.rap.rwt.internal.protocol.Operation.CallOperation;
import org.eclipse.rap.rwt.testfixture.internal.Fixture;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.internal.graphics.GCAdapter;
import org.eclipse.swt.internal.graphics.GCOperation;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The GC methods, Pattern, image drawing and Color constructors added to RWT 4.8.0 for M3, checked on the
 * operations the browser receives (docs/m3/rwt/RWT_GC_COMPAT.md). Runs on RAP's test fixture, no browser.
 */
class M3GCCompatTest {

	private Display display;
	private Canvas canvas;
	private GC gc;

	@BeforeEach
	void setUp () {
		Fixture.setUp ();
		Fixture.fakeResponseWriter ();
		display = new Display ();
		Shell shell = new Shell (display);
		canvas = new Canvas (shell, SWT.NONE);
		canvas.setSize (100, 200);
		gc = new GC (canvas);
	}

	@AfterEach
	void tearDown () {
		Fixture.tearDown ();
	}

	@Test
	void devicelessColors () {
		Color color = new Color (1, 2, 3);
		assertEquals (new RGB (1, 2, 3), color.getRGB ());
		assertEquals (255, color.getAlpha ());
		assertEquals (7, new Color (1, 2, 3, 7).getAlpha ());
		assertEquals (new RGB (4, 5, 6), new Color (new RGB (4, 5, 6)).getRGB ());
		assertEquals (9, new Color (new RGB (4, 5, 6), 9).getAlpha ());
		assertEquals (new RGBA (7, 8, 9, 10), new Color (new RGBA (7, 8, 9, 10)).getRGBA ());
		color.dispose ();
		assertFalse (color.isDisposed ());
		assertEquals (new RGB (1, 2, 3), color.getRGB ());
		Color withDevice = new Color (display, 1, 2, 3);
		withDevice.dispose ();
		assertTrue (withDevice.isDisposed ());
		assertThrows (IllegalArgumentException.class, () -> new Color (256, 0, 0));
		assertThrows (IllegalArgumentException.class, () -> new Color ((RGB) null));
		assertThrows (IllegalArgumentException.class, () -> new Color ((RGBA) null));
	}

	@Test
	void gradientFillWithinOnePeriod () {
		gc.setBackgroundPattern (new Pattern (display, 0, 0, 0, 100, new Color (255, 0, 0), new Color (0, 0, 255)));
		gc.fillRectangle (0, 0, 50, 100);
		List<String> ops = operations ();
		int start = ops.indexOf ("[\"createLinearGradient\",0,0,0,100]");
		assertTrue (start > 0, ops.toString ());
		assertEquals ("[\"save\"]", ops.get (start - 1));
		assertEquals ("[\"addColorStop\",0,[255,0,0,255]]", ops.get (start + 1));
		assertEquals ("[\"addColorStop\",1,[0,0,255,255]]", ops.get (start + 2));
		assertEquals ("[\"fillStyle\",\"linearGradient\"]", ops.get (start + 3));
		assertEquals ("[\"rect\",0,0,50,100]", ops.get (start + 5));
		assertEquals ("[\"fill\"]", ops.get (start + 6));
		assertEquals ("[\"restore\"]", ops.get (start + 7));
		assertTrue (ops.get (start + 8).startsWith ("[\"fillStyle\",["), "client state restored: " + ops);
	}

	@Test
	void gradientRepeatsOutsideItsEndPoints () {
		gc.setBackgroundPattern (new Pattern (display, 0, 0, 0, 50, new Color (255, 0, 0), new Color (0, 0, 255)));
		gc.fillRectangle (0, 0, 10, 100);
		List<String> ops = operations ();
		assertTrue (ops.contains ("[\"createLinearGradient\",0,0,0,100]"), ops.toString ());
		assertEquals (4, count (ops, "[\"addColorStop\","));
		assertTrue (ops.contains ("[\"addColorStop\",0.5,[0,0,255,255]]"), ops.toString ());
		assertTrue (ops.contains ("[\"addColorStop\",0.5,[255,0,0,255]]"), ops.toString ());
	}

	@Test
	void equalAlphaBecomesGlobalAlpha () {
		gc.setBackgroundPattern (new Pattern (display, 0, 0, 100, 0, new Color (0, 0, 0), 128, new Color (9, 9, 9), 128));
		gc.fillOval (0, 0, 100, 100);
		List<String> ops = operations ();
		assertTrue (ops.contains ("[\"globalAlpha\",0.5]"), ops.toString ());
		assertEquals (0, count (ops, "[\"clip\""));
	}

	@Test
	void differentAlphaIsDrawnInBands () {
		gc.setBackgroundPattern (new Pattern (display, 0, 0, 100, 0, new Color (0, 0, 0), 0, new Color (9, 9, 9), 255));
		gc.fillRectangle (0, 0, 100, 10);
		List<String> ops = operations ();
		assertEquals (M3GradientMath.BANDS_PER_PERIOD, count (ops, "[\"clip\""));
		assertEquals (M3GradientMath.BANDS_PER_PERIOD, count (ops, "[\"rect\",0,0,100,10]"));
	}

	@Test
	void foregroundPatternStrokesAndBackgroundColorClearsPattern () {
		Pattern pattern = new Pattern (display, 0, 0, 10, 0, new Color (1, 1, 1), new Color (2, 2, 2));
		gc.setForegroundPattern (pattern);
		assertSame (pattern, gc.getForegroundPattern ());
		gc.drawLine (0, 0, 10, 10);
		gc.setBackgroundPattern (pattern);
		gc.setBackground (new Color (5, 6, 7));
		assertNull (gc.getBackgroundPattern ());
		gc.fillRectangle (0, 0, 5, 5);
		List<String> ops = operations ();
		assertTrue (ops.contains ("[\"strokeStyle\",\"linearGradient\"]"), ops.toString ());
		assertEquals (1, count (ops, "[\"createLinearGradient\""), ops.toString ());
	}

	@Test
	void fillRuleIsEvenOddByDefault () {
		assertEquals (SWT.FILL_EVEN_ODD, gc.getFillRule ());
		gc.fillPolygon (new int[] {0, 0, 10, 0, 5, 10});
		gc.setFillRule (SWT.FILL_WINDING);
		assertEquals (SWT.FILL_WINDING, gc.getFillRule ());
		gc.fillPolygon (new int[] {0, 0, 10, 0, 5, 10});
		List<String> ops = operations ();
		assertEquals (1, count (ops, "[\"fill\",\"evenodd\"]"));
		assertEquals (1, ops.stream ().filter ("[\"fill\"]"::equals).count ());
		assertThrows (IllegalArgumentException.class, () -> gc.setFillRule (7));
	}

	@Test
	void interpolationAndXorAreKept () {
		assertEquals (SWT.DEFAULT, gc.getInterpolation ());
		gc.setInterpolation (SWT.HIGH);
		assertEquals (SWT.HIGH, gc.getInterpolation ());
		assertThrows (IllegalArgumentException.class, () -> gc.setInterpolation (5));
		assertFalse (gc.getXORMode ());
		gc.setXORMode (true);
		assertTrue (gc.getXORMode ());
		assertFalse (gc.isDisposed ());
	}

	@Test
	void doubleBufferedImageIsReplayedAndCompacted () {
		Image buffer = new Image (display, 10, 10);
		GC imageGC = new GC (buffer);
		imageGC.setBackground (new Color (200, 0, 0));
		imageGC.fillRectangle (0, 0, 10, 10);
		imageGC.drawLine (0, 0, 9, 9);
		imageGC.dispose ();
		gc.drawImage (buffer, 5, 6);
		List<String> ops = operations ();
		assertTrue (ops.contains ("[\"setTransform\",1,0,0,1,5,6]"), ops.toString ());
		assertTrue (ops.contains ("[\"fillStyle\",[200,0,0,255]]"), ops.toString ());
		assertTrue (ops.contains ("[\"rect\",0,0,10,10]"), ops.toString ());
		assertEquals (0, count (ops, "[\"drawImage\""), "the full opaque fill drops the image's own blank pixels");
		assertTrue (ops.contains ("[\"setTransform\",1,0,0,1,0,0]"), "transform restored: " + ops);
	}

	@Test
	void overlayKeepsTheImagePixelsAndScales () {
		Image image = new Image (display, 10, 10);
		GC imageGC = new GC (image);
		imageGC.drawLine (0, 0, 9, 9);
		imageGC.dispose ();
		gc.setTransform (new Transform (display, 2, 0, 0, 2, 0, 0));
		gc.drawImage (image, 0, 0, 10, 10, 0, 0, 20, 20);
		List<String> ops = operations ();
		assertEquals (1, count (ops, "[\"drawImage\""), ops.toString ());
		assertTrue (ops.contains ("[\"setTransform\",4,0,0,4,0,0]"), ops.toString ());
	}

	@Test
	void oneGCPerImageAndCopyArea () {
		Image source = new Image (display, 10, 10);
		GC first = new GC (source);
		assertThrows (IllegalArgumentException.class, () -> new GC (source));
		first.setBackground (new Color (0, 200, 0));
		first.fillRectangle (0, 0, 10, 10);
		Image target = new Image (display, 4, 4);
		first.copyArea (target, 3, 3);
		first.dispose ();
		gc.drawImage (target, 0, 0);
		gc.copyArea (target, 0, 0);
		gc.copyArea (0, 0, 10, 10, 5, 5);
		List<String> ops = operations ();
		assertTrue (ops.contains ("[\"setTransform\",1,0,0,1,-3,-3]"), ops.toString ());
		assertTrue (ops.contains ("[\"fillStyle\",[0,200,0,255]]"), ops.toString ());
	}

	@Test
	void gradientMath () {
		float[] g = M3GradientMath.gradient (0, 0, 0, 0, new float[] {0, 0, 1, 1});
		assertEquals (1, g[4]);
		g = M3GradientMath.gradient (0, 0, 1, 0, new float[] {0, 0, 1000, 1});
		assertEquals (1, g[4], "more than MAX_PERIODS periods are clamped");
		assertEquals (0.5f, M3GradientMath.globalAlpha (255, 128));
		int[] tiles = M3GradientMath.tiles (new float[] {5, 5, 25, 15}, 10, 10);
		assertEquals ("[0, 0, 3, 2]", java.util.Arrays.toString (tiles));
		float[] product = M3GradientMath.multiply (new float[] {2, 0, 0, 2, 1, 1}, new float[] {1, 0, 0, 1, 3, 4});
		assertEquals ("[2.0, 0.0, 0.0, 2.0, 7.0, 9.0]", java.util.Arrays.toString (product));
		assertEquals ("[0.0, 0.0, 0.0, 0.0]", java.util.Arrays.toString (M3GradientMath.extent (new int[0])));
	}

	@Test
	void imagePatternTilesInsideTheShape () {
		Image image = new Image (display, 10, 10);
		gc.setBackgroundPattern (new Pattern (display, image));
		gc.fillRectangle (0, 0, 20, 10);
		List<String> ops = operations ();
		assertEquals (2, count (ops, "[\"drawImage\""), ops.toString ());
		assertTrue (ops.contains ("[\"clip\"]"), ops.toString ());
		assertFalse (ops.contains ("[\"fill\"]"), ops.toString ());
	}

	private List<String> operations () {
		GCOperation[] operations = canvas.getAdapter (GCAdapter.class).getGCOperations ();
		GCOperationWriter writer = new GCOperationWriter (canvas);
		for (GCOperation operation : operations) writer.write (operation);
		writer.render ();
		CallOperation draw = Fixture.getProtocolMessage ().findCallOperation (getGcId (canvas), "draw");
		JsonArray array = draw.getParameters ().get ("operations").asArray ();
		List<String> result = new ArrayList<> ();
		for (JsonValue value : array) result.add (value.asArray ().toString ());
		return result;
	}

	private static int count (List<String> ops, String prefix) {
		return (int) ops.stream ().filter (op -> op.startsWith (prefix)).count ();
	}
}
