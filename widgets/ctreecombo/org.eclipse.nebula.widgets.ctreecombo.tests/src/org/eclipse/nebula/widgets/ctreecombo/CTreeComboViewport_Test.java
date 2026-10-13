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
package org.eclipse.nebula.widgets.ctreecombo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * CTreeCombo: the drop-down is built on first use, not at construction; a lazily built drop-down has the bare native
 * items the eager widget had before its first drop; with {@code SWT.VIRTUAL} the drop-down binds only the rows the
 * native tree asks for, and item APIs bind only the item's path.
 */
public class CTreeComboViewport_Test {
	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new GridLayout());
		shell.setBounds(10, 10, 400, 300);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void dropDownIsNotBuiltAtConstruction() {
		int shells = display.getShells().length;
		CTreeCombo combo = new CTreeCombo(shell, SWT.BORDER | SWT.READ_ONLY);
		populate(combo, 5, 2);
		assertNull("no drop-down tree before first use", combo.tree);
		assertEquals("no popup shell before first use", shells, display.getShells().length);
		assertEquals(5, combo.getItemCount());
		assertNull(combo.tree);
	}

	@Test
	public void lazilyBuiltDropDownHasBareNativeItemsLikeTheEagerWidget() {
		CTreeCombo combo = new CTreeCombo(shell, SWT.BORDER | SWT.READ_ONLY);
		populate(combo, 3, 1);
		Tree tree = combo.tree();
		assertEquals(3, tree.getItemCount());
		for (TreeItem row : tree.getItems()) {
			assertEquals("labels reach native items only when a drop rebuilds them", "", row.getText());
			assertTrue(row.getData(CTreeComboItem.DATA_ID) instanceof CTreeComboItem);
		}
	}

	@Test
	public void virtualDropDownBindsOnlyRequestedRowsAndItemPaths() throws Exception {
		CTreeCombo combo = new CTreeCombo(shell, SWT.BORDER | SWT.READ_ONLY | SWT.VIRTUAL);
		populate(combo, 50_000, 3);
		shell.open();
		pump();
		java.lang.reflect.Method drop = CTreeCombo.class.getDeclaredMethod("dropDown", boolean.class);
		drop.setAccessible(true);
		drop.invoke(combo, Boolean.TRUE);
		pump();
		Tree tree = combo.tree;
		assertTrue((tree.getStyle() & SWT.VIRTUAL) != 0);
		assertEquals(50_000, tree.getItemCount());
		assertTrue("rows bound after opening: " + bound(tree), bound(tree) < 200);

		CTreeComboItem deep = combo.getItem(40_000).getItem(2);
		combo.setSelection(new CTreeComboItem[] { deep });
		assertSame(deep, combo.getSelection()[0]);
		assertTrue("an item API binds only the item path: " + bound(tree), bound(tree) < 400);
		combo.getItem(40_000).setExpanded(true);
		assertTrue(combo.getItem(40_000).getExpanded());
		drop.invoke(combo, Boolean.FALSE);
		pump();
	}

	private static int bound(Tree tree) {
		int bound = 0;
		// getItems on a virtual tree does not request data; getData only reads what was bound
		for (TreeItem row : tree.getItems()) {
			if (row.getData(CTreeComboItem.DATA_ID) != null) {
				bound++;
			}
		}
		return bound;
	}

	private static void populate(CTreeCombo combo, int roots, int kids) {
		for (int i = 0; i < roots; i++) {
			CTreeComboItem root = new CTreeComboItem(combo, SWT.NONE);
			root.setText("n" + i);
			for (int j = 0; j < kids; j++) {
				new CTreeComboItem(root, SWT.NONE).setText("n" + i + "." + j);
			}
		}
	}

	private void pump() {
		for (int i = 0; i < 10_000 && display.readAndDispatch(); i++) {
			// drain pending work
		}
	}
}
