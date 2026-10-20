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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.eclipse.nebula.widgets.m3.M3MultiSplitPane;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3MultiSplitPane: tiling by cumulative rounding, a divider move changes exactly two siblings (others not moved),
 * mouse drag, a 50-size resize sweep with no full repaint and exact tiling, the hidden gate, errors. Runs in both
 * startup modes.
 */
public class M3MultiSplitPaneTest {

	Display display;
	Shell shell;
	M3MultiSplitPane pane;
	List<Rectangle> paints;
	int a, column, b, c, d;
	Label la, lb, lc, ld;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 420, 320);
		pane = build(shell);
		pane.setBounds(0, 0, 400, 300);
		paints = M3TestSupport.recordPaints(pane);
		shell.open();
		M3TestSupport.flush(display);
	}

	M3MultiSplitPane build(Shell parent) {
		M3MultiSplitPane split = new M3MultiSplitPane(parent, SWT.HORIZONTAL);
		la = label(split, "A");
		lb = label(split, "B");
		lc = label(split, "C");
		ld = label(split, "D");
		a = split.addLeaf(0, 1, la);
		column = split.addSplit(0, M3MultiSplitPane.COLUMN, 2);
		b = split.addLeaf(column, 1, lb);
		c = split.addLeaf(column, 1, lc);
		d = split.addLeaf(0, 1, ld);
		return split;
	}

	static Label label(M3MultiSplitPane split, String text) {
		Label label = new Label(split, SWT.BORDER);
		label.setText(text);
		return label;
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3MultiSplitPane", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	void assertTiles() {
		Rectangle client = pane.getClientArea();
		Rectangle ra = pane.getNodeBounds(a), rcol = pane.getNodeBounds(column), rd = pane.getNodeBounds(d);
		assertEquals(client.x, ra.x);
		assertEquals(ra.x + ra.width + 6, rcol.x);
		assertEquals(rcol.x + rcol.width + 6, rd.x);
		assertEquals(client.x + client.width, rd.x + rd.width);
		Rectangle rb = pane.getNodeBounds(b), rc = pane.getNodeBounds(c);
		assertEquals(rcol.y, rb.y);
		assertEquals(rb.y + rb.height + 6, rc.y);
		assertEquals(rcol.y + rcol.height, rc.y + rc.height);
		assertEquals(ra, la.getBounds());
		assertEquals(rb, lb.getBounds());
		assertEquals(rc, lc.getBounds());
		assertEquals(rd, ld.getBounds());
	}

	@Test
	public void splitsTileByWeight() {
		assertTiles();
		assertEquals(97, pane.getNodeBounds(a).width);
		assertEquals(194, pane.getNodeBounds(column).width);
		assertEquals(97, pane.getNodeBounds(d).width);
		assertEquals(5, pane.getNodeCount());
		assertEquals(M3MultiSplitPane.COLUMN, pane.getKind(column));
		assertEquals(column, pane.getParent(b));
		assertEquals(new Rectangle(97, 0, 6, 300), pane.getDividerBounds(column));
		assertEquals(0, pane.getDividerBounds(a).width);
	}

	@Test
	public void dividerMoveChangesTwoSiblingsOnly() {
		int[] moves = new int[1];
		ld.addListener(SWT.Move, e -> moves[0]++);
		ld.addListener(SWT.Resize, e -> moves[0]++);
		Rectangle before = pane.getDividerBounds(column);
		paints.clear();
		int[] selections = new int[1];
		pane.addListener(SWT.Selection, e -> selections[0] = e.index);
		pane.moveDivider(column, 20);
		pane.update();
		assertEquals(117, pane.getNodeBounds(a).width);
		assertEquals(174, pane.getNodeBounds(column).width);
		assertEquals(column, selections[0]);
		assertTiles();
		assertEquals("D untouched", 0, moves[0]);
		if (viewport()) {
			Rectangle strip = before.union(pane.getDividerBounds(column));
			M3TestSupport.assertPaintsInside(paints, strip);
		}
		pane.moveDivider(column, -1000);
		assertEquals(0, pane.getNodeBounds(a).width);
		assertTiles();
	}

	@Test
	public void mouseDragMovesTheDivider() {
		Rectangle divider = pane.getDividerBounds(c);
		Event down = new Event();
		down.x = divider.x + divider.width / 2;
		down.y = divider.y + 2;
		down.button = 1;
		pane.notifyListeners(SWT.MouseDown, down);
		Event move = new Event();
		move.x = down.x;
		move.y = down.y + 15;
		move.stateMask = SWT.BUTTON1;
		pane.notifyListeners(SWT.MouseMove, move);
		pane.notifyListeners(SWT.MouseUp, new Event());
		assertEquals(divider.y + 15, pane.getDividerBounds(c).y);
		assertTiles();
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		M3Counters.reset();
		int height = pane.getSize().y;
		for (int width : M3TestSupport.sweepWidths(400)) {
			pane.setSize(width, height);
			assertTiles();
		}
		pane.update();
		assertEquals(M3Counters.line(), viewport() ? 0 : 50, M3Counters.fullRepaints);
		assertEquals(M3Counters.line(), 0, M3Counters.measures);
	}

	@Test
	public void hiddenPaneFoldsDeltasAndPaintsOnceOnShow() {
		if (!viewport()) {
			return;
		}
		Random random = new Random(5);
		double[] weights = new double[5];
		M3TestSupport.assertHiddenGate(pane, paints, i -> {
			int node = 1 + random.nextInt(4);
			weights[node] = 0.5 + random.nextInt(4);
			pane.setWeight(node, weights[node]);
		});
		assertTiles();
		Shell other = new Shell(display, SWT.NO_TRIM);
		other.setBounds(10, 10, 420, 320);
		M3MultiSplitPane fresh = build(other);
		fresh.setBounds(0, 0, 400, 300);
		for (int node = 1; node < 5; node++) {
			fresh.setWeight(node, pane.getWeight(node));
		}
		other.open();
		M3TestSupport.flush(display);
		for (int node = 0; node < 5; node++) {
			assertEquals(fresh.getNodeBounds(node), pane.getNodeBounds(node));
		}
		other.dispose();
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> pane.addLeaf(9, 1, null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.addLeaf(a, 1, null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.addSplit(0, M3MultiSplitPane.LEAF, 1));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.addLeaf(0, 0, null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.setWeight(a, Double.NaN));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_PARENT, () -> pane.addLeaf(0, 1, new Label(shell, SWT.NONE)));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.moveDivider(a, 5));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.setControl(column, null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.setDividerSize(0));
		pane.setDividerSize(4);
		assertEquals(4, pane.getDividerSize());
		assertTrue(pane.computeSize(SWT.DEFAULT, SWT.DEFAULT).x > 0);
	}
}
