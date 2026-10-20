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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.M3TestAccess;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.internal.graphics.GCAdapter;
import org.eclipse.swt.internal.graphics.GCOperation;
import org.eclipse.swt.internal.graphics.M3GCOperation;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Edge cases of the RWT GC additions: strokes, image patterns, nested replay, recorded state and arguments. */
class M3GCCompatEdgeTest {

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
	void strokePatternAppliesToEveryOutline () {
		gc.setLineWidth (3);
		gc.setForegroundPattern (new Pattern (display, 0, 0, 0, 10, new Color (1, 1, 1), new Color (2, 2, 2)));
		gc.drawRectangle (0, 0, 10, 10);
		gc.drawRoundRectangle (0, 0, 10, 10, 4, 4);
		gc.drawOval (0, 0, 10, 10);
		gc.drawPolyline (new int[] {0, 0, 5, 5, 9, 0});
		Path path = new Path (display);
		path.moveTo (0, 0);
		path.lineTo (5, 5);
		gc.drawPath (path);
		gc.fillRectangle (0, 0, 5, 5);
		gc.setForegroundPattern (null);
		gc.drawLine (0, 0, 1, 1);
		List<String> ops = operations ();
		assertEquals (5, count (ops, "[\"strokeStyle\",\"linearGradient\"]"));
		assertEquals (5, count (ops, "[\"createLinearGradient\""));
	}

	@Test
	void imagePatternClipsPolygonsEvenOddAndLimitsTiles () {
		Image image = new Image (display, 10, 10);
		gc.setBackgroundPattern (new Pattern (display, image));
		gc.fillPolygon (new int[] {0, 0, 20, 0, 10, 20});
		gc.fillOval (0, 0, 20, 20);
		Image tiny = new Image (display, 1, 1);
		gc.setBackgroundPattern (new Pattern (display, tiny));
		gc.fillRectangle (0, 0, 100, 100);
		gc.drawLine (0, 0, 5, 5);
		List<String> ops = operations ();
		assertTrue (ops.contains ("[\"clip\",\"evenodd\"]"), ops.toString ());
		assertEquals (1, count (ops, "[\"fill\"]"), "the 10,000-tile fill stays a plain fill: " + ops);
	}

	@Test
	void nestedRecordedImagesComposeTransformsAlphaAndClips () {
		Image inner = new Image (display, 4, 4);
		GC innerGC = new GC (inner);
		innerGC.setAlpha (128);
		innerGC.setTransform (new Transform (display, 1, 0, 0, 1, 1, 1));
		innerGC.setClipping (0, 0, 2, 2);
		innerGC.setLineAttributes (new LineAttributes (2));
		innerGC.setLineCap (SWT.CAP_ROUND);
		innerGC.setLineJoin (SWT.JOIN_ROUND);
		innerGC.setLineDash (new int[] {1, 2});
		innerGC.setFont (new Font (display, "Arial", 20, SWT.BOLD));
		innerGC.setFillRule (SWT.FILL_WINDING);
		innerGC.setForegroundPattern (new Pattern (display, 0, 0, 4, 0, new Color (1, 1, 1), new Color (2, 2, 2)));
		innerGC.drawLine (0, 0, 3, 3);
		Path clip = new Path (display);
		clip.addRectangle (0, 0, 3, 3);
		innerGC.setClipping (clip);
		innerGC.drawPoint (1, 1);
		Image outer = new Image (display, 8, 8);
		GC outerGC = new GC (outer);
		outerGC.setAlpha (128);
		outerGC.drawImage (inner, 2, 2);
		outerGC.dispose ();
		gc.setTransform (new Transform (display, 2, 0, 0, 2, 0, 0));
		gc.drawImage (outer, 0, 0);
		List<String> ops = operations ();
		assertTrue (ops.contains ("[\"setTransform\",2,0,0,2,0,0]"), ops.toString ());
		assertTrue (ops.contains ("[\"setTransform\",2,0,0,2,4,4]"), ops.toString ());
		assertTrue (ops.contains ("[\"setTransform\",2,0,0,2,6,6]"), ops.toString ());
		assertTrue (ops.contains ("[\"globalAlpha\",0.25]"), ops.toString ());
		assertTrue (count (ops, "[\"restore\"]") >= 3, ops.toString ());
		innerGC.dispose ();
		innerGC.dispose ();
	}

