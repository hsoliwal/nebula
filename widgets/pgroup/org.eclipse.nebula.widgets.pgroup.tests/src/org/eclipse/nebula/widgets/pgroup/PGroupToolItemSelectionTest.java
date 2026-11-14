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
package org.eclipse.nebula.widgets.pgroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Selecting a CHECK or RADIO tool item of a PGroup clears the other items and selects it; other styles ignore
 * setSelection. A PGroup subclass is asked for its tool items, and a PGroupToolItem subclass sees its setSelection(false)
 * call, exactly as before.
 */
public class PGroupToolItemSelectionTest {

	private Shell shell;

	/** A PGroup that counts getToolItems() calls. */
	private static final class CountingGroup extends PGroup {
		int itemCalls;

		CountingGroup(final Shell parent) {
			super(parent, SWT.NONE);
		}

		@Override
		public PGroupToolItem[] getToolItems() {
			itemCalls++;
			return super.getToolItems();
		}
	}

	/** A PGroupToolItem that records its setSelection calls. */
	private static final class RecordingItem extends PGroupToolItem {
		final List<Boolean> calls = new ArrayList<>();

		RecordingItem(final PGroup parent, final int style) {
			super(parent, style);
		}

		@Override
		public void setSelection(final boolean selection) {
			calls.add(Boolean.valueOf(selection));
			super.setSelection(selection);
		}
	}

	@Before
	public void setUp() {
		shell = new Shell(Display.getDefault());
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void selectingAnItemClearsTheOthers() {
		for (final int style : new int[] { SWT.RADIO, SWT.CHECK }) {
			for (int size = 1; size <= 12; size++) {
				final PGroup group = new PGroup(shell, SWT.NONE);
				final PGroupToolItem[] items = items(group, size, style);
				for (int selected = 0; selected < size; selected++) {
					items[selected].setSelection(true);
					assertOnly(items, selected);
				}
				items[size - 1].setSelection(false);
				assertOnly(items, -1);
				group.dispose();
			}
		}
	}

	@Test
	public void pushItemsIgnoreSelection() {
		final PGroup group = new PGroup(shell, SWT.NONE);
		final PGroupToolItem[] items = items(group, 3, SWT.PUSH);
		items[1].setSelection(true);
		assertOnly(items, -1);
	}

	@Test
	public void groupSubclassIsAskedForItsItems() {
		final CountingGroup group = new CountingGroup(shell);
		final PGroupToolItem[] items = items(group, 5, SWT.RADIO);
		items[2].setSelection(true);
		assertEquals(1, group.itemCalls);
		items[4].setSelection(true);
		assertEquals(2, group.itemCalls);
		assertOnly(items, 4);
	}

	@Test
	public void itemSubclassSeesItsDeselection() {
		final PGroup group = new PGroup(shell, SWT.NONE);
		final PGroupToolItem first = new PGroupToolItem(group, SWT.RADIO);
		final RecordingItem recording = new RecordingItem(group, SWT.RADIO);
		final PGroupToolItem last = new PGroupToolItem(group, SWT.RADIO);
		recording.setSelection(true);
		assertEquals(2, recording.calls.size());
		assertTrue(recording.calls.get(0).booleanValue());
		assertFalse("cleared through its own setSelection", recording.calls.get(1).booleanValue());
		assertTrue(recording.getSelection());
		recording.calls.clear();
		last.setSelection(true);
		assertEquals(1, recording.calls.size());
		assertFalse(recording.calls.get(0).booleanValue());
		assertFalse(first.getSelection());
		assertFalse(recording.getSelection());
		assertTrue(last.getSelection());
	}

	private static PGroupToolItem[] items(final PGroup group, final int size, final int style) {
		final PGroupToolItem[] items = new PGroupToolItem[size];
		for (int i = 0; i < size; i++) {
			items[i] = new PGroupToolItem(group, style);
		}
		return items;
	}

	private static void assertOnly(final PGroupToolItem[] items, final int selected) {
		for (int i = 0; i < items.length; i++) {
			assertEquals("item " + i, i == selected, items[i].getSelection());
		}
	}
}
