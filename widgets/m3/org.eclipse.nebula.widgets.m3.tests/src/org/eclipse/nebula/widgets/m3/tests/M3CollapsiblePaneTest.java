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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.nebula.widgets.m3.M3CollapsiblePane;
import org.eclipse.nebula.widgets.m3.M3StatusBar;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.M3HiddenGate;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3CollapsiblePane: collapse and expand with and without animation, events, the content slides during an animation
 * (measured once, never laid out per frame), a collapsed content is neither laid out nor shown, a resize sweep never
 * measures the content, horizontal panes, errors. Runs in both startup modes.
 */
public class M3CollapsiblePaneTest {

	/** a vertical stack layout that counts how often the content is measured and laid out */
	static final class CountingGridLayout extends Layout {
		int measures, layouts;

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			measures++;
			int width = 0, height = 0;
			for (Control child : composite.getChildren()) {
				Point size = child.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
				width = Math.max(width, size.x);
				height += size.y + 2;
			}
			return new Point(width + 4, height + 2);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			layouts++;
			int y = 2, width = composite.getClientArea().width - 4;
			for (Control child : composite.getChildren()) {
				int height = child.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache).y;
				child.setBounds(2, y, width, height);
				y += height + 2;
			}
		}
	}

	Display display;
	Shell shell;
	M3CollapsiblePane pane;
	CountingGridLayout counting;
	int expands, collapses;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 300, 400);
		shell.setLayout(new GridLayout(1, false));
		pane = new M3CollapsiblePane(shell, SWT.VERTICAL);
		pane.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		counting = new CountingGridLayout();
		pane.getContent().setLayout(counting);
		for (int i = 0; i < 5; i++) {
			new Label(pane.getContent(), SWT.NONE).setText("row " + i);
		}
		new Label(shell, SWT.NONE).setText("below");
		pane.addListener(SWT.Expand, e -> expands++);
		pane.addListener(SWT.Collapse, e -> collapses++);
		shell.open();
		shell.layout(true, true);
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3CollapsiblePane", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	void waitForAnimation() {
		for (int i = 0; i < 200 && pane.isAnimating(); i++) {
			M3TestSupport.spin(display, 10);
		}
		assertFalse("animation finished", pane.isAnimating());
	}

	@Test
	public void collapseAndExpandWithoutAnimation() {
		int full = pane.getExtent();
		assertTrue(full > 0);
		assertEquals(full, pane.getSize().y);
		pane.setAnimated(false);
		pane.setCollapsed(true);
		assertTrue(pane.isCollapsed());
		assertEquals(0, pane.getExtent());
		assertEquals(0, pane.getSize().y);
		assertEquals(0, pane.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		if (viewport()) {
			assertFalse("collapsed content is hidden", pane.getContent().getVisible());
		}
		assertEquals(1, collapses);
		pane.setCollapsed(true);
		assertEquals("no event without a change", 1, collapses);
		pane.setCollapsed(false);
		assertEquals(full, pane.getExtent());
		assertEquals(full, pane.getSize().y);
		assertTrue(pane.getContent().getVisible());
		assertEquals(1, expands);
	}

	@Test
	public void animationSlidesTheContentWithoutRelayout() {
		pane.setAnimation(4, 5);
		Set<Integer> heights = new HashSet<>();
		pane.addListener(SWT.Resize, e -> heights.add(pane.getSize().y));
		counting.measures = counting.layouts = 0;
		M3Counters.reset();
		pane.setCollapsed(true);
		assertTrue(pane.isAnimating());
		waitForAnimation();
		assertEquals(0, pane.getSize().y);
		assertTrue("intermediate heights " + heights, heights.size() >= 3);
		if (viewport()) {
			assertEquals("measured once", 1, counting.measures);
			assertEquals("content never laid out while sliding", 0, counting.layouts);
		} else {
			assertTrue(counting.measures >= 3);
		}
		assertEquals(1, collapses);
		pane.setCollapsed(false);
		pane.setCollapsed(true);
		pane.setCollapsed(false);
		waitForAnimation();
		assertEquals(pane.getExtent(), pane.getSize().y);
		assertEquals("reversals complete once", 1, expands);
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		int height = pane.getSize().y;
		counting.measures = counting.layouts = 0;
		M3Counters.reset();
		for (int width : M3TestSupport.sweepWidths(280)) {
			pane.setSize(width, height);
			assertEquals(width, pane.getContent().getSize().x);
		}
		assertEquals("a resize never measures the content", 0, counting.measures);
		assertEquals(M3Counters.line(), 0, M3Counters.fullRepaints + M3Counters.fullLayouts);
		pane.setAnimated(false);
		pane.setCollapsed(true);
		counting.measures = counting.layouts = 0;
		for (int width : M3TestSupport.sweepWidths(280)) {
			pane.setSize(width, 20);
		}
		if (viewport()) {
			assertEquals("collapsed content is not laid out", 0, counting.layouts + counting.measures);
		}
	}

	@Test
	public void horizontalPaneCollapsesItsWidth() {
		Shell other = new Shell(display, SWT.NO_TRIM);
		other.setLayout(new GridLayout(2, false));
		M3CollapsiblePane side = new M3CollapsiblePane(other, SWT.HORIZONTAL);
		side.getContent().setLayout(new FillLayout());
		new Label(side.getContent(), SWT.NONE).setText("a wide side panel label");
		new Label(other, SWT.NONE).setText("main");
		other.pack();
		int full = side.getExtent();
		assertEquals(full, side.getSize().x);
		side.setAnimated(false);
		side.setCollapsed(true);
		assertEquals(0, side.getSize().x);
		side.setCollapsed(false);
		assertEquals(full, side.getSize().x);
		other.dispose();
	}

	@Test
	public void collapsedContentIsAHiddenViewport() {
		if (!viewport()) {
			return;
		}
		M3StatusBar bar = new M3StatusBar(pane.getContent(), SWT.NONE);
		bar.addSlot(M3StatusBar.PREFERRED, 0);
		bar.addSlot(M3StatusBar.FILL, 1);
		bar.setText(0, "row 0");
		java.util.List<org.eclipse.swt.graphics.Rectangle> paints = M3TestSupport.recordPaints(bar);
		shell.layout(true, true);
		M3TestSupport.flush(display);
		pane.setAnimated(false);
		pane.setCollapsed(true);
		M3TestSupport.flush(display);
		paints.clear();
		M3Counters.reset();
		for (int i = 0; i < 10_000; i++) {
			bar.setText(0, "row " + i);
			bar.setText(1, "filter " + i);
		}
		M3TestSupport.flush(display);
		assertEquals("no paint inside a collapsed pane", 0, paints.size());
		assertEquals("hidden ancestor found by one probe, or by PartHidden: " + M3Counters.line(), M3HiddenGate.hasPartVisibility() ? 0 : 1, M3Counters.probes);
		assertEquals(M3Counters.line(), 0, M3Counters.measures);
		pane.setCollapsed(false);
		M3TestSupport.flush(display);
		bar.update();
		assertEquals("one paint pass on expand: " + paints, 1, paints.size());
		assertEquals(M3Counters.line(), 1, M3Counters.reveals);
		assertEquals("row 9999", bar.getText(0));
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.setAnimation(0, 1));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.setAnimation(1, 0));
		pane.setLayout(new FillLayout());
		assertFalse("the pane keeps its own layout", pane.getLayout() instanceof FillLayout);
		pane.setAnimation(100, 1000);
		pane.setCollapsed(true);
		pane.dispose();
		M3TestSupport.spin(display, 30);
	}
}