	@Test
	void compactionNeedsAnOpaqueUnclippedUntransformedCover () {
		Image image = new Image (display, 10, 10);
		GC imageGC = new GC (image);
		imageGC.setClipping (0, 0, 5, 5);
		imageGC.fillRectangle (0, 0, 10, 10);
		imageGC.setClipping ((Rectangle) null);
		imageGC.setAlpha (100);
		imageGC.fillRectangle (0, 0, 10, 10);
		imageGC.setAlpha (255);
		imageGC.setTransform (new Transform (display, 1, 0, 0, 1, 1, 0));
		imageGC.fillRectangle (0, 0, 10, 10);
		imageGC.setTransform (null);
		imageGC.setBackgroundPattern (new Pattern (display, 0, 0, 10, 0, new Color (1, 1, 1), 10, new Color (2, 2, 2), 255));
		imageGC.fillRectangle (0, 0, 10, 10);
		imageGC.setBackgroundPattern (new Pattern (display, image));
		imageGC.fillRectangle (0, 0, 10, 10);
		imageGC.fillRectangle (1, 1, 5, 5);
		imageGC.setBackgroundPattern (new Pattern (display, 0, 0, 10, 0, new Color (1, 1, 1), new Color (2, 2, 2)));
		imageGC.fillRectangle (0, 0, 10, 10);
		assertEquals (0, imageGC.getClipping ().x);
		imageGC.dispose ();
		gc.drawImage (image, 0, 0);
		List<String> ops = operations ();
		assertEquals (0, count (ops, "[\"drawImage\""), "the last opaque gradient cover compacts: " + ops);
	}

	@Test
	void argumentsAndStateOnlyMethods () {
		assertThrows (IllegalArgumentException.class, () -> new Pattern (display, (Image) null));
		assertThrows (IllegalArgumentException.class, () -> new Pattern (display, 0, 0, 1, 1, null, new Color (1, 1, 1)));
		Color disposed = new Color (display, 1, 1, 1);
		disposed.dispose ();
		assertThrows (IllegalArgumentException.class, () -> new Pattern (display, 0, 0, 1, 1, disposed, new Color (1, 1, 1)));
		Image gone = new Image (display, 2, 2);
		gone.dispose ();
		assertThrows (IllegalArgumentException.class, () -> new Pattern (display, gone));
		Pattern pattern = new Pattern (display, 0, 0, 1, 1, new Color (1, 1, 1), new Color (2, 2, 2));
		assertTrue (pattern.toString ().startsWith ("Pattern {0.0"));
		assertEquals ("Pattern {image}", new Pattern (display, new Image (display, 2, 2)).toString ());
		pattern.dispose ();
		assertEquals ("Pattern {*DISPOSED*}", pattern.toString ());
		assertThrows (IllegalArgumentException.class, () -> gc.setBackgroundPattern (pattern));
		assertThrows (IllegalArgumentException.class, () -> gc.setForegroundPattern (pattern));
		assertThrows (IllegalArgumentException.class, () -> gc.copyArea (null, 0, 0));
		assertThrows (IllegalArgumentException.class, () -> gc.copyArea (gone, 0, 0));
		gc.setFillRule (SWT.FILL_EVEN_ODD);
		gc.setInterpolation (SWT.NONE);
		gc.setInterpolation (SWT.LOW);
		gc.setInterpolation (SWT.DEFAULT);
		GC displayGC = new GC (display);
		displayGC.setBackgroundPattern (new Pattern (display, 0, 0, 1, 1, new Color (1, 1, 1), new Color (2, 2, 2)));
		Image recorded = new Image (display, 3, 3);
		GC recorder = new GC (recorded);
		recorder.drawLine (0, 0, 2, 2);
		recorder.copyArea (new Image (display, 2, 2), 0, 0);
		recorder.copyArea (0, 0, 1, 1, 1, 1);
		recorder.dispose ();
		displayGC.drawImage (recorded, 0, 0);
		displayGC.dispose ();
		gc.dispose ();
		assertFalse (canvas.isDisposed ());
	}

	@Test
	void longListIsReportedOnceAndStrayEndIsIgnored () {
		Image image = new Image (display, 2, 2);
		GC imageGC = new GC (image);
		for (int i = 0; i <= M3TestAccess.warnOperations (); i++) imageGC.drawPoint (0, 0);
		imageGC.dispose ();
		gc.drawImage (image, 0, 0);
		gc.drawImage (image, 0, 0);
		M3GCWriterSupport support = new M3GCWriterSupport (canvas);
		assertEquals (0, support.handle (new M3GCOperation.EndReplay (), new JsonArray ()).length);
		assertTrue (M3GCWriterSupport.isStroke (new GCOperation.DrawLine (0, 0, 1, 1)));
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
