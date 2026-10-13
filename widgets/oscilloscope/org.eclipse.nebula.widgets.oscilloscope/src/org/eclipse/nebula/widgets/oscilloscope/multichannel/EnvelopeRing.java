/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.nebula.widgets.oscilloscope.multichannel;

/**
 * Packed FIFO ring of column envelopes ({@code first, min, max} as three ints per column; the column's last value
 * travels in the channel's {@link Oscilloscope.IntegerFiFoCircularStack}). Same capacity rule as that stack: when
 * full, a push drops the oldest entry, so both stay in lock step. Not thread safe; the scope guards it.
 */
final class EnvelopeRing {

	private final int capacity;
	private final int[] ring;
	private int bottom;
	private int load;

	EnvelopeRing(int capacity) {
		this.capacity = Math.max(1, capacity);
		this.ring = new int[this.capacity * 3];
	}

	/** New ring holding the newest {@code capacity} entries of {@code old} (old is emptied), as the stack does. */
	EnvelopeRing(int capacity, EnvelopeRing old) {
		this(capacity);
		int[] e = new int[3];
		while (old.load > 0) {
			old.pop(e, 0);
			push(e[0], e[1], e[2]);
		}
	}

	int load() {
		return load;
	}

	void push(int first, int min, int max) {
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
		int o = slot * 3;
		ring[o] = first;
		ring[o + 1] = min;
		ring[o + 2] = max;
	}

	/** Pops the oldest entry into {@code out[at .. at + 2]}; zeros if empty. */
	void pop(int[] out, int at) {
		if (load == 0) {
			out[at] = 0;
			out[at + 1] = 0;
			out[at + 2] = 0;
			return;
		}
		int o = bottom * 3;
		out[at] = ring[o];
		out[at + 1] = ring[o + 1];
		out[at + 2] = ring[o + 2];
		bottom = bottom + 1 == capacity ? 0 : bottom + 1;
		load--;
	}
}
