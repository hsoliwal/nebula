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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link GridSelectionList} answers as the stock {@code ArrayList} of selected items: random adds,
 * removals (by item and by index), reads, clears and bulk adds, compared after every step.
 */
public class GridSelectionList_Test {
	private Display display;
	private Shell shell;

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
	public void randomOperationsAnswerAsAnArrayList() {
		final Grid grid = new Grid(shell, SWT.MULTI);
		final GridItem[] items = new GridItem[200];
		for (int i = 0; i < items.length; i++) {
			items[i] = new GridItem(grid, SWT.NONE);
		}
		final GridSelectionList list = new GridSelectionList();
		final List<GridItem> reference = new ArrayList<>();
		final Random random = new Random(0x5E1EC7L);
		for (int step = 0; step < 20_000; step++) {
			final GridItem item = items[random.nextInt(items.length)];
			switch (random.nextInt(10)) {
			case 0:
			case 1:
			case 2:
				if (!reference.contains(item)) {
					list.add(item);
					reference.add(item);
				}
				break;
			case 3:
			case 4:
				assertEquals(java.lang.Boolean.valueOf(reference.remove(item)), java.lang.Boolean.valueOf(list.remove(item)));
				break;
			case 5:
				if (!reference.isEmpty()) {
					final int index = random.nextInt(reference.size());
					assertEquals(reference.remove(index), list.remove(index));
				}
				break;
			case 6:
				if (!reference.isEmpty()) {
					final int index = random.nextInt(reference.size());
					assertEquals(reference.get(index), list.get(index));
					assertEquals(reference.get(0), list.get(0));
				}
				break;
			case 7:
				assertEquals(reference.indexOf(item), list.indexOf(item));
				break;
			case 8:
				if (random.nextInt(40) == 0) {
					list.clear();
					reference.clear();
				}
				break;
			default:
				if (random.nextInt(60) == 0) {
					list.clear();
					reference.clear();
					final List<GridItem> some = new ArrayList<>(Arrays.asList(items).subList(0, random.nextInt(items.length)));
					list.addAll(some);
					reference.addAll(some);
				}
			}
			assertEquals(reference.size(), list.size());
			assertEquals(java.lang.Boolean.valueOf(reference.contains(item)), java.lang.Boolean.valueOf(list.contains(item)));
			if (step % 97 == 0) {
				assertArrayEquals(reference.toArray(new GridItem[0]), list.toArray(new GridItem[0]));
				final List<GridItem> iterated = new ArrayList<>();
				list.forEach(iterated::add);
				assertEquals(reference, iterated);
			}
		}
	}
}
