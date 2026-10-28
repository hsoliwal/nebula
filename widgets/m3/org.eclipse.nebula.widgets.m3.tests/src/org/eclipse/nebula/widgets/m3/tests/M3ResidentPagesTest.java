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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.nebula.widgets.m3.internal.M3ResidentPages;
import org.junit.Test;

/**
 * The resident-page lanes: lookup, growth, removal by swap, least recently shown victim.
 */
public class M3ResidentPagesTest {

	@Test
	public void addLookupAndGrow() {
		M3ResidentPages pages = new M3ResidentPages();
		for (int i = 0; i < 10; i++) {
			assertEquals(i, pages.add(100 + i, null));
		}
		assertEquals(10, pages.size());
		assertEquals(3, pages.slotOf(103));
		assertEquals(-1, pages.slotOf(7));
		assertEquals(109, pages.page(9));
		assertNull(pages.control(9));
	}

	@Test
	public void removeMovesTheLastSlot() {
		M3ResidentPages pages = new M3ResidentPages();
		pages.add(1, null);
		pages.add(2, null);
		pages.add(3, null);
		pages.remove(0);
		assertEquals(2, pages.size());
		assertEquals(3, pages.page(0));
		assertEquals(0, pages.slotOf(3));
		assertArrayEquals(new int[] { 2, 3 }, pages.pages());
		pages.remove(1);
		assertArrayEquals(new int[] { 3 }, pages.pages());
	}

	@Test
	public void leastRecentSkipsTheKeptPage() {
		M3ResidentPages pages = new M3ResidentPages();
		assertEquals(-1, pages.leastRecent(-1));
		pages.add(5, null);
		pages.add(6, null);
		pages.add(7, null);
		assertEquals(0, pages.leastRecent(-1));
		assertEquals(1, pages.leastRecent(5));
		pages.touch(0);
		assertEquals(1, pages.leastRecent(-1));
		pages.touch(1);
		assertEquals(2, pages.leastRecent(-1));
		assertEquals(0, pages.leastRecent(7));
	}

	@Test
	public void onlyTheKeptPageLeavesNoVictim() {
		M3ResidentPages pages = new M3ResidentPages();
		pages.add(4, null);
		assertEquals(-1, pages.leastRecent(4));
	}
}
