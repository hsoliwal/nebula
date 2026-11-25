/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Synexia contributors - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.oscilloscope.multichannel;

/**
 * Packed FIFO ring of column envelopes ({@code first, min, max, ends} as four ints per column, {@code ends} being
 * {@link #ends(int, int)} of the column's first and last sample step; the column's last value travels in the
 * channel's {@link Oscilloscope.IntegerFiFoCircularStack}). Same capacity rule as that stack: when full, a push drops
 * the oldest entry, so both stay in lock step. Not thread safe; the scope guards it.
 */
final class EnvelopeRing {

	/** Ints per entry. */
	static final int STRIDE = 4;

	/** {@link #ends(int, int)} of a column without a step (all its samples equal). */
	static final int FLAT = 4;

	/**
	 * Packs the direction of a column's first and last non-zero sample step, each -1 (down), 0 (no step) or 1 (up).
	 *
	 * @param depart the sign of the first step away from the column's first sample
	 * @param arrive the sign of the last step onto the column's last sample
	 * @return the packed directions, 0 to 8
	 */
	static int ends(int depart, int arrive) {
		return (depart + 1) * 3 + arrive + 1;
	}

	/** @return the departure sign packed by {@link #ends(int, int)} */
	static int depart(int ends) {
		return ends / 3 - 1;
	}

	/** @return the arrival sign packed by {@link #ends(int, int)} */
	static int arrive(int ends) {
		return ends % 3 - 1;
	}

	private final int capacity;
	private final int[] ring;
	private int bottom;
	private int load;

	EnvelopeRing(int capacity) {
		this.capacity = Math.max(1, capacity);
		this.ring = new int[this.capacity * STRIDE];
	}

	/** New ring holding the newest {@code capacity} entries of {@code old} (old is emptied), as the stack does. */
	EnvelopeRing(int capacity, EnvelopeRing old) {
		this(capacity);
		int[] e = new int[STRIDE];
		while (old.load > 0) {
			old.pop(e, 0);
			push(e[0], e[1], e[2], e[3]);
		}
	}

	int load() {
		return load;
	}

	void push(int first, int min, int max, int ends) {
		int slot;
		if (load == capacity) {
			slot = bottom;
			bottom = bottom + 1 == capacity ? 0 : bottom + 1;
		} else {
			slot = bottom + load;
			if (slot >= capacity) {
				slot -= capacity;
			}
			load++;
		}
		int o = slot * STRIDE;
		ring[o] = first;
		ring[o + 1] = min;
		ring[o + 2] = max;
		ring[o + 3] = ends;
	}

	/** Pops the oldest entry into {@code out[at .. at + 3]}; a flat zero column if empty. */
	void pop(int[] out, int at) {
		if (load == 0) {
			out[at] = 0;
			out[at + 1] = 0;
			out[at + 2] = 0;
			out[at + 3] = FLAT;
			return;
		}
		int o = bottom * STRIDE;
		out[at] = ring[o];
		out[at + 1] = ring[o + 1];
		out[at + 2] = ring[o + 2];
		out[at + 3] = ring[o + 3];
		bottom = bottom + 1 == capacity ? 0 : bottom + 1;
		load--;
	}
}
