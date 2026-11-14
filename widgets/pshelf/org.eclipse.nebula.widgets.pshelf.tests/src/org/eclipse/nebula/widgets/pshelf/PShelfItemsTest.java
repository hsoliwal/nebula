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
package org.eclipse.nebula.widgets.pshelf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Items of a PShelf: an item inserted at an index outside 0..count is rejected; a PShelf subclass that overrides
 * getItems() decides the count and the first item; a disposed shelf fails as a disposed widget; on a shown shelf every
 * item can be selected.
 */
public class PShelfItemsTest {

	private Display display;
	private Shell shell;

	/** A PShelf whose getItems() swaps the first and the last item and counts its calls. */
	private static final class SwappingShelf extends PShelf {
		int itemCalls;

		SwappingShelf(final Shell parent) {
			super(parent, SWT.NONE);
		}

		@Override
		public PShelfItem[] getItems() {
			itemCalls++;
			final PShelfItem[] items = super.getItems();
			if (items.length > 1) {
				final PShelfItem first = items[0];
				items[0] = items[items.length - 1];
				items[items.length - 1] = first;
			}
			return items;
		}
	}

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void insertionIndexIsChecked() {
		for (int count = 0; count <= 6; count++) {
			final PShelf shelf = new PShelf(shell, SWT.NONE);
			for (int i = 0; i < count; i++) {
				new PShelfItem(shelf, SWT.NONE);
			}
			rejected(shelf, -1);
			rejected(shelf, count + 1);
			assertEquals(count, shelf.getItems().length);
			for (int index = 0; index <= count; index++) {
				final PShelfItem item = new PShelfItem(shelf, SWT.NONE, index);
				assertSame(item, shelf.getItems()[index]);
				item.dispose();
			}
			shelf.dispose();
		}
	}

	@Test
	public void firstItem() {
		final PShelf shelf = new PShelf(shell, SWT.NONE);
		final PShelfItem first = new PShelfItem(shelf, SWT.NONE);
		final PShelfItem last = new PShelfItem(shelf, SWT.NONE);
		assertTrue(shelf.isFirstItem(first));
		assertFalse(shelf.isFirstItem(last));
		assertFalse(shelf.isFirstItem(null));
	}

	@Test
	public void subclassItemsDecide() {
		final SwappingShelf shelf = new SwappingShelf(shell);
		final PShelfItem first = new PShelfItem(shelf, SWT.NONE);
		new PShelfItem(shelf, SWT.NONE);
		final PShelfItem last = new PShelfItem(shelf, SWT.NONE);
		shelf.itemCalls = 0;
		assertTrue("the subclass order decides", shelf.isFirstItem(last));
		assertFalse(shelf.isFirstItem(first));
		assertEquals(2, shelf.itemCalls);
		shelf.itemCalls = 0;
		final PShelfItem inserted = new PShelfItem(shelf, SWT.NONE, 3);
		assertTrue("the subclass count decides", shelf.itemCalls >= 1);
		assertEquals(4, shelf.getItems().length);
		inserted.dispose();
		rejected(shelf, 4);
	}

	@Test
	public void disposedShelfFails() {
		final PShelf shelf = new PShelf(shell, SWT.NONE);
		final PShelfItem item = new PShelfItem(shelf, SWT.NONE);
		shelf.dispose();
		try {
			shelf.isFirstItem(item);
			fail("disposed shelf");
		} catch (final SWTException expected) {
			assertEquals(SWT.ERROR_WIDGET_DISPOSED, expected.code);
		}
	}

	@Test
	public void everyItemCanBeSelectedOnAShownShelf() {
		shell.setLayout(new FillLayout());
		final PShelf shelf = new PShelf(shell, SWT.BORDER | SWT.SIMPLE);
		final PShelfItem[] children = new PShelfItem[8];
		for (int i = 0; i < children.length; i++) {
			children[i] = new PShelfItem(shelf, SWT.NONE);
			children[i].setText("Item " + i);
		}
		shell.setSize(550, 650);
		shell.open();
		for (final PShelfItem child : children) {
			shelf.setSelection(child);
			assertSame(child, shelf.getSelection());
			shelf.redraw();
			while (display.readAndDispatch()) {
				// settle
			}
		}
	}

	private static void rejected(final PShelf shelf, final int index) {
		try {
			new PShelfItem(shelf, SWT.NONE, index);
			fail("index " + index + " accepted");
		} catch (final IllegalArgumentException expected) {
			// SWT.ERROR_INVALID_RANGE
		}
	}
}
