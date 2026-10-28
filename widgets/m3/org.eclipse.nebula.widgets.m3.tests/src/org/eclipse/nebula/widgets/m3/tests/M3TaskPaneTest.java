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
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.nebula.widgets.m3.M3TaskPane;
import org.eclipse.nebula.widgets.m3.M3TaskPaneContainer;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3TaskPane and M3TaskPaneContainer: stacking, a collapse moves the panes below without resizing them and leaves the
 * panes above untouched, only the changed pane's content is measured, title-bar click and events, title-bar deltas
 * (title repaints the bar, collapse repaints the chevron), a resize sweep, the hidden gate of the title bar, errors.
 * Runs in both startup modes.
 */
public class M3TaskPaneTest {

	Display display;
	Shell shell;
	M3TaskPaneContainer container;
	M3TaskPane[] panes = new M3TaskPane[3];
	M3CollapsiblePaneTest.CountingGridLayout[] contents = new M3CollapsiblePaneTest.CountingGridLayout[3];
	int[] resizes = new int[3], moves = new int[3];

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 300, 600);
		shell.setLayout(new FillLayout());
		container = new M3TaskPaneContainer(shell, SWT.NONE);
		for (int i = 0; i < panes.length; i++) {
			M3TaskPane pane = new M3TaskPane(container, SWT.NONE);
			pane.setTitle("Tasks " + i);
			pane.setAnimated(false);
			contents[i] = new M3CollapsiblePaneTest.CountingGridLayout();
			pane.getContent().setLayout(contents[i]);
			for (int k = 0; k < 4; k++) {
				new Label(pane.getContent(), SWT.NONE).setText("task " + i + "." + k);
			}
			int index = i;
			pane.addListener(SWT.Resize, e -> resizes[index]++);
			pane.addListener(SWT.Move, e -> moves[index]++);
			panes[i] = pane;
		}
		shell.open();
		shell.layout(true, true);
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3TaskPane", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	void assertStacked() {
		int y = container.getMargin();
		for (M3TaskPane pane : panes) {
			Rectangle bounds = pane.getBounds();
			assertEquals(y, bounds.y);
			assertEquals(pane.computeSize(bounds.width, SWT.DEFAULT).y, bounds.height);
			y += bounds.height + container.getSpacing();
		}
	}

	@Test
	public void collapseMovesOnlyThePanesBelow() {
		assertStacked();
		int height = panes[1].getSize().y;
		java.util.Arrays.fill(resizes, 0);
		java.util.Arrays.fill(moves, 0);
		for (M3CollapsiblePaneTest.CountingGridLayout content : contents) {
			content.measures = content.layouts = 0;
		}
		int[] collapses = new int[1];
		panes[1].addListener(SWT.Collapse, e -> collapses[0]++);
		panes[1].setCollapsed(true);
		M3TestSupport.flush(display);
		assertTrue(panes[1].getSize().y < height);
		assertEquals(1, collapses[0]);
		if (viewport()) {
			assertEquals("pane above untouched", 0, resizes[0] + moves[0]);
			assertEquals("pane below moved, not resized", 0, resizes[2]);
			assertTrue(moves[2] > 0);
			assertEquals("other contents not measured", 0, contents[0].measures + contents[2].measures);
			assertEquals("other contents not laid out", 0, contents[0].layouts + contents[2].layouts);
		}
		assertStacked();
		panes[1].setCollapsed(false);
		M3TestSupport.flush(display);
		assertStacked();
		assertEquals(height, panes[1].getSize().y);
	}

	@Test
	public void titleBarClickTogglesAndRepaintsTheChevron() {
		List<Rectangle> paints = M3TestSupport.recordPaints(panes[0].getTitleBar());
		paints.clear();
		Event down = new Event();
		down.button = 1;
		panes[0].getTitleBar().notifyListeners(SWT.MouseDown, down);
		M3TestSupport.flush(display);
		panes[0].getTitleBar().update();
		assertTrue(panes[0].isCollapsed());
		if (viewport()) {
			M3TestSupport.assertPaintsInside(paints, panes[0].getChevronBounds());
		}
		paints.clear();
		panes[0].setTitle("Renamed");
		panes[0].getTitleBar().update();
		assertFalse(paints.isEmpty());
		assertEquals("Renamed", panes[0].getTitle());
		panes[0].setSpecial(true);
		assertTrue(panes[0].isSpecial());
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		M3Counters.reset();
		int height = container.getSize().y;
		for (int width : M3TestSupport.sweepWidths(300)) {
			container.setSize(width, height);
			assertStacked();
		}
		assertEquals(M3Counters.line(), 0, M3Counters.fullRepaints);
	}

	@Test
	public void hiddenTitleBarFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		List<Rectangle> paints = M3TestSupport.recordPaints(panes[2].getTitleBar());
		M3TestSupport.assertHiddenGate(panes[2].getTitleBar(), paints, i -> {
			panes[2].setTitle("Tasks " + i);
			panes[2].setSpecial(i % 2 == 0);
		});
		assertEquals("Tasks 9999", panes[2].getTitle());
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> panes[0].setTitle(null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> container.setSpacing(-1, 0));
		container.setSpacing(4, 2);
		assertEquals(2, container.getSpacing());
		assertEquals(4, container.getMargin());
		container.setLayout(new FillLayout());
		assertFalse(container.getLayout() instanceof FillLayout);
		panes[0].setLayout(new FillLayout());
		assertFalse(panes[0].getLayout() instanceof FillLayout);
	}
}
