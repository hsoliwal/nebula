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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rap.rwt.testfixture.internal.Fixture;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.graphics.GCAdapter;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Canvas repaint as delta views on RWT (rule 24): damage accumulates within a request, a hidden canvas paints
 * nothing until it is shown, scroll repaints source and destination; ScrollBar keeps its increments.
 */
class M3CanvasPaintTest {

	private Display display;
	private Shell shell;
	private Canvas canvas;
	private final List<Rectangle> paints = new ArrayList<> ();

	@BeforeEach
	void setUp () {
		Fixture.setUp ();
		Fixture.fakeResponseWriter ();
		display = new Display ();
		shell = new Shell (display);
		shell.setSize (300, 300);
		canvas = new Canvas (shell, SWT.V_SCROLL);
		canvas.setSize (200, 200);
		canvas.addListener (SWT.Paint, event -> {
			paints.add (new Rectangle (event.x, event.y, event.width, event.height));
			event.gc.drawLine (0, 0, 1, 1);
		});
		shell.open ();
		paints.clear ();
	}

	@AfterEach
	void tearDown () {
		Fixture.tearDown ();
	}

	@Test
	void partialRedrawsWithinARequestAccumulate () {
		canvas.redraw (0, 0, 10, 10, false);
		canvas.redraw (50, 50, 10, 10, false);
		assertEquals (new Rectangle (0, 0, 60, 60), paints.get (1));
		assertEquals (new Rectangle (0, 0, 60, 60), canvas.getAdapter (GCAdapter.class).getPaintRect ());
	}

	@Test
	void hiddenCanvasPaintsOnceWhenShown () throws IOException {
		canvas.setVisible (false);
		canvas.redraw (0, 0, 10, 10, false);
		canvas.redraw (20, 20, 10, 10, false);
		canvas.setSize (180, 180);
		assertTrue (paints.isEmpty (), paints.toString ());
		assertNotNull (canvas.getAdapter (GCAdapter.class).getHiddenDamage ());
		canvas.setVisible (true);
		CanvasLCA.INSTANCE.renderChanges (canvas);
		assertEquals (1, paints.size (), paints.toString ());
		assertNull (canvas.getAdapter (GCAdapter.class).getHiddenDamage ());
		assertNotNull (Fixture.getProtocolMessage ().findCallOperation (GCOperationWriter.getGcId (canvas), "draw"));
	}

	@Test
	void hiddenAncestorDefersToo () throws IOException {
		shell.setVisible (false);
		canvas.redraw ();
		assertTrue (paints.isEmpty ());
		CanvasLCA.INSTANCE.renderChanges (canvas);
		assertTrue (paints.isEmpty (), "still hidden: nothing painted at render time");
		shell.setVisible (true);
		CanvasLCA.INSTANCE.renderChanges (canvas);
		assertEquals (1, paints.size ());
	}

	@Test
	void scrollRepaintsSourceAndDestination () {
		canvas.scroll (0, 0, 0, 10, 100, 50, false);
		assertEquals (new Rectangle (0, 0, 100, 60), paints.get (0));
		canvas.scroll (0, 0, 0, 0, 0, 10, false);
		assertEquals (1, paints.size ());
		assertNull (M3CanvasPaint.scrollDamage (0, 0, 0, 0, 5, -1));
	}

	@Test
	void scrollBarKeepsIncrementsAndValues () {
		ScrollBar bar = canvas.getVerticalBar ();
		assertEquals (1, bar.getIncrement ());
		assertEquals (10, bar.getPageIncrement ());
		bar.setIncrement (5);
		bar.setIncrement (0);
		bar.setPageIncrement (40);
		bar.setPageIncrement (-1);
		assertEquals (5, bar.getIncrement ());
		assertEquals (40, bar.getPageIncrement ());
		bar.setValues (95, 0, 100, 20, 2, 30);
		assertEquals (80, bar.getSelection ());
		assertEquals (20, bar.getThumb ());
		assertEquals (2, bar.getIncrement ());
		assertEquals (30, bar.getPageIncrement ());
		bar.setValues (5, -1, 100, 20, 2, 30);
		bar.setValues (5, 0, -1, 20, 2, 30);
		bar.setValues (5, 0, 100, 0, 2, 30);
		bar.setValues (5, 0, 100, 20, 0, 30);
		bar.setValues (5, 0, 100, 20, 2, 0);
		assertEquals (80, bar.getSelection (), "out-of-range values change nothing");
	}
}
