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
package org.eclipse.nebula.widgets.m3.internal;

import java.util.Arrays;

import org.eclipse.swt.widgets.Control;

/**
 * The resident pages of a page stack, held in lanes: page index, last-shown tick and control per slot. Lookup and the
 * least recently used victim are one linear scan over the slots, which are as many as the residency budget (a handful).
 * No entry objects; no allocation once the lanes have grown to the budget. Pattern: M3jdk21
 * <code>com.m3.collections.M3BoundedCache</code> (lanes, no entries), with exact LRU instead of CLOCK because the
 * release order is visible to the page provider (docs/m3/custom-widgets/ALGORITHM_CATALOGUE.tsv, ALG-CW-STACK-LRU).
 */
public final class M3ResidentPages {
	private int[] pages = new int[4];
	private long[] ticks = new long[4];
	private Control[] controls = new Control[4];
	private int size;
	private long clock;

	/** @return the number of resident pages */
	public int size() {
		return size;
	}

	/**
	 * @param page a page index
	 * @return the slot of the page, or -1 when it is not resident
	 */
	public int slotOf(int page) {
		for (int slot = 0; slot < size; slot++) {
			if (pages[slot] == page) {
				return slot;
			}
		}
		return -1;
	}

	/** @return the page index held by a slot */
	public int page(int slot) {
		return pages[slot];
	}

	/** @return the control held by a slot */
	public Control control(int slot) {
		return controls[slot];
	}

	/** Marks a slot as the most recently shown. */
	public void touch(int slot) {
		ticks[slot] = ++clock;
	}

	/**
	 * Adds a page as the most recently shown.
	 *
	 * @return its slot
	 */
	public int add(int page, Control control) {
		if (size == pages.length) {
			int capacity = size * 2;
			pages = Arrays.copyOf(pages, capacity);
			ticks = Arrays.copyOf(ticks, capacity);
			controls = Arrays.copyOf(controls, capacity);
		}
		pages[size] = page;
		controls[size] = control;
		ticks[size] = ++clock;
		return size++;
	}

	/** Removes a slot; the last slot moves into its place. */
	public void remove(int slot) {
		int last = --size;
		pages[slot] = pages[last];
		ticks[slot] = ticks[last];
		controls[slot] = controls[last];
		controls[last] = null;
	}

	/**
	 * @param keep a page that must not be chosen (the top page), or -1
	 * @return the slot shown least recently, other than <code>keep</code>'s, or -1 when there is none
	 */
	public int leastRecent(int keep) {
		int victim = -1;
		for (int slot = 0; slot < size; slot++) {
			if (pages[slot] != keep && (victim < 0 || ticks[slot] < ticks[victim])) {
				victim = slot;
			}
		}
		return victim;
	}

	/** @return the resident page indices in ascending order */
	public int[] pages() {
		int[] result = Arrays.copyOf(pages, size);
		Arrays.sort(result);
		return result;
	}
}
