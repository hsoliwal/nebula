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
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * G4 step 2c: in the viewport mode the children that {@link GridItem#setItemCount(int)} gives an
 * item of an {@code SWT.VIRTUAL} Grid are rows without items until they are read. A Grid built
 * that way and a Grid built with one {@code new GridItem(parent, SWT.NONE)} per child must answer,
 * request data and paint alike under the same seeded script; and a million children make no item
 * until they are read.
 */
public class GridSparseChildren_Test {
	private static final int ROOTS = 30;

	private Display display;
	private Shell shell;
	private Button focusSink;

	@Before
	public void setUp() {
		assumeTrue("rows without items exist in the viewport mode only", GridViewport.ENABLED);
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setBounds(10, 10, 700, 420);
		focusSink = new Button(shell, SWT.PUSH);
		focusSink.setBounds(680, 400, 10, 10);
	}

	@After
	public void tearDown() {
		if (shell != null) {
			shell.dispose();
		}
	}

	@Test
	public void sparseAndEagerChildrenAnswerAndPaintAlike() {
		for (int seed = 0; seed < 4; seed++) {
			final Side sparse = new Side(true, 5);
			final Side eager = new Side(false, 345);
			shell.open();
			// neither Grid has the focus, which would draw a focus rectangle in one of them
			focusSink.setFocus();
			flush();
			final Random random = new Random(0x5BA75EL + seed);
			final StringBuilder log = new StringBuilder("seed " + seed + "\n");
			for (int step = 0; step < 120; step++) {
				final int[] op = { random.nextInt(9), random.nextInt(1 << 20), random.nextInt(1 << 20) };
				log.append(step).append(": ").append(java.util.Arrays.toString(op)).append('\n');
				final String expected = eager.apply(op);
				final String actual = sparse.apply(op);
				flush();
				final String expectedState = expected + " | " + eager.state();
				assertEquals("step " + step + "\n" + log, expectedState, actual + " | " + sparse.state());
				log.append("   = ").append(expectedState, 0, Math.min(200, expectedState.length())).append('\n');
				if (step % 10 == 9) {
					assertArrayEquals("pixels after step " + step + "\n" + log, eager.pixels(), sparse.pixels());
				}
			}
			sparse.grid.dispose();
			eager.grid.dispose();
		}
	}

	@Test
	public void aMillionChildrenMakeNoItemUntilRead() {
		final Grid grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL);
		grid.setBounds(0, 0, 300, 300);
		new GridColumn(grid, SWT.NONE).setWidth(200);
		final int[] requests = { 0 };
		grid.addListener(SWT.SetData, e -> {
			requests[0]++;
			((GridItem) e.item).setText("row " + e.index);
		});
		final GridItem root = new GridItem(grid, SWT.NONE);
		new GridItem(grid, SWT.NONE);
		root.setItemCount(1_000_000);
		assertEquals(1_000_000, root.getItemCount());
		assertEquals(1_000_002, grid.getItemCount());
		assertEquals(2, grid.virtualMaterializedItemCount());
		root.setExpanded(true);
		shell.open();
		flush();
		final int painted = grid.virtualMaterializedItemCount();
		assertTrue(painted + " items after painting", painted < 40);
		final GridItem last = root.getItem(999_999);
		assertEquals("row 999999", last.getText(0));
		assertEquals(999_999, root.indexOf(last));
		assertEquals(1_000_000, last.getRowIndex());
		root.setItemCount(10);
		assertEquals(12, grid.getItemCount());
		assertTrue(last.isDisposed());
		root.dispose();
		assertEquals(1, grid.getItemCount());
	}

	@Test
	public void removedChildrenSendDisposeOnlyForTheirItems() {
		final Grid grid = new Grid(shell, SWT.VIRTUAL);
		final GridItem root = new GridItem(grid, SWT.NONE);
		root.setItemCount(6);
		final StringBuilder events = new StringBuilder();
		for (final int i : new int[] { 4, 2 }) {
			final GridItem child = root.getItem(i);
			child.setItemCount(2);
			child.getItem(1).addListener(SWT.Dispose, e -> events.append("c").append(i).append(".1 "));
			child.addListener(SWT.Dispose, e -> events.append("c").append(i).append(' '));
		}
		root.setItemCount(1);
		assertEquals("c2 c2.1 c4 c4.1 ", events.toString());
		assertEquals(2, grid.getItemCount());
		// the root only: the child left was never read
		assertEquals(1, grid.virtualMaterializedItemCount());
	}

	private void flush() {
		while (display.readAndDispatch()) {
			// drain paints
		}
	}

	/** A Grid whose children come from setItemCount (sparse) or from one constructor call each. */
	private final class Side {
		final Grid grid;
		final boolean sparse;
		final StringBuilder events = new StringBuilder();

		Side(final boolean sparse, final int x) {
			this.sparse = sparse;
			grid = new Grid(shell, SWT.VIRTUAL | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
			grid.setBounds(x, 5, 330, 380);
			grid.setHeaderVisible(true);
			final GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setTree(true);
			column.setWidth(220);
			for (int r = 0; r < ROOTS; r++) {
				final GridItem root = new GridItem(grid, SWT.NONE);
				root.setData("root", Integer.valueOf(r));
				count(root, r % 3 == 0 ? 25 : r % 3 == 1 ? 0 : 4);
			}
			grid.addListener(SWT.SetData, e -> {
				final GridItem item = (GridItem) e.item;
				final String path = path(item);
				events.append("SetData(").append(path).append(' ').append(e.index).append(") ");
				item.setText(0, "t" + path);
			});
		}

		/* sets a child count: setItemCount, or constructors and dispose */
		void count(final GridItem item, final int count) {
			if (sparse) {
				item.setItemCount(count);
				return;
			}
			// without requesting data: the sparse side's setItemCount requests none
			for (int i = item.childCount(); i < count; i++) {
				new GridItem(item, SWT.NONE);
			}
			for (int i = item.childCount() - 1; i >= count; i--) {
				item.childAt(i).dispose();
			}
		}

		String path(final GridItem item) {
			final GridItem parent = item.getParentItem();
			return parent == null ? "R" + item.getData("root") : path(parent) + "." + parent.indexOf(item);
		}

		String apply(final int[] op) {
			final GridItem root = grid.getRootItem(op[1] % ROOTS);
			switch (op[0]) {
			case 0:
			case 1:
				root.setExpanded(!root.isExpanded());
				return "expanded " + root.isExpanded();
			case 2:
				grid.setTopIndex(op[2] % Math.max(1, grid.getItemCount()));
				return "top " + grid.getTopIndex();
			case 3:
				count(root, op[2] % 4 == 0 ? 0 : op[2] % 40);
				return "count " + root.getItemCount();
			case 4: {
				final int n = root.getItemCount();
				if (n == 0) {
					return "no child";
				}
				final GridItem child = root.getItem(op[2] % n);
				count(child, op[2] % 3);
				return "child " + child.getText(0) + " count " + child.getItemCount();
			}
			case 5:
				grid.select(op[2] % Math.max(1, grid.getItemCount()));
				return "selected " + grid.getSelectionCount();
			case 6: {
				final int n = root.getItemCount();
				if (n == 0) {
					return "no child";
				}
				root.clear(op[2] % n, (op[2] & 1) != 0);
				return "cleared";
			}
			case 7:
				grid.setItemHeight(16 + op[2] % 9);
				return "height " + grid.getItemHeight();
			default:
				grid.showItem(grid.getItem(op[2] % grid.getItemCount()));
				return "shown, top " + grid.getTopIndex();
			}
		}

		String state() {
			final String result = "count " + grid.getItemCount() + " top " + grid.getTopIndex() + " bottom "
					+ grid.getBottomIndex() + " vbar " + grid.getVerticalBar().getSelection() + "/"
					+ grid.getVerticalBar().getMaximum() + " sel " + java.util.Arrays.toString(grid.getSelectionIndices())
					+ " | " + events;
			events.setLength(0);
			return result;
		}

		byte[] pixels() {
			final Image image = new Image(display, grid.getSize().x, grid.getSize().y);
			final GC gc = new GC(image);
			grid.print(gc);
			gc.dispose();
			final ImageData data = image.getImageData();
			image.dispose();
			return data.data;
		}
	}
}
