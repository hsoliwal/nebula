/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Native Grid hover and drag contracts for collapsed virtual subtrees. */
public class GridRowResizerHover_Test {

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(20, 20, 660, 300);
	}

	@After
	public void tearDown() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	@Test
	public void hoverSkipsUnreadChildrenBeforeFollowingVisibleRoot() {
		assumeTrue(GridViewport.ENABLED);
		assertHoverLeavesHiddenChildrenUnread(true);
	}

	@Test
	public void hoverStopsBeforeUnreadTrailingSubtree() {
		assumeTrue(GridViewport.ENABLED);
		assertHoverLeavesHiddenChildrenUnread(false);
	}

	private void assertHoverLeavesHiddenChildrenUnread(final boolean followingRoot) {
		final Fixture fixture = new Fixture(true, 4096, followingRoot, 0);
		fixture.root.setHeight(20);
		if (fixture.tail != null) {
			fixture.tail.setHeight(30);
		}
		open();
		final int materialized = fixture.grid.virtualMaterializedItemCount();
		final int requests = fixture.requests[0];
		assertTrue("setup must leave hidden children unread", materialized < 40);
		fixture.grid.setRowsResizeable(true);
		mouse(fixture.grid, SWT.MouseMove, 5, 180, 0, 0);
		System.out.println("ROW_RESIZER_HOVER following=" + followingRoot + " before=" + materialized
				+ " after=" + fixture.grid.virtualMaterializedItemCount() + " requestsBefore=" + requests
				+ " requestsAfter=" + fixture.requests[0]);
		assertEquals("hover must not realize hidden virtual children", materialized,
				fixture.grid.virtualMaterializedItemCount());
		assertEquals("hover must not request hidden row data", requests, fixture.requests[0]);
		assertEquals("blank row-header area has no resizer", null, fixture.grid.getCursor());
		for (int repeat = 0; repeat < 5; repeat++) {
			mouse(fixture.grid, SWT.MouseMove, 5, 180, 0, 0);
		}
		assertEquals(materialized, fixture.grid.virtualMaterializedItemCount());
		assertEquals(requests, fixture.requests[0]);
	}

	@Test
	public void neighborQueriesSkipUnreadCollapsedChildrenInBothDirections() {
		assumeTrue(GridViewport.ENABLED);
		final Fixture fixture = new Fixture(true, 4096, true, 0);
		fixture.root.setHeight(20);
		fixture.tail.setHeight(30);
		open();
		final int materialized = fixture.grid.virtualMaterializedItemCount();
		final int requests = fixture.requests[0];
		assertTrue("fixture must retain unread hidden children", materialized < 40);
		assertSame(fixture.root, fixture.grid.getNextVisibleItem(null));
		assertSame(fixture.tail, fixture.grid.getNextVisibleItem(fixture.root));
		assertSame(fixture.root, fixture.grid.getPreviousVisibleItem(fixture.tail));
		assertSame(fixture.tail, fixture.grid.getPreviousVisibleItem(null));
		assertEquals("neighbor queries must not create hidden facades",
				materialized, fixture.grid.virtualMaterializedItemCount());
		assertEquals("neighbor queries must not request hidden payload",
				requests, fixture.requests[0]);
	}

	@Test
	public void nextVisibleAtCollapsedTreeEndDoesNotMaterializeChildren() {
		assumeTrue(GridViewport.ENABLED);
		final Fixture fixture = new Fixture(true, 4096, false, 0);
		open();
		final int materialized = fixture.grid.virtualMaterializedItemCount();
		final int requests = fixture.requests[0];
		assertEquals(null, fixture.grid.getNextVisibleItem(fixture.root));
		assertSame(fixture.root, fixture.grid.getPreviousVisibleItem(null));
		assertEquals(materialized, fixture.grid.virtualMaterializedItemCount());
		assertEquals(requests, fixture.requests[0]);
	}

	@Test
	public void neighborQueriesRespectExpansionThenCollapse() {
		assumeTrue(GridViewport.ENABLED);
		final Fixture fixture = new Fixture(true, 64, true, 0);
		open();
		fixture.root.setExpanded(true);
		final GridItem child = fixture.grid.getNextVisibleItem(fixture.root);
		assertSame(fixture.root, child.getParentItem());
		fixture.root.setExpanded(false);
		final int materialized = fixture.grid.virtualMaterializedItemCount();
		assertSame(fixture.tail, fixture.grid.getNextVisibleItem(fixture.root));
		assertSame(fixture.root, fixture.grid.getPreviousVisibleItem(fixture.tail));
		assertEquals(materialized, fixture.grid.virtualMaterializedItemCount());
	}

	@Test
	public void variableHeightOverlapResizesLastBoundaryLikeEagerRows() {
		final Fixture sparse = new Fixture(true, 8, true, 0);
		final Fixture eager = new Fixture(false, 8, true, 320);
		final GridItem sparseLast = new GridItem(sparse.grid, SWT.NONE);
		final GridItem eagerLast = new GridItem(eager.grid, SWT.NONE);
		for (final Fixture fixture : new Fixture[] { sparse, eager }) {
			fixture.root.setHeight(3);
			fixture.tail.setHeight(2);
		}
		sparseLast.setHeight(17);
		eagerLast.setHeight(17);
		open();
		// Boundaries at 4 and 7 both intersect the three-pixel threshold at y=5.
		// The later boundary belongs to the following visible root, after the hidden subtree.
		final int[] expected = { 3, 14, 17, 1, 14 };
		assertArrayEquals(expected, dragOverlap(eager, eagerLast));
		assertArrayEquals(expected, dragOverlap(sparse, sparseLast));
	}

	private int[] dragOverlap(final Fixture fixture, final GridItem last) {
		final int[] resize = { 0, -1 };
		fixture.tail.addListener(SWT.Resize, event -> {
			assertSame(fixture.tail, event.item);
			resize[0]++;
			resize[1] = event.detail;
		});
		fixture.grid.setRowsResizeable(true);
		mouse(fixture.grid, SWT.MouseMove, 5, 5, 0, 0);
		assertSame(display.getSystemCursor(SWT.CURSOR_SIZENS), fixture.grid.getCursor());
		mouse(fixture.grid, SWT.MouseDown, 5, 5, 1, 0);
		mouse(fixture.grid, SWT.MouseMove, 5, 17, 0, SWT.BUTTON1);
		mouse(fixture.grid, SWT.MouseUp, 5, 17, 1, 0);
		return new int[] { fixture.root.getHeight(), fixture.tail.getHeight(), last.getHeight(),
				resize[0], resize[1] };
	}

	private void mouse(final Grid grid, final int type, final int x, final int y, final int button,
			final int stateMask) {
		final Event event = new Event();
		event.x = x;
		event.y = y;
		event.button = button;
		event.stateMask = stateMask;
		grid.notifyListeners(type, event);
	}

	private void open() {
		shell.open();
		while (display.readAndDispatch()) {
			// Finish initial layout and painting before measuring hover side effects.
		}
	}

	private final class Fixture {
		final Grid grid;
		final GridItem root;
		final GridItem tail;
		final int[] requests = { 0 };

		Fixture(final boolean sparse, final int children, final boolean followingRoot, final int x) {
			grid = new Grid(shell, (sparse ? SWT.VIRTUAL : SWT.NONE) | SWT.V_SCROLL);
			grid.setBounds(x, 0, 300, 240);
			grid.setHeaderVisible(false);
			grid.setRowHeaderVisible(true, 40);
			new GridColumn(grid, SWT.NONE).setWidth(220);
			grid.addListener(SWT.SetData, event -> {
				requests[0]++;
				((GridItem) event.item).setText("row " + event.index);
			});
			root = new GridItem(grid, SWT.NONE);
			if (sparse) {
				root.setItemCount(children);
			} else {
				for (int child = 0; child < children; child++) {
					new GridItem(root, SWT.NONE);
				}
			}
			root.setExpanded(false);
			tail = followingRoot ? new GridItem(grid, SWT.NONE) : null;
		}
	}
}
