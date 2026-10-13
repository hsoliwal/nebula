/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.ctree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * CTree viewport: the painted window and the top item come from a binary search over the laid-out item bottoms and
 * equal the original full scan at every scroll position; a 1,000,000-node tree paints only the rows in view; the body
 * never takes focus.
 */
public class CTreeViewport_Test {
	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setBounds(10, 10, 500, 400);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void paintedWindowAndTopItemMatchTheFullScan() {
		CTree tree = tree(300, 2);
		tree.getItems()[3].setExpanded(true);
		tree.getItems()[200].setExpanded(true);
		shell.open();
		pump();
		List<?> visible = tree.items(false);
		assertTrue(tree.layout.isLaidOut(visible));
		int max = tree.vBar.getMaximum();
		for (int y = 0; y <= max; y += 7) {
			tree.setScrollPosition(new org.eclipse.swt.graphics.Point(0, y));
			int top = tree.getScrollPosition().y;
			int bot = top + tree.body.getClientArea().height;
			assertEquals("painted @" + y, scan(visible, top, bot), tree.getPaintedItems());
			assertSame("top @" + y, scanTop(visible, top), tree.getTopItem());
		}
	}

	@Test
	public void millionNodeTreePaintsOnlyVisibleRows() {
		CTree tree = tree(100_000, 9);
		assertEquals(100_000, tree.getItemCount());
		shell.open();
		pump();
		int rowsInView = tree.body.getClientArea().height / Math.max(1, tree.getItemHeight()) + 2;
		List<?> painted = tree.getPaintedItems();
		assertTrue("painted " + painted.size(), painted.size() > 0 && painted.size() <= rowsInView);
		assertSame(tree.getItem(0), tree.getTopItem());

		CTreeItem last = tree.getItem(99_999);
		tree.showItem(last);
		pump();
		painted = tree.getPaintedItems();
		assertTrue("painted at end " + painted.size(), painted.contains(last) && painted.size() <= rowsInView);

		last.setExpanded(true);
		pump();
		assertEquals(100_009, tree.items(false).size());
		assertTrue(tree.getPaintedItems().size() <= rowsInView);
	}

	@Test
	public void stateSurvivesScrollingAwayAndBack() {
		CTree tree = tree(2_000, 2);
		shell.open();
		pump();
		tree.getItem(1).setExpanded(true);
		tree.setSelection(tree.getItem(3));
		tree.getItem(2).getCell(0).setChecked(true);
		tree.setTopItem(tree.getItem(0));
		pump();
		String before = state(tree);
		String pixels = pixels(tree);

		tree.showItem(tree.getItem(1_999));
		pump();
		tree.setTopItem(tree.getItem(0));
		pump();
		assertEquals(before, state(tree));
		assertEquals(pixels, pixels(tree));
	}

	private static String state(CTree tree) {
		StringBuilder s = new StringBuilder();
		for (CTreeItem item : tree.getSelection()) {
			s.append(item.getText()).append(',');
		}
		s.append('|').append(tree.getTopItem().getText()).append('|').append(tree.getPaintedItems().size());
		for (int i = 0; i < 6; i++) {
			CTreeItem item = tree.getItem(i);
			s.append('|').append(item.getText()).append(item.getExpanded()).append(item.getCell(0).getChecked());
		}
		return s.toString();
	}

	/** SHA-256 of the control's own rendering (Control.print), independent of window occlusion. */
	private static String pixels(org.eclipse.swt.widgets.Control control) {
		org.eclipse.swt.graphics.Point size = control.getSize();
		org.eclipse.swt.graphics.Image image = new org.eclipse.swt.graphics.Image(control.getDisplay(), size.x, size.y);
		org.eclipse.swt.graphics.GC gc = new org.eclipse.swt.graphics.GC(image);
		try {
			control.print(gc);
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
			md.update(image.getImageData().data);
			StringBuilder hex = new StringBuilder();
			for (byte b : md.digest()) {
				hex.append(String.format("%02x", b & 0xff));
			}
			return hex.toString();
		} catch (java.security.NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		} finally {
			gc.dispose();
			image.dispose();
		}
	}

	@Test
	public void bodyNeverTakesFocus() {
		CTree tree = tree(5, 0);
		shell.open();
		pump();
		assertTrue((tree.body.getStyle() & SWT.NO_FOCUS) != 0);
		tree.setFocus();
		pump();
		assertSame(tree, display.getFocusControl());
	}

	/** The pre-image scan of getPaintedItems(), over the same laid-out items. */
	private static List<Object> scan(List<?> visible, int top, int bot) {
		List<Object> list = new ArrayList<>();
		int itop = 0;
		boolean painting = false;
		for (Object o : visible) {
			int ibot = ((CTreeItem) o).getBottom();
			if (itop <= top && top < ibot) {
				painting = true;
			}
			if (painting) {
				list.add(o);
			}
			if (itop < bot && bot <= ibot) {
				break;
			}
			itop = ibot;
		}
		return list;
	}

	private static Object scanTop(List<?> visible, int top) {
		int itop = 0;
		for (Object o : visible) {
			int ibot = ((CTreeItem) o).getBottom();
			if (itop <= top && top < ibot) {
				return o;
			}
			itop = ibot;
		}
		return null;
	}

	private CTree tree(int roots, int kids) {
		CTree tree = new CTree(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		CTreeColumn column = new CTreeColumn(tree, SWT.LEFT);
		column.setText("Name");
		column.setWidth(300);
		tree.setHeaderVisible(true);
		for (int i = 0; i < roots; i++) {
			CTreeItem root = new CTreeItem(tree, SWT.NONE);
			root.setText("n" + i);
			for (int j = 0; j < kids; j++) {
				new CTreeItem(root, SWT.NONE).setText("n" + i + "." + j);
			}
		}
		return tree;
	}

	private void pump() {
		for (int i = 0; i < 100_000 && display.readAndDispatch(); i++) {
			// drain paint and layout work
		}
	}
}
